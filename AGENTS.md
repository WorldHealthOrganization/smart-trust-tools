# AGENTS.md — smart-trust-tools

This file is agent-generic: Claude Code, Gemini CLI, Cursor, Copilot and others
read it natively. The harness's
[`AGENTS.md`](https://github.com/litlfred/cat-harness/blob/HEAD/AGENTS.md) applies
everywhere. [`README.md`](README.md) says what this repository *is*.

## What this instance is

This repository holds the requirements and contracts for the SMART Trust HCERT
API libraries: Kotlin first (targeting JVM, Android and Node), Rust second
(targeting WebAssembly). They are derived from the WHO SMART Trust
implementation guide (`WorldHealthOrganization/smart-trust`, including PR 129's
QR code generation page) and its logical models in `smart-trust-phw`. The
instance is a cat-harness downstream: `smart-trust-tools.json` declares it,
`needs: ["fhir-harness"]`. `index.config.json` pins the layers below it, and
`index.lock.json` records the exact mounts.

## Cold start

```sh
bun run cat mount:lock      # lay the pinned layers down from index.lock.json (gitignored)
bun install
bun run cat mount:remote:check
```

To change a pin, edit `index.config.json` and run `bun run cat mount:remote`
(`--plan` first to see what it would do; `--help` for the flags).

## Rules

- **Only litlfred repositories are changed from here.** WHO repositories
  (`smart-trust`, `smart-trust-phw`) and WHO's gh-pages are read-only. Report
  issues there; never push to them.
- **gh-pages is published by an agent, not a workflow.** main publishes to the
  root. Every other branch publishes to `STAGING/<branch-slug>/`. Never
  dispatch a GitHub workflow to publish.
- **Generated is never edited by hand.** `requirements/generated/`,
  `api/generated/`, `models/generated/` and `kotlin/hcert-api/` are written by
  the generators under `tools/`. Change the source (`skills/requirements/*.json`, `requirements/kg/*.yaml`,
  `api/wit/`, `api/cddl/`, the logical models) and re-run the generator.
- **Every dependency is pinned and verified; nothing is resolved at run
  time.** Node tools come from `tools/node/package-lock.json` (`npm ci
  --ignore-scripts`, never `npx`), Python from the hashed
  `tools/python/requirements.txt` (`pip install --require-hashes --no-deps`),
  Gradle artifacts from `kotlin/gradle/verification-metadata.xml`, the harness
  from `bun.lock` (bunfig `minimumReleaseAge`), and GitHub Actions by commit
  SHA. A new or bumped dependency is a release at least 14 days old, resolved
  with that cutoff (`npm --before`, `uv --exclude-newer`), and its install
  scripts never run. Prefer writing a small, well-specified piece (Base45,
  deterministic CBOR) over adding a package for it.
- **Only open standards.** Every normative reference must be openly available:
  IETF RFCs, W3C, HL7 FHIR, WHO. No paywalled ISO.
