# Part: enchanted_tensura

Requires: `enchanted`, `tensura`. Package: `net.ofatech.supernaturalbloodlines.parts.enchantedtensura`. Plan: `docs/superpowers/plans/02-enchanted-tensura.md`.

Checklist (from the design, section 1). Tick items as they ship.

- [x] Altar power from Tensura magic blocks (data): magic ore, crystal and magisteel block ids, listed from the Tensura jar. See "Data (Task 2)".
- [x] Curse *Magicule Drain*: slowly lowers magicules while active. See "Curses (Task 3)". **Manual rite check pending.**
- [x] Curse *Skill Seal*: one random learned skill can't be activated while active (`SkillEvents.ACTIVATE_SKILL` interrupt). See "Curses (Task 3)". **Manual rite check pending.**
- [x] Rite of Purification: holy damage in an area to Tensura-undead races and `minecraft:undead`; the damage type id contains "holy". See "Rites (Task 4)". **Manual rite check pending.**
- [x] Rite *Magicule Offering*: the caster's magicules become a placeable `condensed_magicule` block with altar power (custom factory). See "Rites (Task 4)". **Manual rite check pending.**
- [ ] Spirit Poppet (code): protects against damage in `tensura:is_spiritual`.
- [x] EP for Enchanted mobs (data): `entity_existence` entries. **Visual check pending** (Task 2 Step 3): `runClient`, spawn an Enchanted mandrake and familiar cat and open Tensura's analysis. Replaced by the GameTest `enchantedMobsHaveEp` (registry contents), which does not prove the analysis screen.


## Hook points (verified)

Verified against the jars in the Gradle cache (`javap -p`, zip listing): Tensura 2.0.1.5, ManasCore skill 4.0.0.2 (`build/manascore-modules`), Enchanted 4.2.7 jar plus its sources on GitHub `Favouriteless/Enchanted`, branch `1.21.1`. The `javap` and source reads agree.

### Tensura

| Item | Result |
|---|---|
| Magic ore blocks | `tensura:magic_ore`, `tensura:deepslate_magic_ore` (the two members of block tag `tensura:magic_ores`). |
| Magic storage / crystal / magisteel blocks | `tensura:magic_ore_block`, `tensura:low_quality_magic_crystal_block`, `tensura:medium_quality_magic_crystal_block`, `tensura:high_quality_magic_crystal_block`, `tensura:low_magisteel_block`, `tensura:high_magisteel_block`, `tensura:pure_magisteel_block`. |
| Magic metal blocks ("similar") | `tensura:mithril_block`, `tensura:orichalcum_block`, `tensura:adamantite_block`, `tensura:hihiirokane_block`. |
| Left out on purpose | stairs, slabs, walls, bricks, chiseled and `*_magic_engine` variants (decoration or machines, many ids), `tensura:labyrinth_crystal`, `tensura:silver_*` (not magic), `tensura:kiln_*`. |
| EP storage | `io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(LivingEntity): IExistence` (impl `ExistenceStorage`). |
| `IExistence` magicule | `double getMagicule()`, `void setMagicule(double)`. Also `getAura/setAura`, `getEP/setEP`, `getSpiritualHealth/setSpiritualHealth`, `markDirty()`. |
| Magicule max attribute | `io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_MAGICULE` (`Holder<Attribute>`). Also `MAX_AURA`, `MAGICULE_REGENERATION_MULTIPLIER`, `LIMITED_SPIRITUAL_MAX_MAGICULE`. Read the cap with `entity.getAttributeValue(TensuraAttributes.MAX_MAGICULE)`. |
| EP data registry | `TensuraCustomData.ENTITY_EXISTENCE` (datapack registry of `EntityExistenceData`: `entity`, `spiritualHP`, `minMagicule`, `maxMagicule`, `minAura`, `maxAura`, optional `abilities`, `evolution`, `abilitiesRandom`). Files: `data/tensura/entity_existence/*.json`. The JSON key for the entity id is `type` (the record field is `entity`); the file name does not matter. |
| Holy damage type | `tensura:holy_damage` (`TensuraDamageTypes.HOLY_DAMAGE`). `TensuraDamageHelper.isHoly` is also true for any damage whose `message_id` contains `holy` or `divine` (see vampirism_tensura notes). |
| `tensura:is_spiritual` | **Damage-type** tag, `data/tensura/tags/damage_type/is_spiritual.json`: `tensura:soul_scatter`, `tensura:soul_consumed`, `tensura:synthesise`. Nothing else. A Spirit Poppet that checks this tag protects against those three only (and any damage type another mod adds to the tag). |

