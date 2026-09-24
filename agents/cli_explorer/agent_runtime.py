"""Docker isolation for the agent CLIENT in strict managed-tool runs.

The CDP whitelist and the agent-channel response whitelist are enforced
server-side, but the agent client (Codex, OpenCode, Claude Code, ...)
normally runs on the host, where its own built-in tools can read the
repository, the reference app source, the ground truth file, other
sessions' finalized topologies and reproduction outputs. This module
removes that channel: the client runs inside a locked-down container with

* NO repository mounts, NO docker socket — every exploration step still
  goes through the MCP tools into the Controller/Runtime, so other agents'
  results are physically unreachable;
* read-only mounts of the generic MCP shim and the task skill only;
* default bridge networking (internet access for the model API), reaching
  the host MCP server via host.docker.internal:<port> with a per-run token.

The MCP server binds 127.0.0.1 on the host: verified reachable from
containers through Docker Desktop's proxy, invisible to the LAN.

One conversation = one TCP connection = one exploration/reproduction
lifecycle (the server resets its binding between connections).
"""
from __future__ import annotations

import argparse
import json
import os
import secrets
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
ASSETS_DIR = Path(__file__).resolve().parent / "agent_runtime_assets"
SKILL_DIR = Path(__file__).resolve().parent / "skill" / "blackbox-explorer"
DEFAULT_IMAGE = "node:22-bookworm-slim"


def _log(message: str) -> None:
    print(f"[managed-agent-runtime] {message}", file=sys.stderr, flush=True)


def _docker_command() -> str:
    configured = os.environ.get("BBB_DOCKER_BIN", "").strip()
    if configured and Path(configured).is_file():
        return configured
    discovered = shutil.which("docker.exe" if os.name == "nt" else "docker")
    if discovered:
        return discovered
    if os.name == "nt":
        local = os.environ.get("LOCALAPPDATA", "")
        candidate = (Path(local) / "Programs" / "DockerDesktop" /
                     "resources" / "bin" / "docker.exe")
        if local and candidate.is_file():
            return str(candidate)
    raise FileNotFoundError("Docker CLI is not installed")


def _free_port() -> int:
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return int(probe.getsockname()[1])


def _docker(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [_docker_command(), *args], text=True, encoding="utf-8",
        errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        check=check, timeout=180,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))


def _container_argv(container: str, port: int, token: str, image: str) -> list[str]:
    """Docker run argv: no repository mounts, no docker socket, read-only
    shim/skill mounts, hardened runtime, host MCP endpoint + token."""
    return ["run", "-d", "--name", container,
            "--add-host", "host.docker.internal:host-gateway",
            "--cap-drop", "ALL", "--security-opt", "no-new-privileges:true",
            "--pids-limit", "512", "--memory", "4g", "--cpus", "2",
            "-v", f"{ASSETS_DIR}:/opt/mcp-shim:ro",
            "-v", f"{SKILL_DIR}:/opt/skill:ro",
            "-e", "BBB_MCP_HOST=host.docker.internal",
            "-e", f"BBB_MCP_PORT={port}",
            "-e", f"BBB_MCP_TOKEN={token}",
            image, "sleep", "infinity"]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--image", default=DEFAULT_IMAGE,
                        help="agent runtime image (see agent_runtime_assets/)")
    parser.add_argument("--app", default="ecommerce_demo")
    parser.add_argument("--port", type=int, default=None,
                        help="MCP TCP port on the host loopback (default: pick a free one)")
    args = parser.parse_args()

    for required in (ASSETS_DIR / "mcp_relay.py", SKILL_DIR / "SKILL.md"):
        if not required.is_file():
            raise SystemExit(f"missing asset: {required}")

    run_id = secrets.token_hex(4)
    container = f"bbb-managed-agent-{run_id}"
    port = args.port or _free_port()
    token = secrets.token_hex(20)

    mcp = subprocess.Popen(
        [sys.executable, "-m", "agents.cli_explorer.mcp_server",
         "--tcp", f"127.0.0.1:{port}", "--token", token],
        cwd=str(PROJECT_ROOT),
        env={**os.environ,
             "PYTHONPATH": str(PROJECT_ROOT), "BBB_APP_ID": args.app},
        stderr=None)

    try:
        _docker(*_container_argv(container, port, token, args.image))
    except Exception:
        mcp.terminate()
        raise

    print(json.dumps({
        "container": container,
        "mcp_endpoint": f"host.docker.internal:{port}",
        "image": args.image,
        "app": args.app,
    }, ensure_ascii=False))
    print("进入容器并注册 MCP(以 CodeBuddy CLI / Codex 为例,其他客户端同理):")
    print(f"  docker exec -it {container} bash")
    print("  # CodeBuddy CLI(shim 直接读容器内的 BBB_MCP_* 环境变量):")
    print("  codebuddy mcp add --scope user blackboxbench \\")
    print("    -- python3 /opt/mcp-shim/mcp_relay.py")
    print("  # Codex CLI:")
    print("  codex mcp add blackboxbench \\")
    print(f"    --env BBB_MCP_HOST=host.docker.internal --env BBB_MCP_PORT={port} \\")
    print(f"    --env BBB_MCP_TOKEN={token} \\")
    print("    -- python3 /opt/mcp-shim/mcp_relay.py")
    print("随后在该容器内按 /opt/skill/SKILL.md 的任务说明开始探索;")
    print("bash-only 镜像可用 bash /opt/mcp-shim/mcp_relay.sh 代替 python shim。")
    print("容器无仓库挂载;一次 TCP 连接即一次完整探索+复现生命周期。")
    print("按 Ctrl+C 结束(清理容器并停止 MCP 服务)。")

    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        pass
    finally:
        _log("cleaning up...")
        try:
            _docker("rm", "-f", container, check=False)
        except Exception as exc:
            _log(f"container cleanup failed: {type(exc).__name__}")
        mcp.terminate()
        try:
            mcp.wait(timeout=10)
        except subprocess.TimeoutExpired:
            mcp.kill()
        _log("done")


if __name__ == "__main__":
    main()
