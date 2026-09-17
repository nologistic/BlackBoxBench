#!/usr/bin/env python3
"""Sync skill sources (repo) into the opencode runtime skills directory.

The runtime reads ~/.config/opencode/skills/<name>/SKILL.md, but the sources
of truth live in this repo. Editing only one side silently splits behaviour
(2026-09-16: three fixes sat in the repo while the runtime kept serving the
old skill, and consumers believed they were live). Run this after any skill
edit; --check only reports drift without writing.

Only SKILL.md is copied. Subdirectories shipped next to a skill (e.g.
agents/) are left alone, and skills that are not deployed are skipped unless
--all is given (our-method / self-built are intentionally not deployed).
"""
from __future__ import annotations

import argparse
import filecmp
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = Path.home() / ".config" / "opencode" / "skills"
PAIRS = [
    ("agents/cli_explorer/skill/blackbox-explorer", "blackbox-explorer"),
    ("agents/android_baseline/skill/android-blackbox-explorer",
     "android-blackbox-explorer"),
    ("agents/our_method/skill/our-method", "our-method"),
    ("agents/android_our_method/skill/android-our-method",
     "android-our-method"),
    ("self_explorer/skill/self-built-explorer", "self-built-explorer"),
    ("agents/app_review/skill/app-review", "app-review"),
    ("agents/web_review/skill/web-review", "web-review"),
]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true",
                        help="report drift without writing anything")
    parser.add_argument("--all", action="store_true",
                        help="also deploy skills whose runtime dir is absent")
    args = parser.parse_args(argv)
    drift = 0
    synced = 0
    for source_rel, name in PAIRS:
        source = ROOT / source_rel / "SKILL.md"
        target = TARGET / name / "SKILL.md"
        if not source.is_file():
            print(f"  ?  源缺失: {source_rel}/SKILL.md")
            continue
        if not target.parent.is_dir() and not args.all:
            print(f"  -  未部署（--all 可补齐）: {name}")
            continue
        if target.is_file() and filecmp.cmp(source, target, shallow=False):
            print(f"  ✓  一致: {name}")
            continue
        drift += 1
        if args.check:
            print(f"  ⚠  漂移: {name}（源与部署不一致，需同步）")
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        print(f"  →  已同步: {name}")
        synced += 1
    print()
    if args.check:
        if drift:
            print(f"漂移 {drift} 处——执行（不带 --check）即可修复")
            return 1
        print("全部一致 ✓")
        return 0
    print(f"同步完成：{synced} 处更新（漂移 {drift} 处）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
