#!/usr/bin/env python3
"""Sweep leftover benchmark sandbox containers (cron / manual hygiene).

Wraps reproduction.workspace.prune_stale_containers: removes created/exited
benchmark sandboxes unconditionally and running ones older than
--max-running-hours (default 24, a healthy reproduction finishes within
hours). Freshly started containers (concurrent reproductions) and every
host artifact (app_output/, website_output/, runs/) are never touched.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from reproduction.workspace import prune_stale_containers  # noqa: E402

SANDBOX_PREFIXES = ("bbb-app-repro-", "bbb-repro-")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--max-running-hours", type=float, default=24.0,
        help="also remove running sandboxes older than this many hours "
             "(default: 24)")
    args = parser.parse_args(argv)
    removed: list[str] = []
    for prefix in SANDBOX_PREFIXES:
        removed.extend(prune_stale_containers(
            prefix, max_running_hours=args.max_running_hours))
    if removed:
        print(f"removed {len(removed)} stale sandbox container(s):")
        for container_id in removed:
            print(f"  {container_id}")
    else:
        print("no stale sandbox containers found")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
