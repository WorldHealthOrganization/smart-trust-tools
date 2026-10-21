---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s008-3-step-2-assemble-the-payload
section_title: "3. Step 2 — Assemble the payload"
file: "input/pagecontent/qr_code_generation.md"
lines: 123-171
source_sha256: 25cc22304a1d036e
granularity: heading
---
#### 3. Step 2 — Assemble the payload

Populate the logical model you selected. The DVC payload uses short element
names because every byte is paid for twice: once in the CBOR encoding and again
in the QR's module count.

For [DVCMin](http://smart.who.int/trust-phw/StructureDefinition-DVCMin.html):

| Element | Card. | Meaning |
|---------|-------|---------|
| `n` | 1..1 | Legal name of the patient, first and last |
| `dob` | 1..1 | Date of birth |
| `s` | 1..1 | Sex |
| `nt` | 1..1 | Nationality, ISO 3166-1 alpha-3 |
| `id` | 0..1 | National identification document number |
| `dt` | 0..1 | National ID document type |
| `gn` | 0..1 | Parent or guardian name |
| `vx` | 1..1 | Vaccine details |
| `v` | 1..1 | Version of the certificate template |

and within `vx`
([DVCMinVaccineDetails](http://smart.who.int/trust-phw/StructureDefinition-DVCMinVaccineDetails.html)):

| Element | Card. | Meaning |
|---------|-------|---------|
| `vp` | 1..1 | Vaccine product identifier |
| `dv` | 1..1 | Date of vaccination |
| `cn` | 0..1 | Name of the supervising clinician |
| `is` | 0..1 | Identifier of the issuing organization |
| `bo` | 1..1 | Batch or lot number |

Three constraints are easy to miss:

* `vx` is **1..1**. One DVC documents one immunization event. A visit at which
  two vaccines are administered produces two certificates and two QR codes.
* At least one of `is` or `cn` **must** be present — this is the
  `must-have-issuer-or-clinician-name` invariant.
* `id` is optional precisely because many certificate subjects have no
  identification document. A newborn is the obvious case; `gn` carries the
  parent or guardian instead.

For `vp`, the profile decides the terminology.
[DVCMinPreQual](http://smart.who.int/trust-phw/StructureDefinition-DVCMinPreQual.html)
binds `vp` to the WHO PreQual product identifiers (required binding), so a
conformant PreQual implementation must resolve the administered product in the
PreQual vaccine database and use that identifier. The unconstrained `DVCMin`
leaves the binding open; the worked example below uses ICD-11 codes from the
[DVC - Vaccines value set](http://smart.who.int/trust-phw/ValueSet-DVCVaccines.html).
