# Plan 19c: Ascension's Blood Sovereign (the `permanent` grant form, `AscensionProbe`, the eclipse rite): Implementation Plan

> **Status: built in PR "plan 19c" on `feature/plan-19c`.** Differences from the task list below: `AscensionProbe.Source` is the test seam (fake config) and `AscensionProbe.Live` the per-target facts; `Probes.overrides` is a GameTest seam; the book-entry and unlock work reuse 19a's generator; two older GameTests that counted Finn's rites now follow `RiteUnlocks.present`. Mutation results and the matrix are in the PR description.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Add the second Ascension road of plan 19: a witch-type performer, at a lunar eclipse, lets a consenting target who holds and has mastered Deus Sanguis become Blood Sovereign, keeping **Ascension's own requirements exactly** (5,000,000 Max EP, True Hero or True Demon Lord, the Awakening cooldown, the `doAscensionUltimate` rule, the Ultimate Catalyst). Only this pair (owner Q5). Deus Sanguis itself (`evolve_deus_sanguis`) is already on main from 19a; so are the engine, the `unique.road` probe and the `ascension` row of `LinkedMods.TESTED`.

**Spec:** [2026-11-22-tensura-evolution-rites-design.md](../specs/2026-11-22-tensura-evolution-rites-design.md) (E1 to E16, section 8.2). Research: [tensura-addons](../research/tensura-addons/README.md). Outline this expands: the end of [plan 19a](2026-11-22-plan-19a-evolution-engine.md) ("Outline: 19c Ascension"). Branch `feature/plan-19c`, one PR.

## What the jar says (read for this plan, `ascension-3.1.0.jar` decompiled with Vineflower into `%TEMP%\opencode\plan19c\asc-src`, nothing of it in the repo)

These close the "unverified" list of the outline (task 1) and are the facts the tasks stand on:

