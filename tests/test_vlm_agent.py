"""Offline harness test: run the VLM explorer loop against the miniapp with a
scripted mock model — no API key, no network. Validates prompt assembly,
JSON parsing, evidence resolution ("last"/"current"), discovery dispatch and
finalize wiring."""
from __future__ import annotations

import json

import pytest


class MockVLM:
    """Returns a fixed script of JSON decisions, one per call."""

    SCRIPT = [
        # turn 1: look at home, record a state anchored to the current frame
        json.dumps({
            "thought": "首页有一个计数按钮和一个 echo 表单", "thought_kind": "observe",
            "action": None,
            "discoveries": [{"kind": "state", "name": "Home",
                             "description": "初始页", "visual_evidence": "current",
                             "confidence": 0.9}],
            "done": False}),
        # turn 2: click the counter, record a feature anchored to that action
        json.dumps({
            "thought": "点击 Increment,计数应变", "thought_kind": "plan",
            "action": {"type": "click", "x": 200, "y": 130},
            "discoveries": [{"kind": "feature", "name": "Increment counter",
                             "behavior": {"postconditions": ["计数加一"]},
                             "evidence": "last", "confidence": 0.9}],
            "done": False}),
        # turn 3: garbage then recovery is tested separately; here a hypothesis + done
        json.dumps({
            "thought": "记录一个假设并收尾", "thought_kind": "decision",
            "action": None,
            "discoveries": [{"kind": "hypothesis",
                             "statement": "计数可能在刷新后保留",
                             "next_probe": "按 F5", "confidence": 0.4}],
            "done": True}),
        # turn 4: the done self-check — re-confirm with no new discoveries
        json.dumps({"thought": "自检:无遗留探针", "action": None,
                    "discoveries": [], "done": True}),
    ]

    def __init__(self):
        self.calls = []
        self._i = 0

    def decide(self, messages) -> str:
        self.calls.append(messages)
        out = self.SCRIPT[min(self._i, len(self.SCRIPT) - 1)]
        self._i += 1
        return out


class TestExplorerHarness:
    @pytest.fixture()
    def vlm_session(self, client):
        """Fresh session per test — a finalized session rejects further calls."""
        r = client.post("/api/sessions", json={"app_id": "miniapp", "budget": {
            "max_actions": 60, "max_duration_s": 600, "max_observations": 200}})
        assert r.status_code == 200, r.text
        sid = r.json()["session_id"]
        yield sid
        client.post(f"/api/sessions/{sid}/close")

    def test_full_loop_offline(self, client, vlm_session):
        from blackbox_bench_sdk import Environment
        from benchmark.server import app
        from agents.kimi_explorer.agent import Explorer

        from fastapi.testclient import TestClient
        env = Environment("http://testserver", vlm_session,
                          client=TestClient(app))
        vlm = MockVLM()
        explorer = Explorer(env, vlm, verbose=False)
        result = explorer.run()

        assert "summary" in result
        # VLM saw system prompt + one user turn per call; 3 scripted turns
        # plus one done self-check confirmation turn
        assert len(vlm.calls) == 4
        first = vlm.calls[0]
        assert first[0]["role"] == "system"
        kinds = [p["type"] for p in first[1]["content"]]
        assert "image_url" in kinds and "text" in kinds

        # the click was actually executed
        assert explorer.stats.actions == 1

        # topology has the state + feature with resolved evidence
        from benchmark.server import manager
        sess = manager.get(vlm_session)
        g = sess.store.graph()
        names = {n.name for n in g.nodes}
        assert "Home" in names and "Increment counter" in names
        feat = next(n for n in g.nodes if n.name == "Increment counter")
        assert feat.evidence and feat.evidence[0].step >= 1
        assert feat.evidence[0].after_frame >= 0
        st = next(n for n in g.nodes if n.name == "Home")
        assert st.visual_evidence, "current frame must be resolved to an id"

        # hypotheses recorded
        assert len(sess.store.hypotheses()) == 1

    def test_garbage_output_is_tolerated(self, client, vlm_session):
        from blackbox_bench_sdk import Environment
        from benchmark.server import app
        from agents.kimi_explorer.agent import Explorer

        class GarbageVLM:
            def __init__(self):
                self.n = 0

            def decide(self, messages):
                self.n += 1
                if self.n == 1:
                    return "我不会输出 JSON 哈哈哈"
                return json.dumps({"thought": "收尾", "action": None,
                                   "discoveries": [], "done": True})

        from fastapi.testclient import TestClient
        env = Environment("http://testserver", vlm_session,
                          client=TestClient(app))
        explorer = Explorer(env, GarbageVLM(), verbose=False)
        result = explorer.run()
        assert "summary" in result
        assert explorer.stats.parse_retries == 1
