// Runs the compiled Kotlin/JS library on Node against the PR 129 vector:
// the same checks as the JVM tests' Pr129VectorTest, without a test framework.
//   gradle -p kotlin :hcert-core:compileProductionLibraryKotlinJs && node kotlin/hcert-core/node-smoke.mjs
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const repo = join(here, "..", "..");
const HcertNode = (await import(join(here, "build/compileSync/js/main/productionLibrary/kotlin/smart-trust-kotlin-hcert-core.mjs"))).HcertNode.getInstance();

const vector = JSON.parse(readFileSync(join(repo, "vectors/smart-trust-pr129/dvc-qr-example-XXA.json"), "utf8"));
const pem = readFileSync(join(repo, "vectors/smart-trust-pr129/dvc-qr-example-XXA-dsc.pem"), "utf8");
const NOW = 1792454400; // 2026-10-20T00:00:00Z

let failures = 0;
function expect(name, got, want) {
  const ok = Object.entries(want).every(([k, v]) => got[k] === v);
  console.log(`${ok ? "ok  " : "FAIL"} ${name}: ${JSON.stringify(got)}`);
  if (!ok) failures++;
}
for (const name of ["hepb1", "bcg"]) {
  const hc1 = vector.certificates[name].hc1;
  expect(`${name} verifies`, JSON.parse(HcertNode.verifySummary(hc1, pem, NOW)),
    { status: "verified", subclaim: -6, iss: "XX", payload: "dvc-min", kid: "58733e058aaa6621" });
  expect(`${name} expired`, JSON.parse(HcertNode.verifySummary(hc1, pem, 1822206600 + 301)), { status: "failed", reason: "EXPIRED" });
  expect(`${name} unknown kid`, JSON.parse(HcertNode.verifySummary(hc1, "", NOW)), { status: "failed", reason: "KID_NOT_FOUND" });
}
expect("bad prefix", JSON.parse(HcertNode.verifySummary("HC2:00", pem, NOW)), { status: "failed", reason: "UNSUPPORTED_CONTEXT_VERSION" });
process.exit(failures ? 1 : 0);
