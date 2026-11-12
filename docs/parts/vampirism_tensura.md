# Part: vampirism_tensura

Requires: `vampirism`, `tensura`. Werewolves is optional. Package: `net.ofatech.supernaturalbloodlines.parts.vampirismtensura`. Plan: `docs/superpowers/plans/01-vampirism-tensura.md`.

Checklist (from the design, section 3). Tick items as they ship.

**Unified vampire**
- [x] Vampirism -> Tensura: **nothing** (R20). Joining the vampire faction never changes the Tensura race; there is no `join_race` option.
- [x] Tensura → Vampirism: a Tensura race in the vampire line joins the faction at the mapped level (`ghoul` no faction; `vampire` level 1 or the current level if higher; `vampire_overcomer` and `vampire_lord` at least level 7; `divine_vampire` max level).
- [x] Leaving the faction (cure) sets the race to `cure_race` (default `tensura:human`), only if the current race is in the Tensura vampire line (R20).
- [x] Sun burn: Tensura's own vampire burn is disabled for faction vampires. Tensura has no config switch for it (verified), so this is a mixin on `VampireRace.shouldBurn`, plus one on `RaceUtils.isUnderSun` for the Weakness/Slowness/Mining Fatigue debuffs (Task 4 decision: Vampirism's sun handling wins). Both listed in `docs/mixins.md`.

**Hunters**
- [x] `CanJoinFaction` returns DENY for hunters whose race has the tags `tensura:undead` or `daemon`.
- [x] A hunter whose race becomes undead or daemon leaves the hunter faction, with a chat message.

**Werewolves (when installed)**
- [x] Joining is denied for the race tags `tensura:undead`, `daemon` and `no_blood`.
- [x] Werewolf levels grant configured Tensura skills (default `tensura:godwolf_sense` at max level).

**Other**
- [x] Holy damage both ways: `vampirism:holy_water` already counts as Tensura holy damage (no tag needed, pinned by `holyWaterCountsAsTensuraHoly`); Tensura holy damage hurts Vampirism vampires x`tensura_holy_vs_vampire` (default 1.5).
- [x] Bloodless races can't be bitten: `tensura:no_blood` race tag (mixin, see below), and blood 0 for Tensura bloodless mobs (data map).
- [x] EP for Vampirism and Werewolves mobs (data `entity_existence`).
- [ ] **Manual check pending** (Task 5 Step 3): `runClient`, spawn a Vampirism vampire and a werewolf, open Tensura's analysis and see their EP. Replaced by the GameTests `vampirismMobsHaveEp` / `werewolfMobsHaveEp` (registry contents), which do not prove the analysis screen.


## Hook points (verified)

Verified with `javap -p` (and `-c` where noted) on the jars in the Gradle cache: Tensura 2.0.1.5, ManasCore race 4.0.0.2 (`build/manascore-modules`), Vampirism 1.10.13 (api jar; no sources jar is published, so everything below is from bytecode), Werewolves 2.0.3.4.

### Tensura

