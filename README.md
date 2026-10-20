# smart-trust-tools
tools for the smart-trust repo

## Contents

- [`requirements/`](requirements/): requirements knowledge graph for the
  SMART Trust HCERT API libraries (Kotlin first, Rust second), derived from
  the WHO SMART Trust IG. Start with
  [`requirements/generated/REQUIREMENTS.md`](requirements/generated/REQUIREMENTS.md).
- [`tools/build_kg.py`](tools/build_kg.py): validates the graph and generates
  the Turtle, JSON-LD and Markdown outputs.
