"""Security and contract tests for the self-built-tool mode (no Docker needed)."""
from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import time
from pathlib import Path

import pytest
from PIL import Image

from self_explorer import hardening, launcher, mcp_server

ROOT = Path(__file__).resolve().parent.parent


def _raw_bridge():
    spec = importlib.util.spec_from_file_location(
        "bbb_raw_device_bridge", ROOT / "docker" / "raw_device_bridge.py")
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def _public_proxy():
    spec = importlib.util.spec_from_file_location(
        "bbb_public_web_proxy", ROOT / "docker" / "public_web_proxy.py")
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def test_hardware_bridge_consumes_standard_scalar_input_events(monkeypatch):
    bridge = _raw_bridge()
    commands = []
    monkeypatch.setattr(bridge, "_run", lambda argv, timeout=15: commands.append(argv))
    monkeypatch.setattr(bridge, "capture_frame", lambda: True)
    monkeypatch.setattr(bridge.time, "sleep", lambda _seconds: None)
    monkeypatch.setattr(bridge, "_actions", 0)
    monkeypatch.setattr(bridge, "_last_action", 0.0)
    monkeypatch.setattr(bridge, "_started", time.monotonic())

    batch = [
        (bridge.EV_ABS, bridge.ABS_X, 20),
        (bridge.EV_ABS, bridge.ABS_Y, 30),
        (bridge.EV_KEY, bridge.BTN_LEFT, 1),
        (bridge.EV_KEY, bridge.BTN_LEFT, 0),
    ]
    assert bridge.apply_batch(batch) is True
    assert ["xdotool", "mousemove", "20", "30"] in commands
    assert ["xdotool", "mousedown", "1"] in commands
    assert ["xdotool", "mouseup", "1"] in commands
    assert bridge.INPUT_EVENT.size == 24


def test_framebuffer_is_plain_lossless_pixels(monkeypatch, tmp_path):
    bridge = _raw_bridge()
    monkeypatch.setattr(bridge, "WIDTH", 2)
    monkeypatch.setattr(bridge, "HEIGHT", 1)
    monkeypatch.setattr(bridge, "FRAME_PATH", tmp_path / "fb0.ppm")
    monkeypatch.setattr(bridge, "_frames", 0)
    monkeypatch.setattr(bridge, "_started", time.monotonic())

    def fake_scrot(argv, timeout=15):
        Image.new("RGB", (2, 1), (10, 20, 30)).save(argv[-1])

    monkeypatch.setattr(bridge, "_run", fake_scrot)
    assert bridge.capture_frame() is True
    assert (tmp_path / "fb0.ppm").read_bytes() == (
        b"P6\n2 1\n255\n" + bytes((10, 20, 30, 10, 20, 30)))


def test_raw_device_blocks_modifiers_devtools_and_budget(monkeypatch):
    bridge = _raw_bridge()
    assert 29 not in bridge.KEYS       # Control_L
    assert 56 not in bridge.KEYS       # Alt_L
    assert 88 not in bridge.KEYS       # F12
    monkeypatch.setattr(bridge, "capture_frame", lambda: True)
    monkeypatch.setattr(bridge, "_actions", bridge.MAX_ACTIONS)
    assert bridge.apply_batch([(bridge.EV_ABS, bridge.ABS_X, 10)]) is False


def test_public_proxy_rejects_any_private_dns_answer(monkeypatch):
    proxy = _public_proxy()
    monkeypatch.setattr(proxy.socket, "getaddrinfo", lambda *args, **kwargs: [
        (proxy.socket.AF_INET, proxy.socket.SOCK_STREAM, 6, "",
         ("93.184.216.34", 443)),
        (proxy.socket.AF_INET, proxy.socket.SOCK_STREAM, 6, "",
         ("127.0.0.1", 443)),
    ])
    with pytest.raises(proxy.Denied):
        proxy._public_addresses("example.com", 443)


def test_public_proxy_accepts_only_web_ports():
    proxy = _public_proxy()
    assert proxy._authority("example.com:443", 443) == ("example.com", 443)
    with pytest.raises(proxy.Denied):
        proxy._authority("example.com:22", 443)


def test_public_proxy_does_not_pin_navigation_to_entry_host(monkeypatch):
    proxy = _public_proxy()
    seen = []

    def resolve(host, port, **_kwargs):
        seen.append((host, port))
        return [(proxy.socket.AF_INET, proxy.socket.SOCK_STREAM, 6, "",
                 ("93.184.216.34", port))]

    monkeypatch.setattr(proxy.socket, "getaddrinfo", resolve)
    assert proxy._public_addresses("entry.example", 443)
    assert proxy._public_addresses("redirected.example", 443)
    assert seen == [("entry.example", 443), ("redirected.example", 443)]


