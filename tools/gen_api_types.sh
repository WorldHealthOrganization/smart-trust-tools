#!/usr/bin/env bash
# Validate api/wit and regenerate the TypeScript review view in api/generated/ts.
# Needs Node.js; fetches @bytecodealliance/jco via npx if not installed.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
jco_version="1.39.0"
out="$root/api/generated/ts"

rm -rf "$out"
npx --yes "@bytecodealliance/jco@$jco_version" types "$root/api/wit" \
  --world-name hcert --out-dir "$out" --quiet
echo "regenerated $out"
