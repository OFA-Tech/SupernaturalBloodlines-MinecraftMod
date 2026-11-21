# Plan 18: Augustines, blood that remembers, and the dying turning

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build, in our own code, the nine things the owner approved on 2026-11-20: Augustine vampires, Rip Heart, Hybrid Leap, the Cursed White Oak Ash Dagger, staged venom, four Vampirism tweaks, force-fed blood, dying-and-standing-up for the turning, and blood bottles that carry their source.

**Architecture:** Pure rules go in `core` (JUnit-tested, no Minecraft classes). Behaviour is NeoForge events in the `species` part (blood, dying, Augustines, actions) and the `supernatural` part (skills data, Vampirism tweaks). **No new mixin anywhere**: `species` stays at 3 of 3, `supernatural` is untouched. Every new action is an `ExtraActions.Action`, so the hub's Slots tab and the HUD list it with no new screen. Nothing here depends on, or is compatible with, the "TVD Vampirism" mod.

**Tech Stack:** NeoForge 1.21.1, Vampirism 1.10.13, Werewolves 2.0.3.4, Tensura 2.0.1.5, Enchanted 4.2.7 (optional ones stay optional), JUnit 5, NeoForge GameTests.

**Spec:** `docs/superpowers/specs/SPECIES-RULES.md` (R20c, R20d, R20f, R31a, R32, R33, R40a, R42v, R110, R120-R127 are amended; new rules are R200-R213, written in each phase). Hidden-knowledge, hub-IA and parts rules: `docs/parts.md`, `docs/mixins.md`, `docs/superpowers/specs/2026-11-17-hub-information-architecture.md`.

## Global Constraints

- **Hidden knowledge never leaks, refusals included.** A refusal must read the same whatever secret caused it (Original, gene, line). The client sees only what a client may know: item components are synced, so a secret never goes into a synced component or tooltip.
- **The server validates everything.** The client sends a slot and an entity id; the server resolves target, reach, line of sight, PvP, claims, cooldown and eligibility itself.
- **UI-first.** Every new action is an `ExtraActions.Action` (HUD slot + hub Slots tab + `available()`), every new skill is a catalogue skill (hub Skills page), every new config key has an `AdminEntries` row (the hub Admin page; `AdminCoverageTest` fails the build otherwise; OP-only, re-checked by `AdminSettings.apply`).
- **Parts.** Species/Augustine/blood/dying/actions: `species`. Skills data and the four tweaks: `supernatural` (needs only Vampirism). Cursed dagger: `artifacts` + `species`. No new part, no new mixin. A phase may not import a part it does not need (`docs/parts.md`).
- **Permanent attribute modifiers** are listed in `StalePartModifiers.OWNED` (Augustine: `supernaturalbloodlines:augustine` under `Part.SPECIES`).
- **Network:** new or changed payloads bump the registrar's version. `SpeciesPart.NETWORK_VERSION` (Downed payload) and `LadderNet.PROTOCOL` (`"9"`, new tweak-sync payload). Changelog says "update the server and every client together".
- **Data of a part** uses the load condition `supernaturalbloodlines:part_enabled` for that part (`docs/parts.md`). A data file of a part that is off must not load.
- **Test matrix, run before every phase PR (12 GameTest runs plus unit tests):**
  1. `./gradlew runGameTestServer`
  2. `./gradlew runGameTestServer -PnoWerewolves -PnoCurios`
  3. `./gradlew runGameTestServer -PnoLinkedMods`
  4. `./gradlew runGameTestServer -PpartsOff=species`
  5. `./gradlew runGameTestServer -PpartsOff=artifacts`
  6. `./gradlew runGameTestServer -PpartsOff=speciesTensura`
  7. `./gradlew runGameTestServer -PpartsOff=supernatural`
  8. `./gradlew runGameTestServer -PpartsOff=supernaturalTensura`
  9. `./gradlew runGameTestServer -PpartsOff=hud`
  10. `./gradlew runGameTestServer -PpartsOff=celestials`
  11. `./gradlew runGameTestServer -PpartsOff=bosses`
  12. `./gradlew runGameTestServer -PpartsOff=species,artifacts`
  plus `./gradlew test`. Every phase's GameTests must pass or skip cleanly (not error) in all 12; a phase says which runs it expects to skip a class in.
- **Numbers live in config**, each with an `AdminEntries` row and a line in `docs/balance.md`.
- Each phase: one branch `feature/plan-18<x>-<name>`, one PR, `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/<part>.md` checklist ticked, in-game check done (`docs/safe-screenshots.md` rules for shots).

## Review Focus

1. **Oracle through Rip Heart:** a target at or under the threshold who is an Original gets the same action-bar line, the same cooldown and the same "done" feedback as one who is not; only the (missing) death differs. Pinned in 18d.
2. **Secrets in synced data:** a stamped bottle's synced components and tooltip must not name an Original, hybrid, tribrid or heretic source, nor reveal its kind (a client can read components). Pinned in 18a.
3. **Dying must still be killable and must not strand anyone:** void and `/kill` kill a player in the turning for real; a relog while dying resumes; giving up kills with the original blow and ends the turning; a second death after waking leaves the turning running as today; `PendingSire` still names the right sire. Pinned in 18b.
4. **Force-feed edge targets:** self, creative/spectator, an NPC, an offline player, a target already marked, a werewolf in transition (existing rules: it goes unstable), a target with vervain (even willing), a claim that cancels the interaction. Pinned in 18f.
5. **Config flips at runtime:** the mob-targeting tweaks apply to mobs that join the level after the flip; the altar tweak reaches clients through the settings copy; turning a tweak off puts nothing permanent on a player or mob. Pinned in 18i.

## Verified hooks (read from the real jars/sources in the Gradle cache)

| Need | Hook (verified) | Result |
|---|---|---|
| Vampirism sun damage | `IVampire.isGettingSundamage(LevelAccessor)` (API, cached every 8 ticks); `VampirePlayer.handleSunDamage` (private) hurts every 40 ticks with `ModDamageSources::sunDamage`; the Sun Ring is `vampirism:sunscreen` (`SunRing`), which caps `ticksInSun` | 18i sets fire from a player tick: `isGettingSundamage && !has(vampirism:sunscreen)`. Vampirism already turns fire into `vampireOnFire` x3 (`VampirePlayer.onEntityAttacked`), so no new damage code |
| Cleansing altar | `AltarCleansingBlock.useWithoutItem` acts **only on the client** (`displayRevertBackScreen`); the leave is a client packet `ServerboundSimpleInputEvent.REVERT_BACK` handled in `ServerPayloadHandler` with **no altar check** | 18i cancels `PlayerInteractEvent.RightClickBlock` on both sides (client gets the flag by the settings copy). **A modified client can still send the packet: not closable without a mixin** (see Questions) |
| Mob targeting | Vampirism swaps the player target goal's selector at `EntityJoinLevelEvent` for zombies/husks/drowned/zombie villagers, skeletons/strays, creepers (config `zombieIgnoreVampire`, `skeletonIgnoreVampire`, `creeperIgnoreVampire`); Werewolves does the same for skeletons (`skeletonIgnoreWerewolves`) | 18i adds **our own** `NearestAttackableTargetGoal<Player>` at `EntityJoinLevelEvent` LOWEST, so we run after both mods |
| DBNO for non-vampires | `VampirePlayer.onDeadlyHit` needs `getLevel() > 0`; `setDBNOTimer` is private; only `giveUpDBNO`/`tryResurrect` are public | **Cannot be driven for a non-vampire.** 18b reuses our `Downed`/`DownedScreen`. A Ghoul (level 1) *does* get Vampirism's DBNO: 18b handles that path separately |
| Garlic | `vampirism:garlic` MobEffect (`ModEffects.GARLIC`: -20% attack, -15% speed) applied by `DamageHandler.affectVampireGarlic*`; strong garlic also adds Blindness; `IVampire.doesResistGarlic` is `false` in `VampirePlayer` | 18c denies the effect with `MobEffectEvent.Applicable` (checked: `setResult`, `getEffectSource`) |
| Holy water | `DamageHandler.affectEntityHolyWaterSplash` -> `hurtModded(ModDamageSources::holyWater, amount)`; damage type `vampirism:holy_water` (`HolyDamageBridge` already keys on it) | 18c halves it in `LivingIncomingDamageEvent` |
| Blood from a bite | `BloodDrinkEvent.PlayerDrinkBloodEvent`: `getAmount/setAmount`, `getBloodSource().getEntity()` (checked with javap; `BloodEffects`, `VervainRules` already use it) | 18c scales the amount |
| `vampire_blood_bottle` | `VampireBloodBottleItem extends Item` (**not drinkable by itself**); made only by loot tables (vampire mob drops, chests), villager trades, task rewards; no capability | 18a stamps drops with `LivingDropsEvent` (checked: `getDrops()`, `isRecentlyHit()`); every other source stays unstamped = common vampire blood |
| Our turning | `TurningHandler.onDeath` (LOW, not cancelled events), `onRightClick` takes the click over because Vampirism's items refuse non-vampires; `PendingSire.settle/completed`; `BloodDrink.drank` | 18a/18b edit these in place |
| Werewolves leap | `LeapAction` is a lasting action that only raises flying speed (`jump while sprinting`); no target, no damage | 18e writes a separate target leap; the sprint-leap stays as it is |

**Not verified (listed in the final report):** a server-side guard for the altar packet; Vampirism's DBNO of a Ghoul under the GameTest harness (needs a real ticking `VampirePlayer`; 18b ships the test and a note if it can only run in a dev client); the new target goals in a live world (a GameTest checks the goal is present and picks the right player, not a full fight).

## File structure (new files; every edit to an existing file is named in its task)

