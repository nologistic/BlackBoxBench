"""Containerized agent-runtime isolation tests (no Docker needed).

Covers: TCP MCP transports with token auth (both exploration conditions),
sealed container construction, relay assets free of device hints, and the
managed-mode cross-session contamination screen.
"""
from __future__ import annotations

import json
import socket
import threading
import time
from pathlib import Path

import pytest

from benchmark.topology import crosscheck

ROOT = Path(__file__).resolve().parent.parent


# ------------------------------------------------------------------ TCP MCP

def _free_port() -> int:
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return int(probe.getsockname()[1])


def _start_tcp(module, token: str) -> int:
    port = _free_port()
    thread = threading.Thread(target=module.serve_tcp,
                              args=("127.0.0.1", port, token), daemon=True)
    thread.start()
    for _ in range(200):
        try:
            socket.create_connection(("127.0.0.1", port), timeout=0.2).close()
            return port
        except OSError:
            time.sleep(0.02)
    raise RuntimeError("TCP MCP server did not come up")


def _rpc(port: int, messages: list[dict], token: str | None) -> list[dict]:
    sock = socket.create_connection(("127.0.0.1", port), timeout=10)
    stream = sock.makefile("rwb")
    replies: list[dict] = []
    if token is not None:
        stream.write(json.dumps({
            "jsonrpc": "2.0", "id": "auth", "method": "auth",
            "params": {"token": token},
        }).encode("utf-8") + b"\n")
        stream.flush()
        replies.append(json.loads(stream.readline()))
    for message in messages:
        stream.write(json.dumps(message).encode("utf-8") + b"\n")
        stream.flush()
        try:
            line = stream.readline()
        except (ConnectionAbortedError, ConnectionResetError, OSError):
            # Windows may report an authenticated rejection close as WSAECONNABORTED
            # rather than the empty EOF returned on POSIX.
            break
        if not line:
            break
        replies.append(json.loads(line))
    sock.close()
    return replies


