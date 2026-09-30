#!/usr/bin/env python3
"""Pick K distinct exploration keyframes (high visual change) for the coverage
judge. Uses observations.jsonl diff_score to favour frames that introduced new
content, so the judge sees distinct screens rather than near-duplicates.

Usage: python3 scripts/_pick_keyframes.py runs/<sid> [K]  -> prints frame paths
"""
import json, os, sys

rd = sys.argv[1]
K = int(sys.argv[2]) if len(sys.argv) > 2 else 20
obs = []
p = os.path.join(rd, "observations.jsonl")
if os.path.exists(p):
    for line in open(p, encoding="utf-8"):
        line = line.strip()
        if line:
            try: obs.append(json.loads(line))
            except Exception: pass

# always keep the first frame (home); then top-K by diff_score, then sort by time
scored = [(o.get("diff_score") or 0, o.get("frame_id"), o.get("path")) for o in obs if o.get("path")]
first = [s for s in scored if s[1] in (0, 1)]
rest = sorted([s for s in scored if s[1] not in (0, 1)], key=lambda x: -x[0])[:K]
chosen = {s[1]: s[2] for s in first + rest}
for fid in sorted(chosen):
    path = chosen[fid]
    ap = path if os.path.isabs(path) else os.path.join(rd, os.path.basename(path)) \
        if not os.path.exists(path) else path
    # normalise to runs/<sid>/frames/frame_XXXXXX.png
    cand = os.path.join(rd, "frames", "frame_%06d.png" % fid)
    print(cand if os.path.exists(cand) else ap)
