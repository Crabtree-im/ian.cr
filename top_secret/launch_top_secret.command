#!/bin/zsh
set -euo pipefail

cd "$(dirname "$0")"

PORT=8080
URL="http://localhost:${PORT}"

echo "Launching TOP SECRET simulation on ${URL}"
echo "Press Control+C in this terminal to stop."

python3 -m http.server "${PORT}" >/tmp/top_secret_server.log 2>&1 &
SERVER_PID=$!

cleanup() {
  if kill -0 "${SERVER_PID}" 2>/dev/null; then
    kill "${SERVER_PID}" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

sleep 1
open "${URL}"

wait "${SERVER_PID}"
