---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s012-7-step-6-compress
section_title: "7. Step 6 — Compress"
file: "input/pagecontent/qr_code_generation.md"
lines: 236-242
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 7. Step 6 — Compress

Compress the COSE structure with ZLIB
([RFC 1950](https://tools.ietf.org/html/rfc1950)) using Deflate
([RFC 1951](https://tools.ietf.org/html/rfc1951)). Compression comes *after*
signing, so the verifier decompresses before it verifies.
