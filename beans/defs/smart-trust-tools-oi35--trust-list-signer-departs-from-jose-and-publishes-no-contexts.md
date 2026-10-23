---
# smart-trust-tools-oi35
title: 'OI-35: The trust list signer departs from JOSE and does not publish its contexts'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-23T09:00:00Z
updated_at: 2026-10-23T10:00:00Z
---

Verifying the GDHCN DID trust list proofs (STT-TRU-006) meant reproducing what WHO's signer, tng-key-distribution 021a9d2 with ld-signatures-java 1.11.0, actually does. In three places that differs from the open standards the proofs name:

1. **The JWS signature is DER, not r||s.** JsonWebSignature2020 proofs carry a detached ES256 JWS, and RFC 7518 3.4 makes an ES256 signature the 64-byte r||s; the signer uses the JCA's DER output as is. Standard JOSE verifiers refuse it.
2. **The canonical form depends on contexts the documents do not pin.** URDNA2015 runs over JSON-LD expanded with contexts the signer bundles (did/v1, jws-2020/v1, the per-environment tng-context, and ld-signatures' copy of the unstable security/v3). Nothing published says which content was used, so a verifier that fetches the context URLs may canonicalize differently.
3. **The JWKs are not RFC 7518 6 JWKs.** EC coordinates come from BigInteger.toByteArray, so about half carry a leading 0x00 and some will be short; P-384 and P-521 keys are named "UNKNOWN CURVE"; RSA e is padded base64url.

## Resolution

The libraries verify the documents as published: a DER or r||s signature, the bundled contexts pinned in the vectors (vectors/gdhcn-dev-trustlist/), and a signer coordinate of 32 bytes, or 33 with a leading 0x00. The unified profile refuses anything else.

Reported upstream, as the owner chose (2026-10-23), as issues on tng-key-distribution:

- WorldHealthOrganization/tng-key-distribution#85 (https://github.com/WorldHealthOrganization/tng-key-distribution/issues/85): the DER signature
- WorldHealthOrganization/tng-key-distribution#86 (https://github.com/WorldHealthOrganization/tng-key-distribution/issues/86): the unpublished contexts. The owner closed it as completed on 2026-10-23, after the guide documented the contexts (WorldHealthOrganization/smart-trust#136)
- WorldHealthOrganization/tng-key-distribution#87 (https://github.com/WorldHealthOrganization/tng-key-distribution/issues/87): the JWK encoding

The owner's decision, 2026-10-23: **keep refusing** what the published documents never contain. That covers a JWK member that is not a string or an array of strings (a number would need RFC 8785's number formatting, an implicit conversion), and any character above U+FFFF (the canonical N-Quads order by code point and Java's UTF-16 order disagree only there). If WHO publishes such a document, the profile is widened on purpose, with a vector.

The verifier's profile is drafted for the guide's DID trust list page as WorldHealthOrganization/smart-trust#136 (https://github.com/WorldHealthOrganization/smart-trust/pull/136): the signer DID per environment, the pinned contexts with their hashes, the JWK member profile, the algorithm and the current DER encoding. It is WHO's to review and merge; the UAT and PROD signer DIDs are still to be stated.

## Requirements affected

- req:stt-tru-006

## Sources

- vectors/gdhcn-dev-trustlist/XXR-DSC-did-2026-10-23.json

## Done when

- [x] the libraries verify the published proofs (STT-TRU-006)
- [x] the guide states the verifier profile (WorldHealthOrganization/smart-trust#136)
- [ ] the guide's DER transition rule is removed once the signer is fixed (WorldHealthOrganization/smart-trust#137)
- [ ] the signer emits r||s, publishes its contexts and encodes JWKs per RFC 7518, or the guide documents the profile
