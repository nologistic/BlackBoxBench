"""One-time installer for the Android evaluation (judge) condition.

After this, evaluating a reproduced APK is just

    codex                       # new task
    $app-review <checklist>

It registers the `app-review` MCP server in the user-level CLI config and
installs the judge Skill into the CLI's skills directory.

  vendor/python/python.exe -m agents.app_review.install                  # kimi(默认)
  vendor/python/python.exe -m agents.app_review.install --cli codex
  vendor/python/python.exe -m agents.app_review.install --cli codebuddy
  vendor/python/python.exe -m agents.app_review.install --cli claude
  vendor/python/python.exe -m agents.app_review.install --uninstall --cli kimi

The judge condition is orthogonal to the four exploration conditions: it reads
only a built APK under app_output/ plus a human checklist under review_specs/,
and never touches a target app, ground truth or an exploration MCP.
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
SERVER_NAME = "app-review"
SKILL_NAME = "app-review"

SKILL_SRC = Path(__file__).resolve().parent / "skill" / SKILL_NAME
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"
DEFAULT_CHECKLIST = "android_commerce_demo.json"


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


def _python() -> str:
    return str(PY) if PY.exists() else sys.executable


def install_kimi(checklist: str = DEFAULT_CHECKLIST) -> None:
    home = _kimi_home()
    home.mkdir(parents=True, exist_ok=True)
    cfg = home / "mcp.json"
    data = json.loads(cfg.read_text(encoding="utf-8")) if cfg.exists() else {}
    servers = data.setdefault("mcpServers", {})
    entry = {
        "command": _python(),
        "args": ["-m", "agents.app_review.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        "env": {"BBB_APP_REVIEW_CHECKLIST": checklist},
    }
    servers[SERVER_NAME] = entry
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
          f'-m agents.app_review.mcp_server')
    print(f"  并把 {SKILL_SRC} 复制到 ~/.claude/skills/{SKILL_NAME}/")


def install_codex(checklist: str = DEFAULT_CHECKLIST) -> None:
    codex = _codex_command()
    subprocess.run([codex, "mcp", "remove", SERVER_NAME],
                   check=False, stdout=subprocess.DEVNULL,
                   stderr=subprocess.DEVNULL)
    subprocess.run([
        codex, "mcp", "add", SERVER_NAME,
        "--env", f"PYTHONPATH={PROJECT_ROOT}",
        "--env", f"BBB_APP_REVIEW_CHECKLIST={checklist}",
        "--", _python(), "-m", "agents.app_review.mcp_server",
    ], check=True)
    destination = _codex_home() / "skills" / SKILL_NAME
    _copy_skill(destination)
    print(f"[install] Codex MCP 已注册: {SERVER_NAME}")
    print(f"[install] Codex skill 已安装: {destination}")


def uninstall_codex() -> None:
    subprocess.run([_codex_command(), "mcp", "remove", SERVER_NAME], check=False)
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
    data = json.loads(config_path.read_text(encoding="utf-8-sig")) \
        if config_path.exists() else {}
    entry = {
        "type": "stdio",
        "command": _python(),
        "args": ["-m", "agents.app_review.mcp_server"],
        "env": {"PYTHONPATH": str(PROJECT_ROOT),
                "BBB_APP_REVIEW_CHECKLIST": checklist},
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
    print()
    print("评测流程:")
    print("  1. 重启 CodeBuddy 让 MCP 与 skill 生效;")
    print("  2. 在 MCP 面板只启用 app-review,禁用四个探索条件的 MCP;")
    print("  3. 对话中调用 app-review skill:")
    print("     list_checklists -> start_evaluation -> observe/坐标动作")
    print("     -> record_result(逐条四档) -> finish_evaluation。")


def uninstall_codebuddy() -> None:
    config_path = _codebuddy_config()
    if config_path.exists():
        data = json.loads(config_path.read_text(encoding="utf-8-sig"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            config_path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False),
                encoding="utf-8")
    shutil.rmtree(_codebuddy_skills_dir() / SKILL_NAME, ignore_errors=True)
    shutil.rmtree(_codebuddy_project_skills_dir() / SKILL_NAME,
                  ignore_errors=True)
    print(f"[install] 已从 CodeBuddy 移除 MCP 与 skill: {SERVER_NAME}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cli", default="kimi",
                   choices=["kimi", "claude", "codex", "codebuddy"])
    p.add_argument("--checklist", default=DEFAULT_CHECKLIST,
                   help="review_specs 下的默认人工清单文件名")
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
        uninstall_codebuddy() if args.uninstall else install_codebuddy(args.checklist)
    else:
        uninstall_codex() if args.uninstall else install_codex(args.checklist)
        if not args.uninstall:
            print(f"[install] 在新的 Codex 任务中使用 ${SKILL_NAME} [清单文件]")


if __name__ == "__main__":
    main()
