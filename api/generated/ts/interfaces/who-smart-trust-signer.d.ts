/** @module Interface who:smart-trust/signer@0.1.0 **/
export function algorithm(): CoseAlgorithm;
/**
 * ES256: r||s or DER; PS256: raw signature.
 */
export function sign(toBeSigned: Uint8Array): Uint8Array;
export type CoseAlgorithm = import('./who-smart-trust-types.js').CoseAlgorithm;
