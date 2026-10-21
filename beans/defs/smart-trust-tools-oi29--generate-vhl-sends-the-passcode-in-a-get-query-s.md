---
# smart-trust-tools-oi29
title: 'OI-29: $generate-vhl sends the passcode in a GET query string'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - upstream
created_at: 2026-10-20T00:00:00Z
updated_at: 2026-10-20T20:00:00Z
---

ITI-YY3's $generate-vhl is a GET with passcode as a query parameter, so the passcode lands in server and proxy logs. Host concern, not the library's; noted for the policy track.

## Requirements affected

- req:stt-vhl-005

## Sources

- library/ihe-iti-vhl-iti-yy3/sections/s010-2-3-yy3-4-1-3-expected-actions

## Done when

- [ ] the upstream source is corrected, or the requirement no longer depends on it
