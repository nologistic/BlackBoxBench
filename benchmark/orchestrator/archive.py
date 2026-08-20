"""Read-only access to archived session artifacts (runs/<sid>/ on disk).

Live sessions are served from memory by SessionManager; after a controller
restart they exist only as artifact dirs. These readers let the operator API
(status / trace / frames / topology / live.png) keep serving them so the
dashboard can replay and audit past sessions.

Security: session ids are strictly validated before any filesystem access
(path-traversal guard), and nothing in this module writes.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

_SID_RE = re.compile(r"^sess_[0-9]{8}_[0-9]{6}_[0-9a-f]{6}$")


def archived_dir(runs_dir: Path, sid: str) -> Path | None:
    """The artifact dir for `sid`, or None (invalid id or no artifacts)."""
    if not _SID_RE.match(sid):
        return None
    d = Path(runs_dir) / sid
    return d if (d / "session.json").exists() else None


def _read_jsonl(p: Path) -> list[dict]:
    if not p.exists():
        return []
    return [json.loads(l) for l in p.read_text(encoding="utf-8").splitlines()
            if l.strip()]


def read_trace(d: Path) -> dict:
    return {"actions": _read_jsonl(d / "actions.jsonl"),
            "observations": _read_jsonl(d / "observations.jsonl")}


def frame_path(d: Path, frame_id: int) -> Path | None:
    p = d / "frames" / f"frame_{frame_id:06d}.png"
    return p if p.exists() else None


def last_frame_path(d: Path) -> Path | None:
    frames = sorted((d / "frames").glob("frame_*.png"))
    return frames[-1] if frames else None


def read_topology(d: Path) -> dict | None:
    for name in ("functional_topology.json", "topology.json"):
        p = d / name
        if p.exists():
            return json.loads(p.read_text(encoding="utf-8"))
    return None


def read_hypotheses(d: Path) -> list:
    p = d / "hypotheses.json"
    if not p.exists():
        return []
    data = json.loads(p.read_text(encoding="utf-8"))
    return data if isinstance(data, list) else data.get("hypotheses", [])


def read_metrics(d: Path) -> dict | None:
    p = d / "metrics.json"
    return json.loads(p.read_text(encoding="utf-8")) if p.exists() else None


def read_status(d: Path) -> dict:
    """Synthesize the live status_dict() shape from on-disk artifacts."""
    meta = json.loads((d / "session.json").read_text(encoding="utf-8"))
    summary = {}
    sp = d / "session_summary.json"
    if sp.exists():
        summary = json.loads(sp.read_text(encoding="utf-8"))
    coverage = summary.get("coverage", {})
    actions = [a for a in _read_jsonl(d / "actions.jsonl") if a.get("accepted")]
    last_frame = last_frame_path(d)
    last_fid = int(last_frame.stem.rsplit("_", 1)[1]) if last_frame else None
    graph = read_topology(d) or {}
    return {
        "session_id": meta.get("session_id", d.name),
        "app_id": meta.get("app_id"),
        "status": "archived",
        "close_reason": summary.get("close_reason") or meta.get("status"),
        "brief": "",
        "step": summary.get("steps"),
        "elapsed_s": (summary.get("metrics") or {}).get("elapsed_s"),
        "current_frame": last_fid,
        "cursor": None,
        "budget": None,
        "counts": {
            "states": coverage.get("states", 0),
            "features": coverage.get("features", 0),
            "edges": coverage.get("edges", 0),
            "hypotheses": len(read_hypotheses(d)),
            "unresolved": len(graph.get("unresolved_questions", [])),
        },
        "last_action": actions[-1]["action"] if actions else None,
    }
