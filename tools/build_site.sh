#!/usr/bin/env bash
# Build the viewer site into site/ (or $1): Python pages plus the TypeDoc
# reference for the jco-generated TypeScript declarations.
# Needs Python (pyyaml markdown rdflib cbor2 base45) and Node.js.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/site}"
ref="${SITE_GIT_REF:-}"

python3 "$root/tools/build_site.py" --out "$out" ${ref:+--git-ref "$ref"}

# Vendor the two browser libraries from npm so the site has no CDN dependency.
vendor="$out/assets/vendor"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$vendor"
(cd "$tmp" && npm pack --silent cytoscape@3.30.2 mermaid@10.9.1 >/dev/null)
tar -xzf "$tmp"/cytoscape-3.30.2.tgz -C "$tmp" package/dist/cytoscape.min.js
cp "$tmp"/package/dist/cytoscape.min.js "$vendor/"
rm -rf "$tmp/package"
tar -xzf "$tmp"/mermaid-10.9.1.tgz -C "$tmp" package/dist/mermaid.min.js
cp "$tmp"/package/dist/mermaid.min.js "$vendor/"

# The jco declarations use Symbol.dispose, hence ESNext.Disposable; type-check
# them strictly before documenting.
npx --yes -p typescript@5.6.3 tsc -p "$root/tools/site/tsconfig.typedoc.json"
rm -rf "$out/api/typedoc"
npx --yes typedoc@0.26.11 --options "$root/tools/site/typedoc.json" --out "$out/api/typedoc" --logLevel Warn
echo "site ready: $out"
