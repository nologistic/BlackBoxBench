"""Stdio-to-TCP relay for MCP clients running inside the agent runtime container.

The agent CLI registers this script as its MCP server command. The script
connects to the host-side BlackBoxBench MCP server (newline-delimited
JSON-RPC over TCP), performs the token handshake, and then pumps bytes in
both directions. It contains no exploration semantics of any kind.
"""
from __future__ import annotations

import json
import os
import socket
import sys
import threading

HOST = os.environ.get("BBB_MCP_HOST", "host.docker.internal")
PORT = int(os.environ.get("BBB_MCP_PORT", "8400"))
TOKEN = os.environ.get("BBB_MCP_TOKEN", "")


def main() -> int:
    try:
        sock = socket.create_connection((HOST, PORT), timeout=15)
    except OSError as exc:
        print(f"mcp relay: cannot reach {HOST}:{PORT}: {exc}", file=sys.stderr)
        return 1
    reader = sock.makefile("rb")
    writer = sock.makefile("wb")

    if TOKEN:
        writer.write(json.dumps({
            "jsonrpc": "2.0", "id": "auth", "method": "auth",
            "params": {"token": TOKEN},
        }).encode("utf-8") + b"\n")
        writer.flush()
        reply = reader.readline()
        try:
            payload = json.loads(reply.decode("utf-8", errors="replace"))
        except json.JSONDecodeError:
            payload = None
        if not isinstance(payload, dict) or payload.get("error"):
            print("mcp relay: authentication rejected", file=sys.stderr)
            return 1

    def pump_up() -> None:
        try:
            for line in sys.stdin.buffer:
                writer.write(line if line.endswith(b"\n") else line + b"\n")
                writer.flush()
        except OSError:
            pass
        finally:
            try:
                sock.shutdown(socket.SHUT_WR)
            except OSError:
                pass

    threading.Thread(target=pump_up, daemon=True).start()
    try:
        for line in reader:
            sys.stdout.buffer.write(line)
            sys.stdout.buffer.flush()
    except OSError:
        pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
