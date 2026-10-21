---
# smart-trust-tools-oi02
title: 'OI-02: One payload per hcert, or several?'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

QR Code Generation 2 and 4 say an HCERT carries exactly one payload and that -260 is a "map whose single key is the subclaim". HCERT Specification 3.2.7 says "Several different types of health certificate MAY exist under the same claim", and the HCert logical model makes every subclaim 0..1, so it does not rule out more than one. The verifier behaviour for several entries is not specified.

## Resolution

State in hcert_spec 3.2.7 whether more than one subclaim per hcert is allowed and how a verifier treats it.

## Requirements affected

- req:stt-sel-004

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-logical-model-for-your-use-c
- library/who-smart-trust-qr-code-generation/sections/s009-4-step-3-build-the-cwt-claim-set
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
