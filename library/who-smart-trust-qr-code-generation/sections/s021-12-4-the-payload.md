---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s021-12-4-the-payload
section_title: "12.4 The payload"
file: "input/pagecontent/qr_code_generation.md"
lines: 343-381
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 12.4 The payload

Hepatitis B birth dose, `hcert` subclaim `-6`, shown in JSON notation:

```json
{
  "n":   "Chanthala SOUVANNALATH",
  "dob": "2026-09-28",
  "s":   "female",
  "nt":  "LAO",
  "gn":  "Mali SOUVANNALATH",
  "vx": {
    "vp": "XM9V38",
    "dv": "2026-09-28",
    "cn": "Dr Pimchanok WATTANA",
    "is": "org-xxa-provincial-hospital",
    "bo": "HEPB-2026-0914"
  },
  "v": "1.0.0"
}
```

`vp` is ICD-11 `XM9V38` *Hepatitis B, purified antigen*; the BCG certificate uses
`XM8142` *Tuberculosis, live attenuated vaccines*. Both are in the
[DVC - Vaccines value set](http://smart.who.int/trust-phw/ValueSet-DVCVaccines.html).
An implementation conforming to `DVCMinPreQual` would instead carry the WHO
PreQual product identifier of the specific product administered.

Wrapped in the CWT claim set:

```json
{
  "1":    "XX",
  "6":    1790670600,
  "4":    1822206600,
  "-260": { "-6": { "...": "the payload above" } }
}
```
