"""Runtime abstraction: the ONLY code allowed to talk to the browser/app.

A Runtime drives a black-box reference application. Its capability surface is
deliberately limited to pixels in / human input out. There is intentionally
NO way to evaluate script, read the DOM, query selectors, read the URL, inspect
network traffic, or access cookies/storage — benchmark validity depends on that
(see docs/security_model.md, threat T2).

Implementations:
- local_chromium.LocalChromiumRuntime  (dev mode: headless Chromium via CDP,
  screenshot + input dispatch only)
- docker_x11.DockerX11Runtime          (container mode: Xvfb + xdotool + scrot
  behind an in-container RPC server)
"""
from __future__ import annotations

import abc
from dataclasses import dataclass


@dataclass
class RuntimeInfo:
    width: int
    height: int
    device_scale_factor: float


class Runtime(abc.ABC):
    """Pixels + HID. Nothing else. Ever."""

    @abc.abstractmethod
    def start(self) -> None:
        """Launch (or attach to) the environment and navigate to the app entry."""

    @abc.abstractmethod
    def stop(self) -> None:
        """Tear down the environment completely (browser + app processes)."""

    @abc.abstractmethod
    def reset(self) -> None:
        """Return the app to its canonical seeded initial state S0.

        Must recreate app data from seed and start from a cold browser
        (no cookies, no cache, no storage carried over).
        """

    @abc.abstractmethod
    def screenshot(self) -> bytes:
        """Capture the current screen as PNG bytes. Pixels only."""

    @abc.abstractmethod
    def info(self) -> RuntimeInfo:
        """Viewport geometry used for both screenshots and input coordinates."""

    # ------------------------------------------------------- human input
    @abc.abstractmethod
    def mouse_move(self, x: int, y: int) -> None: ...

    @abc.abstractmethod
    def mouse_down(self, x: int, y: int, button: str = "left") -> None: ...

    @abc.abstractmethod
    def mouse_up(self, x: int, y: int, button: str = "left") -> None: ...

    @abc.abstractmethod
    def click(self, x: int, y: int, button: str = "left", count: int = 1) -> None:
        """move+press+release at (x, y); count=2 for double click."""

    @abc.abstractmethod
    def scroll(self, dx: int, dy: int) -> None: ...

    @abc.abstractmethod
    def type_text(self, text: str) -> None:
        """Type text as keyboard input into whatever currently has focus.
        There is no element targeting: focus is obtained by clicking first."""

    @abc.abstractmethod
    def key(self, key: str, kind: str = "press") -> None:
        """kind: press | down | up. `key` examples: Enter, Tab, Escape, F5, a."""

    @abc.abstractmethod
    def health(self) -> bool:
        """True if the browser and app are alive and responsive."""

    # ------------------------------------------------------- tab strip
    # Window-management level info only: how many tabs and which is active.
    # URLs/titles are NEVER exposed (benchmark-internal by design).
    def list_tabs(self) -> dict | None:
        """{"count": N, "active": i} in tab-open order; None if the runtime
        has no tab concept."""
        return None

    def switch_tab(self, index: int) -> None:
        raise ValueError("unsupported_action: tabs not supported")

    def close_tab(self) -> None:
        raise ValueError("unsupported_action: tabs not supported")
