#!/bin/bash
# ==========================================================================
# run_web_suite.sh — concurrent web-nograph benchmark runner (Claude Opus 5)
#
# Per target it runs the full pipeline as three Claude conversations:
#   A) explore   (web-nograph skill)  -> finalizes a functional topology
#   B) reproduce (bound to the session) -> builds website_output/<handoff>
#   C) evaluate  (web-review skill)    -> four-tier report vs review_specs
# Targets run up to $CONCURRENCY at a time; each gets its OWN live-controller
# container (the live browser + login profile is a per-target singleton).
#
# PREREQUISITES (must already exist, created once by the operator):
#   - docker images: blackboxbench/live-controller, reproduction-workbench
#   - docker network: bbb-net
#   - per-target login profile: runs/live_targets/<target>/profile
#     (capture via the self_login_ui flow; see docs/使用说明 login section)
#   - the `claude` CLI logged in; run with the ANTHROPIC_* env cleared so it
#     uses the baidu-cc account (see CLEAN_ENV below)
#
# Usage:  bash scripts/run_web_suite.sh  [target1 target2 ...]
#   no args -> uses the built-in DOMESTIC_TARGETS list
# ==========================================================================
set -u
REPO=/mnt/cfs-agent/workspace/xuzhiyao/blackbench/BlackBoxBench-linux-2
PARENT=/mnt/cfs-agent/workspace/xuzhiyao/blackbench
cd "$REPO"

MODEL="${BBB_SUITE_MODEL:-Opus 5[1m]}"
# Per-stage models: exploration + reproduction (generation) vs evaluation.
GEN_MODEL="${BBB_SUITE_GEN_MODEL:-Opus 4.8}"
EVAL_MODEL="${BBB_SUITE_EVAL_MODEL:-gpt-6-astra}"
USERNAME="${BBB_SUITE_USER:-xuzhiyao}"
CONCURRENCY="${BBB_SUITE_CONCURRENCY:-3}"
MAX_ACTIONS="${BBB_SUITE_MAX_ACTIONS:-120}"
MINUTES="${BBB_SUITE_MINUTES:-45}"
IMG=blackboxbench/live-controller:latest
CLIP_IMG=blackboxbench/clip:latest
CLIP_MD=/models/ms/models/AI-ModelScope--clip-vit-large-patch14/snapshots/master
PY3IMG=python:3.12-slim
OUT="$REPO/suite_results"; mkdir -p "$OUT"
CSV="$OUT/results.csv"
RUN_TS="$(date +%Y%m%d_%H%M%S)"

# ~20 network-reachable (domestic) live targets; foreign ones (reddit/quora/
# spotify/dropbox/google*/youtube) need BBB_LIVE_PROXY and are excluded here.
DOMESTIC_TARGETS=(zhihu_web weibo_web taobao_web douban_web dianping_web \
  ctrip_web xiaohongshu_web yuque_web notion_web trello_web todoist_web \
  airtable_web excalidraw_web jspaint_web squoosh_web kleki_web \
  diagrams_net_web desmos_web desmos_geometry_web desmos_3d_web)

