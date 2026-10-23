//! Skill `hcert-trust-list`: each test named after the requirements it verifies.

mod common;
#[allow(unused_imports)]
use common::*;
#[allow(unused_imports)]
use hcert::cbor::{self, Item};
#[allow(unused_imports)]
use hcert::{codec, cose, issuer, payload, testdsc, verifier};
#[allow(unused_imports)]
use hcert::payload::Payload;
#[allow(unused_imports)]
use hcert::testdsc::TestDscRequest;
#[allow(unused_imports)]
use hcert::trust::TrustStore;
#[allow(unused_imports)]
use hcert::types::*;

#[test]
fn stt_tru_005_stt_kid_002_pem_loads_once_and_candidates_are_found_by_kid() {
    let mut s = TrustStore::new();
    let pem = read(PR129_DSC);
    assert_eq!(s.add_pem(&pem).unwrap(), 2);
    assert_eq!(s.add_pem(&pem).unwrap(), 0);
    assert!(matches!(s.add_pem("nothing"), Err(TrustError::Parse(_))));
    assert!(matches!(s.add_der(&[0x30, 0x03, 0x02, 0x01, 0x01]), Err(TrustError::Parse(_))));
}

fn did(domain: &str, der: &[u8]) -> String {
    format!(
        r#"{{"id":"did:web:tng-cdn.who.int:v2:trustlist:{domain}:XX:DSC","verificationMethod":[{{"id":"did:web:tng-cdn.who.int:v2:trustlist:{domain}:XX:DSC#k","publicKeyJwk":{{"x5c":["{}"]}}}}]}}"#,
        cose::base64_encode(der, false)
    )
}

