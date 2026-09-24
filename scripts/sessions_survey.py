"""Survey every exploration session: outcome, topology, health, id quality.

Usage: vendor/python/python.exe scripts/sessions_survey.py
"""
from __future__ import annotations

import json
from pathlib import Path

RUNS = Path(__file__).resolve().parent.parent / "runs"


def jlines(path: Path):
    out = []
    if not path.is_file():
        return out
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if line:
            try:
                out.append(json.loads(line))
            except Exception:
                pass
    return out


def jload(path: Path):
    if not path.is_file():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8", errors="replace"))
    except Exception:
        return None


rows = []
for d in sorted(RUNS.glob("sess_*")):
    if not d.is_dir():
        continue
    meta = jload(d / "session.json") or {}
    summary = jload(d / "session_summary.json") or {}
    cov = summary.get("coverage") or {}
    topo = jload(d / "functional_topology.json") or {}
    nodes = topo.get("nodes") or []
    unnamed = sum(1 for n in nodes if "unnamed" in str(n.get("id", "")))
    actions = jlines(d / "actions.jsonl")
    obs = jlines(d / "observations.jsonl")
    steps = [a for a in actions if not a.get("event")]
    blips = sum(1 for a in actions if a.get("event") == "environment_blip")
    degrading = sum(1 for a in actions
                    if a.get("event") == "environment_degrading")
    reasons: dict[str, int] = {}
    for o in obs:
        reasons[o.get("settle_reason", "?")] = reasons.get(
            o.get("settle_reason", "?"), 0) + 1
    total_obs = sum(reasons.values())
    timeout_rate = (reasons.get("timeout", 0) / total_obs
                    if total_obs else None)
    has_handoff = (d / "reproduction_index.md").is_file()
    rows.append({
        "session": d.name.replace("sess_", ""),
        "app": meta.get("app_id", "?"),
        "status": meta.get("status") or "(none)",
        "steps": summary.get("steps") or len(steps),
        "states": cov.get("states") or len(nodes),
        "features": cov.get("features", ""),
        "edges": cov.get("edges", ""),
        "unnamed": unnamed,
        "timeout%": f"{timeout_rate:.0%}" if timeout_rate is not None else "",
        "blip": blips,
        "degr": degrading,
        "handoff": "R" if has_handoff else "",
    })

hdr = ["session", "app", "status", "steps", "states", "feat", "edges",
       "unnamed", "settleTO", "blip", "degr", "handoff"]
print("  ".join(f"{h:>10}" for h in hdr))
for r in rows:
    print("  ".join(f"{str(r.get(k, '')):>10}" for k in
                    ["session", "app", "status", "steps", "states",
                     "features", "edges", "unnamed", "timeout%", "blip",
                     "degr", "handoff"]))
