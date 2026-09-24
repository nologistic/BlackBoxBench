"""Central configuration for BlackBoxBench.

Resolution order: environment variable (BBB_*) > defaults here.
Viewport/scale/zoom are part of session metadata and must stay fixed
within a session so action coordinates and screenshots stay 1:1.
"""
from __future__ import annotations

import os
import tempfile
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent

# ------------------------------------------------------------------ paths
RUNS_DIR = Path(os.environ.get("BBB_RUNS_DIR", PROJECT_ROOT / "runs"))
SAMPLE_APPS_DIR = PROJECT_ROOT / "sample_apps"
VENDOR_DIR = PROJECT_ROOT / "vendor"
APP_OUTPUT_DIR = Path(os.environ.get("BBB_APP_OUTPUT_DIR",
                                     PROJECT_ROOT / "app_output"))
# Web reproduction outputs; the web-review evaluation condition installs a
# handoff directory from here into a loopback static server for judging.
WEBSITE_OUTPUT_DIR = Path(os.environ.get("BBB_WEBSITE_OUTPUT_DIR",
                                         PROJECT_ROOT / "website_output"))
ANDROID_TARGETS_DIR = Path(os.environ.get(
    "BBB_ANDROID_TARGETS_DIR", PROJECT_ROOT / "runs" / "android_targets"))

# Volatile runtime scratch: Chromium user-data dirs and Android AVD clones.
# These are large (a clone is several GB), numerous (hundreds of cache files)
# and worthless once a session ends, so they live outside the repository:
# `runs/` is the evidence tree, and a teardown must not have to delete
# thousands of workspace files. See benchmark/scratch.py.
SCRATCH_DIR = Path(os.environ.get("BBB_SCRATCH_DIR", "").strip() or
                   Path(tempfile.gettempdir()) / "blackboxbench-scratch")

# Guest UI language for every Android emulator the project starts: exploration,
# reproduction review and third-party evaluation. Pinning it here keeps the
# operator-authored checklist, what the exploring Agent sees, and what the judge
# sees in the same language. Set to an empty value to keep the image default.
ANDROID_LOCALE = os.environ.get("BBB_ANDROID_LOCALE", "zh-CN").strip()

# Free host memory an emulator needs before it may boot. Two concurrent AVDs on
# a 40 GB machine leave single-digit gigabytes free, and adb then starts
# dropping individual commands — a failure that looks like a device fault but
# is really host exhaustion. Refusing up front turns that into a clear message
# instead of a random crash tens of steps into an exploration.
# Set to 0 to disable the admission check.
# 4200 admits a 4th emulator on the 39.4 GB host (3 resident qemus hold
# ~12.3 GB, leaving ~4.5 GB free > 4200), at the cost of a thin system
# margin: when running 4 in parallel, keep heavyweight host applications
# closed, and treat mid-exploration "environment failure" reports as the
# signal to fall back to 3.
ANDROID_MIN_FREE_MEMORY_MB = int(
    os.environ.get("BBB_ANDROID_MIN_FREE_MB", "4200"))

# Resident memory one booted emulator is expected to hold. Used only to reserve
# headroom for emulators that are still booting: their memory is not yet
# reflected in the host's "available" figure, so two sessions starting at the
# same moment would each see enough room and then jointly overcommit.
# Measured on the 2026-09-01 dual-emulator google_clock runs: one resident
# qemu process holds ~4.1 GB (2 GB guest RAM + ~2.1 GB qemu/swiftshader host
# overhead), so the previous 3072 figure admitted parallel boots too
# optimistically; 4100 tracks that measurement while still allowing four
# concurrent boots to pass the reservation arithmetic.
ANDROID_EMULATOR_MEMORY_MB = int(
    os.environ.get("BBB_ANDROID_EMULATOR_MB", "4100"))

# ------------------------------------------------------------------ viewport
VIEWPORT_WIDTH = int(os.environ.get("BBB_VIEWPORT_WIDTH", "1440"))
VIEWPORT_HEIGHT = int(os.environ.get("BBB_VIEWPORT_HEIGHT", "900"))
DEVICE_SCALE_FACTOR = 1.0   # devicePixelRatio; fixed so css px == screenshot px
BROWSER_ZOOM = 1.0

# ------------------------------------------------------------------ browser
# Explicit override; otherwise the runtime looks for vendor/chromium, then
# system Edge/Chrome. See runtime/local_chromium.py.
BROWSER_EXECUTABLE = os.environ.get("BBB_BROWSER", "")

APP_INTERNAL_HOST = "reference-app.internal"

# ------------------------------------------------------------------ capture policy
CAPTURE_AFTER_ACTION = True
SETTLE_POLL_MS = 60
SETTLE_TIMEOUT_MS = 2000
SETTLE_STABLE_DIFF = 0.002   # fraction of changed pixels below which UI is "settled"
# Some targets never go still: a clock, stopwatch or timer repaints a small
# region forever, so the plain "nothing changed" test can never be satisfied and
# every observation waits out the full timeout. Measured on the Google Clock
# dataset, that hit 57-63% of observations and burned 105-303s of the session's
# duration budget per run, for no information.
# A change confined to a small, stable fraction of the screen across consecutive
# polls is therefore treated as an animation rather than an unfinished
# transition. This is a pixel statistic only: no semantic channel is added.
SETTLE_ANIMATION_DIFF = 0.02   # upper bound on the animated area fraction
SETTLE_ANIMATION_POLLS = 3     # consecutive polls that must agree

# ------------------------------------------------------------------ budgets (defaults; per-session overridable)
DEFAULT_MAX_ACTIONS = 1500
DEFAULT_MAX_DURATION_S = 3 * 3600
DEFAULT_MAX_OBSERVATIONS = 2000

# ------------------------------------------------------------------ cursor overlay
DRAW_CURSOR_IN_FRAMES = True
CURSOR_COLOR = (20, 20, 20)
CURSOR_OUTLINE = (255, 255, 255)

# ------------------------------------------------------------------ gateway
GATEWAY_HEADER = "X-BBB-Gateway"
