"""Operator smoke test for the trusted Android pixels/touch runtime."""
from __future__ import annotations

import argparse
import json
import shutil
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android.targets import get_android_target


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--app", default="android_commerce_demo")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    output = args.output.expanduser().resolve()
    output.mkdir(parents=True, exist_ok=True)
    work_dir = Path(tempfile.mkdtemp(prefix="bbb-android-runtime-smoke-"))
    runtime = AndroidEmulatorRuntime(get_android_target(args.app), work_dir)
    try:
        runtime.start()
        initial = runtime.screenshot()
        info = runtime.info()
        (output / "initial.png").write_bytes(initial)
        # Exercise only the public coordinate surface, then prove the trusted
        # runtime can restore and recapture the target App.
        runtime.tap(info.width // 2, info.height // 2)
        runtime.restart_app()
        restarted = runtime.screenshot()
        (output / "restarted.png").write_bytes(restarted)
        if not runtime.health():
            raise RuntimeError("emulator became unhealthy during smoke test")
        print(json.dumps({
            "app_id": args.app,
            "healthy": True,
            "width": info.width,
            "height": info.height,
            "orientation": info.orientation,
            "density_dpi": info.density_dpi,
            "initial_frame": str(output / "initial.png"),
            "restarted_frame": str(output / "restarted.png"),
        }, ensure_ascii=False))
    finally:
        runtime.stop()
        shutil.rmtree(work_dir, ignore_errors=True)


if __name__ == "__main__":
    main()
