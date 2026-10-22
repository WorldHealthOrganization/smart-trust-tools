// One conformance suite for both HCERT libraries (STT-TST-007): the Kotlin/JS
// package and the jco-transpiled Rust component. It is type-checked against
// api/generated/ts, the TypeScript that jco derives from the WIT contract, so
// a call or a result that differs from the contract fails tsc before it runs.
// The data comes from the shared vectors (STT-TST-005), passed in by run.mjs.

import type * as codecT from "../../api/generated/ts/interfaces/who-smart-trust-codec.js";
import type * as coseT from "../../api/generated/ts/interfaces/who-smart-trust-cose.js";
import type * as subclaimsT from "../../api/generated/ts/interfaces/who-smart-trust-subclaims.js";
import type * as dvcT from "../../api/generated/ts/interfaces/who-smart-trust-dvc.js";
import type * as vhlT from "../../api/generated/ts/interfaces/who-smart-trust-vhl.js";
import type * as trustT from "../../api/generated/ts/interfaces/who-smart-trust-trust.js";
import type * as verifierT from "../../api/generated/ts/interfaces/who-smart-trust-verifier.js";
import type * as issuerT from "../../api/generated/ts/interfaces/who-smart-trust-issuer.js";
import type { DecodeLimits } from "../../api/generated/ts/interfaces/who-smart-trust-types.js";
import type { DvcMin } from "../../api/generated/ts/interfaces/who-smart-trust-dvc.js";
import type { VhlPayload } from "../../api/generated/ts/interfaces/who-smart-trust-vhl.js";
import type { VerificationResult, VerifierOptions } from "../../api/generated/ts/interfaces/who-smart-trust-verifier.js";

/** The `hcert` world: what each implementation's loader returns. */
export interface Hcert {
  codec: typeof codecT;
  cose: typeof coseT;
  subclaims: Pick<typeof subclaimsT, "Registry">;
  dvc: typeof dvcT;
  vhl: typeof vhlT;
  trust: Pick<typeof trustT, "TrustStore">;
  verifier: typeof verifierT;
  issuer: typeof issuerT;
}

export interface NegativeCase {
  id: string;
  hc1?: string;
  coseHex?: string;
  trust?: string[];
  now?: number;
  expect: { step: string; reason: string };
}

/** The shared vectors, and a signer standing in for the host's key holder. */
export interface Fixtures {
  pr129: { certificates: Record<string, { hc1: string; payload: { n: string } }> };
  pr129Pem: string;
  /** The merged guide's example (WHO af75be2), GDHCN DEV participant XXR: decodes, but its DSC is on the DEV trust list only. */
  /** XXR's DSC DID document from the GDHCN DEV trust list. */
  xxrDscDid: string;
  xxr: { kid_hex: string; certificates: Record<string, { hc1: string; iss: string; iat: number; cwt_cbor_bytes: number; cose_sign1_bytes: number; deflated_bytes: number; hc1_chars: number; payload: { n: string; vx: { vp: string; bo: string } } }> };
  negative: { defaults: { trust: string[]; now: number; clockSkewSeconds: number }; cases: NegativeCase[] };
  pem: Record<string, string>;
  trustDomains: string;
  manifestCaseB: string;
  jwe: { key: string; plaintext: string; jwe: string; jweZipDef: string };
  testkitDsc: Uint8Array;
  /** ES256 over `data` with the testkit key, as DER (as JCA and OpenSSL give it). */
  signEs256Der(data: Uint8Array): Uint8Array;
  /** Verifies a 64-byte r||s ES256 signature with the testkit key. */
  verifyEs256Raw(data: Uint8Array, raw: Uint8Array): boolean;
}

export interface Outcome {
  name: string;
  ok: boolean;
  detail?: string;
}

export const L: DecodeLimits = { maxInputChars: 8192, maxInflatedBytes: 65536, maxDepth: 16, maxItems: 4096, maxStringBytes: 16384 };
export const OPTIONS: VerifierOptions = { limits: L, clockSkewSeconds: 300 };
const NOW = 1792454400n; // 2026-10-20T00:00:00Z
export const TESTKIT_NOW = 1792520192n + 3600n;

