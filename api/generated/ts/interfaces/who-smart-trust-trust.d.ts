/** @module Interface who:smart-trust/trust@0.1.0 **/
/**
 * URL of a GDHCN DID trustlist v2 document. None for a level means
 * the "-" wildcard. [STT-TRU-002]
 */
export function didTrustlistUrl(env: Environment, which: TrustlistVariant, domain: string | undefined, participant: string | undefined, usage: string | undefined): string;
export type Kid = import('./who-smart-trust-types.js').Kid;
export type NumericDate = import('./who-smart-trust-types.js').NumericDate;
export type TrustError = TrustErrorParse | TrustErrorProofInvalid | TrustErrorChainInvalid;
export interface TrustErrorParse {
  tag: 'parse',
  val: string,
}
/**
 * JWS proof on a DID document did not verify. [STT-TRU-003]
 */
export interface TrustErrorProofInvalid {
  tag: 'proof-invalid',
  val: string,
}
/**
 * DSC not signed by a listed SCA, or AKI/SKI mismatch.
 * [STT-TRU-006]
 */
export interface TrustErrorChainInvalid {
  tag: 'chain-invalid',
  val: string,
}
/**
 * Query for candidate DSCs. Optional fields narrow the match.
 */
export interface KeyQuery {
  kid: Kid,
  /**
   * CWT iss (alpha-2). [STT-VER-010]
   */
  iss?: string,
  domain?: string,
  participant?: string,
  usage?: string,
}
export interface TrustedDsc {
  der: Uint8Array,
  kid: Kid,
  country?: string,
  notBefore: NumericDate,
  notAfter: NumericDate,
  /**
   * Extended Key Usage OIDs; empty means valid for any type.
   * [STT-VER-004]
   */
  extendedKeyUsages: Array<string>,
  domain?: string,
  participant?: string,
}
/**
 * # Variants
 * 
 * ## `"dev"`
 * 
 * ## `"uat"`
 * 
 * ## `"prod"`
 */
export type Environment = 'dev' | 'uat' | 'prod';
/**
 * # Variants
 * 
 * ## `"embedded"`
 * 
 * ## `"reference"`
 */
export type TrustlistVariant = 'embedded' | 'reference';

export class TrustStore implements Disposable {
  constructor()
  /**
  * Each add-* returns the number of DSCs added.
  */
  addPem(pem: string): number;
  addDer(der: Uint8Array): number;
  /**
  * RFC 7517 JWK set with x5c. [STT-TRU-005]
  */
  addJwks(json: string): number;
  /**
  * GDHCN DID trustlist v2 document (embedded variant).
  * [STT-TRU-002]
  */
  addDidDocument(json: string, verifyProof: boolean): number;
  /**
  * SCA certificates for the optional chain check. [STT-TRU-006]
  */
  addScaPem(pem: string): number;
  /**
  * Every DSC matching the kid (kids are not unique). [STT-KID-002]
  */
  candidates(q: KeyQuery): Array<TrustedDsc>;
  size(): number;
  /**
  * When the host last replaced the contents. [STT-TRU-004]
  */
  loadedAt(): NumericDate | undefined;
  setLoadedAt(t: NumericDate): void;
  clear(): void;
  [Symbol.dispose](): void;
}
