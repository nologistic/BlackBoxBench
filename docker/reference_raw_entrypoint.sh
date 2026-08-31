#!/usr/bin/env bash
# Self-built-tool mode trusted side: app + kiosk browser + device files.
set -euo pipefail

# Xorg requires its Unix-socket directory to be created by root.  The entrypoint
# has only the two capabilities needed to drop to the unprivileged runtime user;
# no app, browser, or device-bridge process runs with them.
if [ "$(id -u)" -eq 0 ] && [ "${BBB_PRIVILEGES_DROPPED:-0}" != "1" ]; then
  mkdir -p /tmp/.X11-unix
  chmod 1777 /tmp/.X11-unix
  exec env BBB_PRIVILEGES_DROPPED=1 HOME=/home/bbb USER=bbb LOGNAME=bbb \
    XDG_CONFIG_HOME=/dev/shm/raw-config XDG_CACHE_HOME=/dev/shm/raw-cache \
    setpriv --reuid=1000 --regid=1000 --clear-groups "$0" "$@"
fi

export DISPLAY="${BBB_DISPLAY:-:99}"
SCREEN="${BBB_SCREEN:-1440x900x24}"
APP_ID="${BBB_APP_ID:-ecommerce_demo}"
TARGET_URL="${BBB_TARGET_URL:-http://reference-app.internal:8200/}"
TARGET_MODE="${BBB_TARGET_MODE:-bundled}"
PROFILE_DIR="${BBB_CHROMIUM_PROFILE_DIR:-/dev/shm/raw-chromium}"
PROFILE_SEED_DIR="${BBB_PROFILE_SEED_DIR:-}"
APP_PORT=8100
GATEWAY_PORT=8200
PROXY_PORT=8300
SECRET="${BBB_GATEWAY_SECRET:-bbb-raw-dev-secret}"
DEVICE_DIR="${BBB_DEVICE_DIR:-/device}"
PIDS=()

cleanup() {
  trap - EXIT
  for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
  rm -f "$DEVICE_DIR/fb0.ppm" "$DEVICE_DIR/event0"
}
trap cleanup EXIT TERM INT

rm -rf /data/live
mkdir -p /data/live /data/tmp "$DEVICE_DIR"
mkdir -p /dev/shm/raw-config /dev/shm/raw-cache /dev/shm/raw-crash
rm -f "$DEVICE_DIR/fb0.ppm" "$DEVICE_DIR/event0"

# Docker-native profiles are prepared by the operator in the same Linux image.
# Exploration receives only a read-only golden seed and copies it into tmpfs;
# the untrusted tool_builder never mounts either source or working profile.
if [ -n "$PROFILE_SEED_DIR" ]; then
  [ "$TARGET_MODE" = "external" ] || exit 1
  [ -d "$PROFILE_SEED_DIR" ] || exit 1
  rm -rf "$PROFILE_DIR"
  mkdir -p "$PROFILE_DIR"
  cp -R "$PROFILE_SEED_DIR"/. "$PROFILE_DIR"/
  chmod -R u+rwX "$PROFILE_DIR"
else
  mkdir -p "$PROFILE_DIR"
fi

Xvfb "$DISPLAY" -screen 0 "$SCREEN" -nolisten tcp & PIDS+=($!)
DISPLAY_NUMBER="${DISPLAY#*:}"
DISPLAY_NUMBER="${DISPLAY_NUMBER%%.*}"
DISPLAY_SOCK="/tmp/.X11-unix/X${DISPLAY_NUMBER}"
for _ in $(seq 1 50); do [ -S "$DISPLAY_SOCK" ] && break; sleep 0.1; done
[ -S "$DISPLAY_SOCK" ] || exit 1

CHROMIUM_ROUTE=()
if [ "$TARGET_MODE" = "bundled" ]; then
  (cd /trusted && exec python3 -m "sample_apps.${APP_ID}.app" \
    --port "$APP_PORT" --data-dir /data/live) & PIDS+=($!)
  code=000
  for _ in $(seq 1 100); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 1 \
      -H "X-BBB-Gateway: $SECRET" "http://127.0.0.1:${APP_PORT}/" || true)
    [ "$code" != "000" ] && break
    sleep 0.2
  done

  BBB_GATEWAY_SECRET="$SECRET" python3 /trusted/internal_gateway.py \
    --listen "127.0.0.1:${GATEWAY_PORT}" --target "127.0.0.1:${APP_PORT}" & PIDS+=($!)
  code=000
  for _ in $(seq 1 100); do
    code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 1 \
      "http://127.0.0.1:${GATEWAY_PORT}/" || true)
    [ "$code" != "000" ] && break
    sleep 0.2
  done
  [ "$code" != "000" ] || exit 1
  CHROMIUM_ROUTE+=(--host-resolver-rules="MAP reference-app.internal 127.0.0.1, MAP * ~NOTFOUND")
elif [ "$TARGET_MODE" = "external" ]; then
  python3 /trusted/public_web_proxy.py --listen "127.0.0.1:${PROXY_PORT}" & PIDS+=($!)
  for _ in $(seq 1 50); do
    (echo >/dev/tcp/127.0.0.1/${PROXY_PORT}) >/dev/null 2>&1 && break
    sleep 0.1
  done
  (echo >/dev/tcp/127.0.0.1/${PROXY_PORT}) >/dev/null 2>&1 || exit 1
  CHROMIUM_ROUTE+=(--proxy-server="http://127.0.0.1:${PROXY_PORT}")
  CHROMIUM_ROUTE+=(--proxy-bypass-list="<-loopback>")
else
  exit 1
fi

chromium --no-sandbox --disable-setuid-sandbox --disable-gpu \
  --disable-dev-shm-usage --kiosk --app="$TARGET_URL" \
  --window-size=1440,900 --force-device-scale-factor=1 \
  "${CHROMIUM_ROUTE[@]}" --disable-quic \
  --force-webrtc-ip-handling-policy=disable_non_proxied_udp \
  --user-data-dir="$PROFILE_DIR" --disable-extensions \
  --disable-background-networking --disable-sync --disable-breakpad \
  --disable-crash-reporter --crash-dumps-dir=/dev/shm/raw-crash --no-first-run \
  --no-default-browser-check --noerrdialogs --mute-audio & PIDS+=($!)

for _ in $(seq 1 100); do
  xdotool search --onlyvisible --class chromium >/dev/null 2>&1 && break
  sleep 0.2
done
xdotool search --onlyvisible --class chromium >/dev/null 2>&1 || exit 1
sleep 0.5

if [ "${BBB_OPERATOR_UI:-0}" = "1" ]; then
  python3 /trusted/self_login_ui.py & PIDS+=($!)
fi

BBB_DEVICE_DIR="$DEVICE_DIR" python3 /trusted/raw_device_bridge.py & PIDS+=($!)
for _ in $(seq 1 100); do
  [ -f "$DEVICE_DIR/fb0.ppm" ] && [ -p "$DEVICE_DIR/event0" ] && break
  sleep 0.1
done
[ -f "$DEVICE_DIR/fb0.ppm" ] && [ -p "$DEVICE_DIR/event0" ] || exit 1

wait -n "${PIDS[@]}"
