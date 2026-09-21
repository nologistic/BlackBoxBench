#!/usr/bin/env bash
# Launch a Kimi Code exploration batch (Android apps, 60s stagger).
#
# Usage:
#   scripts/launch_kimi_batch.sh <app1> [app2 ...]
#
# Example:
#   scripts/launch_kimi_batch.sh minesweeper tasks fossify_calculator loop_habit_tracker
#
# Kimi Code runs non-interactively via `kimi --auto -p '<skill> <target>'`.
# The dead proxy (7890) is cleared first — kimi's OAuth rejects it silently.
set -euo pipefail

GAP=60
KIMI="/home/dzj/.kimi-code/bin/kimi"

RUN_DIR="${RUN_DIR:-/storage/dzj/runs}"
mkdir -p "$RUN_DIR"
cd "$RUN_DIR"

# Dead proxy (7890) breaks kimi OAuth and MCP tool calls to the controller.
unset HTTP_PROXY HTTPS_PROXY ALL_PROXY http_proxy https_proxy all_proxy

launch() {
    local target="$1"
    local log="/tmp/kimi_app_${target}.log"
    # -p (non-interactive prompt) inherently auto-approves all tool calls:
    # there is no interactive user to ask. --auto is TUI-only and cannot
    # be combined with -p.
    setsid script -qec \
        "${KIMI} -p '/android-blackbox-explorer ${target}'" \
        /dev/null > "$log" 2>&1 &
    echo "  [kimi] ${target} → pid $!  (log: ${log})"
    sleep "$GAP"
}

echo "=== Kimi Android 探索批次（gap=${GAP}s）==="
for target in "$@"; do
    launch "$target"
done
echo
echo "启动完成。监控: tail -f /tmp/kimi_app_*.log"
