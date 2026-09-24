"""Small cross-process named locks used by shared benchmark services.

Lock files live in the operating-system temporary directory.  The operating
system releases the byte-range lock when a process exits, so a crashed Agent,
Controller or build worker cannot leave a permanent repository lock behind.
"""
from __future__ import annotations

import hashlib
import os
import tempfile
import time
from pathlib import Path


def _try_lock_fd(fd: int) -> bool:
    if os.name == "nt":
        import msvcrt
        try:
            os.lseek(fd, 0, os.SEEK_SET)
            msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
            return True
        except OSError:
            return False
    import fcntl
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        return True
    except OSError:
        return False


def _unlock_fd(fd: int) -> None:
    if os.name == "nt":
        import msvcrt
        try:
            os.lseek(fd, 0, os.SEEK_SET)
            msvcrt.locking(fd, msvcrt.LK_UNLCK, 1)
        except OSError:
            pass
    else:
        import fcntl
        try:
            fcntl.flock(fd, fcntl.LOCK_UN)
        except OSError:
            pass


def lock_for(resource: str, timeout: float = 900.0,
             poll: float = 0.05) -> "_ResourceLock":
    digest = hashlib.sha256(resource.encode("utf-8")).hexdigest()[:16]
    path = (Path(os.environ.get("BBB_LOCK_DIR") or tempfile.gettempdir()) /
            f"bbb-lock-{digest}.lock")
    return _ResourceLock(path, timeout, poll)


def slots_for(resource: str, slots: int, timeout: float = 900.0,
              poll: float = 0.05) -> "_SlotPool":
    """A named pool of ``slots`` concurrent permits, cross-process.

    Same primitive as :func:`lock_for` (an OS-released byte-range lock per
    slot file), but N callers may hold the resource at once. Used for Android
    emulator bring-up: the historical serialization (one boot at a time) was
    calibrated for a host where four parallel qemu bring-ups saturated CPU/IO
    and made every adb operation time out (2026-09-09 incident). On hosts with
    enough CPU/memory the safe operating point is a quota, not a lock, and the
    quota is now a configuration value.

    Permits are claimed fairly-ish by scanning slots 0..N-1 in order and
    sleeping on the first busy one (poll), so there is no starvation.
    """
    if slots < 1:
        raise ValueError("slots must be >= 1")
    digest = hashlib.sha256(resource.encode("utf-8")).hexdigest()[:16]
    root = Path(os.environ.get("BBB_LOCK_DIR") or tempfile.gettempdir())
    paths = [root / f"bbb-slot-{digest}-{i}.lock" for i in range(slots)]
    return _SlotPool(paths, timeout, poll)


class _SlotPool:
    def __init__(self, paths: list[Path], timeout: float, poll: float) -> None:
        self._paths = paths
        self._timeout = timeout
        self._poll = poll
        self._fd: int | None = None

    def acquire(self) -> None:
        deadline = time.monotonic() + self._timeout
        while True:
            # Re-scan from slot 0 after every failed sweep so a free permit
            # is taken as soon as one exists.
            for path in self._paths:
                path.parent.mkdir(parents=True, exist_ok=True)
                fd = os.open(str(path), os.O_RDWR | os.O_CREAT, 0o600)
                if _try_lock_fd(fd):
                    # Deliberately no writes while the range lock is held:
                    # the Windows msvcrt path locks byte 0, and the original
                    # _ResourceLock never touches file contents either — the
                    # same discipline on both platforms keeps behaviour
                    # identical across POSIX and Windows.
                    self._fd = fd
                    return
                os.close(fd)
            if time.monotonic() >= deadline:
                raise TimeoutError(
                    f"no free slot for {self._paths[0].name} "
                    f"({len(self._paths)} slots) within {self._timeout:.0f}s")
            time.sleep(self._poll)

    def release(self) -> None:
        if self._fd is None:
            return
        fd, self._fd = self._fd, None
        try:
            _unlock_fd(fd)
        finally:
            os.close(fd)

    def __enter__(self) -> "_SlotPool":
        self.acquire()
        return self

    def __exit__(self, *_exc) -> None:
        self.release()


class _ResourceLock:
    def __init__(self, path: Path, timeout: float, poll: float) -> None:
        self._path = path
        self._timeout = timeout
        self._poll = poll
        self._fd: int | None = None

    def acquire(self) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(str(self._path), os.O_RDWR | os.O_CREAT, 0o600)
        deadline = time.monotonic() + self._timeout
        while True:
            if _try_lock_fd(fd):
                self._fd = fd
                return
            if time.monotonic() >= deadline:
                os.close(fd)
                raise TimeoutError(
                    f"could not acquire {self._path} within {self._timeout:.0f}s")
            time.sleep(self._poll)

    def release(self) -> None:
        if self._fd is None:
            return
        fd, self._fd = self._fd, None
        try:
            _unlock_fd(fd)
        finally:
            os.close(fd)

    def __enter__(self) -> "_ResourceLock":
        self.acquire()
        return self

    def __exit__(self, *_exc) -> None:
        self.release()
