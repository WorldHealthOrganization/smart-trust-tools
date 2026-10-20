/** @module Interface who:smart-trust/payloads@0.1.0 **/
export type CborBytes = import('./who-smart-trust-types.js').CborBytes;
export type DvcMin = import('./who-smart-trust-dvc.js').DvcMin;
export interface RawPayload {
  subclaim: number,
  cbor: CborBytes,
}
export type Payload = PayloadDvcMin | PayloadRaw;
export interface PayloadDvcMin {
  tag: 'dvc-min',
  val: DvcMin,
}
export interface PayloadRaw {
  tag: 'raw',
  val: RawPayload,
}
