# Part: artifacts

Requires: `vampirism`, `enchanted`. Werewolves and Curios are optional (the Moon Ring needs Werewolves). Package: `net.ofatech.supernaturalbloodlines.parts.artifacts`. Plan: `docs/superpowers/plans/06-artifacts.md`.

Checklist (from the design, section 6). Tick items as they ship.

- [x] `ArtifactSlots.isWorn(player, item)` / `findWorn(player, item)`: the one helper for "is this ring equipped" (Curios first when Curios is loaded, then main inventory, armor and offhand). Tests: `ringWorksWithoutCurios`, `ringFoundInCuriosSlot`, `curiosCheckedBeforeInventory`.
- [ ] Sun Ring (rite *Ward of Daylight*): no sun damage while worn; bound to one vampire by UUID in a data component.
- [ ] Moon Ring (rite *Ward of the Moon*, Werewolves): no forced full-moon transformation.
- [ ] Resurrection Ring (rite *Ward of Return*): cancels death from a supernatural attacker, 3 charges.
- [ ] Vervain: crop and tea; burns vampires and hybrids that bite or hit barehanded; compulsion immunity; harms vampires who eat it.
- [ ] White Oak Ash Dagger: *Desiccated* on an Original until the dagger is pulled; heavy damage to a supernatural wielder.
- [ ] The Cure (rite *Rite of the Cure*): removes every layer and leaves the faction; sets `BloodlineEvent.DRANK_THE_CURE`.
- [ ] Vampire Compass: points to the nearest vampire, hybrid or Original within 64 blocks.
- [ ] Hearing Device: stuns supernaturals within 5 blocks for 5 s; 60 s cooldown.
- [ ] Mountain ash (with packs): dust line, ash tree, blocks supernatural entities.
- [ ] Taser (with packs): *Shocked* effect for werewolves.

## Slot helper and Curios (Task 1)

- `ArtifactSlots` (`parts/artifacts`) is the only entry point. `InventorySlots` (package-private) walks `inventory.items` (hotbar included), `armor`, `offhand`. `curios/CuriosSlots` is the only class that imports Curios; `ArtifactSlots` calls it only behind `ModList.get().isLoaded("curios")`, so with `-PnoCurios` no Curios class is ever loaded (the inventory test would throw `NoClassDefFoundError` otherwise).
- `findWorn` returns the stack itself (not a copy), Curios first, so callers can read and edit data components (bound UUID, charges).
- **Curios 9.5.1+1.21.1** (`top.theillusivec4.curios:curios-neoforge`, repo `maven.theillusivec4.top`; `api` classifier for compile, full jar for `localRuntime`; range `[9.5,10)` in `neoforge.mods.toml`, optional). API packages: `top.theillusivec4.curios.api` (`CuriosApi`, `CuriosCapability`, `SlotContext`, `SlotResult`, `SlotTypePreset`, `CuriosTags`), `.api.type.capability` (`ICuriosItemHandler`, `ICurio`, `ICurioItem`), `.api.type.inventory` (`ICurioStacksHandler`, `IDynamicStackHandler`), `.api.event` (`CurioChangeEvent`, `CurioCanEquipEvent`, `CurioCanUnequipEvent`, `DropRulesEvent`, ...), `.api.client` (`ICurioRenderer`).
- **Registering a ring needs no code in 9.x.** `CuriosApi.registerCurio(Item, ICurioItem)` exists but is only for custom equip behaviour. A ring is equippable when the item is in the item tag `curios:ring` (`data/curios/tags/item/ring.json`, entries `required: false`); the `ring` slot's validator is `curios:tag`. Task 2 adds that tag with the ring items.
- **The player has no Curios inventory until a data file gives it slots.** Curios ships slot types only, no entity assignment. We ship `data/supernaturalbloodlines/curios/entities/player.json` (`entities: ["player"]`, `slots: ["ring"]`). Curios ignores `neoforge:conditions` in that folder (verified: with `-PpartsOff=artifacts` it still logs "Loaded 1 curio entities"), so an empty ring slot also shows with the part off. Harmless.
- **The OFA pack runs Accessories**, which ships a Curios compatibility layer: Curios API calls are expected to go through to Accessories slots (not verified here). `-PnoCurios` only proves the no-Curios path; the Accessories-with-compat path needs a manual check in the pack (Task 8).
- Gotchas found by the tests:
  - `CurioInventory` caches `findFirstCurio` per player for the current game tick (`firstCurioCache`). A lookup made before an equip in the same tick returns the stale "none". Irrelevant in play (equip and check are on different ticks), but a GameTest must not query the same player before equipping.
  - `Inventory.contains(Predicate)` is patched by Curios and also looks into curio slots. To ask "is it in the vanilla inventory", walk `items`, `armor` and `offhand` (as `InventorySlots` does).
  - Tests put a stack straight into `getStacksHandler("ring").getStacks()`, because the stand-in item is not in `#curios:ring`.

## Hook points (verified against the 1.21.1 jars)

Vampirism 1.10.13:

