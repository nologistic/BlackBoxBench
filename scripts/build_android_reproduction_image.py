"""Prepare the pinned offline Android reproduction image.

The flag is intentionally mandatory: building the image downloads Android SDK
components and accepts their licenses. Normal benchmark runs never perform this
networked preparation step.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from app_reproduction.workspace import build_android_build_image


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build the pinned offline Android reproduction image")
    parser.add_argument(
        "--accept-licenses", action="store_true",
        help="confirm acceptance of Android SDK licenses")
    args = parser.parse_args()
    if not args.accept_licenses:
        raise SystemExit(
            "Refusing to download the Android SDK without --accept-licenses. "
            "Review the Android SDK licenses, then rerun with that flag.")
    build_android_build_image(accept_licenses=True)
    print("Android reproduction image is ready.")


if __name__ == "__main__":
    main()
