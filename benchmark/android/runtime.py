"""Trusted Android Emulator pixels + coordinate input Runtime."""
from __future__ import annotations

import io
import ctypes
import hashlib
import os
import re
import shlex
import shutil
import subprocess
import sys
import threading
import time
from contextlib import contextmanager
from pathlib import Path

from PIL import Image

from .. import config
from ..runtime.base import Runtime, RuntimeInfo
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


def _pid_alive(pid: int) -> bool:
    if pid <= 0:
        return False
    if pid == os.getpid():
        return True
    if os.name == "nt":
        # ``os.kill(pid, 0)`` does not provide the Unix existence probe on
        # Windows.  Querying a limited process handle is read-only and avoids
        # incorrectly deleting a live cross-process Android target lease.
        process_query_limited_information = 0x1000
        still_active = 259
        kernel32 = ctypes.windll.kernel32
        handle = kernel32.OpenProcess(
            process_query_limited_information, False, pid)
        if not handle:
            return False
        try:
            exit_code = ctypes.c_ulong()
            if not kernel32.GetExitCodeProcess(
                    handle, ctypes.byref(exit_code)):
                return False
            return exit_code.value == still_active
        finally:
            kernel32.CloseHandle(handle)
    try:
        os.kill(pid, 0)
        return True
    except OSError:
        return False


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
                target = lease_root / f"explore-{os.getpid()}-{id(self):x}.lock"
            fd = os.open(target, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, f"{os.getpid()} {self.lease_mode}".encode("ascii"))
            os.close(fd)
            self._target_lease = target

    def _release_target_lease(self) -> None:
        if self._target_lease is not None:
            self._target_lease.unlink(missing_ok=True)
            self._target_lease = None

    def _run(self, *args: str, timeout: int = 30,
             check: bool = True, binary: bool = False):
        command = [str(self.toolchain.adb)]
        if self.serial:
            command += ["-s", self.serial]
        command += list(args)
        return subprocess.run(
            command, cwd=self.work_dir,
            env=self.toolchain.environment(self._avd_home),
            capture_output=True, text=not binary, timeout=timeout, check=check,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))

    def _reserve_port(self) -> int:
        locks = self.work_dir.parent.parent / "android_port_locks"
        locks.mkdir(parents=True, exist_ok=True)
        for port in range(5554, 5682, 2):
            lock = locks / f"emulator-{port}.lock"
            try:
                fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            except FileExistsError:
                _remove_stale_pid_lock(lock)
                try:
                    fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
                except FileExistsError:
                    continue
            os.write(fd, str(os.getpid()).encode("ascii"))
            os.close(fd)
            self._port_lock = lock
            return port
        raise RuntimeError("no free Android emulator port")

    def _prepare_avd(self) -> str:
        name = "bbb_" + re.sub(r"[^a-z0-9]", "_", self.work_dir.name.lower())
        self._avd_home.mkdir(parents=True, exist_ok=True)
        destination = self._avd_home / f"{name}.avd"
        if destination.exists():
            shutil.rmtree(destination)
        shutil.copytree(self.toolchain.avd_template, destination)
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
            self.process = subprocess.Popen(
                command, cwd=self.work_dir,
                env=self.toolchain.environment(self._avd_home),
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            self._wait_until_booted()
            self._configure_network()
            self._run("install", "-r", "-t", str(apk), timeout=120)
            self._seed_file_picker()
            self._set_orientation()
            self.restart_app()
            self._last_info = self._capture_info()
        except Exception:
            self.stop()
            raise

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
        guard = os.environ.get("BBB_ANDROID_NETWORK_GUARD", "").strip()
        proxy = os.environ.get("BBB_ANDROID_PUBLIC_PROXY", "").strip()
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

    def restart_app(self) -> None:
        self._run("shell", "am", "force-stop", self.spec.package_name,
                  check=False)
        self._run("shell", "am", "start", "-W", "-n", self._component(),
                  timeout=30)
        time.sleep(0.5)

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
        if self.lease_mode != "login" and self._avd_home.exists():
            # On Windows the emulator launcher can exit a moment before its
            # qemu child releases the cloned disk files.  Retry the isolated
            # clone cleanup so normal exploration does not accumulate large,
            # unusable AVD directories.  Login maintenance deliberately keeps
            # its clone until the operator copies the golden profile.
            for attempt in range(20):
                try:
                    shutil.rmtree(self._avd_home)
                    break
                except OSError:
                    if attempt == 19:
                        shutil.rmtree(self._avd_home, ignore_errors=True)
                    else:
                        time.sleep(0.25)

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
            raise RuntimeError("emulator returned an invalid screenshot")
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
        # ADB joins ``shell`` arguments into a remote shell command.  Quote the
        # complete visible-text argument so punctuation cannot become shell
        # syntax inside the trusted emulator.
        encoded = shlex.quote(text.replace("%", "%25").replace(" ", "%s"))
        self._run("shell", "input", "text", encoded)
        self._after_input()

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
