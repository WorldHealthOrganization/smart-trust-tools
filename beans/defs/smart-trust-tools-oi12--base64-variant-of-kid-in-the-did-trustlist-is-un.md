---
# smart-trust-tools-oi12
title: 'OI-12: Base64 variant of kid in the DID trustlist is unspecified'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

The DID trustlist uses a "base64 encoded key id" as the verification method fragment and as JWK kid. It does not say whether that is standard or URL-safe base64, or whether padding is kept. Standard and URL-safe base64 differ on '+' and '/', and the PR example kid WHM+BYqqZiE= contains '+'.

## Reported

Reported 2026-10-24 to WHO as point 5 of WorldHealthOrganization/smart-trust#138 (https://github.com/WorldHealthOrganization/smart-trust/issues/138).

## Requirements affected

- req:stt-kid-003
- req:stt-tru-002

## Sources

- library/who-smart-trust-did-gdhcn/sections/s004-example-did-documents
- library/who-smart-trust-hcert-spec/sections/s027-a-1-the-key-identifier-kid-s

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
