# Part: vampirism_enchanted

Requires: `vampirism`, `enchanted`. Werewolves is optional. Package: `net.ofatech.supernaturalbloodlines.parts.vampirismenchanted`. Plan: `docs/superpowers/plans/03-vampirism-enchanted.md`.

Checklist (from the design, section 2). Tick items as they ship.

- [x] Plant parity (data): Enchanted garlic is accepted where Vampirism garlic is (tags, `c:crops/garlic`). Recipes and the cauldron colour only: the garlic crop's effect and the diffusers do not read the tag. See "Data (Task 2)".
- [x] Plant parity (data): Enchanted wolfsbane counts as Werewolves' wolfsbane. Werewolves has no tag and its recipes name the item, so this is two conversion recipes (1 -> 1 and 2 -> 2), not a tag. See "Data (Task 2)".
- [x] Altar power from Vampirism and Werewolves plants and blocks. (GameTest: the data and the lookup the altar uses. **Manual check pending**: a real altar next to a garlic crop.)
- [x] Rite of Cleansing Blood: cures vampirism by leaving the faction. See "Faction rites (Task 3)". **Manual check pending** (chalk circle).
- [x] Rite of the Silver Moon: cures lycanthropy; only when Werewolves is installed. See "Faction rites (Task 3)". **Manual check pending** (chalk circle, full moon).
- [x] Blood Offering: uses Vampirism blood bottles to make a power block. See "Blood Offering, poppets (Task 4)". **Manual check pending** (chalk circle, item matching of full bottles).
- [x] A custom rite factory that calls `IFactionPlayerHandler` (used by all rites above): `supernaturalbloodlines:faction_rite`.
- [x] Silver Poppet: protects against silver and werewolf bites, when Werewolves is installed. **Manual check pending** (visual, recipe at a real spinning wheel, a real werewolf bite).
- [x] Sun Poppet: protects a vampire from one lethal sun-damage hit. **Manual check pending** (visual, recipe at a real spinning wheel, real sunlight).
- [x] Blood values for Enchanted mobs (data map).

Visual checks (textures are generated placeholders; the poppet and blood stone models) and everything that needs a real altar, chalk circle or spinning wheel are **manual check pending**: GameTests start the rite JSON through `RiteType.create(...).start()`, they do not build circles.


## Hook points (verified)

Verified on the jars in the Gradle cache (`javap -p -c`, zip listing, JSON read from the jars): Vampirism 1.10.13, Werewolves 2.0.3.4, Enchanted 4.2.7. The rite lifecycle, the altar data format and the Enchanted poppet rules are not repeated here: see [enchanted_tensura](enchanted_tensura.md) "Hook points" (`Rite`, `RiteFactoryRegistry`, `cancel()`, `PlayerPoppetItem`, `PoppetEvents`) and [vampirism_tensura](vampirism_tensura.md) "Hook points" (`PlayerFactionEvent`, `IFactionPlayerHandler`, `VReference`, `WReference`, bite/blood data maps). They were reused unchanged.

### Vampirism

