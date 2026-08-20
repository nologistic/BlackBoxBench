"""Session manager: creation, lookup, reset, teardown, replay dispatch."""
from __future__ import annotations

import threading
from datetime import datetime, timezone
from pathlib import Path

from .. import config
from ..runtime.base import Runtime
from ..runtime.local_chromium import LocalChromiumRuntime
from .apps import get_app, list_apps, new_gateway_secret
from .session import Budget, Session


def _new_session_id() -> str:
    import secrets
    ts = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    return f"sess_{ts}_{secrets.token_hex(3)}"


def _free_port() -> int:
    import socket
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


class SessionManager:
    def __init__(self, runs_dir: Path | None = None):
        self.runs_dir = Path(runs_dir or config.RUNS_DIR)
        self.runs_dir.mkdir(parents=True, exist_ok=True)
        self._sessions: dict[str, Session] = {}
        self._runtimes: dict[str, Runtime] = {}
        self._lock = threading.Lock()
        import atexit
        atexit.register(self.shutdown_all)

    def shutdown_all(self) -> None:
        """Best-effort teardown of every live runtime (atexit hygiene: never
        leave orphaned browsers/app processes behind)."""
        with self._lock:
            runtimes = list(self._runtimes.values())
            self._runtimes.clear()
        for rt in runtimes:
            try:
                rt.stop()
            except Exception:
                pass

    # ------------------------------------------------------------ lifecycle

    def create(self, app_id: str, budget: dict | None = None) -> Session:
        return self.create_spec(get_app(app_id), budget)

    def create_spec(self, spec, budget: dict | None = None) -> Session:
        """Create a session from a resolved AppSpec — either a registered app
        or an ad-hoc one (e.g. make_live_spec_for_url for a bare URL)."""
        if spec.kind == "live":
            # one live browser profile → at most one live session per target
            with self._lock:
                for s in self._sessions.values():
                    if s.spec.app_id == spec.app_id and s.status == "running":
                        raise RuntimeError(
                            f"live target {spec.app_id} busy: session {s.id} "
                            f"is still running — close it first")
        sid = _new_session_id()
        sdir = self.runs_dir / sid
        data_dir = sdir / "appdata"
        data_dir.mkdir(parents=True, exist_ok=True)
        b = budget or {}
        runtime: Runtime = self._make_runtime(spec, data_dir, sdir)
        try:
            runtime.start()
            if spec.precheck:
                self._run_precheck(spec, runtime)
        except Exception:
            runtime.stop()
            raise
        sess = Session(
            session_id=sid, spec=spec, runtime=runtime, session_dir=sdir,
            budget=Budget(
                max_actions=b.get("max_actions", config.DEFAULT_MAX_ACTIONS),
                max_duration_s=b.get("max_duration_s", config.DEFAULT_MAX_DURATION_S),
                max_observations=b.get("max_observations",
                                       config.DEFAULT_MAX_OBSERVATIONS),
            ),
        )
        with self._lock:
            self._sessions[sid] = sess
            self._runtimes[sid] = runtime
        # initial frame so every session starts with an observation of S0
        sess._capture_frame(0, settle=True)
        return sess

    @staticmethod
    def _run_precheck(spec, runtime: Runtime) -> None:
        """Operator-state gate for live targets (pixels only). One self-heal
        attempt (restore golden profile) before giving up."""
        from . import prechecks
        fn = prechecks.PRECHECKS[spec.precheck]
        try:
            fn(runtime, spec.app_id)
        except prechecks.PreflightError:
            restore = getattr(runtime, "restore_golden", None)
            if restore is None:
                raise
            restore()
            fn(runtime, spec.app_id)  # raises for real this time

    def _make_runtime(self, spec, data_dir: Path, sdir: Path) -> Runtime:
        import os
        if os.environ.get("BBB_RUNTIME") == "docker":
            from ..runtime.docker_x11 import DockerX11Runtime
            return DockerX11Runtime.from_config()
        if spec.kind == "live":
            from ..runtime.live_chromium import LiveChromiumRuntime
            return LiveChromiumRuntime(spec, work_dir=sdir / "runtime")
        app_port = _free_port()
        secret = new_gateway_secret()
        cmd = spec.launch_command(app_port, data_dir)
        return LocalChromiumRuntime(
            app_cmd=cmd, app_port=app_port, gateway_secret=secret,
            work_dir=sdir / "runtime")

    def get(self, sid: str) -> Session:
        with self._lock:
            if sid not in self._sessions:
                # allow reopening a past session dir read-only? no — keep simple
                raise KeyError(sid)
            return self._sessions[sid]

    def list(self) -> list[dict]:
        with self._lock:
            items = []
            for sid, s in self._sessions.items():
                items.append({"session_id": sid, "app_id": s.spec.app_id,
                              "status": s.status, "step": s.step,
                              "created_at": s.created_at})
            # also surface artifact-only sessions from disk
            for d in sorted(self.runs_dir.glob("sess_*")):
                if d.name not in self._sessions and (d / "session.json").exists():
                    import json
                    meta = json.loads((d / "session.json").read_text())
                    items.append({"session_id": d.name,
                                  "app_id": meta.get("app_id"),
                                  "status": "archived",
                                  "created_at": meta.get("created_at")})
            return items

    def reset(self, sid: str) -> None:
        sess = self.get(sid)
        sess.reset()
        if sess.spec.precheck:
            # live target: re-verify operator state after the cold restart
            from . import prechecks
            prechecks.PRECHECKS[sess.spec.precheck](sess.runtime, sess.spec.app_id)

    def close(self, sid: str) -> None:
        sess = self.get(sid)
        sess.close()
        runtime = self._runtimes.get(sid)
        if runtime:
            runtime.stop()

    def apps(self) -> list[dict]:
        return list_apps()
