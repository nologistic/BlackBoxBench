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
