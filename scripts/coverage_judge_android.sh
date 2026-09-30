#!/bin/bash
# Passive post-hoc EXPLORATION COVERAGE judge for Android (does NOT touch
# exploration). Feeds distinct exploration keyframes + the checklist to
# gpt-6-astra via codex and asks, per requirement, whether the agent
# reached/exercised it during exploration, citing an evidence frame.
# Output: runs/<sid>/coverage_judge.json
#   [ {"id":"f01","reached":true,"evidence_frame":"frame_000012.png","note":"..."} ]
# Usage: bash scripts/coverage_judge_android.sh <sid> <app> [K]
set -u
REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"
SID="$1"; APP="$2"; K="${3:-18}"
RD="runs/$SID"
PY="$REPO/vendor/python/bin/python3"

mapfile -t KF < <("$PY" scripts/_pick_keyframes.py "$RD" "$K" 2>/dev/null)
[ ${#KF[@]} -eq 0 ] && { echo "no keyframes"; exit 1; }

CL=$("$PY" scripts/_checklist_text.py "$APP")
FRAMES=$(printf '  %s\n' "${KF[@]}")
PROMPT="You are a strict COVERAGE JUDGE for a black-box Android exploration of $APP.
Below are distinct keyframes captured DURING the agent's exploration (an Android
app on an emulator, 1080x2400). Read them ALL with the Read tool first:
$FRAMES

For EACH checklist requirement below, decide from the keyframes whether the
exploration actually REACHED / EXERCISED that feature's surface (was it visibly
opened/used in some frame?). Be strict: only 'reached' if a frame clearly shows
that surface. Cite the single best evidence frame filename (or null).
Requirements:
$CL

Output ONLY a JSON array, one object per requirement, no prose:
[{\"id\":\"f01\",\"reached\":true,\"evidence_frame\":\"frame_000012.png\",\"note\":\"...\"}]"

timeout 900 codex exec -m gpt-6-astra -c model_reasoning_effort=xhigh \
  --skip-git-repo-check -s danger-full-access "$PROMPT" \
  > "$RD/coverage_raw.txt" 2>&1

"$PY" scripts/_extract_coverage.py "$RD/coverage_raw.txt" "$RD/coverage_judge.json"
