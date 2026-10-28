---
# smart-trust-tools-oi10
title: 'OI-10: Deployed section anchors and preview paths need checking'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-22T00:00:00Z
---

The library entries ingested from PR 129 (QR Code Generation, Changes, Data Exchange, the reference script) pin the PR branch and its preview build, because the page is not yet merged or deployed. Once PR 129 is merged, re-ingest them from main at the merge commit, so their sections carry the published URL and anchors, and check those links resolve.

## Resolution

PR 129 merged as WHO af75be2. QR Code Generation, Change Log and Data Exchange are re-ingested from main at that commit, with HCERT Specification and the CWT payload model, which changed on main since e277cce; their sections carry https://smart.who.int/trust/ URLs. The reference script's entry is removed, because the merge dropped the script. Requirement and bean references to the old QR Code Generation sections are mapped to the new ones (2.1 to 2.4 are now one section, 2; 12.3 is "Issuer", 12.6 "The certificates"). The published pages were not fetched from here to check their anchors resolve.

## Sources

- library/who-smart-trust-qr-code-generation

## Done when

- [x] fixed in this repository
