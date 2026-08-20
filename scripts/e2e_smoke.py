"""End-to-end controller smoke: create session, observe, click, verify trace.

Run: vendor/python/python.exe scripts/e2e_smoke.py
"""
from __future__ import annotations

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from benchmark.orchestrator import apps
from benchmark.orchestrator.manager import SessionManager
from benchmark.orchestrator.apps import AppSpec

# register the mini test app
apps._REGISTRY["miniapp"] = AppSpec(
    app_id="miniapp", description="test", module="tests.fixtures.miniapp")

mgr = SessionManager()
print("creating session...")
sess = mgr.create("miniapp", {"max_actions": 50, "max_duration_s": 300,
                              "max_observations": 100})
print("session:", sess.id)

obs = sess.observe()
frame0 = obs["frame_id"]
print(f"observed frame {frame0}, {obs['width']}x{obs['height']}, "
      f"png b64 len={len(obs['screenshot_png_b64'])}")
assert obs["width"] == 1440 and obs["height"] == 900
assert set(obs.keys()) == {"frame_id", "timestamp", "width", "height",
                           "screenshot_png_b64", "cursor", "budget"}, obs.keys()

# click the Increment button at (200, 130)
from benchmark.topology import models as m
from benchmark.recorder import diff as imgdiff
from PIL import Image
import io

def crop(fid, box):
    img = Image.open(sess.recorder.frame_path(fid)).convert("RGB")
    return img.crop(box)

# sanity: page actually loaded (light background #f5f5f5), not a browser error page
img0 = crop(frame0, (0, 0, 1440, 900))
px = list(img0.resize((1, 1)).getdata())[0]
assert sum(px) / 3 > 150, f"page looks dark/failed to load: mean rgb={px}"
print("page load check OK (mean rgb:", px, ")")

res = sess.execute(m.Action(type="click", x=200, y=130))
print("click accepted:", res)
assert res["accepted"]

obs2 = sess.observe()
# compare only the counter text region (away from cursor overlay)
assert crop(frame0, (100, 195, 500, 240)).tobytes() != \
       crop(obs2["frame_id"], (100, 195, 500, 240)).tobytes(), \
       "counter region should change after Increment click"
print("counter increment visible: True")

# type into echo field
sess.execute(m.Action(type="click", x=220, y=318))
sess.execute(m.Action(type="type_text", text="hello"))
res = sess.execute(m.Action(type="key_press", key="Enter"))
obs3 = sess.observe()
assert crop(obs2["frame_id"], (100, 375, 700, 410)).tobytes() != \
       crop(obs3["frame_id"], (100, 375, 700, 410)).tobytes(), \
       "echo region should change after submit"

# discovery with real evidence
fid = sess.store.add_feature(m.FeatureCreate(
    name="Increment counter",
    description="点击 Increment 按钮后计数加一",
    behavior=m.FeatureBehavior(
        preconditions=["首页可见"], postconditions=["计数增加"],
        trigger=m.TriggerModel(type="click", target_description="Increment 按钮")),
    evidence=[m.Evidence(step=1, before_frame=frame0,
                         action="click(200,130)", after_frame=res["frame_id"])],
    confidence=0.9))
print("feature recorded:", fid)
sid_state = sess.store.add_state(m.StateCreate(
    name="Home", description="首页", visual_evidence=[frame0], confidence=0.99))
print("state recorded:", sid_state)

# leak check: must be rejected
try:
    sess.store.add_feature(m.FeatureCreate(
        name="bad", description="calls /api/cart endpoint",
        evidence=[], confidence=0.5, status=m.ClaimStatus.HYPOTHESIZED))
    print("LEAK CHECK FAILED — should have raised")
    sys.exit(1)
except Exception as e:
    print("leak check OK:", type(e).__name__)

# evidence check: bogus frame rejected
try:
    sess.store.add_state(m.StateCreate(name="bogus", visual_evidence=[99999]))
    print("EVIDENCE CHECK FAILED"); sys.exit(1)
except Exception as e:
    print("evidence check OK:", type(e).__name__)

summary = sess.store.finalize()
print("finalize:", summary)

status = sess.status_dict()
print("status:", {k: status[k] for k in ("step", "counts", "status")})

mgr.close(sess.id)
print("E2E SMOKE PASS")
