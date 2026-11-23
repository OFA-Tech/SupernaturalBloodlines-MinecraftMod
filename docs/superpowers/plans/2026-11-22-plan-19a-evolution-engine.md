# Plan 19a: the evolution-rite engine and our own chain: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the shared rite engine of plan 19 (a new part, a data recipe per chain, addon probes, refusals without oracles, book and unlock integration, Admin rows) and ship the first chain on it: a witch-type performer evolves a consenting target from a mastered Sage to Tensura's Great Sage on a full-moon night.

**Architecture:** Pure rules in `core.evolution` (nights, order of checks, refusal reasons). A `supernaturalbloodlines:evolution` rite factory whose codec **is** the recipe, so each rite file carries its own recipe. Addon requirements are asked of the installed addon through `AddonProbe`s (reflection, no compile dependency). The unlock table, book generator and `RiteUnlocks` of the rites-unlock work are extended, not replaced. One new part, `evolutionRites`.

**Tech Stack:** Java 21, NeoForge 1.21.1, Enchanted 4.2.7 (`RiteFactory`, `Rite`), Tensura 2.0.1.5 and ManasCore 4.0.x (compile only), JUnit 5, the repo's GameTest runner.

**Spec:** [docs/superpowers/specs/2026-11-22-tensura-evolution-rites-design.md](../specs/2026-11-22-tensura-evolution-rites-design.md). Research and verified signatures: [docs/superpowers/research/tensura-addons/](../research/tensura-addons/README.md). Read both first; this plan does not repeat them.

Repo conventions that apply (from the plan-18 phases): one branch `feature/plan-19a-evolution-engine`, one PR, a `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/evolution_rites.md` ticked, the E-rules copied into `SPECIES-RULES.md` under the next free R-numbers, no narrating code comments.

## Global Constraints

- Part key `evolutionRites`; `requiredMods` `vampirism`, `enchanted`, `tensura`; `requiredParts` `SPECIES_TENSURA`. Package `net.ofatech.supernaturalbloodlines.parts.evolutionrites`; pure rules in `net.ofatech.supernaturalbloodlines.core.evolution`.
- No compile dependency on any addon. Addon classes are reached only by reflection inside `parts.evolutionrites.probe`. 19a has no addon recipe and loads no addon class.
- Nights (spec E3): `ULTIMATE` needs `LunarEclipse.active(server)`; `UNIQUE` and `EXTRA` need `MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime())`.
- Performer: `SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species())`. Consent: `RiteTargets.consentAnswer(caster == target, target.isShiftKeyDown(), RiteTargets.SNEAK_TO_EVOLVE)`, with `SNEAK_TO_EVOLVE = "supernaturalbloodlines.rite.sneak_to_evolve"`.
- Refusal keys (lang): `supernaturalbloodlines.refusal.evolution.closed`, `.not_witch`, `.not_ready`, `.already`; `supernaturalbloodlines.rite.needs_eclipse`; existing `supernaturalbloodlines.rite.needs_full_moon` and `supernaturalbloodlines.rite.no_effect` (`FactionRiteFactory.NEEDS_FULL_MOON`, `NO_EFFECT`); success `supernaturalbloodlines.rite.evolution_done` (target) and `supernaturalbloodlines.rite.evolution_done_caster`.
- Everything about the target goes through `RiteTargets.refuseAbout(caster, target, key)`; the single-owner and every addon requirement share the key `.not_ready`.
- Config file `supernaturalbloodlines-evolution_rites.toml`, section `evolutionRites`; keys `rites_enabled` (true), `ultimate_rites_enabled` (true), `disabled_recipes` (empty list), `announce_ultimates` (false). `AdminEntries.ALL.size()` becomes **161** (156 + `parts.evolutionRites` + 4).
- Data: every file of the part carries `{ "type": "supernaturalbloodlines:part_enabled", "part": "evolutionRites" }`; an addon recipe also `neoforge:mod_loaded`. Suggested rite power: 9000 for a Unique.
- Sacrifice (spec E15): a recipe has `ep_cost` (the codec gets an optional `ep_cost` double, default 0) or an addon tribute; `great_sage` carries an `ep_cost` the builder sets, taken after the confirmed learn; a data test fails a recipe with neither. Plan 19 is built after plan 18.
- No wire change: `LadderNet.PROTOCOL` stays.
- GameTest runs one at a time with `--max-workers=2`; restore `run/config` after any killed run.

## Review Focus

Inputs the spec implies and no task's happy path covers, most likely first. Each has a test in the task named in brackets.

