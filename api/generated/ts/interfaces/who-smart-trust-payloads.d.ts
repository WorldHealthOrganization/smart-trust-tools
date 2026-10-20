/** @module Interface who:smart-trust/payloads@0.1.0 **/
export type CborBytes = import('./who-smart-trust-types.js').CborBytes;
export type DvcMin = import('./who-smart-trust-dvc.js').DvcMin;
export interface RawPayload {
  subclaim: number,
  cbor: CborBytes,
}
/**
 * [STT-PAY-009]
 */
export type Payload = PayloadDvcMin | PayloadVhl | PayloadRaw;
export interface PayloadDvcMin {
  tag: 'dvc-min',
  val: DvcMin,
}
/**
 * Subclaim 5: URI of a Verifiable Health Link.
 */
export interface PayloadVhl {
  tag: 'vhl',
  val: string,
}
/**
 * Any subclaim without a typed codec (1, 3, 4, -7, custom).
 */
export interface PayloadRaw {
  tag: 'raw',
  val: RawPayload,
}
