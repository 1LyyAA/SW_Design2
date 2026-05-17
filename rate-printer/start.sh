#!/bin/bash
set -e

JAR="${JAR:-target/rate-printer-0.0.1-SNAPSHOT.jar}"
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

echo "Starting rate-printer..."
java -jar "$JAR" "${CONFIG_ARGS[@]}"
