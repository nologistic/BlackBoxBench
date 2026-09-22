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

With ``--jobs FILE`` a JSON list of tasks may be used instead:

    [{"kind": "eval", "target": "vinyl",
      "handoff": "sess_…", "checklist": "vinyl.json"},
     {"kind": "explore", "agent": "kimi", "target": "markor"}]
"""
from __future__ import annotations

import argparse
import json
import os
import shlex
import subprocess
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
EXPLORE_CWD = Path("/storage/dzj/runs")
EVAL_CWD = Path("/storage/dzj/review")
LOG_DIR = Path("/tmp")

AGENTS = {
    "dsh": ["/home/dzj/bin/dsh", "--profile", "headless"],
    "kimi": ["/home/dzj/.kimi-code/bin/kimi", "-p"],
    "zcode": ["/home/dzj/bin/zcode", "--prompt"],
}


@dataclass
class Task:
    kind: str                    # "explore" | "eval"
    target: str
    agent: str = ""              # explore only
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
            skill = f"/android-blackbox-explorer {self.target}"
            if self.agent == "dsh":
                skill = f"$android-blackbox-explorer {self.target}"
            self.command = [*AGENTS[self.agent], skill]
            self.cwd = EXPLORE_CWD
            self.name = self.name or f"explore_{self.agent}_{self.target}"
        else:
            prompt = (f"/app-review {self.target}（handoff_id={self.handoff}，"
                      f"checklist={self.checklist}）")
            self.command = ["/home/dzj/bin/codex", "exec", "-m", "gpt-6-astra",
                            "-c", "model_reasoning_effort=xhigh",
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
                                  agent=item["agent"]))
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
            tasks.append(Task(kind="explore", agent=tokens[i + 1],
                              target=tokens[i + 2]))
            i += 3
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
                    help="eval TARGET HANDOFF CHECKLIST | explore AGENT TARGET")
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
