/** @module Interface who:smart-trust/qr@0.1.0 **/
/**
 * Single Alphanumeric-mode segment, smallest version that fits
 * unless pinned. [STT-QR-001, STT-QR-002]
 */
export function encode(hc1: string, ec: ErrorCorrection, pinVersion: number | undefined): QrMatrix;
/**
 * [STT-QR-004]
 */
export function toSvg(m: QrMatrix, opts: RenderOptions): string;
/**
 * module-px giving a diagonal closest to `diagonal-mm` (35 to 60 mm
 * recommended) at `dpi`. [STT-QR-004]
 */
export function modulePxForDiagonal(m: QrMatrix, diagonalMm: number, dpi: number, quietZone: number): number;
export type ErrorCorrection = import('./who-smart-trust-types.js').ErrorCorrection;
export type QrError = QrErrorNotAlphanumeric | QrErrorTooLong;
/**
 * Input has characters outside the QR Alphanumeric set.
 */
export interface QrErrorNotAlphanumeric {
  tag: 'not-alphanumeric',
  val: string,
}
export interface QrErrorTooLong {
  tag: 'too-long',
  val: number,
}
export interface QrMatrix {
  /**
   * Modules per side.
   */
  size: number,
  version: number,
  errorCorrection: ErrorCorrection,
  /**
   * Row-major, size*size entries, 1 = dark.
   */
  modules: Uint8Array,
}
export interface RenderOptions {
  /**
   * Whole device pixels per module, so modules stay square.
   */
  modulePx: number,
  /**
   * Quiet zone in modules, at least 4.
   */
  quietZone: number,
}
