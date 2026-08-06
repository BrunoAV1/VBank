#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

if [ -f "$REPO_ROOT/.env" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line=$(printf '%s' "$line" | tr -d '\r')
    case "$line" in ''|'#'*) continue ;; esac
    name=${line%%=*}
    value=${line#*=}
    case "$name" in *[!A-Z0-9_]*|'') echo "Nome inválido em .env: $name" >&2; exit 1 ;; esac
    export "$name=$value"
  done < "$REPO_ROOT/.env"
fi

for name in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD JWT_SECRET; do
  eval "value=\${$name:-}"
  [ -n "$value" ] || { echo "$name não está definida. Preencha .env sem versioná-lo." >&2; exit 1; }
done

[ -d "$REPO_ROOT/frontend/node_modules" ] || (cd "$REPO_ROOT/frontend" && npm ci --no-audit --no-fund)

(cd "$REPO_ROOT/backend" && ./mvnw spring-boot:run) & BACKEND_PID=$!
(cd "$REPO_ROOT/frontend" && npm run dev) & FRONTEND_PID=$!
trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true' INT TERM EXIT
printf '%s\n' 'Frontend: http://localhost:5173' 'Backend:  http://localhost:8080' 'Pressione Ctrl+C para encerrar ambos.'
wait "$BACKEND_PID" "$FRONTEND_PID"
