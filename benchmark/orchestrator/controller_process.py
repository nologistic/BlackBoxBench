"""Single, crash-aware bootstrap path for the shared local Controller."""
from __future__ import annotations

import json
import os
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
        return
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
