# Sirelines and Bloodline Perks Implementation Plan — part `species`

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players who turn others build a sireline, like Tensura's subordinates. They can see the tree, call their sired, and compel villagers and lesser vampires. An optional "sireline dies with its Original" rule is available for hardcore servers.

**Architecture:** `parts.species.sireline`. Each sired player stores its sire in `BloodlineState.sire`. Sired mobs store it in an entity attachment `supernaturalbloodlines:sire`. A world `SirelineIndex` (`SavedData`) maps each sire to its sired (UUID, entity type, last known name) for fast tree queries. Recording happens at the three turning points: Vampirism infection by a player bite, Vampirism creature conversion by a player, and hybrid completion (plan 4). The Tensura subordinate link is plan 5, Task 5.

**Tech Stack:** As foundation, plus the Vampirism API.

**Spec:** design section "8. Sirelines and bloodline perks".

## Global Constraints

- Everything in foundation's Global Constraints.
- `sireline_death` is a config value, default `false`.
- Compulsion never works on players, vervain users (`VervainRules.protects`) or hunters (`HunterRules.immuneToCompulsion`).
- Depends on plan 4 (Task 3 hybrid completion) for hybrid sires. If plan 6 or 7 isn't merged, compulsion immunity checks those classes only when they exist: put a `CompulsionImmunity` registry in core that plans 6 and 7 add their checks to.

## Review Focus

- **A sired player whose sire is offline or deleted** must still show in the tree, by last known name, and must not crash `/bloodline tree`. Pinned by `treeShowsOfflineSire`.
- **Cycles:** a sired vampire who later "turns" their own sire (after a cure and re-turn) must not create an infinite tree. Pinned by `treeStopsOnCycle`.
- **`/bloodline call` when sired players are in another dimension** must teleport them only if they accept, and across dimensions safely. Pinned by `callAcrossDimensionsAfterAccept`.
- **Sireline death enabled, with sired players offline:** they must die on next login, once. Pinned by `offlineSiredDieOnLogin`.
- **Unloaded sired mobs** must not be force-loaded by `/bloodline call` or sireline death. Pinned by `unloadedMobsUntouched`.

---

### Task 1: Discovery and recording

**Files:**
- Create: `docs/parts/species.md`, a section "Sireline hook points" (how Vampirism stores who infected a player with sanguinare, if anything, else the `MobEffectEvent.Added` source and the bite events; the converted-creature event and source), `parts/species/sireline/SirelineIndex.java`, `parts/species/sireline/SireRecorder.java`, and `core/CompulsionImmunity.java`
- Modify: plan 4's `TransitionHandler`, to call `SireRecorder.record(sire, hybrid)` on completion

**Interfaces:**
- **`SirelineIndex.get(ServerLevel overworld)`**, with:
  - `void add(UUID sire, UUID child, String entityType, String name)`
  - `void remove(UUID child)`
  - `List<Node> childrenOf(UUID)`
  - `Optional<UUID> sireOf(UUID)`
  - `record Node(UUID id, String entityType, String name)`
- **`SireRecorder.record(Entity sire, LivingEntity child)`:** for a player child it sets `BloodlineState.sire` (only if null); for mobs it sets the `supernaturalbloodlines:sire` attachment; then it adds the child to the index and notifies listeners (plan 5 uses this through `BloodlineAccess.addListener`, or a dedicated `SireRecorder.addListener`).
- **Recording points:**
  - a player bitten by a player-vampire who then finishes sanguinare: remember the biter for 24000 ticks in an attachment `pending_sire`, and record it on vampire faction join
  - creature conversion by a player
  - hybrid completion
- **`CompulsionImmunity.register(Predicate<Player>)`** and `static boolean CompulsionImmunity.isImmune(Player)`.

- [ ] **Step 1:** Write the GameTests:
  - `playerBittenAndTurnedRecordsSire`
  - `convertedCreatureRecordsSire`
  - `hybridRecordsOriginalSire`
  - `sireNotOverwritten` (the first sire wins)
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(sirelines): recording and index".

### Task 2: `/bloodline` commands

**Files:**
- Create: `parts/species/sireline/BloodlineCommand.java`, and `core/network/CallRequestPayload.java` (S2C prompt) / `CallAnswerPayload.java` (C2S accept or deny) with a small client confirm screen or chat-click component (prefer a clickable chat `[Accept]` component, which needs no screen)

**Interfaces:**
- `/bloodline tree [player]` prints an indented tree: depth-first, a visited set to stop cycles, max depth 10, offline names from the index.
- `/bloodline call` is allowed for ORIGINAL, ORIGINAL_HYBRID, HYBRID and TRIBRID, with a 12000-tick cooldown stored on the attachment. It sends a call request to online sired players. On accept within 60 s, it teleports them next to the caller (cross-dimension `teleportTo(ServerLevel, …)`). Loaded sired mobs within the caller's dimension path to the caller (Tensura `SubordinateHelper.setFollow` when Tensura is loaded; else a simple move goal for 30 s).

- [ ] **Step 1:** Write the GameTests:
  - `treeShowsChildren`
  - `treeShowsOfflineSire`
  - `treeStopsOnCycle`
  - `callNeedsSpecies`
  - `callAcrossDimensionsAfterAccept`
  - `callHasCooldown`
  - `unloadedMobsUntouched`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(sirelines): tree and call commands".

### Task 3: Compulsion and sireline death

**Files:**
- Create: `parts/species/sireline/Compulsion.java`, `parts/species/sireline/SirelineDeath.java`, and the effect `compelled`.

**Interfaces:**
- **Compulsion:** `PlayerInteractEvent.EntityInteract` with sneaking and an empty main hand.
  - The caster must be ORIGINAL, ORIGINAL_HYBRID, HYBRID or TRIBRID, or a Vampirism vampire at level ≥ 10.
  - The target is within 4 blocks, with eye contact: the target's look vector is within 30° of the caster's position, and the caster's within 15° of the target.
  - On a villager: the next click within 5 s chooses between a trade discount (vanilla `Villager.getGossips()` adds a positive `MAJOR_POSITIVE` gossip worth 20% for 24000 ticks; record it so it can be removed) and follow (5 min move-to-caster goal).
  - On a Vampirism lesser vampire mob (`vampirism:vampire` entity tag, excluding advanced and bosses): record it as sired and make it non-hostile (`Mob.setTarget(null)` and an attachment flag checked in `LivingChangeTargetEvent`).
  - Refused if `CompulsionImmunity.isImmune(target)` (players are always immune).
- **Sireline death** (when `sireline_death` is true): when an Original dies to white oak (plan 4's white oak tag on the killing weapon), walk the index from that Original. Online sired players die once with `supernaturalbloodlines:sireline` damage. Offline ones go into a `pendingDeaths` set and die on login. Loaded sired mobs are discarded. Unloaded ones are removed lazily when they load (`EntityJoinLevelEvent` checks the set).

- [ ] **Step 1:** Write the GameTests:
  - `originalCompelsVillagerDiscount`
  - `compelLesserVampireMakesSired`
  - `compulsionNeedsEyeContact`
  - `compulsionRefusedOnImmune`
  - `sirelineDeathOffByDefault`
  - `sirelineDeathKillsDescendants`
  - `offlineSiredDieOnLogin`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Two-client manual check of call and compulsion. Tick the sireline section of `docs/parts/species.md`. Update `CHANGELOG.md`. Commit "feat(sirelines): compulsion and optional sireline death".
