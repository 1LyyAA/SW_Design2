#!/bin/bash
set -e

echo "Building rate-printer..."
mvn clean package
echo "Build artifact: target/rate-printer-0.0.1-SNAPSHOT.jar"
