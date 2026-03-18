#!/bin/bash
set -euo pipefail

echo "Starting Client..."
mvn clean compile
mvn -Pclient exec:java@run-client