def test_compose_and_agent_image_are_repo_isolated():
    compose = (ROOT / "docker-compose.self-explorer.yml").read_text(encoding="utf-8")
    assert "context: ./self_explorer/agent_image" in compose
    assert compose.count("network_mode: none") == 2
    assert "BROWSER_IO_ROOT: /device" in compose
    assert "./:/" not in compose and "./benchmark" not in compose
    assert "ports:" not in compose
    assert "blackbox.sock" not in compose
    external = (ROOT / "docker-compose.self-explorer.url.yml").read_text(
        encoding="utf-8")
    assert "reference_raw:" in external and "network_mode: bridge" in external
    assert "\n  tool_builder:" in external
    assert external.count("network_mode: bridge") == 2
    profile = (ROOT / "docker-compose.self-explorer.profile.yml").read_text(
        encoding="utf-8")
    assert "reference_raw:" in profile and "read_only: true" in profile
    assert "/profile-seed" in profile
    assert "tool_builder:" not in profile and "ports:" not in profile
    login = (ROOT / "docker-compose.self-login.yml").read_text(encoding="utf-8")
    assert "reference_login:" in login and "tool_builder:" not in login
    assert "127.0.0.1:" in login and "BBB_SELF_LOGIN_PROFILE" in login
    operator_ui = (ROOT / "docker" / "self_login_ui.py").read_text(
        encoding="utf-8")
    for forbidden in ("Runtime.evaluate", "DOM.", "Accessibility.",
                      "Network.", "Storage.", "remote-debugging"):
        assert forbidden not in operator_ui
    assert "Page.captureScreenshot" not in operator_ui

    image_dir = ROOT / "self_explorer" / "agent_image"
    combined = "\n".join(p.read_text(encoding="utf-8") for p in image_dir.iterdir()
                         if p.is_file())
    for forbidden in ("COPY benchmark", "COPY agents", "COPY agent_sdk",
                      "curl ", "wget ", "git clone", "pip install"):
        assert forbidden not in combined
    dockerfile = (image_dir / "Dockerfile").read_text(encoding="utf-8")
    assert "COPY entrypoint.sh" in dockerfile
    for leaked_hint in ("DEVICE_PROTOCOL", "OUTPUT_CONTRACT", "AGENT_TASK",
                        "fb0", "event0", "/device", "BBRD", "input_event"):
        assert leaked_hint not in combined


def test_agent_receives_only_constraints_not_device_hints(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd",
                               "bbb-test", "ecommerce_demo", tmp_path, workspace)
    monkeypatch.setattr(mcp_server, "_environment", env)
    assert mcp_server._t_begin({"target_url": launcher.DEMO_TARGET_URL}) == {
        "content": [{"type": "text", "text":
                     '{"ready": true, "target_url": '
                     '"http://reference-app.internal:8200/", '
                     '"browser_io_root": "/device"}'}],
        "isError": False,
    }
    assert list(workspace.iterdir()) == []

    skill = (ROOT / "self_explorer" / "skill" / "self-built-explorer" /
             "SKILL.md").read_text(encoding="utf-8")
    assert "目标浏览器" in skill and "浏览器 I/O 根位置" in skill
    for leaked_hint in ("fb0", "event0", "/device", "P6", "input_event",
                        "DEVICE_PROTOCOL", "截图", "点击函数", "图像库",
                        "探索策略", "证据工具", "从零实现所需工具",
                        "功能拓扑", "functional_topology", "report.md",
                        "output/evidence", "states", "features"):
        assert leaked_hint not in skill


def test_launcher_creates_only_its_workspace(monkeypatch, tmp_path):
    monkeypatch.setattr(launcher, "RUNS_ROOT", tmp_path)
    calls = []

    def fake_compose(env, *args, **kwargs):
        calls.append(args)
        return subprocess.CompletedProcess(args, 0, "", "")

    monkeypatch.setattr(launcher, "_compose", fake_compose)
    env = launcher.start("ecommerce_demo")
    assert env.workspace.parent == env.run_dir
    metadata = json.loads(env.metadata_path.read_text(encoding="utf-8"))
    assert metadata["mode"] == "self-built-tools"
    assert metadata["status"] == "running"
    assert calls == [("up", "-d")]

    launcher.stop(env.session_id)
    metadata = json.loads(env.metadata_path.read_text(encoding="utf-8"))
    assert metadata["status"] == "finished"
    assert calls[-1] == ("down", "--volumes", "--remove-orphans")


