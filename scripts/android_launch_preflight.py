"""Operator preflight: does `$android-blackbox-explorer <app>` actually work?

Drives the condition's MCP server exactly the way an Agent client does — real
stdio subprocess, real JSON-RPC — and stops right after the first observe. It
verifies the whole launch chain without consuming an exploration budget:

    initialize -> tools/list -> list_targets -> start_session(app_id) -> observe

    vendor/python/python.exe scripts/android_launch_preflight.py --app google_clock
    vendor/python/python.exe scripts/android_launch_preflight.py --app google_clock \
        --condition our-method --close

`--close` closes the session it created so the check leaves nothing behind.
Without it the booted emulator stays up for inspection.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

MODULES = {
    "baseline": "agents.android_baseline.mcp_server",
    "our-method": "agents.android_our_method.mcp_server",
}


class Client:
    def __init__(self, module: str):
        env = dict(os.environ)
        env["PYTHONPATH"] = str(ROOT)
        env["PYTHONIOENCODING"] = "utf-8"
        self.process = subprocess.Popen(
            [sys.executable, "-m", module], cwd=str(ROOT), env=env,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, text=True, encoding="utf-8",
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        self.counter = 0

    def call(self, method: str, params: dict | None = None) -> dict:
        self.counter += 1
        self.process.stdin.write(json.dumps(
            {"jsonrpc": "2.0", "id": self.counter, "method": method,
             "params": params or {}}) + "\n")
        self.process.stdin.flush()
        line = self.process.stdout.readline()
        if not line:
            raise RuntimeError("MCP server closed the connection")
        return json.loads(line)

    def tool(self, name: str, arguments: dict | None = None) -> dict:
        return self.call("tools/call", {"name": name,
                                        "arguments": arguments or {}})

    def close(self) -> None:
        try:
            self.process.stdin.close()
        except OSError:
            pass
        try:
            self.process.wait(timeout=180)
        except subprocess.TimeoutExpired:
            self.process.kill()


def _payload(result: dict) -> dict:
    body = result.get("result", result)
    if body.get("isError"):
        raise RuntimeError(body["content"][0]["text"])
    for item in body.get("content", []):
        if item.get("type") == "text":
            try:
                return json.loads(item["text"])
            except json.JSONDecodeError:
                return {"text": item["text"]}
    return {}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", required=True)
    parser.add_argument("--condition", choices=sorted(MODULES),
                        default="baseline")
    parser.add_argument("--close", action="store_true",
                        help="close the created session before exiting")
    args = parser.parse_args()

    client = Client(MODULES[args.condition])
    session_id = ""
    try:
        info = client.call("initialize")["result"]["serverInfo"]
        print(f"[preflight] server        : {info['name']} {info['version']}")

        tools = [t["name"] for t in
                 client.call("tools/list")["result"]["tools"]]
        print(f"[preflight] tools         : {len(tools)}")
        for required in ("observe", "tap", "start_session", "list_targets",
                         "record_feature", "finalize"):
            assert required in tools, f"missing tool: {required}"

        targets = _payload(client.tool("list_targets"))
        available = [t["app_id"] for t in targets.get("registered", [])]
        print(f"[preflight] targets       : {available}")
        print(f"[preflight] default       : {targets.get('default_target')}")
        assert args.app in available, f"{args.app} is not registered"

        print(f"[preflight] starting session for {args.app} "
              f"(boots an emulator; this takes a few minutes) …")
        started = _payload(client.tool("start_session", {"app_id": args.app}))
        session_id = started.get("session_id", "")
        print(f"[preflight] session       : {session_id}")
        print(f"[preflight] brief         : {started.get('brief', '')[:60]}…")

        observed = _payload(client.tool("observe"))
        print(f"[preflight] observe       : {observed}")
        print()
        print("[preflight] RESULT: launch chain works — "
              f"$android-{'blackbox-explorer' if args.condition == 'baseline' else 'our-method'} "
              f"{args.app}")
    finally:
        if args.close and session_id:
            print("[preflight] closing the session …")
            import httpx
            with httpx.Client(timeout=httpx.Timeout(600.0, connect=10.0),
                              trust_env=False) as http:
                http.post("http://127.0.0.1:7800/api/sessions/"
                          f"{session_id}/close", json={})
        client.close()


if __name__ == "__main__":
    main()
