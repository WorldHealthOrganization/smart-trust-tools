---
# smart-trust-tools-oi08
title: 'OI-08: Logical model link in hcert_spec uses a different case'
status: completed
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

hcert_spec.md links StructureDefinition-hcert.html. The resource id is HCert, so the page is StructureDefinition-HCert.html, which is the link QR Code Generation 2.1 uses. On case-sensitive hosting the lowercase link breaks.

## Resolution

Fixed upstream: on smart-trust main at 422b043 (2026-10-23), both links in hcert_spec.md read StructureDefinition-HCert.html. Recorded as not needing a report in WorldHealthOrganization/smart-trust#138.

## Requirements affected

- req:stt-sel-001

## Sources

- library/who-smart-trust-hcert-spec/sections/s007-3-2-1-cwt-structure-overview
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [x] the upstream source is corrected, or the requirement no longer depends on it
