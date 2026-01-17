#!/usr/bin/env bash
set -euo pipefail

ORCHESTRATOR_JAR=${ORCHESTRATOR_JAR:-"$(cd "$(dirname "$0")/../orchestrator" && pwd)/build/libs/orchestrator.jar"}
JAVA_BIN=${JAVA_BIN:-java}
JAVA_XMS=${JAVA_XMS:-128m}
JAVA_XMX=${JAVA_XMX:-512m}

exec "$JAVA_BIN" \
  -Xms"$JAVA_XMS" \
  -Xmx"$JAVA_XMX" \
  -XX:+UseG1GC \
  -jar "$ORCHESTRATOR_JAR"