| Item | Result |
|---|---|
| Sun-burn switch in `config.race.VampireConfig` | **None.** Fields are only `Ghoul`, `Vampire`, `VampireOvercomer`, `VampireLord`, `DivineVampire` (stat sections: aura, magicule, size, health, attack, speed; `Vampire` also `bloodRequirement`, `bloodRequirementHuman`; the upper races `epRequirement`). So disabling Tensura burn needs **one mixin** on `VampireRace.shouldBurn` (add it to `docs/mixins.md`). |
| `VampireRace.shouldBurn` | `public static boolean shouldBurn(net.minecraft.world.entity.LivingEntity)` in `io.github.manasmods.tensura.race.vampire.VampireRace`. Logic: `true` if gamerule `TensuraGameRules.HARDCORE_RACE`; otherwise if the head slot is non-empty, damages the helmet (`hurtAndBreak(nextInt(2), ...)`) and returns `false`; else `true`. Also called from `GhoulRace` and `WightRace` (and `UndeadHumanoidEntity`), so the mixin must be gated on "player is a Vampirism vampire", not applied blindly. Mind that returning `false` early also skips the helmet damage. |
| What the burn does | `VampireRace.onTick` first checks `RaceUtils.isUnderSun(LivingEntity)` and then adds Weakness, Slowness and Mining Fatigue; only after that it calls `shouldBurn` and `setRemainingFireTicks`. A `shouldBurn` mixin does **not** remove the three debuffs. If R-level behaviour requires "no sun penalty", also mixin/redirect `RaceUtils.isUnderSun` (`public static boolean isUnderSun(LivingEntity)`) for faction vampires. Open decision for Task 5. |
| `RaceEvents.SET_RACE` | `dev.architectury.event.Event<RaceEvents$SetRaceEvent> SET_RACE` (in `io.github.manasmods.manascore.race.api.RaceEvents`, module `manascore-race`). Interface: `EventResult set(ManasRaceInstance oldRace, LivingEntity entity, ManasRaceInstance newRace, boolean <flag>, Changeable<Boolean> <changeable>, Changeable<MutableComponent> <message>)`. The first `ManasRaceInstance` is the old race (`null` when the entity had none), the third is the new one. **Verified by GameTest `raceHookOrderAndNamespace`** (saw `none>tensura:human`, then `tensura:human>tensura:elf`). It fires **before** the change (the `EventResult` can veto it), so a listener must use the id of the new instance, not the entity's current race. `EventResult` is Architectury's. Register with `RaceEvents.SET_RACE.register(...)`. It fires for any `LivingEntity`, so the listener must check `entity instanceof ServerPlayer`. |
| `Races.setRace` overloads (`io.github.manasmods.manascore.race.api.Races`, an interface obtained with `RaceAPI.getRaceFrom(LivingEntity)` in `io.github.manasmods.manascore.race.api.RaceAPI`; there is no `Races.getRaceFrom`; works on mobs too) | `setRace(ResourceLocation, boolean)`, `setRace(ResourceLocation, boolean, MutableComponent)`, `setRace(ManasRace, boolean)`, `setRace(ManasRace, boolean, MutableComponent)`, `setRace(ManasRaceInstance, boolean, boolean)` (default), and the abstract `setRace(ManasRaceInstance, boolean, boolean, MutableComponent)`. All return `boolean`. Also `getRace(): Optional<ManasRaceInstance>`, `evolveRace(...)` (ResourceLocation / ManasRace / ManasRaceInstance, each with an optional `MutableComponent`). |
| Race ids | Registered in `io.github.manasmods.tensura.registry.race.TensuraRaces` by path `human`, `ghoul`, `vampire`, `vampire_overcomer`, `vampire_lord`, `divine_vampire` The namespace is **`tensura`**, verified by GameTest (`tensura:human`, `tensura:elf`, `tensura:vampire_lord`, `tensura:divine_vampire`, `tensura:ghoul` all resolve through `setRace`); `ManasRaceInstance.getRaceId()` returns that id. Fields `HUMAN`, `GHOUL`, `VAMPIRE`, `VAMPIRE_OVERCOMER`, `VAMPIRE_LORD`, `DIVINE_VAMPIRE` are `RegistrySupplier<ManasRace>`. |
| `TensuraDamageHelper.isHoly` | `public static boolean isHoly(net.minecraft.world.damagesource.DamageSource)`. True if `source.tensura$getElement() == Element.HOLY`, **or** `getMsgId()` contains `"holy"` or `"divine"`, **or** `source.is(TensuraDamageTypes.HOLY_DAMAGE)`. Vampirism's `holy_water` damage type has `message_id` `holy_water`, so it already counts as Tensura holy damage with no work (still pin it with a GameTest). |

### Tensura tags (for Task 5)

