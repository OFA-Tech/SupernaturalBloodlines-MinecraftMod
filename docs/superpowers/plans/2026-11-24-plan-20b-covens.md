# Plan 20b: Covens (the circle link, size tiers, governance, three grand rites, the two ring rites): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the coven type of plan 20a its perks. Linked witches split a rite's cost (70% of the altar bill from a per-witch channel meter, the altar pays 30%, the rite's EP split equally), each extra member adds potency with diminishing returns, going over the meter gives a nosebleed and then damage, and if the caster dies while the rite is live the linked witches take heavy damage and weakness (Admin "lethal" option). Group size 3 / 7 / 13 gates three new grand rites (Fete des Benedictions, Night Watch, Great Harvest); the one existing rite that changes is the Rite of Mastery, whose Ultimate tier needs a linked circle of 7. A coven also gets a Regent, Elders and a Shunning vote, and a circle of 3 or more can forge a pre-bound Moon Ring for half the silver and a Sun Ring that binds at half power. No new items, no new mixin.

**Architecture:** Pure rules in `core.group.circle` and `core.group.coven`. The link rides the **existing** `RiteCost.begin / commit / discard` (called from `RiteCostMixin` and `RiteCancelMixin`, already on main): `begin(start = true)` stages a circle plan in the same `ThreadLocal` that holds the siphoned power, `commit()` charges the members' power shares, `discard()` drops them. EP leaves a player in one place only (`RiteEp`), which reads the same staged plan. Circles, the channel meter, consent, votes, bans and grand-rite cooldowns live in `GroupIndex` (new rows, same file, nothing deleted on a type switch). One new S2C payload (`CovenViewPayload`) carries the viewer's own coven card; two request kinds (`CIRCLE_CALL`, `GROUP_VOTE`) ride the existing requests channel; every action is a typed command with a hub button and an `AdminCommands` row.

**Tech Stack:** Java 21, NeoForge 21.1.265 (MC 1.21.1), Enchanted 4.2.7, Vampirism 1.10.13, Werewolves 2.0.3.4 (compile only, optional at run time), Tensura (EP only, through the two evolution factories), JUnit 5, the repo's GameTest runner.

**Spec:** [docs/superpowers/specs/2026-11-23-groups-design.md](../specs/2026-11-23-groups-design.md) (sections 6, 8, 9, 10, 11, 13, 16; this plan corrects it where the code says otherwise, see "Spec corrections"). Base plan: [20a](2026-11-23-plan-20a-groups-base.md), merged. Lore: [research/groups-lore.md](../research/groups-lore.md) sections 1 and 6.1.

Repo conventions that apply: one branch `feature/plan-20b-covens`, one PR, a `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/species.md` ticked, the rules copied into `docs/superpowers/specs/SPECIES-RULES.md` as **R250 to R268** (R269 reserved), no narrating code comments, hidden hub shots per `docs/safe-screenshots.md`.

## Global Constraints

- **No new Part, no new mixin.** Everything is inside `species` (and the two ring rites in `artifacts`, through a seam, see below). `docs/mixins.md` gets a sentence under `RiteCostMixin` ("also stages the circle plan; no new injection point") and the budget line is unchanged. Species is at its mixin limit: if a task seems to need one, stop and use the `ThreadLocal` / public API route instead.
- **Rule ids R250 to R268** (R269 reserved): 20a used R240-R249, plan 21 took R300-R329, so nothing collides. Allocation: R250 Elders, R251 membership and the shun ban, R252 opening a circle, R253 the plan at rite start, R254 potency, R255 tiers and grand rites, R256 the anchor's death, R257 the channel meter and strain, R258 consent and privacy, R259 votes, R260 shunning, R261 Regent election, R262 Moonlight Ring, R263 coven Sun Ring, R264 Fete des Benedictions, R265 Night Watch, R266 Great Harvest, R267 Mastery's Ultimate needs a circle of 7, R268 grand-rite cooldowns.
- **Config:** the existing `supernaturalbloodlines-groups.toml`, section `groups`, 30 new keys (Task 2). `AdminEntries.ALL.size()` is **229 on main today**, so **229 + 30 = 259** (add whatever has landed since; `AdminSnapshotPayload.MAX_ROWS` is 320, so about 61 rows are left for 20c, 20d and later). Every key has an Admin row, an `en_us` key and the section title already exists.
- **Wire:** one new S2C `CovenViewPayload` on the optional registrar of `LadderNet`; `HubRequests.Kind` gets `CIRCLE_CALL, GROUP_VOTE` appended after `DUEL` (it is sent by name, so an old client fails on the unknown name: that is why the bump is required); `LadderNet.PROTOCOL` becomes **the integer after the one on `main` at merge time** (`"15"` today, so `"16"`), and `SnapshotIdsWireTest.GOLDEN_PROTOCOL` follows in the same commit (its `GOLDEN_HEX` does not change: the snapshot bytes are untouched). No new C2S payload: buttons run typed commands.
- **UI-first:** every new literal of the `/group` tree has a hub control (`GroupButtons.IDS`, `GroupUiCoverageTest`) **and** an `AdminCommands` row (`C1`-`C10`, root `group`, `Ui.HUB_BUTTON`) because the Home cards and the hub run rows through `HubCommands`. No admin-only action is added (config rows only; they already ride the Admin payload at permission 2, re-checked by `AdminAccess`). A command the player can only type is a bug.
- **No new items.** The rings are the existing `moon_ring` and `sun_ring`; a ring rite drops one bound to the receiver (`ArtifactItems.BOUND_TO`) exactly as `BindItemRiteFactory` does.
- **Part isolation:** `artifacts` requires only `vampirismEnchanted` (the dependency switch in `core/Part.java`), so the ring factory in `parts/artifacts/rite` must not import a species class. It reads the circle through `shared.supernatural.CircleQuery`, a static seam that `SpeciesPart` installs (default: "no circle, size 0"), the same pattern as `HubGroups` and `HubRequests`.
- **Tests:** GameTests never loop on `entity.tick()` waiting for `tickCount` (pin time with `PackIndex.overrideClock` and drive the code under test directly); fake test players cannot mount vehicles (nothing here needs one); use `TestPlayers.createMortal` for any test where a death must be announced (`FakePlayer.die` is a no-op otherwise); heavy Gradle runs go through `C:\Users\caiom\AppData\Local\Temp\opencode\gt-slot.ps1` with `--max-workers=2`; restore `run/config` after any killed run; hidden screenshots need a **copied world** and must wait while the owner's own Minecraft window is open (`docs/safe-screenshots.md`).
- **Shapes of the existing code to keep:** `PackIndex` and `HubSnapshotPayload` are not edited; `GroupIndex` only gains rows (unknown keys already round-trip, so a 20a jar keeps them); `GroupButtons.IDS` is a `Map.of` with 10 entries today, the **maximum** `Map.of` takes: switch it to `Map.ofEntries` in the first task that adds an id.

## What was verified, and how

Read from the Enchanted 4.2.7 **sources jar** (`~\.gradle\caches\modules-2\files-2.1\net.favouriteless.enchanted\enchanted-neoforge-1.21.1\4.2.7\...-sources.jar`), from `javap` on the Mojang-mapped Minecraft jar in `~\.gradle\caches\neoformruntime\intermediate_results\compiledWithNeoForge_*_output.jar`, from the NeoForge 21.1.265 sources jar, and from the repo on `origin/main` (10aeb5eb):

| Fact | Where | Result |
|---|---|---|
| `GoldChalkBlockEntity.serverTick`: every 20 ticks, once the items are consumed, `int power = be.type.getPower()` is stored **once per start attempt**; then `power == 0 \|\| (provider != null && provider.tryConsume(power))` starts the rite (`type.create(...)`, `RiteManager.addRite`, `rite.start()`), else the chalk's own `cancel()` runs | Enchanted source | verified by reading. So `RiteCostMixin`'s `@ModifyVariable ... STORE` is hit once per attempt, **also when the power is 0**, and `RiteCost.begin` returns early for `base <= 0` today (the plan must stage the circle before that early return) |
| `Rite.start()`: `params.target = findTargetUUID(...)`, then `onStart(params)`; `false` means `stop()` and `RiteManager.removeRite`. `Rite.cancel()` refunds the consumed items and detaches the chalk, and our `RiteCancelMixin` calls `RiteCost.discard()` from it | Enchanted source + repo | verified. A rite that returns `false` from `onStart` **without** calling `cancel()` is a success (every evolution and mastery rite does), and `commit()` still runs after `start()` |
| `Rite.stop()` is public, runs `onStop` and `chalk.detatch()`; `RiteManager.removeRite(ServerLevel, Rite)` and `getRiteAt(ServerLevel, BlockPos)` are public static; `GoldChalkBlockEntity.use` itself does `rite.stop(); RiteManager.removeRite(...)` when a player interrupts a running rite | Enchanted source | verified as the **idiom**. Not yet executed from a `LivingDeathEvent` handler: Task 0 proves it |
| `RiteManager.tick` runs `activeRites.removeIf(rite -> !rite.tick())` | Enchanted source | removing a rite from inside a tick would throw: the plan queues the stop and runs it in `ServerTickEvent.Post` |
| `Rite` has no public getter for its `RiteParams` (the caster); `RiteType` has no public getter for its factory; `RiteType.create(ServerLevel, BlockPos)`, `getFirstMatching(Level, BlockPos)`, `getPower()`, `getTickPower()` are public | Enchanted source | verified. So the EP price is read by creating a throwaway rite and asking it for `EpPriced` (Task 1), and the anchor is stored by the circle itself |
| `Rite.onTick` defaults to `false`, so most rites are instant (`start` then removed at once); a rite with `tick_power > 0` or an `onTick` that returns `true` stays in the manager | Enchanted source | verified. The death window therefore means "30 s" for the instant ones and "while it runs" for Night Watch |
| MC 1.21.1 mojmap: `BonemealableBlock.isValidBonemealTarget(LevelReader, BlockPos, BlockState)` and `performBonemeal(ServerLevel, RandomSource, BlockPos, BlockState)`; `CropBlock.growCrops(Level, BlockPos, BlockState)` is public; `MobEffects.CONFUSION, WEAKNESS, DIG_SLOWDOWN, MOVEMENT_SLOWDOWN, GLOWING, LUCK, REGENERATION, DAMAGE_RESISTANCE` are `Holder<MobEffect>`; `DamageSources.magic()`, `indirectMagic(Entity, Entity)`, `genericKill()`; `Player.giveExperienceLevels(int)` and `experienceLevel` | `javap` | verified |
| `LivingDeathEvent` (cancellable, used with `EventPriority.LOWEST` by `PlagueCurses` and `FinalDeath`), `ServerTickEvent.Post`, `PlayerEvent.PlayerLoggedOutEvent`, `PlayerEvent.PlayerChangedDimensionEvent` | NeoForge sources + repo usage | verified |
| `TestPlayers.createMortal(helper)` announces a death (`die` calls `CommonHooks.onLivingDeath`); `createVulnerable` takes damage | repo | verified |
| Wards (`WardRiteFactory`, modes `area` and `door`) have **no duration** (only `ward_expel_seconds` for enforcement) | repo | verified: spec R254's "three ward durations" has no target (see corrections) |
| The Linking Spell takes `BossesConfig.linkSeconds()` at one place (`LinkingRiteFactory`, `Links.link(...)`) | repo | verified: the one existing potency knob |
| No "coven of 7" stub exists in `MasteryRules`, `MasteryRiteFactory` or the Mastery rite JSON; the requirement is new in this plan | repo (grep) | verified |
| Hub buttons run `AdminCommands` rows through `HubCommands.run` (permission re-checked, `viaGroup` rewrite only for the shared `P1-P10` rows) | repo | verified; `CommandCoverageGameTests.ROOTS` does **not** include `group`, `AdminCommandsTest` pins 62 rows |

