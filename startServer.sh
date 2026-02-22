#!/bin/bash
echo "Starting Server..."
mvn clean compile
mvn -Pserver exec:java@run-server