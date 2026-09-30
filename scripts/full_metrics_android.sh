#!/bin/bash
# Full-metric pipeline for one Android run (B1 coverage + F1 tokens + A-F
# compute_metrics + optional C1 visual similarity when CLIP is available).
# Usage:
#   bash scripts/full_metrics_android.sh <sid> <app> <handoff> <agent> [eval_report]
#     agent  = kimi | zcode | dsh   (token extraction source)
#     eval_report = path (optional; auto-picked latest if omitted)
set -u
REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"
PY="$REPO/vendor/python/bin/python3"
SID="$1"; APP="$2"; HANDOFF="$3"; AGENT="${4:-}"; REP="${5:-}"

# eval report: latest for this app if not given
if [ -z "$REP" ]; then
  REP=$(grep -l "\"checklist_id\": *\"$APP\"" app_output/evaluations/*/evaluation_report.json 2>/dev/null | xargs ls -1t 2>/dev/null | head -1)
fi
RD="runs/$SID"
OUT="$RD/full_metrics"; mkdir -p "$OUT"

echo "[1/4] exploration coverage judge (B1)"
if [ -f "$RD/coverage_judge.json" ]; then
  echo "  cached: $RD/coverage_judge.json"
else
  bash scripts/coverage_judge_android.sh "$SID" "$APP" 18 > "$OUT/coverage.log" 2>&1 \
    && echo "  -> $RD/coverage_judge.json" || echo "  coverage FAILED (see $OUT/coverage.log)"
fi

echo "[2/4] token extraction (F1: source=$AGENT)"
TOK_EXP=0
if [ -n "$AGENT" ] && [ "$AGENT" != "dsh" ]; then
  "$PY" scripts/extract_tokens.py --agent "$AGENT" --sid "$SID" --app "$APP" > "$OUT/tokens.json" 2>&1 \
    && TOK_EXP=$("$PY" -c "import json;print(json.load(open('$OUT/tokens.json'))['explore_reproduce'])" 2>/dev/null) \
    && echo "  explore+reproduce tokens: $TOK_EXP" || echo "  token extraction failed"
else
  echo "  dsh: no local token log (N/A)"
fi

echo "[3/4] artifact metrics (A/B/D/E/F)"
"$PY" scripts/compute_metrics.py --platform android --sid "$SID" --app "$APP" \
  --handoff "$HANDOFF" ${REP:+--eval-report "$REP"} \
  --tokens-explore "$TOK_EXP" --json "$OUT/metrics.json" >/dev/null 2>&1 \
  && echo "  -> $OUT/metrics.json" || echo "  metrics FAILED"
# attach coverage to metrics for the CSV row
if [ -f "$RD/coverage_judge.json" ]; then
  COV=$("$PY" -c "
import json
d=json.load(open('$RD/coverage_judge.json'))
r=sum(1 for x in d if x.get('reached'))
print(f'{r}/{len(d)}')")
  echo "  coverage: $COV"
fi

echo "[4/4] visual similarity (C1, optional)"
if "$PY" -c "import torch, transformers" 2>/dev/null; then
  "$PY" scripts/compute_visual_similarity.py --sid "$SID" --app "$APP" \
    ${REP:+--eval-report "$REP"} --coverage "$RD/coverage_judge.json" \
    --model-dir "$CLIP_MODEL_DIR" --json "$OUT/vissim.json" 2>&1 | tail -1 \
    && echo "  -> $OUT/vissim.json" || echo "  vissim FAILED"
else
  echo "  skipped: CLIP env not ready (torch/transformers missing)"
fi

echo "DONE -> $OUT/"
"$PY" -c "
import json, os
m = json.load(open('$OUT/metrics.json'))
print(json.dumps(m, ensure_ascii=False, indent=2))" 2>/dev/null | head -40
