---
# smart-trust-tools-oi34
title: 'OI-34: The DEV trust list DIDs name a different host from the guide'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-22T17:10:00Z
updated_at: 2026-10-22T17:10:00Z
---

QR Code Generation 12.6 and the DEV endpoints (input/fsh/instances/endpoints-DEV.fsh, WHO af75be2) give XXR's trust list as did:web:tng-cdn.who.int:v2:trustlist:-:XXR:DSC, "resolvable at https://tng-cdn-dev.who.int/v2/trustlist/-/XXR/DSC/did.json". Under the did:web method a DID resolves at the host it names, so did:web:tng-cdn.who.int:... resolves at https://tng-cdn.who.int/..., the production host, not tng-cdn-dev. The document actually served from tng-cdn-dev (retrieved 2026-10-22, vectors/smart-trust-af75be2/XXR-DSC-did.json) gives its own id as did:web:tng-cdn-dev.who.int:v2:trustlist:-:XXR:DSC, which disagrees with the guide.

A verifier that resolves the DID the guide prints therefore asks the production host for a DEV participant, and a verifier that checks a resolved document's id against the DID it asked for refuses the document it gets from tng-cdn-dev. The libraries are unaffected: the host supplies DID documents (STT-TRU-001), and the trust store checks a document's id only against the GDHCN trust domains (STT-SEL-006).

## Resolution

Upstream: make the DEV (and UAT) endpoint addresses and the guide's text name the host the documents are served from (did:web:tng-cdn-dev.who.int:...), or serve the DEV documents under the DID the guide names.

## Requirements affected

- req:stt-tst-011
- req:stt-tru-001

## Sources

- library/who-smart-trust-qr-code-generation/sections/s019-12-6-the-certificates
- vectors/smart-trust-af75be2/XXR-DSC-did.json

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
