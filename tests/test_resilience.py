from __future__ import annotations

import io
import os
import httpx
import pytest
from contextlib import nullcontext
from PIL import Image

from benchmark import config
from benchmark.orchestrator.apps import get_app
from benchmark.orchestrator.manager import SessionManager
from benchmark.orchestrator.session import (
    Budget, Session, SessionClosed, TransientEnvironmentError)
from benchmark.runtime.base import Runtime, RuntimeInfo
from benchmark.topology import models as m
from blackbox_bench_sdk import ActionRejectedError, Environment


def _png() -> bytes:
    image = Image.new("RGB", (1440, 900), (20, 20, 30))
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


class _Runtime(Runtime):
    def __init__(self, *, fail_screenshot: bool = False,
                 fail_click: bool = False, healthy: bool = True):
        self.fail_screenshot = fail_screenshot
        self.fail_click = fail_click
        self.healthy = healthy
        self.started = False
        self.stopped = False

    def start(self):
        self.started = True

    def stop(self):
        self.stopped = True

    def reset(self):
        pass

    def screenshot(self) -> bytes:
        if self.fail_screenshot:
            raise RuntimeError("no pixels")
        return _png()

    def info(self) -> RuntimeInfo:
        return RuntimeInfo(1440, 900, 1.0)

    def mouse_move(self, x, y):
        pass

    def mouse_down(self, x, y, button="left"):
        pass

    def mouse_up(self, x, y, button="left"):
        pass

    def click(self, x, y, button="left", count=1):
        if self.fail_click:
            raise RuntimeError("click failed")

    def scroll(self, dx, dy):
        pass

    def type_text(self, text):
        pass

    def key(self, key, kind="press"):
        pass

    def health(self) -> bool:
        return self.healthy


def test_initial_capture_failure_cleans_up_runtime(tmp_path, monkeypatch):
    import benchmark.orchestrator.manager as manager_module

    runtime = _Runtime(fail_screenshot=True)
    manager = SessionManager(tmp_path / "runs")
    created = {}

    class CapturedSession(Session):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            created["session"] = self

    monkeypatch.setattr(manager_module, "Session", CapturedSession)
    monkeypatch.setattr(manager, "_make_runtime",
                        lambda spec, data_dir, session_dir: runtime)

    with pytest.raises(RuntimeError, match="no pixels"):
        manager.create("ecommerce_demo")

    assert runtime.started and runtime.stopped
    assert not manager._sessions
    assert not manager._runtimes
    recorder = created["session"].recorder
    assert all(handle.closed for handle in
               (recorder._actions, recorder._obs, recorder._disc))


def test_runtime_failure_freezes_budget_and_persists_reason(tmp_path):
    """A dead environment ends the session and hands everything back."""
    runtime = _Runtime(fail_click=True, healthy=False)
    session = Session("sess_failure", get_app("ecommerce_demo"), runtime,
                      tmp_path / "failure", Budget(10, 600, 10))

    with pytest.raises(SessionClosed):
        session.execute(m.Action(type=m.ActionType.CLICK, x=10, y=10))

    assert session.status == "failed"
    assert session.budget.ended_at is not None
    assert runtime.stopped
    persisted = (session.dir / "session.json").read_text("utf-8")
    assert "runtime error" in persisted
    # The reason is kept locally for diagnosis, never handed to the Agent.
    assert "click failed" in persisted


def test_recoverable_runtime_failure_keeps_the_session_alive(tmp_path):
    """A healthy environment that drops one operation must not end a session.

    Discarding 85 steps of exploration over one blip records an environment
    artefact as if it were a limit of the Agent.
    """
    runtime = _Runtime(fail_click=True, healthy=True)
    session = Session("sess_blip", get_app("ecommerce_demo"), runtime,
                      tmp_path / "blip", Budget(10, 600, 10))

    with pytest.raises(TransientEnvironmentError):
        session.execute(m.Action(type=m.ActionType.CLICK, x=10, y=10))

    assert session.status == "running"
    assert not runtime.stopped
    assert session.budget.ended_at is None
    # The blip is recorded for later diagnosis.
    import json
    events = [json.loads(line) for line in
              (session.dir / "actions.jsonl").read_text(
                  encoding="utf-8").splitlines() if line.strip()]
    assert any(e.get("event") == "environment_blip" for e in events)
    session.recorder.close()


