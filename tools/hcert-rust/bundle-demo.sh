#!/usr/bin/env bash
# Builds the WebAssembly module reproducibly and writes the live-demo bundle
# (hcert.wasm, hcert.js, hcert-demo.js, hcert-demo.css, SOURCE.md) to DIR,
# for a page such as the smart-trust QR Code Generation page to load.
#   tools/hcert-rust/bundle-demo.sh <dir>
# Paths are remapped so the module's bytes do not depend on where the
# repository is checked out: the same commit gives the same sha256.
set -euo pipefail
out="${1:?usage: bundle-demo.sh <dir>}"
here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
cargo_home="${CARGO_HOME:-$HOME/.cargo}"
export RUSTFLAGS="--remap-path-prefix=$here=/hcert --remap-path-prefix=$cargo_home=/cargo --remap-path-prefix=$HOME=/home"
cargo build --locked --release --target wasm32-unknown-unknown --manifest-path "$here/Cargo.toml"
mkdir -p "$out"
cp "$here/target/wasm32-unknown-unknown/release/hcert.wasm" "$out/hcert.wasm"
cp "$here/js/hcert.mjs" "$out/hcert.js"
cp "$here/js/demo/hcert-demo.js" "$here/js/demo/hcert-demo.css" "$out/"
commit="$(git -C "$repo" rev-parse HEAD)"
sha() { sha256sum "$1" | cut -d' ' -f1; }
cat > "$out/SOURCE.md" <<MD
# hcert live demo

Built from [litlfred/smart-trust-tools](https://github.com/litlfred/smart-trust-tools)
at commit \`$commit\` by \`tools/hcert-rust/bundle-demo.sh\`. Do not edit
these files here; change them there and rebuild.

| File | sha256 |
|---|---|
| hcert.wasm | \`$(sha "$out/hcert.wasm")\` |
| hcert.js | \`$(sha "$out/hcert.js")\` |
| hcert-demo.js | \`$(sha "$out/hcert-demo.js")\` |
| hcert-demo.css | \`$(sha "$out/hcert-demo.css")\` |

The WebAssembly module imports nothing. Keys are generated and used by
WebCrypto in the reader's browser, and everything issued is TEST ONLY.
MD
echo "wrote $out (hcert.wasm $(sha "$out/hcert.wasm"))"
