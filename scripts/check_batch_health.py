#!/usr/bin/env python3
"""Detect interrupted explorations / reproductions / reviews in outputs.

An opencode session can die mid-review with nobody noticing (2026-09-16:
a tasks review stopped after round_01, no summary, no alert). This scanner
flags artifacts whose run appears to have stopped mid-flight:

  app_output/<handoff>/      review rounds without review_summary.json
  */evaluations/<eval_id>/   frames but no evaluation_report.json
  website_output/<handoff>/  web review rounds without summary
  runs/sess_*/session.json   status not closed/failed while stale

Exit code 1 when any interruption is found, so batch scripts can gate on it.
Read-only: never modifies any artifact.
"""
from __future__ import annotations

import argparse
import json
import os
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def newest_mtime(directory: Path) -> float:
    newest = 0.0
    try:
        entries = directory.rglob("*")
    except OSError:
        return newest
    for path in entries:
        try:
            newest = max(newest, path.stat().st_mtime)
        except OSError:
            continue
    return newest


def _stale(mtime: float, now: float, stale: float) -> bool:
    return bool(mtime) and now - mtime > stale


def scan_evaluations(root: Path, label: str, stale: float,
                     now: float, findings: list[str]) -> None:
    if not root.is_dir():
        return
    for item in sorted(root.iterdir()):
        if not item.is_dir() or (item / "evaluation_report.json").is_file():
            continue
        if not (item / "frames").is_dir():
            continue
        mtime = newest_mtime(item)
        if _stale(mtime, now, stale):
            findings.append(
                f"{label}评测中断: {item.name}"
                f"（有 frames 无 evaluation_report.json，"
                f"停滞 {int((now - mtime) / 60)} 分钟）")


def scan_reproductions(root: Path, label: str, stale: float,
                       now: float, findings: list[str]) -> None:
    if not root.is_dir():
        return
    for handoff in sorted(root.iterdir()):
        if not handoff.is_dir() or handoff.name == "evaluations":
            continue
        review = handoff / "review"
        if not review.is_dir():
            continue
        if not any(review.glob("round_*")):
            continue
        if (review / "review_summary.json").is_file():
            continue
        mtime = newest_mtime(review)
        if _stale(mtime, now, stale):
            findings.append(
                f"{label}复现评审中断: {handoff.name}"
                f"（round_* 存在但无 review_summary.json，"
                f"停滞 {int((now - mtime) / 60)} 分钟）")


def scan_sessions(runs: Path, stale: float, now: float,
                  findings: list[str]) -> None:
    if not runs.is_dir():
        return
    for sess in sorted(runs.glob("sess_*")):
        meta = sess / "session.json"
        if not meta.is_file():
            continue
        try:
            data = json.loads(meta.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        status = str(data.get("status") or "")
        if status.lower() in ("closed", "failed", ""):
            continue
        mtime = newest_mtime(sess)
        if _stale(mtime, now, stale):
            findings.append(
                f"会话疑似挂起: {sess.name}（status={status}，"
                f"停滞 {int((now - mtime) / 60)} 分钟）")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=str(ROOT),
                        help="project root (default: repo root)")
    parser.add_argument("--runs", action="append", default=[],
                        help="extra runs dirs to scan (repeatable; "
                             "BBB_RUNS_DIR is always included when set)")
    parser.add_argument("--stale-minutes", type=float, default=30.0,
                        help="report only items idle longer than this "
                             "(default 30)")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    now = time.time()
    stale = args.stale_minutes * 60.0
    findings: list[str] = []

    scan_reproductions(root / "app_output", "android ", stale, now, findings)
    scan_evaluations(root / "app_output" / "evaluations", "app ", stale,
                     now, findings)
    scan_reproductions(root / "website_output", "web ", stale, now, findings)
    scan_evaluations(root / "website_output" / "evaluations", "web ", stale,
                     now, findings)

    runs_dirs = [root / "runs"]
    extra = os.environ.get("BBB_RUNS_DIR", "").strip()
    if extra:
        runs_dirs.append(Path(extra))
    runs_dirs.extend(Path(value) for value in args.runs)
    seen: set[Path] = set()
    for runs in runs_dirs:
        resolved = runs.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        scan_sessions(resolved, stale, now, findings)

    if findings:
        print(f"发现 {len(findings)} 处疑似中断：")
        for item in findings:
            print(f"  - {item}")
        print()
        print("处理建议：确认对应进程已退出后，重跑相应环节"
              "（探索 /app-review 或 /web-review 重跑）。")
        return 1
    print("批次健康：未发现中断迹象 ✓")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
