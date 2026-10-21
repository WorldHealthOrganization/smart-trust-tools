---
# smart-trust-tools-oi10
title: 'OI-10: Deployed section anchors and preview paths need checking'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

The library entries ingested from PR 129 (QR Code Generation, Changes, Data Exchange, the reference script) pin the PR branch and its preview build, because the page is not yet merged or deployed. Once PR 129 is merged, re-ingest them from main at the merge commit, so their sections carry the published URL and anchors, and check those links resolve.

## Sources

- library/who-smart-trust-qr-code-generation

## Done when

- [ ] fixed in this repository
