#!/bin/bash

JAR="target/currency-rate-provider-0.0.1-SNAPSHOT.jar"

echo "Building project..."
mvn clean package 

echo "Starting producers..."

java -jar "$JAR" --spring.grpc.server.port=9090 --server.port=8081 > producer-9090.log 2>&1 &
echo "Started producer on gRPC 9090, metrics http://localhost:8081/actuator/prometheus, PID=$!"

java -jar "$JAR" --spring.grpc.server.port=9091 --server.port=8082 > producer-9091.log 2>&1 &
echo "Started producer on gRPC 9091, metrics http://localhost:8082/actuator/prometheus, PID=$!"

java -jar "$JAR" --spring.grpc.server.port=9092 --server.port=8083 > producer-9092.log 2>&1 &
echo "Started producer on gRPC 9092, metrics http://localhost:8083/actuator/prometheus, PID=$!"

wait
