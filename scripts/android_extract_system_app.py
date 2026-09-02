"""Operator tool: extract a pre-installed system app from the emulator image
and register it as a black-box Android target.

Targets must be real APK files (the runtime verifies a SHA-256 fingerprint
before every session), but apps such as the stock Calculator ship inside the
system image rather than as a standalone file. This script boots a throwaway
emulator clone, locates the package, pulls its APK into the protected target
tree, resolves its launch activity, and registers it.

It is operator-only: it touches the SDK, the AVD and `runs/android_targets/`,
none of which is ever reachable from an Agent or a reproduction container.

    # See what the system image offers
    vendor/python/python.exe scripts/android_extract_system_app.py --list

    # Extract and register the stock calculator
    vendor/python/python.exe scripts/android_extract_system_app.py \
        --id calculator --match calculator \
        --description "Stock Android calculator" \
        --brief "这是一个计算器 App。请只通过可见屏幕和坐标级触控探索它的功能。"
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.targets import register_android_target
from benchmark.android.toolchain import AndroidToolchain

BOOT_TIMEOUT = 240
# Packages that are part of the platform plumbing rather than a usable app.
_SKIP_PREFIXES = (
    "com.android.internal", "com.android.providers", "com.android.server",
    "com.android.systemui", "com.android.shell", "com.android.externalstorage",
    "com.android.wallpaper", "com.android.inputdevices", "com.android.keychain",
    "com.android.location", "com.android.cts", "com.android.dynsystem",
    "com.android.emulator", "com.google.android.gms", "com.google.android.gsf",
    "com.google.android.ext", "com.google.android.syncadapters",
    "com.google.android.onetimeinitializer", "com.google.android.partner",
    "com.google.android.configupdater", "com.google.android.printservice",
    "android.ext", "android.auto",
)


class _TempEmulator:
    """A throwaway, offline, headless emulator used only for extraction."""

    def __init__(self, toolchain: AndroidToolchain, work_dir: Path):
        self.toolchain = toolchain
        self.work_dir = work_dir
        self.avd_home = work_dir / "avd_home"
        self.process: subprocess.Popen | None = None
        self.serial = ""
        self.name = ""

    def _adb(self, *args: str, timeout: int = 30, check: bool = True,
             binary: bool = False):
        command = [str(self.toolchain.adb)]
        if self.serial:
            command += ["-s", self.serial]
        command += list(args)
        return subprocess.run(
            command, cwd=self.work_dir,
            env=self.toolchain.environment(self.avd_home),
            capture_output=True, text=not binary, timeout=timeout, check=check,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))

    def start(self) -> None:
        self.toolchain.require()
        self.work_dir.mkdir(parents=True, exist_ok=True)
        self.avd_home.mkdir(parents=True, exist_ok=True)
        self.name = "bbb_extract_" + str(int(time.time()))
        destination = self.avd_home / f"{self.name}.avd"
        shutil.copytree(self.toolchain.avd_template, destination)
        (self.avd_home / f"{self.name}.ini").write_text(
            f"avd.ini.encoding=UTF-8\npath={destination}\ntarget=android-35\n",
            encoding="utf-8")
        port = 5584  # outside the exploration range used by the runtime
        self.serial = f"emulator-{port}"
        command = [str(self.toolchain.emulator), "-avd", self.name,
                   "-port", str(port), "-no-window", "-no-audio",
                   "-no-boot-anim", "-no-snapshot-save",
                   "-gpu", "swiftshader_indirect",
                   "-camera-back", "none", "-camera-front", "none"]
        print(f"[extract] booting throwaway emulator on {self.serial} …")
        self.process = subprocess.Popen(
            command, cwd=self.work_dir,
            env=self.toolchain.environment(self.avd_home),
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        deadline = time.monotonic() + BOOT_TIMEOUT
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError("emulator exited during boot")
            try:
                result = self._adb("shell", "getprop", "sys.boot_completed",
                                   timeout=5, check=False)
                if result.returncode == 0 and result.stdout.strip() == "1":
                    print("[extract] emulator booted")
                    return
            except (subprocess.SubprocessError, OSError):
                pass
            time.sleep(2)
        raise TimeoutError("emulator boot timed out")

    def stop(self) -> None:
        if self.serial:
            try:
                self._adb("emu", "kill", timeout=15, check=False)
            except (subprocess.SubprocessError, OSError):
                pass
        if self.process is not None:
            try:
                self.process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                self.process.kill()
            self.process = None
        shutil.rmtree(self.work_dir, ignore_errors=True)

    # ------------------------------------------------------------ queries

    def launchable_packages(self) -> list[str]:
        """Packages that expose a launcher entry — i.e. apps a user can open."""
        result = self._adb("shell", "cmd", "package", "query-activities",
                           "-a", "android.intent.action.MAIN",
                           "-c", "android.intent.category.LAUNCHER",
                           timeout=90, check=False)
        # ResolveInfo dumps carry an explicit `packageName=` field; parsing the
        # component strings instead would also match APK paths and class names.
        packages = {match.group(1) for match in
                    re.finditer(r"packageName=(\S+)", result.stdout or "")}
        if not packages:  # fall back to the plain system package list
            listing = self._adb("shell", "pm", "list", "packages", "-s",
                                timeout=60, check=False)
            packages = {line.split(":", 1)[1].strip()
                        for line in (listing.stdout or "").splitlines()
                        if line.startswith("package:")}
        return sorted(p for p in packages
                      if "." in p and not p.startswith(_SKIP_PREFIXES))

    def all_system_packages(self) -> list[str]:
        listing = self._adb("shell", "pm", "list", "packages", "-s",
                            timeout=60, check=False)
        return sorted(line.split(":", 1)[1].strip()
                      for line in (listing.stdout or "").splitlines()
                      if line.startswith("package:"))

    def apk_paths(self, package: str) -> list[str]:
        result = self._adb("shell", "pm", "path", package, timeout=30,
                           check=False)
        return [line.split(":", 1)[1].strip()
                for line in (result.stdout or "").splitlines()
                if line.startswith("package:")]

    def launch_activity(self, package: str) -> str:
        result = self._adb("shell", "cmd", "package", "resolve-activity",
                           "--brief", package, timeout=30, check=False)
        for line in reversed((result.stdout or "").splitlines()):
            line = line.strip()
            if "/" in line and line.startswith(package):
                activity = line.split("/", 1)[1]
                return activity
        return ""

    def version_name(self, package: str) -> str:
        result = self._adb("shell", "dumpsys", "package", package,
                           timeout=30, check=False)
        match = re.search(r"versionName=(\S+)", result.stdout or "")
        return match.group(1) if match else ""

    def pull(self, remote: str, local: Path) -> None:
        local.parent.mkdir(parents=True, exist_ok=True)
        self._adb("pull", remote, str(local), timeout=180)
        if not local.is_file() or local.stat().st_size == 0:
            raise RuntimeError(f"failed to pull {remote}")


def _resolve_package(emulator: _TempEmulator, match: str,
                     explicit: str) -> str:
    packages = emulator.launchable_packages()
    if explicit:
        if explicit not in packages:
            print(f"[extract] warning: {explicit} is not in the launchable list")
        return explicit
    needle = match.lower()
    hits = [p for p in packages if needle in p.lower()]
    if not hits:
        raise SystemExit(
            f"no launchable package matches {match!r}. Available:\n  " +
            "\n  ".join(packages))
    if len(hits) > 1:
        # Prefer the shortest, most specific name (calculator2 over ...calculator.tests)
        hits.sort(key=len)
        print(f"[extract] multiple matches {hits}; using {hits[0]}")
    return hits[0]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--list", action="store_true",
                        help="only list launchable system packages")
    parser.add_argument("--id", help="target id to register")
    parser.add_argument("--match", default="calculator",
                        help="substring used to find the package")
    parser.add_argument("--package", default="",
                        help="exact package name (skips matching)")
    parser.add_argument("--description", default="")
    parser.add_argument("--brief", default="")
    parser.add_argument("--meta-json", type=Path,
                        help="JSON file with description/brief; avoids shell "
                             "encoding damage for non-ASCII text on Windows")
    parser.add_argument("--orientation",
                        choices=["auto", "portrait", "landscape"],
                        default="portrait")
    parser.add_argument("--replace", action="store_true")
    args = parser.parse_args()

    if not args.list and not args.id:
        raise SystemExit("--id is required unless --list is used")

    description = args.description
    brief = args.brief
    if args.meta_json:
        meta = json.loads(Path(args.meta_json).read_text(encoding="utf-8"))
        description = meta.get("description", description)
        brief = meta.get("brief", brief)

    toolchain = AndroidToolchain.discover()
    work = config.ANDROID_TARGETS_DIR / "extract_work"
    shutil.rmtree(work, ignore_errors=True)
    emulator = _TempEmulator(toolchain, work)
    try:
        emulator.start()
        if args.list:
            print("[extract] launchable apps:")
            for package in emulator.launchable_packages():
                print("   ", package)
            print()
            print("[extract] all system packages:")
            for package in emulator.all_system_packages():
                print("   ", package)
            return

        package = _resolve_package(emulator, args.match, args.package)
        paths = emulator.apk_paths(package)
        if not paths:
            raise SystemExit(f"could not resolve an APK path for {package}")
        if len(paths) > 1:
            raise SystemExit(
                f"{package} is a split APK ({len(paths)} parts); pick a "
                "single-APK app or extend this script for splits")
        activity = emulator.launch_activity(package)
        version = emulator.version_name(package)
        print(f"[extract] package={package} activity={activity or '(unresolved)'} "
              f"version={version or 'unknown'}")

        apk_dir = config.ANDROID_TARGETS_DIR / "apks"
        apk = apk_dir / f"{args.id}.apk"
        emulator.pull(paths[0], apk)
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        print(f"[extract] pulled {apk.name} "
              f"({apk.stat().st_size / 1024:.0f} KiB, sha256={digest[:16]}…)")
    finally:
        emulator.stop()

    spec = register_android_target(
        target_id=args.id, apk=apk,
        description=description or f"System app {package}",
        brief=brief, package_name=package,
        launch_activity=activity, orientation=args.orientation,
        reset_strategy="clear_data", network_policy="offline",
        replace=args.replace)
    print(f"[extract] registered target: {spec.app_id} "
          f"(package={spec.package_name}, activity={spec.launch_activity})")
    print("[extract] next: vendor/python/python.exe scripts/android_manual_session.py "
          f"--app {spec.app_id}")


if __name__ == "__main__":
    main()