1. The centre of the circle is empty and the caster has gone offline: the rite must cancel silently, not throw. [Task 4: `noCentreAndNoCasterCancelsSilently`]
2. A datapack rite names a probe id or a skill id that does not exist: that recipe is unavailable, one warning is logged, the world still loads. [Task 4: `unknownProbeOrSkillMakesTheRecipeUnavailable`]
3. Another mod (Skill Obtain, Skill Books) gives the target the skill between the first check and the grant: refused as `already`, nothing consumed. [Task 4: `aSkillGivenInTheMeantimeIsSeenAtGrantTime`]
4. A recipe whose `tier` disagrees with the skill's real `Skill.getType()` would use the wrong night. [Task 4: `tierMatchesTheSkillType`]
5. A learn that a listener vetoes (not enough magicules, a pack price hook) must not cost a skill that the recipe consumes. [Task 4: `aVetoedLearnConsumesNothing`]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`):
- `core/evolution/Tier.java`, `GrantForm.java`, `EvolutionRecipe.java`, `EvolutionRules.java`: pure.
- `core/rite/RiteAvailability.java`: pure switch the part sets, read by `RiteUnlocks`.
- `parts/evolutionrites/EvolutionRitesPart.java`, `EvolutionRitesConfig.java`, `Recipes.java` (which recipes exist and are available).
- `parts/evolutionrites/probe/AddonProbe.java`, `Hooks.java`, `Probes.java`, `HookList.java`.
- `parts/evolutionrites/rite/EvolutionRiteFactory.java`.
- `parts/evolutionrites/gametest/EvolutionHookProbeGameTests.java`, `EvolutionRiteGameTests.java`, `EvolutionUnlockGameTests.java`.
- `src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite/evolve_great_sage.json`.
- generated by the book test: advancements `known/evolve_great_sage.json` and `sealed/evolve_great_sage.json`, the real entry and the decoy under the book folder.
- `docs/parts/evolution_rites.md`.

Create (test, `src/test/java/net/ofatech/supernaturalbloodlines/`): `core/evolution/EvolutionRulesTest.java`, `core/evolution/TierTest.java`, `core/rite/RiteAvailabilityTest.java`, `parts/evolutionrites/probe/HooksTest.java`, `parts/evolutionrites/EvolutionRecipeCodecTest.java`, `book/EvolutionRitesDataTest.java`.

Modify: `core/Part.java`, `shared/admin/AdminEntries.java`, `core/rite/RiteUnlockTable.java`, `parts/species/unlock/RiteUnlocks.java`, `parts/vampirismenchanted/rite/RiteTargets.java`, `gametest/BloodlineGameTests.java` (line 114, `checkPartRites`), test `shared/admin/AdminCoverageTest.java` and `AdminEntriesTest.java`, test `book/RitesBook.java` (category), `build.gradle`, `assets/supernaturalbloodlines/lang/en_us.json`, `CHANGELOG.md`, `SPECIES-RULES.md`.

---

### Task 1: The part, its config and its Admin rows

**Files:**
- Modify: `core/Part.java`, `shared/admin/AdminEntries.java`, lang.
- Create: `parts/evolutionrites/EvolutionRitesPart.java`, `parts/evolutionrites/EvolutionRitesConfig.java`.
- Test: `shared/admin/AdminCoverageTest.java` (add the config to `CONFIGS`), `shared/admin/AdminEntriesTest.java` (156 to 161).

**Interfaces:**
- Produces: `EvolutionRitesConfig.FILE: String`, `SPEC: ModConfigSpec`; `static boolean ritesEnabled()`, `ultimateRitesEnabled()`, `announceUltimates()`; `static java.util.Set<String> disabledRecipes()`; `static void override(String key, Object value)` (test hook, `null` restores, as `BossesConfig.override`). Before the config loads, the defaults answer.
- Produces: `Part.EVOLUTION_RITES`.

- [ ] **Step 1: Write the failing tests.** In `AdminEntriesTest` change the expected size to 161. In `AdminCoverageTest.CONFIGS` add `new Known(EvolutionRitesConfig.FILE, EvolutionRitesConfig.SPEC, true, "EvolutionRitesConfig.java")`.
- [ ] **Step 2: Run** `./gradlew test --tests '*AdminEntriesTest' --tests '*AdminCoverageTest' --tests '*PartLoaderTest' --tests '*DataGatingTest'`. Expected: compile error (`EvolutionRitesConfig` missing).
- [ ] **Step 3: Add `Part.EVOLUTION_RITES("evolutionRites", "net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesPart", List.of("vampirism", "enchanted", "tensura"))`** and `case EVOLUTION_RITES -> Set.of(SPECIES_TENSURA);` in `requiredParts()`.
- [ ] **Step 4: Implement `EvolutionRitesConfig`** following `BossesConfig` (the four keys above, defaults above; `disabled_recipes` is a `defineListAllowEmpty` of strings).
- [ ] **Step 5: Implement `EvolutionRitesPart.init(IEventBus, ModContainer)`**: `container.registerConfig(ModConfig.Type.COMMON, EvolutionRitesConfig.SPEC, EvolutionRitesConfig.FILE)`, `AdminSpecs.register(FILE, SPEC)` (as `BossesPart` does), log `EvolutionRitesPart enabled`. Nothing else yet.
- [ ] **Step 6: Admin rows.** In `AdminEntries`: `EVOLUTION_RITES_FILE = MOD + "-evolution_rites.toml"`; append `"evolutionRites"` to `SECTIONS`; four `SettingEntry.of(EVOLUTION_RITES_FILE, "evolutionRites", key, BOOL/LIST)` rows. Lang: `supernaturalbloodlines.hub.admin.section.evolutionRites` ("Evolution rites"), one `supernaturalbloodlines.hub.admin.key.supernaturalbloodlines-evolution_rites.toml:<key>` per key, and the `parts.evolutionRites` row key like the other parts'.
- [ ] **Step 7: Run** the Step 2 command. Expected: PASS (`AdminCoverageTest` fails by name if a key has no row, so it also proves the rows).
- [ ] **Step 8: Commit** `feat(evolution): the evolutionRites part, its config and Admin rows`.

---

### Task 2: The pure rules

**Files:**
- Create: `core/evolution/Tier.java`, `GrantForm.java`, `EvolutionRecipe.java`, `EvolutionRules.java`.
- Test: `core/evolution/TierTest.java`, `EvolutionRulesTest.java`.

**Interfaces:**
- Produces: `enum Tier { ULTIMATE, UNIQUE, EXTRA; Night night() }` with nested `enum Night { FULL_MOON, ECLIPSE }`; `enum GrantForm { DEFAULT }` (19b adds `FREE`, 19c `PERMANENT`).
- Produces: `record EvolutionRecipe(String id, Tier tier, String grants, GrantForm grantForm, List<String> consumes, List<Need> needs, String requiresMod, String probe, Set<RiteKind> subjectKinds)` with nested `record Need(String skill, boolean mastered)`.
- Produces: `EvolutionRules.Facts` (a record of booleans: `enabled, modLoaded, probeFound, roadOpen, casterWitch, fullMoonNight, eclipse, casterIsTarget, targetSneaking, holdsGrant, kindOk, holdsNeeds, probeSatisfied, ownedByOther`; 19b adds `retired`); `enum Reason { CLOSED, NOT_WITCH, WRONG_NIGHT, NO_CONSENT, ALREADY, NOT_READY }`; `static Optional<Reason> check(EvolutionRecipe, Facts)`; `static boolean aboutTarget(Reason)`; `static String keyOf(Reason, Tier)`.

- [ ] **Step 1: Write the failing tests** (`EvolutionRulesTest`, one `@Test` each):
  - `anUltimateNeedsTheEclipse`: for a `ULTIMATE` recipe, `Facts` with `fullMoonNight = true, eclipse = false` gives `WRONG_NIGHT`; with `eclipse = true` it gives empty.
  - `aUniqueNeedsAFullMoonNightAndAnEclipseCounts`: `UNIQUE`, `fullMoonNight = false` gives `WRONG_NIGHT`; `eclipse = true` alone passes.
  - `theChecksRunInTheSpecOrder`: start from facts that fail everything and fix one at a time; the reasons come out `CLOSED, NOT_WITCH, WRONG_NIGHT, NO_CONSENT, ALREADY, NOT_READY`, then empty.
  - `aCasterWhoIsTheTargetIsNotAskedForConsent`.
  - `everyTargetRequirementGivesTheSameAnswer`: `kindOk = false`, `holdsNeeds = false`, `probeSatisfied = false`, `ownedByOther = true` each give `NOT_READY` and `keyOf(NOT_READY, tier)` is `supernaturalbloodlines.refusal.evolution.not_ready` (so the single-owner cannot be told from the rest).
  - `aMissingAddonSwitchedOffOrClosedRoadIsClosed`: `modLoaded`, `probeFound`, `roadOpen`, `enabled` false each give `CLOSED`.
  - `onlyTheTargetReasonsAreAboutTheTarget`: `aboutTarget` true for `ALREADY`, `NOT_READY`; false for the other four.
  - `theNightKeyFollowsTheTier`: `keyOf(WRONG_NIGHT, ULTIMATE)` is `supernaturalbloodlines.rite.needs_eclipse`, for `UNIQUE` `supernaturalbloodlines.rite.needs_full_moon`.
  - `TierTest.nightPerTier`: `ULTIMATE.night() == ECLIPSE`, `UNIQUE` and `EXTRA` `FULL_MOON`.
- [ ] **Step 2: Run** `./gradlew test --tests '*EvolutionRulesTest' --tests '*TierTest'`. Expected: compile error.
- [ ] **Step 3: Implement** the types above. `check` applies the order `CLOSED` (any of `enabled, modLoaded, probeFound, roadOpen` false), `NOT_WITCH`, `WRONG_NIGHT`, `NO_CONSENT` (not `casterIsTarget` and not `targetSneaking`), `ALREADY` (`holdsGrant`), `NOT_READY` (any of `!kindOk, !holdsNeeds, !probeSatisfied, ownedByOther`). Keys are the Global Constraints strings.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(evolution): the pure rules of an evolution rite`.

