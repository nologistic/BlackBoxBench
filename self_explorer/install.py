"""Install the self-built workspace MCP and its minimal skill.

This registration is separate from the managed `blackboxbench` server, so the
two experiment conditions can be enabled independently.
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
PYTHON = PROJECT_ROOT / "vendor" / "python" / "python.exe"
SERVER_NAME = "blackboxbench-self-built"
SKILL_SOURCE = Path(__file__).resolve().parent / "skill" / "self-built-explorer"


def _kimi_home() -> Path:
    return Path(os.environ.get("KIMI_CODE_HOME", Path.home() / ".kimi-code"))


def _codex_home() -> Path:
    return Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))


def _codex_command() -> str:
    command = shutil.which("codex.cmd" if sys.platform == "win32" else "codex")
    if not command:
        raise FileNotFoundError("Codex CLI not found on PATH")
    return command


def install_kimi(app_id: str) -> None:
    home = _kimi_home()
    home.mkdir(parents=True, exist_ok=True)
    config = home / "mcp.json"
    data = json.loads(config.read_text(encoding="utf-8-sig")) if config.exists() else {}
    data.setdefault("mcpServers", {})[SERVER_NAME] = {
        "command": str(PYTHON if PYTHON.exists() else Path(sys.executable)),
        "args": ["-m", "self_explorer.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        "env": {"BBB_SELF_APP_ID": app_id, "PYTHONUTF8": "1"},
    }
    config.write_text(json.dumps(data, indent=2), encoding="utf-8")
    destination = home / "skills" / "self-built-explorer"
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SOURCE / "SKILL.md", destination / "SKILL.md")
    print(f"已注册独立 MCP: {SERVER_NAME}")
    print(f"已安装 skill: {destination}")


def uninstall_kimi() -> None:
    home = _kimi_home()
    config = home / "mcp.json"
    if config.exists():
        data = json.loads(config.read_text(encoding="utf-8-sig"))
        data.get("mcpServers", {}).pop(SERVER_NAME, None)
        config.write_text(json.dumps(data, indent=2), encoding="utf-8")
    shutil.rmtree(home / "skills" / "self-built-explorer", ignore_errors=True)
    print("已移除 self-built MCP 与 skill")


def claude_hint(app_id: str) -> None:
    python = str(PYTHON if PYTHON.exists() else Path(sys.executable))
    print("Claude Code 用户级注册命令:")
    print(f'  claude mcp add {SERVER_NAME} --scope user --env BBB_SELF_APP_ID={app_id} '
          '--env PYTHONUTF8=1 -- '
          f'"{python}" -m self_explorer.mcp_server')
    print(f"  再复制 {SKILL_SOURCE} 到 ~/.claude/skills/self-built-explorer/")


def install_codex(app_id: str) -> None:
    python = str(PYTHON if PYTHON.exists() else Path(sys.executable))
    codex = _codex_command()
    subprocess.run([codex, "mcp", "remove", SERVER_NAME],
                   check=False, stdout=subprocess.DEVNULL,
                   stderr=subprocess.DEVNULL)
    subprocess.run([
        codex, "mcp", "add", SERVER_NAME,
        "--env", f"PYTHONPATH={PROJECT_ROOT}",
        "--env", f"BBB_SELF_APP_ID={app_id}",
        "--env", "PYTHONUTF8=1",
        "--", python, "-m", "self_explorer.mcp_server",
    ], check=True)
    destination = _codex_home() / "skills" / "self-built-explorer"
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SOURCE / "SKILL.md", destination / "SKILL.md")
    print(f"已注册 Codex MCP: {SERVER_NAME}")
    print(f"已安装 Codex skill: {destination}")


def uninstall_codex() -> None:
    subprocess.run([_codex_command(), "mcp", "remove", SERVER_NAME], check=False)
    shutil.rmtree(_codex_home() / "skills" / "self-built-explorer",
                  ignore_errors=True)
    print("已移除 Codex self-built MCP 与 skill")


def _codebuddy_config() -> Path:
    return Path.home() / ".codebuddy" / ".mcp.json"


def _codebuddy_skills_dir() -> Path:
    # User-level user skills. The .codebuddy/skills-marketplace/ tree is for
    # skills downloaded FROM the marketplace and is NOT scanned by the chat
    # slash-command picker. Project-level <cwd>/.codebuddy/skills/ is higher
    # priority when in that project; install one of them depending on the
    # caller (use both: the project-level is project-specific, the user-level
    # is globally available).
    return Path.home() / ".codebuddy" / "skills"


def _codebuddy_project_skills_dir() -> Path:
    return PROJECT_ROOT / ".codebuddy" / "skills"


def install_codebuddy(app_id: str) -> None:
    """Register the MCP in CodeBuddy's USER-level config (IDE and CLI).

    User scope on purpose: a project-level .mcp.json inside the isolated
    workspace would leak the repository path to the agent, and the workspace
    must stay blank. Registered once here, the server is available in every
    CodeBuddy window; enable ONLY this MCP for strict experiment chats.
    """
    python = str(PYTHON if PYTHON.exists() else Path(sys.executable))
    from . import hardening
    workspace_root = hardening.resolve_runs_root()
    config_path = _codebuddy_config()
    config_path.parent.mkdir(parents=True, exist_ok=True)
    data = json.loads(config_path.read_text(encoding="utf-8-sig")) \
        if config_path.exists() else {}
    data.setdefault("mcpServers", {})[SERVER_NAME] = {
        "type": "stdio",
        "command": python,
        "args": ["-m", "self_explorer.mcp_server"],
        "env": {
            "PYTHONPATH": str(PROJECT_ROOT),
            "BBB_SELF_APP_ID": app_id,
            "PYTHONUTF8": "1",
        },
    }
    config_path.write_text(json.dumps(data, indent=2, ensure_ascii=False),
                           encoding="utf-8")
    skill_dst = _codebuddy_skills_dir() / "self-built-explorer"
    skill_dst.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SOURCE / "SKILL.md", skill_dst / "SKILL.md")
    project_skill = _codebuddy_project_skills_dir() / "self-built-explorer"
    project_skill.mkdir(parents=True, exist_ok=True)
    shutil.copy2(SKILL_SOURCE / "SKILL.md", project_skill / "SKILL.md")
    print(f"已写入 CodeBuddy 用户级 MCP 配置: {config_path}")
    print(f"已注册 MCP: {SERVER_NAME} (默认被测应用 {app_id})")
    print(f"已安装 skill: {skill_dst}")
    print()
    print("IDE 内一键实验流程:")
    print("  1. 重启 CodeBuddy(或重新加载窗口)让 MCP 与 skill 生效;")
    print("  2. 新开窗口,打开一个空目录(严格实验不要打开本仓库);")
    print("  3. 在 MCP 面板只启用 blackboxbench-self-built,禁用其他 MCP;")
    print("  4. 对话中调用 self-built-explorer skill 并提供明确目标 URL:")
    print("     探索结束后会自动进入复现阶段，复现结束后再次完成工作区。")
    print(f"  workspace 生成于 {workspace_root} 下 self_*/workspace。")
    print("  会话期间 ground truth 自动封存,对话结束自动恢复;")
    print("  异常遗留时运行 python -m self_explorer.launcher recover。")
    print()
    print("严格对比实验请改用客户端容器化(CLI 跑在 Docker 里):")
    print("  python -m self_explorer.agent_runtime --image bbb-agent-runtime")
    print("  (见 self_explorer/README.md \"客户端容器化\" 一节)")


def uninstall_codebuddy() -> None:
    config_path = _codebuddy_config()
    if config_path.exists():
        data = json.loads(config_path.read_text(encoding="utf-8-sig"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            config_path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False),
                encoding="utf-8")
    shutil.rmtree(_codebuddy_skills_dir() / "self-built-explorer",
                  ignore_errors=True)
    shutil.rmtree(_codebuddy_project_skills_dir() / "self-built-explorer",
                  ignore_errors=True)
    print(f"已从 CodeBuddy 移除 MCP 与 skill: {SERVER_NAME}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cli", choices=("kimi", "claude", "codex", "codebuddy"),
                        default="kimi")
    parser.add_argument("--app", default="ecommerce_demo")
    parser.add_argument("--uninstall", action="store_true")
    args = parser.parse_args()
    if args.cli == "kimi":
        uninstall_kimi() if args.uninstall else install_kimi(args.app)
    elif args.cli == "claude" and args.uninstall:
        print(f"请从 Claude 用户配置中移除 MCP: {SERVER_NAME}")
    elif args.cli == "claude":
        claude_hint(args.app)
    elif args.cli == "codebuddy":
        uninstall_codebuddy() if args.uninstall else install_codebuddy(args.app)
    else:
        uninstall_codex() if args.uninstall else install_codex(args.app)
        if not args.uninstall:
            print("在新的 Codex 任务中使用 $self-built-explorer https://目标网址/")
    if not args.uninstall and args.cli != "codebuddy":
        print("正式运行时必须只允许这个 MCP，禁用 Agent 自带的 host shell/文件/网络/浏览器工具。")


if __name__ == "__main__":
    main()
