#!/bin/bash
set -e

VERSION="${1:-0.0.1-SNAPSHOT}"
BUILD_JAR="target/currency-rate-provider-0.0.1-SNAPSHOT.jar"
RELEASE_DIR="release/$VERSION"
RELEASE_JAR="$RELEASE_DIR/currency-rate-provider.jar"

if [ ! -f "$BUILD_JAR" ]; then
  echo "Build artifact not found: $BUILD_JAR"
  echo "Build the application first: ./build.sh"
  exit 1
fi

mkdir -p "$RELEASE_DIR"
cp "$BUILD_JAR" "$RELEASE_JAR"
cp "src/main/resources/application.properties" "$RELEASE_DIR/application.properties"

echo "Release prepared: $RELEASE_DIR"
echo "Run it with: JAR=$RELEASE_JAR ./start.sh"
