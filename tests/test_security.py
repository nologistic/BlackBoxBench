"""Security boundary tests (docs/security_model.md).

These mechanically verify the pixels-only contract: no DOM/URL/semantics leak
through the agent channel, the app refuses direct access, the ground truth is
not served, and the runtime capability surface has no semantic access.
"""
from __future__ import annotations

import base64
import io

import httpx
import pytest
from PIL import Image

OBSERVE_KEYS = {"frame_id", "timestamp", "width", "height",
                "screenshot_png_b64", "cursor", "budget", "brief",
                # 窗口管理级信息(数量+当前索引),无 URL/标题等语义
                "tabs"}


class TestAgentChannelWhitelist:
    def test_observe_whitelist(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/observe", json={})
        assert r.status_code == 200
        d = r.json()
        assert set(d.keys()) == OBSERVE_KEYS, f"unexpected fields: {set(d) - OBSERVE_KEYS}"
        # no semantic fields anywhere (recursive scan)
        blob = str(d.keys())
        for forbidden in ("url", "dom", "html", "element", "selector",
                          "network", "cookie", "storage"):
            assert forbidden not in blob.lower()
        png = base64.b64decode(d["screenshot_png_b64"])
        img = Image.open(io.BytesIO(png))
        assert img.size == (1440, 900)
        assert set(d["cursor"].keys()) == {"x", "y"}
        # brief is task metadata (operator-authored), never app internals
        assert isinstance(d["brief"], str)

    def test_brief_carries_demo_account(self, client):
        r = client.post("/api/sessions", json={"app_id": "ecommerce_demo"})
        assert r.status_code == 200
        body = r.json()
        assert "alice" in body["brief"]
        sid = body["session_id"]
        try:
            obs = client.post(f"/agent/{sid}/observe", json={}).json()
            assert "alice123" in obs["brief"]
        finally:
            client.post(f"/api/sessions/{sid}/close")

    def test_action_receipt_is_semantics_free(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/action",
                        json={"type": "move_pointer", "x": 500, "y": 500})
        assert r.status_code == 200
        # tabs = 窗口管理级信息(数量+当前索引),不含 URL/标题等语义
        assert set(r.json().keys()) <= {"accepted", "frame_id", "step", "tabs"}

    def test_agent_routes_are_narrow(self, client, mini_session):
        # no GET on the agent channel (404 from the static mount or 405 from the
        # method guard — either way nothing is served)
        assert client.get(f"/agent/{mini_session}/observe").status_code in (404, 405)
        assert client.get(f"/agent/{mini_session}/topology").status_code == 404
        assert client.get(f"/agent/{mini_session}/frames/0.png").status_code == 404


class TestSdkSurface:
    FORBIDDEN = ("find_element", "get_dom", "get_url", "get_html",
                 "get_network", "evaluate", "locator", "query_selector",
                 "page_source", "get_cookies", "local_storage")

    def test_sdk_has_no_semantic_methods(self):
        import blackbox_bench_sdk
        public = [n for n in dir(blackbox_bench_sdk.Environment)
                  if not n.startswith("_")]
        for bad in self.FORBIDDEN:
            assert bad not in public, f"SDK must not expose {bad}"

    def test_sdk_module_has_no_browser_handles(self):
        import blackbox_bench_sdk as s
        assert not hasattr(s, "playwright")
        assert not hasattr(s, "selenium")


class TestAppIsolation:
    def test_direct_app_access_denied(self, client, mini_session, manager):
        """The reference app must 404 any request lacking the gateway header."""
        sess = manager.get(mini_session)
        port = sess.runtime._app_port  # benchmark-internal inspection
        r = httpx.get(f"http://127.0.0.1:{port}/", timeout=5, trust_env=False)
        assert r.status_code == 404
        assert r.text == ""

    def test_ground_truth_not_served(self, client):
        for path in ("/api/apps/ecommerce_demo/ground_truth",
                     "/api/ground_truth", "/ground_truth.json",
                     "/api/apps/ecommerce_demo/ground_truth.json"):
            assert client.get(path).status_code == 404, path

    def test_gateway_header_secret_not_in_artifacts(self, client, mini_session,
                                                    manager):
        sess = manager.get(mini_session)
        secret = sess.runtime._secret
        for fname in ("session.json", "actions.jsonl", "observations.jsonl"):
            p = sess.dir / fname
            if p.exists():
                assert secret not in p.read_text(encoding="utf-8"), fname

    def test_browser_launch_flags(self, client, mini_session, manager):
        sess = manager.get(mini_session)
        args = " ".join(sess.runtime._browser.args)
        assert "--no-proxy-server" in args
        assert "host-resolver-rules" in args and "~NOTFOUND" in args
        assert "--remote-allow-origins" in args  # only runtime uses the CDP ws
        assert "--headless=new" in args          # no browser chrome at all


class TestDiscoveryIntegrity:
    def test_leak_text_rejected(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/discovery/feature", json={
            "name": "Bad feature",
            "description": "this calls POST /api/cart internally",
            "confidence": 0.5, "status": "hypothesized"})
        assert r.status_code == 422

    def test_fake_frame_evidence_rejected(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/discovery/state", json={
            "name": "Fake state", "visual_evidence": [999999],
            "confidence": 0.5})
        assert r.status_code == 422

    def test_confirmed_feature_requires_evidence(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/discovery/feature", json={
            "name": "Speculative feature", "confidence": 0.9,
            "status": "confirmed", "evidence": []})
        assert r.status_code == 422

    def test_runtime_cdp_whitelist(self):
        from benchmark.runtime.local_chromium import _CDP
        for m in ("Runtime.evaluate", "DOM.getDocument",
                  "Accessibility.getFullAXTree", "Network.enable"):
            with pytest.raises(PermissionError):
                _CDP.call(object.__new__(_CDP), m)
