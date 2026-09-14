"""Trusted Android Emulator pixels + coordinate input Runtime."""
from __future__ import annotations

import io
import ctypes
import hashlib
import os
import re
import shlex
import shutil
import socket
import subprocess
import sys
import threading
import time
import uuid
from contextlib import contextmanager
from pathlib import Path

from PIL import Image

from .. import config
from ..filelock import lock_for
from ..runtime.base import Runtime, RuntimeInfo
from ..scratch import (
    new_scratch_dir, pid_alive, reclaim_scratch, remove_scratch_dir)
from .network_policy import (
    EMULATOR_DNS, PUBLIC_BLOCK_V4, PUBLIC_BLOCK_V6, parse_controlled_proxy)
from .toolchain import AndroidToolchain

_PNG = b"\x89PNG\r\n\x1a\n"
_ALLOWED_TRANSIENT_PACKAGES = {
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
    "com.android.documentsui",
    "com.google.android.documentsui",
    "com.android.providers.media.module",
    "com.google.android.providers.media.module",
}
_KEY_CODES = {
    "Back": "KEYCODE_BACK", "Enter": "KEYCODE_ENTER",
    "Escape": "KEYCODE_ESCAPE", "Backspace": "KEYCODE_DEL",
    "ArrowUp": "KEYCODE_DPAD_UP", "ArrowDown": "KEYCODE_DPAD_DOWN",
    "ArrowLeft": "KEYCODE_DPAD_LEFT", "ArrowRight": "KEYCODE_DPAD_RIGHT",
}
# BCP-47 subset accepted for the guest UI language. The value is passed to an
# adb shell command, so it is validated rather than quoted defensively.
_LOCALE_PATTERN = re.compile(r"[a-z]{2,3}(-[A-Z][a-z]{3})?(-[A-Z]{2})?")


class DeviceError(RuntimeError):
    """A device-level failure, worded so it can never reach an Agent unsafely.

    Raw adb failures carry the tool path, the command line and the emulator
    serial — e.g. ``CalledProcessError(4294967295, ['.../platform-tools/adb.exe',
    '-s', 'emulator-5554', 'exec-out', 'screencap', '-p'])``. That text used to
    travel out through the controller's error detail and into the Agent's tool
    result, which discloses that ADB exists, what it was asked to do, and the
    host's directory layout. ADB must stay wholly inside this trusted process.

    ``str(self)`` is therefore a neutral, operator-agnostic sentence, marked
    with ``agent_safe`` so the Session layer knows it may be passed on. The full
    technical cause is kept in ``detail`` for the local trace only.
    """

    agent_safe = True

    def __init__(self, message: str, detail: str = "", *,
                 recoverable: bool = False):
        super().__init__(message)
        self.detail = detail or message
        self.recoverable = recoverable


def _redacted_cause(exc: BaseException) -> str:
    """Describe a failure for the local trace without a full command line."""
    if isinstance(exc, subprocess.TimeoutExpired):
        return f"device command timed out after {exc.timeout}s"
    if isinstance(exc, subprocess.CalledProcessError):
        return f"device command exited {exc.returncode}"
    return type(exc).__name__


def _pid_alive(pid: int) -> bool:
    """Backwards-compatible alias; the implementation is shared (scratch.py)."""
    return pid_alive(pid)


def _remove_stale_pid_lock(path: Path) -> None:
    try:
        pid = int(path.read_text(encoding="ascii").split()[0])
    except (OSError, ValueError, IndexError):
        pid = -1
    if not _pid_alive(pid):
        path.unlink(missing_ok=True)


@contextmanager
def _pid_mutex(path: Path, timeout: float = 10.0):
    token = f"{os.getpid()} {threading.get_ident()} {time.time_ns()}"
    deadline = time.monotonic() + timeout
    while True:
        try:
            fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, token.encode("ascii")); os.close(fd)
            break
        except FileExistsError:
            _remove_stale_pid_lock(path)
            if time.monotonic() >= deadline:
                raise TimeoutError("Android target lease mutex timed out")
            time.sleep(0.05)
    try:
        yield
    finally:
        try:
            if path.read_text(encoding="ascii") == token:
                path.unlink(missing_ok=True)
        except OSError:
            pass


# Console ports an emulator may occupy; the adb port is always console + 1.
EMULATOR_PORT_RANGE = range(5554, 5682, 2)
LEGACY_PORT_LOCK_DIRNAME = "android_port_locks"


def port_locks_dir() -> Path:
    """The single host-wide namespace for emulator port reservations.

    Exploration, operator/manual sessions, smoke runs, evaluation and
    reproduction review all compete for the same console ports on one machine,
    so the reservation has to live in exactly one directory.  It used to be
    derived from ``work_dir``, whose nesting depth differs per session kind,
    which silently split the reservations into several mutually invisible
    directories: two sessions would each "reserve" emulator-5554, and when one
    emulator stopped, the other's ``adb -s emulator-5554`` calls started
    addressing a dead or foreign device — the observed ``screencap`` exit -1.
    """
    return config.ANDROID_TARGETS_DIR / "port_locks"


def legacy_port_lock_dirs() -> list[Path]:
    """Pre-unification reservation directories, for reporting and reclaim.

    These are no longer written to, but a crashed session can leave files
    behind that an operator still needs to see and clear.
    """
    candidates = [
        config.RUNS_DIR / LEGACY_PORT_LOCK_DIRNAME,
        config.ANDROID_TARGETS_DIR / "manual_sessions" / LEGACY_PORT_LOCK_DIRNAME,
        config.ANDROID_TARGETS_DIR / "smoke" / LEGACY_PORT_LOCK_DIRNAME,
        config.APP_OUTPUT_DIR / "evaluations" / LEGACY_PORT_LOCK_DIRNAME,
    ]
    if config.APP_OUTPUT_DIR.is_dir():
        candidates += sorted(
            config.APP_OUTPUT_DIR.glob(f"*/review/{LEGACY_PORT_LOCK_DIRNAME}"))
    return [path for path in candidates if path.is_dir()]