```
core/blood/BloodKind.java            core/blood/BloodSource.java        core/blood/BloodBottleRules.java
core/blood/ForceFeedRules.java       core/hud/RipHeartRules.java        core/hud/LeapMath.java
core/boss/DaggerRules.java           core/bloodline/VenomStages.java    core/bloodline/AugustineBloodRules.java
parts/species/item/BloodStamp.java   parts/species/item/BloodBottles.java   parts/species/item/BloodDrops.java
parts/species/item/BloodTooltip.java parts/species/item/BottleBloodAction.java parts/species/item/AugustineSyringeItem.java
parts/species/skill/ActionClock.java parts/species/skill/RipHeartAction.java    parts/species/hybrid/HybridLeap.java
parts/species/blood/GiveBloodAction.java parts/species/blood/VampireBloodBuff.java
parts/species/augustine/AugustineRules.java  parts/species/augustine/AugustineGifts.java  (garlic, holy water, blood)
parts/hud/action/HudTargets.java     parts/supernatural/tweaks/VampirismTweaks.java  parts/supernatural/tweaks/TweakSync.java
data/.../supernatural/ladder/augustine.json  .../tree/augustine.json  .../skill/s-aug-1.json s-aug-2.json s-vam-8.json s-hyb-3.json
data/.../enchanted/circle_magic/rite/augustine.json  .../cursed_dagger.json  data/.../damage_type/rip_heart.json
```

---

## Phase 18a: Blood bottles carry their source (item 9)

PR `feature/plan-18a-blood-sources`. Needs nothing. Everything later that gives blood goes through `BloodBottles`.

### Task 1: Pure blood rules

**Files:** Create `core/blood/BloodKind.java`, `core/blood/BloodSource.java`, `core/blood/BloodBottleRules.java`; Test `src/test/java/net/ofatech/supernaturalbloodlines/core/blood/BloodKindTest.java`, `BloodBottleRulesTest.java`.

**Interfaces (produces):**
```java
public enum BloodKind { COMMON_VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, TRIBRID, HERETIC;
    public static BloodKind of(Species s);                       // ORIGINAL..HERETIC map to themselves; every other species (NONE, AUGUSTINE, witch...) -> COMMON_VAMPIRE
    public static BloodKind parse(@Nullable String speciesName); // null, "", "NONE", unknown -> COMMON_VAMPIRE (an unstamped bottle counts as common vampire blood)
    public boolean namesSire();   // true for ORIGINAL and ORIGINAL_HYBRID only: only an Original's blood names the drinker's pending sire
    public boolean secret();      // true for everything but COMMON_VAMPIRE (tooltips and synced data must not show it)
}
public record BloodSource(UUID id, String name, boolean npc, BloodKind kind) {}
public final class BloodBottleRules {
    public enum Drinker { HUMAN, WEREWOLF, WEREWOLF_IN_TRANSITION, OTHER }   // HUMAN = no faction, a Ghoul, or in the turning
    public enum Effect { MARK, MARK_AND_NAME_SIRE, TRANSITION_BLOOD, UNSTABLE, NO_EFFECT, REFUSED }
    public static Effect decide(Drinker d, BloodKind k, boolean canMark, boolean startsTransition);
}
```
`decide` table (the whole rule; R210):

| Drinker | ORIGINAL / ORIGINAL_HYBRID | every other kind |
|---|---|---|
| HUMAN, `canMark` | `MARK_AND_NAME_SIRE` | `MARK` |
| HUMAN, not `canMark` (already in the turning) | `REFUSED` | `REFUSED` |
| WEREWOLF | `startsTransition ? TRANSITION_BLOOD : REFUSED` | `NO_EFFECT` |
| WEREWOLF_IN_TRANSITION | `TRANSITION_BLOOD` (R45: a second dose changes nothing) | `UNSTABLE` (R40: other blood) |
| OTHER (vampire, hunter) | `REFUSED` | `REFUSED` |

- [ ] **Step 1: Write the tests.** `BloodKindTest`: `of(Species.AUGUSTINE)` is not yet in the enum, so assert `of(Species.NONE)==COMMON_VAMPIRE`, `of(Species.HERETIC)==HERETIC`, `parse(null)==parse("")==parse("bogus")==COMMON_VAMPIRE`, `namesSire()` true only for the two Original kinds, `secret()` false only for COMMON_VAMPIRE. `BloodBottleRulesTest`: one assertion per table cell (14 cells).
- [ ] **Step 2: Run** `./gradlew test --tests '*BloodKindTest' --tests '*BloodBottleRulesTest'` and watch both fail (classes missing).
- [ ] **Step 3: Implement** the three files (plain Java, no Minecraft classes except `UUID`).
- [ ] **Step 4: Run** the same command: PASS.
- [ ] **Step 5: Commit** `feat(blood): the blood kinds and the drinking table`.

### Task 2: Stamp, read and label a bottle

**Files:** Modify `parts/species/SpeciesRegistries.java` (new component after line 87); Create `parts/species/item/BloodStamp.java`, `parts/species/item/BloodTooltip.java`; Test `parts/species/gametest/BloodBottleGameTests.java` (new class).

**Interfaces (produces):**
```java
// SpeciesRegistries: DataComponentType<Boolean> BLOOD_SOURCE_NPC_COMPONENT  ("blood_source_npc", persistent BOOL)
public final class BloodStamp {
    public static void write(ItemStack stack, BloodSource s);   // sets BLOOD_SOURCE (uuid), BLOOD_SOURCE_SPECIES (kind.name() of a Species), BLOOD_SOURCE_NPC; sets the NAME component ONLY when !kind.secret()
    public static Optional<BloodSource> read(ItemStack stack);  // empty for an unstamped bottle; name falls back to ""
    public static BloodKind kindOf(ItemStack stack);            // unstamped -> COMMON_VAMPIRE
}
public final class BloodTooltip { public static void append(ItemStack stack, List<Component> lines); } // COMMON: "Common vampire blood" + "From <name>" (name only when present); any secret kind: one line "Strange blood". Never a kind word, never a name.
```
Detail the implementer cannot guess: the species string stored for a source is the `Species` **enum name** (`ORIGINAL`, `HYBRID`, `NONE` for a common vampire) because `TransitionBlood.accepts` and `OriginalBloodItem` already read it that way; `BloodKind.parse` turns it back. The synced UUID is a pre-existing exposure (see Question 14).

- [ ] **Step 1: Write the failing GameTest** `stampWritesTheComponents` (stamp a bottle with an ORIGINAL source: `BloodStamp.read` returns it, **`BLOOD_SOURCE_NAME_COMPONENT` is absent**) and `tooltipNeverNamesASecret` (for each of the five secret kinds the tooltip has exactly one line and it contains neither the player name nor the kind; for COMMON it contains the name).
- [ ] **Step 2: Run** `./gradlew runGameTestServer`; expect the class missing/failing.
- [ ] **Step 3: Implement** `BloodStamp`, `BloodTooltip`, the component registration.
- [ ] **Step 4: Run** the matrix runs 1 and 4 (`species` off: the class must not load): PASS.
- [ ] **Step 5: Commit** `feat(blood): a bottle remembers where its blood came from`.

### Task 3: Stamp where bottles are made

**Files:** Create `parts/species/item/BloodDrops.java`; Modify `parts/species/rite/OriginalBloodRiteFactory.java:62-70`, `parts/species/rite/HybridBloodRiteFactory.java`, `parts/species/sireline/SiresBlood.java`, `parts/species/SpeciesPart.java` (register `BloodDrops`).

- [ ] **Step 1: Write the failing tests** (in `BloodBottleGameTests`): (a) `vampireMobDropIsStamped`: kill a Vampirism `advanced_vampire` mob with a drops list holding a `vampire_blood_bottle`; the dropped stack carries `kind COMMON_VAMPIRE`, the mob's UUID, `npc=true`. (b) `noBottleFromAHuman`: a zombie's drops are untouched. (c) `originalBloodRiteStampsKind`: the Rite output carries `ORIGINAL`/`ORIGINAL_HYBRID` of its source, `npc=false`, and **no name**. (d) `hybridBloodRiteStampsHybrid`. (e) `siresBloodKeepsItsNameAndGainsKind`: Sire's Blood (cannot be drunk, never shown to a third party, name is the owner's own knowledge) keeps `blood_source_name` and now has the kind and npc flag.
- [ ] **Step 2: Implement** `BloodDrops.register()`: a `LivingDropsEvent` listener (server only) that, for a victim that `VampirismAPI.factionRegistry().getFaction(e) == VReference.VAMPIRE_FACTION` and is not a player, stamps each `ItemEntity` whose stack is in `#supernaturalbloodlines:vampire_blood` with `BloodSource(victim uuid, victim name, true, kind)`; `kind = ORIGINAL` when the victim has `SpeciesRegistries.ORIGINAL_ROOT` data naming an NPC Original other than Klaus, `ORIGINAL_HYBRID` for Klaus, else COMMON. Rite factories call `BloodStamp.write` instead of setting the components by hand.
- [ ] **Step 3: Run** matrix 1, 3 (`-PnoLinkedMods`: the class must skip), 4: PASS.
- [ ] **Step 4: Commit** `feat(blood): stamp the bottles that mobs and rites make`.

### Task 4: Drinking applies the kind's rules

**Files:** Create `parts/species/item/BloodBottles.java`; Modify `parts/species/turning/TurningHandler.java` (`onRightClick` lines 160-185: the `vampire` branch moves to `BloodBottles`; `wouldMark` becomes package-visible), `parts/species/item/OriginalBloodItem.java` (`mayDrink`, `finishUsingItem`), `parts/bosses/item/KlausBloodItem.java:finishUsingItem`, `SpeciesPart` (register).

