---
title: SMART Trust HCERT library tools
nav_order: 1
---

# SMART Trust HCERT library tools

Requirements and contracts for the SMART Trust HCERT API libraries: Kotlin first
(JVM, Android, Node), Rust second (WebAssembly). They are derived from the WHO
SMART Trust implementation guide, in particular the QR Code Generation page of
[WorldHealthOrganization/smart-trust#129](https://github.com/WorldHealthOrganization/smart-trust/pull/129).

## Viewers

| Viewer | What it shows |
|---|---|
| [Overview](viewers/index.html) | The viewers' own landing page, with counts and the pipeline |
| [Graph explorer](viewers/explorer.html) | The requirements knowledge graph, grouped and drillable |
| [Traceability matrix](viewers/trace.html) | Every requirement against its sources, steps, API elements and issues |
| [Requirements](viewers/requirements.html) | The 80 library requirements (cat-harness `req:` nodes) |
| [API contract](viewers/api.html) | The library API in WIT, with its TypeScript reference |
| [Wire and payload models](viewers/models.html) | CDDL wire format; JSON-LD, SHACL and CDDL from the FHIR logical models |
| [Kotlin skeleton](viewers/kotlin.html) | The Kotlin Multiplatform API generated from the WIT |
| [Vocabulary](viewers/vocabulary.html) | The `stt:` terms the graph uses |

## Sources

The requirement nodes are in
[`skills/requirements/`](https://github.com/litlfred/smart-trust-tools/tree/main/skills/requirements),
one `req:` node per `STT-<AREA>-nnn`. The sources, pipeline steps, API elements
and open issues are in
[`requirements/kg/`](https://github.com/litlfred/smart-trust-tools/tree/main/requirements/kg).
