---
# smart-trust-tools-oi15
title: 'OI-15: Asynchronous crypto and I/O across the WIT boundary'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:32:28Z
---

WASI 0.2 component calls are synchronous. WebCrypto (the natural crypto backend for Kotlin/JS on Node and browsers) is promise-based, as are remote KMS signers and HTTP fetches. The design avoids async callbacks into the host: signing is two-phase and trust lists are loaded by the host. The Rust component verifies with in-WASM RustCrypto, so verify stays synchronous. Kotlin/JS must either use Node's synchronous `crypto` module (Node only) or expose a Promise-returning verify, which would diverge from the jco surface. Decide before implementation; revisit when WASI 0.3 async is stable.

## Requirements affected

- req:stt-gen-012
- req:stt-iss-004

## Sources

- external-schemas/bytecodealliance-wit
- external-schemas/w3c-webcrypto

## Decision, 2026-10-24

The owner chose OI-37 option C: no Kotlin browser target. So verify stays synchronous on every target, and the jco surface is the only one browsers see:

- Kotlin/JS keeps `nodejs()` only, with Node's synchronous `crypto` and `zlib`.
- Browsers use the Rust component (tools/hcert-rust), which verifies with in-WASM RustCrypto and needs no Promise.
- Signing stays two-phase and trust lists stay host-loaded, so no async callback crosses the WIT boundary.

api/README.md records this under the WIT limits. Revisit if WASI 0.3 async becomes stable and a target needs it; that would be a new bean.

## Done when

- [x] fixed in this repository (decided: verify stays synchronous, no Kotlin browser target)
