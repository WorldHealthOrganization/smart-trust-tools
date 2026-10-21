---
# smart-trust-tools-oi31
title: 'OI-31: A downstream cannot declare the actors its requirements name'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - harness
created_at: 2026-10-21T00:00:00Z
updated_at: 2026-10-21T00:00:00Z
---

Every requirement here names its actors (issuer, receiver, implementer, trust-network-participant), and scenarios/actors/ declares them, each with the roles of scenarios/roles.json it takes on. cat-harness reads actors only from the platform instance's registry (`actorsDir` resolves `scenarios/actors` inside cat-harness, never the instance under audit), so `kg:audit --instance .` reports every requirement as `requirement-actors-resolve` CRITICAL and every role as `role-has-actor`, although the actors are declared beside them. Renaming them to platform actors (admin, author, ci-pipeline) would pass the audit and make the requirements wrong.

## Resolution

In litlfred/cat-harness, read the actor registry of the instance under audit and of every instance it `needs`, as `readRoleGraph` already does for roles, and keep the platform's as the base. Until then the finding is expected here and nothing is renamed.

## Requirements affected

- req:stt-gen-001

## Sources

- memory/dir-journeys
