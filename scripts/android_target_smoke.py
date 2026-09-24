"""Operator smoke check: install a registered target and exercise it headlessly.

    vendor/python/python.exe scripts/android_target_smoke.py --app google_clock

Boots a dedicated emulator clone, installs the registered APK, captures the
launch screen, sends one coordinate tap, restarts the App, and saves the frames
so an operator can confirm a target is usable before manual exploration.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android.targets import get_android_target


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", required=True)
    args = parser.parse_args()

    spec = get_android_target(args.app)
    out = config.ANDROID_TARGETS_DIR / "smoke" / args.app
    out.mkdir(parents=True, exist_ok=True)
    runtime = AndroidEmulatorRuntime(spec, out / "runtime")
    try:
        print(f"[smoke] booting emulator for {spec.app_id} …")
        runtime.start()
        info = runtime.info()
        first = runtime.screenshot()
        (out / "01_launch.png").write_bytes(first)
        print(f"[smoke] launch frame: {info.width}x{info.height}, "
              f"{len(first)} bytes")

        runtime.tap(info.width // 2, int(info.height * 0.55))
        second = runtime.screenshot()
        (out / "02_after_tap.png").write_bytes(second)
        print(f"[smoke] tap accepted, pixels changed: {first != second}")

        runtime.restart_app()
        third = runtime.screenshot()
        (out / "03_after_restart.png").write_bytes(third)
        print("[smoke] restart_app succeeded")
        print(f"[smoke] frames saved under {out}")
    finally:
        runtime.stop()
        print("[smoke] emulator stopped")


if __name__ == "__main__":
    main()
