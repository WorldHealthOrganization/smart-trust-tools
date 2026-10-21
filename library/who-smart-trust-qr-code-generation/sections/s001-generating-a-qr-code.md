---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s001-generating-a-qr-code
section_title: "Generating a QR Code"
file: "input/pagecontent/qr_code_generation.md"
lines: 1-16
source_sha256: 25cc22304a1d036e
granularity: heading
---
### Generating a QR Code

This page is a step-by-step, implementer-facing walkthrough of how a signed
health certificate becomes a QR code on a piece of paper or a phone screen, and
how a verifier turns that QR code back into trustworthy data.

The container, the signature and the transport encoding are the *same* for every
GDHCN trust domain, and are specified normatively in the
[HCERT Specification](hcert_spec.html). What changes from one use case to the
next is only the **payload**: which logical model you put inside the `hcert`
claim, and which subclaim number you put it under. The first step below is
therefore a selection step, and everything after it is mechanical.

Where this page and the [HCERT Specification](hcert_spec.html) appear to differ,
the HCERT Specification is authoritative.
