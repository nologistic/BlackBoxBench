#!/usr/bin/env python3
"""Trusted hardware boundary for the no-tools exploration condition.

The untrusted workshop receives no client, protocol document, helper library,
or exploration API. This process exposes only two device-like filesystem
objects in a shared volume: a raw display image and a Linux input-event FIFO.
Its source exists only in the trusted reference image.
"""
from __future__ import annotations

import os
import select
import struct
import subprocess
import tempfile
import time
from pathlib import Path

from PIL import Image

WIDTH = int(os.environ.get("BBB_VIEWPORT_WIDTH", "1440"))
HEIGHT = int(os.environ.get("BBB_VIEWPORT_HEIGHT", "900"))
DISPLAY = os.environ.get("DISPLAY", ":99")
DEVICE_DIR = Path(os.environ.get("BBB_DEVICE_DIR", "/device"))
FRAME_PATH = DEVICE_DIR / "fb0.ppm"
EVENT_PATH = DEVICE_DIR / "event0"

# Native Linux input_event fields, fixed to the 64-bit container ABI:
# timeval(sec,usec), type, code, value. This is an OS ABI, not a benchmark
# action protocol. The Agent is not given this definition or any client.
INPUT_EVENT = struct.Struct("=qqHHi")
EV_SYN, EV_KEY, EV_REL, EV_ABS = 0, 1, 2, 3
SYN_REPORT = 0
REL_HWHEEL, REL_WHEEL = 6, 8
ABS_X, ABS_Y = 0, 1
BTN_LEFT, BTN_RIGHT, BTN_MIDDLE = 272, 273, 274

MAX_ACTIONS = int(os.environ.get("BBB_RAW_MAX_ACTIONS", "1500"))
MAX_FRAMES = int(os.environ.get("BBB_RAW_MAX_FRAMES", "2000"))
MAX_DURATION_S = int(os.environ.get("BBB_RAW_MAX_DURATION_S", "10800"))
MIN_ACTION_INTERVAL_S = float(os.environ.get("BBB_RAW_MIN_ACTION_INTERVAL_S", "0.04"))

KEYS = {
    1: "Escape", 14: "BackSpace", 15: "Tab", 28: "Return", 57: "space",
    63: "F5", 102: "Home", 103: "Up", 104: "Page_Up", 105: "Left",
    106: "Right", 107: "End", 108: "Down", 109: "Page_Down",
    111: "Delete", 158: "BrowserBack", 159: "BrowserForward",
    42: "Shift_L", 54: "Shift_R",
}
for _code, _char in {
    2: "1", 3: "2", 4: "3", 5: "4", 6: "5", 7: "6", 8: "7",
    9: "8", 10: "9", 11: "0", 12: "minus", 13: "equal",
    16: "q", 17: "w", 18: "e", 19: "r",
    20: "t", 21: "y", 22: "u", 23: "i", 24: "o", 25: "p", 30: "a",
    31: "s", 32: "d", 33: "f", 34: "g", 35: "h", 36: "j", 37: "k",
    26: "bracketleft", 27: "bracketright", 38: "l", 39: "semicolon",
    40: "apostrophe", 41: "grave", 43: "backslash", 44: "z", 45: "x",
    46: "c", 47: "v", 48: "b", 49: "n", 50: "m", 51: "comma",
    52: "period", 53: "slash",
}.items():
    KEYS[_code] = _char

BUTTONS = {BTN_LEFT: "1", BTN_MIDDLE: "2", BTN_RIGHT: "3"}

_started = time.monotonic()
_last_action = 0.0
_actions = 0
_frames = 0
_x = WIDTH // 2
_y = HEIGHT // 2


def _run(argv: list[str], timeout: float = 15) -> None:
    env = dict(os.environ)
    env["DISPLAY"] = DISPLAY
    result = subprocess.run(argv, env=env, timeout=timeout,
                            stdout=subprocess.DEVNULL,
                            stderr=subprocess.DEVNULL, check=False)
    if result.returncode:
        raise RuntimeError("device unavailable")