| Tag | Kind | Registry / path | Content |
|---|---|---|---|
| `tensura:undead` | **race tag** (ManasCore race registry) | `data/tensura/tags/manascore_race/races/undead.json` | `ghoul`, `wight`, `wight_king`, `spirit_skeleton`, `divine_skeleton` |
| `tensura:daemon` | **race tag** | `data/tensura/tags/manascore_race/races/daemon.json` | `lesser_daemon`, `greater_daemon`, `arch_daemon`, `daemon_lord`, `devil_lord` |
| `tensura:no_blood` | **race tag** | `data/tensura/tags/manascore_race/races/no_blood.json` | `#tensura:spiritual`, `#tensura:undead`, `tensura:slime`, `tensura:metal_slime` |
| `tensura:no_blood` | **entity-type tag** (a different tag with the same id) | `data/tensura/tags/entity_type/no_blood.json` | `#tensura:spiritual`, `#tensura:clones`, `#minecraft:undead`, `#minecraft:arthropod`, `#tensura:slimes`, `#tensura:non_living`, iron golem, shulker, warden, `phantaspore`, `charybdis`, `elemental_colossus` |
| `tensura:daemons` (plural) | entity-type tag | `data/tensura/tags/entity_type/daemons.json` | the three daemon entities (not the race tag) |

The race tags live in the ManasCore race registry folder `tags/manascore_race/races`. Build the key with the race registry's `ResourceKey` and `TagKey.create`, and test membership through the holder of the player's `ManasRace`, not `EntityType`. Note `ghoul` is in `tensura:undead`, so a Tensura ghoul can never be a hunter and is no_blood, but it is still a "vampire line" race for the sync mapping (Task 2).

### Vampirism (api jar, 1.10.13)

