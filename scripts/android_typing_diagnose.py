"""Diagnose what the emulator really accepts from `adb shell input text`.

`input text` maps characters through the device KeyCharacterMap, which has no
CJK entries, so a Chinese argument may be impossible rather than merely
badly quoted. Distinguishing "our escaping is wrong" from "the device cannot do
this" decides whether a Chinese-locale dataset needs a trusted-side IME.

Trusted side only: this drives adb directly and reports nothing to an Agent.

    vendor/python/python.exe scripts/android_typing_diagnose.py --app google_clock
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
    parser.add_argument("--app", default="google_clock")
    args = parser.parse_args()

    spec = get_android_target(args.app)
    work = config.ANDROID_TARGETS_DIR / "typing_diagnose" / args.app
    work.mkdir(parents=True, exist_ok=True)
    runtime = AndroidEmulatorRuntime(spec, work / "runtime")
    try:
        print("[diag] booting emulator …")
        runtime.start()
        print(f"[diag] serial          : {runtime.serial}")

        checks = [
            ("ascii direct", ("shell", "input", "text", "abc")),
            ("cjk direct", ("shell", "input", "text", "北京")),
            ("cjk printf escape",
             ("shell", "input text \"$(printf '\\xe5\\x8c\\x97')\"")),
            ("printf alone works",
             ("shell", "printf '\\xe5\\x8c\\x97' | wc -c")),
            ("ime list", ("shell", "ime", "list", "-s")),
        ]
        for label, command in checks:
            result = runtime._run(*command, check=False, retries=0)
            out = (result.stdout or "").strip()
            err = (getattr(result, "stderr", "") or "").strip()
            print(f"[diag] {label:<20}: exit={result.returncode} "
                  f"out={out[:120]!r} err={err[:160]!r}")
    finally:
        print("[diag] stopping emulator …")
        runtime.stop()


if __name__ == "__main__":
    main()