@pytest.mark.parametrize("url", [
    "http://localhost/", "http://127.0.0.1/", "http://10.0.0.1/",
    "http://host.docker.internal/", "http://example.com:8080/",
    "https://user:secret@example.com/",
])
def test_target_url_rejects_private_or_credentialed_destinations(url):
    with pytest.raises(ValueError):
        launcher.normalize_target_url(url)


def test_target_url_supports_public_https_and_bundled_demo():
    assert launcher.normalize_target_url("https://Example.COM/shop?q=1") == (
        "https://example.com/shop?q=1", "external")
    assert launcher.normalize_target_url(
        "http://reference-app.internal:8200/catalog") == (
            "http://reference-app.internal:8200/catalog", "bundled")


def test_external_environment_uses_network_override(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               launcher.EXTERNAL_APP_ID, tmp_path, workspace,
                               "https://example.com/", "external")
    captured = {}
    monkeypatch.setattr(launcher, "_docker_command", lambda: "docker")

    def fake_run(argv, **kwargs):
        captured["argv"] = argv
        return subprocess.CompletedProcess(argv, 0, "", "")

    monkeypatch.setattr(launcher.subprocess, "run", fake_run)
    launcher._compose(env, "ps")
    assert str(launcher.COMPOSE_FILE) in captured["argv"]
    assert str(launcher.URL_COMPOSE_FILE) in captured["argv"]
    assert launcher._compose_env(env)["BBB_TARGET_URL"] == "https://example.com/"


def test_external_environment_mounts_docker_profile_only_on_trusted_side(
        monkeypatch, tmp_path):
    state_root = tmp_path / "live_targets"
    seed = state_root / "live_example_com" / "docker_profile_golden"
    seed.mkdir(parents=True)
    (seed / "Local State").write_text("{}", encoding="utf-8")
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               launcher.EXTERNAL_APP_ID, tmp_path, workspace,
                               "https://example.com/", "external")
    captured = {}
    monkeypatch.setattr(launcher, "LIVE_STATE_ROOT", state_root)
    monkeypatch.setattr(launcher, "_docker_command", lambda: "docker")

    def fake_run(argv, **kwargs):
        captured["argv"] = argv
        captured["env"] = kwargs["env"]
        return subprocess.CompletedProcess(argv, 0, "", "")

    monkeypatch.setattr(launcher.subprocess, "run", fake_run)
    launcher._compose(env, "ps")
    assert str(launcher.PROFILE_COMPOSE_FILE) in captured["argv"]
    assert captured["env"]["BBB_SELF_PROFILE_SEED"] == str(seed.resolve())
    profile_compose = launcher.PROFILE_COMPOSE_FILE.read_text(encoding="utf-8")
    assert "tool_builder:" not in profile_compose


def test_external_profile_id_is_stable_per_host():
    assert launcher.external_profile_id(
        "https://WWW.YUQUE.COM/path") == "live_www_yuque_com"
    assert launcher.docker_profile_golden_dir(
        "https://www.yuque.com/").name == "docker_profile_golden"


def test_docker_resolver_accepts_explicit_binary(monkeypatch, tmp_path):
    binary = tmp_path / "docker.exe"
    binary.write_bytes(b"stub")
    monkeypatch.setenv("BBB_DOCKER_BIN", str(binary))
    assert launcher._docker_command() == str(binary)


@pytest.mark.skipif(sys.platform != "win32",
                    reason="resolves a Windows-only per-user Docker "
                           "Desktop path (instantiates WindowsPath)")
def test_docker_resolver_finds_per_user_desktop(monkeypatch, tmp_path):
    binary = (tmp_path / "Programs" / "DockerDesktop" / "resources" /
              "bin" / "docker.exe")
    binary.parent.mkdir(parents=True)
    binary.write_bytes(b"stub")
    monkeypatch.delenv("BBB_DOCKER_BIN", raising=False)
    monkeypatch.setenv("LOCALAPPDATA", str(tmp_path))
    monkeypatch.setattr(launcher.shutil, "which", lambda _name: None)
    monkeypatch.setattr(launcher.os, "name", "nt")
    assert launcher._docker_command() == str(binary)


def test_compose_env_adds_docker_helpers_to_path(monkeypatch, tmp_path):
    binary = tmp_path / "docker-bin" / "docker.exe"
    binary.parent.mkdir()
    binary.write_bytes(b"stub")
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               "ecommerce_demo", tmp_path, workspace)
    monkeypatch.setattr(launcher, "_docker_command", lambda: str(binary))
    monkeypatch.setenv("PATH", "C:\\Windows")
    values = launcher._compose_env(env)
    assert values["PATH"].split(launcher.os.pathsep)[0] == str(binary.parent)


