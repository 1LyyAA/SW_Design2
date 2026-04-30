#!/bin/bash

JAR="target/currency-rate-provider-0.0.1-SNAPSHOT.jar"

echo "Building project..."
mvn clean package 

echo "Starting producers..."

java -jar "$JAR" --spring.grpc.server.port=9090 > producer-9090.log 2>&1 &
echo "Started producer on 9090, PID=$!"

java -jar "$JAR" --spring.grpc.server.port=9091 > producer-9091.log 2>&1 &
echo "Started producer on 9091, PID=$!"

java -jar "$JAR" --spring.grpc.server.port=9092 > producer-9092.log 2>&1 &
echo "Started producer on 9092, PID=$!"

wait