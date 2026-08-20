"""Docker-mode Runtime driver: talks to the reference container's Runtime RPC
service over HTTP (docs/architecture.md §4.1).

This is the container-mode counterpart of LocalChromiumRuntime. It implements
the exact capability surface of `runtime.base.Runtime` — pixels in, HID out —
and nothing else. In particular this driver has NO way to evaluate script,
read the DOM/AXTree, query selectors, read the current URL, or inspect
network traffic: the RPC server on the other side does not implement such
endpoints either (docs/security_model.md T2), so the ability does not exist
on either end of the wire.

Environment variables:
- BBB_REFERENCE_RPC   base URL of the Runtime RPC (default http://127.0.0.1:8300;
                      compose sets http://reference:8300)
- BBB_VIEWPORT_WIDTH / BBB_VIEWPORT_HEIGHT   viewport geometry (config.py)

NOTE: statically reviewed only — not exercised against a live reference
container on the authoring machine (no Docker available there).
"""
from __future__ import annotations

import os
import re
import time
from typing import Optional

import httpx

from benchmark import config
from benchmark.runtime.base import Runtime, RuntimeInfo

_KEYSYM_RE = re.compile(r"^[A-Za-z0-9_]{1,32}$")

# Controller-side key names (docs/api_contract.md §1.2) -> X keysyms used by
# xdotool inside the reference container. Single printable characters and
# already-valid keysyms pass through unchanged.
_KEY_MAP = {
    "Enter": "Return",
    "Backspace": "BackSpace",
    "Escape": "Escape",
    "Tab": "Tab",
    "Delete": "Delete",
    "Insert": "Insert",
    "Home": "Home",
    "End": "End",
    "PageUp": "Page_Up",
    "PageDown": "Page_Down",
    "ArrowUp": "Up",
    "ArrowDown": "Down",
    "ArrowLeft": "Left",
    "ArrowRight": "Right",
    "Space": "space",
    **{f"F{i}": f"F{i}" for i in range(1, 13)},
}

_BUTTONS = ("left", "middle", "right")


class RuntimeUnavailableError(RuntimeError):
    """The reference environment did not answer or reported a failure."""


class DockerX11Runtime(Runtime):
    """Runtime backed by the Xvfb+xdotool+scrot stack behind the Runtime RPC.

    Pixels + HID. Nothing else. Ever.
    """

    def __init__(self, rpc_url: str, *, width: Optional[int] = None,
                 height: Optional[int] = None,
                 device_scale_factor: float = config.DEVICE_SCALE_FACTOR,
                 request_timeout_s: float = 30.0,
                 start_timeout_s: float = 60.0) -> None:
        self._rpc = rpc_url.rstrip("/")
        self._info = RuntimeInfo(
            width=width or config.VIEWPORT_WIDTH,
            height=height or config.VIEWPORT_HEIGHT,
            device_scale_factor=device_scale_factor,
        )
        self._start_timeout_s = start_timeout_s
        self._client = httpx.Client(
            base_url=self._rpc,
            timeout=httpx.Timeout(request_timeout_s, connect=5.0),
            trust_env=False,  # in-cluster loopback only; never via system proxy
        )

    @classmethod
    def from_config(cls) -> "DockerX11Runtime":
        """Build from BBB_* environment variables (see benchmark/config.py)."""
        return cls(
            rpc_url=os.environ.get("BBB_REFERENCE_RPC", "http://127.0.0.1:8300"),
            width=config.VIEWPORT_WIDTH,
            height=config.VIEWPORT_HEIGHT,
            device_scale_factor=config.DEVICE_SCALE_FACTOR,
        )

    # ------------------------------------------------------------ plumbing
    def _request(self, method: str, path: str, **kwargs) -> httpx.Response:
        try:
            resp = self._client.request(method, path, **kwargs)
        except httpx.HTTPError as exc:
            raise RuntimeUnavailableError(
                f"reference RPC unreachable: {exc}") from exc
        if resp.status_code >= 400:
            raise RuntimeUnavailableError(
                f"reference RPC {method} {path} -> {resp.status_code}: "
                f"{resp.text[:200]}")
        return resp

    def _post(self, path: str, payload: Optional[dict] = None) -> None:
        self._request("POST", path, json=payload or {})

    # ------------------------------------------------------------ lifecycle
    def start(self) -> None:
        """Attach to the already-running reference container.

        The environment (Xvfb/app/gateway/chromium) is launched by the
        container entrypoint; start() blocks until the RPC reports healthy,
        which implies the kiosk browser is up on the app entry page.
        """
        deadline = time.monotonic() + self._start_timeout_s
        while time.monotonic() < deadline:
            if self.health():
                return
            time.sleep(0.5)
        raise RuntimeUnavailableError(
            f"reference RPC at {self._rpc} not healthy within "
            f"{self._start_timeout_s:.0f}s")

    def stop(self) -> None:
        """Detach from the environment.

        The container lifecycle belongs to docker compose (the RPC exposes no
        shutdown endpoint, by design), so this only releases local resources.
        """
        self._client.close()

    def reset(self) -> None:
        """Cold browser (fresh profile) + data reseeded to S0; see RPC /reset."""
        self._post("/reset")

    # ------------------------------------------------------------ pixels
    def screenshot(self) -> bytes:
        resp = self._request("POST", "/screenshot")
        return resp.content

    def info(self) -> RuntimeInfo:
        return self._info

    # ------------------------------------------------------------ human input
    def mouse_move(self, x: int, y: int) -> None:
        self._post("/input/mouse", {"kind": "move", "x": x, "y": y})

    def mouse_down(self, x: int, y: int, button: str = "left") -> None:
        self._check_button(button)
        self._post("/input/mouse",
                   {"kind": "down", "x": x, "y": y, "button": button})

    def mouse_up(self, x: int, y: int, button: str = "left") -> None:
        self._check_button(button)
        self._post("/input/mouse",
                   {"kind": "up", "x": x, "y": y, "button": button})

    def click(self, x: int, y: int, button: str = "left", count: int = 1) -> None:
        self._check_button(button)
        self._post("/input/mouse",
                   {"kind": "click", "x": x, "y": y, "button": button,
                    "count": count})

    def scroll(self, dx: int, dy: int) -> None:
        self._post("/input/scroll", {"dx": dx, "dy": dy})

    def type_text(self, text: str) -> None:
        self._post("/input/type", {"text": text})

    def key(self, key: str, kind: str = "press") -> None:
        if kind not in ("press", "down", "up"):
            raise ValueError(f"invalid key kind: {kind!r}")
        keysym = _KEY_MAP.get(key, key)
        if not _KEYSYM_RE.match(keysym):
            raise ValueError(f"unmappable key: {key!r}")
        self._post("/input/key", {"key": keysym, "kind": kind})

    # ------------------------------------------------------------ liveness
    def health(self) -> bool:
        try:
            resp = self._client.get("/health")
        except httpx.HTTPError:
            return False
        if resp.status_code != 200:
            return False
        try:
            return resp.json().get("status") == "ok"
        except ValueError:
            return False

    # ------------------------------------------------------------ helpers
    @staticmethod
    def _check_button(button: str) -> None:
        if button not in _BUTTONS:
            raise ValueError(f"invalid button: {button!r}")
