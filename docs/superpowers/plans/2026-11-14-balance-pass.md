# Plan 10: Balance and Lore Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the owner's eight balance and lore decisions of 2026-11-14 to the existing parts, with no new systems.

**Architecture:** Every change lands in code that already exists:
- the `species_tensura` apex and boost code
- the `vampirism_tensura` faction and race rules, plus its werewolf sync
- the `species` pack rules and spark handling
- the `vampirism_enchanted` faction rites
- the client eclipse

State changes still go through the pure rule tables (`PackRules`) with unit rows.

**Tech Stack:** NeoForge 1.21.1, Java 21, Tensura 2.0.1.5 / ManasCore, Vampirism, Werewolves, Enchanted, GameTests + JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-balance-pass-design.md`

## Global Constraints

- Existing saves keep what they have. Only events after the update follow the new rules. No migration rewrites a player's race, rank or level.
- Never add a mixin when an event or tag will do. Mixins stay at most 3 per part, each with its config switch.
- Every decision gets a GameTest or unit row, and every one must be able to fail. Prove it by mutation for the non-trivial ones.
- Test runs that must stay green:
  - `build`
  - `runGameTestServer`
  - `-PnoWerewolves -PnoCurios`
  - `-PnoLinkedMods`
  - `-PpartsOff=species`
  - `-PpartsOff=speciesTensura`
- Update `docs/superpowers/specs/SPECIES-RULES.md`, the affected `docs/parts/*.md`, `CHANGELOG.md` (Unreleased) and the affected `wiki/*.md` pages in the same task as the code.

## Review Focus

1. **A True Alpha killed by a werewolf player.** Nobody gains a spark, and the victim stays True Alpha after respawn. Test: `trueAlphaKilledByWerewolfKeepsSpark` (Task 3).
2. **A True Alpha cured, then rejoining the werewolves.** They are True Alpha again with the rank perks, and the dissolved pack is not restored. Test: `curedTrueAlphaRejoinsAsTrueAlpha` (Task 3).
3. **A non-human race becoming a werewolf** (an Elf, or a Vampire-raced hybrid). The Tensura race is unchanged. Test: `nonHumanWerewolfKeepsRace` (Task 4).
4. **A player bitten while on a vampire-type apex race.** The bite lands, and the race still takes holy damage. Test: `apexRaceIsBitableButUndead` (Task 1).
5. **A full-moon rite at noon of a full-moon day.** It is refused, and the message says "night". Test: `moonRitesRefusedAtFullMoonNoon` (Task 5).

---

### Task 1: Apex races cost 2,000,000 EP and stay bitable

**Files:**
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/parts/speciestensura/apex/ApexRace.java:28` (`EP_REQUIREMENT`)
- Create: `src/main/resources/data/supernaturalbloodlines/tags/manascore_race/races/bitable.json`. Use the same tag folder layout as `data/tensura/tags/manascore_race/races/undead.json`. It lists every apex race id from `ApexSpec.ALL`.
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/shared/tensura/RaceTags.java`. Add `isBitable(ManasRaceInstance)`.
- Modify: `parts/vampirismtensura/FactionRaceRules.java`. `isBloodless` and the R103 bloodless-join check skip bitable races.
- Test: `parts/speciestensura/gametest/ApexGameTests.java`, `parts/speciestensura/gametest/ApexFactionGameTests.java`, and the unit test for `ApexSpec`/`ApexRacesTest`.

**Interfaces:**
- Produces: `RaceTags.isBitable(@Nullable ManasRaceInstance race): boolean`, true when the race is in `supernaturalbloodlines:bitable`.

- [ ] **Step 1: Write the failing tests**
  - Add `apexRaceIsBitableButUndead`, which runs for every vampire-type apex spec. It sets the race and asserts:
    - `!FactionRaceRules.isBloodless(p)`
    - a Vampirism bite lands (reuse the bite helper of the existing `VampirePlayerBiteMixin` tests)
    - `RaceUtils.isUndead(p)` is still true.
  - Add `apexEpRequirementIsTwoMillion`: `ApexRace.EP_REQUIREMENT == 2_000_000`, and the config multiplier is applied on top of it.
  - Update `ApexGameTests:116`. The old assertion "no_blood follows undead" now reads "undead apex races are bitable".
- [ ] **Step 2: Run** `.\gradlew.bat runGameTestServer`. Expected: the new tests FAIL.
- [ ] **Step 3: Implement.** Make the constant change, add the tag file and the `RaceTags.isBitable`, and make the two `FactionRaceRules` checks return false for bitable races.
  - Grep every reader of `no_blood` / `isBloodless` in `src/main`, and for each one decide whether it means "can be bitten / fed on". Every such reader skips bitable races.
  - List each reader and its decision in the task report.
  - Tensura's own `RaceUtils.isBloodless` is not ours, so leave it alone.
- [ ] **Step 4: Run** `runGameTestServer` and `-PpartsOff=speciesTensura`. Expected: PASS.
- [ ] **Step 5: Docs and commit.**
  - Update SPECIES-RULES (R100/R103 notes), `docs/parts/species_tensura.md`, the CHANGELOG, `wiki/Tensura-Integration.md` and `wiki/Known-Limits.md`. Remove the "apex races are bloodless" limit.
  - Commit: `feat(species_tensura): apex races cost 2M EP and stay bitable`.

### Task 2: The evolution boost speeds up EP gain only

**Files:**
- Modify: `parts/speciestensura/apex/EvolutionBoost.java`
- Delete or modify: `mixin/speciestensura/RaceInstanceMixin.java` and its entry in `supernaturalbloodlines.species_tensura.mixins.json`, plus the plugin's hook check
- Test: `parts/speciestensura/gametest/ApexGameTests.java`, `src/test/.../apex/EvolutionBoostTest.java`

**Interfaces:**
- Produces: `EvolutionBoost.boostedEp(Player p, double gain): double`. It returns `gain * 1.25` for a player with a species and `gain` otherwise. It is the only boost entry point.

- [ ] **Step 1: Find the hook.** Use javap to look in the Tensura jar for an EP-gain event, for example in `TensuraEntityEvents` or `TensuraPlayerEvents`, with a `Changeable<Double>` gain. Record what you find in the report.
- [ ] **Step 2: Write the failing tests.**
  - `boostAddsQuarterEpForSpeciesPlayers`: a species player gains 1,000 EP and is credited 1,250. A player with no species is credited 1,000.
  - `boostNoLongerRaisesEvolutionProgress`: `getEvolutionProgress` for a species player equals the value for an identical player with no species.
  - Unit: `EvolutionBoostTest`, with rows for the EP math, NONE and the config multiplier.
- [ ] **Step 3: Run.** Expected: FAIL.
- [ ] **Step 4: Implement.**
  - Register `boostedEp` on the event.
  - Remove the progress multiplier. If `RaceInstanceMixin` has no other injection left, delete the class, its JSON entry and its startup hook check. That frees a slot, which `docs/mixins.md` must record.
  - If no EP event exists, use the smallest hook on Tensura's EP-gain method instead, inside the same mixin class, and say so in the report.
- [ ] **Step 5: Run** all six runs. Expected: PASS.
- [ ] **Step 6: Docs and commit.**
  - Update SPECIES-RULES, `docs/parts/species_tensura.md`, `docs/mixins.md`, the CHANGELOG and the wiki pages. Remove the "boost can skip 20% of requirements" limit.
  - Commit: `feat(species_tensura): evolution boost speeds EP gain only`.

### Task 3: True Alphas keep the spark

**Files:**
- Modify: `core/bloodline/PackRules.java` (`KILLED_ALPHA`, `SPARK_LOST`, the leave/inherit path)
- Modify: `parts/species/pack/SparkHandler.java:102-160`
- Test: `src/test/.../PackRulesTest.java`, `parts/species/gametest/` (the spark GameTests)

**Interfaces:**
- Consumes: `PackEvent`, `Rank`, `PackAccess.applyPack`.
- Produces: no new API. `PackRules` refuses `SPARK_LOST` for `Rank.TRUE_ALPHA`, and refuses `KILLED_ALPHA` when the victim is `TRUE_ALPHA`.

- [ ] **Step 1: Write the failing unit rows** in `PackRulesTest`:
  - `sparkLostRefusedForTrueAlpha`: `TRUE_ALPHA` plus `SPARK_LOST` is refused, and the state is unchanged.
  - `sparkLostStillDemotesPlainAlpha`: an Alpha still becomes an Omega.
- [ ] **Step 2: Write the failing GameTests:**
  - `trueAlphaKilledByWerewolfKeepsSpark`: the killer's rank is unchanged and `stoleSpark` is false. The victim is still `TRUE_ALPHA`.
  - `curedTrueAlphaRejoinsAsTrueAlpha`:
    1. Make a True Alpha with 2 Betas.
    2. Leave the werewolf faction. The pack dissolves as for any Alpha, the rank stays `TRUE_ALPHA`, and the perks are inactive.
    3. Rejoin. The rank is `TRUE_ALPHA` with the perks back, and there are no Betas.
  - `trueAlphaLeavingPassesNoSpark`: no Beta inherits.
- [ ] **Step 3: Run.** Expected: FAIL.
- [ ] **Step 4: Implement.**
  - Change the rules in `PackRules`.
  - In `SparkHandler`, the steal path returns early when the victim is a True Alpha, and the inherit path skips heir selection for a True Alpha but still dissolves the pack.
  - Rank perks follow faction membership. Reuse the existing perk sync, and check that it re-applies on rejoin.
- [ ] **Step 5: Run** all six runs. Expected: PASS.
- [ ] **Step 6: Docs and commit.**
  - Update the SPECIES-RULES R91 note, `docs/parts/species.md`, the CHANGELOG and `wiki/Werewolves-And-Packs.md`.
  - Commit: `feat(species): true alphas keep the spark`.

### Task 4: Werewolves map to Tensura's Beastfolk line

**Files:**
- Create: `parts/vampirismtensura/werewolves/WerewolfRaceSync.java`. Model it on `VampireSync`/`VampireMapping`'s R20a check.
- Modify: `parts/vampirismtensura/VampireSyncConfig.java`, or that part's config. Add the switch `werewolf_beastfolk_enabled`, default `true`.
- Test: `parts/vampirismtensura/gametest/` (the Werewolves-gated GameTest class)

**Interfaces:**
- Consumes: the rank change listener (`BloodlineAccess` listener on `wolf.rank`) and the Werewolves faction join event used by `WerewolfSkillGrants`.
- Produces: `WerewolfRaceSync.targetRace(ResourceLocation current, Rank rank, boolean joining): Optional<ResourceLocation>`. It is pure and unit-tested.

- [ ] **Step 1: Confirm the line.** Use javap or the jar's evolution data to confirm `tensura:beastfolk` → `beast_lord` → `spirit_beast` → `divine_beast`. Record it in the report. If the order differs, follow the jar and note it.
- [ ] **Step 2: Write the failing unit rows** for `targetRace`:
  - human plus joining → `beastfolk`
  - beastfolk plus ALPHA → `beast_lord`
  - beastfolk or beast_lord plus TRUE_ALPHA → `spirit_beast`
  - spirit_beast plus ALPHA → empty (never lower)
  - divine_beast → empty
  - elf plus joining → empty
  - vampire plus ALPHA → empty
- [ ] **Step 3: Write the failing GameTests:**
  - `humanWerewolfBecomesBeastfolk`
  - `alphaOnBeastfolkBecomesBeastLord`
  - `trueAlphaBecomesSpiritBeast`
  - `nonHumanWerewolfKeepsRace` (Elf and Vampire races)
  - `switchOffLeavesRace`
- [ ] **Step 4: Run.** Expected: FAIL.
- [ ] **Step 5: Implement** `targetRace` and the listener that applies it through Tensura's race API, the same way R20a sets `tensura:vampire`.
- [ ] **Step 6: Run** all six runs. The Werewolves-absent runs must load no Werewolves classes. Expected: PASS.
- [ ] **Step 7: Docs and commit.**
  - Add R20b to SPECIES-RULES, and update `docs/parts/vampirism_tensura.md`, the CHANGELOG, `wiki/Tensura-Integration.md`, `wiki/Werewolves-And-Packs.md` and `wiki/Configuration.md`.
  - Commit: `feat(vampirism_tensura): werewolves follow the beastfolk line`.

### Task 5: Night-only moon rites and the eclipse sky

**Files:**
- Modify: `parts/vampirismenchanted/rite/FactionRiteFactory.java:180`. Use `MoonRule.isFullMoonNight`.
- Modify: `src/main/resources/assets/supernaturalbloodlines/lang/en_us.json`. The refusal wording says "full-moon night".
- Modify: `client/EclipseClient.java`
- Test: `parts/vampirismenchanted/gametest/` (the rite GameTests), `src/test/.../MoonRuleTest.java`

**Interfaces:**
- Consumes: `MoonRule.isFullMoonNight(boolean hasFixedTime, int phase, long timeOfDay)`, which already exists.
- Produces: `EclipseClient.SKY_TINT` and `EclipseClient.MOON_TINT`, the colour constants. The moon is dark orange `0xB4501E`; the sky tint is the builder's call, sampled to match the existing fog tint.

- [ ] **Step 1: Write the failing GameTest** `moonRitesRefusedAtFullMoonNoon`. For the Rite of the Moon, Immortality and Silver Moon:
  - at time 6000 on phase 0, each is refused with the night message;
  - at 18000, each succeeds.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** the check change and the lang change.
- [ ] **Step 4: Implement the eclipse sky and moon on the client.**
  - Tint the sky colour during an eclipse, and draw the moon dark orange.
  - Prefer NeoForge render events (`RenderLevelStageEvent` after the sky, or `ViewportEvent`). A mixin only if nothing else works, and within the part's mixin budget.
  - Gate it on the same eclipse state the fog uses.
  - This can't be GameTested. Run `runClient` with `/eclipse start` (the `EclipseCommand`), take a screenshot of the window as was done before, and attach the path to the report. Kill only that PID.
- [ ] **Step 5: Run** all six runs. Expected: PASS.
- [ ] **Step 6: Docs and commit.**
  - Update the CHANGELOG, `wiki/Rites.md` and `wiki/Werewolves-And-Packs.md`, and remove the "eclipse is fog-only" and "full-moon rites work by day" items from `wiki/Known-Limits.md`.
  - Commit: `feat: night-only moon rites, eclipse tints the sky and moon`.

### Task 6: Holy damage docs and final pass

**Files:** `docs/parts/species_tensura.md`, `wiki/Configuration.md`, `wiki/Tensura-Integration.md`, `CHANGELOG.md`

- [ ] **Step 1: Explain `undead_treatment_enabled` plainly in the docs.**
  - On (the default): vampires take ×3 holy damage, plus Tensura's undead rules (infection immunity, Enervation food).
  - Off: they take ×1.5 through the bridge. No code changes.
- [ ] **Step 2: Run** all six runs once more on the final branch. Expected: PASS.
- [ ] **Step 3: Commit** `docs: holy damage setting explained`.
