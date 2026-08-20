"""Central configuration for BlackBoxBench.

Resolution order: environment variable (BBB_*) > defaults here.
Viewport/scale/zoom are part of session metadata and must stay fixed
within a session so action coordinates and screenshots stay 1:1.
"""
from __future__ import annotations

import os
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent

# ------------------------------------------------------------------ paths
RUNS_DIR = Path(os.environ.get("BBB_RUNS_DIR", PROJECT_ROOT / "runs"))
SAMPLE_APPS_DIR = PROJECT_ROOT / "sample_apps"
VENDOR_DIR = PROJECT_ROOT / "vendor"

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

# ------------------------------------------------------------------ replay
REPLAY_MAX_STEP_INTERVAL_MS = 250     # throttle waits during deterministic replay
REPLAY_FRAME_HAMMING_THRESHOLD = 6    # phash (64-bit) tolerance per frame
REPLAY_FINAL_MSE_THRESHOLD = 0.001    # normalized mse tolerance for final frame

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
