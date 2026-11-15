# Plan 13: The Supernatural HUD, the Bite Key, One Blood and One Keymap, Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One 6-slot ability HUD (Tensura mode / Supernatural mode, switched by Alt + Caps Lock) that replaces Tensura's skill HUD and the Tensura Slots mod; one **Bite action** in that HUD (Supernatural-mode slot 1 by default, so R; movable in the hub Loadout tab) that does the right thing for each form, including a hybrid in human form and a human in plan 11's turning; Vampirism's blood as the one blood value, drawn in Tensura's food spot; one keymap in which every key we cover is unbound and hidden, and can be restored; defaults that fit the OFA pack.

**Architecture:**
- A **new part** `HUD`. It has no required mods: Tensura, Vampirism and Werewolves are optional extras inside the part, the way Werewolves already is (`docs/parts.md`). Each extra is reached only through an adapter class that is touched after `ModList.isLoaded(...)`.

| Part | Config key | Package | Required mods | Required parts | Mixins |
|---|---|---|---|---|---|
| `HUD` | `hud` | `parts.hud` | — | — | **0 of 3**. Reserved, each used only after its task shows that no event or API works: (1) `AbilityStorageNextPresetMixin` (13b Task 2), (2) `VampireRaceFeedMixin` (13c Task 2), (3) `KeyBindsListHideMixin` (13a Task 4) |

- **Pure rules in `core/hud/`**, with no game classes and with unit tests: `BiteRules` (the decision table), `PresetPair` (N and N+1), `SlotMode` (which modes exist for the installed mods), `CoveredKeys` (record, unbind, restore).
- **Server decides, client shows.** Every bite and every Supernatural-mode trigger is a C2S payload, and the server validates it. The client only renders and forwards Tensura's own packets. Tensura's skill packets are already validated by Tensura and ManasCore.
- **Nothing is stored twice.** Tensura mode reads `IAbility` presets and `ManasSkillInstance` state on every frame. Supernatural mode stores only 6 ids (`hud_loadout` attachment): Vampirism/Werewolves action ids, plus our own pseudo-action `supernaturalbloodlines:bite`. Cooldown and active state come from Vampirism's `IActionHandler` on every frame (for Bite, from `BiteService`'s own 20-tick cooldown).
- **Bite is an action, not a key.** There is no Bite keybind. A new player's loadout has Bite in slot 1 (key R). When that slot fires, `BiteRules` picks what the bite does from the player's form. Bite exists only in Supernatural mode, and only with Vampirism installed (every row needs it). A Tensura vampire's race-ability feeding stays on Tensura's own `race_ability` key (`[` in the pack), untouched.
- **Parts never import each other.** The hub's Loadout tab reaches the HUD through `shared/hud/LoadoutAccess`, the name plan 12 reserved. Bite reaches plan 11's turning through `shared/supernatural/TurningFeed`, and the hybrid thirst through `shared/supernatural/HybridFeed`. The species part installs both, the way it installs `HumanLikeBlood`.

