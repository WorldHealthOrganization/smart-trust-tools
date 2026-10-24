---
# smart-trust-tools-oi37
title: 'OI-37: hcert-core has no browser target'
status: completed
type: feature
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-23T16:20:00Z
updated_at: 2026-10-24T14:32:28Z
---

hcert-core's Kotlin/JS target is `nodejs()` only: `Platform.js.kt` imports `node:crypto` and `node:zlib`, and ArchitectureTest allows only those modules plus `node:buffer`. So the Kotlin library does not run in a browser, and there is no wasmJs target.

litlfred/smart-trust-demo needs one for the web target of its app (docs/proposals/app-platform.md, constraint 4 and step 3 of the recommendation). Until then the demo's web journeys use the Rust/wasm library (tools/hcert-rust), which already runs in a browser with WebCrypto keys.

The browser's platform APIs are asynchronous: WebCrypto `subtle.verify` and `DecompressionStream` return Promises. hcert-core's verify is synchronous, which is the question OI-15 already raises for the WIT boundary. The options:

- A. A `browser()` JS target whose verify returns a Promise. The API surface then differs between Node/JVM and the browser.
- B. Synchronous ECDSA/RSA-PSS verification and inflate in pure Kotlin common code, for the browser only. This means more code of our own in a security-critical path.
- C. No browser target. The browser uses the Rust/wasm library, and the demo's web QR steps stay on it.

Decide together with OI-15.

## Requirements affected

- req:stt-gen-004
- req:stt-gen-012

## Decision, 2026-10-24

The owner chose C: no browser target for hcert-core. The browser uses the Rust/wasm library, and the demo's web QR steps stay on it. hcert-core's Kotlin/JS target stays `nodejs()`, and ArchitectureTest keeps allowing only `node:crypto`, `node:zlib` and `node:buffer`. OI-15 is closed with the same decision.

## Done when

- [x] decided (A, B or C) and, for A or B, a browser target built and tested in CI (C: nothing to build)
