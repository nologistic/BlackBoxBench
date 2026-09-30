#!/usr/bin/env python3
"""Compute artifact-derived benchmark metrics for one nograph session.

All inputs are files already produced by the pipeline (no agent involvement):
  runs/<sid>/actions.jsonl, observations.jsonl, session_summary.json, crash_report.json
  website_output/<handoff>/.blackboxbench/review_summary.json
  website_output/evaluations/<run>/evaluation_report.json
  review_specs/<app_id>.json  (checklist, for the persistence subset)

Emits one JSON row of metrics. Cost (per stage) is not in the artifacts; pass it
with --cost-explore/-reproduce/-evaluate if you want cost-per-fidelity included.

Usage:
  python3 scripts/compute_metrics.py --sid <sid> --app <app_id> \
      --handoff <handoff_id> --eval-report <path> [--cost-* N] [--json out.json]
"""
from __future__ import annotations
import argparse, json, os, re
from pathlib import Path

GRADE_W = {"full": 1.0, "partial": 0.5, "placeholder": 0.25, "broken": 0.0}
ILLUSION = re.compile(r"fake|shell|decorativ|placeholder|占位|装饰|假|no[- ]?op|non[- ]?functional", re.I)


def _jsonl(p):
    out = []
    if os.path.exists(p):
        for line in open(p, encoding="utf-8"):
            line = line.strip()
            if line:
                try: out.append(json.loads(line))
                except Exception: pass
    return out


def _json(p):
    return json.loads(Path(p).read_text(encoding="utf-8")) if os.path.exists(p) else {}
# _APPEND
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sid", required=True)
    ap.add_argument("--app", required=True)
    ap.add_argument("--handoff", default="")
    ap.add_argument("--eval-report", default="")
    ap.add_argument("--runs", default="runs")
    ap.add_argument("--diff-eps", type=float, default=0.002)
    ap.add_argument("--tokens-explore", type=int, default=0)
    ap.add_argument("--tokens-reproduce", type=int, default=0)
    ap.add_argument("--tokens-evaluate", type=int, default=0)
    ap.add_argument("--json", default="")
    a = ap.parse_args()
    rd = os.path.join(a.runs, a.sid)
    m = {"sid": a.sid, "app": a.app}

    # --- exploration quality (actions.jsonl + observations.jsonl) ---
    acts = _jsonl(os.path.join(rd, "actions.jsonl"))
    obs = _jsonl(os.path.join(rd, "observations.jsonl"))
    n_act = len(acts)
    invalid = sum(1 for x in acts if x.get("accepted") is False or x.get("error"))
    changed = sum(1 for o in obs if (o.get("diff_score") or 0) > a.diff_eps)
    m["explore_actions"] = n_act
    m["invalid_action_rate"] = round(invalid / n_act, 4) if n_act else None
    m["effective_action_rate"] = round(changed / len(obs), 4) if obs else None
    m["distinct_frames_seen"] = changed
    summ = _json(os.path.join(rd, "session_summary.json"))
    m["explore_steps"] = summ.get("steps")
    m["explore_duration_s"] = summ.get("duration_s") or summ.get("elapsed_s")

    # --- platform-attributed failures (validity guard, evaluation_contract §11) ---
    crash = _json(os.path.join(rd, "crash_report.json"))
    degrading = sum(1 for x in acts if x.get("environment_degrading"))
    m["platform_failures"] = (1 if crash else 0) + degrading
    m["crash_error"] = crash.get("error") if crash else None

    # --- reproduction self-correction (review_summary.json) ---
    if a.handoff:
        rs = _json(os.path.join("website_output", a.handoff, ".blackboxbench", "review_summary.json"))
        m["repro_accepted"] = rs.get("accepted")
        m["repro_revisions"] = rs.get("revision_count")
        m["repro_review_rounds"] = len(rs.get("rounds", [])) or None

    # --- reproduction fidelity + illusion (evaluation_report.json) ---
    rep = _json(a.eval_report) if a.eval_report else {}
    reqs = rep.get("requirements", [])
    if reqs:
        counts = {k: sum(1 for r in reqs if r.get("grade") == k) for k in GRADE_W}
        wfs = sum(GRADE_W.get(r.get("grade"), 0) for r in reqs) / len(reqs)
        m["grade_counts"] = counts
        m["wfs"] = round(wfs, 4)
        illus = sum(1 for r in reqs if r.get("grade") in ("placeholder", "broken")
                    or ILLUSION.search(r.get("rationale", "") or ""))
        m["illusion_rate"] = round(illus / len(reqs), 4)
        # persistence subset (long-horizon): join checklist persistence flags
        spec = _json(os.path.join("review_specs", f"{a.app}.json"))
        pflag = {f.get("id"): f.get("persistence") for f in spec.get("features", [])}
        psub = [r for r in reqs if pflag.get(r.get("requirement_id")) is True]
        if psub:
            m["persistence_wfs"] = round(sum(GRADE_W.get(r.get("grade"), 0) for r in psub) / len(psub), 4)
            m["persistence_n"] = len(psub)

    # --- token consumption / efficiency ---
    tot = a.tokens_explore + a.tokens_reproduce + a.tokens_evaluate
    if tot > 0:
        m["tokens_total"] = tot
        m["tokens_by_stage"] = {"explore": a.tokens_explore, "reproduce": a.tokens_reproduce,
                                "evaluate": a.tokens_evaluate}
        if m.get("wfs"):
            m["tokens_per_fidelity_point"] = round(tot / max(m["wfs"] * len(reqs), 1e-6))

    print(json.dumps(m, ensure_ascii=False, indent=2))
    if a.json:
        Path(a.json).write_text(json.dumps(m, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

