"""Live status of the project's processes, sessions and shared resources."""
from __future__ import annotations

import json
import subprocess

import httpx

PS = ("Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -ne $null } "
      "| Select-Object ProcessId, CreationDate, CommandLine | ConvertTo-Json -Depth 3")

WATCH = (
    ("controller", "benchmark.server"),
    ("mcp-android-baseline", "agents.android_baseline.mcp_server"),
    ("mcp-android-our-method", "agents.android_our_method.mcp_server"),
    ("mcp-web-baseline", "agents.cli_explorer.mcp_server"),
    ("mcp-web-our-method", "agents.our_method.mcp_server"),
    ("mcp-app-review", "agents.app_review.mcp_server"),
    ("mcp-web-review", "agents.web_review.mcp_server"),
    ("emulator", "emulator.exe"),
    ("qemu", "qemu-system"),
)


def processes() -> None:
    result = subprocess.run(["powershell", "-NoProfile", "-Command", PS],
                            capture_output=True, text=True, timeout=120)
    try:
        entries = json.loads(result.stdout or "[]")
    except json.JSONDecodeError:
        print("  (could not enumerate)")
        return
    if isinstance(entries, dict):
        entries = [entries]
    found: dict[str, list[int]] = {}
    for entry in entries:
        command = entry.get("CommandLine") or ""
        for label, needle in WATCH:
            if needle in command:
                found.setdefault(label, []).append(entry.get("ProcessId"))
                break
    if not found:
        print("  none")
    for label, pids in sorted(found.items()):
        print(f"  {label:24} pids={pids}")


def sessions() -> None:
    client = httpx.Client(trust_env=False, timeout=10)
    try:
        data = client.get("http://127.0.0.1:7800/api/sessions").json()
    except Exception as exc:
        print(f"  controller unreachable: {type(exc).__name__}")
        return
    if not data:
        print("  no sessions in controller memory")
    for item in data:
        marker = ">>" if item["status"] == "running" else "  "
        print(f"  {marker} {item['session_id']}  app={item['app_id']:20} "
              f"status={item['status']:8} step={item['step']}")
        if item["status"] == "running":
            try:
                detail = client.get(
                    f"http://127.0.0.1:7800/api/sessions/{item['session_id']}"
                ).json()
                counts = detail.get("counts", {})
                budget = detail.get("budget", {})
                print(f"     elapsed={detail.get('elapsed_s')}s "
                      f"actions_left={budget.get('actions_remaining')} "
                      f"states={counts.get('states')} "
                      f"features={counts.get('features')} "
                      f"edges={counts.get('edges')}")
                print(f"     last_action={detail.get('last_action')}")
            except Exception as exc:
                print(f"     (status detail failed: {type(exc).__name__})")


def main() -> None:
    print("PROCESSES");  processes()
    print("SESSIONS");   sessions()


if __name__ == "__main__":
    main()
