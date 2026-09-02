"""Operator diagnostic: why did an Android session fail?

Correlates crash reports with host resource pressure and emulator lifetime, so
an operator can tell an Agent mistake apart from an environment problem.

    vendor/python/python.exe scripts/android_diagnose.py
    vendor/python/python.exe scripts/android_diagnose.py --session sess_...
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config

# Failures that mean "the emulator went away", not "the Agent did something bad".
_ENVIRONMENT_SIGNS = (
    ("screencap", "emulator stopped answering screen capture"),
    ("4294967295", "adb lost the device (exit -1)"),
    ("device offline", "adb reported the device offline"),
    ("device not found", "adb could not find the device"),
    ("am start", "the App could not be relaunched"),
    ("timed out", "an adb command timed out"),
)


def _memory() -> tuple[float, float] | None:
    try:
        out = subprocess.run(
            ["wmic", "OS", "get",
             "TotalVisibleMemorySize,FreePhysicalMemory", "/value"],
            capture_output=True, text=True, timeout=30).stdout
    except (OSError, subprocess.SubprocessError):
        return None
    values = dict(re.findall(r"(\w+)=(\d+)", out))
    if "TotalVisibleMemorySize" not in values:
        return None
    return (int(values["TotalVisibleMemorySize"]) / 1048576,
            int(values["FreePhysicalMemory"]) / 1048576)


def _processes() -> list[tuple[str, str, str]]:
    try:
        out = subprocess.run(["tasklist", "/FO", "CSV", "/NH"],
                             capture_output=True, text=True, timeout=30).stdout
    except (OSError, subprocess.SubprocessError):
        return []
    found = []
    for line in out.splitlines():
        lowered = line.lower()
        if any(token in lowered for token in ("emulator", "qemu", "adb")):
            fields = [field.strip('"') for field in line.split('","')]
            if len(fields) >= 5:
                found.append((fields[1], fields[0], fields[4]))
    return found


def _classify(reason: str) -> tuple[str, str]:
    lowered = (reason or "").lower()
    for token, explanation in _ENVIRONMENT_SIGNS:
        if token in lowered:
            return "ENVIRONMENT", explanation
    if "input text" in lowered:
        return "INPUT", "non-ASCII text was rejected by the guest"
    return "OTHER", "see the reason text"


def _sessions(only: str | None) -> list[Path]:
    runs = config.RUNS_DIR
    if only:
        return [runs / only]
    return sorted(child for child in runs.iterdir()
                  if child.is_dir() and child.name.startswith("sess_"))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--session")
    args = parser.parse_args()

    memory = _memory()
    if memory:
        total, free = memory
        print(f"host RAM     : {total:.1f} GB total, {free:.1f} GB free")
        if free < 3.0:
            print("               ^ low: a second emulator can be killed by "
                  "the OS under this pressure")
    processes = _processes()
    print(f"emulator procs: {len(processes)}")
    for name, pid, mem in processes:
        print(f"   {pid:>7}  {name:<34} {mem}")
    print()

    for directory in _sessions(args.session):
        meta_path = directory / "session.json"
        if not meta_path.is_file():
            continue
        meta = json.loads(meta_path.read_text(encoding="utf-8"))
        if meta.get("status") != "failed":
            continue
        reason = str(meta.get("close_reason") or "")
        kind, explanation = _classify(reason)
        actions = directory / "actions.jsonl"
        steps = (len(actions.read_text(encoding="utf-8").splitlines())
                 if actions.is_file() else 0)
        serial = re.search(r"emulator-\d+", reason)
        print(f"{directory.name}")
        print(f"   verdict : {kind} — {explanation}")
        print(f"   steps   : {steps}")
        if serial:
            print(f"   device  : {serial.group(0)}")
        print(f"   reason  : {reason[:160]}")
        print()


if __name__ == "__main__":
    main()
