"""Process-free integration tests for trusted Android operator entry points."""
from __future__ import annotations

import contextlib
import sys
from pathlib import Path
from types import SimpleNamespace

import pytest

from benchmark.runtime.base import RuntimeInfo


class _OperatorRuntime:
    instances: list["_OperatorRuntime"] = []

    def __init__(self, spec, work_dir, **kwargs):
        self.spec = spec
        self.work_dir = Path(work_dir)
        self.kwargs = kwargs
        self.started = False
        self.stopped = False
        self.serial = "emulator-5554"
        self.frames = 0
        self.calls: list[tuple] = []
        self.__class__.instances.append(self)

    def start(self):
        self.started = True

    def stop(self):
        self.started = False
        self.stopped = True

    def info(self):
        return RuntimeInfo(240, 480, 1.0, "android", "portrait", 420)

    def screenshot(self):
        self.frames += 1
        return f"frame-{self.frames}".encode("ascii")

    def tap(self, *args):
        self.calls.append(("tap", args))

    def restart_app(self):
        self.calls.append(("restart_app", ()))

    def reset(self):
        self.calls.append(("reset", ()))

    def _run(self, *args, **_kwargs):
        self.calls.append(("adb", args))
        value = "0\n" if "airplane_mode_on" in args else "10.0.2.2:8899\n"
        return SimpleNamespace(stdout=value, returncode=0)


def _target(policy: str = "offline"):
    return SimpleNamespace(
        app_id="google_clock", package_name="com.example.clock",
        network_policy=policy)


def test_manual_session_public_override_configures_guard_and_cleans_up(
        monkeypatch, tmp_path, capsys):
    from scripts import android_manual_session as manual

    spec = _target("offline")
    proxy_events: list[str] = []

    @contextlib.contextmanager
    def fake_proxy():
        proxy_events.append("start")
        try:
            yield "http://127.0.0.1:8899"
        finally:
            proxy_events.append("stop")

    _OperatorRuntime.instances.clear()
    monkeypatch.setattr(manual, "get_android_target", lambda _app: spec)
    monkeypatch.setattr(manual, "AndroidEmulatorRuntime", _OperatorRuntime)
    monkeypatch.setattr(manual, "_public_only_proxy", fake_proxy)
    monkeypatch.setattr(manual.config, "ANDROID_TARGETS_DIR", tmp_path)
    monkeypatch.setattr("builtins.input", lambda _prompt="": "q")
    monkeypatch.setattr(sys, "argv", [
        "android_manual_session.py", "--app", "google_clock",
        "--network", "public", "--keep",
    ])

    manual.main()

    runtime = _OperatorRuntime.instances[-1]
    assert spec.network_policy == "public"
    assert runtime.kwargs == {"headed": True, "lease_mode": "explore"}
    assert runtime.stopped is True
    assert proxy_events == ["start", "stop"]
    assert manual.os.environ["BBB_ANDROID_PUBLIC_PROXY"] == (
        "http://127.0.0.1:8899")
    assert manual.os.environ["BBB_ANDROID_NETWORK_GUARD"] == str(
        manual.GUARD_SCRIPT)
    output = capsys.readouterr().out
    assert "offline -> public" in output
    assert "public-only proxy" not in output  # fake proxy stays silent


def test_android_lock_reclaimer_removes_only_dead_pid_locks(
        monkeypatch, tmp_path, capsys):
    from scripts import android_locks

    alive = tmp_path / "leases" / "app" / "explore-alive.lock"
    # The unified namespace is where every session kind reserves a port now.
    unified = tmp_path / "port_locks" / "emulator-5554.lock"
    # Pre-unification reservations still have to be reported and reclaimed.
    stale = tmp_path / "manual_sessions" / "android_port_locks" / "port.lock"
    for path in (alive, unified, stale):
        path.parent.mkdir(parents=True, exist_ok=True)
    alive.write_text("111 explore", encoding="ascii")
    unified.write_text("333", encoding="ascii")
    stale.write_text("222", encoding="ascii")

    monkeypatch.setattr(android_locks.config, "ANDROID_TARGETS_DIR", tmp_path)
    monkeypatch.setattr(android_locks.config, "RUNS_DIR", tmp_path / "runs")
    monkeypatch.setattr(android_locks.config, "APP_OUTPUT_DIR",
                        tmp_path / "app_output")
    monkeypatch.setattr(android_locks, "_emulator_processes", lambda: [])
    monkeypatch.setattr(android_locks, "_pid_alive", lambda pid: pid == 111)
    monkeypatch.setattr(android_locks, "_remove_stale_pid_lock",
                        lambda path: path.unlink(missing_ok=True))
    monkeypatch.setattr(sys, "argv", ["android_locks.py", "--reclaim"])

    android_locks.main()

    assert alive.is_file()
    assert not stale.exists()
    assert not unified.exists()
    output = capsys.readouterr().out
    assert "[ALIVE] owner=111 kept" in output
    assert "[STALE] owner=222 removed" in output
    assert "[STALE] owner=333 removed" in output


