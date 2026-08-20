"""Deterministic replay: reset to S0, re-execute a session's action sequence,
compare per-step frames against the recorded ones.

Visual replay lives in the dashboard (frame player) and the rendered mp4.
"""
from __future__ import annotations

import json
import time
from pathlib import Path

from .. import config
from ..orchestrator.session import Session
from ..recorder import diff as imgdiff
from ..recorder.cursor import draw_cursor
from ..topology import models as m


def _settled_screenshot(sess: Session) -> bytes:
    """Capture after waiting for visual stability — same policy as live capture,
    so replayed frames are comparable to recorded ones even under load."""
    raw = sess.runtime.screenshot()
    t0 = time.monotonic()
    while True:
        img = imgdiff.load(raw)
        time.sleep(config.SETTLE_POLL_MS / 1000)
        nxt = sess.runtime.screenshot()
        d = imgdiff.diff_score(img, imgdiff.load(nxt))
        raw = nxt
        if d < config.SETTLE_STABLE_DIFF:
            return raw
        if (time.monotonic() - t0) * 1000 > config.SETTLE_TIMEOUT_MS:
            return raw


def deterministic_replay(sess: Session, throttle_ms: int | None = None) -> dict:
    """Re-run the accepted action sequence on the live (reset) environment.

    Returns a report; per-step artifacts land in runs/<sid>/replay_<n>/.
    """
    throttle = (throttle_ms if throttle_ms is not None
                else config.REPLAY_MAX_STEP_INTERVAL_MS)
    actions = [r for r in sess.recorder.read_actions() if r.get("accepted")]
    replay_dirs = sorted(sess.dir.glob("replay_*"))
    out_dir = sess.dir / f"replay_{len(replay_dirs) + 1}"
    out_dir.mkdir(parents=True, exist_ok=True)

    sess.reset()  # full reseed + cold browser; guarantees S0 before replay
    time.sleep(0.3)

    results = []
    matches = 0
    for rec in actions:
        a = m.Action(**rec["action"])
        t0 = time.monotonic()
        if a.type == m.ActionType.WAIT:
            pass  # waits are throttled away in replay
        else:
            sess._dispatch(a)
        elapsed = (time.monotonic() - t0) * 1000
        if elapsed < throttle:
            time.sleep((throttle - elapsed) / 1000)
        png = _settled_screenshot(sess)
        # recorded frames carry the rendered cursor; match that here so the
        # comparison is apples-to-apples
        png = draw_cursor(png, sess.cursor["x"], sess.cursor["y"])
        step_no = rec["step"]
        (out_dir / f"replay_step_{step_no:06d}.png").write_bytes(png)
        expected_id = rec.get("after_frame")
        ok, dist, mse_v = False, -1, 1.0
        if expected_id is not None and sess.recorder.frame_exists(expected_id):
            exp = imgdiff.load(sess.recorder.frame_path(expected_id).read_bytes())
            got = imgdiff.load(png)
            dist = imgdiff.hamming(imgdiff.phash(exp), imgdiff.phash(got))
            mse_v = imgdiff.mse(exp, got)
            ok = dist <= config.REPLAY_FRAME_HAMMING_THRESHOLD
        matches += int(ok)
        results.append({"step": step_no, "match": ok,
                        "phash_distance": dist, "mse": round(mse_v, 6)})

    report = {
        "session_id": sess.id,
        "steps_replayed": len(results),
        "steps_matched": matches,
        "match_ratio": round(matches / len(results), 4) if results else 0.0,
        "final_match": results[-1]["match"] if results else False,
        "thresholds": {"phash_hamming": config.REPLAY_FRAME_HAMMING_THRESHOLD,
                       "final_mse": config.REPLAY_FINAL_MSE_THRESHOLD},
        "results": results,
    }
    (out_dir / "report.json").write_text(json.dumps(report, indent=2),
                                         encoding="utf-8")
    sess.recorder.log_event("deterministic_replay",
                            {"steps": len(results), "matched": matches})
    return report
