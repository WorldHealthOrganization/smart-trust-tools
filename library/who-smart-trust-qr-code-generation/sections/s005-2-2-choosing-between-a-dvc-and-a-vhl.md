---
doc_id: who-smart-trust-qr-code-generation
doc_title: "QR Code Generation"
section_id: s005-2-2-choosing-between-a-dvc-and-a-vhl
section_title: "2.2 Choosing between a DVC and a VHL"
file: "input/pagecontent/qr_code_generation.md"
lines: 71-94
source_sha256: 25cc22304a1d036e
granularity: heading
---
##### 2.2 Choosing between a DVC and a VHL

The decisive question is **whether the data must survive offline, and how much of
it there is**.

| | **DVC in the QR (subclaim `-6`)** | **VHL in the QR (subclaim `5`)** |
|---|---|---|
| What the QR holds | The vaccination data itself, signed | A link to the data, signed |
| Works fully offline | Yes — verifier needs only the trust list | No — verifier must dereference the link |
| Data volume | Small, bounded; one immunization event per certificate | Unbounded; a full IPS or immunization history |
| Revocation / update after issue | Not possible; reissue instead | Possible — the content behind the link can change |
| Selective disclosure | Not in the minimal payload | Yes, the endpoint can tailor what is returned |
| Holder device | Paper is sufficient | Paper is sufficient to carry the link, but a network is needed to use it |
| Privacy exposure | All data is in the QR and is readable by anyone who scans it | Only the link is in the QR; access can be gated |

In short: **use a DVC when the QR must be self-sufficient**, for example a paper
card presented at a rural clinic, a border post with intermittent connectivity,
or an infant's home-based record. **Use a VHL when the content is larger than a
QR can hold, when it must be able to change after issue, or when the holder
should control how much is disclosed.**

A deployment may legitimately issue both for the same person: a DVC for the
dose that must be provable offline, and a VHL pointing at the full record.