def capture_frame() -> bool:
    """Atomically refresh the raw P6 framebuffer; reveal no metadata channel."""
    global _frames
    if _frames >= MAX_FRAMES or time.monotonic() - _started > MAX_DURATION_S:
        return False
    fd, screenshot = tempfile.mkstemp(prefix="frame-", suffix=".png")
    os.close(fd)
    os.unlink(screenshot)
    temporary = FRAME_PATH.with_suffix(".tmp")
    try:
        _run(["scrot", "-o", "-z", screenshot])
        with Image.open(screenshot) as image:
            rgb = image.convert("RGB")
            if rgb.size != (WIDTH, HEIGHT):
                rgb = rgb.resize((WIDTH, HEIGHT))
            with temporary.open("wb") as stream:
                stream.write(f"P6\n{WIDTH} {HEIGHT}\n255\n".encode("ascii"))
                stream.write(rgb.tobytes())
        os.replace(temporary, FRAME_PATH)
        os.chmod(FRAME_PATH, 0o640)
        _frames += 1
        return True
    finally:
        for path in (Path(screenshot), temporary):
            try:
                path.unlink()
            except OSError:
                pass


def _pacing() -> bool:
    global _actions, _last_action
    if (_actions >= MAX_ACTIONS
            or time.monotonic() - _started > MAX_DURATION_S):
        return False
    delay = MIN_ACTION_INTERVAL_S - (time.monotonic() - _last_action)
    if delay > 0:
        time.sleep(delay)
    _actions += 1
    _last_action = time.monotonic()
    return True


def apply_batch(batch: list[tuple[int, int, int]]) -> bool:
    """Apply one SYN-delimited batch after strict scalar allowlisting."""
    global _x, _y
    if not batch:
        return capture_frame()
    if not _pacing():
        return False

    target_x, target_y = _x, _y
    operations: list[tuple[str, str, int]] = []
    for event_type, code, value in batch:
        if event_type == EV_ABS and code == ABS_X:
            target_x = min(WIDTH - 1, max(0, value))
        elif event_type == EV_ABS and code == ABS_Y:
            target_y = min(HEIGHT - 1, max(0, value))
        elif event_type == EV_KEY and code in BUTTONS and value in (0, 1):
            operations.append(("button", BUTTONS[code], value))
        elif event_type == EV_KEY and code in KEYS and value in (0, 1, 2):
            operations.append(("key", KEYS[code], value))
        elif event_type == EV_REL and code in (REL_WHEEL, REL_HWHEEL):
            operations.append(("wheel", str(code), max(-20, min(20, value))))
        # Everything else—including Ctrl/Alt/Super/F12—is silently discarded.

    if (target_x, target_y) != (_x, _y):
        _run(["xdotool", "mousemove", str(target_x), str(target_y)], 5)
        _x, _y = target_x, target_y
    for kind, name, value in operations:
        if kind == "button":
            _run(["xdotool", "mousedown" if value else "mouseup", name], 5)
        elif kind == "key":
            command = "keyup" if value == 0 else "keydown" if value == 1 else "key"
            _run(["xdotool", command, name], 5)
        else:
            if not value:
                continue
            if name == str(REL_WHEEL):
                button = "4" if value > 0 else "5"
            else:
                button = "6" if value < 0 else "7"
            _run(["xdotool", "click", "--repeat", str(abs(value)), button], 10)

    time.sleep(0.12)
    capture_frame()
    return True


def _prepare_devices() -> None:
    DEVICE_DIR.mkdir(parents=True, exist_ok=True)
    for path in (FRAME_PATH, EVENT_PATH):
        try:
            path.unlink()
        except OSError:
            pass
    os.mkfifo(EVENT_PATH, 0o620)
    capture_frame()


def _read_events() -> None:
    pending = bytearray()
    batch: list[tuple[int, int, int]] = []
    while True:
        descriptor = os.open(EVENT_PATH, os.O_RDONLY | os.O_NONBLOCK)
        try:
            while True:
                readable, _, _ = select.select([descriptor], [], [], 0.5)
                if not readable:
                    continue
                chunk = os.read(descriptor, INPUT_EVENT.size * 64)
                if not chunk:
                    time.sleep(0.05)
                    break
                pending.extend(chunk)
                while len(pending) >= INPUT_EVENT.size:
                    raw = bytes(pending[:INPUT_EVENT.size])
                    del pending[:INPUT_EVENT.size]
                    _, _, event_type, code, value = INPUT_EVENT.unpack(raw)
                    if event_type == EV_SYN and code == SYN_REPORT:
                        try:
                            apply_batch(batch)
                        except (OSError, RuntimeError, subprocess.SubprocessError):
                            pass
                        batch.clear()
                    elif len(batch) < 256:
                        batch.append((event_type, code, value))
        finally:
            os.close(descriptor)


def main() -> None:
    _prepare_devices()
    try:
        _read_events()
    finally:
        for path in (FRAME_PATH, EVENT_PATH):
            try:
                path.unlink()
            except OSError:
                pass


if __name__ == "__main__":
    main()
