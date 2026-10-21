//! SMART Trust HCERT: the QR Code Generation pipeline and the verifier.
//!
//! Each step is its own public function (STT-GEN-002): [`codec`] holds the
//! prefix, Base45, ZLIB and CBOR steps, [`cose`] the COSE_Sign1 step,
//! [`payload`] the subclaims and the DVC payload, [`trust`] the trust store,
//! [`issuer`] two-phase issuance (the host signs) and [`verifier`] the
//! QR Code Generation section 11 checks. No I/O anywhere (STT-GEN-012).

pub mod api;
pub mod base45;
pub mod cbor;
pub mod codec;
pub mod cose;
pub mod issuer;
pub mod json;
pub mod payload;
pub mod testdsc;
pub mod trust;
pub mod types;
pub mod verifier;
mod x509;

#[cfg(target_arch = "wasm32")]
mod wasm;

pub use types::*;

#[cfg(target_arch = "wasm32")]
fn no_entropy(_: &mut [u8]) -> Result<(), getrandom::Error> {
    Err(getrandom::Error::UNSUPPORTED)
}
#[cfg(target_arch = "wasm32")]
getrandom::register_custom_getrandom!(no_entropy);
