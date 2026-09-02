"""Session: one benchmark run against one seeded reference environment.

Owns: runtime (pixels+HID only), trace recorder, topology store, budget,
virtual cursor. All agent-visible outputs are produced here and nowhere else.
"""
from __future__ import annotations

import base64
import json
import threading
import time
from pathlib import Path

from .. import config
from ..recorder import diff as imgdiff
from ..recorder.cursor import draw_cursor
from ..recorder.trace import TraceRecorder, utc_now
from ..runtime.base import Runtime
from ..topology import crosscheck
from ..topology import models as m
from ..topology.store import TopologyStore
from .apps import AppSpec


class BudgetExhausted(Exception):
    pass


class SessionClosed(Exception):
    pass


class TransientEnvironmentError(Exception):
    """A recoverable environment blip: the step failed, the session lives on.

    A device or browser that drops one operation while remaining healthy is an
    environment artefact, not an observation about the Agent. Ending an
    exploration dozens of steps in because of it discards real work, so the
    Agent is told this single step did not go through and may simply continue.
    """


def _agent_safe_message(exc: Exception) -> str:
    """A neutral description of a runtime failure, safe to hand to an Agent.

    Raw runtime exceptions name the tools behind the pixels-only boundary: an
    adb command line and emulator serial, a CDP method, a websocket URL, host
    absolute paths. Those must never reach the Agent — they disclose that ADB or
    CDP exist and what they were asked to do. Only exceptions that were
    deliberately worded for this purpose (`agent_safe`) are passed through; full
    technical detail stays in the local crash report, which no Agent can read.
    """
    if getattr(exc, "agent_safe", False):
        return str(exc)
    return "the environment did not complete this operation"


# Live targets run HEADED: F12 would open DevTools and leak the DOM as pixels.
# It is the only single-key escape hatch — the action schema has no modifier
# keys (no Ctrl+Shift+I / Ctrl+L) and no right-click — so blocking F12 plus
# the resolver-rule allowlist keeps the live browser on the target site.
_LIVE_BLOCKED_KEYS = {"F12"}


class Budget:
    def __init__(self, max_actions: int, max_duration_s: int, max_observations: int):
        self.max_actions = max_actions
        self.max_duration_s = max_duration_s
        self.max_observations = max_observations
        self.actions_used = 0
        self.observations_used = 0
        self.started_at = time.monotonic()
        # Set by freeze() when the session reaches a terminal state; until
        # then the clock is live. Status views of closed/failed sessions must
        # not keep ticking.
        self.ended_at: float | None = None

    def freeze(self) -> None:
        if self.ended_at is None:
            self.ended_at = time.monotonic()

    def elapsed(self) -> float:
        end = self.ended_at if self.ended_at is not None else time.monotonic()
        return end - self.started_at

    def remaining(self) -> dict:
        elapsed = self.elapsed()
        return {
            "actions_remaining": max(0, self.max_actions - self.actions_used),
            "seconds_remaining": max(0, int(self.max_duration_s - elapsed)),
            "observations_remaining": max(0, self.max_observations - self.observations_used),
        }

    def check_action(self) -> None:
        r = self.remaining()
        if r["actions_remaining"] <= 0 or r["seconds_remaining"] <= 0:
            raise BudgetExhausted("budget_exhausted")

    def check_observe(self) -> None:
        r = self.remaining()
        if r["observations_remaining"] <= 0 or r["seconds_remaining"] <= 0:
            raise BudgetExhausted("budget_exhausted")


