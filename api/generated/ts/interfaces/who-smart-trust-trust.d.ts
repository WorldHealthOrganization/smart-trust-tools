/** @module Interface who:smart-trust/trust@0.1.0 **/
export type Kid = import('./who-smart-trust-types.js').Kid;
export type NumericDate = import('./who-smart-trust-types.js').NumericDate;
export type TrustError = TrustErrorParse;
export interface TrustErrorParse {
  tag: 'parse',
  val: string,
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
  /**
   * Trust domain and participant, when the DSC came from a DID
   * trustlist document.
   */
  domain?: string,
  participant?: string,
}

export class TrustStore implements Disposable {
  constructor()
  /**
  * Each add-* returns the number of DSCs added. [STT-TRU-005]
  */
  addPem(pem: string): number;
  addDer(der: Uint8Array): number;
  /**
  * A GDHCN DID trustlist v2 document, embedded or reference
  * variant. [STT-TRU-002]
  */
  addDidDocument(json: string): number;
  /**
  * Every DSC with this kid (kids are not unique). [STT-KID-002]
  */
  candidates(k: Kid): Array<TrustedDsc>;
  size(): number;
  clear(): void;
  [Symbol.dispose](): void;
}