def test_status_requests_untruncated_json(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               "ecommerce_demo", tmp_path, workspace)
    captured = {}

    monkeypatch.setattr(launcher, "_load_environment", lambda _session_id: env)

    def fake_compose(selected, *args, **kwargs):
        captured["args"] = args
        return subprocess.CompletedProcess(args, 0, '{"State":"running"}', "")

    monkeypatch.setattr(launcher, "_compose", fake_compose)
    selected, detail = launcher.status(env.session_id)
    assert selected is env
    assert detail == '{"State":"running"}'
    assert captured["args"] == ("ps", "--format", "json", "--no-trunc")


def test_windows_proxy_parser_supports_scalar_and_protocol_map():
    assert launcher._parse_windows_proxy("127.0.0.1:7890") == {
        "HTTP_PROXY": "http://127.0.0.1:7890",
        "HTTPS_PROXY": "http://127.0.0.1:7890",
        "NO_PROXY": "localhost,127.0.0.1,::1",
    }
    assert launcher._parse_windows_proxy(
        "http=proxy.local:80;https=https://secure.local:443") == {
        "HTTP_PROXY": "http://proxy.local:80",
        "HTTPS_PROXY": "https://secure.local:443",
        "NO_PROXY": "localhost,127.0.0.1,::1",
    }


def test_failed_start_leaves_no_run_or_volume(monkeypatch, tmp_path):
    monkeypatch.setattr(launcher, "RUNS_ROOT", tmp_path)
    calls = []

    def fail_then_cleanup(env, *args, **kwargs):
        calls.append(args)
        if args[0] == "up":
            raise subprocess.CalledProcessError(1, args)
        return subprocess.CompletedProcess(args, 0, "", "")

    monkeypatch.setattr(launcher, "_compose", fail_then_cleanup)
    with pytest.raises(subprocess.CalledProcessError):
        launcher.start("ecommerce_demo")
    assert list(tmp_path.iterdir()) == []
    assert calls[-1] == ("down", "--volumes", "--remove-orphans")


@pytest.mark.parametrize("logger", [launcher._log, hardening._log,
                                     mcp_server._log])
def test_diagnostic_log_pipe_cannot_break_lifecycle(monkeypatch, logger):
    class ClosedStderr:
        def write(self, _value):
            raise BrokenPipeError("client closed stderr")

        def flush(self):
            raise BrokenPipeError("client closed stderr")

    module = sys.modules[logger.__module__]
    monkeypatch.setattr(module.sys, "stderr", ClosedStderr())
    logger("diagnostic only …")


def test_workspace_mcp_seals_before_starting_environment(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd",
                               "bbb-test", "ecommerce_demo", tmp_path,
                               workspace)
    events = []
    monkeypatch.setattr(mcp_server, "_environment", None)
    monkeypatch.setattr(mcp_server, "_seal_begin",
                        lambda app_id: events.append(("seal", app_id)))
    monkeypatch.setattr(launcher, "start",
                        lambda app_id, target_url: events.append(
                            ("start", app_id, target_url)) or env)

    assert mcp_server._ensure_environment(launcher.DEMO_TARGET_URL) is env
    assert events == [("seal", "ecommerce_demo"),
                      ("start", "ecommerce_demo", launcher.DEMO_TARGET_URL)]


def test_workspace_mcp_releases_seal_when_start_fails(monkeypatch):
    events = []
    monkeypatch.setattr(mcp_server, "_environment", None)
    monkeypatch.setattr(mcp_server, "_seal_begin",
                        lambda app_id: events.append(("seal", app_id)))
    monkeypatch.setattr(mcp_server, "_seal_end",
                        lambda: events.append(("release", None)))

    def fail_start(app_id, target_url):
        events.append(("start", app_id, target_url))
        raise subprocess.CalledProcessError(1, ["docker", "compose", "up"])

    monkeypatch.setattr(launcher, "start", fail_start)
    with pytest.raises(subprocess.CalledProcessError):
        mcp_server._ensure_environment(launcher.DEMO_TARGET_URL)
    assert mcp_server._environment is None
    assert events == [("seal", "ecommerce_demo"),
                      ("start", "ecommerce_demo", launcher.DEMO_TARGET_URL),
                      ("release", None)]


def test_workspace_mcp_rejects_escape_and_handles_text(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd",
                               "bbb-test", "ecommerce_demo", tmp_path, workspace)
    monkeypatch.setattr(mcp_server, "_environment", env)

    result = mcp_server._t_write({"path": "tools/client.py", "content": "print('ok')\n"})
    assert result["isError"] is False
    result = mcp_server._t_read({"path": "tools/client.py"})
    assert "print('ok')" in result["content"][0]["text"]

    with pytest.raises(ValueError):
        mcp_server._relative("../managed/mcp_server.py")
    with pytest.raises(ValueError):
        mcp_server._relative("/etc/passwd")


