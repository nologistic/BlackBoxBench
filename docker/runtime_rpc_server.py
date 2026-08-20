#!/usr/bin/env python3
"""Runtime RPC server — the ONLY process a controller may talk to inside the
reference container (docs/architecture.md §4.1).

Capability surface is deliberately pixels-in / HID-out, mirroring
`benchmark/runtime/base.py`:

    POST /screenshot      -> PNG bytes of X display :99 (scrot, fallback
                             ImageMagick `import -window root`)
    POST /input/mouse     {kind: move|down|up|click, x, y, button?, count?}
    POST /input/key       {key, kind: press|down|up}
    POST /input/type      {text}
    POST /input/scroll    {dx, dy}          (X11 wheel buttons 4/5/6/7)
    POST /reset           -> cold browser (fresh tmpfs profile) + reseeded data
    GET  /health          -> liveness of Xvfb / chromium / app / gateway

Hard rules (docs/security_model.md T2):
- There is NO endpoint that executes arbitrary commands, reads files, returns
  the DOM/AXTree, evaluates script, exposes the URL, or touches the network.
  Every subprocess invocation below is a fixed argv list with validated scalar
  arguments; no shell is ever involved.
- Chromium is launched WITHOUT --remote-debugging-port, so no CDP endpoint
  exists inside the container at all.
- The server binds 0.0.0.0:8300 but the container sits on an `internal: true`
  docker network with no published host port; only the controller can route
  to it (compose topology is the access control).

This service also owns the chromium lifecycle (launch/stop/restart) because
/reset must be able to restart the browser with a fresh --user-data-dir.

NOTE: statically reviewed only — not executed on the authoring machine
(no Docker / no Linux X stack available there).
"""
from __future__ import annotations

import argparse
import http.client
import io
import json
import logging
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path
from typing import Literal, Optional

from fastapi import FastAPI, HTTPException, Response
from PIL import Image
from pydantic import BaseModel, Field

log = logging.getLogger("bbb-runtime-rpc")

# ------------------------------------------------------------------ config
DISPLAY = os.environ.get("DISPLAY", ":99")
DISPLAY_NUM = DISPLAY.rsplit(":", 1)[-1].split(".")[0]  # ":99" -> "99"
VIEWPORT_WIDTH = int(os.environ.get("BBB_VIEWPORT_WIDTH", "1440"))
VIEWPORT_HEIGHT = int(os.environ.get("BBB_VIEWPORT_HEIGHT", "900"))

APP_ID = os.environ.get("BBB_APP_ID", "ecommerce_demo")
APP_HOST = os.environ.get("BBB_APP_INTERNAL_HOST", "reference-app.internal")
APP_PORT = int(os.environ.get("BBB_APP_PORT", "8100"))
GATEWAY_PORT = int(os.environ.get("BBB_GATEWAY_PORT", "8200"))
APP_ENTRY_URL = os.environ.get(
    "BBB_APP_ENTRY", f"http://{APP_HOST}:{GATEWAY_PORT}")
GATEWAY_SECRET = os.environ.get("BBB_GATEWAY_SECRET", "")
GATEWAY_HEADER = os.environ.get("BBB_GATEWAY_HEADER", "X-BBB-Gateway")

# Seed resolution mirrors docker/reference_entrypoint.sh: pristine seed ships
# inside the app package; the app runs against a disposable live copy.
DATA_SEED = Path(os.environ.get(
    "BBB_DATA_SEED", f"/app/sample_apps/{APP_ID}/seed"))
DATA_LIVE = Path(os.environ.get("BBB_DATA_LIVE", "/data/live"))
CHROMIUM_BIN = os.environ.get("BBB_CHROMIUM_BIN", "chromium")

MAX_TYPE_CHARS = 4096
MAX_SCROLL_CLICKS = 40
SCROLL_PIXELS_PER_CLICK = 120  # conventional wheel delta
_KEYSYM_RE = re.compile(r"^[A-Za-z0-9_]{1,32}$")  # single X keysym, no chords

_XDG_RUNTIME_FALLBACK = "/tmp"