---

### Task 3: Addon probes, the hook lookup, the hook probe

**Files:**
- Create: `parts/evolutionrites/probe/AddonProbe.java`, `Hooks.java`, `Probes.java`, `HookList.java`; `parts/evolutionrites/gametest/EvolutionHookProbeGameTests.java`.
- Modify: `EvolutionRitesPart.java` (register the GameTest class from a `RegisterGameTestsEvent` listener, as `EnchantedTensuraPart` does).
- Test: `parts/evolutionrites/probe/HooksTest.java`.

**Interfaces:**
- Produces: `interface AddonProbe { Optional<String> missingHook(); boolean roadOpen(); boolean satisfied(ServerPlayer target, EvolutionRecipe recipe); default boolean ownedByOther(ServerPlayer target, EvolutionRecipe recipe) { return false; } }`.
- Produces: `Hooks.method(String className, String name, Class<?>... params): Hooks.Hit` with `record Hit(Optional<MethodHandle> handle, Optional<String> missing)`; `missing` names the class or `Class#name(params)` that was not found.
- Produces: `Probes.of(String id): Optional<AddonProbe>` (19a knows `own.sage_great_sage`: no hooks, road open, always satisfied; the engine checks `needs` itself); an unknown id is empty and logs one warning per id.
- Produces: `HookList.ROWS: List<Row>` where `Row(String phase, String className, String name, Class<?>[] params)`; 19a lists the Tensura and ManasCore hooks the part calls (`SkillHelper.learnSkill(LivingEntity, ManasSkill)`, `SkillUtils.hasSkill`, `TensuraStorages.getExistenceFrom`, `ManasSkillInstance.isMastered(LivingEntity)`, `SkillAPI.getSkillsFrom`, `SkillStorage.forgetSkill(ResourceLocation)`); 19b to 19d append their rows from `verified-hooks.md`.

