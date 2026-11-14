# Part: supernatural_tensura

Requires: `vampirism`, `tensura`, and the `supernatural` part. Optional: `werewolves`. Package: `net.ofatech.supernaturalbloodlines.parts.supernaturaltensura`. Config key `supernaturalTensura`; no config file. Plan: `docs/superpowers/plans/2026-11-14-supernatural-system.md` (phase 12a). Spec: `docs/superpowers/specs/2026-11-14-supernatural-system-design.md`.

**Mixin budget: 0 of 3 used.** One is reserved for Tensura's menu tab (phase 12e Task 5), only if `ScreenEvent.Init.Post` cannot place it.

## Scope

Everything in the supernatural system that touches Tensura: EP rewards (`TensuraEpReward`, phase 12a Task 4), the apex rungs and the layer slices of `SpeciesLayer`, the Tensura skills our skills grant, the Tensura menu tab. `init` is empty in this version.

Without Tensura, or with this part off, the ladders have no EP rewards, no Tensura-state tasks and no apex rungs: **each ladder ends one rung lower, before the apex rung, because there are no apex races to evolve into.** The apex rungs of the data carry `mods: ["tensura"]` and `parts: ["supernaturalTensura"]`, and `tensura_tier` (the apex tier of the player's current Tensura line) is read by this part in phase 12d.
