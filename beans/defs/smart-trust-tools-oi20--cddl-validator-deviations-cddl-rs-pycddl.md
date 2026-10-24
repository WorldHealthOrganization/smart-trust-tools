---
# smart-trust-tools-oi20
title: 'OI-20: CDDL validator deviations (cddl-rs / pycddl)'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T16:25:00Z
---

The only Python CDDL validator, pycddl (wrapping the Rust cddl crate), deviates from RFC 8610 in four ways found here: .cbor inside arrays is applied to the wrong items; .regexp is not implicitly anchored; escaped regex classes such as \s are treated as literals; and map closure is not enforced across group-choice alternatives. The CDDL files stay spec-correct, and tools/check-models/check_models.py works around the first three. The schema avoids relying on the fourth. Cross-check with a second validator (e.g. cddlc) before upstreaming.

## Requirements affected

- req:stt-cbr-004

## Sources

- external-schemas/ietf-rfc8610
- external-schemas/ietf-rfc9682

## Done when

- [x] fixed in this repository

## Cross-check, 2026-10-24

Claude (session https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz) ran a second validator: the `cddl` tool by Carsten Bormann (Ruby gem cddl 0.12.14, released 2026-06-11, by one of RFC 8610's authors). It ran on the CDDL files as committed, with none of check_models.py's rewrites. The instances were the ones check_models.py checks, from vectors/smart-trust-pr129 and vectors/smart-trust-af75be2 (bcg, hepb1).

- The COSE_Sign1, protected header and CWT claims of all four certificates are accepted (12 of 12).
- The four mutations check_models.py rejects (no is and no cn, dob 2026-13-01, no n, an is with spaces) are rejected, and the two it accepts (cn only, year-only dob) are accepted, for each certificate (24 of 24). So `.regexp` anchors and `\s` classes behave as RFC 8610 and XSD say, which confirms deviations 2 and 3 are pycddl's.
- `.cbor` inside the COSE_Sign1 array is applied: a COSE_Sign1 whose embedded protected header has a text alg, or whose embedded claims have a text exp, is rejected. pycddl with check_models.py's rewrite accepts both, because the rewrite turns the embedded layers into plain bstr and checks them on their own. This confirms deviation 1, and that the separate per-layer checks are needed.

The two validators agree on every case, so the CDDL is fit to offer upstream (OI-19). The Ruby tool is not added to CI: it would be a new, unpinned toolchain (AGENTS.md), and the cross-check is needed once before upstreaming, not on every run.
