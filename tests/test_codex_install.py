"""Codex/CodeBuddy registration tests for both exploration conditions."""
from __future__ import annotations

import json
import subprocess
import sys

from agents.cli_explorer import install as managed_install
from agents.app_review import install as app_review_install
from self_explorer import install as self_install


def _completed(argv, **_kwargs):
    return subprocess.CompletedProcess(argv, 0, "", "")


def test_managed_codex_install_registers_mcp_and_skill(monkeypatch, tmp_path):
    calls = []

    def run(argv, **kwargs):
        calls.append(argv)
        return _completed(argv, **kwargs)

    monkeypatch.setattr(managed_install, "_codex_home", lambda: tmp_path)
    monkeypatch.setattr(managed_install, "_codex_command", lambda: "codex.cmd")
    monkeypatch.setattr(managed_install.subprocess, "run", run)
    managed_install.install_codex("ecommerce_demo")

    add = calls[-1]
    assert add[:5] == ["codex.cmd", "mcp", "add", "blackboxbench", "--env"]
    assert "BBB_APP_ID=ecommerce_demo" in add
    expected = (str(managed_install.PY) if managed_install.PY.exists()
                else sys.executable)
    assert add[-3:] == [expected, "-m",
                        "agents.cli_explorer.mcp_server"]
    assert (tmp_path / "skills" / "blackbox-explorer" / "SKILL.md").is_file()


def test_self_built_codex_install_is_separate(monkeypatch, tmp_path):
    calls = []

    def run(argv, **kwargs):
        calls.append(argv)
        return _completed(argv, **kwargs)

    monkeypatch.setattr(self_install, "_codex_home", lambda: tmp_path)
    monkeypatch.setattr(self_install, "_codex_command", lambda: "codex.cmd")
    monkeypatch.setattr(self_install.subprocess, "run", run)
    self_install.install_codex("ecommerce_demo")

    add = calls[-1]
    assert add[:5] == ["codex.cmd", "mcp", "add",
                       "blackboxbench-self-built", "--env"]
    assert "BBB_SELF_APP_ID=ecommerce_demo" in add
    assert "PYTHONUTF8=1" in add
    expected = (str(self_install.PYTHON) if self_install.PYTHON.exists()
                else sys.executable)
    assert add[-3:] == [expected, "-m",
                        "self_explorer.mcp_server"]
    assert (tmp_path / "skills" / "self-built-explorer" / "SKILL.md").is_file()


def test_app_review_codex_install_registers_judge_only(monkeypatch, tmp_path):
    calls = []

    def run(argv, **kwargs):
        calls.append(argv)
        return _completed(argv, **kwargs)

    monkeypatch.setattr(app_review_install, "_codex_home", lambda: tmp_path)
    monkeypatch.setattr(app_review_install, "_codex_command",
                        lambda: "codex.cmd")
    monkeypatch.setattr(app_review_install.subprocess, "run", run)
    app_review_install.install_codex("demo.json")

    add = calls[-1]
    assert add[:5] == ["codex.cmd", "mcp", "add", "app-review", "--env"]
    # The checklist env default was removed: the MCP requires an
    # explicit checklist per evaluation (a silent default graded the
    # wrong object on 2026-09-08).
    assert "BBB_APP_REVIEW_CHECKLIST" not in " ".join(add)
    assert add[-3:] == [app_review_install._python(), "-m",
                        "agents.app_review.mcp_server"]
    skill = tmp_path / "skills" / "app-review" / "SKILL.md"
    assert skill.is_file()
    text = skill.read_text(encoding="utf-8")
    assert "只调用 `app-review` MCP" in text
    assert "android-blackboxbench" not in add


# ------------------------------------------------------ CodeBuddy (user level)

def _codebuddy_config(tmp_path):
    path = tmp_path / ".codebuddy" / ".mcp.json"
    skills = tmp_path / ".codebuddy" / "skills"
    managed_install._codebuddy_config = lambda: path
    self_install._codebuddy_config = lambda: path
    managed_install._codebuddy_skills_dir = lambda: skills
    self_install._codebuddy_skills_dir = lambda: skills
    managed_install._codebuddy_project_skills_dir = lambda: (
        managed_install.PROJECT_ROOT / ".codebuddy" / "skills")
    self_install._codebuddy_project_skills_dir = lambda: (
        self_install.PROJECT_ROOT / ".codebuddy" / "skills")
    return path


def test_codebuddy_registration_merges_and_keeps_both_conditions(tmp_path):
    path = _codebuddy_config(tmp_path)
    user_skills = tmp_path / ".codebuddy" / "skills"
    project_skills = managed_install.PROJECT_ROOT / ".codebuddy" / "skills"
    # pre-existing unrelated server must survive the merge
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"mcpServers": {"other": {"type": "stdio",
                                                         "command": "x"}}}),
                    encoding="utf-8-sig")

    managed_install.install_codebuddy("ecommerce_demo")
    self_install.install_codebuddy("ecommerce_demo")

    data = json.loads(path.read_text(encoding="utf-8"))
    assert "other" in data["mcpServers"]
    managed = data["mcpServers"]["blackboxbench"]
    self_built = data["mcpServers"]["blackboxbench-self-built"]
    assert managed["type"] == "stdio" and self_built["type"] == "stdio"
    assert managed["args"] == ["-m", "agents.cli_explorer.mcp_server"]
    assert self_built["args"] == ["-m", "self_explorer.mcp_server"]
    assert managed["env"]["PYTHONPATH"] == str(managed_install.PROJECT_ROOT)
    assert self_built["env"]["BBB_SELF_APP_ID"] == "ecommerce_demo"
    assert self_built["env"]["PYTHONUTF8"] == "1"
    # user-level skills (always available, the correct path CodeBuddy scans)
    assert (user_skills / "blackbox-explorer" / "SKILL.md").is_file()
    assert (user_skills / "self-built-explorer" / "SKILL.md").is_file()
    # project-level skills (for the current repo workspace)
    assert (project_skills / "blackbox-explorer" / "SKILL.md").is_file()
    assert (project_skills / "self-built-explorer" / "SKILL.md").is_file()
    # 正式实验要求一次只启用一个条件：注册必须可独立移除
    self_install.uninstall_codebuddy()
    data = json.loads(path.read_text(encoding="utf-8"))
    assert "blackboxbench-self-built" not in data["mcpServers"]
    assert not (user_skills / "self-built-explorer" / "SKILL.md").exists()
    assert not (project_skills / "self-built-explorer" / "SKILL.md").exists()
    assert "blackboxbench" in data["mcpServers"] and "other" in data["mcpServers"]
    assert (user_skills / "blackbox-explorer" / "SKILL.md").is_file()