- [ ] **Step 1: Write the failing tests.** `HooksTest`: `findsAJdkMethod` (`java.lang.String`, `length`), `namesAMissingClass` (`no.such.Clazz` gives a `missing` containing `no.such.Clazz`), `namesAMissingMethod` (`java.lang.String`, `noSuchMethod`: `missing` contains `String#noSuchMethod`). GameTest `everyListedHookResolves` (`EvolutionHookProbeGameTests`): for each row whose class is loaded, `Hooks.method(...)` is present, else fail with the row's `missing` text.
- [ ] **Step 2: Run** `./gradlew test --tests '*HooksTest'`. Expected: compile error.
- [ ] **Step 3: Implement** `Hooks` with `Class.forName(name, false, loader)` and `MethodHandles.publicLookup().unreflect`; never throws, always returns a `Hit`.
- [ ] **Step 4: Implement** `AddonProbe`, `Probes`, `HookList` and register the GameTest class.
- [ ] **Step 5: Run** `./gradlew test --tests '*HooksTest'` then `./gradlew runGameTestServer`. Expected: PASS, and the GameTest lists each hook it resolved.
- [ ] **Step 6: Commit** `feat(evolution): addon probes and the hook probe test`.

---

### Task 4: The rite, the `great_sage` recipe and its tests

**Files:**
- Create: `parts/evolutionrites/rite/EvolutionRiteFactory.java`, `parts/evolutionrites/Recipes.java`, `parts/evolutionrites/gametest/EvolutionRiteGameTests.java`, the rite JSON `evolve_great_sage.json`, test `parts/evolutionrites/EvolutionRecipeCodecTest.java`.
- Modify: `parts/vampirismenchanted/rite/RiteTargets.java` (add `SNEAK_TO_EVOLVE`), `EvolutionRitesPart.java` (register the factory with `RiteFactoryRegistry.get().register(EvolutionRiteFactory.ID, EvolutionRiteFactory.CODEC)` before the datapacks load), `gametest/BloodlineGameTests.java:114` (add `checkPartRites(helper, access, "evolutionRites", Set.of("evolve_great_sage"))`), lang.

**Interfaces:**
- Consumes: `EvolutionRules` and `EvolutionRecipe` (task 2); `AddonProbe`, `Probes` (task 3); `EvolutionRitesConfig` (task 1); `RiteTargets.consentAnswer`, `refuseAbout`, `centrePlayerOrCaster`; `SeveranceRiteFactory.isWitchType`; `MoonRule.isFullMoonNight`; `LunarEclipse.active(MinecraftServer)`; `SkillHelper.learnSkill(LivingEntity, ManasSkill)`; ManasCore `SkillAPI.getSkillsFrom(p)` (`getSkill`, `forgetSkill`).
- Produces: `record EvolutionRiteFactory(EvolutionRecipe recipe) implements RiteFactory` with `ID = supernaturalbloodlines:evolution` and `CODEC` whose fields are the flat `factory` block of the spec (section 6.3: `recipe` (the id), `tier`, `grants`, optional `grant_form` (`default`), optional `consumes`, optional `needs` (list of `{skill, mastered}`), `requires_mod`, `probe`, optional `subject_kinds`).
- Produces: `Recipes.available(EvolutionRecipe, java.util.function.Predicate<String> loaded): boolean` (the part is on, the mod loaded via `ModList.get()::isLoaded` in production, the probe found, the road open) and `Recipes.exists(String riteId): boolean` (used by Task 5).

