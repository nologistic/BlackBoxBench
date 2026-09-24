"""Prepare a persistent Linux Chromium login for self-built exploration.

The regular scripts/live_login.py profile is created by the host browser and
is not portable into Linux Docker because browser cookies are OS-encrypted.
This utility starts the trusted reference image with a Docker-native writable
profile and a temporary loopback-only pixel/coordinate operator UI. No Agent
container is started. After manual login, the cleanly stopped profile becomes
the read-only golden seed for future self-built sessions of the same host.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import webbrowser
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from self_explorer import launcher

PROJECT_ROOT = Path(__file__).resolve().parent.parent
COMPOSE_FILE = PROJECT_ROOT / "docker-compose.self-login.yml"


def _free_port() -> int:
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


def _project_name(target_url: str) -> str:
    digest = hashlib.sha256(target_url.encode("utf-8")).hexdigest()[:10]
    return f"bbb-self-login-{digest}"


def _environment(target_url: str, profile: Path, port: int) -> dict[str, str]:
    values = dict(os.environ)
    docker_dir = str(Path(launcher._docker_command()).parent)
    current_path = values.get("PATH", "")
    if docker_dir.lower() not in {
            item.lower() for item in current_path.split(os.pathsep) if item}:
        values["PATH"] = docker_dir + (
            os.pathsep + current_path if current_path else "")
    for key, value in launcher._windows_proxy_environment().items():
        values.setdefault(key, value)
    values.update({
        "BBB_TARGET_URL": target_url,
        "BBB_SELF_LOGIN_PROFILE": str(profile.resolve()),
        "BBB_SELF_LOGIN_PORT": str(port),
    })
    return values


def _compose(project: str, env: dict[str, str], *args: str,
             check: bool = True, timeout: int = 600) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        [launcher._docker_command(), "compose", "-f", str(COMPOSE_FILE),
         "-p", project, *args],
        cwd=PROJECT_ROOT, env=env, text=True, encoding="utf-8",
        errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=timeout, check=False,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    if check and result.returncode:
        detail = result.stderr.strip() or result.stdout.strip()
        raise RuntimeError(detail or f"docker compose exited {result.returncode}")
    return result


def _wait_ui(port: int, timeout: float = 90) -> None:
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    deadline = time.monotonic() + timeout
    url = f"http://127.0.0.1:{port}/health"
    last: Exception | None = None
    while time.monotonic() < deadline:
        try:
            with opener.open(url, timeout=2) as response:
                if json.loads(response.read()).get("status") == "ok":
                    return
        except Exception as exc:
            last = exc
            time.sleep(0.5)
    raise RuntimeError(f"Docker login UI did not become ready: {last}")


def _publish_golden(working: Path, golden: Path) -> None:
    for name in ("SingletonCookie", "SingletonLock", "SingletonSocket",
                 "DevToolsActivePort"):
        path = working / name
        try:
            path.unlink()
        except OSError:
            pass
    pending = golden.with_name(golden.name + ".next")
    shutil.rmtree(pending, ignore_errors=True)
    shutil.copytree(working, pending)
    shutil.rmtree(golden, ignore_errors=True)
    pending.replace(golden)


def capture(target_url: str, *, build: bool = False) -> Path:
    normalized, mode = launcher.normalize_target_url(target_url)
    if mode != "external":
        raise ValueError("self-built live login requires a public website URL")
    state = launcher.docker_profile_state_dir(normalized)
    working = state / "docker_profile"
    golden = state / "docker_profile_golden"
    state.mkdir(parents=True, exist_ok=True)
    if not working.exists():
        if golden.exists():
            shutil.copytree(golden, working)
        else:
            working.mkdir()

    port = _free_port()
    project = _project_name(normalized)
    env = _environment(normalized, working, port)
    _compose(project, env, "down", "--volumes", "--remove-orphans",
             check=False, timeout=120)
    started = False
    completed = False
    try:
        argv = ["up", "-d"]
        if build:
            argv.append("--build")
        print("[self-live-login] 正在启动 Docker 可信浏览器…", flush=True)
        _compose(project, env, *argv)
        started = True
        _wait_ui(port)
        ui = f"http://127.0.0.1:{port}/"
        print(f"[self-live-login] 操作员登录页面: {ui}")
        print("[self-live-login] 请在页面的浏览器画面中完成登录。")
        webbrowser.open(ui)
        input("[self-live-login] 确认已登录后回到这里按 Enter 保存…")
        completed = True
    finally:
        if started:
            _compose(project, env, "down", "--volumes", "--remove-orphans",
                     check=False, timeout=180)
    if not completed:
        raise RuntimeError("Docker login capture was interrupted")
    _publish_golden(working, golden)
    print(f"[self-live-login] Docker golden profile 已保存: {golden}")
    print("[self-live-login] 后续同一站点的自建探索会自动复制并使用该登录态。")
    return golden


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", required=True,
                        help="public http(s) entry URL")
    parser.add_argument("--build", action="store_true",
                        help="rebuild the trusted reference image first")
    args = parser.parse_args()
    capture(args.url, build=args.build)


if __name__ == "__main__":
    main()
