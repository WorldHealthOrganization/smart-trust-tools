---
# smart-trust-tools-oi18
title: 'OI-18: Sex binding in DVCMin points at a CodeSystem, not a ValueSet'
status: scrapped
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:03:22Z
---

DVCMin.s is bound (extensible) to $GENDER, which smart-trust-phw aliases to http://hl7.org/fhir/administrative-gender, the CodeSystem URL. A binding must reference a ValueSet: http://hl7.org/fhir/ValueSet/administrative-gender.

## Requirements affected

- req:stt-pay-010

## Sources

- library/who-smart-trust-phw-dvcmin
- library/who-smart-trust-phw-aliases

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it

## Scrapped

2026-10-24, Claude (https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz): a duplicate of OI-36, which records the same binding error (found by the FHIR Validator), its upstream report WorldHealthOrganization/smart-trust-phw#20 and the fix smart-trust-phw#21. models/README.md now names OI-36.