def test_finalize_failure_keeps_session_retryable(tmp_path, monkeypatch):
    session = Session("sess_finalize", get_app("ecommerce_demo"), _Runtime(),
                      tmp_path / "finalize", Budget(10, 600, 10))

    def fail_finalize():
        raise RuntimeError("cannot write topology")

    monkeypatch.setattr(session.store, "finalize", fail_finalize)
    with pytest.raises(RuntimeError, match="cannot write topology"):
        session.finalize()

    assert session.status == "running"
    assert session.budget.ended_at is None
    session.recorder.close()


def test_sdk_revise_uses_standard_error_mapping(client, mini_session):
    environment = Environment("http://testserver", mini_session, client=client)

    with pytest.raises(ActionRejectedError, match="invalid_kind"):
        environment.revise("invalid", "state_missing", "delete")
    with pytest.raises(httpx.HTTPStatusError):
        environment.revise("state", "state_missing", "delete")


def test_session_budget_rejects_invalid_conditions(client):
    response = client.post("/api/sessions", json={
        "app_id": "miniapp", "budget": {"max_actions": 0}})

    assert response.status_code == 422


def test_controller_bootstrap_is_shared_and_rejects_remote_autostart(monkeypatch):
    from benchmark.orchestrator import controller_process

    with pytest.raises(RuntimeError, match="loopback"):
        controller_process.ensure_local_controller(
            "http://example.com:7800", lambda: False)

    starts = []
    monkeypatch.setattr(controller_process, "lock_for", lambda *a, **k: nullcontext())
    monkeypatch.setattr(controller_process, "_read_state", lambda _: {})
    monkeypatch.setattr(controller_process, "start_local_controller",
                        lambda controller, is_up, timeout=40.0:
                        starts.append(controller) or 123)
    controller_process.ensure_local_controller(
        "http://127.0.0.1:7811", lambda: False)
    assert starts == ["http://127.0.0.1:7811"]


def test_controller_bootstrap_does_not_spawn_over_live_unhealthy_pid(monkeypatch):
    from benchmark.orchestrator import controller_process

    monkeypatch.setattr(controller_process, "lock_for", lambda *a, **k: nullcontext())
    monkeypatch.setattr(controller_process, "_read_state", lambda _: {
        "controller": "http://127.0.0.1:7812", "pid": 42,
        "log": "controller.log"})
    monkeypatch.setattr(controller_process, "_pid_alive", lambda _: True)
    monkeypatch.setattr(controller_process.time, "sleep", lambda _: None)
    clock = iter([0.0, 10.0])
    monkeypatch.setattr(controller_process.time, "monotonic", lambda: next(clock))
    with pytest.raises(RuntimeError, match="alive but unhealthy"):
        controller_process.ensure_local_controller(
            "http://127.0.0.1:7812", lambda: False, timeout=1)


def test_browser_profile_is_created_outside_the_repository(monkeypatch, tmp_path):
    """A Chromium user-data dir must not be written into the session tree.

    It is a few hundred cache files per session. Creating it under `runs/` meant
    every teardown bulk-deleted workspace files, which is what made test runs
    trip the editor's delete protection, and it put browser cache into the
    evidence tree.
    """
    from benchmark import scratch

    monkeypatch.setattr(scratch.config, "SCRATCH_DIR", tmp_path / "scratch")
    profile = scratch.new_scratch_dir("chromium", "sess_20260901_x")
    try:
        assert (tmp_path / "scratch") in profile.parents
        assert profile.is_dir() and not any(profile.iterdir())
        assert str(os.getpid()) in profile.name
        assert "chromium" in profile.name
    finally:
        assert scratch.remove_scratch_dir(profile) is True
    assert not profile.exists()


