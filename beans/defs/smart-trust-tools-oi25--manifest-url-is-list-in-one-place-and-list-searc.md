---
# smart-trust-tools-oi25
title: 'OI-25: Manifest URL is List? in one place and List/_search? in another'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

ITI-YY3 builds the manifest URL as .../List/_search?...; ITI-YY5 shows .../List?... and says the receiver POSTs the parameters to _search. The library accepts both and always POSTs to <base>/List/_search (STT-VHL-005).

## Reported

Reported 2026-10-24 to IHE as point 2 of a comment on IHE/ITI.VHL#48, the July 2026 update PR (https://github.com/IHE/ITI.VHL/pull/48#issuecomment-6385626316). That PR does not yet address it.

## Requirements affected

- req:stt-vhl-005

## Sources

- library/ihe-iti-vhl-iti-yy3/sections/s010-2-3-yy3-4-1-3-expected-actions
- library/ihe-iti-vhl-iti-yy5/sections/s008-2-3-yy5-4-1-2-message-semantics

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