export const hex = (b: Uint8Array) => Array.from(b, (x) => x.toString(16).padStart(2, "0")).join("");
const fromHex = (h: string) => new Uint8Array(h.match(/../g)?.map((x) => parseInt(x, 16)) ?? []);
const text = (b: Uint8Array) => new TextDecoder().decode(b);

/** The error variant a call threw, as both libraries throw it: `{ payload: { tag, val } }`. */
function thrownTag(f: () => unknown): string | undefined {
  try {
    f();
    return undefined;
  } catch (e) {
    return (e as { payload?: { tag?: string } }).payload?.tag;
  }
}

export const newborn: DvcMin = {
  n: "Test CHILD", dob: "2026-09-28", s: "female", nt: "LAO", gn: "Test MOTHER",
  vx: { vp: "XM9V38", dv: "2026-09-28", is: "org-test", bo: "HEPB-TEST" }, v: "1.0.0", extensions: [],
};

export const ihe: VhlPayload = {
  url: "https://vhl-sharer.example.org/List/_search?_id=abc123def456&code=folder&status=current&patient.identifier=urn:oid:2.16.840.1.113883.2.4.6.3|PASSPORT123&_include=List:item",
  key: "86F8LY5LlWAa1-OS_FgrTnYNqFHJP2ey5RSKLJBN9jk", exp: 1735689600n, flag: "LP", label: "Patient Health Summary", v: 1,
  fhirBaseUrl: "https://vhl-sharer.example.org",
};

