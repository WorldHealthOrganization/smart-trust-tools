---
# smart-trust-tools-oi16
title: 'OI-16: WIT type-system limits'
status: completed
type: task
priority: normal
tags:
    - open-issue
    - this-repository
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-24T14:03:22Z
---

WIT has no recursive types, so generic CBOR crosses as encoded bytes and only DVCMin and VHL are typed records. s64 becomes bigint in JavaScript, so it is used only for NumericDate, and subclaim numbers are s32. result<T, E> is thrown as ComponentError by jco, so APIs whose failures are expected (verify) return a variant instead.

## Requirements affected

- req:stt-gen-010

## Sources

- external-schemas/bytecodealliance-wit

## Done when

- [x] fixed in this repository

## Closed

2026-10-24, Claude (https://claude.ai/code/session_01PeeabG4WKeuCzLfTEqH3Yz): the design takes each limit, and api/README.md states them ("No recursive types", "`s64` maps to `bigint`", `verify` returns a variant). In api/wit, `s64` is used only for `numeric-date` and the VHL `exp` timestamp, subclaims are `s32`, and `verify` returns `verification-result`. Nothing is left to fix here; a WIT change (for example recursive types) would be a new bean.
