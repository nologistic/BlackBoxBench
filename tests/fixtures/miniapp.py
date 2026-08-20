"""Minimal deterministic reference app for controller tests.

A single server-rendered page: a counter button, a text echo form, and a
persistent (file-backed) counter so reset behavior can be verified.
Requires the X-BBB-Gateway header like a real reference app.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse

from benchmark import config

PAGE = """<!doctype html><html><head><title>miniapp</title>
<style>
body {{ font-family: sans-serif; margin: 0; background: #f5f5f5; }}
#counter-btn {{ position: absolute; left: 100px; top: 100px; width: 200px; height: 60px; font-size: 20px; }}
#count {{ position: absolute; left: 100px; top: 200px; font-size: 28px; color: navy; }}
#echo-form {{ position: absolute; left: 100px; top: 300px; }}
#echo-in {{ width: 240px; height: 36px; font-size: 16px; }}
#echo-out {{ position: absolute; left: 100px; top: 380px; font-size: 20px; color: darkgreen; }}
#newtab-link {{ position: absolute; left: 100px; top: 470px; font-size: 22px; }}
</style></head><body>
<form method="post" action="/inc"><button id="counter-btn" type="submit">Increment</button></form>
<div id="count">Count: {count}</div>
<form id="echo-form" method="post" action="/echo">
<input id="echo-in" name="text" type="text" placeholder="type here"/>
<button type="submit">Echo</button></form>
<div id="echo-out">{echo}</div>
<a id="newtab-link" href="/detail" target="_blank">Open detail in new tab</a>
</body></html>"""

DETAIL_PAGE = """<!doctype html><html><head><title>miniapp detail</title>
<style>
body { font-family: sans-serif; margin: 0; background: #f5f5f5; }
#detail-marker { position: absolute; left: 100px; top: 100px; width: 300px;
  height: 80px; background: #ff8c00; color: white; font-size: 26px;
  text-align: center; line-height: 80px; }
#back-link { position: absolute; left: 100px; top: 250px; font-size: 22px; }
</style></head><body>
<div id="detail-marker">DETAIL PAGE</div>
<a id="back-link" href="/">Back home (same tab)</a>
</body></html>"""


def create_app(data_dir: Path) -> FastAPI:
    app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None)
    state_file = Path(data_dir) / "state.json"

    def load() -> dict:
        if state_file.exists():
            return json.loads(state_file.read_text())
        return {"count": 0}

    def save(s: dict) -> None:
        state_file.write_text(json.dumps(s))

    @app.middleware("http")
    async def gateway_guard(request: Request, call_next):
        if request.headers.get(config.GATEWAY_HEADER.lower(),
                               request.headers.get(config.GATEWAY_HEADER)) is None:
            return HTMLResponse("", status_code=404)
        return await call_next(request)

    @app.get("/", response_class=HTMLResponse)
    def home():
        return PAGE.format(count=load()["count"], echo="")

    @app.post("/inc", response_class=HTMLResponse)
    def inc():
        s = load()
        s["count"] += 1
        save(s)
        return PAGE.format(count=s["count"], echo="")

    @app.post("/echo", response_class=HTMLResponse)
    def echo(text: str = Form("")):
        return PAGE.format(count=load()["count"], echo=f"Echo: {text}")

    @app.get("/detail", response_class=HTMLResponse)
    def detail():
        return DETAIL_PAGE

    return app


def main() -> None:
    import uvicorn
    p = argparse.ArgumentParser()
    p.add_argument("--port", type=int, required=True)
    p.add_argument("--data-dir", required=True)
    args = p.parse_args()
    uvicorn.run(create_app(Path(args.data_dir)), host="127.0.0.1",
                port=args.port, log_level="error")


if __name__ == "__main__":
    main()
