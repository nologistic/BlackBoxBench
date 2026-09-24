"""Operator check: does non-ASCII typing survive on a real emulator?

`adb shell input text` is ASCII-only, so a CJK argument used to exit 255 and end
the whole exploration. The fix routes non-ASCII through a per-character UTF-8
escape and downgrades a rejection to ValueError, which the Session layer turns
into a 400 (agent gets an error, session keeps running).

This drives the real MCP the way an Agent does: boot, open the world-clock
search field, type Chinese, and confirm the session is still alive afterwards.

    vendor/python/python.exe scripts/android_typing_smoke.py --app google_clock
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

MODULE = "agents.android_baseline.mcp_server"


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


def _body(result: dict) -> tuple[bool, dict | str]:
    body = result.get("result", result)
    text = ""
    for item in body.get("content", []):
        if item.get("type") == "text":
            text = item["text"]
            break
    if body.get("isError"):
        return True, text
    try:
        return False, json.loads(text)
    except json.JSONDecodeError:
        return False, text


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", default="google_clock")
    parser.add_argument("--text", default="北京")
    args = parser.parse_args()

    client = Client(MODULE)
    session_id = ""
    try:
        client.call("initialize")
        failed, started = _body(client.tool("start_session",
                                           {"app_id": args.app}))
        assert not failed, started
        session_id = started["session_id"]
        print(f"[typing] session       : {session_id}")

        failed, observed = _body(client.tool("observe"))
        assert not failed, observed
        width, height = observed["width"], observed["height"]
        print(f"[typing] geometry      : {width}x{height}")

        # Any focusable field will do; the point is the non-ASCII path, not the
        # particular screen. Tap the search affordance area, then type.
        client.tool("tap", {"x": width // 2, "y": int(height * 0.5)})
        failed, receipt = _body(client.tool("type_text", {"text": args.text}))
        print(f"[typing] non-ascii     : "
              f"{'REJECTED -> ' if failed else 'accepted '}{receipt}")

        failed, after = _body(client.tool("observe"))
        assert not failed, after
        print(f"[typing] session alive : frame {after['frame_id']}, "
              f"{after['budget']['actions_remaining']} actions left")
        print()
        print("[typing] RESULT: a non-ASCII type_text no longer ends the "
              "session — either it was accepted, or it was refused as a "
              "recoverable error while the session kept running.")
    finally:
        if session_id:
            import httpx
            with httpx.Client(timeout=httpx.Timeout(600.0, connect=10.0),
                              trust_env=False) as http:
                http.post("http://127.0.0.1:7800/api/sessions/"
                          f"{session_id}/close", json={})
        client.close()


if __name__ == "__main__":
    main()