def test_scratch_reclaim_spares_live_owners_and_clears_dead_ones(
        monkeypatch, tmp_path):
    from benchmark import scratch

    monkeypatch.setattr(scratch.config, "SCRATCH_DIR", tmp_path / "scratch")
    root = tmp_path / "scratch"
    dead = root / "chromium_sess-a_999999_aaaa"
    live = root / "avd_sess-b_4242_bbbb"
    mine = scratch.new_scratch_dir("chromium", "sess-mine")
    for path in (dead, live):
        path.mkdir(parents=True)
        (path / "Default").mkdir()
        (path / "Default" / "Cache").write_bytes(b"x" * 100)

    results = dict(scratch.reclaim_scratch(lambda pid: pid == 4242))
    assert results.get(dead) is True and not dead.exists()
    assert live.is_dir(), "a live owner's scratch must be left alone"
    assert mine.is_dir(), "this process's own scratch must never be reclaimed"

    count, gigabytes = scratch.scratch_usage()
    assert count == 2 and gigabytes >= 0.0


def test_rejected_discovery_is_auditable(client, mini_session):
    """A refused discovery call must leave a trace.

    Only successful calls used to be recorded, which made two very different
    situations indistinguishable after the fact: an Agent that never tried to
    record a transition edge, and an Agent that tried and was refused. Scoring
    the second as Agent behaviour would attribute a platform artefact to the
    Agent, so the distinction has to survive in the trace.
    """
    import json

    from benchmark.server import manager

    response = client.post(f"/agent/{mini_session}/discovery/edge", json={
        "source": "state_does_not_exist", "target": "state_also_missing",
        "type": "TRANSITIONS_TO",
        "evidence": [{"step": 1, "before_frame": 0, "after_frame": 1,
                      "action": "click(10,10)"}]})
    assert response.status_code == 422

    session = manager.get(mini_session)
    records = [json.loads(line) for line in
               (session.dir / "discovery.jsonl").read_text(
                   encoding="utf-8").splitlines() if line.strip()]
    # The session fixture is shared, so select by operation rather than order.
    refusals = [r for r in records
                if r.get("accepted") is False and r.get("op") == "create_edge"]
    assert refusals, "a refused discovery call must be recorded"
    assert "state_does_not_exist" in refusals[-1]["reason"]
    # The attempt itself is kept, so a reviewer can see what was intended.
    assert refusals[-1]["request"]["target"] == "state_also_missing"


class _AnimatedRuntime(_Runtime):
    """A target that never goes still — a clock, stopwatch or spinner.

    Only a small corner repaints, exactly like a ticking seconds field.
    """

    def __init__(self):
        super().__init__()
        self.tick = 0

    def screenshot(self) -> bytes:
        self.tick += 1
        image = Image.new("RGB", (1440, 900), (20, 20, 30))
        # ~0.6% of the frame keeps changing, forever. The change alternates
        # between near-black and near-white so it survives the greyscale
        # conversion the diff uses, like real ticking digits would.
        shade = 240 if self.tick % 2 else 15
        for x in range(60):
            for y in range(140):
                image.putpixel((x, y), (shade, shade, shade))
        output = io.BytesIO()
        image.save(output, format="PNG")
        return output.getvalue()


def test_settle_treats_a_perpetual_animation_as_stable(tmp_path, monkeypatch):
    """A target that animates forever must not burn the whole settle timeout.

    Measured on the Google Clock dataset, 57-63% of observations waited out the
    full timeout and spent 105-303s of the duration budget per run for no
    information, because a ticking clock can never satisfy "nothing changed".
    """
    import json

    monkeypatch.setattr(config, "SETTLE_TIMEOUT_MS", 2000)
    runtime = _AnimatedRuntime()
    session = Session("sess_anim", get_app("ecommerce_demo"), runtime,
                      tmp_path / "anim", Budget(10, 600, 10))
    _, _, settle_ms = session._capture_frame(1, settle=True)
    assert settle_ms < 1000, "an animation must settle well before the timeout"

    record = json.loads((tmp_path / "anim" / "observations.jsonl").read_text(
        encoding="utf-8").splitlines()[-1])
    assert record["settle_reason"] == "animation"
    session.recorder.close()


