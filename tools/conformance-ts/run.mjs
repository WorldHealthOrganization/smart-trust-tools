// Runs the conformance suite (suite.ts) against one implementation:
//   node tools/conformance-ts/run.mjs rust   <dir from tools/hcert-rust/build-component.sh>
//   node tools/conformance-ts/run.mjs kotlin <Kotlin/JS productionLibrary dir>
// The loaders only adapt how each package is laid out to the `hcert` world;
// every call the suite makes goes to the library unchanged. Node strips the
// suite's types at load (Node 22.18+); tsconfig.json type-checks it.
import { readFileSync } from "node:fs";
import { createPrivateKey, sign, createVerify } from "node:crypto";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { runSuite } from "./suite.ts";

const [kind, dirArg] = process.argv.slice(2);
const dir = resolve(dirArg ?? "");
const repo = fileURLToPath(new URL("../../", import.meta.url));
const read = (p) => readFileSync(join(repo, p), "utf8");

const loaders = {
  // jco gives one namespace per WIT interface already.
  rust: async () => import(join(dir, "js", "hcert.js")),
  // Kotlin/JS exports each interface as an object behind getInstance(), and
  // the resources as classes; this regroups them under the interface names.
  kotlin: async () => {
    const m = await import(join(dir, "smart-trust-kotlin-hcert-core.mjs"));
    const ns = (o) => {
      const i = o.getInstance();
      const out = {};
      for (const k of Object.getOwnPropertyNames(Object.getPrototypeOf(i))) if (k !== "constructor" && typeof i[k] === "function") out[k] = i[k].bind(i);
      return out;
    };
    return {
      codec: ns(m.WitCodec), cose: ns(m.WitCose), dvc: ns(m.WitDvc), vhl: ns(m.WitVhl), verifier: ns(m.WitVerifier),
      issuer: { ...ns(m.WitIssuer), PendingSignature: m.WitPendingSignature },
      subclaims: { Registry: m.WitRegistry }, trust: { TrustStore: m.WitTrustStore },
    };
  },
};
if (!loaders[kind]) {
  console.error("usage: run.mjs rust|kotlin <dir>");
  process.exit(2);
}

const negative = JSON.parse(read("vectors/negative/negative-vectors.json"));
const pemPaths = new Set([...negative.defaults.trust, ...negative.cases.flatMap((c) => c.trust ?? [])]);
const pemDer = (pem) => new Uint8Array(Buffer.from(pem.split("-----BEGIN CERTIFICATE-----")[1].split("-----END CERTIFICATE-----")[0].replace(/\s/g, ""), "base64"));
const key = createPrivateKey(read("vectors/testkit/es256-key.pem"));
const fixtures = {
  pr129: JSON.parse(read("vectors/smart-trust-pr129/dvc-qr-example-XXA.json")),
  pr129Pem: read("vectors/smart-trust-pr129/dvc-qr-example-XXA-dsc.pem"),
  negative,
  pem: Object.fromEntries([...pemPaths].map((p) => [p, read(p)])),
  trustDomains: read("vectors/gdhcn-trust-domains/CodeSystem-Domains.json"),
  manifestCaseB: read("vectors/ihe-vhl/iti-yy5-manifest-case-b.json"),
  jwe: JSON.parse(read("vectors/ihe-vhl/jwe-dir-a256gcm.json")),
  testkitDsc: pemDer(read("vectors/testkit/es256-dsc.pem")),
  signEs256Der: (data) => new Uint8Array(sign("sha256", data, key)),
  verifyEs256Raw: (data, raw) => createVerify("sha256").update(data).verify({ key, dsaEncoding: "ieee-p1363" }, raw),
};

const outcomes = runSuite(await loaders[kind](), fixtures);
for (const o of outcomes) console.log(`${o.ok ? "ok  " : "FAIL"} ${o.name}${o.detail && !o.ok ? ": " + o.detail : ""}`);
const failed = outcomes.filter((o) => !o.ok).length;
console.log(`${kind}: ${outcomes.length - failed} of ${outcomes.length} passed`);
process.exit(failed ? 1 : 0);
