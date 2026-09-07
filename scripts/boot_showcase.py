"""Boot the saved manual-showcase AVD (19 test apps, zh-CN, folder layout).

The showcase image lives outside scratch in runs/android_targets/
manual_show_saved/ so it survives reboots; this script reserves the
emulator port in the project-wide namespace, boots it headed and verifies
the installed application set. This is an operator convenience, NOT a
benchmark condition: nothing is recorded as evidence.

    vendor/python/python.exe scripts/boot_showcase.py
"""
from __future__ import annotations

import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark.android.toolchain import AndroidToolchain  # noqa: E402
from benchmark.android.runtime import port_locks_dir  # noqa: E402

SAVED = ROOT / "runs" / "android_targets" / "manual_show_saved"
APK_STORE = SAVED / "apks"
PORT = 5554
SERIAL = f"emulator-{PORT}"
# apk file -> package. A fast guest shutdown occasionally drops one freshly
# installed package from the qcow2; the boot check re-installs it from this
# local store so the showcase is always complete.
SHOWCASE_APKS = {
    "loop_habit_tracker.apk": "org.isoron.uhabits",
    "fossify_gallery.apk": "org.fossify.gallery",
    "fossify_calendar.apk": "org.fossify.calendar",
    "snapseed.apk": "com.niksoftware.snapseed",
    "cashew.apk": "com.budget.tracker_app",
    "antennapod.apk": "de.danoeh.antennapod",
    "organic_maps.apk": "app.organicmaps.web",
    "aegis.apk": "com.beemdevelopment.aegis",
    "ankidroid.apk": "com.ichi2.anki",
    "feeder.apk": "com.nononsenseapps.feeder",
    "fossify_paint.apk": "org.fossify.paint",
    "librera.apk": "com.foobnix.pro.pdf.reader",
    "vlc.apk": "org.videolan.vlc",
    "joplin.apk": "net.cozic.joplin",
    "tasks.apk": "org.tasks",
    "keepassdx.apk": "com.kunzisoft.keepass.libre",
    "material_files.apk": "me.zhanghai.android.files",
    "fossify_calculator.apk": "org.fossify.math",
    "vinyl.apk": "com.poupa.vinylmusicplayer",
    "termux.apk": "com.termux",
    "acode.apk": "com.foxdebug.acode",
}


def main() -> None:
    tc = AndroidToolchain.discover()
    tc.require()
    if not (SAVED / "bbb_manual_show.ini").is_file():
        raise SystemExit(f"saved AVD not found under {SAVED}")

    lock = port_locks_dir() / f"emulator-{PORT}.lock"
    port_locks_dir().mkdir(parents=True, exist_ok=True)
    fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    os.write(fd, str(os.getpid()).encode("ascii"))
    os.close(fd)

    env = tc.environment(SAVED)
    cmd = [str(tc.emulator), "-avd", "bbb_manual_show", "-port", str(PORT),
           "-no-audio", "-no-boot-anim", "-no-snapshot-save",
           "-gpu", "swiftshader_indirect", "-camera-back", "none",
           "-camera-front", "none", "-dns-server", "1.1.1.1,8.8.8.8"]
    proc = subprocess.Popen(cmd, env=env,
                            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    print(f"booting saved showcase AVD (emulator pid {proc.pid})...")

    adb = str(tc.adb)
    deadline = time.monotonic() + 240
    while time.monotonic() < deadline:
        r = subprocess.run([adb, "-s", SERIAL, "shell", "getprop",
                            "sys.boot_completed"], capture_output=True,
                           text=True, timeout=15)
        if r.returncode == 0 and r.stdout.strip() == "1":
            break
        time.sleep(3)
    else:
        proc.terminate()
        raise SystemExit("boot timed out")
    print("boot complete")

    out = subprocess.run([adb, "-s", SERIAL, "shell", "pm", "list",
                          "packages", "-3"], capture_output=True, text=True,
                         timeout=30).stdout
    packages = {l.split(":")[1] for l in out.splitlines() if l.strip()}
    missing = [(apk, pkg) for apk, pkg in SHOWCASE_APKS.items()
               if pkg not in packages]
    if missing:
        print(f"repairing {len(missing)} missing package(s)...")
        for apk, pkg in missing:
            source = APK_STORE / apk
            if not source.is_file():
                print(f"  {pkg}: repair apk missing from {APK_STORE}")
                continue
            r = subprocess.run([adb, "-s", SERIAL, "install", "-r", "-t",
                                str(source)], capture_output=True,
                               text=True, timeout=180)
            ok = "Success" in (r.stdout or "")
            print(f"  {pkg}: {'reinstalled' if ok else 'FAILED'}")
        subprocess.run([adb, "-s", SERIAL, "shell", "sync"],
                       capture_output=True, timeout=30)
        out = subprocess.run([adb, "-s", SERIAL, "shell", "pm", "list",
                              "packages", "-3"], capture_output=True,
                             text=True, timeout=30).stdout
        packages = {l.split(":")[1] for l in out.splitlines() if l.strip()}
    print(f"third-party packages: {len(packages)} "
          f"(expected {len(SHOWCASE_APKS)})")
    r = subprocess.run([adb, "-s", SERIAL, "shell", "pm", "path",
                        "com.google.android.deskclock"], capture_output=True,
                       text=True, timeout=15)
    print("google_clock installed:", bool(r.stdout.strip()))
    subprocess.run([adb, "-s", SERIAL, "shell",
                    "input keyevent KEYCODE_WAKEUP"], capture_output=True,
                   timeout=15)
    subprocess.run([adb, "-s", SERIAL, "shell",
                    "svc power stayon true"], capture_output=True,
                   timeout=15)
    print("screen awake and stay-on set; teardown later with:")
    print("  vendor/python/python.exe scripts/android_locks.py "
          "--reclaim --kill-emulators")


if __name__ == "__main__":
    main()
