/** @module Interface who:smart-trust/verifier@0.1.0 **/
/**
 * `now` is passed in: no ambient clock. [STT-VER-005]
 */
export function verify(hc1: string, store: TrustStore, reg: Registry, now: NumericDate, options: VerifierOptions): VerificationResult;
/**
 * Same, for raw COSE_Sign1 bytes. [STT-GEN-009]
 */
export function verifyCose(cose: Uint8Array, store: TrustStore, reg: Registry, now: NumericDate, options: VerifierOptions): VerificationResult;
/**
 * [STT-VER-011]
 */
export function readUnverified(hc1: string, reg: Registry, limits: DecodeLimits): UnverifiedHcert;
export type Kid = import('./who-smart-trust-types.js').Kid;
export type NumericDate = import('./who-smart-trust-types.js').NumericDate;
export type DecodeLimits = import('./who-smart-trust-types.js').DecodeLimits;
export type CborBytes = import('./who-smart-trust-types.js').CborBytes;
export type Step = import('./who-smart-trust-types.js').Step;
export type FailureReason = import('./who-smart-trust-types.js').FailureReason;
export type Diagnostic = import('./who-smart-trust-types.js').Diagnostic;
export type Payload = import('./who-smart-trust-payloads.js').Payload;
export type TrustStore = import('./who-smart-trust-trust.js').TrustStore;
export type TrustedDsc = import('./who-smart-trust-trust.js').TrustedDsc;
export type Registry = import('./who-smart-trust-subclaims.js').Registry;
export interface VerifierOptions {
  limits: DecodeLimits,
  clockSkewSeconds: number,
}
export interface VerifiedHcert {
  iss?: string,
  iat: NumericDate,
  exp: NumericDate,
  subclaim: number,
  payload: Payload,
  /**
   * The DSC that verified the signature.
   */
  dsc: TrustedDsc,
  /**
   * Unknown CWT claims, as (encoded key, encoded value).
   * [STT-CWT-006]
   */
  otherClaims: Array<[CborBytes, CborBytes]>,
  diagnostics: Array<Diagnostic>,
}
export interface VerificationFailure {
  step: Step,
  reason: FailureReason,
  detail: string,
  diagnostics: Array<Diagnostic>,
}
/**
 * Payload content is only reachable through `verified`.
 * [STT-VER-002, STT-VER-008]
 */
export type VerificationResult = VerificationResultVerified | VerificationResultFailed;
export interface VerificationResultVerified {
  tag: 'verified',
  val: VerifiedHcert,
}
export interface VerificationResultFailed {
  tag: 'failed',
  val: VerificationFailure,
}
/**
 * Decoded, NOT verified: no trust lookup, no signature check. For
 * display and triage where verification is impossible (an offline
 * household visit, journey J2); never a basis for trust. [STT-VER-011]
 */
export interface UnverifiedHcert {
  kid?: Kid,
  iss?: string,
  iat?: NumericDate,
  exp?: NumericDate,
  subclaim?: number,
  /**
   * The payload, when it decoded.
   */
  payload?: Payload,
  diagnostics: Array<Diagnostic>,
}
