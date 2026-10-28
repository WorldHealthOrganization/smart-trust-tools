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
updated_at: 2026-10-24T14:54:54Z
---

Neither page says whether issuers should use deterministic encoding, or which rules. The reference script uses length-first canonical ordering (RFC 7049 section 3.9). For the integer and short text keys used here that gives the same order as RFC 8949 core deterministic encoding, but that is luck, not a rule.

## Reported

Reported 2026-10-24 to WHO as point 3 of WorldHealthOrganization/smart-trust#138 (https://github.com/WorldHealthOrganization/smart-trust/issues/138).

## Requirements affected

- req:stt-cbr-001
- req:stt-tst-002

## Sources

- library/who-smart-trust-qr-code-generation/sections/s006-5-step-4-serialise-as-cbor
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- external-schemas/ietf-rfc7049
- external-schemas/ietf-rfc8949

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
