# smart-trust-tools
tools for the smart-trust repo

## Contents

- [`api/`](api/): the library API contract, defined first in WIT
  (WebAssembly Component Model IDL), with a generated TypeScript view for
  review. Start with [`api/README.md`](api/README.md).
- [`models/`](models/): payload models derived from the WHO SMART FHIR
  logical models: JSON-LD context, SHACL shapes and CDDL.
- [`vectors/`](vectors/): conformance vectors (the PR 129 DVC example).
- [`requirements/`](requirements/): requirements knowledge graph for the
  SMART Trust HCERT API libraries (Kotlin first, Rust second), derived from
  the WHO SMART Trust IG. Start with
  [`requirements/generated/REQUIREMENTS.md`](requirements/generated/REQUIREMENTS.md).
- [`tools/`](tools/): `build_kg.py` (requirements graph), `lm_derive.py`
  (logical models to JSON-LD/SHACL/CDDL), `check_models.py` (schemas against
  vectors), `gen_api_types.sh` (WIT to TypeScript), `fetch_lms.sh`.
