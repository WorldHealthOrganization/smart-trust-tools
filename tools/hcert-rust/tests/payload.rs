//! Skills `hcert-assemble-payload`, `hcert-select-payload`: each test named after the requirements it verifies.

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
fn stt_pay_001_stt_pay_008_a_dvcmin_round_trips_and_keeps_unknown_elements() {
    let p = newborn();
    let bytes = payload::to_cbor(&p).unwrap();
    assert_eq!(payload::from_cbor(&bytes, &DEFAULT_LIMITS).unwrap(), p);
}

#[test]
fn stt_pay_002_stt_pay_003_the_logical_model_rules_are_checked() {
    let mut p = newborn();
    p.nt = "LA".into();
    p.dob = "2026-13-01".into();
    p.vx.is = None;
    let codes: Vec<_> = payload::validate(&p).into_iter().map(|i| i.code).collect();
    assert!(codes.contains(&"not-alpha3".to_string()));
    assert!(codes.contains(&"invalid-date".to_string()));
    assert!(codes.contains(&"must-have-issuer-or-clinician-name".to_string()));
    assert!(payload::validate(&newborn()).is_empty());
}

#[test]
fn stt_sel_001_stt_sel_003_the_subclaims_are_the_guides_and_development_ones_need_opting_in() {
    let keys: Vec<i64> = reg().all().iter().map(|s| s.key).collect();
    assert_eq!(keys, vec![1, 3, 4, 5, -6, -7]);
    let mut r = request(CoseAlgorithm::Es256);
    r.allow_development_subclaims = false;
    assert_eq!(issuer::prepare(&r, &reg()), Err(IssueError::DevelopmentSubclaimNotAllowed(-6)));
}
