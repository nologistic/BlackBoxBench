"""One-time installer: after this, exploring is just

    kimi        # 新开一个对话
    /skill:blackbox-explorer

It registers the blackboxbench MCP server in the user-level CLI config
(persistent; the server self-bootstraps controller + session on first use)
and installs the exploration skill into the CLI's skills directory.

  vendor/python/python.exe -m agents.cli_explorer.install            # kimi(默认)
  vendor/python/python.exe -m agents.cli_explorer.install --cli claude
  vendor/python/python.exe -m agents.cli_explorer.install --uninstall --cli kimi
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

from .adapters import PROJECT_ROOT, SERVER_NAME

SKILL_SRC = Path(__file__).resolve().parent / "skill" / "blackbox-explorer"
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"


def _kimi_home() -> Path:
    import os
    return Path(os.environ.get("KIMI_CODE_HOME", Path.home() / ".kimi-code"))


def install_kimi(app_id: str = "ecommerce_demo") -> None:
    home = _kimi_home()
    home.mkdir(parents=True, exist_ok=True)
    cfg = home / "mcp.json"
    data = json.loads(cfg.read_text(encoding="utf-8")) if cfg.exists() else {}
    servers = data.setdefault("mcpServers", {})
    entry = {
        "command": str(PY) if PY.exists() else sys.executable,
        "args": ["-m", "agents.cli_explorer.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        # no controller/session env on purpose: the server self-bootstraps
        # controller + session on first use
    }
    if app_id != "ecommerce_demo":
        entry["env"] = {"BBB_APP_ID": app_id}
    servers[SERVER_NAME] = entry
    cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"[install] mcp.json 已注册: {cfg}")

    dst = home / "skills" / "blackbox-explorer"
    dst.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SRC / "SKILL.md", dst / "SKILL.md")
    print(f"[install] skill 已安装: {dst}")
    print(f"[install] 新会话默认被测应用: {app_id}"
          + ("(默认)" if app_id == "ecommerce_demo"
             else " —— 切回电商 demo: install --app ecommerce_demo"))


def uninstall_kimi() -> None:
    home = _kimi_home()
    cfg = home / "mcp.json"
    if cfg.exists():
        data = json.loads(cfg.read_text(encoding="utf-8"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
            print("[install] mcp.json 条目已移除")
    shutil.rmtree(home / "skills" / "blackbox-explorer", ignore_errors=True)
    print("[install] skill 已移除")


def print_claude_hint() -> None:
    py = str(PY) if PY.exists() else sys.executable
    print("[install] Claude Code 请执行(一次性,用户级):")
    print(f'  claude mcp add {SERVER_NAME} --scope user -- "{py}" '
          f'-m agents.cli_explorer.mcp_server')
    print(f"  并把 {SKILL_SRC} 复制到 ~/.claude/skills/blackbox-explorer/")
    print("  (或用 --cwd 指向本仓库后,claude 会读取项目 .mcp.json —— 不推荐,"
          "隔离起见用用户级)")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cli", default="kimi", choices=["kimi", "claude"])
    p.add_argument("--app", default="ecommerce_demo",
                   help="自举会话的被测应用 (见 /api/apps), 如 douyin_web")
    p.add_argument("--uninstall", action="store_true")
    args = p.parse_args()

    if args.cli == "kimi":
        uninstall_kimi() if args.uninstall else install_kimi(args.app)
        if not args.uninstall:
            print()
            print("完成。之后的使用方式:")
            print("  1. 新开一个 kimi 对话:  kimi")
            print("  2. 输入:  /skill:blackbox-explorer [目标]")
            print("     目标可以是已注册 app_id 或任意网址;缺省用上方默认应用。")
            print("  探索即自动开始(Controller 与会话由 MCP 服务器自举)。")
            print("  Dashboard: http://127.0.0.1:7800/")
    else:
        print_claude_hint()


if __name__ == "__main__":
    main()
