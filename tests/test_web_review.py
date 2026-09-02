"""web-review: checklist loading, session guardrails, source channel, surface."""
from __future__ import annotations

import json
from pathlib import Path

import pytest

from app_evaluation.checklist import Checklist
from benchmark.runtime.base import RuntimeInfo
from web_evaluation.session import WebEvaluationSession


class _FakeWebRuntime:
    """Pixels that change every call, actions recorded, no browser at all."""

    def __init__(self):
        self.calls = []
        self._frame = 0

    def start(self) -> None:
        self.calls.append(("start",))

    def stop(self) -> None:
        self.calls.append(("stop",))

    def reset(self) -> None:
        self.calls.append(("reset",))

    def health(self) -> bool:
        return True

    def info(self) -> RuntimeInfo:
        return RuntimeInfo(1440, 900, 1.0, "web")

    def screenshot(self) -> bytes:
        self._frame += 1
        return b"fake-png-frame-%d" % self._frame

    def click(self, x, y, button="left", count=1):
        self.calls.append(("click", x, y))

    def type_text(self, text):
        self.calls.append(("type_text",))

    def scroll(self, dx, dy):
        self.calls.append(("scroll", dx, dy))

    def key(self, key, kind="press"):
        self.calls.append(("key", key))

    def reload(self):
        self.calls.append(("reload",))


def _make_handoff(tmp_path: Path) -> Path:
    handoff = tmp_path / "handoff"
    handoff.mkdir(parents=True)
    (handoff / "index.html").write_text(
        "<html><body><button>save</button></body></html>", encoding="utf-8")
    (handoff / "app.js").write_text("console.log('x')", encoding="utf-8")
    (handoff / "logo.png").write_bytes(b"\x89PNG fake")
    return handoff


def _features(*, persistence=False, multi_user=False) -> list[dict]:
    return [{
        "id": "save_flow", "name": "保存流程",
        "steps": ["点击保存"], "expected": "内容保留",
        "persistence": persistence, "multi_user": multi_user,
    }, {
        "id": "menu_open", "name": "菜单打开", "steps": [], "expected": "菜单出现",
    }]


def _session(tmp_path, features, runtime=None):
    runtime = runtime or _FakeWebRuntime()
    checklist = Checklist.from_object({"checklist_id": "t_web",
                                       "platform": "web",
                                       "features": features})
    return WebEvaluationSession(
        handoff=_make_handoff(tmp_path), checklist=checklist,
        output_root=tmp_path / "evals",
        runtime_factory=lambda handoff, work_dir: runtime), runtime


def _persistence_flow(session):
    """#1 (before) -> click -> #2 (after) -> reload -> #3 (persisted)."""
    session.start()
    session.observe()                      # 1
    session.action("click", {"x": 10, "y": 10})
    session.observe()                      # 2
    session.action("reload", {})
    session.observe()                      # 3


def test_yuque_web_checklist_loads_and_matches_platform():
    path = Path("review_specs/yuque_web.json")
    checklist = Checklist.load(path)
    assert checklist.platform == "web"
    assert len(checklist.features) == 29
    assert sum(1 for f in checklist.features if f["persistence"]) == 2
    assert sum(1 for f in checklist.features if f["multi_user"]) == 5
    checklist.expect_platform("web")
    with pytest.raises(ValueError):
        checklist.expect_platform("android")


