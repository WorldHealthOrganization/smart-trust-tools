/** @module Interface who:smart-trust/terminology@0.1.0 **/
export function isMember(valueSet: string, code: string): Membership;
/**
 * # Variants
 * 
 * ## `"member"`
 * 
 * ## `"not-member"`
 * 
 * ## `"unknown"`
 */
export type Membership = 'member' | 'not-member' | 'unknown';
