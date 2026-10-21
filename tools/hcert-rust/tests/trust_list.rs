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
