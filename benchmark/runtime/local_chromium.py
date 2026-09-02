"""Local dev-mode runtime: headless Chromium driven over CDP.

SECURITY CRITICAL — read before modifying:
The CDP connection below is a *capability whitelist*. The only CDP domains
used are Page (navigate + navigation-history back/forward + screenshot),
Input (mouse/key/wheel), Emulation (fixed viewport) and Browser (close).
There is deliberately NO access to Runtime.evaluate, DOM.*, Accessibility.*,
Network.*, Storage.*, or any other semantic channel. Do not add such methods:
benchmark validity depends on the agent receiving pixels and only pixels
(docs/security_model.md, threat T2).

The browser itself is network-locked: --host-resolver-rules only resolves
`reference-app.internal` to loopback; every other name fails to resolve, so
even in-page navigation cannot leave the benchmark environment.

Multi-tab: sites that open links in new tabs (target=_blank) are followed
automatically — screenshots and input always attach to the newest content
tab (_follow_latest), so exploration continues on the page the click opened.
"""
from __future__ import annotations

import base64
import json
import socket
import subprocess
import threading
import time
import urllib.request
from pathlib import Path

import websocket  # websocket-client

from .. import config
from ..scratch import new_scratch_dir, reclaim_scratch, remove_scratch_dir
from .base import Runtime, RuntimeInfo


# ------------------------------------------------------------------ key tables

_NAMED_KEYS: dict[str, tuple[str, str, int]] = {
    # name -> (key, code, windowsVirtualKeyCode)
    "Enter": ("Enter", "Enter", 13),
    "Tab": ("Tab", "Tab", 9),
    "Escape": ("Escape", "Escape", 27),
    "Backspace": ("Backspace", "Backspace", 8),
    "Delete": ("Delete", "Delete", 46),
    "ArrowLeft": ("ArrowLeft", "ArrowLeft", 37),
    "ArrowUp": ("ArrowUp", "ArrowUp", 38),
    "ArrowRight": ("ArrowRight", "ArrowRight", 39),
    "ArrowDown": ("ArrowDown", "ArrowDown", 40),
    "Home": ("Home", "Home", 36),
    "End": ("End", "End", 35),
    "PageUp": ("PageUp", "PageUp", 33),
    "PageDown": ("PageDown", "PageDown", 34),
    " ": (" ", "Space", 32),
    "Space": (" ", "Space", 32),
    **{f"F{i}": (f"F{i}", f"F{i}", 111 + i) for i in range(1, 13)},
}

_PUNCT = {
    "-": ("Minus", 189), "=": ("Equal", 187), "[": ("BracketLeft", 219),
    "]": ("BracketRight", 221), "\\": ("Backslash", 220), ";": ("Semicolon", 186),
    "'": ("Quote", 222), ",": ("Comma", 188), ".": ("Period", 190),
    "/": ("Slash", 191), "`": ("Backquote", 192),
}


def _char_key_params(ch: str) -> dict | None:
    """CDP params for a printable ASCII char as a physical key press."""
    if "a" <= ch <= "z":
        return {"key": ch, "code": f"Key{ch.upper()}",
                "windowsVirtualKeyCode": ord(ch.upper()), "text": ch}
    if "A" <= ch <= "Z":
        return {"key": ch, "code": f"Key{ch}",
                "windowsVirtualKeyCode": ord(ch), "text": ch}
    if "0" <= ch <= "9":
        return {"key": ch, "code": f"Digit{ch}",
                "windowsVirtualKeyCode": ord(ch), "text": ch}
    if ch in _PUNCT:
        code, vk = _PUNCT[ch]
        return {"key": ch, "code": code, "windowsVirtualKeyCode": vk, "text": ch}
    if ch == " ":
        return {"key": " ", "code": "Space", "windowsVirtualKeyCode": 32, "text": " "}
    return None


# ------------------------------------------------------------------ CDP client

