# smart-trust-tools
tools for the smart-trust repo

## Contents

- [`api/`](api/): the library API contract, defined first in WIT
  (WebAssembly Component Model IDL), with a generated TypeScript view for
  review. Start with [`api/README.md`](api/README.md).
- [`requirements/`](requirements/): requirements knowledge graph for the
  SMART Trust HCERT API libraries (Kotlin first, Rust second), derived from
  the WHO SMART Trust IG. Start with
  [`requirements/generated/REQUIREMENTS.md`](requirements/generated/REQUIREMENTS.md).
- [`tools/build_kg.py`](tools/build_kg.py): validates the graph and generates
  the Turtle, JSON-LD and Markdown outputs.
