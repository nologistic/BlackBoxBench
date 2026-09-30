#!/bin/bash
# Operator login helper: bring up a headed Chromium on a live target inside the
# container and expose docker/self_login_ui.py (pixel view + input forwarding)
# on :8400 so a human can log in through a browser tab. The resulting Chromium
# profile is what LiveChromiumRuntime later reuses.
set -eu
TARGET_URL="${BBB_TARGET_URL:?set BBB_TARGET_URL}"
PROFILE="${BBB_LOGIN_PROFILE:?set BBB_LOGIN_PROFILE}"

# self_login_ui.py drives the display through scrot + xdotool.
if ! command -v scrot >/dev/null 2>&1 || ! command -v xdotool >/dev/null 2>&1; then
  apt-get update -qq >/dev/null 2>&1
  apt-get install -y -qq --no-install-recommends scrot xdotool >/dev/null 2>&1
fi

mkdir -p /data/tmp "$PROFILE"
rm -f /tmp/.X99-lock 2>/dev/null || true
Xvfb :99 -screen 0 1440x900x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
export DISPLAY=:99
for _ in $(seq 1 25); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 0.4; done

# Headed Chromium filling the 1440x900 display, on the target, persistent profile.
/usr/bin/chromium --no-sandbox --disable-crashpad --mute-audio \
  --user-data-dir="$PROFILE" \
  --window-position=0,0 --window-size=1440,900 --force-device-scale-factor=1 \
  --no-first-run --no-default-browser-check --disable-extensions \
  --disable-component-update --disable-sync --disable-translate \
  --password-store=basic --disable-features=TranslateUI \
  --disable-blink-features=AutomationControlled --lang=zh-CN \
  "$TARGET_URL" >/tmp/chromium.log 2>&1 &
sleep 3

export BBB_OPERATOR_UI_PORT="${BBB_OPERATOR_UI_PORT:-8400}"
export BBB_VIEWPORT_WIDTH=1440 BBB_VIEWPORT_HEIGHT=900
echo "[login-ui] serving operator UI on :$BBB_OPERATOR_UI_PORT for $TARGET_URL"
exec python3 /app/docker/self_login_ui.py
