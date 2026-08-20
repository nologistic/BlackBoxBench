"""Run a real agent CLI (Kimi Code / Claude Code / Codex / OpenCode) against a
BlackBoxBench session via the MCP tool server.

  # terminal 1
  vendor/python/python.exe -m benchmark.server --port 7800
  # terminal 2
  vendor/python/python.exe -m agents.cli_explorer.run --cli kimi

The CLI runs headless in an isolated workspace with ONLY the blackboxbench MCP
tools. Its stdout is streamed through. On exit/timeout the session is
finalized if the agent didn't do it itself.
"""
from __future__ import annotations

import argparse
import contextlib
import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

import httpx

from .adapters import ADAPTERS, PROJECT_ROOT, SERVER_NAME, RunContext


@contextlib.contextmanager
def user_mcp_injection(adapter):
    """Temporarily add our MCP server to the user-level kimi mcp.json.

    Backs up the original file bytes and restores them on exit, even on error.
    """
    home = Path(os.environ.get("KIMI_CODE_HOME",
                               Path.home() / ".kimi-code"))
    cfg = home / "mcp.json"
    original = cfg.read_bytes() if cfg.exists() else None
    data = json.loads(original) if original else {}
    data.setdefault("mcpServers", {})[SERVER_NAME] = adapter.mcp_entry()
    cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
    try:
        yield
    finally:
        if original is None:
            cfg.unlink(missing_ok=True)
        else:
            cfg.write_bytes(original)


def _stream(pipe, prefix: str) -> None:
    try:
        for line in iter(pipe.readline, ""):
            if line:
                print(f"{prefix}{line}", end="", flush=True)
    except Exception:
        pass


def main() -> None:
    p = argparse.ArgumentParser(description="BlackBoxBench CLI-framework runner")
    p.add_argument("--cli", required=True, choices=sorted(ADAPTERS))
    p.add_argument("--controller", default="http://127.0.0.1:7800")
    p.add_argument("--app-id", default="ecommerce_demo")
    p.add_argument("--url", default=None,
                   help="ad-hoc live target URL(覆盖 --app-id),如 https://example.com")
    p.add_argument("--session", default=None,
                   help="attach to an existing session instead of creating one")
    p.add_argument("--max-actions", type=int, default=1500)
    p.add_argument("--minutes", type=int, default=180,
                   help="budget minutes AND wall-clock timeout for the CLI")
    args = p.parse_args()

    http = httpx.Client(base_url=args.controller.rstrip("/"), trust_env=False,
                        timeout=180)
    brief = ""
    sid = args.session
    if sid:
        st = http.get(f"/api/sessions/{sid}")
        st.raise_for_status()
        brief = st.json().get("brief", "") or ""
    else:
        payload = {"budget": {"max_actions": args.max_actions,
                              "max_duration_s": args.minutes * 60,
                              "max_observations": 2000}}
        if args.url:
            payload["live_url"] = args.url
        else:
            payload["app_id"] = args.app_id
        r = http.post("/api/sessions", json=payload)
        r.raise_for_status()
        body = r.json()
        sid = body["session_id"]
        brief = body.get("brief", "")
    print(f"[cli-explorer] session: {sid}  cli: {args.cli}")

    ctx = RunContext(controller=args.controller.rstrip("/"), session_id=sid,
                     brief=brief, max_actions=args.max_actions,
                     minutes=args.minutes)
    adapter = ADAPTERS[args.cli](ctx)

    ws = PROJECT_ROOT / "runs" / sid / "agent_workspace"
    ws.mkdir(parents=True, exist_ok=True)
    adapter.prepare(ws)
    (ws / "TASK.md").write_text(adapter.task_text(), encoding="utf-8")

    cmd = adapter.command(ws)
    print(f"[cli-explorer] workspace: {ws}")

    with contextlib.ExitStack() as stack:
        if getattr(adapter, "needs_user_mcp_injection", False):
            stack.enter_context(user_mcp_injection(adapter))
            print("[cli-explorer] user-level mcp.json patched (will restore)")
        proc = subprocess.Popen(
            cmd, cwd=str(ws), stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace", bufsize=1)
        reader = threading.Thread(target=_stream, args=(proc.stdout, "[cli] "),
                                  daemon=True)
        reader.start()

        deadline = time.monotonic() + args.minutes * 60
        timed_out = False
        while proc.poll() is None:
            if time.monotonic() > deadline:
                timed_out = True
                proc.kill()
                break
            time.sleep(2)
        rc = proc.wait()
    print(f"[cli-explorer] CLI exited rc={rc} timed_out={timed_out}")

    # wrap up: finalize if the agent didn't
    try:
        st = http.get(f"/api/sessions/{sid}").json()
        if st.get("status") == "running":
            r = http.post(f"/agent/{sid}/finalize", json={})
            print(f"[cli-explorer] harness-side finalize: "
                  f"{r.status_code} {r.text[:200]}")
        else:
            print(f"[cli-explorer] session status: {st.get('status')}")
    except Exception as e:
        print(f"[cli-explorer] finalize check failed: {e}")
    print(f"[cli-explorer] artifacts: runs/{sid}/")
    if timed_out:
        sys.exit(3)


if __name__ == "__main__":
    main()
