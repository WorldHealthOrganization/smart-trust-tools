---
# smart-trust-tools-oi24
title: 'OI-24: IHE VHL leaves JWE alg/enc non-normative'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

Document encryption names dir / A256GCM only in an example and defers to "Cryptographic Algorithm Selection", which mandates nothing for encryption (its only baseline is RS256 for signatures). Two conformant implementations need not interoperate. SMART Health Links, which the section cites, fixes dir / A256GCM.

## Resolution

The library implements dir / A256GCM only and refuses anything else (STT-VHL-007).

## Reported

Reported 2026-10-24 to IHE as point 1 of a comment on IHE/ITI.VHL#48, the July 2026 update PR (https://github.com/IHE/ITI.VHL/pull/48#issuecomment-6385626316). That PR does not yet address it.

## Requirements affected

- req:stt-vhl-007

## Sources

- library/ihe-iti-vhl-iti-yy5/sections/s019-2-3-yy5-4-2-5-document-encryption
- library/ihe-iti-vhl-volume-1/sections/s029-1-xx-5-3-cryptographic-algorithm-selection

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
