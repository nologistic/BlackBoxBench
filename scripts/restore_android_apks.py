# -*- coding: utf-8 -*-
"""Restore dataset APKs from android_apks/ into the runtime location.

Copies each <app_id>.apk to runs/android_targets/artifacts/<app_id>/<sha256>.apk
(the layout benchmark/android targets expect) and verifies the checksum.

Joplin is shipped as two split parts (GitHub 100MB limit); they are merged
into memory before verification.

Usage (from the repo root):
    vendor/python/python.exe scripts/restore_android_apks.py
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
SRC = REPO / "android_apks"
DEST = REPO / "runs" / "android_targets" / "artifacts"


def load_expected() -> dict[str, str]:
    """Parse SHA256SUMS.txt -> {app_id: sha256}.

    Regular lines: '<sha>  <app_id>.apk'. Split-part entries are written as
    comment lines ending with '(after merge)'; those are parsed too so the
    merged blob is verified against the original hash.
    """
    expected: dict[str, str] = {}
    for line in (SRC / "SHA256SUMS.txt").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        if line.startswith("#"):
            if "(after merge)" in line:
                body = line.lstrip("# ").removesuffix("(after merge)").strip()
                sha, name = body.split(None, 1)
                expected[name.strip().removesuffix(".apk")] = sha
            continue
        sha, name = line.split(None, 1)
        expected[name.strip().removesuffix(".apk")] = sha
    return expected


def blob_for(app_id: str) -> bytes | None:
    apk = SRC / f"{app_id}.apk"
    if apk.is_file():
        return apk.read_bytes()
    parts = [SRC / f"{app_id}.apk.part-aa", SRC / f"{app_id}.apk.part-ab"]
    if all(p.is_file() for p in parts):
        return b"".join(p.read_bytes() for p in parts)
    return None


def main() -> int:
    expected = load_expected()
    ok = failed = 0
    for app_id, sha in sorted(expected.items()):
        data = blob_for(app_id)
        if data is None:
            print(f"  MISSING {app_id}")
            failed += 1
            continue
        actual = hashlib.sha256(data).hexdigest()
        if actual != sha:
            print(f"  BAD HASH {app_id}: expected {sha[:12]}… got {actual[:12]}…")
            failed += 1
            continue
        target = DEST / app_id / f"{actual}.apk"
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.is_file():
            target.write_bytes(data)
        print(f"  ok {app_id:<20} -> {target.relative_to(REPO)}")
        ok += 1
    print(f"\n{ok} restored, {failed} failed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
