"""Android runtime, target isolation, APK workspace and MCP surface tests."""
from __future__ import annotations

import hashlib
import json
import os
import subprocess
import zipfile
from contextlib import nullcontext
from pathlib import Path
from types import SimpleNamespace

from PIL import Image
import pytest

from app_reproduction import workspace as app_workspace
from app_reproduction.review import AndroidReproductionReview
from app_evaluation.checklist import Checklist
from app_evaluation.session import AppEvaluationSession
from agents.android_baseline import agent_runtime as baseline_agent_runtime
from agents.android_our_method import agent_runtime as ours_agent_runtime
from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android import runtime as android_runtime
from benchmark.android.toolchain import AndroidToolchain, load_toolchain_lock
from benchmark.android import targets
from benchmark import config as android_config
from benchmark.orchestrator.apps import list_apps
from benchmark.orchestrator.session import (
    Budget, Session, SessionClosed, TransientEnvironmentError)
from benchmark.runtime.base import RuntimeInfo
from benchmark.topology.models import Action, ActionType


def _png(path: Path | None = None, color=(20, 30, 40), size=(320, 640)) -> bytes:
    import io
    out = io.BytesIO()
    Image.new("RGB", size, color).save(out, "PNG")
    data = out.getvalue()
    if path:
        path.write_bytes(data)
    return data


class _MobileRuntime:
    def __init__(self):
        self.calls = []
        self.frame = _png()

    def info(self):
        return RuntimeInfo(320, 640, 1.0, "android", "portrait", 420)

    def screenshot(self): return self.frame
    def tap(self, *args): self.calls.append(("tap", args))
    def long_press(self, *args): self.calls.append(("long_press", args))
    def swipe(self, *args): self.calls.append(("swipe", args))
    def type_text(self, *args): self.calls.append(("type_text", args))
    def key(self, *args): self.calls.append(("key", args))
    def restart_app(self): self.calls.append(("restart_app", ()))
    def reset(self): self.calls.append(("reset", ()))
    def start(self): pass
    def stop(self): pass
    def list_tabs(self): return None


def _android_spec():
    return SimpleNamespace(app_id="android_test", seed="seed", kind="android",
                           platform="android", brief="test", precheck="")


def test_session_uses_dynamic_android_geometry_and_touch(tmp_path):
    runtime = _MobileRuntime()
    session = Session("sess_android", _android_spec(), runtime, tmp_path,
                      Budget(20, 60, 20))
    session._capture_frame(0, settle=False)
    observed = session.observe()
    assert observed["width"] == 320 and observed["height"] == 640
    assert observed["platform"] == "android"
    session.execute(Action(type=ActionType.TAP, x=10, y=20))
    session.execute(Action(type=ActionType.LONG_PRESS, x=30, y=40,
                           duration_ms=800))
    session.execute(Action(type=ActionType.SWIPE, x1=30, y1=500, x2=30, y2=100))
    session.execute(Action(type=ActionType.PRESS_BACK))
    session.execute(Action(type=ActionType.RESTART_APP))
    assert [call[0] for call in runtime.calls] == [
        "tap", "long_press", "swipe", "key", "restart_app"]
    try:
        session.execute(Action(type=ActionType.TAP, x=321, y=10))
        assert False, "out-of-bounds tap should fail"
    except ValueError as exc:
        assert "invalid_coordinates" in str(exc)
    try:
        session.execute(Action(type=ActionType.CLICK, x=10, y=20))
        assert False, "browser click should not be accepted on Android"
    except ValueError as exc:
        assert "unavailable on Android" in str(exc)
    session.close()


