---
# smart-trust-tools-oi15
title: 'OI-15: Asynchronous crypto and I/O across the WIT boundary'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

WASI 0.2 component calls are synchronous. WebCrypto (the natural crypto backend for Kotlin/JS on Node and browsers) is promise-based, as are remote KMS signers and HTTP fetches. The design avoids async callbacks into the host: signing is two-phase and trust lists are loaded by the host. The Rust component verifies with in-WASM RustCrypto, so verify stays synchronous. Kotlin/JS must either use Node's synchronous `crypto` module (Node only) or expose a Promise-returning verify, which would diverge from the jco surface. Decide before implementation; revisit when WASI 0.3 async is stable.

## Requirements affected

- req:stt-gen-012
- req:stt-iss-004

## Sources

- external-schemas/bytecodealliance-wit
- external-schemas/w3c-webcrypto

## Done when

- [ ] fixed in this repository
