# Plan 12: The Supernatural Race System and the Hub, Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every player has a supernatural race (a creature race, plus a magic race for witches and siphoners). Each race has a ladder of rungs climbed through our tasks, Vampirism/Werewolves level steps and Enchanted rites, paying our skill points (SP) and Tensura EP. SP buy our species skills. One hub, opened from Tensura's menu or our key, shows and acts on all of it. Also in this plan: doppelgängers, genes, the Rite of Revelation, the compass's second mode, `hybrid_transition_blood`, and two new apex races offered from every Tensura line.

**Architecture:**
- The race is **computed**, never stored: `SupernaturalRace.creature/magic(state, faction)` in `core`.
- Ladder progress, SP and skills live in a **new attachment** `supernaturalbloodlines:supernatural` (`SupernaturalState`, own codec). Only the gene fields go into `BloodlineState.Extra`.
- Ladders and skill trees are **datapack JSON**, parsed by pure classes with unit tests (the `LayerTable` pattern). Detectors and skill effects are a fixed code set.
- Two **new parts**, each with its own 3-mixin budget:

| Part | Config key | Package | Required mods | Required parts | Mixins |
|---|---|---|---|---|---|
| `SUPERNATURAL` | `supernatural` | `parts.supernatural` | vampirism | — | 0, 1 reserved: Enchanted `Rite` completion (12a Task 3) |
| `SUPERNATURAL_TENSURA` | `supernaturalTensura` | `parts.supernaturaltensura` | vampirism, tensura | `SUPERNATURAL` | 0, 1 reserved: Tensura menu tab (12e Task 5) |

- Parts never import each other. Shared hooks live in `shared/supernatural/` (`TaskSignals`, `LadderQuery`, `EpReward`) and `shared/hud/LoadoutAccess`.