### ManasCore skill (`SkillEvents.ACTIVATE_SKILL`)

- Field: `dev.architectury.event.Event<SkillEvents.SkillActivationEvent> SkillEvents.ACTIVATE_SKILL`.
- Listener: `EventResult activateSkill(Changeable<ManasSkillInstance> skill, LivingEntity owner, int keyNumber, int mode)`. Registered with `SkillEvents.ACTIVATE_SKILL.register(...)`; it fires for any `LivingEntity`, so check `owner instanceof ServerPlayer`.
- The only call site is `SkillStorage.startHoldSkill(ManasSkillInstance, int keyNumber, int mode)` (the `ResourceLocation` overload delegates). It fires **before** `canActivateSkill`, cooldown and `onPressed`.
- **Interrupt:** return `EventResult.interruptFalse()` (the caller does `if (result.isFalse()) return false`, so the skill does not run). Setting the `Changeable` to `null` also aborts (`skill == null` returns false). `EventResult.pass()` lets it through.
- Related: `SkillEvents.TOGGLE_SKILL` (`toggleSkill(Changeable<ManasSkillInstance>, LivingEntity)`) is the same shape for toggles, and `RELEASE_SKILL` for the release of held skills. Skill Seal should block activation **and** toggle.
- Skills API: `SkillAPI.getSkillsFrom(LivingEntity): SkillStorage` (implements `Skills`): `getLearnedSkills()`, `getSkill(ResourceLocation)`, `learnSkill(...)`, `forgetSkill(...)`, `forEachSkill(...)`. `ManasSkillInstance`: `getSkillId()`, `isTemporarySkill()`, `canActivateSkill(entity, mode)`, `isToggled()`, `onToggleOff(entity)`.

### Enchanted 4.2.7

Altar power (read by `AltarPowerData` / `common.enchanted.altar.PowerProvider`):

- Datapack registries `EData.ALTAR_BLOCK_REGISTRY` and `EData.ALTAR_TAG_REGISTRY`. Files live at `data/<block namespace>/enchanted/altar/block/<block path>.json` (and `.../altar/tag/<tag path>.json` for block tags); the registry id is `<namespace>:<path>`. Enchanted itself ships `data/enchanted/enchanted/altar/block/belladonna.json` and `data/minecraft/enchanted/altar/block/dirt.json`, so `data/tensura/enchanted/altar/block/magic_ore.json` is the right place for `tensura:magic_ore`.
- Format: `{"power": <int>, "limit": <int>}` (record `PowerProvider(int power, int limit)`, both required ints). An altar adds `power` per block up to `limit` blocks of that kind.
- Lookup: **by block first, then by tag** (`tryChangeBlock`: `PowerProvider.get(level, block)`, and only if absent the first block tag with an entry). So a per-block file is enough and a Tensura block that is also in a tagged group is not double counted.
- The altar scans an ellipsoid of `ServerConfig.altarRange` around its centre in `setupBlockData()` (first tick), and later changes through `AltarStateObserver`. `AltarBlockEntity.firstTick()` is public, `powerData` (private) holds the capacity (`getCapacity()`), and `createMenu(...)` exposes capacity as container data index 1.

Rites:

