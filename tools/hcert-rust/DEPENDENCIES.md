# hcert-rust dependencies

Every crate the library builds with, from `Cargo.lock`, checked by
`python3 tools/hcert-rust/dep_audit.py`. Each is a release at least 14 days
older than the cutoff (2026-10-07; the audit ran on 2026-10-21), pinned in
`Cargo.lock` with its checksum. CI builds with `--locked`, so nothing is
resolved at build time.

## Why these

- **Cryptography: RustCrypto `p256`, `rsa`, `sha2`.** STT-SEC-002 names these
  crates and forbids the library's own primitives. `x509-cert` (also RustCrypto)
  reads DSCs. Together they bring most of the tree below: `der`, `spki`,
  `pkcs1`, `pkcs8`, `elliptic-curve`, `crypto-bigint`, `num-bigint-dig` and
  their helpers.
- **DEFLATE: `miniz_oxide`.** Pure Rust, with `adler2` as its only dependency.
- **NFC: `unicode-normalization`.** STT-CBR-002 requires NFC normalisation
  of CBOR text. The Kotlin library takes it from the platform.
- **`getrandom` with `custom` on wasm32.** `rsa` and `p256` reach `rand_core`.
  Verification draws no randomness, and signing is the host's, so the
  WebAssembly build registers an entropy source that always fails. It does not
  import JavaScript (the `js` feature, through wasm-bindgen).
- **Live demos only: `qrcode` 0.14.1 (feature `qr`).** This renders the HC1
  string as an SVG QR symbol on the demo page. It sits behind a feature the
  library proper never enables, because the library leaves rendering to the
  host. The owner allowed one vetted, pinned QR encoder for the demos on
  2026-10-21. Why this one:
  - it is the most-used QR encoder on crates.io (23 million downloads);
  - it is MIT or Apache-2.0;
  - it is pure Rust, with no build script;
  - without default features it has no dependencies.

  A test checks it: the PR 129 string encodes as the version 16, level Q
  symbol the page states, which is only possible in Alphanumeric mode.
- **Tests only: `rand_core`.** It is already in the tree; the tests use it for
  a deterministic RNG to sign PS256 with.

Base45, CBOR, COSE, JSON, the trust store and the test DSC builder are
written here and add no crates.

## Build scripts and macros, reviewed

Cargo runs a crate's `build.rs`, and the compiler runs a procedural macro, at
build time. Each one was read at the version pinned here:

| Crate | What its build-time code does |
|---|---|
| `generic-array` 0.14.9 | Sets a cfg flag from the compiler version. 19 lines; no commands. |
| `libc` 0.2.190 | Runs `rustc --version`, and `freebsd-version` or `emcc` on those targets only. Sets cfg flags. |
| `libm` 0.2.16 | Reads one environment variable (`ENSURE_NO_PANIC`) and sets cfg flags. |
| `num-bigint-dig` 0.8.6 | Three lines: sets one cfg flag. |
| `num-traits` 0.2.19 | `autocfg` probes of the compiler. |
| `proc-macro2` 1.0.107, `quote` 1.0.47 | Run `rustc --version` and compile a probe into `OUT_DIR`. They are needed by `der_derive` and `zeroize_derive`. |
| `zerocopy` 0.8.61 | Runs `rustc --version` and sets cfg flags. |
| `der_derive`, `zeroize_derive`, `zerocopy-derive` | Procedural macros from RustCrypto and Google that derive DER, zeroize and zerocopy impls. |

None of them reaches the network or writes outside `OUT_DIR`.

## The pinned set

