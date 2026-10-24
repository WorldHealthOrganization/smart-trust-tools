---
# smart-trust-tools-oi09
title: 'OI-09: DVC subclaim -6 is in the development range'
status: in-progress
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:40:00Z
---

The number "is not guaranteed to be stable". The library must not bake -6 into its wire format, and has to be able to move DVC to a WHO-assigned number without a breaking API change.

## Requirements affected

- req:stt-sel-003

## Sources

- library/who-smart-trust-qr-code-generation/sections/s003-2-step-1-select-the-payload-type

## Done when

- [ ] fixed in this repository

## Notes

- 2026-10-24: claimed by Claude, session https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz (smart-trust-tools bean drain). The issuer and the payload decoder already take the number from the registry; the EKU check in both libraries still names -6.
