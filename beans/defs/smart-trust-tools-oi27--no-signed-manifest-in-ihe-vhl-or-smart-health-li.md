---
# smart-trust-tools-oi27
title: 'OI-27: No signed manifest in IHE VHL or SMART Health Links'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

The journeys ask to "verify DVC and signed VHL manifests against the DEV trust list". Neither specification signs the manifest: the VHL link is signed (the HCERT), and the manifest's integrity rests on mutual TLS and receiver authentication. Either the journey wording changes, or a signed-manifest option is proposed upstream.

## Reported

Reported 2026-10-24 to IHE as point 4 of a comment on IHE/ITI.VHL#48, the July 2026 update PR (https://github.com/IHE/ITI.VHL/pull/48#issuecomment-6385626316). That PR does not yet address it.

## Requirements affected

- req:stt-vhl-006

## Sources

- library/ihe-iti-vhl-iti-yy5/sections/s016-2-3-yy5-4-2-2-message-semantics
- memory/dir-journeys

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
