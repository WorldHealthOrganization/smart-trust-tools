---
title: User journey coverage
nav_order: 2
---

# User journey coverage (DRAFT journeys J1 to J4)

What the HCERT library must do for each step of the workshop storyline, and
whether it does. OHS Player, OHS Stack and the CHW and wallet apps are
consumers of the Kotlin library; rendering, scanning, storage, HTTP and
FHIR record keeping are theirs. Source: stakeholder directive
`dir-journeys`, 2026-10-20.

| Journey step | Library capability | Status |
|---|---|---|
| **J1** Country A clinic issues a DVC for each newborn vaccine (Hep B, polio) | DVCMin payload, one event per certificate (STT-PAY-003); no identity document for a newborn (STT-PAY-005); guardian name `gn`; any national vaccine code in `vp` (extensible binding) | Done: `HcertIssuer.prepare` / `finish` |
| J1 signs with the clinic's DSC key held in Android Keystore or an HSM | Two-phase signing, no private key in process (STT-ISS-004, STT-COS-006) | Done |
| J1 printed paper QR, or SMS/MMS | HC1 string is QR Alphanumeric only (STT-QR-001); the app renders it at error correction Q | Done (rendering is the app's) |
| J1 DEV keys and DEV trust list | Trust store loads GDHCN DID trustlist v2 documents (DEV, UAT, PROD alike) and PEM | Done: `addDidDocument`, `addPem`; the app fetches from the [trust list distribution endpoints](https://smart.who.int/trust/endpoints.html#trustlist-distribution-endpoints) |
| **J2** CHW, offline, reads the foreign QR to register the child and refer | Explicitly named unverified read, its result typed as unverified (STT-VER-011) | Done: `HcertVerifier.readUnverified` |
| J2 alternative: verify offline on the CHW device | Verification is synchronous and offline against a store the app filled earlier (STT-GEN-012, STT-VER-009) | Done; which one J2 uses is the journey's open question |
| **J3** Clinic verifies the foreign DVC | GDHCN verification in the STT-VER-001 order, typed failures (STT-VER-008), EKU check (STT-VER-004) | Done: `HcertVerifier.verify`, PR 129 vector verifies on JVM and Node |
| J3 imports the immunization into the domestic record | The verified DVCMin fields; mapping to FHIR R4 `Immunization`/`Patient` | Gap: the mapping belongs with the logical models (smart-trust-phw), not in this library; the app maps until it is published |
| J3 foreign vaccine code with no equivalence map | Codes pass through unchanged; nothing is rejected for an unknown national code | Done; adjudication is a policy question |
| **J4.0** National portal issues one passcode-protected VHL QR per family member | VHL payload (`vhlink:/`, url, key, exp, flag P, label) in HCERT claim -260 key 5; key generation; JWE encryption of each document | **Gap: VHL module not built yet** |
| **J4.1** Clinic abroad scans a VHL, fetches with passcode, reads IPS | Verify the VHL HCERT (done); parse the link; build the manifest request; parse the manifest Bundle; decrypt each JWE document | **Gap: VHL module not built yet**; the HTTP, mTLS and audit are the app's and the VHL host's |

## VHL: what the specifications say, and where they disagree

The IHE ITI VHL profile and the WHO `VerifiableHealthLinkPayload` model
share the SMART Health Links payload fields, but:

- IHE's manifest is a FHIR `List` search returning a searchset Bundle of
  `DocumentReference`s (ITI-YY5), not the SMART Health Links `files[]`
  manifest; the Bundle is not signed, and the journey's "signed VHL
  manifests" exist in neither specification.
- The value at hcert key 5 is a `vhlink:/` text string in ITI-YY3 but a
  decoded JSON object in the ITI-YY4 example; HCERT 3.2.7.1.5 calls it a
  SMART Health Link (OI-06).
- WHO requires `type` (`vhl`) and types `exp` and `v` as strings; IHE has no
  `type` and uses numbers.
- JWE `alg`/`enc` are not normative (the example is `dir` / `A256GCM`); no
  `zip`.
- Requester authentication (RFC 9421 HTTP signatures, UDAP, or a VC) and the
  DID trust list's mapping to a COSE `kid` are under-specified.
