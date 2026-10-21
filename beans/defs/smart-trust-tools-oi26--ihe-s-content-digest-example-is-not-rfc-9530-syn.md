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
updated_at: 2026-10-20T20:00:00Z
---

The ITI-YY5 example sends "Content-Digest: sha-256=X48E...=", but RFC 9530 makes the value a Structured Field byte sequence, "sha-256=:X48E...=:". A sharer verifying strictly rejects the example form. The library emits RFC 9530 syntax (STT-VHL-008).

## Requirements affected

- req:stt-vhl-008

## Sources

- library/ihe-iti-vhl-iti-yy5/sections/s009-2-3-yy5-4-1-3-authentication-option-http-message
- external-schemas/ietf-rfc9530

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
