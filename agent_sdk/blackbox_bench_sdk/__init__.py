"""BlackBoxBench Agent SDK — pixels in, human input out, structured memory.

This SDK is the ONLY supported way for an agent to interact with a benchmark
session. It deliberately has no methods like find_element(), get_dom(),
get_url(), get_html() or get_network() — the information boundary is the
benchmark (see docs/security_model.md).

Example
-------
    from blackbox_bench_sdk import Environment

    env = Environment("http://127.0.0.1:7800", session_id="sess_...")
    obs = env.observe()
    obs.save("frame.png")          # PNG bytes are yours to feed a VLM
    env.click(812, 431)
    env.type_text("camera")
    env.key_press("Enter")
    env.record_feature({...})
    env.finalize()
"""
from __future__ import annotations

import base64
from dataclasses import dataclass
from typing import Any, Optional

import httpx


class BudgetExhaustedError(RuntimeError):
    pass


class SessionClosedError(RuntimeError):
    pass


class ActionRejectedError(RuntimeError):
    pass


@dataclass
class Observation:
    """What the agent is allowed to see. Pixels + cursor + budget + task brief."""
    frame_id: int
    timestamp: str
    width: int
    height: int
    screenshot_png: bytes
    cursor: dict
    budget: dict
    brief: str = ""     # operator-authored task info (e.g. a demo account);
                        # never app internals
    tabs: dict | None = None  # {"count": N, "active": i}; window-manager level
    platform: str = "web"
    orientation: str | None = None
    density_dpi: int | None = None

    def save(self, path: str) -> None:
        with open(path, "wb") as f:
            f.write(self.screenshot_png)


@dataclass
class ActionResult:
    """Receipt of an OS-level input event. `accepted` means the input was
    delivered — never what it *did*; read the next observation for that.
    `tabs` is window-manager info only: {"count": N, "active": i}."""
    accepted: bool
    frame_id: int
    step: int
    tabs: dict | None = None


