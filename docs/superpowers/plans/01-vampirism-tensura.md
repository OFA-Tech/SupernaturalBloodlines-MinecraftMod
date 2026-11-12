# Part `vampirism_tensura` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tensura vampires and Vampirism vampires become one vampire. Hunters and werewolves respect Tensura races. Holy damage works in both mods, and bloodless races can't be bitten.

**Architecture:** All code is in `net.ofatech.supernaturalbloodlines.parts.vampirismtensura`. A pure `VampireMapping` decides the race ↔ level rules and is unit-tested. Thin listeners translate Vampirism events (NeoForge bus) and ManasCore events (Architectury `RaceEvents.SET_RACE`) into mapping calls, wrapped in `ReentrancyGuard.run(uuid, "vampire_sync", …)`. Data files carry the EP entries and the blood values.

**Tech Stack:** As foundation, plus the Vampirism API, ManasCore race/skill API, Tensura `TensuraStorages`/`RaceUtils`, and Werewolves (optional).

**Spec:** `docs/superpowers/specs/2026-11-11-supernaturalbloodlines-design.md`, section "3. `vampirism_tensura`" and "Unified vampire priority".

## Global Constraints

- Everything in foundation's Global Constraints.
- **R20 (round 3): the Tensura race is never changed by joining Vampirism.** Only Tensura's own vampire line syncs *into* the Vampirism faction (level floor). A cure resets the race to `cure_race` (default `tensura:human`) only if the race is in Tensura's vampire line. Vampirism level-ups never evolve the race.
- Default mapping:
  - `tensura:ghoul` → no faction change
  - `tensura:vampire` → level ≥ 1
  - `tensura:vampire_overcomer` and `tensura:vampire_lord` → level ≥ 7
  - `tensura:divine_vampire` → maximum vampire level
- ~~Join race default: `tensura:vampire`.~~ Superseded by R20 (joining Vampirism never changes the race; no `join_race` option).
- Werewolf parts load only if `ModList.isLoaded("werewolves")`, and no Werewolves class is linked from main code (the faction is matched by id).

## Review Focus

- **A player who is already a high-level Vampirism vampire and evolves in Tensura** keeps their higher level. Pinned by `VampireMappingTest.floorNeverLowersLevel`.
- **A cure while the player's Tensura race is not in the vampire line** (e.g. a species race) must not overwrite that race. Pinned by `VampireMappingTest.cureOnlyResetsVampireLine`.
- **`/vampirism level` or another command that sets the faction directly** must still sync once and not loop. Pinned by the GameTest `commandFactionChangeSyncsOnce`.
- **A Tensura race change on the client side or for non-players** (mobs with races) must be ignored. Pinned by `RaceSyncListenerGameTest.mobRaceChangeIgnored`.
- **Werewolves absent** must not throw `NoClassDefFoundError`. Pinned by the Task 6 clean run with `-PnoWerewolves`.

---

### Task 1: Discovery notes

**Files:**
- Create: `docs/parts/vampirism_tensura.md`, a section "Hook points (verified)".

- [ ] **Step 1:** Using `javap -p` on the Tensura jar from the Gradle cache, record:
  - whether `config.race.VampireConfig` has a sun-burn switch, and its exact name
  - the exact signature of `VampireRace.shouldBurn`
  - the `RaceEvents.SET_RACE` functional interface signature
  - the names of the `Races.setRace` overloads
  - the `tensura:human` race id
  - the `TensuraDamageHelper.isHoly` signature
- [ ] **Step 2:** From the Vampirism API sources jar, record:
  - the `PlayerFactionEvent.CanJoinFaction` / `FactionLevelChanged` field accessors
  - `IFactionPlayerHandler.setFactionAndLevel`
  - the max-level accessor of the vampire faction
  - whether sun damage has an event or must be cancelled via `LivingIncomingDamageEvent` on Vampirism's sun damage type
- [ ] **Step 3:** Commit "docs: vampirism_tensura hook points".