**Interfaces (produces):**
```java
public final class BloodBottles {
    public static void register();                                    // PlayerInteractEvent.RightClickItem takeover for #vampire_blood bottles
    /** Applies one drink of {@code source}'s blood to {@code target}. Does not touch an item. @return the effect applied (REFUSED = nothing happened, keep the bottle). */
    public static BloodBottleRules.Effect apply(ServerPlayer target, BloodSource source);
    public static BloodBottleRules.Drinker drinkerOf(ServerPlayer p);  // werewolf faction by id "werewolves:werewolf"; in transition by BloodlineState.inTransition()
}
```
`apply` runs the table: `MARK` -> `TurningHandler.drankVampireBlood(target)`; `MARK_AND_NAME_SIRE` -> `BloodDrink.drank(target, source.id(), source.kind().species name, false)` then `drankVampireBlood`; `TRANSITION_BLOOD` -> `BloodDrink.drank(...)` only (as `OriginalBloodItem` does today, gated by `OriginalBloodItem.startsTransition`); `UNSTABLE` -> `TransitionHandler.drankOtherBlood`; `NO_EFFECT` -> action bar `supernaturalbloodlines.bottle.no_effect`; `REFUSED` -> `OriginalBloodItem.REFUSED` for the werewolf case, nothing otherwise. A bottle with no stamp is `COMMON_VAMPIRE` and the human case behaves **exactly as today** (this is the regression test).