- [ ] **Step 1: Write the failing tests.**
  - `EvolutionRecipeCodecTest`: `theGreatSageFileDecodes` (the real `evolve_great_sage.json` factory block decodes to tier `UNIQUE`, grants `tensura:great_sage`, needs `[{tensura:sage, mastered true}]`, empty consumes), `aMissingFieldIsAnError`, `anUnknownTierIsAnError`.
  - `EvolutionRiteGameTests` (use the player fixtures and `receivedOnActionBar` of `AugustineMakingGameTests`; each test builds a performer who is a witch and a target on the centre, and a Sage the target holds mastered, unless the name says otherwise):
    - `greatSageIsGrantedToAConsentingTargetOnAFullMoon`: target sneaking, full-moon time: target holds `tensura:great_sage`, still holds `tensura:sage`, the target got `rite.evolution_done`, the performer `rite.evolution_done_caster`.
    - `noGrantOnAnOrdinaryNight`: performer hears `rite.needs_full_moon`; target holds no Great Sage.
    - `onlyAWitchTypeWorksIt`: a human performer hears `refusal.evolution.not_witch`.
    - `aTargetWhoDoesNotSneakIsAskedAndNothingChanges`: the target receives `rite.sneak_to_evolve`, the performer `rite.no_effect`.
    - `thePerformerHearsTheSameWhateverTheTargetIs`: three non-sneaking targets (a human, one who already holds Great Sage, one with no Sage) give the performer the same message; the same three sneaking give the performer `rite.no_effect` and the targets `refusal.evolution.not_ready`, `.already` and `.not_ready`.
    - `aSelfCastIsToldPlainly`: performer is the target, no Sage: hears `refusal.evolution.not_ready`.
    - `everyRefusalGivesTheIngredientsBack`: after each refusal above the consumed stacks are back (assert as the existing rite tests do).
    - `noCentreAndNoCasterCancelsSilently` (Review Focus 1).
    - `aSkillGivenInTheMeantimeIsSeenAtGrantTime` (Review Focus 3): grant Great Sage to the target after the checks and before the grant by a test hook on the factory's grant step; result `already`, nothing consumed.
    - `aVetoedLearnConsumesNothing` (Review Focus 5): a fixture recipe with `consumes = [tensura:sage]` and a test listener on `SkillEvents.UNLOCK_SKILL` returning `EventResult.interruptFalse()`: the target keeps Sage, holds no Great Sage, and is told `.not_ready`.
    - `consumesAreForgottenOnlyAfterTheLearn`: the same fixture without the veto: Great Sage held, Sage gone.
    - `tierMatchesTheSkillType` (Review Focus 4): for every `evolve_*` rite loaded, the recipe tier equals the registry skill's `Skill.getType()` (`ULTIMATE` when it carries both).
    - `unknownProbeOrSkillMakesTheRecipeUnavailable` (Review Focus 2): a fixture recipe naming `probe: "no.such"` and another naming `grants: "nomod:nothing"`: `Recipes.available` is false and the rite refuses `closed`.
    - `theSwitchesCloseTheRite`: `EvolutionRitesConfig.override("rites_enabled", false)`, then `disabled_recipes = ["great_sage"]`: the performer hears `refusal.evolution.closed` and nothing changes; restore after.
    - `performingTeachesThePerformer`: after a success the performer knows `evolve_great_sage` (`RiteUnlocks.known`).
