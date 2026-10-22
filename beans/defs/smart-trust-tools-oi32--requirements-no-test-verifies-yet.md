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
updated_at: 2026-10-22T00:00:00Z
---

85 of the 93 requirements are verified by a test named after them (the tests the skills' tested-by names; the traceability matrix's "Not verified by a test" filter lists the rest). Two of the other 8 are new and have their own work: STT-TST-009 (agreement with the WHO-ITB, bean OI-33) and STT-TST-010 (the coverage floor, enforced by :hcert-core:jvmTestCoverageCheck rather than a named test). The remaining 6 are verified by review; the Kotlin gaps and the Rust group are closed. Every requirement now carries success criteria; the test plans in test/plans/ use them as assertion ids.

The Rust group is closed. STT-TST-007: tools/conformance-ts runs one type-checked TypeScript suite (45 checks) against the Kotlin/JS package and the jco component in CI. STT-TST-003 and STT-GEN-006: cross.ts has each library verify the other's ES256 and PS256 certificates (DVC and VHL), built from byte-identical Sig_structures and COSE_Sign1 bytes, and both suites reach the same step and reason on every negative vector. STT-TST-003 had a clause asking the smart-trust reference script (generate_dvc_qr.py) to verify the libraries' certificates; the owner dropped it on 2026-10-22, since PR 129 (217373d) removed the script and running it would need an unpinned `cryptography`. Its certificates verify in both libraries through the PR 129 vector (STT-TST-001).

Not testable in code, verified by review: STT-GEN-007 (the HCERT Specification is authoritative), STT-GEN-008 (CBOR is the wire format), STT-GEN-013 (types trace to open standards), STT-NFR-001 (versioning and spec traceability), STT-NFR-002 (distribution), STT-QR-005 (camera scanning out of scope). Their success criteria should say so (verification: review or inspection) rather than wait for a test.

Closed gaps in the Kotlin library: STT-SEL-006 (the trust store loads the GDHCN Trust Domain CodeSystem, vectors/gdhcn-trust-domains, and refuses a DID document naming another domain), STT-TST-005 (vectors/negative/negative-vectors.json, made by the negative-vectors Tool and loaded by the JVM suite and the Node smoke test), STT-TST-006 (FuzzTest, a seeded mutation fuzzer over the six decoder entry points; cargo-fuzz when Rust comes).

## Resolution

The three Kotlin gaps are implemented, each with a test named after its requirement. The review-verified requirements' success criteria now say review or inspection. The Rust group is closed by tools/conformance-ts.

## Requirements affected

- req:stt-sel-006
- req:stt-tst-005
- req:stt-tst-006
- req:stt-tst-003
- req:stt-gen-006
- req:stt-tst-007
- req:stt-gen-006
- req:stt-tst-003

## Sources

- library/who-smart-trust-qr-code-generation/sections/s017-12-worked-example-a-newborn-birth-dose-dvc
