#!/usr/bin/env python3
"""Batch login precheck for live web targets.

Runs `live_login.py --app <id> --check` for every registered web target (or
an explicit subset) and prints a summary. Run this BEFORE launching a batch:
a stale server-side session looks exactly like a valid cookie file, and a
full exploration round can be lost to a login page (2026-09-17: todoist).

Never run while a benchmark session of the same target is active - the
browser profile is a singleton shared with the live runtime.
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))


def live_target_ids() -> list[str]:
    from benchmark.orchestrator.apps import list_apps
    ids: list[str] = []
    for app in list_apps():
        record = app if isinstance(app, dict) else {}
        app_id = str(record.get("app_id") or "")
        platform = str(record.get("platform") or "")
        if app_id and platform == "web":
            ids.append(app_id)
    return ids


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", action="append", default=[],
                        help="check only these app ids (repeatable)")
    parser.add_argument("--timeout", type=float, default=240.0,
                        help="per-target timeout in seconds (default 240)")
    args = parser.parse_args(argv)
    targets = args.app or live_target_ids()
    if not targets:
        print("没有可检查的目标（--app 或注册表为空）")
        return 1
    bad: list[tuple[str, str]] = []
    skipped: list[str] = []
    for app_id in targets:
        try:
            proc = subprocess.run(
                [sys.executable, str(ROOT / "scripts" / "live_login.py"),
                 "--app", app_id, "--check"],
                cwd=str(ROOT), capture_output=True, text=True,
                timeout=args.timeout)
            output = (proc.stdout or "") + (proc.stderr or "")
            tail = [line for line in output.strip().splitlines() if line.strip()]
            message = tail[-1] if tail else f"exit={proc.returncode}"
            ok = proc.returncode == 0
        except subprocess.TimeoutExpired:
            ok, message = False, f"检查超时（>{args.timeout:.0f}s）"
        except Exception as exc:  # pragma: no cover - defensive
            ok, message = False, f"{type(exc).__name__}: {exc}"
        if "未注册 precheck" in message:
            print(f"  [SKIP] {app_id}（未注册 precheck，无法自动检查）")
            skipped.append(app_id)
        elif ok:
            print(f"  [OK ] {app_id}")
        else:
            print(f"  [BAD] {app_id} :: {message[:140]}")
            bad.append((app_id, message))
    print()
    print(f"登录体检: {len(targets) - len(bad) - len(skipped)}/{len(targets)} 通过"
          f"（跳过 {len(skipped)}）")
    if skipped:
        print("未注册 precheck（需先 capture 校准才能自动检查）:")
        for app_id in skipped:
            print(f"  - {app_id}")
    if bad:
        print("未通过（先人工登录再跑批）:")
        for app_id, _ in bad:
            print(f"  - {app_id}")
        print("修复命令: python3 scripts/live_login.py --app <id> --capture")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