**Not verified (each has a task that proves it first):**

1. A rite started from a `LivingDeathEvent` stop request really ends: `rite.stop(); RiteManager.removeRite(level, rite)` on a persistent rite, run on the next `ServerTickEvent.Post`, leaves `getRiteAt` empty and the chalk detached (Task 0 test `aRunningRiteCanBeStoppedFromOutside`; Task 7 repeats it with Night Watch). If it fails, R256 loses its "stops the rite" sentence and says so in the PR.
2. `RiteCost.begin(start = true)` is called exactly once per start and `commit()` runs before the rite's first `onTick` (Task 0 `beginIsCalledOncePerStart`; reading says yes, the mixin has `require = 0` so a miss is silent).
3. `RiteType.create(level, pos)` has no side effect (reading says it only builds the rite object) and the two private rite classes can implement `EpPriced` (Task 1).
4. The current counts the plan quotes: `AdminCommandsTest` 62 rows (25 `pack`), `AdminEntries.ALL` 229, `LadderNet.PROTOCOL` `"15"`; the tests `UiReferencesTest` and `ReplyScopeTest` loop over every `AdminCommands` row and may need a line per new row (Task 0 reads them, Task 11 fixes them).
5. How the rite book data for a new rite is produced: `RiteUnlocksDataTest`, `RitesBookTest` and `RiteUnlockTableTest` fail by name for every missing piece (advancements `known`/`sealed`, two book entries, a category row, an unlock row); Tasks 9 and 10 follow them.
6. Which Enchanted chalk blocks the new rite files may use: only the ones the repo's rite files already use (`enchanted:ritual_chalk`, `nether_chalk`, `enchanted:medium_circle`, `large_circle`); nothing else is assumed.
7. Plan 19a is merged (its two EP sites are on main as read here); if either site moved by the time this branch starts, Task 1 re-reads it.

## Spec corrections (made in this PR, in the spec and in the 20a outline)

