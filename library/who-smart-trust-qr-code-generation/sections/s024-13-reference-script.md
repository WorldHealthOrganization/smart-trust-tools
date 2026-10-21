---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s024-13-reference-script
section_title: "13. Reference script"
file: "input/pagecontent/qr_code_generation.md"
lines: 427-444
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 13. Reference script

`input/scripts/generate_dvc_qr.py` in this guide's source repository implements
every step on this page and regenerates the example above byte for byte:

```
pip install cbor2 base45 qrcode pillow cryptography
python3 input/scripts/generate_dvc_qr.py --out-dir input/images
```

The script builds the test SCA and DSC, derives the `kid`, assembles both DVC
payloads, signs, compresses, encodes and renders — and then decodes its own
output and verifies the signature, so a failed run is a failed round trip rather
than a silently broken certificate. It is a documentation aid, not a production
issuer: it uses fixed test keys in source, and a real issuer holds its private
keys in an HSM. See
[section 6.2](hcert_spec.html#62-key-management) and the
[Reference Implementations](reference_implementation.html).
