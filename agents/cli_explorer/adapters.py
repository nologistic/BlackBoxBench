"""Per-CLI adapters: materialize an isolated workspace + configs, and build the
headless command that runs the agent framework with ONLY our MCP tools.

Black-box enforcement strategy (defense in depth):
1. cwd = an isolated, empty workspace (the repo with sources/ground truth is
   NOT the working directory);
2. CLI-level tool restrictions (kimi agent-file allowlist / claude
   disallowedTools / codex read-only sandbox / opencode permission deny);
3. the task prompt forbids any local file/shell/web use;
4. residual risk of local mode (a shell-capable agent sniffing loopback ports
   or absolute paths) is documented in docs/security_model.md — container
   deployment closes it.

Every adapter gets the MCP server command as absolute paths; the server reads
BBB_CONTROLLER / BBB_SESSION from env.
"""
from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
PY = PROJECT_ROOT / "vendor" / "python" / "python.exe"
MCP_MODULE = "agents.cli_explorer.mcp_server"
SERVER_NAME = "blackboxbench"

TASK_TEMPLATE = """你在一个黑盒软件理解 Benchmark 中。一个 Web 应用正在运行,你要像第一次
使用它的人类一样,通过截图和坐标级 GUI 操作,系统性地探索并理解它的功能,
最后产出功能拓扑。

# 严格规则
- 你只能使用名为 blackboxbench 的 MCP 工具(observe/click/type_text/key_press/
  scroll/drag/wait/record_*/revise/finalize)。
- 禁止使用任何本地文件、shell、网络访问;不要尝试读取本机任何路径。
- 你没有 DOM、没有 URL——observe 返回的截图是你唯一的信息来源。

# 探索要求
1. 先 observe 看首页,识别可交互元素;每次动作后 observe 确认效果。
2. 覆盖主要功能 + 错误路径(空输入/错误输入/超量)+ 持久化(F5 刷新后状态)。
3. 在回复中说明你的观察与计划——运行你的 CLI 界面会实时展示给用户。
4. 发现用 record_state / record_feature / record_data / record_edge 结构化记录,
   evidence 用真实 step 与 frame_id;没验证的猜测用 record_hypothesis 登记,
   验证后 resolve_hypothesis。confirmed 的声明必须有 evidence。
5. 描述只写可观察行为,禁止任何实现细节(URL/接口/框架名等会被系统拒收)。
6. 覆盖完成后调用 finalize。

任务简报: {brief}
坐标系: 1440×900, 原点左上。动作预算 {max_actions} 次, 时间 {minutes} 分钟。
"""

KIMI_AGENT_MD = """---
name: blackbox-explorer
description: 黑盒 App 功能拓扑探索 Agent(仅允许 blackboxbench MCP 工具)
tools:
  - mcp__blackboxbench__*
---

你是一个黑盒软件理解 Benchmark 中的探索 Agent。你只能使用 blackboxbench MCP
服务器提供的 GUI 工具: observe 看截图,click/type/key/scroll 等坐标级操作,
record_* 记录结构化发现,finalize 收尾。

你没有文件系统、shell、网络工具——也不允许以任何方式试图获取它们。
严格按照用户消息中的任务要求执行系统性探索。
"""


@dataclass
class RunContext:
    controller: str
    session_id: str
    brief: str
    max_actions: int
    minutes: int
    extra_env: dict | None = None


class CLIAdapter:
    name = "base"
    #: whether the runner must temporarily inject the MCP server entry into the
    #: user-level CLI config (kimi) instead of a workspace-local file
    needs_user_mcp_injection = False

    def __init__(self, ctx: RunContext):
        self.ctx = ctx

    @property
    def mcp_command(self) -> list[str]:
        return [str(PY), "-m", MCP_MODULE]

    def mcp_env(self) -> dict:
        return {"BBB_CONTROLLER": self.ctx.controller,
                "BBB_SESSION": self.ctx.session_id}

    def task_text(self) -> str:
        return TASK_TEMPLATE.format(brief=self.ctx.brief or "(无)",
                                    max_actions=self.ctx.max_actions,
                                    minutes=self.ctx.minutes)

    def prepare(self, ws: Path) -> None:
        raise NotImplementedError

    def command(self, ws: Path) -> list[str]:
        raise NotImplementedError


