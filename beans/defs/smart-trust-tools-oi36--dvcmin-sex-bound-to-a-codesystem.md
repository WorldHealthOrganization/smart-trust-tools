---
# smart-trust-tools-oi36
title: 'OI-36: DVCMin.s is bound to the administrative-gender CodeSystem, not a ValueSet'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-23T13:00:00Z
updated_at: 2026-10-23T13:00:00Z
---

The HL7 FHIR Validator, now run on the logical models before main publishes (tools/fhir-validate), reports one error in the upstream DVCMin model: "The valueSet reference http://hl7.org/fhir/administrative-gender on element DVCMin.s points to something that is not a value set (CodeSystem)". The cause is smart-trust-phw's Aliases.fsh: $GENDER names the CodeSystem, which is right for the ICVP.Core ConceptMap but wrong for the bindings DVCMin.s and DVC.sex.

The derived payload models carry the same URL, as a CDDL comment and as the SHACL annotation lmx:valueSet on DVCMin.s. The binding is extensible and is not enforced, so no verdict changes. The URL corrects itself when the models are recompiled from the fixed source.

## Resolution

Reported as WorldHealthOrganization/smart-trust-phw#20 (https://github.com/WorldHealthOrganization/smart-trust-phw/issues/20), with the fix in WorldHealthOrganization/smart-trust-phw#21 (https://github.com/WorldHealthOrganization/smart-trust-phw/pull/21), which is WHO's to merge. Until then tools/fhir-validate/known-errors.json lists the error, so main still publishes; the entry fails the run once the validator stops reporting it.

## Requirements affected

- req:stt-pay-001

## Sources

- library/who-smart-trust-phw-dvcmin

## Done when

- [ ] WHO merges the fix, models/fhir is recompiled at the new sha (tools/fetch-lms/fetch_lms.sh), and the known-errors entry is removed
