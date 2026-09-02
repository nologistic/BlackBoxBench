"""Volatile runtime scratch space, deliberately outside the repository.

Two runtime artefacts are large, numerous and worthless the moment a session
ends: a Chromium `user-data-dir` (hundreds of cache files) and an Android AVD
clone (several GB of disk images). Both used to be created inside the session
directory, i.e. inside the repository, which had three bad consequences:

- every teardown recursively deleted hundreds to thousands of files *in the
  workspace*, which is slow and trips editor/IDE bulk-delete protection;
- `runs/` is the evidence tree — frames, traces, topology. Browser cache and
  emulator disk images do not belong in an audit log;
- when deletion failed (Windows holds qemu/renderer handles for a moment), the
  leftovers stayed in the repository. Two such AVD clones were found holding
  7.8 GB.

Scratch entries are therefore created here, named after the process that owns
them, so an entry whose owner is gone can be reclaimed without knowing anything
about the session that made it.
"""
from __future__ import annotations

import ctypes
import os
import shutil
import tempfile
import time
from pathlib import Path

from . import config


def pid_alive(pid: int) -> bool:
    """Whether a process is still running.

    This is the shared basis for every "reclaim it once its owner is gone" rule
    in the project: scratch directories, emulator port reservations, target
    leases and orphaned AVD clones. It lives here because this module has no
    dependencies beyond `config`, so anything may import it.
    """
    if pid <= 0:
        return False
    if pid == os.getpid():
        return True
    if os.name == "nt":
        # ``os.kill(pid, 0)`` does not provide the Unix existence probe on
        # Windows. Querying a limited process handle is read-only and avoids
        # incorrectly reclaiming a resource a live process still owns.
        process_query_limited_information = 0x1000
        still_active = 259
        kernel32 = ctypes.windll.kernel32
        handle = kernel32.OpenProcess(
            process_query_limited_information, False, pid)
        if not handle:
            return False
        try:
            exit_code = ctypes.c_ulong()
            if not kernel32.GetExitCodeProcess(handle, ctypes.byref(exit_code)):
                return False
            return exit_code.value == still_active
        finally:
            kernel32.CloseHandle(handle)
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


def scratch_root() -> Path:
    return config.SCRATCH_DIR


def _owner_of(path: Path) -> int:
    """PID encoded in a scratch directory name, or 0 when unknown."""
    parts = path.name.split("_")
    for part in reversed(parts):
        if part.isdigit():
            return int(part)
    return 0


def new_scratch_dir(kind: str, label: str = "") -> Path:
    """Create an empty scratch directory owned by this process."""
    safe_kind = "".join(c for c in kind if c.isalnum() or c in "-") or "run"
    safe_label = "".join(c for c in label if c.isalnum() or c in "-_")[:48]
    root = scratch_root()
    root.mkdir(parents=True, exist_ok=True)
    prefix = f"{safe_kind}_{safe_label}_{os.getpid()}_" if safe_label else \
        f"{safe_kind}_{os.getpid()}_"
    # mkdtemp appends its own random suffix and guarantees exclusive creation.
    return Path(tempfile.mkdtemp(prefix=prefix, dir=str(root)))


def remove_scratch_dir(path: Path, *, attempts: int = 40,
                       delay: float = 0.25) -> bool:
    """Delete one scratch directory, waiting out a slow handle release.

    Windows regularly keeps a renderer or qemu file handle open for a moment
    after the process exits, so the first attempt legitimately fails.
    """
    target = Path(path)
    for attempt in range(max(1, attempts)):
        try:
            shutil.rmtree(target)
            return True
        except FileNotFoundError:
            return True
        except OSError:
            if attempt == max(1, attempts) - 1:
                return False
            time.sleep(delay)
    return False


def reclaim_scratch(is_alive=None) -> list[tuple[Path, bool]]:
    """Delete scratch directories whose owning process is gone.

    A crashed or force-killed session cannot clean up after itself. Ownership is
    encoded in the directory name precisely so this does not need a registry or
    any knowledge of session layout.

    `is_alive` is resolved at call time rather than bound as a default, so the
    liveness probe stays substitutable.
    """
    probe = is_alive or pid_alive
    root = scratch_root()
    if not root.is_dir():
        return []
    results: list[tuple[Path, bool]] = []
    for entry in sorted(root.iterdir()):
        if not entry.is_dir():
            continue
        owner = _owner_of(entry)
        if owner == os.getpid():
            continue
        if owner and probe(owner):
            continue
        results.append((entry, remove_scratch_dir(entry, attempts=4)))
    return results


def scratch_usage() -> tuple[int, float]:
    """(entry count, total gigabytes) for operator reporting."""
    root = scratch_root()
    if not root.is_dir():
        return 0, 0.0
    entries = [p for p in root.iterdir() if p.is_dir()]
    total = 0
    for entry in entries:
        for path in entry.rglob("*"):
            try:
                if path.is_file():
                    total += path.stat().st_size
            except OSError:
                continue
    return len(entries), total / 1e9


__all__ = ["new_scratch_dir", "pid_alive", "reclaim_scratch",
           "remove_scratch_dir", "scratch_root", "scratch_usage"]
