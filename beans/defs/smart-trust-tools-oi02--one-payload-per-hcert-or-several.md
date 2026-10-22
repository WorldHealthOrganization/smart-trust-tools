---
# smart-trust-tools-oi02
title: 'OI-02: One payload per hcert, or several?'
status: completed
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

The owner's decision, 2026-10-22: **keep the one-payload profile.** STT-SEL-004 stands as a deliberate profile of the HCERT container, not a reading of it:

- the issuer emits an hcert (-260) map with exactly one entry, the choice QR Code Generation 2 makes throughout;
- the verifier refuses a map with more than one entry (MultipleSubclaims, at the HCERT step) and one with none (HcertMissing);
- the verification result in api/wit carries one payload, and does not become a list.

So a certificate that uses the container's freedom to carry several payloads, which HCERT Specification 3.2.7 and the CWT payload model (-260 1..*) allow, does not verify in these libraries. That is a known interoperability limit, not a defect. Revisit it if a trust domain registers a use of several payloads in one certificate, or if upstream specifies how a verifier treats them; the API change would then be a new major version (STT-NFR-001).

## Requirements affected

- req:stt-sel-004

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-payload-type
- library/who-smart-trust-qr-code-generation/sections/s005-4-step-3-build-the-cwt-claim-set
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [x] the upstream source is corrected, or the requirement no longer depends on it (upstream now leaves the number to the implementation; the profile is recorded here)