#[test]
fn stt_tru_002_stt_sel_006_did_documents_load_and_trust_domains_come_from_the_code_system() {
    let der = es_der();
    let mut s = TrustStore::new();
    assert_eq!(s.add_did_document(&did("FUTURE", &der)).unwrap(), 1, "before loading, any domain is kept");
    assert_eq!(s.candidates(&kid_of(&der))[0].trust_domain.as_deref(), Some("FUTURE"));
    let mut s = TrustStore::new();
    assert_eq!(s.load_trust_domains(&read("vectors/gdhcn-trust-domains/CodeSystem-Domains.json")).unwrap(), 4);
    assert_eq!(s.trust_domains(), vec!["DDCC", "ICVP", "IPS-PILGRIMAGE", "PH4H"]);
    assert_eq!(s.add_did_document(&did("FUTURE", &der)), Err(TrustError::UnknownTrustDomain("FUTURE".into())));
    assert_eq!(s.add_did_document(&did("DDCC", &der)).unwrap(), 1);
    assert_eq!(s.add_did_document(&did("-", &ps_der())).unwrap(), 1, "the wildcard is allowed");
    assert!(matches!(s.load_trust_domains(r#"{"resourceType":"ValueSet"}"#), Err(TrustError::Parse(_))));
}

const DEV: &str = "vectors/gdhcn-dev-trustlist";

fn who_signer() -> String {
    read(&format!("{DEV}/WHO-signing-did.json"))
}

fn proof_err(r: Result<(), TrustError>) -> String {
    match r {
        Err(TrustError::ProofInvalid(m)) => m,
        other => panic!("expected ProofInvalid, got {other:?}"),
    }
}

/// `doc` with its proof's JWS signature replaced by `f` of its bytes.
fn resign(doc: &str, f: impl Fn(&[u8]) -> Vec<u8>) -> String {
    let at = doc.find("..").unwrap() + 2;
    let end = at + doc[at..].find('"').unwrap();
    let sig = cose::base64_decode(&doc[at..end]).unwrap();
    let new = cose::base64_encode(&f(&sig), true).trim_end_matches('=').to_owned();
    format!("{}{}{}", &doc[..at], new, &doc[end..])
}

#[test]
fn stt_tru_006_canonical_n_quads_equal_the_golden_files() {
    let doc = read(XXR_DSC_DID);
    assert_eq!(hcert::didproof::canonical_document(&doc).unwrap(), read(&format!("{DEV}/XXR-DSC-did.doc.nq")));
    assert_eq!(hcert::didproof::canonical_proof_options(&doc).unwrap(), read(&format!("{DEV}/XXR-DSC-did.proof-options.nq")));
}

#[test]
fn stt_tru_006_dev_trust_list_documents_verify_against_who_signing_did() {
    let signer = who_signer();
    for f in [XXR_DSC_DID.to_owned(), format!("{DEV}/XXR-DSC-did-2026-10-23.json"), format!("{DEV}/XXR-did.json"), format!("{DEV}/XXA-did.json"), format!("{DEV}/root-did.json")] {
        let t = std::time::Instant::now();
        hcert::didproof::verify_did_proof(&read(&f), &signer).unwrap_or_else(|e| panic!("{f}: {e}"));
        eprintln!("{f}: verified in {:?}", t.elapsed());
    }
}

#[test]
fn stt_tru_006_a_changed_byte_the_wrong_signer_an_unmodelled_term_or_header_is_proof_invalid() {
    let doc = read(XXR_DSC_DID);
    let signer = who_signer();
    let verify = |d: &str, s: &str| hcert::didproof::verify_did_proof(d, s);
    verify(&doc, &signer).unwrap();

    // One changed byte in an x5c certificate.
    let at = doc.find("\"x5c\":[\"MIIC").unwrap() + 20;
    let flipped = format!("{}{}{}", &doc[..at], if &doc[at..at + 1] == "A" { "B" } else { "A" }, &doc[at + 1..]);
    assert!(proof_err(verify(&flipped, &signer)).contains("does not verify"));

    // Another P-256 key under the same DID.
    let point = p256::ecdsa::SigningKey::from_slice(&[7; 32]).unwrap().verifying_key().to_encoded_point(false);
    let b64 = |b: &[u8]| cose::base64_encode(b, true).trim_end_matches('=').to_owned();
    let other = format!(r#"{{"kty":"EC","crv":"P-256","x":"{}","y":"{}"}}"#, b64(point.x().unwrap()), b64(point.y().unwrap()));
    let wrong = format!(
        r#"{{"id":"{id}","verificationMethod":[{{"id":"{id}","type":"JsonWebKey2020","controller":"{id}","publicKeyJwk":{other}}}]}}"#,
        id = "did:web:raw.githubusercontent.com:WorldHealthOrganization:tng-participants-dev:main:WHO:signing:DID"
    );
    let m = proof_err(verify(&doc, &wrong));
    assert!(m.contains("does not verify"), "{m}");
    // A signer document of another DID, or without the method.
    assert!(proof_err(verify(&doc, &wrong.replace("main:WHO", "main:XXX"))).contains("not the DID"));
    assert!(proof_err(verify(&doc, r#"{"id":"did:web:raw.githubusercontent.com:WorldHealthOrganization:tng-participants-dev:main:WHO:signing:DID","verificationMethod":[]}"#)).contains("not exactly one"));

    // Unmodelled keys, anywhere.
    assert!(proof_err(verify(&doc.replacen("\"id\":", "\"alsoKnownAs\":\"did:web:x\",\"id\":", 1), &signer)).contains("alsoKnownAs"));
    assert!(proof_err(verify(&doc.replacen("\"type\":\"JsonWebKey2020\"", "\"type\":\"JsonWebKey2020\",\"service\":[]", 1), &signer)).contains("service"));
    assert!(proof_err(verify(&doc.replacen("\"nonce\"", "\"expires\":\"2030-01-01T00:00:00Z\",\"nonce\"", 1), &signer)).contains("expires"));
    assert!(proof_err(verify(&doc.replacen("\"kty\":\"EC\"", "\"kty\":\"EC\",\"ext\":true", 1), &signer)).contains("ext"));
    assert!(proof_err(verify(&doc.replacen("{\"code\":\"#PH4H\"}", "{\"code\":\"#PH4H\",\"display\":\"x\"}", 1), &signer)).contains("display"));
    assert!(proof_err(verify(&doc.replace("v1-DEV.jsonld", "v2.jsonld"), &signer)).contains("@context"));

    // A JWS header other than {"b64":false,"crit":["b64"],"alg":"ES256"}.
    let h = "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzI1NiJ9";
    for other in [r#"{"b64":true,"crit":["b64"],"alg":"ES256"}"#, r#"{"b64":false,"crit":["b64"],"alg":"ES384"}"#, r#"{"b64":false,"alg":"ES256"}"#, r#"{"b64":false,"crit":["b64"],"alg":"ES256","kid":"x"}"#] {
        let enc = cose::base64_encode(other.as_bytes(), true).trim_end_matches('=').to_owned();
        assert!(proof_err(verify(&doc.replace(h, &enc), &signer)).contains("JWS header"), "{other}");
    }
    assert!(proof_err(verify(&doc.replace(&format!("{h}.."), &format!("{h}.e30.")), &signer)).contains("detached"));
    assert!(proof_err(verify("[]", &signer)).contains("not a JSON object"));
}

#[test]
fn stt_tru_006_a_64_byte_r_s_signature_also_verifies() {
    let doc = read(XXR_DSC_DID);
    let raw = resign(&doc, |der| cose::ecdsa_der_to_raw(der).unwrap().to_vec());
    hcert::didproof::verify_did_proof(&raw, &who_signer()).unwrap();
    let truncated = resign(&doc, |der| der[..der.len() - 1].to_vec());
    proof_err(hcert::didproof::verify_did_proof(&truncated, &who_signer()));
}

#[test]
fn stt_tru_006_add_verified_did_document_adds_only_verified_documents() {
    let signer = who_signer();
    let doc = read(&format!("{DEV}/XXR-did.json"));
    let mut s = TrustStore::new();
    assert!(matches!(s.add_verified_did_document(&doc.replacen("\"kty\"", "\"ext\":\"1\",\"kty\"", 1), &signer), Err(TrustError::ProofInvalid(_))));
    assert!(s.is_empty(), "nothing is added on failure");
    assert_eq!(s.add_verified_did_document(&doc, &signer).unwrap(), 2);
    assert_eq!(s.len(), 2);
    assert_eq!(s.add_verified_did_document(&read(XXR_DSC_DID), &signer).unwrap(), 0, "already held through XXR");
}

#[test]
fn stt_tru_006_signer_coordinates_and_profile_variants() {
    let doc = read(XXR_DSC_DID);
    let signer = who_signer();
    // A Java BigInteger sign byte on a coordinate is tolerated; other widths are not.
    let x = "mB9T6oJ57ZKlC1f150-34M3JkxN22Ld1BQVPqJfgH6k";
    let mut wide = vec![0u8];
    wide.extend(cose::base64_decode(x).unwrap());
    let b64 = |b: &[u8]| cose::base64_encode(b, true).trim_end_matches('=').to_owned();
    hcert::didproof::verify_did_proof(&doc, &signer.replace(x, &b64(&wide))).unwrap();
    wide.insert(0, 0);
    assert!(proof_err(hcert::didproof::verify_did_proof(&doc, &signer.replace(x, &b64(&wide)))).contains("32 bytes"));
    assert!(proof_err(hcert::didproof::verify_did_proof(&doc, &signer.replace("P-256", "P-384"))).contains("P-256"));

    // The reference variant, and the UAT and production tng-contexts.
    let reference = r#"{"@context":["https://www.w3.org/ns/did/v1","https://w3id.org/security/suites/jws-2020/v1","https://smart.who.int/trust/tng-context/v1.jsonld"],"id":"did:web:tng-cdn.who.int:v2:trustlist-ref","controller":"did:web:tng-cdn.who.int:v2:trustlist-ref","verificationMethod":["did:web:tng-cdn.who.int:v2:trustlist-ref:-:XXA","did:web:tng-cdn.who.int:v2:trustlist-ref:-:XXA"]}"#;
    assert_eq!(
        hcert::didproof::canonical_document(reference).unwrap(),
        "<did:web:tng-cdn.who.int:v2:trustlist-ref> <https://w3id.org/security#controller> <did:web:tng-cdn.who.int:v2:trustlist-ref> .\n\
         <did:web:tng-cdn.who.int:v2:trustlist-ref> <https://w3id.org/security#verificationMethod> <did:web:tng-cdn.who.int:v2:trustlist-ref:-:XXA> .\n"
    );
    let uat = hcert::didproof::canonical_document(&doc.replace("v1-DEV.jsonld", "v1-UAT.jsonld")).unwrap();
    assert!(uat.contains("<https://smart.who.int/trust/ValueSet-Domains-UAT.jsonld> <https://smart.who.int/trust/ValueSet-Domains-UAT.jsonld#PH4H>"));
    let prod = hcert::didproof::canonical_document(&doc.replace("v1-DEV.jsonld", "v1.jsonld")).unwrap();
    assert!(prod.contains("<https://smart.who.int/trust/ValueSet-KeyUsage.jsonld#DSC>"));
    // What JSON-LD would drop or rewrite is refused: relative IRIs, codes that are not fragments, controls in literals.
    for (from, to) in [("\"id\":\"did:web:tng-cdn-dev", "\"id\":\"tng-cdn-dev"), ("\"#PH4H\"", "\"PH4H\""), ("\"#DSC\"", "\"#\""), ("\"type\":\"JsonWebKey2020\"", "\"type\":\"Ed25519VerificationKey2020\""), ("\"kty\":\"EC\"", "\"kty\":7")] {
        proof_err(hcert::didproof::verify_did_proof(&doc.replacen(from, to, 1), &signer));
    }
    for (from, to) in [("\"z9n9", "\"\\u0001z9n9"), ("\"assertionMethod\"", "\"authentication\""), ("\"JsonWebSignature2020\"", "\"Ed25519Signature2020\""), ("\"verificationMethod\":\"did:web:raw", "\"verificationMethod\":\"web:raw")] {
        proof_err(hcert::didproof::canonical_proof_options(&doc.replacen(from, to, 1)).map(|_| ()));
    }
    let extra = doc.replacen("\"nonce\"", "\"domain\":\"tng\",\"challenge\":\"c\",\"nonce\"", 1);
    let opts = hcert::didproof::canonical_proof_options(&extra).unwrap();
    assert!(opts.contains("<https://w3id.org/security#domain> \"tng\" .\n") && opts.contains("<https://w3id.org/security#challenge> \"c\" .\n"));
    proof_err(hcert::didproof::verify_did_proof(&extra, &signer));
    proof_err(hcert::didproof::canonical_proof_options(reference).map(|_| ()));
    proof_err(hcert::didproof::verify_did_proof(&doc.replacen("\"verificationMethod\":[", "\"verificationMethod\":{\"a\":[", 1), &signer));
}
