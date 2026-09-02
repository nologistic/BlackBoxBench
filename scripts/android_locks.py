"""Operator tool: report and reclaim stale Android emulator locks.

Session locks are owned by a live PID. When a manual session window is closed
without `q`, or a launcher is killed, the lock file outlives its owner and the
next start is refused. The runtime reclaims such files automatically, but an
operator sometimes needs to see and clear the state explicitly.

Reported state covers the unified port reservation namespace, the pre-
unification directories a crashed session may still have entries in, the target
leases, which emulator ports are actually answering — a port that answers while
its lock is stale is an orphaned qemu, which `--kill-emulators` clears — and the
volatile scratch space holding AVD clones and browser profiles.

    vendor/python/python.exe scripts/android_locks.py            # report only
    vendor/python/python.exe scripts/android_locks.py --reclaim   # drop stale
"""
from __future__ import annotations

import argparse
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.runtime import (
    EMULATOR_PORT_RANGE, _pid_alive, _port_pair_free, _remove_stale_pid_lock,
    legacy_port_lock_dirs, port_locks_dir)
from benchmark.scratch import reclaim_scratch, scratch_root, scratch_usage


def _emulator_processes() -> list[tuple[str, str]]:
    try:
        result = subprocess.run(["tasklist", "/FO", "CSV", "/NH"],
                                capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return []
    found = []
    for line in result.stdout.splitlines():
        lowered = line.lower()
        if "emulator" in lowered or "qemu" in lowered:
            parts = [field.strip('"') for field in line.split('","')]
            if len(parts) >= 2:
                found.append((parts[1], parts[0]))
    return found


def _lock_files() -> list[Path]:
    """Every reservation an Android session can leave behind.

    The port namespace used to be derived from each session's work directory,
    so reservations landed in several different trees — including ones outside
    the Android target root, which this tool never looked at.  Report the
    unified namespace plus the legacy directories, otherwise "no stale locks"
    is a false reassurance.
    """
    found: set[Path] = set((config.ANDROID_TARGETS_DIR / "leases").rglob("*.lock"))
    found.update(port_locks_dir().glob("*.lock"))
    for legacy in legacy_port_lock_dirs():
        found.update(legacy.glob("*.lock"))
    return sorted(found)


def _display(path: Path) -> str:
    for base in (config.ANDROID_TARGETS_DIR, config.RUNS_DIR,
                 config.APP_OUTPUT_DIR, config.PROJECT_ROOT):
        try:
            return path.relative_to(base).as_posix()
        except ValueError:
            continue
    return str(path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reclaim", action="store_true",
                        help="delete locks whose owning process is gone")
    parser.add_argument("--kill-emulators", action="store_true",
                        help="force-stop leftover emulator/qemu processes first")
    args = parser.parse_args()

    if args.kill_emulators:
        for pid, name in _emulator_processes():
            try:
                subprocess.run(["taskkill", "/PID", pid, "/F"],
                               capture_output=True, timeout=30, check=False)
                print(f"killed {pid}:{name}")
            except (OSError, subprocess.SubprocessError):
                print(f"could not kill {pid}:{name}")
        time.sleep(5)

    processes = _emulator_processes()
    print("emulator/qemu processes:",
          ", ".join(f"{pid}:{name}" for pid, name in processes) or "none")
    answering = [port for port in EMULATOR_PORT_RANGE if not _port_pair_free(port)]
    # A port that still answers while its lock is stale means an orphaned qemu:
    # the reservation can be reclaimed, but the slot stays unusable until the
    # process is killed (--kill-emulators).
    print("emulator ports answering:",
          ", ".join(str(port) for port in answering) or "none")

    locks = _lock_files()
    if locks:
        for lock in locks:
            try:
                owner = lock.read_text(encoding="ascii",
                                       errors="ignore").split()[0]
            except (OSError, IndexError):
                owner = ""
            alive = _pid_alive(int(owner)) if owner.isdigit() else False
            state = "ALIVE" if alive else "STALE"
            if args.reclaim and not alive:
                _remove_stale_pid_lock(lock)
            action = ("removed" if args.reclaim and not lock.exists()
                      else "kept")
            print(f"  [{state}] owner={owner or '?'} {action}  {_display(lock)}")
    else:
        print("locks: none")

    _report_scratch(args.reclaim)


def _report_scratch(reclaim: bool) -> None:
    """Volatile scratch holds AVD clones and browser profiles — the costliest
    leftovers by far (a single clone is several GB)."""
    count, gigabytes = scratch_usage()
    print(f"scratch ({scratch_root()}): {count} entries, {gigabytes:.1f}GB")
    if not count:
        return
    if reclaim:
        for path, removed in reclaim_scratch():
            print(f"  {'removed' if removed else 'STILL LOCKED'}  {path.name}")
        remaining, gigabytes = scratch_usage()
        if remaining:
            print(f"  {remaining} entries left ({gigabytes:.1f}GB): owned by a "
                  f"live process, or still held open (try --kill-emulators)")
        return
    for entry in sorted(p for p in scratch_root().iterdir() if p.is_dir()):
        print(f"  [SCRATCH] {entry.name}")


if __name__ == "__main__":
    main()
