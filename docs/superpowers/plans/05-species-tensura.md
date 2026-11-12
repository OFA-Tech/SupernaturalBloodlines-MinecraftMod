# Part `species_tensura` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Species become a **layer on the player's Tensura race** (R100):
- The race is never replaced.
- Each species adds Tensura skills, attributes and an EP bonus.
- Tensura treats vampire types as undead.
- Each species boosts evolution and unlocks one **apex evolution** in Tensura's own evolution screen.

Sires become Tensura owners of their sired, and can **name** them through Tensura's naming (R101). Werewolf ranks grant Tensura skills.

**Architecture:** `parts.speciestensura`. A `BloodlineAccess` listener applies the layer whenever the species or wolf state changes; skills granted by this mod are tracked so only they are removed. The apex races are registered in ManasCore's `manascore_race:races`. A hook found in Task 1 offers them in Tensura's evolution list: an event if one exists, otherwise one `require = 0` mixin on the evolution-options method. "Treated as undead" uses the smallest hook available: an event, otherwise a `require = 0` mixin on `RaceUtils.isUndead` / `isAffectedByHolyExposure`. Mixins are listed in `docs/mixins.md`.

**Tech Stack:** As foundation, plus the ManasCore race/skill API, Tensura `TensuraRace` / `RaceHelper` / `RaceUtils` / `TensuraStorages` / `SubordinateHelper`, and the naming packet classes.

**Spec:** `SPECIES-RULES.md` R100–R103, and design section 10.

## Global Constraints

- Everything in foundation's Global Constraints.
- **Never `setRace` for a species change.** The only race changes this part makes are the ones the player picks in Tensura's evolution screen (apex races).
- Apex races (working ids, the owner can rename):

| Species | Apex race |
|---|---|
| ORIGINAL | `supernaturalbloodlines:primordial_vampire` |
| ORIGINAL_HYBRID | `primordial_hybrid` |
| HYBRID | `moonblood_hybrid` |
| WITCH | `ancestral_witch` |
| SIPHONER / HERETIC | `void_siphon` |
| TRIBRID | `tribrid_ascendant` |
| HUNTER_OF_THE_FIVE | `eternal_hunter` |

- Apex races are offered only to a player whose current race is tagged `tensura:human_like` and is at the "saint / overcomer" tier or higher of its line (Task 1 defines the exact list from the race ids), and only while the player has the matching species.
- Apex races are tagged `tensura:human_like`, and vampire-type ones also `tensura:undead`.
- Layer skills are tracked in a `grantedSkills` set (one for the species layer, a separate one in `WolfState` for ranks). Only tracked skills are ever removed.

## Review Focus

- **A player with a species who evolves normally in Tensura** (e.g. elf → enlightened elf) must keep the species layer and its skills. Pinned by `layerSurvivesTensuraEvolution`.
- **Losing the species after picking an apex race:** the race stays, and its species-only skills go dormant (removed and tracked) until the species is regained. Pinned by `apexKeptWhenSpeciesLost`.
- **A skill the player already had before the layer** must not be removed when the layer goes. Pinned by `preexistingSkillKept`.
- **A sire trying to name a sired player when the server's player-naming setting is off** must work only for their own sired, and never for anyone else. Pinned by `sireCanNameOwnSiredOnly`.
- **The evolution hook mixin fails to apply** (Tensura update): no apex options and a warning in the log; species keep working. Pinned by `apexHookMissingIsHarmless` (config switch simulation).

---

### Task 1: Discovery

**Files:**
- Create: `docs/parts/species_tensura.md`, "Hook points (verified)", recording (from `javap` on the Tensura and ManasCore race jars):
  - `TensuraRace` abstract methods and constructor
  - how evolution options are built for the evolution screen: `getNextEvolutions`, any event or packet such as `setTrackedEvolution`, and `RaceHelper.evolveRace`
  - `getEvolutionRequirements` / `EvolutionRequirement` and how progress is counted
  - `RaceUtils.isUndead`, `isAffectedByHolyExposure` and `isBloodless`, and whether an event can override them
  - race tag loading
  - the full list of human-like race ids and which are "saint / overcomer tier or higher"
  - naming: `RequestNamingKeyPacket.canName`, the `playerNaming` gamerule check, `NAMING_EVENT`, `IExistence.isNameable`
  - how to clear `setPermanentOwner`
  - the commander-type skill id
