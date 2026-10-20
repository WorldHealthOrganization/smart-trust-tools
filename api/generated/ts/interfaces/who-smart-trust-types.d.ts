/** @module Interface who:smart-trust/types@0.1.0 **/
/**
 * COSE algorithm. es256 = -7 (primary), ps256 = -37 (secondary).
 * Both MUST be implemented. [STT-COS-002]
 * # Variants
 * 
 * ## `"es256"`
 * 
 * ## `"ps256"`
 */
export type CoseAlgorithm = 'es256' | 'ps256';
/**
 * 8-byte key identifier: first 8 bytes of SHA-256 over the DER DSC.
 * [STT-KID-001]
 */
export type Kid = Uint8Array;
/**
 * CWT NumericDate: integer seconds since the Unix epoch. [STT-CWT-002]
 */
export type NumericDate = bigint;
/**
 * Encoded CBOR data item.
 */
export type CborBytes = Uint8Array;
/**
 * Status of an hcert subclaim number. [STT-SEL-001, STT-SEL-003]
 * # Variants
 * 
 * ## `"assigned"`
 * 
 * Allocated by WHO (>= 0).
 * ## `"proposed"`
 * 
 * Listed by WHO as proposed (>= 0), not yet assigned.
 * ## `"development"`
 * 
 * Negative: development range, free to use, not stable.
 */
export type SubclaimStatus = 'assigned' | 'proposed' | 'development';
export interface Subclaim {
  key: number,
  status: SubclaimStatus,
  /**
   * Canonical URL of the logical model, if any.
   */
  logicalModel?: string,
  title: string,
}
/**
 * Bounds applied while decoding untrusted input.
 * [STT-CBR-003, STT-ZIP-002]
 */
export interface DecodeLimits {
  maxInputChars: number,
  maxInflatedBytes: number,
  maxDepth: number,
  maxItems: number,
  maxStringBytes: number,
}
/**
 * QR error-correction level. Default q. [STT-QR-002]
 * # Variants
 * 
 * ## `"l"`
 * 
 * ## `"m"`
 * 
 * ## `"q"`
 * 
 * ## `"h"`
 */
export type ErrorCorrection = 'l' | 'm' | 'q' | 'h';
/**
 * # Variants
 * 
 * ## `"blocking"`
 * 
 * ## `"warning"`
 * 
 * ## `"information"`
 */
export type Severity = 'blocking' | 'warning' | 'information';
export interface ValidationIssue {
  /**
   * Element path, e.g. "vx.is".
   */
  path: string,
  /**
   * Stable machine code, e.g. "must-have-issuer-or-clinician-name".
   */
  code: string,
  message: string,
  severity: Severity,
}
/**
 * Pipeline stages, used to say where verification failed.
 * # Variants
 * 
 * ## `"prefix"`
 * 
 * ## `"base45"`
 * 
 * ## `"inflate"`
 * 
 * ## `"cose"`
 * 
 * ## `"header"`
 * 
 * ## `"trust-lookup"`
 * 
 * ## `"signature"`
 * 
 * ## `"key-usage"`
 * 
 * ## `"cwt"`
 * 
 * ## `"time"`
 * 
 * ## `"hcert"`
 * 
 * ## `"payload"`
 */
export type Step = 'prefix' | 'base45' | 'inflate' | 'cose' | 'header' | 'trust-lookup' | 'signature' | 'key-usage' | 'cwt' | 'time' | 'hcert' | 'payload';
/**
 * Closed failure taxonomy, identical in Kotlin and Rust. [STT-VER-008]
 * # Variants
 * 
 * ## `"bad-prefix"`
 * 
 * ## `"unsupported-context-version"`
 * 
 * ## `"base45-invalid"`
 * 
 * ## `"inflate-failed"`
 * 
 * ## `"limit-exceeded"`
 * 
 * ## `"cose-malformed"`
 * 
 * ## `"header-invalid"`
 * 
 * ## `"unsupported-algorithm"`
 * 
 * ## `"kid-not-found"`
 * 
 * ## `"signature-invalid"`
 * 
 * ## `"key-usage-mismatch"`
 * 
 * ## `"expired"`
 * 
 * ## `"not-yet-valid"`
 * 
 * ## `"hcert-missing"`
 * 
 * ## `"multiple-subclaims"`
 * 
 * ## `"payload-invalid"`
 */
export type FailureReason = 'bad-prefix' | 'unsupported-context-version' | 'base45-invalid' | 'inflate-failed' | 'limit-exceeded' | 'cose-malformed' | 'header-invalid' | 'unsupported-algorithm' | 'kid-not-found' | 'signature-invalid' | 'key-usage-mismatch' | 'expired' | 'not-yet-valid' | 'hcert-missing' | 'multiple-subclaims' | 'payload-invalid';
export interface Diagnostic {
  step: Step,
  severity: Severity,
  message: string,
}
/**
 * Sizes at each stage of issuance. [STT-QR-006]
 */
export interface EncodingReport {
  cborBytes: number,
  coseBytes: number,
  deflatedBytes: number,
  hc1Chars: number,
  qrVersion?: number,
  errorCorrection?: ErrorCorrection,
  warnings: Array<string>,
}