class _CDP:
    """Minimal synchronous CDP client over a page-level websocket.

    Whitelisted methods only; see module docstring.
    """

    ALLOWED_METHOD_PREFIXES = ("Page.", "Input.", "Emulation.", "Browser.")

    def __init__(self, ws_url: str, timeout: float = 30.0):
        self._ws = websocket.create_connection(
            ws_url, timeout=timeout, origin="http://127.0.0.1",
            suppress_origin=False,
        )
        self._next_id = 0
        self._lock = threading.Lock()

    def call(self, method: str, params: dict | None = None, timeout: float = 30.0):
        if not method.startswith(self.ALLOWED_METHOD_PREFIXES):
            raise PermissionError(f"CDP method not whitelisted: {method}")
        with self._lock:
            self._next_id += 1
            mid = self._next_id
            self._ws.send(json.dumps({"id": mid, "method": method,
                                      "params": params or {}}))
            deadline = time.monotonic() + timeout
            while time.monotonic() < deadline:
                msg = json.loads(self._ws.recv())
                if msg.get("id") != mid:
                    continue  # skip async events
                if "error" in msg:
                    raise RuntimeError(f"CDP {method} failed: {msg['error']}")
                return msg.get("result", {})
        raise TimeoutError(f"CDP {method} timed out")

    def wait_event(self, name: str, timeout: float = 10.0) -> bool:
        deadline = time.monotonic() + timeout
        self._ws.settimeout(max(0.1, deadline - time.monotonic()))
        try:
            while time.monotonic() < deadline:
                msg = json.loads(self._ws.recv())
                if msg.get("method") == name:
                    return True
        except Exception:
            return False
        return False

    def close(self):
        try:
            self._ws.close()
        except Exception:
            pass


# ------------------------------------------------------------------ helpers

def _free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


# loopback-only opener that ignores any system proxy configuration
_NO_PROXY_OPENER = urllib.request.build_opener(
    urllib.request.ProxyHandler({}))


def find_browser() -> Path:
    """Resolution order: BBB_BROWSER env > vendored Chrome for Testing > system."""
    if config.BROWSER_EXECUTABLE:
        p = Path(config.BROWSER_EXECUTABLE)
        if p.exists():
            return p
        raise FileNotFoundError(f"BBB_BROWSER set but not found: {p}")
    vendored = config.VENDOR_DIR / "chromium" / "chrome-win64" / "chrome.exe"
    if vendored.exists():
        return vendored
    for candidate in (
        r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        "/usr/bin/chromium", "/usr/bin/chromium-browser", "/usr/bin/google-chrome",
    ):
        p = Path(candidate)
        if p.exists():
            return p
    raise FileNotFoundError(
        "No browser found. Run scripts/bootstrap.py or set BBB_BROWSER."
    )


def _is_content_page(t: dict) -> bool:
    """A page target showing real content (not blank/new-tab/devtools)."""
    u = t.get("url") or ""
    return (u not in ("", "about:blank", "chrome://newtab/")
            and not u.startswith(("devtools://", "chrome-devtools://")))


# ------------------------------------------------------------------ runtime

