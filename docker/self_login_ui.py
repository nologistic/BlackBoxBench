#!/usr/bin/env python3
"""Loopback-published operator UI for preparing a Docker browser profile.

This process is started only by docker-compose.self-login.yml.  It exposes a
pixel frame and coordinate/keyboard input to the human operator; it has no DOM,
URL, selector, network-response, or browser-debugging channel.  It is never
present in a self-built exploration session and is not copied into the Agent
image.
"""
from __future__ import annotations

import os
import subprocess
import tempfile
import threading
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse, Response

DISPLAY = os.environ.get("DISPLAY", ":99")
WIDTH = int(os.environ.get("BBB_VIEWPORT_WIDTH", "1440"))
HEIGHT = int(os.environ.get("BBB_VIEWPORT_HEIGHT", "900"))
_LOCK = threading.Lock()

app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None)

_PAGE = """<!doctype html>
<meta charset="utf-8">
<title>BlackBoxBench Docker 登录</title>
<style>
  html,body{margin:0;background:#181818;color:#eee;font:14px system-ui,sans-serif}
  #bar{height:38px;display:flex;gap:8px;align-items:center;padding:0 12px;background:#242424}
  button{background:#3a3a3a;color:#fff;border:1px solid #666;border-radius:5px;padding:4px 10px}
  #hint{margin-left:6px;color:#ccc}
  #screen{display:block;max-width:100vw;max-height:calc(100vh - 38px);margin:auto;
          user-select:none;touch-action:none;cursor:default;background:#000}
</style>
<div id="bar"><button onclick="nav('back')">← 后退</button>
<button onclick="nav('forward')">前进 →</button><button onclick="nav('reload')">刷新</button>
<span id="hint">Docker 可信浏览器（若误入 Chrome Help，请点“后退”；登录后回终端按 Enter）</span></div>
<img id="screen" draggable="false" tabindex="0">
<script>
const screen=document.getElementById('screen');
let down=false;
const send=(body)=>fetch('/input',{method:'POST',headers:{'content-type':'application/json'},
  body:JSON.stringify(body)}).catch(()=>{});
const nav=(command)=>send({kind:'navigation',command});
function xy(e){const r=screen.getBoundingClientRect();return {
  x:Math.max(0,Math.min(1439,Math.floor((e.clientX-r.left)*1440/r.width))),
  y:Math.max(0,Math.min(899,Math.floor((e.clientY-r.top)*900/r.height)))};}
function refresh(){const next=new Image();next.onload=()=>{screen.src=next.src};
  next.src='/frame?t='+Date.now();}
setInterval(refresh,300); refresh();
screen.addEventListener('pointerdown',e=>{e.preventDefault();screen.focus();down=true;
  screen.setPointerCapture(e.pointerId);send({kind:'mouse_down',...xy(e),button:e.button});});
screen.addEventListener('pointermove',e=>{if(down)send({kind:'mouse_move',...xy(e)});});
screen.addEventListener('pointerup',e=>{e.preventDefault();down=false;
  send({kind:'mouse_up',...xy(e),button:e.button});});
screen.addEventListener('contextmenu',e=>e.preventDefault());
screen.addEventListener('wheel',e=>{e.preventDefault();send({kind:'scroll',dy:e.deltaY});},
  {passive:false});
const special={Enter:'Return',Backspace:'BackSpace',Tab:'Tab',Escape:'Escape',
  ArrowUp:'Up',ArrowDown:'Down',ArrowLeft:'Left',ArrowRight:'Right',Delete:'Delete',
  Home:'Home',End:'End',PageUp:'Page_Up',PageDown:'Page_Down'};
window.addEventListener('keydown',e=>{
  if(e.ctrlKey||e.altKey||e.metaKey)return;
  if(e.key.length===1){e.preventDefault();send({kind:'text',text:e.key});}
  else if(special[e.key]){e.preventDefault();send({kind:'key',key:special[e.key]});}
});
window.addEventListener('compositionend',e=>{if(e.data)send({kind:'text',text:e.data});});
</script>
"""


def _run(argv: list[str], *, data: bytes | None = None,
         timeout: float = 10) -> None:
    env = dict(os.environ)
    env["DISPLAY"] = DISPLAY
    result = subprocess.run(
        argv, input=data, env=env, timeout=timeout,
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
    if result.returncode:
        raise HTTPException(503, "display input unavailable")


def _coordinate(value: object, maximum: int) -> int:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise HTTPException(400, "invalid coordinate")
    return max(0, min(maximum - 1, int(value)))


def _button(value: object) -> str:
    try:
        return {0: "1", 1: "2", 2: "3"}[int(value)]
    except (KeyError, TypeError, ValueError):
        raise HTTPException(400, "invalid button")


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return _PAGE


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.get("/frame")
def frame() -> Response:
    with _LOCK:
        fd, name = tempfile.mkstemp(prefix="operator-frame-", suffix=".png",
                                    dir="/data/tmp")
        os.close(fd)
        path = Path(name)
        path.unlink(missing_ok=True)
        try:
            _run(["scrot", "-o", "-z", str(path)], timeout=5)
            payload = path.read_bytes()
        finally:
            path.unlink(missing_ok=True)
    return Response(payload, media_type="image/png",
                    headers={"Cache-Control": "no-store"})


@app.post("/input")
def input_action(payload: dict) -> dict:
    kind = payload.get("kind")
    with _LOCK:
        if kind in {"mouse_move", "mouse_down", "mouse_up"}:
            x = _coordinate(payload.get("x"), WIDTH)
            y = _coordinate(payload.get("y"), HEIGHT)
            _run(["xdotool", "mousemove", str(x), str(y)], timeout=5)
            if kind != "mouse_move":
                command = "mousedown" if kind == "mouse_down" else "mouseup"
                _run(["xdotool", command, _button(payload.get("button", 0))],
                     timeout=5)
        elif kind == "scroll":
            raw = payload.get("dy", 0)
            if isinstance(raw, bool) or not isinstance(raw, (int, float)):
                raise HTTPException(400, "invalid scroll")
            clicks = max(1, min(12, int(abs(raw) / 80) or 1))
            _run(["xdotool", "click", "--repeat", str(clicks),
                  "5" if raw > 0 else "4"], timeout=8)
        elif kind == "key":
            allowed = {"Return", "BackSpace", "Tab", "Escape", "Up", "Down",
                       "Left", "Right", "Delete", "Home", "End", "Page_Up",
                       "Page_Down"}
            key = payload.get("key")
            if key not in allowed:
                raise HTTPException(400, "invalid key")
            _run(["xdotool", "key", "--clearmodifiers", key], timeout=5)
        elif kind == "navigation":
            command = payload.get("command")
            keys = {"back": "alt+Left", "forward": "alt+Right",
                    "reload": "ctrl+r"}
            if command not in keys:
                raise HTTPException(400, "invalid navigation")
            _run(["xdotool", "key", "--clearmodifiers", keys[command]],
                 timeout=5)
        elif kind == "text":
            text = payload.get("text")
            if not isinstance(text, str) or not text or len(text) > 256 or \
                    any(ord(ch) < 0x20 for ch in text):
                raise HTTPException(400, "invalid text")
            _run(["xdotool", "type", "--clearmodifiers", "--delay", "1",
                  "--file", "/dev/stdin"], data=text.encode("utf-8"), timeout=8)
        else:
            raise HTTPException(400, "invalid input kind")
    return {"accepted": True}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0",
                port=int(os.environ.get("BBB_OPERATOR_UI_PORT", "8400")),
                access_log=False)
