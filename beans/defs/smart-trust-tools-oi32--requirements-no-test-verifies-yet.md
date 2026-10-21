---
# smart-trust-tools-oi32
title: 'OI-32: Requirements no test verifies yet'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - testing
created_at: 2026-10-21T00:00:00Z
updated_at: 2026-10-21T00:00:00Z
---

78 of the 91 requirements are verified by a test named after them (the tests the skills' tested-by names; the traceability matrix's "Not verified by a test" filter lists the rest). The other 13 fall into three groups.

Need the Rust implementation: STT-GEN-005 (Rust for WebAssembly), STT-GEN-006 and STT-TST-003 (the two implementations behave the same, checked against each other), STT-TST-007 (one TypeScript conformance suite for both Node packages).

Not testable in code, verified by review: STT-GEN-007 (the HCERT Specification is authoritative), STT-GEN-008 (CBOR is the wire format), STT-GEN-013 (types trace to open standards), STT-NFR-001 (versioning and spec traceability), STT-NFR-002 (distribution), STT-QR-005 (camera scanning out of scope). Their success criteria should say so (verification: review or inspection) rather than wait for a test.

Gaps in the Kotlin library: STT-SEL-006 (trust domain codes are checked against the GDHCN trust domain value set, which the trust store does not do), STT-TST-005 (a language-neutral vector format beyond the PR 129 JSON), STT-TST-006 (fuzzing the decoders).

## Resolution

Implement the three gaps in tools/hcert-kotlin, each with a test named after its requirement; set the review-verified requirements' success criteria to review or inspection; leave the Rust group to the Rust implementation.

## Requirements affected

- req:stt-sel-006
- req:stt-tst-005
- req:stt-tst-006
- req:stt-gen-006
- req:stt-tst-003

## Sources

- library/who-smart-trust-qr-code-generation/sections/s017-12-worked-example-a-newborn-birth-dose-dvc
