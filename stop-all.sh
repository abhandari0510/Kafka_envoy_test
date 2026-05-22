#!/usr/bin/env bash
set -e

echo "Stopping credit-ui, credit-input, policy-generator, Kafka detector forwarder, and Envoy processes..."
pkill -f "mvn -pl policy-generator spring-boot:run" || true
pkill -f "mvn -pl credit-input spring-boot:run" || true
pkill -f "mvn -pl credit-ui spring-boot:run" || true
pkill -f "mvn -pl kafka-detector-forwarder exec:java" || true
pkill -f "com.example.policygenerator.PolicyGeneratorApplication" || true
pkill -f "com.example.creditinput.CreditInputApplication" || true
pkill -f "com.example.creditui.CreditUiApplication" || true
pkill -f "com.example.forwarder.KafkaDetectorForwarder" || true
pkill -f "envoy-1.37.2-linux-x86_64 -c envoy.yaml" || true
pkill -f "envoy-1.37.2-linux-x86_64 -c envoy-pii-detector.yaml" || true

echo "Stop command issued. Verify with 'ss -ltnp | grep -E \":8080|:8081|:8082|:8088|:8089|:8090|:8091\"' if needed."