def _env() -> dict:
    """Environment for X client subprocesses (xdotool/scrot/import/chromium)."""
    env = dict(os.environ)
    env["DISPLAY"] = DISPLAY
    return env


def _run(argv: list[str], timeout: float) -> None:
    """Run a fixed argv list; any failure becomes a 503 to the controller."""
    log.debug("exec: %s", " ".join(argv))
    try:
        proc = subprocess.run(
            argv, env=_env(), timeout=timeout,
            stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=False,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        log.error("tool failed to run: %s: %s", argv[0], exc)
        raise HTTPException(status_code=503, detail="runtime_unavailable") from exc
    if proc.returncode != 0:
        log.error("tool exited %d: %s: %s", proc.returncode, argv[0],
                  proc.stderr.decode(errors="replace")[:500])
        raise HTTPException(status_code=503, detail="runtime_unavailable")


# ------------------------------------------------------------------ chromium
class ChromiumManager:
    """Owns the kiosk chromium child process and its throwaway profile dir."""

    def __init__(self) -> None:
        self._proc: Optional[subprocess.Popen] = None
        self._profile: Optional[Path] = None
        self._lock = threading.Lock()

    # ---- profile handling: fresh tmpfs-backed dir per launch (T8)
    @staticmethod
    def _profile_root() -> str:
        override = os.environ.get("BBB_PROFILE_ROOT")
        if override:
            return override
        for cand in ("/dev/shm", _XDG_RUNTIME_FALLBACK):
            if os.path.isdir(cand) and os.access(cand, os.W_OK):
                return cand
        return _XDG_RUNTIME_FALLBACK

    def _new_profile(self) -> Path:
        return Path(tempfile.mkdtemp(prefix="bbb-chromium-",
                                     dir=self._profile_root()))

    def _drop_profile(self) -> None:
        if self._profile is not None:
            shutil.rmtree(self._profile, ignore_errors=True)
            self._profile = None

    # ---- lifecycle
    def launch(self) -> None:
        with self._lock:
            if self.alive():
                return
            self._drop_profile()
            self._profile = self._new_profile()
            argv = [
                CHROMIUM_BIN,
                # container hardening: no setuid sandbox is possible under
                # no-new-privileges; the app content is trusted and the
                # network is locked down by --host-resolver-rules + refnet.
                "--no-sandbox", "--disable-setuid-sandbox",
                "--disable-dev-shm-usage", "--disable-gpu",
                # kiosk/app mode: no address bar, no tabs, no menus (T4).
                "--kiosk", f"--app={APP_ENTRY_URL}",
                f"--window-size={VIEWPORT_WIDTH},{VIEWPORT_HEIGHT}",
                "--force-device-scale-factor=1",
                # resolver lock: only the internal app name resolves (T3).
                f"--host-resolver-rules=MAP {APP_HOST} 127.0.0.1, MAP * ~NOTFOUND",
                # cold profile every launch: no cookies/cache/storage (T8).
                f"--user-data-dir={self._profile}",
                # no escape hatches / no noise.
                "--disable-extensions", "--disable-component-update",
                "--disable-sync", "--disable-default-apps",
                "--disable-background-networking",
                "--no-first-run", "--no-default-browser-check",
                "--disable-session-crashed-bubble",
                "--hide-crash-restore-bubble", "--noerrdialogs",
                "--disable-features=Translate,OptimizationHints,MediaRouter",
                "--mute-audio",
                # NB: intentionally NO --remote-debugging-port (T2).
            ]
            log.info("launching chromium (profile %s)", self._profile)
            try:
                self._proc = subprocess.Popen(
                    argv, env=_env(),
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                    start_new_session=True,
                )
            except OSError as exc:
                self._proc = None
                raise HTTPException(status_code=503,
                                    detail="runtime_unavailable") from exc

    def stop(self) -> None:
        with self._lock:
            proc = self._proc
            self._proc = None
            if proc is not None and proc.poll() is None:
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    proc.kill()
                    try:
                        proc.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        log.error("chromium did not die after SIGKILL")
            self._drop_profile()

    def restart(self) -> None:
        self.stop()
        self.launch()

    def alive(self) -> bool:
        proc = self._proc
        return proc is not None and proc.poll() is None


CHROMIUM = ChromiumManager()

# ------------------------------------------------------------------ seed data
def reseed() -> None:
    """Recreate the app's live data dir from the pristine seed (T8).

    The app process is NOT restarted; the reference-app contract
    (docs/adding_reference_app.md) requires that it treats the data dir as
    the source of truth and never caches data across requests.
    """
    if not DATA_SEED.is_dir():
        log.error("seed dir missing: %s", DATA_SEED)
        raise HTTPException(status_code=500, detail="seed_missing")
    shutil.rmtree(DATA_LIVE, ignore_errors=True)
    shutil.copytree(DATA_SEED, DATA_LIVE, symlinks=True)
    log.info("reseeded %s -> %s", DATA_SEED, DATA_LIVE)


def _wait_app_ready(timeout_s: float = 15.0) -> None:
    """Block until the app answers HTTP again (used after /reset)."""
    deadline = time.monotonic() + timeout_s
    while time.monotonic() < deadline:
        try:
            conn = http.client.HTTPConnection("127.0.0.1", APP_PORT, timeout=2)
            conn.request("GET", "/", headers={GATEWAY_HEADER: GATEWAY_SECRET})
            resp = conn.getresponse()
            resp.read()
            conn.close()
            return  # any HTTP response means the app process is alive
        except OSError:
            time.sleep(0.2)
    raise HTTPException(status_code=503, detail="runtime_unavailable")


# ------------------------------------------------------------------ requests
_OP_LOCK = threading.Lock()  # serialize every operation against the display


class MouseRequest(BaseModel):
    kind: Literal["move", "down", "up", "click"]
    x: int = Field(ge=0, le=VIEWPORT_WIDTH - 1)
    y: int = Field(ge=0, le=VIEWPORT_HEIGHT - 1)
    button: Literal["left", "middle", "right"] = "left"
    count: int = Field(default=1, ge=1, le=5)


class KeyRequest(BaseModel):
    key: str = Field(min_length=1, max_length=32)
    kind: Literal["press", "down", "up"] = "press"


class TypeRequest(BaseModel):
    text: str = Field(min_length=0, max_length=MAX_TYPE_CHARS)


class ScrollRequest(BaseModel):
    dx: int = Field(default=0, ge=-4000, le=4000)
    dy: int = Field(default=0, ge=-4000, le=4000)


_BUTTON = {"left": "1", "middle": "2", "right": "3"}


def _wheel_clicks(pixels: int) -> int:
    if pixels == 0:
        return 0
    return max(1, min(MAX_SCROLL_CLICKS, abs(pixels) // SCROLL_PIXELS_PER_CLICK))


# ------------------------------------------------------------------ app
app = FastAPI(title="bbb-runtime-rpc", docs_url=None, redoc_url=None,
              openapi_url=None)


@app.post("/screenshot")
def screenshot() -> Response:
    """Pixels of the root window on DISPLAY, re-encoded as PNG. Nothing else."""
    with _OP_LOCK:
        fd, path = tempfile.mkstemp(prefix="bbb-shot-", suffix=".png")
        os.close(fd)
        os.unlink(path)  # the capture tool creates it itself
        try:
            try:
                _run(["scrot", "-o", "-z", path], timeout=15)
            except HTTPException:
                log.warning("scrot failed; falling back to imagemagick import")
                _run(["import", "-window", "root", path], timeout=15)
            try:
                with Image.open(path) as im:
                    im.load()
                    buf = io.BytesIO()
                    im.convert("RGB").save(buf, format="PNG")
            except Exception as exc:
                log.error("captured image unreadable: %s", exc)
                raise HTTPException(status_code=503,
                                    detail="runtime_unavailable") from exc
        finally:
            try:
                os.unlink(path)
            except OSError:
                pass
    return Response(content=buf.getvalue(), media_type="image/png")


@app.post("/input/mouse")
def input_mouse(req: MouseRequest) -> dict:
    btn = _BUTTON[req.button]
    with _OP_LOCK:
        _run(["xdotool", "mousemove", str(req.x), str(req.y)], timeout=5)
        if req.kind == "down":
            _run(["xdotool", "mousedown", btn], timeout=5)
        elif req.kind == "up":
            _run(["xdotool", "mouseup", btn], timeout=5)
        elif req.kind == "click":
            argv = ["xdotool", "click"]
            if req.count > 1:
                argv += ["--repeat", str(req.count), "--delay", "80"]
            argv.append(btn)
            _run(argv, timeout=10)
    return {"ok": True}


@app.post("/input/key")
def input_key(req: KeyRequest) -> dict:
    if not _KEYSYM_RE.match(req.key):
        # single X keysym only; chords/flags are not expressible here
        raise HTTPException(status_code=400, detail="invalid_key")
    verb = {"press": "key", "down": "keydown", "up": "keyup"}[req.kind]
    with _OP_LOCK:
        _run(["xdotool", verb, req.key], timeout=5)
    return {"ok": True}


@app.post("/input/type")
def input_type(req: TypeRequest) -> dict:
    if not req.text:
        return {"ok": True}
    with _OP_LOCK:
        _run(["xdotool", "type", "--delay", "12", "--clearmodifiers",
              "--", req.text], timeout=60)
    return {"ok": True}


@app.post("/input/scroll")
def input_scroll(req: ScrollRequest) -> dict:
    with _OP_LOCK:
        # X11 wheel buttons: 4=up 5=down 6=left 7=right; dy>0 scrolls down.
        for pixels, neg_btn, pos_btn in (
            (req.dy, "4", "5"),
            (req.dx, "6", "7"),
        ):
            clicks = _wheel_clicks(pixels)
            if clicks:
                btn = pos_btn if pixels > 0 else neg_btn
                _run(["xdotool", "click", "--repeat", str(clicks),
                      "--delay", "25", btn], timeout=30)
    return {"ok": True}


@app.post("/reset")
def reset() -> dict:
    """Cold-start the environment: fresh browser profile + reseeded data (T8)."""
    with _OP_LOCK:
        log.info("reset requested")
        CHROMIUM.stop()
        reseed()
        CHROMIUM.launch()
        _wait_app_ready()
        time.sleep(0.5)  # let the kiosk window map before the next screenshot
    return {"ok": True}


@app.get("/health")
def health() -> Response:
    display_ok = os.path.exists(f"/tmp/.X11-unix/X{DISPLAY_NUM}")
    chromium_ok = CHROMIUM.alive()
    app_ok = False
    try:
        conn = http.client.HTTPConnection("127.0.0.1", APP_PORT, timeout=2)
        conn.request("GET", "/", headers={GATEWAY_HEADER: GATEWAY_SECRET})
        resp = conn.getresponse()
        resp.read()
        conn.close()
        app_ok = True
    except OSError:
        pass
    status = {
        "display": display_ok,
        "chromium": chromium_ok,
        "app": app_ok,
    }
    ok = all(status.values())
    body = {"status": "ok" if ok else "degraded", **status}
    return Response(content=json.dumps(body), status_code=200 if ok else 503,
                    media_type="application/json")


# ------------------------------------------------------------------ main
def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int,
                        default=int(os.environ.get("BBB_RPC_PORT", "8300")))
    args = parser.parse_args(argv)

    if not DATA_LIVE.exists():
        # first boot: entrypoint normally reseeds before starting us, but
        # tolerate being started standalone inside the container
        reseed()
    CHROMIUM.launch()

    def _shutdown(*_sig) -> None:
        CHROMIUM.stop()
        sys.exit(0)

    signal.signal(signal.SIGTERM, _shutdown)
    signal.signal(signal.SIGINT, _shutdown)

    import uvicorn

    uvicorn.run(app, host=args.host, port=args.port, log_level="info",
                access_log=False)
    return 0


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="[runtime-rpc] %(message)s")
    sys.exit(main())