- **Sun damage.** Damage type `vampirism:sun_damage` (`ModDamageTypes.SUN_DAMAGE`, full jar; `message_id` `sun`). **No sun event and no per-player skip hook.** The related events are only `VampireFogEvent` and the registry `ISundamageRegistry` (`isGettingSundamage(LivingEntity, LevelAccessor)`, biome/dimension settings, `data/<ns>/vampirism/no_sun_damage.json`; per biome and dimension, not per player). For players it runs in `VampirePlayer.onUpdate -> isGettingSundamage(level, true) -> handleSunDamage(boolean)` (private): it counts `ticksInSun` (cap 100), reads the `vampirism:sunscreen` effect (`ModEffects.SUNSCREEN`, amplifier 0..4), and past 50 ticks deals damage; at 100 ticks it can kill outright (`vpSundamageInstantDeath`, `DamageHandler.kill`, then `turnToAsh`); it also adds Nausea and Weakness above configured levels. `isGettingSundamage` is false while the main hand holds the Umbrella.
  - Options for the Sun Ring (Task 3): (a) cancel `LivingIncomingDamageEvent` when `source.is(SUN_DAMAGE)` and the victim wears its bound ring (the damage type key can be built without linking the class: `ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"))`; see `vampirism_enchanted.md`); that stops the damage but not Nausea/Weakness/instant death, which are applied without a damage event; (b) additionally keep a short `vampirism:sunscreen` effect on the wearer, since `handleSunDamage` reads it. Task 3 decides, with a GameTest.
- **No glowing / compulsion API.** There is no compulsion class or event. `GlowingEyeCommand` / `EyeCommand` only set eye rendering. `IVampireVision` (`NightVision`, `BloodVision`) is the nearest "see through" feature and is client side. Vervain's compulsion immunity is therefore ours: a rule (`VervainRules.protects`) that plan 8's compulsion checks (core `CompulsionImmunity` registry).
- **Bite.** Victims implement `IBiteableEntity` (`canBeBitten(IVampire)`, `onBite(IVampire)`); the vampire side is `VampirePlayer.biteEntity(int)` / `determineBiteType` (`IVampirePlayer.BITE_TYPE`). Neither fires an event. The only event on a bite is `BloodDrinkEvent` (`PlayerDrinkBloodEvent` / `EntityDrinkBloodEvent`; not cancellable, but `setAmount` and `setSaturationModifier` work; `getBloodSource().getEntity()` should be the victim; not yet exercised). That is the hook for "vampires burn when they bite a vervain wearer": listen for `BloodDrinkEvent.PlayerDrinkBloodEvent`, check the victim with `VervainRules.protects`, hurt `getVampire()`'s player entity and set the amount to 0. A hit barehanded is plain `LivingIncomingDamageEvent` / `LivingDamageEvent.Post` on the victim with an empty-hand attacker.
- **Vampire eating.** Vampirism's own handler is `ModPlayerEventHandler.onItemUse(LivingEntityUseItemEvent.Finish)`: for a vampire finishing a `GarlicBreadItem` it calls `DamageHandler.affectVampireGarlicDirect(IVampire, EnumStrength.MEDIUM)` (`de.teamlapen.vampirism.util`, full jar). That is the precedent for "vampires who eat vervain take damage": our own `LivingEntityUseItemEvent.Finish` listener, `Helper.isVampire`-style faction check via `VampirismAPI.factionPlayerHandler(p)`, then hurt the player (or call `affectVampireGarlicDirect` if linking the full jar is acceptable). `onItemUse(Start)` only cancels use for faction-restricted items (`IFactionExclusiveItem`, `IFactionLevelItem`) and the "cannot interact" state.
- **Forced full-moon shift (Werewolves 2.0.3.4).** `WerewolfPlayer.onUpdate()` once a second toggles a form action through `ActionHandler.toggleAction`, which fires the cancellable `ActionEvent.ActionActivatedEvent`. A voluntary shift comes from a packet outside the player tick. Already implemented for hybrids in `parts/species/hybrid/FormControl` (thread-local "inside this player's `PlayerTickEvent`" flag; no mixin); notes in `docs/parts/species.md`. The Moon Ring (Task 2) reuses that detection through `shared.werewolves.MoonControl` (see the plan 6 ledger).

Enchanted 4.2.7:

- **Kettle recipe JSON**, `data/<ns>/recipe/<name>.json`, type `enchanted:kettle`:
  ```json
  {
    "type": "enchanted:kettle",
    "cook_colour": "801E17",
    "final_colour": "DE311D",
    "ingredients": [ { "id": "minecraft:redstone", "count": 1 }, { "id": "enchanted:mandrake_root", "count": 1 } ],
    "power": 1000,
    "requires_bottle": true,
    "result": { "id": "enchanted:redstone_soup", "count": 1 }
  }
  ```
  `power` (altar power, optional, absent = none) and `requires_bottle` (optional, default false) are optional; colours are hex without `#`. Ingredients are `{id, count}` (all seen examples name items; tags not checked). Vanilla examples: `data/enchanted/recipe/kettle/*.json` in the Enchanted jar. Wrap with `neoforge:conditions` (`part_enabled` artifacts).
