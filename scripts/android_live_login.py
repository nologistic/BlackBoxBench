"""Trusted, headed login maintenance for a registered Android target."""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android.targets import get_android_target, register_android_target


def main() -> None:
    parser = argparse.ArgumentParser(description="Maintain an Android target login snapshot")
    parser.add_argument("--app", required=True)
    args = parser.parse_args()
    spec = get_android_target(args.app)
    work = config.ANDROID_TARGETS_DIR / "login_work" / args.app
    if work.exists():
        resolved = work.resolve()
        expected = (config.ANDROID_TARGETS_DIR / "login_work").resolve()
        resolved.relative_to(expected)
        shutil.rmtree(resolved)
    runtime = AndroidEmulatorRuntime(
        spec, work, headed=True, lease_mode="login")
    try:
        runtime.start()
        print("A trusted Android emulator window is open.")
        print("Complete login manually, return here, then press Enter to capture the profile.")
        input()
    finally:
        runtime.stop()
    clones = list((work / "avd_home").glob("*.avd"))
    if len(clones) != 1:
        raise RuntimeError("login emulator profile clone was not found")
    register_android_target(
        target_id=spec.app_id, apk=spec.apk, description=spec.description,
        brief=spec.brief, package_name=spec.package_name,
        launch_activity=spec.launch_activity, orientation=spec.orientation,
        reset_strategy="snapshot", network_policy=spec.network_policy,
        profile_snapshot=clones[0], protected_strings=spec.protected_strings,
        protected_regions=spec.protected_regions, replace=True)
    print(f"Golden login snapshot saved for {spec.app_id}.")


if __name__ == "__main__":
    main()
