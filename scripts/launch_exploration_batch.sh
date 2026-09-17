#!/usr/bin/env bash
# Launch a mixed exploration batch with staggered starts.
#
# Usage:
#   scripts/launch_exploration_batch.sh [--gap SECONDS] app1 [app2 ...] -- web1 [web2 ...]
#
# Example:
#   scripts/launch_exploration_batch.sh --gap 15 \
#       minesweeper tasks -- trello_web airtable_web
#
# Staggering matters: a dozen tasks at once spawn 48+ MCP processes that
# race the shared Controller's cold start. On 2026-09-17 a 4s gap let that
# race produce 217 empty sessions and killed one task; the default gap is
# now 15s. Sessions land in repo/runs; the batch runs from /storage/dzj/runs.
set -euo pipefail

GAP=15
if [ "${1:-}" = "--gap" ]; then
    GAP="${2:?--gap needs a value}"
    shift 2
fi

RUN_DIR="${RUN_DIR:-/storage/dzj/runs}"
mkdir -p "$RUN_DIR"
cd "$RUN_DIR"
export HTTP_PROXY= HTTPS_PROXY= ALL_PROXY= NO_PROXY='*'

launch() {  # kind(name: app|web) target
    local kind="$1" target="$2" skill log
    if [ "$kind" = app ]; then skill="android-blackbox-explorer"
    else skill="blackbox-explorer"; fi
    log="/tmp/oc2_${kind}_${target}.log"
    setsid script -qec \
        "opencode run --auto --print-logs -m paratera/DeepSeek-V4.1-Flash '/${skill} ${target}'" \
        /dev/null > "$log" 2>&1 &
    echo "  [${kind}] ${target} → pid $!  (log: ${log})"
    sleep "$GAP"
}

APPS=()
WEBS=()
mode=apps
for arg in "$@"; do
    if [ "$arg" = "--" ]; then mode=webs; continue; fi
    if [ "$mode" = apps ]; then APPS+=("$arg"); else WEBS+=("$arg"); fi
done

if [ "${#APPS[@]}" -gt 0 ]; then
    echo "=== app 任务（gap=${GAP}s）==="
    for a in "${APPS[@]}"; do launch app "$a"; done
fi
if [ "${#WEBS[@]}" -gt 0 ]; then
    echo "=== web 任务（gap=${GAP}s）==="
    for w in "${WEBS[@]}"; do launch web "$w"; done
fi
echo
echo "启动完成（平滑窗口 ${GAP}s × 任务数，避免 MCP 冷启动洪峰）。"
echo "监控：tail -f /tmp/oc2_*.log"
