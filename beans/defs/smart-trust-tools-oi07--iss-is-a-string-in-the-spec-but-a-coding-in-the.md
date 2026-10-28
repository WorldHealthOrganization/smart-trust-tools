---
# smart-trust-tools-oi07
title: 'OI-07: iss is a string in the spec but a Coding in the logical model'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

The CWTPayload logical model types claim 1 as Coding. HCERT 3.2.4 and RFC 8392 type it as a string.

## Reported

Reported 2026-10-24 to WHO as point 4 of WorldHealthOrganization/smart-trust#138 (https://github.com/WorldHealthOrganization/smart-trust/issues/138).

## Requirements affected

- req:stt-cwt-005

## Sources

- library/who-smart-trust-hcert-spec/sections/s010-3-2-4-issuer
- library/who-smart-trust-cwt-payload-model

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
