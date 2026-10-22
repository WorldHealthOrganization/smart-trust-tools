//! Skills `hcert-build-cwt`, `hcert-sign-cose`: each test named after the requirements it verifies.

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
fn stt_cwt_001_stt_cos_002_stt_cos_003_prepare_builds_the_claims_and_a_protected_alg_and_kid() {
    let p = issuer::prepare(&request(CoseAlgorithm::Es256), &reg()).unwrap();
    let ph = cbor::decode(&p.protected, &DEFAULT_LIMITS).unwrap();
    assert_eq!(ph.get_int(1), Some(&Item::Int(-7)));
    assert_eq!(ph.get_int(4), Some(&Item::Bytes(kid_of(&es_der()).to_vec())));
    let claims = cbor::decode(&p.payload, &DEFAULT_LIMITS).unwrap();
    assert_eq!(claims.get_int(1), Some(&Item::Text("XX".into())));
    assert_eq!(claims.get_int(6), Some(&Item::Int(TESTKIT_NOW)));
    assert!(claims.get_int(-260).unwrap().get_int(-6).is_some());
    assert_eq!(p.to_be_signed, cose::sig_structure(&p.protected, &p.payload));
}

#[test]
fn stt_cwt_004_validity_outside_the_dsc_is_refused() {
    let mut r = request(CoseAlgorithm::Es256);
    r.exp = r.iat + 86_400 * 365 * 20;
    assert_eq!(issuer::prepare(&r, &reg()), Err(IssueError::ValidityOutsideDsc));
}

#[test]
fn stt_iss_004_stt_cos_005_two_phase_issuance_with_es256_and_ps256_verifies() {
    for (alg, der) in [(CoseAlgorithm::Es256, es_der()), (CoseAlgorithm::Ps256, ps_der())] {
        let issued = issue(alg);
        assert!(issued.hc1.starts_with("HC1:"));
        let r = verifier::verify(&issued.hc1, &store_with(&der), &reg(), TESTKIT_NOW, &options());
        match (alg, r) {
            (CoseAlgorithm::Es256, Ok(v)) => assert_eq!(only(&v).payload, Payload::DvcMin(newborn())),
            // the PS256 testkit DSC's EKU is test certificates only (STT-VER-004)
            (CoseAlgorithm::Ps256, Err(f)) => assert_eq!((f.step, f.reason), (Step::KeyUsage, FailureReason::KeyUsageMismatch)),
            (a, r) => panic!("{a:?}: {r:?}"),
        }
    }
}

#[test]
fn stt_cos_006_finish_refuses_a_signature_from_another_key() {
    let p = issuer::prepare(&request(CoseAlgorithm::Es256), &reg()).unwrap();
    let mut sig = sign(CoseAlgorithm::Es256, &p.to_be_signed);
    sig[10] ^= 1;
    assert!(matches!(issuer::finish(&p, &sig), Err(IssueError::BadSignatureFormat(_))));
    // DER, as JCA gives it, is accepted
    let der = testdsc::ecdsa_raw_to_der(&sign(CoseAlgorithm::Es256, &p.to_be_signed));
    assert!(issuer::finish(&p, &der).is_ok());
}

#[test]
fn stt_iss_003_a_test_dsc_around_a_host_key_issues_and_verifies() {
    use p256::pkcs8::EncodePublicKey;
    use p256::ecdsa::signature::Signer;
    let key = p256::ecdsa::SigningKey::from_slice(&[7u8; 32]).unwrap();
    let spki = p256::PublicKey::from(key.verifying_key()).to_public_key_der().unwrap().as_bytes().to_vec();
    let tbs = testdsc::tbs_certificate(&TestDscRequest {
        spki_der: spki,
        alg: CoseAlgorithm::Es256,
        country: "XX".into(),
        common_name: "TEST ONLY demo DSC".into(),
        not_before: NOW - 60,
        not_after: NOW + 86_400 * 365,
        serial: 42,
        extended_key_usages: vec!["vaccinations".into()],
    })
    .unwrap();
    let tbs_sig: p256::ecdsa::Signature = key.sign(&tbs);
    let der = testdsc::certificate(&tbs, CoseAlgorithm::Es256, &tbs_sig.to_bytes()).unwrap();
    let mut req = request(CoseAlgorithm::Es256);
    req.dsc_der = der.clone();
    req.iat = NOW;
    req.exp = NOW + 86_400;
    let p = issuer::prepare(&req, &reg()).unwrap();
    let sig: p256::ecdsa::Signature = key.sign(&p.to_be_signed);
    let issued = issuer::finish(&p, &sig.to_bytes()).unwrap();
    let v = verifier::verify(&issued.hc1, &store_with(&der), &reg(), NOW, &options()).unwrap();
    assert_eq!(v.dsc.country.as_deref(), Some("XX"));
    assert_eq!(v.dsc.extended_key_usages, vec!["1.3.6.1.4.1.1847.2021.1.2".to_string()]);
}
