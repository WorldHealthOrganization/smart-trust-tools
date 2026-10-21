---
# smart-trust-tools-oi31
title: 'OI-31: The instance audit ignores the actors a downstream declares'
status: todo
type: bug
priority: normal
tags:
    - open-issue
    - harness
created_at: 2026-10-21T00:00:00Z
updated_at: 2026-10-21T00:00:00Z
---

The requirements name their actors by the IHE ITI VHL names first: VHL Sharer (53 requirements), VHL Receiver (36), Trust Anchor (2), plus this project's own implementer (25). scenarios/actors/ declares them, together with VHL Holder and smart-trust's Trust Network Participant, the external actor that holds the signing keys. Each actor names its IHE and smart-trust ActorDefinition in meta.definedBy, and both actor files are library entries (ihe-iti-vhl-actors, who-smart-trust-actors).

`kg:audit --instance .` read actors only from the platform's registry (cat-harness scenarios/actors), never from the instance under audit or the instances it needs. So all 91 requirements failed `requirement-actors-resolve` (CRITICAL) and every role failed `role-has-actor`.

## Resolution

litlfred/cat-harness#391 adds `neededActors`: the platform's registry overlaid with every needed instance's actors and then the instance's own. litlfred/cat-harness-tools#507 makes the instance audit use it. Measured here with both changes applied: 0 CRITICAL, no role-has-actor finding. Close once both are merged and this repository is repinned.

## Requirements affected

- req:stt-gen-001

## Sources

- library/ihe-iti-vhl-actors/sections/s001-actors-fsh
- library/who-smart-trust-actors/sections/s001-actors-fsh