def test_workspace_exec_is_argv_form_and_container_bound(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd",
                               "bbb-test", "ecommerce_demo", tmp_path, workspace)
    monkeypatch.setattr(mcp_server, "_environment", env)
    captured = {}

    def fake_compose(selected, *args, **kwargs):
        captured["args"] = args
        return subprocess.CompletedProcess(args, 0, "done", "")

    monkeypatch.setattr(launcher, "_compose", fake_compose)
    result = mcp_server._t_run({"argv": ["python3", "tools/client.py"], "cwd": "."})
    assert result["isError"] is False
    assert captured["args"][:6] == (
        "exec", "-T", "--workdir", "/workspace", "tool_builder", "python3")
    assert "tools/client.py" in captured["args"]


def test_self_exploration_artifacts_are_layout_neutral_and_exclude_code(tmp_path):
    workspace = tmp_path / "workspace"
    (workspace / "anything").mkdir(parents=True)
    note = workspace / "anything" / "my-own-name.txt"
    note.write_text("an observation", encoding="utf-8")
    image = workspace / "frame.ppm"
    image.write_bytes(b"P6\n1 1\n255\n\x00\x00\x00")
    tool = workspace / "capture.py"
    tool.write_text("print('agent code')", encoding="utf-8")
    dependency = workspace / ".deps" / "package" / "metadata.json"
    dependency.parent.mkdir(parents=True)
    dependency.write_text('{"name":"third-party"}', encoding="utf-8")
    env = launcher.Environment("self_20260825_120000_1234abcd",
                               "bbb-test", "ecommerce_demo", tmp_path, workspace)
    assert mcp_server._self_exploration_files(env) == {
        "agent_artifacts/anything/my-own-name.txt": note.resolve(),
        "agent_artifacts/frame.ppm": image.resolve(),
    }


def test_finish_workspace_needs_no_topology_report_or_evidence(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               "ecommerce_demo", tmp_path, workspace)
    captured = {}

    class FakeReproduction:
        source_id = "self_20260825_120000_1234abcd"
        output_dir = tmp_path / "reproduction_output"

        @classmethod
        def start(cls, **kwargs):
            captured.update(kwargs)
            return cls()

        def started_payload(self):
            return {"stage": "reproduction", "ready": True,
                    "handoff_id": "handoff", "output": "website_output/handoff"}

        def finish(self):
            return {"finished": True, "output": "website_output/handoff"}

    monkeypatch.setattr(mcp_server, "ReproductionWorkspace", FakeReproduction)
    monkeypatch.setattr(mcp_server, "_environment", env)
    monkeypatch.setattr(mcp_server, "_reproduction", None)
    monkeypatch.setattr(launcher, "stop", lambda _session_id: env)
    # Keep the finish-time integrity screening hermetic and fast: no repo
    # fingerprint collection inside unit tests.
    monkeypatch.setattr(mcp_server.hardening, "collect_fingerprints",
                        lambda *args, **kwargs: {})

    first = mcp_server._t_finish({})
    payload = json.loads(first["content"][0]["text"])
    assert payload["exploration_finished"] is True
    assert payload["stage"] == "reproduction"
    assert captured["source_mode"] == "self-built-tools"
    assert captured["source_id"] == env.session_id
    assert captured["topology_path"] is None
    assert captured["exploration_files"] == {}

    second = mcp_server._t_finish({})
    assert json.loads(second["content"][0]["text"])["finished"] is True
    assert mcp_server._reproduction is None


# --------------------------------------------------------------------------
# Host-side hardening: runs root, sealing, integrity screening, audit.
# --------------------------------------------------------------------------

def test_hardening_runs_root_uses_project_directory(monkeypatch, tmp_path):
    monkeypatch.delenv("BBB_SELF_RUNS_ROOT", raising=False)
    root = hardening.resolve_runs_root()
    assert root == ROOT / "runs" / "self_built"
    monkeypatch.setenv("BBB_SELF_RUNS_ROOT", str(tmp_path))
    assert hardening.resolve_runs_root() == tmp_path


