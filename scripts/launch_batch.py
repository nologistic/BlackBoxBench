#!/usr/bin/env python3
"""Batch launcher for exploration and evaluation runs.

Replaces the hand-rolled ``sleep 300; script -qec "…" &`` dance: one command
starts any mix of Android exploration runs and astra evaluations, with
bounded parallelism, per-task log files and a summary table. Tasks run in
the canonical directories (exploration under /storage/dzj/runs, evaluation
under /storage/dzj/review) with the same environment the manual launches
used (NO_PROXY for the local controller, DISPLAY for headed tooling, and a
TTY via ``script`` for the agent CLIs that insist on one).

Examples:
    # one astra evaluation
    scripts/launch_batch.py eval vinyl sess_20260921_153303_4a8a3f-88bd6c vinyl.json

    # three explorations (kimi / dsh / zcode) and two evaluations, at most 4 at once
    scripts/launch_batch.py --max-parallel 4 \
        explore kimi markor explore dsh nonogram explore zcode librera

    # same, but wait for everything and report exit codes
    scripts/launch_batch.py --wait eval vinyl sess_… vinyl.json

    # web baseline instead of Android (4th token = condition)
    scripts/launch_batch.py explore codex ecommerce_demo web

With ``--jobs FILE`` a JSON list of tasks may be used instead:

    [{"kind": "eval", "target": "vinyl",
      "handoff": "sess_…", "checklist": "vinyl.json"},
     {"kind": "explore", "agent": "kimi", "target": "markor"},
     {"kind": "explore", "agent": "codex", "target": "ecommerce_demo",
      "condition": "web"}]

Machine-local paths are resolved as follows (all overridable):
    BBB_EXPLORE_CWD  agent cwd for explorations   (default: <repo>/../scratch/explore)
    BBB_EVAL_CWD     agent cwd for evaluations    (default: <repo>/../scratch/review)
    BBB_AGENT_<NAME> agent binary                 (default: PATH, then /home/dzj/bin)
    BBB_EVAL_MODEL / BBB_EVAL_EFFORT              judge model / reasoning effort
"""
from __future__ import annotations

import argparse
import json
import os
import shlex
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
# Working directories the agent CLIs are launched in (NOT the repo's evidence
# `runs/`): keeping the agent's cwd outside the repository is part of the
# isolation story. Defaults live under the workspace scratch dir the project
# already owns (workspace = repo's parent, see benchmark/config.py SCRATCH_DIR);
# override with BBB_EXPLORE_CWD / BBB_EVAL_CWD.
_SCRATCH = Path(os.environ.get("BBB_SCRATCH_DIR", str(REPO.parent / "scratch")))
EXPLORE_CWD = Path(os.environ.get("BBB_EXPLORE_CWD", str(_SCRATCH / "explore")))
EVAL_CWD = Path(os.environ.get("BBB_EVAL_CWD", str(_SCRATCH / "review")))
LOG_DIR = Path(os.environ.get("BBB_BATCH_LOG_DIR", "/tmp"))


def _agent(name: str, *default_args: str) -> list[str]:
    """Resolve an agent CLI: BBB_AGENT_<NAME> > PATH > legacy absolute path."""
    exe = (os.environ.get(f"BBB_AGENT_{name.upper()}") or shutil.which(name)
           or f"/home/dzj/bin/{name}")
    return [exe, *default_args]


AGENTS = {
    "dsh": _agent("dsh", "--profile", "headless"),
    "kimi": _agent("kimi", "-p"),
    "zcode": _agent("zcode", "--prompt"),
    # codex 非交互式：与评测用的 codex exec 同形态，只是提示词换成探索 Skill
    "codex": _agent("codex", "exec", "--skip-git-repo-check",
                    "-s", "danger-full-access"),
}

# 各 CLI 引用 Skill 的语法不同（实测口径，见 launch_kimi_batch.sh /
# launch_batch.py 历史版本）：dsh/codex/zcode 用 $skill，其余用 /skill。
SKILL_PREFIX = {"dsh": "$", "codex": "$", "zcode": "$"}

# 可选：探索阶段使用的模型（透传给支持 -m 的 CLI，如 codex）。
EXPLORE_MODEL = os.environ.get("BBB_EXPLORE_MODEL", "")

# Skill invoked per exploration condition (see agents/*/skill/*/SKILL.md).
SKILLS = {
    "android": "android-blackbox-explorer",   # Android baseline
    "web": "blackbox-explorer",               # 网页 baseline
    "nograph": "baseline-nograph",            # Android 消融（不要求记录）
}

# Judge settings are pinned so every evaluation is comparable (see
# docs/evaluation_contract.md §1.1 "评测模型唯一性").
EVAL_MODEL = os.environ.get("BBB_EVAL_MODEL", "gpt-6-astra")
EVAL_EFFORT = os.environ.get("BBB_EVAL_EFFORT", "xhigh")


