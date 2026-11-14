# Plan 11: Turning the Show's Way, Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vampires are made by vampire blood, then death, then human blood. Vampirism's Sanguinare routes leave a player stuck as a Ghoul until they finish that turning.

**Architecture:**
- The new state goes in `BloodlineState.Extra`, and the state changes go in the pure `BloodlineRules` table.
- The turning reuses plan 04's `TransitionHandler`, generalised to a second kind of transition.
- Ghoul gates use Vampirism's cancellable `PlayerFactionEvent.FactionLevelChangePre`, a right-click refusal on the level-up altars, and ManasCore's `RaceEvents.SET_RACE`. No new mixin, because every part is at 3/3.

**Tech Stack:** NeoForge 1.21.1, Java 21, Vampirism, Tensura 2.0.1.5 / ManasCore, GameTests + JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-show-turning-design.md`. The hook research is in `.superpowers/sdd/11-show-turning/hooks.md`.

## Global Constraints

- **No new mixin.** `vampirism_tensura`, `species` and `species_tensura` are all at 3/3.
- **Existing vampires are untouched.** The Ghoul flag is set only when Sanguinare finishes after this update.
- **Nothing earned is taken back.** EP keeps accruing, and no levels or items are removed.
- **Every refusal tells the player why** in chat or on the action bar, using lang keys under `supernaturalbloodlines.turning.*`.
- **Any Sanguinare that finishes makes a Ghoul**, whatever caused it (bite, fang, MedChair or Totem). The proper turning never uses Sanguinare, so there is no need to tell the sources apart.
- **The transition lasts** `SpeciesConfig.transitionTicks()` (24,000 by default), the same setting as the hybrid transition.
- **Test runs:**
  - `build`
  - `runGameTestServer`
  - `-PnoWerewolves -PnoCurios`
  - `-PnoLinkedMods`
  - `-PpartsOff=species`
  - `-PpartsOff=speciesTensura`
- **Docs:** update `SPECIES-RULES.md` (new R20c to R20f), the part docs, the CHANGELOG and the wiki (`Vampires-And-Originals.md`, `Known-Limits.md`, `Troubleshooting.md`) in the task that changes the behaviour.

## Review Focus

1. **A Ghoul uses an Altar of Infusion.** The ritual never starts, no items are used up, and the player is told why. Test: `ghoulCannotStartAltarRitual` (Task 3).
2. **A player with vampire blood dies while already in the hybrid transition.** The existing hybrid rules win and no second transition starts. Test: `vampireBloodDuringHybridTransitionIgnored` (Task 2).
3. **A Ghoul who completes the turning.** They keep their current Vampirism level 1 and level normally afterwards, and a Human-raced Ghoul ends on `tensura:vampire`. Test: `ghoulCompletesTurning` (Task 2 and Task 4).
4. **Vampire blood wears off before death.** Dying then is a normal death, and no transition starts. Test: `expiredVampireBloodNoTransition` (Task 2).
5. **An existing vampire after the update.** They are not a Ghoul, can level and can evolve. Test: `existingVampireUnaffected` (Task 3).

---

### Task 1: Ghoul and turning state plus rules

**Files:**
- Modify: `core/bloodline/BloodlineState.java`. Add to `Extra`:
  - `ghoul` (boolean, default false)
  - `vampireBloodUntil` (long game time, default 0)
  - `turningTicksLeft` (int, default 0)
- Modify: `core/bloodline/BloodlineEvent.java`. Add `SANGUINARE_FINISHED`, `DRANK_VAMPIRE_BLOOD`, `DIED_WITH_VAMPIRE_BLOOD`, `TURNING_FED`, `TURNING_FAILED`.
- Modify: `core/bloodline/BloodlineRules.java`
- Modify: `core/BloodlineAccess.java`. Make the listener change-check include the new fields. Server-only fields go through `BloodlineState.withTrackingOf`.
- Test: `src/test/.../BloodlineRulesTest.java`, `src/test/.../BloodlineStateCodecTest.java`

**Interfaces:**
- Produces:
  - `BloodlineState.ghoul(): boolean`
  - `vampireBloodUntil(): long`
  - `turningTicksLeft(): int`
  - `BloodlineRules.apply(state, event, ctx)` rows for the five events
  - `BloodlineState.inTurning(): boolean` (true when `turningTicksLeft > 0`)

- [ ] **Step 1: Write the failing unit rows.**
  - `sanguinareFinishedMakesGhoul`: a human with species NONE becomes `ghoul=true`. If the player is already a vampire species, nothing changes.
  - `drankVampireBloodSetsExpiry`: `vampireBloodUntil = now + duration`.
  - `diedWithVampireBloodStartsTurning`: within the expiry, `turningTicksLeft = transitionTicks` and `vampireBloodUntil = 0`. Refused when the expiry has passed, or when a hybrid transition is running.
  - `turningFedCompletes`: `turningTicksLeft = 0` and `ghoul = false`. The outcome tells the executor to make the player a full vampire.
  - `turningFailedEnds`: `turningTicksLeft = 0`. The ghoul flag is unchanged.
  - A codec round-trip with the new fields. Old saves without the keys load with the defaults.
- [ ] **Step 2: Run** `.\gradlew.bat test`. Expected: FAIL.
- [ ] **Step 3: Implement** the fields with `lenientOptionalFieldOf` and defaults, and the rule rows.
- [ ] **Step 4: Run** `build`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(core): ghoul and turning state`.

