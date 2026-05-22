#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
mkdir -p logs

: "${ENVOY_BIN:=$ROOT_DIR/envoy-1.37.2-linux-x86_64}"

start_envoy() {
  local name=$1
  local config=$2
  local port=$3

  if ss -ltn "sport = :${port}" | tail -n +2 | grep -q .; then
    echo "Port ${port} is already in use. Start aborted for ${name}."
    ss -ltnp "sport = :${port}" || true
    exit 1
  fi

  echo "Starting ${name} with ${config}..."
  nohup "$ENVOY_BIN" --disable-hot-restart --local-address-ip-version v6 -c "$config" --base-id "$port" > "logs/${name}.shell.log" 2>&1 &
  echo "${name} PID: $!"
}

start_envoy traffic-envoy envoy.yaml 8088
start_envoy detector-envoy envoy-pii-detector.yaml 8091

echo "Traffic Envoy: http://localhost:8088"
echo "Detector Envoy: http://localhost:8091/detect"
