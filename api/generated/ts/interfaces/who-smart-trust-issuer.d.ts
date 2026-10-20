/** @module Interface who:smart-trust/issuer@0.1.0 **/
/**
 * Phase 1: validate, build CWT, CBOR and protected header.
 */
export function prepare(req: IssueRequest, reg: Registry, policy: IssuerPolicy): PendingSignature;
/**
 * Phase 2: attach the signature. ES256 accepts r||s or DER and always
 * emits r||s. [STT-COS-005]
 */
export function finish(pending: PendingSignature, signature: Uint8Array): IssuedHcert;
/**
 * One call, using the host `signer` import. [STT-ISS-001]
 */
export function issue(req: IssueRequest, reg: Registry, policy: IssuerPolicy): IssuedHcert;
export type CoseAlgorithm = import('./who-smart-trust-types.js').CoseAlgorithm;
export type Kid = import('./who-smart-trust-types.js').Kid;
export type NumericDate = import('./who-smart-trust-types.js').NumericDate;
export type ErrorCorrection = import('./who-smart-trust-types.js').ErrorCorrection;
export type EncodingReport = import('./who-smart-trust-types.js').EncodingReport;
export type ValidationIssue = import('./who-smart-trust-types.js').ValidationIssue;
export type Payload = import('./who-smart-trust-payloads.js').Payload;
export type QrMatrix = import('./who-smart-trust-qr.js').QrMatrix;
export type Registry = import('./who-smart-trust-subclaims.js').Registry;
export interface Validity {
  iat: NumericDate,
  /**
   * Mandatory: exp is the only revocation mechanism. [STT-CWT-003]
   */
  exp: NumericDate,
}
export interface IssueRequest {
  payload: Payload,
  /**
   * CWT iss, alpha-2. [STT-CWT-005]
   */
  iss?: string,
  validity: Validity,
  /**
   * DSC whose key will sign; kid is derived from it. [STT-KID-001]
   */
  dscDer: Uint8Array,
  alg: CoseAlgorithm,
}
export interface IssuerPolicy {
  /**
   * Refuse negative subclaims unless true. [STT-SEL-003]
   */
  allowDevelopmentSubclaims: boolean,
  errorCorrection: ErrorCorrection,
  renderQr: boolean,
  /**
   * Verify the result against dsc-der before returning.
   * [STT-ISS-002]
   */
  selfVerify: boolean,
  /**
   * Warn above this HC1 length, suggesting VHL. [STT-QR-006]
   */
  hc1WarnChars: number,
}
export type IssueError = IssueErrorPayloadInvalid | IssueErrorDevelopmentSubclaimNotAllowed | IssueErrorUnknownSubclaim | IssueErrorValidityOutsideDsc | IssueErrorBadCertificate | IssueErrorBadSignatureFormat | IssueErrorSelfVerifyFailed;
export interface IssueErrorPayloadInvalid {
  tag: 'payload-invalid',
  val: Array<ValidationIssue>,
}
export interface IssueErrorDevelopmentSubclaimNotAllowed {
  tag: 'development-subclaim-not-allowed',
  val: number,
}
export interface IssueErrorUnknownSubclaim {
  tag: 'unknown-subclaim',
  val: number,
}
/**
 * iat before or exp after the DSC validity. [STT-CWT-004]
 */
export interface IssueErrorValidityOutsideDsc {
  tag: 'validity-outside-dsc',
}
export interface IssueErrorBadCertificate {
  tag: 'bad-certificate',
  val: string,
}
export interface IssueErrorBadSignatureFormat {
  tag: 'bad-signature-format',
  val: string,
}
export interface IssueErrorSelfVerifyFailed {
  tag: 'self-verify-failed',
  val: string,
}
export interface IssuedHcert {
  hc1: string,
  /**
   * Raw COSE_Sign1 for 8-bit-safe transports. [STT-GEN-009]
   */
  cose: Uint8Array,
  qr?: QrMatrix,
  report: EncodingReport,
}

export class PendingSignature implements Disposable {
  /**
   * This type does not have a public constructor.
   */
  private constructor();
  /**
  * Bytes to sign (CBOR Sig_structure). [STT-COS-004]
  */
  toBeSigned(): Uint8Array;
  algorithm(): CoseAlgorithm;
  kid(): Kid;
  [Symbol.dispose](): void;
}
