#!/usr/bin/env bash
# Build static WASM site into composeApp/build/web-pages-site
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

SITE="${SITE_DIR:-$ROOT/composeApp/build/web-pages-site}"
KOTLIN="$ROOT/composeApp/build/compileSync/wasmJs/main/developmentExecutable/kotlin"
SKIKO="$ROOT/composeApp/build/compose/skiko-for-web-runtime"
NPM="$ROOT/build/js/node_modules"
RES="$ROOT/composeApp/src/wasmJsMain/resources"
WEBPACK="$ROOT/composeApp/build/kotlin-webpack/wasmJs/developmentExecutable"

rm -rf "$SITE"
mkdir -p "$SITE"

build_esm_bundle() {
  ./gradlew :composeApp:compileDevelopmentExecutableKotlinWasmJs \
    :composeApp:wasmJsDevelopmentExecutableCompileSync \
    :composeApp:wasmJsPackageJson \
    :kotlinNpmInstall \
    --no-daemon
  JS_JODA="$NPM/@js-joda/core/dist/js-joda.esm.js"
  if [ ! -f "$JS_JODA" ]; then
    echo "js-joda not found at $JS_JODA after kotlinNpmInstall"
    exit 1
  fi
  mkdir -p "$SITE/kotlin" "$SITE/node_modules/@js-joda/core/dist"
  cp "$KOTLIN/"* "$SITE/kotlin/"
  cp "$SKIKO/skiko.mjs" "$SKIKO/skiko.wasm" "$SITE/kotlin/"
  cp "$NPM/@js-joda/core/dist/js-joda.esm.js" "$SITE/node_modules/@js-joda/core/dist/"
  python3 - "$RES/index.html" "$SITE/index.html" <<'PY'
import sys
from pathlib import Path
src = Path(sys.argv[1]).read_text()
old = '''<script src="kkalscan.js"></script>
<script>
    // Webpack WASM bundle is async; dev server injects this, static serve needs it explicitly.
    globalThis.composeApp.then((module) => module.main());
</script>'''
new = '''<script type="importmap">
{
  "imports": {
    "@js-joda/core": "./node_modules/@js-joda/core/dist/js-joda.esm.js"
  }
}
</script>
<script type="module">
import { main } from './kotlin/KkalScan-composeApp-wasm-js.mjs';
main();
</script>'''
Path(sys.argv[2]).write_text(src.replace(old, new))
PY
}

if [ "${CI:-}" = "true" ]; then
  echo "CI build: using ESM bundle (reliable for Maestro/Pages)."
  build_esm_bundle
elif ./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack --no-daemon; then
  cp -R "$WEBPACK"/. "$SITE"/
  cp "$RES/index.html" "$SITE"/
else
  echo "Webpack failed; falling back to ESM bundle..."
  build_esm_bundle
fi

cp "$RES/"*.png "$RES/"*.svg "$RES/"*.webmanifest "$SITE/" 2>/dev/null || true
touch "$SITE/.nojekyll"

if [ "${PAGES_DEPLOY:-0}" = "1" ]; then
  python3 - "$SITE/index.html" <<'PY'
import sys
from pathlib import Path
path = Path(sys.argv[1])
html = path.read_text()
base = '    <base href="/mobile/">\n'
if "<base href=" not in html:
    html = html.replace("<head>\n", f"<head>\n{base}", 1)
    path.write_text(html)
PY
fi

echo "Site ready: $SITE"