def test_seal_manager_seals_and_restores(tmp_path):
    target = tmp_path / "ground_truth.json"
    target.write_text("{}", encoding="utf-8")
    runs = tmp_path / "runs"
    manager = hardening.SealManager(lambda: runs)
    manager.acquire("exploration", [target])
    assert not target.exists()
    manager.acquire("reproduction", [target])   # second holder, same files
    manager.release("exploration")
    assert not target.exists()                  # still held by reproduction
    manager.release("reproduction")
    assert target.is_file()
    assert target.read_text(encoding="utf-8") == "{}"
    assert not (runs / ".seal" / "active").exists()


def test_seal_manager_shares_one_seal_across_managers(tmp_path):
    """Two manager instances (i.e. two concurrent agent processes) share a
    single seal; the files return only after the LAST holder releases."""
    target = tmp_path / "ground_truth.json"
    target.write_text("{}", encoding="utf-8")
    runs = tmp_path / "runs"
    first = hardening.SealManager(lambda: runs)
    second = hardening.SealManager(lambda: runs)
    first.acquire("agent-a", [target])
    second.acquire("agent-b", [])               # joins the existing seal
    assert not target.exists()
    assert sorted(str(p) for p in second.sealed_origins()) == [str(target)]
    first.release("agent-a")
    assert not target.exists()                  # agent-b still exploring
    second.release("agent-b")
    assert target.is_file()


def test_seal_manager_does_not_recover_live_holder(tmp_path):
    """The old single-process bug: a second process's acquire/recover must
    NOT restore a seal whose holder is still alive."""
    target = tmp_path / "ground_truth.json"
    target.write_text("{}", encoding="utf-8")
    runs = tmp_path / "runs"
    live = hardening.SealManager(lambda: runs)
    live.acquire("agent-a", [target])
    other = hardening.SealManager(lambda: runs)
    assert other.recover() == []
    assert not target.exists()
    live.release("agent-a")
    assert target.is_file()


_SUBPROCESS_SEAL_SCRIPT = """
import os, sys, time
sys.path.insert(0, {root!r})
from pathlib import Path
from self_explorer import hardening
manager = hardening.SealManager(lambda: Path({runs!r}))
manager.acquire({holder!r}, [Path({target!r})])
Path({marker!r}).write_text("held", encoding="utf-8")
{tail}
"""


def test_seal_manager_recovers_orphaned_seals(tmp_path):
    target = tmp_path / "ground_truth.json"
    target.write_text("{}", encoding="utf-8")
    runs = tmp_path / "runs"
    marker = tmp_path / "held.marker"
    script = _SUBPROCESS_SEAL_SCRIPT.format(
        root=str(ROOT), runs=str(runs), holder="crashed", target=str(target),
        marker=str(marker), tail="os._exit(0)   # crash: no release")
    proc = subprocess.run([sys.executable, "-c", script],
                          capture_output=True, timeout=120)
    assert proc.returncode == 0
    assert marker.is_file()                     # the holder really ran
    assert not target.exists()                  # seal survived the crash
    revived = hardening.SealManager(lambda: runs)
    restored = revived.recover()
    assert target in restored
    assert target.read_text(encoding="utf-8") == "{}"


def test_seal_manager_concurrent_processes_share_one_seal(tmp_path):
    """Two real processes hold the seal at the same time; the ground truth
    stays sealed until BOTH are gone, then recover() restores it."""
    target = tmp_path / "ground_truth.json"
    target.write_text("{}", encoding="utf-8")
    runs = tmp_path / "runs"
    holders = []
    tail = "while True:\n    time.sleep(1)"    # stay alive until killed
    for name in ("agent-a", "agent-b"):
        marker = tmp_path / f"{name}.marker"
        script = _SUBPROCESS_SEAL_SCRIPT.format(
            root=str(ROOT), runs=str(runs), holder=name, target=str(target),
            marker=str(marker), tail=tail)
        holders.append((name, marker, subprocess.Popen(
            [sys.executable, "-c", script],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)))
    manifest = runs / ".seal" / "active" / "manifest.json"
    deadline = time.monotonic() + 60
    try:
        while time.monotonic() < deadline:
            if all(m.is_file() for _, m, _ in holders):
                break
            time.sleep(0.1)
        assert all(m.is_file() for _, m, _ in holders)
        assert not target.exists()
        names = {h.get("name") for h in json.loads(
            manifest.read_text(encoding="utf-8")).get("holders", [])}
        assert {"agent-a", "agent-b"} <= names
    finally:
        for _, _, proc in holders:
            proc.kill()
            proc.wait(timeout=30)
    revived = hardening.SealManager(lambda: runs)
    restored = revived.recover()
    assert target in restored
    assert target.read_text(encoding="utf-8") == "{}"


