import { toolFactory } from "../../cat-harness/schemas/tool-folders.js";
import { defineTool } from "../../cat-harness/schemas/tool.js";

export default toolFactory((t) =>
  defineTool({
    id: "hcert-rust",
    title: "HCERT library (Rust, native and WebAssembly)",
    description:
      "The Rust implementation of the HCERT pipeline: Base45, ZLIB, deterministic CBOR, COSE_Sign1 ES256/PS256 (RustCrypto p256, rsa, sha2), CWT, the DVCMin payload, the trust store (PEM, DER, GDHCN DID trustlist v2, trust domains), the section 11 verifier and two-phase issuance. It builds natively and for wasm32-unknown-unknown as a module with no imports; js/hcert.mjs loads it in a browser or Node and signs with WebCrypto, and a TEST ONLY builder makes a self-signed DSC around a key pair the page generates. Every crate is pinned in Cargo.lock (DEPENDENCIES.md).",
    install: { none: true },
    invoke: { shell: "cargo test --locked --manifest-path tools/hcert-rust/Cargo.toml" },
    io: {
      inputs: [
        { name: "target", schema: t("Slug"), required: false, arg: { flag: "--target" }, description: "wasm32-unknown-unknown for the WebAssembly module (with `cargo build --release`), else the host." },
      ],
      outputs: [
        { name: "library", schema: t("RepoPath"), description: "tools/hcert-rust/target/<target>/release/: the rlib, and hcert.wasm for wasm32." },
      ],
    },
    satisfies: ["hcert-select-payload", "hcert-assemble-payload", "hcert-build-cwt", "hcert-encode-cbor", "hcert-sign-cose", "hcert-compress", "hcert-encode-base45", "hcert-prefix-context", "hcert-verify", "hcert-trust-list", "hcert-conformance-testing"],
    selection: {
      when: "A Rust program or a web page issues or verifies an HCERT, or a demo shows the QR Code Generation steps live; or the second implementation is checked against the first on the shared vectors.",
      limits: "No VHL payload or JWE yet. The WebAssembly build is a core module with a JSON call surface (js/hcert.mjs), not yet the WIT component jco transpiles (STT-GEN-005). No coverage-guided fuzzing in CI (cargo-fuzz needs nightly); the seeded fuzzer runs instead.",
      cost: "Seconds once the crates are cached; the first build downloads the 63 crates Cargo.lock pins.",
    },
    requires: { runtime: ["cargo", "node"], network: true },
    remedies: [
      { host: "index.crates.io", none: "Every crate is pinned with its checksum in Cargo.lock; a vendored copy (cargo vendor) serves the same bytes offline." },
    ],
  }),
);
