"""Single, crash-aware bootstrap path for the shared local Controller."""
from __future__ import annotations

import json
import os
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable
from urllib.parse import urlsplit

from benchmark import config
from benchmark.filelock import lock_for


def _endpoint(controller: str) -> tuple[str, int]:
    parsed = urlsplit(controller)
    if (parsed.scheme != "http" or parsed.username or parsed.password or
            parsed.query or parsed.fragment or parsed.path not in ("", "/") or
            parsed.hostname not in ("127.0.0.1", "localhost", "::1") or
            parsed.port is None):
        raise RuntimeError(
            "automatic Controller startup is allowed only for an explicit "
            "loopback http://host:port endpoint")
    host = "127.0.0.1" if parsed.hostname == "localhost" else parsed.hostname
    return host, parsed.port


def _state_root() -> Path:
    override = os.environ.get("BBB_CONTROLLER_STATE_DIR")
    return (Path(override).expanduser().resolve() if override else
            (config.RUNS_DIR / "controller").resolve())


def _state_path(controller: str) -> Path:
    import hashlib
    key = hashlib.sha256(controller.encode("utf-8")).hexdigest()[:12]
    return _state_root() / f"controller-{key}.json"


def _pid_alive(pid: int) -> bool:
    if pid <= 0:
        return False
    try:
        os.kill(pid, 0)
        return True
    except (OSError, ValueError):
        return False


def _env_fingerprint() -> str:
    """Fingerprint of the environment a Controller was launched with.

    The Controller is a machine-wide singleton keyed only by host:port, but
    its behaviour depends on the launching environment (DISPLAY for Android
    emulators, BBB_RUNS_DIR for live targets, PROJECT_ROOT, live proxy).
    Reusing a Controller started by a *different* environment silently breaks
    every session it serves - 2026-09-16: eight fresh runs inherited an old
    Android Controller without DISPLAY and every browser launch failed. The
    fingerprint lets ensure_local_controller detect and heal that mismatch.
    """
    import hashlib
    parts = [
        os.environ.get("DISPLAY", ""),
        os.environ.get("BBB_RUNS_DIR", ""),
        os.environ.get("BBB_LIVE_PROXY", ""),
        str(config.PROJECT_ROOT.resolve()),
    ]
    digest = hashlib.sha256("|".join(parts).encode("utf-8")).hexdigest()
    return digest[:16]


def _state_env_matches(controller: str) -> bool:
    """True when reusing the recorded Controller is considered safe.

    - no state record: trust it. The Controller was started outside this
      bootstrap path (operator or test harness) and carries no stale
      environment question.
    - recorded fingerprint: must match this environment.
    - stale record WITHOUT a fingerprint (older revision): NOT safe - an
      unverifiable leftover from another environment is exactly the hazard
      this guard exists for (2026-09-16: eight runs inherited an old
      Android Controller without DISPLAY).
    """
    state = _read_state(controller)
    if not state:
        return True
    return str(state.get("env_fingerprint") or "") == _env_fingerprint()


def _controller_has_active_sessions(controller: str) -> bool:
    """Ask the Controller whether any session is still running.

    Conservative on purpose: any probe failure or unknown status counts as
    active so a stale Controller with live work is never killed blindly.
    """
    import urllib.request
    try:
        # Loopback only: never let a host http_proxy intercept this probe.
        opener = urllib.request.build_opener(
            urllib.request.ProxyHandler({}))
        with opener.open(f"{controller}/api/sessions", timeout=5) as response:
            sessions = json.loads(response.read().decode("utf-8"))
    except Exception:
        return True
    if not isinstance(sessions, list):
        return True
    idle = {"closed", "failed"}
    for session in sessions:
        if not isinstance(session, dict):
            return True
        if str(session.get("status") or "").lower() not in idle:
            return True
    return False