def test_mcp_seals_ground_truth_for_whole_conversation(monkeypatch, tmp_path):
    secret = tmp_path / "ground_truth.json"
    secret.write_text("{}", encoding="utf-8")
    monkeypatch.setattr(launcher, "RUNS_ROOT", tmp_path)
    monkeypatch.setattr(mcp_server, "_seal", None)
    monkeypatch.setattr(mcp_server.hardening, "seal_targets",
                        lambda *args, **kwargs: [secret])
    mcp_server._seal_begin("ecommerce_demo")
    assert not secret.exists()
    mcp_server._seal_end()
    assert secret.is_file()


def test_mcp_seal_disabled_by_env(monkeypatch, tmp_path):
    monkeypatch.setenv("BBB_SELF_SEAL", "0")
    secret = tmp_path / "ground_truth.json"
    secret.write_text("{}", encoding="utf-8")
    monkeypatch.setattr(mcp_server, "_seal", None)
    mcp_server._seal_begin("ecommerce_demo")
    assert secret.exists()
    assert mcp_server._seal is None


def _fake_project(tmp_path: Path) -> Path:
    """A miniature repository tree for fingerprint collection."""
    root = tmp_path / "repo"
    app = root / "sample_apps" / "demo"
    app.mkdir(parents=True)
    states = [
        {"id": "state_cart_with_items", "type": "STATE", "evidence": [],
         "description": "Cart page showing quantity editors, remove buttons, "
                        "line subtotals, a coupon field and order totals with "
                        "a proceed to checkout button."},
        {"id": "state_product_detail", "type": "STATE", "evidence": [],
         "description": "Product detail page showing a gallery, a long "
                        "description, price, quantity picker and add-to-cart "
                        "button."},
        {"id": "state_checkout_address", "type": "STATE", "evidence": [],
         "description": "Checkout address form with name, street, city, "
                        "postal code and a continue to payment button."},
    ]
    (app / "ground_truth.json").write_text(json.dumps({
        "session_id": "reference-ground-truth",
        "_comment": "benchmark-private: never served to agents",
        "states": states}, indent=2), encoding="utf-8")
    (app / "app.py").write_text(
        "def update_cart_quantity(item_id: int, quantity: int):\n"
        "    flash('Invalid quantity', 'error')\n", encoding="utf-8")
    docker = root / "docker"
    docker.mkdir()
    (docker / "raw_device_bridge.py").write_text(
        'INPUT_EVENT = struct.Struct("=qqHHi")\n', encoding="utf-8")
    return root


_GT_SMUGGLED_LINES = (
    '"id": "state_cart_with_items",',
    '"description": "Cart page showing quantity editors, remove buttons, '
    'line subtotals, a coupon field and order totals with a proceed to '
    'checkout button.",',
    '"id": "state_product_detail",',
)


