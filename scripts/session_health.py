"""Health of exploration sessions: blips, latency drift, settle behaviour.

Usage: vendor/python/python.exe scripts/session_health.py [runs/sess_* ...]
With no arguments it scans every session directory under runs/.
"""
from __future__ import annotations

import json
import sys
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


def report(d: Path) -> None:
    actions = jlines(d / "actions.jsonl")
    obs = jlines(d / "observations.jsonl")
    disc = jlines(d / "discovery.jsonl")

    events = [a for a in actions if a.get("event")]
    steps = [a for a in actions if not a.get("event")]
    blips = [e for e in events if e.get("event") == "environment_blip"]
    degrading = [e for e in events if e.get("event") == "environment_degrading"]
    rejected_disc = [r for r in disc if r.get("accepted") is False]

    durations = [a.get("duration_ms") or 0 for a in steps]
    settles = [o.get("settle_ms") or 0 for o in obs]
    reasons: dict[str, int] = {}
    for o in obs:
        reasons[o.get("settle_reason", "?")] = reasons.get(
            o.get("settle_reason", "?"), 0) + 1

    print(f"=== {d.name}")
    print(f"  steps={len(steps)} observations={len(obs)} discovery={len(disc)}")
    print(f"  environment_blip     : {len(blips)}")
    print(f"  environment_degrading: {len(degrading)}")
    print(f"  rejected discovery   : {len(rejected_disc)}")
    if durations:
        print(f"  action ms: mean={sum(durations)/len(durations):.0f} "
              f"max={max(durations):.0f}")
    if settles:
        print(f"  settle ms: mean={sum(settles)/len(settles):.0f} "
              f"reasons={reasons}")
    for item in blips[-3:]:
        print(f"    blip: {item}")
    for item in degrading[-2:]:
        print(f"    degrading: {item}")
    for item in rejected_disc[-3:]:
        print(f"    refused {item.get('op')}: {str(item.get('reason'))[:110]}")


def main() -> None:
    args = sys.argv[1:]
    targets = [Path(a) for a in args] if args else sorted(
        d for d in RUNS.glob("sess_*") if d.is_dir())
    for d in targets:
        report(d)


if __name__ == "__main__":
    main()