| Item | Result |
|---|---|
| `PlayerFactionEvent` (`de.teamlapen.vampirism.api.event`) | NeoForge `Event` (not cancellable at the base). Accessors: `getCurrentFaction(): IPlayableFaction<?>`, `getPlayer(): IFactionPlayerHandler` (the handler; the entity is `getPlayer().getPlayer()`). |
| `PlayerFactionEvent.CanJoinFaction` | `getFactionToJoin(): IPlayableFaction<?>`, `getBehavior()`, `setBehavior(Behavior)`. `Behavior` enum: `ONLY_WHEN_NO_FACTION`, `ALLOW`, `DENY`. Constructor `(IFactionPlayerHandler, IPlayableFaction current, IPlayableFaction toJoin)`. |
| `PlayerFactionEvent.FactionLevelChanged` | `getOldLevel()`, `getNewLevel()`, `getOldFaction()`, plus inherited `getCurrentFaction()` (the **new** faction; **`null` when the player left**, that is the cure case; verified by GameTest `leavingFactionReportsNullFaction`: old = vampire faction, current = `null`, new level 0) and `getPlayer()`. Fires after the change. |
| `PlayerFactionEvent.FactionLevelChangePre` | **Cancellable** (`ICancellableEvent`). `getCurrentLevel()`, `getNewLevel()`, `getNewFaction()`. Usable to veto a join before it happens. |
| `IFactionPlayerHandler.setFactionAndLevel` | `boolean setFactionAndLevel(IPlayableFaction<?> faction, int level)`. Also `setFactionLevel(IPlayableFaction<?>, int)`, `joinFaction(IPlayableFaction<?>)`, `leaveFaction(boolean)`, `canJoin(IPlayableFaction<?>)`, `canLeaveFaction()`, `getCurrentFaction()`, `getCurrentLevel()`, `getCurrentLevel(IPlayableFaction<?>)`, `isInFaction(IFaction<?>)`. |
| Vampire faction + max level | `VReference.VAMPIRE_FACTION` (`IPlayableFaction<IVampirePlayer>`, a mutable static filled at registration, so read it lazily), `VReference.HUNTER_FACTION`; ids `VReference.VAMPIRE_FACTION_ID` / `HUNTER_FACTION_ID`. Max level: `VReference.VAMPIRE_FACTION.getHighestReachableLevel()` (`int`). `getHighestLordLevel()` is the lord one. |
| Sun damage | **No event in the API** (the only related one is `VampireFogEvent`). Damage type key `ModDamageTypes.SUN_DAMAGE` = `vampirism:sun_damage` (`message_id` `sun`). If it ever has to be cancelled, do it with `LivingIncomingDamageEvent` and `source.is(ModDamageTypes.SUN_DAMAGE)` (class `de.teamlapen.vampirism.core.ModDamageTypes`, in the full jar, not the api jar). For this part it is not needed: Tensura's burn is a separate mechanism (see above). `ISundamageRegistry.isGettingSundamage(LivingEntity, LevelAccessor)` exists in the API. |
| Holy water damage source | `ModDamageTypes.HOLY_WATER` = `vampirism:holy_water`. Other keys: `VAMPIRE_ON_FIRE`, `VAMPIRE_IN_FIRE`, `NO_BLOOD`, `STAKE`, `MINION`, `DBNO`, `MOTHER`. Tags in `vampirism:` `damage_type`: `entity_physical`, `remains_invulnerable`, `mother_resistant_to`, `vampire_immortal`. There is **no** `is_holy` damage-type tag. |
| Bite / blood-drink event | `BloodDrinkEvent` (NeoForge `Event`, **not cancellable**) with subclasses `BloodDrinkEvent.PlayerDrinkBloodEvent` (player vampire; constructor takes `IVampirePlayer`) and `EntityDrinkBloodEvent`. Accessors: `getVampire(): IVampire`, `getAmount()`, `setAmount(int)`, `getSaturation()`, `setSaturationModifier(float)`, `getBloodSource(): IDrinkBloodContext`, `useRemaining()`. So the bite cannot be cancelled with this event; `setAmount(0)` gives the "blood 0" outcome. To refuse the bite itself use `IBiteableEntity.canBeBitten(IVampire)` (player-side `IVampirePlayer.determineBiteType(LivingEntity)` returns a `BITE_TYPE`: `SUCK_BLOOD_CREATURE`, `SUCK_BLOOD_PLAYER`, `SUCK_BLOOD`, `NONE`, `HUNTER_CREATURE`) via a mixin, or feed zero through the blood data map (`vampirism:entity_type/entity_blood`, entries `{ "blood": N }`; `0` means not bitable). The data map route is the plan's choice for Tensura bloodless **mobs**; the event route only covers the amount. |

### Werewolves 2.0.3.4

- Faction accessor: `de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION` (`IPlayableFaction<IWerewolfPlayer>`, mutable static; `IWerewolfPlayer` is in package `de.teamlapen.werewolves.api.entities.player`, **plural** `entities`). Mod id constant `WReference.MODID`.
- Test "is a werewolf" with `handler.isInFaction(WReference.WEREWOLF_FACTION)` or `handler.getCurrentFaction() == WReference.WEREWOLF_FACTION`; keep every `WReference` / Werewolves reference in a nested class loaded only after `ModList.get().isLoaded("werewolves")`.
- The jar publishes no `api` classifier, so compile uses the full jar (see `docs/dependencies.md`).

### Consequences for the next tasks

- Task 5 sun burn: one mixin on `VampireRace.shouldBurn` (and decide about `RaceUtils.isUnderSun` for the debuffs); no Tensura config option exists.
- R20: no Vampirism -> Tensura race write anywhere. `FactionLevelChanged` with `getOldFaction() == vampire` and `getCurrentFaction() != vampire` is the cure signal (current is `null` on leaving, verified); there is no dedicated event.
- Command-set factions (`/vampirism level`) arrive through the same `FactionLevelChanged`, so the sync listener is the single entry point; guard writes with `ReentrancyGuard`.
- `CanJoinFaction.setBehavior(DENY)` is the hunter/werewolf denial hook; `FactionLevelChangePre` is the cancellable fallback.

