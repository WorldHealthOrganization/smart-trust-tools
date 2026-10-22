---
# smart-trust-tools-oi33
title: 'OI-33: WHO-ITB track data is not held as vectors'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - testing
created_at: 2026-10-21T00:00:00Z
updated_at: 2026-10-22T17:10:00Z
---

The who-itb-agreement test plan (test/plans/who-itb-agreement.test-plan.json, STT-TST-009) compares each library with the WHO Interoperability Test Bed on its three HCERT tracks. It cannot run in CI yet, for three reasons.

1. WHO-ITB (commit 883291d) carries no licence file, so its test-data QR images and the gdhcn-helper's responses cannot be copied here.
2. The images must be decoded to HC1 text once. The libraries do not read images (STT-QR-005), and no QR reader is pinned here.
3. The track data is signed by GDHCN DEV participants, so verifying it needs the DEV trust list DID documents. Since 2026-10-22 tng-cdn-dev.who.int is an allowed domain here, so they can be fetched and held as vectors once item 1 is resolved. The same fetch unblocked STT-TST-011: XXR's DSC DID document is now in vectors/smart-trust-af75be2/, and both suites verify the merged guide's example end to end.

## Resolution

Asked WHO for a licence covering the code and the test data on 2026-10-21: WorldHealthOrganization/WHO-ITB#30 (https://github.com/WorldHealthOrganization/WHO-ITB/issues/30). Once one exists, hold in vectors/who-itb/ the three HC1 texts, the DEV trust list DID documents they need, and the gdhcn-helper's decode and verify responses. Add a SOURCE.yaml giving the commit, then load them from the Kotlin and Rust suites as tests named STT-TST-009. Until then, a tester runs the ITB beside the library and records a test report against the plan.

## Requirements affected

- req:stt-tst-009

## Sources

- references/who-itb
