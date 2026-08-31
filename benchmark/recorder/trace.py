"""Trace recorder: frames + JSONL logs. The auditable memory of a session.

Artifacts (see docs/architecture.md §8):
  frames/frame_000123.png   (cursor rendered in)
  actions.jsonl             ActionRecord per line
  observations.jsonl        ObservationRecord per line
  discovery.jsonl           append-only discovery ops log
"""
from __future__ import annotations

import json
import threading
from datetime import datetime, timezone
from pathlib import Path

from ..topology.models import ActionRecord, ObservationRecord


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


class TraceRecorder:
    def __init__(self, session_dir: Path):
        self.dir = Path(session_dir)
        (self.dir / "frames").mkdir(parents=True, exist_ok=True)
        self._actions = open(self.dir / "actions.jsonl", "a", encoding="utf-8")
        self._obs = open(self.dir / "observations.jsonl", "a", encoding="utf-8")
        self._disc = open(self.dir / "discovery.jsonl", "a", encoding="utf-8")
        self._lock = threading.Lock()
        self._frame_count = self._init_frame_count()

    def _init_frame_count(self) -> int:
        existing = sorted((self.dir / "frames").glob("frame_*.png"))
        return len(existing)

    # ------------------------------------------------------------ frames

    @property
    def frame_count(self) -> int:
        return self._frame_count

    def save_frame(self, png: bytes) -> int:
        """Store a frame, return its frame_id."""
        with self._lock:
            fid = self._frame_count
            self._frame_count += 1
        (self.dir / "frames" / f"frame_{fid:06d}.png").write_bytes(png)
        return fid

    def frame_path(self, frame_id: int) -> Path:
        return self.dir / "frames" / f"frame_{frame_id:06d}.png"

    def frame_exists(self, frame_id: int) -> bool:
        return 0 <= frame_id < self._frame_count and self.frame_path(frame_id).exists()

    # ------------------------------------------------------------ logs

    def log_action(self, rec: ActionRecord) -> None:
        with self._lock:
            self._actions.write(rec.model_dump_json() + "\n")
            self._actions.flush()

    def log_observation(self, rec: ObservationRecord) -> None:
        with self._lock:
            self._obs.write(rec.model_dump_json() + "\n")
            self._obs.flush()

    def log_discovery(self, op: str, payload: dict) -> None:
        with self._lock:
            self._disc.write(json.dumps(
                {"ts": utc_now(), "op": op, "payload": payload},
                ensure_ascii=False) + "\n")
            self._disc.flush()

    def log_event(self, kind: str, payload: dict | None = None) -> None:
        """Lifecycle events (reset, close...) into actions.jsonl."""
        with self._lock:
            self._actions.write(json.dumps(
                {"event": kind, "ts": utc_now(), **(payload or {})},
                ensure_ascii=False) + "\n")
            self._actions.flush()

    # ------------------------------------------------------------ reads

    def read_actions(self) -> list[dict]:
        p = self.dir / "actions.jsonl"
        if not p.exists():
            return []
        return [json.loads(l) for l in p.read_text(encoding="utf-8").splitlines() if l.strip()]

    def read_observations(self) -> list[dict]:
        p = self.dir / "observations.jsonl"
        if not p.exists():
            return []
        return [json.loads(l) for l in p.read_text(encoding="utf-8").splitlines() if l.strip()]

    def close(self) -> None:
        for fh in (self._actions, self._obs, self._disc):
            try:
                fh.close()
            except Exception:
                pass