def test_settle_still_waits_for_a_transition_in_progress(tmp_path, monkeypatch):
    """The animation allowance must not shortcut a real, large repaint."""
    import json

    monkeypatch.setattr(config, "SETTLE_TIMEOUT_MS", 400)

    class _Churning(_Runtime):
        def __init__(self):
            super().__init__()
            self.tick = 0

        def screenshot(self) -> bytes:
            self.tick += 1
            shade = (self.tick * 60) % 255
            image = Image.new("RGB", (1440, 900), (shade, shade, shade))
            output = io.BytesIO()
            image.save(output, format="PNG")
            return output.getvalue()

    session = Session("sess_churn", get_app("ecommerce_demo"), _Churning(),
                      tmp_path / "churn", Budget(10, 600, 10))
    _, _, settle_ms = session._capture_frame(1, settle=True)
    assert settle_ms >= 400, "a full-screen repaint must not count as settled"
    record = json.loads((tmp_path / "churn" / "observations.jsonl").read_text(
        encoding="utf-8").splitlines()[-1])
    assert record["settle_reason"] == "timeout"
    session.recorder.close()


def test_environment_degradation_is_recorded_before_it_fails(tmp_path):
    """A slow collapse must be visible in the trace, not just the crash report.

    An emulator starved of host resources drifts from ~1.9s to ~4.9s per action
    over dozens of steps before an adb call finally fails. Without this note the
    crash alone makes a gradual environment collapse look like a sudden fault,
    and the lost exploration looks like an Agent problem.
    """
    import json

    session = Session("sess_slow", get_app("ecommerce_demo"), _Runtime(),
                      tmp_path / "slow", Budget(100, 600, 100))
    for value in [100.0] * 10 + [200.0] * 9:
        session._watch_latency(value)
    events = [json.loads(line) for line in
              (tmp_path / "slow" / "actions.jsonl").read_text(
                  encoding="utf-8").splitlines() if line.strip()]
    assert not [e for e in events if e.get("event") == "environment_degrading"], \
        "a 2x drift is normal jitter and must not be reported"

    for _ in range(10):
        session._watch_latency(600.0)
    events = [json.loads(line) for line in
              (tmp_path / "slow" / "actions.jsonl").read_text(
                  encoding="utf-8").splitlines() if line.strip()]
    warnings = [e for e in events if e.get("event") == "environment_degrading"]
    assert warnings, "a sustained 3x+ drift must be recorded"
    assert warnings[0]["ratio"] >= 3.0
    session.recorder.close()


def test_manager_close_releases_runtime_handle(tmp_path, monkeypatch):
    runtime = _Runtime()
    manager = SessionManager(tmp_path / "runs")
    monkeypatch.setattr(manager, "_make_runtime",
                        lambda spec, data_dir, session_dir: runtime)
    session = manager.create("ecommerce_demo")
    assert session.id in manager._runtimes
    manager.close(session.id)
    assert session.status == "closed"
    assert session.id not in manager._runtimes
    assert runtime.stopped


def test_observe_failure_fails_session_and_releases_runtime(tmp_path):
    """Capturing pixels is a runtime operation and can fail like any action.

    It used to raise straight out of the controller, so the session stayed
    "running" while its browser or emulator — and for Android its target lease
    and port reservation — were already unusable and never handed back.
    """
    runtime = _Runtime(fail_screenshot=True, healthy=False)
    session = Session("sess_observe", get_app("ecommerce_demo"), runtime,
                      tmp_path / "observe", Budget(10, 600, 10))

    with pytest.raises(SessionClosed):
        session.observe()

    assert session.status == "failed"
    assert session.budget.ended_at is not None
    assert runtime.stopped
    assert (session.dir / "crash_report.json").is_file()
    assert "runtime error" in (session.dir / "session.json").read_text("utf-8")


