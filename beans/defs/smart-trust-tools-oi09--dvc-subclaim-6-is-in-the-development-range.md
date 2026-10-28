---
# smart-trust-tools-oi09
title: 'OI-09: DVC subclaim -6 is in the development range'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:07:28Z
---

The number "is not guaranteed to be stable". The library must not bake -6 into its wire format, and has to be able to move DVC to a WHO-assigned number without a breaking API change.

## Requirements affected

- req:stt-sel-003

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-payload-type

## Done when

- [x] fixed in this repository

## Notes

- 2026-10-24: claimed by Claude, session https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz (smart-trust-tools bean drain). The issuer and the payload decoder already take the number from the registry; the EKU check in both libraries still names -6.
- 2026-10-24: fixed in smart-trust-tools#34. The EKU check in both libraries now takes DVC's number from the registry (`eku_for` in verifier.rs, `ekuFor` in Pipeline.kt); before, a DVC moved off -6 lost its key-usage check (a test-only DSC could sign it). New tests in each library, `STT-SEL-003 DVC moves to an assigned subclaim without an API change`, register 6, issue without the development opt-in, verify under 6, and refuse it under a test-only DSC.