def test_web_session_persistence_evidence_structure(tmp_path):
    session, runtime = _session(tmp_path, _features(persistence=True))
    _persistence_flow(session)
    result = session.record_result(
        requirement_id="save_flow", grade="full",
        rationale="保存后内容变化，reload 后仍保持",
        evidence_observations=[1, 2, 3],
        persistence_evidence={"before_observation": 1,
                              "after_observation": 2,
                              "persisted_observation": 3})
    assert result["recorded"] == "save_flow"
    assert ("reload",) in runtime.calls

    # No write-class interaction between before and after.
    session2, _ = _session(tmp_path / "b", _features(persistence=True))
    session2.start()
    session2.observe()                     # 1
    session2.action("wait", {"ms": 0})
    session2.observe()                     # 2
    session2.action("reload", {})
    session2.observe()                     # 3
    with pytest.raises(ValueError, match="write-class"):
        session2.record_result(
            requirement_id="save_flow", grade="full",
            rationale="x", evidence_observations=[1, 2, 3],
            persistence_evidence={"before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3})

    # No persistence probe between after and persisted.
    session3, _ = _session(tmp_path / "c", _features(persistence=True))
    session3.start()
    session3.observe()                     # 1
    session3.action("click", {"x": 1, "y": 1})
    session3.observe()                     # 2
    session3.action("wait", {"ms": 0})
    session3.observe()                     # 3
    with pytest.raises(ValueError, match="reload, reset_browser"):
        session3.record_result(
            requirement_id="save_flow", grade="full",
            rationale="x", evidence_observations=[1, 2, 3],
            persistence_evidence={"before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 3})

    # Citation of an observation that was never captured is refused.
    session4, _ = _session(tmp_path / "d", _features())
    session4.start()
    session4.observe()
    with pytest.raises(ValueError, match="never captured"):
        session4.record_result(
            requirement_id="menu_open", grade="broken",
            rationale="x", evidence_observations=[9])


def test_web_finish_refuses_ungraded_and_pins_report(tmp_path):
    session, _ = _session(tmp_path, _features(persistence=True))
    _persistence_flow(session)
    session.record_result(
        requirement_id="save_flow", grade="full", rationale="ok",
        evidence_observations=[1, 2, 3],
        persistence_evidence={"before_observation": 1,
                              "after_observation": 2,
                              "persisted_observation": 3})
    with pytest.raises(ValueError, match="ungraded"):
        session.finish()

    session.read_source("app.js")
    session.record_result(requirement_id="menu_open", grade="partial",
                          rationale="菜单可开但部分项无响应",
                          evidence_observations=[1])
    result = session.finish()
    report = json.loads(Path(result["report"]).read_text(encoding="utf-8"))
    assert report["schema_version"] == 1
    assert report["platform"] == "web"
    assert report["checklist_id"] == "t_web"
    assert len(report["handoff_sha256"]) == 64
    assert report["counts"]["full"] == 1
    assert report["counts"]["partial"] == 1
    assert {"type": "read_source", "path": "app.js"} in report["actions"]
    # Coordinates and typed text never reach the report.
    kinds = {a["type"] for a in report["actions"]}
    assert "click" in kinds
    assert not any("x" in a or "text" in a for a in report["actions"])
    assert [o["number"] for o in report["observations"]] == [1, 2, 3]


def test_web_read_source_scoping(tmp_path):
    session, _ = _session(tmp_path, _features())
    session.start()

    result = session.read_source("app.js")
    assert result["path"] == "app.js"
    assert "console.log" in result["content"]

    with pytest.raises(ValueError):
        session.read_source("../outside.js")          # escapes the handoff
    with pytest.raises(ValueError):
        session.read_source(str(tmp_path / "elsewhere.txt"))  # absolute escape
    with pytest.raises(ValueError):
        session.read_source("logo.png")               # binary extension
    with pytest.raises(ValueError):
        session.read_source("missing.js")             # does not exist


def test_web_handoff_needs_index_entry(tmp_path):
    checklist = Checklist.from_object({"checklist_id": "t2", "platform": "web",
                                       "features": _features()})
    empty = tmp_path / "empty"
    empty.mkdir()
    with pytest.raises(FileNotFoundError):
        WebEvaluationSession(handoff=empty, checklist=checklist,
                             output_root=tmp_path / "e")


def test_web_review_mcp_surface_has_pixel_and_source_tools_only():
    from agents.web_review import mcp_server as web_mcp
    required = {"list_checklists", "start_evaluation", "observe", "click",
                "type_text", "scroll", "key", "reload", "reset_browser",
                "wait", "read_source", "record_result",
                "evaluation_status", "finish_evaluation", "abort_evaluation"}
    forbidden = {"get_dom", "eval", "eval_js", "query_selector", "selector",
                 "network", "storage", "switch_tab", "tap", "restart_app"}
    names = set(web_mcp._TOOLS)
    assert required <= names
    assert not (forbidden & names)
    # The two evaluation conditions stay implementation-isolated.
    source = Path(web_mcp.__file__).read_text(encoding="utf-8")
    assert "agents.app_review" not in source


def test_web_review_mcp_lists_web_platform_checklists_only():
    from agents.web_review import mcp_server as web_mcp
    payload = {}
    text = web_mcp._t_list_checklists({})
    payload = json.loads(text["content"][0]["text"])
    assert all(item["platform"] in ("web", "any")
               for item in payload["checklists"])
    assert any(item["checklist_id"] == "yuque_web"
               for item in payload["checklists"])
    assert set(payload["grades"]) == {"full", "partial", "placeholder",
                                      "broken"}
