---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s004-2-1-subclaims-defined-in-this-guide
section_title: "2.1 Subclaims defined in this guide"
file: "input/pagecontent/qr_code_generation.md"
lines: 41-70
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 2.1 Subclaims defined in this guide

These are the subclaims enumerated by the
[HCert logical model](StructureDefinition-HCert.html). Section
[3.2.7.1](hcert_spec.html#3271-subclaims) of the HCERT Specification remains
authoritative.

| Subclaim | Logical model | Carries | Status |
|----------|---------------|---------|--------|
| `1` | [HCertDCC](http://smart.who.int/ddcc/StructureDefinition-HCertDCC.html) | EU Digital Covid Certificate core data set | Assigned |
| `3` | [DDCCCoreDataSetVS](http://smart.who.int/ddcc/StructureDefinition-DDCCCoreDataSetVS.html) | DDCC vaccination status | Proposed |
| `4` | [DDCCCoreDataSetTR](http://smart.who.int/ddcc/StructureDefinition-DDCCCoreDataSetTR.html) | DDCC test result | Proposed |
| `5` | string | URI of a Verifiable Health Link (VHL) | Assigned |
| `-6` | [DVCMin](http://smart.who.int/trust-phw/StructureDefinition-DVCMin.html) | Minimal Digital Vaccine Certificate | Development range |
| `-7` | [MedicationTreatmentLineMin](http://smart.who.int/ph4h/StructureDefinition-MedicationTreatmentLineMin.html) | Minimal medication treatment line | Development range |

Two properties of this registry matter when you pick a number:

* Subclaims **0 and above are reserved by WHO** and are allocated together with a
  trust domain. Request one through the GDHCN Secretariat.
* Subclaims that are **negative integers are the development range** and are free
  to use. `-6` (DVC) and `-7` are in that range today, which means an
  implementation using them is a pilot or a development deployment, and the
  number is not guaranteed to be stable. Do not treat a negative subclaim as a
  production allocation.

The registered production [trust domains](trust_domains.html) are `DDCC`,
`IPS-PILGRIMAGE`, `PH4H` and `ICVP`. There is no separate production trust
domain code for the generic DVC at the time of writing.