- [ ] **Step 1: Write the failing tests** (`BloodBottleGameTests`): `unstampedBottleMarksAHuman` (same state as `TurningHandler.drankVampireBlood`), `originalStampNamesThePendingSire` (and `Lines.shownName` is what the chat line uses: an undiscovered line prints `?`, never the name), `werewolfDrinksAStampedBottle` (common: consumed, `no_effect`; original with `hybrid_transition_blood=original_hybrid` and a plain Original source: refused and the bottle kept), `werewolfInTransitionGoesUnstableOnCommonBlood`, `vampireAndHunterKeepTheBottle`, `aMarkedPlayerKeepsTheBottle`, and the existing `TurningGameTests` must still pass untouched.
- [ ] **Step 2: Run** and watch them fail.
- [ ] **Step 3: Implement.** `OriginalBloodItem.finishUsingItem` and `KlausBloodItem` call `BloodBottles.apply(player, source)` instead of `BloodDrink.drank` directly (Klaus's source: `BloodSource(NpcOriginal.KLAUS.uuid(), "Klaus", true, ORIGINAL_HYBRID)`). Original's Blood and Klaus's Blood now also mark vampire blood for a human (Question 12: one bottle is enough).
- [ ] **Step 4: Run** matrix 1-12: PASS (the class skips with `species` off).
- [ ] **Step 5: Commit** `feat(blood): drinking a bottle applies its kind's rules`.

### Task 5: "Bottle my blood" action and the shared action clock

**Files:** Create `parts/species/skill/ActionClock.java`, `parts/species/item/BottleBloodAction.java`; Modify `parts/species/SpeciesRegistries.java` (attachment `ACTION_READY`), `parts/species/SpeciesConfig.java` (two keys), `shared/admin/AdminEntries.java`, `shared/hud/ExtraActions.java` (id constant `BOTTLE_BLOOD`), `SpeciesPart.java:204` (install), lang, `assets/.../textures/gui/hud/bottle_blood.png` (art, see Art).

**Interfaces (produces):**
```java
public final class ActionClock {   // one saved cooldown per action id, shared by every action of this plan
    public static boolean ready(ServerPlayer p, ResourceLocation action);
    public static void start(ServerPlayer p, ResourceLocation action, int ticks); // also calls ExtraActions.cooldownStarted
    public static int left(ServerPlayer p, ResourceLocation action);
}
// SpeciesRegistries.ACTION_READY: AttachmentType<Map<String,Long>> "action_ready", unboundedMap(STRING, LONG), copyOnDeath
// SpeciesConfig: bottle_blood_cooldown_seconds INT 60 [1,3600]; bottle_blood_health_cost DOUBLE 3.0 [0,19]
```
Rules (R211): available to any player for whom `Supernatural.isVampiric(p)` and who is **not an Original or Original hybrid** (the Rite of Original's Blood stays the only way to bottle an Original; Question 13). Needs a `minecraft:glass_bottle` in the inventory (used up). Costs `bottle_blood_health_cost` health but never kills (stops at 1). Output: one `vampirism:vampire_blood_bottle` stamped `BloodSource(p, name, false, BloodKind.of(species))`, given to the inventory or dropped. Refusals (action bar, never revealing species): no bottle, cooldown, `cannot_act`. An Original asking gets the same "nothing to bottle" line as a player with no blood, so it never says "you are an Original".

- [ ] **Step 1: Write the failing GameTests** `bottleBloodGivesAStampedBottle` (hybrid: kind HYBRID, name absent, uuid is the player's, a glass bottle used, health -3, cooldown set), `bottleBloodNeverKills` (at 2 health ends at 1), `bottleBloodRefusesOriginalsLikeAnEmptyPlayer` (identical message key for an Original and for a vampire with no glass bottle), `actionClockSurvivesDeath`.
- [ ] **Step 2: Run** and watch fail. **Step 3: Implement.** **Step 4: Run** matrix 1-12: PASS. **Step 5: Commit** `feat(blood): the bottle-my-blood action`.

### Task 6: Docs

- [ ] Rewrite R20c, R20d (`blood bottle`), R40a (kind recorded on every bottle) in `SPECIES-RULES.md`; add **R210** (the table above) and **R211** (bottle my blood); `docs/parts/species.md` checklist; `docs/balance.md` (two numbers); `CHANGELOG.md` with: an unstamped bottle still counts as common vampire blood, old Original's Blood bottles keep their source and species; rolling back drops the stamps and keeps every bottle drinkable.
- [ ] **Commit** `docs(blood): R210, R211`.

---

## Phase 18b: Dying with vampire blood stands you up (item 8)

PR `feature/plan-18b-dying-turning`. Independent of 18a. Read first: PR #56 (`PendingSire`, `BloodDrink`, `TurningHandler`): nothing here changes who becomes the sire.

### Task 1: The rule that ends a given-up turning

**Files:** Modify `core/bloodline/BloodlineEvent.java` (add `TURNING_GAVE_UP`), `core/bloodline/BloodlineRules.java` (case in `transition`, new `turningGaveUp`); Test `core/bloodline/BloodlineRulesTest.java` (or the existing rules test class).

`TURNING_GAVE_UP` on someone `inTurning()`: returns `Outcome.of(s.toBuilder().turningTicksLeft(0).vampireBloodUntil(0).pendingSire(null).build())` and **no** `KILL_PLAYER` (the death is the player's own, done by the caller). Not in the turning: unchanged. A Ghoul stays a Ghoul.

- [ ] **Step 1: Write the failing tests** `gaveUpEndsTheTurning` (state cleared, no actions), `gaveUpOutsideTheTurningChangesNothing`, `gaveUpKeepsAGhoulAGhoul`. **Step 2:** run `./gradlew test --tests '*BloodlineRules*'`: FAIL. **Step 3:** implement. **Step 4:** PASS. **Step 5:** commit `feat(turning): the rule for giving up`.

### Task 2: The dying state for the turning

**Files:** Modify `parts/species/original/Downed.java`, `DownedPayload.java`, `DownedScreen.java`, `parts/species/SpeciesPart.java:241` (handler passes both flags; bump `NETWORK_VERSION`), `parts/species/original/OriginalRules.java:128` (`bypassesProtection` becomes `public static`), lang.

**Interfaces (produces):**
```java
// Downed
public static void startDying(ServerPlayer p, DamageSource cause);   // = start(p, cause) made public for the turning
public static boolean isDyingForTurning(ServerPlayer p);              // isDowned(p) && BloodlineAccess.get(p).inTurning(); an Original hybrid can never be in the turning, so the two flags are unambiguous
// DownedPayload: record DownedPayload(boolean downed, boolean turning)   // screen text only
```
Behaviour: while `isDyingForTurning`, `TurningHandler.secondPassed` does not tick the clock (the 30 s of dying are free). `Downed.tick` at `left == 1` already wakes the player at half health: add the action-bar line `supernaturalbloodlines.turning.woke` when they are in the turning. `Downed.giveUp`: if `inTurning` (state), first `BloodlineAccess.applyEvent(p, TURNING_GAVE_UP, ...)`, then the existing `clear` + `kill(p, cause)` (the original blow, so the death message and kill credit are the real ones). The screen shows `downed.turning.title`/`downed.turning.hint` when `turning` is true; the buttons stay Give up / Wait.

- [ ] **Step 1: Write the failing GameTests** (`parts/species/gametest/TurningDyingGameTests.java`): `dyingScreenFlagIsSentForTheTurning` (assert the payload object through a recording player, as `ActionRoarGameTests` does), `giveUpEndsTheTurningAndKills` (state cleared, `vampireBloodUntil == 0`, player dead with the original source, **a respawn is a plain human with no turning**), `waitWakesAtHalfHealthInTheSamePlace` (position unchanged, `inTurning` still true, clock unchanged), `clockDoesNotTickWhileDying`, `relogWhileDyingResumes` (use the reload helper `AshDaggerGameTests` uses).
- [ ] **Step 2:** run, watch fail. **Step 3:** implement. **Step 4:** run runs 1 and 4: PASS. **Step 5:** commit `feat(turning): the dying state of the turning`.

### Task 3: Cancel the death into it

**Files:** Modify `parts/species/turning/TurningHandler.java` (`onDeath` lines 99-111, `register` lines 60-75).

Behaviour: in `onDeath`, after `apply(p, DIED_WITH_VAMPIRE_BLOOD, source)` succeeds (`inTurning()` now true) and `!OriginalRules.bypassesProtection(e.getSource())`: `e.setCanceled(true)`, `p.setHealth(1)` via `Downed.startDying(p, e.getSource())`, send `turning.dying` instead of `turning.started`. A source that bypasses protection (void, `/kill`, `failed_transition`, white oak) is a real death exactly as today (the player respawns in the turning). Add two listeners:
1. A second `LivingDeathEvent` listener registered with `receiveCanceled = true` at LOW: if the event is cancelled, the player is a Ghoul, `VampirismAPI.vampirePlayer(p).isDBNO()` and `vampireBloodUntil != 0`, apply `DIED_WITH_VAMPIRE_BLOOD` and send `turning.dying` (Vampirism's own dying screen is the dying state; we add nothing).
2. A `LivingDeathEvent` listener at HIGH: if the dying player `inTurning()` and `e.getSource().is(vampirism:dbno)` (Vampirism's give-up damage type, from `giveUpDBNO`), apply `TURNING_GAVE_UP` (so a Ghoul who gives up also ends the turning).

- [ ] **Step 1: Write the failing GameTests:** `lethalBlowWithVampireBloodStandsYouUp` (a zombie-class damage that would kill: not dead, `isDowned`, `inTurning`, not respawned), `voidStillKills` (`FELL_OUT_OF_WORLD`), `killCommandStillKills`, `drinkingHumanBloodWhileDyingIsRefused` (the click is `Downed`-denied: `RightClickItem` is cancelled for a downed player; assert the bottle is kept), `afterWakingHumanBloodCompletesTheTurning` (joins the vampires, `PendingSire.completed` message for a hidden line is `HIDDEN_LINE`, sire is the pending one), `originalBloodSourceSurvivesTheDying` (pending sire intact), `aSecondRealDeathAfterWakingKeepsTheTurning`, `ghoulDbnoStartsTheTurning` and `ghoulGiveUpEndsTheTurning` (if the harness cannot tick `VampirePlayer`, mark them `@GameTest(enabled=false)` with a comment and list them in the PR as dev-client checks).
- [ ] **Step 2:** run, watch fail. **Step 3:** implement. **Step 4:** run matrix 1-12 (class skips with `species` off): PASS, and the existing `TurningGameTests`, `OriginalGameTests` (Downed), `KlausGameTests` unchanged. **Step 5:** commit `feat(turning): death with vampire blood stands you up where you fell`.

### Task 4: Docs

- [ ] Amend R20c (dying instead of dying-and-respawning; give up = a real death and the turning fails), R32's note that `Downed` also serves the turning; **R209** (the full flow). CHANGELOG: network bump, upgrade note, "a player who is mid-turning when the update lands keeps their old clock; the new flow starts at the next lethal blow". `docs/parts/species.md`. **Commit** `docs(turning): R209`.

---

## Phase 18c: Augustine vampires (item 1)

PR `feature/plan-18c-augustines`. Needs 18a only for the `BloodKind` default (an Augustine's blood is `COMMON_VAMPIRE`; nothing to add).

### Task 1: The species in the pure rules

**Files:** Modify `core/bloodline/Species.java` (append `AUGUSTINE` **last**; the codec is by name, so order is free, but keep it last), `core/bloodline/BloodlineEvent.java` (`RITE_OF_AUGUSTINE`, `AUGUSTINE_SYRINGE`), `core/bloodline/BloodlineRules.java`, `core/supernatural/SupernaturalRace.java` (add `AUGUSTINE`; `creature()` case), `core/rite/RiteKinds.java:48` (`case AUGUSTINE -> VAMPIRE`), `core/supernatural/LayerSlices.java` (no plan: it keeps the whole layer, nothing to add; add a line to `raceOf` returning `SupernaturalRace.AUGUSTINE`); Test `BloodlineRulesTest`, `RiteKindsTest`, `SupernaturalRaceTest`.

Rules (R200):
- `RITE_OF_AUGUSTINE` and `AUGUSTINE_SYRINGE` -> `madeAugustine`: needs `c.faction() == VAMPIRE`, species `NONE`, not a Ghoul, not `inTransition`. Else `Outcome.refuse(s, key)` with keys `not_vampire` (any non-vampire), `has_species` (a species, a witch layer included), `ghoul`. Success: `Outcome.of(to(s, Species.AUGUSTINE, c).build())` (no faction action; the sire is kept).
- `isVampireSpecies` and `isPrey` and `FactionRiteFactory.java:115`-style lists include `AUGUSTINE` (they are listed in the Files of Task 3).
- `cleansingBlood`: `case AUGUSTINE -> Outcome.of(humanAgain(becomeNone(s, c)).build(), Action.LEAVE_FACTION)` (the Rite of Cleansing Blood cures an Augustine like a vampire). `drankTheCure`: already cures (it is `becomeNone`); add `AUGUSTINE` to the `leaves` list. `leftBaseFaction`: `case AUGUSTINE -> Outcome.of(humanAgain(becomeNone(s, c)).build())`. `silverMoon`: default (unchanged). `awakenAsHybrid`: `Outcome.refuse(s, "augustine_hybrid")` for an Augustine (Question 2). `joinedVampire`: default.
- [ ] **Step 1: Write the tests** (one per rule above, plus: an Original, a hybrid, a heretic and a witch are refused with the right key; `RiteKinds.of(AUGUSTINE, vampire=true,...)` contains `VAMPIRE`; `SupernaturalRace.creature(Augustine state, "vampirism:vampire") == AUGUSTINE`). **Step 2:** run `./gradlew test`: FAIL (does not compile). **Step 3:** implement. **Step 4:** PASS. **Step 5:** commit `feat(augustine): the species and its rules`.

### Task 2: Every exhaustive switch and list

**Files (each already matches the other vampire species; add `AUGUSTINE` to each):** `parts/vampirismenchanted/rite/FactionRiteFactory.java:115`, `parts/species/SpeciesSupernatural.java:26`, `parts/speciestensura/ApexRule.java:20` (undead treatment; **no apex race for an Augustine in 18**, so `ApexSpec` stays), `parts/species/pack/PackAccess.java:87` (`wolf_wrong_species`), `core/boss/PikeRules.java:32` (leave out: a Valet Pike hunts Originals, hybrids and the Five), `shared/tensura/ApexLines.java`, `core/supernatural/MigrationRules.java:200` (check, leave), `parts/supernatural/client/hub/page/LinePageSupport.java:103,113` (labels), `parts/species/profile/SpeciesProfiles.java` and `ProfileKnowledge.java` (**treat like Heretic: species is a fact the viewer must have discovered**; a test asserts a stranger's profile says Unknown), `shared/hub/kit/LineColors.java` (a colour), `core/Supernatural.java` (`isVampiric`).

- [ ] **Step 1:** `./gradlew compileJava compileTestJava`: fix every non-exhaustive `switch` the compiler reports. **Step 2:** run the species and hud GameTests (run 1): the existing ones stay green. **Step 3: Commit** `feat(augustine): the species in every list`.

### Task 3: Stats, blood and the two gifts

**Files:** Create `parts/species/augustine/AugustineRules.java`, `parts/species/augustine/AugustineGifts.java`, `core/bloodline/AugustineBloodRules.java`; Modify `core/StalePartModifiers.java:25` (add `id("augustine")` to `Part.SPECIES`), `parts/species/SpeciesConfig.java`, `shared/admin/AdminEntries.java`, `parts/species/SpeciesPart.java` (register), `parts/species/turning/GhoulGates.java:138`, lang; Test `core/bloodline/AugustineBloodRulesTest.java`, `parts/species/gametest/AugustineGameTests.java`.

```java
public final class AugustineBloodRules {   // pure
    /** Blood an Augustine gets from one drink of Vampirism's {@code amount}; 0 stays 0, a positive amount never rounds to 0. */
    public static int scale(int amount, boolean victimIsVampire, double fromVampire, double fromOther);
}
```
Config keys in `supernaturalbloodlines-species.toml` (all with `AdminEntries` rows): `augustine_attack_bonus` DOUBLE 0.10 [0,1] (R200: "a little stronger than a vampire"; an Original has 0.20), `augustine_extra_health` DOUBLE 2.0 [0,20] (Original 4), `augustine_blood_from_vampire_factor` DOUBLE 3.0 [0,10], `augustine_blood_from_other_factor` DOUBLE 0.3333 [0,10].
- `AugustineRules` (like `OriginalRules`): permanent modifiers `supernaturalbloodlines:augustine` (ADD_MULTIPLIED_TOTAL attack, ADD_VALUE max health), synced on bloodline change, login, respawn and dimension change; removed when the species goes.
- Blood: a `BloodDrinkEvent.PlayerDrinkBloodEvent` listener at LOWEST (after `BloodEffects`, `VervainRules` and the Ash Dagger's `starve`, so a refused bite stays 0): for an Augustine, `setAmount(AugustineBloodRules.scale(e.getAmount(), victimIsVampire, ...))`, `victimIsVampire = faction VAMPIRE of the source` (a hybrid player counts: R2). **R20f is lifted for an Augustine**: `GhoulGates:138` becomes `if (LadderQuery.owns(p, "S-VAM-6") || species == AUGUSTINE) return;` and the drain half of Predator's Thirst in `BloodEffects:60` takes the same test (an Augustine feeds from and drains vampires without the skill).
- Garlic (S-AUG-1): `MobEffectEvent.Applicable` listener: for an Augustine owning S-AUG-1, `setResult(DO_NOT_APPLY)` for `vampirism:garlic`, and for `minecraft:blindness` while `VampirismAPI.vampirePlayer(p).isGettingGarlicDamage(level) == STRONG`.
- Holy water (S-AUG-2): `LivingIncomingDamageEvent` at LOW: damage type `vampirism:holy_water` on an Augustine owning S-AUG-2 -> `setAmount(amount * (1 - LadderQuery.value(p, "S-AUG-2", "holy_reduction", 0.5)))`.

- [ ] **Step 1: Write the tests.** Unit: `scale(3,true,3,1/3d)==9`, `scale(3,false,..)==1`, `scale(1,false,..)==1`, `scale(0,*,..)==0`, `scale(10,false,..)==3`. GameTests: `augustineHasTheBonuses` (attack +10%, health +2, modifiers removed on a cure), `augustineFeedsTripleFromAVampire` (mob vampire, amount 3 -> 9), `augustineFeedsThirdFromAHuman`, `augustineBiteOnAVampireIsNotRefusedByR20f` (the Ghoul gate does not fire), `garlicEffectIsDenied` (apply `vampirism:garlic`: absent with S-AUG-1, present without), `holyWaterHalved` (5 -> 2.5 with S-AUG-2), `skillsDormantOutsideTheBranch` (a plain vampire who owns none of them takes full damage), `staleModifierStrippedWhenSpeciesIsOff` (extend `disabledPartsLoseTheirSavedModifiers`).
- [ ] **Step 2:** run, watch fail. **Step 3:** implement. **Step 4:** run runs 1, 4, 6 (`speciesTensura` off): PASS. **Step 5:** commit `feat(augustine): stats, blood and the two gifts`.

### Task 4: The ladder, the branch and the skills

**Files:** Create `data/.../supernatural/ladder/augustine.json`, `tree/augustine.json`, `skill/s-aug-1.json`, `skill/s-aug-2.json`; Modify the lang file and `LadderDataGameTests` (the "every race has a ladder and a tree" check), `docs/parts/supernatural.md`.

`ladder/augustine.json`: rungs `L-AUG-0` (Subject, 0/0), `L-AUG-1` (Tested, sp 2, ep 2000: `level` vampirism:vampire n=2, `feed` n=10), `L-AUG-2` (Refined, sp 3, ep 5000: `level` 4, `kill` category `vampire` n=3), `L-AUG-3` (Hunter-bred, sp 4, ep 15000: `level` 8, `signal compel` n=3 part `species`), `L-AUG-4` (Apex Predator, sp 4, ep 30000: `level` 12, `kill` category `hunter` n=10), `L-AUG-5` (Perfected, sp 5, ep 60000: `level` 14, `days_as` race `augustine` n=30). No Tensura rung (no apex in 18). Names are placeholders the owner may change in the lang.
`tree/augustine.json`: `S-VAM-1` r1, `S-VAM-3` r2, `S-VAM-4` r3, `{ "skill": "S-VAM-6", "rung": 2, "after": "S-VAM-1" }` (**no quest: an Augustine earns Predator's Thirst by growing**), `{ "skill": "S-AUG-1", "rung": 3, "after": "S-VAM-6" }`, `{ "skill": "S-AUG-2", "rung": 4, "after": "S-VAM-6" }`.
`s-aug-1.json`: `{ "id": "S-AUG-1", "name": "Garlic Ward", "cost": 3, "kinds": ["rule"] }`. `s-aug-2.json`: `{ "id": "S-AUG-2", "name": "Blessed Skin", "cost": 3, "kinds": ["rule"], "params": { "holy_reduction": 0.5 } }`.

- [ ] **Step 1:** the data test fails (no ladder for `AUGUSTINE`). **Step 2:** add the files. **Step 3:** run runs 1, 7 (`supernatural` off: no skills, the species rules still work): PASS. **Step 4: Commit** `feat(augustine): its ladder and its branch`.

### Task 5: Making one: the rite and Wes's syringe

**Files:** Create `data/.../enchanted/circle_magic/rite/augustine.json`, `parts/species/item/AugustineSyringeItem.java`, recipe `data/.../recipe/augustine_syringe.json`, item model + lang; Modify `SpeciesRegistries.java` (item `augustine_syringe`, stacksTo 16), `core/rite/RiteUnlockTable.java` (`kind(m, "augustine", RiteKind.VAMPIRE)`), `src/test/.../RiteUnlockTableTest.java` (counts +1 gated, +1 kind-only), `FactionRiteFactory` (event name lookup if it is a map), the rites book (regenerate with `RITES_BOOK_WRITE=1 ./gradlew test --tests '*RitesBookTest'` and commit what it writes: chapter entry, `rites/known/augustine.json`, `rites/sealed/augustine.json`).

Rite JSON (R201): `part_enabled species`; items `vampirism:garlic` x4, `vampirism:holy_water_bottle_normal` x1, `vampirism:human_heart` x1, `minecraft:golden_apple` x1; `enchanted:medium_circle`; power 16000; factory `supernaturalbloodlines:faction_rite`, `event: rite_of_augustine`, `faction: vampire`. The target is whoever stands on the circle's centre, or the caster, as `RiteTargets.centrePlayerOrCaster` already decides.
Syringe (R202): shapeless recipe `glass_bottle` + `iron_ingot` + `vampirism:vampire_blood_bottle` + `vampirism:garlic`. `AugustineSyringeItem`: right-click in the air on **yourself** (sneaking; the same refusal as any ineligible vampire) or on **another player** who is sneaking (consent) applies `AUGUSTINE_SYRINGE`; any refusal shows one line, `supernaturalbloodlines.syringe.nothing`, whatever the reason (not a vampire, species, Ghoul, not sneaking); on success one syringe is used up, the target is told, `Lines`/profile knowledge is not touched.

- [ ] **Step 1: Write the failing tests:** `syringeMakesAPlainVampireAnAugustine`, `syringeOnAnOriginalSaysNothingHappened` (the same key as on a human), `syringeOnAnotherNeedsTheirSneak`, `riteMakesTheCircleCentreAnAugustine`, `riteRefusesAWitchVampire` (the existing refusal path, ingredients returned), `augustineRiteIsKnownToVampires` (RiteUnlockTable), `bookIsNotStale` (`RitesBookTest`). **Step 2:** run, fail. **Step 3:** implement. **Step 4:** matrix 1-12 + `./gradlew test`: PASS. **Step 5:** commit `feat(augustine): the rite and Wes's syringe`.

### Task 6: Hub and docs

- [ ] The Bloodline page, Profile and the Skills page list the species and skills from data: check in game (shots to the owner), and that a stranger's profile says Unknown. `SPECIES-RULES.md`: **R200** (the species), **R201** (rite), **R202** (syringe), **R203** (blood and gifts); R20f gets a line (lifted for an Augustine); `docs/parts/species.md`, `docs/parts/supernatural.md`, `docs/balance.md`. CHANGELOG with the migration note: no existing player changes; a world saved with an `AUGUSTINE` species and rolled back loads it as `NONE` (codec is lenient: assert in a test). **Commit** `docs(augustine): R200-R203`.

---

## Phase 18d: Targeted HUD actions and Rip Heart (item 2)

PR `feature/plan-18d-rip-heart`. Needs 18a (`ActionClock`) and 18c (the Augustine tree).

### Task 1: Actions get their target

**Files:** Create `parts/hud/action/HudTargets.java`; Modify `shared/hud/ExtraActions.java`, `parts/hud/action/ActionService.java:93-107` (`runExtra`), `parts/hud/action/FactionActions.java:79-92` (use `HudTargets`); Test `parts/hud/gametest/ActionTargetGameTests.java`.

```java
// ExtraActions.Action: add
default Component trigger(ServerPlayer p, @Nullable LivingEntity target) { return trigger(p); }   // old actions are untouched
// HudTargets
public static Optional<LivingEntity> resolve(ServerPlayer p, int entityId); // same level, alive, not p, within HudConfig.biteReach(), in sight: exactly the checks FactionActions.toggleFor does today
```
`runExtra(p, action, targetEntityId)` resolves once and calls `action.trigger(p, target.orElse(null))`; the existing Roar ignores it.
- [ ] **Step 1: Write the failing tests:** `extraActionReceivesTheResolvedTarget` (a recording action), `farTargetIsNull`, `targetBehindAWallIsNull`, and the existing `ActionRoarGameTests` unchanged. **Step 2:** run, fail. **Step 3:** implement. **Step 4:** run runs 1 and 9 (`hud` off): PASS. **Step 5:** commit `feat(hud): extra actions can have a target`.

### Task 2: The pure Rip Heart rule

**Files:** Create `core/hud/RipHeartRules.java`; Test `RipHeartRulesTest.java`.
```java
public enum Result { TOO_STRONG, FINISH, NO_EFFECT }
public static Result decide(float targetHealth, float threshold, boolean targetIsOriginal);  // > threshold: TOO_STRONG; <= threshold and Original: NO_EFFECT; else FINISH
```
- [ ] Tests: boundary 10.0/10.0 -> `FINISH`; 10.01 -> `TOO_STRONG`; Original at 3 -> `NO_EFFECT`; Original at 50 -> `TOO_STRONG` (so the refusal of a healthy target never depends on the species). Run, implement, commit `feat(hud): the Rip Heart rule`.

### Task 3: Rip Heart

**Files:** Create `parts/species/skill/RipHeartAction.java`, `data/.../skill/s-vam-8.json`, `data/.../damage_type/rip_heart.json`; Modify `data/minecraft/tags/damage_type/{bypasses_armor,bypasses_resistance,bypasses_effects,is_player_attack}.json` (add `rip_heart`), `tree/vampire.json`, `tree/heretic.json`, `tree/augustine.json` (`{ "skill": "S-VAM-8", "rung": 5, "after": "S-VAM-6" }`; **not** in Original, hybrid, tribrid trees: Question 4), `SpeciesConfig`, `AdminEntries`, `ExtraActions` (`RIP_HEART`), `SpeciesPart.java:204`, lang, art.
`s-vam-8.json`: `{ "id": "S-VAM-8", "name": "Rip Heart", "cost": 5, "kinds": ["rule"] }`. Config (species toml): `rip_heart_health` DOUBLE 10.0 [1,40], `rip_heart_cooldown_seconds` INT 60 [1,3600].
`RipHeartAction.available(p)` = `LadderQuery.owns(p, "S-VAM-8")`. `trigger(p, target)`: target null -> `hud.action.rip_heart.no_target`; cooldown -> `ActionClock` line; then `RipHeartRules.decide`. `TOO_STRONG` -> `hud.action.rip_heart.refused`, no cooldown. `NO_EFFECT` (an Original: a player of species ORIGINAL/ORIGINAL_HYBRID via `OriginalRules.isOriginal`, or an entity with `SpeciesRegistries.ORIGINAL_ROOT` data) -> the **same success line and the same cooldown as `FINISH`**, no damage, no knockback. `FINISH` -> post `AttackEntityEvent(p, target)`; cancelled -> the same `refused` line, no cooldown (claims reveal nothing); else `target.hurt(rip_heart damage source with p as attacker, target.getHealth())`; `hurt` false (PvP off, claim, immunity) -> `refused`, no cooldown; true -> success line `hud.action.rip_heart.done` with the target's display name, `ActionClock.start`, `TaskSignals.fire(p, "rip_heart", 1)`. Creative/spectator targets: `refused`.

- [ ] **Step 1: Write the failing GameTests** (`RipHeartGameTests`): `finishesAWeakMob` (health 6, dies, cooldown set), `refusesAStrongMob` (no cooldown), `originalAtLowHealthLooksTheSame` (same message key, same cooldown value, health unchanged; **this pins Review Focus 1**), `claimCancelsTheAttack` (the `BiteGameTests` pattern: a listener that cancels `AttackEntityEvent`; same `refused` key, no cooldown), `pvpOffRefusesAPlayer`, `npcOriginalBossIsUnhurt`, `notAvailableWithoutTheSkill`, `slotShowsInTheHub` (`ExtraActions.get(RIP_HEART)` present and in `LoadoutService`'s listing for an owner).
- [ ] **Step 2:** run, fail. **Step 3:** implement. **Step 4:** matrix 1-12 (class skips with `species` off; the skill data skips with `supernatural` off): PASS. **Step 5:** commit `feat(skill): Rip Heart`.

### Task 4: Docs

- [ ] **R204** (Rip Heart, with the "Originals never" and the same-feedback sentence), R32 note, `docs/parts/species.md`, `docs/parts/hud.md`, balance, CHANGELOG. **Commit** `docs(skill): R204`.

---

## Phase 18e: Hybrid Leap (item 3)

PR `feature/plan-18e-hybrid-leap`. Needs 18d (target actions, `ActionClock`).

**Reconciling with the sprint-leap (R212).** `SprintLeapClient` is a convenience for Werewolves' own leap: jump while sprinting asks for `werewolves:leap`, which only raises flying speed (no target, no damage; transformed forms only). Hybrid Leap is a different thing: a slot action that throws you at a target. They stay separate and share **one guard**: the sprint-leap never arms while a Hybrid Leap is in flight. No second Werewolves action is registered and no Werewolves class is touched.

### Task 1: The pure leap

**Files:** Create `core/hud/LeapMath.java`; Modify `core/hud/SprintLeap.java` (new parameter); Test `LeapMathTest.java`, `SprintLeapTest.java`.
```java
public static double[] impulse(double dx, double dy, double dz, double speed, double lift); // unit horizontal direction * speed, y = lift (clamped to >= 0.2)
public static boolean inReach(double distance, double maxRange);
public static boolean hits(double distanceToTarget, double hitRadius);                       // 2.5
// SprintLeap.shouldLeap(..., boolean hybridLeapInFlight) -> false while true
```
- [ ] Tests: zero vector -> no NaN (returns straight up); `inReach(12, 12)` true, `(12.01, 12)` false; `shouldLeap` false when `hybridLeapInFlight`, and every other combination unchanged (update the existing test's call sites). Run, implement, commit `feat(leap): the leap maths and the one guard`.

### Task 2: The skill and the action

**Files:** Create `data/.../skill/s-hyb-3.json`, `parts/species/hybrid/HybridLeap.java`; Modify `tree/hybrid.json`, `tree/original_hybrid.json` (`{ "skill": "S-HYB-3", "rung": 3, "after": "S-HYB-2" }`; not tribrid), `ExtraActions` (`HYBRID_LEAP`), `SpeciesConfig` + `AdminEntries` (`hybrid_leap_range` DOUBLE 12 [2,32], `hybrid_leap_damage` DOUBLE 6 [0,40], `hybrid_leap_weakness_ticks` INT 100 [0,1200], `hybrid_leap_cooldown_seconds` INT 20 [1,600]), `SpeciesPart`, `parts/hud/client/HudSend.java` + `parts/hud/client/werewolves/SprintLeapClient.java` (the client sets `hybridLeapUntil = gameTime + 20` when it sends a trigger for `HYBRID_LEAP`, and passes `now < hybridLeapUntil` as the new parameter), the hub Settings page row for `sprint_leap` gets a one-line note (lang), lang, art.
`s-hyb-3.json`: `{ "id": "S-HYB-3", "name": "Hybrid Leap", "cost": 4, "kinds": ["rule"] }`.
`HybridLeap.trigger(p, target)`: needs the skill, a target (`HudTargets`), `LeapMath.inReach`, an `ActionClock` that is ready, the player on the ground or in water-free air (not riding, not downed). Server sets `p.setDeltaMovement(impulse)`, `p.hurtMarked = true`, records `(target uuid, expiresAt = now + 30)` in a server map, `ActionClock.start`. A `PlayerTickEvent.Post` listener ends the flight on landing or expiry, and when `LeapMath.hits(distance, 2.5)` first holds: `target.hurt(p.damageSources().playerAttack(p), damage)` (PvP, claims and totems apply by the normal events) and `Weakness` for `hybrid_leap_weakness_ticks`; a `LivingFallEvent` listener cancels fall damage for 20 ticks after a flight. An Original target is hurt like anyone (there is no instant kill here). Refusals: `no_target`, `too_far`, `cooldown`, `cannot_act`; none names a species.

- [ ] **Step 1: Write the failing GameTests** (`HybridLeapGameTests`): `leapMovesTowardsTheTargetAndHits` (a zombie 6 blocks away takes the damage and Weakness, a second hit in the same flight does not repeat it), `leapIsRefusedBeyondRange`, `leapNeedsTheSkill`, `fallDamageIsWaivedAfterTheLeap`, `sprintLeapDoesNotArmDuringALeap` (calls the pure `shouldLeap`), `pvpAndClaimsApply` (cancelling `AttackEntityEvent`-equivalent: `LivingIncomingDamageEvent` cancelled -> no Weakness), `werewolfFormIsNotRequired`. **Step 2:** run, fail. **Step 3:** implement. **Step 4:** matrix 1-12 (`-PnoWerewolves`: the class must still run, it links no Werewolves class): PASS. **Step 5:** commit `feat(skill): Hybrid Leap`.

### Task 3: Docs

- [ ] **R212** (the leap and the guard); R42 note; Settings-page note; `docs/parts/hud.md`, `species.md`; balance; CHANGELOG. **Commit** `docs(skill): R212`.

---

## Phase 18f: Give blood: force-feeding (item 7)

PR `feature/plan-18f-give-blood`. Needs 18a (`BloodBottles.apply`, `BloodSource`), 18d (target actions, `ActionClock`), and benefits from 18b.

### Task 1: The pure rule

**Files:** Create `core/blood/ForceFeedRules.java`; Test `ForceFeedRulesTest.java`.
```java
public enum Refusal { NOT_A_PLAYER, SELF, VERVAIN, NOT_WILLING }
public static Optional<Refusal> check(boolean targetIsPlayer, boolean self, boolean vervain, boolean willing,
                                      boolean pvp, float healthFraction, boolean downed, float healthLimit);
```
Order: `!targetIsPlayer` -> `NOT_A_PLAYER`; `self` -> `SELF`; `vervain` -> `VERVAIN` (even when willing); `willing` (the target is sneaking) -> allowed; else allowed only when `pvp && (downed || healthFraction <= healthLimit)`, otherwise `NOT_WILLING`.
- [ ] Tests for each branch, including "willing + vervain = VERVAIN", "unwilling, pvp off, downed = NOT_WILLING", "unwilling, pvp on, 30% exactly = allowed", "31% = NOT_WILLING". Run, implement, commit `feat(blood): the force-feed rule`.

### Task 2: The action and the window's gifts

**Files:** Create `parts/species/blood/GiveBloodAction.java`, `parts/species/blood/VampireBloodBuff.java`; Modify `ExtraActions` (`GIVE_BLOOD`), `SpeciesConfig` + `AdminEntries` (`give_blood_cooldown_seconds` INT 120 [1,3600], `force_feed_health_limit` DOUBLE 0.30 [0,1], `vampire_blood_buff_enabled` BOOL true), `SpeciesPart`, lang, art; Test `parts/species/gametest/GiveBloodGameTests.java`.
`GiveBloodAction.available(p)` = `Supernatural.isVampiric(p)` (every vampire type: plain, Ghoul, Original, hybrid types, heretic, Augustine; R2: a hybrid counts). `trigger(p, target)`: cooldown/`cannot_act`/`no_target` lines; `ForceFeedRules.check(...)` with `vervain = VervainRules.protects(target)` (only when the artifacts part is on: through `BiteProtection`-style hook, or `false`), `willing = target.isShiftKeyDown()`, `pvp = server.isPvpAllowed()`; a refusal shows `hud.action.give_blood.<reason>` (no cooldown). Then claims: a willing target -> post `PlayerInteractEvent.EntityInteract(p, hand, target)`; an unwilling one -> post `AttackEntityEvent(p, target)`; cancelled -> `hud.action.give_blood.refused` (same key for every refusal that is not a rules refusal). Then `BloodBottles.apply(target, BloodSource.of(giver))` where the source is the giver (`npc=false`, `BloodKind.of(giver species)`; an Original giver names the pending sire as an Original's Blood does, through `BloodDrink.drank`). **Whatever effect `apply` returns, the giver sees the same line `give_blood.done` and the same cooldown starts** (`NO_EFFECT`/`REFUSED` included), so the action is not a probe for who is human; the target gets `give_blood.received` with the giver's display name and no species. A werewolf-faction target follows `BloodBottleRules` (a werewolf in transition goes unstable: existing rule).
`VampireBloodBuff` (config `vampire_blood_buff_enabled`): every 20 ticks, for a player with `vampireBloodUntil > gameTime`: `Regeneration` amplifier 1 for 60 ticks (ambient, no particles), and `food.setExhaustion(max(0, exhaustion - 0.15f))` (slight hunger resistance). "Faster healing" is the Regeneration; nothing else is added.
- [ ] **Step 1: Write the failing GameTests:** `forceFeedMarksAnUnwillingHumanAtLowHealth`, `refusesWhenPvpIsOff`, `refusesAHealthyUnwillingTarget`, `sneakingTargetAccepts` (even at full health, even with PvP off), `vervainBlocksEvenAWillingTarget`, `claimBlocksIt` (cancel `AttackEntityEvent` / `EntityInteract`), `originalGiverNamesThePendingSire` (and the message uses `Lines.shownName`), `werewolfInTransitionGoesUnstable`, `vampireTargetGetsTheSameDoneLine` (no oracle), `selfAndMobAndCreativeAreRefused`, `buffLastsWhileTheBloodDoes` (Regeneration II present, absent after the window), `cooldownSurvivesDeath`. **Step 2:** run, fail. **Step 3:** implement. **Step 4:** matrix 1-12 (skips with `species` off; `-PnoWerewolves`: runs): PASS. **Step 5:** commit `feat(blood): give your blood`.

### Task 3: Docs

- [ ] **R208** (the action), **R207** (the buff), CHANGELOG (the buff applies to anyone with vampire blood in them, a bottle included), `docs/parts/species.md`, balance. **Commit** `docs(blood): R207, R208`.

---

## Phase 18g: The Cursed White Oak Ash Dagger (item 4)

PR `feature/plan-18g-cursed-dagger`. Independent (artifacts + species).

### Task 1: The pure rule

**Files:** Create `core/boss/DaggerRules.java`; Test `DaggerRulesTest.java`.
```java
public static boolean lodges(boolean cursedDagger, Species victim);  // ORIGINAL: either dagger; ORIGINAL_HYBRID: the cursed dagger only; every other species: false
```
- [ ] Tests (6 cells). Run, implement, commit `feat(dagger): the rule`.

### Task 2: The item and the hit

**Files:** Modify `parts/artifacts/ArtifactItems.java:117,141` (new `CURSED_ASH_DAGGER = ITEMS.register("cursed_white_oak_ash_dagger", AshDaggerItem::new)`, creative tab), `parts/artifacts/AshDagger.java:94-118` (`isOriginal` becomes `DaggerRules.lodges(held is cursed, species)`, the wielder check accepts either dagger; the **backlash stays for both**), `lodge` stores the dagger that was used (the stack already is a copy), item model/texture/lang + `ItemShots`, `assets/.../models/item/cursed_white_oak_ash_dagger*.json` (art); Test `AshDaggerGameTests` (extend) and `ItemModelCoverageGameTests` (automatic).
- [ ] **Step 1: Write the failing GameTests:** `plainDaggerStillDesiccatesAnOriginal`, `plainDaggerDoesNotDesiccateAnOriginalHybrid` (damage lands, no effect, **wielder still takes the backlash**, dagger not used up), `cursedDaggerDesiccatesBoth`, `pullingDropsTheCursedOne` (the lodged stack stays cursed), `npcOriginalBossesAcceptEitherDagger` (`NpcOriginalEvents.kindOf` unchanged: Question 7). **Step 2:** run, fail. **Step 3:** implement. **Step 4:** run runs 1, 5 (`artifacts` off), 4: PASS. **Step 5:** commit `feat(dagger): the cursed dagger`.

### Task 3: The rite

**Files:** Create `data/.../enchanted/circle_magic/rite/cursed_dagger.json`; Modify `core/rite/RiteUnlockTable.java` (`kind(m, "cursed_dagger", RiteKind.WITCH, RiteKind.SIPHONER, RiteKind.TRIBRID)`), `RiteUnlockTableTest` counts, the book (regenerate as in 18c Task 5).
Rite JSON (R205): conditions `part_enabled artifacts` and `part_enabled species`; items `supernaturalbloodlines:white_oak_ash_dagger` x1, `supernaturalbloodlines:white_oak_ash` x2, `minecraft:echo_shard` x1; `enchanted:medium_circle`; power 20000; factory `enchanted:create_item` with `supernaturalbloodlines:cursed_white_oak_ash_dagger` x1. (The create-item factory is the one `hearing_device.json` uses.) No loot, no recipe: the rite is the only way.
- [ ] **Step 1:** tests `riteCreatesTheCursedDagger` (via the `RiteTestSupport` of the vampirism_enchanted tests), `cursedDaggerRiteIsKnownToWitches`, `bookIsNotStale`. **Step 2-4:** run, implement, matrix 1-12 + `./gradlew test`. **Step 5:** commit `feat(dagger): the rite that curses it`.

### Task 4: Rules and migration

- [ ] Rewrite **R33**: "The White Oak Ash Dagger desiccates an Original, not an Original hybrid. The Cursed White Oak Ash Dagger (the Rite of the Cursed Dagger) desiccates both." **R205**. CHANGELOG **migration note:** plain daggers keep working on Originals and are not converted; a dagger already lodged in an Original hybrid stays lodged until it is pulled or runs out; players who relied on the plain dagger against an Original hybrid need the rite; NPC Original fights are unchanged. `docs/parts/artifacts.md`. **Commit** `docs(dagger): R33, R205`.

---

## Phase 18h: Staged venom (item 5)

PR `feature/plan-18h-venom-stages`. Independent.

### Task 1: The pure stages

**Files:** Create `core/bloodline/VenomStages.java`; Test `VenomStagesTest.java`.
```java
public enum Stage { MILD, SICK, DEATH }
public static Stage of(int remainingTicks, int totalTicks, int sickAfterTicks);  // elapsed = total - remaining; remaining <= 1 -> DEATH; elapsed >= sickAfterTicks -> SICK; else MILD
```
- [ ] Tests: `of(12000,12000,4000)==MILD`, `of(8001,12000,4000)==MILD`, `of(8000,...)==SICK`, `of(2,...)==SICK`, `of(1,...)==DEATH`; `sickAfter=0` is SICK from the start. Run, implement, commit.

### Task 2: Apply them

**Files:** Modify `parts/species/hybrid/Venom.java:78-101` (`pulse`), `SpeciesConfig` + `AdminEntries` (`venom_sick_after_ticks` INT 4000 [0,12000]), lang; Test `HybridTraitsGameTests` (the existing venom tests) and a new `VenomStageGameTests`.
`pulse(target, remaining)`: MILD = today's 1 damage that cannot kill and nothing else (no hallucination); SICK = the same damage plus `Nausea` (60 ticks, hidden particles) and `Slowness I` (60 ticks) refreshed every pulse, the hallucination every 600 ticks (a mob or a client that cannot take the payload gets Nausea II as today), and one action-bar line `supernaturalbloodlines.venom.sick` the first time the stage is entered (a player; remembered by checking that the target did not already have the slowness from this pulse); DEATH = today's last pulse. **Originals keep Weakness II only, no stages.** The Venom Fang skill's 5 s of Slowness is untouched.
- [ ] **Step 1: Write the failing tests:** `mildStageOnlyHurts` (no Nausea, no Slowness at elapsed 100), `sickStageAddsNauseaAndSlowness` (elapsed 4000), `originalsAreNeverSick`, `lastPulseKillsANonOriginal` (the existing assertion), `hybridBloodStillCuresAtAnyStage`, `configMovesTheThreshold`. **Step 2:** run, fail. **Step 3:** implement. **Step 4:** run runs 1, 4, 12: PASS. **Step 5:** commit `feat(venom): mild, then sick, then death`.

### Task 3: Docs

- [ ] R42v amended, **R206**; CHANGELOG (a venom already running continues in the new stages; the clock is the effect's remaining time, so nothing is stored); `docs/parts/species.md`; balance. **Commit** `docs(venom): R206`.

---

## Phase 18i: Vampirism tweaks (item 6)

PR `feature/plan-18i-vampirism-tweaks`. Independent. Part: `supernatural` (needs only Vampirism). Werewolves is optional inside it.

### Task 1: The four switches

**Files:** Modify `parts/supernatural/SupernaturalConfig.java`, `shared/admin/AdminEntries.java` (four rows in `SUPERNATURAL_FILE`, section `supernatural`, kind BOOL; the altar row `.withClientCopy()`), lang (Admin page label and one-line note per key).
COMMON keys, all default `false` (R213): `tweak_sunlight_ignites_vampires`, `tweak_cleansing_altar_disabled`, `tweak_hostile_mobs_attack_vampires`, `tweak_skeletons_attack_werewolves`.
- [ ] **Step 1:** `AdminCoverageTest` fails until the rows exist. **Step 2:** add keys and rows. **Step 3:** `./gradlew test`: PASS. **Step 4: Commit** `feat(tweaks): the four switches and their admin rows`.

### Task 2: Sunlight sets vampires on fire

**Files:** Create `parts/supernatural/tweaks/VampirismTweaks.java`; Modify `SupernaturalPart` (register); Test `parts/supernatural/gametest/TweaksGameTests.java`.
`PlayerTickEvent.Post`, server, every 20 ticks, switch on: for a player in the Vampirism vampire faction who is alive, not creative/spectator, `VampirismAPI.vampirePlayer(p).isGettingSundamage(level)`, **no** `vampirism:sunscreen` effect (the Sun Ring), not in water: `p.igniteForSeconds(4)`. Vampirism turns that fire into its own x3 vampire fire damage (verified), so nothing else is needed. Mobs are not touched (Vampirism already burns its own vampire mobs).
- [ ] Tests: `sunlitVampireBurnsWhenOn`, `offByDefault` (no fire), `sunRingWearerDoesNotBurn` (`sunscreen` present), `vampireInTheShadeDoesNotBurn`, `humanDoesNotBurn`. Run, implement, commit `feat(tweaks): sunlight sets vampires on fire`.

### Task 3: The cleansing altar

**Files:** Create `parts/supernatural/tweaks/TweakSync.java` (copy the shape of `SpeciesTensuraSync`: a payload carrying the flag, sent at login and from `AdminSettings.onClientCopySaved`), a payload class, registered in `LadderNet` (bump `PROTOCOL` to `"10"`); Modify `VampirismTweaks`.
Both sides: `PlayerInteractEvent.RightClickBlock` on `vampirism:altar_cleansing` with the flag on: cancel with result `FAIL`; on the server also an action-bar line `tweaks.altar_disabled`. The client reads the flag from the synced copy (never from its own file). R213 states the limit: **the altar's confirm is a client packet with no altar check (verified), so a modified client can still send it; closing that needs a mixin, which is not planned** (Question 10).
- [ ] Tests: `altarClickIsCancelledWhenOn`, `altarWorksWhenOff`, `flagReachesTheClientCopy` (the sync payload round-trips; the `SpeciesTensuraSyncGameTests` pattern), `medChairIsNotAffected` (a different block). Run, implement, commit `feat(tweaks): the cleansing altar can be disabled`.

### Task 4: Mob targeting

**Files:** Modify `VampirismTweaks`.
`EntityJoinLevelEvent` at `EventPriority.LOWEST`, server side only, for a `Mob` that is not an `IFactionEntity`:
- switch `tweak_hostile_mobs_attack_vampires`: for `Zombie` (zombie, husk, drowned, zombie villager) and `AbstractSkeleton` (skeleton, stray): `mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, 10, true, false, p -> faction(p) == VReference.VAMPIRE_FACTION))`.
- switch `tweak_skeletons_attack_werewolves`: for `AbstractSkeleton`, the same with `p -> VampirismSupernatural.isWerewolf(p)` (false without Werewolves).
Creepers are not changed (Question 10). Mobs already in the level keep their goals until they reload; a flag turned off removes nothing from a mob that already has the goal (R213 says so).
- [ ] Tests: `zombieGetsTheGoalWhenOn` (goal present, picks a vampire player, ignores a human who is not a vampire... it already targets humans, so assert the **vampire** is a candidate that was not before: spawn with the flag off, assert no `Player` goal selects the vampire (use Vampirism's selector), then with it on, assert the added goal selects the vampire), `skeletonTargetsAWerewolf` (needs Werewolves: skip under `-PnoWerewolves`), `offByDefaultAddsNothing`, `aMobAlreadyInTheLevelIsUntouched`. Run, implement. **Run** matrix 1-12 (`supernatural` off: nothing loads; `-PnoWerewolves`: the skeleton test skips). Commit `feat(tweaks): hostile mobs and skeletons choose their targets`.

### Task 5: Docs

- [ ] **R213** with the three limits (altar packet, loaded mobs, creepers); `docs/parts/supernatural.md`; `docs/mixins.md` gets one line: "plan 18 adds no mixin: species stays 3 of 3, supernatural has none"; CHANGELOG (network `"10"`, update together). **Commit** `docs(tweaks): R213`.

---

## Art needed (hand to Fable 5.2, Blockbench)

| Asset | For | Notes |
|---|---|---|
| `augustine_syringe` item model + texture (and 3D model like `white_oak_ash_dagger_3d`) | 18c | Wes's syringe: glass barrel, dark red/blue serum, brass plunger |
| `cursed_white_oak_ash_dagger` item model + texture + 3D | 18g | the plain dagger's silhouette, black-purple ash, a faint violet glow; must read as "cursed" next to the plain one at 16x16 |
| HUD icons `rip_heart.png`, `hybrid_leap.png`, `give_blood.png`, `bottle_blood.png` | 18a, 18d-f | `textures/gui/hud/`, the size of `alpha_roar.png` |
| Skill icons S-AUG-1, S-AUG-2, S-VAM-8, S-HYB-3 | 18c-e | `SkillIcons` |
| Augustine species colour and emblem | 18c | `LineColors`, `EmblemData`, hub Bloodline page |
| Rite icons (book) for Augustine and Cursed Dagger | 18c, 18g | reuse the item icons if the book accepts them |
Until the art lands every task uses a placeholder (the `unknown.png` pattern) so GameTests and `ItemModelCoverageGameTests` pass.

## Questions for the owner (each with my recommendation)

1. **Who can become an Augustine?** Recommend: a plain vampire (species none, not a Ghoul, no transition); no minimum level; the syringe on yourself or on a sneaking (consenting) vampire player; everyone else refused with one uniform line.
2. **May an Augustine awaken as a hybrid?** Recommend: no (refused like a heretic); revisit if you want Augustine hybrids.
3. **Tensura for Augustines?** Recommend: none in 18 (kept race, no layer, no apex). Say if you want an apex race later.
4. **Who gets Rip Heart?** Recommend: the plain vampire, Augustine and heretic trees (rung 5, 5 SP), not Originals or hybrids. Defaults: 10 HP, 60 s cooldown, players and mobs. An Original at low health gets the *same* message and cooldown and takes nothing, so nobody can use it to find an Original.
5. **Rip Heart and PvP/claims:** recommend a refusal with one generic line, no cooldown.
6. **Hybrid Leap numbers:** recommend range 12, damage 6, Weakness 5 s, cooldown 20 s, any form, hybrid and Original hybrid only (not tribrid), 4 SP. Confirm.
7. **NPC Original hybrids (Klaus) and the daggers:** recommend NPC boss fights keep accepting both daggers (a soft-lock risk otherwise); only players are affected by the R33 change.
8. **How is the Cursed Dagger rite learned and paid for?** Recommend: known to witches, siphoners and tribrids; dagger + 2 white oak ash + 1 echo shard, 20,000 power; no loot (excluded).
9. **Venom stages:** recommend mild for the first third (4,000 of 12,000 ticks: damage only), then sick (Nausea + Slowness I, hallucinations) until death at 12,000. Originals unchanged.
10. **Tweaks, three limits:** (a) the altar's confirm is a client packet with no altar check, so a modified client can still leave the faction; fixing it needs a mixin. Recommend accepting it (UI-level disable). (b) a flipped switch affects mobs that load afterwards. (c) creepers are not "hostile mobs attack vampires" (Vampirism also gave them an avoid goal): recommend leaving them out.
11. **The blood buff:** Regeneration II lasts the whole window (24,000 ticks, 20 minutes) for anyone with vampire blood in them, a plain bottle included. That is what you asked for; recommend keeping it and adding the config switch `vampire_blood_buff_enabled` (done in 18f). Tell me if you want it shorter.
12. **Do Original's Blood and Klaus's Blood also put vampire blood in a human?** Today they do not (a human needs a vampire-blood bottle too). Recommend yes, so one bottle is enough (18a does this).
13. **Bottle my blood:** recommend: any vampire type except Originals and Original hybrids (the rite stays the only way to bottle an Original), costs 3 health (never lethal), a glass bottle, 60 s cooldown.
14. **Tooltips and secrets:** recommend: a common vampire's bottle shows its kind and source name; every other kind says only "Strange blood", and the name of a secret source is never stored in a synced component. Note: the source UUID *is* synced today (pre-existing, `BLOOD_SOURCE_COMPONENT`); for a player Original it can be resolved to a name by a modified client through Mojang's API. Recommend accepting it for now, or I add a follow-up that syncs a unit codec and keeps the UUID server-side.
15. **A real death after waking** (a mob kills you while you are still in the turning): recommend it keeps the turning, as today; only "Give up" fails it. Dying lasts 30 s (the existing `Downed` time); you wake at half health.
16. **Network versions:** recommend bumping both registrars (done in 18b and 18i), with the "update together" note.

## Self-review

- **Spec coverage:** 1 -> 18c; 2 -> 18d; 3 -> 18e (reconciled with the sprint-leap by one guard, not a second action); 4 -> 18g (R33 changed, migration note in Task 4); 5 -> 18h; 6 -> 18i (four COMMON keys, defaults false, Admin rows); 7 -> 18f (rules in `ForceFeedRules`, buff in `VampireBloodBuff`); 8 -> 18b (PR #56's `PendingSire` is re-tested, not changed); 9 -> 18a (stamped in drops, rites and the new action; tooltip; drinking by kind; unstamped = common). Not included: Inventory Sight, loot-table acquisition (no loot anywhere in this plan; the syringe is crafted, the rites are performed).
- **Types:** `BloodKind`, `BloodSource`, `BloodBottleRules.Effect`, `BloodBottles.apply` (18a) are the only blood types 18f uses; `ActionClock` (18a Task 5) is used by 18d-f; `HudTargets.resolve` and `ExtraActions.Action.trigger(p, target)` (18d Task 1) by 18e and 18f; `Downed.startDying` and `TURNING_GAVE_UP` (18b) only by 18b.
- **Proportion:** each phase's tests are named with the assertion they pin; bodies are left to the implementer except the three tables (blood, ForceFeed order, venom stages).
