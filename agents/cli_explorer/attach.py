"""Attach mode: you start your agent CLI yourself (interactive TUI); this
command prepares the benchmark session, MCP registration, and exploration
skill, then supervises until the session ends.

  # terminal 1
  vendor/python/python.exe -m benchmark.server --port 7800
  # terminal 2
  vendor/python/python.exe -m agents.cli_explorer.attach --cli kimi

Then follow the printed instructions: start your CLI and either invoke
  /skill:blackbox-explorer
or paste the printed kickoff message. The agent's own chat UI shows its
reasoning live; the dashboard shows the app screen. Ctrl+C here (or the
agent calling finalize) ends and cleans up.
"""
from __future__ import annotations

import argparse
import functools
import os
import shutil
import sys
import time
from pathlib import Path

import httpx

from .adapters import ADAPTERS, PROJECT_ROOT, RunContext
from .run import user_mcp_injection

print = functools.partial(print, flush=True)  # supervise under redirection

SKILL_SRC = Path(__file__).resolve().parent / "skill" / "blackbox-explorer"

# per-CLI: (interactive launch hint, skill dir to install into)
SKILL_DIRS = {
    "kimi": Path(os.environ.get("KIMI_CODE_HOME",
                                Path.home() / ".kimi-code")) / "skills",
    "claude": Path.home() / ".claude" / "skills",
}


def install_skill(cli: str) -> Path | None:
    dst_root = SKILL_DIRS.get(cli)
    if dst_root is None or not SKILL_SRC.exists():
        return None
    dst = dst_root / "blackbox-explorer"
    dst.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SRC / "SKILL.md", dst / "SKILL.md")
    return dst


def main() -> None:
    p = argparse.ArgumentParser(description="BlackBoxBench attach supervisor")
    p.add_argument("--cli", required=True, choices=sorted(ADAPTERS))
    p.add_argument("--controller", default="http://127.0.0.1:7800")
    p.add_argument("--app-id", default="ecommerce_demo")
    p.add_argument("--session", default=None)
    p.add_argument("--max-actions", type=int, default=400)
    p.add_argument("--minutes", type=int, default=30)
    p.add_argument("--no-skill", action="store_true")
    args = p.parse_args()

    http = httpx.Client(base_url=args.controller.rstrip("/"), trust_env=False,
                        timeout=180)
    if args.session:
        sid = args.session
        brief = http.get(f"/api/sessions/{sid}").json().get("brief", "")
    else:
        r = http.post("/api/sessions", json={
            "app_id": args.app_id,
            "budget": {"max_actions": args.max_actions,
                       "max_duration_s": args.minutes * 60,
                       "max_observations": 1000}})
        r.raise_for_status()
        body = r.json()
        sid, brief = body["session_id"], body.get("brief", "")
    print(f"[attach] session: {sid}")

    ctx = RunContext(controller=args.controller.rstrip("/"), session_id=sid,
                     brief=brief, max_actions=args.max_actions,
                     minutes=args.minutes)
    adapter = ADAPTERS[args.cli](ctx)
    ws = PROJECT_ROOT / "runs" / sid / "agent_workspace"
    ws.mkdir(parents=True, exist_ok=True)
    adapter.prepare(ws)
    (ws / "TASK.md").write_text(adapter.task_text(), encoding="utf-8")

    skill_path = None
    if not args.no_skill:
        skill_path = install_skill(args.cli)
        if skill_path:
            print(f"[attach] skill 已安装: {skill_path}")

    launch_hints = {
        "kimi": f'kimi --agent-file "{ws / "explorer.md"}"',
        "claude": f'claude --mcp-config "{ws / "mcp.json"}" '
                  f"--disallowedTools Bash Read Write Edit Glob Grep WebFetch WebSearch",
        "codex": f'codex --cd "{ws}" --sandbox read-only '
                 f'(mcp 配置见 {ws}/TASK.md 旁的命令,建议用 run --cli codex 无头)',
        "opencode": f'opencode   # 在 {ws} 下启动(读取该目录 opencode.json)',
    }
    print()
    print("=" * 64)
    print(f"1) 另开一个终端,启动你的 Agent CLI:")
    print(f"   {launch_hints[args.cli]}")
    print()
    if args.cli in SKILL_DIRS and skill_path:
        print(f"2) 在 CLI 对话框里输入:")
        print(f"   /skill:blackbox-explorer")
        print()
    print("3) 或直接粘贴以下启动语:")
    print("-" * 64)
    print(f"开始黑盒探索任务: 调用 blackboxbench 的 observe 工具查看应用画面,"
          f"然后系统性探索它的功能。任务简报: {brief or '(无)'}")
    print("-" * 64)
    print(f"思考过程会显示在 CLI 自己的界面里;应用画面在 Dashboard:")
    print(f"   {args.controller}/")
    print("=" * 64)
    print("[attach] 监督中: Agent 调用 finalize 或 Ctrl+C 结束")

    injected = None
    if getattr(adapter, "needs_user_mcp_injection", False):
        injected = user_mcp_injection(adapter)
        injected.__enter__()
        print("[attach] 用户级 mcp.json 已注入(结束时自动还原)")
    try:
        while True:
            time.sleep(5)
            try:
                st = http.get(f"/api/sessions/{sid}").json()
            except Exception:
                break
            if st.get("status") != "running":
                print(f"[attach] 会话已结束: {st.get('status')} "
                      f"({st.get('close_reason')})")
                break
    except KeyboardInterrupt:
        print("\n[attach] 收到 Ctrl+C")
    finally:
        if injected is not None:
            injected.__exit__(None, None, None)
            print("[attach] mcp.json 已还原")
        try:
            st = http.get(f"/api/sessions/{sid}").json()
            if st.get("status") == "running":
                http.post(f"/agent/{sid}/finalize", json={})
                print("[attach] 已代为 finalize")
        except Exception:
            pass
    print(f"[attach] artifacts: runs/{sid}/")


if __name__ == "__main__":
    main()
