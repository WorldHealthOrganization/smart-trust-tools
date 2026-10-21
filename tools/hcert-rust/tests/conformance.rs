//! Skill `hcert-conformance-testing`: the shared vectors, the same files the
//! Kotlin suite and the Node smoke test load (STT-TST-001, 002, 004, 005).

mod common;
use common::*;
use hcert::codec;
use hcert::cose;
use hcert::payload::Payload;
use hcert::trust::TrustStore;
use hcert::types::*;
use hcert::verifier::{self, VerifierOptions};

#[test]
fn stt_tst_001_stt_kid_001_the_vectors_kid_is_the_dscs() {
    let kid = cose::base64_decode("WHM+BYqqZiE=").unwrap();
    let store = pr129_store();
    let c = store.candidates(&kid.clone().try_into().unwrap());
    assert_eq!(c.len(), 1);
    assert_eq!(to_hex(&c[0].kid), "58733e058aaa6621");
}

#[test]
fn stt_tst_001_stt_pay_001_both_pr129_certificates_verify_and_decode_as_published() {
    let store = pr129_store();
    let vector = json(PR129);
    for name in ["hepb1", "bcg"] {
        let v = verifier::verify(&pr129_hc1(name), &store, &reg(), NOW, &options()).unwrap_or_else(|f| panic!("{name}: {f:?}"));
        assert_eq!(v.iss.as_deref(), Some("XX"));
        assert_eq!(v.subclaim, -6);
        let Payload::DvcMin(p) = &v.payload else { panic!("{name}: not a DVCMin") };
        let expected = vector.get("certificates").unwrap().get(name).unwrap().get("payload").unwrap();
        assert_eq!(Some(p.n.as_str()), expected.get("n").and_then(|j| j.as_str()));
        assert_eq!(Some(p.nt.as_str()), expected.get("nt").and_then(|j| j.as_str()));
        assert_eq!(Some(p.vx.vp.as_str()), expected.get("vx").unwrap().get("vp").and_then(|j| j.as_str()));
        assert_eq!(Some(p.vx.bo.as_str()), expected.get("vx").unwrap().get("bo").and_then(|j| j.as_str()));
        assert!(v.diagnostics.is_empty(), "{:?}", v.diagnostics);
    }
}

#[test]
fn stt_tst_002_stt_cbr_001_deterministic_re_encoding_reproduces_the_issued_cwt_byte_for_byte() {
    let cose_bytes = codec::from_hc1(&pr129_hc1("hepb1"), &DEFAULT_LIMITS).unwrap();
    let s = cose::parse_sign1(&cose_bytes, &DEFAULT_LIMITS).unwrap();
    assert_eq!(codec::cbor_deterministic(&s.payload, &DEFAULT_LIMITS).unwrap(), s.payload);
    assert_eq!(s.payload.len(), 210);
    assert_eq!(cose_bytes.len(), 295);
    // and the HC1 string round-trips through the QR layers
    assert_eq!(codec::from_hc1(&codec::to_hc1(&cose_bytes), &DEFAULT_LIMITS).unwrap(), cose_bytes);
}

#[test]
fn stt_tst_004_stt_tst_005_the_shared_negative_vectors_fail_at_the_named_step_with_the_named_reason() {
    let doc = json("vectors/negative/negative-vectors.json");
    let defaults = doc.get("defaults").unwrap();
    let cases = doc.get("cases").unwrap().as_arr().unwrap();
    assert!(cases.len() >= 15);
    let skew = defaults.get("clockSkewSeconds").unwrap().as_f64().unwrap() as i64;
    let options = VerifierOptions { clock_skew_seconds: skew, ..VerifierOptions::default() };
    for c in cases {
        let id = c.get("id").unwrap().as_str().unwrap();
        let mut store = TrustStore::new();
        for p in c.get("trust").or(defaults.get("trust")).unwrap().as_arr().unwrap() {
            store.add_pem(&read(p.as_str().unwrap())).unwrap();
        }
        let now = c.get("now").or(defaults.get("now")).unwrap().as_f64().unwrap() as i64;
        let r = match c.get("coseHex").and_then(|j| j.as_str()) {
            Some(hex) => verifier::verify_cose(&from_hex(hex).unwrap(), &store, &reg(), now, &options),
            None => verifier::verify(c.get("hc1").unwrap().as_str().unwrap(), &store, &reg(), now, &options),
        };
        let f = r.err().unwrap_or_else(|| panic!("{id}: verified"));
        let expect = c.get("expect").unwrap();
        assert_eq!(
            (f.step.wire_name(), f.reason.wire_name()),
            (expect.get("step").unwrap().as_str().unwrap(), expect.get("reason").unwrap().as_str().unwrap()),
            "{id}: {}",
            f.detail
        );
    }
}
