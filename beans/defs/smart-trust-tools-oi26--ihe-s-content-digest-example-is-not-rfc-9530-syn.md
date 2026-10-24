---
# smart-trust-tools-oi26
title: 'OI-26: IHE''s Content-Digest example is not RFC 9530 syntax'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:54:54Z
---

The ITI-YY5 example sends "Content-Digest: sha-256=X48E...=", but RFC 9530 makes the value a Structured Field byte sequence, "sha-256=:X48E...=:". A sharer verifying strictly rejects the example form. The library emits RFC 9530 syntax (STT-VHL-008).

## Reported

Reported 2026-10-24 to IHE as point 3 of a comment on IHE/ITI.VHL#48, the July 2026 update PR (https://github.com/IHE/ITI.VHL/pull/48#issuecomment-6385626316). That PR does not yet address it.

## Requirements affected

- req:stt-vhl-008

## Sources

- library/ihe-iti-vhl-iti-yy5/sections/s009-2-3-yy5-4-1-3-authentication-option-http-message
- external-schemas/ietf-rfc9530

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