- `net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite` (abstract). Constructor `protected Rite(Rite.BaseRiteParams baseParams, Rite.RiteParams params)`. `BaseRiteParams` is a record `(RiteType type, ServerLevel level, BlockPos pos, int tickPower)`. `RiteParams` has public `UUID caster`, `@Nullable UUID target`, mutable `List<ItemStack> consumedItems`, and `ticks()`; created with `RiteParams.of(caster, items)` or `RiteParams.empty()` (private constructor), which a unit test can call directly.
- Lifecycle: `start()` first sets `params.target` from the consumed item's `ENTITY_REF` component (override `findTargetUUID` to change), then calls `onStart(RiteParams)`; returning `false` stops the rite at once. Each server tick `tick()` charges `tickPower` from the gold chalk's altar power (if it cannot pay the rite stops), then calls `onTick(RiteParams)`; **return `true` to keep going, `false` to finish** (the default returns `false`). `stop()` calls `onStop(RiteParams)` and detaches the chalk. Failure: call `cancel()` from `onStart`/`onTick`; it plays a sound, drops the consumed items back at the chalk, detaches and returns `false`, so `return cancel();` is the "fail the rite and take nothing" idiom.
- Caster and centre: the caster is a UUID (`params.caster`), resolved with `protected Entity findEntity(UUID)` (cached; searches all levels). The centre is `protected final BlockPos pos`, the gold chalk block the rite runs on; `level` is the `ServerLevel`. There is no radius field: each rite chooses its own.
- The one-off `power` in the rite JSON is paid before the rite starts (by the chalk); `tick_power` is the per-tick cost passed as `tickPower`.
- Factories: `net.favouriteless.enchanted.api.circle_magic.RiteFactory` (`Rite create(BaseRiteParams, RiteParams)`, `ResourceLocation id()`, optional `getOutputs()`), registered with `RiteFactoryRegistry.get().register(id, MapCodec<? extends RiteFactory>)`. In the rite JSON the factory is `{"type": "<factory id>", ...fields}`.
- Rite JSON (`data/<ns>/enchanted/circle_magic/rite/<name>.json`): `items` (list of `{id, count}`), `shapes` (`"enchanted:small_circle": "enchanted:ritual_chalk"`), optional `entities`, `power`, `tick_power`, `weather`, `time` (`[from, to]`), `dimensions`, and `factory`. Built-in factory ids: `apply_curse`, `remove_curse`, `sanctity`, `protection`, `sky_wrath`, `summon_entity`, `command`, `create_item`, and others (see `ERiteFactories`). `command` and `apply_curse` can be used from data alone.

Curses (`net.favouriteless.enchanted.api.curses`):

- `Curse` interface: `onInitialise(ServerPlayer, int strength, long age)`, `onRemove(...)`, `tick(...)`, `getMaxStrength()` (default 2), `type()`. Register a `Curse.Type<T>(ResourceLocation id, Supplier<T>, MapCodec<T>)` with `CurseManager.get().register(type)` during common setup (duplicate id throws). Curses are stored per player UUID in saved data and are re-initialised (`onInitialise`) on login (`CommonEvents.onPlayerLoggedIn`; the javadoc also promises clone/respawn, which was not checked in code).
- `CurseManager.applyCurse(type, uuid, curseLevel, ServerLevel)`, `removeCurse(type, uuid, ServerLevel)` (marks removed; `onRemove` runs on the next level tick for online players), `getCurses(uuid, ServerLevel)`. `RemoveCurseFactory`/`ApplyCurseFactory` take `{"curse": "<curse id>"}`; the **remove-curse rite calls `removeCurse`, which ends in `Curse.onRemove`**, so everything a curse changes must be undone in `onRemove`.
- Enchanted does **not** remove curses on death (nothing in `CommonEvents`); a death-clears-curse rule has to be ours, using `CurseManager.removeCurse`.

Poppets:

