#!/usr/bin/env bash
set -e

echo "Starting infrastructure: ZooKeeper + Pact Broker..."
docker compose up -d

echo "Waiting for Pact Broker..."
sleep 10

echo "Generating gRPC pact in rate-printer..."
cd rate-printer
mvn -Dtest=RatePrinterGrpcConsumerPactTest test

echo "Publishing pact to Pact Broker..."
mvn au.com.dius.pact.provider:maven:4.7.1:publish

cd ..

echo "Verifying provider against pact from Pact Broker..."
cd currency-rate-provider
mvn verify \
  "-Djava.net.preferIPv4Stack=true" \
  "-Dpact.verifier.publishResults=true" \
  "-Dpact.provider.version=0.0.1-SNAPSHOT"

cd ..

echo "Done."
echo "Open Pact Broker: http://localhost:9292"