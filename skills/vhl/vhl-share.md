---
name: vhl-share
description: >
  Issue a VHL: a key, documents encrypted as JWE dir / A256GCM, and the payload as a CBOR map at HCERT key 5 (journey J4.0).
satisfies:
  - "req:stt-vhl-001#statement"
  - "req:stt-vhl-002#statement"
  - "req:stt-vhl-003#statement"
  - "req:stt-vhl-004#statement"
  - "req:stt-vhl-007#statement"
  - "req:stt-vhl-010#statement"
tested-by:
  - tools/hcert-kotlin/hcert-core/src/jvmTest/kotlin/who/smart/trust/core/VhlShareTest.kt
  - tools/hcert-rust/tests/vhl_share.rs
---

# Issue a VHL

Issue a VHL: a key, documents encrypted as JWE dir / A256GCM, and the payload as a CBOR map at HCERT key 5 (journey J4.0).

## Library operations

In the WIT contract (`api/wit/`):

- `vhl.vhl-payload`
- `payloads.raw-payload`
- `vhl.to-vhlink`
- `vhl.new-key`
- `vhl.decrypt`
- `vhl.build-manifest-request`

## Requirements

This skill satisfies 6 requirement(s), listed in `satisfies` above; each is a `req:` node in `skills/requirements/`.
