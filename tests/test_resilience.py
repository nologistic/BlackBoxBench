from __future__ import annotations

import io
import httpx
import pytest
from contextlib import nullcontext
from PIL import Image

from benchmark.orchestrator.apps import get_app
from benchmark.orchestrator.manager import SessionManager
from benchmark.orchestrator.session import Budget, Session
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
                 fail_click: bool = False):
        self.fail_screenshot = fail_screenshot
        self.fail_click = fail_click
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
        return True


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
    runtime = _Runtime(fail_click=True)
    session = Session("sess_failure", get_app("ecommerce_demo"), runtime,
                      tmp_path / "failure", Budget(10, 600, 10))

    with pytest.raises(RuntimeError, match="click failed"):
        session.execute(m.Action(type=m.ActionType.CLICK, x=10, y=10))

    assert session.status == "failed"
    assert session.budget.ended_at is not None
    assert runtime.stopped
    assert "runtime error: click failed" in \
        (session.dir / "session.json").read_text("utf-8")


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
