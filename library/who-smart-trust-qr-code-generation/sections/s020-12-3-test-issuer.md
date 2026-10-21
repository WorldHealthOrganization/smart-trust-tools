---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s020-12-3-test-issuer
section_title: "12.3 Test issuer"
file: "input/pagecontent/qr_code_generation.md"
lines: 331-342
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 12.3 Test issuer

The example is issued by the GDHCN **test participant `XXA`**, not by any Member
State. GDHCN participant codes are ISO 3166-1 alpha-3, while the CWT `iss` claim
and the X.509 country attribute take the alpha-2 form, here the user-assigned
code `XX`.

The signing keys are throw-away test keys generated deterministically by the
script. They appear on no trust list, and nothing signed with them verifies
anywhere. In a real deployment `iss` would be the issuing country and the DSC
would be one published by that country and aggregated by the GDHCN Secretariat.