def test_screen_blocks_outright_ground_truth_copy(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    (workspace / "notes.md").write_text(
        "\n".join(_GT_SMUGGLED_LINES) + "\n", encoding="utf-8")
    report = hardening.screen_workspace(
        workspace, hardening.collect_fingerprints(root, "demo"),
        phase="exploration")
    assert any(item["category"] == "ground_truth" for item in report["blocked"])


def test_screen_blocks_structural_ground_truth_markers(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    (workspace / "notes.md").write_text(
        "copied: reference-ground-truth\n", encoding="utf-8")
    report = hardening.screen_workspace(
        workspace, hardening.collect_fingerprints(root, "demo"),
        phase="exploration")
    assert any(item["category"] == "ground_truth" and item["structural"]
               for item in report["blocked"])


def test_screen_blocks_device_protocol_leak(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    (workspace / "tool.py").write_text(
        'INPUT_EVENT = struct.Struct("=qqHHi")\n', encoding="utf-8")
    report = hardening.screen_workspace(
        workspace, hardening.collect_fingerprints(root, "demo"),
        phase="exploration")
    assert any(item["category"] == "device_protocol"
               for item in report["blocked"])


def test_screen_blocks_implementation_leak_anywhere_in_workspace(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    (workspace / "arbitrary-name.txt").write_text(
        "The page seemed to be built with React and querySelector.",
        encoding="utf-8")
    fingerprints = hardening.collect_fingerprints(root, "demo")
    exploration = hardening.screen_workspace(workspace, fingerprints,
                                              phase="exploration")
    assert any(item["category"] == "implementation_leak"
               for item in exploration["blocked"])
    reproduction = hardening.screen_workspace(workspace, fingerprints,
                                              phase="reproduction")
    assert reproduction["blocked"] == []


def test_screen_ignores_third_party_dependency_tree_but_scans_agent_files(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    dependency = workspace / ".deps" / "package" / "module.py"
    dependency.parent.mkdir(parents=True)
    dependency.write_text(
        "React querySelector localhost /api/ node_modules",
        encoding="utf-8")
    (workspace / "own-note.txt").write_text(
        "I pressed the visible blue button.", encoding="utf-8")
    report = hardening.screen_workspace(
        workspace, hardening.collect_fingerprints(root, "demo"),
        phase="exploration")
    assert report["blocked"] == []


def test_screen_allows_agent_own_observations(tmp_path):
    root = _fake_project(tmp_path)
    workspace = tmp_path / "workspace"
    output = workspace / "output"
    output.mkdir(parents=True)
    (output / "report.md").write_text(
        "I pressed the blue button and a list with three items appeared.",
        encoding="utf-8")
    (workspace / "tools").mkdir()
    (workspace / "tools" / "capture.py").write_text(
        "from __future__ import annotations\n"
        "if __name__ == \"__main__\":\n    pass\n", encoding="utf-8")
    report = hardening.screen_workspace(
        workspace, hardening.collect_fingerprints(root, "demo"),
        phase="exploration")
    assert report["blocked"] == []


def test_finish_refusal_reports_category_without_content(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    (workspace / "notes.md").write_text(
        "\n".join(_GT_SMUGGLED_LINES) + "\n", encoding="utf-8")
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               "ecommerce_demo", tmp_path, workspace)
    started = []

    class NoStart:
        @classmethod
        def start(cls, **kwargs):
            started.append(kwargs)
            return cls()

    monkeypatch.setattr(mcp_server, "ReproductionWorkspace", NoStart)
    monkeypatch.setattr(mcp_server, "_environment", env)
    monkeypatch.setattr(mcp_server, "_reproduction", None)
    monkeypatch.setattr(launcher, "stop", lambda _session_id: env)
    # Point the screening at the miniature repository.
    monkeypatch.setattr(mcp_server, "PROJECT_ROOT", _fake_project(tmp_path))
    monkeypatch.setattr(launcher, "RUNS_ROOT", tmp_path)

    result = mcp_server._t_finish({})
    assert result["isError"] is True
    message = result["content"][0]["text"]
    assert "ground_truth" in message and "notes.md" in message
    assert "state_cart_with_items" not in message   # never echo matched content
    assert not started
    assert mcp_server._environment is env           # session not torn down
    audit = (tmp_path / "audit.jsonl").read_text(encoding="utf-8")
    assert "integrity_scan" in audit


def test_second_finish_screens_reproduction_output(monkeypatch, tmp_path):
    root = _fake_project(tmp_path)
    repro_output = tmp_path / "website_output" / "handoff"
    repro_output.mkdir(parents=True)
    (repro_output / "index.html").write_text(
        "\n".join(_GT_SMUGGLED_LINES) + "\n", encoding="utf-8")

    class FakeReproduction:
        source_id = "self_20260825_120000_1234abcd"
        output_dir = repro_output

        def finish(self):
            return {"finished": True}

    monkeypatch.setattr(mcp_server, "_reproduction", FakeReproduction())
    monkeypatch.setattr(mcp_server, "_reproduction_app_id", "demo")
    monkeypatch.setattr(mcp_server, "PROJECT_ROOT", root)
    monkeypatch.setattr(launcher, "RUNS_ROOT", tmp_path)
    result = mcp_server._t_finish({})
    assert result["isError"] is True
    assert "ground_truth" in result["content"][0]["text"]


def test_workspace_tool_calls_are_audited(monkeypatch, tmp_path):
    workspace = tmp_path / "workspace"
    workspace.mkdir()
    env = launcher.Environment("self_20260825_120000_1234abcd", "bbb-test",
                               "ecommerce_demo", tmp_path, workspace)
    monkeypatch.setattr(mcp_server, "_environment", env)
    message = {"jsonrpc": "2.0", "id": 1, "method": "tools/call",
               "params": {"name": "write_file",
                          "arguments": {"path": "notes.md", "content": "hi"}}}
    response = mcp_server._handle(message)
    assert response["result"]["isError"] is False
    entries = [json.loads(line) for line in
               (tmp_path / "audit.jsonl").read_text(encoding="utf-8").splitlines()]
    assert entries[-1]["tool"] == "write_file"
    assert entries[-1]["ok"] is True
    assert entries[-1]["path"] == "notes.md"
    assert "content" not in entries[-1]    # file contents are never audited


def test_docker_build_contexts_exclude_private_material():
    ignore = (ROOT / ".dockerignore").read_text(encoding="utf-8")
    assert "sample_apps/*/ground_truth.json" in ignore
    assert "runs" in ignore
    assert "website_output" in ignore
