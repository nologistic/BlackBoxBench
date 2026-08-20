"""Render the overlay video for an existing (closed) session.

Usage: vendor/python/python.exe scripts/render_video.py runs/<session_id>
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from benchmark.recorder.trace import TraceRecorder
from benchmark.recorder.video import render_video


def main() -> None:
    session_dir = Path(sys.argv[1])
    rec = TraceRecorder(session_dir)
    try:
        out = render_video(session_dir, rec)
    finally:
        rec.close()
    print(out if out else "video render skipped/failed (frames still usable)")


if __name__ == "__main__":
    main()
