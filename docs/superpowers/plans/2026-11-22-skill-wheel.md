# Skill Wheel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One onion wheel (mods, then groups, then powers) that lists every power the player has from our mod, Vampirism, Werewolves, Tensura (skills, spells and battlewill arts, plus addons), Infinity Stones and Great Sage, drawn in our three themes, with every other mod's skill key covered.

**Architecture:** Pure rules in `core/wheel` (JUnit). A server catalogue and use/mode path in the `hud` part that reuses `ActionService`, Tensura's own skill packets and Tensura's own preset path. A client wheel screen on the art kit, plus two optional client compats (Great Sage, Infinity Stones) by reflection. Five phases, five PRs, in the spec's order. Points the owner added on 2026-11-22 are marked "(owner, 2026-11-22)".

**Tech Stack:** NeoForge 1.21.1, Tensura 2.0.1.5 + ManasCore 4.0.0.2, Vampirism 1.10.13, Werewolves 2.0.3.4, Great Sage 0.0.7.1, Infinity Stones 8.0.0-alpha.6, JUnit 5, NeoForge GameTests, Figma file `USFBf74c4I2BWDyfmRxFZO`.

**Spec:** `docs/superpowers/specs/2026-11-22-skill-wheel-design.md`. Overlap research (owner's pair answers pending): `docs/superpowers/research/skill-overlaps.md` (groups M01 to M12, section 6 risks). Executors read both.

## Global Constraints

- **Server trust:** "the wheel only asks. Every use goes through the existing server paths (`ActionService` for our and Vampirism/Werewolves actions, Tensura's own skill packets for Tensura skills, his mod's own path for stones). The server re-checks that the player has and may use the power."
- **Network:** "New or changed payloads bump the network version, with a CHANGELOG note ("update server and clients together")." The hud registrar is `HudNet.VERSION` (now `"1"`): W1 makes it `"2"`, W2 `"3"`.
- **Existing rules hold:** "downed, snapped and dying players cannot use anything; targeted powers use the 18d target pipeline; no refusal may reveal what a target is."
- **Performance:** "the entry list is built when the wheel opens and when the player's powers change, never per frame; text is measured once and cached."
- **Accessibility:** "keyboard (arrows/Tab move, Enter uses, a slot key assigns), screen-reader narration of category, power and state, 4.5:1 text contrast, state never shown by colour alone, 'Reduce motion' turns off wheel animation."
- **UI-first:** "every new setting is reachable in the hub (Settings for client options, Admin for server options)". A server key needs an `AdminEntries` row, a client key a `PlayerSettings` row; `AdminCoverageTest` fails the build otherwise.
- **Parts:** "the wheel lives in the `hud` part; each mod's category is gated on that mod (and our part for it) being loaded. With the `hud` part off, nothing is covered and other mods keep their own keys." `hud` has no required mods; Tensura, Vampirism, Werewolves are reached only after `ModList.isLoaded`.
- **Mixins:** budget 3 per part, each `require = 0`, listed in `docs/mixins.md`. `hud` uses **2 of 3**. This plan adds **none** (it extends `NextPreset.matches`, which the existing mixin (1) already calls). If a task ever needs one, it takes the third and updates `docs/mixins.md`.
- **Onion wheel (owner, 2026-11-22):** inner ring = mods; middle ring = groups of the hovered mod, **only when it has more than one group** (Tensura: Skills, Magic, Battlewill; every other mod has one group); outer ring = the powers of the hovered group, **paging above about 12 entries** (`WheelLayout.PAGE_SIZE = 12`). Alt stays mode-cycling only. **Scroll precedence:** (1) Alt + scroll over a power with modes: mode; (2) plain scroll over a power with levels: level; (3) plain scroll over the outer ring on empty space or a power without levels, in a group with more than one page: page; (4) anywhere else: nothing.
- **Spells and arts are in scope (owner, 2026-11-22).** They are Tensura skills and use the Tensura path.
- **Dedup (owner, 2026-11-22):** whole powers only, never a mode inside a bigger skill; all 13 loose matches stay separate; M09 keeps both; tiers show the highest owned; the main side's cooldown applies to every side when a lasting action ends.
- **The game is not paused** while the wheel is open. No gamepad. No change to any other mod's trees, costs or balance; no new powers.
- **Test matrix, run before every phase PR (12 GameTest runs plus unit tests, memory #1188):**
  1. `./gradlew runGameTestServer`
  2. `./gradlew runGameTestServer -PnoWerewolves -PnoCurios`
  3. `./gradlew runGameTestServer -PnoLinkedMods` (local only)
  4. `./gradlew runGameTestServer -PpartsOff=species`
  5. `./gradlew runGameTestServer -PpartsOff=artifacts`
  6. `./gradlew runGameTestServer -PpartsOff=speciesTensura`
  7. `./gradlew runGameTestServer -PpartsOff=supernatural`
  8. `./gradlew runGameTestServer -PpartsOff=supernaturalTensura`
  9. `./gradlew runGameTestServer -PpartsOff=hud`
  10. `./gradlew runGameTestServer -PpartsOff=celestials`
  11. `./gradlew runGameTestServer -PpartsOff=bosses`
  12. `./gradlew runGameTestServer -PpartsOff=species,artifacts`

  plus `./gradlew test`. CI runs only 7; the other five are local. Every new GameTest passes or **skips cleanly** (not errors) in all 12; a task says where it skips (for example: all wheel Tensura tests skip in 3, all wheel tests skip in 9). Logs of each run go in `C:\Users\caiom\AppData\Local\Temp\opencode\wheel-plan\matrix-<phase>.log` (one line per run, last line `DONE`). A docs-only phase (W3) still runs the matrix on its head; red there means main is red.
- **Hidden shots (memories #1216, #1218, #1122; `docs/safe-screenshots.md`):** only W4 takes game shots, only with `.\gradlew.bat runSafeShots --no-daemon`.
  - A fresh worktree has no world: copy `D:\Development\OFA-TECH\SupernaturalBloodlines-MinecraftMod\run-safeshots\saves\New World` (it has `level.dat`) into `<worktree>\run-safeshots\saves\New World` first, or the client sits at loading forever (a folder with only a `session.lock` is the same failure).
  - **Owner-game rule:** before any client launch run `Get-Process javaw | Where-Object Path -like '*curseforge*'`. If the owner's game is open, do **not** launch a hidden client: use only `-JavaOnly` focus-watch runs of `runGameTestServer`, defer the shots, and say so in the PR. Never launch repeatedly. Main stays clean: no run, no copy, no write there.
  - Never write into the pack folder (`...\OFA - Holy Packs (Client)\mods`); jars used for checks are copies in `C:\Users\caiom\AppData\Local\Temp\opencode\wheel-plan\jars`.
- **Every phase:** its own branch `feature/wheel-w<n>-<name>`, one PR into main, a `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/hud.md` ticked, mutation targets run (each one: break the line, see the named test go red, restore).
- **Numbers live in config**, each with an Admin or Settings row and a line in `docs/balance.md`.

## Review Focus

The spec is silent on these; each is pinned by a test in the task named in brackets. Most likely first.

1. **A Tensura skill, spell or art that is in no preset slot.** Tensura's activate listener lets it start (with our scope), but `TensuraSkill.getMaxHeldTime` is **20 ticks** for a skill not in the active preset (72000 when slotted, 7200 for staff casting), so a held art or a spell with a cast time over one second is cut off, and a toggle may stop at its next tick; the mixin may also be off (`next_preset_active = false`). The wheel must show the true state (`inst.isToggled()`), never the optimistic one, and must lock the entry ("put it in a slot first") instead of lying. [W1 T6 `unslottedToggleSurvivesFortyTicks`, `unslottedHeldSkillRunsPastTwentyTicks`, `unslottedLongSpellCompletes`, `mixinOffLocksUnslotted`]
2. **A forged or stale payload.** A power the player no longer has (skill forgotten while the wheel is open), a mode index out of range or negative, a flood, a `release` with nothing held, a use while downed, snapped, dying or dead, a held skill when the player dies, logs out or changes dimension. Expected: one refusal line, nothing runs, nothing stays held. [W1 T7]
3. **A "lasting" action in a shared cooldown.** Vampirism and Werewolves lasting actions start their cooldown when they **end** (research section 6, risk 1). Turned off early, ended by time, player dies or relogs mid-action: the group cooldown starts once, at the end, and survives a relog. [W2 T3]
4. **Covered keys must come back, and must not be taken early.** Hud part off, server without the channel, the title screen, a crash after covering, a mod that is not installed (its id is absent). Nothing is covered while no wheel screen exists in the build (W1 to W3), so a player is never left with a hidden key and no way to use it. [W1 T9, W4 T10]
5. **The G hold, tap and release machine.** G held when a screen opens or the window loses focus, release outside the window, a tap while the last power is on cooldown or no longer owned, Alt held when the wheel opens, a held Tensura skill when the mouse button is released over a different entry. Expected: a held skill is always released, a tap never opens a ghost wheel. [W4 T2, T5]

## Verified hooks (javap -p -c against the real jars, 2026-11-22)

Copies of the jars are in `C:\Users\caiom\AppData\Local\Temp\opencode\wheel-plan\jars` (Tensura and Great Sage from the pack, read only; Vampirism and Werewolves from the Gradle cache; ManasCore's skill, network and storage jars unpacked from `META-INF/jars` of `manascore-neoforge-4.0.0.2.jar`).

| Need | Hook (jar and version) | Result |
|---|---|---|
| Activate a Tensura skill by id | `SkillAPI.skillActivationPacket(ResourceLocation, int keyNumber, int mode)` (client) sends `RequestSkillActivationPacket`; its handler calls `SkillStorage.startHoldSkill(ResourceLocation, int, int)` (ManasCore 4.0.0.2). `startHoldSkill(ManasSkillInstance, int, int)` fires `SkillEvents.ACTIVATE_SKILL`, then checks `canActivateSkill`, the mode range, the cooldown, then `onPressed`, `addTickingSkill` | Server can call `startHoldSkill` itself. ManasCore has **no preset check** |
| Does Tensura accept a skill in no preset slot? | Tensura 2.0.1.5 `AbilityHandler.init` registers on `ACTIVATE_SKILL` (lambda 4): for a `TensuraSkill`, `if (!Magic.isStaffCasting(inst, p) && !IAbility.isAbilityInActivePreset(keyNumber, skill, mode)) return EventResult.interruptFalse()`. Also `scroll` (lambda 6: `isAbilityInActivePreset(skill, mode)`) and `NumberKeyPress` (lambda 7: `isAbilityInActivePreset(skill)`) | **No: refused.** Our existing mixin (1) `AbilityStorageNextPresetMixin` hooks all four `isAbilityInActivePreset` overloads and calls `NextPreset.matches`. We extend `matches` with a thread-local scope that only our server path opens. No new mixin |
| Release | `RequestSkillReleasePacket` handler calls `SkillStorage.handleSkillRelease(inst, int, int, boolean)`; no preset listener | Release needs no change |
| **Spells** (owner, 2026-11-22) | Tensura 2.0.1.5 `ability.magic.Magic extends TensuraSkill` (about 190 spell classes in `ability/magic/<element>/`); `Magic$MagicType` = `ASPECTUAL, SPIRITUAL, SUMMONING, MISC`; `isInstantCast(inst, p)`, `getCastingTime(inst, p)`, `isCastingBlocked`, `canActivateSkill` (adds: air supply must be full, no `SILENCE` effect, unless instant cast), `onHeld` applies the casting visual. Spells live in `SkillStorage` like every skill | **Castable server-side exactly like a skill**: `startHoldSkill(inst, key, mode)` then hold for the cast time. `RequestSpellChangePacket(delta, modeChange, hand)` is the staff and hand spell picker, not needed |
| **Arts** (owner, 2026-11-22) | `ability.battlewill.Battlewill extends TensuraSkill` (`getMaxMastery`); `utility.BattlewillArt extends Battlewill` (`onHeld`, `getAuraCost`); melee and projectile arts extend `Battlewill` directly and override `onPressed` (instant) | **Castable like a skill**; kinds vary per class (instant `HeavySlashArt`; hold and release `AirFlightArt`) |
| Held limit when not slotted | `TensuraSkill.getMaxHeldTime(inst, p)`: `72000` if `isInSlot` (= `isAbilityInActivePreset(skill)`), else `7200` if `Magic.isStaffCasting`, else **`20`** | A held art or a long spell started from the wheel with an empty slot dies after one second unless that check is true while it ticks. Settled by the T6 spike. **Feasibility flag** |
| Tensura race ability (owner, 2026-11-22) | ManasCore 4.0.0.2 `RaceAPI.getRaceFrom(p)` returns `Races`: `startHeldAbility(): boolean`, `releaseHeldAbility()`, `interruptHeldAbility()`, `getRace()`; client `RaceAPI.raceAbilityActivationPacket()` / `raceAbilityReleasePacket()` (no id, no preset); `ManasRace.canActivateAbility(inst, p)`; `RaceEvents.ACTIVATE_ABILITY` listeners can refuse. Tensura key `TensuraKeybinds.RACE_ABILITY` (`tensura.keybinding.race_ability`) | **Castable server-side directly**: one entry "Race ability" per player (ref `tensura:race_ability`), held |
| Vampirism vision (owner, 2026-11-22) | Vampirism 1.10.13: key `keys.vampirism.vision` sends `ServerboundSimpleInputEvent.TOGGLE_VAMPIRE_VISION`; server `VampirePlayer.switchVision()` (public, **concrete class, not API**); API: `IVampirePlayer.getActiveVision()`, `activateVision(IVampireVision)`, `IVampireVisionRegistry.getVisions()/getVisionId`. The set of unlocked visions is private (`VisionStatus.unlockedVisions`) | One entry "Vision" (ref `vampirism:vision`), shown while the player is a Vampirism vampire, used by `switchVision()` by method handle like `VampirismBite`; its caption is the active vision. Whether `getActiveVision()` is null for "none" is not verified |
| Where Tensura stores the mode | `AbilitySlot.mode` (per preset slot; `getMode/setMode`); `ManasSkillInstance` has **no mode field** (only `toggled`, `cooldownList` per mode, `getModes()`) | The spec's "the skill's own mode" is the **slot's** mode. For a skill in no slot there is none: we keep our own (`WheelModes`), see Spec points that changed |
| Cycle a mode / assign a slot the Tensura way | `RequestAbilityModeChangePacket.changeModePacket(int slot, int preset, boolean backwards)` + public `changeMode(Player)`; `changeAbilityPacket(int slot, int preset, ResourceLocation skill, int mode)` + public `changeAbility(ServerPlayer)`; `TensuraSkill.nextMode(LivingEntity, ManasSkillInstance, int, boolean)`, `getModeName`, `getModeId`, `canBeSlotted(inst, entity, mode)` | Slotted skill: call Tensura's own `changeMode`. Right-click assign: the client sends Tensura's `changeAbilityPacket` (no payload of ours). `nextMode` is overridden per skill (Infinity's `StoneSkill` skips locked modes); the base body was not read |
| Preset model | `IAbility`: `getActivePreset`, `getPresets`, `getAbilitySlot(preset, slot)`, `isAbilityInPresets(skill[, mode])` | Already used by `TensuraSlots`, `NextPreset`, `PresetPair` |
| Vampirism action API | `IActionHandler`: `getUnlockedActions`, `isActionUnlocked`, `isActionActive(ILastingAction / ResourceLocation)`, `isActionOnCooldown`, `getPercentageForAction`, `toggleAction(action, ctx)`, `deactivateAction`, `resetTimer`. `ILastingAction.getDuration`. `IAction.canUse(player)` returns `PERM` (`ALLOWED, DISABLED, NOT_UNLOCKED, DISALLOWED, COOLDOWN, PERMISSION_DISALLOWED`). `ActivationContext` has only `targetBlock`, `targetEntity` (Vampirism 1.10.13) | `FactionActions` already uses it. **No cooldown setter and no "needs aim" flag** exist: aimed actions are a table of ours |
| Vampirism action events | `ActionEvent.ActionActivatedEvent` (**cancellable**, `get/setCooldown`, `get/setDuration`) and `ActionDeactivatedEvent` (`getRemainingDuration`, `get/setCooldown`), NeoForge bus | Lets W2 refuse a second side from any key and raise the cooldown a lasting action starts when it ends |
| Vampirism keys | lang: `keys.vampirism.action` (R wheel), `suck`, `action1`..`action9`, `vision`, `skill_screen`, `select_skills` | R wheel is not covered in code today (`CoveredKeyIds`) |
| Werewolves 2.0.3.4 | `WerewolfPlayer.getActionHandler()` is Vampirism's `ActionHandler<IWerewolfPlayer>`; actions in `ModActions` (form x3, howling, rage, sense, fear, leap, hide_name); keys `keys.werewolves.leap`, `keys.werewolves.bite` only | `FactionActions` covers both mods with one code path |
| Great Sage 0.0.7.1 keys | `key.greatsage.menu` (G), `.assess` (V), `.scan` (B), `.voice`, `.panic` (Delete), `.magic_sense` (N), `.thought_acceleration` (H, lang) | Defaults **clash with our slot V** and Infinity's H. `GreatSageKeys.onClientTick` calls static `MagicSense.toggle()`, `AssessMode.toggle()`, `AssessMode.press/release/tick()` (scan, held), `VoiceInput.onKeyDown/onKeyUp`, `GreatSageKeys.menu()` (Screen) |
| What the G menu holds | lang `greatsage.menu.*`: Analyzing, Observations (archive), Battle Mode, Allocation, Settings, skill search, learn/analyse confirmations. Gamerule `requireGreatSage` | **Not powers.** The wheel's Great Sage category is its four key powers plus an "Open Great Sage" entry |
| Infinity Stones 8.0.0-alpha.6 (jar equals source `f82452f`) | `AbilityMeta.needsAim(StoneTree$Node)`, `isSwitch`, `isChannel`, `setting(Node)`; `DualModes.isDual(String modeId)`; `VariableAbility.range/modes/onChange/get`; `WheelClientState.get()` -> `WheelPayloads.State(entries, holding)`; `WheelPayloads.Entry(stone, node, unlocked, toggled, cooldownLeft, cooldownTotal, setting)`; `Use(int stone, String node, int phase)` (**phase 0 use, 1 stop, 2 second option**); `Setting(stone, node, delta)` | His wheel sends phase 2 for the dual option, not "sneaking". His server re-validates. Keys in his lang: `key.infinity.wheel` (H), `key.infinity.soul_control` (**G**), `key.infinity.soul_release` (Backspace). `StoneSkill.canBeSlotted` returns **false** |

### Not verified (listed in each PR report)

- The base body of `TensuraSkill.nextMode` (skipping locked modes is proven only for `StoneSkill`); `canBeSlotted`, `canBeToggled`, "declares `onHeld`/`onRelease`" and `Magic.isInstantCast` as the kind rule for the Tensura skills, spells and arts (T5 prints them for every skill and pins a table; `getMaxHeldTime` is **not** a kind signal: it is 20 or 72000 depending on the slot).
- Whether a toggle, a held art or a long spell in no slot keeps running after Tensura's own tick (T6 settles it; the fallback is a lock, never a hidden slot).
- Whether ManasCore ticks held skills inside the player's `PlayerTickEvent` (the T6 bracket depends on it); whether Tensura's own `ACTIVATE_ABILITY` listeners refuse a race ability without a slot.
- That `keyNumber` passed by the wheel (we use `WHEEL_KEY_NUMBER = 0`) is only stored by ManasCore's ticking list (T6 pins press/release symmetry).
- Where Great Sage handles `thought_acceleration` (not in `GreatSageKeys`); the private `hasSage` gate. Great Sage's Battle Mode may fire linked skills by itself.
- Whether Infinity's server-side toggled state can be read without his client payload; his `StoneTree`/`Stone` shapes beyond the signatures above.
- Any in-game look: no game was started for this plan.

### Spec points that changed

1. **Mode storage.** Tensura keeps the mode in the preset slot, not on the skill. Rule: slotted skill = the first slot holding it (active, next, then any preset), changed through Tensura's own `changeMode` on every slot holding it; skill in no slot (and every stone) = our own `WheelModes` (server attachment, synced in the catalogue). The wheel, slots and Tensura's screens agree whenever the skill is slotted.
2. **Infinity Stones cannot be slotted** (`StoneSkill.canBeSlotted = false`, "never from Tensura's slots/presets"). Right click on a stone entry shows a hint and does nothing. The spec's fallback "stones show as plain Tensura skills" is kept: the Tensura provider also lists skills of namespace `infinity` (use goes through the scope, `StoneSkill.canActivateSkill` still checks unlocked and carried).
3. **Dual option** is `Use(..., phase 2)`, not sneaking. Stone channels (his `HOLD`) are started and stopped by clicks, not by holding the mouse.
4. **Infinity "use/favourite keys" are not key mappings** (his favourites are H then 1 to 4 inside his wheel). Only `key.infinity.wheel` is covered. `key.infinity.soul_control` defaults to **G** like our wheel: owner call (default: left alone, flagged in W5).
5. **Great Sage entries are client built** (its functions are client statics), so W1's server catalogue has five sources and W4 adds the sixth.
6. **Shared cooldown** cannot be written into another mod's own cooldown. Vampirism has `ActionDeactivatedEvent.setCooldown`, so a lasting action's own end-cooldown can be raised to the group's; for every other side it is enforced by our gate: Vampirism and Werewolves by cancelling `ActionActivatedEvent`, Tensura by our own listener on `ACTIVATE_SKILL`, and shown from our group clock. Keys we do not cover reach the same gates for those two mods. Infinity has no member in any group (see 8), so nothing needs gating on his side. **Owner rule (2026-11-22): the cooldown applied to every side is the main side's own.** Vampirism and Werewolves give it (`IAction.getCooldown(player)`), ours gives it (`ExtraActions.Action.cooldownTicks`), Tensura gives none through a verified getter (the value read from the instance right after the press is used) and Infinity has none: a main side with no known cooldown means the group has none.
7. **Onion wheel, spells, arts, race ability, vision** are new since the spec's first version (owner, 2026-11-22); they add `PowerGroup` (below), the groups ring and paging. Infinity Stones has one group (a stone is one entry), so it has no middle ring.
8. **Spec: "dedup is keyed by power plus mode"** is replaced by the owner's "never inside a bigger skill". The race ability is a member of both M03 and M12, so a power may be in two groups (applied in file order). **Conflict:** every Infinity member of the accepted rows (M01, M02, M05, M08) is a power inside a stone, which is a mode of the stone's one entry, so by that rule those members cannot match; the production table omits them and those groups run between the remaining sides. If the owner wants the stone powers deduplicated, a stone would have to list its powers as separate entries again (the spec's superseded "one entry per unlocked power").

## File structure

```
core/wheel/PowerSource.java  PowerKind.java  LockReason.java  PowerRef.java  PowerState.java  WheelCategories.java
core/wheel/ActRules.java  WheelRules.java  SlotPlan.java  ReplacedPowers.java            (W1, pure)
core/wheel/overlap/OverlapTable.java  OverlapGroup.java  Dedup.java  GroupClock.java  StackRules.java   (W2, pure)
core/wheel/GestureMachine.java  WheelLayout.java  ClickPlan.java  ModeCycle.java  WheelNarration.java   (W4, pure)
parts/hud/wheel/WheelCatalogue.java  PowerProvider.java  OursProvider.java  FactionProvider.java  TensuraProvider.java
parts/hud/wheel/WheelService.java  WheelSync.java  WheelModes.java  WheelGate.java  TensuraGate.java  VampirismGate.java
parts/hud/tensura/WheelActivation.java                   (scope used by NextPreset.matches)
parts/hud/net/WheelEntry.java  WheelViewPayload.java  WheelCataloguePayload.java  UsePowerPayload.java  SetPowerModePayload.java
parts/hud/client/wheel/WheelScreen.java  WheelState.java  AimMode.java  LastPower.java  GreatSageProvider.java  SlotPicker.java
parts/hud/client/wheel/look/WheelLook.java  GrimoireWheel.java  BloodMoonWheel.java  TensuraWheel.java
parts/hud/client/wheel/infinity/InfinityCompat.java  StoneApi.java                         (W5)
parts/hud/gametest/WheelCatalogueGameTests.java  WheelUseGameTests.java  WheelModeGameTests.java  WheelOverlapGameTests.java
data/supernaturalbloodlines/wheel/overlaps.json  (production: empty groups until the owner approves)
src/test/resources/wheel/overlaps-seed.json  (M01 and M06, tests only)
docs/superpowers/design/skill-wheel/README.md  (+ PNG exports)                           (W3)
```

Existing files touched: `ActionService`, `HudNet`, `HudPart`, `HudConfig`, `HudClientConfig`, `HudKeys`, `HudInput`, `KeyCover`, `CoveredKeyIds`, `NextPreset`, `ExtraActions` (adds `default boolean aimed()`), `RipHeartAction`/`NeckSnapAction` (override it), `ModAttachments`, `AdminEntries`, `PlayerSettings`, `LoadoutTabModel`/Loadout page (reads the catalogue), `docs/mixins.md`, `docs/parts/hud.md`, `docs/balance.md`, `CHANGELOG.md`.

---

# Phase W1: catalogue, server path, payloads, key covering (headless)

Branch `feature/wheel-w1-catalogue`. Nothing is drawn. Network version `HudNet.VERSION` `"1"` to `"2"`.

### Task 1: The pure model

**Files:**
- Create: `core/wheel/PowerSource.java`, `PowerKind.java`, `LockReason.java`, `PowerRef.java`, `PowerState.java`, `WheelCategories.java`
- Test: `src/test/java/.../core/wheel/PowerRefTest.java`, `WheelCategoriesTest.java`

**Interfaces:**
- Produces: `enum PowerSource { BLOODLINE, VAMPIRISM, WEREWOLVES, TENSURA, INFINITY, GREAT_SAGE; String modId(); static List<PowerSource> displayOrder() }` (that order). `enum PowerKind { INSTANT, TOGGLE, AIMED, HELD }`. `enum LockReason { NONE, NOT_UNLOCKED, CANNOT_ACT, NEEDS_SLOT, REFUSED_NOW }` (append-only: the wire sends the ordinal; lang `supernaturalbloodlines.wheel.lock.<lowercase name>`). `record PowerRef(PowerSource source, ResourceLocation id)` with `String key()` (`"tensura/tensura:coercion"`) and `static Optional<PowerRef> parse(String)`. `record PowerState(boolean on, int cooldownLeft, int cooldownTotal, LockReason lock, int mode, int modeCount)`. `enum PowerGroup { MAIN, SKILLS, MAGIC, BATTLEWILL }` (owner, 2026-11-22; every source but Tensura uses `MAIN`; Tensura uses the other three; `displayOrder()` is SKILLS, MAGIC, BATTLEWILL). `WheelCategories.visible(Collection<PowerSource> withEntries, Predicate<PowerSource> installed): List<PowerSource>` (the inner ring). `WheelCategories.groups(PowerSource, Collection<PowerGroup> withEntries): List<PowerGroup>` (the middle ring; **empty when the source has fewer than two non-empty groups**, which means "skip the ring").

- [ ] **Step 1: Write the failing tests.** `PowerRefTest`: `parseRoundTripsEveryKey` (for each source), `rejectsUnknownSourceEmptyAndOverlongKeys` (`""`, `"x/y"`, `"tensura/"`, 300 chars: all `Optional.empty()`). `WheelCategoriesTest`: `emptyCategoryIsHidden` (Enchanted-like source with no entries is absent), `orderIsFixedWhateverTheInput`, `uninstalledSourceIsHiddenEvenWithEntries` (a stale entry from a removed mod), `tensuraWithThreeGroupsHasAMiddleRing`, `tensuraWithOnlySkillsHasNoMiddleRing` (a player with no spells and no arts), `tensuraWithSkillsAndSpellsHasTwoGroups`, `otherSourcesNeverHaveAMiddleRing`.
- [ ] **Step 2: Run** `./gradlew test --tests "*core.wheel.*"`. Expected: FAIL (classes missing).
- [ ] **Step 3: Implement** the types above (records with compact validation; `parse` caps the key at 256 chars and uses `ResourceLocation.tryParse`).
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): power model and category order`.

### Task 2: Pure rules, and one source of truth for "cannot act"

**Files:**
- Create: `core/wheel/ActRules.java`, `WheelRules.java`, `SlotPlan.java`, `ReplacedPowers.java`
- Modify: `parts/hud/action/ActionService.java` (use `ActRules`)
- Test: `ActRulesTest`, `WheelRulesTest`, `SlotPlanTest`, `ReplacedPowersTest`

**Interfaces:**
- Produces: `ActRules.cannotAct(boolean blockedByPart, boolean spectator, boolean alive, boolean sleeping): boolean`. `enum SlotTarget { LOADOUT, TENSURA_PRESET, NONE }`; `WheelRules.slotTarget(PowerSource, boolean slottable): SlotTarget` (BLOODLINE, VAMPIRISM, WEREWOLVES -> LOADOUT; TENSURA -> TENSURA_PRESET if slottable else NONE; INFINITY, GREAT_SAGE -> NONE). `record TensuraSlot(int preset, int slot)`; `SlotPlan.tensura(int hudSlot, int activePreset, int presetCount): TensuraSlot` built on `InputPlan.rightColumn/row` and `PresetPair`. `ReplacedPowers.hidden(PowerRef, boolean sprintLeapOn): boolean` (true only for `werewolves/werewolves:leap` while the client setting `sprint_leap` is on; the server always lists it).

- [ ] **Step 1: Write the failing tests.** `ActRulesTest.everyCombinationMatchesTheOldInlineChecks` (truth table over the four booleans equals `blocked || spectator || !alive || sleeping`). `SlotPlanTest.leftColumnIsActivePreset` (`tensura(0, 3, 9)` is `(3, 0)`; slot 2 is `(3, 2)`), `rightColumnIsNextPreset` (`tensura(3, 3, 9)` is `(4, 0)`), `lastPresetPairsWithFirst` (`tensura(5, 8, 9)` is `(0, 2)`), `outOfRangePresetWraps`. `WheelRulesTest.stoneIsNeverSlottable`, `tensuraNotSlottableIsNone`. `ReplacedPowersTest.leapHiddenOnlyWhileSprintLeapIsOn`, `nothingElseIsHidden`.
- [ ] **Step 2: Run** `./gradlew test --tests "*core.wheel.*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the four classes; in `ActionService` replace the two inline `p.isSpectator() || !p.isAlive() || p.isSleeping()` tests and the `ExtraActions.blocked(p)` test with `ActRules.cannotAct(...)` (same lang key `cannot_act`, same order, a release is still never blocked).
- [ ] **Step 4: Run** `./gradlew test` and the existing `ActionGameTests` through run 1. Expected: all PASS (behaviour unchanged).
- [ ] **Step 5: Commit** `refactor(hud): one cannot-act rule; feat(wheel): slot plan and replaced powers`.

### Task 3: Payloads and the entry codec

**Files:**
- Create: `parts/hud/net/WheelEntry.java`, `WheelViewPayload.java`, `WheelCataloguePayload.java`, `UsePowerPayload.java`, `SetPowerModePayload.java`
- Modify: `parts/hud/net/HudNet.java` (`VERSION = "2"`, register the four; `playToClient` ones use `RegistryFriendlyByteBuf`), `CHANGELOG.md`
- Test: `src/test/java/.../parts/hud/net/WheelPayloadCodecTest.java`

**Interfaces:**
- Produces: `record WheelEntry(PowerRef ref, PowerGroup group, PowerKind kind, Component name, Component modeName, List<Component> modeNames, ResourceLocation icon, PowerState state, boolean slottable, float mastery)`; `mastery` is `-1` for none. `record WheelViewPayload(boolean open)` (c2s); `record WheelCataloguePayload(int revision, List<WheelEntry> entries)` (s2c); `record UsePowerPayload(String ref, int mode, int targetEntityId, boolean release)` (c2s); `record SetPowerModePayload(String ref, int delta)` (c2s, delta is +1 or -1; the server clamps anything else to its sign).
- Limits: at most 512 entries, 16 mode names, 256 characters per string; a decode over a limit throws (the connection drops the packet).

- [ ] **Step 1: Write the failing test.** `WheelPayloadCodecTest`: `entryRoundTripsThroughARegistryBuffer` (build a `RegistryFriendlyByteBuf` over `RegistryAccess.EMPTY`; every field equal after decode), `catalogueOverTheEntryCapFailsToDecode` (513 entries), `useAndModePayloadsRoundTrip`, `stringOverTheLimitFailsToDecode`.
- [ ] **Step 2: Run** `./gradlew test --tests "*WheelPayloadCodecTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** the records and their `STREAM_CODEC`s (`ComponentSerialization.STREAM_CODEC` for names; `ByteBufCodecs.stringUtf8(256)` for strings; `VAR_INT` for ints). In `HudNet.onRegisterPayloads` register `WheelViewPayload`, `UsePowerPayload`, `SetPowerModePayload` with `playToServer` (handlers added in T7/T8) and the catalogue with `playToClient` (the handler stores it in a client holder, `ClientCatalogue`, in W4; until then a no-op that drops it). Set `VERSION = "2"`.
- [ ] **Step 4: Run** the test again. Expected: PASS.
- [ ] **Step 5: Add the CHANGELOG entry** (Added: the four payloads; Changed: "the hud network version is 2. **Update the server and every client together.** An older client has no wheel channel: nothing is sent to it, and its keys are not covered for the wheel"; Rolling back).
- [ ] **Step 6: Commit** `feat(wheel): payloads and codec (hud network 2)`.

### Task 4: Our and the factions' powers

**Files:**
- Create: `parts/hud/wheel/PowerProvider.java`, `OursProvider.java`, `FactionProvider.java`, `WheelCatalogue.java`
- Modify: `shared/hud/ExtraActions.java` (add `default boolean aimed() { return false; }`), the species part's `RipHeartAction` and `NeckSnapAction` (return true)
- Test: `parts/hud/gametest/WheelCatalogueGameTests.java`

**Interfaces:**
- Produces: `interface PowerProvider { List<WheelEntry> list(ServerPlayer p); }`. `WheelCatalogue.install(PowerProvider)`; `WheelCatalogue.build(ServerPlayer): List<WheelEntry>` (providers in `PowerSource` order; a provider that throws is logged once and skipped, never breaks the list; stable order inside a source). `FactionProvider` also lists the **Vision** entry (owner, 2026-11-22): ref `vampirism:vision`, source VAMPIRISM, group MAIN, kind INSTANT (it cycles), `on` = the player has an active vision, caption = `getActiveVision()` name, only for a Vampirism vampire; it is used by `VampirePlayer.switchVision()` through a method handle (concrete class, not API; one `WARN` and no entry if the handle cannot be built). `OursProvider`: Bite (`Loadout.BITE`, kind HELD, only when `BiteService.canBite(p)`) and every `ExtraActions.all()` with `available(p)` (kind AIMED when `aimed()`, else INSTANT; cooldown from `cooldownLeft`/`cooldownTicks`). `FactionProvider` (linked to Vampirism, so only after `isLoaded`): every id of `FactionActions.unlockedIds(p)`, source VAMPIRISM or WEREWOLVES by the action's faction, kind TOGGLE for an `ILastingAction`, AIMED for the ids in `AimedActions.IDS` (`vampirism:teleport`), else INSTANT; `on` from `isActionActive`, cooldown from `isActionOnCooldown` and `getPercentageForAction`; lock `REFUSED_NOW` when `action.canUse(fp)` is `DISALLOWED` or `DISABLED`.

- [ ] **Step 1: Write the failing GameTests** (`@GameTest` style of `LoadoutVampirismGameTests`; registered in `HudPart` with the same `isLoaded` guards; skip in runs 3 and 9). `oursListsBiteOnlyForABiter`, `alphaRoarListedOnlyWhenAvailable`, `ripHeartAndNeckSnapAreAimed`, `unlockedVampirismActionsListedLockedOnesNot` (an action not unlocked is absent), `lastingActionIsToggleWithOnState`, `teleportIsAimed`, `werewolfActionsAreSourceWerewolves`, `aThrowingProviderIsSkippedNotFatal`, `noPowersGivesAnEmptyListNotAnError` (a human with no mods' powers), `visionEntryIsListedOnlyForAVampirismVampire`, `visionUseCallsSwitchVisionAndChangesTheCaption`, `visionHandleMissingMeansNoEntryAndOneWarning`.
- [ ] **Step 2: Run** `./gradlew runGameTestServer`. Expected: the new tests FAIL (classes missing).
- [ ] **Step 3: Implement** the providers; install them from `HudPart.init` (`OursProvider` always; `FactionProvider` inside the `isLoaded("vampirism")` branch).
- [ ] **Step 4: Run** runs 1, 2, 3, 9. Expected: PASS, and `skipped cleanly` in 3 and 9.
- [ ] **Step 5: Commit** `feat(wheel): catalogue of our and the factions' powers`.

### Task 5: Tensura's skills, spells, arts and race ability, and the kind rule

**Files:**
- Create: `parts/hud/wheel/TensuraProvider.java`, `parts/hud/gametest/TensuraKindProbe.java` (a GameTest that only writes a file), `src/main/resources/data/supernaturalbloodlines/wheel/tensura_kinds.json`
- Test: `WheelCatalogueGameTests` (more cases), `src/test/.../TensuraKindsTest.java` (pure parsing of the json)

**Interfaces:**
- Consumes: `PowerProvider` (T4).
- Produces: `TensuraProvider implements PowerProvider` (linked to Tensura). It lists a learned skill when `canBeSlotted(inst, p, 0)` is true **or** the registry namespace is `infinity`. **Group (owner, 2026-11-22):** `instanceof Magic` -> `MAGIC`, `instanceof Battlewill` -> `BATTLEWILL`, anything else (Tensura, addon and stone skills, and the race ability) -> `SKILLS`. Name `skill.getName()`, mode name `TensuraSkill.getModeName`, icon `getSkillIcon()`, `on` = `inst.isToggled()`, cooldown = `inst.getCoolDown(mode)` seconds times 20 (ManasCore counts cooldowns in seconds: see `SkillCooldown`), `mastery` from `core/hud/Mastery`, `modeCount = inst.getModes()`, `slottable = canBeSlotted`. Lock `REFUSED_NOW` when `!inst.canInteractSkill(p)` or `!skill.canActivateSkill(inst, p, mode)` (a spell is refused while silenced or underwater). Tiers (owner, 2026-11-22): a skill that is not the highest owned of a chain in `tensura_kinds.json` is not listed. **Kind rule (replaces any use of `getMaxHeldTime`, which is 20 or 72000 by slot):** a `Magic` is HELD unless `isInstantCast(inst, p)` (then INSTANT); a skill whose class (below `TensuraSkill`) declares `onHeld` or `onRelease` is HELD; `canBeToggled` is TOGGLE; else INSTANT; then overridden by `tensura_kinds.json` (`{"overrides": {"tensura:coercion": "INSTANT"}, "chains": [["tensura:self_regeneration", "tensura:ultraspeed_regeneration", "tensura:infinite_regeneration"]]}`). Tensura has no AIMED. **Race ability (owner, 2026-11-22):** one extra entry per player with a race that has an ability: ref `tensura:race_ability`, group SKILLS, kind HELD, name and icon from `Races.getRace()`, lock from `ManasRace.canActivateAbility`; used by `RaceAPI.getRaceFrom(p).startHeldAbility()` and `releaseHeldAbility()` (T7), no slot and no preset involved.

- [ ] **Step 1: Write the probe.** `TensuraKindProbe` (run by hand, not registered for the matrix) walks the registry and writes `(id, class group, canBeSlotted, declaresOnHeld, declaresOnRelease, canBeToggled, isInstantCast, getModes)` per skill to `C:\Users\caiom\AppData\Local\Temp\opencode\wheel-plan\tensura-kinds.txt`. Run it once with `./gradlew runGameTestServer`; read the file.
- [ ] **Step 2: Write the failing tests.** Pin the rule with real ids from the research table and the probe: `coercionIsInstant`, `selfRegenerationIsToggle`, `gravityFlightIsHeld`, `passiveSkillIsNotListed` (pick one passive id from the probe file), `multiModeSkillHasOneEntryWithModeNames`, `unlearnedSkillIsNotListed`, `namespaceInfinityIsListedEvenIfNotSlottable` (skips when Infinity is absent), `aSpellIsInTheMagicGroup` and `aSpellWithACastTimeIsHeld` (pick two spells from the probe: one with a cast time, one `isInstantCast` that is INSTANT), `anArtIsInTheBattlewillGroup` (`HeavySlashArt` is INSTANT, `AirFlightArt` is HELD), `silencedPlayerSeesSpellsLocked`, `onlyTheHighestRegenerationTierIsListed`, `raceAbilityEntryIsHeldInSkills`, `playerWithoutARaceAbilityHasNoRaceEntry`. `TensuraKindsTest.overridesParseAndRejectUnknownKinds`, `chainsParseAndRejectAnIdInTwoChains`.
- [ ] **Step 3: Implement** the provider and the json loader; add any override the probe shows the rule gets wrong (each with a one-line `why`). Install from `HudPart` inside `isLoaded("tensura")`.
- [ ] **Step 4: Run** runs 1, 2, 3. Expected: PASS (skip in 3).
- [ ] **Step 5: Commit** `feat(wheel): Tensura provider with a pinned kind table`.

### Task 6: Activating a Tensura skill that is in no slot (the spike comes first)

**Files:**
- Create: `parts/hud/tensura/WheelActivation.java`, `parts/hud/wheel/TensuraUse.java`
- Modify: `parts/hud/tensura/NextPreset.java`, `parts/hud/HudConfig.java` (`wheel_unslotted_tensura`), `shared/admin/AdminEntries.java`, `docs/mixins.md` (row (1): "also true inside `WheelActivation.scope`"), `docs/balance.md`
- Test: `parts/hud/gametest/WheelUseGameTests.java`

**Interfaces:**
- Produces: `WheelActivation.scope(ManasSkill skill, int mode, Supplier<T> body): T` (thread-local, restored in `finally`, nests) and `WheelActivation.allows(ManasSkill skill, int slot, int mode): boolean` (true only inside a scope for that same skill; mode must equal the scoped mode or be `NextPreset.ANY`). `NextPreset.matches` returns `true` first when `allows(...)`. `TensuraUse.press(ServerPlayer, ResourceLocation skill, int mode): @Nullable String` (lang key of a refusal) calls `SkillAPI.getSkillsFrom(p).startHoldSkill(inst, TensuraUse.WHEEL_KEY_NUMBER, mode)` inside the scope; `TensuraUse.release(ServerPlayer, ResourceLocation, int mode)` calls `handleSkillRelease(inst, WHEEL_KEY_NUMBER, mode, false)`; `WHEEL_KEY_NUMBER = 0`. `TensuraUse.pressRace(ServerPlayer): @Nullable String` / `releaseRace(ServerPlayer)` call `Races.startHeldAbility()` / `releaseHeldAbility()`. **Held bracket:** `getMaxHeldTime` is 20 ticks for a skill that fails `isAbilityInActivePreset(skill)`, and it is asked while the skill ticks, long after our scope closed. So `WheelActivation.holding(ServerPlayer, ManasSkill, int)` / `stopHolding(...)` keep a per-player set of wheel-held skills, and `PlayerTickEvent.Pre` opens (and `Post` closes) a scope for each of them around that player's tick, so `matches` is true while ManasCore ticks the held skill. Config `wheel_unslotted_tensura` (BOOL, default true).

- [ ] **Step 1: Write the spike tests** (skip without Tensura, and in run 9): `unslottedInstantSkillActivates` (a skill that is in no preset activates through `press`, its cooldown starts), `unslottedToggleSurvivesFortyTicks` (toggle on, tick 40, `isToggled()` still true), `scopeDoesNotLeakToOtherSkills` (inside the scope another skill is still refused; after the scope the same skill is refused), `scopeIsRestoredAfterAnException`, `mixinOffLocksUnslotted` (with `next_preset_active=false` press returns the refusal `supernaturalbloodlines.wheel.refused.tensura` and nothing runs), `heldSkillPressAndReleaseAreSymmetric` (a held skill started with key 0 ends on `release` with key 0), `unslottedHeldSkillRunsPastTwentyTicks` (an art that is HELD in the probe file, held for 60 ticks, still running), `unslottedLongSpellCompletes` (a spell with a cast time over 20 ticks, held for its cast time, takes effect; and one released early does not), `heldBracketEndsOnReleaseDeathAndLogout` (after each, `matches` is false again for that skill), `raceAbilityStartsAndReleasesWithoutASlot`.
- [ ] **Step 2: Run** `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement** `WheelActivation`, the `NextPreset.matches` line, `TensuraUse`, the config key and its Admin row (`AdminEntries.HUD_FILE`, `BOOL`).
- [ ] **Step 4: Run** run 1. Decision point, written into `docs/parts/hud.md` with the date: if `unslottedToggleSurvivesFortyTicks` passes, done. If it fails with the scope correct, set `TensuraProvider.TOGGLE_NEEDS_SLOT = true` (a toggle in no slot lists with lock `NEEDS_SLOT`; `TensuraUse.press` refuses it), rename the test to `unslottedToggleIsLockedNeedsSlot` and assert that instead. Same for held: if `unslottedHeldSkillRunsPastTwentyTicks` or `unslottedLongSpellCompletes` fails with the bracket in place (ManasCore may tick held skills outside the player tick), set `TensuraProvider.HELD_NEEDS_SLOT = true` for every HELD skill whose cast or hold exceeds 20 ticks (spells with a cast time, held arts): they list locked `NEEDS_SLOT`, and instant skills, instant-cast spells and instant arts still work unslotted. **Report the outcome to the owner as the feasibility flag for spells and arts.** Never park a skill in a hidden preset slot.
- [ ] **Step 5: Run** runs 1, 2, 3, 9. Expected: PASS or clean skips.
- [ ] **Step 6: Commit** `feat(wheel): use a Tensura skill that is in no slot (scope on NextPreset.matches)`.

### Task 7: The server use and mode path

**Files:**
- Create: `parts/hud/wheel/WheelService.java`, `WheelModes.java`
- Modify: `parts/hud/action/ActionService.java` (add `triggerById`), `parts/hud/net/HudNet.java` (handlers, logout cleanup), `core/attachment/ModAttachments.java` (`WHEEL_MODES`: `Map<ResourceLocation, Integer>`, kept on death)
- Test: `WheelUseGameTests`, `WheelModeGameTests`

**Interfaces:**
- Consumes: `ActRules`, `PowerRef`, `TensuraUse` (T6), `HudNet.allowed`.
- Produces: `ActionService.triggerById(ServerPlayer p, ResourceLocation id, int targetEntityId, boolean release): @Nullable String` (the same body as `trigger` after the slot is resolved: refactor `trigger` to resolve the id then call it, so both paths share every check, refusal and the `HELD` mark, which becomes keyed by id as well as slot). `WheelService.use(ServerPlayer, PowerRef, int mode, int targetEntityId, boolean release): @Nullable String`; `WheelService.changeMode(ServerPlayer, PowerRef, int delta): @Nullable String`; `WheelService.forget(ServerPlayer)` (releases every held power; called from the logout, death, dimension and `ExtraActions.addStopper` hooks). `WheelModes.get(ServerPlayer, ResourceLocation): int` and `set(...)` (clamped to `[0, getModes())`).
- Rules: source BLOODLINE/VAMPIRISM/WEREWOLVES go to `triggerById` (`vampirism:vision` goes to `switchVision`); TENSURA to `TensuraUse` (a skill, spell or art: mode checked against `getModes()` and `canActivateSkill`; `tensura:race_ability` to `pressRace`); INFINITY and GREAT_SAGE are refused (`not_server_power`, they never reach the server). `changeMode`: a Tensura skill in a slot runs Tensura's own `RequestAbilityModeChangePacket.changeModePacket(slot, preset, backwards).changeMode(p)` for **every** slot holding it; a skill in no slot uses `TensuraSkill.nextMode(p, inst, current, backwards)` and stores the result in `WheelModes`; a result of `-1` or the same mode is "no unlocked mode" (one line, nothing changes).

- [ ] **Step 1: Write the failing GameTests.** `WheelUseGameTests`: `downedPlayerCannotUseFromWheel`, `snappedAndDyingCannotUse`, `forgedUseOfALockedActionRefused` (id of an action the player has not unlocked), `wheelRefusalsEqualSlotRefusals` (for each case in `ActionGameTests`, `triggerById` returns the same lang key as `trigger`), `releaseWithNothingHeldIsIgnored`, `forgetReleasesAHeldBite`, `forgetReleasesAHeldTensuraSkill`, `unknownRefAndWrongSourceAreRefused`, `noRefusalNamesTheTarget` (a refused aimed use returns the same key whether the target is an Original or not), `usePowerFloodIsDropped` (rate limit: the 11th press in a second returns `RATE_LIMITED`; releases are not counted). `WheelModeGameTests`: `slottedSkillModeChangeUsesTensurasOwnSlotMode` (the `AbilitySlot` mode moved, and so for a second slot holding it), `unslottedModeIsStoredAndSurvivesRelog`, `modeIndexOutOfRangeIsClamped`, `lockedModesAreSkipped` (a multi-mode skill chosen from the probe file; mode locked by `canActivateSkill`), `noUnlockedModeSaysSoAndChangesNothing`, `deltaZeroOrHugeIsTreatedAsItsSign`.
- [ ] **Step 2: Run** run 1. Expected: FAIL.
- [ ] **Step 3: Implement** `ActionService.triggerById` (move, do not copy), `WheelService`, `WheelModes` and the attachment; add `HudNet.handleUse` and `handleMode` (rate limited like `handleTrigger`: a release is free) and register them for the payloads of T3.
- [ ] **Step 4: Run** runs 1, 2, 3, 9, then the whole existing `hud` GameTest set. Expected: PASS, unchanged results for the old tests.
- [ ] **Step 5: Commit** `feat(wheel): server use and mode path`.

### Task 8: Open, push on change, and the server settings

**Files:**
- Create: `parts/hud/wheel/WheelSync.java`
- Modify: `parts/hud/HudConfig.java` (`wheel_enabled`, `wheel_catalogue_interval_ticks`), `shared/admin/AdminEntries.java`, `docs/balance.md`, `HudNet.java`
- Test: `WheelCatalogueGameTests`, `src/test/.../core/wheel/ChangeGateTest.java`

**Interfaces:**
- Produces: `WheelSync.open(ServerPlayer)` / `close(ServerPlayer)` (viewer set by UUID; a logout, respawn or dimension change closes), `WheelSync.tick(ServerPlayer)` (every `wheel_catalogue_interval_ticks`, only for viewers: build, compare a hash with the last sent, send when different, `revision++`), `WheelSync.setSink(BiConsumer<ServerPlayer, WheelCataloguePayload>)` as the test seam. Pure `ChangeGate` holds the last hash. Config: `wheel_enabled` (BOOL, true: off means `open` answers an empty catalogue and `use` refuses with `wheel_disabled`), `wheel_catalogue_interval_ticks` (INT 1 to 40, default 5).

- [ ] **Step 1: Write the failing tests.** `ChangeGateTest.sameHashSendsNothingDifferentSends`. GameTests: `openSendsOneCatalogueAtOnce`, `idleViewerGetsNothingForFortyTicks`, `aCooldownChangeSendsExactlyOneUpdate`, `closedPlayerIsNeverBuilt` (the build counter stays 0), `logoutRemovesTheViewer`, `wheelDisabledSendsAnEmptyListAndRefusesUse`.
- [ ] **Step 2: Run** run 1. Expected: FAIL.
- [ ] **Step 3: Implement** the sync (a `PlayerTickEvent.Post` listener, off for `FakePlayer`, like `LoadoutService.register`) and the two settings with their Admin rows.
- [ ] **Step 4: Run** runs 1, 2, 9, and `./gradlew test` (`AdminCoverageTest` green). Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): open, change-gated catalogue push, server settings`.

### Task 9: Cover the other mods' skill keys (not yet applied)

**Files:**
- Modify: `core/hud/CoveredKeyIds.java`, `parts/hud/client/KeyCover.java`, `docs/parts/hud.md`
- Test: `CoveredKeyIdsTest` (extend), `CoveredKeysTest`, `CoverRunTest` (extend)

**Interfaces:**
- Produces: `CoveredKeyIds.WHEEL_IDS` = `keys.vampirism.action`, **`keys.vampirism.vision`**, `key.greatsage.menu`, `key.greatsage.assess`, `key.greatsage.scan`, `key.greatsage.magic_sense`, `key.greatsage.thought_acceleration`, `key.infinity.wheel`, **`tensura.keybinding.race_ability`** (the last two named in bold are the owner's, 2026-11-22); `CoveredKeyIds.all(boolean sprintLeap, boolean wheelReady)` adds them only when `wheelReady`; the old `all(boolean)` stays and means `wheelReady = false`. `WheelBuild.screenPresent()` returns `false` until W4 T10 flips it; `KeyCover.covered()` passes it. **Left alone** (spec): Tensura status, naming, dodge, mode; Vampirism skill screen; Great Sage voice, panic; Infinity `soul_control`, `soul_release`; Ascension, Horizons, Enchanted keys.

- [ ] **Step 1: Write the failing tests.** `coveredIdsMatchTheSpecList` (exact set, with `wheelReady = true`), `wheelIdsAreAbsentWhileNoScreenExists` (`all(true)` unchanged from main), `leftAloneIdsAreNeverCovered` (a literal list of the ones above; `keys.vampirism.vision` and `tensura.keybinding.race_ability` are no longer in it), `anUnknownIdIsIgnoredAndItsRecordKept` (mod not installed: the existing `CoveredKeys` rule, now with a wheel id), `restoreGivesEveryRecordedWheelKeyBack` (cover then `hide=false`, binds equal the originals), `coverWithAnUnreadableRecordTouchesNothing`.
- [ ] **Step 2: Run** `./gradlew test --tests "*Cover*"`. Expected: FAIL.
- [ ] **Step 3: Implement** the list and the flag.
- [ ] **Step 4: Run** `./gradlew test`. Expected: PASS; main's cover behaviour is byte-for-byte the same while the flag is false.
- [ ] **Step 5: Commit** `feat(wheel): covered-key ids for the wheel, off until the screen exists`.

### Task 10: W1 phase gate

- [ ] **Step 1: Run the matrix** (12 runs plus `./gradlew test`) into `matrix-w1.log`. Expected: 13 green lines, last `DONE`; the wheel GameTests skip cleanly in 3 (Tensura ones) and 9 (all).
- [ ] **Step 2: Mutation targets**, each broken alone, the named test must go red, then restored:
  1. `ActRules.cannotAct`: drop `blockedByPart` -> `downedPlayerCannotUseFromWheel`.
  2. `WheelService.use`: skip the `FactionActions.unlocked` re-check -> `forgedUseOfALockedActionRefused`.
  3. `WheelActivation.allows`: return `true` always -> `scopeDoesNotLeakToOtherSkills`.
  4. `NextPreset.matches`: remove the `allows` line -> `unslottedInstantSkillActivates`.
  5. `WheelCategories.visible`: keep empty sources -> `emptyCategoryIsHidden`.
  6. `CoveredKeyIds.WHEEL_IDS`: drop `key.greatsage.scan` -> `coveredIdsMatchTheSpecList`.
  7. `SlotPlan.tensura`: right column uses `active` -> `rightColumnIsNextPreset`.
  8. `HudNet.handleUse`: skip `allowed(p)` -> `usePowerFloodIsDropped`.
  9. `WheelSync.tick`: send every tick -> `idleViewerGetsNothingForFortyTicks`.
  10. `WheelActivation`: skip the `PlayerTickEvent` bracket -> `unslottedHeldSkillRunsPastTwentyTicks` (or `unslottedToggleIsLockedNeedsSlot` if the fallback applies).
  11. `WheelCategories.groups`: return the groups for a one-group source -> `otherSourcesNeverHaveAMiddleRing`.
  12. `TensuraProvider`: classify `instanceof Magic` as SKILLS -> `aSpellIsInTheMagicGroup`.
- [ ] **Step 3: Settings and docs.** Admin rows exist for `wheel_enabled`, `wheel_catalogue_interval_ticks`, `wheel_unslotted_tensura` (`AdminCoverageTest` green). No client setting in W1 (nothing is drawn). `docs/parts/hud.md` has the T6 decision. CHANGELOG has Added, Changed (network 2) and Rolling back.
- [ ] **Step 4: Report** the hooks not verified (list above) and open the PR "feat(wheel): W1 catalogue and server path". Hidden shots: none in W1.

---

# Phase W2: overlap data and rule C

Branch `feature/wheel-w2-overlaps`, from W1. **The owner accepted every pick of the research table (2026-11-22)**, so the production table ships with the accepted groups (T1 Step 4), and a later change to one pair is a data edit with no code change. Tests use two seeds, M01 (invisibility) and M06 (rage), in `src/test/resources/wheel/overlaps-seed.json`, so a change to production data never breaks a unit test. Network version `"2"` to `"3"` (the entry gains the group id).

### Task 1: The data format and its loader

**Files:**
- Create: `core/wheel/overlap/OverlapTable.java`, `OverlapGroup.java`, `data/supernaturalbloodlines/wheel/overlaps.json`, `src/test/resources/wheel/overlaps-seed.json`
- Test: `OverlapTableTest`

**Interfaces:**
- File: `{"format": 1, "chains": [ ["tensura/tensura:self_regeneration", "tensura/tensura:ultraspeed_regeneration", "tensura/tensura:infinite_regeneration"] ], "groups": [ {"id": "invisibility", "research": "M01", "confidence": "same", "main": 0, "members": ["vampirism/vampirism:vampire_invisibility", "infinity/infinity:reality.invisibility", "tensura/tensura:invisible"] } ]}`. A **member is one whole power** (a `PowerRef` key): never a mode of a skill (owner, 2026-11-22), so there is no `mode` field. `main` is the index of the main member (fixed per group, never per player: where the research proposes a different main for a different kind of player, the file has one group per main). `chains` (owner, 2026-11-22) list the tiers of one skill, lowest first: only the highest tier the player has is listed, and a chain member counts as the same power as any other id of its chain when a group is matched. There is no `requires` and no `warnings`: the vision and race-ability keys are on the wheel now, and the `soul.regen` hazard is a known limit, not a shown warning.
- Produces: `OverlapTable.parse(JsonObject): OverlapTable` (throws `IllegalArgumentException` with the group id), `OverlapTable.groups(): List<OverlapGroup>`, `OverlapTable.chains(): List<List<PowerRef>>`, `OverlapGroup.members()`, `main()`.
- Validation: `format == 1`; at least 2 members; `main` in range; only `same` and `close` (a `loose` group is rejected: the 13 loose matches stay separate); a ref may be in **two groups** (the race ability is in M03 and M12; groups apply in file order), but not twice in one group; an id in two chains is rejected; an unknown `PowerSource` prefix is rejected.

- [ ] **Step 1: Write the failing tests.** `seedFileParses` (M01 has the Vampirism action, the stone power and `tensura:invisible`, main 0; M06 has `vampirism:vampire_rage` and `werewolves:rage`), `looseGroupRejected`, `sameRefInTwoGroupsIsAllowed`, `sameRefTwiceInOneGroupRejected`, `mainOutOfRangeRejected`, `singleMemberRejected`, `unknownSourceRejected`, `idInTwoChainsRejected`, `everyIdInTheResearchTableUsesAKnownSource` (read `docs/superpowers/research/skill-overlaps.md`, take every backticked `mod:id`, assert its mod prefix maps to a `PowerSource` or is a documented addon prefix: guards the format against the research), `shippedGroupsAreExactlyTheAcceptedRows` (below).
- [ ] **Step 2: Run** `./gradlew test --tests "*OverlapTableTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** the loader (Gson `JsonObject`, no Minecraft codec needed in the pure layer). Load the production file in `HudPart` from the mod jar resource at server start.
- [ ] **Step 4: Write the production table** `data/supernaturalbloodlines/wheel/overlaps.json` from the research, rows **M01 to M08, M10, M11, M12** (owner, 2026-11-22), whole powers only. **Left out on purpose:** every Infinity member (a stone power is a mode inside a stone, and no dedup looks inside modes: flagged for the owner in the report), M09 (keep both), the haki skills in M07 (`mortal_fear`, `hero_haki`, `demon_lord_haki`, `sacred_haki`), `tensura:traveler` and `tensura:suppressor` in M08, every row of research section 3 (loose), and boost-style race abilities in M04. M01 includes the Tensura **Invisibility spell** (`tensura:invisible`, close); M02 is one group, main `vampirism:vision` against `tensura:godwolf_sense` (the research's werewolf-side main is a second group only if the owner wants it: not shipped); M03 and M12 name `tensura:race_ability` as their Tensura member (mains `vampirism:bat` and `supernaturalbloodlines:bite`); M05 has the regeneration chain. `shippedGroupsAreExactlyTheAcceptedRows` pins the list of group ids and their mains so a typo fails.
- [ ] **Step 5: Run** the test. Expected: PASS.
- [ ] **Step 6: Commit** `feat(wheel): overlap table format, loader and the accepted groups`.

### Task 2: Deduplication (one entry)

**Files:**
- Create: `core/wheel/overlap/Dedup.java`
- Test: `DedupTest`

**Interfaces:**
- Produces: `interface DedupItem { PowerRef ref(); }`, `Dedup.apply(List<E> entries, OverlapTable): Result<E>` where `Result` has `shown: List<E>` and `hidden: Map<E, OverlapGroup>`. First the chains collapse (only the highest tier owned stays); then the groups apply in file order.
- Rules: a group applies only when at least two of its members are present among the entries that are still shown; the shown entry is the main member when the player has it, else the next present member in file order; a missing main never hides the others; an entry already hidden by an earlier group stays hidden and does not take part in a later one; **a skill with modes is matched as a whole power only**, never by one of its modes (owner, 2026-11-22), so it is never hidden because a mode resembles another mod's power.

- [ ] **Step 1: Write the failing tests.** Seeds: `m01OneEntryWhenBothSidesPresent`, `m01SpellIsAThirdMemberAndIsHidden` (Invisibility spell is hidden for a vampire), `m01OnlyOneSidePresentChangesNothing`, `m01MainMissingFallsBackToTheOtherSide`, `m06FakeSidesDedupLikeM01` (a vampire and a werewolf side only meet in a fake: research risk 7), `noStackingOfTheGroupAcrossThreeMembers` (a three-member group shows one), `onlyTheHighestChainTierIsShown`, `chainMemberMatchesAGroupMemberOfItsChain`, `skillWithModesIsNeverHiddenByAModeMatch`, `aRefInTwoGroupsIsHiddenOnceAndStaysHidden` (the race ability under M03 then M12), `emptyTableChangesNothing`, `dedupIsStableAcrossRuns` (same input, same output order), `looseMatchesStaySeparate` (a pair from research section 3 is listed twice with the production table).
- [ ] **Step 2: Run** `./gradlew test --tests "*DedupTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `Dedup`.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): rule C deduplication`.

### Task 3: The shared cooldown, lasting actions included

**Files:**
- Create: `core/wheel/overlap/GroupClock.java`, `StackRules.java`
- Test: `GroupClockTest`, `StackRulesTest`

**Interfaces:**
- Produces: `enum SideEvent { ON, OFF, FIRED }`; `GroupClock` (immutable): `GroupClock.idle()`, `onEvent(String groupId, int side, SideEvent e, boolean lasting, int mainCooldownTicks, long now): GroupClock`, `cooldownLeft(String groupId, long now): int`, `activeSide(String groupId): OptionalInt`, `CODEC` (persisted as an attachment, `now` is game time, so a relog keeps the end tick). `StackRules.mayStart(GroupClock, String groupId, int side, long now): Optional<LockReason>` (`GROUP_ACTIVE` while another side is on, `GROUP_COOLDOWN` while the clock runs; append both to `LockReason` and to the lang file).
- Rules: an instant side (`FIRED`) starts the group cooldown at once. A **lasting** side starts **nothing** on `ON` (only `activeSide` is set, which blocks the others) and starts the cooldown on `OFF`, whether the player turned it off early or the duration ended, and the length is **the main side's own cooldown whichever side was active** (owner, 2026-11-22); `mainCooldownTicks` comes from a `CooldownSource` per source: Vampirism and Werewolves `IAction.getCooldown(player)`, ours `ExtraActions.Action.cooldownTicks(p)`, Tensura the value read from the instance right after the press, Infinity none; `0` means the group has no cooldown. `OFF` for a side that is not the active one is ignored (no double start). Dying or logging out while on is an `OFF` through the same path.

- [ ] **Step 1: Write the failing tests** (research section 6, risk 1). `instantStartsCooldownAtOnce`, `lastingStartsNothingOnOn`, `lastingStartsCooldownAtOff`, `earlyOffStartsCooldownAtThatTick`, `offOfAnInactiveSideIsIgnored`, `onWhileAnotherSideIsOnIsRefusedGroupActive`, `fireDuringCooldownIsRefusedGroupCooldown`, `endOfTheNonMainSideStartsTheMainSidesCooldown` (a 600 tick main and a 200 tick other side: the group gets 600), `mainWithNoCooldownMeansNoGroupCooldown`, `cooldownSurvivesACodecRoundTrip`, `relogMidLastingThenOffStartsExactlyOneCooldown`, `twoGroupsDoNotShareAClock`.
- [ ] **Step 2: Run** `./gradlew test --tests "*GroupClockTest" --tests "*StackRulesTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** both (the clock is a small map of `groupId -> (activeSide, cooldownEnd)`).
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): group clock with lasting actions`.

### Task 4: Enforce it on every server path

**Files:**
- Create: `parts/hud/wheel/WheelGate.java`, `VampirismGate.java`, `TensuraGate.java`
- Modify: `core/attachment/ModAttachments.java` (`WHEEL_CLOCK`), `WheelService.java`, `ActionService.java`, `WheelCatalogue.java` (dedup and the group fields), `WheelEntry.java` (adds `String groupId`, empty when the power is in no group), `HudNet.java` (`VERSION = "3"`), `HudConfig.java` (`wheel_dedup_enabled`, `wheel_stack_guard_enabled`), `AdminEntries.java`, `CHANGELOG.md`
- Test: `parts/hud/gametest/WheelOverlapGameTests.java`

**Interfaces:**
- Produces: `WheelGate.check(ServerPlayer, PowerRef, int mode): Optional<LockReason>` (reads the table through `OverlapTable`, the clock from the attachment); `WheelGate.record(ServerPlayer, PowerRef, int mode, SideEvent, boolean lasting)` (the main side's cooldown comes from `CooldownSource`). `VampirismGate.install()`: a NeoForge listener on `ActionEvent.ActionActivatedEvent` that **cancels** when `check` refuses and records `ON`/`FIRED` (and sets nothing else), and on `ActionDeactivatedEvent` that records `OFF` and calls `setCooldown(max(event.getCooldown(), group cooldown left))`, so a lasting action's own end-cooldown is at least the main side's. `TensuraGate.install()`: a listener on `SkillEvents.ACTIVATE_SKILL` that returns `EventResult.interruptFalse()` when `check` refuses, and a `SkillToggleEvent` listener that records `ON`/`OFF`. Both gates also run for a press from a slot or from Tensura's own UI, since they sit below those paths.
- Settings: `wheel_dedup_enabled` (BOOL, true), `wheel_stack_guard_enabled` (BOOL, true: off means no gate cancels anything and the clock is not read), each with an Admin row.

- [ ] **Step 1: Write the failing GameTests** (Vampirism for the vampire side, the clock tested through a second fake side registered by the test; skip in runs 3, 9). `secondSideIsRefusedWhileTheFirstIsOn`, `firstSideOffStartsTheGroupCooldownOnTheSecond`, `lastingEndCooldownIsRaisedToTheGroupFloor`, `stackGuardOffAllowsBoth`, `dedupOffListsBoth`, `dedupHidesTheNonMainEntryInTheCatalogue`, `slotAndWheelShareTheSameGate`, `tensuraSideIsRefusedFromAnyKey` (press through `startHoldSkill` directly), `playerDiesWhileLastingStartsOneCooldown`, `clockSurvivesRelog`.
- [ ] **Step 2: Run** run 1. Expected: FAIL.
- [ ] **Step 3: Implement** the gates; call `WheelGate.check` first in `WheelService.use` and `ActionService.triggerById/trigger` (a refusal is one action-bar line with the lock's lang key); install the gates from `HudPart`; apply `Dedup` in `WheelCatalogue.build` when `wheel_dedup_enabled`.
- [ ] **Step 4: Run** runs 1, 2, 3, 9. Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): shared cooldown and no stacking on every server path (hud network 3)`.

### Task 5: W2 phase gate

- [ ] **Step 1: Matrix** into `matrix-w2.log`. Expected: 13 green, last `DONE`.
- [ ] **Step 2: Mutation targets:**
  1. `GroupClock.onEvent`: start the cooldown at `ON` for a lasting side -> `lastingStartsNothingOnOn`.
  2. `GroupClock.onEvent`: ignore `OFF` of the active side -> `lastingStartsCooldownAtOff`.
  3. `Dedup.apply`: match a skill by one of its modes -> `skillWithModesIsNeverHiddenByAModeMatch`.
  3b. `GroupClock.onEvent`: use the ending side's own cooldown -> `endOfTheNonMainSideStartsTheMainSidesCooldown`.
  3c. `Dedup.apply`: skip the chain collapse -> `onlyTheHighestChainTierIsShown`.
  4. `OverlapTable.parse`: accept `loose` -> `looseGroupRejected`.
  5. `VampirismGate`: do not cancel -> `secondSideIsRefusedWhileTheFirstIsOn`.
  6. `TensuraGate`: return pass -> `tensuraSideIsRefusedFromAnyKey`.
  7. `WheelCatalogue.build`: skip dedup -> `dedupHidesTheNonMainEntryInTheCatalogue`.
- [ ] **Step 3: Settings and docs.** Admin rows for `wheel_dedup_enabled` and `wheel_stack_guard_enabled`; `docs/balance.md` lines; `docs/parts/hud.md` documents the file format and "how to add an approved pair" (add the row, run `OverlapTableTest`, no code). CHANGELOG: Added (rule C), Changed (hud network 3, **update the server and every client together**), Rolling back.
- [ ] **Step 4: Report:** the production table has the accepted rows (owner, 2026-11-22); the `infinity:soul.regen` hazard is a known limit (W5 T3), not handled here; Infinity has no member in the table; a main side with no readable cooldown (Tensura) uses the value read after its press. Open the PR.

---

# Phase W3: Figma mockups (owner approval gate)

Branch `docs/wheel-w3-mockups`. No game code. Figma file `USFBf74c4I2BWDyfmRxFZO`, a **new page** "Skill wheel". No payload, no setting (nothing to add to Admin or Settings: the settings of W4 are listed in the README so the owner sees them). The matrix still runs on the head (the rule), expecting the same results as main.

### Task 1: Read the file and the kit before drawing

- [ ] **Step 1:** Load `/figma-use` (mandatory before `use_figma`). `create_session` for the file key.
- [ ] **Step 2:** `get_metadata` on the file; list its pages and the plan-16 frames (Grimoire, Blood Moon, Tensura, HUD). `search_design_system` for the existing colour variables, text styles and the 32 px skill icon cells. **Reuse them; create no new colour that a theme already has.**
- [ ] **Step 3:** Write down, in `docs/superpowers/design/skill-wheel/README.md`, the frame size every state uses: 480 x 270 GUI (1080p at GUI scale 4, the size the kit's layouts are checked at), plus a 1920 x 1080 context frame per theme.

### Task 2: Shared parts and the Grimoire, six states

- [ ] **Step 1:** Create the page, the section per theme, and components: category tab, power medallion (32 px icon, frame, state ring), cooldown sweep, "on" glow, locked overlay with reason chip, mode caption, mode list popup, slot picker chip row (six slots with the keys R F V Z X C), aim hint banner, "Open Great Sage" entry, tooltip.
- [ ] **Step 2:** Draw the six states in the Grimoire. The wheel is an **onion of three rings** (owner, 2026-11-22): **inner** = the mods, **middle** = the groups of the hovered mod (drawn only when it has more than one: Tensura shows Skills, Magic, Battlewill; Vampirism shows no middle ring), **outer** = the powers of the hovered group. States: **closed** (the HUD's slots only, with the G hint), **open** (draw it twice: Tensura hovered with all three rings and Magic open, and Vampirism hovered with two rings; the outer ring with 12 entries and with 40 entries, showing the **paging**: ring-end arrows, a page marker "2 / 4", and the scroll rule; entries with levels show a level bar), **Alt held** (each moded entry shows its mode list; Alt changes nothing else), **aiming** (the hint "Click to release · Right-click to cancel", crosshair free), **locked** (greyed entry, reason on hover; one for a silenced spell), **slot picker** (six slots popped up at the entry; the Tensura variant shows the active and next preset columns).
- [ ] **Step 3: Verify** with `get_screenshot` of each frame. Check by eye and write in the README: no text under 8 px, state never by colour alone (each state also has a mark or shape), focus ring visible on every focusable, the narrow-window case (480 x 270) has nothing off-screen.

### Task 3: Blood Moon and Tensura, six states each

- [ ] **Step 1:** Repeat Task 2 Step 2 for the Blood Moon and for Tensura. Each theme has its own geometry and ornament from its own kit (no recolour of the Grimoire: an anti-goal). Hand-built from the kit's parts, not generated.
- [ ] **Step 2: Verify** the same list. A third reviewer pass: put the three "open" frames side by side; they must not read as one layout in three colours.

### Task 4: Numbers, annotations, exports

- [ ] **Step 1:** In each frame add an annotation layer and copy the numbers into the README table: inner, middle and outer radius per theme (the three ring widths), segment angle for 6, 12 and 24 entries, entry hit box, the page size (12) and the page arrows' size and place, the scroll-precedence table of the Global Constraints drawn as a legend, slot picker offset, animation durations (open, hover, aim banner) and the static fallback for "Reduce motion", focus order, the narration string for each state, and a contrast table with measured ratios (text 4.5:1, focus ring 3:1).
- [ ] **Step 2:** Export the 18 frames as PNG into `docs/superpowers/design/skill-wheel/<theme>-<state>.png`; list the Figma node ids and the file URL in the README.
- [ ] **Step 3:** The README ends with "Owner decisions needed": where the G hint sits, Infinity's `soul_control` G clash, the order of the inner ring, and whether a page change animates (static under Reduce motion either way). Paging itself is decided (owner, 2026-11-22).

### Task 5: Gate

- [ ] **Step 1:** Run the matrix on the head into `matrix-w3.log`. Expected: identical to main.
- [ ] **Step 2:** Open the PR "docs: skill wheel mockups". **W4 does not start until the owner has approved each theme in a PR comment**; record the approval and the owner's answers at the top of the README.
- [ ] **Step 3: Report** which mockup choices the owner must make. Mutation targets: none (no code); the check that can fail is the README's number table, which W4 T1 turns into tests.

---

# Phase W4: the wheel screen in three themes

Branch `feature/wheel-w4-screen`, from W2 (W3 only supplies the README numbers). Client only: no payload change (network stays `"3"`). Settings (client, `PlayerSettings` rows in the `hud` section): `wheel_tap_ms` (INT 100 to 500, default 200), `wheel_remember_last` (BOOL, true). "Reduce motion" reuses the hub's existing `reduce_motion` through `ReduceMotion.on()`.

### Task 1: Geometry from the approved numbers

**Files:** Create `core/wheel/WheelLayout.java`; Test `WheelLayoutTest`.

**Interfaces:** `WheelLayout.PAGE_SIZE = 12`. `WheelLayout.compute(int screenW, int screenH, int guiScale, Look look, int mods, int groups, int entryCount, int page): Layout` where `groups < 2` means no middle ring; `Layout` has `ring(Ring)` (`INNER`, `MIDDLE`, `OUTER`) returning `List<Cell(x, y, w, h, index)>`, `pageCount()`, `pageArrows(): List<Cell>`, `pickerAnchor(int index)`; `Layout.hit(int x, int y): Optional<Hit(Ring, int index)>`, `Layout.neighbour(Ring ring, int from, Radial.Dir dir): int` (built on `Radial.neighbour`; Left/Right move along the ring, Up/Down step to the ring inside or outside, and Left/Right past the first or last cell of the outer ring turn the page). `enum Look { GRIMOIRE, BLOOD_MOON, TENSURA }`; the constants are copied from `docs/superpowers/design/skill-wheel/README.md`.

- [ ] **Step 1: Write the failing tests** from the README numbers: `sixTwelveAndTwentyFourEntriesMatchTheMockup` (each cell centre within 1 px of the README's list), `nothingLeavesTheScreenAt480x270`, `noCellsOverlap`, `hitReturnsTheRingAndCellOrEmpty`, `oneGroupHasNoMiddleRing`, `threeGroupsHaveAMiddleRing`, `fortyEntriesAreFourPages` (12, 12, 12, 4), `exactlyTwelveEntriesIsOnePageAndNoArrows`, `pageClampsAtTheFirstAndLastPage`, `arrowKeysReachEveryCellOfEveryRing`, `leftPastTheLastOuterCellTurnsThePage`, `emptyListHasNoCells`.
- [ ] **Step 2: Run** `./gradlew test --tests "*WheelLayoutTest"`. Expected: FAIL.
- [ ] **Step 3: Implement.** **Step 4: Run.** Expected: PASS. **Step 5: Commit** `feat(wheel): layout from the approved mockups`.

### Task 2: The G machine (hold, tap, sticky)

**Files:** Create `core/wheel/GestureMachine.java`; Test `GestureMachineTest`.

**Interfaces:** `GestureMachine` (immutable, `step(Input, long nowMs, int tapMs): Step`). `Input`: `G_DOWN`, `G_UP`, `CLICK_LEFT`, `CLICK_RIGHT`, `ESC`, `SCREEN_OPENED`, `FOCUS_LOST`. `Step.effect()`: `NONE`, `OPEN`, `CLOSE`, `USE_HOVERED`, `REPEAT_LAST`. Rules (spec): held past `tapMs` opens, release over a hovered power uses it, release over nothing leaves it open until a click, G or Esc; a tap under `tapMs` opens nothing and repeats the last power; G again or Esc closes.

- [ ] **Step 1: Write the failing tests** with exact times (`tapMs = 200`): `tapUnder200RepeatsLastAndOpensNothing`, `heldPast200OpensWhileHeld`, `releaseOverAPowerUsesIt`, `releaseOverNothingStaysOpen`, `gAgainClosesAStickyWheel`, `escClosesInEveryState`, `exactly200IsAHold`, `screenOpenedWhileHeldReleasesWithoutAnEffect`, `focusLostWhileHeldCloses`, `gUpWithoutGDownIsIgnored`, `tapMsFromSettingIsUsed` (100 and 500).
- [ ] **Step 2 to 5:** run red, implement, run green, commit `feat(wheel): hold/tap machine`.

### Task 3: Clicks, modifiers, modes

**Files:** Create `core/wheel/ClickPlan.java`, `ModeCycle.java`; Test `ClickPlanTest`, `ModeCycleTest`.

**Interfaces:** `ClickPlan.of(int button, boolean alt, boolean shift): Action` with `enum Action { USE, SLOT_PICKER, USE_SECOND_OPTION, MODE_NEXT, MODE_PREVIOUS }`; `ClickPlan.scroll(double delta, boolean alt, ScrollTarget over): Action` (`LEVEL_UP`, `LEVEL_DOWN`, `MODE_NEXT`, `MODE_PREVIOUS`, `PAGE_NEXT`, `PAGE_PREVIOUS`, `NONE`), with `record ScrollTarget(boolean overOuterRing, boolean overPower, boolean hasLevels, boolean hasModes, boolean pageable)`. **Precedence (owner, 2026-11-22):** Alt held and the hovered power has modes -> mode; else, no Alt and the hovered power has levels -> level; else, no Alt, over the outer ring and `pageable` -> page; else `NONE`. Alt never pages and never changes a level. `ModeCycle.next(int current, boolean[] unlocked, boolean backwards): int` (skips locked, `-1` when none).

- [ ] **Step 1: Write the failing tests** (the spec's rules): `leftUses`, `rightOpensTheSlotPicker`, `shiftLeftIsTheSecondOption`, `altLeftIsNextMode`, `altRightIsPreviousMode`, `altScrollMovesEitherWay`, `plainScrollChangesALevelOnlyWhenThePowerHasLevels`, `altWithoutModesDoesNothing` (Alt over a power without modes is `NONE`, not a page), `levelBeatsPagingOverAHoveredVariablePower`, `plainScrollOverEmptyOuterRingPagesBothWays`, `plainScrollOverAPowerWithoutLevelsPages`, `scrollOverInnerOrMiddleRingDoesNothing`, `scrollOnAOnePageGroupDoesNothing`, `altNeverPages`, `lockedModesAreSkippedBothWays`, `allLockedReturnsMinusOne`, `wrapsAround`.
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 4: Last power, client state, Great Sage, aim mode

**Files:** Create `parts/hud/client/wheel/LastPower.java`, `WheelState.java`, `AimMode.java`, `GreatSageProvider.java`; Modify `HudClientConfig.java`, `PlayerSettings.java`; Test `LastPowerTest`, `AimModeTest`.

**Interfaces:** `LastPower.load(Path)`, `LastPower.get(): Optional<PowerRef>`, `LastPower.set(PowerRef, int mode)` (a file `config/supernaturalbloodlines-wheel-last.json` through the existing `RecordFile`; unreadable means none, never a crash; honours `wheel_remember_last`). `AimMode.start(PowerRef, int mode)`, `confirm()` (sends `UsePowerPayload(ref, mode, crosshairTargetId, false)`), `cancel()` (right click, Esc, or the owner's G tap), `AimMode.active()`, hint `supernaturalbloodlines.wheel.aim.hint` = "Click to release · Right-click to cancel". `GreatSageProvider` (reflection, client only, one `Probe` at startup, logs once and disappears if a method is missing): entries Magic Sense (toggle, `MagicSense.toggle`), Assess (toggle, `AssessMode.toggle`), Scan (held, `AssessMode.press/release`), Thought Acceleration (instant; the call site is not verified: the entry is listed only if the probe finds a handler, else it is omitted and the log says why), Voice omitted, and "Open Great Sage" (`GreatSageKeys.menu()`). It lists nothing unless `GreatSageRules.allows(player)` is true (the `requireGreatSage` gamerule).

- [ ] **Step 1: Write the failing tests:** `lastPowerSurvivesAReload`, `corruptLastPowerFileMeansNone`, `lastPowerOffSettingNeverWrites`, `aimConfirmSendsOnePayloadAndEnds`, `aimCancelSendsNothing`, `aimIsCancelledWhenTheScreenOpens`, `aimedPowerFromASlotStartsAimInsteadOfFiring` (`HudInput` plan: a slot whose power is AIMED starts `AimMode`; a HELD Tensura skill keeps its press/release flow), `heldReleaseIsAlwaysSent` (focus lost, screen opened, player died).
- [ ] **Step 2 to 5:** red, implement (the slot hook is in `HudInput.press`, guarded by the catalogue's kind), green, commit.

### Task 5: The screen, held skills, the slot picker

**Files:** Create `WheelScreen.java`, `SlotPicker.java`, `look/WheelLook.java`; Test `SlotPickerModelTest`, `HeldSkillTest`.

**Interfaces:** `WheelScreen(Look look)` (no pause: `isPauseScreen() == false`). Left click on a HELD power sends `UsePowerPayload(..., release=false)` and sends `release=true` when the button goes up, wherever the pointer is (also on close, focus loss, death). `SlotPicker`: `slotsFor(PowerEntry, Mode current): List<Slot>` (six slots; for a Tensura power the picker shows the active and next preset columns), `assign(int slot)`: for SLOT target LOADOUT send `SetLoadoutPayload(slot, id)`; for TENSURA_PRESET send Tensura's own `RequestAbilityModeChangePacket.changeAbilityPacket(slot.slot(), slot.preset(), id, mode)` using `SlotPlan.tensura`; then switch `HudClientConfig.Mode` to the power's mode (TENSURA or SUPERNATURAL). A slot key pressed over a hovered entry does the same as clicking that slot. Target NONE (stones, Great Sage): a hint line, nothing sent.

- [ ] **Step 1: Write the failing tests:** `slotKeyOverAnEntryAssignsThatSlot`, `tensuraAssignUsesTheNextPresetForTheRightColumn`, `assignSwitchesTheHudToThePowersMode`, `stoneAndGreatSageHaveNoSlotPicker`, `releaseIsSentOnceOnButtonUp`, `releaseIsSentWhenTheScreenClosesWhileHeld`.
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 6: Keyboard and narration

**Files:** Create `core/wheel/WheelNarration.java`; Modify `WheelScreen.java`; Test `WheelNarrationTest`.

**Interfaces:** `WheelNarration.lines(String mod, String group, String power, String mode, String state, String lockReason, int index, int total, int page, int pages): List<String>` (mod, group only when the mod has more than one, "page 2 of 4" only when paged, then "n of total", power, mode, state in words: "on", "ready", "cooling down 12 seconds", "locked: <reason>"). Keys: arrows (Left/Right along a ring, Up/Down between rings, via `WheelLayout.neighbour`) and Tab (Tab and Shift+Tab step mods, then groups), Enter uses, a slot key assigns, Esc closes; `updateNarratedWidget` says the lines on focus change only.

- [ ] **Step 1: Write the failing tests:** `narrationNamesCategoryPowerAndStateInOrder`, `stateIsWordsNotColour`, `lockedReasonIsRead`, `modeIsReadWhenThereIsOne`, `tabWalksModesThenGroupsThenWraps`, `groupIsOmittedForAOneGroupMod`, `pageIsReadOnlyWhenPaged`, `enterOnALockedEntryDoesNothingAndSaysWhy`.
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 7: The three looks and their sprites

**Files:** Create `look/GrimoireWheel.java`, `BloodMoonWheel.java`, `TensuraWheel.java`; Modify `shared/hub/kit/KitSprites.java`, `tools/gen_icons.py` (only if new icon cells are needed), the three kit sprite folders; Test `WheelContrastTest`, `WheelTextCacheTest`, `KitSpritesTest` (extended).

- [ ] **Step 1: Write the failing tests:** `WheelContrastTest` runs `ThemeContrast.assertReadable` on each look's text pairs (entry name, mode caption, locked reason, hint, tab) at 4.5:1 and the focus ring at 3:1; `everyNewSpriteNameIsValidAndExists` (each theme has each new part); `textIsMeasuredOncePerEntryChange` (the cache counter does not grow over 1000 frames); `reduceMotionDrawsStatic` (the animation clock is not read when `ReduceMotion.on()`); `noLookDrawsOutsideTheScreenAt480x270`.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** each look with `UiKit.draw`, `KitDraw.medallion/icon/tickRing`, `PieSweep` (cooldown), `TextCache`, `LabelFit`; new parts exactly as in the approved README (ring band for each of the three rings, segment frame, hover glow, locked overlay, picker chip, page arrow, page marker), drawn by hand in the kit's style, never generated. A look never reuses another theme's frame.
- [ ] **Step 4: Run** `./gradlew test`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(wheel): three looks on the art kit`.

### Task 8: Wire it: key, open, rings, entries

**Files:** Modify `HudKeys.java` (`WHEEL`, `GLFW_KEY_G`, `KeyConflictContext.IN_GAME`, registered), `HudInput.java` (G events into `GestureMachine`, only while `HudClientConfig.hudEnabled()`), `WheelScreen.java` (sends `WheelViewPayload(true/false)`, reads the catalogue), `ClientCatalogue`, `HudClientConfig.java`, `PlayerSettings.java`, `HudKeyDefaults.java`; Test `HudKeysDefaultsTest` (extend), `WheelWiringTest`.

- [ ] **Step 1: Write the failing tests:** `wheelKeyDefaultsToG`, `wheelKeyIsInOurCategory`, `replacedLeapIsHiddenWhileSprintLeapIsOn` (`ReplacedPowers` applied on the client list), `categoriesComeFromTheCatalogueAndGreatSage`, `emptyCategoryNeverShows`, `middleRingOnlyForTensuraWithMoreThanOneGroup`, `hoveringAModChoosesTheGroupsAndTheFirstGroupIsOpen`, `groupHiddenWhenEmpty`, `noWheelWhenTheServerHasNoChannel` (`HudSend.canSend(WheelViewPayload.TYPE)` false: G does nothing and says so once).
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 9: The Loadout page reads the catalogue

**Files:** Modify the hub Loadout tab (`LoadoutTabModel` callers), `docs/parts/hud.md`; Test `LoadoutTabModelTest` (extend).

- [ ] **Step 1: Write the failing test** `loadoutPageListsTheSameLoadoutPowersAsTheWheel` (the entries with `slotTarget == LOADOUT`).
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 10: Turn the covering on

**Files:** Modify `WheelBuild.screenPresent()` (returns `true`), `KeyCover.java`, `docs/parts/hud.md`; Test `CoveredKeyIdsTest` (extend).

- [ ] **Step 1: Write the failing tests:** `wheelIdsCoveredNowThatTheScreenExists` (including `keys.vampirism.vision` and `tensura.keybinding.race_ability`), `hudPartOffRestoresEveryWheelKey` (`registerRestoreOnly` path), `serverWithoutTheChannelRestoresThem` (`CoverPolicy.cover(.., serverHasHud=false)`), `modOffRestoresItsKeyAlone`.
- [ ] **Step 2 to 5:** red, flip the flag, green, commit `feat(wheel): cover the other mods' skill keys`.

### Task 11: Dev shots, in-game check, W4 gate

- [ ] **Step 1:** Add `-Dsupernaturalbloodlines.devshots=wheel:<theme>:<state>` to `DevShots` (states `closed, open, alt, aim, locked, picker`; demo data from `HudDemo`; client only, inert without the property). Test: `DevShotsGateTest` still green; a new `WheelShotsParseTest` for the property.
- [ ] **Step 2: Owner-game check, then shots** (the rules in Global Constraints): `Get-Process javaw | Where-Object Path -like '*curseforge*'`. Owner playing: skip the shots, write "shots deferred: owner game open" in the PR, and finish with the unit tests and `-JavaOnly` GameTest runs. Not playing: copy `New World`, then `pwsh scripts/dev/focus-watch.ps1 -JavaOnly -Run '.\gradlew.bat runSafeShots --no-daemon'` with `JAVA_TOOL_OPTIONS=-Dsupernaturalbloodlines.devshots=wheel:grimoire:open,... -Dsupernaturalbloodlines.devshots.scales=2,4 -Dsupernaturalbloodlines.devshots.demo=true -Dsupernaturalbloodlines.devshots.quit=true`, once per run, never in a loop. Compare each of the 18 pictures with its Figma frame; commit 6 per theme under `docs/superpowers/design/skill-wheel/shots/`.
- [ ] **Step 3: Matrix** into `matrix-w4.log`: 13 green.
- [ ] **Step 4: Mutation targets:**
  1. `GestureMachine`: treat `exactly 200` as a tap -> `exactly200IsAHold`.
  2. `GestureMachine`: open on `G_DOWN` -> `tapUnder200RepeatsLastAndOpensNothing`.
  3. `ClickPlan`: swap left and right -> `rightOpensTheSlotPicker`.
  3b. `ClickPlan.scroll`: page before level -> `levelBeatsPagingOverAHoveredVariablePower`; let Alt page -> `altNeverPages`.
  3c. `WheelCategories.groups`: always return the groups -> `middleRingOnlyForTensuraWithMoreThanOneGroup`.
  3d. `WheelLayout.PAGE_SIZE`: 13 -> `fortyEntriesAreFourPages`.
  4. `ModeCycle`: do not skip locked -> `lockedModesAreSkippedBothWays`.
  5. `SlotPlan` use in `SlotPicker`: ignore the column -> `tensuraAssignUsesTheNextPresetForTheRightColumn`.
  6. `AimMode.confirm`: send twice -> `aimConfirmSendsOnePayloadAndEnds`.
  7. `WheelScreen.removed()`: skip the held release -> `releaseIsSentWhenTheScreenClosesWhileHeld`.
  8. `WheelBuild.screenPresent`: false -> `wheelIdsCoveredNowThatTheScreenExists`.
  9. `WheelLook`: a text colour below 4.5:1 -> `WheelContrastTest`.
- [ ] **Step 5: Settings and docs.** `PlayerSettings` rows for `wheel_tap_ms` and `wheel_remember_last` (`AdminCoverageTest` green); a README line that the wheel follows `reduce_motion`. CHANGELOG: Added (the wheel, G, covered keys), Changed (the R wheel, Great Sage and Infinity wheel keys are unbound while the HUD works; restored when it does not), **no payload change**, Rolling back (`hide_covered_keys = false` or `hud_enabled = false` returns every key).
- [ ] **Step 6:** Open the PR "feat(wheel): W4 the wheel screen".

---

# Phase W5: Infinity Stones compat

Branch `feature/wheel-w5-infinity`, from W4. Client only, by reflection, no build dependency, his mod unchanged. No payload change. Setting (`PlayerSettings`, `hud` section): `infinity_compat_enabled` (BOOL, true).

### Task 1: The probe and the seam

**Files:** Create `client/wheel/infinity/StoneApi.java` (an interface of the calls we need), `InfinityCompat.java` (the reflective implementation and the switch); Test `InfinityCompatTest` (with a fake `StoneApi`).

**Interfaces:** `StoneApi`: `List<StoneEntry> entries()` (from `WheelClientState.get().entries()`), `boolean needsAim(int stone, String node)`, `boolean isSwitch`, `boolean isChannel`, `boolean isDual(String modeId)`, `Optional<Levels> levels(int stone, String node)`, `void use(int stone, String node, int phase)` (builds `WheelPayloads.Use(int, String, int)`, phase 0 use, 1 stop, 2 second option), `void setLevel(int stone, String node, int delta)` (`WheelPayloads.Setting`), `String stoneName(int)`. `InfinityCompat.probe(): State { ACTIVE, OFF_MISSING, OFF_BROKEN }`: resolves every class and method once; one missing or reshaped method logs **one** warning ("Infinity Stones compat off: <what>") and the state is `OFF_BROKEN`; the Tensura category then lists the stones as plain Tensura skills.

- [ ] **Step 1: Write the failing tests:** `missingModIsOffMissingAndSilent`, `brokenSignatureLogsOnceAndTurnsOff`, `offStateKeepsStonesInTheTensuraCategory`, `activeStateRemovesNamespaceInfinityFromTheTensuraCategory`, `settingOffBehavesAsMissing`.
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 2: One entry per stone, modes are its powers

**Files:** Modify `WheelState.java`, `InfinityCompat.java`; Test `StoneEntriesTest`.

**Interfaces:** one `PowerEntry` per carried stone (category "Infinity Stones", the one group `MAIN`, so **no middle ring**; icon his stone icon), `modeCount` = its unlocked nodes, current node remembered client-side per stone (his server takes the node explicitly, so no storage of ours). Kinds: switch -> TOGGLE (wheel stays open, glows), `needsAim` -> AIMED (our aim flow, then `use`), channel -> HELD-style start/stop by clicks (second choice stops it), else INSTANT. `Alt + click` cycles the node through `ModeCycle` over `unlocked`; plain scroll over a power with `levels` sends `setLevel`; `Shift + left` sends phase 2 when `isDual`. A stone not carried has no entries (category hidden). The entry's ref is the stone (`infinity:<stone>`), its current mode is the node. **Conflict to settle with the owner:** the research's Infinity members of M01, M02, M05 and M08 are single stone powers, that is modes inside a stone, and the owner ruled "no dedup for a mode inside a bigger skill" (2026-11-22). The production table therefore has no Infinity member, and those groups run between the other sides only.

- [ ] **Step 1: Write the failing tests:** `oneEntryPerCarriedStone`, `lockedNodesAreSkippedWhenCycling`, `needsAimStartsAimNotUse`, `switchStaysOpenAndShowsToggled`, `shiftLeftOnADualSendsPhaseTwo`, `plainScrollSendsALevelDeltaOnly`, `rightClickShowsTheNoSlotHint`, `stoneNotCarriedHidesTheCategory`, `staleStateIsNotUsed` (`receivedAt` older than 5 s while the wheel is open: entries are shown locked, not usable), `channelSecondChoiceStops`.
- [ ] **Step 2 to 5:** red, implement, green, commit.

### Task 3: Key covering, and the Known-Limits entry

**Files:** Modify `CoveredKeyIds.java` (already lists `key.infinity.wheel`), `wiki/Known-Limits.md`, `docs/parts/hud.md`; Create `docs/superpowers/research/infinity-soul-regen-bug-report.md`; Test `StoneKeysTest`, `CoveredKeyIdsTest`.

- [ ] **Step 1: Write the failing tests:** `stoneEntryIsNeverHiddenByTheOverlapTable` (production table with a carried stone: its entry stays), `keyInfinityWheelCoveredAndSoulControlLeftAlone`, `hudPartOffRestoresTheInfinityWheelKey`.
- [ ] **Step 2 to 5:** red, implement, green, commit. In the doc: `key.infinity.soul_control` defaults to G like the wheel; both are in game, so Controls shows a conflict; **owner decides** (default: left alone).
- [ ] **Step 6: Known limit (owner, 2026-11-22), not a fix.** Add to `wiki/Known-Limits.md` a row "Infinity Stones: turning off `soul.regen` clears every effect" (Reality/Soul power `soul.regen` ends with `effect clear @s`, which also removes Vampirism's and Werewolves' rage, our buffs and any potion; rule C's no-stacking cannot prevent it; the wheel shows no warning). Write the bug report for the friend's mod in `docs/superpowers/research/infinity-soul-regen-bug-report.md`: title, mod version `8.0.0-alpha.6`, steps (enable `soul.regen`, have Vampirism rage on, turn `soul.regen` off), expected (only the effects the power added are removed), actual (all effects cleared), suggested fix (remove only the effects the power applied: `regeneration` and `instant_health`). Do not change his mod.

### Task 4: W5 phase gate

- [ ] **Step 1: In-game check** under the owner-game rule (Global Constraints): needs the Infinity jar in `run-safeshots/mods` (copy from the pack, never write into it). Owner playing: skip, say so; otherwise one hidden run of the Tensura-theme `open` state with a fake stone entry from `HudDemo`.
- [ ] **Step 2: Matrix** into `matrix-w5.log`: 13 green (client code: GameTests unchanged).
- [ ] **Step 3: Mutation targets:**
  1. `InfinityCompat.probe`: log on every call -> `brokenSignatureLogsOnceAndTurnsOff`.
  2. `StoneApi.use` for dual: phase 0 instead of 2 -> `shiftLeftOnADualSendsPhaseTwo`.
  3. Stone entry filter: keep `infinity` entries in the Tensura category while ACTIVE -> `activeStateRemovesNamespaceInfinityFromTheTensuraCategory`.
  4. `ModeCycle` over stone nodes: ignore `unlocked` -> `lockedNodesAreSkippedWhenCycling`.
  5. Staleness check removed -> `staleStateIsNotUsed`.
  6. `OverlapTable` production file: add an `infinity/...` member -> `stoneEntryIsNeverHiddenByTheOverlapTable`.
- [ ] **Step 4: Settings and docs.** `infinity_compat_enabled` row in `PlayerSettings`. CHANGELOG: Added (stones on the wheel), no payload change, Rolling back (the setting off). Open the PR.

---

## Self-review

- **Spec coverage:** goals (categories, hide empty: W1 T1; click rules, slot picker: W4 T3, T5; three kinds, aimed flow for slots: W4 T4; locked, cooldown, mastery: W1 T3 to T5 and W4 T7; no duplicates: W1 T2 `ReplacedPowers`, W2); keys (G machine: W4 T2; covered and left-alone lists: W1 T9, W4 T10); constraints (trust: W1 T7, performance: W1 T8 and W4 T7, accessibility: W4 T6 and T7, UI-first: every phase gate, parts: Global Constraints and W1 T4, no pause: W4 T5); settled items (slot picker per mode and Tensura preset: W4 T5; modes: W1 T7 and W4 T3; unslotted skills: W1 T6; Great Sage menu: W4 T4; main side per pair: W2 T1); Infinity (W5). Mockups and approval: W3.
- **Owner answers of 2026-11-22:** onion rings, paging and scroll precedence (Global Constraints, W3 T2, W4 T1, T3, T6, T8); spells, arts and race ability (W1 T5, T6, T7); vision (W1 T4); the covered vision and race-ability keys (W1 T9, W4 T10); every dedup answer (W2 T1 to T4); `soul.regen` as a known limit (W5 T3).
- **Gaps closed here:** Great Sage's power list is client built (W4 T4); stones cannot be slotted (W5 T2); mode storage rule (Verified hooks); `getMaxHeldTime` is not a kind signal (W1 T5) and cuts unslotted held skills at 20 ticks (W1 T6).
- **Type consistency:** `PowerRef`, `PowerState`, `WheelEntry`, `UsePowerPayload(String ref, int mode, int targetEntityId, boolean release)`, `WheelGate.check(ServerPlayer, PowerRef, int)`, `GroupClock.onEvent(...)` are used with the same names in W1 to W5. `LockReason` is append-only (W2 adds `GROUP_ACTIVE`, `GROUP_COOLDOWN`).
- **Proportion:** the spec is about 110 lines; this plan is longer because it covers five PRs, but every step is a signature, a test name or a value, not a body.
