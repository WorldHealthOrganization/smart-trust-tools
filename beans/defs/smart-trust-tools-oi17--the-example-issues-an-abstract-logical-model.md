---
# smart-trust-tools-oi17
title: 'OI-17: The example issues an abstract logical model'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

In smart-trust-phw, DVCMin and DVCMinVaccineDetails are declared abstract (^abstract = true). Only DVCMinPreQual and DVCMinVaccineDetailsPreQual are concrete. The HCert logical model puts DVCMin at subclaim -6, and the PR 129 example issues a DVCMin with ICD-11 codes. So no concrete model exists for a non-PreQual DVC: either DVCMin should not be abstract, or a concrete ICD-11 profile is needed.

## Reported

Reported 2026-10-24 to WHO as point 7 of WorldHealthOrganization/smart-trust#138 (https://github.com/WorldHealthOrganization/smart-trust/issues/138). The issue was filed on smart-trust; the model is in smart-trust-phw.

## Requirements affected

- req:stt-pay-010

## Sources

- library/who-smart-trust-phw-dvcmin
- library/who-smart-trust-phw-dvcmin-vaccine-details
- library/who-smart-trust-phw-dvcmin-prequal
- library/who-smart-trust-qr-code-generation/sections/s004-3-step-2-assemble-the-payload
- library/who-smart-trust-qr-code-generation/sections/s017-12-4-the-payload

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
