#!/usr/bin/env bash
# Keep the BlackBoxBench controller alive.
#
# The controller is a single point of failure: when it dies, every session it
# manages goes dark at once (observed 2026-09-22 ~01:07, six tasks lost to one
# exit). This supervisor restarts it with a doubling backoff (2s -> 60s cap),
# so a transient crash costs seconds while a controller that cannot start at
# all is retried gently. Each run appends stdout/stderr to a log file.
#
# Usage:
#   scripts/controller_supervisor.sh [port]     # foreground; run under nohup
#
# Stopping it: kill THIS script (it is the parent of the controller). Killing
# the controller alone will just make the supervisor restart it.
#
# Env:
#   BBB_PYTHON          python to run (default: the project env's python3.12)
#   BBB_CONTROLLER_LOG  log path (default /tmp/bbb_controller.log)
set -u

PORT="${1:-7800}"
REPO="$(cd "$(dirname "$0")/.." && pwd)"
PY="${BBB_PYTHON:-/home/dzj/ENTER/envs/app/bin/python3.12}"
LOG="${BBB_CONTROLLER_LOG:-/tmp/bbb_controller.log}"

cd "$REPO"
backoff=2
while true; do
  echo "[supervisor] $(date '+%F %T') starting controller on :$PORT" >>"$LOG"
  "$PY" -m benchmark.server --port "$PORT" >>"$LOG" 2>&1
  code=$?
  echo "[supervisor] $(date '+%F %T') controller exited (code $code);" \
       "restarting in ${backoff}s" >>"$LOG"
  sleep "$backoff"
  if [ "$code" -eq 0 ]; then
    backoff=2
  else
    backoff=$(( backoff < 60 ? backoff * 2 : 60 ))
  fi
done
