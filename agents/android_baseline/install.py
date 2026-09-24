"""One-time installer: after this, exploring is just

    kimi        # 新开一个对话
    /skill:android-blackbox-explorer

It registers the android-blackboxbench MCP server in the user-level CLI config
(persistent; the server self-bootstraps controller + session on first use)
and installs the exploration skill into the CLI's skills directory.

  vendor/python/python.exe -m agents.android_baseline.install            # kimi(默认)
  vendor/python/python.exe -m agents.android_baseline.install --cli claude
  vendor/python/python.exe -m agents.android_baseline.install --cli codex
  python -m agents.android_baseline.install --cli opencode               # Linux/opencode
  vendor/python/python.exe -m agents.android_baseline.install --uninstall --cli kimi
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
SERVER_NAME = "android-blackboxbench"

SKILL_SRC = Path(__file__).resolve().parent / "skill" / "android-blackbox-explorer"
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"


def _copy_skill(destination: Path) -> None:
    shutil.rmtree(destination, ignore_errors=True)
    shutil.copytree(SKILL_SRC, destination)


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


def _opencode_config() -> Path:
    return Path.home() / ".config" / "opencode" / "opencode.jsonc"


def _opencode_skills_dir() -> Path:
    return Path.home() / ".config" / "opencode" / "skills"


def _strip_jsonc_comments(text: str) -> str:
    """Remove // and /* */ comments outside string literals.

    A naive regex would eat "https://..." inside strings; this walks the
    text with a string-literal state instead.
    """
    out = []
    i, n = 0, len(text)
    in_string = False
    while i < n:
        ch = text[i]
        if in_string:
            out.append(ch)
            if ch == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
        elif ch == '"':
            in_string = True
            out.append(ch)
            i += 1
        elif ch == "/" and text[i:i + 2] == "//":
            while i < n and text[i] != "\n":
                i += 1
        elif ch == "/" and text[i:i + 2] == "/*":
            end = text.find("*/", i + 2)
            i = n if end < 0 else end + 2
            out.append(" ")
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def _opencode_load(config_path: Path) -> dict:
    if not config_path.exists():
        return {}
    text = config_path.read_text(encoding="utf-8")
    try:
        return json.loads(text)
    except ValueError:
        # JSONC: opencode's own config ships with comments. Strip them
        # (string-aware) and retry instead of aborting the install.
        try:
            data = json.loads(_strip_jsonc_comments(text))
        except ValueError as exc:
            raise SystemExit(
                f"无法解析 {config_path}——请先整理为合法 JSON 再运行: {exc}")
        # Rewriting will drop the user's comments: keep a one-shot backup.
        backup = config_path.with_name(config_path.name + ".bak")
        if not backup.exists():
            backup.write_text(text, encoding="utf-8")
            print(f"[install] {config_path} 含注释，原文件已备份: {backup}")
        return data


def install_opencode(app_id: str = "android_commerce_demo") -> None:
    """Register the MCP + skill in opencode's user config (~/.config/opencode).

    Matches the existing hand-written entries for the web conditions: local
    stdio server with explicit cwd/PYTHONPATH and a large per-request timeout,
    so slow first tools (controller/session bootstrap) never die on the
    client's 5s default.
    """
    python = str(PY) if PY.exists() else sys.executable
    config_path = _opencode_config()
    config_path.parent.mkdir(parents=True, exist_ok=True)
    data = _opencode_load(config_path)
    env = {"PYTHONPATH": str(PROJECT_ROOT)}
    if app_id != "android_commerce_demo":
        env["BBB_ANDROID_APP_ID"] = app_id
    data.setdefault("mcp", {})[SERVER_NAME] = {
        "type": "local",
        "command": [python, "-m", "agents.android_baseline.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        "environment": env,
        "enabled": True,
        "timeout": 600000,
    }
    config_path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n",
                           encoding="utf-8")
    print(f"[install] opencode MCP 已注册: {SERVER_NAME} → {config_path}")

    skill_dst = _opencode_skills_dir() / "android-blackbox-explorer"
    _copy_skill(skill_dst)
    print(f"[install] opencode skill 已安装: {skill_dst}")
    print(f"[install] 默认被测应用: {app_id}")


def uninstall_opencode() -> None:
    config_path = _opencode_config()
    if config_path.exists():
        data = _opencode_load(config_path)
        if data.get("mcp", {}).pop(SERVER_NAME, None) is not None:
            config_path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8")
            print("[install] opencode MCP 条目已移除")
    shutil.rmtree(_opencode_skills_dir() / "android-blackbox-explorer",
                  ignore_errors=True)
    print("[install] opencode skill 已移除")


def install_kimi(app_id: str = "android_commerce_demo") -> None:
    home = _kimi_home()
    home.mkdir(parents=True, exist_ok=True)
    cfg = home / "mcp.json"
    data = json.loads(cfg.read_text(encoding="utf-8")) if cfg.exists() else {}
    servers = data.setdefault("mcpServers", {})
    entry = {
        "command": str(PY) if PY.exists() else sys.executable,
        "args": ["-m", "agents.android_baseline.mcp_server"],
        "cwd": str(PROJECT_ROOT),
        # no controller/session env on purpose: the server self-bootstraps
        # controller + session on first use
    }
    if app_id != "android_commerce_demo":
        entry["env"] = {"BBB_ANDROID_APP_ID": app_id}
    servers[SERVER_NAME] = entry
    cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"[install] mcp.json 已注册: {cfg}")

    dst = home / "skills" / "android-blackbox-explorer"
    _copy_skill(dst)
    print(f"[install] skill 已安装: {dst}")
    print(f"[install] 新会话默认被测应用: {app_id}"
          + ("(默认)" if app_id == "android_commerce_demo"
             else " —— 切回Android Commerce Demo: install --app android_commerce_demo"))


def uninstall_kimi() -> None:
    home = _kimi_home()
    cfg = home / "mcp.json"
    if cfg.exists():
        data = json.loads(cfg.read_text(encoding="utf-8"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            cfg.write_text(json.dumps(data, indent=2), encoding="utf-8")
            print("[install] mcp.json 条目已移除")
    shutil.rmtree(home / "skills" / "android-blackbox-explorer", ignore_errors=True)
    print("[install] skill 已移除")


def print_claude_hint() -> None:
    py = str(PY) if PY.exists() else sys.executable
    print("[install] Claude Code 请执行(一次性,用户级):")
    print(f'  claude mcp add {SERVER_NAME} --scope user -- "{py}" '
          f'-m agents.android_baseline.mcp_server')
    print(f"  并把 {SKILL_SRC} 复制到 ~/.claude/skills/android-blackbox-explorer/")
    print("  (或用 --cwd 指向本仓库后,claude 会读取项目 .mcp.json —— 不推荐,"
          "隔离起见用用户级)")


def install_codex(app_id: str = "android_commerce_demo") -> None:
    """Register a cwd-independent stdio server and user-level Codex skill."""
    python = str(PY) if PY.exists() else sys.executable
    codex = _codex_command()
    subprocess.run([codex, "mcp", "remove", SERVER_NAME],
                   check=False, stdout=subprocess.DEVNULL,
                   stderr=subprocess.DEVNULL)
    subprocess.run([
        codex, "mcp", "add", SERVER_NAME,
        "--env", f"PYTHONPATH={PROJECT_ROOT}",
        "--env", f"BBB_ANDROID_APP_ID={app_id}",
        "--", python, "-m", "agents.android_baseline.mcp_server",
    ], check=True)
    destination = _codex_home() / "skills" / "android-blackbox-explorer"
    _copy_skill(destination)
    print(f"[install] Codex MCP 已注册: {SERVER_NAME}")
    print(f"[install] Codex skill 已安装: {destination}")


def uninstall_codex() -> None:
    subprocess.run([_codex_command(), "mcp", "remove", SERVER_NAME], check=False)
    shutil.rmtree(_codex_home() / "skills" / "android-blackbox-explorer",
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


def install_codebuddy(app_id: str = "android_commerce_demo") -> None:
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
        "args": ["-m", "agents.android_baseline.mcp_server"],
        "env": {"PYTHONPATH": str(PROJECT_ROOT)},
    }
    if app_id != "android_commerce_demo":
        entry["env"]["BBB_ANDROID_APP_ID"] = app_id
    data.setdefault("mcpServers", {})[SERVER_NAME] = entry
    config_path.write_text(json.dumps(data, indent=2, ensure_ascii=False),
                           encoding="utf-8")
    skill_dst = _codebuddy_skills_dir() / "android-blackbox-explorer"
    _copy_skill(skill_dst)
    project_skill = _codebuddy_project_skills_dir() / "android-blackbox-explorer"
    _copy_skill(project_skill)
    print(f"[install] 已写入 CodeBuddy 用户级 MCP 配置: {config_path}")
    print(f"[install] 已注册 MCP: {SERVER_NAME} (默认被测应用 {app_id})")
    print(f"[install] 已安装 skill: {skill_dst}")
    print()
    print("IDE 内一键实验流程:")
    print("  1. 重启 CodeBuddy(或重新加载窗口)让 MCP 与 skill 生效;")
    print("  2. 新开窗口,打开一个空目录(实验不要打开本仓库);")
    print("  3. 在 MCP 面板只启用 android-blackboxbench,禁用其他 MCP;")
    print("  4. 对话中调用 android-blackbox-explorer skill 即可开始:")
    print("     observe -> 坐标动作 -> discovery 记录 -> finalize")
    print("     (自动进入复现阶段) -> 本地像素复验/修正 -> finish_reproduction。")
    print("     Controller 与会话由 MCP 服务器自动自举。")
    print()
    print("严格实验时请在 Agent 客户端禁用宿主文件、Shell、浏览器和其他 MCP。")


def uninstall_codebuddy() -> None:
    config_path = _codebuddy_config()
    if config_path.exists():
        data = json.loads(config_path.read_text(encoding="utf-8-sig"))
        if data.get("mcpServers", {}).pop(SERVER_NAME, None) is not None:
            config_path.write_text(
                json.dumps(data, indent=2, ensure_ascii=False),
                encoding="utf-8")
    shutil.rmtree(_codebuddy_skills_dir() / "android-blackbox-explorer",
                  ignore_errors=True)
    shutil.rmtree(_codebuddy_project_skills_dir() / "android-blackbox-explorer",
                  ignore_errors=True)
    print(f"[install] 已从 CodeBuddy 移除 MCP 与 skill: {SERVER_NAME}")


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cli", default="kimi",
                   choices=["kimi", "claude", "codex", "codebuddy", "opencode"])
    p.add_argument("--app", default="android_commerce_demo",
                   help="自举会话的 Android 目标 app_id，如 android_commerce_demo")
    p.add_argument("--uninstall", action="store_true")
    args = p.parse_args()

    if args.cli == "kimi":
        uninstall_kimi() if args.uninstall else install_kimi(args.app)
        if not args.uninstall:
            print()
            print("完成。之后的使用方式:")
            print("  1. 新开一个 kimi 对话:  kimi")
            print("  2. 输入:  /skill:android-blackbox-explorer [目标]")
            print("     目标必须是样例或操作员预先注册的本地 APK app_id。")
            print("  探索即自动开始(Controller 与会话由 MCP 服务器自举)。")
    elif args.cli == "claude":
        print_claude_hint()
    elif args.cli == "codebuddy":
        uninstall_codebuddy() if args.uninstall else install_codebuddy(args.app)
    elif args.cli == "opencode":
        uninstall_opencode() if args.uninstall else install_opencode(args.app)
        if not args.uninstall:
            print()
            print("完成。之后的使用方式:")
            print("  1. 重启 opencode 让 MCP 与 skill 生效;")
            print("  2. 新开对话, 调用 android-blackbox-explorer skill [目标 app_id]。")
    else:
        uninstall_codex() if args.uninstall else install_codex(args.app)
        if not args.uninstall:
            print("[install] 在新的 Codex 任务中使用 $android-blackbox-explorer [目标]")


if __name__ == "__main__":
    main()
