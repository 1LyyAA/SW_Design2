#!/bin/bash
set -e

echo "Building currency-rate-provider..."
mvn clean package
echo "Build artifact: target/currency-rate-provider-0.0.1-SNAPSHOT.jar"
