---
# smart-trust-tools-oi22
title: 'OI-22: IHE VHL examples break their own rules'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

The ITI-YY3 example JSON has "extension" but its base64url encodes "extensions". The ITI-YY4 example key is 44 characters and decodes to 33 bytes ("this is a secret key used for enc"), against the 43-character, 32-byte rule stated beside it.

## Resolution

Fixed in the IHE patch in upstream/ihe-iti-vhl/.

## Requirements affected

- req:stt-vhl-001

## Sources

- library/ihe-iti-vhl-iti-yy3/sections/s010-2-3-yy3-4-1-3-expected-actions
- library/ihe-iti-vhl-iti-yy4

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
