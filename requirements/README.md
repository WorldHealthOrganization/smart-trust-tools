# Requirements knowledge graph

This folder holds the derived requirements for the SMART Trust HCERT API
libraries, as a knowledge graph. The Kotlin library comes first and the Rust
library second; both are built against the same requirement set.

The requirements are derived from the WHO SMART Trust IG, mainly the
**QR Code Generation** page proposed in
[WorldHealthOrganization/smart-trust#129](https://github.com/WorldHealthOrganization/smart-trust/pull/129)
(`input/pagecontent/qr_code_generation.md`, plus its entries in `changes.md`
and `data_exchange.md`), and the deployed HCERT Specification, DID trustlist
and FHIR `Requirements` resources.

**Read this first:** [`generated/REQUIREMENTS.md`](generated/REQUIREMENTS.md)

## Layout

| Path | What it holds |
|---|---|
| [`../skills/requirements/`](../skills/requirements/) | The library requirements, one cat-harness `req:` node per `STT-<AREA>-nnn` (below) |
| `kg/sources.yaml` | smart-trust documents and sections, pinned commits, URL tiers; external standards; stakeholder directives |
| `kg/upstream.yaml` | smart-trust FHIR `Requirements` statements and actors that library requirements `satisfy` |
| `kg/steps.yaml` | The 9-step HCERT QR pipeline, plus verification and trust-list steps |
| `kg/api.yaml` | API elements linked to their WIT items in [`../api/wit/`](../api/wit/), with Kotlin and Rust sketches |
| `kg/issues.yaml` | Gaps and inconsistencies found in the sources (most to fix upstream) |
| `generated/kg.ttl`, `generated/kg.jsonld` | The graph as RDF (Turtle and JSON-LD; same triples) |
| `generated/REQUIREMENTS.md` | Human-readable rendering with traceability tables |

Do not edit `generated/` by hand. Edit the requirement nodes or the YAML, then run:

```sh
pip install pyyaml
python3 tools/build_kg.py           # validate + regenerate
python3 tools/build_kg.py --check   # CI: fail if generated/ is stale
python3 tools/build_kg.py --check-links   # probe every URL (after deploy)
```

The build fails on duplicate ids, dangling references, requirements with no
source, unknown conformance, category or derivation values, and pipeline steps
that no requirement covers.

## Requirement nodes

Each requirement is a file `skills/requirements/stt-<area>-<nnn>.json` holding a
cat-harness `req:` node (bootstrap `RequirementSchema`), so the harness's
requirement tooling reads it:

| `STT` field | In the `req:` node |
|---|---|
| `id` | `id` = `req:stt-<area>-<nnn>`, the file name; `stt.id` keeps the original |
| `title`, `statement`, `conformance`, `category` | `title`; one statement, key `statement`, with `label`, `requirement`, `conformance`, `category` |
| `refines` | `derivedFrom` entries `req:stt-…` |
| `satisfies` | `derivedFrom` entries `http://smart.who.int/trust/Requirements/<id>[#<statement-key>]` |
| `verified_by` | the statement's `successCriteria`, one per test requirement, `verification: test` |
| actors | `issuer` / `receiver` from the steps and the satisfied smart-trust statements; otherwise `implementer` |
| `derivation`, `targets`, `steps`, `api`, `issues`, `derived_from` | the `stt` block (`derived_from` as `stt.sources`) |

`description` names the derivation and the source sections; `status` is
`proposed` until the requirement set is signed off.

## Graph model

```
Requirement ──prov:wasDerivedFrom──▶ Section ──dcterms:isPartOf──▶ Document
     │  └─stt:hasDerivation─▶ Derivation (stt:source, stt:quote)
     ├─stt:satisfies──────▶ UpstreamStatement ─stt:statementOf─▶ UpstreamRequirement ─stt:actor─▶ Actor
     ├─stt:implementsStep─▶ PipelineStep
     ├─stt:constrainsApi──▶ ApiElement ─stt:inModule─▶ ApiModule
     ├─stt:refines────────▶ Requirement
     ├─stt:verifiedBy─────▶ Requirement (category test)
     └─stt:affectedBy─────▶ OpenIssue ─stt:concerns─▶ Section | Document
```

Vocabulary: `https://github.com/litlfred/smart-trust-tools/kg/vocab#` (`stt:`).
Instances: `https://github.com/litlfred/smart-trust-tools/kg/id/<id>`.
The graph reuses `dcterms:` and `prov:` terms.

## Current and post-merge references

Each smart-trust document and section has these URLs:

- `stt:pinnedUrl`: GitHub source at the exact commit drafted against
  (`main@e277cce`, or PR 129 head `50bcb25`). Always resolves.
- `stt:previewUrl`: PR branch IG build
  (`…/smart-trust/branches/intelligent-bell-usgxfu/…`). Only for content
  that is still `pending-merge`.
- `stt:mainUrl`, `stt:ciUrl`: source and CI build on main. These resolve for
  PR 129 content once it is merged.
- `stt:publishedUrl`: `https://smart.who.int/trust/…`. Resolves once released.

After PR 129 merges: set `status: deployed`, move the documents to `ref: main`
with the new main SHA, regenerate, and run `--check-links` (OI-10).

## Example query

```sparql
PREFIX stt: <https://github.com/litlfred/smart-trust-tools/kg/vocab#>
PREFIX dcterms: <http://purl.org/dc/terms/>
# Which library requirements satisfy each smart-trust requirement statement?
SELECT ?upstream ?req WHERE {
  ?r stt:satisfies ?u ; dcterms:identifier ?req .
  ?u dcterms:identifier ?upstream .
} ORDER BY ?upstream ?req
```
