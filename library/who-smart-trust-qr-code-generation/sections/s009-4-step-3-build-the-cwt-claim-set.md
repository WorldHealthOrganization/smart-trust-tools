---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s009-4-step-3-build-the-cwt-claim-set
section_title: "4. Step 3 — Build the CWT claim set"
file: "input/pagecontent/qr_code_generation.md"
lines: 172-189
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 4. Step 3 — Build the CWT claim set

Place the payload under its subclaim, inside the `hcert` claim, inside a
[CWT claim set](StructureDefinition-CWTPayload.html):

| Claim key | Name | Notes |
|-----------|------|-------|
| `1` | `iss` | Issuer. Optionally the ISO 3166-1 alpha-2 code of the issuing country |
| `6` | `iat` | Issued At, integer NumericDate. MUST NOT predate the DSC validity period |
| `4` | `exp` | Expiration Time, integer NumericDate. MUST NOT exceed the DSC validity period |
| `-260` | `hcert` | Map whose single key is the subclaim chosen in Step 1 |

`exp` is the only revocation mechanism this specification offers: certificates
cannot be reliably revoked once issued, so the validity period must be chosen to
bound the damage from a compromised key, balanced against the holder's ability
to come back for a reissue. See
[section 6.1](hcert_spec.html#61-hcert-signature-validity-time).
