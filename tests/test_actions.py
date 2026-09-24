"""Action channel + recorder tests on the deterministic miniapp."""
from __future__ import annotations

import io
import json

from PIL import Image


def _frame(client, sid, fid) -> Image.Image:
    r = client.get(f"/api/sessions/{sid}/frames/{fid}.png")
    assert r.status_code == 200
    return Image.open(io.BytesIO(r.content)).convert("RGB")


def _crop(img, box):
    return img.crop(box).tobytes()


class TestActions:
    def test_click_changes_page(self, client, mini_session):
        r0 = client.post(f"/agent/{mini_session}/observe", json={}).json()
        before = _frame(client, mini_session, r0["frame_id"])
        r = client.post(f"/agent/{mini_session}/action",
                        json={"type": "click", "x": 200, "y": 130})
        assert r.status_code == 200 and r.json()["accepted"]
        # 证据三元组的另一半：动作返回直接给出 before_frame（= 动作前那次
        # observe 的 frame_id），agent 无需凭记忆重建（2026-09-16 事件根因）。
        assert r.json()["before_frame"] == r0["frame_id"]
        after = _frame(client, mini_session, r.json()["frame_id"])
        # counter text region (away from the cursor overlay)
        assert _crop(before, (100, 195, 500, 240)) != _crop(after, (100, 195, 500, 240))

    def test_type_and_enter_echo(self, client, mini_session):
        client.post(f"/agent/{mini_session}/action",
                    json={"type": "click", "x": 220, "y": 318})
        client.post(f"/agent/{mini_session}/action",
                    json={"type": "type_text", "text": "probe"})
        obs = client.post(f"/agent/{mini_session}/observe", json={}).json()
        before = _frame(client, mini_session, obs["frame_id"])
        r = client.post(f"/agent/{mini_session}/action",
                        json={"type": "key_press", "key": "Enter"})
        after = _frame(client, mini_session, r.json()["frame_id"])
        assert _crop(before, (100, 375, 700, 410)) != _crop(after, (100, 375, 700, 410))

    def test_scroll_and_wait_accepted(self, client, mini_session):
        for payload in ({"type": "scroll", "dx": 0, "dy": 300},
                        {"type": "wait", "ms": 50},
                        {"type": "drag", "x1": 600, "y1": 600,
                         "x2": 700, "y2": 620, "duration_ms": 100}):
            r = client.post(f"/agent/{mini_session}/action", json=payload)
            assert r.status_code == 200 and r.json()["accepted"], payload

    def test_invalid_coordinates_rejected(self, client, mini_session):
        r = client.post(f"/agent/{mini_session}/action",
                        json={"type": "click", "x": -5, "y": 100})
        assert r.status_code == 400
        r = client.post(f"/agent/{mini_session}/action",
                        json={"type": "click", "x": 5000, "y": 100})
        assert r.status_code == 400

    def test_budget_enforced(self, client):
        r = client.post("/api/sessions", json={
            "app_id": "miniapp",
            "budget": {"max_actions": 2, "max_duration_s": 300,
                       "max_observations": 100}})
        sid = r.json()["session_id"]
        try:
            ok = {"type": "move_pointer", "x": 100, "y": 100}
            assert client.post(f"/agent/{sid}/action", json=ok).status_code == 200
            assert client.post(f"/agent/{sid}/action", json=ok).status_code == 200
            r3 = client.post(f"/agent/{sid}/action", json=ok)
            assert r3.status_code == 403
            assert r3.json()["detail"] == "budget_exhausted"
        finally:
            client.post(f"/api/sessions/{sid}/close")


class TestRecorderArtifacts:
    def test_trace_files_exist(self, client, mini_session, manager):
        sess = manager.get(mini_session)
        d = sess.dir
        assert (d / "session.json").exists()
        assert (d / "actions.jsonl").exists()
        assert (d / "observations.jsonl").exists()
        assert any((d / "frames").glob("frame_*.png"))
        meta = json.loads((d / "session.json").read_text())
        assert meta["viewport"]["width"] == 1440
        assert meta["viewport"]["device_scale_factor"] == 1.0
        # every accepted action has before/after frames
        for line in (d / "actions.jsonl").read_text().splitlines():
            rec = json.loads(line)
            if rec.get("accepted"):
                assert rec["before_frame"] is not None
                assert rec["after_frame"] is not None
