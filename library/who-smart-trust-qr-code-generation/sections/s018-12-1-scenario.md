---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s018-12-1-scenario
section_title: "12.1 Scenario"
file: "input/pagecontent/qr_code_generation.md"
lines: 296-319
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 12.1 Scenario

A family from the Lao People's Democratic Republic is visiting relatives abroad
when the mother delivers. The newborn receives the routine birth-dose
vaccinations of the **host country's** national childhood immunization schedule
before discharge. The clinic records the doses and issues a signed certificate;
the family has one shared phone between them, so the clinic prints a paper card
with the QR code and offers digital delivery as an option.

This drives the design decisions directly:

* **DVC, not ICVP.** Hepatitis B and BCG are routine schedule vaccines, not IHR
  entry requirements, so the generic DVC is the correct instrument and the
  Yellow Book ICVP is not.
* **DVC, not VHL.** The certificate has to verify at a rural clinic back home
  with no reliable connectivity, and on paper. The data must be *in* the QR.
* **Two certificates.** `vx` is 1..1, so the birth visit yields one certificate
  per vaccine.
* **No `id`, but a `gn`.** The newborn has no identification document. The
  guardian element carries the mother's name instead.
* **Nationality and issuer differ.** `nt` is the infant's nationality (`LAO`);
  the issuer is the country whose clinic administered the dose. This separation
  is exactly what the model is for.
