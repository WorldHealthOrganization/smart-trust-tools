/** @module Interface who:smart-trust/subclaims@0.1.0 **/
/**
 * The IG defaults: 1, 3, 4, 5, -6, -7. [STT-SEL-001]
 */
export function defaults(): Array<Subclaim>;
/**
 * >= 0 is WHO-reserved; negative is the development range.
 * [STT-SEL-003]
 */
export function isDevelopmentRange(key: number): boolean;
export function advise(facts: UseCaseFacts): PayloadAdvice;
export type Subclaim = import('./who-smart-trust-types.js').Subclaim;
export type SubclaimStatus = import('./who-smart-trust-types.js').SubclaimStatus;
/**
 * Payload kinds that have a typed codec. Others travel as raw CBOR.
 * # Variants
 * 
 * ## `"dvc-min"`
 * 
 * ## `"vhl"`
 */
export type PayloadKind = 'dvc-min' | 'vhl';
/**
 * Inputs to the QR Code Generation 2.4 decision tree. [STT-SEL-005]
 */
export interface UseCaseFacts {
  ihrEntryRequirement: boolean,
  mustVerifyOffline: boolean,
  contentMustChangeAfterIssue: boolean,
  holderControlsDisclosure: boolean,
  /**
   * Estimated HC1 length, if known.
   */
  estimatedHc1Chars?: number,
}
/**
 * # Variants
 * 
 * ## `"icvp"`
 * 
 * ## `"dvc"`
 * 
 * ## `"vhl"`
 */
export type Advice = 'icvp' | 'dvc' | 'vhl';
export interface PayloadAdvice {
  advice: Advice,
  reasons: Array<string>,
}

export class Registry implements Disposable {
  /**
  * Starts with `defaults()`.
  */
  constructor()
  lookup(key: number): Subclaim | undefined;
  register(s: Subclaim): void;
  /**
  * Which subclaim number carries the given payload kind.
  */
  subclaimFor(kind: PayloadKind): number | undefined;
  setSubclaimFor(kind: PayloadKind, key: number): void;
  all(): Array<Subclaim>;
  [Symbol.dispose](): void;
}