1. **The road** is `handler.AwakeningRitual.prepare(altar, player)` then `complete(altar, player, Prepared)`. `prepare` checks, in order: the rule `AddonGameRules.DO_ASCENSION_ULTIMATE` (a `GameRules.Key<BooleanValue>`, default true), the altar pattern, the cooldown (`AddonConfig.AWAKENING_COOLDOWN_MINUTES`, an `IntValue`, **times 60 times 20 as game ticks** although its comment says "real-time minutes"; refused when `last != 0 && now - last < ticks`, `last` being the player's persistent long `ascension:last_awakening_tick`, `now` the level's `getGameTime()`), `EnergyHelper.getMaxEP(player) >= AddonConfig.AWAKENING_MIN_EP` (a `DoubleValue`, 5,000,000), `IExistence.isTrueDemonLord() || isTrueHero()` (`TensuraStorages.getExistenceFrom(player)`, Tensura's `storage.ep.IExistence`), then per pair: the required Unique is held and `TensuraSkill.isMastered`, the pair's `extraGate` passes, the Ultimate is not held. The **Deus Sanguis to Blood Sovereign pair has no extra gate** (null predicate and null success hook in the table).
2. **The grant** is `SkillHelper.learnSkill(player, ultimate, -1)`, then `forgetSkill(requiredUnique, null)`; then it writes `ascension:last_awakening_tick = now`, grants the awakener `AWAKENING_BONUS` (+25% Max EP) and shares `WITNESS_BONUS` with witnesses in `WITNESS_RADIUS`, and plays effects. The catalyst is `AddonItems.ULTIMATE_CATALYST`, registry id **`ascension:ultimate_catalyst`** (crafted from eight `ascension:soul_echo` around a `tensura:hihiirokane_ingot`).
3. **`SkillHelper.learnSkill(LivingEntity, ManasSkill)` already passes `-1`** (javap: `iconst_m1` then the 3-argument form). So Tensura's default learn **is** the permanent learn; `GrantForm.PERMANENT` is, in 2.0.1.5, the same call written out. The plan keeps the form (spec E6, owner outline) because it states the intent in the data and survives a Tensura change of the default, and a test pins the instance's remove time to -1. Reported to the owner.
4. **Not read, still open:** `EpUtil.grantMaxEp` (the altar's EP bonus) is not called by the rite (see decisions), so nothing about it matters here. The Shadow Monarch gate and `hasOtherBirdsMastered` are other pairs (not in 19c).

## Decisions of this plan

- **D1. The tribute is the catalyst, not EP** (E15: "the addon's own tribute where its path has one, mirrored exactly"). `evolve_blood_sovereign` has `ep_cost` 0 and `ascension:ultimate_catalyst` x1 among its circle items; Enchanted takes the items from the circle, and `cancel()` gives them back on every refusal. The EP the learn costs is Tensura's own price, charged by the learn, as on the altar.
- **D2. The rite grants no EP bonus and no witness share.** They are the altar's rewards, not its requirements; skipping them keeps the rite from being *easier* than the road (a rite must never be), and it keeps the altar worth building. Written in the book text and the docs.
- **D3. The cooldown is shared.** A completed rite writes `ascension:last_awakening_tick` exactly as the altar does, and the probe reads the same tag, so the two roads share one cooldown.
- **D4. The rule `doAscensionUltimate` closes the rite as `closed`** (E10 d, a server setting, a plain line to the performer), not as `not_ready` to the target: `AddonProbe` gets `enabledIn(ServerLevel)`, and the factory ANDs it into the `enabled` fact. It does not hide the book entry (a rule is a switch, not a version).
- **D5. No `subject_kinds`.** Ascension's road asks for no race, only Deus Sanguis mastered; a rite that asked for our vampire species would shut out a Bloodfiend or a human who rolled Deus Sanguis, and that is stricter than the road, which E2 allows but nothing needs. Deus Sanguis's own rite stays vampire-only (19a).
- **D6. Lore-gated** (E7, Q4): `lore(FINN)` in `RiteUnlockTable`. The two 19a data tests that assume every evolution rite is kind-gated and has an EP cost are changed to say what E7 and E15 say: an Ultimate rite is lore-gated; a rite has an EP cost **or** the addon's tribute.
- **D7. The Bloodfiend race mapping is not in 19c.** The spec puts it in 19d (section 9, "race links": `ApexLines`/`RiteKinds` change, its own ids read with `getNextEvolutions`). 19c needs no mapping (D5). Reported, not built.
- **D8. No new setting and no wire change.** `ultimate_rites_enabled` and `disabled_recipes` already govern an eclipse rite; `LadderNet.PROTOCOL` is untouched; no Admin row is added (`AdminCoverageTest` proves it).

## Global Constraints

- No compile dependency on Ascension: its classes are reached only by reflection in `parts.evolutionrites.probe` (`Hooks`), exactly as `UniqueRoadProbe` does. Tensura and ManasCore are compile-only dependencies already used (`EnergyHelper`, `TensuraStorages`, `SkillHelper`).
- Pure rules in `core.evolution` (no game class), so unit tests run without Minecraft and CI (which never has the addon jars) still proves every branch.
- Everything about the target goes through `RiteTargets.refuseAbout` (the existing `NOT_READY` key); the probe's `satisfied` answers only a boolean, so no message can say which Ascension requirement failed (no oracle).
- Heavy Gradle runs go through `gt-slot.ps1` with a timeout; logs under `%TEMP%\opencode\plan19c\`; paths absolute; the main folder stays clean.
- GameTests that need the real addon skills (`ascension:deus_sanguis`, `ascension:blood_sovereign`) skip themselves when Ascension is not loaded; they run under `-PaddonJars`.

## Review Focus

Inputs no happy path covers, most likely first; each is a test below.

1. A target at 4,999,999 Max EP, or exactly 5,000,000 (the boundary: `>=`). [T2, T6]
2. A target inside the cooldown, at the very last tick, and one whose tag is 0 (never awakened). [T2, T6]
3. A true Demon Lord and a true Hero each qualify; neither does not. [T2, T6]
4. The catalyst is missing: the circle never starts the rite (Enchanted), nothing is asked. A refused rite gives the catalyst back. [T6]
5. Deus Sanguis held but not mastered; not held; Blood Sovereign already held. [T6]
6. A vetoed learn must not forget Deus Sanguis nor write the cooldown nor spend EP. [T6]
7. The rule off; the performer not witch-type; no eclipse (a plain full moon is not enough); a target who does not sneak. [T6]
8. Performer messages are identical for a human, a vampire without Deus Sanguis and a target who lacks the EP (no oracle). [T6]
9. The tag the rite writes is the tag the altar reads (a second rite right after is refused as cooldown). [T6]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`): `core/evolution/AwakeningRules.java`; `parts/evolutionrites/probe/AscensionProbe.java`; `parts/evolutionrites/gametest/EvolutionAscensionGameTests.java`; resources `data/supernaturalbloodlines/enchanted/circle_magic/rite/evolve_blood_sovereign.json` and (generated by the book test) its two unlock advancements and its two book entries.

Modify: `core/evolution/GrantForm.java`, `core/rite/RiteUnlockTable.java`, `parts/evolutionrites/probe/AddonProbe.java`, `Probes.java`, `rite/EvolutionRiteFactory.java`, test `book/RitesBook.java`, `EvolutionRitesDataTest.java`, `RiteUnlockTableTest` and whatever else the lore row breaks, lang, `docs/parts/evolution_rites.md`, `CHANGELOG.md`, `SPECIES-RULES.md`, the part's hook-probe GameTest list.

Create (test): `core/evolution/AwakeningRulesTest.java`, `core/evolution/GrantFormTest.java`, `parts/evolutionrites/probe/AscensionProbeTest.java`.

---

### Task 1: `GrantForm.PERMANENT`

**Files:** `core/evolution/GrantForm.java`, test `core/evolution/GrantFormTest.java`, `EvolutionRecipeCodecTest`.

**Interfaces:** Produces `GrantForm.PERMANENT`, `OptionalInt removeTime()` (`PERMANENT` gives `-1`, `DEFAULT` gives empty: the default form calls Tensura's 2-argument learn).

- [ ] **Step 1: Write the failing tests.** `GrantFormTest`: `permanentParses` (`GrantForm.parse("permanent")` is `PERMANENT`), `onlyPermanentNamesARemoveTime` (`PERMANENT.removeTime()` is `OptionalInt.of(-1)`, `DEFAULT.removeTime()` is empty), `everyFormRoundTripsThroughItsName`. `EvolutionRecipeCodecTest`: `grantFormPermanentDecodes` (a factory block with `"grant_form": "permanent"` decodes to `PERMANENT` and encodes back to the same name).
- [ ] **Step 2: Run** `./gradlew test --tests '*GrantFormTest' --tests '*EvolutionRecipeCodecTest'` through `gt-slot.ps1`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement** the constant and `removeTime()` in `GrantForm`.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(evolution): the permanent grant form`.

### Task 2: `AwakeningRules` (pure)

**Files:** Create `core/evolution/AwakeningRules.java`; test `AwakeningRulesTest.java`.

**Interfaces:** Produces `record AwakeningRules.Facts(boolean ruleOn, long now, long lastAwakeningTick, int cooldownMinutes, double maxEp, double minEp, boolean trueHero, boolean trueDemonLord)`; `static boolean satisfied(Facts)`; `static long cooldownTicks(int minutes)` (`minutes * 60 * 20`, as the altar); `static boolean coolingDown(long now, long last, int minutes)` (`ticks > 0 && last != 0 && now - last < ticks`); `static boolean enoughEp(double maxEp, double minEp)` (`>=`); `static boolean qualifyingStatus(boolean hero, boolean demonLord)`.

- [ ] **Step 1: Write the failing tests** (`AwakeningRulesTest`): `theEpBoundaryIsAtOrAbove` (4,999,999 fails, 5,000,000 passes, 5,000,001 passes), `aNeverAwakenedPlayerIsNotCoolingDown` (`last == 0`), `theCooldownEndsAtTheTick` (`now - last == ticks` is over, one tick less is not), `aZeroCooldownNeverBlocks` (minutes 0), `theCooldownIsGameTicksOfMinutes` (120 minutes is 144,000 ticks), `eitherStatusQualifiesNeitherDoesNot`, `theRuleOffClosesIt`, `everyRequirementIsNeeded` (start from facts that pass, break each of rule, cooldown, EP, status one at a time: each fails; nothing else changes the answer), `aLowerMinEpOpensItLower` (the threshold is read from the facts, never a literal: `minEp` 1000 with `maxEp` 1000 passes).
- [ ] **Step 2: Run** `--tests '*AwakeningRulesTest'`. Expected: compile error.
- [ ] **Step 3: Implement** the pure class.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(evolution): the pure rules of Ascension's awakening`.

### Task 3: `AscensionProbe`, the probe seams and the factory

**Files:** Create `probe/AscensionProbe.java`, test `probe/AscensionProbeTest.java`; modify `AddonProbe.java`, `Probes.java`, `EvolutionRiteFactory.java`, `HooksTest` or the hook GameTest list.

**Interfaces:**
- `AddonProbe` gets `default boolean enabledIn(ServerLevel level) { return true; }` (a world switch the addon owns, E10 d) and `default void onGranted(ServerPlayer target, EvolutionRecipe recipe) { }` (what the addon's own road writes when it grants).
- `Probes.ASCENSION_AWAKENING = "ascension.awakening"`; `Probes.of` returns an `AscensionProbe` for it.
- `AscensionProbe.missingHook()` names the first missing of `com.simplygray.tensuraaddon.AddonConfig#AWAKENING_MIN_EP`, `#AWAKENING_COOLDOWN_MINUTES`, `com.simplygray.tensuraaddon.AddonGameRules#DO_ASCENSION_ULTIMATE`. `roadOpen()` is true. `enabledIn(level)` reads the rule through the key found by reflection. `satisfied(target, recipe)` builds `AwakeningRules.Facts` from the live config values (a not-yet-loaded config answers with its default, as `UniqueRoadProbe` does), `EnergyHelper.getMaxEP`, `TensuraStorages.getExistenceFrom`, the target's `ascension:last_awakening_tick` and the level's `getGameTime()`. `onGranted` writes the tag. The reading of facts goes through a small `Source` interface so the unit test feeds fakes and the GameTest the real jar.
- `EvolutionRiteFactory`: `grant()` learns with `learnSkill(target, skill, form.removeTime().getAsInt())` for `PERMANENT` and the 2-argument call otherwise; the rite's `facts()` ANDs `probe.enabledIn(level)` into `enabled`; after a `DONE` it calls `probe.onGranted`.

- [ ] **Step 1: Write the failing tests.** `AscensionProbeTest` (pure, fake `Source`): `satisfiedFollowsTheRules` (passes with all facts good; one test per requirement failing), `thePersistentTagIsTheAltarsTag` (`AscensionProbe.LAST_AWAKENING_TAG` is `"ascension:last_awakening_tick"`), `aMissingConfigFieldIsNamedAndNotGuessed` (`missingHook` of a probe pointed at a fake class with one field absent names it), `onGrantedWritesTheGameTime`, `aMissingHookNeverGrants` (with a missing hook `Recipes.usableProbe` is empty). A GameTest in `EvolutionHookProbeGameTests` (Ascension on the classpath): `ascensionHooksResolve` (`AscensionProbe.missingHook()` is empty with the real jar). `EvolutionRiteGameTests`: `permanentGrantsWithRemoveTimeMinusOne` (a fixture recipe with `PERMANENT` gives a held instance whose remove time is -1; skipped when no Ascension skill exists, using a Tensura one: `tensura:great_sage` as the fixture), `enabledInFalseClosesTheRite` (a fixture probe whose `enabledIn` is false gives `closed` to the performer, nothing taken), `onGrantedRunsOnlyAfterAConfirmedLearn` (a vetoed learn does not call it).
- [ ] **Step 2: Run** `--tests '*AscensionProbeTest'` and the GameTest default run. Expected: compile error / FAIL.
- [ ] **Step 3: Implement** the interface defaults, `AscensionProbe`, the `Probes` entry, the factory changes.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(evolution): the Ascension awakening probe and the permanent learn`.

### Task 4: The rite file, the unlock, the book, the texts

**Files:** Create `evolve_blood_sovereign.json`; modify `RiteUnlockTable.java`, test `RitesBook.java` (`CATS`, the icon and name maps), `EvolutionRitesDataTest.java`, lang, the generated advancements and entries.

**Rite file:** conditions `part_enabled evolutionRites` and `mod_loaded ascension`; items `ascension:ultimate_catalyst` x1, `minecraft:nether_star` x1, `enchanted:spirit_of_otherwhere` x1; `enchanted:large_circle` of `enchanted:nether_chalk`; power 20000; factory `recipe blood_sovereign`, `tier ultimate`, `grants ascension:blood_sovereign`, `grant_form permanent`, `consumes [ascension:deus_sanguis]`, `needs [{ascension:deus_sanguis, mastered true}]`, `requires_mod ascension`, `probe ascension.awakening`, no `subject_kinds`, no `ep_cost`.

- [ ] **Step 1: Write the failing tests.** `EvolutionRitesDataTest`: `thereAreSevenRites`; `theBloodSovereignRecipeIsWhatTheSpecSays` (tier ultimate, grant `ascension:blood_sovereign`, form `PERMANENT`, consumes and needs `ascension:deus_sanguis` mastered, probe `ascension.awakening`, no subject kinds, `ep_cost` 0, the catalyst among the items, `mod_loaded ascension`, power 20000); `anUltimateEvolutionIsLoreGated` (every evolution recipe of tier ultimate has a lore source and no kind in `RiteUnlockTable`; every other evolution rite has a kind: E7); `everyRiteHasASacrifice` (an EP cost, or the addon's tribute: the rite file's items hold an item of the recipe's mod whose id ends in `_catalyst`); the two changed tests of D6; `theLoreAndTheHintAreInTheLang`. `RiteUnlockTable` tests: `finnTeachesBloodSovereign` (`taughtBy("finn")` holds it), the table-size test updated.
- [ ] **Step 2: Run** the data tests and `*RiteUnlockTableTest`. Expected: FAIL.
- [ ] **Step 3: Implement.** The rite file, `lore(m, "evolve_blood_sovereign", FINN)`, the `RitesBook` lists, the lang (`rite.supernaturalbloodlines.evolve_blood_sovereign`, `hub.rites.desc.evolve_blood_sovereign.1..3`, the sealed hint), then regenerate with `$env:RITES_BOOK_WRITE = '1'; ./gradlew test --tests '*RitesBookTest'` and unset it; review the generated files (a lore-gated entry carries no `assigned_items`).
- [ ] **Step 4: Run** the full `./gradlew test`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(evolution): the Rite of the Blood Sovereign, its lore gate and book entry`.

### Task 5: GameTests with the real addon

**Files:** Create `gametest/EvolutionAscensionGameTests.java` (registered in the part's `RegisterGameTestsEvent` listener); modify `EvoTestSupport` only if a helper is missing (an existence setter).

Each test skips cleanly when Ascension is not loaded (`ModList`), and the part-off skip of `EvoTestSupport.skipped`. Fixture: a witch-type performer; a target on the circle at 6,000,000 base Max EP, sneaking, holding `ascension:deus_sanguis` mastered, `IExistence.setTrueHero(true)`; the items laid on the circle; `atEclipse`.

- [ ] **Step 1: Write the tests** (all fail before Tasks 3 and 4 exist, so they are written after, and each is mutation-checked in Task 7): `bloodSovereignIsGrantedAtAnEclipse` (held, remove time -1, Deus Sanguis forgotten, the done lines, the catalyst consumed, the tag written to the game time, Tensura's price taken, nothing else), `aTrueDemonLordQualifiesToo`, `aPlainFullMoonIsNotEnough` (`rite.needs_eclipse` to the performer, ingredients back), `lowEpIsRefused` (5,999,999 -> 4,999,999 base is below the threshold: `not_ready` to the target, `no_effect` to the performer, catalyst back; and exactly at the threshold passes), `noStatusIsRefused`, `deusSanguisNotMasteredIsRefused`, `noDeusSanguisIsRefused`, `alreadyBloodSovereignIsRefusedAsAlready`, `theCooldownIsSharedWithTheAltar` (a second rite right after is `not_ready`; with the tag set to long ago it passes), `aVetoedLearnKeepsDeusSanguisAndWritesNoCooldown`, `theRuleOffClosesTheRiteForThePerformer` (`closed`, nothing taken), `theSwitchesCloseTheRite` (`ultimate_rites_enabled` off and `disabled_recipes`), `noOracleForThePerformer` (the performer's messages are identical for a human, a vampire without the skill and a target below the EP), `onlyAWitchTypePerformsIt`, `noConsentNoRite`, `theRiteIsAbsentWithoutAscension` (the unlock test of 19a already covers the registry; add this rite to its list).
- [ ] **Step 2: Run** `./gradlew runGameTestServer -PaddonJars=<boot-addons>` through `gt-slot.ps1`. Expected: PASS; and without `-PaddonJars` the tests skip.
- [ ] **Step 3: Commit** `test(evolution): the Blood Sovereign rite against the real Ascension`.

### Task 6: Docs

- [ ] `docs/parts/evolution_rites.md`: tick `19c`, record D1 to D8, the jar findings (learn default is -1, the cooldown is ticks of "minutes", the altar's EP bonus not given), "Mixin budget: 0 of 3 used".
- [ ] `SPECIES-RULES.md`: copy rule E-numbers for Blood Sovereign under the next free R-number if 19a added a block; one paragraph.
- [ ] `CHANGELOG.md`: the rite, the shared cooldown, no wire change, "Rolling back" (a granted Ultimate is an ordinary Tensura skill and stays; the rite file disappears; nothing to migrate).
- [ ] `docs/superpowers/research/tensura-addons/verified-hooks.md`: the resolved items (the catalyst id, no extra gate for the pair, the persistent tag, `learnSkill` default).
- [ ] Commit `docs(evolution): plan 19c`.

### Task 7: Mutation checks, the matrix, the addon runs, the server boot

- [ ] **Mutants**, each must fail a test named: (M1) the EP comparison `>` instead of `>=`; (M2) the threshold a literal 5,000,000 instead of the config; (M3) the cooldown ignored; (M4) `last == 0` treated as cooling down; (M5) Deus Sanguis not forgotten; (M6) Deus Sanguis forgotten before the learn; (M7) the cooldown tag not written; (M8) the tag written even on a veto; (M9) the status check skipped; (M10) the game rule ignored (`enabledIn` true); (M11) the eclipse replaced by the full-moon rule for `ULTIMATE`; (M12) a non-witch performer accepted; (M13) the `needs` mastered flag ignored; (M14) `PERMANENT` given a finite remove time; (M15) the refusal of a low-EP target said to the performer; (M16) the catalyst dropped from the rite file.
- [ ] **The matrix**, one run at a time, `--max-workers=2`, through `gt-slot.ps1`: 1 default; 2 `-PnoWerewolves -PnoCurios`; 3 `-PnoLinkedMods`; 4 to 11 `-PpartsOff=` each of `species`, `artifacts`, `speciesTensura`, `supernatural`, `supernaturalTensura`, `hud`, `celestials`, `bosses`; 12 `species,artifacts`; 13 `-PpartsOff=evolutionRites`.
- [ ] **The addon-jar runs** (`-PaddonJars`): boot A (8 addons), boot B (the pack set), boot C (without Mysticism); the full default run on each.
- [ ] **A real dedicated server** (`runServer` with the Ascension jars): reaches `Done`, `EvolutionRitesPart enabled`, no `Registry loading errors`, no `Unknown type supernaturalbloodlines:evolution`, `/bloodline rites list` runs, `/gamerule doAscensionUltimate` exists; a boot without Ascension and one with the part off pass too.
- [ ] Push, open the PR, wait for green CI. Do not merge.

## Self-review

- Spec coverage: section 8.2's `blood_sovereign` row is Tasks 2 to 5; E6's `permanent` form is Task 1; E7 is Task 4; E10 d is D4; E15 is D1; the outline's "resolve the unverified items" is the section above. The Bloodfiend links (section 9) are 19d (D7), the other ten Ascension pairs are one rite file each later (Q5).
- Placeholder scan: every task names its files, tests and mutants; the item list of the rite file and its power are the builder's call within E5 (suggested 20000 for an Ultimate).
- Type consistency: `GrantForm.removeTime()`, `AwakeningRules.Facts`, `AddonProbe.enabledIn` / `onGranted`, `AscensionProbe.LAST_AWAKENING_TAG` are used with those names throughout.