### Task 2: `VampireMapping` (pure)

**Files:**
- Create: `parts/vampirismtensura/VampireMapping.java`, `parts/vampirismtensura/VampireSyncConfig.java` (ModConfigSpec section `vampirism_tensura`: `cure_race`, and `race_level_floor` as a list of `"race=level"` strings)
- Test: `src/test/java/.../parts/vampirismtensura/VampireMappingTest.java`

**Interfaces:**
- **`record VampireMapping(String cureRace, Map<String,Integer> floors, int maxLevel)`**, with:
  - `OptionalInt levelForRace(String raceId, int currentLevel)`. Empty when the race isn't in the vampire line; otherwise `max(floor, currentLevel)`. `-1` in the config means `maxLevel`.
  - `Optional<String> raceOnCure(@Nullable String currentRace)`. Returns `cureRace` only if the current race is in the vampire line (the floors keys plus `tensura:ghoul`).
  - `static VampireMapping defaults(int maxLevel)`

- [ ] **Step 1:** Write the tests:
  - `vampireRaceGivesLevelOne`
  - `floorNeverLowersLevel`: vampire_lord at current 12 → 12
  - `lordFloorSeven`: current 3 → 7
  - `divineIsMax`
  - `ghoulAndHumanGiveEmpty`
  - `cureOnlyResetsVampireLine`: species race `supernaturalbloodlines:hybrid` → empty
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Commit "feat(vampirism_tensura): vampire mapping".

### Task 3: Two-way vampire sync

**Files:**
- Create: `parts/vampirismtensura/VampireSync.java`
- Modify: `VampirismTensuraPart.init`, to register the listeners
- Test: `src/main/java/.../gametest/VampirismTensuraGameTests.java`

**Interfaces:**
- Consumes `VampireMapping`, `ReentrancyGuard.run`, and the hook signatures from Task 1.
- Produces the listeners:
  - **On `PlayerFactionEvent.FactionLevelChanged`:** joining the vampire faction doesn't change the race (R20). Leaving it → `raceOnCure`.
  - **On `RaceEvents.SET_RACE`, server and players only:** call `levelForRace`; then `setFactionAndLevel(vampireFaction, level)`, but only if `canJoin` allows it or the player is already a vampire.
- Species (plan 5) overrides this. The sync skips players whose `BloodlineAccess.get(p).species()` is not NONE.

- [ ] **Step 1:** Write the GameTests:
  - `joiningVampireFactionKeepsRace` (an elf stays an elf)
  - `settingTensuraVampireLordJoinsAtSeven`
  - `leavingFactionCuresRace`
  - `commandFactionChangeSyncsOnce`: count `SET_RACE` calls with a test listener; expect 1.
  - `mobRaceChangeIgnored`
- [ ] **Step 2:** Run `./gradlew runGameTestServer`. Expected: the new tests FAIL.
- [ ] **Step 3:** Implement `VampireSync`.
- [ ] **Step 4:** Run the GameTests again. Expected: PASS.
- [ ] **Step 5:** Commit "feat(vampirism_tensura): unified vampire sync".

### Task 4: No double sun burn

**Files:**
- Create: either `parts/vampirismtensura/SunBurnFix.java` (if Task 1 found a Tensura config switch or an event), or `mixin/vampirismtensura/VampireRaceSunMixin.java` plus `supernaturalbloodlines.vampirism_tensura.mixins.json`, registered in `neoforge.mods.toml` and loaded only when the part is on via the per-part mixin plugin `VampirismTensuraMixinPlugin` (all `Part.requiredMods()` present and the part switch on).
- Modify: `docs/mixins.md`, if a mixin is used.

**Interfaces:**
- Behaviour: when the player is in the Vampirism vampire faction, Tensura's `shouldBurn` returns false. Vampirism's own sun damage stays.