def test_agent_never_sees_internal_failure_detail(tmp_path):
    """The message handed to the Agent must not name the machinery.

    A raw runtime failure carries an adb command line and emulator serial, or a
    CDP method and websocket URL, plus host absolute paths. Those disclose what
    sits behind the pixels-only boundary and must stay in the local trace.
    """
    class _Leaky(_Runtime):
        def screenshot(self) -> bytes:
            raise RuntimeError(
                "CalledProcessError(4294967295, ['D:/proj/vendor/android/sdk/"
                "platform-tools/adb.exe', '-s', 'emulator-5554', 'exec-out', "
                "'screencap', '-p'])")

    for healthy in (True, False):
        runtime = _Leaky(healthy=healthy)
        session = Session(f"sess_leak_{healthy}", get_app("ecommerce_demo"),
                          runtime, tmp_path / f"leak_{healthy}",
                          Budget(10, 600, 10))
        with pytest.raises((TransientEnvironmentError, SessionClosed)) as caught:
            session.observe()
        message = str(caught.value)
        for forbidden in ("adb", "emulator-5554", "screencap", "D:/proj",
                          "CalledProcessError", "platform-tools"):
            assert forbidden not in message, f"{forbidden!r} reached the Agent"
        session.recorder.close()


def test_abandoned_running_sessions_are_recorded_as_failed(tmp_path):
    """Sessions live in memory, so a killed controller leaves a lying status.

    Such a session can never be observed, closed or resumed again, so leaving
    `session.json` claiming "running" misleads every later operator tool.
    """
    import json

    runs = tmp_path / "runs"
    zombie = runs / "sess_20260831_133445_2bab2f"
    zombie.mkdir(parents=True)
    (zombie / "session.json").write_text(json.dumps({
        "session_id": zombie.name, "app_id": "google_clock",
        "status": "running", "close_reason": None}), encoding="utf-8")
    finished = runs / "sess_20260831_143247_866df2"
    finished.mkdir()
    (finished / "session.json").write_text(json.dumps({
        "session_id": finished.name, "app_id": "google_clock",
        "status": "closed", "close_reason": "agent_finalized"}),
        encoding="utf-8")

    manager = SessionManager(runs)
    assert manager.mark_abandoned_sessions() == [zombie.name]

    corrected = json.loads((zombie / "session.json").read_text(encoding="utf-8"))
    assert corrected["status"] == "failed"
    assert "cannot be resumed" in corrected["close_reason"]
    # A finalized session is never rewritten.
    assert json.loads((finished / "session.json").read_text(
        encoding="utf-8"))["close_reason"] == "agent_finalized"
    # Idempotent: a second controller start finds nothing left to correct.
    assert manager.mark_abandoned_sessions() == []


def test_cdp_retry_backs_off_long_enough_for_a_network_hiccup(monkeypatch):
    """A transient CDP/socket failure must not end an exploration.

    Two live explorations were lost tens of steps in to `WinError 10054` and
    `Connection timed out`. The retry existed but its total window was under two
    seconds — enough for a renderer swap, not for a real site's network blip.
    """
    from benchmark.runtime.local_chromium import LocalChromiumRuntime

    runtime = LocalChromiumRuntime.__new__(LocalChromiumRuntime)
    waits: list[float] = []
    monkeypatch.setattr("benchmark.runtime.local_chromium.time.sleep",
                        lambda seconds: waits.append(seconds))
    runtime._reconnect = lambda: None

    attempts = {"n": 0}

    class _Flaky:
        def call(self, method, params=None):
            attempts["n"] += 1
            if attempts["n"] < 6:
                raise ConnectionResetError(
                    10054, "远程主机强迫关闭了一个现有的连接")
            return {"data": "ok"}

    runtime._cdp = _Flaky()
    assert runtime._cdp_call("Page.captureScreenshot") == {"data": "ok"}
    assert attempts["n"] == 6
    # Backoff, not a flat delay, and patient enough to ride out a real blip.
    assert waits == sorted(waits) and waits[0] < waits[-1]
    assert sum(waits) > 5.0
    # Every wait stays bounded so a dead browser is still reported promptly.
    assert max(waits) <= 3.2

    # A non-transient error is reported immediately, never retried away.
    class _Broken:
        def call(self, method, params=None):
            raise PermissionError("CDP method not whitelisted: DOM.getDocument")

    runtime._cdp = _Broken()
    with pytest.raises(PermissionError):
        runtime._cdp_call("DOM.getDocument")
