---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s010-5-step-4-serialise-as-cbor
section_title: "5. Step 4 — Serialise as CBOR"
file: "input/pagecontent/qr_code_generation.md"
lines: 190-199
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 5. Step 4 — Serialise as CBOR

Encode the claim set as CBOR ([RFC 7049](https://tools.ietf.org/html/rfc7049)).
The JSON shown throughout this page and in the logical models is schema
notation only — **the wire format is CBOR**, and an implementation need never
produce the JSON form at all.

Strings SHOULD be NFC-normalised. Decoders SHOULD be permissive about input that
is not.