def _terminate_stale_controller(controller: str) -> None:
    """Stop a leftover Controller whose environment no longer matches."""
    state = _read_state(controller)
    pid = int(state.get("pid") or 0)
    if pid and _pid_alive(pid):
        try:
            os.kill(pid, signal.SIGTERM)
        except OSError:
            pass
        deadline = time.monotonic() + 10.0
        while time.monotonic() < deadline and _pid_alive(pid):
            time.sleep(0.25)
        if _pid_alive(pid):
            hard = getattr(signal, "SIGKILL", None)
            if hard is not None:
                try:
                    os.kill(pid, hard)
                except OSError:
                    pass
    try:
        _state_path(controller).unlink()
    except OSError:
        pass


def _read_state(controller: str) -> dict:
    path = _state_path(controller)
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (OSError, ValueError):
        return {}


def _write_state(controller: str, process: subprocess.Popen,
                 log_path: Path) -> None:
    path = _state_path(controller)
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "schema_version": 1,
        "controller": controller,
        "pid": process.pid,
        "started_at": datetime.now(timezone.utc).isoformat(),
        "python": str(Path(sys.executable).resolve()),
        "project_root": str(config.PROJECT_ROOT.resolve()),
        "log": str(log_path),
        "env_fingerprint": _env_fingerprint(),
    }
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    os.replace(temporary, path)


def _log_tail(path: Path, limit: int = 3000) -> str:
    try:
        data = path.read_bytes()[-limit:]
        return data.decode("utf-8", errors="replace").strip()
    except OSError:
        return ""


def start_local_controller(controller: str, is_up: Callable[[], bool],
                           *, timeout: float = 40.0) -> int:
    """Start one loopback Controller and return its PID when healthy."""
    host, port = _endpoint(controller)
    root = _state_root()
    root.mkdir(parents=True, exist_ok=True)
    log_path = root / "server.log"
    flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    flags |= getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0)
    with log_path.open("ab", buffering=0) as log:
        process = subprocess.Popen(
            [sys.executable, "-m", "benchmark.server", "--host", host,
             "--port", str(port)],
            cwd=str(config.PROJECT_ROOT), stdin=subprocess.DEVNULL,
            stdout=log, stderr=subprocess.STDOUT, close_fds=True,
            creationflags=flags)
        _write_state(controller, process, log_path)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if is_up():
            return process.pid
        code = process.poll()
        if code is not None:
            detail = _log_tail(log_path)
            raise RuntimeError(
                f"Controller exited during startup (code {code})" +
                (f": {detail}" if detail else ""))
        time.sleep(0.25)
    try:
        process.terminate()
        process.wait(timeout=5)
    except (OSError, subprocess.SubprocessError):
        try:
            process.kill()
        except OSError:
            pass
    raise RuntimeError(
        f"Controller did not become healthy at {controller}; "
        f"see {log_path}")


def ensure_local_controller(controller: str, is_up: Callable[[], bool],
                            *, announce: Callable[[str], None] | None = None,
                            timeout: float = 40.0) -> None:
    """Ensure one shared local Controller without duplicate process storms."""
    if is_up():
        if _state_env_matches(controller):
            return
        # The reachable Controller was launched with a different environment
        # (e.g. an Android batch's leftover serving a new live run). Heal it
        # only when it has no running session; otherwise surface the conflict.
        if _controller_has_active_sessions(controller):
            raise RuntimeError(
                f"Controller at {controller} was started with a different "
                "environment and still has running sessions; close them (or "
                "stop the stale Controller) before starting new runs")
        if announce:
            announce(f"Controller at {controller} belongs to a different "
                     "environment; restarting it for this run")
        _terminate_stale_controller(controller)
    _endpoint(controller)
    if announce:
        announce(f"Controller not reachable at {controller}; starting it…")
    with lock_for(f"controller:{controller}", timeout=max(120.0, timeout + 10)):
        if is_up():
            return
        state = _read_state(controller)
        pid = int(state.get("pid") or 0)
        if state.get("controller") == controller and _pid_alive(pid):
            deadline = time.monotonic() + min(5.0, timeout)
            while time.monotonic() < deadline:
                if is_up():
                    return
                time.sleep(0.25)
            raise RuntimeError(
                f"Controller process {pid} is alive but unhealthy; inspect "
                f"{state.get('log') or _state_root() / 'server.log'}")
        start_local_controller(controller, is_up, timeout=timeout)


__all__ = ["ensure_local_controller", "start_local_controller"]