def _port_pair_free(port: int) -> bool:
    """Whether both the console and adb ports of an emulator slot are unused.

    A reservation file whose owner process is gone is reclaimed automatically,
    but the qemu child it launched can outlive that owner.  Handing the serial
    out while the old emulator still answers would point every later ``adb -s``
    call at the wrong device, so probe the ports as well as the lock file.

    Binding is used rather than connecting: a connect attempt to an unused
    loopback port is silently dropped on some hosts and only fails after the
    timeout, which would make scanning the whole range take half a minute.
    """
    for candidate in (port, port + 1):
        with socket.socket() as probe:
            try:
                probe.bind(("127.0.0.1", candidate))
            except OSError:
                return False
    return True


def clone_root_is_volatile(lease_mode: str) -> bool:
    """Whether this session's AVD clone belongs in scratch space.

    An exploration clone is several GB of disk images nobody reads afterwards,
    so it lives outside the repository (benchmark/scratch.py) and is reclaimed
    by owner PID. Login maintenance is the exception: `scripts/android_live_login.py`
    copies the golden profile out of its clone, so an operator must be able to
    find it at a stable path under `runs/android_targets/login_work/`.
    """
    return lease_mode != "login"


def reclaim_orphan_clones() -> list[tuple[Path, bool]]:
    """Delete AVD clones whose owning process is gone.

    Called before a new emulator boots, so an operator does not have to notice
    the leak: a crashed or force-killed session cannot clean up after itself.
    Ownership comes from the scratch directory name, so no registry is needed.
    """
    return [(path, removed) for path, removed in reclaim_scratch()
            if path.name.startswith("avd_")]


