"""Reset and determinism tests."""
from __future__ import annotations

import io

from PIL import Image

from benchmark.recorder import diff as imgdiff


def _frame(client, sid, fid) -> Image.Image:
    r = client.get(f"/api/sessions/{sid}/frames/{fid}.png")
    assert r.status_code == 200
    return Image.open(io.BytesIO(r.content)).convert("RGB")


def _counter_region(img):
    return img.crop((100, 195, 500, 240))


class TestReset:
    def test_reset_restores_initial_state(self, client, mini_session):
        obs0 = client.post(f"/agent/{mini_session}/observe", json={}).json()
        initial = _frame(client, mini_session, obs0["frame_id"])
        # mutate: increment counter twice
        for _ in range(2):
            client.post(f"/agent/{mini_session}/action",
                        json={"type": "click", "x": 200, "y": 130})
        # reset must return to S0
        r = client.post(f"/api/sessions/{mini_session}/reset")
        assert r.status_code == 200
        obs1 = client.post(f"/agent/{mini_session}/observe", json={}).json()
        after = _frame(client, mini_session, obs1["frame_id"])
        assert _counter_region(initial).tobytes() == \
               _counter_region(after).tobytes(), "counter must be back to 0"


class TestDeterminism:
    SCRIPT = [
        {"type": "click", "x": 200, "y": 130},          # increment
        {"type": "click", "x": 220, "y": 318},          # focus echo input
        {"type": "type_text", "text": "abc"},
        {"type": "key_press", "key": "Enter"},          # submit echo
    ]

    def _run_script(self, client, sid):
        client.post(f"/api/sessions/{sid}/reset")
        last = None
        for a in self.SCRIPT:
            r = client.post(f"/agent/{sid}/action", json=a)
            assert r.status_code == 200
            last = r.json()["frame_id"]
        return _frame(client, sid, last)

    def test_same_trajectory_same_final_state(self, client, mini_session):
        f1 = self._run_script(client, mini_session)
        f2 = self._run_script(client, mini_session)
        # cursor position is deterministic here (same script), so full-frame
        # comparison is meaningful; still compare with tolerance for safety.
        dist = imgdiff.hamming(imgdiff.phash(f1), imgdiff.phash(f2))
        assert dist <= 6, f"final frames diverged (phash hamming={dist})"
