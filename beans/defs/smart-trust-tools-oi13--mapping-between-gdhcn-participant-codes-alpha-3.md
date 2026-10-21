---
# smart-trust-tools-oi13
title: 'OI-13: Mapping between GDHCN participant codes (alpha-3) and iss (alpha-2)'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

Participant codes are ISO 3166-1 alpha-3, while iss and the X.509 C attribute use alpha-2. The test pair XXA/XX is user-assigned and not a formal ISO mapping. A verifier narrowing trust-list lookups by iss needs an authoritative mapping.

## Requirements affected

- req:stt-cwt-005

## Sources

- library/who-smart-trust-qr-code-generation/sections/s020-12-3-test-issuer
- library/who-smart-trust-hcert-spec/sections/s010-3-2-4-issuer
- references/who-trust-gdhcn-participants-vs

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
