"""Install the web-review judge Skill and MCP server into a CLI.

    codex                       # new task
    $web-review <checklist>

It registers the `web-review` MCP server in the user-level CLI config and
installs the judge Skill into the CLI's skills directory.

  vendor/python/python.exe -m agents.web_review.install                  # kimi(默认)
  vendor/python/python.exe -m agents.web_review.install --cli codex
  vendor/python/python.exe -m agents.web_review.install --cli codebuddy
  vendor/python/python.exe -m agents.web_review.install --cli claude
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
SERVER_NAME = "web-review"
SKILL_NAME = "web-review"

SKILL_SRC = Path(__file__).resolve().parent / "skill" / SKILL_NAME
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"
DEFAULT_CHECKLIST = "yuque_web.json"


def _copy_skill(destination: Path) -> None:
    shutil.rmtree(destination, ignore_errors=True)
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copytree(SKILL_SRC, destination)


def _kimi_home() -> Path:
    return Path(os.environ.get("KIMI_CODE_HOME", Path.home() / ".kimi-code"))


def _codex_home() -> Path:
    return Path(os.environ.get("CODEX_HOME", Path.home() / ".codex"))


def _codex_command() -> str:
    command = shutil.which("codex.cmd" if sys.platform == "win32" else "codex")
    if not command:
        raise FileNotFoundError("Codex CLI not found on PATH")
    return command


def install_kimi(checklist: str = DEFAULT_CHECKLIST) -> None:
    home = _kimi_home()
    cfg = home / "mcp.json"
    data = {}
    if cfg.exists():
        data = json.loads(cfg.read_text(encoding="utf-8"))
    entry = {
        "command": str(PY),
        "args": ["-m", "agents.web_review.mcp_server"],
        "env": {"PYTHONPATH": str(PROJECT_ROOT),
                "BBB_WEB_REVIEW_CHECKLIST": checklist},
    }
    data.setdefault("mcpServers", {})[SERVER_NAME] = entry
    cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"[install] mcp.json 已注册: {cfg}")

    dst = home / "skills" / SKILL_NAME
    _copy_skill(dst)
    print(f"[install] skill 已安装: {dst}")
    print(f"[install] 默认清单: {checklist}")


def uninstall_kimi() -> None:
    home = _kimi_home()
    cfg = home / "mcp.json"
    if cfg.exists():
        data = json.loads(cfg.read_text(encoding="utf-8"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
            print("[install] mcp.json 条目已移除")
    shutil.rmtree(home / "skills" / SKILL_NAME, ignore_errors=True)
    print("[install] skill 已移除")


def print_claude_hint() -> None:
    print("[install] Claude Code 请执行(一次性,用户级):")
    print(f'  claude mcp add {SERVER_NAME} --scope user -- "{_python()}" '
          f'-m agents.web_review.mcp_server')
    print(f'  并把 {SKILL_SRC} 复制到 ~/.claude/skills/{SKILL_NAME}/')


def install_codex(checklist: str = DEFAULT_CHECKLIST) -> None:
    codex = _codex_command()
    subprocess.run([codex, "mcp", "remove", SERVER_NAME],
                   check=False, capture_output=True)
    subprocess.run([
        codex, "mcp", "add", SERVER_NAME,
        "--env", f"PYTHONPATH={PROJECT_ROOT}",
        "--env", f"BBB_WEB_REVIEW_CHECKLIST={checklist}",
        "--", _python(), "-m", "agents.web_review.mcp_server",
    ], check=True)
    destination = _codex_home() / "skills" / SKILL_NAME
    _copy_skill(destination)
    print(f"[install] Codex MCP 已注册: {SERVER_NAME}")
    print(f"[install] Codex skill 已安装: {destination}")


def uninstall_codex() -> None:
    subprocess.run([_codex_command(), "mcp", "remove", SERVER_NAME],
                   check=False)
    shutil.rmtree(_codex_home() / "skills" / SKILL_NAME, ignore_errors=True)
    print("[install] Codex MCP 与 skill 已移除")


def _codebuddy_config() -> Path:
    return Path.home() / ".codebuddy" / ".mcp.json"


def _codebuddy_skills_dir() -> Path:
    return Path.home() / ".codebuddy" / "skills"


def _codebuddy_project_skills_dir() -> Path:
    return PROJECT_ROOT / ".codebuddy" / "skills"


def install_codebuddy(checklist: str = DEFAULT_CHECKLIST) -> None:
    config_path = _codebuddy_config()
    config_path.parent.mkdir(parents=True, exist_ok=True)
    data = {}
    if config_path.exists():
        try:
            data = json.loads(config_path.read_text(encoding="utf-8-sig"))
        except Exception:
            data = {}
    entry = {
        "command": str(_python()),
        "args": ["-m", "agents.web_review.mcp_server"],
        "env": {"PYTHONPATH": str(PROJECT_ROOT),
                "BBB_WEB_REVIEW_CHECKLIST": checklist},
    }
    data.setdefault("mcpServers", {})[SERVER_NAME] = entry
    config_path.write_text(json.dumps(data, indent=2, ensure_ascii=False),
                           encoding="utf-8")
    skill_dst = _codebuddy_skills_dir() / SKILL_NAME
    _copy_skill(skill_dst)
    _copy_skill(_codebuddy_project_skills_dir() / SKILL_NAME)
    print(f"[install] 已写入 CodeBuddy 用户级 MCP 配置: {config_path}")
    print(f"[install] 已注册 MCP: {SERVER_NAME} (默认清单 {checklist})")
    print(f"[install] 已安装 skill: {skill_dst}")


def uninstall_codebuddy() -> None:
    config_path = _codebuddy_config()
    if config_path.exists():
        try:
            data = json.loads(config_path.read_text(encoding="utf-8-sig"))
            if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
                config_path.write_text(
                    json.dumps(data, indent=2, ensure_ascii=False),
                    encoding="utf-8")
        except Exception:
            pass
    shutil.rmtree(_codebuddy_skills_dir() / SKILL_NAME, ignore_errors=True)
    shutil.rmtree(_codebuddy_project_skills_dir() / SKILL_NAME,
                  ignore_errors=True)
    print(f"[install] 已从 CodeBuddy 移除 MCP 与 skill: {SERVER_NAME}")


def _python() -> str:
    return str(PY if PY.exists() else Path(sys.executable))


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cli", default="kimi",
                   choices=["kimi", "claude", "codex", "codebuddy"])
    p.add_argument("--checklist", default=DEFAULT_CHECKLIST,
                   help="review_specs 下的默认清单文件名")
    p.add_argument("--uninstall", action="store_true")
    args = p.parse_args()
    if args.cli == "kimi":
        uninstall_kimi() if args.uninstall else install_kimi(args.checklist)
        if not args.uninstall:
            print()
            print("完成。之后的使用方式:")
            print("  1. 新开一个 kimi 对话:  kimi")
            print(f"  2. 输入:  /skill:{SKILL_NAME} [清单文件]")
    elif args.cli == "claude":
        print_claude_hint()
    elif args.cli == "codebuddy":
        uninstall_codebuddy() if args.uninstall else install_codebuddy(
            args.checklist)
    else:
        uninstall_codex() if args.uninstall else install_codex(args.checklist)
        if not args.uninstall:
            print(f"[install] 在新的 Codex 任务中使用 ${SKILL_NAME} [清单文件]")


if __name__ == "__main__":
    main()
