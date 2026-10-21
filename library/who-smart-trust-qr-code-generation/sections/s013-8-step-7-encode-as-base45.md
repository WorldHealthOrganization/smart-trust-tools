---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s013-8-step-7-encode-as-base45
section_title: "8. Step 7 — Encode as Base45"
file: "input/pagecontent/qr_code_generation.md"
lines: 243-249
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 8. Step 7 — Encode as Base45

Encode the compressed bytes with
[Base45](https://datatracker.ietf.org/doc/draft-faltstrom-base45). Base45 exists
for one reason: its 45-character alphabet is exactly the QR Alphanumeric
character set, which stores data far more densely than byte mode.
