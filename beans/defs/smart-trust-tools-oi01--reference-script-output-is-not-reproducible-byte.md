---
# smart-trust-tools-oi01
title: 'OI-01: Reference script output is not reproducible byte for byte'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

QR Code Generation 12 and 13 say the script "regenerates the example above byte for byte". It does not. The DSC is signed by the SCA, and the COSE_Sign1 by the DSC, using randomized ECDSA, so every run produces a different DSC, a different kid and different HC1 strings. Running the script at PR head 50bcb25 twice produced kids ZfzK2Csfpsk= and 2sMkNOHFctA=, against WHM+BYqqZiE= in the PR, and different HC1 strings on every run. The committed artefacts are internally consistent: both committed HC1 strings verify against the committed DSC and the kid matches. The sizes in 12.5 are not stable either: the deflated size came out as 283 bytes instead of 282, and the HC1 length as 429 characters instead of 427.

## Resolution

Either use RFC 6979 deterministic ECDSA for both signatures, or reword the claim to "regenerates an equivalent example" and treat the committed JSON and PEM as the normative test vector. The library requirements assume the committed artefacts are the vector (STT-TST-001).

## Requirements affected

- req:stt-tst-001
- req:stt-tst-002

## Sources

- library/who-smart-trust-qr-code-generation/sections/s017-12-worked-example-a-newborn-birth-dose-dvc
- library/who-smart-trust-qr-code-generation/sections/s024-13-reference-script
- library/who-smart-trust-dvc-qr-script

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