class Session:
    def __init__(self, session_id: str, spec: AppSpec, runtime: Runtime,
                 session_dir: Path, budget: Budget):
        self.id = session_id
        self.spec = spec
        self.runtime = runtime
        self.dir = Path(session_dir)
        self.budget = budget
        self.recorder = TraceRecorder(self.dir)
        self.store = TopologyStore(session_id, spec.app_id, self.recorder, self.dir)
        self.status = "running"          # running | closed | failed
        self.close_reason: str | None = None
        self.step = 0
        self.cursor = {"x": 0, "y": 0}
        self._last_frame_png: bytes | None = None
        self._last_frame_id: int | None = None
        self._latencies: list[float] = []
        self._last_latency_warning = 0
        self._lock = threading.RLock()
        self.created_at = utc_now()
        self._write_session_meta()

    # ------------------------------------------------------------ metadata

    def _write_session_meta(self) -> None:
        info = self.runtime.info()
        meta = {
            "session_id": self.id,
            "app_id": self.spec.app_id,
            "seed": self.spec.seed,
            "created_at": self.created_at,
            "platform": getattr(self.spec, "platform", info.platform),
            "viewport": {"width": info.width,
                         "height": info.height,
                         "device_scale_factor": info.device_scale_factor,
                         "orientation": info.orientation,
                         "density_dpi": info.density_dpi,
                         "browser_zoom": (config.BROWSER_ZOOM
                                          if info.platform == "web" else None),
                         "browser_chrome": ("none (headless/kiosk)"
                                            if info.platform == "web" else None)},
            "budget": {"max_actions": self.budget.max_actions,
                       "max_duration_s": self.budget.max_duration_s,
                       "max_observations": self.budget.max_observations},
            "status": self.status,
            "close_reason": self.close_reason,
        }
        (self.dir / "session.json").write_text(json.dumps(meta, indent=2),
                                               encoding="utf-8")

    # ------------------------------------------------------------ capture

    def _capture_frame(self, step: int, settle: bool) -> tuple[int, float, float]:
        """Screenshot (+optional settle wait), render cursor, store. Returns
        (frame_id, diff_vs_prev, settle_ms)."""
        t0 = time.monotonic()
        settle_ms = 0.0
        settle_reason = "none"
        raw = self.runtime.screenshot()
        prev = None
        if self._last_frame_png is not None:
            prev = imgdiff.load(self._last_frame_png)
        if settle and config.CAPTURE_AFTER_ACTION:
            # Poll until visually stable. "Stable" has two acceptable forms: the
            # screen stops changing, or it keeps changing only inside a small,
            # steady area — a clock face, stopwatch or spinner. Without the
            # second form a target that animates forever can never settle, so
            # every observation waits out the full timeout and spends the
            # session's duration budget on nothing.
            animated_polls = 0
            settle_reason = "timeout"
            while True:
                img = imgdiff.load(raw)
                time.sleep(config.SETTLE_POLL_MS / 1000)
                nxt = self.runtime.screenshot()
                d = imgdiff.diff_score(img, imgdiff.load(nxt))
                raw = nxt
                settle_ms = (time.monotonic() - t0) * 1000
                if d < config.SETTLE_STABLE_DIFF:
                    settle_reason = "still"
                    break
                if d <= config.SETTLE_ANIMATION_DIFF:
                    animated_polls += 1
                    if animated_polls >= config.SETTLE_ANIMATION_POLLS:
                        settle_reason = "animation"
                        break
                else:
                    # A large change means the transition is still in progress.
                    animated_polls = 0
                if settle_ms > config.SETTLE_TIMEOUT_MS:
                    break
        png = draw_cursor(raw, self.cursor["x"], self.cursor["y"]) \
            if (config.DRAW_CURSOR_IN_FRAMES and
                self.runtime.info().platform == "web") else raw
        fid = self.recorder.save_frame(png)
        d_prev = imgdiff.diff_score(imgdiff.load(png), prev) if prev else 1.0
        self._last_frame_png = png
        self._last_frame_id = fid
        self.recorder.log_observation(m.ObservationRecord(
            step=step, frame_id=fid, timestamp=utc_now(),
            path=f"frames/frame_{fid:06d}.png", cursor=dict(self.cursor),
            diff_score=round(d_prev, 5), settle_ms=round(settle_ms, 1),
            settle_reason=settle_reason))
        return fid, d_prev, settle_ms

    # ------------------------------------------------------------ agent API

    def observe(self) -> dict:
        with self._lock:
            self._require_running()
            self.budget.check_observe()
            self.budget.observations_used += 1
            try:
                fid, _, _ = self._capture_frame(self.step, settle=False)
            except Exception as e:
                # Capturing pixels is a runtime operation like any action: on
                # Android it drives dumpsys and screencap, which are exactly
                # where a transient device failure surfaces. A device that is
                # still healthy costs the Agent one observation, not the whole
                # session; only an unusable one ends it.
                self.budget.observations_used -= 1
                raise self._classify_runtime_failure(e) from e
            png = (self.dir / "frames" / f"frame_{fid:06d}.png").read_bytes()
            info = self.runtime.info()
            response = {
                "frame_id": fid,
                "timestamp": utc_now(),
                "width": info.width,
                "height": info.height,
                "screenshot_png_b64": base64.b64encode(png).decode(),
                "cursor": dict(self.cursor),
                "budget": self.budget.remaining(),
                "brief": self.spec.brief,
                "tabs": self._tabs_info(),
            }
            # Preserve the frozen web Agent-channel whitelist byte-for-byte.
            # Mobile geometry is variable, so Android alone receives these
            # non-semantic display facts.
            if info.platform == "android":
                response.update({
                    "platform": "android",
                    "orientation": info.orientation,
                    "density_dpi": info.density_dpi,
                })
            return response

    def _tabs_info(self) -> dict | None:
        """Tab-strip awareness for receipts: count + active index only.
        Runtimes without a tab concept return None."""
        try:
            return self.runtime.list_tabs()
        except Exception:
            return None

    def execute(self, action: m.Action) -> dict:
        with self._lock:
            self._require_running()
            self.budget.check_action()
            self._validate(action)
            before_fid = self._last_frame_id
            t0 = time.monotonic()
            try:
                if before_fid is None:
                    before_fid, _, _ = self._capture_frame(self.step, settle=False)
                self._dispatch(action)
                dur = (time.monotonic() - t0) * 1000
                self.budget.actions_used += 1
                self.step += 1
                self.store.set_step(self.step)
                after_fid, _, _ = self._capture_frame(self.step, settle=True)
            except ValueError:
                # invalid action surfacing from dispatch (e.g. tab index out of
                # range at runtime): a 400 for the agent, NOT a session failure
                raise
            except Exception as e:  # runtime failure
                raise self._classify_runtime_failure(e) from e
            self.recorder.log_action(m.ActionRecord(
                step=self.step, timestamp=utc_now(), action=action,
                accepted=True, error=None, duration_ms=round(dur, 1),
                before_frame=before_fid, after_frame=after_fid))
            self._watch_latency(dur)
            return {"accepted": True, "frame_id": after_fid, "step": self.step,
                    "tabs": self._tabs_info()}

    def _watch_latency(self, duration_ms: float) -> None:
        """Note when the environment starts degrading, before it fails outright.

        An emulator running out of host resources does not stop cleanly: action
        latency drifts upward for dozens of steps and only then does an adb call
        fail. One lost exploration went 1.9s -> 4.9s mean with a 16s spike before
        `screencap` finally died, and nothing recorded that — the crash report
        alone made a slow collapse look like a sudden fault.

        This is trace-only telemetry: the Agent's receipt is unchanged, so no
        information channel is widened and no session decision depends on it.
        """
        self._latencies.append(duration_ms)
        window = 10
        if len(self._latencies) < window * 2:
            return
        baseline = sum(self._latencies[:window]) / window
        recent = sum(self._latencies[-window:]) / window
        if baseline <= 0:
            return
        ratio = recent / baseline
        # Throttle on measurements, not on `self.step`: capture-only paths also
        # feed this, and a step-based guard silently never fires for them.
        measured = len(self._latencies)
        if ratio >= 3.0 and measured - self._last_latency_warning >= window:
            self._last_latency_warning = measured
            self.recorder.log_event("environment_degrading", {
                "step": self.step,
                "baseline_ms": round(baseline, 1),
                "recent_ms": round(recent, 1),
                "ratio": round(ratio, 2),
            })

    def _classify_runtime_failure(self, exc: Exception) -> Exception:
        """Decide whether one runtime failure ends the session.

        The environment sometimes drops a single operation while staying
        perfectly usable. Ending an exploration at step 85 because of one such
        blip throws away real work and records an environment artefact as if it
        were a limit of the Agent. So: ask the runtime whether it is still
        healthy, and only give up when it is not.

        Either way the Agent receives a neutral sentence — never an adb command
        line, CDP method or host path.
        """
        recoverable = bool(getattr(exc, "recoverable", False))
        if not recoverable:
            # Not every runtime tags its errors; ask the runtime directly.
            try:
                recoverable = bool(self.runtime.health())
            except Exception:
                recoverable = False
        detail = getattr(exc, "detail", None) or repr(exc)
        if recoverable:
            self.recorder.log_event("environment_blip", {
                "step": self.step, "detail": str(detail)[:500],
                "recovered": True})
            return TransientEnvironmentError(_agent_safe_message(exc))
        self._fail(exc, f"runtime error: {detail}")
        return SessionClosed(_agent_safe_message(exc))

    def _validate(self, a: m.Action) -> None:
        info = self.runtime.info()
        W, H = info.width, info.height
        mobile_actions = {
            m.ActionType.TAP, m.ActionType.LONG_PRESS, m.ActionType.SWIPE,
            m.ActionType.TYPE_TEXT, m.ActionType.PRESS_BACK,
            m.ActionType.PRESS_ENTER, m.ActionType.WAIT,
            m.ActionType.RESTART_APP,
        }
        is_android = info.platform == "android"
        if is_android and a.type not in mobile_actions:
            raise ValueError("invalid_action: unavailable on Android")
        if not is_android and a.type in {
                m.ActionType.TAP, m.ActionType.LONG_PRESS, m.ActionType.SWIPE,
                m.ActionType.PRESS_BACK, m.ActionType.PRESS_ENTER,
                m.ActionType.RESTART_APP}:
            raise ValueError("invalid_action: unavailable on web")
        def pt(x, y):
            if not (0 <= x < W and 0 <= y < H):
                raise ValueError("invalid_coordinates")
        coord_actions = {m.ActionType.CLICK, m.ActionType.DOUBLE_CLICK,
                         m.ActionType.MOVE_POINTER, m.ActionType.MOUSE_DOWN,
                         m.ActionType.MOUSE_UP, m.ActionType.TAP,
                         m.ActionType.LONG_PRESS}
        if a.type in coord_actions:
            pt(a.x, a.y)
        elif a.type in (m.ActionType.DRAG, m.ActionType.SWIPE):
            pt(a.x1, a.y1)
            pt(a.x2, a.y2)
        elif a.type == m.ActionType.TYPE_TEXT and not a.text:
            raise ValueError("invalid_action: text required")
        elif a.type in (m.ActionType.KEY_PRESS, m.ActionType.KEY_DOWN,
                        m.ActionType.KEY_UP) and not a.key:
            raise ValueError("invalid_action: key required")
        if (self.spec.kind == "live" and a.key in _LIVE_BLOCKED_KEYS
                and a.type in (m.ActionType.KEY_PRESS, m.ActionType.KEY_DOWN,
                               m.ActionType.KEY_UP)):
            raise ValueError(f"key_not_allowed_on_live_target: {a.key}")
        elif a.type == m.ActionType.SCROLL and (a.dx is None or a.dy is None):
            raise ValueError("invalid_action: dx/dy required")
        elif a.type == m.ActionType.WAIT and (a.ms is None or a.ms < 0):
            raise ValueError("invalid_action: ms required")
        elif a.type == m.ActionType.SWITCH_TAB and \
                (a.tab_index is None or a.tab_index < 0):
            raise ValueError("invalid_action: tab_index >= 0 required")

    def _dispatch(self, a: m.Action) -> None:
        rt = self.runtime
        if a.type == m.ActionType.CLICK:
            rt.click(a.x, a.y)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.TAP:
            rt.tap(a.x, a.y)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.LONG_PRESS:
            rt.long_press(a.x, a.y, a.duration_ms or 700)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.SWIPE:
            rt.swipe(a.x1, a.y1, a.x2, a.y2, a.duration_ms or 400)
            self.cursor = {"x": a.x2, "y": a.y2}
        elif a.type == m.ActionType.PRESS_BACK:
            rt.key("Back", "press")
        elif a.type == m.ActionType.PRESS_ENTER:
            rt.key("Enter", "press")
        elif a.type == m.ActionType.RESTART_APP:
            rt.restart_app()
        elif a.type == m.ActionType.DOUBLE_CLICK:
            rt.click(a.x, a.y, count=2)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.MOVE_POINTER:
            rt.mouse_move(a.x, a.y)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.MOUSE_DOWN:
            rt.mouse_down(a.x, a.y)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.MOUSE_UP:
            rt.mouse_up(a.x, a.y)
            self.cursor = {"x": a.x, "y": a.y}
        elif a.type == m.ActionType.DRAG:
            dur = (a.duration_ms or 500) / 1000
            steps = max(2, int(dur / 0.02))
            rt.mouse_move(a.x1, a.y1)
            rt.mouse_down(a.x1, a.y1)
            for i in range(1, steps + 1):
                xi = int(a.x1 + (a.x2 - a.x1) * i / steps)
                yi = int(a.y1 + (a.y2 - a.y1) * i / steps)
                rt.mouse_move(xi, yi)
                time.sleep(dur / steps)
            rt.mouse_up(a.x2, a.y2)
            self.cursor = {"x": a.x2, "y": a.y2}
        elif a.type == m.ActionType.TYPE_TEXT:
            rt.type_text(a.text)
        elif a.type == m.ActionType.KEY_PRESS:
            rt.key(a.key, "press")
        elif a.type == m.ActionType.KEY_DOWN:
            rt.key(a.key, "down")
        elif a.type == m.ActionType.KEY_UP:
            rt.key(a.key, "up")
        elif a.type == m.ActionType.SCROLL:
            rt.scroll(a.dx, a.dy)
        elif a.type == m.ActionType.WAIT:
            time.sleep(min(a.ms, 10_000) / 1000)
        elif a.type == m.ActionType.SWITCH_TAB:
            rt.switch_tab(a.tab_index)
        elif a.type == m.ActionType.CLOSE_TAB:
            rt.close_tab()
        else:
            raise ValueError(f"invalid_action: unsupported {a.type}")

    # ------------------------------------------------------------ lifecycle

    def reset(self) -> None:
        """Full environment reset to canonical S0: stop everything, reseed app
        data, cold-restart app + browser.

        Live targets (spec.kind == "live") have NO S0: reset is only a cold
        browser restart back to the entry URL on the same login profile;
        server-side state is out of scope (docs/security_model.md)."""
        import shutil
        with self._lock:
            self._require_running()
            if getattr(self.spec, "platform", "web") == "android":
                self.runtime.reset()
                self.recorder.log_event("reset_android")
            elif self.spec.kind == "live":
                self.runtime.reset()
                self.recorder.log_event("reset_live_entry")
            else:
                self.runtime.stop()
                data_dir = self.dir / "appdata"
                shutil.rmtree(data_dir, ignore_errors=True)
                data_dir.mkdir(parents=True, exist_ok=True)
                self.runtime.start()
                self.recorder.log_event("reset")
            self.cursor = {"x": 0, "y": 0}
            self._capture_frame(self.step, settle=True)

    def finalize(self) -> dict:
        with self._lock:
            # Refuse handover when the topology copies another agent's
            # completed deliverables verbatim (cross-session contamination).
            output_root = (config.APP_OUTPUT_DIR if
                           getattr(self.spec, "platform", "web") == "android"
                           else config.PROJECT_ROOT / "website_output")
            crosscheck.assert_clean(
                self.store.graph().model_dump_json(indent=2),
                self.dir.parent,
                output_root,
                self.store.session_id)
            summary = self.store.finalize()
            # If topology finalization fails, leave a normal running budget so
            # the agent can correct its discoveries and retry.
            self.budget.freeze()
            self.status = "closed"
            self.close_reason = "agent_finalized"
            self._write_session_meta()
            self._write_summary()
            try:
                self.runtime.stop()
            finally:
                self.recorder.close()
            return summary

    def close(self, reason: str = "operator_closed") -> None:
        with self._lock:
            if self.status == "closed":
                return
            self.budget.freeze()
            self.status = "closed"
            self.close_reason = reason
            self._write_session_meta()
            self._write_summary()
            try:
                self.runtime.stop()
            finally:
                self.recorder.close()

    def _write_summary(self) -> None:
        """Write the compact session summary; raw frames remain the audit log."""
        try:
            g = self.store.graph()
            (self.dir / "session_summary.json").write_text(json.dumps({
                "session_id": self.id, "app_id": self.spec.app_id,
                "status": self.status, "close_reason": self.close_reason,
                "actions_used": self.budget.actions_used,
                "observations_used": self.budget.observations_used,
                "steps": self.step, "frames": self.recorder.frame_count,
                "coverage": g.coverage_summary.model_dump(),
                "elapsed_s": int(self.budget.elapsed()),
            }, indent=2, ensure_ascii=False), encoding="utf-8")
        except Exception:
            pass

    def mark_failed(self, exc: Exception) -> None:
        with self._lock:
            self._fail(exc, str(exc))

    def _fail(self, exc: Exception, reason: str) -> None:
        """Terminal runtime failure. Caller must hold the session lock.

        A failed session never accepts work again, so everything it holds is
        handed back here: the browser or emulator process, the live-profile
        singleton, the Android target lease and the emulator port reservation.
        Leaving those attached to a dead session is what made a single
        transient device hiccup block every later run against the same target.
        """
        self.budget.freeze()
        self.status = "failed"
        self.close_reason = reason
        self._write_crash(exc)
        self._write_session_meta()
        try:
            self.runtime.stop()
        except Exception:
            pass

    def _write_crash(self, exc: Exception) -> None:
        (self.dir / "crash_report.json").write_text(json.dumps({
            "error": repr(exc), "at": utc_now(), "step": self.step,
            "last_frame": self._last_frame_id,
        }, indent=2), encoding="utf-8")

    def _require_running(self) -> None:
        if self.status == "closed":
            raise SessionClosed("session_closed")
        if self.status == "failed":
            raise SessionClosed("session_failed")

    # ------------------------------------------------------------ status

    def status_dict(self) -> dict:
        actions = self.recorder.read_actions()
        accepted = [a for a in actions if a.get("accepted")]
        last = accepted[-1]["action"] if accepted else None
        g = self.store.graph()
        elapsed = int(self.budget.elapsed())
        return {
            "session_id": self.id,
            "app_id": self.spec.app_id,
            "platform": getattr(self.spec, "platform", "web"),
            "status": self.status,
            "close_reason": self.close_reason,
            "brief": self.spec.brief,
            "step": self.step,
            "elapsed_s": elapsed,
            "current_frame": self._last_frame_id,
            "cursor": dict(self.cursor),
            "budget": self.budget.remaining(),
            "counts": {
                "states": g.coverage_summary.states,
                "features": g.coverage_summary.features,
                "edges": g.coverage_summary.edges,
                "hypotheses": len(self.store.hypotheses()),
                "unresolved": len(g.unresolved_questions),
            },
            "last_action": last,
        }