1. **R254:** wards have no duration. Potency scales the Linking Spell's `link_seconds` and the radius or duration of the three grand rites (R264-R266); the three ward rites take none unless Task 0's re-read of `WardRules` finds a real knob.
2. **Q2 (16.1) and 6.2 "evolution rites take no potency":** none of the existing rites becomes a *grand* rite, with one owner-decided exception: the Rite of Mastery's **Ultimate** tier needs a linked circle of 7 (R267). 19a's evolution rites still only get the cost split.
3. **`channel` rows** are `(value, at-millis)` on the wall clock (`PackIndex.now()`, pinned in tests with `overrideClock`), not ticks; a clock that moves backwards counts as no time passed. **`bans`** rows are `{scope, player, until}` where scope is a group UUID (exile, 20c) or `type:coven` (a shunning bars **every** coven). New rows `consent` (non-default levels only) and `cooldowns` (group, key, until).
4. **10.2 table:** the Regent call is the coven's route of the one `challenge` literal (R245), not a `/coven regent` literal (20a's coverage test forbids two literals on one control). `link join [token]` takes an optional token like `accept`.
5. **7.4 and section 8 (Sun Ring):** the half-power Sun Ring is built in 20b for a vampire receiver; `sun_ring_day_walker_only` (default **false**) is the switch 20c turns on once Day Walkers exist (owner answer 1).
6. **13 items 1 and 2:** now "read in the Enchanted sources, proved by 20b Task 0" instead of "only signatures were read".
7. **9 (config table):** the 20b row lists the 30 keys of Task 2 (spec said 21).
8. **11:** the new payload is `CovenViewPayload` (a separate record, as 20a's wire test anticipated) and the request kinds are `CIRCLE_CALL` and `GROUP_VOTE` (`CHARTER_CONTEST` stays with 20c).

## Review Focus

Inputs the spec implies and no happy path covers, most likely first. Each has a test in the task named in brackets.

1. **The altar cannot pay.** The chalk cancels itself after `begin` ran: nobody may pay a power share or EP. [Task 5: `anAltarThatCannotPayChargesNobody`]
2. **Someone else uses the chalk where a circle is open.** The circle belongs to its anchor; another caster's rite is a solo cast and the circle is untouched. [Task 4: `aCircleBelongsToItsAnchorAndItsChalk`; Task 5: `someoneElseUsingTheChalkIsASoloCast`]
3. **A rite whose power is 0** (the bill is 0 after the siphoned power, or the data says 0) still needs its EP split: the early `base <= 0` return in `RiteCost.begin` must not skip the staging. [Task 5: `aZeroPowerRiteStillSplitsItsEp`]
4. **A persistent rite's per-tick power** stays on the altars: `begin(start = false)` never reads the circle and never doubles the witch discount. [Task 5: `perTickPowerIsNeverSplit`]
5. **The target is not in the circle** (or is): the target owes only its own share of the rite's EP, plus Tensura's own skill price (that part is never shared). [Task 5: `aTargetOutsideTheCirclePaysNoRiteEp`, `aTargetInsideTheCirclePaysItsShare`]
6. **A member dropped by the EP floor makes the circle too small for a grand rite or for Mastery's Ultimate:** the refusal is one generic line to the performer and says nothing about the target (E5, no oracle). [Task 10: `aCircleShortOfSevenAfterTheEpFloorIsRefusedGenerically`]
7. **The anchor dies while a member is dead, offline, in another dimension, or at 1 HP:** nobody dies except by the lethal option, nobody is hurt twice, an offline member is skipped, a member's own death only removes that member. [Task 7]
8. **A vote whose subject leaves, is shunned elsewhere, or becomes the Regent; an electorate of fewer than 3; a restart mid-vote; a double ballot; a type switch mid-vote.** [Task 8]
9. **A shunned witch is invited by another coven** or accepts an old invitation: refused in both places, the whole `shun_days`. [Task 8: `aShunnedWitchCannotJoinAnyCoven`]
10. **The clock goes backwards** (server time reset): the meter does not refill and does not go negative. [Task 3: `aClockThatGoesBackGivesNoTime`]
11. **Great Harvest is a server-cost bomb in the wrong hands:** radius, potency and blocks touched are all capped and the cap is an Admin row. [Task 9: `greatHarvestStopsAtTheBlockCap`]
12. **`link_enabled` off** must not leave Mastery's Ultimate unreachable: the requirement is waived with the link (R267). [Task 10: `theGateIsWaivedWhenTheLinkIsOff`]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`):
- `core/group/circle/`: `Consent.java`, `CirclePlan.java`, `ChannelMeter.java`, `Potency.java`, `Tiers.java`: pure.
- `core/group/coven/`: `VoteKind.java`, `VoteTally.java`, `CovenRules.java`, `RingRules.java`, `GrandRites.java`, `CovenView.java`, `CovenWire.java`: pure.
- `parts/species/group/circle/`: `CircleLink.java` (sessions, in memory), `CircleSites.java` (find the chalk, the radius), `CircleStage.java` (the staged plan the cost hook and `RiteEp` read), `RiteEp.java`, `EpPriced.java`, `Strain.java`, `LinkWindow.java`, `CovenPotency.java`.
- `parts/species/group/coven/`: `CovenCommand.java` (the literals added to the shared tree), `CovenGovernance.java`, `CovenViews.java`, `CovenRegistry.java`; `parts/species/group/GroupLead.java` (the lead hand-over extracted from `PackAbsence.depose`).
- `parts/species/rite/`: `GrandGate.java`, `FeteRiteFactory.java`, `NightWatchRiteFactory.java`, `HarvestRiteFactory.java`; `parts/artifacts/rite/CovenForgeRiteFactory.java`.
- `shared/supernatural/CircleQuery.java`, `HubCoven.java` (seams).
- `parts/supernatural/net/CovenViewPayload.java`; `parts/supernatural/client/CovenClient.java`; `parts/supernatural/client/hub/page/CovenCards.java`, `CovenLabels.java`.
- Data: `data/supernaturalbloodlines/enchanted/circle_magic/rite/fete_des_benedictions.json`, `night_watch.json`, `great_harvest.json`, `coven_moon_ring.json`, `coven_sun_ring.json`, plus what `RitesBookTest` and `RiteUnlocksDataTest` ask for each (advancements, book entries, category rows).
- GameTests (`parts/species/gametest/`): `CovenHookGameTests`, `CovenCircleGameTests`, `CovenCostGameTests`, `CovenDeathGameTests`, `CovenGovernanceGameTests`, `CovenRiteGameTests`, `CovenRingGameTests`.
- `docs/superpowers/specs/` corrections, `wiki/Groups.md` (extend), `wiki/Rites.md`.

Create (test, `src/test/java/net/ofatech/supernaturalbloodlines/`): `core/group/circle/CirclePlanTest`, `ChannelMeterTest`, `PotencyTest`, `TiersTest`; `core/group/coven/VoteTallyTest`, `CovenRulesTest`, `RingRulesTest`, `GrandRitesTest`; `parts/species/group/GroupIndexCovenTest`, `CovenConfigTest`, `CovenTextTest`; `parts/evolutionrites/NoDirectEpSpendTest`; `parts/supernatural/net/CovenViewWireTest`.

Modify: `parts/species/witch/RiteCost.java`, `parts/evolutionrites/rite/EvolutionRiteFactory.java` and `MasteryRiteFactory.java` (EP through `RiteEp`, the Ultimate gate), `parts/evolutionrites/mastery/MasteryApply.java` (the refund through `RiteEp.restore`), `core/evolution/MasteryRules.java` (a new reason), `parts/bosses/rite/LinkingRiteFactory.java` (potency), `parts/species/group/GroupIndex.java`, `GroupCommand.java`, `GroupGate.java`, `GroupsConfig.java`, `GroupsRegistry.java`, `parts/species/pack/PackCommand.java` (the shun check in invite and accept, circle closing in leave and kick), `PackAbsence.java` (extract), `parts/species/SpeciesPart.java` (factory registration, seams), `parts/species/SpeciesHub.java` (request source), `parts/artifacts/ArtifactsPart.java`, `shared/supernatural/HubRequests.java`, `shared/admin/AdminEntries.java`, `AdminCommands.java`, `parts/supernatural/net/LadderNet.java`, `client/hub/page/GroupButtons.java`, `GroupLabels.java`, `HomePage.java`, `PackPage.java`, `BookPackPage.java`, `ProfilePage.java`, `core/rite/RiteUnlockTable.java`, the tests named in each task, `en_us.json`, `CHANGELOG.md`, `SPECIES-RULES.md`, `docs/parts/species.md`, `docs/parts/artifacts.md`, `docs/mixins.md`, `wiki/Rites.md`, `wiki/Configuration.md`, `wiki/Rules-Reference.md`, `wiki/Witches-And-Siphoners.md`.

---

### Task 0: Baseline, rebase and prove the hooks (no feature code)

**Files:** Create `parts/species/gametest/CovenHookGameTests.java` (kept as a regression test); Modify `docs/parts/species.md` (a "Covens: hooks read" note).

- [ ] **Step 1:** `git fetch origin`; branch `feature/plan-20b-covens` from `origin/main`. Record `AdminEntries.ALL.size()`, `AdminCommandsTest` counts, `LadderNet.PROTOCOL`, `AdminSnapshotPayload.MAX_ROWS`; if plan 19a's EP sites or anything under `parts/species/group` moved since this plan was written, re-read them and amend the plan notes in the PR (do not copy other branches' changes).
- [ ] **Step 2: Baseline run** `./gradlew test` and `./gradlew runGameTestServer` once, through `gt-slot.ps1`; record the counts. Every later "unchanged" claim is measured against them.
- [ ] **Step 3: Read** `MixinHooksGameTests` (how it reaches the chalk's `gameTime % 20 == 0` branch without waiting on `tickCount`: reuse that way, never loop), `RiteTestSupport`, `CommandCoverageGameTests`, `AdminCommandsTest`, `UiReferencesTest`, `ReplyScopeTest`, `GroupUiCoverageTest`, `RitesBookTest`, `RiteUnlocksDataTest`, `PackCommand.accept/leave/kick`, `PackAbsence.depose` and `WardRules`; list in the PR what each new row, literal and rite must add to them.
- [ ] **Step 4: Write the failing proofs** (`CovenHookGameTests`, each prints what it saw so the PR can quote it):
  - `beginIsCalledOncePerStart`: a real gold chalk with a linked altar and a one-item rite type; a counter on `RiteCost.begin(start = true)` (a package-private test seam, `AtomicInteger`, reset per test) is 1 after the start; with an altar that cannot pay it is 1 and `discard` ran (the chalk cancelled itself).
  - `commitRunsRightAfterStartAndBeforeTheFirstTick`: a persistent rite (any `RiteType` of the registry with `getTickPower() > 0`; the test picks the first one and skips with a message when there is none) records the order `begin`, `onStart`, `commit`, first `onTick`.
  - `aRunningRiteCanBeStoppedFromOutside`: with that rite running, queue `rite.stop(); RiteManager.removeRite(level, rite)` for the next `ServerTickEvent.Post` and assert `RiteManager.getRiteAt` is `null` and the chalk is detached; then do the same from a `LivingDeathEvent` handler of a `createMortal` player.
  - `aThrowawayRiteCanBeCreatedFromItsType`: `RiteType.create(level, pos)` on `mastery_ultimate` returns a rite, adds nothing to `RiteManager` and consumes nothing.
  - `aZeroPowerRiteStillReachesBegin`: a rite type with `power` 0 calls `begin` with `base = 0`.
- [ ] **Step 5: Run** `./gradlew runGameTestServer` (through the slot). Expected: the proofs that read as true pass; **a failing proof is a finding**: write it in the PR and amend the task it affects (Tasks 4 and 6 name the fallback).
- [ ] **Step 6: Hidden hub shots** of the Coven page in the three themes with a coven of 3 online and 1 offline (copied world, owner's window closed), saved outside the repo as the "before" set for Task 12.
- [ ] **Step 7: Commit** `test(covens): prove the rite cost and stop hooks the link relies on`.

---

### Task 1: `RiteEp`: the one place EP leaves a player (refactor, behaviour unchanged)

**Files:**
- Create: `parts/species/group/circle/RiteEp.java`, `EpPriced.java`; test `parts/evolutionrites/NoDirectEpSpendTest.java`.
- Modify: `EvolutionRiteFactory.java` (`grant` lines ~94, ~100 restore, ~110 spend), `MasteryRiteFactory.java` (~124 spend), `MasteryApply.java` (~37 restore).

**Interfaces:**
- Produces: `interface EpPriced { double epCost(); }` implemented by the two private rite classes (`EvolutionRite`: `recipe.epCost()`; `MasteryRite`: `EvolutionRitesConfig.masteryEp(riteTier)`).
- Produces: `RiteEp.charge(ServerPlayer target, double total)`: when no staged plan exists it is exactly `EnergyHelper.setBaseMaxEP(target, base - total)` (skipped for `total <= 0`); `RiteEp.restore(ServerPlayer, double value)` (what the three "give back Tensura's price" lines do today); `RiteEp.owedBy(ServerPlayer target, double total)`: what the **target** must hold (`total` today); `RiteEp.priceOf(RiteType, ServerLevel, BlockPos)`: `Optional<Double>` from the throwaway rite. Tasks 3 and 4 fill in the staged branch; here it is a stub that always answers "no plan".

- [ ] **Step 1: Write the failing tests.** `NoDirectEpSpendTest` (reads the sources under `parts/evolutionrites/rite` and `parts/evolutionrites/mastery` as text): no `setBaseMaxEP` outside `RiteEp` (and the test-support classes, named in an allow-list with a reason each). The existing `EvolutionRiteGameTests`, `MasteryGameTests`, `FreeGrantGameTests` run **unchanged** and are the behaviour proof.
- [ ] **Step 2: Run** `./gradlew test --tests '*NoDirectEpSpendTest'`. Expected: FAIL (three direct calls).
- [ ] **Step 3: Implement.** Replace the five direct calls; keep the order inside `grant` and `MasteryRite.onStart` exactly (EP is still the last mutation after the confirmed learn).
- [ ] **Step 4: Run** the Step 2 command, then `./gradlew runGameTestServer`. Expected: PASS; the 19a GameTest counts equal the Task 0 baseline.
- [ ] **Step 5: Mutants** (apply, run the named test, confirm FAIL, revert): C1 `charge` spends nothing for a solo target (`MasteryGameTests` EP rows); C2 `restore` removed from the veto path (`EvolutionRiteGameTests` veto row); C3 a direct `setBaseMaxEP` added to a rite factory (`NoDirectEpSpendTest`).
- [ ] **Step 6: Commit** `refactor(rites): EP leaves a player in one place`.

---

### Task 2: Config and Admin rows

**Files:**
- Modify: `parts/species/group/GroupsConfig.java`, `shared/admin/AdminEntries.java`, `AdminEntriesTest` (expected size), `AdminCoverageTest` (no change: the file is already listed), `en_us.json`.
- Test: `parts/species/group/CovenConfigTest.java`.

**Interfaces:** accessors on `GroupsConfig` (same file `supernaturalbloodlines-groups.toml`, section `groups`; `override(key, value)` already exists). The 30 keys (default; range):

| Group | Keys |
|---|---|
| link | `link_enabled` (true), `link_member_share` (0.70; 0.0-0.9, so the altar always pays something), `channel_cap` (3000; 1-100000), `channel_other_factor` (0.5; 0-1), `channel_regen_minutes` (10; 1-1440), `ep_keep_percent` (10; 0-90) |
| potency and tiers | `potency_max_bonus` (1.0; 0-10), `potency_falloff` (0.8; 0-0.99), `tier1_members` (3), `tier2_members` (7), `tier3_members` (13): each 2-13, read as a non-decreasing triple (`Tiers.of` clamps), 13 being the circle's size limit |
| the anchor's death | `link_window_seconds` (30; 5-3600), `link_death_damage_percent` (60; 0-100), `link_death_lethal` (false), `link_shaken_minutes` (30; 0-1440) |
| governance | `elder_min_days` (3; 0-365), `members_per_elder` (4; 1-100), `vote_hours` (24; 1-168), `election_cooldown_days` (7; 0-365), `shun_days` (7; 1-365) |
| rings and Mastery | `moonlight_ring_enabled` (true), `sun_ring_forge_enabled` (true), `sun_ring_day_walker_only` (false), `mastery_ultimate_circle` (7; 0-13, 0 = no requirement) |
| grand rites | `grand_rites_enabled` (true), `grand_rite_cooldown_hours` (12; 0-720), `grand_rite_radius` (24; 4-64), `fete_minutes` (10; 1-120), `harvest_xp_levels` (30; 0-1000), `harvest_max_blocks` (2048; 64-65536) |

- [ ] **Step 1: Write the failing tests.** `AdminEntriesTest`: size +30; `CovenConfigTest`: `defaultsAnswerBeforeTheFileLoads` (every default above), `overrideRestores`, `everyRangeIsClamped` (each int and double key against its bounds), `tiersAreReadAsANonDecreasingTriple` (7, 3, 13 answers 7, 7, 13), `theMemberShareNeverReachesOne` (0.95 override answers 0.9). The existing `AdminCoverageTest` fails by key name if a row is missing.
- [ ] **Step 2: Run** `./gradlew test --tests '*CovenConfigTest' --tests '*AdminEntriesTest' --tests '*AdminCoverageTest'`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement** the keys in `GroupsConfig` (copy the shape of the five existing ones) and one `SettingEntry.of(GROUPS_FILE, "groups", key, kind)` per key in the existing `groups` block of `AdminEntries`; lang `supernaturalbloodlines.hub.admin.key.supernaturalbloodlines-groups.toml:<key>` per key, with a one-line comment text for the three rows a server owner will ask about (`link_death_lethal`, `link_member_share`, `mastery_ultimate_circle`).
- [ ] **Step 4: Check the bound.** `AdminEntries.ALL.size() <= AdminSnapshotPayload.MAX_ROWS` still holds (259 of 320); if other plans landed first and it does not, raise `MAX_ROWS` here (the protocol bump of Task 11 covers it).
- [ ] **Step 5: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 6: Mutants:** C4 a key without its Admin row (`AdminCoverageTest` names it); C5 a default changed (`defaultsAnswerBeforeTheFileLoads`); C6 a range removed (`everyRangeIsClamped`).
- [ ] **Step 7: Commit** `feat(covens): the coven config and its Admin rows`.

---

### Task 3: The pure circle rules

**Files:**
- Create: `core/group/circle/Consent.java`, `CirclePlan.java`, `ChannelMeter.java`, `Potency.java`, `Tiers.java`.
- Test: `core/group/circle/CirclePlanTest.java`, `ChannelMeterTest.java`, `PotencyTest.java`, `TiersTest.java`.

**Interfaces (all pure, no game classes, every number a parameter):**
- `enum Consent { NONE, NOSEBLEED, DAMAGE; String id(); static Consent parse(String) }` (unknown text answers `NONE`).
- `ChannelMeter.valueAt(int stored, long atMillis, long nowMillis, int cap, int regenMinutes)` (lazy refill of `cap` per `regenMinutes`, clamped to `cap`, a `now` before `at` adds nothing); `ChannelMeter.capacity(boolean witchType, int cap, double otherFactor)`; `ChannelMeter.zone(int overdraw, int cap)`: `CLEAN`, `NOSEBLEED` (`0 < d <= cap/2`), `DAMAGE` (`cap/2 < d <= cap`), `REFUSED` (`d > cap`); `ChannelMeter.damageHp(int overdraw, int cap)` = `floor(10 x (d - cap/2) / (cap/2))`, 0 to 10.
- `CirclePlan.Member(UUID id, int meter, int cap, Consent consent, double baseMaxEp, boolean anchor)`; `CirclePlan.plan(int altarBill, double epTotal, List<Member> ordered, double memberShare, int epKeepPercent)` returns `Result(int altarPays, Map<UUID, Share> shares, List<Dropped> dropped, int passes)` with `Share(int power, double ep, ChannelMeter.Zone zone)` and `Dropped(UUID id, DropReason reason)` (`TOO_HEAVY`, `NEEDS_CONSENT`, `EP_FLOOR`); `CirclePlan.split(long total, int n)` (floor, the remainder one each to the first members, anchor first).
- `Potency.of(int n, double max, double falloff)` = `1 + max x (1 - falloff^(n-1))`, `n < 1` answers 1; `Tiers.of(int n, int t1, int t2, int t3)` returns 0 to 3.

- [ ] **Step 1: Write the failing tests** (the spec's worked table is the fixture; names are the rows):
  - `CirclePlanTest`: `aSoloCastChangesNothing` (N = 1: altar pays `A`, no shares); `threeShareAFortyFiveHundredBillClean` (A 4500, N 3: pool 3150, 1050 each, altar 1350); `twentyThousandRiteAtThreeNeedsConsent` (A 15000, N 3: 3500 each, over the cap by 500, `NOSEBLEED`; consent `NONE` drops all three and the plan recomputes to a solo cast, consent `NOSEBLEED` keeps them); `twentyThousandRiteAtSevenIsClean` (1500 each); `thirtySixThousandAtThirteenSplitsExactly` (A 27000, pool 18900, eleven pay 1454 and two pay 1453, altar 8100, the shares add up to the pool); `sharesAlwaysAddUpToThePool` (property over N 2..13 and many bills); `theAnchorPaysTheExtraUnit`; `aMemberOverTheHardCapIsDroppedAndTheRestRecomputed`; `consentNoneDropsANosebleedMember`; `consentNosebleedDropsADamageMember`; `damageConsentKeepsADamageMember`; `epIsSplitTheSameWay` (2,000,000 over 7: five 285,714 and two 285,715... assert `sum == total` and the two extra go to the first two); `aMemberBelowTheEpFloorIsDropped` (share would leave under 10% of their base: `EP_FLOOR`); `recomputeConvergesInAtMostThirteenPasses` (a cascade where each drop pushes the next over); `fewerThanTwoLeftIsASoloCast`; `theMemberShareZeroGivesAnAllAltarBill`.
  - `ChannelMeterTest`: `refillsLazilyUpToTheCap`, `aFullMeterStaysFull`, `aClockThatGoesBackGivesNoTime` (Review Focus 10), `otherKindsHaveHalfTheCap`, `zoneBoundariesAreInclusiveOnTheLowerZone` (d = cap/2 is `NOSEBLEED`, cap/2 + 1 is `DAMAGE`, cap is `DAMAGE`, cap + 1 `REFUSED`), `damageGrowsFromZeroToTen`.
  - `PotencyTest`: the table 1.000, 1.200, 1.360, 1.488, 1.590, 1.738, 1.866, 1.931 (N = 1, 2, 3, 4, 5, 7, 10, 13); `strictlyIncreasingWithShrinkingSteps`; `nBelowOneIsOne`.
  - `TiersTest`: 0 below `t1`, 1 at `t1`, 2 at `t2`, 3 at `t3`, a triple that is not increasing is clamped.
- [ ] **Step 2: Run** `./gradlew test --tests '*CirclePlanTest' --tests '*ChannelMeterTest' --tests '*PotencyTest' --tests '*TiersTest'`. Expected: compile error.
- [ ] **Step 3: Implement.** The plan is a loop: compute shares for the current list, drop the first (in list order) offender class by class (`REFUSED`/`TOO_HEAVY`, then consent, then EP floor), repeat; at most 13 passes (the circle's size), then it is a pure function of the final list.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Mutants:** C7 the member share not divided by N (`threeShareAFortyFiveHundredBillClean`); C8 the altar charged the full bill (same test); C9 the remainder lost (`sharesAlwaysAddUpToThePool`); C10 consent ignored (`consentNoneDropsANosebleedMember`); C11 a member over the hard cap kept (`aMemberOverTheHardCapIsDropped...`); C12 one recompute pass only (`recomputeConvergesInAtMostThirteenPasses`); C13 potency linear (`strictlyIncreasingWithShrinkingSteps`); C14 tier `>` for `>=` (`TiersTest`); C15 the meter refills past the cap (`refillsLazilyUpToTheCap`); C16 a backwards clock adds time (`aClockThatGoesBackGivesNoTime`).
- [ ] **Step 6: Commit** `feat(covens): the pure circle plan, channel meter, potency and tiers`.

---

### Task 4: Circle sessions and the stage

**Files:**
- Create: `parts/species/group/circle/CircleLink.java`, `CircleSites.java`, `CircleStage.java`, `shared/supernatural/CircleQuery.java`; test `parts/species/gametest/CovenCircleGameTests.java`, `parts/species/group/GroupIndexCovenTest.java`.
- Modify: `GroupIndex.java` (rows `channel`, `consent`), `GroupsRegistry.java`, `PackCommand.java` (`leave` and `kick` close the leaver's circle), `SpeciesPart.java` (install the seam), `HubRequests.Kind` (+`CIRCLE_CALL`, +`GROUP_VOTE`), `SpeciesHub.java` (the request source: circle calls to the invitee), `GroupCommand.java` / new `CovenCommand.java` (the `link` literals).

**Interfaces:**
- `GroupIndex`: `ChannelRow channelOf(UUID)` / `void setChannel(UUID, int value, long atMillis)` (a full and idle row is not stored), `Consent consentOf(UUID)` / `setConsent(UUID, Consent)` (only non-default stored); both added to the `KNOWN` set so an older build keeps them as extras.
- `CircleSites.chalkNear(ServerLevel, BlockPos feet)`: the `GoldChalkBlockEntity` within 5.5 blocks flat and 2 up or down (the radius `LinkingRiteFactory` uses), found by scanning the 11 x 5 x 11 box once per button press (never per tick); `CircleSites.inside(chalk, entity)`.
- `CircleLink`: in-memory sessions keyed by `(dimension, chalk pos)` with `anchor`, `members` in join order (anchor first, at most 13), `lastActive`; `open(anchor)`, `close(anchor)`, `invite`/`join(token)`/`leave(member)`, `current(level, chalk)`, `of(member)`. Calls use a second `CallRequests` instance (`CIRCLE_CALLS`, caller = anchor, sired = invitee; a minute, single use); `join` takes the token (or the newest one, like `accept`).
- Commands (literals of the shared tree, `wrong_type` outside a coven; Brigadier and the action both re-check): `link open`, `link close`, `link join [token]`, `link leave`, `link consent <level>`. Their rows (added in Task 11): `C1` `group link open`, `C2` `group link close`, `C3` `group link join [token]` (page REQUESTS), `C4` `group link leave`, `C5` `group link consent <level>`.
- `CircleStage`: the plan the cost hook stages (Task 5) and `RiteEp`, `GrandGate` and `CovenPotency` read: `CircleStage.current()` (nullable, a `ThreadLocal` the cost hook owns), `size()` (1 when none).
- `CircleQuery` (shared seam): `size(ServerLevel, BlockPos chalk)` and `staged()`; default 0 and "no plan".

Rules enforced here: the anchor is a coven member standing inside the circle; invitees are members **of the same coven** within 32 blocks; a player is in one circle; a circle closes after 5 minutes idle, on a type switch (`GroupSwitcher.onSwitched`), when the anchor logs out or changes dimension before the rite starts, when the anchor presses Close, and for a member who leaves, is kicked, is shunned or logs out (only that member goes); `none` is the default consent and it is stored per player, not per circle.

- [ ] **Step 1: Write the failing tests.**
  - `GroupIndexCovenTest`: `channelRoundTrips`, `aFullIdleChannelRowIsNotStored`, `consentStoresOnlyNonDefault`, `unknownKeysStillRoundTrip` (the 20a test, extended with the new keys), `aDamagedChannelRowCostsOnlyThatRow`.
  - `CovenCircleGameTests`: `anAnchorOpensACircleAtAChalk`, `aNonMemberCannotOpenOne` (`not_member` text, no circle), `aMemberOfAnotherCovenIsNotCalled`, `joiningNeedsTheToken` (single use, 60 s), `aCircleHoldsThirteen` (the fourteenth is refused), `aPlayerIsInOneCircleAtATime`, `aCircleBelongsToItsAnchorAndItsChalk` (Review Focus 2: another caster at the same chalk, the same anchor at another chalk: `CircleLink.current` is empty for them), `aCircleClosesOnATypeSwitch`, `aCircleClosesWhenTheAnchorLeavesTheDimension`, `aMemberWhoLeavesTheCovenLeavesTheCircle`, `idleCirclesClose` (clock pinned), `consentIsKeptAcrossARelog`, `theLinkLiteralsAnswerWrongTypeInAPack`, `aCardOpensForTheInvitee` (`HubRequests.of(invitee)` lists one `CIRCLE_CALL` with a token and ticks left).
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupIndexCovenTest'` and `./gradlew runGameTestServer`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement.** `GroupButtons.IDS` becomes `Map.ofEntries` with `link open` -> `coven:link_open`, `link close` -> `coven:link_close`, `link join` -> `coven:link_join`, `link leave` -> `coven:link_leave`, `link consent` -> `coven:consent` (a three-segment selector, one control); the labels map and `GroupLabels` get their keys (the page itself is Task 12, so the controls are registered here and drawn there; `GroupUiCoverageTest` lists them as "drawn in Task 12" until then, with the test that flips it). The `AdminCommands` rows `C1`-`C5` for these literals are added with the others in Task 11.
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 5: Mutants:** C17 a circle keyed by the chalk alone (`aCircleBelongsToItsAnchorAndItsChalk`); C18 invitees from any coven (`aMemberOfAnotherCovenIsNotCalled`); C19 the circle survives a type switch; C20 a token usable twice; C21 a fourteenth member accepted; C22 a leaver stays in the circle.
- [ ] **Step 6: Commit** `feat(covens): circle sessions, calls and consent`.

---

### Task 5: The cost hook, the staged plan and the EP split

**Files:**
- Modify: `parts/species/witch/RiteCost.java`, `RiteEp.java`, `CircleStage.java`, `EvolutionRiteFactory.java`, `MasteryRiteFactory.java` (the target's owed EP), `core/evolution/MasteryRules.java` only if `enoughEp` needs the owed amount passed in.
- Test: `parts/species/gametest/CovenCostGameTests.java`.

**Interfaces:**
- `RiteCost.begin(...)` (start only): after the existing chain gives the altar bill `A` (siphoned power already taken off), if a circle with `anchor == caster` exists for this `(dimension, chalk)`: drop members who are no longer inside the circle, online, alive and at 50% health or more; read the rite's EP total from `RiteEp.priceOf(type, ...)` (0 when the type is not `EpPriced`); run `CirclePlan.plan(...)` with each member's meter (`ChannelMeter.valueAt`), cap, consent and base Max EP; return `altarPays`; remember the result in `Pending` next to the siphoned power. This runs **before** the `base <= 0` early return for the start case (Review Focus 3). `begin(start = false)` never reads a circle.
- `RiteCost.commit()`: after the siphoned power is taken, for each share: `ChannelMeter` spend (store the new value), then `Strain.apply` (Task 6), then mark the circle **live** for the death window (Task 7); `discard()` drops the plan.
- `RiteEp.charge(target, total)`: with a staged plan, subtract each member's EP share (`EnergyHelper.setBaseMaxEP` inside `RiteEp` only), the target pays nothing of the rite's EP unless it is a member (then its share); `owedBy(target, total)`: the target's share (0 if outside the plan). The two factories' EP checks use `owedBy`: Tensura's own skill price stays the target's (it is charged by the learn, never shared).

- [ ] **Step 1: Write the failing tests** (`CovenCostGameTests`; direct calls of `RiteCost.begin/commit/discard` like `SkillRuleGameTests`, plus one real chalk):
  - `aSoloCastIsTodaysBill` (no circle: the bill equals the Task 0 baseline for witch, Emissary, skills, lunar mood and siphoned power, byte for byte).
  - `aLinkedRiteCutsTheAltarBillToTheAltarShare` (the 6000/4500 and 20000/15000 rows of the worked table).
  - `membersPayOnCommitOnly` (before `commit` every meter and every base Max EP is unchanged; after, the shares are gone).
  - `anAltarThatCannotPayChargesNobody` (Review Focus 1, with a real chalk and an empty altar: `discard` ran, meters and EP unchanged).
  - `aRiteThatCancelsItselfChargesNobody` (a wrong-target rite: `cancel()` -> `discard`).
  - `aZeroPowerRiteStillSplitsItsEp` (Review Focus 3: a Mastery rite type at power 0 through `begin(base = 0)`).
  - `perTickPowerIsNeverSplit` (Review Focus 4: `begin(start = false)` with a circle open returns the witch-discounted bill and charges nobody).
  - `someoneElseUsingTheChalkIsASoloCast`.
  - `aMemberWhoWalkedOutIsDroppedAndTold`, `aMemberAtLowHealthIsDropped`, `anOfflineMemberIsDropped`.
  - `aTargetOutsideTheCirclePaysNoRiteEp` and `aTargetInsideTheCirclePaysItsShare` (Review Focus 5, a Mastery Unique rite at N = 5; Tensura's price still comes off the target in both).
  - `theEpFloorDropsAMemberAndTheSharesAreRecomputed`.
  - `aRealChalkStartsALinkedRite` (end to end with Enchanted's own `use` and `serverTick`).
  - the existing `SkillRuleGameTests`, `CelestialsRiteGameTests` and the 19a tests **unchanged**.
- [ ] **Step 2: Run** `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** Keep `RiteCost`'s public method signatures; `Pending` gains the plan; every refusal path of the hook returns today's bill (a thrown exception in the circle code is caught, logged once, and the rite is a solo cast: a rite must never break because of the link).
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS; the baseline counts plus the new tests.
- [ ] **Step 5: Mutants:** C23 members charged in `begin` (`membersPayOnCommitOnly`); C24 `discard` does not drop the plan (`anAltarThatCannotPayChargesNobody`); C25 the early `base <= 0` return before staging (`aZeroPowerRiteStillSplitsItsEp`); C26 `begin(start = false)` reads the circle (`perTickPowerIsNeverSplit`); C27 a non-anchor caster uses the circle (`someoneElseUsingTheChalkIsASoloCast`); C28 the target always pays the full EP (`aTargetOutsideTheCirclePaysNoRiteEp`); C29 Tensura's price shared among the members.
- [ ] **Step 6: Commit** `feat(covens): the circle plan rides the rite cost hook`.

---

### Task 6: The channel meter's strain

**Files:** Create `parts/species/group/circle/Strain.java`; Modify `RiteCost.commit`; Test `CovenCostGameTests` (extended; `Strain` touches players and effects, so it has GameTests only, its numbers are already unit-tested in `ChannelMeterTest`).

**Rules (R257):** after a share is paid, a member who went over the meter gets, by the zone of `ChannelMeter`: **nosebleed** = Nausea I for 10 s, Weakness I for 30 s, red dust particles at the face; **damage** = the same plus `damageHp` hit points of magic damage, **never below 1 HP**. The meter ends at 0 after an overdraw. All strain is applied to the members, never to the altar's owner or the target (unless they are members).

- [ ] **Step 1: Write the failing tests:** `aCleanShareOnlyLowersTheMeter`, `aNosebleedShareGivesNauseaAndWeakness`, `aDamageShareHurtsByTheTableAndNeverBelowOneHeart` (a member at 3 HP is left at 1), `damageNeverKillsEvenWithTheLethalOptionOff`, `theMeterIsZeroAfterAnOverdraw`, `theMeterRefillsLazilyAfterTenMinutes` (clock pinned; no tick loop), `aShareIsChargedToTheMemberWhoseConsentAllowedIt` (a `NONE` member never reaches strain: they were dropped at plan time).
- [ ] **Step 2: Run** `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement** with the verified names: `MobEffects.CONFUSION`, `WEAKNESS`, `DamageSources.magic()`, `ServerLevel.sendParticles`. The damage amount is clamped to `health - 1` before `hurt`, so no listener can turn it lethal.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Mutants:** C30 damage may go below 1 HP (`aDamageShareHurtsByTheTable...`); C31 the meter is not zeroed (`theMeterIsZeroAfterAnOverdraw`); C32 nosebleed applied in the clean case; C33 damage applied to a member in the nosebleed zone.
- [ ] **Step 6: Commit** `feat(covens): nosebleed and damage when a share is over the meter`.

---

### Task 7: The window and the anchor's death

**Files:** Create `parts/species/group/circle/LinkWindow.java`; Modify `RiteCost.commit` (opens the window), `GroupsRegistry.java`/`CovenRegistry.java` (listeners); Test `parts/species/gametest/CovenDeathGameTests.java`.

**Rules (R256):** `commit()` opens a **live link**: anchor, members, chalk, start time, a `WeakReference<Rite>` taken with `RiteManager.getRiteAt` right after `start()` (null for an instant rite). It stays live for `link_window_seconds`, or while that rite is still the one running at the chalk. A `LivingDeathEvent` (priority `LOWEST`, `receiveCanceled = false`, so a cancelled death or a totem is not a death) for the anchor inside the window: every other member that is online and alive takes `link_death_damage_percent` of max health (never below 1 HP), Weakness II and Mining Fatigue I for 5 minutes, their meter goes to 0 and `link_shaken_minutes` bars them from any circle; with `link_death_lethal` they die instead; a still-running rite is stopped through the queue of Task 0 (`ServerTickEvent.Post`). **Not penalties:** the anchor logging out (the live link is released, the rite goes on), a member dying (that member leaves the link, nobody else is touched), a death after the window, a restart (links are in memory; nothing owed survives).

- [ ] **Step 1: Write the failing tests** (`createMortal` players; time pinned; no tick loops):
  - `theAnchorsDeathHurtsEveryLinkedMember` (60% of max health, Weakness II, Mining Fatigue I, meter 0, shaken).
  - `nobodyDiesWithTheLethalOptionOff` (a member at 5 HP is left at 1).
  - `theLethalOptionKillsTheMembers`.
  - `aDeathAfterTheWindowIsNotAPenalty` (clock pinned past `link_window_seconds`).
  - `theAnchorLoggingOutIsNotAPenaltyAndTheRiteGoesOn`.
  - `aMembersDeathOnlyRemovesThatMember`.
  - `anOfflineOrDeadMemberIsSkipped` and `aMemberInAnotherDimensionIsStillHurt` (they are linked, not near).
  - `aCancelledDeathIsNotAPenalty`.
  - `aShakenMemberCannotJoinACircle` until `link_shaken_minutes` pass.
  - `theAnchorsDeathStopsAPersistentRite` (a rite with `getTickPower() > 0`, or Night Watch once Task 9 exists; the test is written against the first and gains the second there).
  - `aSecondDeathInTheWindowIsNotAPenaltyTwice`.
  - `aSoloCastOpensNoWindow`.
- [ ] **Step 2: Run** `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** The stop is queued, never done inside the event or inside `RiteManager.tick`. If Task 0's `aRunningRiteCanBeStoppedFromOutside` failed, the stop line is dropped here, from R256 and from the test list, and the PR says so.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Mutants:** C34 the penalty skipped (`theAnchorsDeathHurtsEveryLinkedMember`); C35 lethal ignores the option (`nobodyDiesWithTheLethalOptionOff`); C36 the window never closes (`aDeathAfterTheWindowIsNotAPenalty`); C37 logout counts (`theAnchorLoggingOutIsNotAPenalty...`); C38 a member's death hurts the others; C39 `receiveCanceled` true (`aCancelledDeathIsNotAPenalty`); C40 the stop done inside the event.
- [ ] **Step 6: Commit** `feat(covens): the anchor's death in the live window`.

---

### Task 8: Governance: Elders, votes, shunning, the Regent

**Files:**
- Create: `core/group/coven/VoteKind.java`, `VoteTally.java`, `CovenRules.java`; `parts/species/group/coven/CovenGovernance.java`, `CovenCommand.java` (the remaining literals), `parts/species/group/GroupLead.java`; tests `core/group/coven/VoteTallyTest.java`, `CovenRulesTest.java`, `parts/species/gametest/CovenGovernanceGameTests.java`.
- Modify: `GroupIndex.java` (rows `votes`, `bans`), `GroupGate.java` (`canJoin`), `PackCommand.java` (`invite` and `accept` check the ban; `leave`, `kick` and a shun clear ranks and circles), `PackAbsence.java` (`depose` calls `GroupLead.handOver`), `GroupCommand.java` (the coven branch of `challenge`), `GroupSwitcher` hook (open votes cancelled), `SpeciesHub.java` (`GROUP_VOTE` source), `AdminCommands.java` rows.

**Pure rules:**
- `VoteTally.result(int electors, int yes, int no, boolean everyoneVoted, boolean expired)`: `PENDING`, `PASSED`, `FAILED`; passes on a strict majority of the **electorate** (Regent + Elders) once everyone voted or `vote_hours` passed, non-voters count as no; an electorate under 3 means the Regent decides alone for appoint, remove and shun, and no election can be called.
- `CovenRules`: `mayBeElder(daysInCoven, minDays)`; `elderCap(members, perElder)` (at least 1 once there are 3 members, at most one per `members_per_elder`); `mayCallShun(rank, target)` (an Elder or the Regent, never on the Regent); `mayCallElection(callerRank, nomineeRank, lastElectionMillis, cooldownDays)` (an Elder calls, the nominee is an Elder); `banUntil(now, days)` and `banned(until, now)`.

**Runtime (R250, R251, R259, R260, R261):** `/coven elder add|remove <player>` (Regent; with Elders present a vote of kind `ELDER_ADD`/`ELDER_REMOVE`), `/coven shun <player>` (a `SHUN` vote), `/coven vote yes|no`, and the coven route of `challenge <leader>` (a `REGENT` vote naming an Elder nominee). One open vote per kind per coven; votes are rows in `GroupIndex.votes` (`group, kind, subject, caller, opened, ballots`), survive restarts, die after 7 days, and are **cancelled** (row kept, ignored) when the group changes type, when the subject leaves, is kicked, or becomes the Regent. A passed `SHUN`: `PackMembers.remove` with the reason "shunned", ranks cleared, a `type:coven` ban for `shun_days`, notices to the member and the coven; a passed `REGENT`: `GroupLead.handOver` (promote, old Regent becomes an Elder, `PartyLink.leadershipChanged`, notices), one election per `election_cooldown_days`; a passed `ELDER_ADD`: `GroupIndex.setRank(member, ELDER)`. `GroupLead.handOver(server, oldLeader, newLeader, oldRank)` is the body `PackAbsence.depose` already has for CLAN and COVEN, extracted so the absence rule, this election and 20c's Game of Kings share one path (the absence tests run unchanged). Rows `C6` `group elder add <player>`, `C7` `group elder remove <player>`, `C8` `group shun <player>`, `C9` `group vote yes` (REQUESTS), `C10` `group vote no` (REQUESTS); the coven `challenge` uses the existing row `P8`. Controls: `elder add` -> `coven:elder_add`, `elder remove` -> `coven:elder_remove`, `shun` -> `coven:shun`, `vote yes` -> `coven:vote_yes`, `vote no` -> `coven:vote_no`, `challenge` stays `pack:challenge` (drawn as "Call for a new Regent" with an Elder picker on a coven page).

- [ ] **Step 1: Write the failing tests.**
  - `VoteTallyTest`: `aStrictMajorityPasses`, `aTieFails`, `nonVotersCountAsNoAfterTheTime`, `itWaitsUntilEveryoneVotedOrTheTimeIsUp`, `anElectorateOfTwoIsTheRegentAlone`, `noElectionWithAnElectorateUnderThree`, `anExpiredEmptyVoteFails`.
  - `CovenRulesTest`: `aCandidateNeedsTheDays`, `theElderCapFollowsTheMembers` (2 members 0, 3 members 1, 8 members 2), `theRegentCannotBeShunned`, `onlyAnElderOrTheRegentCallsAShun`, `theElectionCooldown`, `banArithmetic`.
  - `CovenGovernanceGameTests`: `theRegentAppointsTheFirstElderAlone`, `withEldersAMajorityIsNeeded`, `anElderNeedsTheDaysInTheCoven`, `aVoteSurvivesARestart` (save and load the index), `aDoubleBallotChangesNothing`, `aVoteWhoseSubjectLeftIsCancelled`, `aTypeSwitchCancelsTheOpenVotesAndKeepsTheElderRanks` (Review Focus 8; ranks come back on the switch back), `aShunRemovesTheMemberAndBarsEveryCoven`, `aShunnedWitchCannotJoinAnyCoven` (Review Focus 9: invite refused `target_shunned`, an old token refused at `accept`, free again after `shun_days`), `aShunnedWitchKeepsTheirMagic` (species and skills untouched), `theRegentCallIsRoutedFromChallenge` (and a pack's `challenge` is still the duel), `aPassedElectionMovesTheLeadAndKeepsTheOldRegentAsAnElder`, `oneElectionPerCooldown`, `theAbsenceHeirIsTheLongestElder` (20a's rule still holds, now through `GroupLead`), `aGroupVoteCardOpensForEachElectorWhoHasNotVoted`.
- [ ] **Step 2: Run** `./gradlew test --tests '*VoteTallyTest' --tests '*CovenRulesTest'` and `./gradlew runGameTestServer`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement.** Persist votes and bans through `GroupIndex` (every row in its own try/catch, as the other rows); the 200-tick sweep (`GroupSwitcher.sweep`) resolves expired votes and prunes dead ones; notices through `PackIndex.addNotice` so an offline member hears.
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS; the existing `GroupAbsenceGameTests` and pack tests unchanged.
- [ ] **Step 5: Mutants:** C41 majority off by one (`aTieFails`); C42 non-voters ignored (`nonVotersCountAsNoAfterTheTime`); C43 a shun that bars one coven only (`aShunnedWitchCannotJoinAnyCoven`); C44 the shunned member loses their skills; C45 the Regent can be shunned (`theRegentCannotBeShunned`); C46 an electorate of two needs a vote; C47 a switch deletes the Elder ranks (`aTypeSwitchCancelsTheOpenVotes...`); C48 the election cooldown ignored; C49 the old Regent dropped to a plain member; C50 the ban not checked at `accept`.
- [ ] **Step 6: Commit** `feat(covens): Elders, votes, shunning and the Regent`.

---

### Task 9: Potency, the three grand rites and the rite data

**Files:**
- Create: `core/group/coven/GrandRites.java`; `parts/species/group/circle/CovenPotency.java`; `parts/species/rite/GrandGate.java`, `FeteRiteFactory.java`, `NightWatchRiteFactory.java`, `HarvestRiteFactory.java`; the three rite JSONs and their book data; test `core/group/coven/GrandRitesTest.java`, `parts/species/gametest/CovenRiteGameTests.java`.
- Modify: `SpeciesPart.java` (three registrations), `LinkingRiteFactory.java` (potency), `core/rite/RiteUnlockTable.java` (+ `RiteUnlockTableTest`), `GroupIndex.java` (row `cooldowns`), `en_us.json`.

**Pure rules (`GrandRites`):** the table `id -> tier -> default min circle`: `fete_des_benedictions` 1 (3), `night_watch` 2 (7), `great_harvest` 3 (13); the minimum is `Tiers` of the config (`tier1_members` and so on), a rite file's own `min_circle` may only raise it; `cooldownUntil(now, hours)`.

**The shared gate (`GrandGate`, R255, R268):** called first in each factory's `onStart`: `grand_rites_enabled`; a coven (the caster is in a group of type `COVEN`); `CircleQuery`/`CircleStage.size() >= min_circle` (the staged plan, after the drops); the coven's cooldown for this rite (`GroupIndex.cooldowns`, a coven-wide `grand_rite_cooldown_hours`, stamped only on success). A refusal is one line to the performer (`rite.grand.circle`, `rite.grand.cooldown`, `rite.grand.off`) and `cancel()` (so the ingredients come back and no member pays). Nothing is said about anyone but the caster's own coven.

**The three rites (R264-R266; effects chosen here, numbers are config or data; owner answer 2 keeps them):**
- **Fete des Benedictions** (tier 1, medium circle, `ritual_chalk`, power 12 000, items 4 gold ingots + 4 emeralds + 2 honey bottles): every player within `grand_rite_radius` x potency (cap 48) of the chalk gets Luck I and Regeneration I for `fete_minutes` x potency minutes. Outsiders benefit: that is the lore (the community gives gifts, the coven blesses).
- **Night Watch** (tier 2, large circle, `nether_chalk`, `time` night only, power 20 000, items 4 lanterns + 4 iron ingots + 1 bell): a persistent rite that runs until dawn; every 40 ticks monsters within the radius get Glowing and Slowness I for 5 s. The radius is fixed at start from the staged potency and saved with the rite. The anchor's death window lasts as long as it runs (the one case where R256's "while it runs" matters).
- **Great Harvest** (tier 3, large circle, `nether_chalk`, power 36 000, items 16 each of wheat, carrots, potatoes and beetroots): the linked members split `harvest_xp_levels` of experience **levels** (the same `CirclePlan.split`; a member who cannot pay their share refuses the rite before anyone is charged), then every crop within the radius gets one bonemeal step through `BonemealableBlock.isValidBonemealTarget` and `performBonemeal` (three passes), touching at most `harvest_max_blocks` blocks. It never kills and never takes a mob.
- `CovenPotency.scale(double base)` reads the staged plan's N (1 outside a plan) through `Potency.of`; the Linking Spell's `link_seconds` goes through it in `LinkingRiteFactory` (a solo cast is unchanged).

- [ ] **Step 1: Write the failing tests.**
  - `GrandRitesTest`: `theThreeTiersAreFixed`, `aFilesMinCircleOnlyRaises`, `cooldownArithmetic`.
  - `CovenRiteGameTests`: `aSoloWitchCannotCastAGrandRite` (`rite.grand.circle`, items back, nobody charged), `aCircleOfThreeCastsTheFete` (nearby players blessed, an outsider too, a player outside the radius not), `theFeteLengthFollowsPotency`, `aCircleOfSixCannotCastNightWatchButSevenCan`, `nightWatchSlowsAndGlowsMonstersUntilDawn` (the rite is driven by calling its `onTick` through `RiteManager`, not by waiting), `nightWatchSavesItsRadius` (save and load the rite), `theAnchorsDeathStopsNightWatch`, `aCircleOfTwelveCannotCastGreatHarvest`, `greatHarvestGrowsCropsAndChargesTheLevels` (the levels add up to `harvest_xp_levels`), `aMemberWithTooFewLevelsRefusesTheHarvestBeforeAnyCharge`, `greatHarvestStopsAtTheBlockCap` (Review Focus 11), `greatHarvestNeverKills` (no entity is hurt or removed), `theCooldownIsPerCovenAndPerRite`, `aFailedCastDoesNotStartTheCooldown`, `theGrandRitesSwitchClosesAllThree`, `theLinkingSpellScalesWithPotency` (N = 3: 60 s -> 81.6 s, rounded; solo 60 s), `anExistingRiteIsNotGrand` (a sample of `ward_of_daylight`, `rite_of_severance`, `evolve_great_sage` start solo as before), plus the book and unlock tests of the repo for the three new rites (kind-gated to the witch kinds, like `rite_of_the_door_ward`).
- [ ] **Step 2: Run** `./gradlew test --tests '*GrandRitesTest' --tests '*RiteUnlock*' --tests '*RitesBookTest'` and `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** Rite JSON conditions: `part_enabled species`; each factory's codec has the optional `min_circle` field. Follow `RitesBookTest` / `RiteUnlocksDataTest` to the letter for the book and advancement data (they fail by name). Verified Minecraft API used: `BonemealableBlock`, `Player.giveExperienceLevels`, `MobEffects` names (see "What was verified").
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 5: Mutants:** C51 tier thresholds off by one (`aCircleOfSixCannotCastNightWatchButSevenCan`); C52 the gate reads the open circle instead of the staged plan (`aCircleShortOfSevenAfterTheEpFloor...` of Task 10 and the drop test here); C53 the cooldown stamped on a failed cast; C54 the harvest ignores the block cap; C55 the harvest takes a mob; C56 potency not applied to the Fete; C57 an existing rite given a gate; C58 a levels share charged before the refusal check.
- [ ] **Step 6: Commit** `feat(covens): potency and the three grand rites`.

---

### Task 10: The two ring rites and Mastery's Ultimate circle

**Files:**
- Create: `core/group/coven/RingRules.java`; `parts/artifacts/rite/CovenForgeRiteFactory.java`; the two rite JSONs (`coven_moon_ring.json`, `coven_sun_ring.json`) and their book data; tests `core/group/coven/RingRulesTest.java`, `parts/species/gametest/CovenRingGameTests.java`.
- Modify: `ArtifactsPart.java` (register `coven_forge`), `core/evolution/MasteryRules.java` (+ reason `CIRCLE`), `MasteryRiteFactory.java` (the gate), `core/rite/RiteUnlockTable.java`, `en_us.json`, `docs/parts/artifacts.md`.

**Pure rules (`RingRules`):** `moonReceiverOk(boolean werewolfFaction, boolean inAPack)`; `sunReceiverOk(boolean vampireFaction, boolean dayWalkerOfAClan, boolean dayWalkerOnly)`; `circleOk(int size, int minCircle)`.

**The forge rite (R262, R263):** `CovenForgeRiteFactory` (`type supernaturalbloodlines:coven_forge`, fields `output`, `faction`, optional `min_circle` default 3, `needs_pack`): reads the circle through `CircleQuery` (no species import), takes **no ring** (the file's items are the materials), and drops the output **bound to the player on the circle's centre** (else the caster) with `ArtifactItems.BOUND_TO`, exactly as `BindItemRiteFactory` does; refuses with the existing wording keys when the faction is wrong. `coven_moon_ring`: 2 silver ingots + 1 amethyst shard (half the silver of the 4 the ring recipe takes), power 6000, `faction werewolf`, `needs_pack true`, conditions `part_enabled artifacts`, `part_enabled species`, `mod_loaded werewolves`. `coven_sun_ring`: the items of `ward_of_daylight` (2 sunflowers, 4 glowstone dust) plus 2 gold ingots in place of the ring crafting, **power 3000** (half of `ward_of_daylight`'s 6000), `faction vampire`, the Day Walker switch of `sun_ring_day_walker_only`. Both need `CircleQuery.size >= 3` (tier 1) and their config switches. Every plain recipe and bind is untouched.

**Mastery's Ultimate (R267):** in `MasteryRite.onStart`, for `riteTier == ULTIMATE` and `mastery_ultimate_circle > 0` and `link_enabled`, before the target is read and before the offering is taken: if the staged plan has fewer than `mastery_ultimate_circle` members the performer gets the one generic line `refusal.mastery.circle` and the rite cancels (items back). It says nothing about the target (E5). With `link_enabled` off, or `covens_enabled` off, the gate is waived (Review Focus 12).

- [ ] **Step 1: Write the failing tests.**
  - `RingRulesTest`: `aMoonRingNeedsAWerewolfInAPack`, `aSunRingNeedsAVampire`, `theDayWalkerSwitchOnNeedsARank`, `theCircleMinimum`.
  - `CovenRingGameTests`: `aCircleOfThreeForgesAMoonRingBoundToTheReceiver` (bound UUID is the receiver's, half the silver taken, one ring), `aCircleOfTwoCannotForgeIt` (items back), `aReceiverOutsideAPackIsRefused`, `aSunRingBindsAtHalfPower` (`RiteCost.begin` with the file's 3000), `aPlainSunRingBindStillCosts6000AndWorksAlone`, `aPlainMoonRingRecipeAndBindUnchanged`, `theRingSwitchesCloseThem`, `theDayWalkerSwitchMakesItNeedARank` (a rank row written by the test), `noNewItemIsRegistered` (the item registry of the mod is unchanged), `theForgeFactoryLoadsWithSpeciesOff` (run 4: the artifacts part alone, no species class loaded).
  - Mastery: `aCircleOfSevenCanDoUltimateMastery` (EP 2,000,000 split, each member's base Max EP down by their share, the target's Mastery done), `aCircleOfSixIsRefusedGenerically` and `aCircleShortOfSevenAfterTheEpFloorIsRefusedGenerically` (Review Focus 6; the performer's text is identical for a good and a bad target), `soloUltimateMasteryIsRefused`, `theLowerTiersNeedNoCircle` (common, extra, unique unchanged), `theGateIsWaivedWhenTheLinkIsOff` (Review Focus 12), `theGateIsWaivedWhenTheValueIsZero`, `aRefusedUltimateGivesTheItemsBackAndChargesNobody`; the existing `MasteryGameTests` Ultimate rows are updated to run inside a circle of 7 (the one existing test change in the plan, listed in the PR).
- [ ] **Step 2: Run** `./gradlew test --tests '*RingRulesTest' --tests '*RiteUnlock*' --tests '*RitesBookTest'` and `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** The factory is registered by `ArtifactsPart`; `SpeciesPart` installs the `CircleQuery` source; with species off the seam answers 0 and the forge rites refuse.
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 5: Mutants:** C59 the forged ring bound to the anchor instead of the receiver (`aCircleOfThreeForgesAMoonRingBoundToTheReceiver`); C60 the full silver taken; C61 half power not applied (`aSunRingBindsAtHalfPower`); C62 the plain bind changed; C63 the circle minimum off by one; C64 the Ultimate gate on the lower tiers (`theLowerTiersNeedNoCircle`); C65 the gate reads the open circle instead of the staged one; C66 the refusal text names the target; C67 the waiver missing (`theGateIsWaivedWhenTheLinkIsOff`).
- [ ] **Step 6: Commit** `feat(covens): the Moon and Sun ring rites and the circle of seven for Ultimate Mastery`.

---

### Task 11: The coven view payload, the protocol and the command rows

**Files:**
- Create: `core/group/coven/CovenView.java`, `CovenWire.java`; `parts/species/group/coven/CovenViews.java`; `shared/supernatural/HubCoven.java`; `parts/supernatural/net/CovenViewPayload.java`; `parts/supernatural/client/CovenClient.java`; test `parts/supernatural/net/CovenViewWireTest.java`.
- Modify: `LadderNet.java` (register, send next to `GroupViewPayload`, `PROTOCOL`), `SnapshotIdsWireTest.java`, `shared/supernatural/HubRequests.java` (done in Task 4), `AdminCommands.java` (rows `C1`-`C10`), `AdminCommandsTest` (row counts), `UiReferencesTest` / `ReplyScopeTest` (if they need a line per row), `CommandCoverageGameTests.java`.

**`CovenView` (only the viewer's own, never another person's):** `tier` (0-3), `size`, `circle` (`NONE`, `ANCHOR`, `JOINED`, `CALLED`), `anchorName`, `circleNames` (at most 13, names of the viewer's own coven only), `meter`, `meterCap`, `regenSeconds`, `consent`, `sharePower` (an example from the highest listed rite and the circle; no per-rite preview, which would load chunks), `shunnedDaysLeft`, an optional open vote (`kind`, `subjectName`, `yes`, `no`, `electors`, `mine`, `hoursLeft`: counts only, never who voted what). Bounded by `CovenWire` (13 names, 32 characters).

- [ ] **Step 1: Write the failing tests.** `CovenViewWireTest`: `roundTrip` (a circle of 3 with a vote), `anEmptyViewRoundTrips`, `maxCircleRoundTrips` (13 names), `aCountOverTheCapIsRejected`, `aLongNameIsRejected`, `theViewNeverCarriesAnotherMembersMeterConsentOrBallot` (reflection over the record components, plus a structural check that `CovenViews.of(p)` for two members of one coven gives each their own meter), `onlyTheViewersOwnCovenIsCarried`, `aNonWitchViewerStillGetsTheirCard` (a human member: the half-capacity meter); `SnapshotIdsWireTest.GOLDEN_PROTOCOL` = main's + 1 (`"16"`); `AdminCommandsTest`: the `group` root has 10 `C` rows and the total is 72 (62 + 10); `CommandCoverageGameTests`: `group` is added to `ROOTS` and every shape that is the shared tree's (`invite`, `accept`, `leave`, `kick`, `ally ...`, `info`, `challenge ...`) is matched to its `pack` twin (the `SHARED_PACK_ROWS` ids) so nothing in the shared tree needs a second row.
- [ ] **Step 2: Run** `./gradlew test --tests '*CovenViewWireTest' --tests '*SnapshotIdsWireTest' --tests '*AdminCommandsTest' --tests '*UiReferencesTest'` and `./gradlew runGameTestServer`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement.** The payload rides the optional registrar (`playToClient`, the client handler named only on the client like its neighbours), is sent where `GroupViewPayload` is sent, and is pushed again through `HubRequests.changed(member)` whenever a circle, a call, a consent, a meter or a vote changes for that member. Say the number change in the CHANGELOG ("the hub channel changed: update client and server together").
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 5: Mutants:** C68 the view carries another member's meter (`theViewNeverCarriesAnotherMembers...`); C69 the codec accepts a count over the cap (`aCountOverTheCapIsRejected`); C70 a row without its command (`CommandCoverageGameTests`); C71 `PROTOCOL` unchanged (`SnapshotIdsWireTest`); C72 a vote that shows who voted.
- [ ] **Step 6: Commit** `feat(covens): the coven view payload, the new requests and the command rows`.

---

### Task 12: The hub (UI-first), three themes, the Profile and Home

**Files:**
- Create: `client/hub/page/CovenCards.java`, `CovenLabels.java`; test additions to `GroupUiCoverageTest` and `CovenTextTest`.
- Modify: `PackPage.java`, `BookPackPage.java`, `ProfilePage.java` (+ the profile action rows), `HomePage.java` (`answers`: `CIRCLE_CALL` -> `C3`, `GROUP_VOTE` -> yes `C9` and no `C10`, no token on the vote), `GroupButtons.java`, `GroupLabels.java`, `en_us.json`.

The page for `type == COVEN` (drawn from `GroupClient.view()` and `CovenClient.view()`; a pack's page stays pixel-identical):
- **Header:** "Coven", the viewer's rank (Regent / Elder / Member), the **tier badge** from the group's size, "since".
- **Members:** the Regent, the Elders (a mark on the row), members, allies; an Elder or Regent opens a member's detail with **Appoint Elder / Remove Elder** (Regent) and **Call a shunning vote** (confirm, two presses).
- **Circle card:** Open circle / Close (anchor); Join the circle (on a `CALLED` card, also a Home card with the countdown); Leave; the three-segment **consent** selector (Strictly my meter / Allow a nosebleed / Allow pain); the **meter bar** with the refill time; the **share preview** (power and EP; the worst case when no rite lies on the circle yet; "lay the offering to see your share" otherwise); the circle's names.
- **Votes card:** the open vote with counts and time left, **Yes / No** for an elector who has not voted; **Call for a new Regent** (an Elder picker) for an Elder.
- **Ring art:** the circle-of-candles role set is the art task of the UI kit; until then the existing ring is drawn with the coven roles tinted and flagged "placeholder art" in the PR (as 20a does).
- **Profile:** a coven member's Profile shows the Elder and shun actions exactly when the group page would (the viewer's type and rank come from `GroupClient.view()`, so no new payload).

- [ ] **Step 1: Write the failing tests.** `GroupUiCoverageTest`: every literal of the `/group` tree is a key of `GroupButtons.IDS` or in the "no button" set, no id is used twice, every id has a lang key **and is drawn by a page** (the 20a check; Task 4's "drawn in Task 12" exemption is removed here); `CovenTextTest` (data test over `en_us.json`): every coven lang key the plan names exists, none is a copy of the pack text containing "pack"; `PackUiTest`, `PackPanelTest`, `PackRingTest` run **unchanged**.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupUiCoverageTest' --tests '*CovenTextTest' --tests '*PackUiTest' --tests '*PackPanelTest' --tests '*PackRingTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Parametrise, do not fork: the pages already take a `GroupView`; the coven cards are drawn only for `COVEN`.
- [ ] **Step 4: Hidden hub shots** (`docs/safe-screenshots.md`: a **copied world**, wait while the owner's own Minecraft window is open, no live account name): the Task 0 coven page again in the three themes (pixel-identical apart from the new cards), then the circle card in each state (none, anchor, joined, called) and the votes card in each theme, the Home request cards (`CIRCLE_CALL`, `GROUP_VOTE`), a Profile with the Elder and shun actions, a pack page (unchanged). Save outside the repo, attach to the PR.
- [ ] **Step 5: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 6: Mutants:** C73 a coven literal with no button (`GroupUiCoverageTest`); C74 a coven card drawn on a pack page; C75 an id used twice; C76 the consent selector sends an unknown level; C77 a Home card with no token for `CIRCLE_CALL`.
- [ ] **Step 7: Commit** `feat(covens): the circle, votes and Elder cards on the Group page`.

---

### Task 13: Rules, docs, changelog, the matrix and the mutation pass

**Files:** Modify `docs/superpowers/specs/SPECIES-RULES.md`, the spec (the corrections above), the 20a outline pointer, `docs/parts/species.md`, `docs/parts/artifacts.md`, `docs/mixins.md`, `CHANGELOG.md`, `wiki/Groups.md`, `wiki/Rites.md`, `wiki/Configuration.md`, `wiki/Rules-Reference.md`, `wiki/Witches-And-Siphoners.md`.

- [ ] **Step 1:** `SPECIES-RULES.md`: "Covens (plan 20b, owner 2026-11-23)" with **R250 to R268** in rule form after R249, and a pointer from the Rite of Mastery rule ("Ultimate needs a circle of 7: R267").
- [ ] **Step 2:** `docs/parts/species.md`: a "Covens" checklist (ticked), the settings table (30 keys), "what vanishes with the switch" (`link_enabled` off: no new circle, the staged plan is never built, Mastery's Ultimate needs no circle, nothing paid is refunded; `covens_enabled` off: no new coven; existing rows kept), the hooks read in Task 0. `docs/parts/artifacts.md`: the `coven_forge` factory and the seam. `docs/mixins.md`: one sentence.
- [ ] **Step 3:** `CHANGELOG.md`, with **"Rolling back"**: switch `link_enabled` off; shares already paid are gone (a debt is never saved); the rows `channel`, `consent`, `votes`, `bans`, `cooldowns` stay in `supernaturalbloodlines_groups.dat` and a 20a jar keeps them as unknown keys; an Elder rank row stays dormant; the hub channel number changed; the five new rite files vanish with their jar.
- [ ] **Step 4:** `./gradlew test` (all unit tests). Expected: PASS.
- [ ] **Step 5: The matrix**, one run at a time, `--max-workers=2`, through `gt-slot.ps1`, each `./gradlew runGameTestServer` plus: 1 none; 2 `-PnoWerewolves -PnoCurios` (**the run that matters most here**: a coven links and casts with no Werewolves; the Moon Ring rite is absent, nothing throws); 3 `-PnoLinkedMods`; 4 `-PpartsOff=species` (groups, circles and grand rites vanish; the artifacts part still loads and its forge rites refuse; nothing throws); 5 `-PpartsOff=artifacts` (grand rites present, both ring rites absent); 6 `-PpartsOff=speciesTensura`; 7 `-PpartsOff=supernatural` (the payloads are not registered; circles and rites still work); 8 `-PpartsOff=supernaturalTensura`; 9 `-PpartsOff=hud`; 10 `-PpartsOff=celestials`; 11 `-PpartsOff=bosses` (the Linking Spell's potency is simply absent); 12 `-PpartsOff=species,artifacts`. Record the counts: they equal the Task 0 baseline plus the new tests. Run 6 and any run with the evolution rites part off prove Mastery's Ultimate gate is not reachable code.
- [ ] **Step 6: The mutation pass.** Re-run every mutant of Tasks 1 to 12 (C1-C77): apply, run the named test, confirm FAIL, revert; record the table in the PR (`# | mutant | test that failed`). A mutant that survives is a missing test: write it first.
- [ ] **Step 7: Open the PR** with the matrix counts, the mutation table, the Task 0 findings, the before/after shots, the list of changed existing tests (the Ultimate Mastery rows), and the owner answers below.

---

## Self-review

- **Spec coverage:** 6.1 (Elders, votes, shunning, Regent) Task 8; 6.2 (circle, plan, meter, potency, tiers, death) Tasks 3 to 7; 6.3 and 8 (rings) Task 10; grand rites (Q2) Task 9; 9 (config) Task 2; 10 (hub) Task 12; 11 (protocol) Task 11; 13 items 1 and 2 Task 0. The owner's exception to Q2 (Mastery Ultimate) is Task 10; its absence from the spec is fixed in "Spec corrections".
- **Types:** `CirclePlan.Result`, `Share`, `Dropped` (Task 3) are consumed by `RiteCost` and `RiteEp` (Task 5) and `Strain` (Task 6); `CircleStage` is read by `GrandGate` (Task 9), `CovenPotency` (Task 9), `CovenForgeRiteFactory` through `CircleQuery` (Task 10) and `MasteryRite` (Task 10); `LinkWindow` (Task 7) is opened by `RiteCost.commit` (Task 5); `GroupLead.handOver` (Task 8) is also what `PackAbsence.depose` calls; `GroupButtons.IDS` entries of Tasks 4 and 8 are drawn in Task 12.
- **Proportion:** bodies are left to the signatures and the tests; the decisions the builder cannot make alone are the numbers, the order of the cost hook, and the privacy of the view, and those are here.
- **Not done here, on purpose:** the celestial battery, the consecration bank, the forbidden rite, Merge succession, Fete de Cadeau (the Fete here is the Benedictions), the cosmetic "backs turned" shunning walk, the circle-of-candles ring art, an admin action to lift a shunning (the ban is `shun_days` long and a config row), the Day Walker rank itself (20c), per-tick power splitting (a stated shortcut: it stays on the altars; upgrade only if a long-running rite shows the need).

## Owner answers, 2026-11-24

The owner accepted all four recommended picks. They are decided, not open:

1. **The half-power Sun Ring works for any vampire now.** `sun_ring_day_walker_only` defaults to `false`; plan 20c flips the default to `true` once the Day Walker rank exists.
2. **The three grand rites keep the effects chosen in Task 9** (Fete des Benedictions: Luck I and Regeneration I near the circle; Night Watch: monsters slowed and outlined until dawn; Great Harvest: crops bonemealed in a radius for a split cost in XP levels, never a kill). Every number is an Admin row and the items are data.
3. **When the target is not in the circle, the circle pays all of the rite's EP** and the target pays only Tensura's own price for the skill (Task 5).
4. **"A coven of 7" for Ultimate Mastery means 7 linked members standing in the circle when the rite starts.** The requirement is waived while `link_enabled` or `covens_enabled` is off (R267, Task 10).

There are no open questions left in this plan.
