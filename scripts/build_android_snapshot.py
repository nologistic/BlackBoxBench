#!/usr/bin/env python3
"""Bake the Android emulator base snapshot into the AVD template.

One cold boot here (5-7 minutes) saves every later session the same cost:
each session clones the template, restores the snapshot in seconds, and —
because the snapshot already pins the guest locale — skips the
framework-restart half of locale pinning too.

Run once after the AVD template is created or upgraded:

    python scripts/build_android_snapshot.py

Writes ``<template>.avd/snapshots/<BBB_ANDROID_SNAPSHOT>/`` in place. The
runtime auto-detects the snapshot per clone and falls back to a cold boot
when it is absent, so running this script is always optional — never
load-bearing.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from benchmark import config
from benchmark.android.toolchain import AndroidToolchain

AVD_NAME = "BBB_Snapshot"
SNAPSHOT = config.ANDROID_SNAPSHOT_NAME
PORT = int(os.environ.get("BBB_SNAPSHOT_BUILD_PORT", "5584"))
BOOT_TIMEOUT_S = int(os.environ.get("BBB_SNAPSHOT_BOOT_TIMEOUT_S", "600"))


def log(msg: str) -> None:
    print(f"[snapshot] {msg}", flush=True)


def adb(tc: AndroidToolchain, serial: str, *args: str,
        timeout: int = 60, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run([str(tc.adb), "-s", serial, *args],
                          capture_output=True, text=True, timeout=timeout,
                          check=check)


def main() -> int:
    if not SNAPSHOT:
        log("BBB_ANDROID_SNAPSHOT is empty — nothing to bake")
        return 1
    tc = AndroidToolchain.discover()
    tc.require()
    log(f"template: {tc.avd_template}")
    log(f"snapshot name: {SNAPSHOT}  port: {PORT}")
    if not tc.avd_template.is_dir():
        log(f"AVD template missing: {tc.avd_template}")
        return 1

    tmp = Path(tempfile.mkdtemp(prefix="bbb_snapshot_build_"))
    avd = tmp / f"{AVD_NAME}.avd"
    serial = f"emulator-{PORT}"
    process: subprocess.Popen | None = None
    try:
        shutil.copytree(tc.avd_template, avd)
        # Re-bake from a clean state: drop any previously cloned snapshot.
        shutil.rmtree(avd / "snapshots", ignore_errors=True)
        # The template ships a Windows-authored config.ini; normalize like
        # the runtime's per-session clone does.
        config_ini = avd / "config.ini"
        if config_ini.is_file():
            text = config_ini.read_text(encoding="utf-8")
            fixed = re.sub(r"^(image\.sysdir\.1=.*)$",
                           lambda m: m.group(1).replace("\\", "/"),
                           text, flags=re.MULTILINE)
            if fixed != text:
                config_ini.write_text(fixed, encoding="utf-8")
        (tmp / f"{AVD_NAME}.ini").write_text(
            f"avd.ini.encoding=UTF-8\npath={avd}\ntarget=android-35\n",
            encoding="utf-8")

        command = [str(tc.emulator), "-avd", AVD_NAME, "-port", str(PORT),
                   "-no-audio", "-no-boot-anim", "-no-snapshot-save",
                   "-gpu", "swiftshader_indirect", "-camera-back", "none",
                   "-camera-front", "none", "-no-window",
                   "-dns-server", "1.1.1.1,8.8.8.8"]
        log("booting template cold (this is the 5-7 minute part)…")
        process = subprocess.Popen(command, cwd=tmp,
                                   env=tc.environment(tmp),
                                   stdout=subprocess.DEVNULL,
                                   stderr=subprocess.DEVNULL)

        deadline = time.monotonic() + BOOT_TIMEOUT_S
        while time.monotonic() < deadline:
            if process.poll() is not None:
                log("emulator exited during boot")
                return 1
            try:
                result = adb(tc, serial, "shell", "getprop",
                             "sys.boot_completed", timeout=10, check=False)
                if result.returncode == 0 and result.stdout.strip() == "1":
                    break
            except (subprocess.SubprocessError, OSError):
                pass
            time.sleep(3)
        else:
            log(f"boot timed out after {BOOT_TIMEOUT_S}s")
            return 1
        log("boot completed; pinning locale before the snapshot")

        locale = str(getattr(config, "ANDROID_LOCALE", "") or "").strip()
        if locale:
            adb(tc, serial, "root", check=False, timeout=60)
            time.sleep(3)
            current = adb(tc, serial, "shell", "getprop", "persist.sys.locale",
                          check=False).stdout.strip()
            if current != locale:
                result = adb(tc, serial, "shell", "setprop",
                             "persist.sys.locale", locale, check=False)
                if result.returncode != 0:
                    log(f"warning: could not pin locale ({locale}); "
                        f"sessions will apply it themselves")
                else:
                    adb(tc, serial, "shell", "settings", "put", "system",
                        "system_locales", locale, check=False)
                    # Restart the framework so system UI strings re-inflate
                    # before the snapshot is taken.
                    adb(tc, serial, "shell", "stop", check=False, timeout=60)
                    adb(tc, serial, "shell", "start", check=False, timeout=60)
                    while time.monotonic() < deadline:
                        result = adb(tc, serial, "shell", "getprop",
                                     "sys.boot_completed", timeout=10,
                                     check=False)
                        if result.stdout.strip() == "1":
                            break
                        time.sleep(3)

        log(f"saving snapshot '{SNAPSHOT}'…")
        result = adb(tc, serial, "emu", "avd", "snapshot", "save", SNAPSHOT,
                     timeout=300, check=False)
        if "OK" not in (result.stdout + result.stderr).upper():
            log(f"snapshot save failed: {result.stdout} {result.stderr}")
            return 1
        log("snapshot saved; shutting the emulator down")
        adb(tc, serial, "emu", "kill", check=False, timeout=60)
        try:
            process.wait(timeout=60)
        except subprocess.TimeoutExpired:
            process.kill()

        baked = avd / "snapshots" / SNAPSHOT
        if not baked.is_dir():
            log(f"snapshot directory not found at {baked}")
            return 1
        # A snapshot is more than snapshots/<name>/: the disk state lives in
        # qcow2 internal snapshot layers inside the partition images
        # (userdata/cache/…). Copying only the snapshots/ directory produces
        # a template that fails to load with "Device 'cache' does not have
        # the requested snapshot". Replace the whole .avd tree (staged then
        # renamed, so the template is never half-written) and both halves
        # travel together.
        staging = tc.avd_template.parent / f".{tc.avd_template.name}.bake"
        shutil.rmtree(staging, ignore_errors=True)
        shutil.copytree(avd, staging)
        shutil.rmtree(tc.avd_template, ignore_errors=True)
        staging.rename(tc.avd_template)
        size_mb = sum(f.stat().st_size for f in tc.avd_template.rglob("*")
                      if f.is_file()) // (1024 * 1024)
        log(f"done: {tc.avd_template} now carries snapshot '{SNAPSHOT}' "
            f"({size_mb} MiB total) — sessions will quick-boot")
        return 0
    finally:
        if process is not None and process.poll() is None:
            try:
                adb(tc, serial, "emu", "kill", check=False, timeout=30)
            except Exception:
                pass
            try:
                process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                process.kill()
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
