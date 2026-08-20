#!/usr/bin/env python3
"""BlackBoxBench bootstrap: build a fully self-contained environment.

Downloads (into vendor/, platform-detected) and installs everything needed so
the benchmark runs without depending on machine-level installs:

  vendor/python/    python-build-standalone (Windows/Linux/macOS)
  vendor/chromium/  Chrome for Testing (pinned stable)
  pip deps          requirements.txt into the vendored Python

Usage:  python scripts/bootstrap.py            # any system Python 3.10+
"""
from __future__ import annotations

import json
import platform
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VENDOR = ROOT / "vendor"

PY_VERSION_TAG = "20260814"          # python-build-standalone release tag
PY_RELEASE = "3.12.14"               # CPython version inside that release

CHROME_VERSIONS_URL = ("https://googlechromelabs.github.io/chrome-for-testing/"
                       "last-known-good-versions-with-downloads.json")


def _platform_key() -> tuple[str, str]:
    sysname, machine = platform.system(), platform.machine().lower()
    arch = "aarch64" if machine in ("arm64", "aarch64") else "x86_64"
    if sysname == "Windows":
        return ("windows", f"x86_64-pc-windows-msvc")
    if sysname == "Linux":
        return ("linux", f"{arch}-unknown-linux-gnu")
    if sysname == "Darwin":
        return ("macos", f"{arch}-apple-darwin")
    raise RuntimeError(f"unsupported platform: {sysname}/{machine}")


def _download(url: str, dest: Path) -> None:
    print(f"  downloading {url}")
    with urllib.request.urlopen(url, timeout=300) as r, open(dest, "wb") as f:
        shutil.copyfileobj(r, f)


def ensure_python() -> Path:
    key, triple = _platform_key()
    exe_rel = "python.exe" if key == "windows" else "bin/python3"
    target = VENDOR / "python"
    exe = target / exe_rel
    if exe.exists():
        print(f"[python] already vendored: {exe}")
        return exe
    name = (f"cpython-{PY_RELEASE}+{PY_VERSION_TAG}-{triple}-install_only.tar.gz")
    url = ("https://github.com/astral-sh/python-build-standalone/releases/"
           f"download/{PY_VERSION_TAG}/{name}")
    VENDOR.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory() as td:
        arc = Path(td) / "py.tar.gz"
        _download(url, arc)
        target.mkdir(exist_ok=True)
        with tarfile.open(arc) as tf:
            tf.extractall(target, filter="data")
        # archive nests everything under python/
        nested = target / "python"
        if nested.exists():
            for item in nested.iterdir():
                shutil.move(str(item), target)
            nested.rmdir()
    if not exe.exists():
        raise RuntimeError(f"python extraction failed: {exe} missing")
    print(f"[python] vendored {exe}")
    return exe


def ensure_chromium() -> Path:
    key, _ = _platform_key()
    plat = {"windows": "win64", "linux": "linux64", "macos": "mac-arm64"}[key]
    if key == "macos" and platform.machine().lower() not in ("arm64", "aarch64"):
        plat = "mac-x64"
    exe_rel = {"windows": "chrome-win64/chrome.exe",
               "linux": "chrome-linux64/chrome",
               "macos": "chrome-mac-x64/Google Chrome for Testing.app/"
                        "Contents/MacOS/Google Chrome for Testing"}[key] \
        if not (key == "macos" and plat == "mac-arm64") else \
        "chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing"
    target = VENDOR / "chromium"
    exe = target / exe_rel
    if exe.exists():
        print(f"[chromium] already vendored: {exe}")
        return exe
    with urllib.request.urlopen(CHROME_VERSIONS_URL, timeout=60) as r:
        meta = json.loads(r.read())
    stable = meta["channels"]["Stable"]
    url = next(d["url"] for d in stable["downloads"]["chrome"]
               if d["platform"] == plat)
    print(f"[chromium] stable {stable['version']} for {plat}")
    with tempfile.TemporaryDirectory() as td:
        arc = Path(td) / "chrome.zip"
        _download(url, arc)
        target.mkdir(exist_ok=True)
        with zipfile.ZipFile(arc) as zf:
            zf.extractall(target)
    if not exe.exists():
        raise RuntimeError(f"chromium extraction failed: {exe} missing")
    exe.chmod(0o755) if key != "windows" else None
    print(f"[chromium] vendored {exe}")
    return exe


def install_deps(py: Path) -> None:
    print("[deps] installing requirements.txt into vendored python")
    subprocess.check_call([str(py), "-m", "pip", "install", "-q", "-r",
                           str(ROOT / "requirements.txt")])
    subprocess.check_call([str(py), "-m", "pip", "install", "-q", "-e",
                           str(ROOT / "agent_sdk")])


def main() -> None:
    print("== BlackBoxBench bootstrap ==")
    py = ensure_python()
    ensure_chromium()
    install_deps(py)
    print("\nDone. Start the controller with:")
    if platform.system() == "Windows":
        print(r"  vendor\python\python.exe -m benchmark.server --port 7800")
    else:
        print("  vendor/python/bin/python3 -m benchmark.server --port 7800")


if __name__ == "__main__":
    main()
