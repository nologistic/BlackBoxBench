"""Exploration metrics — benchmark-internal analytics.

Loop/revisit telemetry uses perceptual hashing of frames; it is never exposed
to the agent (docs/security_model.md §2).
"""
from __future__ import annotations

import json
from pathlib import Path

from .recorder import diff as imgdiff
from .recorder.trace import TraceRecorder


def compute_metrics(session_dir: Path, recorder: TraceRecorder,
                    actions_used: int, observations_used: int,
                    elapsed_s: int, store) -> dict:
    actions = [r for r in recorder.read_actions() if r.get("accepted")]
    by_type: dict[str, int] = {}
    for r in actions:
        t = r["action"]["type"]
        by_type[t] = by_type.get(t, 0) + 1

    # repeated consecutive action runs (same type+params)
    runs = 0
    prev_key = None
    for r in actions:
        a = r["action"]
        key = json.dumps(a, sort_keys=True)
        if key == prev_key:
            runs += 1
        prev_key = key

    # unique screens via phash
    obs = recorder.read_observations()
    hashes: list[int] = []
    for o in obs:
        p = recorder.frame_path(o["frame_id"])
        if p.exists():
            hashes.append(imgdiff.phash(imgdiff.load(p.read_bytes())))
    unique = 0
    clusters: list[int] = []
    for h in hashes:
        if all(imgdiff.hamming(h, c) > 6 for c in clusters):
            clusters.append(h)
            unique += 1
    revisits = len(hashes) - unique

    g = store.graph()
    hyps = store.hypotheses()
    metrics = {
        "actions_used": actions_used,
        "observations_used": observations_used,
        "elapsed_s": elapsed_s,
        "actions_by_type": by_type,
        "states": g.coverage_summary.states,
        "features": g.coverage_summary.features,
        "data_entities": g.coverage_summary.data_entities,
        "edges": g.coverage_summary.edges,
        "hypotheses_total": len(hyps),
        "hypotheses_confirmed": sum(1 for h in hyps if h.status == "confirmed"),
        "hypotheses_rejected": sum(1 for h in hyps if h.status == "rejected"),
        "hypotheses_uncertain": sum(1 for h in hyps if h.status == "uncertain"),
        "hypotheses_unverified": sum(1 for h in hyps if h.status == "unverified"),
        "unique_screens": unique,
        "repeated_action_runs": runs,
        "state_revisits": revisits,
        "features_per_100_actions": round(
            100 * g.coverage_summary.features / actions_used, 2) if actions_used else 0.0,
    }
    (session_dir / "metrics.json").write_text(
        json.dumps(metrics, indent=2, ensure_ascii=False), encoding="utf-8")
    return metrics