### Task 2: The turning (blood, death, transition, feeding)

**Files:**
- Create: `parts/species/turning/TurningHandler.java`. Generalise or reuse `parts/species/hybrid/TransitionHandler.java`: the same `LivingDeathEvent` LOW listener, `PlayerTickEvent.Post` and `STEP=20` pattern. Share the code by extracting the common timer, and don't copy it.
- Create: the item tag `data/supernaturalbloodlines/tags/item/vampire_blood.json`, containing `vampirism:vampire_blood_bottle` (`required:false`).
- Modify: `parts/species/SpeciesConfig.java`. Add `vampire_blood_ticks`, default 24,000.
- Test: `parts/species/gametest/TurningGameTests.java`

**Interfaces:**
- Consumes: Task 1, `BloodlineAccess.applyEvent`, and `VampireBloodSource.isVampire(Entity)` (Vampirism `Helper.isVampire`).
- Produces: `TurningHandler.isHumanLikeBlood(LivingEntity victim): boolean`. It is true for Vampirism human creatures (villagers, players, hunters), and, when Tensura is loaded, only for players whose race is human-like (`HumanLikeLine` plus the base human-like races).

- [ ] **Step 1: Write the failing GameTests.**
  - `vampireBloodItemMarksPlayer`
  - `drinkingFromVampireMarksPlayer`
  - `deathWithVampireBloodStartsTurning`: the player respawns in turning with `turningTicksLeft` counting down.
  - `expiredVampireBloodNoTransition`
  - `turningFedHumanBloodMakesVampire`: the player is in the vampire faction at level 1.
  - `turningNotFedDiesAndStaysHuman`
  - `ghoulCompletesTurning` (the Vampirism part of it)
  - `vampireBloodDuringHybridTransitionIgnored`
- [ ] **Step 2: Run** `runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - **Marking.** `LivingEntityUseItemEvent.Finish` filtered by the tag. `BloodDrinkEvent.PlayerDrinkBloodEvent`, when `getBloodSource().getEntity()` is a vampire.
  - **Turning start.** On death within the expiry, apply `DIED_WITH_VAMPIRE_BLOOD`.
  - **Completion.** During turning, a `PlayerDrinkBloodEvent` from a human-like source applies `TURNING_FED`. The executor joins the vampire faction through `FactionPlayerHandler.joinFaction`, or for a Ghoul (already in the faction) only clears the flag.
  - **Failure.** When the timer ends, apply `TURNING_FAILED` and kill the player with a mod damage source and a message.
- [ ] **Step 4: Run** all six runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R20c and R20d, `docs/parts/species.md`, the CHANGELOG and the wiki. Commit `feat(species): turning the show's way`.

