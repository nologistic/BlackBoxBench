#!/bin/bash
# Live-controller entrypoint: bring up a virtual display for the headed
# LiveChromiumRuntime, then run the FastAPI controller with the local (non
# -docker) runtime path so live targets use LiveChromiumRuntime.
set -eu

# BBB_RUNTIME must NOT be "docker" here (that path forces the reference
# container). Force it empty so manager._make_runtime picks LiveChromiumRuntime
# for kind==live specs.
export BBB_RUNTIME=""

# Virtual X display for the headed browser.
rm -f /tmp/.X99-lock 2>/dev/null || true
Xvfb :99 -screen 0 1600x1200x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
export DISPLAY=:99

# Wait for the display to accept connections.
for _ in $(seq 1 25); do
  if xdpyinfo -display :99 >/dev/null 2>&1; then break; fi
  sleep 0.4
done

exec uvicorn benchmark.server:app --host 0.0.0.0 --port 7800
