"""Compare two google_clock evaluation reports side by side.

Maps each report to its handoff product via the accepted review summary,
then prints per-requirement grades next to each other.

Usage: vendor/python/python.exe scripts/compare_evaluations.py
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EVALS = ROOT / "app_output" / "evaluations"

# Map apk hash -> condition via the accepted review summaries.
products = {}
for handoff in ("sess_20260901_093928_ad32e3-6919fa",
                "sess_20260901_094934_3b7a3f-cd968f"):
    summary = json.loads(
        (ROOT / "app_output" / handoff / "review" /
         "review_summary.json").read_text(encoding="utf-8"))
    products[summary["accepted_apk_hash"]] = handoff.split("-")[0][-6:]

reports = {}
for run in sorted(EVALS.iterdir()):
    path = run / "evaluation_report.json"
    if not path.is_file():
        continue
    report = json.loads(path.read_text(encoding="utf-8"))
    key = products.get(report.get("apk_sha256", ""), run.name)
    reports[key] = report

missing = [name for name in ("ad32e3", "3b7a3f") if name not in reports]
if missing:
    raise SystemExit(f"MISSING reports for: {', '.join(missing)}")

a, b = reports["ad32e3"], reports["3b7a3f"]
print(f"baseline=ad32e3  run={a['run_id']}  obs={len(a['observations'])}"
      f"  actions={len(a['actions'])}")
print(f"report-b=3b7a3f  run={b['run_id']}  obs={len(b['observations'])}"
      f"  actions={len(b['actions'])}")
print()
print("counts:")
for grade in ("full", "partial", "placeholder", "broken"):
    print(f"  {grade:12} baseline={a['counts'].get(grade, 0):2}"
          f"  report-b ={b['counts'].get(grade, 0):2}")
print()
print(f"{'requirement':28} {'base':10} {'ours':10}")
diffs = 0
for ra, rb in zip(a["requirements"], b["requirements"]):
    mark = "" if ra["grade"] == rb["grade"] else "   <<<"
    if mark:
        diffs += 1
    print(f"{ra['requirement_id']:28} {ra['grade']:10} {rb['grade']:10}{mark}")
print()
print(f"same-grade requirements: {25 - diffs}/25")
for name, r in (("baseline", a), ("report-b", b)):
    pe = sum(1 for q in r["requirements"] if q["persistence_evidence"])
    restarts = sum(1 for act in r["actions"]
                   if act["type"] in ("restart_app", "reset_app"))
    print(f"{name}: persistence_evidence={pe}  restart_probes={restarts}")
