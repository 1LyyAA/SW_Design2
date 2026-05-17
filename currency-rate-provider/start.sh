#!/bin/bash
set -e

JAR="${JAR:-target/currency-rate-provider-0.0.1-SNAPSHOT.jar}"
JAR_DIR="$(dirname "$JAR")"
CONFIG_ARGS=()

if [ ! -f "$JAR" ]; then
  echo "Jar file not found: $JAR"
  echo "Build the application first: ./build.sh"
  exit 1
fi

if [ -f "$JAR_DIR/application.properties" ]; then
  CONFIG_ARGS+=(--spring.config.additional-location="file:$JAR_DIR/application.properties")
fi

echo "Starting producers..."

java -jar "$JAR" "${CONFIG_ARGS[@]}" --spring.grpc.server.port=9090 --server.port=8081 > producer-9090.log 2>&1 &
echo "Started producer on gRPC 9090, metrics http://localhost:8081/actuator/prometheus, PID=$!"

java -jar "$JAR" "${CONFIG_ARGS[@]}" --spring.grpc.server.port=9091 --server.port=8082 > producer-9091.log 2>&1 &
echo "Started producer on gRPC 9091, metrics http://localhost:8082/actuator/prometheus, PID=$!"

java -jar "$JAR" "${CONFIG_ARGS[@]}" --spring.grpc.server.port=9092 --server.port=8083 > producer-9092.log 2>&1 &
echo "Started producer on gRPC 9092, metrics http://localhost:8083/actuator/prometheus, PID=$!"

wait
