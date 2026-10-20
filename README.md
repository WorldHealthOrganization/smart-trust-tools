# smart-trust-tools
tools for the smart-trust repo

## Viewers

The site is built and pushed to the `gh-pages` branch with `tools/publish_site.sh` (run by
hand, not by a workflow) and served by GitHub Pages:
**https://litlfred.github.io/smart-trust-tools/**. It includes the graph explorer, the
traceability matrix, the requirements, the WIT API contract with its TypeScript reference, and
the wire and payload models. Build it locally with `tools/build_site.sh` (needs Python and
Node.js).

## Contents

- [`api/`](api/): the library API contract, defined first in WIT
  (WebAssembly Component Model IDL), with a generated TypeScript view for
  review. Start with [`api/README.md`](api/README.md).
- [`models/`](models/): payload models derived from the WHO SMART FHIR
  logical models: JSON-LD context, SHACL shapes and CDDL.
- [`vectors/`](vectors/): conformance vectors (the PR 129 DVC example).
- [`kotlin/`](kotlin/): the Kotlin Multiplatform API skeleton generated from the WIT.
- [`requirements/`](requirements/): requirements knowledge graph for the
  SMART Trust HCERT API libraries (Kotlin first, Rust second), derived from
  the WHO SMART Trust IG. Start with
  [`requirements/generated/REQUIREMENTS.md`](requirements/generated/REQUIREMENTS.md).
- [`tools/`](tools/): `build_kg.py` (requirements graph), `lm_derive.py`
  (logical models to JSON-LD/SHACL/CDDL), `check_models.py` (schemas against
  vectors), `gen_api_types.sh` (WIT to TypeScript), `fetch_lms.sh`.
