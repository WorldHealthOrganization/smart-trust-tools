---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s016-11-verifying-the-same-steps-upwards
section_title: "11. Verifying: the same steps upwards"
file: "input/pagecontent/qr_code_generation.md"
lines: 270-290
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 11. Verifying: the same steps upwards

1. Confirm the `HC1:` prefix and strip it.
2. Base45-decode, then ZLIB-decompress.
3. Parse the COSE_Sign1 and read `alg` and `kid` from the protected header.
4. **Validate the input before trusting it.** Parsing the protected header
   necessarily happens before signature verification, which makes it an attack
   surface; check types and lengths defensively. See
   [section 6.3](hcert_spec.html#63-input-data-validation).
5. Look the `kid` up on the trust list, retrieving every DSC that matches.
6. Rebuild the `Sig_structure` and verify the signature against each candidate
   DSC until one succeeds. **Verify the signature before processing the
   payload.**
7. If the DSC carries an Extended Key Usage extension, check it against the
   certificate type —  OID `1.3.6.1.4.1.1847.2021.1.1` for test,
   `1.3.6.1.4.1.1847.2021.1.2` for vaccinations, `1.3.6.1.4.1.1847.2021.1.3` for
   recovery. A DSC with no such extension may verify any HCERT type.
8. Decode the CBOR payload and check `exp` and `iat` against the current time.
9. Read the `hcert` claim, dispatch on the subclaim to the right logical model,
   and only then apply business rules.