| Item | Result |
|---|---|
| Sun damage type | `vampirism:sun_damage` (`message_id` `sun`, `ModDamageTypes.SUN_DAMAGE`). The Sun Poppet's predicate is `source.is(ModDamageTypes.SUN_DAMAGE)` (full jar) or, without linking the class, `source.is(ResourceKey.create(Registries.DAMAGE_TYPE, "vampirism:sun_damage"))`. Other keys: `holy_water`, `vampire_on_fire`, `vampire_in_fire`, `blood_loss` (`vampirism:blood_loss`, the `NO_BLOOD` key), `stake`, `minion`, `dbno`, `mother`. |
| Garlic tags | **One tag only: `c:crops/garlic`**, as an item tag **and** a block tag (`ModTags.Items.GARLIC` / `ModTags.Blocks.GARLIC`, both built from the path `crops/garlic` in the `c` namespace). Vampirism ships both files with the single entry `vampirism:garlic`. There is **no** `vampirism:`-namespaced garlic tag. Item ids: `vampirism:garlic` (also the seed, block `vampirism:garlic` is a `CropBlock`), `vampirism:purified_garlic`, `vampirism:garlic_bread`, `vampirism:garlic_finder`, `vampirism:garlic_diffuser_core(_improved)`, blocks `vampirism:garlic_diffuser_weak/normal/improved`. |
| Where the garlic tag is read | Only by data: the recipes `garlic_diffuser_core(_improved)` (alchemical cauldron `fluid`), `purified_garlic` (cauldron `ingredient`), `garlic_bread`, `garlic_finder`, and the `liquid_color` data map entry `#c:crops/garlic` (cauldron colour). `ModTags.Items.GARLIC` is not read by any runtime class (checked: the only classes referencing the field are `DataMapsProvider`, `RecipesProvider` and the two tag providers). |
| Garlic diffusers and the garlic effect | **Do not use the tag or any item.** `GarlicDiffuserBlockEntity` registers an emitter in `LevelGarlic` (`IGarlicChunkHandler`); `VampireBaseEntity`, `VampirePlayer` and `FleeGarlicVampireGoal` query that chunk handler. The only other source is `GarlicBlock.entityInside` (a vampire player standing in a **`vampirism:garlic`** crop gets the garlic effect), which is hardcoded to the block class. So an `enchanted:garlic` crop never hurts a vampire, whatever the tag says. The part does not add that (it would need a mixin or a tick poll); see "Garlic" in Task 2. |
| Blood items | `vampirism:blood_bottle` (item model `blood_bottle_0..9`, the fill level is a component/model, one item id), `vampirism:vampire_blood_bottle`, `vampirism:pure_blood_0..4`, `vampirism:impure_blood_bucket`, `vampirism:human_heart`, `vampirism:weak_human_heart`, holy water `vampirism:holy_water_bottle_normal/enhanced/ultimate` (tag `vampirism:holy_water`, splash variants in `vampirism:holy_water_splash`). Fluid tags `vampirism:vampirism_blood`, `vampirism:vampirism_impure_blood`. |
| `entity_blood` data map | `data/vampirism/data_maps/entity_type/entity_blood.json` (NeoForge registry data map for `minecraft:entity_type`). Format `{"values": {"<entity id>": {"blood": <int>}}}`, per entry optional `neoforge:conditions`. The entries merge with Vampirism's own and ours from other parts (the file has the same path, and it **has to stay one file in this repo**: the data map namespace is `vampirism`, so the path is fixed; add entries to `src/main/resources/data/vampirism/data_maps/entity_type/entity_blood.json`). `blood: 0` means not bitable. Read: `BuiltInRegistries.ENTITY_TYPE.getData(ModDataMaps.ENTITY_BLOOD_MAP, key)` (`ModDataMaps` is in the full jar, reflection as in `VampirismTensuraGameTests.entityBlood`). `BiteableEntryManager.calculate` auto-computes blood from max health (config `autoCalculateEntityBlood`) only for an `Animal` that is not an `IVampire`, whose category is not `MONSTER`/`WATER_CREATURE`, not in the `vampirism:vampire` entity tag and not in the config blacklist. |
| `item_blood` data map | `data/vampirism/data_maps/item/item_blood.json`, same shape: `{"values": {"<item id>": {"blood": <int>}}}` (a food or item's blood content: beef 200, porkchop and mutton 100, `vampirism:human_heart` 200, `weak_human_heart` 100, cooked and chicken 0). Not used by this part. |
| `liquid_color` data map | `data/vampirism/data_maps/item/liquid_color.json`, `"#c:crops/garlic": 12303291` (so the cauldron tints with any member of the garlic tag; Enchanted garlic gets the colour once it is in the tag). |

### Werewolves

| Item | Result |
|---|---|
| Wolfsbane ids | Item **`werewolves:wolfsbane`** (the flower, also what the block drops; no seeds item), block `werewolves:wolfsbane` (and `werewolves:potted_wolfsbane`). Also `werewolves:wolfsbane_finder`, `wolfsbane_diffuser_core(_improved)`, blocks `wolfsbane_diffuser_normal/long/improved`. |
| Wolfsbane tag | **None.** There is no item or block tag for wolfsbane in Werewolves. Its recipes (`wolfsbane_finder`, `wolfsbane_diffuser_core(_improved)`) name the item `werewolves:wolfsbane` directly (cauldron `fluid` and crafting `Y`). So "Enchanted wolfsbane counts as wolfsbane" can only be done with conversion recipes (controller ruling 2). |
| Silver | Item tags `c:ingots/silver` (`werewolves:silver_ingot`), `c:nuggets/silver`, `c:raw_materials/silver`, `c:ores/silver`, `c:storage_blocks/silver`, `c:storage_blocks/raw_silver`, plus `werewolves:tools/silver` (axe, hoe, sword, pickaxe, shovel), `werewolves:armor/silver` (4 pieces) and the union `werewolves:type/silver`. Block tags `c:storage_blocks/silver`, `c:ores/silver`. |
| Werewolf damage | Damage types `werewolves:bite` (tag `werewolves:bite`) and `werewolves:blood_loss`. There is no silver damage type: silver is an item property (`werewolves:type/silver`). |
| Other plants | `werewolves:wolf_berry_bush` (block), `werewolves:jacaranda_sapling`, `werewolves:magic_sapling`. |

### Enchanted 4.2.7

| Item | Result |
|---|---|
| Garlic | Item **`enchanted:garlic`** (also the seed), block `enchanted:garlic` (crop with `age` 0-4, drops 1 + binomial bonus). |
| Wolfsbane | Item **`enchanted:wolfsbane_flower`** and seeds `enchanted:wolfsbane_seeds`; block `enchanted:wolfsbane`. **There is no item `enchanted:wolfsbane`** (the plan's brief says so; the item id is `wolfsbane_flower`). |
| Other plants | Items `belladonna_flower`, `belladonna_seeds`, `mandrake_root`, `mandrake_seeds`, `snowbell_seeds`, `water_artichoke`, `water_artichoke_seeds`, `blood_poppy`, `glint_weed`, `ember_moss`, `spanish_moss`; blocks of the same names without suffix (`belladonna`, `mandrake`, `snowbell`, `water_artichoke`, `blood_poppy`, ...). |
| Altar power of its plants | `garlic`, `wolfsbane`, `belladonna`, `mandrake`, `snowbell`, `water_artichoke`: `{"power": 8, "limit": 20}`. `glint_weed`, `ember_moss`, `spanish_moss`: `8/10`. `blood_poppy`: `30/5` (the rare one). Logs 4/50 and leaves 3/150 are tags. |
| Tags | Enchanted ships no `c:crops/garlic` tag. Its own tags are under `enchanted:` (`crops`, `saplings`, `logs`, ...). |
| Mobs | `enchanted:familiar_cat` is a `Cat` (an `Animal`, category creature), `enchanted:mandrake` is a `Monster` (category monster). Vampirism's auto blood calculation therefore covers the cat (from its max health) but never the mandrake. |

### Consequences for the next tasks

- Tags: `c:crops/garlic` item (Task 2), optionally the block tag; no `vampirism:` garlic tag exists, so there is one file, not two.
- Wolfsbane: no tag to extend, so two shapeless recipes, 1 -> 1 and 2 -> 2 (gated `part_enabled` + `mod_loaded werewolves`), item ids `enchanted:wolfsbane_flower` and `werewolves:wolfsbane`.
- Cure rites (Tasks 3+): `VReference.VAMPIRE_FACTION`, `IFactionPlayerHandler.leaveFaction(boolean)` / `setFactionAndLevel`, `PlayerFactionEvent.FactionLevelChanged` (current faction `null` on leaving). Werewolves by id `werewolves:werewolf` as in vampirism_tensura.
- Sun Poppet predicate: `vampirism:sun_damage`. Enchanted's poppet rule (lethal hit only) applies, see enchanted_tensura "Spirit Poppet".


## Data (Task 2)

All files carry `{ "type": "supernaturalbloodlines:part_enabled", "part": "vampirismEnchanted" }` (implies Vampirism and Enchanted); the Werewolves ones also `{ "type": "neoforge:mod_loaded", "modid": "werewolves" }`. GameTests are in `parts/vampirismenchanted/gametest/VampirismEnchantedDataGameTests`, registered from `VampirismEnchantedPart.init`.

**Garlic** (`data/c/tags/item/crops/garlic.json`): `enchanted:garlic`, `"required": false`. Vampirism has only this one tag (item and block), so there is no second `vampirism:` file. Tag files cannot take conditions, so with `parts.vampirismEnchanted=false` (Enchanted and Vampirism still installed) the tag still contains `enchanted:garlic`: acceptable, it only names an id. The block tag `c:crops/garlic` is not extended: nothing reads it at runtime.
- What this gives: the recipes `garlic_bread`, `garlic_finder`, `purified_garlic`, `garlic_diffuser_core(_improved)` (cauldron fluid) accept `enchanted:garlic`, and the cauldron tints it (`liquid_color` data map `#c:crops/garlic`). Test `enchantedGarlicCountsAsGarlic` asserts tag membership and crafts garlic bread from Enchanted garlic.
- **What it does not give (checked in the bytecode, see Hook points):** garlic diffusers and the garlic effect do not read the tag or any item; a vampire standing in a crop is hurt only by `vampirism:garlic`'s own `GarlicBlock.entityInside`. An `enchanted:garlic` crop therefore does **not** hurt a vampire. Making it do so needs a mixin on `GarlicBlock`-like logic or a per-tick poll; not done (out of scope for a data task). Left as an open decision for the controller.

**Wolfsbane**: Werewolves has no wolfsbane tag, so `data/supernaturalbloodlines/recipe/wolfsbane_from_enchanted.json` (`enchanted:wolfsbane_flower` -> `werewolves:wolfsbane`) and `wolfsbane_from_werewolves.json` (back, **two** werewolves:wolfsbane -> two flowers: a single wolfsbane is Werewolves' own shapeless purple_dye recipe, and a second recipe with the same single input collides with it), shapeless, gated `part_enabled` + `mod_loaded werewolves`. Test `enchantedWolfsbaneConverts` crafts both ways with Werewolves, and asserts one Werewolves wolfsbane still crafts minecraft:purple_dye, and asserts the recipes are absent without it. The brief's item id `enchanted:wolfsbane` does not exist; the item is `enchanted:wolfsbane_flower`.

**Altar power** (`enchanted/altar/block/<block>.json`, format as in enchanted_tensura): all `{"power": 8, "limit": 20}`, the value Enchanted gives garlic, wolfsbane, belladonna, mandrake and snowbell.

| Block | File |
|---|---|
| `vampirism:garlic` | `data/vampirism/enchanted/altar/block/garlic.json` |
| `vampirism:vampire_orchid` | `data/vampirism/enchanted/altar/block/vampire_orchid.json` |
| `werewolves:wolfsbane` | `data/werewolves/enchanted/altar/block/wolfsbane.json` (+ `mod_loaded werewolves`) |
| `werewolves:wolf_berry_bush` | `data/werewolves/enchanted/altar/block/wolf_berry_bush.json` (+ `mod_loaded werewolves`) |

Left out: potted variants, saplings and logs (Enchanted counts logs and leaves through tags). The orchid gets the plain flower value, not the blood poppy's 30/5. Test `vampirismPlantsPowerAltar` reads the entries through `PowerProvider.get(id, ALTAR_BLOCK_REGISTRY)`, the lookup the altar itself uses (no real altar was built here, unlike `magicBlocksPowerAltar`), and asserts the Werewolves entries are absent without Werewolves.

**Blood** (added to the one `data/vampirism/data_maps/entity_type/entity_blood.json`, entries before the Tensura ones): `enchanted:familiar_cat` 3 (= `minecraft:cat`), `enchanted:mandrake` 0 (a root monster; Vampirism never auto-calculates blood for monsters, so this entry only pins the intent). Test `enchantedMobsHaveBlood`.

Runs on this branch: `build`, `runGameTestServer` (65 tests), `-PnoWerewolves` (60), `-PnoLinkedMods` (5), `-PpartsOff=vampirismEnchanted` (61), all passing.


## Faction rites (Task 3)

Code: `parts/vampirismenchanted/rite/` (`FactionRiteFactory`, `RiteTargets`), `werewolves/WerewolfFaction` (the only Werewolves-linking class, called behind `ModList.isLoaded("werewolves")`). Rites: `cleansing_blood` (garlic + holy water + golden apple, power 4000) and `silver_moon` (2 silver ingots + wolfsbane, power 4000, also `mod_loaded werewolves`), both `enchanted:medium_circle` of `enchanted:ritual_chalk`.

- **Factory** `supernaturalbloodlines:faction_rite`: codec `event` (a `BloodlineEvent`, any case), `faction` (`vampire`|`werewolf`|`hunter`|`none`|`vampire_or_werewolf`; `none` = no check, the rules decide), optional `requires_full_moon`. **Extension point:** a new requirement is one more value of `FactionRiteFactory.Faction` plus its `accepts(BaseFaction)` case; a new event needs only a rite JSON. On start: target = `RiteTargets.centrePlayerOrCaster(rite, params, level)` (the closest player in the 1x2x1 column on the centre block, else the caster; `params` was added to the planned signature because Enchanted keeps the caster id private), full moon check (`level.getMoonPhase() == 0`, Enchanted's `time` field is a time-of-day range and has no moon phase), faction check, `BloodlineAccess.applyEvent` with the target's real faction and game time, then the Outcome actions (all `LEAVE_FACTION` first, then the joins; `KILL_PLAYER` is ignored with a debug log). Failures use `cancel()`, so Enchanted gives the ingredients back; the caster gets the message on the action bar (`supernaturalbloodlines.rite.wrong_faction`, `.needs_full_moon`, or the refusal key).
- **The faction check is skipped for ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID and HERETIC**: those are decided by `BloodlineRules` (a hybrid is a werewolf-faction player, and Cleansing Blood has to turn it into a plain werewolf; an Original gets `original_cure`).
- **The silver moon does not need night**, only moon phase 0 (the whole full-moon day).
- GameTests: `VampirismEnchantedGameTests` (`ritesLoad`, `cleansingCuresPlainVampire`, `cureRiteOnNonVampireFails`, `cureRiteRespectsBloodlineOutcome`, `cleansingTargetsThePlayerOnTheCircle`) and, only with Werewolves, `VampirismEnchantedWerewolfGameTests` (`silverMoonCuresPlainWerewolf`, `silverMoonOnHybridMakesPlainVampire`, `silverMoonOutsideFullMoonIsRefused`, `silverMoonOnNonWerewolfFails`, `cleansingKeepsHybridAWerewolf`). They start the real rite JSON with `RiteType.create(...).start()`; the chalk circle and item matching on a real altar are not proven (manual check pending).

**Review fixes (Task 3).** Before anything is changed the factory previews the event with the pure `BloodlineRules.apply` (same context): a refusal, or a result with no actions and an unchanged state (`supernaturalbloodlines.rite.no_effect`), cancels and refunds. The full moon needs a dimension without fixed time (`MoonRule.isFullMoon`; the Nether and End report phase 0 forever). Every JOIN action is checked first by posting Vampirism's `CanJoinFaction` event as if the target had already left (`canJoin` itself only accepts players without a faction); a denial cancels with `supernaturalbloodlines.rite.join_denied` and nothing changes. A `leaveFaction`/`setFactionAndLevel` that fails after the state is saved is logged as a warning. Extra GameTests: `silverMoonOnHereticHasNoEffect`, `deniedJoinChangesNothing`, `factionFieldAcceptsEveryRequirement`; unit test `MoonRuleTest`.


## Blood Offering, poppets (Task 4)

Code: `parts/vampirismenchanted/PartBlocks` and `PartItems` (this part's own DeferredRegisters, registered from `VampirismEnchantedPart.init`; not shared with enchanted_tensura), `item/SunPoppetEffect`, `werewolves/SilverPoppetEffect`. Both effects read damage types and item tags by id and link no Vampirism or Werewolves class; the silver poppet is nevertheless only registered when Werewolves is loaded (`PartItems.SILVER_POPPET` is `null` otherwise), and nothing outside the `werewolves` package names it.

- **Blood Offering** (`data/supernaturalbloodlines/enchanted/circle_magic/rite/blood_offering.json`, no Java): `enchanted:create_item` factory, `enchanted:small_circle` of `enchanted:ritual_chalk`, power 1000, ingredients 4 `vampirism:blood_bottle` **with the component `vampirism:bottle_blood` = `{"blood": 9}`** (a full bottle) and 1 `minecraft:stone`; output 1 `supernaturalbloodlines:blood_stone`. The full-bottle component is needed because the item id is the same for empty bottles; Enchanted matches components that the rite names (`ItemUtils.isSameItemPartial`), so empty or partly filled bottles do not count. Whether a partly filled bottle can be matched by a real altar is a manual check.
- **`blood_stone`**: plain block (generated 16x16 placeholder texture, cube model, loot table drops itself, pickaxe tag entry with `"required": false`). Altar power `{"power": 25, "limit": 16}` (`enchanted/altar/block/blood_stone.json`).
- **Poppets follow Enchanted's lethal-hit rule** (proven in enchanted_tensura): `PoppetEvents.onLivingEntityHurt` only consults a poppet when the incoming damage is at least the player's health and the poppet is bound to that player. So the **Sun Poppet** absorbs a *lethal* hit of `vampirism:sun_damage` (damage zeroed, one durability used) and the **Silver Poppet** a *lethal* hit that is a `werewolves:bite` damage type or comes from a weapon in the item tag `werewolves:type/silver` (`source.getWeaponItem()`), or from an arrow whose ammunition (`getPickupItemStackOrigin()`; for an arrow `getWeaponItem()` is the bow) is in that tag or is `werewolves:crossbow_arrow_silver_bolt` (not in the tag, so named by id). A non-lethal hit lands normally and the poppet is untouched; any other lethal damage is not absorbed and the poppet is not used.
- **Colour and durability** copy Enchanted's plain protection poppets: durability 1, stacks to 1; colour `FIRE` (sun) and `EARTH` (silver). Recipes (`data/supernaturalbloodlines/recipe/spinning/{sun,silver}_poppet.json`, `enchanted:wheel`, power 500): `enchanted:poppet` + `enchanted:anointing_paste` + `minecraft:glowstone_dust` or `werewolves:silver_ingot`. The silver recipe also has `mod_loaded werewolves`.
- **Placeholders:** textures `blood_stone`, `sun_poppet`, `silver_poppet` are generated 16x16 images.
- GameTests: `VampirismEnchantedPoppetGameTests` (`bloodOfferingPowersAltar` (altars at 120 and 160 blocks up, clear of the 40 and 80 of `magicBlocksPowerAltar`: altar blocks stay in the world and the ranges overlap between neighbouring cells) builds two real formed altars with ten stone / ten blood stone and asserts the difference is exactly 250; `bloodOfferingRiteMakesBloodStone`, `bloodOfferingNeedsFullBottles` (four item entities of full bottles plus a stone on a built small ritual-chalk circle match `RiteType.matches`; four empty bottles do not), `sunPoppetAbsorbsSunDamage`, `sunPoppetIgnoresOtherDamage` (a lethal fire hit kills and the poppet is unused), `sunPoppetIgnoresNonLethalHits`, `poppetsMatchEnchantedStyle`, which also pins that the silver item and recipe exist exactly when Werewolves is loaded) and, only with Werewolves, `VampirismEnchantedSilverPoppetGameTests` (`silverPoppetBlocksSilver`, `silverPoppetBlocksSilverArrows`, `silverPoppetBlocksWerewolfBite`, `silverPoppetIgnoresOtherDamage`, `silverPoppetMatchesEnchantedStyle`). The silver tests use a zombie as attacker: the test server has PvP off, so a player attacker never damages anyone. Mutation check: making either predicate accept everything fails `sunPoppetIgnoresOtherDamage` and `silverPoppetIgnoresOtherDamage`.
- **Disabled part:** same rules as enchanted_tensura "Disabled part": the block and items vanish from a world if the part is switched off or Werewolves removed (the silver poppet); every data file carries `part_enabled` (`vampirismEnchanted`), the silver recipe also `mod_loaded werewolves`.

## Checks (Task 5)

`runClient -PnoLinkedMods` and `runClient -PnoWerewolves` were started in the background and left until the log ended at "Loaded 0 entity animations" (the title screen was up); the logs were searched for `ERROR`, `Exception` and `NoClassDefFound` mentioning `supernaturalbloodlines` or `net.ofatech`. Results are in the Task 4+5 report. Nothing was clicked in game: the visual items, the real altar and the real chalk circles stay **manual check pending** (see the checklist).
