#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Declare seed_files on every registered target that has android_seeds/.

Idempotent: re-running refreshes the declarations. The runtime reads them
on the NEXT controller/MCP start (running processes keep old code).
"""
import json
from pathlib import Path

from benchmark import config

REG = config.ANDROID_TARGETS_DIR / "targets.json"

# app_id -> [(src relative to android_seeds/<app_id>/, device dst)]
DECLS = {
    "vinyl":          [(".", "/sdcard/Music")],
    "fossify_gallery": [(".", "/sdcard/DCIM/Camera")],
    "snapseed":       [(".", "/sdcard/Pictures/Snapseed")],
    "vlc":            [(".", "/sdcard/Media")],
    "antennapod":     [(".", "/sdcard/Download/BlackBoxBench/AntennaPod")],
    "markor":         [(".", "/sdcard/Documents")],
    "librera":        [(".", "/sdcard/Books")],
    "mj_pdf":         [(".", "/sdcard/Download/BlackBoxBench/PDF")],
    "material_files": [(".", "/sdcard/SeedFiles")],
    "ankidroid":      [(".", "/sdcard/Download/BlackBoxBench/Anki")],
    "feeder":         [(".", "/sdcard/Download/BlackBoxBench/Feeder")],
}


def main() -> None:
    data = json.loads(REG.read_text(encoding="utf-8"))
    seeds_root = config.PROJECT_ROOT / "android_seeds"
    changed = 0
    for app_id, pairs in DECLS.items():
        if app_id not in data:
            print(f"  [skip] {app_id}: not registered")
            continue
        if not (seeds_root / app_id).is_dir():
            print(f"  [skip] {app_id}: no seeds dir")
            continue
        data[app_id]["seed_files"] = [
            {"src": src, "dst": dst} for src, dst in pairs]
        changed += 1
        print(f"  [ok] {app_id}: {len(pairs)} push rule(s)")
    REG.write_text(json.dumps(data, ensure_ascii=False, indent=2),
                   encoding="utf-8")
    print(f"\n{changed} targets updated -> {REG}")


if __name__ == "__main__":
    main()