def test_android_target_smoke_saves_three_frames_and_stops_runtime(
        monkeypatch, tmp_path):
    from scripts import android_target_smoke as smoke

    _OperatorRuntime.instances.clear()
    monkeypatch.setattr(smoke, "get_android_target", lambda _app: _target())
    monkeypatch.setattr(smoke, "AndroidEmulatorRuntime", _OperatorRuntime)
    monkeypatch.setattr(smoke.config, "ANDROID_TARGETS_DIR", tmp_path)
    monkeypatch.setattr(sys, "argv", [
        "android_target_smoke.py", "--app", "google_clock"])

    smoke.main()

    runtime = _OperatorRuntime.instances[-1]
    output = tmp_path / "smoke" / "google_clock"
    assert (output / "01_launch.png").read_bytes() == b"frame-1"
    assert (output / "02_after_tap.png").read_bytes() == b"frame-2"
    assert (output / "03_after_restart.png").read_bytes() == b"frame-3"
    assert runtime.calls[:2] == [
        ("tap", (120, 264)), ("restart_app", ())]
    assert runtime.stopped is True


def test_system_app_package_resolution_is_deterministic():
    from scripts.android_extract_system_app import _resolve_package

    emulator = SimpleNamespace(launchable_packages=lambda: [
        "com.example.calculator.tests", "com.example.calculator"])
    assert _resolve_package(emulator, "calculator", "") == (
        "com.example.calculator")
    assert _resolve_package(emulator, "ignored", "com.example.calculator") == (
        "com.example.calculator")
    with pytest.raises(SystemExit, match="no launchable package"):
        _resolve_package(emulator, "missing", "")


def test_dataset_rename_preserves_fields_and_protects_the_sample(
        monkeypatch, tmp_path):
    from benchmark.android import targets

    monkeypatch.setattr(targets.config, "ANDROID_TARGETS_DIR", tmp_path)
    apk = tmp_path / "source.apk"
    apk.write_bytes(b"dataset apk")
    original = targets.register_android_target(
        target_id="provisional_id", apk=apk, description="Stock clock",
        brief="只通过可见屏幕探索", package_name="com.google.android.deskclock",
        launch_activity="com.android.deskclock.DeskClock",
        orientation="portrait", network_policy="offline",
        protected_strings=["secret-value"])

    renamed = targets.rename_android_target("provisional_id", "google_clock")
    assert renamed.app_id == "google_clock"
    for field in ("package_name", "launch_activity", "orientation",
                  "network_policy", "brief", "apk_sha256", "protected_strings"):
        assert getattr(renamed, field) == getattr(original, field)
    assert renamed.apk.is_file()

    listed = [item["app_id"] for item in targets.list_android_targets()]
    assert "google_clock" in listed and "provisional_id" not in listed
    with pytest.raises(KeyError):
        targets.get_android_target("provisional_id")
    # The old artifact tree is reclaimed, the new one is populated.
    assert not (tmp_path / "artifacts" / "provisional_id").exists()
    assert (tmp_path / "artifacts" / "google_clock").is_dir()

    # The bundled sample is not an operator dataset and must stay immutable.
    for call in (lambda: targets.rename_android_target("android_commerce_demo",
                                                       "x"),
                 lambda: targets.unregister_android_target(
                     "android_commerce_demo")):
        with pytest.raises(ValueError):
            call()


def test_dataset_rename_refuses_collisions_and_bad_ids(monkeypatch, tmp_path):
    from benchmark.android import targets

    monkeypatch.setattr(targets.config, "ANDROID_TARGETS_DIR", tmp_path)
    for name in ("first_app", "second_app"):
        apk = tmp_path / f"{name}.apk"
        apk.write_bytes(name.encode("ascii"))
        targets.register_android_target(
            target_id=name, apk=apk, description=name,
            package_name="com.example.app", launch_activity=".Main")

    with pytest.raises(FileExistsError):
        targets.rename_android_target("first_app", "second_app")
    with pytest.raises(ValueError):
        targets.rename_android_target("first_app", "Bad Id")
    with pytest.raises(KeyError):
        targets.rename_android_target("missing_app", "third_app")
    # A failed rename must leave the original registered and usable.
    assert targets.get_android_target("first_app").app_id == "first_app"


def test_shipped_google_clock_checklist_is_loadable():
    """The formal checklist replaced the retired template skeleton.

    google_clock.template.json was deleted once review_specs/google_clock.json
    existed: both carried checklist_id "google_clock", which made the
    evaluation MCP list two entries of the same id. This test now pins the
    formal checklist (and that no template twin lingers).
    """
    from app_evaluation.checklist import Checklist

    root = Path(__file__).resolve().parent.parent
    formal = root / "review_specs" / "google_clock.json"
    checklist = Checklist.load(formal)
    checklist.expect_platform("android")
    assert checklist.checklist_id == "google_clock"
    assert len(checklist.features) == 25
    assert sum(1 for item in checklist.features if item["persistence"]) == 8
    assert not (root / "review_specs" /
                "google_clock.template.json").exists()
