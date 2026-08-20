"""Scratch/manual smoke for the MCP stdio server.

Usage: start controller first, then
  vendor/python/python.exe scripts/mcp_smoke.py
Creates a miniapp session, handshakes MCP, lists tools, calls observe+click.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

import httpx


def rpc(proc: subprocess.Popen, method: str, params: dict | None = None,
        mid: int = 1) -> dict:
    proc.stdin.write(json.dumps(
        {"jsonrpc": "2.0", "id": mid, "method": method,
         "params": params or {}}) + "\n")
    proc.stdin.flush()
    while True:
        line = proc.stdout.readline()
        if not line:
            raise RuntimeError("server closed stdout")
        msg = json.loads(line)
        if msg.get("id") == mid:
            return msg


def main() -> None:
    http = httpx.Client(base_url="http://127.0.0.1:7800", trust_env=False,
                        timeout=120)
    r = http.post("/api/sessions", json={"app_id": "ecommerce_demo"})
    r.raise_for_status()
    sid = r.json()["session_id"]
    print("session:", sid)

    env = dict(os.environ)
    env["BBB_SESSION"] = sid
    env["BBB_CONTROLLER"] = "http://127.0.0.1:7800"
    proc = subprocess.Popen(
        [str(ROOT / "vendor/python/python.exe"), "-m",
         "agents.cli_explorer.mcp_server"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, env=env, text=True,
        cwd=str(ROOT))
    try:
        init = rpc(proc, "initialize", {
            "protocolVersion": "2025-06-18",
            "capabilities": {}, "clientInfo": {"name": "smoke", "version": "0"}}, 1)
        print("initialize OK:", init["result"]["serverInfo"])
        proc.stdin.write(json.dumps(
            {"jsonrpc": "2.0", "method": "notifications/initialized"}) + "\n")
        proc.stdin.flush()

        tools = rpc(proc, "tools/list", mid=2)["result"]["tools"]
        names = [t["name"] for t in tools]
        print(f"tools ({len(names)}):", ",".join(names))
        assert "observe" in names and "click" in names and "finalize" in names

        obs = rpc(proc, "tools/call",
                  {"name": "observe", "arguments": {}}, 3)["result"]
        assert not obs.get("isError"), obs
        types = [c["type"] for c in obs["content"]]
        assert types == ["image", "text"], types
        meta = json.loads(obs["content"][1]["text"])
        print("observe OK: image+meta, frame", meta["frame_id"],
              "budget", meta["budget"]["actions_remaining"])

        click = rpc(proc, "tools/call",
                    {"name": "click", "arguments": {"x": 200, "y": 130}}, 4)["result"]
        assert not click.get("isError"), click
        print("click OK:", click["content"][0]["text"])

        fin = rpc(proc, "tools/call", {"name": "finalize", "arguments": {}}, 6)["result"]
        assert not fin.get("isError"), fin
        print("finalize OK:", fin["content"][0]["text"][:120])
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except Exception:
            proc.kill()
    print("MCP SMOKE PASS")


if __name__ == "__main__":
    main()
