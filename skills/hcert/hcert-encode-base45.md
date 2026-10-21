---
name: hcert-encode-base45
description: >
  Step 7: RFC 9285 Base45, decoded strictly.
satisfies:
  - "req:stt-b45-001#statement"
  - "req:stt-b45-002#statement"
  - "req:stt-gen-001#statement"
---

# Step 7

Step 7: RFC 9285 Base45, decoded strictly.

## Library operations

In the WIT contract (`api/wit/`):

- `codec.base45-decode`
- `issuer.prepare`
- `verifier.verify`

## Requirements

This skill satisfies 3 requirement(s), listed in `satisfies` above; each is a `req:` node in `skills/requirements/`.