- `PoppetColour` is a record `(int primary, int secondary)` with constants `EARTH, EQUIPMENT, FIRE, HUNGER, MAGIC, VOID, VOODOO_PROTECTION, WATER`.
- `PlayerPoppetItem(PoppetColour colour, PlayerPoppetEffect effect, Item.Properties properties)`. `PlayerPoppetEffect` (api) has `boolean protectsAgainst(DamageSource)` and `void protect(Player)`. Enchanted's helpers: `EffectsPlayerPoppetEffect.ofDefault(Predicate<DamageSource>)` / `ofDefaultPlus(predicate, effects...)` (default effect: Regeneration II for 100 ticks) and `ConsumerPlayerPoppetEffect`. Durability is set in the `Item.Properties` (Enchanted's `poppetProps(n)`).
- `PoppetEvents.onLivingEntityHurt` only triggers a poppet when the incoming amount is **at least the player's current health** (a would-be lethal hit), the poppet is bound to the player (`PoppetHelper.isBoundTo`), and `protectsAgainst(source)` is true. Poppets are looked up in the inventory and the poppet shelves (`PoppetHelper.tryTriggerAll`). Each trigger costs one durability.
- Enchanted's own poppet predicates (`PoppetPredicates`) use `DamageSource.is(DamageTypeTags / DamageTypes)`; a Spirit Poppet predicate would be `s -> s.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("tensura:is_spiritual")))`.

Entities (`EEntityTypes`, all in namespace `enchanted`): `broomstick`, `familiar_cat`, `mandrake`, `throwable_brew`, `voodoo_item`. Only `familiar_cat` (a `FamiliarCat`, tamed cat) and `mandrake` (monster) are living mobs that Tensura can analyse; the other three are vehicles or projectiles/item entities. So EP data is needed for exactly those two.

Damage types: `enchanted:sacrifice`, `enchanted:sound`, `enchanted:voodoo`, `enchanted:internal_explosion`.

## ManasCore and Tensura import packages (verified)

- `io.github.manasmods.manascore.network.api.util.Changeable`: the mutable wrapper passed to `ACTIVATE_SKILL` and `TOGGLE_SKILL`.
- `io.github.manasmods.manascore.skill.api`: `SkillAPI`, `SkillEvents`, `ManasSkillInstance`, `Skills`, `ManasSkill`.
- `io.github.manasmods.manascore.skill.impl`: `SkillStorage` (the class that calls `ACTIVATE_SKILL`; do not use it directly).
- `io.github.manasmods.manascore.race.api`: `RaceAPI`, `RaceEvents`, `ManasRaceInstance`.
- `io.github.manasmods.tensura.storage`: `TensuraStorages` (`getExistenceFrom`). `io.github.manasmods.tensura.storage.ep`: `IExistence`, `ExistenceStorage`.
- `io.github.manasmods.tensura.registry.attribute`: `TensuraAttributes` (`MAX_MAGICULE`).
- `io.github.manasmods.tensura.registry.data`: `TensuraCustomData` (`ENTITY_EXISTENCE`). `io.github.manasmods.tensura.data.existence`: `EntityExistenceData`.
- `io.github.manasmods.tensura.damage`: `TensuraDamageHelper`, `TensuraDamageTypes`. `io.github.manasmods.tensura.registry.block`: `TensuraBlocks`.
- `dev.architectury.event`: `Event`, `EventResult` (used by both skill and race events).

## Magicule Offering: how a rite can give power or items

The design uses a placed `condensed_magicule` block whose altar power comes from data, so no power API is needed. The rite (a factory or a data rite) removes the caster's magicules and places the block; the altar then counts it through its normal `enchanted/altar/block/condensed_magicule.json` entry. A custom rite needs only `Rite` and the level to place a block; no Enchanted power interface is called. Items use the same idea: `onStart`/`onTick` place or spawn the item, and `cancel()` refunds the consumed items when the caster lacks magicules.

## Data (Task 2)

Files under `src/main/resources/data/tensura/`, all with `neoforge:conditions` `mod_loaded enchanted`.

