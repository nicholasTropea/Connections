#!/bin/bash
echo "Starting Client..."
mvn clean compile
mvn -Pclient exec:java@run-client