def test_self_built_tcp_transport_serves_tools_after_auth():
    from self_explorer import mcp_server as module
    token = "s" * 24
    port = _start_tcp(module, token)
    replies = _rpc(port, [
        {"jsonrpc": "2.0", "id": 1, "method": "initialize",
         "params": {"protocolVersion": "2025-06-18"}},
        {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
    ], token=token)
    assert replies[0]["result"] == {"ok": True}
    assert replies[1]["result"]["serverInfo"]["name"] == "blackboxbench-self-built"
    names = {tool["name"] for tool in replies[2]["result"]["tools"]}
    assert {"begin_workspace", "read_file", "run_program",
            "finish_workspace"} <= names


def test_self_built_tcp_transport_rejects_wrong_token():
    from self_explorer import mcp_server as module
    port = _start_tcp(module, "s" * 24)
    replies = _rpc(port, [
        {"jsonrpc": "2.0", "id": 1, "method": "tools/list"},
    ], token="wrong-token-0123456789")
    assert replies and "error" in replies[0]
    # unauthorized connection is closed: no tools/list reply arrives
    assert len(replies) == 1


def test_managed_tcp_transport_serves_tools_after_auth():
    from agents.cli_explorer import mcp_server as module
    token = "m" * 24
    port = _start_tcp(module, token)
    replies = _rpc(port, [
        {"jsonrpc": "2.0", "id": 1, "method": "initialize",
         "params": {"protocolVersion": "2025-06-18"}},
        {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
    ], token=token)
    assert replies[0]["result"] == {"ok": True}
    assert replies[1]["result"]["serverInfo"]["name"] == "blackboxbench"
    names = {tool["name"] for tool in replies[2]["result"]["tools"]}
    assert {"observe", "click", "record_state", "finalize",
            "start_reproduction_review", "review_observe", "review_click",
            "complete_reproduction_review", "finish_reproduction"} <= names


def test_managed_tcp_transport_rejects_wrong_token():
    from agents.cli_explorer import mcp_server as module
    port = _start_tcp(module, "m" * 24)
    replies = _rpc(port, [], token="nope-nope-nope-nope")
    assert replies and "error" in replies[0]


# ---------------------------------------------------------- container seal

def _volume_mounts(argv: list[str]) -> list[tuple[str, str, str]]:
    out = []
    for index, item in enumerate(argv):
        if item == "-v":
            source, target, mode = argv[index + 1].rsplit(":", 2)
            out.append((source, target, mode))
    return out


@pytest.mark.parametrize("module_name,expected", [
    ("self_explorer.agent_runtime", "bbb-self-agent-x"),
    ("agents.cli_explorer.agent_runtime", "bbb-managed-agent-x"),
])
def test_agent_runtime_container_is_sealed(module_name, expected):
    module = __import__(module_name, fromlist=["_container_argv"])
    argv = module._container_argv(expected, 8400, "tok-123", "node:22-slim")
    assert expected in argv
    # hardened runtime
    assert argv[argv.index("--cap-drop") + 1] == "ALL"
    assert "--security-opt" in argv and "--pids-limit" in argv
    # exactly two read-only mounts: shim + skill, nothing from the repository
    mounts = _volume_mounts(argv)
    assert len(mounts) == 2
    for source, target, mode in mounts:
        assert mode == "ro"
        assert target in ("/opt/mcp-shim", "/opt/skill")
        assert source in (str(module.ASSETS_DIR), str(module.SKILL_DIR))
    # no docker socket, no repository path passed anywhere else
    joined = " ".join(argv)
    assert "docker.sock" not in joined
    assert str(module.PROJECT_ROOT) + ":" not in joined
    # host MCP endpoint + per-run token reach the container via env only
    assert "BBB_MCP_HOST=host.docker.internal" in argv
    assert "BBB_MCP_PORT=8400" in argv
    assert "BBB_MCP_TOKEN=tok-123" in argv


def test_relay_assets_carry_no_device_hints():
    for assets in (ROOT / "self_explorer" / "agent_runtime_assets",
                   ROOT / "agents" / "cli_explorer" / "agent_runtime_assets"):
        combined = "\n".join(
            p.read_text(encoding="utf-8") for p in sorted(assets.iterdir())
            if p.is_file())
        for forbidden in ("fb0", "event0", "/device", "input_event",
                          "DEVICE_PROTOCOL", "raw_device", "xdotool"):
            assert forbidden not in combined


# ------------------------------------------------------- cross-session screen

_COPIED_LINES = [
    f'"description": "Distinctive cart behavior description line {i} '
    f'with plenty of very specific verbatim words to be unique",'
    for i in range(3)
]


def _fake_runs(tmp_path: Path) -> Path:
    runs = tmp_path / "runs"
    other = runs / "sess_other"
    other.mkdir(parents=True)
    (other / "functional_topology.json").write_text(
        "\n".join(_COPIED_LINES), encoding="utf-8")
    return runs


def test_crosscheck_blocks_copied_deliverables(tmp_path):
    runs = _fake_runs(tmp_path)
    website = tmp_path / "website_output"
    with pytest.raises(ValueError):
        crosscheck.assert_clean("\n".join(_COPIED_LINES), runs, website,
                                "sess_mine")


def test_crosscheck_ignores_own_session_and_clean_text(tmp_path):
    runs = tmp_path / "runs"
    mine = runs / "sess_mine"
    mine.mkdir(parents=True)
    (mine / "functional_topology.json").write_text(
        "\n".join(_COPIED_LINES), encoding="utf-8")
    website = tmp_path / "website_output"
    # own deliverables are excluded from the fingerprint set
    crosscheck.assert_clean("\n".join(_COPIED_LINES), runs, website, "sess_mine")
    # natural short overlap never becomes a fingerprint
    crosscheck.assert_clean("cart page with items\nadd to cart button\n",
                            runs, website, "sess_mine")


def test_crosscheck_reads_website_output(tmp_path):
    website = tmp_path / "website_output" / "handoff-1"
    website.mkdir(parents=True)
    (website / "index.html").write_text(
        "\n".join(_COPIED_LINES), encoding="utf-8")
    runs = tmp_path / "runs"
    runs.mkdir()
    fingerprints = crosscheck.collect_fingerprints(runs, website, "sess_mine")
    assert crosscheck.matched_lines("\n".join(_COPIED_LINES), fingerprints) >= 3
