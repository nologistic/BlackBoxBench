"""Regression tests for the 2026-09-17 full-project review fixes.

Each test pins one fix from the review: stdin isolation for docker exec
(F1), no silent checklist env fallback (F2), live-session reservation
(F3), in-container timeout wrapping (F6), JSONC-tolerant install (F9) and
doom-loop telemetry (F13).
"""
from __future__ import annotations

import inspect
import json
import sys
import threading
from pathlib import Path
from types import SimpleNamespace

import pytest

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))


# ----------------------------------------------------------------- F1 + F6

def test_docker_helper_never_inherits_stdin():
    """An exec child must not eat bytes from the MCP stdio channel."""
    from reproduction import workspace
    src = inspect.getsource(workspace._docker)
    assert "stdin=subprocess.DEVNULL" in src


def test_run_program_kills_the_process_inside_the_sandbox():
    """A timed-out build must die inside the container, not just on the
    client side (leftover gradle builds stacked up until pids-limit/OOM)."""
    from reproduction import workspace as web_ws
    from app_reproduction import workspace as app_ws
    for mod in (web_ws, app_ws):
        src = inspect.getsource(mod)
        assert '"timeout", "--signal=KILL"' in src, mod.__name__


# --------------------------------------------------------------------- F2

def test_app_review_checklist_has_no_env_fallback(monkeypatch):
    """A silent env default graded the wrong object on 2026-09-08."""
    monkeypatch.setenv("BBB_APP_REVIEW_CHECKLIST", "android_commerce_demo")
    from agents.app_review import mcp_server
    with pytest.raises(ValueError) as ei:
        mcp_server._resolve_checklist("")
    assert "显式指定" in str(ei.value)


def test_web_review_checklist_has_no_env_fallback(monkeypatch):
    monkeypatch.setenv("BBB_WEB_REVIEW_CHECKLIST", "youtube_web")
    from agents.web_review import mcp_server
    with pytest.raises(ValueError) as ei:
        mcp_server._resolve_checklist("")
    assert "显式指定" in str(ei.value)


# --------------------------------------------------------------------- F3

def test_live_session_reservation_blocks_concurrent_create(monkeypatch,
                                                           tmp_path):
    """Two concurrent creates must never boot two browsers on one login
    profile: the second create is refused while the first is mid-boot."""
    from benchmark.orchestrator import manager as mgr

    m = mgr.SessionManager(runs_dir=tmp_path)
    in_start = threading.Event()
    release = threading.Event()

    class SlowRuntime:
        def __init__(self, *a, **k):
            pass

        def start(self):
            in_start.set()
            assert release.wait(timeout=10)
            raise RuntimeError("boot aborted by test")

        def stop(self):
            pass

    monkeypatch.setattr(m, "_make_runtime", lambda spec, d, s: SlowRuntime())
    spec = SimpleNamespace(kind="live", app_id="todoist_web", precheck=None)

    def first_create():
        try:
            m.create_spec(spec)
        except RuntimeError:
            pass  # aborted by the test via release()

    t = threading.Thread(target=first_create, daemon=True)
    t.start()
    try:
        assert in_start.wait(timeout=5)  # first create is mid-boot now
        with pytest.raises(RuntimeError, match="concurrent"):
            m.create_spec(spec)           # second create must be refused
    finally:
        release.set()
        t.join(timeout=10)


# --------------------------------------------------------------------- F9

def test_jsonc_stripper_preserves_urls_inside_strings():
    from agents.app_review.install import _strip_jsonc_comments
    text = ('{\n  // a comment with "quotes" and a url https://x.y\n'
            '  "url": "https://example.com/a//b",\n'
            '  /* block comment */ "k": 1\n}')
    out = _strip_jsonc_comments(text)
    assert '"https://example.com/a//b"' in out
    assert json.loads(out) == {"url": "https://example.com/a//b", "k": 1}


# -------------------------------------------------------------------- F13

def test_doomloop_emits_trace_event_after_repetition():
    """A stuck agent (same action, same pre-frame) surfaces early; the
    receipt and the session itself are untouched (trace-only)."""
    from benchmark.orchestrator.session import Session
    s = Session.__new__(Session)
    s.step = 3
    s._last_action_key = ""
    s._action_repeat_count = 0
    events = []
    s.recorder = SimpleNamespace(
        log_event=lambda name, data: events.append((name, data)))

    action = {"type": "tap", "x": 1, "y": 2}
    for _ in range(24):
        s._watch_doomloop(action, 7)
    assert events, "no doom-loop event after 24 identical repeats"
    assert events[0][0] == "action_loop_suspected"
    assert events[0][1]["repeat"] == 24
    assert events[0][1]["action_type"] == "tap"

    # A different frame means the screen moved: the counter resets.
    s._watch_doomloop(action, 8)
    s._watch_doomloop(action, 8)
    assert s._action_repeat_count == 2
