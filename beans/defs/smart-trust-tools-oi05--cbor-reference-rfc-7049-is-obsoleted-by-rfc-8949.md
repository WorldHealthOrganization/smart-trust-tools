---
# smart-trust-tools-oi05
title: 'OI-05: CBOR reference RFC 7049 is obsoleted by RFC 8949'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

Neither page says whether issuers should use deterministic encoding, or which rules. The reference script uses length-first canonical ordering (RFC 7049 section 3.9). For the integer and short text keys used here that gives the same order as RFC 8949 core deterministic encoding, but that is luck, not a rule.

## Requirements affected

- req:stt-cbr-001
- req:stt-tst-002

## Sources

- library/who-smart-trust-qr-code-generation/sections/s010-5-step-4-serialise-as-cbor
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- external-schemas/ietf-rfc7049
- external-schemas/ietf-rfc8949

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
