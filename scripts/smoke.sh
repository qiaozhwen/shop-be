#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_text() {
  local file="$1"
  local pattern="$2"
  grep -Eq -- "$pattern" "$ROOT/$file" || {
    echo "STATIC_CHECK_FAILED: $file missing $pattern" >&2
    exit 1
  }
}

require_text "docker-compose.yml" "postgres:"
require_text "docker-compose.yml" "shop-be:"
require_text "docker-compose.yml" "shop-fe:"
require_text "docker-compose.yml" "healthcheck:"
require_text "Dockerfile" "^HEALTHCHECK "
require_text ".env.example" "^AUTH_JWT_SECRET="
require_text "README.md" "备份"
require_text "README.md" "回滚"

if [[ "${1:-}" == "--static" ]]; then
  echo "STATIC_DELIVERY_OK"
  exit 0
fi

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1}"
curl --fail --silent --show-error "$BACKEND_URL/health" | grep -q '"status":"UP"'
curl --fail --silent --show-error "$FRONTEND_URL/" >/dev/null
echo "RUNTIME_SMOKE_OK"
