---
# smart-trust-tools-oi20
title: 'OI-20: CDDL validator deviations (cddl-rs / pycddl)'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

The only Python CDDL validator, pycddl (wrapping the Rust cddl crate), deviates from RFC 8610 in four ways found here: .cbor inside arrays is applied to the wrong items; .regexp is not implicitly anchored; escaped regex classes such as \s are treated as literals; and map closure is not enforced across group-choice alternatives. The CDDL files stay spec-correct, and tools/check-models/check_models.py works around the first three. The schema avoids relying on the fourth. Cross-check with a second validator (e.g. cddlc) before upstreaming.

## Requirements affected

- req:stt-cbr-004

## Sources

- external-schemas/ietf-rfc8610
- external-schemas/ietf-rfc9682

## Done when

- [ ] fixed in this repository
