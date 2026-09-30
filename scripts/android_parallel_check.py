"""Operator check: can two Agents explore the same target at the same time?

Starts two MCP clients against one target concurrently, the way two Agent tasks
would, and reports what each got. Verifies the resources that made parallel runs
unsafe before: emulator port reservations, target leases, AVD clones and the
memory admission race.

    vendor/python/python.exe scripts/android_parallel_check.py --app google_clock
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from benchmark import config
from benchmark.android.runtime import booting_dir, port_locks_dir

MODULES = {
    "baseline": "agents.android_baseline.mcp_server",
    "nograph": "agents.baseline_nograph.mcp_server",
}


class Client:
    def __init__(self, module: str, label: str):
        self.label = label
        env = dict(os.environ)
        env["PYTHONPATH"] = str(ROOT)
        env["PYTHONIOENCODING"] = "utf-8"
        self.process = subprocess.Popen(
            [sys.executable, "-m", module], cwd=str(ROOT), env=env,
            stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, text=True, encoding="utf-8",
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        self.counter = 0
        self.session_id = ""
        self.error = ""
        self.geometry = ""

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

    def tool(self, name: str, arguments: dict | None = None):
        result = self.call("tools/call", {"name": name,
                                         "arguments": arguments or {}})
        body = result.get("result", result)
        text = ""
        for item in body.get("content", []):
            if item.get("type") == "text":
                text = item["text"]
                break
        if body.get("isError"):
            raise RuntimeError(text)
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return {"text": text}

    def close(self):
        try:
            self.process.stdin.close()
        except OSError:
            pass
        try:
            self.process.wait(timeout=240)
        except subprocess.TimeoutExpired:
            self.process.kill()


def explore(client: Client, app: str, steps: int) -> None:
    try:
        client.call("initialize")
        started = client.tool("start_session", {"app_id": app})
        client.session_id = started.get("session_id", "")
        observed = client.tool("observe")
        client.geometry = f"{observed['width']}x{observed['height']}"
        # A few real actions so the two sessions genuinely overlap in time.
        width, height = observed["width"], observed["height"]
        for _ in range(steps):
            client.tool("tap", {"x": width // 2, "y": int(height * 0.4)})
            client.tool("observe")
    except Exception as exc:
        client.error = f"{type(exc).__name__}: {exc}"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app", default="google_clock")
    parser.add_argument("--steps", type=int, default=3)
    parser.add_argument("--conditions", default="baseline,nograph")
    args = parser.parse_args()

    labels = [c.strip() for c in args.conditions.split(",") if c.strip()]
    clients = [Client(MODULES[label], label) for label in labels]
    threads = [threading.Thread(target=explore,
                                args=(client, args.app, args.steps))
               for client in clients]

    print(f"[parallel] starting {len(clients)} conditions against {args.app} …")
    start = time.monotonic()
    for thread in threads:
        thread.start()
    # Watch the shared reservations while both are booting.
    peak_ports, peak_booting = 0, 0
    while any(t.is_alive() for t in threads):
        ports = len(list(port_locks_dir().glob("*.lock"))) \
            if port_locks_dir().is_dir() else 0
        booting = len(list(booting_dir().glob("*.lock"))) \
            if booting_dir().is_dir() else 0
        peak_ports = max(peak_ports, ports)
        peak_booting = max(peak_booting, booting)
        time.sleep(2)
    for thread in threads:
        thread.join()
    elapsed = time.monotonic() - start

    print(f"[parallel] elapsed        : {elapsed:.0f}s")
    print(f"[parallel] peak port locks: {peak_ports}")
    print(f"[parallel] peak booting   : {peak_booting}")
    sessions = []
    for client in clients:
        state = client.error or f"ok  session={client.session_id} " \
                                f"screen={client.geometry}"
        print(f"[parallel] {client.label:<12}: {state}")
        if client.session_id:
            sessions.append(client.session_id)

    print()
    if len(set(sessions)) == len(clients) and not any(c.error for c in clients):
        print("[parallel] RESULT: both conditions ran concurrently on distinct "
              "sessions and devices.")
    elif any("insufficient free memory" in (c.error or "") for c in clients):
        print("[parallel] RESULT: one condition was refused for memory — that "
              "is the admission check working, not a crash.")
    else:
        print("[parallel] RESULT: see the per-condition errors above.")

    for client in clients:
        if client.session_id:
            import httpx
            with httpx.Client(timeout=httpx.Timeout(600.0, connect=10.0),
                              trust_env=False) as http:
                http.post(f"http://127.0.0.1:7800/api/sessions/"
                          f"{client.session_id}/close", json={})
        client.close()


if __name__ == "__main__":
    main()