def test_android_runtime_translates_only_coordinate_commands(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    calls = []
    runtime._run = lambda *args, **kwargs: calls.append(args) or SimpleNamespace(
        stdout="", returncode=0)
    runtime._after_input = lambda: None
    runtime.tap(1, 2)
    runtime.long_press(3, 4, 900)
    runtime.swipe(5, 6, 7, 8, 450)
    runtime.type_text("hello world")
    runtime.key("Back")
    assert calls == [
        ("shell", "input", "tap", "1", "2"),
        ("shell", "input", "swipe", "3", "4", "3", "4", "900"),
        ("shell", "input", "swipe", "5", "6", "7", "8", "450"),
        ("shell", "input", "text", "hello%sworld"),
        ("shell", "input", "keyevent", "KEYCODE_BACK"),
    ]
    calls.clear()
    runtime.type_text("hello; id")
    assert calls == [("shell", "input", "text", "'hello;%sid'")]


def test_android_runtime_restores_unknown_or_foreign_foreground(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    responses = iter([
        SimpleNamespace(stdout="mCurrentFocus=null", returncode=0),
        SimpleNamespace(stdout="mResumedActivity: null", returncode=0),
        SimpleNamespace(stdout="mCurrentFocus=null", returncode=0),
        SimpleNamespace(
            stdout=("topResumedActivity=ActivityRecord{617e498 u0 "
                    "com.example.app/.MainActivity t8}"), returncode=0),
    ])
    runtime._run = lambda *args, **kwargs: next(responses)
    restarted = []
    runtime.restart_app = lambda: restarted.append(True)
    runtime._enforce_foreground()
    assert restarted == [True]


def test_android_runtime_accepts_android_35_resumed_activity(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    responses = iter([
        SimpleNamespace(stdout="", returncode=0),
        SimpleNamespace(
            stdout=("topResumedActivity=ActivityRecord{617e498 u0 "
                    "com.example.app/.MainActivity t8}"), returncode=0),
    ])
    runtime._run = lambda *args, **kwargs: next(responses)
    runtime.restart_app = lambda: (_ for _ in ()).throw(
        AssertionError("visible target must not be restarted"))
    runtime._enforce_foreground()


def test_android_runtime_stop_removes_explore_clone_but_keeps_login(
        monkeypatch, tmp_path):
    monkeypatch.setattr(android_config, "SCRATCH_DIR", tmp_path / "scratch")
    spec = SimpleNamespace(app_id="target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    explore = AndroidEmulatorRuntime(spec, tmp_path / "explore")
    explore._prepare_clone_root()
    (explore._avd_home / "disk.img").write_bytes(b"clone")
    explore.stop()
    assert not explore._avd_home.exists()

    login = AndroidEmulatorRuntime(spec, tmp_path / "login", lease_mode="login")
    login._prepare_clone_root()
    login._avd_home.mkdir(parents=True)
    (login._avd_home / "disk.img").write_bytes(b"profile")
    login.stop()
    assert (login._avd_home / "disk.img").is_file()


def test_android_screenshot_refreshes_orientation_and_density(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    landscape = _png(size=(640, 320))
    runtime._run = lambda *args, **kwargs: SimpleNamespace(
        stdout=(landscape if args[:2] == ("exec-out", "screencap") else
                "Physical density: 420\n"), returncode=0)
    assert runtime.screenshot() == landscape
    info = runtime.info()
    assert (info.width, info.height, info.orientation, info.density_dpi) == (
        640, 320, "landscape", 420)


def test_android_network_policy_is_enforced_inside_guest(monkeypatch, tmp_path):
    monkeypatch.delenv("BBB_ANDROID_NETWORK_GUARD", raising=False)
    monkeypatch.delenv("BBB_ANDROID_PUBLIC_PROXY", raising=False)
    spec = SimpleNamespace(network_policy="offline")
    runtime = AndroidEmulatorRuntime(spec, tmp_path / "offline")
    calls = []
    runtime._run = lambda *args, **kwargs: calls.append(args) or SimpleNamespace(
        stdout="0\n" if args == ("shell", "id", "-u") else "",
        returncode=0)
    runtime._configure_network()
    assert ("shell", "iptables", "-P", "OUTPUT", "DROP") in calls
    assert ("shell", "ip6tables", "-P", "OUTPUT", "DROP") in calls

    spec.network_policy = "public"; calls.clear()
    monkeypatch.setenv("BBB_ANDROID_NETWORK_GUARD", str(tmp_path / "guard"))
    monkeypatch.setenv("BBB_ANDROID_PUBLIC_PROXY", "http://127.0.0.1:8899")
    monkeypatch.setattr("benchmark.android.runtime.subprocess.run",
                        lambda *args, **kwargs: SimpleNamespace(returncode=0))
    runtime._configure_network()
    assert ("shell", "iptables", "-A", "OUTPUT", "-p", "udp", "-d",
            "10.0.2.3", "--dport", "53", "-j", "ACCEPT") in calls
    assert ("shell", "iptables", "-A", "OUTPUT", "-d",
            "10.0.0.0/8", "-j", "REJECT") in calls
    assert ("shell", "ip6tables", "-A", "OUTPUT", "-d",
            "fc00::/7", "-j", "REJECT") in calls


def test_android_runtime_retries_transient_adb_failures(tmp_path, monkeypatch):
    """A single dropped adb call must not end an exploration in progress.

    Long sessions issue thousands of adb calls and the emulator occasionally
    loses one (screencap exit -1, dumpsys timeout) while recovering at once.
    Observed in practice: sessions died at step 49 and step 2 for exactly this.
    """
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    runtime.serial = "emulator-5554"
    monkeypatch.setattr("benchmark.android.runtime.time.sleep", lambda _s: None)

    attempts = []

    def flaky(command, **kwargs):
        attempts.append(command)
        if "wait-for-device" in command:
            return SimpleNamespace(stdout="", returncode=0)
        real = [c for c in attempts if "wait-for-device" not in c]
        if len(real) == 1:                      # first try drops
            raise subprocess.CalledProcessError(4294967295, command)
        return SimpleNamespace(stdout="ok", returncode=0)

    monkeypatch.setattr("benchmark.android.runtime.subprocess.run", flaky)
    assert runtime._run("exec-out", "screencap", "-p").stdout == "ok"
    # It waited for the device between attempts instead of retrying blindly.
    assert any("wait-for-device" in command for command in attempts)

    # A persistent failure still surfaces, so real breakage is not hidden —
    # but as a neutral DeviceError, never as the raw adb command line.
    monkeypatch.setattr(
        "benchmark.android.runtime.subprocess.run",
        lambda command, **kwargs: (_ for _ in ()).throw(
            subprocess.CalledProcessError(1, command))
        if "wait-for-device" not in command else SimpleNamespace(returncode=0))
    with pytest.raises(android_runtime.DeviceError):
        runtime._run("exec-out", "screencap", "-p")


def test_android_restart_app_retries_before_reporting_failure(tmp_path,
                                                              monkeypatch):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    monkeypatch.setattr("benchmark.android.runtime.time.sleep", lambda _s: None)

    starts = []

    def responses(*args, **kwargs):
        if args[:3] == ("shell", "am", "start"):
            starts.append(args)
            if len(starts) == 1:                # window manager still busy
                return SimpleNamespace(
                    stdout="Error: Activity not started", returncode=0)
            return SimpleNamespace(stdout="Status: ok", returncode=0)
        return SimpleNamespace(stdout="", returncode=0)

    runtime._run = responses
    runtime.restart_app()
    assert len(starts) == 2                     # retried, did not raise

    starts.clear()
    runtime._run = lambda *args, **kwargs: SimpleNamespace(
        stdout="Error: Activity not started", returncode=0)
    with pytest.raises(RuntimeError, match="could not be relaunched"):
        runtime.restart_app()


def _responsive_runtime(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    runtime.serial = "emulator-5554"
    return runtime


def test_android_probe_silence_reads_recoverable_not_terminal(tmp_path,
                                                              monkeypatch):
    """A starved probe must not read as a dead device.

    Under host memory pressure `adb get-state` times out at the same moment
    as the command it is probing for; treating that silence as "device
    gone" is what failed four healthy-but-starved sessions on 2026-09-01.
    The probe's own silence is conservatively recoverable.
    """
    runtime = _responsive_runtime(tmp_path)
    monkeypatch.setattr("benchmark.android.runtime.time.sleep", lambda _s: None)

    def starved(command, **kwargs):
        raise subprocess.TimeoutExpired(command, timeout=15)
    monkeypatch.setattr("benchmark.android.runtime.subprocess.run", starved)
    assert runtime._device_responsive() is True


def test_android_explicit_non_device_state_is_terminal(tmp_path, monkeypatch):
    """adb answering a non-device state while qemu lives is terminal."""
    runtime = _responsive_runtime(tmp_path)
    monkeypatch.setattr("benchmark.android.runtime.time.sleep", lambda _s: None)
    monkeypatch.setattr(
        "benchmark.android.runtime.subprocess.run",
        lambda command, **kwargs: SimpleNamespace(
            stdout="offline", stderr="", returncode=0))
    assert runtime._device_responsive() is False


def test_android_probe_recovering_to_device_is_healthy(tmp_path, monkeypatch):
    """One offline answer followed by "device" reads recoverable."""
    runtime = _responsive_runtime(tmp_path)
    monkeypatch.setattr("benchmark.android.runtime.time.sleep", lambda _s: None)
    answers = iter(["offline", "device"])
    monkeypatch.setattr(
        "benchmark.android.runtime.subprocess.run",
        lambda command, **kwargs: SimpleNamespace(
            stdout=next(answers), stderr="", returncode=0))
    assert runtime._device_responsive() is True


def test_android_dead_emulator_process_is_terminal(tmp_path, monkeypatch):
    runtime = _responsive_runtime(tmp_path)
    runtime.process = SimpleNamespace(poll=lambda: 1)
    monkeypatch.setattr(
        "benchmark.android.runtime.subprocess.run",
        lambda command, **kwargs: SimpleNamespace(
            stdout="device", stderr="", returncode=0))
    assert runtime._device_responsive() is False


def test_android_runtime_types_non_ascii_without_failing_the_session(tmp_path):
    """CJK input must not kill the session.

    `adb shell input text` resolves characters through the device
    KeyCharacterMap, which has no CJK entries, so a CJK argument answers with
    `NullPointerException` and exit 255 regardless of quoting or escaping
    (scripts/android_typing_diagnose.py). That used to surface as a runtime
    failure and end the whole exploration. It is now refused as an invalid
    action (ValueError -> 400), pointing the Agent at the on-screen keyboard,
    which taps can reach.
    """
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    calls = []
    runtime._run = lambda *args, **kwargs: calls.append(args) or SimpleNamespace(
        stdout="", returncode=0)
    runtime._after_input = lambda: None

    runtime.type_text("Shanghai")
    assert calls == [("shell", "input", "text", "Shanghai")]

    # A refusal must be recoverable, must not touch the device, and must send
    # the Agent somewhere that actually works.
    for rejected in ("\u5317\u4eac", "a\u4e2d"):
        calls.clear()
        with pytest.raises(ValueError, match="on-screen keyboard"):
            runtime.type_text(rejected)
        assert calls == [], "a refusal must not spend adb calls"


def _locale_runtime(tmp_path, monkeypatch, locale, current="en-US"):
    monkeypatch.setattr(android_config, "ANDROID_LOCALE", locale)
    runtime = AndroidEmulatorRuntime(SimpleNamespace(network_policy="offline"),
                                     tmp_path)
    calls = []

    def fake_run(*args, **kwargs):
        calls.append(args)
        if args == ("shell", "getprop", "persist.sys.locale"):
            return SimpleNamespace(stdout=f"{current}\n", returncode=0)
        if args[:3] == ("shell", "cmd", "package"):
            return SimpleNamespace(
                stdout="package:/system/framework/framework-res.apk\n",
                returncode=0)
        return SimpleNamespace(stdout="", returncode=0)

    runtime._run = fake_run
    runtime._wait_until_booted = lambda: None
    return runtime, calls


def test_android_locale_is_pinned_before_apk_install(tmp_path, monkeypatch):
    runtime, calls = _locale_runtime(tmp_path, monkeypatch, "zh-CN")
    runtime._apply_locale()
    assert ("shell", "setprop", "persist.sys.locale", "zh-CN") in calls
    assert ("shell", "settings", "put", "system", "system_locales",
            "zh-CN") in calls
    # The framework is restarted, never the emulator, and the package manager is
    # awaited so the install that follows cannot race it.
    assert ("shell", "stop") in calls and ("shell", "start") in calls
    assert any(args[:3] == ("shell", "cmd", "package") for args in calls)


def test_android_locale_skipped_when_already_correct(tmp_path, monkeypatch):
    runtime, calls = _locale_runtime(tmp_path, monkeypatch, "zh-CN",
                                     current="zh-CN")
    runtime._apply_locale()
    assert ("shell", "stop") not in calls
    assert not any(args[:2] == ("shell", "setprop") for args in calls)


def test_android_locale_empty_keeps_image_default_and_rejects_junk(
        tmp_path, monkeypatch):
    runtime, calls = _locale_runtime(tmp_path, monkeypatch, "")
    runtime._apply_locale()
    assert calls == []
    # The value reaches an adb shell command, so it must be validated.
    runtime, _ = _locale_runtime(tmp_path / "b", monkeypatch, "zh-CN; rm -rf /")
    with pytest.raises(ValueError):
        runtime._apply_locale()


def test_android_login_lease_blocks_exploration_but_explorers_can_parallel(
        monkeypatch, tmp_path):
    monkeypatch.setattr(targets.config, "ANDROID_TARGETS_DIR", tmp_path / "targets")
    # runtime.py and targets.py reference the same config module object
    spec = SimpleNamespace(app_id="same_target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    first = AndroidEmulatorRuntime(spec, tmp_path / "one")
    second = AndroidEmulatorRuntime(spec, tmp_path / "two")
    login = AndroidEmulatorRuntime(spec, tmp_path / "login", lease_mode="login")
    first._acquire_target_lease(); second._acquire_target_lease()
    try:
        # Two live runtimes in one process must never share a lease file name.
        assert len(list((tmp_path / "targets" / "leases" / "same_target").glob(
            "explore-*.lock"))) == 2
        try:
            login._acquire_target_lease()
            assert False, "login must not overlap exploration"
        except RuntimeError as exc:
            assert "cannot overlap" in str(exc)
    finally:
        first._release_target_lease(); second._release_target_lease()
    login._acquire_target_lease()
    try:
        try:
            first._acquire_target_lease()
            assert False, "exploration must not overlap login"
        except RuntimeError as exc:
            assert "maintenance" in str(exc)
    finally:
        login._release_target_lease()


def test_android_port_reservation_uses_one_namespace_for_every_session_kind(
        monkeypatch, tmp_path):
    """Every session kind must compete in the same reservation directory.

    The namespace used to be derived from ``work_dir``, whose nesting depth
    differs per session kind, so an exploration and a manual session could both
    believe they owned emulator-5554 and then address each other's device.
    """
    monkeypatch.setattr(android_config, "ANDROID_TARGETS_DIR",
                        tmp_path / "targets")
    monkeypatch.setattr("benchmark.android.runtime._port_pair_free",
                        lambda _port: True)
    spec = SimpleNamespace(app_id="target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    work_dirs = [
        tmp_path / "runs" / "sess_a" / "runtime",                  # exploration
        tmp_path / "targets" / "manual_sessions" / "m1" / "runtime",
        tmp_path / "targets" / "smoke" / "google_clock" / "runtime",
        tmp_path / "app_output" / "evaluations" / "e1" / "runtime",
        tmp_path / "app_output" / "h1" / "review" / "round_01" / "runtime",
    ]
    runtimes = [AndroidEmulatorRuntime(spec, path) for path in work_dirs]
    ports = [runtime._reserve_port() for runtime in runtimes]
    try:
        assert len(set(ports)) == len(ports), "reservations must not collide"
        locks = {runtime._port_lock.parent for runtime in runtimes}
        assert locks == {tmp_path / "targets" / "port_locks"}
    finally:
        for runtime in runtimes:
            runtime._port_lock.unlink(missing_ok=True)


def test_android_port_reservation_skips_ports_an_orphan_emulator_still_holds(
        monkeypatch, tmp_path):
    """A stale lock file does not prove the emulator behind it is gone."""
    monkeypatch.setattr(android_config, "ANDROID_TARGETS_DIR",
                        tmp_path / "targets")
    busy = {5554, 5556}
    monkeypatch.setattr("benchmark.android.runtime._port_pair_free",
                        lambda port: port not in busy)
    spec = SimpleNamespace(app_id="target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path / "runs" / "s" / "runtime")
    assert runtime._reserve_port() == 5558
    # A rejected candidate must not leave its reservation file behind.
    locks = tmp_path / "targets" / "port_locks"
    assert not (locks / "emulator-5554.lock").exists()
    assert (locks / "emulator-5558.lock").is_file()
    runtime._port_lock.unlink()


def test_android_start_refuses_a_host_without_memory_headroom(monkeypatch):
    """Host exhaustion must be an explicit refusal, not a random adb fault."""
    monkeypatch.setattr(android_config, "ANDROID_MIN_FREE_MEMORY_MB", 8192)
    monkeypatch.setattr(android_runtime, "available_memory_mb", lambda: 2048)
    with pytest.raises(RuntimeError, match="insufficient free memory"):
        android_runtime._require_memory_headroom()
    # A host that cannot report its memory must never be blocked.
    monkeypatch.setattr(android_runtime, "available_memory_mb", lambda: None)
    android_runtime._require_memory_headroom()
    # Enough headroom, and the operator override, both pass.
    monkeypatch.setattr(android_runtime, "available_memory_mb", lambda: 9000)
    android_runtime._require_memory_headroom()
    monkeypatch.setattr(android_config, "ANDROID_MIN_FREE_MEMORY_MB", 0)
    monkeypatch.setattr(android_runtime, "available_memory_mb", lambda: 1)
    android_runtime._require_memory_headroom()


def test_android_available_memory_probe_works_on_this_host():
    value = android_runtime.available_memory_mb()
    assert value is None or value > 0


class _FailingRuntime(_MobileRuntime):
    """Screenshots succeed until `broken`, mimicking a transient adb blip."""

    def __init__(self):
        super().__init__()
        self.broken = False
        self.stopped = False
        self.healthy = True

    def screenshot(self):
        if self.broken:
            raise RuntimeError("emulator returned an invalid screenshot")
        return self.frame

    def health(self):
        return self.healthy

    def stop(self):
        self.stopped = True


def test_android_observe_failure_is_survivable_or_terminal_by_device_health(
        tmp_path):
    """A capture failure ends the session only when the device is truly gone.

    Observation drives dumpsys and screencap, so it is where a device hiccup
    surfaces. A healthy device that drops one frame costs the Agent that
    observation, not 85 steps of work; an unusable device ends the session and
    must hand back the emulator, target lease and port reservation at once.
    """
    runtime = _FailingRuntime()
    session = Session("sess_android", _android_spec(), runtime, tmp_path,
                      Budget(20, 60, 20))
    session.observe()

    # Healthy device, one bad frame: recoverable, session lives on.
    runtime.broken = True
    runtime.healthy = True
    used_before = session.budget.observations_used
    with pytest.raises(TransientEnvironmentError):
        session.observe()
    assert session.status == "running"
    assert runtime.stopped is False
    # A blip must not silently consume the observation budget.
    assert session.budget.observations_used == used_before
    runtime.broken = False
    assert session.observe()["frame_id"] is not None

    # Device gone: terminal, and everything it held is handed back.
    runtime.broken = True
    runtime.healthy = False
    with pytest.raises(SessionClosed):
        session.observe()
    assert session.status == "failed"
    assert runtime.stopped is True
    assert (tmp_path / "crash_report.json").is_file()
    assert json.loads((tmp_path / "session.json").read_text(
        encoding="utf-8"))["status"] == "failed"


def test_volatile_clones_and_profiles_never_live_in_the_repository(
        monkeypatch, tmp_path):
    """A session's AVD clone must not sit under runs/.

    An AVD clone is several GB of disk images; a Chromium user-data dir is
    hundreds of cache files. Creating them inside the session directory meant
    every teardown had to delete thousands of workspace files -- slow, and it
    trips bulk-delete protection -- and left 7.8 GB behind when it failed.
    `runs/` is the evidence tree; volatile runtime state belongs outside it.
    """
    monkeypatch.setattr(android_config, "SCRATCH_DIR", tmp_path / "scratch")
    spec = SimpleNamespace(app_id="target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path / "runs" / "sess_a" / "runtime")
    runtime._prepare_clone_root()
    assert (tmp_path / "scratch") in runtime._avd_home.parents
    assert (tmp_path / "runs") not in runtime._avd_home.parents
    # The owning PID is in the name; that is what makes unattended reclaim work.
    assert str(os.getpid()) in runtime._avd_home.name

    # Login maintenance is the deliberate exception: the operator copies the
    # golden profile out of that clone, so it keeps a stable, findable path.
    login = AndroidEmulatorRuntime(spec, tmp_path / "login_work",
                                   lease_mode="login")
    login._prepare_clone_root()
    assert login._avd_home == tmp_path / "login_work" / "avd_home"


def test_orphan_clone_is_reclaimed_when_its_owner_is_gone(monkeypatch, tmp_path):
    """A crashed session cannot delete its own clone, so the next start does."""
    monkeypatch.setattr(android_config, "SCRATCH_DIR", tmp_path / "scratch")
    dead = tmp_path / "scratch" / "avd_sess-dead_999999_abcd"
    live = tmp_path / "scratch" / "avd_sess-live_1234_efgh"
    for path in (dead, live):
        path.mkdir(parents=True)
        (path / "userdata.img").write_bytes(b"disk")

    monkeypatch.setattr("benchmark.scratch.pid_alive", lambda pid: pid == 1234)
    reclaimed = android_runtime.reclaim_orphan_clones()
    assert [path for path, _ in reclaimed] == [dead]
    assert not dead.exists()
    # A clone whose owner is still running is never touched.
    assert live.is_dir()


def test_parallel_output_domains_fail_closed_instead_of_colliding(
        monkeypatch, tmp_path):
    """Two Agents working on one target must never share an output domain.

    Reproduction handoffs and evaluation runs both derive a directory name from
    a random suffix. What makes that safe is not the entropy but the exclusive
    create: a collision must abort loudly, never silently reuse or overwrite
    another Agent's deliverables.
    """
    output_root = tmp_path / "app_output"
    (output_root / "sess_a-abc123").mkdir(parents=True)
    with pytest.raises(FileExistsError):
        (output_root / "sess_a-abc123").mkdir(parents=False, exist_ok=False)

    # The evaluation session applies the same rule to its own run directory.
    apk = tmp_path / "app.apk"
    apk.write_bytes(b"apk")
    checklist = Checklist.from_object(
        {"platform": "android", "features": [{"id": "a", "name": "A"}]},
        default_id="fixture")
    first = AppEvaluationSession(
        apk=apk, checklist=checklist, output_root=tmp_path / "evaluations",
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    assert first.output_dir.is_dir()
    monkeypatch.setattr(
        "app_evaluation.session.time.time_ns", lambda: 1)
    second = AppEvaluationSession(
        apk=apk, checklist=checklist, output_root=tmp_path / "evaluations",
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    assert second.output_dir != first.output_dir
    with pytest.raises(FileExistsError):
        AppEvaluationSession(
            apk=apk, checklist=checklist, output_root=tmp_path / "evaluations",
            runtime_factory=lambda spec, directory: _ReviewRuntime())


def test_device_failures_never_disclose_adb_to_an_agent(tmp_path):
    """A device failure must not leak the machinery behind the pixels boundary.

    A raw adb failure reads
    ``CalledProcessError(4294967295, ['.../platform-tools/adb.exe', '-s',
    'emulator-5554', 'exec-out', 'screencap', '-p'])``. That text used to travel
    out through the controller's error detail into the Agent's tool result,
    disclosing that ADB exists, what it was asked to do, the emulator serial and
    the host's directory layout. ADB must stay wholly inside the trusted
    runtime.
    """
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    runtime.serial = "emulator-5554"
    runtime._device_responsive = lambda: True

    def always_fails(*args, **kwargs):
        raise subprocess.CalledProcessError(
            4294967295, [str(tmp_path / "platform-tools" / "adb.exe"),
                         "-s", "emulator-5554", "exec-out", "screencap", "-p"])

    monkeypatched = subprocess.run
    try:
        subprocess.run = always_fails
        with pytest.raises(android_runtime.DeviceError) as caught:
            runtime._run("exec-out", "screencap", "-p", retries=0)
    finally:
        subprocess.run = monkeypatched

    message = str(caught.value)
    for forbidden in ("adb", "emulator-5554", "screencap", "platform-tools",
                      str(tmp_path), "CalledProcessError", "4294967295"):
        assert forbidden not in message, f"{forbidden!r} leaked to the Agent"
    # The cause is still available locally, for the crash report only.
    assert "exited" in caught.value.detail
    # A live device makes this recoverable: one blip must not end a session.
    assert caught.value.recoverable is True


def test_screenshot_blip_is_recoverable_while_the_device_lives(tmp_path):
    spec = SimpleNamespace(package_name="com.example.app",
                           launch_activity=".MainActivity")
    runtime = AndroidEmulatorRuntime(spec, tmp_path)
    runtime.serial = "emulator-5554"
    runtime._enforce_foreground = lambda: None
    runtime._run = lambda *a, **k: SimpleNamespace(stdout=b"not-a-png",
                                                   returncode=0)
    runtime._device_responsive = lambda: True
    with pytest.raises(android_runtime.DeviceError) as caught:
        runtime.screenshot()
    assert caught.value.recoverable is True
    assert "emulator" not in str(caught.value)

    # A device that has stopped answering is terminal, not a blip.
    runtime._device_responsive = lambda: False
    with pytest.raises(android_runtime.DeviceError) as caught:
        runtime.screenshot()
    assert caught.value.recoverable is False


def test_memory_admission_reserves_headroom_for_booting_emulators(
        monkeypatch, tmp_path):
    """Parallel starts must not both pass the same memory check.

    A booting emulator has not claimed its memory yet, so two sessions starting
    together would each see enough room and then jointly overcommit the host —
    which is exactly the condition that makes adb drop commands at random.
    """
    monkeypatch.setattr(android_config, "ANDROID_TARGETS_DIR",
                        tmp_path / "targets")
    monkeypatch.setattr(android_config, "ANDROID_MIN_FREE_MEMORY_MB", 5120)
    monkeypatch.setattr(android_config, "ANDROID_EMULATOR_MEMORY_MB", 3072)
    monkeypatch.setattr(android_runtime, "available_memory_mb", lambda: 8000)

    first = android_runtime._require_memory_headroom()
    assert first is not None and first.is_file()

    # 8000 - 3072 reserved = 4928 < 5120 required: the second start is refused
    # instead of being allowed to overcommit.
    with pytest.raises(RuntimeError, match="still booting"):
        android_runtime._require_memory_headroom()

    # Once the first emulator is resident its footprint shows in the host
    # figures, so the reservation is released and a second start may proceed.
    first.unlink()
    second = android_runtime._require_memory_headroom()
    assert second is not None
    second.unlink()


def test_topology_ids_keep_non_latin_names_readable():
    """Ids must stay referenceable when the UI language is not Latin.

    The slug pattern used to be ASCII-only, so every Chinese state name
    collapsed to `state_unnamed_N`. With the guest UI pinned to zh-CN that left
    an Agent referencing its own states by number when recording transitions —
    a usability defect the platform imposed on the Agent.
    """
    from benchmark.topology.store import _slug

    assert _slug("闹钟列表") == "闹钟列表"
    assert _slug("Alarm List") == "alarm_list"
    assert _slug("世界时钟 / World") == "世界时钟_world"
    # Punctuation-only names still degrade gracefully rather than crash.
    assert _slug("!!!") == "unnamed"
    assert _slug("") == "unnamed"


def test_operator_apk_registry_hides_sensitive_metadata(monkeypatch, tmp_path):
    root = tmp_path / "targets"
    monkeypatch.setattr(targets.config, "ANDROID_TARGETS_DIR", root)
    apk = tmp_path / "input.apk"; apk.write_bytes(b"fake apk")
    spec = targets.register_android_target(
        target_id="local_demo", apk=apk, description="Local demo",
        package_name="com.example.demo", launch_activity=".MainActivity",
        protected_strings=["private@example.com"])
    assert spec.apk.is_file() and spec.apk != apk
    listing = next(item for item in targets.list_android_targets()
                   if item["app_id"] == "local_demo")
    assert "apk_path" not in listing and "package_name" not in listing
    assert "apk_sha256" not in listing and spec.apk_sha256 == hashlib.sha256(b"fake apk").hexdigest()
    registry_text = (root / "targets.json").read_text(encoding="utf-8")
    assert "private@example.com" in registry_text
    assert str(root) not in json.dumps(listing)

    try:
        targets.register_android_target(
            target_id="bad_target", apk=apk, description="bad",
            package_name="not-a-package", launch_activity=".MainActivity")
        assert False, "invalid metadata must reject registration"
    except ValueError:
        pass
    assert not (root / "artifacts" / "bad_target").exists()


def test_web_target_listing_remains_frozen_by_default():
    web = list_apps()
    assert web and all(item["platform"] == "web" for item in web)
    android = list_apps("android")
    assert all(item["platform"] == "android" for item in android)


def test_android_handoff_redacts_text_and_blurs_regions(tmp_path):
    note = tmp_path / "note.md"; note.write_text("account Secret Name", encoding="utf-8")
    frame = tmp_path / "frame.png"
    patterned = Image.new("RGB", (100, 100), (255, 255, 255))
    for x in range(0, 50, 4):
        for y in range(0, 50, 4):
            patterned.putpixel((x, y), (0, 0, 0))
    patterned.save(frame, "PNG")
    bundle = app_workspace._create_handoff_bundle(
        "handoff", "android-baseline",
        {"note.md": note, "screenshots/frame.png": frame},
        ["Secret Name"], [{"x": 0, "y": 0, "width": 50, "height": 50}])
    try:
        assert "Secret Name" not in (bundle / "note.md").read_text(encoding="utf-8")
        assert (bundle / "screenshots/frame.png").read_bytes() != frame.read_bytes()
        with Image.open(bundle / "screenshots/frame.png") as redacted:
            assert redacted.getpixel((10, 10)) == (24, 24, 24)
            assert redacted.getpixel((40, 40)) == (24, 24, 24)
        manifest = json.loads((bundle / "manifest.json").read_text(encoding="utf-8"))
        assert manifest["privacy_filtered"] is True
    finally:
        app_workspace._remove_bundle(bundle, "handoff")

    forbidden = tmp_path / "target.apk"; forbidden.write_bytes(b"apk")
    try:
        app_workspace._create_handoff_bundle(
            "blocked", "android-baseline", {"target.apk": forbidden}, [], [])
        assert False, "APK must never enter the exploration handoff"
    except ValueError as exc:
        assert "visible frames and text" in str(exc)


def test_android_workspace_mounts_only_project_and_filtered_inputs(
        monkeypatch, tmp_path):
    scaffold = tmp_path / "scaffold"; scaffold.mkdir()
    (scaffold / "settings.gradle.kts").write_text("rootProject.name='x'", encoding="utf-8")
    common = tmp_path / "common"; common.mkdir()
    mobile = tmp_path / "mobile"; mobile.mkdir()
    topology = tmp_path / "functional_topology.json"
    topology.write_text('{"nodes":[],"private":"Secret"}', encoding="utf-8")
    frame = tmp_path / "frame.png"; _png(frame)
    calls = []
    monkeypatch.setattr(app_workspace, "SCAFFOLD_ROOT", scaffold)
    monkeypatch.setattr(app_workspace, "OUTPUT_ROOT", tmp_path / "app_output")
    monkeypatch.setattr(app_workspace, "IMAGE_CONTEXT", tmp_path / "image")
    monkeypatch.setattr(app_workspace, "ensure_docker_available", lambda: "ok")
    monkeypatch.setattr(app_workspace, "ensure_materials", lambda: common)
    monkeypatch.setattr(app_workspace, "ensure_app_materials", lambda: mobile)
    monkeypatch.setattr(app_workspace, "_docker",
                        lambda *args, **kwargs: calls.append(args) or
                        SimpleNamespace(returncode=0, stdout="", stderr=""))
    ws = app_workspace.AppReproductionWorkspace.start(
        source_mode="android-baseline", source_id="sess_android",
        topology_path=topology,
        exploration_files={"screenshots/frame.png": frame},
        protected_strings=["Secret"])
    try:
        run = next(call for call in calls if call and call[0] == "run")
        command = "\n".join(run)
        assert "--network\nnone" in command
        assert f"source={ws.project_dir},target=/workspace" in command
        assert "target=/materials/common,readonly" in command
        assert "target=/materials/mobile,readonly" in command
        assert "target=/input/functional_topology.json,readonly" in command
        assert "Secret" not in ws.topology_path.read_text(encoding="utf-8")
        assert str(tmp_path / "input.apk") not in command
        assert ws.output_dir.parent == (tmp_path / "app_output").resolve()
    finally:
        ws.close()


def test_android_apk_archive_gate_rejects_nested_apk_and_private_text(tmp_path):
    ws = _workspace(tmp_path)
    ws.protected_strings = ["Private Person"]
    clean = ws.artifacts_dir / "clean.apk"
    with zipfile.ZipFile(clean, "w") as archive:
        archive.writestr("AndroidManifest.xml", b"manifest")
        archive.writestr("classes.dex", b"dex")
    ws._apk_archive_gate(clean)

    nested = ws.artifacts_dir / "nested.apk"
    with zipfile.ZipFile(nested, "w") as archive:
        archive.writestr("AndroidManifest.xml", b"manifest")
        archive.writestr("classes.dex", b"dex")
        archive.writestr("assets/target.apk", b"forbidden")
    try:
        ws._apk_archive_gate(nested)
        assert False, "nested APK should fail"
    except ValueError as exc:
        assert "embeds another APK" in str(exc)

    leaked = ws.artifacts_dir / "leaked.apk"
    with zipfile.ZipFile(leaked, "w") as archive:
        archive.writestr("AndroidManifest.xml", b"manifest")
        archive.writestr("classes.dex", "Private Person".encode("utf-16le"))
    try:
        ws._apk_archive_gate(leaked)
        assert False, "private text should fail"
    except ValueError as exc:
        assert "protected target information" in str(exc)


def test_android_image_builder_uses_pinned_cached_base(monkeypatch):
    calls = []

    def fake_docker(*args, **kwargs):
        calls.append(args)
        return SimpleNamespace(returncode=0, stdout="", stderr="")

    monkeypatch.setattr(app_workspace, "ensure_docker_available", lambda: "29")
    monkeypatch.setattr(app_workspace, "lock_for", lambda _: nullcontext())
    monkeypatch.setattr(app_workspace, "_docker", fake_docker)
    app_workspace.build_android_build_image(accept_licenses=True)
    assert calls[0] == ("image", "inspect", app_workspace.GRADLE_BASE_IMAGE)
    assert not any(call[0] == "pull" for call in calls)
    assert calls[-1][0] == "build"
    dockerfile = (app_workspace.IMAGE_CONTEXT / "agent_image" /
                  "Dockerfile").read_text(encoding="utf-8")
    assert app_workspace.GRADLE_BASE_IMAGE.split("@", 1)[1] in dockerfile


class _ReviewRuntime(_MobileRuntime):
    def __init__(self, *_):
        super().__init__(); self.counter = 0; self.started = False

    def start(self): self.started = True
    def stop(self): self.started = False
    def screenshot(self):
        self.counter += 1
        return _png(color=(self.counter * 20 % 255, 30, 40))


def _workspace(tmp_path):
    output = tmp_path / "app_output" / "handoff"
    project = output / "project"; review = output / "review"; artifacts = output / "artifacts"
    project.mkdir(parents=True); review.mkdir(); artifacts.mkdir()
    (project / "MainActivity.kt").write_text("fun main() {}", encoding="utf-8")
    ws = app_workspace.AppReproductionWorkspace(
        "handoff", "android-our-method", "sess", tmp_path / "topology.json",
        output, project, review, artifacts, "container", None, [], running=False)
    def build():
        apk = artifacts / "app-debug.apk"; apk.write_bytes(b"apk")
        return {"apk": "artifacts/app-debug.apk", "sha256": hashlib.sha256(b"apk").hexdigest()}
    ws.build_apk = build
    return ws


def test_android_review_requires_restart_persistence_for_ours(tmp_path):
    ws = _workspace(tmp_path)
    review = AndroidReproductionReview(
        ws, require_write_evidence=True,
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    review.start_round()  # observation 1
    review.action("tap", {"x": 10, "y": 20})
    review.observe()      # observation 2
    review.action("restart_app", {})
    review.observe()      # observation 3
    result = review.complete_round(
        decision="accept", checked_flows=["favorite"],
        findings=["favorite remains after restart"],
        write_flow_evidence=[{"flow": "favorite",
                              "before_observation": 1,
                              "after_observation": 2,
                              "persisted_observation": 3}])
    assert result["accepted"] is True
    review.ensure_accepted()


def test_android_review_rejects_non_write_and_back_without_reentry(tmp_path):
    ws = _workspace(tmp_path)
    review = AndroidReproductionReview(
        ws, require_write_evidence=True,
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    review.start_round()
    review.action("wait", {"ms": 0})
    review.observe()
    review.action("restart_app", {})
    review.observe()
    with pytest.raises(ValueError, match="write-class"):
        review.complete_round(
            decision="accept", checked_flows=["save"], findings=["changed"],
            write_flow_evidence=[{
                "flow": "save", "before_observation": 1,
                "after_observation": 2, "persisted_observation": 3}])
    review.close()

    ws = _workspace(tmp_path / "second")
    review = AndroidReproductionReview(
        ws, require_write_evidence=True,
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    review.start_round()
    review.action("tap", {"x": 10, "y": 20})
    review.observe()
    review.action("press_back", {})
    review.observe()
    with pytest.raises(ValueError, match="followed by a visible re-entry"):
        review.complete_round(
            decision="accept", checked_flows=["save"], findings=["changed"],
            write_flow_evidence=[{
                "flow": "save", "before_observation": 1,
                "after_observation": 2, "persisted_observation": 3}])
    review.close()


def test_android_review_does_not_store_typed_private_text(tmp_path):
    ws = _workspace(tmp_path)
    review = AndroidReproductionReview(
        ws, runtime_factory=lambda spec, directory: _ReviewRuntime())
    review.start_round()
    review.action("type_text", {"text": "private@example.test"})
    assert review.actions == [{"type": "type_text"}]
    assert "private@example.test" not in json.dumps(review.summary())
    review.close()


def test_android_visual_privacy_rejects_transformed_frames_and_review_patches(
        tmp_path):
    original = tmp_path / "original.png"
    image = Image.new("RGB", (120, 160), (230, 230, 230))
    for x in range(10, 70, 3):
        for y in range(20, 80, 4):
            image.putpixel((x, y), ((x * 3) % 255, (y * 5) % 255, 40))
    image.save(original)
    regions = [{"x": 10, "y": 20, "width": 60, "height": 60}]
    frames, patches = app_workspace._source_visual_signatures(
        {"frame.png": original}, regions)
    bundle = app_workspace._create_handoff_bundle(
        "visual", "android-our-method", {"frame.png": original}, [], regions)
    ws = _workspace(tmp_path / "workspace")
    ws.artifact_dir = bundle
    ws.source_visual_signatures = frames
    ws.protected_patch_signatures = patches
    ws.protected_regions = regions
    transformed = ws.project_dir / "app" / "src" / "main" / "res" / "drawable"
    transformed.mkdir(parents=True)
    with Image.open(original) as source:
        source.resize((240, 320)).save(transformed / "copied.jpg", quality=92)
    try:
        with pytest.raises(ValueError, match="copied or transformed"):
            ws._handoff_copy_gate()
        (transformed / "copied.jpg").unlink()
        review_frame = ws.review_dir / "round_01" / "observation_001.png"
        review_frame.parent.mkdir(parents=True)
        Image.open(original).save(review_frame)
        with pytest.raises(ValueError, match="protected target pixels"):
            ws._review_visual_privacy_gate()
    finally:
        app_workspace._remove_bundle(bundle, "visual")
        ws.artifact_dir = None


def test_android_toolchain_lock_rejects_revision_drift(tmp_path):
    lock = load_toolchain_lock()
    sdk = tmp_path / "sdk"
    paths = {
        "platform-tools": sdk / "platform-tools" / "source.properties",
        "emulator": sdk / "emulator" / "source.properties",
        "platforms;android-35": sdk / "platforms" / "android-35" / "source.properties",
        "build-tools;35.0.0": sdk / "build-tools" / "35.0.0" / "source.properties",
        "system-images;android-35;google_apis;x86_64": sdk / "system-images" /
            "android-35" / "google_apis" / "x86_64" / "source.properties",
    }
    for package, path in paths.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"Pkg.Revision={lock['sdk_packages'][package]}\n",
                        encoding="utf-8")
    tools = sdk / "cmdline-tools" / "latest" / "source.properties"
    tools.parent.mkdir(parents=True)
    tools.write_text(
        f"Pkg.Revision={lock['command_line_tools']['revision']}\n",
        encoding="utf-8")
    avd = tmp_path / "avd" / "BBB_Base.avd"
    avd.mkdir(parents=True)
    (avd / "config.ini").write_text(
        "abi.type=x86_64\ntag.id=google_apis\nhw.device.name=pixel_6\n",
        encoding="utf-8")
    (avd.parent / "BBB_Base.ini").write_text("target=android-35\n",
                                               encoding="utf-8")
    toolchain = AndroidToolchain(sdk, sdk / "platform-tools" / "adb",
                                 sdk / "emulator" / "emulator", avd, None)
    assert toolchain.version_mismatches() == []
    paths["emulator"].write_text("Pkg.Revision=0\n", encoding="utf-8")
    assert any("emulator revision" in item
               for item in toolchain.version_mismatches())


def test_android_network_guard_verifies_complete_ordered_policy():
    from scripts.android_network_guard import verify_rules
    from benchmark.android.network_policy import PUBLIC_BLOCK_V4, PUBLIC_BLOCK_V6

    ipv4_lines = [
        "-P OUTPUT ACCEPT",
        "-A OUTPUT -d 10.0.2.2/32 -p tcp --dport 8899 -j ACCEPT",
        "-A OUTPUT -d 10.0.2.3/32 -p udp --dport 53 -j ACCEPT",
        "-A OUTPUT -d 10.0.2.3/32 -p tcp --dport 53 -j ACCEPT",
    ] + [f"-A OUTPUT -d {network} -j REJECT" for network in PUBLIC_BLOCK_V4]
    ipv6_lines = ["-P OUTPUT ACCEPT"] + [
        f"-A OUTPUT -d {network} -j REJECT" for network in PUBLIC_BLOCK_V6]
    verified = verify_rules(
        "public-only", "http://127.0.0.1:8899",
        "\n".join(ipv4_lines), "\n".join(ipv6_lines))
    assert verified["proxy_endpoint"] == "10.0.2.2:8899"
    with pytest.raises(RuntimeError, match="100.64.0.0/10"):
        verify_rules(
            "public-only", "http://127.0.0.1:8899",
            "\n".join(line for line in ipv4_lines if "100.64.0.0/10" not in line),
            "\n".join(ipv6_lines))
    with pytest.raises(RuntimeError, match="unexpected ACCEPT"):
        verify_rules(
            "public-only", "http://127.0.0.1:8899",
            "\n".join(ipv4_lines[:1] + [
                "-A OUTPUT -d 192.168.0.0/16 -j ACCEPT"] + ipv4_lines[1:]),
            "\n".join(ipv6_lines))


def test_android_finalize_refuses_implicit_session_bootstrap(monkeypatch):
    """A dead session must not turn finalize into a fresh default-app one.

    When the bound session fails and releases, finalize used to fall through
    to _post -> _ensure_session, silently creating a zero-exploration session
    on BBB_ANDROID_APP_ID (default android_commerce_demo) — the path by which
    a Google Clock handoff once ended up on a blank demo topology. finalize
    must refuse instead of bootstrapping.
    """
    from agents.android_baseline import mcp_server as baseline_mcp
    from agents.android_our_method import mcp_server as ours_mcp
    for mcp in (baseline_mcp, ours_mcp):
        def no_bootstrap():
            raise AssertionError("finalize must not lazily create a session")
        monkeypatch.setattr(mcp, "_ensure_session", no_bootstrap)
        monkeypatch.setattr(mcp, "_session", "")
        monkeypatch.setattr(mcp, "_reproduction", None)
        result = mcp._t_finalize({})
        assert result["isError"] is True
        assert "start_session" in result["content"][0]["text"]


def test_android_mcp_surfaces_do_not_expose_web_or_device_tools():
    from agents.android_baseline import mcp_server as baseline
    from agents.android_our_method import mcp_server as ours
    required = {"tap", "long_press", "swipe", "type_text", "press_back",
                "press_enter", "restart_app", "wait"}
    forbidden = {"click", "switch_tab", "close_tab", "get_dom", "adb",
                 "logcat", "selector"}
    assert required <= set(baseline._TOOLS)
    assert required <= set(ours._TOOLS)
    assert not (forbidden & set(baseline._TOOLS))
    assert not (forbidden & set(ours._TOOLS))
    assert "input_read" not in baseline._TOOLS
    assert {"input_read", "input_list"} <= set(ours._TOOLS)


def test_android_strict_agent_runtimes_mount_no_repository_or_docker_socket():
    for runtime in (baseline_agent_runtime, ours_agent_runtime):
        argv = runtime._container_argv("agent", 8400, "x" * 20, "image")
        joined = "\n".join(argv)
        assert "--read-only" in argv
        assert "/workspace:rw,nosuid,nodev,size=256m" in argv
        assert f"{runtime.PROJECT_ROOT}:" not in joined
        assert "docker.sock" not in joined
        assert str(runtime.ASSETS_DIR) in joined
        assert str(runtime.SKILL_DIR) in joined


def _evaluation_session(tmp_path, features):
    apk = tmp_path / "app.apk"
    apk.write_bytes(b"apk")
    checklist = Checklist.from_object({"platform": "android",
                                       "features": features},
                                      default_id="fixture")
    return AppEvaluationSession(
        apk=apk, checklist=checklist, package_name="com.example.app",
        launch_activity=".MainActivity",
        output_root=tmp_path / "evaluations",
        runtime_factory=lambda spec, directory: _ReviewRuntime())


def test_app_evaluation_requires_evidence_and_four_level_grades(tmp_path):
    session = _evaluation_session(tmp_path, [
        {"id": "login", "name": "Login", "expected": "opens home"},
        {"id": "save", "name": "Save", "expected": "shows a list"},
    ])
    session.start()
    session.observe()
    session.observe()
    session.record_result(requirement_id="login", grade="full",
                          rationale="visible home", evidence_observations=[1])
    # A grade may not cite a frame that was never captured.
    with pytest.raises(ValueError):
        session.record_result(requirement_id="save", grade="full",
                              rationale="x", evidence_observations=[99])
    # Grades come from the closed four-value set.
    with pytest.raises(ValueError):
        session.record_result(requirement_id="save", grade="mostly-ok",
                              rationale="x", evidence_observations=[1])
    # Finishing is refused while a requirement is ungraded.
    with pytest.raises(ValueError):
        session.finish()
    session.record_result(requirement_id="save", grade="placeholder",
                          rationale="button does not persist",
                          evidence_observations=[2])
    result = session.finish()
    assert result["counts"]["full"] == 1
    assert result["counts"]["placeholder"] == 1
    report = json.loads(Path(result["report"]).read_text(encoding="utf-8"))
    assert [item["grade_label"] for item in report["requirements"]] == ["完整", "占位"]
    assert report["platform"] == "android"


def test_app_evaluation_trace_omits_typed_values(tmp_path):
    session = _evaluation_session(tmp_path, [{"id": "login", "name": "Login"}])
    session.start()
    session.observe()
    session.action("type_text", {"text": "private@example.test"})
    session.observe()
    session.record_result(requirement_id="login", grade="full",
                          rationale="visible", evidence_observations=[1, 2])
    report = Path(session.finish()["report"]).read_text(encoding="utf-8")
    assert "private@example.test" not in report
    assert json.loads(report)["actions"] == [{"type": "type_text"}]


def test_app_evaluation_persistence_evidence_is_structural(tmp_path):
    session = _evaluation_session(
        tmp_path, [{"id": "favorite", "name": "Favorite", "persistence": True}])
    session.start()
    session.observe()                                     # 1: before
    # A persistence requirement cannot reach `full` without a probe triple.
    with pytest.raises(ValueError):
        session.record_result(requirement_id="favorite", grade="full",
                              rationale="looks saved",
                              evidence_observations=[1])
    session.action("tap", {"x": 10, "y": 10})
    session.observe()                                     # 2: after
    # Missing restart/re-entry probe between after and persisted is rejected.
    with pytest.raises(ValueError):
        session.record_result(
            requirement_id="favorite", grade="full", rationale="saved",
            evidence_observations=[1, 2],
            persistence_evidence={"before_observation": 1,
                                  "after_observation": 2,
                                  "persisted_observation": 2})
    session.action("restart_app", {})
    session.observe()                                     # 3: persisted
    result = session.record_result(
        requirement_id="favorite", grade="full",
        rationale="收藏后列表新增该商品；重启后仍在收藏列表",
        evidence_observations=[1, 2, 3],
        persistence_evidence={"before_observation": 1,
                              "after_observation": 2,
                              "persisted_observation": 3})
    assert result["grade"] == "full"
    report = json.loads(Path(session.finish()["report"]).read_text(encoding="utf-8"))
    assert report["requirements"][0]["persistence_evidence"][
        "persisted_observation"] == 3


def test_app_evaluation_multi_user_requirement_needs_caveat(tmp_path):
    session = _evaluation_session(
        tmp_path, [{"id": "notify", "name": "Notify", "multi_user": True}])
    session.start()
    session.observe()
    session.observe()
    with pytest.raises(ValueError):
        session.record_result(requirement_id="notify", grade="partial",
                             rationale="sender side works",
                             evidence_observations=[1])
    session.record_result(
        requirement_id="notify", grade="partial",
        rationale="发送端提示正常显示",
        evidence_observations=[1, 2],
        not_verifiable_reason="单台模拟器无法登录第二个账号确认对端是否收到通知")
    report = json.loads(Path(session.finish()["report"]).read_text(encoding="utf-8"))
    assert report["requirements"][0]["not_verifiable_reason"]


def test_checklist_validation_rejects_malformed_specs():
    with pytest.raises(ValueError):
        Checklist.from_object({"features": []})
    with pytest.raises(ValueError):
        Checklist.from_object({"features": [{"id": "a", "name": "A"},
                                            {"id": "a", "name": "B"}]})
    with pytest.raises(ValueError):
        Checklist.from_object({"features": [{"id": "Bad Id", "name": "A"}]})
    with pytest.raises(ValueError):
        Checklist.from_object({"features": [{"id": "a"}]})
    checklist = Checklist.from_object({"platform": "web",
                                       "features": [{"id": "a", "name": "A"}]},
                                      default_id="fixture")
    with pytest.raises(ValueError):
        checklist.expect_platform("android")


def test_shipped_android_checklist_is_valid_and_flagged():
    root = Path(__file__).resolve().parent.parent
    checklist = Checklist.load(root / "review_specs" / "android_commerce_demo.json")
    checklist.expect_platform("android")
    assert len(checklist.features) >= 10
    assert any(item["persistence"] for item in checklist.features)
    # Every ground-truth feature family must appear in the human checklist.
    for required in ("login", "search", "favorite", "cart", "checkout",
                     "profile", "logout", "media"):
        assert required in checklist.ids


def test_dependency_locks_are_referenced_by_build_files():
    root = Path(__file__).resolve().parent.parent
    images = json.loads((root / "requirements" /
                         "container-images.lock.json").read_text(encoding="utf-8"))
    docker_text = "\n".join(
        path.read_text(encoding="utf-8")
        for path in root.rglob("Dockerfile*")
        if not any(part in ("vendor", "runs", ".test-tmp") for part in path.parts))
    for image in images["images"]:
        assert image["digest"] in docker_text
    android_lock = load_toolchain_lock()
    android_dockerfile = (root / "app_reproduction" / "agent_image" /
                          "Dockerfile").read_text(encoding="utf-8")
    assert android_lock["gradle"]["base_image"].split("@", 1)[1] in android_dockerfile
    for revision in ("platform-tools", "platforms;android-35",
                     "build-tools;35.0.0"):
        assert revision in android_dockerfile
