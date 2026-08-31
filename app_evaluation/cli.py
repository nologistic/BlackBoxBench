"""Run a pixels-only Android checklist from an explicit coordinate plan."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from .evaluator import AppEvaluator


def execute_plan(evaluator: AppEvaluator, plan: dict) -> dict:
    steps = plan.get("steps") if isinstance(plan, dict) else None
    if not isinstance(steps, list) or not steps:
        raise ValueError("evaluation plan needs a non-empty steps array")
    try:
        for step in steps:
            if not isinstance(step, dict):
                raise ValueError("evaluation plan steps must be objects")
            kind = step.get("type")
            if kind == "observe":
                evaluator.observe()
            elif kind == "action":
                evaluator.action(str(step.get("action") or ""),
                                 dict(step.get("args") or {}))
            elif kind == "record_result":
                evaluator.record_result(
                    feature_id=str(step.get("feature_id") or ""),
                    grade=str(step.get("grade") or ""),
                    rationale=str(step.get("rationale") or ""),
                    evidence_observations=[int(value) for value in
                                           step.get("evidence_observations", [])])
            else:
                raise ValueError(f"unsupported evaluation plan step: {kind}")
        return evaluator.finish()
    except Exception:
        evaluator.close()
        raise


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--activity", required=True)
    parser.add_argument("--checklist", type=Path, required=True)
    parser.add_argument("--plan", type=Path,
                        help="JSON coordinate/evidence plan; omit to print requirements")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    evaluator = AppEvaluator(
        apk=args.apk, package_name=args.package,
        launch_activity=args.activity, checklist_path=args.checklist,
        output_root=args.output)
    if args.plan is None:
        print(json.dumps({"requirements": evaluator.requirements(),
                          "plan_schema": {
                              "steps": [
                                  {"type": "observe"},
                                  {"type": "action", "action": "tap",
                                   "args": {"x": 1, "y": 1}},
                                  {"type": "record_result",
                                   "feature_id": "...",
                                   "grade": "full|partial|placeholder|broken",
                                   "rationale": "...",
                                   "evidence_observations": [1]},
                              ]}}, ensure_ascii=False, indent=2))
        return
    plan = json.loads(args.plan.read_text(encoding="utf-8"))
    print(json.dumps(execute_plan(evaluator, plan), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