**Tech Stack:** NeoForge 1.21.1, Java 21, Vampirism 1.21-1.10.13 (API jar on the compile classpath, full jar at runtime), Werewolves 1.21-2.0.3.4 (full jar on the compile classpath), Tensura 2.0.1.5 / ManasCore 4.0.0.2, GameTests + JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-supernatural-hud-design.md`. This plan adds four things the spec predates:
- the hub's Loadout tab (plan 12) as the only place to assign slots;
- the Home hub key in the keymap;
- the turning bite;
- the OFA pack keymap.

## Verified hook points (javap, 2026-11-15)

Jars:
- `tensura-reincarnated-643695-9476869.jar`
- `manascore-619025-8022425.jar` (its inner `manascore-skill`/`keybind` jars)
- `Vampirism-1.21-1.10.13.jar` and `-api.jar`
- `Werewolves-1.21-2.0.3.4.jar`
- `tensuraslots-1.0.0.jar`, from the OFA instance's `mods` folder

| Need | What the jar has | Plan uses |
|---|---|---|
| Tensura's skill HUD | **Not a NeoForge GUI layer.** `mixin.client.MixinGui.renderPre` calls `OverlayHandler.renderHud(GuiGraphics)` at the start of `Gui.render`. `pre_renderAbilities()` returns early unless `HudConfig.abilities.render` (a public boolean) is true. The config comes from `ConfigRegistry.getConfig(HudConfig.class)` (ManasCore). `OverlayHandler.ABILITIES_LAYER` is a public `OverlayElement` | Set `abilities.render = false` in memory while our HUD is on, and put the player's value back when it is off. No mixin |
| Tensura's food spot | `HudConfig.decorations.food.render` (boolean). Public statics `OverlayHandler.getPositionsById(int)`, `getScaleById(int)`, `isLeftSide(int)` and the constant `FOOD_DECO`. `renderFoodDecoration` draws `FoodData.getFoodLevel()` with `tensura:textures/player_hud/decorations/food_full.png` | For a Vampirism vampire: `food.render = false`, then draw blood at the same position and scale in `RenderGuiEvent.Post` |
| Tensura presets | `TensuraStorages.getAbilityFrom(LivingEntity) -> IAbility`: `getActivePreset()`, `getPresets()`, `getAbilitySlot(preset, slot) -> AbilitySlot(getSkill(), getMode(), isEmpty())`. Tensura Slots pairs preset `(active + 1) % 9`, so there are **9 presets and preset 9 pairs with preset 1** (confirmed in its bytecode) | `PresetPair` |
| Tensura triggers | `TensuraKeybinds.onAbilityPressed(int)` / `onAbilityReleased(int)`: if `NEXT_ABILITY_MODE` (or `PREVIOUS_`) is down, it sends `RequestAbilityModeChangePacket.changeModePacket(slot, boolean)`; otherwise `SkillAPI.skillActivationPacket(skillId, keyNumber, mode)`. Release sends `SkillAPI.skillReleasePacket(skillId, keyNumber, heldTicks)`. ManasCore also has `skillTogglePacket(ResourceLocation)`. `ManasSkillInstance`: `getCoolDown(mode)`, `onCoolDown(mode)`, `isToggled()`, `canBeToggled(entity)`, `getMaxHeldTime(entity)`, `getModes()` | Our slot keys call the same methods in the same order. For the right column (preset N+1) the mode change uses the 3-argument `changeModePacket(preset, slot, boolean)`. Tensura Slots changes the mode only on the client, which is a bug we don't copy |
| Alt + scroll | Tensura's `PlayerInputHandler` already sends `RequestAbilityModeChangePacket.changePresetPacket(delta, boolean)` on scroll while `NEXT_ABILITY_MODE` is down. In the owner's live `options.txt`, `next_mode` is Left Alt | Nothing to build. The keymap keeps `next_mode` on Left Alt |
| Tensura Slots | One client mixin draws preset N+1 inside `OverlayHandler.renderAbilities`. A common mixin, `IsAbilityInActivePresetMixin`, injects at RETURN of the four `AbilityStorage.isAbilityInActivePreset(...)` overloads so that preset N+1 also counts as active. Three `ManasKeybinding`s: `tensuraslots.keybinding.secondary_ability.slot_1..3` | The pack removes the mod. Its `isAbilityInActivePreset` behaviour is what reserved mixin (1) would copy, **if** 13b Task 2 shows that Tensura refuses or drops a right-column skill without it |
| Vampirism actions | API `IFactionPlayer.getActionHandler()`: `getUnlockedActions()`, `isActionUnlocked`, `isActionOnCooldown`, `getPercentageForAction`, `isActionActive(ILastingAction)` / `(ResourceLocation)`, `toggleAction(action, IAction.ActivationContext) -> PERM`. `IAction.canUse(player) -> PERM`, `getCooldown`, `showInSelectAction`. `ActivationContext` is a 2-method interface (`targetBlock`, `targetEntity`). The client overlays `ActionCooldownOverlay`/`ActionDurationOverlay` exist, so the action state is on the client | Our `TriggerActionPayload` calls `toggleAction` on the server. Rendering reads the client handler. The werewolf player is an `IFactionPlayer` too |
| Vampirism bite | `ServerboundStartFeedingPacket(int entityId)` → public `VampirePlayer.biteEntity(int)`. Public `VampirePlayer.endFeeding(boolean)`. Both are in the **full jar only** (the compile classpath has the API jar) | Our server handler calls them through `MethodHandles.publicLookup()` on the runtime class of the API's vampire player. Looked up once; a miss logs once and the bite does nothing |
| Vampirism blood bar | `api.client.VIngameOverlays.BLOOD_BAR_ID` = `vampirism:blood_bar`, a NeoForge GUI layer. `IVampirePlayer.getBloodLevel()`, `getBloodStats()` | `RenderGuiLayerEvent.Pre` cancels that layer while Tensura is installed and our setting is on |
| Vampirism keys | `ModKeys`: `keys.vampirism.suck`, `.action`, `.select_skills`, `.vision`, `.action1..9`, `.minion_task`, `.select_ammo`, `.skill_screen` | We cover `suck` and `action1..9`. The action wheel (`action`) stays |
| Werewolves bite | `ServerboundBiteEventPackage(int entityId)` → public `WerewolfPlayer.bite(int)`, `canBite()`, `canBiteEntity(LivingEntity)`, `getForm()`. Keys: `keys.werewolves.bite`, `keys.werewolves.leap` | The server handler calls `WerewolfPlayer.bite(id)` directly (the jar is on the compile classpath). We cover `keys.werewolves.bite` |
| Hybrid thirst (plan 04, read in the repo) | `parts/species/hybrid/BloodThirst`: `BloodlineState.thirst()` 0–20, `GAIN` 5 per blood bottle, falls 1 per 1200 ticks; its javadoc says "Deferred: a bite as a source of blood". `HybridTraits.isHybrid(species)`. `Venom.onDamaged` (`LivingDamageEvent.Post`) poisons a Vampirism vampire victim when a werewolf-faction attacker is the **direct** entity of melee damage (R42) | Our hybrid feed adds `GAIN` thirst through `HybridFeed` and hurts the victim with `damageSources().playerAttack(p)`, so `Venom` fires by itself (no venom code of ours) |
| Tensura race feeding | `VampireRace.onActivateAbility(ManasRaceInstance, LivingEntity)` checks `RaceUtils.isBloodless`, then `FoodData.needsFood()`, then `FoodData.eat(int, float)`. No event | Not part of Bite: it stays on Tensura's `race_ability` key. Crediting Vampirism blood is 13c Task 2, the candidate for reserved mixin (2) |

**Not verified (each task checks before relying on it):**
- whether Tensura's server refuses `skillActivationPacket` for a skill outside the active preset (13b Task 2);
- the `keyNumber` that Tensura Slots passes for the right column: the bytecode shows the call, but not the constant;
- whether `NEXT_ABILITY_MODE` + scroll wraps preset 9 to 1 on the server;
- NeoForge's modifier priority for Alt + Caps against the plain Caps radio key (13a Task 4 checks it);
- Vampirism's API getter for "this mob has blood" (`entity_blood` data map / extended creature) for the hybrid feed (13a Task 2);
- whether ManasCore saves `HudConfig` to disk when Tensura's settings screen closes while we hold `render = false` (13b Task 2);
- how Controlling's search list rebuilds its entries (13a Task 4).

**OFA pack facts (read 2026-11-15):**
- The owner's live `options.txt` differs from `OFA-notes/KEYMAP.md` and `apply_keymap.py`, which are stale:
  - Tensura ability slots are R / F / V and Tensura Slots are Z / X / C. That matches the spec.
  - `tensura.keybinding.next_mode` is Left Alt, `race_ability` is `[`, `swapOffhand` is unbound and `greatsage.menu` is G.
- `configureddefaults/options.txt` still ships `swapOffhand` on F together with Tensura slot 2 on F, which is a clash for new players.
- **Vampirism, Werewolves and SupernaturalBloodlines are deliberately out of the pack** until this mod is done; the owner adds all three back at once. So their keys can't be read from that `options.txt`; their key names in this plan come from the jars. The pack task (13a Task 5) prepares the keymap and `configureddefaults` for that moment and installs nothing.

## Phases

Each phase is one mergeable PR.

| Phase | Content | Tasks | Needs merged first | Can run in parallel with |
|---|---|---|---|---|
| **13a** | the part, the Bite rules and server bite (hybrid feed, turning bite), the keymap and hidden keys, the pack keymap | 5 | plans 11 and 12 (the hub key, `HumanLikeBlood`) | — |
| **13b** | the slot model and loadout, the Tensura source, the Vampirism/Werewolves source, the HUD renderer and mode switch, the hub Loadout tab | 5 | 13a | 13c |
| **13c** | one blood value: the blood bar moved to Tensura's food spot, Tensura race feeding into Vampirism blood, player-ready defaults | 3 | 13a | 13b |

13b and 13c both edit `parts/hud/client/HudClient.java` (registration), `HudClientConfig`, `en_us.json`, `CHANGELOG.md` and `docs/parts/hud.md`. Resolve those conflicts by keeping both sides.

## Global Constraints

- **Mixins:** the existing parts are full or reserved, and none of them is touched. The `hud` part starts at **0 of 3**. A reserved mixin is added only after its task shows that no event or API works. It uses `require = 0`, has its own config switch and its own `@Pseudo`/string target, and the part's `HudMixinPlugin` applies each mixin only when that mixin's target mod is installed (Tensura for 1 and 2, Vampirism too for 2). Follow `PartMixinPlugin`. List every added mixin in `docs/mixins.md` in the same task.
- **Never change another mod's data.** We don't change skills, actions, cooldowns or presets. The only writes are Tensura's and Vampirism's own packets and API calls, and the in-memory `HudConfig` render flags. Those flags are put back exactly as they were (13b Task 2, 13c Task 1).
- **Never delete another mod's key.** A covered key is set to unbound only after its current bind is recorded in `config/supernaturalbloodlines-hud-keys.json`. Turning `hide_covered_keys` off, or turning the part off, restores the recorded binds.
- **Missing mods show nothing.** No Tensura means no Tensura mode, and Vampirism's blood bar stays where it is. No Vampirism means no Supernatural mode and no blood. With neither, the HUD does not render. With `-PnoLinkedMods`, the part loads, renders nothing, logs no ERROR, and its keys do nothing.
- **Client-only code behind Dist guards.** Everything under `parts/hud/client/` is registered from `HudPart` only inside `if (FMLEnvironment.dist == Dist.CLIENT)`, through a separate `HudClient.register(modBus)` class, as `parts/supernatural/client/HubKeybinds` does. Tensura client classes (`OverlayHandler`, `TensuraKeybinds`, `HudConfig`) are referenced only from `parts/hud/client/tensura/`, and Vampirism client classes only from `parts/hud/client/vampirism/`. Both are reached after `ModList.isLoaded`. A dedicated-server GameTest run must not load any of them; `runGameTestServer` is the check.
- **Every refusal explains itself** with a lang key under `supernaturalbloodlines.hud.*` (shown in the action bar or the Alt tooltip).
- **No per-tick scans, no per-frame allocation storms.** The HUD reads six slots per frame and builds nothing else. Server payloads are rate-limited to 10 per second per player.
- **GameTests where possible.** Everything on the server side gets GameTests: bites, the turning bite, triggers, loadout validation, and attachment copy on death. Pure rules get JUnit. Client-only behaviour gets pure unit tests where a pure core exists, plus the screenshot check.
- **Screenshots for visual tasks.** Use a real `runClient` with an F2 screenshot from `run/screenshots/`, attached to the PR, at GUI scale 2 and 4. **Only when the owner isn't playing:** before launching, check that no OFA instance `javaw` is running (`Get-Process javaw` shows no window with "OFA" or "Minecraft*" in the title). If one is running, skip the screenshot, say so in the PR, and leave the step unchecked.
- **Test runs.** All 9 must pass at the end of every task. Run them **one at a time**: they share `run/` and the `partsOff` config rewrite.
  1. `build`
  2. `runGameTestServer`
  3. `runGameTestServer -PnoWerewolves -PnoCurios`
  4. `runGameTestServer -PnoLinkedMods`
  5. `runGameTestServer -PpartsOff=species`
  6. `runGameTestServer -PpartsOff=speciesTensura`
  7. `runGameTestServer -PpartsOff=supernatural`
  8. `runGameTestServer -PpartsOff=supernaturalTensura`
  9. `runGameTestServer -PpartsOff=hud` (from 13a Task 1 on)
- **Docs in the same task as the code:**
  - a new `docs/parts/hud.md` (scope, optional mods, mixin budget, every hook point above);
  - `docs/parts.md` (table row);
  - `docs/mixins.md` (a `hud (0 of 3)` section);
  - `SPECIES-RULES.md` (the Bite table as rule R110, the turning bite as R20e, and a line under R42: a bite now also feeds a hybrid's thirst);
  - `CHANGELOG.md` (Unreleased);
  - the wiki: `wiki/Key-Binds.md`, a new `wiki/Ability-HUD.md` linked from `_Sidebar.md`, `wiki/Configuration.md`, `wiki/Known-Limits.md`, `wiki/Supernatural-Hub.md` (Loadout).

## Review Focus

1. **The Bite action, by form (R110).** Each of rows 1, 2, 3, T, 4 and 5 has a GameTest or unit row. A hybrid, Original hybrid or tribrid in human form feeds through our hybrid feed: thirst rises by 5, and a vampire victim is still poisoned (R42). A Tensura vampire who is also a Vampirism vampire takes row 2. Bite fires only from a HUD slot, never from its own key. Tests: `BiteRulesTest` (every row and every tie), plus `wolfFormBiteUsesWerewolves`, `vampireBiteStartsFeeding`, `hybridHumanFormFeedsThirst`, `hybridHumanFormPoisonsVampire`, `turningBiteCompletesTurning`, `turningBiteRefusesNonHumanLike` (13a Tasks 2 and 3), `biteFiresFromItsSlot` (13b Task 3).
2. **Keys are never lost.** Unbind, restart, switch off: the player's own earlier bind is back, not just the mod default. A key the player rebound while it was hidden is not overwritten. Tests: `CoveredKeysTest.restoresPlayersOwnBind`, `restoreSkipsKeysThePlayerRebound`, `secondRunRecordsNothingNew` (13a Task 4).
3. **Tensura input parity.** On a slot key: press, hold (held ticks reach `skillReleasePacket`), release, toggle skill, Alt + key mode change (on the right column too, applied on the server), and Tensura's held abilities keep working. Checked by hand in `runClient` against Tensura's own keys with Tensura Slots removed (13b Task 2).
4. **Tensura's config is never saved with our flags.** Open and close Tensura's settings screen while the HUD is on, quit, and switch the HUD off: `abilities.render` and `decorations.food.render` on disk are the player's own values (13b Task 2, 13c Task 1).
5. **Server validation.** A `TriggerActionPayload` for a slot whose id is not unlocked, belongs to another faction, or is on cooldown does nothing and says why. A `SetLoadoutPayload` with an unknown id is refused. A Bite trigger whose target is out of reach or in another level is refused. Tests in 13a Task 2 and 13b Tasks 1 and 3.
6. **Missing mods.** With `-PnoLinkedMods` and with `-PpartsOff=hud`, nothing crashes and no other part changes. With `-PpartsOff=hud`, every covered key is back.

---

# Phase 13a: the part, the Bite rules and the keymap

### Task 1: The `hud` part, its configs and the empty mixin plugin

**Files:**
- Modify: `core/Part.java`. Add `HUD("hud", "net.ofatech.supernaturalbloodlines.parts.hud.HudPart", List.of())`.
- Modify: `core/Config.java` (part switch), `build.gradle` only if `partsOff` needs the new key listed.
- Create: `parts/hud/HudPart.java` (server registrations; on the client, `HudClient.register(modBus)` behind `Dist.CLIENT`), `parts/hud/HudConfig.java` (common: `rate_limit_per_second` 10, `bite_reach` 4.0; and the reserved mixin switches `next_preset_active`, `tensura_feed_fills_blood`, `hide_in_controls_mixin`, all `true`), `parts/hud/client/HudClient.java` (empty), `parts/hud/client/HudClientConfig.java` (client toml: `hud_enabled` true, `hide_covered_keys` true, `hide_tensura_skill_hud` true, `blood_in_food_spot` true, `mode` `AUTO|TENSURA|SUPERNATURAL`).
- Create: `mixin/HudMixinPlugin.java` and `supernaturalbloodlines.hud.mixins.json` (empty `mixins`/`client` lists), registered in `neoforge.mods.toml`, following `PartMixinPlugin`. It already supports per-mixin mod checks (`shouldApplyMixin` by class name → required mod ids), so a later task only adds the class and one map entry.
- Test: update `PartLoaderTest` (`HUD` needs no mod; enabled under `-PnoLinkedMods`).

- [ ] **Step 1: Write the failing rows** in `PartLoaderTest`: `hudNeedsNoMod`, `hudOffBySwitch`.
- [ ] **Step 2: Run** `.\gradlew.bat test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs, one at a time. Under `-PnoLinkedMods`, the log shows `Part HUD: enabled`. Expected: PASS.
- [ ] **Step 5: Docs and commit.** Create `docs/parts/hud.md` (scope, "no required mods; Tensura, Vampirism and Werewolves are optional extras", mixin budget 0/3 with the three reserved candidates, and the verified hook table from this plan). Add rows to `docs/parts.md`, `docs/mixins.md` (`## hud (0 of 3)`: "No mixin yet; reserved: …") and `wiki/Parts-And-Compatibility.md`. Commit `feat(hud): hud part`.

### Task 2: The Bite rules (R110) and the server bite

Bite is an **action**, not a key. This task builds the rules and the server side. 13b Task 1 puts Bite in the loadout (slot 1 by default), and 13b Task 3 fires it from its slot. Until then, the GameTests call `BiteService` directly.

**Files:**
- Create: `core/hud/BiteRules.java` (pure). `decide(BiteContext) -> BiteAction { WEREWOLF_BITE, VAMPIRISM_DRINK, HYBRID_FEED, TURNING_BITE, NONE }`. `BiteContext` holds `werewolfFaction`, `wolfForm`, `vampirismVampire`, `hybridType` (hybrid, Original hybrid, tribrid: `HybridTraits.isHybrid`), `inTurning`, `turningWouldComplete`. First match wins. This is **R110** (owner, 2026-11-15: "by form"; hybrid ruling by the planner):

| # | When | Action |
|---|---|---|
| 1 | werewolf faction (werewolf, hybrid, Original hybrid, tribrid) **in a wolf form** | `WEREWOLF_BITE`: Werewolves' bite (turns humans R92, poisons vampires R42) |
| 2 | Vampirism vampire faction (a Ghoul too; a Tensura vampire who is a Vampirism vampire too) | `VAMPIRISM_DRINK`: Vampirism's blood drinking |
| 3 | hybrid, Original hybrid or tribrid **in human form** | `HYBRID_FEED`: our own feeding. It fills the hybrid's thirst (plan 04's `BloodlineState.thirst`), and hybrid venom still poisons a vampire victim (R42) |
| T | `inTurning && turningWouldComplete` and not a Vampirism vampire | `TURNING_BITE` (Task 3) |
| 4 | werewolf in human form with no vampire side | `WEREWOLF_BITE` |
| 5 | anyone else (including a Tensura vampire race with no Vampirism faction: it feeds with Tensura's own race-ability key, `[`) | `NONE`: the slot shows greyed with the reason |

  Hybrids are in the Werewolves faction, not Vampirism's vampire faction, so Vampirism's `biteEntity` can't serve them. That is why row 3 is our own feed.
- Create: `shared/supernatural/HybridFeed.java` (`isHybrid(ServerPlayer)`, `feed(ServerPlayer)`; false and a no-op without an installer).
- Modify: `parts/species/hybrid/BloodThirst.java`. Add `public static void addThirst(ServerPlayer p, int amount)` (capped at 20, through the existing `setThirst`), and drop "Deferred: a bite as a source of blood" from its javadoc. Modify `parts/species/SpeciesPart.java` to install `HybridFeed.set(p -> HybridTraits.isHybrid(...), p -> BloodThirst.addThirst(p, BloodThirst.GAIN))` next to `HumanLikeBlood.set`.
- Create: `parts/hud/bite/BiteService.java` (server): `press(ServerPlayer, int targetId)` and `release(ServerPlayer)`. It builds `BiteContext` from the player, and resolves the target in the same level, within `bite_reach`, in line of sight, alive and not self. Then by action:
  - `WEREWOLF_BITE` calls `WerewolfPlayer.get(p).bite(targetId)` after `canBite()` (only with Werewolves installed, behind `ModList`). The wolf form is `WerewolfPlayer.getForm()` not being the human form; verify the `WerewolfForm` getter in the jar.
  - `VAMPIRISM_DRINK` calls `biteEntity(int)` on press and `endFeeding(true)` on release, through `parts/hud/bite/VampirismBite.java` (`MethodHandles.publicLookup()` on the runtime class of `VampirismAPI`'s vampire player; one lookup, one warning if it's missing).
  - `HYBRID_FEED` (press only, one bite per 20 ticks): the victim must have blood by Vampirism's rule, meaning a mob with `entity_blood` > 0 or a player who isn't bloodless (`FactionRaceRules.isBloodless` through a shared hook; verify Vampirism's API getter). The victim takes 2 damage from `p.damageSources().playerAttack(p)`, so the player is the direct entity and `Venom.onDamaged` poisons a vampire victim by itself. Then `HybridFeed.feed(p)` (+5 thirst). A full thirst (20) is refused with a reason. With `-PpartsOff=species`, `HybridFeed` has no installer and row 3 never matches.
  - `NONE` sends the action-bar reason.
  - Venom (R42) and the bloodless refusal (`VampirePlayerBiteMixin`) still run on rows 1, 2 and 4, because those paths go through the mods' own bite methods.
- Modify: `en_us.json` (`supernaturalbloodlines.hud.bite.*`, including the action's name "Bite" and one tooltip line per `BiteAction`).
- Test: `src/test/.../core/hud/BiteRulesTest.java`, `parts/hud/gametest/BiteGameTests.java`.

- [ ] **Step 1: Write the failing tests.**
  - `BiteRulesTest`:
    - every row;
    - a tribrid in wolf form (row 1);
    - a hybrid, an Original hybrid and a tribrid in human form (row 3);
    - a Tensura vampire who is a Vampirism vampire (row 2);
    - a Tensura vampire race with no faction (row 5);
    - a human not in the turning (row 5).
  - GameTests (mock players, `setFactionLevel`):
    - `vampireBiteStartsFeeding`: a villager's blood drops and the vampire's blood rises within 40 ticks.
    - `wolfFormBiteUsesWerewolves` (skipped under `-PnoWerewolves`).
    - `hybridHumanFormFeedsThirst`: thirst goes 0 → 5, and the villager took 2 damage.
    - `hybridHumanFormPoisonsVampire`: the vampire victim has `hybrid_venom` (R42; skipped under `-PnoWerewolves`).
    - `hybridFeedRefusedWhenFull`, `hybridFeedRefusesBloodless`.
    - `biteOutOfReachRefused`, `biteOtherLevelRefused`, `nobodyBitesNothing`, `rateLimited`.
- [ ] **Step 2: Run** `test` and `runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.**
  - `SPECIES-RULES.md`: R110 (the table above), and under R42 "a hybrid's Bite in human form also feeds the thirst (+5)".
  - `docs/parts/hud.md` (the bite paths and the reflective Vampirism call).
  - `docs/parts/species.md` (the `HybridFeed` hook; the bite is no longer deferred).
  - `wiki/Hybrids-And-Original-Hybrids.md` (feeding by Bite).

  Commit `feat(hud): bite rules by form`.

### Task 3: The turning bite

**Files:**
- Create: `shared/supernatural/TurningFeed.java` (`canFeed(ServerPlayer)`, `feed(ServerPlayer)`; both no-op or false without an installer).
- Modify: `parts/species/turning/TurningHandler.java`. Make `wouldComplete` package-visible as is, and install `TurningFeed.set(p -> inTurning && wouldComplete(p), TurningHandler::drankHumanBlood)` from `SpeciesPart` next to `HumanLikeBlood.set`. No behaviour change.
- Modify: `parts/hud/bite/BiteService.java`. `turningWouldComplete = TurningFeed.canFeed(p)`. `TURNING_BITE` needs `HumanLikeBlood.test(target)` (plan 11's rule: villagers, hunters, human-like players; nothing in the vampire faction). On success: the victim takes 1 damage of a bite source (`supernaturalbloodlines:turning_bite`, a new damage type with the same tags as a player attack), the bite sound plays, and `TurningFeed.feed(p)` runs. A non-human-like target is refused with `supernaturalbloodlines.hud.bite.not_human`.
- Modify: `en_us.json`, the damage type JSON (gated with `part_enabled: hud`).
- Test: `parts/hud/gametest/TurningBiteGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.**
  - `turningBiteCompletesTurning`: a human in the turning bites a villager and becomes a vampire, with the "completed" message.
  - `turningBiteRefusesNonHumanLike`: a cow and a vampire villager are refused, and the turning continues.
  - `turningBiteWithoutSpeciesDoesNothing` (under `-PpartsOff=species`: row T never matches).
  - `ghoulInTurningUsesVampirismBite`: row 2, which already feeds the turning through `onDrinkBlood`.
  - `werewolfInTurningNotOffered`: `wouldComplete` is false, so row 4.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** R20e in `SPECIES-RULES.md` (under R20c/R20d: "a human in the turning may also bite a human-like creature with the Bite action of the HUD"), `docs/parts/species.md` (the new hook), `wiki/Vampires-And-Originals.md` (turning section). Commit `feat(hud): bite to finish the turning`.

### Task 4: Our keymap, covered keys and hiding them

**Files:**
- Create: `parts/hud/client/HudKeys.java`. All our HUD keys go in category `PackKeybinds.CATEGORY`, with conflict context `IN_GAME`:

| Key id | Default | Does |
|---|---|---|
| `key.supernaturalbloodlines.slot_1` / `_2` / `_3` | R / F / V | left column |
| `key.supernaturalbloodlines.slot_4` / `_5` / `_6` | Z / X / C | right column |
| `key.supernaturalbloodlines.hud_mode` | Alt + Caps Lock (`KeyModifier.ALT`) | Tensura / Supernatural mode |
| `key.supernaturalbloodlines.hub` | Home | existing, in the supernatural part; listed here, not moved |

  **There is no Bite key** (owner, 2026-11-15). Bite is an action in the HUD, in Supernatural-mode slot 1 (R) by default (13b Tasks 1 and 3), and Vampirism's `suck` and Werewolves' `bite` keys are covered by it. These keys stay with their owners and are not touched: Tensura `next_mode` (Left Alt: mode change on a slot, and Alt + scroll for presets), `race_ability` (`[` in the pack: a Tensura vampire's own feeding stays there), `main_gui`; and Vampirism's action wheel, vision and skill screen.
- Create: `core/hud/CoveredKeys.java` (pure). Input: a map of key id to current bind, the covered list and the recorded file. Output: the binds to set and the new record. Rules:
  - record only on first cover;
  - restore the recorded bind on switch-off;
  - leave alone a key the player rebound while it was covered (its bind is no longer unbound);
  - drop ids of mods that aren't loaded.
- Create: `parts/hud/client/KeyCover.java`. The covered list holds:
  - `tensura.keybinding.ability.slot_1..3` (only if Tensura is loaded);
  - `tensuraslots.keybinding.secondary_ability.slot_1..3` (if loaded);
  - `keys.vampirism.suck` and `keys.vampirism.action1..9` (if loaded);
  - `keys.werewolves.bite` (if loaded).

  It applies `CoveredKeys` on `ClientStartedEvent` (and again when `hide_covered_keys` changes): `KeyMapping.setKey(InputConstants.UNKNOWN)`, `KeyMapping.resetMapping()`, `options.save()`. It writes `config/supernaturalbloodlines-hud-keys.json`.
- Hiding from the Controls screen: first try `ScreenEvent.Init.Post` on `KeyBindsScreen` and Controlling's screen (matched by class name), removing the `KeyEntry` children whose mapping is covered (an access transformer for `KeyBindsScreen.keyBindsList` and `KeyBindsList.KeyEntry.key`, added to `META-INF/accesstransformer.cfg`). Check it with Controlling's search, which may rebuild the list. Only if the search brings hidden entries back, add the reserved `KeyBindsListHideMixin` (filter at the list's entry build, `require = 0`, switch `hide_in_controls_mixin`) and record why in `docs/mixins.md`.
- Test: `src/test/.../core/hud/CoveredKeysTest.java`; unit `HudKeysDefaultsTest` (no two of our defaults are equal; none collides with the OFA live binds listed in this plan).

- [ ] **Step 1: Write the failing unit rows.** `CoveredKeysTest`: `recordsBeforeUnbinding`, `restoresPlayersOwnBind`, `restoreSkipsKeysThePlayerRebound`, `secondRunRecordsNothingNew`, `missingModIgnored`. `HudKeysDefaultsTest`.
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Then the manual check in `runClient` (when the owner isn't playing):
  - covered keys are gone from Controls and from Controlling's search;
  - `hide_covered_keys=false` brings them back with their earlier binds;
  - Alt + Caps fires only `hud_mode`, never the radio;
  - F2 screenshot of the Controls screen.
- [ ] **Step 5: Docs and commit.** `wiki/Key-Binds.md` (the full table and "covered keys and how to get them back"), `wiki/Configuration.md` (client toml), `docs/parts/hud.md`. Commit `feat(hud): one keymap with covered keys`.

### Task 5: The OFA pack keymap, ready for the return of the three mods

Vampirism, Werewolves and SupernaturalBloodlines are **deliberately out of the pack** right now; the owner adds all three back at once when the mod is done. This task prepares the keymap and `configureddefaults` for that moment, and fixes the stale notes. **It installs no mod.** The owner runs it, or we run it in the pack instance with the owner's OK, with the game closed. The only repo change is a doc.

**Files (pack instance `C:\Users\caiom\curseforge\minecraft\Instances\OFA - Holy Packs`, not the repo):**
- `OFA-notes/apply_keymap.py`, `KEYMAP`:
  - Fix the stale lines to the owner's live binds:
    - `tensura.keybinding.next_mode` `K("left.alt")`;
    - `race_ability` `K("left.bracket")` (Tensura vampires feed here);
    - `key.greatsage.menu` `K("g")`;
    - `key.swapOffhand` `U`. This fixes the **F clash**: `configureddefaults` ships `swapOffhand` and Tensura slot 2 both on F.
  - Fix the Tensura lines to the live binds for today: `tensura.keybinding.ability.slot_1..3` `r f v`, `tensuraslots.*` `z x c` (no Alt). The old shared V line (`keys.vampirism.suck` / `keys.werewolves.bite` on V) is replaced by the `SUPERNATURAL` dict below.
  - Add a second dict, `SUPERNATURAL`, merged over `KEYMAP` **only when a `supernaturalbloodlines-*.jar` is in `mods/`** (one `glob` check). Today it has no effect, so Tensura keeps R/F/V while our HUD isn't there. When the three mods are back, the same run switches the pack over. It holds:
    - covered keys `U`: `tensura.keybinding.ability.slot_1..3`, `tensuraslots.keybinding.secondary_ability.slot_1..3`, `keys.vampirism.suck`, `keys.werewolves.bite`, `keys.vampirism.action1..9`;
    - our keys: `key.supernaturalbloodlines.slot_1..6` (`r f v z x c`), `key.supernaturalbloodlines.hud_mode` (`K("caps.lock:ALT")`), `key.supernaturalbloodlines.hub` (`K("home")`). There is no Bite key: Bite is slot 1's action.
  - Add a `--with-supernatural` flag that forces the merge. The owner can use it to preview the future defaults, which are written to `configureddefaults/options.supernatural.txt` and never to the live `options.txt`.
- `OFA-notes/KEYMAP.md`: rewrite it to match.
  - Rows: R/F/V and Z/X/C = the 6 HUD slots (Tensura presets N and N+1, or Supernatural actions); Bite = slot 1's action by default (it does what your form does); Alt + Caps = HUD mode; Alt + scroll = Tensura presets; Alt held = slot names; Alt + slot key = Tensura mode change; `[` = Tensura race ability; Home = Supernatural hub; G = Great Sage menu.
  - Drop the stale rows: Z/X/C Tensura, Alt + Z/X/C Tensura Slots, R race ability, Shift + R next mode, V suck/bite.
  - Note that Caps alone is still the radio, and that F (swap offhand) is unbound.
- `mods/tensuraslots-1.0.0.jar`: remove it when the three mods go back in (our HUD replaces it), and record that in `OFA-notes/CHANGELOG.md`. Until then it can stay.
- `configureddefaults/options.txt`: regenerated by the script.
- Repo: `docs/parts/hud.md`, an "OFA pack" section with the exact commands and "run again after the three mods are installed".

- [ ] **Step 1:** Make sure the game is closed. Back up `options.txt` as `OFA-notes/options.backup-2026-11-15.txt`.
- [ ] **Step 2:** Edit `apply_keymap.py` and `KEYMAP.md` as above. Show the diff to the owner.
- [ ] **Step 3:** Run `py OFA-notes\apply_keymap.py` from the instance folder (without the flag). Expected: only the stale-line fixes change. In `configureddefaults/options.txt`, `key_key.swapOffhand:key.keyboard.unknown` and Tensura slot 2 is the only plain-F bind, so the F clash is gone.
- [ ] **Step 4:** Run `py OFA-notes\apply_keymap.py --with-supernatural`. In `configureddefaults/options.supernatural.txt`:
  - `key_key.supernaturalbloodlines.slot_2:key.keyboard.f`;
  - `key_tensura.keybinding.ability.slot_2:key.keyboard.unknown`;
  - nothing else on plain F;
  - every `tensuraslots`/`suck`/`werewolves.bite` line is `unknown`.

  The live `options.txt` is unchanged by this run. Start the pack once (without the three mods) and confirm that R/F/V still fire Tensura's skills and F doesn't swap hands.
- [ ] **Step 5: Commit** (repo doc only) `docs(hud): ofa pack keymap steps`. When the owner adds the three mods back, run `apply_keymap.py` once more, remove Tensura Slots, and check Controls. That run is a checklist line in `docs/parts/hud.md`, not part of this phase.

---

# Phase 13b: the HUD

### Task 1: Slot model, the loadout attachment and `LoadoutAccess`

**Files:**
- Create: `core/hud/PresetPair.java` (pure: `left = active`, `right = (active + 1) % presetCount`; Tensura has 9), `core/hud/SlotMode.java` (pure: available modes from `tensuraLoaded`, `vampirismLoaded`, and the next mode on toggle), `core/hud/SlotView.java` (record: icon id, key label, cooldown fraction and seconds, active, greyed and reason key, name).
- Create: `core/hud/Loadout.java` (record of 6 `Optional<ResourceLocation>` with a codec; `Loadout.DEFAULT` has `supernaturalbloodlines:bite` in slot 1 and the rest empty, and is the attachment's default, so a new player has Bite on R). Modify `core/attachment/ModAttachments.java`: register `hud_loadout` with `copyOnDeath()`.
- Create: `parts/hud/net/SetLoadoutPayload.java` (C2S `slot`, `actionId|empty`). The server accepts `supernaturalbloodlines:bite` always, or an id in the Vampirism action registry that the player has unlocked in their current faction (`getUnlockedActions()`). Duplicates are allowed. Bite can be moved to any slot or cleared; it is never put back on its own. Add `LoadoutSyncPayload` (S2C) on login, respawn, dimension change and set.
- Create: `shared/hud/LoadoutAccess.java` (client view: `available()`, `get()`, `assignable()` as id, name and icon, Bite first, `set(slot, id)`; the HUD part installs it, and it is unavailable without it).
- Test: `PresetPairTest` (9 pairs with 1), `SlotModeTest` (each mod combination), `LoadoutTest` (codec, empty tag), GameTests `LoadoutGameTests`.

- [ ] **Step 1: Write the failing tests.** Unit as listed. GameTests:
  - `newPlayerHasBiteInSlotOne`, `biteCanMoveAndClear`;
  - `setLoadoutAcceptsUnlocked`;
  - `setLoadoutRefusesLockedOrForeign`;
  - `loadoutSurvivesDeath`;
  - `loadoutNotClearedOnFactionChange` (a cured vampire keeps the list; its slots show greyed later).
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/hud.md` (state and payloads). Commit `feat(hud): slot model and loadout`.

### Task 2: Tensura source, input forwarding and hiding Tensura's skill HUD

**Files:**
- Create: `parts/hud/client/tensura/TensuraSlots.java`. Six `SlotView`s from `TensuraStorages.getAbilityFrom(player)`, `PresetPair`, and `SkillAPI.getSkillsFrom(player).getSkill(skill)`: cooldown is `getCoolDown(mode)`, active is `isToggled()`, an empty slot shows empty, and the icon comes from the skill's own texture (the one Tensura's `renderAbility` uses; find the getter in the jar).
- Create: `parts/hud/client/tensura/TensuraInput.java`. For slot `i` (column `c`, row `r`), on press:
  - if `NEXT_ABILITY_MODE` or `PREVIOUS_` is down: `changeModePacket(r, bool)` for the left column, `changeModePacket(rightPreset, r, bool)` for the right;
  - else `skillActivationPacket(id, keyNumber, mode)`;
  - on release: `skillReleasePacket(id, keyNumber, heldTicks)`, counting held ticks the way `TensuraKeybinds` does.

  Choose `keyNumber` by reading `TensuraKeybinds.onAbilityPressed` and Tensura Slots' lambdas (the `iconst` pushed before `skillActivationPacket`), and record it in the part doc.
- Create: `parts/hud/client/tensura/TensuraHudFlags.java`. On `RenderGuiEvent.Pre`, while the HUD is on in any mode and `hide_tensura_skill_hud` is on, remember the player's `HudConfig.abilities.render` once and set it to false. Put it back when our HUD turns off, on `ScreenEvent.Opening` of Tensura's `SettingsScreen` (class by name), and on client stop. Re-apply on that screen's close. This is so ManasCore never saves our value.
- Reserved mixin (1), `AbilityStorageNextPresetMixin`: first check in `runClient`, with Tensura Slots removed and the mixin absent, whether a right-column skill (a) activates and (b) keeps its passive effect. If either fails, add the mixin: RETURN of the four `AbilityStorage.isAbilityInActivePreset(...)` overloads, true also for preset `(active + 1) % 9`, `require = 0`, switch `next_preset_active`, the same target as Tensura Slots' mixin. Record the result either way in `docs/mixins.md` and `docs/parts/hud.md`.
- Test: unit `TensuraInputPlanTest` on a pure planner (`InputPlan.forPress(slot, modeKeyDown, presets) -> packet kind and arguments`), so the forwarding table is tested without the game.

- [ ] **Step 1: Write the failing unit rows.** Left press activates; right press activates the N+1 slot; Alt + right press changes the N+1 mode with the preset id; an empty slot sends nothing; release carries held ticks.
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.** Then the manual parity check, with each case side by side with Tensura's own keys (temporarily rebound for the check): a press skill, a held skill (charge and release), a toggle skill, a mode-cycling skill, Alt + scroll through all 9 presets (9 pairs with 1), and opening and closing Tensura's HUD settings.
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/hud.md` (forwarding table, `keyNumber`, flag restore), `docs/mixins.md` if the mixin was added, `wiki/Known-Limits.md` (Tensura Slots no longer needed; with it still installed our HUD hides its second column too). Commit `feat(hud): tensura slots`.

### Task 3: Vampirism and Werewolves source and triggers

**Files:**
- Create: `parts/hud/client/vampirism/ActionSlots.java`. Six `SlotView`s from `Loadout` and the client `IActionHandler`:
  - cooldown: `getPercentageForAction` / `isActionOnCooldown`;
  - active: `isActionActive(id)`;
  - greyed when not unlocked or `canUse` is not `PERM.ALLOWED`, with the `PERM` name as the reason key;
  - **the Bite slot**: our own bite icon. `BiteRules` runs on the client view (synced `BloodlineState`, client faction, Werewolves form) to pick the Alt tooltip line ("Werewolf bite", "Drink blood", "Feed", "Finish the turning") or grey it with row 5's reason. The cooldown is `BiteService`'s 20 ticks. The server stays the authority.

  With the `supernatural` part off (no hub to assign in), empty slots 2–6 show the first 5 `getUnlockedActions()` that have `showInSelectAction`. Those are shown, not stored. (`shortcut: no assignment UI without the hub, add a mini screen if a server runs hud without supernatural`)
- Create: `parts/hud/net/TriggerActionPayload.java` (C2S `slot`, `targetEntityId` (crosshair, or -1), `boolean release`) and `parts/hud/action/ActionService.java`. The server reads the slot's id from the player's `Loadout`, never from the packet.
  - For `supernaturalbloodlines:bite` it calls `BiteService.press(p, targetEntityId)` on press and `release(p)` on release (13a Task 2).
  - For any other id it checks that the action is unlocked, then calls `toggleAction(action, ctx)` on press only, with `ctx` from a server ray-trace (`ActivationContext` implemented by a small record). A refusal `PERM` gets the action-bar reason.

  It is rate-limited. **`rate_limit_per_second` applies to every press payload, refusals included** (a refused press costs as much as a bite, so spamming refusals cannot flood the server; `BiteService`'s own 20-tick cooldown counts only bites that happened). The Bite slot also gets `BiteService`'s refusal reason keys (`supernaturalbloodlines.hud.bite.*`) for its greyed state.
- Modify: `parts/hud/client/HudInput.java` (from Task 2's key handling): in Supernatural mode the six keys send `TriggerActionPayload` on press and on release.
- Test: `parts/hud/gametest/ActionGameTests.java`.

- [ ] **Step 1: Write the failing GameTests.**
  - `triggerTogglesUnlockedAction`: a level-4 vampire with an unlocked lasting action finds it active after the trigger and off after a second one.
  - `triggerRespectsCooldown`, `triggerLockedRefused`, `triggerUsesLoadoutNotPacket`.
  - `werewolfActionTriggers` (skipped under `-PnoWerewolves`).
  - `hybridGetsWerewolfActions`.
  - `biteFiresFromItsSlot`: Bite in slot 1 starts a vampire's feeding and ends it on release; moved to slot 4, slot 1 does nothing and slot 4 bites.
  - `biteInTensuraModeNotOffered`: Tensura mode's slots never trigger Bite.
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/parts/hud.md`, `wiki/Ability-HUD.md` (Supernatural mode). Commit `feat(hud): faction action slots`.

### Task 4: The renderer, the mode switch and the Alt tooltips

**Files:**
- Create: `parts/hud/client/AbilityHudLayer.java`, registered with `RegisterGuiLayersEvent.registerAbove(VanillaGuiLayers.HOTBAR, supernaturalbloodlines:ability_hud, ...)`. It draws two columns of 3 hotbar-style slots (vanilla `hud/hotbar` sprites):
  - the left column at the hotbar's left and the right column at its right;
  - the key label in the corner (`KeyMapping.getTranslatedKeyMessage`, cut to 3 chars);
  - the cooldown as a dark fill from the top plus seconds;
  - the active state as an outline;
  - greyed slots at half alpha.

  It is hidden while `options.hideGui` is on, in spectator mode, and when `SlotMode` has no mode.
- Create: `parts/hud/client/HudModeState.java` (current mode, default from `mode`/`AUTO` = Tensura when installed; `hud_mode` key toggles through `SlotMode.next`; an action-bar line names the new mode). Remembered in the client config.
- Create: `parts/hud/client/AltTooltips.java`. While `Screen.hasAltDown()` and no screen is open, it draws a vanilla tooltip panel per slot (name, mode name or action name, cooldown seconds, greyed reason).
- Create: `src/main/resources/assets/supernaturalbloodlines/textures/gui/hud/slot_active.png` (an outline, our own art).
- Test: unit `HudLayoutTest` (pure: slot rectangles for widths 320 and 427 at GUI scale 2 and 4; they never overlap the hotbar or the offhand slot, and never go off screen).

- [ ] **Step 1: Write the failing unit rows** (layout).
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Then `runClient` (when the owner isn't playing). Take F2 screenshots at GUI scale 2 and 4 of:
  - Tensura mode with a full preset pair;
  - Tensura mode with an empty slot;
  - a skill on cooldown;
  - Supernatural mode with an active action and a greyed slot;
  - Alt held;
  - `-PnoLinkedMods`, where there is no HUD.

  Attach them to the PR.
- [ ] **Step 5: Docs and commit.** `wiki/Ability-HUD.md` (layout, modes, Alt; screenshots later), `_Sidebar.md`, `wiki/Configuration.md`. Commit `feat(hud): ability hud`.

### Task 5: The hub's Loadout tab

**Files:**
- Modify: `parts/supernatural/client/hub/page/SkillsPage.java`. Add `Tab.LOADOUT`, shown only when `LoadoutAccess.available()` (and remove the `shortcut:` comment on line 36). The left page lists `LoadoutAccess.assignable()`, Bite first: icon, name, greyed with a reason. Bite's row says what it does for the player's current form. The right page shows the 6 slots in HUD order with their keys. Pick an action and then a slot, or a slot and then an action. A cleared slot sends empty. Keyboard focus follows `HubFocus`.
- Modify: `en_us.json` (`...hub.skills.tab.loadout`, hints).
- Test: unit `LoadoutTabModelTest` (pure selection logic: pick, then assign, then clear).

- [ ] **Step 1: Write the failing unit rows.**
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. With `-PpartsOff=hud`, the tab is gone and the Skills page is unchanged. Then `runClient`:
  - assign 6 actions in the Grimoire and Blood Moon themes, and see them on the HUD;
  - F2 screenshots at GUI scale 2 and 4.
- [ ] **Step 5: Docs and commit.** `wiki/Supernatural-Hub.md` (Loadout tab), `docs/parts/supernatural.md` (the tab reads `LoadoutAccess`). Commit `feat(supernatural): hub loadout tab`.

---

# Phase 13c: one blood value and defaults

### Task 1: Vampirism blood in Tensura's food spot

**Files:**
- Create: `parts/hud/client/tensura/BloodInFoodSpot.java`. It runs only with Tensura **and** Vampirism loaded and `blood_in_food_spot` on.
  - `RenderGuiLayerEvent.Pre`: cancel `VIngameOverlays.BLOOD_BAR_ID` (`vampirism:blood_bar`).
  - For a Vampirism vampire (client faction is vampire), remember and set `HudConfig.decorations.food.render = false`, with the same restore rules as Task 2 of 13b (shared helper `TensuraHudFlags`).
  - `RenderGuiEvent.Post`: draw a blood drop and the number `getBloodLevel()` at `OverlayHandler.getPositionsById(FOOD_DECO)`, scaled by `getScaleById(FOOD_DECO)` and flipped by `isLeftSide(FOOD_DECO)`, matching the layout of `renderFoodDecoration`. The drop is our own texture, `textures/gui/hud/blood_full.png` / `blood_empty.png`.
  - Non-vampires keep Tensura's food. Without Tensura, nothing changes and Vampirism's bar stays.
- Test: unit `BloodSpotRuleTest` (pure: which bar shows for each combination of mods, faction and setting).

- [ ] **Step 1: Write the failing unit rows.**
- [ ] **Step 2: Run** `test`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Then `runClient` F2 screenshots of a vampire, a human, and a vampire with `-PnoTensura` if such a flag exists, or with the setting off. Open and close Tensura's HUD settings, quit, and check that `food.render` on disk is the player's value.
- [ ] **Step 5: Docs and commit.** `wiki/Ability-HUD.md` (blood), `wiki/Vampires-And-Originals.md`. Commit `feat(hud): one blood value`.

### Task 2: Tensura race feeding fills Vampirism blood

**Files:**
- First check, in `runClient`, what happens today when a Tensura `vampire` race that is also a Vampirism vampire uses its race ability on a villager:
  - Does `FoodData.needsFood()` refuse it? A Vampirism vampire's food is kept full.
  - Does `FoodData.eat` change anything visible?

  Then check `manascore-race` for a race-ability event. If one exists, use it to credit blood after the feed: on the server, a Vampirism vampire whose Tensura race is vampire-line, victim not bloodless.
- Only if there is no event, or the `needsFood` refusal blocks it: add reserved mixin (2), `VampireRaceFeedMixin`. It `@WrapOperation`s `FoodData.needsFood()` and `FoodData.eat(int, float)` inside `VampireRace.onActivateAbility`. For a Vampirism vampire (gated by `VampireSunRule`'s faction check, through a shared hook), `needsFood` means "blood not full" and `eat(n, s)` becomes `drinkBlood(n, s, IDrinkBloodContext of the victim)`. This credits the same blood a Vampirism bite on that target would give (the spec's call). It uses `require = 0`, switch `tensura_feed_fills_blood`, and the plugin applies it only with Tensura and Vampirism loaded.
- Create: `parts/hud/feed/TensuraFeedRules.java` (pure: the blood credited = Vampirism's value for that victim; 0 for a bloodless victim).
- Test: unit `TensuraFeedRulesTest`; GameTest `TensuraFeedGameTests.raceFeedFillsBlood` (call `VampireRace.onActivateAbility` on a mock player with a villager in front; skip without Tensura).

- [ ] **Step 1: Write the failing tests.**
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement** the event route, or the mixin route with the reason recorded.
- [ ] **Step 4: Run** all 9 test runs. Expected: PASS.
- [ ] **Step 5: Docs and commit.** `docs/mixins.md` (if added, with "Why"), `docs/parts/hud.md`, `wiki/Tensura-Integration.md`. Commit `feat(hud): tensura feeding fills vampirism blood`.

### Task 3: Player-ready defaults and the release check

**Files:**
- Modify: `HudClientConfig` defaults: `hud_enabled` true, `mode` AUTO, `hide_covered_keys` true, `hide_tensura_skill_hud` true, `blood_in_food_spot` true. `KeyCover` runs on the first start with no record file, so a new player gets the keymap with no action, and `Loadout.DEFAULT` gives them Bite on R in Supernatural mode.
- Modify: `CHANGELOG.md`, and add the final rows in `wiki/Known-Limits.md`:
  - Alt + scroll is Tensura's own (bound to `next_mode`);
  - in Supernatural mode, Alt + scroll still moves Tensura's presets;
  - Shift + R / F belong to Book of Dragons in the OFA pack, so sneaking blocks slots 1 and 2 there.
- Test: GameTest `HudPartOffGameTests.partOffChangesNothingElse`; unit `DefaultsTest` (the config defaults are exactly the spec's "player-ready" list).

- [ ] **Step 1: Write the failing rows.**
- [ ] **Step 2: Run.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** all 9 test runs. Then a fresh `runClient` profile (delete `run/options.txt` and the record file first): the HUD is on, covered keys are hidden, and the blood is in the food spot. F2 screenshot. Switch the part off with `parts.hud=false` and restart: every covered key is back with its earlier bind.
- [ ] **Step 5: Docs and commit.** `wiki/For-Players.md` (one paragraph and a link), `wiki/For-Server-Owners.md` (the part is client-heavy; the server needs it for bites and triggers). Commit `feat(hud): player-ready defaults`.
