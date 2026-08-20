"""Deterministic replay test."""
from __future__ import annotations


class TestDeterministicReplay:
    def test_replay_matches(self, client, mini_session):
        # a short deterministic trajectory
        for a in ({"type": "click", "x": 200, "y": 130},
                  {"type": "click", "x": 200, "y": 130},
                  {"type": "click", "x": 220, "y": 318},
                  {"type": "type_text", "text": "xy"},
                  {"type": "key_press", "key": "Enter"}):
            r = client.post(f"/agent/{mini_session}/action", json=a)
            assert r.status_code == 200
        r = client.post(f"/api/sessions/{mini_session}/replay"
                        "?mode=deterministic")
        assert r.status_code == 200
        report = r.json()
        assert report["steps_replayed"] == 5
        assert report["match_ratio"] >= 0.8, report
        assert report["final_match"], "final frame must match after replay"