export function runSuite(h: Hcert, f: Fixtures): Outcome[] {
  const out: Outcome[] = [];
  const check = (name: string, ok: boolean, detail?: string) => out.push({ name, ok, detail });
  const run = (name: string, body: () => boolean | [boolean, string]) => {
    try {
      const r = body();
      Array.isArray(r) ? check(name, r[0], r[1]) : check(name, r);
    } catch (e) {
      check(name, false, `threw ${(e as Error).message}`);
    }
  };
  const hepb1 = f.pr129.certificates.hepb1.hc1;

  // codec (steps 4, 6, 7, 8)
  run("STT-HC1-001 STT-B45-001 STT-ZIP-001 the PR 129 HC1 string decodes to the 295-byte COSE_Sign1", () => h.codec.fromHc1(hepb1, L).length === 295);
  run("STT-HC1-001 the context identifier is read and other versions refused", () => {
    const [v, rest] = h.codec.detectContext("HC1:XYZ");
    return v === "1" && rest === "XYZ" && thrownTag(() => h.codec.fromHc1("HC2:00", L)) === "unsupported-context-version" && thrownTag(() => h.codec.detectContext("XYZ")) === "bad-prefix";
  });
  run("STT-B45-001 STT-B45-002 Base45: RFC 9285 examples, and strict decoding", () =>
    h.codec.base45Encode(new TextEncoder().encode("Hello!!")) === "%69 VD92EX0" && text(h.codec.base45Decode("UJCLQE7W581")) === "base-45" && thrownTag(() => h.codec.base45Decode("abc")) === "base45-invalid");
  run("STT-ZIP-001 STT-ZIP-002 ZLIB round-trips, and inflation stops at the limit", () => {
    const z = h.codec.zlibCompress(new Uint8Array(10000).fill(7), 9);
    return h.codec.zlibInflate(z, 10000).length === 10000 && thrownTag(() => h.codec.zlibInflate(z, 9999)) === "limit-exceeded";
  });
  run("STT-QR-001 to-hc1 inverts from-hc1, in the Alphanumeric alphabet", () => {
    const c = h.codec.fromHc1(hepb1, L);
    const back = h.codec.toHc1(c);
    return /^HC1:[0-9A-Z $%*+\-./:]+$/.test(back) && hex(h.codec.fromHc1(back, L)) === hex(c);
  });
  run("STT-CBR-001 deterministic re-encoding: RFC 8949 Appendix A, and the PR 129 CWT byte for byte", () => {
    const det = (x: string) => hex(h.codec.cborDeterministic(fromHex(x), L));
    const s = h.cose.parseSign1(h.codec.fromHc1(hepb1, L), L);
    return det("fb3ff0000000000000") === "f93c00" && det("bf6346756ef563416d7421ff") === "a263416d74216346756ef5" && hex(h.codec.cborDeterministic(s.payload, L)) === hex(s.payload);
  });
  run("STT-CBR-003 nesting beyond the limit is refused", () => thrownTag(() => h.codec.cborDeterministic(new Uint8Array([...new Array(40).fill(0x81), 0]), L)) === "limit-exceeded");

  run("STT-TST-011 STT-TST-002 the merged guide's XXR certificates decode as published, at the published sizes", () => {
    const store = new h.trust.TrustStore();
    store.addPem(f.pr129Pem);
    return Object.entries(f.xxr.certificates).every(([, c]) => {
      const u = h.verifier.readUnverified(c.hc1, new h.subclaims.Registry(), L);
      const cose = h.codec.fromHc1(c.hc1, L);
      const s = h.cose.parseSign1(cose, L);
      const r = h.verifier.verify(c.hc1, store, new h.subclaims.Registry(), NOW, OPTIONS);
      return hex(u.kid ?? new Uint8Array()) === f.xxr.kid_hex && u.iss === c.iss && u.entries.length === 1 && u.entries[0]!.subclaim === -6 &&
        u.entries[0]!.payload?.tag === "dvc-min" && u.entries[0]!.payload.val.n === c.payload.n && u.entries[0]!.payload.val.vx.vp === c.payload.vx.vp && u.entries[0]!.payload.val.vx.bo === c.payload.vx.bo &&
        hex(h.codec.cborDeterministic(s.payload, L)) === hex(s.payload) && s.payload.length === c.cwt_cbor_bytes && cose.length === c.cose_sign1_bytes &&
        h.codec.base45Decode(c.hc1.slice(4)).length === c.deflated_bytes && c.hc1.length === c.hc1_chars &&
        r.tag === "failed" && r.val.step === "trust-lookup" && r.val.reason === "kid-not-found";
    });
  });

  run("STT-TST-011 the merged guide's XXR certificates verify against the DEV trust list's DSC", () => {
    const dev = new h.trust.TrustStore();
    if (dev.addDidDocument(f.xxrDscDid) !== 1) return [false, "the DID document gave no DSC"];
    return Object.entries(f.xxr.certificates).every(([, c]) => {
      const r = h.verifier.verify(c.hc1, dev, new h.subclaims.Registry(), BigInt(c.iat) + 3600n, OPTIONS);
      return r.tag === "verified" && r.val.iss === "XR" && r.val.dsc.country === "XR" && hex(r.val.dsc.kid) === f.xxr.kid_hex &&
        r.val.entries.length === 1 && r.val.entries[0]!.keyUsageAuthorised && r.val.entries[0]!.payload.tag === "dvc-min" &&
        r.val.entries[0]!.payload.val.vx.vp === c.payload.vx.vp && r.val.diagnostics.length === 0;
    });
  });

  // cose (step 5)
  run("STT-COS-001 STT-COS-002 STT-COS-003 STT-KID-001 the COSE_Sign1 is tagged ES256 with the vector's kid in the protected header", () => {
    const s = h.cose.parseSign1(h.codec.fromHc1(hepb1, L), L);
    return [s.tagged && s.alg === "es256" && s.kidInProtected && s.kid !== undefined && hex(s.kid) === "58733e058aaa6621", hex(s.kid ?? new Uint8Array())];
  });
  run("STT-KID-003 kid base64, standard and URL-safe", () => {
    const k = fromHex("58733e058aaa6621");
    return h.cose.kidToBase64(k, "standard") === "WHM+BYqqZiE=" && h.cose.kidToBase64(k, "url-safe").startsWith("WHM-BYqqZiE") && hex(h.cose.kidFromBase64("WHM-BYqqZiE")) === hex(k);
  });
  run("STT-COS-004 STT-COS-002 verify-sign1 checks the Sig_structure against the DSC", () => {
    const s = h.cose.parseSign1(h.codec.fromHc1(hepb1, L), L);
    const store = new h.trust.TrustStore();
    store.addPem(f.pr129Pem);
    const dsc = store.candidates(s.kid ?? new Uint8Array())[0]!.der;
    h.cose.verifySign1(s, dsc);
    const bad = { ...s, signature: s.signature.map((b, i) => (i === 0 ? b ^ 1 : b)) };
    return thrownTag(() => h.cose.verifySign1(bad, dsc)) === "signature-invalid" && h.cose.sigStructure(s.protected, s.payload)[0] === 0x84;
  });
  run("STT-COS-005 ECDSA DER and r||s convert both ways", () => {
    const der = f.signEs256Der(new Uint8Array([1, 2, 3]));
    const raw = h.cose.ecdsaDerToRaw(der);
    return raw.length === 64 && hex(h.cose.ecdsaDerToRaw(h.cose.ecdsaRawToDer(raw))) === hex(raw) && thrownTag(() => h.cose.ecdsaRawToDer(new Uint8Array(10))) === "bad-signature-format";
  });

  // subclaims
  run("STT-SEL-001 STT-SEL-002 the registry starts with the guide's subclaims, and is configuration", () => {
    const reg = new h.subclaims.Registry();
    const keys = reg.all().map((s) => s.key).join();
    const dvc = reg.subclaimFor("dvc-min");
    reg.setSubclaimFor("dvc-min", -99);
    reg.register({ key: -99, title: "DVCMin moved" });
    return [keys === "1,3,4,5,-6,-7" && dvc === -6 && reg.subclaimFor("vhl") === 5 && reg.subclaimFor("dvc-min") === -99 && reg.lookup(-99)?.title === "DVCMin moved", keys];
  });

  // dvc (step 2)
  run("STT-PAY-001 STT-PAY-008 a DVCMin round-trips through CBOR", () => h.dvc.fromCbor(h.dvc.toCbor(newborn), L).gn === "Test MOTHER" && h.dvc.validate(newborn).length === 0);
  run("STT-PAY-002 STT-PAY-007 the logical model's rules are enforced", () => {
    const codes = h.dvc.validate({ ...newborn, nt: "LA", vx: { ...newborn.vx, is: undefined } }).map((i) => i.code);
    return [codes.includes("not-alpha3") && codes.includes("must-have-issuer-or-clinician-name"), codes.join()];
  });

  // trust
  run("STT-TRU-005 STT-KID-002 PEM loads once; candidates by kid", () => {
    const st = new h.trust.TrustStore();
    const n = st.addPem(f.pr129Pem);
    return n === 2 && st.addPem(f.pr129Pem) === 0 && st.size() === 2 && st.candidates(fromHex("58733e058aaa6621")).length === 1 && thrownTag(() => st.addPem("nothing")) === "parse";
  });
  run("STT-SEL-006 trust domains come from the GDHCN code system", () => {
    const st = new h.trust.TrustStore();
    const n = st.loadTrustDomains(f.trustDomains);
    const did = (d: string) => JSON.stringify({ id: `did:web:x:v2:trustlist:${d}:XX:DSC`, verificationMethod: [{ id: `did:web:x:v2:trustlist:${d}:XX:DSC#k`, publicKeyJwk: { x5c: [btoa(String.fromCharCode(...f.testkitDsc))] } }] });
    return n === 4 && st.trustDomains().join() === "DDCC,ICVP,IPS-PILGRIMAGE,PH4H" && thrownTag(() => st.addDidDocument(did("FUTURE"))) === "unknown-trust-domain" && st.addDidDocument(did("DDCC")) === 1 && st.candidates(h.cose.kidFromDsc(f.testkitDsc))[0]?.domain === "DDCC";
  });

  // verifier (section 11)
  const reg = new h.subclaims.Registry();
  const store = new h.trust.TrustStore();
  store.addPem(f.pr129Pem);
  for (const [name, c] of Object.entries(f.pr129.certificates)) {
    run(`STT-TST-001 STT-VER-001 the PR 129 ${name} certificate verifies as published`, () => {
      const r = h.verifier.verify(c.hc1, store, reg, NOW, OPTIONS);
      return r.tag === "verified" && r.val.iss === "XX" && r.val.entries.length === 1 && r.val.entries[0]!.subclaim === -6 && r.val.entries[0]!.payload.tag === "dvc-min" && r.val.entries[0]!.payload.val.n === c.payload.n && r.val.dsc.notBefore < NOW;
    });
  }
  run("STT-VER-011 read-unverified decodes and says it is not verified", () => {
    const u = h.verifier.readUnverified(hepb1, reg, L);
    return u.diagnostics[0]!.message.startsWith("NOT VERIFIED") && u.entries[0]?.subclaim === -6 && u.iss === "XX" && typeof u.iat === "bigint";
  });
  const negative = f.negative;
  for (const c of negative.cases) {
    run(`STT-TST-004 STT-TST-005 STT-VER-008 negative vector ${c.id} stops at ${c.expect.step} ${c.expect.reason}`, () => {
      const st = new h.trust.TrustStore();
      for (const p of c.trust ?? negative.defaults.trust) st.addPem(f.pem[p]!);
      const now = BigInt(c.now ?? negative.defaults.now);
      const options: VerifierOptions = { limits: L, clockSkewSeconds: negative.defaults.clockSkewSeconds };
      const r: VerificationResult = c.coseHex ? h.verifier.verifyCose(fromHex(c.coseHex), st, reg, now, options) : h.verifier.verify(c.hc1!, st, reg, now, options);
      const want = [c.expect.step, c.expect.reason].map((x) => x.toLowerCase().replace(/_/g, "-"));
      return r.tag === "failed" ? [r.val.step === want[0] && r.val.reason === want[1], `${r.val.step} ${r.val.reason}`] : [false, "verified"];
    });
  }

  // issuer (two-phase, the host signs)
  run("STT-ISS-004 STT-COS-005 two-phase issuance with a host DER signature verifies", () => {
    const p = h.issuer.prepare({ payloads: [{ tag: "dvc-min", val: newborn }], iss: "XX", validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n }, dscDer: f.testkitDsc, alg: "es256", allowDevelopmentSubclaims: true }, reg);
    // finish takes the pending signature by ownership (WIT `own`): read it first.
    const alg = p.algorithm();
    const kid = hex(p.kid());
    const issued = h.issuer.finish(p, f.signEs256Der(p.toBeSigned()));
    const st = new h.trust.TrustStore();
    st.addDer(f.testkitDsc);
    const r = h.verifier.verify(issued.hc1, st, reg, TESTKIT_NOW, OPTIONS);
    return alg === "es256" && kid === hex(h.cose.kidFromDsc(f.testkitDsc)) && r.tag === "verified" && r.val.entries[0]?.payload.tag === "dvc-min" && r.val.entries[0].payload.val.n === "Test CHILD";
  });
  run("STT-SEL-003 STT-CWT-004 development subclaims need opting in; validity must sit inside the DSC", () => {
    const req = { payloads: [{ tag: "dvc-min" as const, val: newborn }], validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n }, dscDer: f.testkitDsc, alg: "es256" as const, allowDevelopmentSubclaims: false };
    return thrownTag(() => h.issuer.prepare(req, reg)) === "development-subclaim-not-allowed" &&
      thrownTag(() => h.issuer.prepare({ ...req, allowDevelopmentSubclaims: true, validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n * 365n * 20n } }, reg)) === "validity-outside-dsc";
  });
  run("STT-COS-006 finish refuses a signature from another key", () => {
    const p = h.issuer.prepare({ payloads: [{ tag: "dvc-min", val: newborn }], validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n }, dscDer: f.testkitDsc, alg: "es256", allowDevelopmentSubclaims: true }, reg);
    return thrownTag(() => h.issuer.finish(p, f.signEs256Der(new Uint8Array([9])))) === "bad-signature-format";
  });

  // vhl
  run("STT-VHL-001 STT-VHL-003 the IHE example payload is valid and round-trips through vhlink:/", () =>
    h.vhl.validate(ihe, undefined).length === 0 && h.vhl.fromVhlink(h.vhl.toVhlink(ihe)).key === ihe.key && h.vhl.validate({ ...ihe, flag: "PL" }, undefined).map((i) => i.code).join() === "bad-flag" && thrownTag(() => h.vhl.fromVhlink("shlink:/x")) === "not-a-vhlink");
  run("STT-VHL-002 a VHL is issued as a CBOR map at key 5 and verifies", () => {
    const payload = { ...ihe, exp: TESTKIT_NOW + 86400n };
    const p = h.issuer.prepare({ payloads: [{ tag: "vhl", val: payload }], iss: "XX", validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n }, dscDer: f.testkitDsc, alg: "es256", allowDevelopmentSubclaims: true }, reg);
    const issued = h.issuer.finish(p, f.signEs256Der(p.toBeSigned()));
    const st = new h.trust.TrustStore();
    st.addDer(f.testkitDsc);
    const r = h.verifier.verify(issued.hc1, st, reg, TESTKIT_NOW, OPTIONS);
    return r.tag === "verified" && r.val.entries[0]?.subclaim === 5 && r.val.entries[0].payload.tag === "vhl" && r.val.entries[0].payload.val.exp === payload.exp;
  });
  run("STT-VHL-004 new keys are 32 random bytes, base64url", () => {
    const a = h.vhl.newKey();
    return a.length === 43 && a !== h.vhl.newKey() && /^[A-Za-z0-9_-]+$/.test(a);
  });
  run("STT-VHL-005 the ITI-YY5 request is the exact body of the IHE example", () =>
    h.vhl.buildManifestRequest(ihe, "Dr. Smith Hospital", "user-pin", 10000).body ===
      "_id=abc123def456&code=folder&status=current&patient.identifier=urn%3Aoid%3A2.16.840.1.113883.2.4.6.3%7CPASSPORT123&_include=List%3Aitem&recipient=Dr.+Smith+Hospital&passcode=user-pin&embeddedLengthMax=10000" &&
    thrownTag(() => h.vhl.buildManifestRequest(ihe, "Clinic B", undefined, undefined)) === "manifest");
  run("STT-VHL-006 the IHE Case B manifest Bundle reads as documents", () => {
    const d = h.vhl.parseManifest(f.manifestCaseB);
    return d.length === 4 && d[0]!.url === "https://vhl-sharer.example.org/Binary/doc001-content" && d[1]!.reference === "DocumentReference/doc002";
  });
  run("STT-VHL-007 JWE dir/A256GCM: the node:crypto fixture decrypts, and a round trip refuses the wrong key", () => {
    const ok = [f.jwe.jwe, f.jwe.jweZipDef].every((j) => text(h.vhl.decrypt(j, f.jwe.key).content) === f.jwe.plaintext);
    const k = h.vhl.newKey();
    const jwe = h.vhl.encrypt(new TextEncoder().encode("hello"), "text/plain", k);
    return ok && text(h.vhl.decrypt(jwe, k).content) === "hello" && h.vhl.decrypt(jwe, k).contentType === "text/plain" && thrownTag(() => h.vhl.decrypt(jwe, h.vhl.newKey())) === "decrypt-failed";
  });
  run("STT-VHL-008 RFC 9421 signature base, RFC 9530 digest, and a DER signature as the raw header value", () => {
    const b = h.vhl.buildSignatureBase({ method: "POST", authority: "vhl-sharer.example.org", path: "/List/_search", contentType: "application/x-www-form-urlencoded", body: new TextEncoder().encode('{"hello": "world"}'), keyid: "receiver-key-123", alg: "ecdsa-p256-sha256", created: 1735689600n });
    const header = h.vhl.signatureHeader(f.signEs256Der(b.base), "ecdsa-p256-sha256");
    const raw = Uint8Array.from(atob(header.slice("sig1=:".length, -1)), (c) => c.charCodeAt(0));
    return b.contentDigest === "sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:" && b.signatureInput.startsWith('sig1=("@method" "@path" "@authority" "content-type" "content-digest");created=1735689600') && raw.length === 64 && f.verifyEs256Raw(b.base, raw);
  });
  run("STT-VHL-009 an expired link fails verification by its own exp", () => {
    const payload = { ...ihe, exp: TESTKIT_NOW - 1000n };
    const p = h.issuer.prepare({ payloads: [{ tag: "vhl", val: payload }], validity: { iat: TESTKIT_NOW, exp: TESTKIT_NOW + 86400n }, dscDer: f.testkitDsc, alg: "es256", allowDevelopmentSubclaims: true }, reg);
    const issued = h.issuer.finish(p, f.signEs256Der(p.toBeSigned()));
    const st = new h.trust.TrustStore();
    st.addDer(f.testkitDsc);
    const r = h.verifier.verify(issued.hc1, st, reg, TESTKIT_NOW, OPTIONS);
    return r.tag === "failed" && r.val.reason === "expired";
  });
  return out;
}
