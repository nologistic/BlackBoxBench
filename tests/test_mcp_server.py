"""MCP stdio server tests: real subprocess, real JSON-RPC handshake, real HTTP
to an in-process uvicorn controller. No agent CLI, no VLM needed."""
from __future__ import annotations

import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

import httpx
import pytest

ROOT = Path(__file__).resolve().parent.parent
PY = ROOT / "vendor" / "python" / "python.exe"
if not PY.exists():  # fallback for other platforms
    PY = Path(sys.executable)

TEST_PORT = 7819


@pytest.fixture(scope="module")
def live_server():
    """Run the controller on a real loopback port in a daemon thread."""
    import uvicorn
    from benchmark.server import app  # noqa: F401  (ensures import works)
    config = uvicorn.Config("benchmark.server:app", host="127.0.0.1",
                            port=TEST_PORT, log_level="error")
    server = uvicorn.Server(config)
    t = threading.Thread(target=server.run, daemon=True)
    t.start()
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        try:
            httpx.get(f"http://127.0.0.1:{TEST_PORT}/api/apps",
                      timeout=2, trust_env=False)
            break
        except Exception:
            time.sleep(0.3)
    yield TEST_PORT
    server.should_exit = True


@pytest.fixture(scope="module")
def mcp_proc(live_server):
    http = httpx.Client(base_url=f"http://127.0.0.1:{live_server}",
                        trust_env=False, timeout=120)
    r = http.post("/api/sessions", json={"app_id": "miniapp", "budget": {
        "max_actions": 30, "max_duration_s": 600, "max_observations": 50}})
    assert r.status_code == 200, r.text
    sid = r.json()["session_id"]
    env = dict(os.environ)
    env["BBB_SESSION"] = sid
    env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
    # This fixture tests the exploration MCP protocol in a subprocess.  The
    # reproduction handoff itself is covered with a mocked sandbox below.
    env["BBB_REPRODUCTION_AUTOSTART"] = "0"
    proc = subprocess.Popen(
        [str(PY), "-m", "agents.cli_explorer.mcp_server"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        env=env, text=True, encoding="utf-8", errors="replace", cwd=str(ROOT))
    yield proc, sid
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:
        proc.kill()
    http.post(f"/api/sessions/{sid}/close")


def rpc(proc, method, params=None, mid=1):
    proc.stdin.write(json.dumps({"jsonrpc": "2.0", "id": mid,
                                 "method": method, "params": params or {}}) + "\n")
    proc.stdin.flush()
    while True:
        line = proc.stdout.readline()
        if not line:
            raise RuntimeError("mcp server closed stdout")
        msg = json.loads(line)
        if msg.get("id") == mid:
            return msg


def test_managed_finalize_immediately_enters_reproduction(monkeypatch, tmp_path):
    from agents.cli_explorer import mcp_server as managed

    sid = "sess_20260825_120000_abcd1234"
    topology = tmp_path / "runs" / sid / "functional_topology.json"
    topology.parent.mkdir(parents=True)
    topology.write_text('{"nodes":[],"edges":[]}', encoding="utf-8")
    frames = topology.parent / "frames"
    frames.mkdir()
    frame = frames / "frame_000007.png"
    frame.write_bytes(b"png")
    topology_md = topology.parent / "functional_topology.md"
    topology_md.write_text("# topology", encoding="utf-8")
    captured = {}

    class FakeReproduction:
        @classmethod
        def start(cls, **kwargs):
            captured.update(kwargs)
            return cls()

        def started_payload(self):
            return {"stage": "reproduction", "ready": True,
                    "handoff_id": "handoff", "output": "website_output/handoff"}

    monkeypatch.setattr(managed, "PROJECT_ROOT", tmp_path)
    monkeypatch.setattr(managed.benchmark_config, "RUNS_DIR",
                        tmp_path / "runs")
    monkeypatch.setattr(managed, "_session", sid)
    monkeypatch.setattr(managed, "_reproduction", None)
    monkeypatch.setattr(managed, "_review", None)
    monkeypatch.setattr(managed, "_bound_target", "app:miniapp")
    monkeypatch.setattr(managed, "ReproductionWorkspace", FakeReproduction)
    monkeypatch.delenv("BBB_REPRODUCTION_AUTOSTART", raising=False)
    specs = tmp_path / "review_specs"
    specs.mkdir()
    (specs / "miniapp.json").write_text(json.dumps({
        "checklist_id": "miniapp", "platform": "web",
        "features": [{"id": "f1", "name": "F1", "steps": [], "expected": ""}],
        "exclusions": [{"feature": "实时协作", "treatment": "排除",
                        "reason": "需要第二客户端"}],
    }, ensure_ascii=False), encoding="utf-8")

    result = managed._t_finalize({})
    payload = json.loads(result["content"][0]["text"])
    assert payload["exploration_finished"] is True
    assert payload["stage"] == "reproduction"
    # The exclusion boundary reaches the reproduction agent at the moment
    # generation starts, not only during exploration.
    assert payload["exclusions"][0]["feature"] == "实时协作"
    assert captured["source_mode"] == "managed-tools"
    assert captured["source_id"] == sid
    assert captured["topology_path"] == topology
    assert captured["exploration_files"] == {
        "screenshots/frame_000007.png": frame,
        "functional_topology.md": topology_md,
    }


def test_finish_reproduction_is_gated_by_accepted_review(monkeypatch):
    from agents.cli_explorer import mcp_server as managed

    class FakeReproduction:
        def finish(self, *, review_summary=None):
            return {"finished": True, "review_summary": review_summary}

    class FakeReview:
        def ensure_accepted(self):
            pass

        def summary(self):
            return {"accepted": True, "rounds": [{"round": 1}]}

        def close(self):
            pass

    monkeypatch.setattr(managed, "_reproduction", FakeReproduction())
    monkeypatch.setattr(managed, "_review", None)
    with pytest.raises(ValueError, match="start_reproduction_review"):
        managed._t_finish_reproduction({})

    monkeypatch.setattr(managed, "_review", FakeReview())
    result = managed._t_finish_reproduction({})
    payload = json.loads(result["content"][0]["text"])
    assert payload["finished"] is True
    assert payload["review_summary"]["accepted"] is True
    assert managed._reproduction is None and managed._review is None


class TestMCPServer:
    def test_handshake_and_tools(self, mcp_proc):
        proc, _ = mcp_proc
        r = rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
        assert r["result"]["serverInfo"]["name"] == "blackboxbench"
        tools = rpc(proc, "tools/list", mid=2)["result"]["tools"]
        names = {t["name"] for t in tools}
        for expected in ("observe", "click", "type_text", "key_press", "scroll",
                         "record_state", "record_feature", "record_edge",
                         "record_hypothesis", "revise", "finalize",
                         "start_reproduction_review", "review_observe",
                         "review_click", "complete_reproduction_review",
                         "finish_reproduction"):
            assert expected in names
        for t in tools:
            assert "inputSchema" in t

    def test_observe_returns_image(self, mcp_proc):
        proc, _ = mcp_proc
        r = rpc(proc, "tools/call", {"name": "observe", "arguments": {}}, 10)
        result = r["result"]
        assert not result.get("isError")
        types = [c["type"] for c in result["content"]]
        assert types == ["image", "text"]
        img = result["content"][0]
        assert img["mimeType"] == "image/png" and len(img["data"]) > 1000

    def test_click_and_discovery_and_finalize(self, mcp_proc):
        proc, sid = mcp_proc
        r = rpc(proc, "tools/call",
                {"name": "click", "arguments": {"x": 200, "y": 130}}, 20)
        d = json.loads(r["result"]["content"][0]["text"])
        assert d["accepted"] and d["step"] >= 1

        r = rpc(proc, "tools/call", {"name": "record_state", "arguments": {
            "name": "Home", "visual_evidence": [0], "confidence": 0.9}}, 21)
        assert "state_home" in r["result"]["content"][0]["text"]

        # invalid action surfaces as isError with a readable message
        r = rpc(proc, "tools/call",
                {"name": "click", "arguments": {"x": -1, "y": 0}}, 22)
        assert r["result"]["isError"]

        r = rpc(proc, "tools/call", {"name": "finalize", "arguments": {}}, 23)
        assert not r["result"]["isError"]


class TestSelfBootstrap:
    """No BBB_SESSION env: the server must find/start the controller, create
    a session lazily on first tool call, and finalize it when stdin closes."""

    def test_auto_session_and_finalize_on_eof(self, live_server):
        env = {k: v for k, v in os.environ.items()
               if k not in ("BBB_SESSION", "BBB_CONTROLLER")}
        env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
        env["BBB_APP_ID"] = "miniapp"
        proc = subprocess.Popen(
            [str(PY), "-m", "agents.cli_explorer.mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, env=env, text=True, encoding="utf-8", errors="replace", cwd=str(ROOT))
        try:
            rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
            r = rpc(proc, "tools/call", {"name": "observe", "arguments": {}}, 2)
            result = r["result"]
            assert not result.get("isError")
            meta = json.loads(result["content"][1]["text"])
            assert meta["session_id"].startswith("sess_")
            sid = meta["session_id"]
        finally:
            proc.stdin.close()
            proc.wait(timeout=30)
        # after stdin EOF the self-created session must be finalized
        st = httpx.get(f"http://127.0.0.1:{live_server}/api/sessions/{sid}",
                       trust_env=False, timeout=10).json()
        assert st["status"] == "closed"
        assert st["close_reason"] == "agent_finalized"


class TestTargetSelection:
    """list_targets / start_session: explicit target choice per conversation."""

    def test_list_targets(self, mcp_proc):
        proc, _ = mcp_proc
        r = rpc(proc, "tools/call", {"name": "list_targets", "arguments": {}}, 30)
        assert not r["result"].get("isError")
        d = json.loads(r["result"]["content"][0]["text"])
        assert any(a["app_id"] == "miniapp" for a in d["registered"])
        assert "default_target" in d and "adhoc" in d

    def test_start_session_after_bound_session_closed(self, mcp_proc):
        proc, sid = mcp_proc
        # the env-bound session was finalized by TestMCPServer; a dead binding
        # must be released and rebound, not brick the conversation
        r = rpc(proc, "tools/call",
                {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 31)
        assert not r["result"].get("isError"), r
        d = json.loads(r["result"]["content"][0]["text"])
        assert d["session_id"] != sid

    def test_start_session_self_bootstrap(self, live_server):
        env = {k: v for k, v in os.environ.items()
               if k not in ("BBB_SESSION", "BBB_CONTROLLER")}
        env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
        env["BBB_APP_ID"] = "miniapp"
        proc = subprocess.Popen(
            [str(PY), "-m", "agents.cli_explorer.mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, env=env, text=True, encoding="utf-8", errors="replace", cwd=str(ROOT))
        sid = None
        try:
            rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 2)
            assert not r["result"].get("isError"), r
            d = json.loads(r["result"]["content"][0]["text"])
            sid = d["session_id"]
            assert sid.startswith("sess_") and d["app_id"] == "miniapp"
            # same target again → idempotent ok
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 3)
            assert not r["result"]["isError"]
            # different target while bound → refused
            r = rpc(proc, "tools/call",
                    {"name": "start_session",
                     "arguments": {"url": "https://example.com"}}, 4)
            assert r["result"]["isError"]
            # agent channel works on the chosen session
            r = rpc(proc, "tools/call", {"name": "observe", "arguments": {}}, 5)
            assert not r["result"]["isError"]
        finally:
            proc.stdin.close()
            proc.wait(timeout=30)
        if sid:
            st = httpx.get(f"http://127.0.0.1:{live_server}/api/sessions/{sid}",
                           trust_env=False, timeout=10).json()
            assert st["status"] == "closed"  # finalized on stdin EOF


    def test_start_session_rebinds_after_session_death(self, live_server):
        """If the bound session died/closed externally, start_session must
        release the dead binding and create a fresh one (not brick the
        conversation demanding a finalize that can never succeed)."""
        env = {k: v for k, v in os.environ.items()
               if k not in ("BBB_SESSION", "BBB_CONTROLLER")}
        env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
        env["BBB_APP_ID"] = "miniapp"
        proc = subprocess.Popen(
            [str(PY), "-m", "agents.cli_explorer.mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, env=env, text=True, encoding="utf-8", errors="replace", cwd=str(ROOT))
        sid1 = sid2 = None
        try:
            rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 2)
            sid1 = json.loads(r["result"]["content"][0]["text"])["session_id"]

            # session dies controller-side (crash, cleanup, another operator…)
            httpx.post(f"http://127.0.0.1:{live_server}/api/sessions/{sid1}/close",
                   trust_env=False, timeout=10)

            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 3)
            assert not r["result"].get("isError"), r
            sid2 = json.loads(r["result"]["content"][0]["text"])["session_id"]
            assert sid2 != sid1
            # the rebound session actually works
            r = rpc(proc, "tools/call", {"name": "observe", "arguments": {}}, 4)
            assert not r["result"]["isError"]
        finally:
            proc.stdin.close()
            proc.wait(timeout=30)


    def test_dead_session_error_releases_and_guides(self, live_server):
        """410/404 from the agent channel must release the dead binding and
        tell the agent the one-step recovery (start_session again)."""
        env = {k: v for k, v in os.environ.items()
               if k not in ("BBB_SESSION", "BBB_CONTROLLER")}
        env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
        env["BBB_APP_ID"] = "miniapp"
        proc = subprocess.Popen(
            [str(PY), "-m", "agents.cli_explorer.mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, env=env, text=True, encoding="utf-8", errors="replace", cwd=str(ROOT))
        try:
            rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 2)
            sid = json.loads(r["result"]["content"][0]["text"])["session_id"]
            # session dies controller-side while the conversation stays open
            httpx.post(f"http://127.0.0.1:{live_server}/api/sessions/{sid}/close",
                       trust_env=False, timeout=10)
            r = rpc(proc, "tools/call", {"name": "observe", "arguments": {}}, 3)
            assert r["result"]["isError"]
            assert "start_session" in r["result"]["content"][0]["text"]
            # binding was released → immediate rebind works
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 4)
            assert not r["result"].get("isError"), r
        finally:
            proc.stdin.close()
            proc.wait(timeout=30)


    def test_chinese_payloads(self, live_server):
        """Non-ASCII (Chinese) tool payloads must survive the stdio channel:
        JSON-RPC is UTF-8, the server must not decode with the OS locale."""
        env = {k: v for k, v in os.environ.items()
               if k not in ("BBB_SESSION", "BBB_CONTROLLER")}
        env["BBB_CONTROLLER"] = f"http://127.0.0.1:{live_server}"
        env["BBB_APP_ID"] = "miniapp"
        proc = subprocess.Popen(
            [str(PY), "-m", "agents.cli_explorer.mcp_server"],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, env=env, text=True, encoding="utf-8",
            cwd=str(ROOT))
        try:
            rpc(proc, "initialize", {"protocolVersion": "2025-06-18",
                                     "capabilities": {},
                                     "clientInfo": {"name": "t", "version": "0"}}, 1)
            r = rpc(proc, "tools/call",
                    {"name": "start_session", "arguments": {"app_id": "miniapp"}}, 2)
            assert not r["result"].get("isError"), r
            r = rpc(proc, "tools/call", {"name": "record_state", "arguments": {
                "name": "首页-已登录", "description": "顶部有头像与消息入口",
                "visual_evidence": [0], "confidence": 0.9}}, 3)
            assert not r["result"].get("isError"), r
            assert "state_" in r["result"]["content"][0]["text"]
            # 中文输入走同一通道
            r = rpc(proc, "tools/call",
                    {"name": "type_text", "arguments": {"text": "你好"}}, 4)
            assert not r["result"].get("isError"), r
        finally:
            proc.stdin.close()
            proc.wait(timeout=30)
