"""Session manager: creation, lookup, reset and teardown."""
from __future__ import annotations

import json
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
        # Live targets whose session is mid-create. runtime.start() takes
        # minutes and runs outside the lock; without this reservation two
        # concurrent creates can both pass the "already running" check and
        # race to boot two browsers on one login profile (SingletonLock
        # flapping, restore_golden corrupting the golden profile).
        self._live_reservations: set[str] = set()
        self._lock = threading.Lock()
        import atexit
        atexit.register(self.shutdown_all)

    def mark_abandoned_sessions(self) -> list[str]:
        """Correct on-disk state left by a Controller that did not exit cleanly.

        Sessions live in memory only, so a force-killed Controller leaves
        `session.json` claiming `running` for a session no API call can ever
        reach again: it cannot be observed, closed or resumed. Recording that
        honestly at startup keeps operator tooling and post-hoc analysis from
        trusting a status that is certainly wrong. Frames, traces and topology
        are left untouched — they are the audit log.
        """
        corrected: list[str] = []
        for meta_path in sorted(self.runs_dir.glob("sess_*/session.json")):
            try:
                meta = json.loads(meta_path.read_text(encoding="utf-8"))
            except (OSError, ValueError):
                continue
            if not isinstance(meta, dict) or meta.get("status") != "running":
                continue
            meta["status"] = "failed"
            meta["close_reason"] = (
                "controller exited without closing this session; its runtime "
                "is gone and the session cannot be resumed")
            try:
                meta_path.write_text(json.dumps(meta, indent=2),
                                     encoding="utf-8")
            except OSError:
                continue
            corrected.append(meta_path.parent.name)
        return corrected

    def shutdown_all(self) -> None:
        """Best-effort teardown of every live runtime (atexit hygiene: never
        leave orphaned browsers/app processes behind)."""
        with self._lock:
            entries = [(sid, self._sessions.get(sid), runtime)
                       for sid, runtime in self._runtimes.items()]
            self._runtimes.clear()
        for _sid, session, runtime in entries:
            try:
                if session is not None and session.status != "closed":
                    session.close(reason="controller_shutdown")
                else:
                    runtime.stop()
            except Exception:
                try:
                    runtime.stop()
                except Exception:
                    pass

    # ------------------------------------------------------------ lifecycle

    def create(self, app_id: str, budget: dict | None = None) -> Session:
        return self.create_spec(get_app(app_id), budget)

    def create_spec(self, spec, budget: dict | None = None) -> Session:
        """Create a session from a resolved AppSpec — either a registered app
        or an ad-hoc one (e.g. make_live_spec_for_url for a bare URL)."""
        import os
        reserved_live = False
        if spec.kind == "live":
            # one live browser profile → at most one live session per target.
            # Reserve BEFORE releasing the lock: runtime.start() below takes
            # minutes, so a plain check-then-start would let two concurrent
            # creates both pass and open the same profile simultaneously.
            with self._lock:
                for s in self._sessions.values():
                    if s.spec.app_id == spec.app_id and s.status == "running":
                        raise RuntimeError(
                            f"live target {spec.app_id} busy: session {s.id} "
                            f"is still running — close it first")
                if spec.app_id in self._live_reservations:
                    raise RuntimeError(
                        f"live target {spec.app_id} busy: a concurrent "
                        f"session creation is still starting — retry shortly")
                self._live_reservations.add(spec.app_id)
                reserved_live = True
        if os.environ.get("BBB_RUNTIME") == "docker":
            # docker-mode runtimes all attach to the SAME reference container
            # (one browser, one data dir): two concurrent sessions would see
            # and mutate each other's screen. Refuse instead of contaminating;
            # run concurrent sessions with the host controller (local runtime).
            with self._lock:
                for s in self._sessions.values():
                    if s.status == "running" and getattr(
                            s.runtime, "shared_environment", False):
                        raise RuntimeError(
                            f"docker reference environment busy: session {s.id} "
                            f"is still running — the containerized deployment "
                            f"shares one reference browser; close it first or "
                            f"use the host controller (local runtime) for "
                            f"concurrent sessions")
        sid = _new_session_id()
        sdir = self.runs_dir / sid
        data_dir = sdir / "appdata"
        data_dir.mkdir(parents=True, exist_ok=True)
        b = budget or {}
        runtime: Runtime = self._make_runtime(spec, data_dir, sdir)
        sess: Session | None = None
        try:
            runtime.start()
            if spec.precheck:
                self._run_precheck(spec, runtime)
            sess = Session(
                session_id=sid, spec=spec, runtime=runtime, session_dir=sdir,
                budget=Budget(
                    max_actions=b.get("max_actions", config.DEFAULT_MAX_ACTIONS),
                    max_duration_s=b.get("max_duration_s", config.DEFAULT_MAX_DURATION_S),
                    max_observations=b.get("max_observations",
                                           config.DEFAULT_MAX_OBSERVATIONS),
                ),
            )
            # Capture S0 before publishing the session. If startup produced
            # no usable pixels, callers must not receive a broken live session.
            sess._capture_frame(0, settle=True)
        except Exception:
            if reserved_live:
                with self._lock:
                    self._live_reservations.discard(spec.app_id)
            if sess is not None:
                sess.recorder.close()
            try:
                runtime.stop()
            except Exception:
                pass
            raise
        with self._lock:
            self._sessions[sid] = sess
            self._runtimes[sid] = runtime
            if reserved_live:
                # Hand the reservation over to the registered session inside
                # the same lock — no gap where the target looks free.
                self._live_reservations.discard(spec.app_id)
        return sess

    def reopen_session(self, sid: str) -> Session:
        """Reopen a closed session: fresh runtime, preserved topology.

        Used when an agent resumes after an interruption (provider quota,
        crash) and needs to continue the same exploration. The session's
        TopologyStore (states, features, edges) survives in the controller's
        memory; only the runtime (emulator/browser) and recorder are rebuilt.
        """
        with self._lock:
            old = self._sessions.get(sid)
            if old is None:
                raise ValueError(f"no such session: {sid}")
            if old.status != "closed":
                raise ValueError(f"session {sid} is {old.status}, not closed")
            data_dir = old.dir / "appdata"
            runtime = self._make_runtime(old.spec, data_dir, old.dir)
            runtime.start()
            old.reopen(runtime)
            self._runtimes[sid] = runtime
            return old

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
        if getattr(spec, "platform", "web") == "android":
            from ..android.runtime import AndroidEmulatorRuntime
            return AndroidEmulatorRuntime(spec, work_dir=sdir / "runtime")
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
            return [{"session_id": sid, "app_id": s.spec.app_id,
                     "platform": getattr(s.spec, "platform", "web"),
                     "status": s.status, "step": s.step,
                     "created_at": s.created_at}
                    for sid, s in self._sessions.items()]

    def reset(self, sid: str) -> None:
        sess = self.get(sid)
        sess.reset()
        if sess.spec.precheck:
            # live target: re-verify operator state after the cold restart
            from . import prechecks
            prechecks.PRECHECKS[sess.spec.precheck](sess.runtime, sess.spec.app_id)

    def close(self, sid: str) -> None:
        sess = self.get(sid)
        try:
            sess.close()
        finally:
            self.release_runtime(sid)

    def release_runtime(self, sid: str) -> None:
        """Drop a stopped runtime handle while retaining session history."""
        with self._lock:
            self._runtimes.pop(sid, None)

    def apps(self, platform: str = "web") -> list[dict]:
        return list_apps(platform)