def available_memory_mb() -> int | None:
    """Free host memory in MiB, or None when the platform cannot report it."""
    if os.name == "nt":
        class _MemoryStatus(ctypes.Structure):
            _fields_ = [("dwLength", ctypes.c_ulong),
                        ("dwMemoryLoad", ctypes.c_ulong),
                        ("ullTotalPhys", ctypes.c_ulonglong),
                        ("ullAvailPhys", ctypes.c_ulonglong),
                        ("ullTotalPageFile", ctypes.c_ulonglong),
                        ("ullAvailPageFile", ctypes.c_ulonglong),
                        ("ullTotalVirtual", ctypes.c_ulonglong),
                        ("ullAvailVirtual", ctypes.c_ulonglong),
                        ("ullAvailExtendedVirtual", ctypes.c_ulonglong)]

        status = _MemoryStatus()
        status.dwLength = ctypes.sizeof(_MemoryStatus)
        if not ctypes.windll.kernel32.GlobalMemoryStatusEx(
                ctypes.byref(status)):
            return None
        return int(status.ullAvailPhys // (1024 * 1024))
    try:
        for line in Path("/proc/meminfo").read_text(
                encoding="ascii").splitlines():
            if line.startswith("MemAvailable:"):
                return int(line.split()[1]) // 1024
    except (OSError, IndexError, ValueError):
        pass
    return None


def booting_dir() -> Path:
    """Markers for emulators that have started but are not yet resident."""
    return config.ANDROID_TARGETS_DIR / "booting"


def _booting_reservations() -> int:
    """How many emulators are mid-boot right now, across all processes."""
    root = booting_dir()
    if not root.is_dir():
        return 0
    count = 0
    for marker in root.glob("*.lock"):
        _remove_stale_pid_lock(marker)
        if marker.exists():
            count += 1
    return count


def _require_memory_headroom() -> Path | None:
    """Refuse to boot an emulator the host cannot actually feed.

    Under memory pressure adb does not fail cleanly: individual commands start
    returning exit -1 or timing out while the device looks alive, which ends an
    exploration dozens of steps in and reads like a device fault. An explicit
    refusal before the AVD clone is both honest and cheap to recover from.

    Parallel sessions make this a race: an emulator that is still booting has
    not yet claimed its memory, so two admissions checked at the same moment
    would both pass and then jointly overcommit the host. The check and the
    reservation therefore happen together under a cross-process lock, and each
    booting emulator is charged its expected footprint until it is up.

    Returns the reservation marker to release once the device has booted.
    """
    required = int(getattr(config, "ANDROID_MIN_FREE_MEMORY_MB", 0) or 0)
    if required <= 0:
        return None
    per_emulator = int(getattr(config, "ANDROID_EMULATOR_MEMORY_MB", 0) or 0)
    with lock_for("android:memory-admission", timeout=120):
        available = available_memory_mb()
        if available is None:
            return None
        reserved = _booting_reservations() * per_emulator
        effective = available - reserved
        if effective < required:
            raise RuntimeError(
                f"insufficient free memory for an Android emulator: "
                f"{available} MiB available"
                + (f" minus {reserved} MiB reserved for emulators still booting"
                   if reserved else "")
                + f", {required} MiB required. Close another emulator or "
                f"session, or lower BBB_ANDROID_MIN_FREE_MB if this host is "
                f"known to cope.")
        root = booting_dir()
        root.mkdir(parents=True, exist_ok=True)
        marker = root / f"{os.getpid()}-{uuid.uuid4().hex[:8]}.lock"
        fd = os.open(marker, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
        os.write(fd, str(os.getpid()).encode("ascii"))
        os.close(fd)
        return marker


class AndroidEmulatorRuntime(Runtime):
    """Headless emulator controlled by a deliberately tiny ADB allowlist.

    ADB remains wholly inside this trusted process.  Callers see only PNG
    pixels and generic success/failure receipts from Session.
    """

    def __init__(self, spec, work_dir: Path,
                 toolchain: AndroidToolchain | None = None,
                 boot_timeout: int = 180, *, headed: bool = False,
                 lease_mode: str = "explore"):
        self.spec = spec
        self.work_dir = Path(work_dir)
        self.toolchain = toolchain or AndroidToolchain.discover()
        self.boot_timeout = boot_timeout
        self.headed = headed
        if lease_mode not in ("explore", "login"):
            raise ValueError("lease_mode must be explore or login")
        self.lease_mode = lease_mode
        self.process: subprocess.Popen | None = None
        self.port: int | None = None
        self.serial = ""
        self._port_lock: Path | None = None
        self._avd_home = self.work_dir / "avd_home"
        self._last_info: RuntimeInfo | None = None
        self._target_lease: Path | None = None
        self._guard_attached = False

    def _acquire_target_lease(self) -> None:
        lease_root = config.ANDROID_TARGETS_DIR / "leases" / self.spec.app_id
        lease_root.mkdir(parents=True, exist_ok=True)
        with _pid_mutex(lease_root / ".mutex.lock"):
            login = lease_root / "login.lock"
            _remove_stale_pid_lock(login)
            for candidate in lease_root.glob("explore-*.lock"):
                _remove_stale_pid_lock(candidate)
            explorers = list(lease_root.glob("explore-*.lock"))
            if self.lease_mode == "login":
                if login.exists() or explorers:
                    raise RuntimeError(
                        "target is busy; login maintenance cannot overlap exploration")
                target = login
            else:
                if login.exists():
                    raise RuntimeError("target login maintenance is in progress")
                # A memory address (``id(self)``) is reused once the object it
                # belonged to is collected, so two live runtimes in the same
                # process could end up sharing one lease file name.
                target = (lease_root /
                          f"explore-{os.getpid()}-{uuid.uuid4().hex[:8]}.lock")
            fd = os.open(target, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, f"{os.getpid()} {self.lease_mode}".encode("ascii"))
            os.close(fd)
            self._target_lease = target

    def _release_target_lease(self) -> None:
        if self._target_lease is not None:
            self._target_lease.unlink(missing_ok=True)
            self._target_lease = None

    def _run(self, *args: str, timeout: int = 30,
             check: bool = True, binary: bool = False,
             retries: int = 2):
        """Run one adb command, retrying transient device hiccups.

        A long exploration issues thousands of adb calls. The emulator
        occasionally drops a single one — `screencap` returns exit -1, `am
        start` loses the window, `dumpsys` times out — while the device itself
        recovers immediately. Treating the first such blip as fatal used to end
        an exploration that was tens of steps in, which is an environment
        artefact rather than an observation about the Agent. Retry those blips
        after waiting for the device, and only then give up.

        A persistent failure is re-raised as `DeviceError`, never as the raw
        `CalledProcessError`: the latter carries the adb path, the command line
        and the serial, all of which must stay inside this trusted process.
        """
        command = [str(self.toolchain.adb)]
        if self.serial:
            command += ["-s", self.serial]
        command += list(args)
        environment = self.toolchain.environment(self._avd_home)

        last_error: Exception | None = None
        for attempt in range(max(0, retries) + 1):
            try:
                return subprocess.run(
                    command, cwd=self.work_dir, env=environment,
                    capture_output=True, text=not binary, timeout=timeout,
                    check=check,
                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            except (subprocess.CalledProcessError,
                    subprocess.TimeoutExpired) as exc:
                last_error = exc
                if attempt >= max(0, retries):
                    break
                # Give the device a moment, then confirm it is back before
                # spending the next attempt on the real command.
                time.sleep(1.0 + attempt)
                try:
                    subprocess.run(
                        [str(self.toolchain.adb), "-s", self.serial,
                         "wait-for-device"] if self.serial else
                        [str(self.toolchain.adb), "wait-for-device"],
                        cwd=self.work_dir, env=environment,
                        capture_output=True, timeout=30, check=False,
                        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
                except (OSError, subprocess.SubprocessError):
                    pass
        assert last_error is not None
        raise DeviceError(
            "the device did not complete this operation",
            detail=f"{_redacted_cause(last_error)} after "
                   f"{max(0, retries) + 1} attempts: {args[0] if args else '?'}",
            recoverable=self._device_responsive())

    def _device_responsive(self) -> bool:
        """Is the emulator still usable, or is this failure terminal?

        A single dropped command on a live device is recoverable — the Agent can
        simply try again — whereas a device that no longer answers at all ends
        the session. Distinguishing them is what stops one transient blip from
        discarding an exploration dozens of steps in.

        The probe runs on the very host that just dropped the command it is
        asked about, so the probe's own silence proves nothing: under memory
        pressure ``adb get-state`` times out at the same moment the target
        command did, and reading that as "device gone" is what ended four
        sessions on 2026-09-01 whose emulators were merely starved. adb is
        therefore asked repeatedly, and only an adb that actually answers
        with a non-device state (while the emulator process is still alive)
        counts as terminal. A probe that never answers is conservatively
        read as recoverable: the worst outcome is a retryable failure the
        Agent can see, not the loss of an exploration dozens of steps in.

        The poll ladder spans about a minute. An emulator whose framework
        is wedged under memory pressure answers ``offline`` for tens of
        seconds before settling back to ``device`` — the 2026-09-08
        our-method session was killed at step 51 because the old ~6s
        window saw only the offline phase. Waiting out that phase costs a
        minute on a genuinely dead device, which is far cheaper than
        discarding an exploration that is dozens of steps in.
        """
        if self.process is not None and self.process.poll() is not None:
            return False
        if not self.serial:
            return False
        answered = False
        for delay in (0.0, 2.0, 4.0, 8.0, 16.0, 30.0):
            if delay:
                time.sleep(delay)
            try:
                probe = subprocess.run(
                    [str(self.toolchain.adb), "-s", self.serial, "get-state"],
                    cwd=self.work_dir,
                    env=self.toolchain.environment(self._avd_home),
                    capture_output=True, text=True, timeout=15, check=False,
                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            except (OSError, subprocess.SubprocessError):
                continue
            state = (probe.stdout or "").strip()
            if state == "device":
                return True
            if state or (probe.stderr or "").strip() or probe.returncode != 0:
                # An explicit non-device answer ("offline", "unauthorized",
                # or "error: device not found" on stderr). A transient
                # offline phase right after a dropped connection gets the
                # remaining polls to clear, but adb has answered.
                answered = True
        return not answered

    def _reserve_port(self) -> int:
        locks = port_locks_dir()
        locks.mkdir(parents=True, exist_ok=True)
        for port in EMULATOR_PORT_RANGE:
            lock = locks / f"emulator-{port}.lock"
            try:
                fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            except FileExistsError:
                _remove_stale_pid_lock(lock)
                try:
                    fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                except FileExistsError:
                    continue
            if not _port_pair_free(port):
                # The reservation was stale but its emulator is still answering
                # (a killed launcher leaves an orphaned qemu behind).  Taking
                # this serial now would address someone else's device.
                os.close(fd)
                lock.unlink(missing_ok=True)
                continue
            os.write(fd, str(os.getpid()).encode("ascii"))
            os.close(fd)
            self._port_lock = lock
            return port
        raise RuntimeError("no free Android emulator port")

    def _prepare_clone_root(self) -> None:
        """Move this session's AVD clone out of the repository.

        The clone is several GB of disk images that nobody reads after the
        session, so keeping it under `runs/` both polluted the evidence tree and
        made teardown delete thousands of workspace files. Login maintenance
        keeps its clone at a stable path because the operator copies the golden
        profile out of it.
        """
        if not clone_root_is_volatile(self.lease_mode):
            return
        self._avd_home = new_scratch_dir("avd", self.work_dir.name)

    def _prepare_avd(self) -> str:
        name = "bbb_" + re.sub(r"[^a-z0-9]", "_", self.work_dir.name.lower())
        self._avd_home.mkdir(parents=True, exist_ok=True)
        destination = self._avd_home / f"{name}.avd"
        if destination.exists():
            shutil.rmtree(destination)
        shutil.copytree(self.toolchain.avd_template, destination)
        # The AVD template ships a Windows-authored config.ini whose
        # image.sysdir.1 uses backslashes; the Linux emulator rejects those
        # ("Broken AVD system path") while the Windows one accepts both, so
        # normalize to forward slashes on every clone regardless of host.
        config_path = destination / "config.ini"
        if config_path.is_file():
            text = config_path.read_text(encoding="utf-8")
            fixed = re.sub(r"^(image\.sysdir\.1=.*)$",
                           lambda m: m.group(1).replace("\\", "/"),
                           text, flags=re.MULTILINE)
            if fixed != text:
                config_path.write_text(fixed, encoding="utf-8")
        profile = Path(getattr(self.spec, "profile_snapshot", "") or ".")
        if str(profile) != "." and profile.is_dir():
            shutil.copytree(profile, destination, dirs_exist_ok=True)
        (self._avd_home / f"{name}.ini").write_text(
            f"avd.ini.encoding=UTF-8\npath={destination}\ntarget=android-35\n",
            encoding="utf-8")
        return name

    def start(self) -> None:
        if self.process is not None:
            return
        self.toolchain.require()
        apk = Path(self.spec.apk_path).resolve()
        if not apk.is_file():
            raise FileNotFoundError(
                f"target APK is not built/registered: {self.spec.app_id}")
        expected_sha = getattr(self.spec, "apk_sha256", "")
        if expected_sha and hashlib.sha256(apk.read_bytes()).hexdigest() != expected_sha:
            raise RuntimeError("target APK fingerprint mismatch")
        self.work_dir.mkdir(parents=True, exist_ok=True)
        booting_marker = _require_memory_headroom()
        # A crashed or force-killed session cannot delete its own multi-GB AVD
        # clone; reclaim such leftovers before adding another one.
        reclaim_scratch()
        self._prepare_clone_root()
        self._acquire_target_lease()
        try:
            self.port = self._reserve_port()
            self.serial = f"emulator-{self.port}"
            name = self._prepare_avd()
            command = [str(self.toolchain.emulator), "-avd", name,
                       "-port", str(self.port), "-no-audio",
                       "-no-boot-anim", "-no-snapshot-save", "-gpu",
                       "swiftshader_indirect", "-camera-back", "none",
                       "-camera-front", "none", "-dns-server",
                       "1.1.1.1,8.8.8.8"]
            if not self.headed:
                command.append("-no-window")
            # Boot serialization: concurrent qemu bring-ups saturate host
            # CPU/IO and adb, so simultaneous boots each time out and every
            # caller hangs in an "environment init failed" retry loop
            # (observed with 4 parallel evaluation runs on 2026-09-09). The
            # clone above is per-session scratch and safe to overlap; the
            # emulator bring-up below — boot, the framework restart behind
            # locale pinning, APK install — is not. Sessions submitted
            # together queue here instead of racing each other to death.
            with lock_for("android:boot-serial", timeout=1800):
                self.process = subprocess.Popen(
                    command, cwd=self.work_dir,
                    env=self.toolchain.environment(self._avd_home),
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
                self._wait_until_booted()
                self._configure_network()
                self._apply_locale()
                self._run("install", "-r", "-t", str(apk), timeout=120)
                self._pregrant()
                self._restore_app_data_profile()
                self._seed_file_picker()
                self._set_orientation()
                self.restart_app()
            self._last_info = self._capture_info()
        except Exception:
            self.stop()
            raise
        finally:
            # The emulator is resident (or gone) — its memory now shows up in
            # the host's own figures, so stop double-charging it.
            if booting_marker is not None:
                booting_marker.unlink(missing_ok=True)

    def _wait_until_booted(self) -> None:
        deadline = time.monotonic() + self.boot_timeout
        while time.monotonic() < deadline:
            if self.process is not None and self.process.poll() is not None:
                raise RuntimeError("Android emulator exited during boot")
            try:
                result = self._run("shell", "getprop", "sys.boot_completed",
                                   timeout=5, check=False)
                if result.returncode == 0 and result.stdout.strip() == "1":
                    return
            except (subprocess.SubprocessError, OSError):
                pass
            time.sleep(1)
        raise TimeoutError("Android emulator boot timed out")

    def _configure_network(self) -> None:
        # Defaults keep public sessions working in every process that creates
        # an emulator (controller, reproduction review MCP, evaluation
        # scripts): each has its own environment, so the operator-set
        # variables cannot reach all of them. The canonical proxy listens on
        # a fixed loopback port; the guard ships with the repository.
        guard = (os.environ.get("BBB_ANDROID_NETWORK_GUARD", "").strip()
                 or str(config.PROJECT_ROOT / "scripts"
                        / "android_network_guard.py"))
        proxy = (os.environ.get("BBB_ANDROID_PUBLIC_PROXY", "").strip()
                 or "http://127.0.0.1:8899")
        guest_proxy: tuple[str, int] | None = None
        if self.spec.network_policy == "public":
            if not proxy or not guard:
                raise RuntimeError(
                    "public Android targets require BBB_ANDROID_PUBLIC_PROXY "
                    "and BBB_ANDROID_NETWORK_GUARD")
            guest_proxy = self._parse_controlled_proxy(proxy)
        self._apply_guest_firewall(self.spec.network_policy, guest_proxy)
        if self.spec.network_policy == "offline":
            if guard:
                self._call_external_guard(guard, "attach", self.serial,
                                          "deny-all")
                self._guard_attached = True
            self._run("shell", "settings", "put", "global", "airplane_mode_on",
                      "1", check=False)
            self._run("shell", "am", "broadcast", "-a",
                      "android.intent.action.AIRPLANE_MODE", "--ez", "state",
                      "true", check=False)
            self._run("shell", "svc", "wifi", "disable", check=False)
            self._run("shell", "svc", "data", "disable", check=False)
        else:
            self._call_external_guard(guard, "attach", self.serial,
                                      "public-only", proxy)
            self._guard_attached = True
            assert guest_proxy is not None
            self._run("shell", "settings", "put", "global", "http_proxy",
                      f"{guest_proxy[0]}:{guest_proxy[1]}")

    @staticmethod
    def _call_external_guard(executable: str, *args: str) -> None:
        command = ([sys.executable, executable] if
                   Path(executable).suffix.lower() == ".py" else [executable])
        subprocess.run([*command, *args], check=True, timeout=30,
                       capture_output=True, text=True)

    def _apply_locale(self) -> None:
        """Pin the guest UI language before the target APK is installed.

        The AVD is re-cloned from the template on every start, so a locale set
        by hand inside one session would vanish with that clone. Applying it
        here instead keeps exploration, reproduction review and evaluation in
        one language, which is what makes an operator-authored checklist
        comparable with what an Agent and a judge actually see.

        This is a device-level display setting, not a semantic channel: no
        target data is read and nothing is reported back to the Agent. A failure
        is non-fatal — a wrong UI language degrades comparability, not safety.
        """
        locale = getattr(config, "ANDROID_LOCALE", "").strip()
        if not locale or not _LOCALE_PATTERN.fullmatch(locale):
            if locale:
                raise ValueError(f"invalid BBB_ANDROID_LOCALE: {locale!r}")
            return
        current = self._run("shell", "getprop", "persist.sys.locale",
                            timeout=15, check=False)
        if (current.stdout or "").strip() == locale:
            return
        # setprop on a persist.* key needs root; the guest firewall step above
        # already established it, so a failure here means the image forbids it.
        result = self._run("shell", "setprop", "persist.sys.locale", locale,
                           timeout=15, check=False)
        if result.returncode != 0:
            return
        self._run("shell", "settings", "put", "system", "system_locales",
                  locale, timeout=15, check=False)
        # Restarting the framework (not the emulator) re-inflates every system
        # UI string. The APK is installed afterwards, so no target state is lost.
        self._run("shell", "setprop", "sys.boot_completed", "0",
                  timeout=15, check=False)
        self._run("shell", "stop", timeout=30, check=False)
        self._run("shell", "start", timeout=30, check=False)
        self._wait_until_booted()
        self._wait_for_package_manager()

    def _wait_for_package_manager(self) -> None:
        """After a framework restart `pm`/`am` reject work for a short while."""
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            result = self._run("shell", "cmd", "package", "path", "android",
                               timeout=10, check=False)
            if result.returncode == 0 and "package:" in (result.stdout or ""):
                return
            time.sleep(1)
        raise TimeoutError("Android package manager did not come back after "
                           "the locale restart")

    @staticmethod
    def _parse_controlled_proxy(value: str) -> tuple[str, int]:
        return parse_controlled_proxy(value)

    def _apply_guest_firewall(self, policy: str,
                              proxy: tuple[str, int] | None = None) -> None:
        """Install a mandatory guest-side egress boundary before APK install."""
        identity = self._run("shell", "id", "-u", check=False)
        if identity.stdout.strip() != "0":
            self._run("root", check=False)
            self._run("wait-for-device", timeout=30)
            identity = self._run("shell", "id", "-u", check=False)
        if identity.stdout.strip() != "0":
            raise RuntimeError(
                "Android system image does not permit trusted network isolation")

        def rule(binary: str, *parts: str) -> None:
            result = self._run("shell", binary, *parts, check=False)
            if result.returncode != 0:
                raise RuntimeError("Android guest firewall setup failed")

        for binary in ("iptables", "ip6tables"):
            rule(binary, "-F", "OUTPUT")
        if policy == "offline":
            rule("iptables", "-P", "OUTPUT", "DROP")
            rule("ip6tables", "-P", "OUTPUT", "DROP")
            return
        if policy != "public":
            raise ValueError("Android network policy must be offline or public")
        rule("iptables", "-P", "OUTPUT", "ACCEPT")
        rule("ip6tables", "-P", "OUTPUT", "ACCEPT")
        if proxy is not None:
            binary = "ip6tables" if ":" in proxy[0] else "iptables"
            rule(binary, "-A", "OUTPUT", "-p", "tcp", "-d", proxy[0],
                 "--dport", str(proxy[1]), "-j", "ACCEPT")
        # 10.0.2.3 is the Android Emulator's fixed DNS proxy.  Permit only its
        # DNS port before rejecting the entire private 10/8 range; answers
        # resolving to private destinations remain blocked by the later rules.
        for protocol in ("udp", "tcp"):
            rule("iptables", "-A", "OUTPUT", "-p", protocol, "-d",
                 EMULATOR_DNS, "--dport", "53", "-j", "ACCEPT")
        for network in PUBLIC_BLOCK_V4:
            rule("iptables", "-A", "OUTPUT", "-d", network, "-j", "REJECT")
        for network in PUBLIC_BLOCK_V6:
            rule("ip6tables", "-A", "OUTPUT", "-d", network, "-j", "REJECT")

    def _set_orientation(self) -> None:
        orientation = getattr(self.spec, "orientation", "auto")
        if orientation == "auto":
            self._run("shell", "settings", "put", "system",
                      "accelerometer_rotation", "1", check=False)
            return
        self._run("shell", "settings", "put", "system",
                  "accelerometer_rotation", "0", check=False)
        value = "0" if orientation == "portrait" else "1"
        self._run("shell", "settings", "put", "system", "user_rotation",
                  value, check=False)

    def _seed_file_picker(self) -> None:
        """Populate DocumentsUI with synthetic, operator-owned test files."""
        from app_reproduction.materials.build import ensure_app_materials
        source = ensure_app_materials() / "file_picker"
        destination = "/sdcard/Download/BlackBoxBench"
        self._run("shell", "mkdir", "-p", destination)
        for path in sorted(item for item in source.iterdir() if item.is_file()):
            self._run("push", str(path), f"{destination}/{path.name}", timeout=60)
            self._run("shell", "am", "broadcast", "-a",
                      "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d",
                      f"file://{destination}/{path.name}", check=False)

    def _component(self) -> str:
        activity = self.spec.launch_activity
        if activity.startswith("."):
            activity = self.spec.package_name + activity
        return f"{self.spec.package_name}/{activity}"

    def _restore_app_data_profile(self) -> None:
        """Restore operator-preconfigured app data from a golden profile.

        File-level userdata profiles do not survive this emulator's data
        partition management (disk.dataPartition.path=<temp> rebuilds the
        partition every boot, so copying userdata-qemu.img* into the clone is
        ignored). The profile therefore carries a tarball of
        /data/data/<package> instead: extracted after the APK install, with
        the package's freshly assigned uid/gid and SELinux contexts fixed
        up. Used for apps needing operator pre-configuration, e.g. a TOTP
        app whose FLAG_SECURE screen must be disabled before pixel
        exploration (observed on aegis, 2026-09-10).
        """
        profile = Path(getattr(self.spec, "profile_snapshot", "") or ".")
        tarball = profile / "app_data.tar.gz"
        if str(profile) == "." or not tarball.is_file():
            return
        pkg = self.spec.package_name
        # The fresh install has already created /data/data/<pkg>; capture its
        # uid/gid before replacing the directory so ownership matches this
        # boot's assignment (uids are re-assigned on every clean data boot).
        stat_uid = self._run("shell", "stat", "-c", "%u", f"/data/data/{pkg}",
                             timeout=15, check=False)
        stat_gid = self._run("shell", "stat", "-c", "%g", f"/data/data/{pkg}",
                             timeout=15, check=False)
        uid = (stat_uid.stdout or "").strip() or "0"
        gid = (stat_gid.stdout or "").strip() or "0"
        self._run("root", check=False)
        self._run("wait-for-device", timeout=30, check=False)
        self._run("shell", "rm", "-rf", f"/data/data/{pkg}", check=False)
        self._run("push", str(tarball), "/data/local/tmp/.profile.tar.gz",
                  timeout=120)
        self._run("shell", "tar", "-xzf", "/data/local/tmp/.profile.tar.gz",
                  "-C", "/data/data/", timeout=120, check=False)
        self._run("shell", "chown", "-R", f"{uid}:{gid}", f"/data/data/{pkg}",
                  timeout=60, check=False)
        self._run("shell", "restorecon", "-R", f"/data/data/{pkg}",
                  timeout=60, check=False)
        self._run("shell", "rm", "-f", "/data/local/tmp/.profile.tar.gz",
                  check=False)

    def _pregrant(self) -> None:
        """Apply special-access grants right after the APK install.

        Permission gates such as MANAGE_EXTERNAL_STORAGE route the target to
        the system All-files-access settings page, where the per-step
        foreground-restore guard fails repeatedly and exploration dead-ends
        at the gate ("target App could not be restored to foreground",
        observed on the 2026-09-08 fossify_gallery baseline run). Granting
        before the first launch keeps the whole exploration inside the app.
        Failures are non-fatal: an ungrantable entry simply falls back to the
        app's own permission flow.
        """
        for permission in (getattr(self.spec, "pregrant_permissions", None)
                           or []):
            self._run("shell", "pm", "grant", self.spec.package_name,
                      permission, timeout=15, check=False)
        for appop in (getattr(self.spec, "pregrant_appops", None) or []):
            self._run("shell", "appops", "set", self.spec.package_name,
                      appop, "allow", timeout=15, check=False)

    def restart_app(self) -> None:
        self._run("shell", "am", "force-stop", self.spec.package_name,
                  check=False)
        # `am start` can transiently fail right after a force-stop while the
        # window manager is still tearing the old task down — and under host
        # memory pressure it can stay flaky for tens of seconds (observed on
        # the 2026-09-01 dual-emulator runs). Retry on a widening window and
        # only report a problem if the App really will not come back — a
        # relaunch hiccup must not end an exploration in progress.
        component = self._component()
        for attempt in range(5):
            result = self._run("shell", "am", "start", "-W", "-n", component,
                               timeout=60, check=False, retries=1)
            output = ((result.stdout or "") +
                      (getattr(result, "stderr", "") or ""))
            if result.returncode == 0 and "Error" not in output:
                time.sleep(0.5)
                return
            if attempt < 4:
                time.sleep(1.0 + attempt * attempt)
        raise DeviceError("the target App could not be relaunched",
                          detail="am start kept failing after force-stop",
                          recoverable=self._device_responsive())

    def stop(self) -> None:
        if self._guard_attached and self.serial:
            guard = os.environ.get("BBB_ANDROID_NETWORK_GUARD", "").strip()
            if guard:
                try:
                    self._call_external_guard(guard, "detach", self.serial)
                except (OSError, subprocess.SubprocessError):
                    pass
            self._guard_attached = False
        if self.serial:
            try:
                self._run("emu", "kill", timeout=10, check=False)
            except Exception:
                pass
        if self.process is not None:
            try:
                self.process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.process.terminate()
                try:
                    self.process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.process.kill()
            self.process = None
        if self._port_lock is not None:
            self._port_lock.unlink(missing_ok=True)
            self._port_lock = None
        self.serial = ""
        self.port = None
        self._release_target_lease()
        if clone_root_is_volatile(self.lease_mode) and self._avd_home.exists():
            # Windows can hold a qemu file handle open for a moment after the
            # launcher exits, so this retries patiently. The clone lives in
            # owner-tagged scratch space, so even a hard failure is reclaimed by
            # the next emulator start instead of sitting in the repository.
            remove_scratch_dir(self._avd_home)

    def reset(self) -> None:
        strategy = getattr(self.spec, "reset_strategy", "clear_data")
        if strategy == "restart":
            self.restart_app()
            return
        if strategy == "clear_data":
            self._run("shell", "pm", "clear", self.spec.package_name)
            self.restart_app()
            return
        # A snapshot/profile reset must discard the entire writable clone.
        self.stop()
        shutil.rmtree(self._avd_home, ignore_errors=True)
        self.start()

    def screenshot(self) -> bytes:
        if self.serial:
            self._enforce_foreground()
        result = self._run("exec-out", "screencap", "-p", timeout=20,
                           binary=True)
        raw = bytes(result.stdout)
        if not raw.startswith(_PNG):
            # A truncated or empty capture is almost always transient: the
            # surface was mid-flip. Treat it as a blip on a live device so one
            # bad frame cannot end an exploration.
            raise DeviceError("the screen could not be captured this time",
                              detail="screencap returned a non-PNG payload",
                              recoverable=self._device_responsive())
        self._last_info = self._info_from_png(raw)
        return raw

    def _info_from_png(self, raw: bytes) -> RuntimeInfo:
        with Image.open(io.BytesIO(raw)) as image:
            width, height = image.size
        density = 0
        try:
            result = self._run("shell", "wm", "density", timeout=10,
                               check=False)
            values = re.findall(r"(?:Physical|Override) density:\s*(\d+)",
                                result.stdout or "")
            if values:
                density = int(values[-1])
        except (OSError, subprocess.SubprocessError, ValueError):
            pass
        return RuntimeInfo(width=width, height=height,
                           device_scale_factor=1.0, platform="android",
                           orientation=("portrait" if height >= width else
                                        "landscape"), density_dpi=density)

    def _capture_info(self) -> RuntimeInfo:
        self.screenshot()
        assert self._last_info is not None
        return self._last_info

    def info(self) -> RuntimeInfo:
        if self._last_info is None:
            self._last_info = self._capture_info()
        return self._last_info

    def _after_input(self) -> None:
        time.sleep(0.08)
        # Screenshot enforces foreground and refreshes rotation-aware geometry.
        self._last_info = self._capture_info()

    def _enforce_foreground(self) -> None:
        def focused_package() -> str:
            result = self._run("shell", "dumpsys", "window", "windows",
                               timeout=10, check=False)
            text = result.stdout or ""
            match = re.search(
                r"mCurrentFocus=Window\{[^ ]+ [^ ]+ ([^/\s}]+)/", text)
            if not match:
                match = re.search(r"mFocusedApp=.*? ([^/\s}]+)/", text)
            if match:
                return match.group(1)
            # Android 35 may omit both legacy focus fields from ``dumpsys
            # window windows`` even while an Activity is visibly resumed.
            # This fallback remains inside the trusted Runtime; only the
            # resulting generic success/failure receipt reaches the Agent.
            result = self._run("shell", "dumpsys", "activity", "activities",
                               timeout=10, check=False)
            text = result.stdout or ""
            match = re.search(
                r"(?:topResumedActivity|mResumedActivity)\s*[=:]\s*"
                r"ActivityRecord\{[^}]*?\s([A-Za-z][A-Za-z0-9_.]*)/",
                text,
            )
            return match.group(1) if match else ""

        package = focused_package()
        allowed = {self.spec.package_name, *_ALLOWED_TRANSIENT_PACKAGES}
        if package not in allowed:
            # An unknown focus is not treated as safe.  Restore the target and
            # verify once more before returning any pixels to the Agent.
            self.restart_app()
            if focused_package() not in allowed:
                raise RuntimeError("target App could not be restored to foreground")

    def tap(self, x: int, y: int) -> None:
        self._run("shell", "input", "tap", str(x), str(y))
        self._after_input()

    def long_press(self, x: int, y: int, duration_ms: int = 700) -> None:
        self.swipe(x, y, x, y, duration_ms)

    def swipe(self, x1: int, y1: int, x2: int, y2: int,
              duration_ms: int = 400) -> None:
        self._run("shell", "input", "swipe", str(x1), str(y1), str(x2),
                  str(y2), str(max(1, min(duration_ms, 10_000))))
        self._after_input()

    def type_text(self, text: str) -> None:
        if any(c in text for c in "\r\n\0"):
            raise ValueError("type_text accepts one visible line")
        if not text.isascii():
            self._reject_non_ascii()
        # ADB joins ``shell`` arguments into a remote shell command.  Quote the
        # complete visible-text argument so punctuation cannot become shell
        # syntax inside the trusted emulator.
        encoded = shlex.quote(text.replace("%", "%25").replace(" ", "%s"))
        self._run("shell", "input", "text", encoded)
        self._after_input()

    @staticmethod
    def _reject_non_ascii() -> None:
        """Refuse non-ASCII text as a recoverable action, never as a crash.

        `input text` resolves each character through the device
        KeyCharacterMap, which has no CJK entries: the emulator answers any CJK
        argument with `NullPointerException: Attempt to get length of null
        array` and exit 255, no matter how the bytes are quoted or escaped
        (verified with scripts/android_typing_diagnose.py — a `printf` escape
        delivers the right bytes and still fails). So there is no shortcut to
        repair, and this must not read like a device fault: a person without an
        IME shortcut is in exactly this position and uses the on-screen
        keyboard, which is fully reachable through taps.

        Raising ValueError keeps the session alive (Session.execute turns it
        into a 400) instead of ending an exploration dozens of steps in.
        """
        raise ValueError(
            "unsupported_action: this device can only inject ASCII text "
            "directly. Type the characters on the App's on-screen keyboard, "
            "or use its own picker or suggestion list.")

    def key(self, key: str, kind: str = "press") -> None:
        if kind != "press":
            raise ValueError("Android runtime supports key press only")
        code = _KEY_CODES.get(key)
        if code is None:
            if len(key) == 1 and key.isascii() and key.isalnum():
                code = "KEYCODE_" + key.upper()
            else:
                raise ValueError("unsupported Android key")
        self._run("shell", "input", "keyevent", code)
        self._after_input()

    # Browser-only input is deliberately unavailable to Android Agent tools.
    def click(self, x: int, y: int, button: str = "left", count: int = 1) -> None:
        if button != "left" or count != 1:
            raise ValueError("unsupported Android click")
        self.tap(x, y)

    def mouse_move(self, x: int, y: int) -> None:
        raise ValueError("unsupported_action: pointer hover not available")

    def mouse_down(self, x: int, y: int, button: str = "left") -> None:
        raise ValueError("unsupported_action: mouse not available")

    def mouse_up(self, x: int, y: int, button: str = "left") -> None:
        raise ValueError("unsupported_action: mouse not available")

    def scroll(self, dx: int, dy: int) -> None:
        info = self.info()
        x = info.width // 2
        if dy >= 0:
            self.swipe(x, int(info.height * .75), x, int(info.height * .25), 400)
        else:
            self.swipe(x, int(info.height * .25), x, int(info.height * .75), 400)

    def health(self) -> bool:
        if self.process is None or self.process.poll() is not None:
            return False
        try:
            return self._run("get-state", timeout=5,
                             check=False).stdout.strip() == "device"
        except Exception:
            return False