### Task 3: Ghoul gates and biting vampires

**Files:**
- Create: `parts/species/turning/GhoulGates.java`
- Modify: `en_us.json`. Add `supernaturalbloodlines.turning.ghoul_no_level`, `.ghoul_no_altar`, `.ghoul_no_evolve`, `.vampire_blood_no_fill`.
- Test: `parts/species/gametest/GhoulGameTests.java`

**Interfaces:**
- Consumes: Task 1, and Vampirism `PlayerFactionEvent.FactionLevelChangePre` (`getNewLevel()`, cancellable), `ISkillPointProvider` / `SkillPointProviders.register`.
- Produces: `GhoulGates.isGhoul(Player): boolean`, which Task 4 uses.

- [ ] **Step 1: Write the failing GameTests.**
  - `ghoulMadeBySanguinare`: apply Sanguinare and let it finish. The player is Ghoul and in the vampire faction at level 1.
  - `ghoulCannotLevelUp`: `setFactionLevel(2)` is refused, the level stays 1, and the message is sent.
  - `ghoulCannotStartAltarRitual`: right-clicking an Altar of Infusion or Inspiration is cancelled, the inventory is unchanged, and the message is sent.
  - `ghoulGetsNoSkillPoints`: if `SkillPointProviders` can reduce points, the Ghoul has 0. If it can't, record that in the report and assert the level-1 cap instead.
  - `existingVampireUnaffected`
  - `bitingVampireGivesNoBloodAndCosts`: a vampire bites a vampire mob. The blood level does not rise and drops by `bite_vampire_cost` (config, default 1).
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
  - A `MobEffectEvent.Remove`/`Expired` listener or the Sanguinare finish path applies `SANGUINARE_FINISHED`. Check which one fires reliably and record it.
  - Cancel `FactionLevelChangePre` for a Ghoul when `newLevel > 1`.
  - `PlayerInteractEvent.RightClickBlock` on the altar blocks cancels for a Ghoul.
  - `PlayerDrinkBloodEvent` from a vampire source: `setAmount(0)`, `setSaturationModifier(0)`, then `useBlood(cost, true)`. Plan 12's skill will skip this.
- [ ] **Step 4: Run** all six runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R20e and R20f, the docs and the wiki. Commit `feat(species): ghouls are stuck until they turn`.

### Task 4: Tensura side (the Ghoul race and the evolution block)

**Files:**
- Create: `parts/speciestensura/GhoulRace.java`
- Test: `parts/speciestensura/gametest/GhoulTensuraGameTests.java`

**Interfaces:**
- Consumes: `GhoulGates.isGhoul`, reached through a shared hook in `shared/` and never by importing from `parts/species` (parts never import each other). ManasCore `RaceEvents.SET_RACE` with `EventResult.interruptFalse()` and the message `Changeable` (the pattern in `parts/enchantedtensura/curse/Curses.java:36`).

- [ ] **Step 1: Write the failing GameTests.**
  - `humanGhoulShowsGhoulRace`: a `tensura:human` player who becomes a Ghoul is `tensura:ghoul`.
  - `elfGhoulKeepsRace`
  - `ghoulCannotEvolve`: a Tensura evolution through `RaceHelper.evolveRace` is refused, with the message.
  - `ghoulCompletesTurning`: after `TURNING_FED`, a human Ghoul is `tensura:vampire`, and evolutions work again.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
  - Set the race on a Ghoul change. Leave `VampireMapping.raceOnJoin` unchanged: a test pins it.
  - The `SET_RACE` listener refuses any change while `isGhoul`, except the completion change to `tensura:vampire`, which our own code makes after clearing the flag.
  - First verify that `evolveRace` goes through `SET_RACE`. If it doesn't, find the narrowest event or stop and report. Do not add a mixin.
- [ ] **Step 4: Run** all six runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.**
  - Update the wiki `Tensura-Integration.md` and the Known Limit "Tensura's progress bar can show 100% for a Ghoul; the evolution is refused".
  - Commit `feat(species_tensura): ghoul race and evolution block`.
