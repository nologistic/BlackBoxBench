"""Android runtime, target isolation, APK workspace and MCP surface tests."""
from __future__ import annotations

import hashlib
import json
import zipfile
from contextlib import nullcontext
from pathlib import Path
from types import SimpleNamespace

from PIL import Image
import pytest

from app_reproduction import workspace as app_workspace
from app_reproduction.review import AndroidReproductionReview
from app_evaluation.cli import execute_plan
from app_evaluation.evaluator import AppEvaluator
from agents.android_baseline import agent_runtime as baseline_agent_runtime
from agents.android_our_method import agent_runtime as ours_agent_runtime
from benchmark.android.runtime import AndroidEmulatorRuntime
from benchmark.android.toolchain import AndroidToolchain, load_toolchain_lock
from benchmark.android import targets
from benchmark.orchestrator.apps import list_apps
from benchmark.orchestrator.session import Budget, Session
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


def test_android_runtime_stop_removes_explore_clone_but_keeps_login(tmp_path):
    spec = SimpleNamespace(app_id="target", package_name="com.example.app",
                           launch_activity=".MainActivity")
    explore = AndroidEmulatorRuntime(spec, tmp_path / "explore")
    explore._avd_home.mkdir(parents=True)
    (explore._avd_home / "disk.img").write_bytes(b"clone")
    explore.stop()
    assert not explore._avd_home.exists()

    login = AndroidEmulatorRuntime(spec, tmp_path / "login", lease_mode="login")
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


def test_app_evaluator_requires_evidence_and_four_level_grades(tmp_path):
    apk = tmp_path / "app.apk"; apk.write_bytes(b"apk")
    checklist = tmp_path / "checklist.json"
    checklist.write_text(json.dumps({"features": [
        {"id": "login", "name": "Login", "expected": "opens home"},
        {"id": "save", "name": "Save", "expected": "persists"},
    ]}), encoding="utf-8")
    evaluator = AppEvaluator(
        apk=apk, package_name="com.example.app",
        launch_activity=".MainActivity", checklist_path=checklist,
        output_root=tmp_path / "evaluations",
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    evaluator.observe()
    evaluator.record_result(feature_id="login", grade="full",
                            rationale="visible home", evidence_observations=[1])
    evaluator.record_result(feature_id="save", grade="placeholder",
                            rationale="button does not persist", evidence_observations=[1])
    result = evaluator.finish()
    assert result["counts"]["full"] == 1
    assert result["counts"]["placeholder"] == 1
    report = json.loads(Path(result["report"]).read_text(encoding="utf-8"))
    assert [item["grade_label"] for item in report["features"]] == ["完整", "占位"]


def test_app_evaluation_cli_plan_executes_and_sanitizes_actions(tmp_path):
    apk = tmp_path / "app.apk"; apk.write_bytes(b"apk")
    checklist = tmp_path / "checklist.json"
    checklist.write_text(json.dumps({"features": [
        {"id": "login", "name": "Login"},
    ]}), encoding="utf-8")
    evaluator = AppEvaluator(
        apk=apk, package_name="com.example.app",
        launch_activity=".MainActivity", checklist_path=checklist,
        output_root=tmp_path / "evaluations",
        runtime_factory=lambda spec, directory: _ReviewRuntime())
    result = execute_plan(evaluator, {"steps": [
        {"type": "observe"},
        {"type": "action", "action": "type_text",
         "args": {"text": "private@example.test"}},
        {"type": "record_result", "feature_id": "login", "grade": "full",
         "rationale": "visible", "evidence_observations": [1]},
    ]})
    report = Path(result["report"]).read_text(encoding="utf-8")
    assert "private@example.test" not in report
    assert json.loads(report)["actions"] == [{"type": "type_text"}]


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
