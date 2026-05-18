#!/bin/bash
set -e

JAR="${JAR:-target/rate-printer-0.0.1-SNAPSHOT.jar}"
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

echo "Starting rate-printer..."
echo "Spring profile: ${SPRING_PROFILES_ACTIVE:-dev}"
java -jar "$JAR" "${CONFIG_ARGS[@]}"