- [ ] **Step 1:** Write the notes. Commit "docs: species_tensura hook points".

### Task 2: Species layer (skills, attributes, EP, undead treatment)

**Files:**
- Create: `parts/speciestensura/SpeciesLayer.java`, `parts/speciestensura/LayerSpec.java` (`record LayerSpec(List<String> skills, double maxMagiculeBonus, double maxAuraBonus, boolean treatAsUndead)`, plus a config map `species_tensura.layers` with defaults below), `parts/speciestensura/UndeadTreatment.java` (an event or a `mixin/speciestensura/RaceUtilsMixin.java`), and a `SpeciesTensuraPart.init` that registers the Tensura `Supernatural` predicate (races tagged `tensura:daemon` or `tensura:undead`) and installs the `TensuraHumanLikeCheck` into plan 4's `SpeciesEligibility`
- Modify: `docs/mixins.md`

**Interfaces:**
- **Default layers** (verify every skill id in Task 1 and drop missing ones):

| Species | Skills | Bonus | Treated as undead |
|---|---|---|---|
| ORIGINAL | blood_mist, drain, charm, self_regeneration | +20% max magicule | yes |
| ORIGINAL_HYBRID | ORIGINAL + beast_transformation, godwolf_sense | +25% | yes |
| HYBRID | drain, beast_transformation, self_regeneration | +10% | yes |
| WITCH | none | +30% max magicule, +10% max aura | no |
| SIPHONER | drain | +10% | no |
| HERETIC | drain, charm, self_regeneration | +20% | yes |
| TRIBRID | HYBRID + charm | +30% | yes |
| HUNTER_OF_THE_FIVE | none | +10% max aura | no |
| a plain Vampirism vampire (no species; R20) | drain | none | yes |

- **On every listener change and on login:** compute the target layer, grant the missing skills (tracked), forget tracked skills no longer in the layer, and set the attribute modifiers `supernaturalbloodlines:layer_magicule` / `layer_aura` on `TensuraAttributes.MAX_MAGICULE` / `MAX_AURA`.
- **`UndeadTreatment`:** Tensura's undead and holy-exposure checks return true for players whose layer has `treatAsUndead`.
- **Plan 01 interplay:** `FactionRaceRules` (plan 01) must exempt vampire-type species from the undead/daemon werewolf-join denial and hunter ejection (R103); `VampireSync` already skips species players.
- **Holy stacking:** `HolyDamageBridge` must not stack with the species `treatAsUndead` holy treatment; make it skip players whose layer has `treatAsUndead`, or document the intended stacking.

- [ ] **Step 1:** Write the GameTests:
  - `originalElfKeepsRaceAndGetsSkills`
  - `layerSurvivesTensuraEvolution`
  - `preexistingSkillKept`
  - `curedLayerRemovesOnlyGrantedSkills`
  - `vampireLayerTakesTensuraHolyDamage`
  - `witchGetsMagiculeBonus`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species_tensura): species layer on the race".

### Task 3: Evolution bonus and apex races

**Files:**
- Create: `parts/speciestensura/apex/ApexRace.java` (one class extending `TensuraRace`, configured by `record ApexSpec(String id, double health, double attack, double speed, List<String> intrinsicSkills, boolean undead)`), `parts/speciestensura/apex/ApexRaces.java` (registration + `Optional<ResourceLocation> apexFor(Species)`), `parts/speciestensura/apex/ApexOffer.java` (the evolution-options hook from Task 1; an event or `mixin/speciestensura/EvolutionOptionsMixin.java` with `require = 0` and config `apex_evolutions_enabled`), and `parts/speciestensura/apex/EvolutionBoost.java`
- Create the race tags under `data/tensura/tags/manascore_race/races/`: `human_like.json` (all apex races) and `undead.json` (primordial_vampire, primordial_hybrid, moonblood_hybrid, void_siphon, tribrid_ascendant)
- Create the lang entries