- [ ] **Step 1:** Write the GameTest `factionVampireTakesOnlyVampirismSunDamage`: a faction vampire with race `tensura:vampire` in the sun for 200 ticks. Assert that only damage of Vampirism's sun damage type was taken; use a `LivingIncomingDamageEvent` recorder.
- [ ] **Step 2:** Run it (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Commit "fix(vampirism_tensura): single sun burn for unified vampires".

### Task 5: Hunter and werewolf rules, holy damage, bloodless

**Files:**
- Create: `parts/vampirismtensura/FactionRaceRules.java`, `parts/vampirismtensura/werewolves/WerewolfSkillGrants.java` (loaded only with Werewolves), and `parts/vampirismtensura/HolyDamageBridge.java`
- Create the data files:
  - `src/main/resources/data/supernaturalbloodlines/data_maps/entity_type/…`: Vampirism `entity_blood` = 0 for every Tensura entity in `tensura:no_blood`. The file is conditioned on `vampirism` + `tensura` being loaded. Data-map files live under the `vampirism` namespace path the data map uses (`data/vampirism/data_maps/entity_type/entity_blood.json` merges per-entry).
  - `data/tensura/entity_existence/supernaturalbloodlines_*.json`: EP for Vampirism and Werewolves mobs, conditioned on those mods
- Test: GameTests in `VampirismTensuraGameTests`

**Interfaces:**
- **`CanJoinFaction` listener:** DENY the hunter faction if the race has the tag `tensura:undead` or `tensura:daemon`. DENY werewolves if the race has `tensura:undead`, `tensura:daemon` or `tensura:no_blood`.
- **`SET_RACE` listener:** if a hunter becomes undead or daemon, `leaveFaction(false)` and send the message `supernaturalbloodlines.vampirism_tensura.hunter_left`.
- **`WerewolfSkillGrants`:** on `FactionLevelChanged` for werewolves, grant the skills in config `werewolf_skill_grants` (`"level=skillId"`, default `"-1=tensura:godwolf_sense"`). Remove them when the player leaves the faction.
- **`HolyDamageBridge`:** a `LivingIncomingDamageEvent` listener. If the target is a Vampirism vampire and `TensuraDamageHelper.isHoly(source)` but the source is not Vampirism holy water, multiply the damage by the config `tensura_holy_vs_vampire` (default 1.5).
- **Bite blocking:** cancel `BloodDrinkEvent.PlayerDrinkBloodEvent` (or the bite action event, per Task 1) when the bitten player's race has `tensura:no_blood`.

- [ ] **Step 1:** Write the GameTests:
  - `undeadCannotJoinHunters`
  - `hunterBecomingUndeadLeavesFaction`
  - `werewolfMaxLevelGetsGodwolfSense` (skip unless werewolves is loaded)
  - `tensuraHolyHurtsVampireMore`
  - `holyWaterCountsAsTensuraHoly`: assert `TensuraDamageHelper.isHoly` on Vampirism's holy water source. If it fails, add the damage type to a Tensura holy damage tag (if one exists), and record which in `docs/parts/vampirism_tensura.md`.
  - `bloodlessPlayerCannotBeBitten`
- [ ] **Step 2:** Run them (FAIL), implement the listeners and data, run again (PASS).
- [ ] **Step 3:** Check in game with `runClient`: spawn a Vampirism vampire mob and a werewolf, and use the Tensura analysis to confirm their EP shows. Tick the checklist in `docs/parts/vampirism_tensura.md`.
- [ ] **Step 4:** Commit "feat(vampirism_tensura): faction/race rules, holy damage, bloodless, EP data".

### Task 6: Missing-mod checks and release notes

- [ ] **Step 1:** Run `runClient` with `-PnoLinkedMods`, then with only Vampirism + Tensura (no Werewolves, using a `-PnoWerewolves` flag added to `build.gradle`). Expected: no errors from `supernaturalbloodlines`, and the part reports enabled or disabled correctly.
- [ ] **Step 2:** Update `CHANGELOG.md` under "Unreleased: vampirism_tensura". Commit.