class Environment:
    def __init__(self, controller_url: str, session_id: str,
                 timeout: float = 60.0, client: "httpx.Client | None" = None):
        self._base = controller_url.rstrip("/")
        self._sid = session_id
        # trust_env=False: never route controller traffic through a system
        # proxy — the controller is always loopback/in-cluster.
        # `client` allows injecting a prebuilt transport (e.g. starlette
        # TestClient for in-process tests).
        self._client = client or httpx.Client(timeout=timeout,
                                              trust_env=False)

    # ------------------------------------------------------------ core I/O

    @staticmethod
    def _response_json(r) -> dict:
        """Apply the SDK's error mapping consistently to every HTTP verb."""
        if r.status_code == 403:
            raise BudgetExhaustedError(r.json().get("detail", "budget_exhausted"))
        if r.status_code == 410:
            raise SessionClosedError(r.json().get("detail", "session_closed"))
        if r.status_code == 400:
            raise ActionRejectedError(r.json().get("detail", "invalid_action"))
        if r.status_code == 422:
            raise ActionRejectedError(
                f"{r.json().get('detail')}: {r.json().get('message', '')}")
        r.raise_for_status()
        return r.json()

    def _post(self, path: str, payload: dict) -> dict:
        r = self._client.post(f"{self._base}/agent/{self._sid}{path}",
                              json=payload)
        return self._response_json(r)

    def observe(self) -> Observation:
        d = self._post("/observe", {})
        return Observation(
            frame_id=d["frame_id"], timestamp=d["timestamp"],
            width=d["width"], height=d["height"],
            screenshot_png=base64.b64decode(d["screenshot_png_b64"]),
            cursor=d["cursor"], budget=d["budget"],
            brief=d.get("brief", ""), tabs=d.get("tabs"),
            platform=d.get("platform", "web"),
            orientation=d.get("orientation"),
            density_dpi=d.get("density_dpi"))

    # ------------------------------------------------------------ actions

    def _action(self, **kw) -> ActionResult:
        d = self._post("/action", kw)
        return ActionResult(accepted=d["accepted"], frame_id=d["frame_id"],
                            step=d["step"], tabs=d.get("tabs"))

    def click(self, x: int, y: int) -> ActionResult:
        return self._action(type="click", x=x, y=y)

    def double_click(self, x: int, y: int) -> ActionResult:
        return self._action(type="double_click", x=x, y=y)

    def move_pointer(self, x: int, y: int) -> ActionResult:
        return self._action(type="move_pointer", x=x, y=y)

    def mouse_down(self, x: int, y: int) -> ActionResult:
        return self._action(type="mouse_down", x=x, y=y)

    def mouse_up(self, x: int, y: int) -> ActionResult:
        return self._action(type="mouse_up", x=x, y=y)

    def drag(self, x1: int, y1: int, x2: int, y2: int,
             duration_ms: int = 500) -> ActionResult:
        return self._action(type="drag", x1=x1, y1=y1, x2=x2, y2=y2,
                            duration_ms=duration_ms)

    def type_text(self, text: str) -> ActionResult:
        return self._action(type="type_text", text=text)

    def key_press(self, key: str) -> ActionResult:
        return self._action(type="key_press", key=key)

    def key_down(self, key: str) -> ActionResult:
        return self._action(type="key_down", key=key)

    def key_up(self, key: str) -> ActionResult:
        return self._action(type="key_up", key=key)

    def scroll(self, dx: int, dy: int) -> ActionResult:
        return self._action(type="scroll", dx=dx, dy=dy)

    def wait(self, ms: int) -> ActionResult:
        return self._action(type="wait", ms=ms)

    def switch_tab(self, tab_index: int) -> ActionResult:
        """Switch to the tab at `tab_index` (0-based, in tab-open order)."""
        return self._action(type="switch_tab", tab_index=tab_index)

    def close_tab(self) -> ActionResult:
        """Close the current tab and fall back to the previous one."""
        return self._action(type="close_tab")

    # Android pixels + touch.  These methods expose no device or UI semantics.
    def tap(self, x: int, y: int) -> ActionResult:
        return self._action(type="tap", x=x, y=y)

    def long_press(self, x: int, y: int,
                   duration_ms: int = 700) -> ActionResult:
        return self._action(type="long_press", x=x, y=y,
                            duration_ms=duration_ms)

    def swipe(self, x1: int, y1: int, x2: int, y2: int,
              duration_ms: int = 400) -> ActionResult:
        return self._action(type="swipe", x1=x1, y1=y1, x2=x2, y2=y2,
                            duration_ms=duration_ms)

    def press_back(self) -> ActionResult:
        return self._action(type="press_back")

    def press_enter(self) -> ActionResult:
        return self._action(type="press_enter")

    def restart_app(self) -> ActionResult:
        return self._action(type="restart_app")

    # ------------------------------------------------------------ discovery memory

    def record_state(self, name: str, description: str = "",
                     visual_evidence: Optional[list[int]] = None,
                     entry_conditions: Optional[list[str]] = None,
                     observed_elements: Optional[list[str]] = None,
                     confidence: float = 0.5) -> str:
        return self._post("/discovery/state", {
            "name": name, "description": description,
            "visual_evidence": visual_evidence or [],
            "entry_conditions": entry_conditions or [],
            "observed_elements": observed_elements or [],
            "confidence": confidence})["state_id"]

    def record_feature(self, name: str, description: str = "",
                       behavior: Optional[dict] = None,
                       evidence: Optional[list[dict]] = None,
                       confidence: float = 0.5,
                       status: str = "confirmed") -> str:
        return self._post("/discovery/feature", {
            "name": name, "description": description,
            "behavior": behavior or {}, "evidence": evidence or [],
            "confidence": confidence, "status": status})["feature_id"]

    def record_data(self, name: str, description: str = "",
                    confidence: float = 0.5) -> str:
        return self._post("/discovery/data", {
            "name": name, "description": description,
            "confidence": confidence})["data_id"]

    def record_edge(self, source: str, target: str, type: str,
                    evidence: Optional[list[dict]] = None,
                    confidence: float = 0.5, status: str = "confirmed",
                    note: Optional[str] = None) -> str:
        return self._post("/discovery/edge", {
            "source": source, "target": target, "type": type,
            "evidence": evidence or [], "confidence": confidence,
            "status": status, "note": note})["edge_id"]

    def record_hypothesis(self, statement: str, confidence: float = 0.5,
                          next_probe: str = "",
                          evidence: Optional[list[dict]] = None) -> str:
        return self._post("/discovery/hypothesis", {
            "statement": statement, "confidence": confidence,
            "next_probe": next_probe, "evidence": evidence or []})["hypothesis_id"]

    def resolve_hypothesis(self, hypothesis_id: str, status: str,
                           note: Optional[str] = None,
                           evidence: Optional[list[dict]] = None) -> None:
        self._post(f"/discovery/hypothesis/{hypothesis_id}/resolve", {
            "status": status, "note": note, "evidence": evidence or []})

    def revise(self, kind: str, node_id: str, op: str,
               fields: Optional[dict] = None, into: Optional[str] = None,
               reason: Optional[str] = None) -> dict:
        response = self._client.patch(
            f"{self._base}/agent/{self._sid}/discovery/{kind}/{node_id}",
            json={"op": op, "fields": fields, "into": into,
                  "reason": reason})
        return self._response_json(response)

    def finalize(self) -> dict:
        return self._post("/finalize", {})

    # ------------------------------------------------------------ context manager

    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "Environment":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


__all__ = ["Environment", "Observation", "ActionResult", "BudgetExhaustedError",
           "SessionClosedError", "ActionRejectedError"]
