#!/usr/bin/env bash
set -euo pipefail

SEARXNG_CONTAINER_NAME=${SEARXNG_CONTAINER_NAME:-searxng}
SEARXNG_PORT=${SEARXNG_PORT:-8080}
SEARXNG_IMAGE=${SEARXNG_IMAGE:-searxng/searxng:latest}
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
SEARXNG_CONFIG_DIR=${SEARXNG_CONFIG_DIR:-"${SCRIPT_DIR}/searxng"}
SEARXNG_SECRET_FILE="${SEARXNG_CONFIG_DIR}/secret_key"

mkdir -p "${SEARXNG_CONFIG_DIR}"
if [ ! -f "${SEARXNG_CONFIG_DIR}/settings.yml" ]; then
  echo "Missing settings.yml at ${SEARXNG_CONFIG_DIR}/settings.yml"
  exit 1
fi

if [ ! -f "${SEARXNG_SECRET_FILE}" ]; then
  python3 - <<'PY' > "${SEARXNG_SECRET_FILE}"
import secrets
print(secrets.token_hex(32))
PY
  chmod 600 "${SEARXNG_SECRET_FILE}"
fi

SEARXNG_SECRET=$(cat "${SEARXNG_SECRET_FILE}")

if docker ps -a --format '{{.Names}}' | grep -q "^${SEARXNG_CONTAINER_NAME}$"; then
  echo "Container ${SEARXNG_CONTAINER_NAME} already exists."
  if [ "${SEARXNG_RECREATE:-0}" = "1" ]; then
    echo "Recreating ${SEARXNG_CONTAINER_NAME} because SEARXNG_RECREATE=1..."
    docker rm -f "${SEARXNG_CONTAINER_NAME}" >/dev/null
  else
    echo "Run: docker start ${SEARXNG_CONTAINER_NAME}"
    echo "Or recreate to apply updated config: SEARXNG_RECREATE=1 $0"
    exit 0
  fi
fi

docker run -d \
  --name "${SEARXNG_CONTAINER_NAME}" \
  -p "${SEARXNG_PORT}:8080" \
  -e SEARXNG_SECRET="${SEARXNG_SECRET}" \
  -v "${SEARXNG_CONFIG_DIR}:/etc/searxng:ro" \
  --restart unless-stopped \
  "${SEARXNG_IMAGE}"

echo "SearXNG started on http://localhost:${SEARXNG_PORT}"
echo "Export for Java orchestrator: SEARXNG_BASE_URL=http://localhost:${SEARXNG_PORT}"