TARGETS=("$@"); [ ${#TARGETS[@]} -eq 0 ] && TARGETS=("${DOMESTIC_TARGETS[@]}")

# claude with the harness ANTHROPIC_* env stripped so it uses the CLI account
clean_claude() { env -u ANTHROPIC_MODEL -u ANTHROPIC_API_KEY -u ANTHROPIC_BASE_URL \
  -u ANTHROPIC_AUTH_TOKEN -u ANTHROPIC_CUSTOM_HEADERS -u ANTHROPIC_DEFAULT_HAIKU_MODEL \
  -u ANTHROPIC_DEFAULT_OPUS_MODEL -u ANTHROPIC_DEFAULT_SONNET_MODEL claude "$@"; }

cost_of() { grep -oE '"total_cost_usd":[0-9.]+' "$1" 2>/dev/null | head -1 | cut -d: -f2; }
# total tokens consumed = sum of the token fields in the final result event
tokens_of() { grep '"type":"result"' "$1" 2>/dev/null | tail -1 \
  | grep -oE '"(input_tokens|output_tokens|cache_creation_input_tokens|cache_read_input_tokens)":[0-9]+' \
  | grep -oE '[0-9]+$' | awk '{s+=$1} END{print s+0}'; }
PY3() { docker run --rm -v "$PARENT:$PARENT" -w "$REPO" "$PY3IMG" python3 "$@"; }
gen_mcp() { # $1=path $2=controller_host $3=session(optional) $4=repro(0/1) $5=module
  local sess_env=""; [ -n "$3" ] && sess_env="\"-e\",\"BBB_SESSION=$3\","
  cat > "$1" <<JSON
{ "mcpServers": { "bbb": { "command": "docker", "args": [
  "run","--rm","-i","--network","bbb-net",
  "-e","BBB_CONTROLLER=http://$2:7800","-e","BBB_APP_ID=$TGT",
  "-e","BBB_REPRODUCTION_AUTOSTART=$4","-e","BBB_RUNS_DIR=$REPO/runs",
  "-e","BBB_MAX_ACTIONS=$MAX_ACTIONS","-e","BBB_MINUTES=$MINUTES",
  "-e","BBB_REPRO_REVISIONS=2","-e","BBB_MCP_LOG=/tmp/mcp.log", $sess_env
  "-v","/usr/bin/docker:/usr/bin/docker:ro","-v","/var/run/docker.sock:/var/run/docker.sock",
  "-v","$PARENT:$PARENT","-w","$REPO","--entrypoint","python",
  "$IMG","-m","$5" ] } } }
JSON
}

run_target() { # $1=target $2=port
  local TGT="$1" PORT="$2" C="bbb-lc-$1" LOG="$OUT/$RUN_TS.$1"
  mkdir -p "$LOG"
  if [ ! -d "runs/live_targets/$TGT/profile" ]; then
    echo "$TGT,SKIP_NO_LOGIN,,,,,,," >> "$CSV"; return; fi
  docker rm -f "$C" >/dev/null 2>&1
  docker run -d --name "$C" --network bbb-net -p "127.0.0.1:$PORT:7800" \
    -v "$PARENT:$PARENT" -w "$REPO" -e BBB_RUNS_DIR="$REPO/runs" "$IMG" >/dev/null 2>&1
  for _ in $(seq 1 25); do curl -sf -m4 "http://127.0.0.1:$PORT/api/apps" >/dev/null 2>&1 && break; sleep 3; done

  # ---- Stage A: explore (web-nograph) ----
  gen_mcp "$LOG/mcp_explore.json" "$C" "" 0 agents.web_nograph.mcp_server
  clean_claude -p "Use the web-nograph skill to explore the target $TGT (you are logged in). Explore the main features thoroughly through pixels only, then call finalize and STOP. Do NOT use workspace_* or review_* tools." \
    --username "$USERNAME" --model "$GEN_MODEL" --mcp-config "$LOG/mcp_explore.json" \
    --strict-mcp-config --allowedTools "mcp__bbb" --output-format stream-json --verbose \
    > "$LOG/explore.jsonl" 2>&1
  local SID; SID=$(curl -sf -m8 "http://127.0.0.1:$PORT/api/sessions" 2>/dev/null | tr ',' '\n' | grep -oE 'sess_[0-9_a-z]+' | tail -1)
  # ---- Stage B: reproduce (bound to the finalized session) ----
  gen_mcp "$LOG/mcp_repro.json" "$C" "$SID" 1 agents.web_nograph.mcp_server
  clean_claude -p "Reproduction stage for $TGT. Call finalize to enter reproduction, inspect /exploration frames + /materials, build a working static reproduction of the app's core surfaces into /workspace with workspace_write/workspace_run, run start_reproduction_review -> review_observe -> complete_reproduction_review(decision=\"accept\"), then finish_reproduction. Offline sandbox." \
    --username "$USERNAME" --model "$GEN_MODEL" --mcp-config "$LOG/mcp_repro.json" \
    --strict-mcp-config --allowedTools "mcp__bbb" --output-format stream-json --verbose \
    > "$LOG/repro.jsonl" 2>&1
  local HANDOFF; HANDOFF=$(ls -1t website_output/ 2>/dev/null | grep "${SID#sess_}" | head -1)

  # ---- Stage C: evaluate (web-review) ----
  gen_mcp "$LOG/mcp_eval.json" "$C" "" 0 agents.web_review.mcp_server
  clean_claude -p "Use the web-review skill: start_evaluation(checklist=\"${TGT}.json\", handoff_id=\"$HANDOFF\"), grade EVERY requirement as full/partial/placeholder/broken via record_result with frame evidence, then finish_evaluation. Leave nothing ungraded." \
    --username "$USERNAME" --model "$EVAL_MODEL" --mcp-config "$LOG/mcp_eval.json" \
    --strict-mcp-config --allowedTools "mcp__bbb" --output-format stream-json --verbose \
    > "$LOG/eval.jsonl" 2>&1

  # ---- collect: this target's eval report + all metrics ----
  local REP; REP=$(grep -l "\"checklist_id\": *\"$TGT\"" website_output/evaluations/*/evaluation_report.json 2>/dev/null | xargs ls -1t 2>/dev/null | head -1)
  local te tr tv; te=$(tokens_of "$LOG/explore.jsonl"); tr=$(tokens_of "$LOG/repro.jsonl"); tv=$(tokens_of "$LOG/eval.jsonl")
  # nograph exploration-coverage judge (passive; reuses exploration frames)
  bash scripts/coverage_judge.sh "$SID" "$TGT" 14 > "$LOG/coverage.log" 2>&1
  # artifact metrics (token-based)
  PY3 scripts/compute_metrics.py --sid "$SID" --app "$TGT" --handoff "$HANDOFF" --eval-report "$REP" \
    --tokens-explore "${te:-0}" --tokens-reproduce "${tr:-0}" --tokens-evaluate "${tv:-0}" \
    --json "$LOG/metrics.json" >/dev/null 2>&1
  # coverage-discounted CLIP visual similarity (reuses exploration frames)
  docker run --rm -v "$PARENT:$PARENT" -w "$REPO" -v "$PARENT/clip_models:/models" "$CLIP_IMG" \
    python3 scripts/compute_visual_similarity.py --sid "$SID" --app "$TGT" --eval-report "$REP" \
    --coverage "runs/$SID/coverage_judge.json" --model-dir "$CLIP_MD" --json "$LOG/vissim.json" >/dev/null 2>&1
  # assemble one CSV row from the metric JSONs
  PY3 scripts/_row.py "$LOG/metrics.json" "$LOG/vissim.json" "runs/$SID/coverage_judge.json" "$TGT" "$SID" >> "$CSV"
  docker rm -f "$C" >/dev/null 2>&1
}

# ---- concurrency pool ----
echo "target,session,full,partial,placeholder,broken,wfs,persistence_wfs,coverage,visual_sim,eff_action,invalid,platform_fail,revisions,illusion,tok_explore,tok_reproduce,tok_evaluate,tok_total,tok_per_fidelity" > "$CSV"
port=7810
for t in "${TARGETS[@]}"; do
  while [ "$(jobs -rp | wc -l)" -ge "$CONCURRENCY" ]; do sleep 5; done
  run_target "$t" "$port" &
  port=$((port+1)); sleep 2
done
wait

# ---- render markdown table (key columns; full data in results.csv) ----
{
  echo "# web-nograph benchmark — gen=$GEN_MODEL eval=$EVAL_MODEL ($RUN_TS)"; echo
  echo "| target | full | part | plc | brk | WFS | persist | coverage | visual | eff-act | illus | tokens |"
  echo "|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|"
  tail -n +2 "$CSV" | while IFS=, read -r tg sid f p pl br wfs pw cov vs ea inv pf rev ill te tr tv tt tpf; do
    echo "| $tg | $f | $p | $pl | $br | $wfs | $pw | $cov | $vs | $ea | $ill | $tt |"; done
  echo; awk -F, 'NR>1{s+=$19} END{printf "**Suite total tokens: %d across %d targets**\n", s, NR-1}' "$CSV"
} > "$OUT/summary_$RUN_TS.md"
echo "DONE -> $OUT/summary_$RUN_TS.md"
cat "$OUT/summary_$RUN_TS.md"