### Task 3 findings (GameTest-verified)

- Vampirism's faction data works on a FakePlayer (VampirismAPI.factionPlayerHandler(p), setFactionAndLevel, events all fire), so TestPlayers needed no change beyond becoming public.
- /vampirism level none @s (the command) leaves the faction and goes through the same FactionLevelChanged; /vampirism level <faction> <level> [player] sets one.
- The sync tests (parts/vampirismtensura/gametest/) are registered from VampirismTensuraPart.init through RegisterGameTestsEvent, not @GameTestHolder, so they exist only when the part is enabled (the holder scan would load them without Vampirism/Tensura).
- Because SET_RACE fires before the change, another listener that vetoes the race change after ours would leave the faction raised without the race; accepted.

### Task 5 findings (GameTest-verified)

- **Bite blocking path: mixin.** None of the preferred routes exists. `BloodDrinkEvent` is not cancellable and fires after the victim has already lost food (`VampirePlayer.onBite`), so `setAmount(0)` is not a refusal. The bite starts from the custom packet `ServerboundStartFeedingPacket` -> `VampirePlayer.biteEntity`, with no NeoForge interaction event in front. A player victim has no blood data map or capability to override (`entity_blood` covers mobs only). So one `require = 0` mixin on `VampirePlayer.determineBiteType` returns `BITE_TYPE.NONE` for a victim player with the race tag `tensura:no_blood` (`VampirePlayerBiteMixin`, listed in `docs/mixins.md`; the mod now has 3 mixins). Bloodless **mobs** use the data map.
- Race tags are ManasCore race tags: `ManasRaceInstance.is(TagKey.create(RaceAPI.getRaceRegistryKey(), tensura:undead))`. `tensura:no_blood` (race) already contains `#tensura:undead`, so ghoul is bloodless; `daemon` is not in it.
- The Werewolves faction is matched by id (`werewolves:werewolf`, pinned by `werewolfFactionIdMatchesTheRule`), so `FactionRaceRules` and `WerewolfSkillGrants` link no Werewolves class. `WerewolfSkillGrants` is still registered only when Werewolves is loaded, and the tests that link `WReference` are in `WerewolvesGameTests`, registered only then (22 tests without Werewolves, 26 with).
- Tensura charges a learned skill's magicule cost from max magicule and vetoes it (`UNLOCK_SKILL`, `AbilityHandler`) when there is not enough, so a plain `learnSkill` fails for a low-magicule player (and for a FakePlayer). The grant sets the instance tag `NoMagiculeCost` (Tensura's own switch, removed by Tensura after the event).
- Skills are removed only when a level drop or leaving crosses a grant's threshold (so a skill the player had from another source is not stripped by an unrelated level change).
- The GameTest server has PvP off and Vampirism refuses to bite a player then; the bite test turns it on for its duration.
- Vampirism's `entity_blood` file is at `data/vampirism/data_maps/entity_type/entity_blood.json` and merges per entry with ours (cow stays 10). Entries take `neoforge:conditions`. We list the 34 Tensura entities of the entity-type tag `tensura:no_blood` (Tensura namespace only, so vanilla undead are untouched) with blood 0 under `neoforge:mod_loaded tensura`; `bloodlessEntitiesHaveNoBlood` fails if the tag gains a Tensura entity we lack.
- Tensura's `entity_existence` is a datapack registry (`TensuraCustomData.ENTITY_EXISTENCE`), files at `data/<ns>/entity_existence/<name>.json` with the fields `type`, `min/max_aura`, `min/max_magicule`, `spiritualHealth`, optional `abilities`/`evolution`. Ours are `data/tensura/entity_existence/supernaturalbloodlines_<mod>_<entity>.json` with `neoforge:conditions` (`mod_loaded` vampirism / werewolves), which load cleanly with Werewolves absent. The EP numbers are first guesses scaled against Tensura's zombie (1400 magicule), skeleton, orc and lesser daemon; tune after playtesting.
