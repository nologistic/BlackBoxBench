#!/bin/bash
# Passive post-hoc EXPLORATION COVERAGE judge (does NOT touch exploration).
# Feeds distinct exploration keyframes + the checklist to gpt-6-astra and asks,
# per requirement, whether the agent reached/exercised it during exploration,
# citing an evidence frame. Output: runs/<sid>/coverage_judge.json
#   [ {"id":"f01","reached":true,"evidence_frame":"frame_000012.png","note":"..."} ]
set -u
REPO=/mnt/cfs-agent/workspace/xuzhiyao/blackbench/BlackBoxBench-linux-2
cd "$REPO"
SID="$1"; APP="$2"; K="${3:-18}"
RD="runs/$SID"
# host has no python3 -> run the python helpers in a container
PY3() { docker run --rm -v "$REPO:$REPO" -w "$REPO" python:3.12-slim python3 "$@"; }
mapfile -t KF < <(PY3 scripts/_pick_keyframes.py "$RD" "$K" 2>/dev/null)
[ ${#KF[@]} -eq 0 ] && { echo "no keyframes"; exit 1; }

# checklist requirements as compact text
CL=$(PY3 scripts/_checklist_text.py "$APP")
FRAMES=$(printf '%s\n' "${KF[@]}")
PROMPT="You are a strict COVERAGE JUDGE for a black-box web exploration of $APP.
Below are distinct keyframes captured DURING the agent's exploration (read them all with the Read tool):
$FRAMES

For EACH checklist requirement below, decide from the keyframes whether the exploration actually REACHED / EXERCISED that feature's surface (was it visibly opened/used in some frame?). Be strict: only 'reached' if a frame clearly shows that surface. Cite the single best evidence frame filename (or null).
Requirements:
$CL

Output ONLY a JSON array, one object per requirement, no prose:
[{\"id\":\"f01\",\"reached\":true,\"evidence_frame\":\"frame_000012.png\",\"note\":\"...\"}]"

env -u ANTHROPIC_MODEL -u ANTHROPIC_API_KEY -u ANTHROPIC_BASE_URL -u ANTHROPIC_AUTH_TOKEN \
  -u ANTHROPIC_CUSTOM_HEADERS -u ANTHROPIC_DEFAULT_HAIKU_MODEL -u ANTHROPIC_DEFAULT_OPUS_MODEL \
  -u ANTHROPIC_DEFAULT_SONNET_MODEL claude -p "$PROMPT" \
  --username "${BBB_SUITE_USER:-xuzhiyao}" --model "${BBB_JUDGE_MODEL:-gpt-6-astra}" \
  --allowedTools "Read" --add-dir "$REPO/runs" --output-format text > "$RD/coverage_raw.txt" 2>&1

# extract the JSON array
PY3 scripts/_extract_coverage.py "$RD/coverage_raw.txt" "$RD/coverage_judge.json"
