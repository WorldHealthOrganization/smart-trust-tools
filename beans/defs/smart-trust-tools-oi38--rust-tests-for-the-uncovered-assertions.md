---
# smart-trust-tools-oi38
title: 'OI-38: Rust tests for the assertions hcert-rust leaves uncovered'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - testing
created_at: 2026-10-24T14:30:00Z
updated_at: 2026-10-24T16:10:00Z
---

test/systems/hcert-rust.system.json (smart-trust-tools#32) lists 42 required assertions that no test verifies on at least one of the system's targets: 8 on native only, 12 on wasm32 only, 22 on both. Many are tested in substance already, under a test whose name does not start with the requirement id, so the generator cannot bind it. The rest need a test.

For each assertion: tag the existing test (rename it, as the systems README says, `stt_xxx_nnn_...` in Rust, `STT-XXX-nnn ...` in tools/conformance-ts), or write the test. A gap that should stay open goes in the system's `unbound` with its reason. Re-run `python3 tools/test-systems/make_test_systems.py` so the bindings and `uncovered` are regenerated, never edited.

Work in small batches, one requirement group (COS, CWT, GEN, PAY, SEC, SEL, TRU, VER, ...) per PR.

## Done when

- [x] `uncovered` in test/systems/hcert-rust.system.json is empty, each remaining gap stated in `unbound`

## Notes

- 2026-10-24: claimed by Claude, session https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz (smart-trust-tools bean drain). Batch 1, COS and CWT on native: smart-trust-tools#35.
- 2026-10-24: batch 1 merged (smart-trust-tools#35). Batches 2 and 3, the native gaps (KID, PAY, SEL, VER, GEN, SEC and an architecture test), in smart-trust-tools#36: 33 left, 31 of them on wasm32 only, for tools/conformance-ts to name.
- 2026-10-24: batches 2 and 3 merged (smart-trust-tools#36). Batch 4, smart-trust-tools#37: tools/conformance-ts names the wasm32 gaps (19 new checks, run on both packages), and the source-checked and target-specific ones go to `unbound` with reasons. `uncovered` is empty in both systems (hcert-kotlin had 32 on node, now 0). Closed.
