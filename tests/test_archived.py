"""Archived-session read-only access.

After a controller restart a session exists only as artifacts on disk. The
operator GET routes must keep serving it (dashboard replay/audit), while
mutating routes and the agent channel stay live-only.
"""
from __future__ import annotations

import shutil


def test_archived_session_read_only(client, manager):
    r = client.post("/api/sessions", json={
        "app_id": "miniapp",
        "budget": {"max_actions": 20, "max_duration_s": 300,
                   "max_observations": 50}})
    assert r.status_code == 200, r.text
    sid = r.json()["session_id"]
    try:
        client.post(f"/agent/{sid}/action",
                    json={"type": "click", "x": 200, "y": 130})
        client.post(f"/agent/{sid}/finalize", json={})

        # simulate a controller restart: drop all in-memory state
        manager._sessions.pop(sid, None)
        manager._runtimes.pop(sid, None)

        st = client.get(f"/api/sessions/{sid}")
        assert st.status_code == 200, st.text
        body = st.json()
        assert body["status"] == "archived"
        assert body["app_id"] == "miniapp"
        assert body["current_frame"] is not None
        assert isinstance(body["elapsed_s"], int)  # frozen at finalize

        tr = client.get(f"/api/sessions/{sid}/trace")
        assert tr.status_code == 200
        assert any(a.get("accepted") for a in tr.json()["actions"])

        fr = client.get(f"/api/sessions/{sid}/frames/0.png")
        assert fr.status_code == 200
        assert fr.headers["content-type"] == "image/png"

        lv = client.get(f"/api/sessions/{sid}/live.png")
        assert lv.status_code == 200  # static last frame

        tp = client.get(f"/api/sessions/{sid}/topology")
        assert tp.status_code == 200 and "nodes" in tp.json()

        hy = client.get(f"/api/sessions/{sid}/hypotheses")
        assert hy.status_code == 200 and isinstance(hy.json(), list)

        mt = client.get(f"/api/sessions/{sid}/metrics")
        assert mt.status_code in (200, 404)  # metrics.json is best-effort

        # mutations + agent channel stay live-only
        assert client.post(f"/api/sessions/{sid}/reset").status_code == 404
        assert client.post(f"/api/sessions/{sid}/close").status_code == 404
        assert client.post(f"/agent/{sid}/observe", json={}).status_code == 404

        # malformed / nonexistent ids never touch the filesystem
        assert client.get("/api/sessions/not_a_session").status_code == 404
        assert client.get(
            "/api/sessions/sess_00000000_000000_abcdef/trace").status_code == 404
    finally:
        shutil.rmtree(manager.runs_dir / sid, ignore_errors=True)
