---
# smart-trust-tools-oi14
title: 'OI-14: Package coordinates and licence for the libraries'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-23T09:00:00Z
---

Maven group / Kotlin package root and crate names are placeholders. The natural reverse-DNS root, int.who, cannot be used because `int` is a reserved Java keyword. The licence has not been chosen.

## Resolution

The owner's decisions of 2026-10-23. Licence: Apache-2.0 (LICENSE at the root, the official apache.org text, copied into the crate, the npm packages and every jar). Coordinates: they follow the repository's GitHub org, from smart-trust-tools.json's "repository": Maven group io.github.<org>, npm scope @<org>, crate <org>-hcert (library hcert); today io.github.litlfred, @litlfred, litlfred-hcert. The Kotlin package root stays who.smart.trust. tools/release-packaging/check-version.mjs enforces all of it; docs/RELEASING.md says what changes if the repository moves org.

## Requirements affected

- req:stt-nfr-002

## Sources

- memory/dir-languages

## Done when

- [x] fixed in this repository
