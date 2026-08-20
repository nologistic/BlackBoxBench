#!/usr/bin/env bash
# reference_entrypoint.sh — orchestration for the reference container
# (docs/architecture.md §4.1).
#
# Brings up, in order:
#   1. Xvfb            virtual display :99, 1440x900x24
#   2. reference app   python -m sample_apps.<APP_ID>.app --port 8100
#                      --data-dir <live dir reseeded from seed>   (127.0.0.1)
#   3. internal gateway 127.0.0.1:8200 -> 127.0.0.1:8100, injects
#                      X-BBB-Gateway header (docker/internal_gateway.py)
#   4. Runtime RPC     0.0.0.0:8300 (docker/runtime_rpc_server.py) — this
#                      service owns the kiosk chromium child process, because
#                      /reset must restart it with a fresh --user-data-dir.
#
# Process supervision: any critical process (Xvfb / app / gateway / RPC)
# dying kills the container with a non-zero exit so `restart: unless-stopped`
# brings back a clean environment. Chromium is deliberately NOT in this set —
# it is restartable by design (reset).
#
# The gateway secret comes from BBB_GATEWAY_SECRET; the built-in default is
# for local development only and MUST be overridden for real benchmark runs.
#
# NOTE: statically reviewed only — not executed on the authoring machine
# (no Docker / no Linux X stack available there).
set -euo pipefail

log() { echo "[reference-entrypoint] $*" >&2; }

# ------------------------------------------------------------------ config
APP_ID="${BBB_APP_ID:-ecommerce_demo}"
export DISPLAY="${BBB_DISPLAY:-:99}"
SCREEN="${BBB_SCREEN:-1440x900x24}"
APP_PORT="${BBB_APP_PORT:-8100}"
GATEWAY_PORT="${BBB_GATEWAY_PORT:-8200}"
RPC_PORT="${BBB_RPC_PORT:-8300}"
export BBB_GATEWAY_SECRET="${BBB_GATEWAY_SECRET:-bbb-dev-only-secret}"
export BBB_GATEWAY_HEADER="${BBB_GATEWAY_HEADER:-X-BBB-Gateway}"

# Seed layout: pristine seed ships inside the image under the app package;
# the app runs against a disposable live copy recreated from it (T8).
export BBB_DATA_SEED="${BBB_DATA_SEED:-/app/sample_apps/${APP_ID}/seed}"
export BBB_DATA_LIVE="${BBB_DATA_LIVE:-/data/live}"

export BBB_APP_PORT="$APP_PORT"
export BBB_GATEWAY_PORT="$GATEWAY_PORT"
export BBB_APP_ENTRY="http://${BBB_APP_INTERNAL_HOST:-reference-app.internal}:${GATEWAY_PORT}"

APP_DIR="/app"

# ------------------------------------------------------------------ helpers
reseed() {
  log "reseed: ${BBB_DATA_SEED} -> ${BBB_DATA_LIVE}"
  if [ ! -d "$BBB_DATA_SEED" ]; then
    log "FATAL: seed dir not found: ${BBB_DATA_SEED}"
    exit 1
  fi
  rm -rf "$BBB_DATA_LIVE"
  cp -a "$BBB_DATA_SEED" "$BBB_DATA_LIVE"
}

# wait_http_any <url> [header] — succeed on ANY HTTP response (even 404),
# fail only if nothing answers before the deadline.
wait_http_any() {
  local url="$1"; shift
  local header="${1:-}"
  local i code
  for i in $(seq 1 100); do
    if [ -n "$header" ]; then
      code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 -H "$header" "$url" || true)
    else
      code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$url" || true)
    fi
    if [ "$code" != "000" ] && [ -n "$code" ]; then
      return 0
    fi
    sleep 0.2
  done
  return 1
}

# ------------------------------------------------------------------ supervision
PIDS=()
SHUTDOWN_REQUESTED=0

on_term() { SHUTDOWN_REQUESTED=1; }
trap on_term TERM INT

cleanup() {
  trap - EXIT
  log "stopping ${#PIDS[@]} supervised process(es)"
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT

# ------------------------------------------------------------------ 1. Xvfb
log "starting Xvfb on ${DISPLAY} (${SCREEN})"
Xvfb "$DISPLAY" -screen 0 "$SCREEN" -nolisten tcp &
PIDS+=($!)

DISPLAY_SOCK="/tmp/.X11-unix/X${DISPLAY#*:}"
DISPLAY_SOCK="${DISPLAY_SOCK%%.*}"
for _ in $(seq 1 50); do
  [ -S "$DISPLAY_SOCK" ] && break
  sleep 0.1
done
[ -S "$DISPLAY_SOCK" ] || { log "FATAL: Xvfb did not create $DISPLAY_SOCK"; exit 1; }

# ------------------------------------------------------------------ 2. app
reseed
log "starting sample app ${APP_ID} on 127.0.0.1:${APP_PORT} (data ${BBB_DATA_LIVE})"
(cd "$APP_DIR" && exec python3 -m "sample_apps.${APP_ID}.app" \
  --port "$APP_PORT" --data-dir "$BBB_DATA_LIVE") &
PIDS+=($!)

if ! wait_http_any "http://127.0.0.1:${APP_PORT}/" "${BBB_GATEWAY_HEADER}: ${BBB_GATEWAY_SECRET}"; then
  log "FATAL: app did not answer on ${APP_PORT}"
  exit 1
fi

# ------------------------------------------------------------------ 3. gateway
log "starting internal gateway 127.0.0.1:${GATEWAY_PORT} -> 127.0.0.1:${APP_PORT}"
python3 "${APP_DIR}/internal_gateway.py" \
  --listen "127.0.0.1:${GATEWAY_PORT}" --target "127.0.0.1:${APP_PORT}" &
PIDS+=($!)

if ! wait_http_any "http://127.0.0.1:${GATEWAY_PORT}/"; then
  log "FATAL: gateway did not answer on ${GATEWAY_PORT}"
  exit 1
fi

# ------------------------------------------------------------------ 4. runtime RPC (owns chromium)
log "starting runtime RPC on 0.0.0.0:${RPC_PORT}"
python3 "${APP_DIR}/runtime_rpc_server.py" --host 0.0.0.0 --port "$RPC_PORT" &
PIDS+=($!)

if ! wait_http_any "http://127.0.0.1:${RPC_PORT}/health"; then
  log "FATAL: runtime RPC did not answer on ${RPC_PORT}"
  exit 1
fi

log "reference environment up (app=${APP_ID}, display=${DISPLAY}, rpc=${RPC_PORT})"

# ------------------------------------------------------------------ guard loop
# Return non-zero as soon as ANY supervised process exits (unless we were
# asked to stop). `wait -n` wakes on the first child death.
rc=0
wait -n "${PIDS[@]}" || rc=$?
if [ "$SHUTDOWN_REQUESTED" = "1" ]; then
  log "shutdown requested"
  exit 0
fi
log "FATAL: a critical process exited (status ${rc}); container will restart"
exit 1
