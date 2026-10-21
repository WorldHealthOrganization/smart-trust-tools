---
# smart-trust-tools-oi03
title: 'OI-03: EKU certificate type for non-DCC payloads is undefined'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

Appendix A.2 defines EKU OIDs for test, vaccinations and recovery, and QR Code Generation 11 step 7 says to check the EKU "against the certificate type". There is no mapping from subclaims 3, 4, 5, -6 or -7 to a certificate type. DVC (-6) is presumably "vaccinations". VHL (5) has no obvious type.

## Resolution

Add a subclaim-to-certificate-type table, or use the GDHCN Key Usage value set.

## Requirements affected

- req:stt-ver-004

## Sources

- library/who-smart-trust-qr-code-generation/sections/s016-11-verifying-the-same-steps-upwards
- library/who-smart-trust-hcert-spec/sections/s028-a-2-extended-key-usage-identifiers

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
