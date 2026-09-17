"""Cold-start smoke for the two evaluation MCP servers over stdio.

Sends initialize -> tools/list -> tools/call(list_checklists) to each server
and checks that the judge-facing surface answers without a real device.

Usage: vendor/python/python.exe scripts/evaluation_mcp_smoke.py
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PY = ROOT / "vendor" / "python" / "python.exe"  # Windows dev default
if not PY.exists():  # Linux/opencode: use the interpreter running this script
    PY = Path(sys.executable)


def smoke(module: str) -> dict:
    env = dict(os.environ)
    env["PYTHONPATH"] = str(ROOT)
    env["PYTHONIOENCODING"] = "utf-8"
    proc = subprocess.Popen(
        [str(PY), "-m", module],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL, env=env)

    def call(payload: dict) -> dict:
        proc.stdin.write((json.dumps(payload) + "\n").encode("utf-8"))
        proc.stdin.flush()
        raw = proc.stdout.readline()
        return json.loads(raw.decode("utf-8"))

    try:
        init = call({"jsonrpc": "2.0", "id": 1, "method": "initialize",
                     "params": {}})
        tools = call({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
        listing = call({"jsonrpc": "2.0", "id": 3, "method": "tools/call",
                        "params": {"name": "list_checklists",
                                   "arguments": {}}})
    finally:
        proc.terminate()
        proc.wait(timeout=10)

    names = [t["name"] for t in tools["result"]["tools"]]
    body = json.loads(
        listing["result"]["content"][0]["text"])
    return {"server": init["result"]["serverInfo"]["name"],
            "tools": names,
            "checklists": [(c["file"], c["checklist_id"],
                            c["requirements"]) for c in body["checklists"]],
            "handoffs": sorted(
                h["handoff_id"] if isinstance(h, dict) else str(h)
                for h in body.get("installable_handoffs",
                                  body.get("available_handoffs", [])))}


for module in ("agents.app_review.mcp_server",
               "agents.web_review.mcp_server"):
    info = smoke(module)
    print(f"=== {info['server']} ({module})")
    print(f"  tools({len(info['tools'])}): {', '.join(sorted(info['tools']))}")
    for file, cid, count in info["checklists"]:
        print(f"  checklist: {file} -> {cid} ({count} requirements)")
    print(f"  handoffs: {info['handoffs']}")
    print()
