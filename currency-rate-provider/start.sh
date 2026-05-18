#!/bin/bash
set -e

JAR="${JAR:-target/currency-rate-provider-0.0.1-SNAPSHOT.jar}"
JAR_DIR="$(dirname "$JAR")"
ENV_FILE="${ENV_FILE:-.env.dev}"
CONFIG_ARGS=()

if [ -f "$ENV_FILE" ]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

if [ ! -f "$JAR" ]; then
  echo "Jar file not found: $JAR"
  echo "Build the application first: ./build.sh"
  exit 1
fi

if [ -f "$JAR_DIR/application.properties" ]; then
  CONFIG_ARGS+=(--spring.config.additional-location="file:$JAR_DIR/")
fi

PROVIDER_INSTANCES="${PROVIDER_INSTANCES:-9090:8081}"

echo "Starting producers..."
echo "Spring profile: ${SPRING_PROFILES_ACTIVE:-dev}"

for instance in $PROVIDER_INSTANCES; do
  grpc_port="${instance%%:*}"
  http_port="${instance##*:}"
  java -jar "$JAR" "${CONFIG_ARGS[@]}" --spring.grpc.server.port="$grpc_port" --server.port="$http_port" > "producer-$grpc_port.log" 2>&1 &
  echo "Started producer on gRPC $grpc_port, metrics http://localhost:$http_port/actuator/prometheus, PID=$!"
done

wait
