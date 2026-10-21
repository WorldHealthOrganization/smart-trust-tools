# Kotlin libraries

`hcert-api/` holds the **API skeleton**: Kotlin Multiplatform declarations
generated from the WIT contract in [`../api/wit/`](../api/wit/) by
[`../wit2kotlin/wit2kotlin.py`](../wit2kotlin/wit2kotlin.py). It contains interfaces and
types only, no behaviour [STT-GEN-014].

| WIT | Kotlin |
|---|---|
| exported interface (e.g. `codec`, `verifier`) | `interface Codec`, `interface Verifier`, implemented by the library |
| resource (e.g. `trust-store`) | `interface TrustStore : AutoCloseable`, created by `newTrustStore()` on its interface |
| record / enum / variant | `data class` / `enum class` / `sealed interface` |
| `result<T, E>` | `WitResult<T, E>` (`Ok` / `Err`) |
| world (e.g. `hcert-verifier`) | `interface HcertVerifierWorld` plus `fun interface HcertVerifierFactory` taking the world's imports |

Targets: JVM (Java 11+, also used on Android) and Node.js (Kotlin/JS IR, with
TypeScript declarations). An `androidTarget()` is added with the first
Android-specific module.

```sh
python3 tools/wit2kotlin/wit2kotlin.py          # regenerate after editing api/wit
python3 tools/wit2kotlin/wit2kotlin.py --check  # CI: fail if stale
gradle -p tools/hcert-kotlin build               # compile JVM + JS
```

Do not edit files under `hcert-api/src/commonMain`. Implementations go in
separate modules (e.g. `hcert-core`) that depend on `:hcert-api` and
implement its interfaces.

## hcert-core

`hcert-core/` implements every interface of `:hcert-api` (object `Hcert`
is the whole `HcertWorld`) with the Kotlin standard library and the platform
only: the JDK (`java.security`, `java.util.zip`) on JVM and Android, and
`node:crypto`, `node:zlib` and `node:buffer` on Node. No third-party
runtime dependency. Base45, deterministic CBOR, ASN.1 signature conversion and
the small JSON reader for DID documents are written here.

```sh
gradle -p tools/hcert-kotlin :hcert-core:jvmTest                           # JVM tests, incl. the PR 129 vector
gradle -p tools/hcert-kotlin :hcert-core:compileProductionLibraryKotlinJs  # Node build (ES modules)
node tools/hcert-kotlin/hcert-core/node-smoke.mjs                          # the PR 129 vector on Node
```

The tests sign with the throw-away keys in [`../vectors/testkit/`](../vectors/testkit/).
Kotlin/JS uses the machine's Node.js; its npm packages (the mocha runner) come only from
`kotlin-js-store/yarn.lock`, and Gradle artifacts only as listed in
`gradle/verification-metadata.xml`.
