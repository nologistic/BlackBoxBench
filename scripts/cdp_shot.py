"""One-off CDP screenshot of a URL (operator tooling, not part of benchmark).

Usage: vendor/python/python.exe scripts/cdp_shot.py <url> <out.png> [wait_s] [w] [h]
"""
from __future__ import annotations

import base64
import json
import subprocess
import sys
import tempfile
import time
import urllib.request
import socket
from pathlib import Path

import websocket

_CHROME = Path(__file__).resolve().parent.parent / "vendor" / "chromium" / "chrome-win64" / "chrome.exe"
CHROME = str(_CHROME)

_NO_PROXY = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def main() -> None:
    url, out = sys.argv[1], sys.argv[2]
    wait_s = float(sys.argv[3]) if len(sys.argv) > 3 else 2.0
    w = int(sys.argv[4]) if len(sys.argv) > 4 else 1600
    h = int(sys.argv[5]) if len(sys.argv) > 5 else 1000
    port = free_port()
    proc = subprocess.Popen(
        [CHROME, "--headless=new", "--no-sandbox", "--disable-crashpad",
         "--disable-gpu", f"--remote-debugging-port={port}",
         "--remote-allow-origins=*", f"--user-data-dir={tempfile.mkdtemp()}",
         f"--window-size={w},{h}", "--no-first-run", "--no-proxy-server",
         "about:blank"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        ws_url = None
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            try:
                with _NO_PROXY.open(
                        f"http://127.0.0.1:{port}/json/list", timeout=2) as r:
                    for t in json.loads(r.read()):
                        if t.get("type") == "page":
                            ws_url = t["webSocketDebuggerUrl"]
                            break
                if ws_url:
                    break
            except Exception:
                time.sleep(0.3)
        ws = websocket.create_connection(ws_url, timeout=15,
                                         origin="http://127.0.0.1")
        mid = 0

        def call(method, params=None):
            nonlocal mid
            mid += 1
            ws.send(json.dumps({"id": mid, "method": method,
                                "params": params or {}}))
            while True:
                msg = json.loads(ws.recv())
                if msg.get("id") == mid:
                    if "error" in msg:
                        raise RuntimeError(msg["error"])
                    return msg.get("result", {})

        call("Page.enable")
        call("Emulation.setDeviceMetricsOverride",
             {"width": w, "height": h, "deviceScaleFactor": 1, "mobile": False})
        call("Page.navigate", {"url": url})
        time.sleep(wait_s)
        png = base64.b64decode(
            call("Page.captureScreenshot", {"format": "png"})["data"])
        Path(out).write_bytes(png)
        print(out)
        ws.close()
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except Exception:
            proc.kill()


if __name__ == "__main__":
    main()