@dataclass
class Task:
    kind: str                    # "explore" | "eval"
    target: str
    agent: str = ""              # explore only
    condition: str = "android"   # explore only: android | web | nograph
    handoff: str = ""            # eval only
    checklist: str = ""          # eval only
    name: str = ""
    command: list[str] = field(default_factory=list)
    cwd: Path = EVAL_CWD
    log: Path = LOG_DIR / "batch.log"
    proc: subprocess.Popen | None = None

    def build(self) -> None:
        if self.kind == "explore":
            if self.agent not in AGENTS:
                raise SystemExit(f"unknown agent '{self.agent}' "
                                 f"(known: {', '.join(AGENTS)})")
            skill_name = SKILLS.get(self.condition)
            if skill_name is None:
                raise SystemExit(f"unknown condition '{self.condition}' "
                                 f"(known: {', '.join(SKILLS)})")
            prefix = SKILL_PREFIX.get(self.agent, "/")
            skill = f"{prefix}{skill_name} {self.target}"
            self.command = [*AGENTS[self.agent]]
            if EXPLORE_MODEL and self.agent in ("codex", "opencode", "zcode"):
                self.command += ["-m", EXPLORE_MODEL]
            self.command.append(skill)
            # nograph 条件必须只挂 nograph 的 MCP（条件隔离）：
            # 换成专用 profile —— 否则 agent 会拿到 baseline 的记录工具面 ✗
            # （2026-09-27 实测：误连 baseline MCP，消融失效）。
            if self.agent == "dsh" and self.condition == "nograph":
                self.command = ["headless-nograph" if c == "headless" else c
                                for c in self.command]
            self.cwd = EXPLORE_CWD
            suffix = "" if self.condition == "android" else f"_{self.condition}"
            self.name = self.name or f"explore_{self.agent}_{self.target}{suffix}"
        else:
            prompt = (f"/app-review {self.target}（handoff_id={self.handoff}，"
                      f"checklist={self.checklist}）")
            codex = (os.environ.get("BBB_AGENT_CODEX") or shutil.which("codex")
                     or "/home/dzj/bin/codex")
            self.command = [codex, "exec", "-m", EVAL_MODEL,
                            "-c", f"model_reasoning_effort={EVAL_EFFORT}",
                            "--skip-git-repo-check", "-s",
                            "danger-full-access", prompt]
            self.cwd = EVAL_CWD
            self.name = self.name or f"eval_{self.target}"
        stamp = datetime.now().strftime("%m%d_%H%M%S")
        self.log = LOG_DIR / f"{self.name}_{stamp}.log"


def launch(task: Task) -> None:
    for path in (task.cwd,):
        path.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ)
    env.pop("BBB_RUNS_DIR", None)
    env["NO_PROXY"] = "127.0.0.1,localhost"
    env.setdefault("DISPLAY", ":99")
    inner = " ".join(shlex.quote(c) for c in task.command)
    with open(task.log, "wb") as fh:
        task.proc = subprocess.Popen(
            ["script", "-qec", inner, "/dev/null"],
            cwd=str(task.cwd), env=env, stdout=fh, stderr=subprocess.STDOUT,
            start_new_session=True)
    print(f"[launch] {task.name:28s} pid={task.proc.pid:<8d} "
          f"cwd={task.cwd} log={task.log}", flush=True)


def parse_tasks(args) -> list[Task]:
    if args.jobs:
        raw = json.loads(Path(args.jobs).read_text(encoding="utf-8"))
        tasks = []
        for item in raw:
            kind = item.get("kind") or item.get("type")
            if kind in ("eval", "evaluate", "app-review"):
                tasks.append(Task(kind="eval", target=item["target"],
                                  handoff=item["handoff"],
                                  checklist=item["checklist"]))
            elif kind in ("explore", "exploration"):
                tasks.append(Task(kind="explore", target=item["target"],
                                  agent=item["agent"],
                                  condition=item.get("condition", "android")))
            else:
                raise SystemExit(f"unknown job kind: {kind!r}")
        return tasks
    tokens = list(args.tasks)
    tasks: list[Task] = []
    i = 0
    while i < len(tokens):
        kind = tokens[i]
        if kind == "eval":
            if i + 3 >= len(tokens) + 0 and i + 3 > len(tokens) - 1:
                raise SystemExit("eval needs: TARGET HANDOFF CHECKLIST")
            tasks.append(Task(kind="eval", target=tokens[i + 1],
                              handoff=tokens[i + 2],
                              checklist=tokens[i + 3]))
            i += 4
        elif kind == "explore":
            if i + 2 >= len(tokens):
                raise SystemExit("explore needs: AGENT TARGET [CONDITION]")
            # Optional 4th token selects the condition; conditions are not task
            # kinds, so the parse stays unambiguous.
            condition = "android"
            consumed = 3
            if i + 3 < len(tokens) and tokens[i + 3] in SKILLS:
                condition = tokens[i + 3]
                consumed = 4
            tasks.append(Task(kind="explore", agent=tokens[i + 1],
                              target=tokens[i + 2], condition=condition))
            i += consumed
        else:
            raise SystemExit(f"unknown task kind: {kind!r} "
                             "(expected 'eval' or 'explore')")
    return tasks


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--jobs", help="JSON file with a task list")
    ap.add_argument("--max-parallel", type=int,
                    default=int(os.environ.get("BBB_BATCH_PARALLEL", "16")),
                    help="how many tasks may run at once (default 16, "
                         "matching BBB_ANDROID_BOOT_CONCURRENCY)")
    ap.add_argument("--wait", action="store_true",
                    help="wait for all tasks and report exit codes")
    ap.add_argument("tasks", nargs="*",
                    help="eval TARGET HANDOFF CHECKLIST | explore AGENT TARGET [android|web|nograph]")
    args = ap.parse_args()

    tasks = parse_tasks(args)
    if not tasks:
        ap.print_help()
        return 2
    for t in tasks:
        t.build()
    print(f"[launch] {len(tasks)} task(s), max_parallel={args.max_parallel}")
    for t in tasks:
        while True:
            alive = [x for x in tasks if x.proc and x.proc.poll() is None]
            if len(alive) < args.max_parallel:
                break
            time.sleep(2)
        launch(t)
    if args.wait:
        failed = 0
        for t in tasks:
            code = t.proc.wait()
            status = "ok" if code == 0 else f"exit {code}"
            print(f"[launch] {t.name:28s} {status}  log={t.log}")
            failed += code != 0
        return 1 if failed else 0
    print("[launch] tasks running in background; "
          "watch the log paths above (or rerun with --wait)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
