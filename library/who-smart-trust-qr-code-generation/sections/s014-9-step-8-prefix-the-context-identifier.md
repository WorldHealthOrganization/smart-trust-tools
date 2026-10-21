---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s014-9-step-8-prefix-the-context-identifier
section_title: "9. Step 8 — Prefix the context identifier"
file: "input/pagecontent/qr_code_generation.md"
lines: 250-256
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 9. Step 8 — Prefix the context identifier

Prefix the Base45 string with `HC1:`. Verifiers use this to recognise what they
have scanned and which decoding scheme to apply. A future
backwards-incompatible revision will use a new identifier, incrementing through
`[1-9]` and then `[A-Z]`.
