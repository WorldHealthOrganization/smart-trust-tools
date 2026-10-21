---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s022-12-5-size-at-each-stage
section_title: "12.5 Size at each stage"
file: "input/pagecontent/qr_code_generation.md"
lines: 382-396
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 12.5 Size at each stage

| Stage | Size |
|-------|------|
| CWT claim set, CBOR | 210 bytes |
| COSE_Sign1, signed | 295 bytes |
| ZLIB/Deflate compressed | 282 bytes |
| Base45 + `HC1:` prefix | 427 characters |
| QR symbol | Version 16, error correction `Q`, Alphanumeric mode |

The signature and its headers account for 85 of the 295 bytes — on payloads this
small, the cryptography costs more than the clinical data. This is why the
minimal models use two-character element names, and why a payload that grows
much beyond this belongs behind a VHL instead.