**Tech Stack:** NeoForge 1.21.1, Java 21, Vampirism 1.21-1.10.13, Werewolves 1.21-2.0.3.4, Enchanted 4.2.7, Tensura 2.0.1.5 / ManasCore 4.0.0.2, GameTests + JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-supernatural-system-design.md` (ids `L-*`, `S-*`, `Q-*`, owner decisions D1–D9). Keep every id exactly as in the spec.

## Phases

Each phase is one mergeable PR.

| Phase | Content | Tasks | Needs merged first | Can run in parallel with |
|---|---|---|---|---|
| **12a** | parts, attachment, races, ladders, tasks, claims, SP/EP, layer slicing, migration | 5 | plan 11 (Ghoul flag, bite refusal) | 12b |
| **12b** | doppelgängers, genes, Rite of Revelation, compass mode, `hybrid_transition_blood` | 5 | plan 11 (it edits `BloodlineState.Extra` too) | 12a |
| **12c** | skill catalogue and trees, skill effects, Predator's Thirst, Vampirism/Werewolves skill unlocking | 5 | 12a | 12d |
| **12d** | `elder_vampire`, `alpha_primeval`, apex offers from every line, apex rung gate | 3 | 12a | 12b, 12c |
| **12e** | hub screen model, Grimoire theme, all pages, launchers, key, Tensura tab | 5 | 12a, 12b, 12c (12d optional); plan 13 for the Loadout tab | — |
| **12f** | Blood Moon and Tensura-expansion themes | 2 | 12e | — |

**Not parallel:** 12c and 12e, 12d and 12e, 12e and 12f. 12c and 12d both add methods to `shared/supernatural/LadderQuery.java`: whichever merges second rebases. Every phase edits `en_us.json`, `CHANGELOG.md` and `SPECIES-RULES.md`; resolve those merges by keeping both sides.

## Global Constraints

- **Mixins:** none in the existing parts (all at 3/3). The two new parts start at 0/3. A reserved mixin is added only after the task shows no event works, with `require = 0` and a config switch, like the existing part mixin plugins.
- **Nothing earned is taken back.** No layer skill, level, EP, rank, apex race or item is removed by this plan. Rungs are a high-water mark. Migration only adds.
- **The rules stay the authority.** Species changes still go through `BloodlineRules`; this plan reads state and never sets species, faction or Tensura race.
- **Ghouls are frozen:** no task progress, no claim, no purchase while `ghoul`.
- **Every refusal explains itself** with lang keys under `supernaturalbloodlines.supernatural.*` (hub, ladders, skills) or `supernaturalbloodlines.gene.*` (12b).
- **Server decides, client shows.** Every C2S packet is validated on the server. Undiscovered genes are never sent to the client.
- **No per-tick scans.** Periodic checks run every 200 ticks per online player (the `RankSkills` pattern). The compass uses an index, never a world scan.
- **Data-first.** Ladders: `data/supernaturalbloodlines/supernatural/ladder/<race>.json`. Skills: `data/supernaturalbloodlines/supernatural/skill/<id>.json` and `.../tree/<race>.json`. Gate each file with `supernaturalbloodlines:part_enabled` and `neoforge:mod_loaded` as needed. A bad entry is rejected with one log line, never a crash.
- **Tensura is ARR.** The Tensura theme draws Tensura's textures by resource location at runtime; nothing is copied into the repo.
- **Test runs** (all must pass at the end of every task):
  - `build`
  - `runGameTestServer`
  - `-PnoWerewolves -PnoCurios`
  - `-PnoLinkedMods`
  - `-PpartsOff=species`
  - `-PpartsOff=speciesTensura`
  - From 12a Task 1 on, also `-PpartsOff=supernatural` and `-PpartsOff=supernaturalTensura`.
- **Docs in the same task as the code:** `SPECIES-RULES.md` (new rule ids as the spec suggests), a new `docs/parts/supernatural.md` and `docs/parts/supernatural_tensura.md`, the affected part docs, `CHANGELOG.md` (Unreleased), and the wiki (a new `wiki/Supernatural-Hub.md` linked from `_Sidebar.md`, plus the pages named per task).

## Review Focus

1. **Migration of an existing Original, Alpha and True Alpha.** Every layer skill and bonus stays, rungs are at least the layer/rank floor, SP is paid, no EP is paid. Tests: `migrationKeepsOriginalLayer`, `migrationAlphaFloor`, `migrationTrueAlphaDormantFloor` (12a Task 5).
2. **A witch-werewolf.** Both L-WIT and L-WOL tasks track and claim from one event where both match; both trees show; a shared skill is bought once. Tests: `witchWerewolfClimbsBothLadders` (12a Task 2), `sharedSkillBoughtOnce` (12c Task 1).
3. **A Ghoul.** No counter moves, claims and purchases are refused with a message, and after the turning the player is at L-VAM-0. Test: `ghoulFrozenThenVampire` (12a Task 4).
4. **Gene secrecy.** With `gene_visibility=discovered`, a client sync for a player with an unrevealed gene carries "unknown". Test: `undiscoveredGeneNotSynced` (12b Task 3).
5. **Predator's Thirst.** Without S-VAM-6, plan 11's bite refusal still happens; with it, the biter gains blood and the victim loses it; venom still applies for a hybrid biter. Tests: `predatorsThirstFeedsAndDrains`, `noSkillStillRefused`, `hybridBiterStillPoisons` (12c Task 4).
6. **Apex lines.** A hybrid on `tensura:vampire_lord` is offered `moonblood_hybrid` only after L-HYB-5; a werewolf on `tensura:spirit_beast` is offered `alpha_primeval`; an Original hybrid on Beastfolk is not offered from that line. Tests in 12d Task 2 and 3.
7. **Missing mods.** With `-PnoLinkedMods`, the hub key does nothing and nothing crashes; with `-PnoWerewolves`, the Pack page and every [WW] task are gone.

---

# Phase 12a: foundation

### Task 1: The two parts, the attachment and the race

**Files:**
- Modify: `core/Part.java`. Add `SUPERNATURAL("supernatural", "...parts.supernatural.SupernaturalPart", List.of("vampirism"))` and `SUPERNATURAL_TENSURA("supernaturalTensura", "...parts.supernaturaltensura.SupernaturalTensuraPart", List.of("vampirism", "tensura"))`, `requiredParts` of the latter = `SUPERNATURAL`.
- Modify: `core/Config.java` (part switches), `build.gradle` only if `partsOff` needs the new keys listed.
- Create: `core/supernatural/SupernaturalRace.java` (enum HUMAN, GHOUL, VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, TRIBRID, HERETIC, WITCH, SIPHONER, HUNTER, WEREWOLF; `creature(BloodlineState, String factionId)`, `magic(BloodlineState)`). No game classes.
- Create: `core/supernatural/SupernaturalState.java` (record + builder + codec): `schema`, `rungs`, `raceSince`, `sp`, `spEarned`, `done`, `claimed`, `counters`, `skills`, `quests`, `granted`, `backfill`. `withTrackingOf` for the server-only `granted` and `backfill`, as `BloodlineState` does.
- Modify: `core/attachment/ModAttachments.java`. Register `supernatural` with `copyOnDeath()`.
- Create: `core/supernatural/SupernaturalAccess.java` (get/set/listener, mirroring `BloodlineAccess`), `core/network/SupernaturalSyncPayload.java`, and the registration in `ModNetwork`.
- Create: `parts/supernatural/SupernaturalPart.java`, `parts/supernaturaltensura/SupernaturalTensuraPart.java` (empty `init` for now), and `parts/supernatural/SupernaturalConfig.java`.
- Test: `src/test/.../core/supernatural/SupernaturalRaceTest.java`, `SupernaturalStateTest.java`; update `PartLoaderTest`.

**Interfaces:**
- Produces: `SupernaturalRace.creature(...)`, `SupernaturalRace.magic(...): Optional<SupernaturalRace>`, `SupernaturalAccess.get/set/addListener`, `SupernaturalState.CODEC`.

- [ ] **Step 1: Write the failing unit rows.**
  - `SupernaturalRaceTest`: one row per line of both spec tables, including a witch with no faction (HUMAN + WITCH), a witch-werewolf (WEREWOLF + WITCH), a siphoner-werewolf, a tribrid (TRIBRID, no magic), a heretic (HERETIC, no magic), a Ghoul with a Tensura vampire race (GHOUL), a potential (HUNTER).
  - `SupernaturalStateTest`: codec round-trip; an empty tag loads `NONE`; server-only fields are dropped by the client view.
  - `PartLoaderTest`: the new part needs vampirism; `SUPERNATURAL_TENSURA` is off when `SUPERNATURAL` is off.
- [ ] **Step 2: Run** `.\gradlew.bat test`. Expected: FAIL.
- [ ] **Step 3: Implement.** If plan 11's `ghoul()` doesn't exist yet, stop and report: this phase needs plan 11.
- [ ] **Step 4: Run** all test runs, including the two new `partsOff` runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** Create `docs/parts/supernatural.md` and `docs/parts/supernatural_tensura.md` (scope, required mods, mixin budget 0/3). Update `wiki/Parts-And-Compatibility.md`. Commit `feat(supernatural): parts, state and race`.

### Task 2: Ladders as data and the task rules

**Files:**
- Create: `core/supernatural/Ladder.java`, `Rung.java`, `TaskSpec.java` (id, detector, args, source mod, `anyOf` members), `LadderTable.java` (parse + reject list, the `LayerTable` pattern), `LadderRules.java` (pure: active rung per ladder, record progress, mark done, claim, rung-up, any-of, missing-mod drop, Ghoul freeze).
- Create: the 12 JSON files `data/supernaturalbloodlines/supernatural/ladder/*.json` with every rung and task of the spec, ids exactly as written. Rewards per rung; the split across tasks is computed.
- Create: `parts/supernatural/LadderReloadListener.java` (`AddReloadListenerEvent`).
- Test: `src/test/.../core/supernatural/LadderTableTest.java`, `LadderRulesTest.java`, `LadderDataTest.java` (loads the shipped JSON).

**Interfaces:**
- Produces: `LadderRules.record(state, race, taskId, amount)`, `markDone`, `claim(state, taskId) -> ClaimResult(sp, ep, rungReached)`, `activeTasks(state, races)`, `LadderTable.get(race)`.

- [ ] **Step 1: Write the failing unit rows.**
  - Parser: a bad detector id, a duplicate id, an unknown race are rejected and logged; good entries survive.
  - `LadderDataTest`: every spec id from L-HUM-0 to L-WOL-6 exists; SP/EP totals equal the spec's "Rung counts" table.
  - Rules: claim split (3 SP over 2 tasks → 1 and 2); last claim reaches the rung; an any-of group pays once; a dropped-mod rung with no tasks is auto-reached; Ghoul: `record` and `claim` refuse.
  - `witchWerewolfClimbsBothLadders`: one `rite` event completes the matching task on L-WIT while L-WOL's active rung is unaffected; a level event completes L-WOL only.
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/supernatural.md` (JSON format with one example). Commit `feat(supernatural): ladders as data`.

### Task 3: Detectors and task signals

**Files:**
- Create: `shared/supernatural/TaskSignals.java` (`fire(Player, String id, long amount)`, a listener list; no-op without listeners).
- Create: `parts/supernatural/task/Detectors.java` (registers listeners): `FactionLevelChanged`, `BloodDrinkEvent.PlayerDrinkBloodEvent`, `LivingDeathEvent`, `LivingEntityUseItemEvent.Finish`, `BlockEvent.EntityPlaceEvent`, the `BloodlineAccess` and `SirelineIndex` change listeners, a 200-tick `PlayerTickEvent.Post` for `days_as`, `thirst_days` and state re-checks.
- Modify (one `TaskSignals.fire` line each, no logic change): compulsion (`parts/species/sireline/...Compulsion*`), siphon (`parts/species/witch/...`), `parts/species/hybrid/Venom.java`, the R32 dying-state handler in `parts/species/original/`, `shared/werewolves/ForcedShift.java`, `parts/artifacts/item/BoundRingItem.java` (Sun Ring bound), the mountain ash block placement in `artifacts`, plan 11's vampire-bite refusal, and every rite factory of ours (`rite:<id>:performer` / `:target` on success).
- Maybe create: `mixin/supernatural/RiteCompleteMixin.java` + `supernaturalbloodlines.supernatural.mixins.json` + `SupernaturalMixinPlugin` (follow `PartMixinPlugin`), registered in `neoforge.mods.toml`.
- Test: `parts/supernatural/gametest/DetectorGameTests.java`.

**Interfaces:**
- Consumes: Task 2. Human-like blood from plan 11's `isHumanLikeBlood` through a shared hook (never by importing `parts/species`).
- Produces: signal ids exactly as the spec's detector table.

- [ ] **Step 1: Write the failing GameTests.**
  - `levelStepCompletesTask`: `setFactionLevel(vampire, 2)` completes L-VAM-1a.
  - `levelReachedEarlyCountsOnActivation`: a level-5 vampire reaching L-VAM-1 finds L-VAM-2a already done.
  - `humanLikeFeedCounts` (a villager counts) and `nonHumanFeedIgnored`.
  - `riteSignalCompletesTask`: an Ancestral Calling performed by a witch on another player credits the performer's `rite(any, performer)`.
  - `compelSignalCounts`, `venomHitCounts`.
  - `ghoulRecordsNothing`.
  - `countingOnlyWhileActive`: a kill before the rung is active doesn't count.
- [ ] **Step 2: Run** `runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** Then check whether Enchanted fires anything on rite completion. If it does, use it. If not, add `RiteCompleteMixin` (`require = 0`, switch `supernatural.mixin.rite_complete`) and record the choice in the part doc. Without the mixin, only our rites count; say so on the Rites page later.
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** Part docs (each fire point), `wiki/Known-Limits.md` if Enchanted's own rites don't count. Commit `feat(supernatural): task detectors`.

### Task 4: Claims, SP, EP, sync and layer slicing

**Files:**
- Create: `parts/supernatural/net/ClaimTaskPayload.java` (C2S) and its server handler; `parts/supernatural/LadderService.java` (applies `LadderRules`, pays SP, calls `EpReward`, sends a toast, syncs).
- Create: `shared/supernatural/EpReward.java` (a settable hook; no-op by default) and `parts/supernaturaltensura/TensuraEpReward.java` (adds EP through `TensuraStorages.getExistenceFrom(p)`, then `markDirty()`; flat, no boost).
- Create: `shared/supernatural/LadderQuery.java`: `layerSlice(Player) -> Optional<LayerSlice(skills, magiculeFraction, auraFraction)>` (empty = the whole layer, today's behaviour), `rung(Player, race)`.
- Modify: `parts/speciestensura/SpeciesLayer.java`. Ask `LadderQuery.layerSlice` and grant only the slice; undead treatment and the EP boost stay from rung 0.
- Modify: `en_us.json` (`supernaturalbloodlines.supernatural.claim.*`, `.rung_reached`, `.ghoul_frozen`).
- Test: `parts/supernatural/gametest/ClaimGameTests.java`, `parts/speciestensura/gametest/LayerSliceGameTests.java`, a unit test of the slice table (the slices of each race add up to its `LayerTable` entry).

**Interfaces:**
- Consumes: Task 2, Task 3.
- Produces: `LadderService.claim(ServerPlayer, String)`, `LadderQuery.*`, `EpReward.pay(Player, long)`.

- [ ] **Step 1: Write the failing tests.**
  - `claimPaysSpAndEp` (EP only with Tensura), `claimTwiceRefused`, `claimOtherRaceRefused`, `ghoulFrozenThenVampire`.
  - `rungGrantsLayerSlice`: an Original at L-ORI-1 has `drain` and `self_regeneration`, not `charm`.
  - `supernaturalOffKeepsWholeLayer`: with the part off, `SpeciesLayer` grants the whole layer as today.
  - `sliceRemovalOnlyOurs`: lowering nothing ever happens, but a race change still takes back only `SpeciesLayer`'s own skills.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/species_tensura.md` (layer by rung), `wiki/Tensura-Integration.md`. Commit `feat(supernatural): claims, rewards and layer slices`.

### Task 5: Migration and debug commands

**Files:**
- Create: `parts/supernatural/Migration.java` (on `PlayerLoggedInEvent` when `schema == 0`, inside `ReentrancyGuard`).
- Create: `core/supernatural/MigrationRules.java` (pure: proven tasks, layer floor, rank floor, apex floor; SP for back-filled rungs, no EP).
- Modify: `core/command/DebugCommand.java`. Add `debug supernatural <player> show|rung <race> <n>|sp <n>|reset|backfill`.
- Test: `src/test/.../MigrationRulesTest.java`, `parts/supernatural/gametest/MigrationGameTests.java`.

**Interfaces:**
- Consumes: Tasks 1–4. Pack rank and `markKills` from `BloodlineState`.

- [ ] **Step 1: Write the failing tests.**
  - Unit: every floor alone and combined; a counting task is never proven; back-filled SP equals the sum of those rungs' SP; EP is 0.
  - GameTests: `migrationKeepsOriginalLayer`, `migrationAlphaFloor`, `migrationTrueAlphaDormantFloor`, `migrationWitchWerewolfBothLadders`, `migrationRunsOnce` (a second login changes nothing), `migrationApexRaceFloor` (a player already on `primordial_vampire` is at L-ORI-5).
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `wiki/Commands.md`, `wiki/For-Server-Owners.md` (what migration does), CHANGELOG. Commit `feat(supernatural): migration and debug commands`.

---

# Phase 12b: doppelgängers, genes and blood

All in `core`, `species` and `artifacts`. No new mixin. Independent of 12a.

### Task 1: Gene state, rolls and the admin command

**Files:**
- Modify: `core/bloodline/BloodlineState.java`. Add to `Extra`: `doppelgangerGene` (bool), `doppelgangerRolled` (bool), `revealedGenes` (set of strings: `werewolf`, `doppelganger`).
- Modify: `core/bloodline/BloodlineEvent.java` and `BloodlineRules.java` only if a reveal goes through the rules; otherwise a plain state update via `BloodlineAccess`.
- Modify: `parts/species/pack/GeneHandler.java` (roll the doppelgänger gene next to the werewolf gene at first join, and on the next login for players without `doppelgangerRolled`; mark `werewolf` revealed on the awakening kill and on the Rite of Lineage (Reveal)).
- Modify: `parts/species/SpeciesConfig.java`: `doppelganger_gene_chance` (0.05), `gene_visibility` (`discovered`|`always`).
- Modify: `parts/species/sireline/BloodlineCommand.java`: `/bloodline gene doppelganger <player> <true|false>` (op 2).
- Test: `BloodlineStateTest` (codec, old saves), `parts/species/gametest/GeneGameTests.java`.

- [ ] **Step 1: Write the failing tests.** Codec round-trip; `geneRolledOnceOnJoin`; `existingPlayerRolledOnNextLogin`; `bothGenesCoexist`; `adminCommandSetsGene`; `awakeningRevealsWerewolfGene`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `SPECIES-RULES.md` R14 and R10a, `wiki/Werewolves-And-Packs.md` (genes), `wiki/Commands.md`. Commit `feat(species): doppelganger gene`.

### Task 2: Doppelgänger villagers

**Files:**
- Create: `parts/species/doppelganger/DoppelgangerVillagers.java`: an entity attachment `doppelganger` (bool) plus `rolled` (bool), rolled once in `FinalizeSpawnEvent` (or `EntityJoinLevelEvent` for entities not yet rolled and not loaded from disk; check which is reliable and record it), and a per-level index of loaded doppelgänger villagers kept on `EntityJoinLevelEvent` / `EntityLeaveLevelEvent`.
- Modify: `core/attachment/ModAttachments.java` (the entity attachment), `SpeciesConfig` (`doppelganger_villager_chance`, 0.005).
- Test: `parts/species/gametest/DoppelgangerVillagerGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.** With the chance at 1.0, a spawned villager is a doppelgänger; at 0.0 it isn't; a reloaded villager keeps its roll and is never re-rolled; the index adds and removes on load/unload.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R14a, `docs/parts/species.md`. Commit `feat(species): doppelganger villagers`.

### Task 3: The doppelgänger rite taglock, the Rite of Revelation and gene sync

**Files:**
- Create: `parts/species/rite/DoppelgangerBloodRiteFactory.java`. Replace `enchanted:create_item` in `data/.../rite/doppelganger_blood.json` with it: same items and power; checks the taglock per `doppelganger_rite_taglock` (`doppelganger`|`human`), refuses with a reason, gives the bottle on success and reveals a player owner's gene with a message.
- Create: `parts/species/rite/RevelationRiteFactory.java` and `data/.../rite/rite_of_revelation.json` (small circle, 1,000 power, the subject's own filled taglock, part-gated).
- Modify: `core/network/BloodlineSyncPayload.java` (or its client view): send each gene as `UNKNOWN`/`TRUE`/`FALSE`, never the raw value when undiscovered under `discovered`.
- Modify: `SpeciesConfig` (`doppelganger_rite_taglock`), `en_us.json` (`supernaturalbloodlines.gene.*`).
- Test: `parts/species/gametest/RevelationGameTests.java`, `DoppelgangerRiteGameTests.java`, unit `GeneViewTest`.

**Interfaces:**
- Consumes: Task 1, Task 2. The taglock's owner is resolved by UUID: an online player or a loaded entity; otherwise the rite refuses ("the taglock's owner must be nearby or online"). Record this limit in Known Limits.

- [ ] **Step 1: Write the failing tests.** `riteRefusesNonDoppelgangerTaglock`; `riteAcceptsDoppelgangerVillager`; `riteAcceptsGenePlayerAndReveals`; `humanModeAcceptsAnyHumanLike`; `revelationRevealsAllGenes`; `undiscoveredGeneNotSynced`; `alwaysVisibilitySendsGenes`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R10b, `wiki/Rites.md` (Revelation row, Doppelgänger Blood taglock), `wiki/Known-Limits.md`. Commit `feat(species): rite of revelation and doppelganger taglocks`.

### Task 4: The Vampire Compass doppelgänger mode

**Files:**
- Modify: `parts/artifacts/item/VampireCompassItem.java`: a `compass_mode` data component; sneak + use toggles it (no target search on the toggle); tooltip shows the mode; Doppelgängers mode searches the Task 2 index within `compass_doppelganger_range` (512) and online players with the gene in the same dimension.
- The index is in `species`: reach it through a shared hook (`shared/species/DoppelgangerIndex` or similar), never by importing `parts/species`.
- Modify: the artifacts config (`compass_doppelganger_range`), `en_us.json`.
- Test: `parts/artifacts/gametest/CompassGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.** `sneakUseTogglesMode`; `pointsAtNearestDoppelgangerVillager`; `ignoresVillagerOutOfRange`; `pointsAtGenePlayerAnywhereInDimension`; `targetGetsNoMessage`; `vampireModeUnchanged`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `wiki/Items-And-Blocks.md`, `docs/parts/artifacts.md`. Commit `feat(artifacts): compass doppelganger mode`.

### Task 5: `hybrid_transition_blood`

**Files:**
- Modify: `parts/species/rite/OriginalBloodRiteFactory.java`: write the source's species into a data component on the bottle.
- Modify: `parts/species/hybrid/TransitionHandler.java`: on drinking, check the component against `hybrid_transition_blood` (`original_hybrid` default | `original`). A bottle with no component is accepted. A refusal keeps the bottle and tells the player.
- Modify: `SpeciesConfig`, `en_us.json`.
- Test: `parts/species/gametest/HybridGameTests.java` (new rows).

- [ ] **Step 1: Write the failing GameTests.** `originalHybridBloodStartsTransition`; `plainOriginalBloodRefusedByDefault`; `plainOriginalBloodAcceptedInOriginalMode`; `legacyBottleAccepted`; `refusedBottleKept`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R40a, `wiki/Hybrids-And-Original-Hybrids.md`, `wiki/Configuration.md`. Commit `feat(species): hybrid transition blood setting`.

---

# Phase 12c: skills

### Task 1: Catalogue, trees, purchase and dormancy

**Files:**
- Create: `core/supernatural/SkillSpec.java`, `SkillTree.java`, `SkillTable.java` (parse + reject), `SkillRules.java` (pure: can buy, why not, active vs dormant across creature and magic trees).
- Create: the JSON: 40 catalogue skills and 11 trees, ids and costs exactly as the spec.
- Create: `parts/supernatural/net/BuySkillPayload.java` + handler; `parts/supernatural/skill/SkillEffects.java` (applies ATTR modifiers with our ids on login, respawn, race change and purchase; removes them when dormant); `parts/supernaturaltensura/TensuraSkillEffects.java` (TENSURA kind through `SkillOwners` source `supernatural_skill`, tracked in `granted`).
- Modify: `shared/supernatural/LadderQuery.java`: `owns(Player, String skillId)` (true only when owned **and** active).
- Test: unit `SkillTableTest`, `SkillRulesTest`, `SkillDataTest`; GameTests `SkillGameTests`.

- [ ] **Step 1: Write the failing tests.**
  - Data: every tree's SP total matches the spec; every listed id exists.
  - Rules: rung, prerequisite, quest and SP checks each refuse with their own reason; Ghoul refused.
  - `sharedSkillBoughtOnce` (a witch-werewolf buying S-WOL-1 owns it in both trees; the cost is paid once).
  - `dormantSkillRemovesModifier` (a vampire cured to human loses S-VAM-4's +2 health; rejoining brings it back).
  - `tensuraSkillOnlyOursRemoved`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/supernatural.md` (skills), `wiki/Supernatural-Hub.md` (skills section). Commit `feat(supernatural): skill trees`.

### Task 2: RULE effects in `species` and `artifacts`

**Files** (each a read of `LadderQuery.owns(p, id)` where the number is used; no other behaviour change):
- `species`: compulsion (S-VAM-3), Original's Blood rite count (S-ORI-2), `/bloodline call` cooldown (S-ORI-3), `BloodThirst` (S-HYB-1), `Venom` (S-HYB-2), `ForcedShift` (S-WOL-2), wolfsbane (S-WOL-4), `PackBonus` (S-WOL-5), `AlphaRoar` (S-WOL-6), `RiteCost` / altar range (S-WIT-1, S-WIT-2, S-WIT-6), `Emissary` (S-WIT-4), siphon cap, rate, heal, rite payment, weakness (S-SIP-1..5), hunters: prey damage, sense radius, instinct, mark caps (S-HUN-1, -2, -3, -5).
- `artifacts`: vervain tea duration (S-HUM-2), mountain ash breaking (S-WIT-3).
- `vampirismtensura` or `species`: Vampirism sun damage (S-VAM-7), through `LivingDamageEvent` on Vampirism's sun damage type.
- `speciestensura`: the extra magicule/aura bonus (S-ORI-4, S-WIT-7) added to the layer attributes.
- Test: one GameTest per skill, in the part that owns the rule (`*SkillRuleGameTests.java`).

- [ ] **Step 1: Write the failing GameTests**, each comparing with and without the skill.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.** If a rule's number isn't reachable without a mixin, skip that skill (hidden in its tree) and report it.
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `SPECIES-RULES.md` note under each touched rule ("a skill can change this number"), the part docs. Commit `feat(species): skill-tuned rules`.

### Task 3: BLOOD effects and the human-like feed

**Files:**
- Modify: the `species` feeding listener (or a new `parts/supernatural/skill/BloodEffects.java`): S-VAM-1 adds 1 blood on a human-like feed through `PlayerDrinkBloodEvent` (check whether the amount is settable; if not, add the blood after the drink).
- Test: `BloodSkillGameTests.keenFangsAddsBlood`.

- [ ] **Step 1–4** as above. Expected: FAIL, then PASS on all runs.
- [ ] **Step 5: Commit** `feat(supernatural): keen fangs`.

### Task 4: Predator's Thirst and Q-VAM-6

**Files:**
- Create: the quest JSON `data/.../supernatural/quest/q-vam-6.json` (tasks a–c of the spec; listed in every vampire-type tree).
- Modify: plan 11's vampire-bite refusal (`parts/species/turning/GhoulGates.java` or where it landed): skip the refusal when `LadderQuery.owns(p, "S-VAM-6")`; then feed and drain: the victim's `vampirism:entity_blood` or `vampire_feed_blood` (config, 3); a vampire player victim loses that blood; a mob takes 2 damage and Weakness I for 10 s; an Original is never killed by the drain.
- Modify: `SupernaturalConfig` (`vampire_feed_blood`), `en_us.json`.
- Test: `parts/supernatural/gametest/PredatorsThirstGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.** `questOpensAtRung`; `questCompletes`; `cannotBuyWithoutQuest`; `predatorsThirstFeedsAndDrains`; `noSkillStillRefused`; `hybridBiterStillPoisons`; `originalVictimSurvivesDrain`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `SPECIES-RULES.md` (a line under plan 11's bite rule), `wiki/Vampires-And-Originals.md`. Commit `feat(supernatural): predator's thirst`.

### Task 5: Vampirism and Werewolves skill unlocking from the hub (server side)

**Files:**
- Create: `parts/supernatural/net/UnlockModSkillPayload.java` + handler: resolve the faction player, call its skill handler's own can-enable check and enable path (verify the exact `ISkillHandler` methods in 1.21-1.10.13 and whether Werewolves uses the same handler; record them in the part doc). Refuse for Ghouls and with the mod's own reason.
- Test: `parts/supernatural/gametest/ModSkillGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.** `unlockSpendsModPoints`; `unlockRefusedWithoutPoints`; `unlockRefusedWithoutParent`; `werewolfSkillUnlock` (skipped under `-PnoWerewolves`); `ghoulRefused`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.** No mixin; if the handler isn't reachable through the API, stop and report.
- [ ] **Step 4: Run** all test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** Part doc. Commit `feat(supernatural): unlock faction skills`.

---

# Phase 12d: apex additions

All in `species_tensura` (no new mixin: `RaceInstanceMixin` already hooks `getNextEvolutions`).

### Task 1: `elder_vampire` and `alpha_primeval`

**Files:**
- Modify: `parts/speciestensura/apex/ApexSpec.java`: the two specs (spec stats), `forRace(SupernaturalRace)` replacing `forSpecies` (callers updated).
- Modify: `ApexRaces.java` (registration), the race tags (`human_like`, `supernaturalbloodlines:bitable`, `tensura:undead` for `elder_vampire` only), lang and placeholder textures.
- Test: `ApexRacesTest`, `ApexGameTests` (new rows).

- [ ] **Step 1: Write the failing tests.** Both races register; costs are 2,000,000 EP; `alpha_primeval` is not undead (R103); both are bitable.
- [ ] **Step 2–4** FAIL, implement, PASS on all runs.
- [ ] **Step 5: Docs and commit.** `SPECIES-RULES.md` R100 table (two rows), `wiki/Tensura-Integration.md`. Commit `feat(species_tensura): two new apex races`.

### Task 2: Offers from every line

**Files:**
- First verify in Tensura's jar: the vampire line ids and tiers (`vampire_lord`, `vampire_overcomer`, `divine_vampire`) and the Beastfolk line (`spirit_beast`, `divine_beast`). Record them in the part doc.
- Create: `parts/speciestensura/apex/ApexLines.java` (pure: line of a race id, tier check, allowed races per line, per the spec table).
- Modify: `ApexOffer.apexIdFor` to use `ApexLines`.
- Test: unit `ApexLinesTest`, `ApexOfferTest` rows.

- [ ] **Step 1: Write the failing rows.** A hybrid on `vampire_lord` and on `spirit_beast` both qualify; a tribrid on `vampire_lord` doesn't; an Original hybrid on `spirit_beast` doesn't; a werewolf on `beastfolk` (below the tier) doesn't; Saint rules unchanged.
- [ ] **Step 2–4** FAIL, implement, PASS on all runs.
- [ ] **Step 5: Docs and commit.** R100a in `SPECIES-RULES.md`. Commit `feat(species_tensura): apex offers from every line`.

### Task 3: The apex rung gate

**Files:**
- Modify: `shared/supernatural/LadderQuery.java`: `apexRungReached(Player, race)`; answers `true` when the `supernatural` part is off (today's behaviour).
- Modify: `ApexOffer` to require it.
- Test: `ApexGameTests.apexNeedsTopRung`, `apexWithoutSupernaturalPartAsToday`, `witchWerewolfOfferedBothApexes`.

- [ ] **Step 1–4** FAIL, implement, PASS on all runs.
- [ ] **Step 5: Docs and commit.** `docs/parts/species_tensura.md`. Commit `feat(species_tensura): apex needs the top rung`.

---

# Phase 12e: the hub with the Grimoire theme

Client code in `parts/supernatural/client/` (and `parts/supernaturaltensura/client/` for Tensura-only bits), registered on `Dist.CLIENT` only.

### Task 1: Screen model, themes and the snapshot

**Files:**
- Create: `client/hub/HubScreen.java` (state: page, selection; rebuilds on theme change), `HubPage` (Path, Skills, Quests, Rites, Bloodline, Pack; `visible()` per installed mod and race), `HubTheme` interface (regions, draw calls), `HubClientConfig` (`hub_theme`, default `GRIMOIRE`).
- Create: `net/HubSnapshotRequest` (C2S) / `HubSnapshotPayload` (S2C): sire/sired names, pack members and ranks, Five count, severance, genes (as the client view from 12b).
- Test: unit `HubPageVisibilityTest` (pure: pages per mods and race); GameTest `snapshotHasNoOtherPlayersAttachments`.

- [ ] **Step 1–4** FAIL, implement, PASS on all runs.
- [ ] **Step 5: Commit** `feat(supernatural): hub model`.

### Task 2: Grimoire theme, Path and Quests

**Files:** `client/hub/theme/GrimoireTheme.java`, `client/hub/page/PathPage.java`, `QuestsPage.java`; placeholder textures under `assets/supernaturalbloodlines/textures/gui/hub/grimoire/`; lang keys.
- [ ] **Step 1:** Client-safe unit tests for layout math (two pages, ribbons) where pure.
- [ ] **Step 2: Implement:** left page story and ladder (both ladders for a magic race), right page active tasks with claim stamps (sends `ClaimTask`); Ghoul view; keyboard navigation.
- [ ] **Step 3: Run** all test runs, then the manual check: GUI scale 2 and 4, a vampire, a witch-werewolf, a Ghoul.
- [ ] **Step 4: Commit** `feat(supernatural): grimoire path and quests`.

### Task 3: Skills page and Loadout

**Files:** `client/hub/page/SkillsPage.java` (our trees, dormant marks, buy; faction tree tabs drawn from the mod's client data, unlock through `UnlockModSkill`), `LoadoutTab.java` (plan 13's 6 slots through `shared/hud/LoadoutAccess`; hidden when plan 13's part is absent).
- [ ] If plan 13 hasn't merged, build the tab behind `LoadoutAccess.available()` returning false and note it in the PR.
- [ ] Run all test runs; manual check of buy, unlock and assign.
- [ ] **Commit** `feat(supernatural): skills page`.

### Task 4: Rites, Bloodline, Pack and launchers

**Files:** `client/hub/page/RitesPage.java`, `BloodlinePage.java` (genes as Unknown / Yes / No), `PackPage.java` (pack actions through the existing `/pack` commands), `client/hub/Launchers.java` (Enchanted book, Vampirism and Werewolves skill screens, and in `supernaturaltensura` the Tensura screen and live EP), `client/hub/ReturnToHub.java` (`ScreenEvent.Closing` + reopen next tick on the saved page).
- [ ] Unit test the return rule (pure state). Manual: each launcher opens unchanged, Esc returns to the same page; with `-PnoWerewolves` the Pack page is gone.
- [ ] Run all test runs. **Commit** `feat(supernatural): bloodline, rites and pack pages`.

### Task 5: The key and Tensura's 5th tab

**Files:** `client/HubKeybinds.java` (`key.supernaturalbloodlines.hub`, default `J` unless plan 13's keymap says otherwise); `parts/supernaturaltensura/client/TensuraMenuTab.java` (`ScreenEvent.Init.Post` on Tensura's menu screen, class matched by name, under the three "coming soon" tabs; log once if not found).
- [ ] Find Tensura's menu screen class and tab positions in the jar first. Only if the event can't place the tab, add the reserved mixin in `supernatural_tensura` (`require = 0`, switch) and record why.
- [ ] Run all test runs. Manual: the tab opens the hub; with `-PpartsOff=supernaturalTensura` there's no tab and the key still works.
- [ ] **Docs and commit.** `wiki/Key-Binds.md`, `wiki/Supernatural-Hub.md` (screens), CHANGELOG. Commit `feat(supernatural): hub key and tensura tab`.

---

# Phase 12f: the other themes

### Task 1: Blood Moon

**Files:** `client/hub/theme/BloodMoonTheme.java`, textures under `.../gui/hub/blood_moon/`.
- [ ] The moon fills with progress to the next rung (both ladders as two arcs for a magic race); left rail pages; right rail launchers and SP/EP counters.
- [ ] Manual check of every page at GUI scale 2 and 4; switching theme keeps the page. Run all test runs. **Commit** `feat(supernatural): blood moon theme`.

### Task 2: Tensura expansion

**Files:** `parts/supernaturaltensura/client/TensuraHubTheme.java`, drawing Tensura's own textures by resource location (find them in the jar; nothing copied). Without Tensura, `hub_theme=TENSURA` falls back to Grimoire.
- [ ] Unit test the fallback rule. Manual check of every page. Run all test runs.
- [ ] **Docs and commit.** `wiki/Supernatural-Hub.md` (themes, screenshots later), `wiki/Configuration.md` (`hub_theme`). Commit `feat(supernatural): tensura hub theme`.