class KimiAdapter(CLIAdapter):
    name = "kimi"
    #: kimi only loads project mcp.json for *trusted* folders (headless print
    #: mode can't answer the trust prompt), so the runner injects the server
    #: entry into the user-level ~/.kimi-code/mcp.json for the duration of the
    #: run and restores the original afterwards.
    needs_user_mcp_injection = True

    def mcp_entry(self) -> dict:
        return {"command": self.mcp_command[0],
                "args": self.mcp_command[1:],
                "env": self.mcp_env(),
                "cwd": str(PROJECT_ROOT)}

    def prepare(self, ws: Path) -> None:
        (ws / "explorer.md").write_text(KIMI_AGENT_MD, encoding="utf-8")

    def command(self, ws: Path) -> list[str]:
        return ["kimi", "-p", self.task_text(),
                "--agent-file", str(ws / "explorer.md")]


class ClaudeAdapter(CLIAdapter):
    name = "claude"

    DISALLOWED = ["Bash", "Read", "Write", "Edit", "MultiEdit", "NotebookEdit",
                  "Glob", "Grep", "LS", "WebFetch", "WebSearch", "Task",
                  "TodoWrite", "SlashCommand", "Skill", "Agent", "NotebookRead"]

    def prepare(self, ws: Path) -> None:
        (ws / "mcp.json").write_text(json.dumps({
            "mcpServers": {SERVER_NAME: {
                "command": self.mcp_command[0],
                "args": self.mcp_command[1:],
                "env": self.mcp_env(),
                "cwd": str(PROJECT_ROOT),
            }}}, indent=2), encoding="utf-8")

    def command(self, ws: Path) -> list[str]:
        return ["claude", "-p", self.task_text(),
                "--mcp-config", str(ws / "mcp.json"),
                "--disallowedTools", *self.DISALLOWED,
                "--permission-mode", "bypassPermissions"]


class CodexAdapter(CLIAdapter):
    name = "codex"

    def prepare(self, ws: Path) -> None:
        pass  # all config via -c overrides (never touch the user's ~/.codex)

    def command(self, ws: Path) -> list[str]:
        env = self.mcp_env()
        env_toml = ", ".join(f'{k} = "{v}"' for k, v in env.items())
        return [
            "codex", "exec",
            "--cd", str(ws),
            "--sandbox", "read-only",
            "-c", 'approval_policy = "never"',
            "-c", f'mcp_servers.{SERVER_NAME}.command = "{self.mcp_command[0]}"',
            "-c", 'mcp_servers.%s.args = ["%s"]' % (
                SERVER_NAME, '", "'.join(self.mcp_command[1:])),
            "-c", f"mcp_servers.{SERVER_NAME}.env = {{ {env_toml} }}",
            self.task_text(),
        ]


class OpenCodeAdapter(CLIAdapter):
    name = "opencode"

    def prepare(self, ws: Path) -> None:
        (ws / "opencode.json").write_text(json.dumps({
            "$schema": "https://opencode.ai/config.json",
            "mcp": {SERVER_NAME: {
                "type": "local",
                "command": self.mcp_command,
                "enabled": True,
                "environment": self.mcp_env(),
            }},
            "permission": {
                "bash": "deny", "edit": "deny", "write": "deny",
                "read": "deny", "grep": "deny", "glob": "deny",
                "list": "deny", "webfetch": "deny", "task": "deny",
            },
        }, indent=2), encoding="utf-8")

    def command(self, ws: Path) -> list[str]:
        return ["opencode", "run", self.task_text()]


ADAPTERS: dict[str, type[CLIAdapter]] = {
    a.name: a for a in (KimiAdapter, ClaudeAdapter, CodexAdapter,
                        OpenCodeAdapter)
}
