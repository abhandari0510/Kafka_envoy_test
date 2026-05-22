#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
mkdir -p logs

: "${KAFKA_BOOTSTRAP_SERVERS:=localhost:9092}"
: "${KAFKA_TOPIC:=envoy-body-events}"
: "${KAFKA_DETECTOR_GROUP_ID:=envoy-body-events-detector-forwarder}"
: "${DETECTOR_ENVOY_URL:=http://[::1]:8091/detect}"

export KAFKA_BOOTSTRAP_SERVERS
export KAFKA_TOPIC
export KAFKA_DETECTOR_GROUP_ID
export DETECTOR_ENVOY_URL

echo "Starting Kafka detector forwarder..."
echo "Kafka bootstrap: $KAFKA_BOOTSTRAP_SERVERS"
echo "Kafka topic: $KAFKA_TOPIC"
echo "Detector Envoy: $DETECTOR_ENVOY_URL"

nohup mvn -pl kafka-detector-forwarder exec:java > logs/kafka-detector-forwarder.shell.log 2>&1 &
echo "Kafka detector forwarder PID: $!"
echo "Log: logs/kafka-detector-forwarder.shell.log"