| Crate | Version | Released | Build-time code |
|---|---|---|---|
| `adler2` | 2.0.1 | 2025-06-09 | — |
| `autocfg` | 1.5.1 | 2026-05-22 | — |
| `base16ct` | 0.2.0 | 2023-02-26 | — |
| `base64ct` | 1.8.3 | 2026-01-12 | — |
| `block-buffer` | 0.10.4 | 2023-03-09 | — |
| `cfg-if` | 1.0.5 | 2026-09-16 | — |
| `const-oid` | 0.9.6 | 2023-12-15 | — |
| `cpufeatures` | 0.2.17 | 2025-01-25 | — |
| `crypto-bigint` | 0.5.5 | 2023-11-18 | — |
| `crypto-common` | 0.1.6 | 2022-07-16 | — |
| `der` | 0.7.10 | 2025-04-18 | — |
| `der_derive` | 0.7.3 | 2024-07-09 | proc-macro |
| `digest` | 0.10.7 | 2023-05-19 | — |
| `ecdsa` | 0.16.9 | 2023-11-16 | — |
| `elliptic-curve` | 0.13.8 | 2023-11-19 | — |
| `ff` | 0.13.1 | 2025-03-09 | — |
| `flagset` | 0.4.7 | 2025-04-04 | — |
| `generic-array` | 0.14.9 | 2025-10-13 | build.rs |
| `getrandom` | 0.2.17 | 2026-01-11 | — |
| `group` | 0.13.0 | 2022-12-06 | — |
| `hmac` | 0.12.1 | 2022-02-17 | — |
| `lazy_static` | 1.5.1 | 2026-10-01 | — |
| `libc` | 0.2.190 | 2026-10-02 | build.rs |
| `libm` | 0.2.16 | 2026-01-24 | build.rs |
| `miniz_oxide` | 0.8.9 | 2025-06-09 | — |
| `num-bigint-dig` | 0.8.6 | 2025-11-12 | build.rs |
| `num-integer` | 0.1.47 | 2026-08-12 | — |
| `num-iter` | 0.1.46 | 2026-07-07 | — |
| `num-traits` | 0.2.19 | 2024-05-03 | build.rs |
| `p256` | 0.13.2 | 2023-04-15 | — |
| `pkcs1` | 0.7.5 | 2023-04-24 | — |
| `pkcs8` | 0.10.2 | 2023-04-05 | — |
| `ppv-lite86` | 0.2.21 | 2025-03-09 | — |
| `primeorder` | 0.13.6 | 2023-11-16 | — |
| `proc-macro2` | 1.0.107 | 2026-07-19 | build.rs |
| `quote` | 1.0.47 | 2026-07-19 | build.rs |
| `rand` | 0.8.8 | 2026-08-25 | — |
| `rand_chacha` | 0.3.1 | 2021-06-09 | — |
| `rand_core` | 0.6.4 | 2022-09-15 | — |
| `rfc6979` | 0.4.0 | 2023-02-28 | — |
| `rsa` | 0.9.10 | 2026-01-06 | — |
| `sec1` | 0.7.3 | 2023-07-16 | — |
| `sha2` | 0.10.9 | 2025-04-30 | — |
| `signature` | 2.2.0 | 2023-11-12 | — |
| `smallvec` | 1.16.2 | 2026-09-25 | — |
| `spin` | 0.9.9 | 2026-07-13 | — |
| `spki` | 0.7.3 | 2023-11-28 | — |
| `subtle` | 2.6.1 | 2024-06-24 | — |
| `syn` | 2.0.119 | 2026-07-15 | — |
| `tinyvec` | 1.13.3 | 2026-09-13 | — |
| `tls_codec` | 0.4.2 | 2025-02-10 | — |
| `tls_codec_derive` | 0.4.2 | 2025-02-10 | proc-macro |
| `typenum` | 1.20.1 | 2026-05-29 | — |
| `unicode-ident` | 1.0.26 | 2026-09-17 | — |
| `unicode-normalization` | 0.1.25 | 2025-10-30 | — |
| `version_check` | 0.9.5 | 2024-07-25 | — |
| `wasi` | 0.11.1+wasi-snapshot-preview1 | 2025-06-10 | — |
| `x509-cert` | 0.2.5 | 2023-12-24 | — |
| `zerocopy` | 0.8.61 | 2026-10-07 | build.rs |
| `zerocopy-derive` | 0.8.61 | 2026-10-07 | proc-macro |
| `zeroize` | 1.9.1 | 2026-10-06 | — |
| `zeroize_derive` | 1.5.0 | 2026-06-12 | proc-macro |
| `qrcode` | 0.14.1 | 2024-07-05 | — (demo feature `qr` only) |
