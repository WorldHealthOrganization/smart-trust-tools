# hcert-rust

The Rust HCERT library. It is the second implementation of the requirement
set, after `tools/hcert-kotlin`, and it loads the same shared vectors.

```sh
cargo test --locked --manifest-path tools/hcert-rust/Cargo.toml
cargo build --locked --release --manifest-path tools/hcert-rust/Cargo.toml --target wasm32-unknown-unknown
node tools/hcert-rust/js/smoke.mjs
```

| Module | What it is |
|---|---|
| `codec` | Steps 4, 6, 7 and 8: deterministic CBOR, ZLIB, Base45 and the HC1 context identifier. |
| `cose` | Step 5: COSE_Sign1 parse, Sig_structure, verification and the kid. |
| `payload` | The subclaim registry and DVCMin. |
| `issuer` | Two-phase issuance: `prepare` returns the bytes to sign, the host signs them, and `finish` checks and assembles. |
| `verifier` | QR Code Generation section 11. Every failure names its step and reason. |
| `trust` | The trust store: PEM, DER, GDHCN DID trustlist v2 and trust domains. |
| `testdsc` | TEST ONLY: a self-signed DSC around a key the host generated. |
| `api`, `wasm` | The JSON call surface and the C ABI that `js/hcert.mjs` loads. |

`js/hcert.mjs` loads the WebAssembly module in a browser or in Node and signs
with WebCrypto. Private keys stay non-extractable CryptoKeys and never enter
the module. The module imports nothing.

`js/demo/` holds the live demos for the smart-trust QR Code Generation page:
one widget per pipeline step, plus verify, all sharing one certificate.
`bundle-demo.sh <dir>` builds the module reproducibly (paths are remapped,
so the same commit gives the same sha256) and writes the bundle and its
`SOURCE.md` to `<dir>`.

[`DEPENDENCIES.md`](DEPENDENCIES.md) lists every pinned crate and the review
of each build script.
