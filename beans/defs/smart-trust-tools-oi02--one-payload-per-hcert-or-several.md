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
updated_at: 2026-10-22T00:00:00Z
---

QR Code Generation 2 and 4 say an HCERT carries exactly one payload and that -260 is a "map whose single key is the subclaim". HCERT Specification 3.2.7 says "Several different types of health certificate MAY exist under the same claim", and the HCert logical model makes every subclaim 0..1, so it does not rule out more than one. The verifier behaviour for several entries is not specified.

Upstream has answered half of it (WHO af75be2, PR 129 merged): QR Code Generation 2 now says "Carrying exactly one is an implementation choice, not a constraint of the container", and the CWT payload logical model makes -260 1..* ("HCERT spec 3.2.7 allows several types of health certificate under this claim"). So an issuer that emits exactly one is conformant, but a verifier that fails on several (STT-SEL-004, MultipleSubclaims) is stricter than the container.

## Resolution

An owner's decision: keep STT-SEL-004 as a deliberate profile (exactly one payload, several refused), or let the verifier accept several, which changes the verification result in api/wit from one payload to a list. Upstream still does not say how a verifier treats several.

## Requirements affected

- req:stt-sel-004

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-payload-type
- library/who-smart-trust-qr-code-generation/sections/s005-4-step-3-build-the-cwt-claim-set
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
