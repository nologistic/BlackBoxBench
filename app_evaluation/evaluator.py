"""Deprecated: the plan-batch evaluator was replaced by an interactive session.

The old ``AppEvaluator`` + ``execute_plan`` pair required a judge to declare the
whole coordinate plan *before* seeing a single frame, which made adaptive
probing impossible — the opposite of what black-box functional review needs.

Use :mod:`app_evaluation.session` instead, driven by the ``app-review`` Skill
through ``agents/app_review/mcp_server.py``:

    from app_evaluation import AppEvaluationSession, Checklist

    session = AppEvaluationSession(apk=apk, checklist=Checklist.load(path))
    session.start()
    png, meta = session.observe()          # look first
    session.action("tap", {"x": 120, "y": 480})   # then decide
    session.record_result(requirement_id=..., grade=..., rationale=...,
                          evidence_observations=[...])
    session.finish()
"""
from __future__ import annotations

_MESSAGE = (
    "app_evaluation.evaluator was removed: use app_evaluation.session."
    "AppEvaluationSession (driven by the app-review Skill/MCP) so the judge can "
    "observe before choosing each action."
)


def __getattr__(name: str):
    raise AttributeError(f"{_MESSAGE} (requested attribute: {name!r})")
