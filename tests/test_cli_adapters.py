"""CLI adapter config tests — pure, no CLI processes launched."""
from __future__ import annotations

import json

from agents.cli_explorer.adapters import (ADAPTERS, ClaudeAdapter, CodexAdapter,
                                          KimiAdapter, OpenCodeAdapter,
                                          RunContext)

CTX = RunContext(controller="http://127.0.0.1:7800", session_id="sess_x",
                 brief="账号 alice/alice123", max_actions=400, minutes=30)


class TestAdapters:
    def test_all_registered(self):
        assert set(ADAPTERS) == {"kimi", "claude", "codex", "opencode"}

    def test_kimi(self, tmp_path):
        ad = KimiAdapter(CTX)
        ad.prepare(tmp_path)
        assert (tmp_path / "explorer.md").exists()
        body = (tmp_path / "explorer.md").read_text(encoding="utf-8")
        assert "mcp__blackboxbench__*" in body  # tool allowlist
        assert ad.needs_user_mcp_injection
        entry = ad.mcp_entry()
        assert entry["env"]["BBB_SESSION"] == "sess_x"
        assert entry["args"][:2] == ["-m", "agents.cli_explorer.mcp_server"]
        cmd = ad.command(tmp_path)
        assert cmd[0] == "kimi" and "-p" in cmd and "--agent-file" in cmd
        assert "alice123" in cmd[cmd.index("-p") + 1]  # brief in task prompt

    def test_claude(self, tmp_path):
        ad = ClaudeAdapter(CTX)
        ad.prepare(tmp_path)
        cfg = json.loads((tmp_path / "mcp.json").read_text())
        srv = cfg["mcpServers"]["blackboxbench"]
        assert srv["env"]["BBB_SESSION"] == "sess_x"
        cmd = ad.command(tmp_path)
        assert "--mcp-config" in cmd and "--disallowedTools" in cmd
        blocked = cmd[cmd.index("--disallowedTools") + 1:
                      cmd.index("--permission-mode")]
        for t in ("Bash", "Read", "Write", "WebFetch", "WebSearch"):
            assert t in blocked

    def test_codex(self, tmp_path):
        ad = CodexAdapter(CTX)
        ad.prepare(tmp_path)  # no-op by design
        cmd = ad.command(tmp_path)
        assert cmd[:2] == ["codex", "exec"]
        joined = "\n".join(cmd)
        assert "mcp_servers.blackboxbench.command" in joined
        assert "BBB_SESSION" in joined
        assert "read-only" in cmd  # sandbox restriction
        assert "--cd" in cmd

    def test_opencode(self, tmp_path):
        ad = OpenCodeAdapter(CTX)
        ad.prepare(tmp_path)
        cfg = json.loads((tmp_path / "opencode.json").read_text())
        mcp = cfg["mcp"]["blackboxbench"]
        assert mcp["type"] == "local" and mcp["enabled"]
        assert mcp["environment"]["BBB_SESSION"] == "sess_x"
        perm = cfg["permission"]
        assert perm["bash"] == "deny" and perm["read"] == "deny"
        cmd = ad.command(tmp_path)
        assert cmd[:2] == ["opencode", "run"]

    def test_task_prompt_rules(self):
        text = KimiAdapter(CTX).task_text()
        assert "observe" in text and "finalize" in text
        assert "alice123" in text           # brief injected
        assert "DOM" in text                # black-box rule stated
