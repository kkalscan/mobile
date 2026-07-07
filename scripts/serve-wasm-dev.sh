#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

chmod +x scripts/prepare-wasm-site.sh
./scripts/prepare-wasm-site.sh

PORT="${PORT:-8081}"
lsof -ti ":$PORT" | xargs kill 2>/dev/null || true
echo "Serving http://127.0.0.1:$PORT/?fake=1"
exec python3 -m http.server "$PORT" --bind 127.0.0.1 --directory "$ROOT/composeApp/build/web-pages-site"
