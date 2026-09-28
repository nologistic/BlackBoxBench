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


# 句柄名 → 文件名（_write_locked 重开用）
_LOG_FILES = {
    "_actions": "actions.jsonl",
    "_obs": "observations.jsonl",
    "_disc": "discovery.jsonl",
}


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

    def _write_locked(self, handle_name: str, text: str) -> None:
        """写一行日志（调用方已持锁）。句柄若已被关掉就重开再写。

        会话 close 之后仍有记录写进来时，原来会抛
        ``ValueError: I/O operation on closed file`` → 控制器 500/503，
        把整条 agent 链路带崩（2026-09-25 实测：一次误关源会话即触发）。
        重开追加是无痛续写：记录不丢，链路不崩。
        """
        handle = getattr(self, handle_name)
        try:
            handle.write(text)
            handle.flush()
            return
        except (ValueError, OSError):
            pass
        try:
            handle.close()
        except Exception:
            pass
        handle = open(self.dir / _LOG_FILES[handle_name], "a",
                      encoding="utf-8")
        setattr(self, handle_name, handle)
        handle.write(text)
        handle.flush()

    def log_action(self, rec: ActionRecord) -> None:
        with self._lock:
            self._write_locked("_actions", rec.model_dump_json() + "\n")

    def log_observation(self, rec: ObservationRecord) -> None:
        with self._lock:
            self._write_locked("_obs", rec.model_dump_json() + "\n")

    def log_discovery(self, op: str, payload: dict) -> None:
        with self._lock:
            self._write_locked("_disc", json.dumps(
                {"ts": utc_now(), "op": op, "payload": payload},
                ensure_ascii=False) + "\n")

    def log_discovery_rejected(self, op: str, reason: str,
                               request: dict | None = None) -> None:
        """Record a discovery call the platform refused.

        Only successful calls used to be written, which made two very different
        situations indistinguishable afterwards: an Agent that never attempted
        to record a transition edge, and an Agent that attempted it and was
        refused (bad evidence, unknown node id). The first is a finding about
        the Agent; the second is a platform artefact that must not be scored as
        Agent behaviour. This log is local audit only — the Agent still receives
        exactly the same error it did before.
        """
        with self._lock:
            self._write_locked("_disc", json.dumps(
                {"ts": utc_now(), "op": op, "accepted": False,
                 "reason": reason, "request": request or {}},
                ensure_ascii=False) + "\n")

    def log_event(self, kind: str, payload: dict | None = None) -> None:
        """Lifecycle events (reset, close...) into actions.jsonl."""
        with self._lock:
            self._write_locked("_actions", json.dumps(
                {"event": kind, "ts": utc_now(), **(payload or {})},
                ensure_ascii=False) + "\n")

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
