#!/bin/bash
set -e

PATTERN="rate-printer-0.0.1-SNAPSHOT.jar|rate-printer.jar"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-35}"

mapfile -t PIDS < <(pgrep -f "$PATTERN" || true)

if [ "${#PIDS[@]}" -eq 0 ]; then
  echo "No rate-printer processes found"
  exit 0
fi

echo "Sending SIGTERM to rate-printer processes: ${PIDS[*]}"
kill -TERM "${PIDS[@]}"

deadline=$((SECONDS + TIMEOUT_SECONDS))
while [ "$SECONDS" -lt "$deadline" ]; do
  running=()
  for pid in "${PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      running+=("$pid")
    fi
  done

  if [ "${#running[@]}" -eq 0 ]; then
    echo "rate-printer stopped gracefully"
    exit 0
  fi

  sleep 1
done

echo "Timed out waiting for graceful shutdown: ${running[*]}"
echo "Sending SIGKILL"
kill -KILL "${running[@]}" 2>/dev/null || true
