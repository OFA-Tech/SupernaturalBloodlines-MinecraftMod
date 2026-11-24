# Plan 19b: Great Sage and FoxAblaze, the Raphael merge: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Every task is test first: write the test, run it, see it fail for the stated reason, then write the code.

**Goal:** Merge the two Raphaels (spec D14, section 8.3). With FoxAblaze installed, its `foxablazeultimate:raphael` is the one real Raphael: any learn of `greatsage:raphael` (Great Sage's evolution, a command, a later Great Sage update, our dormant rite) is redirected to it through a `SkillEvents.UNLOCK_SKILL` listener, FoxAblaze's one-owner rule covers both, owning either counts as owning Raphael, and a world that already has `greatsage:raphael` is converted at login. Add the dormant `raphael` rite, which switches on only when Great Sage's own road to Raphael opens. The one cost of doing it with an event (a refused redirect gives back Great Sage and Degenerate at default mastery) is **accepted by the owner** and documented.

**Architecture:** Pure rules in `core.evolution` (`RaphaelRules`, `GrantForm.FREE`, a `retired` fact). The listener is `parts.evolutionrites.merge.RaphaelMerge`, parameterised by a `Merge` record (the production one names the two Raphaels; GameTests use Tensura skills as stand-ins, so the logic runs in the normal matrix and only the real-jar checks need `-PaddonJars`). Addon classes are reached only by reflection (`Hooks`). **No mixin: the part stays at 0 of 3.** No new payload, no hub protocol change, no new config key (so no Admin row).

**Tech Stack:** Java 21, NeoForge 1.21.1, Tensura 2.0.1.5, ManasCore skill 4.0.0.2 (compile only), Great Sage 0.0.7.1 and FoxAblaze 1.0.6.8 (run time only, by reflection), JUnit 5, the repo's GameTest runner.

**Spec and research:** [spec 8.3](../specs/2026-11-22-tensura-evolution-rites-design.md), [verified-hooks.md](../research/tensura-addons/verified-hooks.md), the 19a plan (`2026-11-22-plan-19a-evolution-engine.md`) for the engine this extends. Not repeated here.

## What was read before writing this plan (2026-11-23, javap on the jars of `boot/` with their SHA-256 in `plan19a/boot/sha256.txt`)

These close the "read before building" items of the 19b outline (spec 8.3 items 6 and 7):

1. **The owner is registered by FoxAblaze's own skill, not by Tensura's listener.** `RaphaelSkill.onLearnSkill` calls `registerTrulyUnique(ServerPlayer)` and `onForgetSkill` calls `unregisterTrulyUnique`. `registerTrulyUnique` returns at once when the game rule `RAPHAEL_TRULY_UNIQUE` is off; otherwise, for the Raphael **and the Ciel** ids, it adds the player as owner when there is none (or the player already is) and calls `markDirty`. Tensura's own writers of `ITrulyUnique.addSkill` are only `/trulyunique` (`TrulyUniqueCommand`) and the reincarnation menu. **So a redirected learn through `SkillHelper.learnSkill` registers the owner by itself; we write nothing to `ITrulyUnique`.** It also means FoxAblaze does **not** refuse a learn when another player owns Raphael (it just does not register): the refusal is in its fusion's own check, which we must repeat (task 3).
2. **FoxAblaze's single-owner check, exactly** (`RaphaelFusion.isTrulyUniqueEnabled`, package-private, and `isOwnedByOther(ITrulyUnique, ServerPlayer, ResourceLocation)`, private): rule = `player.level().getGameRules().getBoolean(FoxAblazeGameRules.RAPHAEL_TRULY_UNIQUE)` (`com.foxablazeultimate.world.FoxAblazeGameRules`, a public static non-final field); owned by another = `id != null && storage.hasSkill(id) && storage.getOwner(id) != null && !owner.equals(player.getUUID())`, asked for the Raphael id **or** the Ciel id, on `TensuraStorages.getUniqueStorageFrom(server.overworld())`. Both are non-public, so `FoxAblazeProbe` re-implements them from the public rule key and Tensura's public storage API (compile-time, no addon class).
3. **`SkillStorage.learnSkill`** (ManasCore): already held -> `false`; else fires `SkillEvents.UNLOCK_SKILL`; `EventResult.isFalse()` -> `false` and **nothing is stored**; else stores the skill, sends its message, calls `ManasSkillInstance.onLearnSkill`. So our listener may learn another skill inside the event (nested, before the outer `put`), and an `interruptFalse` leaves Great Sage's Raphael unlearned.
4. **Great Sage `RaphaelEvolutionService.finish`**: unless `holdsRaphael`, forgets `tensura:degenerate`, then `tensura:great_sage`, then `grantIfMissing(greatsage:raphael)` (a `TensuraSkillInstance` with `NoMagiculeCost`, `SkillHelper.learnSkill(LivingEntity, ManasSkillInstance)`, **the result ignored**), then `SageProgress.completeCapstone` whatever happened. So a refused redirect has already cost both skills (the hazard), and Great Sage carries on as if it had worked. `GreatSageSkills.RAPHAEL` is a `RegistrySupplier`.
5. **FoxAblaze `RaphaelFusion.doGrant`** (what a Raphael grant looks like): `new TensuraSkillInstance(skill)`, mastery set from `RaphaelConfig` (`fusionGrantMastery`, clamped), tags `NoMagiculeCost` and `GiftedInstance` = true, `markDirty`, `SkillHelper.learnSkill(LivingEntity, ManasSkillInstance)`, then forgets the two prerequisites inside `UltimateSkillProtector.withEvolutionRemoval` (FoxAblaze's own guard against removal of its ultimates; it protects FoxAblaze's skills only, so forgetting Great Sage's Raphael or `tensura:*` skills is not touched by it). **Our free grant copies the two tags and leaves mastery at default**, which is what Great Sage's own grant does (the one deliberate difference from FoxAblaze, noted in the part doc).
6. **`CapstoneGate`**: `public static final boolean OBTAINABLE` (false in 0.0.7.1), `eligible(ServerPlayer)`, `available(ServerPlayer)` (constant false in 0.0.7.1). `OBTAINABLE` is readable without a player, so the road-open question has a player-free answer. `GreatSageGameRules.ADDON_RAPHAELS` lists both Raphaels and `holdsRaphael(LivingEntity)` tests them, so Great Sage's features already accept FoxAblaze's Raphael (spec 8.3 item 1); nothing to build there.
7. **Order of UNLOCK_SKILL listeners**: Architectury calls them in registration order and stops at the first non-pass result; nothing in the jars gives a stable order across mods, so we do not depend on it. What we depend on is only that our listener, when it runs, sees `greatsage:raphael` before it is stored (item 3). Tensura's and Mysticism's listeners may run before ours and veto the Raphael learn (Mysticism does when Soul Energy cannot pay, 19a "Found while building"): then our listener never runs and the learn fails as it would without us; Great Sage's `finish` has already forgotten the two skills, which is Great Sage's own existing behaviour and not ours to fix. The outcome checks (one Raphael held, FoxAblaze's, no second ceremony) are GameTests.

## Global Constraints

- Part `evolutionRites` (unchanged: requires `vampirism`, `enchanted`, `tensura`, part `speciesTensura`). **Mixin budget 0 of 3, stays 0 of 3.** No compile dependency on any addon. Addon classes only by name through `Hooks`; Tensura and ManasCore types are compile-time (already so in 19a).
- The merge is active only when `foxablazeultimate` is loaded. Without it nothing is redirected, no skill is touched, and `greatsage:raphael` behaves as Great Sage made it.
- **Hidden knowledge never leaks.** A refused redirect says the same line whatever the cause (owned by another player, a vetoed learn): `supernaturalbloodlines.refusal.evolution.not_ready`. A second line, `supernaturalbloodlines.raphael.restored`, is said only when something was actually given back (the player's own state, not a secret).
- Everything about the target in the rite goes through `RiteTargets.refuseAbout` (19a); single-owner and the addon's requirements share `not_ready`.
- Wire: no payload, no registrar change. The hub protocol is **not** touched (if a task ends up changing it, take main's number + 1 at that moment).
- Data of the part carries `part_enabled evolutionRites`; the Raphael rite also `neoforge:mod_loaded greatsage`.
- Logs only in `C:\Users\caiom\AppData\Local\Temp\opencode\plan19b\`; every heavy run goes through `gt-slot.ps1` (scripts `gw.ps1`, `gt.ps1`, `server-boot.ps1` there), `--max-workers=2`, a hard timeout.
- Matrix (spec 13): 12 GameTest runs + `-PpartsOff=evolutionRites` (13), then the **addon runs** `-PaddonJars=<folder>`: `fox-gs` (the two jars, the merge in earnest), `boot-addons` (the 8-addon set of 19a), `boot-pack` (the pack set minus Trainer), then **real dedicated-server boots** with `fox-gs`, `gs-only` and `fox-only`.

## Review Focus

Inputs the spec implies and no happy path covers, most likely first. Each has a test (task in brackets).

1. A refusal must cost the player nothing they would not have lost anyway: Great Sage and Degenerate are given back **only if missing**, so the command route (player still holds both) loses nothing. [T4 `refusalGivesBackOnlyWhatIsMissing`]
2. A player who holds FoxAblaze's Raphael and is handed Great Sage's gets no second Raphael and no duplicate ceremony. [T4 `eitherRaphaelHeldBlocksTheOther`, T5 `noSecondCeremony`]
3. Conversion at login must learn FoxAblaze's first and forget Great Sage's only after the learn is confirmed, or a veto leaves the player with no Raphael. [T4 `loginForgetsTheOldOneOnlyAfterTheNewOneIsLearned`]
4. Reentrancy: the redirect learns inside the listener of the same event; the nested learn of the target must not be redirected again, and a second player's learn at the same tick is independent. [T4 `theNestedLearnIsNotRedirectedAgain`]
5. The performer of the dormant rite learns nothing about a target who owns Raphael elsewhere: same `no_effect`/`not_ready` as any other requirement. [T6 `ownedElsewhereRefusesLikeAnyOtherRequirement`]
6. The dormant rite's book entry must not appear while Great Sage's road is closed, and must appear (without a restart of the world) when it opens. [T6 `aClosedRoadHidesTheRite`]
7. A retired skill (Rebalanced's tag) is never granted by a recipe. [T2 `aRetiredGrantIsRefusedAsNotReady`]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`):
- `core/evolution/RaphaelRules.java`: pure verdicts of the merge (learn, login).
- `parts/evolutionrites/EvoGrant.java`: the free grant (`TensuraSkillInstance`, `NoMagiculeCost`, `GiftedInstance`).
- `parts/evolutionrites/probe/FoxAblazeProbe.java` (reader of the single-owner rule, no `AddonProbe`), `probe/GreatSageProbe.java` (`AddonProbe`, id `greatsage.raphael`).
- `parts/evolutionrites/merge/RaphaelMerge.java` (listener + login sweep + `Merge` record + test seams).
- `parts/evolutionrites/gametest/FreeGrantGameTests.java`, `RaphaelMergeGameTests.java` (stand-in skills, normal matrix), `RaphaelMergeAddonGameTests.java` (real jars, skipped without both).
- `src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite/evolve_raphael.json`; two book entries and a category row (like `evolve_deus_sanguis`).
- `docs/parts/evolution_rites.md` (edit), `docs/superpowers/research/tensura-addons/verified-hooks.md` (edit), `CHANGELOG.md` (edit).

Modify: `core/evolution/GrantForm.java` (`FREE`), `core/evolution/EvolutionRules.java` (`retired` in `Facts`), `parts/evolutionrites/probe/AddonProbe.java` (`grantOverride`, `alsoHolds`), `probe/Probes.java` (id `greatsage.raphael`), `rite/EvolutionRiteFactory.java` (free form, override, either-held, retired), `parts/evolutionrites/EvoSkills.java` (`retiredHook` seam), `EvolutionRitesPart.java` (register the listener, the dormant seam, the tests), `parts/species/unlock/RiteUnlocks.java` (the `dormant` seam), `core/rite/RiteUnlockTable.java` (a row), `core/LinkedMods.java` (two rows), `en_us.json`, `EvolutionRitesDataTest`, `RitesBook` (test generator), `EvolutionHookProbeGameTests`.

---

### Task 1: the pure rules (`GrantForm.FREE`, `RaphaelRules`, the `retired` fact)

**Files:** `core/evolution/GrantForm.java`, `core/evolution/RaphaelRules.java`, `core/evolution/EvolutionRules.java`; tests `src/test/java/.../core/evolution/GrantFormTest.java`, `RaphaelRulesTest.java`, and `EvolutionRulesTest.java` (edit).

- [ ] **Step 1: write the tests** (they fail to compile first: that is the red).
  - `GrantFormTest`: `parse("free")` is `FREE`; `parse("default")` still `DEFAULT`; `parse("permanent")` is empty (19c); the codec decodes a recipe with `"grant_form": "free"` (use `EvolutionRecipeCodec` as `EvolutionRecipeCodecTest` does).
  - `RaphaelRulesTest` (the verdict of a learn of the source id, `Facts(merged, holdsEither, ownedByOther)`):
    `notMergedPassesThrough`; `heldEitherIsAlreadyHeldWhateverElse` (also when `ownedByOther`); `ownedByAnotherRefuses`; `freeAndNotHeldRedirects`; `heldWinsOverOwnedByAnother`.
    Login (`onLogin(merged, holdsSource, holdsTarget, ownedByOther)`): `nothingToConvertWhenSourceNotHeld`; `notMergedLeavesAlone`; `bothHeldDropsTheSource`; `ownedByAnotherLeaves`; `convertsWhenFree`.
  - `EvolutionRulesTest`: `aRetiredGrantIsNotReady` (a `Facts` with `retired = true` and all else fine -> `NOT_READY`, which `aboutTarget`); `retiredNeverBeatsClosed` (closed first).
- [ ] **Step 2: run** `test --tests "*GrantFormTest" --tests "*RaphaelRulesTest" --tests "*EvolutionRulesTest"`: red, compile errors naming `FREE`, `RaphaelRules`, `retired`.
- [ ] **Step 3: code.** `GrantForm { DEFAULT, FREE }`. `RaphaelRules`: `enum Verdict { PASS, ALREADY_HELD, REFUSE, REDIRECT }`, `record Facts(boolean merged, boolean holdsEither, boolean ownedByOther)`, `static Verdict ofLearn(Facts)` (not merged -> PASS; holdsEither -> ALREADY_HELD; ownedByOther -> REFUSE; else REDIRECT); `enum Sweep { NOTHING, CONVERT, DROP_SOURCE, LEAVE }`, `static Sweep onLogin(boolean merged, boolean holdsSource, boolean holdsTarget, boolean ownedByOther)` (not merged or not holdsSource -> NOTHING; holdsTarget -> DROP_SOURCE; ownedByOther -> LEAVE; else CONVERT). `Facts` of `EvolutionRules` gets `boolean retired` after `enoughEp`; `check` adds it to the NOT_READY group.
- [ ] **Step 4: green**, same command. Fix every other `Facts(...)` construction the compiler names (`EvolutionRiteFactory`, `EvolutionRulesTest`).
- [ ] **Step 5: mutants** (do in task 8): `ofLearn` checks `ownedByOther` before `holdsEither`; `onLogin` returns CONVERT when `holdsTarget`; `FREE` parse missing.
- [ ] **Step 6: commit** `feat(evolution): FREE grant form, the Raphael merge rules, the retired fact (19b task 1)`.

### Task 2: the engine learns the free form, a grant override, "either held" and the retired check

**Files:** `parts/evolutionrites/EvoGrant.java`, `probe/AddonProbe.java`, `rite/EvolutionRiteFactory.java`, `EvoSkills.java`; test `gametest/FreeGrantGameTests.java` (registered in `EvolutionRitesPart`).

- [ ] **Step 1: write the GameTests** (normal matrix; Tensura skills only; use `EvoTestSupport.learnOpen` to skip a learn an addon closes, as 19a's tests do):
  - `aFreeGrantCarriesBothTagsAndChargesNothing`: `EvoGrant.learnFree(player, skill)` returns true; the instance has `NoMagiculeCost` and `GiftedInstance`; base Max EP unchanged (a plain `SkillHelper.learnSkill` of the same skill on a second player does charge, as the control).
  - `aFreeLearnThatIsVetoedReturnsFalseAndLeavesNothing`: a listener (registered for the test through the `SkillEvents.UNLOCK_SKILL` event and removed in `finally`) vetoes; `learnFree` is false, the skill is not held.
  - `aFreeRecipeSkipsTensurasPriceInTheEpCheck`: a rite on the circle with `grant_form` free whose skill Tensura would price above the target's EP is **not** refused for EP (default would be). Build the recipe through `EvolutionRecipe` and run `EvolutionRiteFactory.grant` directly with a `FREE` recipe and a target whose base Max EP is below the price: `DONE`.
  - `aGrantOverrideReplacesTheGrantedSkill` (a test `AddonProbe` registered through a `Probes` test seam, or a probe built in the test and passed to the factory's `resolveGrant`): the override id is learned, the recipe's own id is not.
  - `aSkillOfTheAlsoHoldsListCountsAsHeld`: the target holds one of `alsoHolds`; the rite answers `ALREADY` through `refuseAbout` (same `no_effect` to the performer as any refusal).
  - `aRetiredGrantIsRefusedAsNotReady`: `EvoSkills.retiredHook` (a `volatile Predicate<ManasSkill>`, null in play) says the grant is retired; the performer hears `no_effect`, the target `not_ready`, nothing is consumed, the cooldown is untouched.
- [ ] **Step 2: run** the class (`runGameTestServer` with `-Dsb.gametest.only=FreeGrantGameTests` as in `docs/parts/evolution_rites.md`, through `gw.ps1`): red.
- [ ] **Step 3: code.**
  - `EvoGrant.learnFree(ServerPlayer, ManasSkill)`: `new TensuraSkillInstance(skill)`, `getOrCreateTag().putBoolean("NoMagiculeCost", true)` and `"GiftedInstance"`, `markDirty`, `SkillHelper.learnSkill(player, instance)`.
  - `AddonProbe`: `default Optional<String> grantOverride() { return Optional.empty(); }`, `default List<String> alsoHolds() { return List.of(); }`.
  - `EvolutionRiteFactory`: resolve the grant as `probe.flatMap(AddonProbe::grantOverride).orElse(recipe.grants())`; `holds` is true when the grant or any `alsoHolds` id is held; `grant(...)` uses `EvoGrant.learnFree` when `recipe.grantForm() == FREE`; `enoughEp` uses price 0 for FREE; `Facts.retired` = `EvoSkills.retired(server, skill)` (which first asks `retiredHook`).
- [ ] **Step 4: green** (the class, then the whole `EvolutionRiteGameTests` and `MasteryGameTests`: nothing of 19a may change).
- [ ] **Step 5: commit** `feat(evolution): the free grant form, a grant override, either-held and the retired check in the engine (19b task 2)`.

### Task 3: the readers of the two addons (hook rows, `FoxAblazeProbe`, `GreatSageProbe`)

**Files:** `probe/FoxAblazeProbe.java`, `probe/GreatSageProbe.java`, `probe/Probes.java`, `gametest/EvolutionHookProbeGameTests.java`; unit test `src/test/java/.../parts/evolutionrites/ProbeIdsTest.java` (if one exists for the ids, extend it).

- [ ] **Step 1: tests.**
  - `EvolutionHookProbeGameTests.everyListedHookResolves` gets the new rows **only when the addon is loaded** (a second test, `theAddonHooksResolveWhereTheAddonIsInstalled`, so the normal run is unchanged): Great Sage `dev.dedek.greatsage.server.CapstoneGate` (`OBTAINABLE` static field, `eligible(ServerPlayer)`, `available(ServerPlayer)`), `dev.dedek.greatsage.GreatSageGameRules` (`ADDON_RAPHAELS`, `holdsRaphael(LivingEntity)`), `dev.dedek.greatsage.server.RaphaelEvolutionService.begin(ServerPlayer)`; FoxAblaze `com.foxablazeultimate.world.FoxAblazeGameRules#RAPHAEL_TRULY_UNIQUE`, `com.foxablazeultimate.registry.FoxAblazeUltimateSkills#RAPHAEL` and `#CIEL`; ManasCore `SkillEvents#UNLOCK_SKILL`.
  - `FoxAblazeProbeGameTests` cases inside `RaphaelMergeGameTests` (task 4) use the seams; the pure unit test is the verdict table of task 1.
  - `GreatSageProbe` (GameTest `greatSageProbeReadsTheRoad`, addon-gated): `roadOpen()` equals `CapstoneGate.OBTAINABLE` (false in 0.0.7.1, so the test asserts equality with the live value, not a constant); `missingHook()` is empty; without FoxAblaze `grantOverride()` is empty, with it `foxablazeultimate:raphael`; `alsoHolds()` is both Raphael ids.
- [ ] **Step 2: run** (with `-PaddonJars=...\boot\fox-gs`): red.
- [ ] **Step 3: code.**
  - `FoxAblazeProbe` (final, static): `loaded()` (`Recipes.modLoaded("foxablazeultimate")`, with a `volatile BooleanSupplier loadedHook` test seam), `ruleOn(ServerPlayer)` (the key read by `Hooks.staticField`, then `player.level().getGameRules().getBoolean(key)`; a missing key means off), `ownedByOther(ServerPlayer)` (rule on, then for the Raphael and Ciel ids of `FoxAblazeUltimateSkills` read by name: `storage.hasSkill(id) && owner != null && !owner.equals(uuid)` on `TensuraStorages.getUniqueStorageFrom(server.overworld())`), with a `volatile Predicate<ServerPlayer> ownedHook` seam. Never throws: any reflection failure is "not owned" **and** logged once (it must not make the rite refuse or grant by accident; the real-jar test pins the happy path).
  - `GreatSageProbe implements AddonProbe`: `missingHook()` names the first missing among the rows above; `roadOpen()` reads `CapstoneGate.OBTAINABLE`; `satisfied(target, recipe)` calls `CapstoneGate.available(ServerPlayer)` (Great Sage's whole gate: EP, MP, both mastered, True Hero or True Demon Lord, every payable node); `ownedByOther` delegates to `FoxAblazeProbe`; `grantOverride()` is FoxAblaze's Raphael id when `FoxAblazeProbe.loaded()`; `alsoHolds()` both ids.
  - `Probes`: id `greatsage.raphael` in the fixed map.
- [ ] **Step 4: green** (fox-gs run of the hook class; normal run unchanged).
- [ ] **Step 5: commit** `feat(evolution): the Great Sage probe and the FoxAblaze single-owner reader (19b task 3)`.

### Task 4: `RaphaelMerge` (the redirect, the refusal, the login sweep) with stand-in skills

**Files:** `parts/evolutionrites/merge/RaphaelMerge.java`, `EvolutionRitesPart.java` (register), `gametest/RaphaelMergeGameTests.java`, lang keys.

The listener takes a `Merge` (`from`, `to`, `family` = both ids, `restoreOnRefusal` = the two skills a refused evolution gave up, an `active` supplier). Production: from `greatsage:raphael`, to `foxablazeultimate:raphael`, family both, restore `tensura:great_sage` and `tensura:degenerate`, active = `FoxAblazeProbe.loaded()`. Tests set `RaphaelMerge.override(Merge)` and `FoxAblazeProbe.ownedHook`, and use three Tensura skills that the 19a tests already learn (`EvoTestSupport.learnOpen` guards each) as source, target and restore pair.

- [ ] **Step 1: write the GameTests** (normal matrix, stand-in `Merge`; each ends with `finally { RaphaelMerge.override(null); FoxAblazeProbe.ownedHook = null; forgetAll }`):
  - `aLearnOfTheSourceBecomesTheTarget`: `SkillHelper.learnSkill(player, source)` returns false (the event interrupted), the source is **not** held, the target **is**, its instance has `NoMagiculeCost` and `GiftedInstance`.
  - `theNestedLearnIsNotRedirectedAgain`: with `to == a skill whose own learn is also listened for`, the target is held exactly once and the listener ran once for the source.
  - `ownedByAnotherPlayerRefusesAndRestoresTheRestoreSkills`: `ownedHook` true; the player held neither restore skill (as after Great Sage's `finish`); after the learn both are held again at default mastery; the target is not held; the player heard `not_ready` and `raphael.restored`.
  - `refusalGivesBackOnlyWhatIsMissing`: the player still holds one restore skill at mastery 50: it keeps mastery 50, only the missing one is given; if both are held nothing is learned and `raphael.restored` is **not** said.
  - `aVetoedRedirectIsARefusalToo`: a listener vetoes the target learn; same outcome as owned by another (the two causes read the same to the player: the chat lines are equal).
  - `eitherRaphaelHeldBlocksTheOther`: the player holds the target, then the source is learned: false, nothing else changes, no message; and the player holds the source (put in by a test listener removal-free path: learn it with the merge `active = false`, then switch it on), then the target is learned directly: held both is impossible through the merge, the second learn of the family is `ALREADY_HELD`.
  - `withoutTheOtherAddonNothingIsRedirected`: `active = false`: the source is learned as itself, nothing else changes.
  - `loginConvertsAHeldSource`: the player holds the source; `RaphaelMerge.sweep(player)` (what the login event calls) leaves the target held, the source forgotten.
  - `loginForgetsTheOldOneOnlyAfterTheNewOneIsLearned`: a listener vetoes the target; the sweep leaves the source held and logs once (assert through a log hook counter, not the log file).
  - `loginWithBothHeldDropsTheSource`; `loginLeavesASourceWhoseTargetIsOwnedElsewhere`.
  - `aRedirectIsPerPlayer`: two players learn the source in the same tick; each ends with the target; `ownedHook` true for one only: that one is refused, the other is not.
- [ ] **Step 2: run** (class only, normal run): red (class missing).
- [ ] **Step 3: code `RaphaelMerge`.**
  - `register()`: `SkillEvents.UNLOCK_SKILL.register(RaphaelMerge::onUnlock)` and `NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent)`. Called from `EvolutionRitesPart.init` (after the factories), so the event exists when ManasCore starts storing skills.
  - `onUnlock(instance, entity, message)`: `EventResult.pass()` unless `entity instanceof ServerPlayer`, the instance's skill id is `merge.from()`, and `merge.active()`. Build `RaphaelRules.Facts(merged = true, holdsEither = any id of family held, ownedByOther = FoxAblazeProbe.ownedByOther(player))` and switch: `PASS` -> pass; `ALREADY_HELD` -> `interruptFalse`; `REFUSE` -> `refuse(player)` then `interruptFalse`; `REDIRECT` -> `EvoGrant.learnFree(player, targetSkill)`; when it returns false -> `refuse(player)`; then `interruptFalse`. A `ThreadLocal<Boolean> inside` guards the nested learn (the target's own event is passed through, not redirected).
  - `refuse(player)`: tell `not_ready` (action bar); for each skill of `restoreOnRefusal` not held, `EvoGrant.learnFree`; if any was given back, tell `supernaturalbloodlines.raphael.restored` in chat.
  - `sweep(ServerPlayer)`: `RaphaelRules.onLogin(...)`: `CONVERT` -> learn the target free, and **only if that returned true** forget the source (`SkillAPI.getSkillsFrom(p).forgetSkill(id)`); `DROP_SOURCE` -> forget the source; `LEAVE` and a failed convert -> one warning per player per session (`WARNED` set), nothing changed.
  - Lang: `supernaturalbloodlines.raphael.restored` ("Great Sage and Degenerate came back to you, but their mastery did not.").
- [ ] **Step 4: green**, then the full `runGameTestServer` (the merge is on in every run now; nothing else may change).
- [ ] **Step 5: commit** `feat(evolution): the Raphael merge listener and the login conversion (19b task 4)`.

### Task 5: the real jars (`-PaddonJars`)

**Files:** `gametest/RaphaelMergeAddonGameTests.java` (registered in the part, each test starts with `skippedUnless(helper, "greatsage", "foxablazeultimate")` which succeeds at once when either is absent).

- [ ] **Step 1: write the eight tests of the outline** against the real ids (`greatsage:raphael`, `foxablazeultimate:raphael`, `tensura:great_sage`, `tensura:degenerate`), using `RaphaelEvolutionService.begin` by reflection on a `RecordingPlayer` and ticking `RaphaelEvolutionService.tick` to the end (1,270 ticks: call `tick` in a loop on the test thread, not by waiting game time):
  `greatSageRaphaelGrantBecomesFoxAblazes` (after `begin`... the ritual: the player holds FoxAblaze's Raphael, not Great Sage's, and neither Great Sage nor Degenerate); `aCommandGrantIsRedirectedToo` (`SkillHelper.learnSkill(player, greatsage:raphael)`); `ownedByAnotherPlayerRefusesAndRestoresTheTwoSkills` (a second player registered as owner through `ITrulyUnique.addSkill`, rule on); `loginConvertsAHeldGreatSageRaphael` (hold it with the merge off, switch on, `sweep`); `eitherRaphaelHeldBlocksTheOther`; `greatSageFeaturesStillWorkWithOnlyFoxAblazesRaphael` (`GreatSageGameRules.carries/access/allowed` true, read by reflection); `withoutFoxAblazeNothingIsRedirected` (a run where only Great Sage's jar is present: skipped when FoxAblaze is loaded, so it is the `gs-only` run's test; the merge `active` is `FoxAblazeProbe.loaded()`); `noSecondCeremony` (a counter on `RaphaelEvolutionService.witness` calls through a `Learned`-style check: one Raphael, one ceremony).
  Also `foxAblazeRegistersTheOwnerOfARedirectedLearn`: after a redirect with the rule on, `ITrulyUnique.getOwner(foxablazeultimate:raphael)` is the player: this is the proof of item 1 above, and a **second** player's redirect is then refused.
- [ ] **Step 2: run** with `-PaddonJars=...\boot\fox-gs`: green or the failure names the hook that moved; **normal run**: the class is all skips (assert the count in the log: they succeed, they do not fail).
- [ ] **Step 3: commit** `test(evolution): the Raphael merge against the real Great Sage and FoxAblaze jars (19b task 5)`.

### Task 6: the dormant `raphael` rite, its unlock row, book entries and the closed-road seam

**Files:** `evolve_raphael.json`, `RiteUnlockTable`, `RiteUnlocks` (`dormant` seam), `EvolutionRitesPart` (sets it), `en_us.json`, the two book entries and category row, `RitesBook` (test generator), `EvolutionRitesDataTest`, `EvolutionUnlockGameTests`, `EvolutionRiteGameTests`.

- [ ] **Step 1: tests first.**
  - `EvolutionRitesDataTest`: the count of rites is seven; `theRaphaelRecipeIsTheDormantUltimate` (tier ultimate, grants `greatsage:raphael`, `grant_form` free, consumes and needs exactly `tensura:great_sage` and `tensura:degenerate`, both mastered, `requires_mod` `greatsage`, probe `greatsage.raphael`, `ep_cost` 0); the E15 assertion becomes "an EP cost **or** the addon's own tribute", the tribute being a recipe that consumes skills the addon's own path forgets (`recipe.epCost() > 0 || !recipe.consumes().isEmpty() && recipe.tier() == ULTIMATE`), with the message naming E15; the eclipse test lists `evolve_raphael` with the Ultimate ones; the unlock-row and advancement checks cover it; `everyKeyTheRitesSayExistsInTheLang` covers its keys.
  - `EvolutionUnlockGameTests`: `evolve_raphael` is registered only when `greatsage` is loaded (as `evolve_deus_sanguis` and `ascension`), and **present** only while its road is open: `aClosedRoadHidesTheRite` sets `RiteUnlocks.dormant` through the part's own hook with a `Probes` road stub (closed), asserts `RiteUnlocks.present(player, "evolve_raphael")` is false and `presentRites` omits it, then opens the road and asserts it is present and `known` is awarded by the next sweep. Without Great Sage the whole test is a skip (the advancement does not exist).
  - `EvolutionRiteGameTests` (addon-gated, `fox-gs`): `theDormantRaphaelRiteIsClosedInGreatSage007` (the rite performed on the circle with every requirement made true by the test refuses `refusal.evolution.closed` to the performer and consumes nothing, because `OBTAINABLE` is false); `whenTheRoadOpensTheRiteGrantsTheMergedRaphael` (the probe's road stubbed open through the `Probes` seam: the target ends with FoxAblaze's Raphael, free and gifted, Great Sage and Degenerate forgotten **after** the learn, the owner registered); `ownedElsewhereRefusesLikeAnyOtherRequirement` (the performer hears `rite.no_effect`, the target `not_ready`, equal to the answer for a target that lacks the mastery); `eitherRaphaelHeldRefusesAsAlready`; `noFoxAblazeGrantsGreatSagesOwn` (in the `gs-only` run: the grant is `greatsage:raphael` itself).
- [ ] **Step 2: run**: red (file, rows and seam missing).
- [ ] **Step 3: code.**
  - `evolve_raphael.json` (conditions `part_enabled evolutionRites` and `mod_loaded greatsage`; `power` 20000; ingredients like the other Ultimate sacrifice rites: nether stars and dragon's breath kinds the Mastery ultimate file uses; `shapes` large circle; factory `recipe` `raphael`, `tier` `ultimate`, `grants` `greatsage:raphael`, `grant_form` `free`, `consumes` the two, `needs` both mastered, `requires_mod` `greatsage`, `probe` `greatsage.raphael`, `subject_kinds` `[]`, `ep_cost` 0).
  - `RiteUnlockTable`: `kind(m, "evolve_raphael", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID)` and the lore gate the Ultimate rites use (look at how `mastery_ultimate` is gated; copy it).
  - `RiteUnlocks`: `public static volatile Predicate<String> dormant = r -> false;` and `present` returns false when `dormant.test(rite)`. The part sets it to "this rite is an evolution recipe whose road is closed" through `Recipes` (`species` does not import the part).
  - Lang keys for the rite's name, book text and refusal-free success lines (the existing `evolution_done` ones are reused).
- [ ] **Step 4: green** (`test`, the unlock class, then the `fox-gs` run).
- [ ] **Step 5: commit** `feat(evolution): the dormant Raphael rite, its unlock row, book entries and the closed-road seam (19b task 6)`.

### Task 7: versions and docs

- [ ] `LinkedMods.TESTED`: `greatsage` -> `0.0` (the jar declares 0.0.7.1; compare with how `ascension` was entered and what `LinkedModsTest` expects; a test row asserting both keys exist), `foxablazeultimate` -> `1.0`. A `LinkedModsTest` case: both keys present.
- [ ] `docs/parts/evolution_rites.md`: tick 19b; a "The Raphael merge" section (what is redirected, the order of reads above, **Mixin budget: 0 of 3 used**, the accepted hazard in plain words: *a refused redirect gives back Great Sage and Degenerate at default mastery; the mastery they had is lost; the owner accepted this on 2026-11-23, and the one-mixin alternative is documented in spec 8.3 item 4*), "Found while building" (the owner is registered by FoxAblaze's `onLearnSkill`; FoxAblaze never refuses a learn itself; Mysticism can veto the restore too).
- [ ] `verified-hooks.md`: rows for the reads above (class names `dev.dedek.greatsage...`, `com.foxablazeultimate.world.FoxAblazeGameRules`), and the resolved items 9 of "Not read".
- [ ] `docs/superpowers/specs/2026-11-22-tensura-evolution-rites-design.md`: section 8.3 items 6 and 7 get "verified (19b)" with a one-line pointer. `SPECIES-RULES.md`: the next free R-numbers for E9 (the merge) with the accepted hazard.
- [ ] `CHANGELOG.md`: entry with a **Rolling back** paragraph: part `evolutionRites` off removes the listener and the rite; with FoxAblaze absent the merge does nothing; a world that was converted keeps FoxAblaze's Raphael (nothing converts back). No network change: no "update together" note.
- [ ] `docs/superpowers/plans/2026-11-22-plan-19a-evolution-engine.md`: the 19b outline gets one line "written in full: 2026-11-23-plan-19b-great-sage-foxablaze.md".
- [ ] commit `docs(evolution): the Raphael merge, verified hooks and the accepted hazard (19b task 7)`.

### Task 8: mutants (every one must make a named test fail; revert after each)

| # | Mutant | Caught by |
|---|---|---|
| M1 | `RaphaelMerge.onUnlock` redirects when `active()` is false | `withoutTheOtherAddonNothingIsRedirected` (and the `gs-only` real test) |
| M2 | the login sweep skipped (`sweep` returns at once) | `loginConvertsAHeldSource`, `loginConvertsAHeldGreatSageRaphael` |
| M3 | `refuse` does not give back the restore skills | `ownedByAnotherPlayerRefusesAndRestoresTheRestoreSkills` |
| M4 | single-owner skipped (`ownedByOther` always false) | `ownedByAnotherPlayerRefusesAndRestores...`, `aRedirectIsPerPlayer`, the real `ownedByAnother...` |
| M5 | either-Raphael skipped (`holdsEither` ignores the target) | `eitherRaphaelHeldBlocksTheOther`, `eitherRaphaelHeldRefusesAsAlready` |
| M6 | `refuse` gives back what is held too (re-learn without the `missing` check) | `refusalGivesBackOnlyWhatIsMissing` |
| M7 | login forgets the source before the learn | `loginForgetsTheOldOneOnlyAfterTheNewOneIsLearned` |
| M8 | the redirect returns `pass` instead of `interruptFalse` | `aLearnOfTheSourceBecomesTheTarget` (the source ends up held too) |
| M9 | free form charges the price (`learnFree` without `NoMagiculeCost`) | `aFreeGrantCarriesBothTagsAndChargesNothing` |
| M10 | the dormant rite granted with the road closed (`roadOpen` returns true) | `theDormantRaphaelRiteIsClosedInGreatSage007`, `aClosedRoadHidesTheRite` |
| M11 | `present` ignores `dormant` | `aClosedRoadHidesTheRite` |
| M12 | `retired` not in the NOT_READY group | `aRetiredGrantIsNotReady`, `aRetiredGrantIsRefusedAsNotReady` |
| M13 | the two causes of a refusal say different lines | `aVetoedRedirectIsARefusalToo` |
| M14 | the nested learn is redirected again (guard removed) | `theNestedLearnIsNotRedirectedAgain` |

The script of 19a (`plan19a/mut.ps1`) is the model: patch one string, run the class under load of the gametest slot, expect the failure, `git checkout --` the file. Record the table of results in the PR.

### Task 9: verification and the PR

- [ ] `test` (unit, all) through `gt-slot.ps1`.
- [ ] The 13 runs (the 12 of the spec plus `-PpartsOff=evolutionRites`), each through the slot with a hard timeout. Expect the GameTest counts of 19a plus the new ones (the merge tests run in every run where the part is on).
- [ ] The addon runs: `-PaddonJars=...\plan19b\boot\fox-gs` (the merge in earnest), `...\plan19a\boot\boot-addons`, `...\plan19a\boot\boot-pack`, `...\plan19b\boot\gs-only`, `...\plan19b\boot\fox-only`.
- [ ] **Real dedicated-server boots** (`server-boot.ps1`, RCON, as in 19a): `fox-gs` (reaches Done, `gamerule raphaelTrulyUnique`, a clean stop, no `Exception` or `ERROR` naming our package or the merge), `gs-only` and `fox-only` (each reaches Done: the merge is inert with one of the two), and with `evolutionRites = false` (the part off: no listener, no rite).
- [ ] The merge of `origin/main` before the final matrix (other plans are building in parallel).
- [ ] Push, PR (body: what was read, the accepted hazard in one paragraph, the mutants table, the matrix and boots, what is **not** claimed: a real Great Sage evolution cannot be run in survival because its road is closed in 0.0.7.1, so the real-jar tests drive `RaphaelEvolutionService.begin` and the learn directly), wait for green CI. **Do not merge.**