**Altar power** (`enchanted/altar/block/<block>.json`; path and format verified in the Enchanted jar, see above. The plan's path matches how Enchanted loads it, so nothing differs from the plan. Enchanted counts power by block first, then by tag; our files are per block):

| Blocks | `power` | `limit` |
|---|---|---|
| `magic_ore`, `deepslate_magic_ore` | 6 | 20 |
| `magic_ore_block`, `low_/medium_/high_quality_magic_crystal_block`, `low_/high_/pure_magisteel_block`, `mithril_block`, `orichalcum_block`, `adamantite_block`, `hihiirokane_block` | 10 | 10 |

Plain stone adds nothing. Grass, dirt and similar already carry Enchanted data, which is why the test altars are built high up.

**EP** (`entity_existence/supernaturalbloodlines_enchanted_<mob>.json`; only the two living Enchanted mobs, the other three entities are a vehicle, a projectile and an item-like entity):

| Entity | magicule min-max | aura min-max | spiritualHealth | Reasoning |
|---|---|---|---|---|
| `enchanted:mandrake` (small hostile plant) | 300-500 | 50-100 | 30 | a little below Tensura's `cattledeer` (500-700) |
| `enchanted:familiar_cat` (bound pet cat) | 600-900 | 100-200 | 60 | a bit above the cattledeer, far below `tensura:winged_cat` (8000-9000) |

**GameTests** (`parts/enchantedtensura/gametest/EnchantedTensuraDataGameTests`, registered from `EnchantedTensuraPart.init`):

- `magicBlocksPowerAltar`: builds two **real** formed Enchanted altars (`AltarHelper.tryFormAltar`, `AltarBlockEntity.firstTick`), one with ten stone and one with ten `tensura:magic_ore` beside it, reads the capacity through `AltarMenu.getMaxPower`, and asserts ore minus stone is exactly 60. Both altars are 40 or more blocks above the floor and 40 apart, because the altar range is 32 and the grass and dirt floor has altar power of its own (a first run showed 200 with plain stone at floor height).
- `altarPowerDataLoaded`: every block above has the expected `power` and `limit` in Enchanted's altar block registry.
- `enchantedMobsHaveEp`: Tensura's `ENTITY_EXISTENCE` registry holds both mobs (control entry: `tensura:orc`).

## Curses (Task 3)

Code: `parts/enchantedtensura/curse/` (`Curses.register()` from the part init, `MagiculeDrainCurse`, `SkillSealCurse`). Rites: `data/supernaturalbloodlines/enchanted/circle_magic/rite/curse_magicule_drain.json` and `curse_skill_seal.json` (`enchanted:apply_curse`, `enchanted:medium_circle` with `enchanted:nether_chalk`, power 2000, conditions: enchanted and tensura loaded). Items: `enchanted:taglock` (the **filled** taglock; `taglock_kit` is the empty one), a Tensura magic crystal (low quality for the drain, medium for the seal) and `minecraft:soul_sand` (Tensura has no soul item; this is a choice, easy to change). Lang: `rite.supernaturalbloodlines.<rite>` (Enchanted's key format) and `curse.supernaturalbloodlines.<curse>` (Enchanted has no curse-name key; ours is unused by it).

- **Magicule Drain.** Every 20 calls of `tick` (one per server tick) it removes `max(1, MAX_MAGICULE * 0.005 * (strength + 1))`, never below 0. It counts its own calls: Enchanted 4.2.7 passes an `age` that grows by 2 a tick and is not restored on load (`CurseInstanceImpl`'s constructor ignores `level` and `age`), so `age` cannot time a period. Nothing to undo in `onRemove`: the magicules already spent are spent and Tensura regenerates them.
- **Skill Seal.** The curse holds `seed` and the sealed skill id in its codec. The choice: learned skill ids sorted by string, index `floorMod(seed, n)`. It is made in `onInitialise` and again in `tick` while nothing is sealed, because Enchanted only calls `onInitialise` on login and clone, not when a rite curses an online player. No skills means nothing is sealed and nothing happens; a skill learned later gets sealed. `SkillEvents.ACTIVATE_SKILL` is interrupted for the sealed skill, and `TOGGLE_SKILL` too unless the skill is already toggled on (so it can always be switched off). The listeners ignore a curse already marked removed, so the skill is usable the moment the remove-curse rite marks it, before `onRemove`.
- **Death.** A `LivingDeathEvent` listener (lowest priority, skips cancelled) calls `CurseManager.removeCurse` for both, so `onRemove` runs on Enchanted's next curse tick.
- **Enchanted 4.2.7 quirks found** (not ours to fix): a first `applyCurse` always creates strength 0 (the level argument is dropped; only a second application raises it); strength and age are not restored on load.
- **Manual rite check pending.** The GameTests start the real rite JSON through `RiteType.create(...).start()` with a filled taglock on a second FakePlayer, and Enchanted's own `CurseManagerImpl.tick` for `onRemove`. They do not prove the chalk circle, the item matching on a real altar, or the client.

GameTests (`EnchantedTensuraGameTests`): `drainReducesMagicules`, `neverBelowZero`, `sealChoiceIsDeterministic`, `skillSealBlocksChosenSkill`, `cursesSurviveSaving`, `skillSealWithNoSkillsIsNoop`, `removingSkillSealRestoresSkill`, `cursesAreClearedOnDeath`, `curseRitesLoad`, `ritesCurseTheTaglockTarget`.

## Rites (Task 4)

Code: `parts/enchantedtensura/rite/` (`PurificationRiteFactory`, `MagiculeOfferingRiteFactory`; both registered with `RiteFactoryRegistry` from the part init), `PartBlocks` and `PartItems` (DeferredRegisters for `condensed_magicule`, registered from the part init; Task 5 reuses `PartItems`). A rite that finishes in `onStart` returns `false` (Enchanted then calls `stop()`); a failure returns `cancel()`.

- **Purification** (`supernaturalbloodlines:purification`, `damage` default 8): on start, `hurt` with `supernaturalbloodlines:holy_purification` for every living entity within 8 blocks (sphere, from the centre of the chalk block) that is `RaceUtils.isUndead` or in the `minecraft:undead` entity tag. The damage type is `data/supernaturalbloodlines/damage_type/holy_purification.json` (`message_id` `holy_purification`, so Tensura's `isHoly` is true; GameTest `holyPurificationDamageIsHoly`). Rites: `purification` (2 gold ingots + glowstone dust) and `purification_holy_water` (gold ingot + `vampirism:holy_water_bottle_normal`, also needs Vampirism), both `enchanted:medium_circle` of `enchanted:ritual_chalk`, power 3000.
- **Magicule Offering** (`supernaturalbloodlines:magicule_offering`, `magicules` default 5000, `count` default 1, 1-64): on start it needs `IExistence.getMagicule() >= magicules`, subtracts them and drops `count` condensed magicule one block above the chalk. With too few it sends the caster `supernaturalbloodlines.enchanted_tensura.offering_failed` and returns `cancel()` (Enchanted gives the items back; nothing is taken). If the caster cannot be found (offline) it cancels silently. Rite `magicule_offering`: low quality magic crystal + amethyst shard, `enchanted:small_circle`, power 0. The factory lists the block as its output.
- **`condensed_magicule`**: plain block (placeholder generated 16x16 texture, cube model, loot table drops itself, pickaxe-mineable); altar power `{"power": 30, "limit": 16}` from `data/supernaturalbloodlines/enchanted/altar/block/condensed_magicule.json`.
- **Enchanted 4.2.7:** `findEntity` only finds players that are in the level, so the GameTests add their FakePlayers with `ServerLevel.addNewPlayer` first.
- **Tensura listener note (Task 3 follow-up):** Tensura's own `ACTIVATE_SKILL` listener interrupts first for a test player, so firing the event proves nothing about the seal. `Curses.onActivate` / `Curses.onToggle` are the seal's listener bodies and the GameTests call them directly. A blocked activation or toggle-on sends the player the action-bar message `supernaturalbloodlines.enchanted_tensura.skill_sealed` (only when Tensura's listener lets the activation through).
- **Manual rite check pending.** The GameTests start the rite JSON through `RiteType.create(...).start()`. They do not prove the chalk circle, item matching on a real altar, the placeholder texture, or the client.

GameTests (`EnchantedTensuraRiteGameTests`): `ritesLoad`, `holyPurificationDamageIsHoly`, `condensedMagiculeIsAnAltarBlock`, `purificationHurtsUndeadRace`, `purificationHurtsUndeadMobsInRangeOnly`, `purificationSparesLiving`, `offeringFailsWithoutMagicules`, `offeringConsumesMagicules`.