class LocalChromiumRuntime(Runtime):
    """Runs: app subprocess (with gateway secret) + gateway proxy + headless Chromium.

    The agent never sees any of these ports or the secret; it only receives
    pixels through the controller.
    """

    def __init__(self, app_cmd: list[str], app_port: int, gateway_secret: str,
                 work_dir: Path, width: int | None = None, height: int | None = None):
        self._app_cmd = app_cmd
        self._app_port = app_port
        self._secret = gateway_secret
        self._work_dir = Path(work_dir)
        self._w = width or config.VIEWPORT_WIDTH
        self._h = height or config.VIEWPORT_HEIGHT
        self._gw_port = _free_port()
        self._cdp_port = _free_port()
        self._browser: subprocess.Popen | None = None
        self._app: subprocess.Popen | None = None
        self._profile_dir: Path | None = None
        self._cdp: _CDP | None = None
        self._ws_url: str | None = None
        self._current_target_id: str | None = None
        self._seen_target_ids: set = set()
        self._tab_order: list = []   # target ids in first-seen (open) order
        self._last_target_poll = 0.0
        self._gateway: threading.Thread | None = None
        self._gateway_server = None
        self._pointer = (0, 0)

    # ------------------------------------------------------------ lifecycle

    def start(self) -> None:
        self._work_dir.mkdir(parents=True, exist_ok=True)
        # A crashed or force-killed session cannot delete its own profile;
        # reclaim such leftovers before adding another one.
        reclaim_scratch()
        self._start_app()
        self._start_gateway()
        self._start_browser()

    def stop(self) -> None:
        self._kill_browser()
        self._kill_app()
        if self._gateway_server:
            try:
                self._gateway_server.shutdown()
            except Exception:
                pass

    def reset(self) -> None:
        # cold browser (fresh profile dir) — app data reset is the caller's job
        # (session manager reseeds before calling runtime.reset()).
        self._kill_browser()
        self._start_browser()

    def health(self) -> bool:
        try:
            if self._browser and self._browser.poll() is not None:
                return False
            if self._app and self._app.poll() is not None:
                return False
            self._cdp.call("Page.captureScreenshot", {"format": "png"}, timeout=5)
            return True
        except Exception:
            return False

    # ------------------------------------------------------------ app + gateway

    def _start_app(self) -> None:
        import os
        log = open(self._work_dir / "app.log", "ab")
        env = dict(os.environ)
        # the app module must resolve regardless of where it writes data
        env["PYTHONPATH"] = str(config.PROJECT_ROOT) + os.pathsep + \
            env.get("PYTHONPATH", "")
        self._app = subprocess.Popen(
            self._app_cmd, stdout=log, stderr=subprocess.STDOUT,
            cwd=str(config.PROJECT_ROOT), env=env,
        )
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            try:
                req = urllib.request.Request(
                    f"http://127.0.0.1:{self._app_port}/",
                    headers={config.GATEWAY_HEADER: self._secret},
                )
                with _NO_PROXY_OPENER.open(req, timeout=2) as r:
                    if r.status < 500:
                        return
            except Exception:
                if self._app.poll() is not None:
                    raise RuntimeError(
                        f"reference app exited early, see {self._work_dir/'app.log'}")
                time.sleep(0.25)
        raise TimeoutError("reference app did not become ready")

    def _kill_app(self) -> None:
        if self._app:
            self._app.terminate()
            try:
                self._app.wait(timeout=5)
            except Exception:
                self._app.kill()
            self._app = None

    def _start_gateway(self) -> None:
        """Threaded loopback proxy: reference-app.internal -> app, injecting the
        gateway secret header and scrubbing identifying response headers."""
        from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
        import httpx

        app_port, secret = self._app_port, self._secret
        # trust_env=False: never pick up system/env proxy settings — this proxy
        # must talk directly to the loopback app port.
        client = httpx.Client(timeout=10.0, trust_env=False)

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *a):  # silence
                pass

            def _proxy(self):
                url = f"http://127.0.0.1:{app_port}{self.path}"
                body = self.rfile.read(int(self.headers.get("Content-Length", 0) or 0)) \
                    if self.command in ("POST", "PUT", "PATCH", "DELETE") else None
                headers = {k: v for k, v in self.headers.items()
                           if k.lower() not in ("host", "content-length", "connection")}
                headers[config.GATEWAY_HEADER] = secret
                try:
                    resp = client.request(self.command, url, content=body,
                                          headers=headers, follow_redirects=False)
                except Exception:
                    self.send_error(502)
                    return
                self.send_response(resp.status_code)
                for k, v in resp.headers.items():
                    if k.lower() in ("server", "x-powered-by", "transfer-encoding",
                                     "connection", "content-length"):
                        continue
                    self.send_header(k, v)
                self.send_header("Content-Length", str(len(resp.content)))
                self.end_headers()
                self.wfile.write(resp.content)

            do_GET = do_POST = do_PUT = do_PATCH = do_DELETE = _proxy

        self._gateway_server = ThreadingHTTPServer(("127.0.0.1", self._gw_port), Handler)
        self._gateway = threading.Thread(target=self._gateway_server.serve_forever,
                                         daemon=True)
        self._gateway.start()

    # ------------------------------------------------------------ browser

    def _start_browser(self) -> None:
        exe = find_browser()
        # A Chromium user-data dir is a few hundred cache files that nobody
        # reads after the session. Keeping it out of the repository stops every
        # teardown from bulk-deleting workspace files and keeps `runs/` an
        # evidence tree. See benchmark/scratch.py.
        self._profile_dir = new_scratch_dir(
            "chromium", self._work_dir.parent.name)
        log = open(self._work_dir / "browser.log", "ab")
        args = [
            str(exe),
            "--headless=new", "--no-sandbox", "--disable-crashpad",
            "--disable-gpu", "--mute-audio",
            f"--remote-debugging-port={self._cdp_port}",
            "--remote-allow-origins=*",
            f"--user-data-dir={self._profile_dir}",
            f"--window-size={self._w},{self._h}",
            "--force-device-scale-factor=1",
            "--no-first-run", "--no-default-browser-check",
            "--disable-extensions", "--disable-component-update",
            "--disable-background-networking", "--disable-sync",
            "--disable-translate", "--hide-scrollbars=false",
            "--password-store=basic", "--disable-features=TranslateUI",
            # network lock: no proxy (system proxies must not see our traffic),
            # and only the internal app hostname resolves
            "--no-proxy-server",
            f"--host-resolver-rules=MAP {config.APP_INTERNAL_HOST} 127.0.0.1,"
            " MAP * ~NOTFOUND",
            "about:blank",
        ]
        self._browser = subprocess.Popen(args, stdout=log, stderr=subprocess.STDOUT)
        self._attach(self._await_debugger())
        self._cdp.call("Page.navigate", {
            "url": f"http://{config.APP_INTERNAL_HOST}:{self._gw_port}/"})
        self._cdp.wait_event("Page.loadEventFired", timeout=15)
        time.sleep(0.3)

    # ------------------------------------------------------------ target tracking

    def _attach(self, ws_url: str) -> None:
        """Attach to a page target: CDP + Page.enable + fixed viewport.
        The emulation override is per-target and must be re-applied on every
        (re)attach or screenshots/coordinates drift."""
        if self._cdp:
            try:
                self._cdp.close()
            except Exception:
                pass
        self._cdp = _CDP(ws_url)
        self._ws_url = ws_url
        self._current_target_id = ws_url.rsplit("/", 1)[-1]
        self._seen_target_ids.add(self._current_target_id)
        self._cdp.call("Page.enable")
        self._cdp.call("Emulation.setDeviceMetricsOverride", {
            "width": self._w, "height": self._h,
            "deviceScaleFactor": config.DEVICE_SCALE_FACTOR, "mobile": False})

    def _page_targets(self, attempts: int = 3) -> list[dict]:
        # DevTools' /json/list is loopback HTTP, but it still drops the odd
        # request while the browser is busy swapping renderers. Callers on the
        # action path (switch_tab, close_tab) would turn that into a session
        # failure, so absorb a single blip here. Callers that already poll in a
        # loop pass attempts=1 and keep their own cadence.
        last: Exception | None = None
        for attempt in range(max(1, attempts)):
            try:
                with _NO_PROXY_OPENER.open(
                        f"http://127.0.0.1:{self._cdp_port}/json/list",
                        timeout=2) as r:
                    return [t for t in json.loads(r.read())
                            if t.get("type") == "page"]
            except Exception as e:
                last = e
                if attempt < max(1, attempts) - 1:
                    time.sleep(0.2)
        assert last is not None
        raise last

    @staticmethod
    def _target_to_follow(targets: list[dict], seen_ids: set,
                          current_id: str | None) -> dict | None:
        """Which page target the agent should be attached to.

        Follows tabs NEWLY opened by the site (target=_blank jumps) so
        exploration continues on the new page; otherwise stays on the current
        target; falls back when the current target vanished. Deliberately
        list-order independent (DevTools /json/list ordering varies).
        """
        content = [t for t in targets if _is_content_page(t)]
        pool = content or targets
        if not pool:
            return None
        for t in content:  # a freshly opened content tab wins
            if t.get("id") not in seen_ids and t.get("id") != current_id:
                return t
        cur = next((t for t in pool if t.get("id") == current_id), None)
        return cur or pool[0]

    def _follow_latest(self) -> None:
        """If the site opened a new content tab since we last looked, re-attach
        to it. Throttled; never raises — target listing is best-effort."""
        now = time.monotonic()
        if now - self._last_target_poll < 0.25:
            return
        self._last_target_poll = now
        try:
            targets = self._page_targets()
        except Exception:
            return
        t = self._target_to_follow(targets, self._seen_target_ids,
                                   self._current_target_id)
        # Only CONTENT tabs are marked seen. A tab first observed as
        # about:blank must NOT be recorded here — real sites open blank tabs
        # that navigate a moment later, and marking them early would make the
        # follow logic ignore them forever (agent stuck on the old page).
        self._seen_target_ids |= {x.get("id") for x in targets
                                  if _is_content_page(x)}
        if t:
            ws = t.get("webSocketDebuggerUrl")
            if ws and ws != self._ws_url:
                try:
                    self._attach(ws)
                except Exception:
                    pass

    # ------------------------------------------------------------ tab strip

    def _alive_tabs(self) -> list[dict]:
        """All page targets (devtools excluded) in first-seen order — the
        stable tab indices the agent reasons about."""
        targets = [t for t in self._page_targets()
                   if not (t.get("url") or "").startswith(
                       ("devtools://", "chrome-devtools://"))]
        alive = {t.get("id"): t for t in targets}
        self._tab_order = [i for i in self._tab_order if i in alive]
        for t in targets:
            if t.get("id") not in self._tab_order:
                self._tab_order.append(t.get("id"))
        return [alive[i] for i in self._tab_order]

    def list_tabs(self) -> dict | None:
        try:
            tabs = self._alive_tabs()
        except Exception:
            return None
        ids = [t.get("id") for t in tabs]
        try:
            active = ids.index(self._current_target_id)
        except ValueError:
            active = -1
        return {"count": len(tabs), "active": active}

    def switch_tab(self, index: int) -> None:
        tabs = self._alive_tabs()
        if not 0 <= index < len(tabs):
            raise ValueError(f"invalid_tab_index: {index} (count={len(tabs)})")
        ws = tabs[index].get("webSocketDebuggerUrl")
        if ws and ws != self._ws_url:
            self._attach(ws)

    def close_tab(self) -> None:
        """Close the CURRENT tab (the human's × on the tab strip) and fall
        back to the previous one. Refused when it is the last tab."""
        tabs = self._alive_tabs()
        if len(tabs) <= 1:
            raise ValueError("cannot_close_last_tab")
        cur = self._current_target_id
        ids = [t.get("id") for t in tabs]
        i = ids.index(cur) if cur in ids else len(tabs) - 1
        self._cdp_call("Page.close")  # closes the attached target; ws dies
        self._seen_target_ids.discard(cur)
        self._cdp = None
        self._ws_url = None
        self._current_target_id = None
        remaining = [t for t in self._alive_tabs() if t.get("id") != cur]
        nxt = remaining[min(max(i - 1, 0), len(remaining) - 1)]
        self._attach(nxt["webSocketDebuggerUrl"])

    def _await_debugger(self, timeout: float = 30.0) -> str:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self._browser.poll() is not None:
                raise RuntimeError(
                    f"browser exited early, see {self._work_dir/'browser.log'}")
            try:
                targets = self._page_targets(attempts=1)
                t = self._target_to_follow(targets, self._seen_target_ids,
                                           self._current_target_id) \
                    or (targets[0] if targets else None)
                if t:
                    return t["webSocketDebuggerUrl"]
            except Exception:
                time.sleep(0.25)
        raise TimeoutError("CDP endpoint did not come up")

    def _kill_browser(self) -> None:
        if self._cdp:
            try:
                self._cdp.call("Browser.close", timeout=3)
            except Exception:
                pass
            self._cdp.close()
            self._cdp = None
        if self._browser:
            try:
                self._browser.wait(timeout=5)
            except Exception:
                self._browser.kill()
                self._browser.wait(timeout=5)
            self._browser = None
        if self._profile_dir:
            # Windows can hold a renderer handle open for a moment after exit,
            # so this retries rather than giving up silently. The directory is
            # outside the repository and owner-tagged, so even a hard failure is
            # reclaimed by the next runtime start.
            remove_scratch_dir(self._profile_dir)
            self._profile_dir = None

    # ------------------------------------------------------------ pixels

    def _reconnect(self) -> None:
        """Re-establish the page-level CDP connection (navigation or renderer
        swap may invalidate it)."""
        self._attach(self._await_debugger(timeout=10))

    def _cdp_call(self, method: str, params: dict | None = None,
                  retries: int = 8, delay: float = 0.2):
        """CDP call with resilience to transient navigation detach and socket
        resets (WinError 10054 et al. surface as bare OSError from the ws lib).

        The wait between attempts backs off. A fixed 0.2 s gave a total window
        of well under two seconds, which is enough for a renderer swap but not
        for a real website's network hiccup or a browser that is briefly busy —
        two live explorations were lost tens of steps in to `WinError 10054` and
        `Connection timed out` that a slightly more patient retry would have
        ridden out. An environment blip must not be recorded as an observation
        about the Agent. A reconnect that itself fails also costs an attempt
        rather than being retried instantly against a dead socket.
        """
        last: Exception | None = None
        for attempt in range(retries):
            try:
                return self._cdp.call(method, params)
            except Exception as e:
                last = e
                msg = str(e)
                transient = (
                    "Not attached" in msg
                    or "closed" in msg.lower()
                    or "timed out" in msg.lower()
                    or "WebSocket" in type(e).__name__
                    or isinstance(e, (OSError, TimeoutError))
                )
                if not transient or attempt == retries - 1:
                    raise
                # 0.2, 0.4, 0.8, 1.6, 3.2, 3.2, 3.2 → ~12.6 s in total
                time.sleep(min(delay * (2 ** attempt), 3.2))
                if "Not attached" not in msg:
                    # connection-level failure: reconnect before retrying
                    try:
                        self._reconnect()
                    except Exception:
                        pass
        raise last  # unreachable, keeps type checkers happy

    def screenshot(self) -> bytes:
        self._follow_latest()
        result = self._cdp_call("Page.captureScreenshot", {"format": "png"})
        return base64.b64decode(result["data"])

    def reload(self) -> None:
        """Page.reload — stays inside the whitelisted Page domain.

        The web evaluation condition uses this as the browser counterpart of
        an app restart (persistence probes). Exploration conditions do not
        expose it to their agents; a raw F5 key event would not reach the
        browser's command controller from page-level CDP input.
        """
        self._follow_latest()
        self._cdp_call("Page.reload", {})
        self._cdp.wait_event("Page.loadEventFired", timeout=15)
        time.sleep(0.2)

    def info(self) -> RuntimeInfo:
        return RuntimeInfo(self._w, self._h, config.DEVICE_SCALE_FACTOR)

    # ------------------------------------------------------------ input

    def mouse_move(self, x: int, y: int) -> None:
        self._cdp_call("Input.dispatchMouseEvent",
                       {"type": "mouseMoved", "x": x, "y": y})
        self._pointer = (x, y)

    def mouse_down(self, x: int, y: int, button: str = "left") -> None:
        self._follow_latest()
        self.mouse_move(x, y)
        self._cdp_call("Input.dispatchMouseEvent", {
            "type": "mousePressed", "x": x, "y": y, "button": button,
            "clickCount": 1})

    def mouse_up(self, x: int, y: int, button: str = "left") -> None:
        self._cdp_call("Input.dispatchMouseEvent", {
            "type": "mouseReleased", "x": x, "y": y, "button": button,
            "clickCount": 1})

    def click(self, x: int, y: int, button: str = "left", count: int = 1) -> None:
        self.mouse_move(x, y)
        for i in range(count):
            self._cdp_call("Input.dispatchMouseEvent", {
                "type": "mousePressed", "x": x, "y": y, "button": button,
                "clickCount": i + 1})
            self._cdp_call("Input.dispatchMouseEvent", {
                "type": "mouseReleased", "x": x, "y": y, "button": button,
                "clickCount": i + 1})

    def scroll(self, dx: int, dy: int) -> None:
        self._follow_latest()
        x, y = self._pointer
        self._cdp_call("Input.dispatchMouseEvent", {
            "type": "mouseWheel", "x": x, "y": y,
            "deltaX": dx, "deltaY": dy})

    def type_text(self, text: str) -> None:
        self._follow_latest()
        for ch in text:
            params = _char_key_params(ch)
            if params is None:
                # non-ASCII: text-level insertion (no physical key equivalent)
                self._cdp_call("Input.insertText", {"text": ch})
                continue
            self._cdp_call("Input.dispatchKeyEvent",
                           {"type": "keyDown", **params})
            self._cdp_call("Input.dispatchKeyEvent",
                           {"type": "keyUp", **{k: v for k, v in params.items()
                                                if k != "text"}})

    def _history_navigate(self, delta: int) -> None:
        """Browser Back/Forward without modifiers: Page.getNavigationHistory +
        navigateToHistoryEntry stay inside the whitelisted Page domain, expose
        nothing to the agent (receipts remain accepted-only), and work in both
        headless and headed mode — a raw BrowserBack key event does not reach
        the browser UI's command controller."""
        h = self._cdp_call("Page.getNavigationHistory")
        entries = h.get("entries", [])
        target = h.get("currentIndex", 0) + delta
        if 0 <= target < len(entries):
            self._cdp_call("Page.navigateToHistoryEntry",
                           {"entryId": entries[target]["id"]})

    def key(self, key: str, kind: str = "press") -> None:
        self._follow_latest()
        if key in ("BrowserBack", "BrowserForward"):
            if kind == "press":
                self._history_navigate(-1 if key == "BrowserBack" else 1)
            return
        if key in _NAMED_KEYS:
            k, code, vk = _NAMED_KEYS[key]
            params = {"key": k, "code": code, "windowsVirtualKeyCode": vk}
            # Enter needs a text payload on keyDown to generate the keypress
            # that triggers form submission / activation behavior.
            if k == "Enter":
                params["text"] = "\r"
        elif len(key) == 1:
            params = _char_key_params(key) or {"key": key, "code": "",
                                               "windowsVirtualKeyCode": 0}
        else:
            raise ValueError(f"unknown key: {key!r}")
        if kind == "press":
            self._cdp_call("Input.dispatchKeyEvent", {"type": "keyDown", **params})
            self._cdp_call("Input.dispatchKeyEvent", {"type": "keyUp", **params})
        elif kind == "down":
            self._cdp_call("Input.dispatchKeyEvent", {"type": "keyDown", **params})
        elif kind == "up":
            self._cdp_call("Input.dispatchKeyEvent", {"type": "keyUp", **params})
        else:
            raise ValueError(f"unknown key kind: {kind!r}")
