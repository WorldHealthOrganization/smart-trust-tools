---
# smart-trust-tools-oi16
title: 'OI-16: WIT type-system limits'
status: todo
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

WIT has no recursive types, so generic CBOR crosses as encoded bytes and only DVCMin and VHL are typed records. s64 becomes bigint in JavaScript, so it is used only for NumericDate, and subclaim numbers are s32. result<T, E> is thrown as ComponentError by jco, so APIs whose failures are expected (verify) return a variant instead.

## Requirements affected

- req:stt-gen-010

## Sources

- external-schemas/bytecodealliance-wit

## Done when

- [ ] fixed in this repository
