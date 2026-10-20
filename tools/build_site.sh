#!/usr/bin/env bash
# Build the viewer site into site/ (or $1): Python pages plus the TypeDoc
# reference for the jco-generated TypeScript declarations.
# Needs Python (pyyaml markdown rdflib cbor2 base45) and Node.js.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/site}"
ref="${SITE_GIT_REF:-}"

python3 "$root/tools/build_site.py" --out "$out" ${ref:+--git-ref "$ref"}

# The browser libraries, TypeScript and TypeDoc come from the pinned
# tools/node lockfile, so the site has no CDN dependency and nothing is
# resolved at build time.
. "$root/tools/node_tools.sh"
vendor="$out/assets/vendor"
mkdir -p "$vendor"
cp "$NODE_TOOLS/cytoscape/dist/cytoscape.min.js" "$NODE_TOOLS/mermaid/dist/mermaid.min.js" "$vendor/"

# The jco declarations use Symbol.dispose, hence ESNext.Disposable; type-check
# them strictly before documenting.
tsc -p "$root/tools/site/tsconfig.typedoc.json"
rm -rf "$out/api/typedoc"
typedoc --options "$root/tools/site/typedoc.json" --out "$out/api/typedoc" --logLevel Warn
echo "site ready: $out"
