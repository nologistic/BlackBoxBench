#!/usr/bin/env bash
# Stdio-to-TCP relay for MCP clients inside the agent runtime container
# (for images without Python). Requires bash with /dev/tcp support.
#
# Bash cannot half-close a socket, so after stdin reaches EOF the relay
# allows a short grace period for the final in-flight response and then
# exits; the server reaps the conversation when the socket drops.
set -uo pipefail

HOST="${BBB_MCP_HOST:-host.docker.internal}"
PORT="${BBB_MCP_PORT:-8400}"
TOKEN="${BBB_MCP_TOKEN:-}"

exec 3<>"/dev/tcp/${HOST}/${PORT}" || exit 1

if [[ -n "$TOKEN" ]]; then
  printf '{"jsonrpc":"2.0","id":"auth","method":"auth","params":{"token":"%s"}}\n' "$TOKEN" >&3
  if ! head -n 1 <&3 | grep -q '"ok"'; then
    echo "mcp relay: authentication rejected" >&2
    exit 1
  fi
fi

# NOTE: without job control, bash points a background command's stdin at
# /dev/null unless it is redirected explicitly — hence the explicit <&0.
cat <&0 >&3 &
PUMP=$!
cat <&3 &
READER=$!
wait $PUMP || true
exec 3>&- || true
# Grace window for the final in-flight response (and for server-side zombie
# eviction, which can take a few seconds before this conversation is served).
for _ in $(seq 1 150); do
  kill -0 "$READER" 2>/dev/null || break
  sleep 0.1
done
kill "$READER" 2>/dev/null || true
wait "$READER" 2>/dev/null || true
exit 0
