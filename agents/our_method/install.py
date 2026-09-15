"""One-time installer: after this, exploring is just

    kimi        # 新开一个对话
    /skill:our-method

It registers the blackboxbench MCP server in the user-level CLI config
(persistent; the server self-bootstraps controller + session on first use)
and installs the exploration skill into the CLI's skills directory.

  vendor/python/python.exe -m agents.our_method.install            # kimi(默认)
  vendor/python/python.exe -m agents.our_method.install --cli claude
  vendor/python/python.exe -m agents.our_method.install --cli codex
  vendor/python/python.exe -m agents.our_method.install --uninstall --cli kimi
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
SERVER_NAME = "our-method"

SKILL_SRC = Path(__file__).resolve().parent / "skill" / "our-method"
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"


def _kimi_home() -> Path:
    return Path(os.environ.get("KIMI_CODE_HOME", Path.home() / ".kimi-code"))


def _codex_home() -> Path:
    return Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))


def _codex_command() -> str:
    # On Windows shutil.which("codex") may select npm's extensionless POSIX
    # shim, which CreateProcess cannot execute. Prefer the .cmd launcher.
    command = shutil.which("codex.cmd" if sys.platform == "win32" else "codex")
    if not command:
        raise FileNotFoundError("Codex CLI not found on PATH")
    return command


def install_kimi(app_id: str = "ecommerce_demo") -> None:
    home = _kimi_home()
    home.mkdir(parents=True, exist_ok=True)
    cfg = home / "mcp.json"
    data = json.loads(cfg.read_text(encoding="utf-8")) if cfg.exists() else {}
    servers = data.setdefault("mcpServers", {})
    entry = {
        "command": str(PY) if PY.exists() else sys.executable,
        "args": ["-m", "agents.our_method.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        # no controller/session env on purpose: the server self-bootstraps
        # controller + session on first use
    }
    if app_id != "ecommerce_demo":
        entry["env"] = {"BBB_APP_ID": app_id}
    servers[SERVER_NAME] = entry
    cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"[install] mcp.json 已注册: {cfg}")

    dst = home / "skills" / "our-method"
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
    shutil.rmtree(home / "skills" / "our-method", ignore_errors=True)
    print("[install] skill 已移除")


def print_claude_hint() -> None:
    py = str(PY) if PY.exists() else sys.executable
    print("[install] Claude Code 请执行(一次性,用户级):")
    print(f'  claude mcp add {SERVER_NAME} --scope user -- "{py}" '
          f'-m agents.our_method.mcp_server')
    print(f"  并把 {SKILL_SRC} 复制到 ~/.claude/skills/our-method/")
    print("  (或用 --cwd 指向本仓库后,claude 会读取项目 .mcp.json —— 不推荐,"
          "隔离起见用用户级)")


BASELINE_MCP = "blackboxbench"


def install_codex(app_id: str = "ecommerce_demo",
                  exclusive: bool = False) -> None:
    """Register a cwd-independent stdio server and user-level Codex skill.

    With ``exclusive`` the baseline MCP registration is removed first, so a
    Codex task can only see this condition's tools — hard isolation instead
    of prompt-level separation.  Re-run the baseline installer to restore it.
    """
    python = str(PY) if PY.exists() else sys.executable
    codex = _codex_command()
    subprocess.run([codex, "mcp", "remove", SERVER_NAME],
                   check=False, stdout=subprocess.DEVNULL,
                   stderr=subprocess.DEVNULL)
    subprocess.run([
        codex, "mcp", "add", SERVER_NAME,
        "--env", f"PYTHONPATH={PROJECT_ROOT}",
        "--env", f"BBB_APP_ID={app_id}",
        "--", python, "-m", "agents.our_method.mcp_server",
    ], check=True)
    if exclusive:
        subprocess.run([codex, "mcp", "remove", BASELINE_MCP],
                       check=False, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL)
        print(f"[install] 硬隔离: 基线 MCP '{BASELINE_MCP}' 已从 Codex 移除"
              f"（重装基线: python -m agents.cli_explorer.install --cli codex）")
    destination = _codex_home() / "skills" / "our-method"
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SRC / "SKILL.md", destination / "SKILL.md")
    print(f"[install] Codex MCP 已注册: {SERVER_NAME}")
    print(f"[install] Codex skill 已安装: {destination}")


def uninstall_codex() -> None:
    subprocess.run([_codex_command(), "mcp", "remove", SERVER_NAME], check=False)
    shutil.rmtree(_codex_home() / "skills" / "our-method",
                  ignore_errors=True)
    print("[install] Codex MCP 与 skill 已移除")


def _codebuddy_config() -> Path:
    return Path.home() / ".codebuddy" / ".mcp.json"


def _codebuddy_skills_dir() -> Path:
    # User-level user skills; the .codebuddy/skills-marketplace/ tree is for
    # marketplace downloads and is NOT scanned by the chat slash picker.
    return Path.home() / ".codebuddy" / "skills"


def _codebuddy_project_skills_dir() -> Path:
    return PROJECT_ROOT / ".codebuddy" / "skills"


def install_codebuddy(app_id: str = "ecommerce_demo") -> None:
    """Register the MCP in CodeBuddy's USER-level config (IDE and CLI).

    User scope on purpose: the agent should explore via MCP tools only, and a
    project-level .mcp.json inside some working folder adds nothing but a
    repository-path leak. Enable ONLY this MCP for strict experiment chats.
    """
    python = str(PY) if PY.exists() else sys.executable
    config_path = _codebuddy_config()
    config_path.parent.mkdir(parents=True, exist_ok=True)
    data = json.loads(config_path.read_text(encoding="utf-8-sig")) \
        if config_path.exists() else {}
    entry = {
        "type": "stdio",
        "command": python,
        "args": ["-m", "agents.our_method.mcp_server"],
        "env": {"PYTHONPATH": str(PROJECT_ROOT)},
    }
    if app_id != "ecommerce_demo":
        entry["env"]["BBB_APP_ID"] = app_id
    data.setdefault("mcpServers", {})[SERVER_NAME] = entry
    config_path.write_text(json.dumps(data, indent=2, ensure_ascii=False),
                           encoding="utf-8")
    skill_dst = _codebuddy_skills_dir() / "our-method"
    skill_dst.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SRC / "SKILL.md", skill_dst / "SKILL.md")
    project_skill = _codebuddy_project_skills_dir() / "our-method"
    project_skill.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SRC / "SKILL.md", project_skill / "SKILL.md")
    print(f"[install] 已写入 CodeBuddy 用户级 MCP 配置: {config_path}")
    print(f"[install] 已注册 MCP: {SERVER_NAME} (默认被测应用 {app_id})")
    print(f"[install] 已安装 skill: {skill_dst}")
    print()
    print("IDE 内一键实验流程:")
    print("  1. 重启 CodeBuddy(或重新加载窗口)让 MCP 与 skill 生效;")
    print("  2. 新开窗口,打开一个空目录(实验不要打开本仓库);")
    print("  3. 在 MCP 面板只启用 our-method,禁用基线 blackboxbench 及其他 MCP;")
    print("  4. 对话中调用 our-method skill 即可开始:")
    print("     observe -> 坐标动作 -> discovery 记录 -> finalize")
    print("     (自动进入复现阶段) -> 本地像素复验/修正 -> finish_reproduction。")
    print("     Controller 与会话由 MCP 服务器自动自举。")
    print()
    print("严格对比实验请改用客户端容器化:")
    print("  python -m agents.our_method.agent_runtime --image bbb-agent-runtime")


def uninstall_codebuddy() -> None:
    config_path = _codebuddy_config()
    if config_path.exists():
        data = json.loads(config_path.read_text(encoding="utf-8-sig"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            config_path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False),
                encoding="utf-8")
    shutil.rmtree(_codebuddy_skills_dir() / "our-method",
                  ignore_errors=True)
    shutil.rmtree(_codebuddy_project_skills_dir() / "our-method",
                  ignore_errors=True)
    print(f"[install] 已从 CodeBuddy 移除 MCP 与 skill: {SERVER_NAME}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cli", default="kimi",
                   choices=["kimi", "claude", "codex", "codebuddy"])
    p.add_argument("--app", default="ecommerce_demo",
                   help="自举会话的被测应用 (见 /api/apps), 如 yuque_web")
    p.add_argument("--exclusive", action="store_true",
                   help="Codex: 注册 our-method 的同时移除基线 MCP,实现硬隔离"
                        "(重装基线即可恢复: python -m agents.cli_explorer.install --cli codex)")
    p.add_argument("--uninstall", action="store_true")
    args = p.parse_args()

    if args.cli == "kimi":
        uninstall_kimi() if args.uninstall else install_kimi(args.app)
        if not args.uninstall:
            print()
            print("完成。之后的使用方式:")
            print("  1. 新开一个 kimi 对话:  kimi")
            print("  2. 输入:  /skill:our-method [目标]")
            print("     目标可以是已注册 app_id 或任意网址;缺省用上方默认应用。")
            print("  探索即自动开始(Controller 与会话由 MCP 服务器自举)。")
    elif args.cli == "claude":
        print_claude_hint()
    elif args.cli == "codebuddy":
        uninstall_codebuddy() if args.uninstall else install_codebuddy(args.app)
    else:
        uninstall_codex() if args.uninstall else install_codex(
            args.app, exclusive=args.exclusive)
        if not args.uninstall:
            print("[install] 在新的 Codex 任务中使用 $our-method [目标]")


if __name__ == "__main__":
    main()
