"""CLI: run the Kimi VLM explorer against a benchmark session.

  # terminal 1
  vendor/python/python.exe -m benchmark.server --port 7800
  # terminal 2 (key 通过环境变量提供)
  set BBB_VLM_API_KEY=sk-...        # PowerShell: $env:BBB_VLM_API_KEY="sk-..."
  vendor/python/python.exe -m agents.kimi_explorer.run
"""
from __future__ import annotations

import argparse
import sys

import httpx

from blackbox_bench_sdk import Environment

from .agent import Explorer
from .client import KimiClient
from .config import VLMConfig


def main() -> None:
    p = argparse.ArgumentParser(description="Kimi VLM black-box explorer")
    p.add_argument("--controller", default="http://127.0.0.1:7800")
    p.add_argument("--app-id", default="ecommerce_demo")
    p.add_argument("--session", default=None,
                   help="attach to an existing session instead of creating one")
    p.add_argument("--max-turns", type=int, default=250)
    p.add_argument("--max-actions", type=int, default=400)
    p.add_argument("--minutes", type=int, default=30)
    args = p.parse_args()

    cfg = VLMConfig.from_env()
    cfg.validate()

    sid = args.session
    if not sid:
        with httpx.Client(timeout=180, trust_env=False) as c:
            r = c.post(f"{args.controller.rstrip('/')}/api/sessions", json={
                "app_id": args.app_id,
                "budget": {"max_actions": args.max_actions,
                           "max_duration_s": args.minutes * 60,
                           "max_observations": args.max_turns + 50},
            })
            r.raise_for_status()
            sid = r.json()["session_id"]
    print(f"[kimi-explorer] session: {sid}  model: {cfg.model} @ {cfg.base_url}")

    env = Environment(args.controller, sid, timeout=180)
    vlm = KimiClient(cfg)
    explorer = Explorer(env, vlm, max_turns=args.max_turns,
                        history_images=cfg.history_images)
    try:
        result = explorer.run()
    finally:
        vlm.close()
        env.close()
    print(f"[kimi-explorer] done: {result}")
    print(f"[kimi-explorer] stats: turns={explorer.stats.turns} "
          f"actions={explorer.stats.actions} "
          f"discoveries={explorer.stats.discoveries} "
          f"parse_retries={explorer.stats.parse_retries}")
    if "summary" not in result:
        sys.exit(1)


if __name__ == "__main__":
    main()
