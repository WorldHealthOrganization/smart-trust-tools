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

The owner's decision, 2026-10-22, replacing the one-payload profile recorded earlier the same day: **the libraries verify several payloads.**

- The issuer accepts one or more payloads, one hcert entry each; no payload (NoPayload) and two under one subclaim (DuplicateSubclaim) are refused.
- The verifier returns every valid entry with its subclaim, in wire order (deterministic CBOR puts 5 before -6).
- Key usage is checked per entry. An entry whose certificate type the DSC's EKU does not permit is returned marked `keyUsageAuthorised: false`, with a diagnostic; verification fails at KEY_USAGE only when no entry is authorised (STT-VER-004).
- An entry whose payload fails its model check is left out and reported as a diagnostic; verification fails with that entry's own reason only when none is left. An unknown subclaim is returned as raw CBOR, as before.
- The strict profile is opt-in: `maxPayloads` in the verifier options; more entries than that fail HCERT MULTIPLE_SUBCLAIMS.

With one entry, every result is as before, so every existing vector keeps its verdict.

## What is needed from WHO (upstream text)

The libraries now implement a reading the specification does not yet state. To make it normative rather than a local choice, HCERT Specification 3.2.7 (or the QR Code Generation page) needs:

1. **Verifier treatment of several entries**: that a verifier processes every entry, and returns or rejects each independently.
2. **Key usage with mixed types**: whether a DSC whose EKU permits only some entries' types invalidates the whole certificate or only those entries (implemented here: only those entries). Related: the EKU certificate type for non-DCC payloads is itself undefined (bean OI-03), which decides when an entry counts as "unknown type".
3. **An invalid entry beside valid ones**: whether it invalidates the certificate (implemented here: no, it is dropped and reported).
4. **Limits**: whether a trust domain may cap the number of entries (implemented here as the verifier's optional `maxPayloads`), and whether one entry per subclaim is implied (CBOR map keys are unique, so it is).
5. A worked example of a certificate with two entries, as a test vector.

## Requirements affected

- req:stt-sel-004
- req:stt-ver-004

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-payload-type
- library/who-smart-trust-qr-code-generation/sections/s005-4-step-3-build-the-cwt-claim-set
- library/who-smart-trust-hcert-spec/sections/s013-3-2-7-health-certificate-claim
- library/who-smart-trust-hcert-model

## Done when

- [x] the libraries implement several payloads (STT-SEL-004, STT-VER-004)
- [ ] the upstream source states the verifier behaviour (the five points above)
