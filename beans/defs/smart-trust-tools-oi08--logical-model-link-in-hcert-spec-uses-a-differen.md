---
# smart-trust-tools-oi08
title: 'OI-08: Logical model link in hcert_spec uses a different case'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

hcert_spec.md links StructureDefinition-hcert.html. The resource id is HCert, so the page is StructureDefinition-HCert.html, which is the link QR Code Generation 2.1 uses. On case-sensitive hosting the lowercase link breaks.

## Requirements affected

- req:stt-sel-001

## Sources

- library/who-smart-trust-hcert-spec/sections/s007-3-2-1-cwt-structure-overview
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