**Interfaces:**
- **`ApexOffer`:** when Tensura builds a player's next-evolution list, append `apexFor(species)` if the current race is in the Task 1 high-tier list and the player has that species.
- **`EvolutionBoost`:** a species with a layer multiplies the progress counted toward the current line's evolution requirements by `evolution_progress_multiplier` (config, default 1.25). Use the requirement hook from Task 1, or skip it with a note if there's no clean hook.
- **Apex intrinsic skills** are the species layer skills + one signature skill each (owner to design; placeholder: the strongest existing related Tensura skill from Task 1). `getNextEvolutions` of apex races is empty.
- **Species lost after picking an apex race:** the race stays, and the species layer skills are removed (tracked) until the species is regained (`apexKeptWhenSpeciesLost`).

- [ ] **Step 1:** Write the tests:
  - unit `ApexRacesTest.everyApexSpeciesHasRace`
  - unit `langHasEveryApexRace`
  - GameTests:
    - `apexOfferedAtHighTierWithSpecies`
    - `apexNotOfferedWithoutSpecies`
    - `apexNotOfferedToNonHumanLike`
    - `apexKeptWhenSpeciesLost`
    - `apexHookMissingIsHarmless`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species_tensura): apex evolutions and evolution boost".

### Task 4: Sireline as Tensura subordinates, and naming (R101)

**Files:**
- Create: `parts/speciestensura/SubordinateLink.java`, `parts/speciestensura/SireNaming.java`

**Interfaces:**
- **Link:** when plan 8's `SireRecorder` records a sire: `IExistence e = TensuraStorages.getExistenceFrom(child); e.setPermanentOwner(sireUuid); e.markDirty();`. If the child is an `ISubordinate` mob and the sire is online, also call `tame(sirePlayer)`.
  - Never overwrite a different existing permanent owner (log it and keep Tensura's owner).
  - When the bond ends (cure or species loss), clear the owner (method from the Task 1 notes) and `markDirty()`.
- **Naming:** a sire may open Tensura's naming on their own sired, players included, even when the `playerNaming` gamerule is off.
  - Use `NAMING_EVENT` if `canName` can be bypassed through it. Otherwise use a `require = 0` mixin on `RequestNamingKeyPacket.canName` that returns true when `SirelineIndex.sireOf(target) == namer`.
  - Tensura's normal costs and rewards apply. For player sired, the reward is the EP and stat gain only, with no race change (R100).

- [ ] **Step 1:** Write the GameTests:
  - `siredPlayerBecomesSubordinate` (`SubordinateHelper.isSubordinate(child, sire)`)
  - `siredMobKillCreditsSire`
  - `cureRemovesSubordinateLink`
  - `existingTensuraOwnerKept`
  - `sireCanNameOwnSiredOnly`
  - `namingSiredPlayerDoesNotChangeRace`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species_tensura): sireline subordinates and naming".

### Task 5: Werewolf ranks as Tensura skills

**Files:**
- Create: `parts/speciestensura/RankSkills.java` (config section `species_tensura.rank_skills` as `"RANK=skill,skill"` strings; defaults `ALPHA=tensura:beast_transformation,tensura:godwolf_sense`, `TRUE_ALPHA=` the Alpha skills + the commander skill from Task 1, `BORN_LEVEL5=tensura:godwolf_sense`)

**Interfaces:**
- On a listener change of `wolf.rank`, or of `wolf.lineage` + Werewolves level, grant the missing skills (tracked in `WolfState.grantedSkills`), and forget only tracked skills when the rank is lost. The race never changes.

- [ ] **Step 1:** Write the GameTests:
  - `alphaGetsRankSkills`
  - `losingAlphaRemovesOnlyGrantedSkills`
  - `bornLevelFiveGetsGodwolfSense`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species_tensura): werewolf rank skills".

### Task 6: Checks

- [ ] **Step 1:** In game, with all four mods: take an elf through Original → apex evolution offer; a human through witch; a sire naming a sired player; an Alpha getting skills. Tick `docs/parts/species_tensura.md`.
- [ ] **Step 2:** Turn `species_tensura` off with a world that has an apex-race player. Expected: the player loads (ManasCore's unknown-race fallback). If ManasCore doesn't fall back, add a login check in core that resets unregistered `supernaturalbloodlines:*` races to `tensura:human`. Record the result.
- [ ] **Step 3:** Update `CHANGELOG.md`. Commit "feat(species_tensura): verified".
