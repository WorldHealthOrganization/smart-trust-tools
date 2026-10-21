---
# smart-trust-tools-oi28
title: 'OI-28: How a receiver''s keyid, and a DID trust list entry, map to keys'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

ITI-YY5 says keyid is "used to locate key in trust list" but not how; and ITI-YY2's DID trust list is not tied to the COSE kid (8-byte SHA-256 of the DSC) that ITI-YY4 uses to find the VHL signer.

## Requirements affected

- req:stt-vhl-008

## Sources

- library/ihe-iti-vhl-iti-yy5/sections/s009-2-3-yy5-4-1-3-authentication-option-http-message
- library/ihe-iti-vhl-volume-1

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
