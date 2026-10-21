//! Skill `hcert-verify`: each test named after the requirements it verifies.

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
fn stt_ver_003_headers_are_checked_before_any_signature() {
    let s = |ph: Item, uh: Item| cbor::encode(&Item::Array(vec![Item::Bytes(cbor::encode(&ph).unwrap()), uh, Item::Bytes(vec![0xa0]), Item::Bytes(vec![0; 64])])).unwrap();
    let alg_in_unprotected = s(Item::Map(vec![(Item::Int(1), Item::Int(-7))]), Item::Map(vec![(Item::Int(1), Item::Int(-7))]));
    let no_kid = s(Item::Map(vec![(Item::Int(1), Item::Int(-7))]), Item::Map(vec![]));
    let short_kid = s(Item::Map(vec![(Item::Int(1), Item::Int(-7)), (Item::Int(4), Item::Bytes(vec![1, 2]))]), Item::Map(vec![]));
    for bytes in [alg_in_unprotected, no_kid, short_kid] {
        let f = verifier::verify_cose(&bytes, &TrustStore::new(), &reg(), NOW, &options()).unwrap_err();
        assert_eq!((f.step, f.reason), (Step::Header, FailureReason::HeaderInvalid), "{}", f.detail);
    }
}

#[test]
fn stt_ver_007_an_hcert_with_two_entries_is_refused() {
    let claims = Item::Map(vec![
        (Item::Int(4), Item::Int(TESTKIT_NOW + 1000)),
        (Item::Int(6), Item::Int(TESTKIT_NOW)),
        (Item::Int(-260), Item::Map(vec![(Item::Int(-6), payload::to_item(&newborn()).unwrap()), (Item::Int(-7), Item::Map(vec![]))])),
    ]);
    let payload_bytes = cbor::encode(&claims).unwrap();
    let ph = issuer::protected_header(CoseAlgorithm::Es256, &kid_of(&es_der()));
    let sig = sign(CoseAlgorithm::Es256, &cose::sig_structure(&ph, &payload_bytes));
    let cose_bytes = cose::assemble_sign1(&ph, &payload_bytes, &sig);
    let f = verifier::verify_cose(&cose_bytes, &store_with(&es_der()), &reg(), TESTKIT_NOW, &options()).unwrap_err();
    assert_eq!((f.step, f.reason), (Step::Hcert, FailureReason::MultipleSubclaims));
}

#[test]
fn stt_ver_011_read_unverified_says_so() {
    let u = verifier::read_unverified(&pr129_hc1("bcg"), &reg(), &DEFAULT_LIMITS).unwrap();
    assert!(u.diagnostics[0].message.starts_with("NOT VERIFIED"));
    assert_eq!(u.subclaim, Some(-6));
    assert_eq!(u.iss.as_deref(), Some("XX"));
}
