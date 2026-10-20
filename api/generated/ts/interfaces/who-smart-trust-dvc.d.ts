/** @module Interface who:smart-trust/dvc@0.1.0 **/
/**
 * Cardinality, invariant (must-have-issuer-or-clinician-name), nt
 * and vp checks. vp goes through the host terminology import.
 * [STT-PAY-004, STT-PAY-006, STT-PAY-007]
 */
export function validate(p: DvcMin, profile: DvcProfile): Array<ValidationIssue>;
export function perEvent(subject: DvcSubject, events: Array<DvcMinVaccineDetails>): Array<DvcMin>;
export function toCbor(p: DvcMin): CborBytes;
export function fromCbor(cbor: CborBytes, limits: DecodeLimits): DvcMin;
export type ValidationIssue = import('./who-smart-trust-types.js').ValidationIssue;
export type CborBytes = import('./who-smart-trust-types.js').CborBytes;
export type DecodeLimits = import('./who-smart-trust-types.js').DecodeLimits;
/**
 * DVCMinVaccineDetails. [STT-PAY-002]
 */
export interface DvcMinVaccineDetails {
  /**
   * Vaccine product identifier. PreQual ID under DVCMinPreQual.
   */
  vp: string,
  /**
   * Date of vaccination (FHIR date).
   */
  dv: string,
  /**
   * Supervising clinician.
   */
  cn?: string,
  /**
   * Issuing organization identifier.
   */
  is?: string,
  /**
   * Batch or lot number.
   */
  bo: string,
}
/**
 * Everything in DVCMin except vx. Used to split a visit into one
 * certificate per vaccine. [STT-PAY-003]
 */
export interface DvcSubject {
  n: string,
  dob: string,
  s: string,
  nt: string,
  id?: string,
  dt?: string,
  gn?: string,
  v: string,
}
/**
 * DVCMin. vx is 1..1: one immunization event per certificate.
 * [STT-PAY-001, STT-PAY-005]
 */
export interface DvcMin {
  n: string,
  dob: string,
  s: string,
  /**
   * ISO 3166-1 alpha-3. [STT-PAY-007]
   */
  nt: string,
  id?: string,
  dt?: string,
  gn?: string,
  vx: DvcMinVaccineDetails,
  v: string,
  /**
   * Elements this version does not know, kept as CBOR map entries
   * (key, encoded value). [STT-PAY-008]
   */
  extensions: Array<[string, CborBytes]>,
}
/**
 * # Variants
 * 
 * ## `"dvc-min"`
 * 
 * Unconstrained vp binding.
 * ## `"dvc-min-prequal"`
 * 
 * vp MUST be a WHO PreQual product ID (required binding).
 */
export type DvcProfile = 'dvc-min' | 'dvc-min-prequal';