- [ ] **Step 2: Run** `./gradlew test --tests '*EvolutionRecipeCodecTest'` and `./gradlew runGameTestServer`. Expected: FAIL (no factory, no rite file).
- [ ] **Step 3: Implement `EvolutionRiteFactory`** with the check order of spec 6.4. The rite body gathers `Facts` (the night by `LunarEclipse.active` and `MoonRule`; `holdsNeeds` and `mastered` by ManasCore's skill storage; `kindOk` by `RiteKinds` of the target; `probeSatisfied` and `ownedByOther` from the probe), calls `EvolutionRules.check`, and maps the reason: `NO_CONSENT` through `RiteTargets.consentAnswer`, `ALREADY` and `NOT_READY` through `refuseAbout`, the rest said to the performer; every failure `return cancel();`. On no reason: `SkillHelper.learnSkill(target, skill)`; if it returns `false`, treat as `NOT_READY`; only then forget `consumes`, tell both players, call `RiteSignal.completed` as the other factories do, return `false`.
- [ ] **Step 4: Implement `Recipes`**, add the constant to `RiteTargets`, register the factory and `EvolutionRiteGameTests`, write `evolve_great_sage.json` (conditions `part_enabled evolutionRites`; two ingredients of the builder's choice that name no state of the target; `enchanted:medium_circle` of `enchanted:ritual_chalk`; power 9000; the factory block with `tier: unique`, `grants: tensura:great_sage`, `needs: [{skill: tensura:sage, mastered: true}]`, `requires_mod: tensura`, `probe: own.sage_great_sage`, `subject_kinds: []`).
- [ ] **Step 5: Lang**: every key of Global Constraints, the rite's name `rite.supernaturalbloodlines.evolve_great_sage`, and the `hub.rites.desc.evolve_great_sage.<n>` lines Task 5 needs.
- [ ] **Step 6: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 7: Mutants for this task** (apply one, run the named test, expect FAIL, revert): see the table in Task 8. Run them now for the rows marked M4 to M10.
- [ ] **Step 8: Commit** `feat(evolution): the evolution rite and Sage to Great Sage`.

---

### Task 5: Book, unlock and absent recipes

**Files:**
- Create: `core/rite/RiteAvailability.java`, `parts/evolutionrites/gametest/EvolutionUnlockGameTests.java`, test `core/rite/RiteAvailabilityTest.java`, test `book/EvolutionRitesDataTest.java`.
- Modify: `core/rite/RiteUnlockTable.java` (the `GATED` row), `parts/species/unlock/RiteUnlocks.java` (`fix`, `reconcile`, `isKnown`, `known`), test `book/RitesBook.java` (`CATS`), `EvolutionRitesPart.java` (install the availability), lang, the generated book and advancement files.

**Interfaces:**
- Produces: `RiteAvailability.set(java.util.function.Predicate<String> exists)`, `RiteAvailability.exists(String rite): boolean` (default `true` for every rite; the part installs `Recipes::exists` for `evolve_*` ids).
- Consumes: `Recipes.exists(String)` (task 4).

- [ ] **Step 1: Write the failing tests.**
  - `RiteAvailabilityTest.everyRiteExistsUntilTheSwitchSaysOtherwise`, `theSwitchAnswersPerRite`.
  - `EvolutionUnlockGameTests`: `aWitchKnowsTheRiteFromBeingOne` (witch: `known` done, `sealed` not), `aHumanSeesItSealed` (`sealed` done, `known` not), `anAbsentRecipeIsNeitherKnownNorSealed` (set the predicate to false for `evolve_great_sage` and `reconcile`: neither advancement is done, `RiteUnlocks.known` omits it; restore the predicate), `theInvariantStillHoldsAfterAdvancementGrantEverything`.
  - `EvolutionRitesDataTest` (file walk, as `RiteUnlocksDataTest`): every `evolve_*.json` has the `part_enabled evolutionRites` condition and, when `requires_mod` is not `tensura`, a `neoforge:mod_loaded` for it; an `ULTIMATE` recipe's rite has `RiteUnlockTable` lore and no kinds, a Unique or Extra has kinds; every `consumes` skill id is also a `needs` skill id (a rite never consumes what it did not require); every `evolve_*` rite has a `RiteUnlockTable.GATED` row, both advancement files, a real and a decoy entry; no decoy page names the skill or a circle; every key of the Global Constraints exists in `en_us.json`.
- [ ] **Step 2: Run** `./gradlew test --tests '*RiteAvailabilityTest' --tests '*EvolutionRitesDataTest' --tests '*RitesBookTest' --tests '*RiteUnlocksDataTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `RiteAvailability`; make `RiteUnlocks.fix`, `reconcile`, `isKnown` and `known` treat a rite whose `RiteAvailability.exists` is false as absent (revoke `sealed`, never award `known`); add `kind(m, "evolve_great_sage", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID)` to `RiteUnlockTable`; install the predicate in the part's init.
- [ ] **Step 4: Book.** Add `new Cat("evolutions", "Evolution Rites", "minecraft:nether_star", "Rites that open a road to a skill that already exists.", List.of("evolve_great_sage"))` to `RitesBook.CATS`; write the lang (`hub.rites.desc.evolve_great_sage.1..n`, the kind label, the sealed hint) with the 230-character page limit; regenerate with `$env:RITES_BOOK_WRITE = '1'; ./gradlew test --tests '*RitesBookTest'` (PowerShell; `RITES_BOOK_WRITE=1 ./gradlew ...` in a POSIX shell), unset it afterwards, and review the diff of the generated files. Register `EvolutionUnlockGameTests` in the part's `RegisterGameTestsEvent` listener.
- [ ] **Step 5: Run** the Step 2 command and `./gradlew runGameTestServer`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(evolution): the book entry, the unlock and absent recipes`.

---

### Task 6: Docs, rules, changelog

**Files:** Create `docs/parts/evolution_rites.md`. Modify `SPECIES-RULES.md`, `CHANGELOG.md`, README (the parts list), lang (one Guide paragraph).

- [ ] **Step 1:** `docs/parts/evolution_rites.md`: checklist (ticked items of 19a), the verified hooks the part uses (copy the Tensura rows of `verified-hooks.md` that it calls, with the version), the disabled-part note (what vanishes with the part off; data conditions), the settings table, the manual checks (spec section 13), and "Booting with the addon jars" (Task 7).
- [ ] **Step 2:** `SPECIES-RULES.md`: E1 to E14 of the spec under the next free R-numbers; `CHANGELOG.md`: entry with a "Rolling back" paragraph (turning the part off removes the rite and its book entries; a Great Sage already granted stays, it is an ordinary Tensura skill).
- [ ] **Step 3:** Guide paragraph in lang: the two nights and the consent, no spoilers.
- [ ] **Step 4:** `./gradlew test` (all unit tests). Expected: PASS.
- [ ] **Step 5: Commit** `docs(evolution): part doc, rules, changelog`.

---

### Task 7: Addon jars on the run classpath, and the real server boot

**Files:** Modify `build.gradle`. Modify `docs/parts/evolution_rites.md` (the procedure).

- [ ] **Step 1:** In `build.gradle`, next to `ecJars`: `if (project.hasProperty('addonJars')) { localRuntime(fileTree(dir: project.property('addonJars'), include: '*.jar')) }` with a comment saying what the folder holds and that it is never on by default (CI and the GameTests never have it).
- [ ] **Step 2: Prepare two folders** outside the repo (copy, never edit the pack): `boot-addons` with `ascension-3.1.0.jar`, `ascensioncore-1.0.0.jar`, `greatsage-0.0.7.1.jar`, `foxablaze-1.0.6.8.jar`, `mysticism-neoforge-2.2.1.jar`, `Tensura_Horizons-0.31.jar`, `tensura_kumodesu-neoforge-2.0.3-1.21.1-HOTFIX2.jar`, `SlimeThroneExtras-neoforge-2.1.4.jar`; and `boot-pack` = `boot-addons` plus `tensura_skillobtain-neoforge-1.1.jar`, `tensura_skill_books-neoforge-2.2.3.jar`, `tensura-trainer-neoforge-2.0.5.jar` (add `tensura_rebalanced_combined-3.2.17.jar` in a third attempt and record whether the dev classpath accepts the locator). SHA-256 of each is in the research README; check them.
- [ ] **Step 3: Boot A** `./gradlew runGameTestServer -PaddonJars=<boot-addons>`: all GameTests PASS and the hook probe lists no missing hook. **Boot B** same with `<boot-pack>`.
- [ ] **Step 4: A real dedicated server.** `./gradlew runServer -PaddonJars=<boot-addons>` with `run/eula.txt` set and `online-mode=false` in `run/server.properties`; wait for `Done`; run `/bloodline rites list` and `/gamerule doAscensionUltimate` and `/gamerule requireGreatSage` (both addon rules exist) from the console, and note the name of FoxAblaze's single-owner rule from the `/gamerule` completion list (its string was not read in the bytecode); `stop`. Pass: reached `Done`, the log has `EvolutionRitesPart enabled`, no `Registry loading errors`, no `Unknown type supernaturalbloodlines:evolution`, no stack trace from our mod. Repeat with `<boot-pack>`.
- [ ] **Step 5: Negative boots:** the same server with `-PnoLinkedMods` is the matrix run 3; with `-PpartsOff=evolutionRites` is run 13; both must reach `Done`.
- [ ] **Step 6:** Record the log lines and the SHA-256 list in the PR. If the boot shows a warning from `LinkedMods.warnUntested` for an addon, note it (the addons' series are added in their phases).
- [ ] **Step 7: Commit** `build: -PaddonJars puts addon jars on the dev run classpath`.

---

### Task 8: The 12-run matrix, the 13th run and the mutation pass

- [ ] **Step 1: The matrix**, one run at a time with `--max-workers=2`, each `./gradlew runGameTestServer` plus: 1 none; 2 `-PnoWerewolves -PnoCurios`; 3 `-PnoLinkedMods`; 4 `-PpartsOff=species`; 5 `-PpartsOff=artifacts`; 6 `-PpartsOff=speciesTensura`; 7 `-PpartsOff=supernatural`; 8 `-PpartsOff=supernaturalTensura`; 9 `-PpartsOff=hud`; 10 `-PpartsOff=celestials`; 11 `-PpartsOff=bosses`; 12 `-PpartsOff=species,artifacts`; **13 `-PpartsOff=evolutionRites`** (the new part alone; runs 4 and 6 also turn it off through `requiredParts`, but only 13 tests that the rest of the mod is whole without it). The evolution GameTest classes skip themselves when the part is off or `-PnoLinkedMods`; `partDataFollowsSwitch` must see `evolve_great_sage` in the registry exactly when the part is on. Expected: all PASS; record the counts.
- [ ] **Step 2: The mutation pass.** For each row: apply the change, run the named test, confirm it FAILS, revert. Record each in the PR.

| # | Mutant | Test that must fail |
|---|---|---|
| M1 | `Tier.ULTIMATE.night()` returns `FULL_MOON` | `TierTest.nightPerTier`, `EvolutionRulesTest.anUltimateNeedsTheEclipse` |
| M2 | consent check removed from `EvolutionRules.check` | `aCasterWhoIsTheTargetIsNotAskedForConsent` and `theChecksRunInTheSpecOrder`, GameTest `aTargetWhoDoesNotSneakIsAskedAndNothingChanges` |
| M3 | the witch check removed | `theChecksRunInTheSpecOrder`, GameTest `onlyAWitchTypeWorksIt` |
| M4 | `ownedByOther` mapped to `ALREADY` | `everyTargetRequirementGivesTheSameAnswer` |
| M5 | a target-dependent reason said to the performer instead of through `refuseAbout` | `thePerformerHearsTheSameWhateverTheTargetIs` |
| M6 | `consumes` forgotten before the learn | `aVetoedLearnConsumesNothing` |
| M7 | the learn result ignored (forget even when `false`) | `aVetoedLearnConsumesNothing` |
| M8 | "already holds" checked only once, before the grant | `aSkillGivenInTheMeantimeIsSeenAtGrantTime` |
| M9 | the config switches ignored | `theSwitchesCloseTheRite` |
| M10 | an unknown probe id treated as satisfied | `unknownProbeOrSkillMakesTheRecipeUnavailable` |
| M11 | `RiteAvailability` ignored by `RiteUnlocks.fix` | `anAbsentRecipeIsNeitherKnownNorSealed` |
| M12 | `Hooks.method` returns a handle for a missing class | `HooksTest.namesAMissingClass` |
| M13 | the part-data condition removed from `evolve_great_sage.json` | `EvolutionRitesDataTest`, run 13 |
| M14 | `tier` of `great_sage` set to `ULTIMATE` | `tierMatchesTheSkillType` |

- [ ] **Step 3: Open the PR** with the matrix counts, the mutation table with results, the boot logs and the SHA-256 list.

---

## Self-review

- Spec coverage: sections 5 (E1 to E14) map to Tasks 2 and 4 (E3, E4, E5, E6, E8, E10, E11, E12), 1 (settings, section 11), 5 (E7, section 10), 3 (E2 hooks); E9 (single owner) and E14 (retired skills) are 19b work, in its outline (the engine's `ownedByOther` seam is built and tested here; `retired` is added with its source in 19b); section 9 (race links) is 19d; section 13 is Tasks 7 and 8.
- Types: `EvolutionRules.Facts` fields are used by name in Task 4; `Recipes.exists` is defined in Task 4 and consumed in Task 5; `AddonProbe` methods match the spec 6.3.
- Proportion: the plan is the decisions the builder cannot make alone; bodies are left to the signatures and the tests.

---

## Outline: 19b Great Sage and FoxAblaze (a plan of its own is written when 19a is merged)

Gate: the owner answers Q1 (Raphael) and Q7 (retired skills). Branch `feature/plan-19b-great-sage-fox`.
1. Hook probe rows for `CapstoneGate` (`eligible`, `available`, `OBTAINABLE`), `GreatSageGameRules.holdsRaphael`, `RaphaelFusion.isEligibleRace`/`isOwnedByOther`, FoxAblaze's game rule and `ITrulyUnique`; re-read the changed classes if a version moved.
2. `GrantForm.FREE` (a `TensuraSkillInstance` with `NoMagiculeCost`), with its test and mutant.
3. `GreatSageProbe` (calls `CapstoneGate.eligible` and `available`; `roadOpen` is `available`'s class constant read once) and `FoxAblazeProbe` (the single-owner reader: rule + `ITrulyUnique` of the overworld; either Raphael counts).
4. The `raphael` recipe per Q1 (dormant unless `roadOpen`); lore gate; tests: owner elsewhere, either Raphael held, race flag, both skills mastered, closed road hides the book entry, FoxAblaze absent.
5. `LinkedMods.TESTED` rows `greatsage` 0.0.7 and `foxablazeultimate` 1.0.6; retired-skill check (E14) if Q7 says yes.
6. Matrix 1 to 13, mutants (owner check skipped, either-Raphael skipped, closed road granted), boots A and B.

## Outline: 19c Ascension

Gate: Q5 answered. Branch `feature/plan-19c-ascension`.
1. Probe rows for `AwakeningRitual` helpers and `AddonConfig`/`AddonGameRules` fields; resolve the unverified items of `verified-hooks.md` (Shadow Monarch gate, `hasOtherBirdsMastered`, what `EpUtil.grantMaxEp` does to a rite's target, the exact registry id of the Ultimate Catalyst item).
2. `GrantForm.PERMANENT` (`learnSkill(target, skill, -1)`).
3. `AscensionProbe`: reads `doAscensionUltimate`, `minMaxEp`, `cooldownMinutes` live, `IExistence`, the pair from the addon's own table (`forcedAwakenings` shows the pairs the target can use), the extra gates through the public helpers; writes the cooldown tag on success and applies the addon's own consumption (`consumeSuppressionStoneOver400k`, `consumeDragonEssences30`).
4. `deus_sanguis` (Unique, vampire-kind subject) and `blood_sovereign` (Ultimate, catalyst as an ingredient, lore gate); more pairs per Q5, one rite file and one lang block each.
5. `LinkedMods.TESTED` row `ascension` 3.1; tests per spec section 8.2, including every extra gate the addon has for the chosen pairs; mutants (EP threshold read from a constant, cooldown ignored, Unique not forgotten, Deus Sanguis granted to a non-vampire).
6. Matrix, boots A and B; a manual check on a real eclipse with the real Ascension altar left untouched.

## Outline: 19d the others and the race links

Gate: Q6 answered. Branch `feature/plan-19d-others-and-races`.
1. Read, then record in `verified-hooks.md`: `getNextEvolutions` of the five Bloodfiend races, of `mysticism:direwolf` and every `*_spirit_wolf`, the Beastfolk evolution Ascension's `MixinBeastfolkRace` adds (its id), and the addon races in `tensura:undead`.
2. The generic `unique.road` probe (the addon's own on/off switch where one exists, nothing otherwise); `great_mage`, `king_of_curses`, `butcher`, `imitator` per Q6, each with its subject-kind rule.
3. Race links (spec section 9) as data plus `ApexLines`/`RiteKinds` changes: Bloodfiend = vampire, Direwolf and Spirit Wolf = werewolf (the fang and guitar-wolf lines excluded), undead by tag, Beastfolk evolution picked by id; tests per link with the real id lists, and a test that no id outside the list is mapped.
4. `LinkedMods.TESTED` rows `mysticism` 2.2, `tensurahorizons` 0.31, `tensura_kumodesu` 2.0.3, `stextras` 2.1.
5. Matrix, boots A and B, then a boot with Horizons and Mysticism removed (Horizons lists them as optional) to prove the absent-addon path with a real jar set.
