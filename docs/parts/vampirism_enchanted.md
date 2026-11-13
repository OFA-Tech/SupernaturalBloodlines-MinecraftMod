# Part: vampirism_enchanted

Requires: `vampirism`, `enchanted`. Werewolves is optional. Package: `net.ofatech.supernaturalbloodlines.parts.vampirismenchanted`. Plan: `docs/superpowers/plans/03-vampirism-enchanted.md`.

Checklist (from the design, section 2). Tick items as they ship.

- [x] Plant parity (data): Enchanted garlic is accepted where Vampirism garlic is (tags, `c:crops/garlic`). Recipes and the cauldron colour only: the garlic crop's effect and the diffusers do not read the tag. See "Data (Task 2)".
- [x] Plant parity (data): Enchanted wolfsbane counts as Werewolves' wolfsbane. Werewolves has no tag and its recipes name the item, so this is two 1:1 conversion recipes, not a tag. See "Data (Task 2)".
- [x] Altar power from Vampirism and Werewolves plants and blocks.
- [ ] Rite of Cleansing Blood: cures vampirism by leaving the faction.
- [ ] Rite of the Silver Moon: cures lycanthropy; only when Werewolves is installed.
- [ ] Blood Offering: uses Vampirism blood bottles as power.
- [ ] A custom rite factory that calls `IFactionPlayerHandler` (used by all rites above).
- [ ] Silver Poppet: protects against silver and werewolf bites, when Werewolves is installed.
- [ ] Sun Poppet: absorbs one sun-damage tick burst for vampires.
- [x] Blood values for Enchanted mobs (data map).


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
- Wolfsbane: no tag to extend, so two shapeless 1:1 recipes (gated `part_enabled` + `mod_loaded werewolves`), item ids `enchanted:wolfsbane_flower` and `werewolves:wolfsbane`.
- Cure rites (Tasks 3+): `VReference.VAMPIRE_FACTION`, `IFactionPlayerHandler.leaveFaction(boolean)` / `setFactionAndLevel`, `PlayerFactionEvent.FactionLevelChanged` (current faction `null` on leaving). Werewolves by id `werewolves:werewolf` as in vampirism_tensura.
- Sun Poppet predicate: `vampirism:sun_damage`. Enchanted's poppet rule (lethal hit only) applies, see enchanted_tensura "Spirit Poppet".


## Data (Task 2)

All files carry `{ "type": "supernaturalbloodlines:part_enabled", "part": "vampirismEnchanted" }` (implies Vampirism and Enchanted); the Werewolves ones also `{ "type": "neoforge:mod_loaded", "modid": "werewolves" }`. GameTests are in `parts/vampirismenchanted/gametest/VampirismEnchantedDataGameTests`, registered from `VampirismEnchantedPart.init`.

**Garlic** (`data/c/tags/item/crops/garlic.json`): `enchanted:garlic`, `"required": false`. Vampirism has only this one tag (item and block), so there is no second `vampirism:` file. Tag files cannot take conditions, so with `parts.vampirismEnchanted=false` (Enchanted and Vampirism still installed) the tag still contains `enchanted:garlic`: acceptable, it only names an id. The block tag `c:crops/garlic` is not extended: nothing reads it at runtime.
- What this gives: the recipes `garlic_bread`, `garlic_finder`, `purified_garlic`, `garlic_diffuser_core(_improved)` (cauldron fluid) accept `enchanted:garlic`, and the cauldron tints it (`liquid_color` data map `#c:crops/garlic`). Test `enchantedGarlicCountsAsGarlic` asserts tag membership and crafts garlic bread from Enchanted garlic.
- **What it does not give (checked in the bytecode, see Hook points):** garlic diffusers and the garlic effect do not read the tag or any item; a vampire standing in a crop is hurt only by `vampirism:garlic`'s own `GarlicBlock.entityInside`. An `enchanted:garlic` crop therefore does **not** hurt a vampire. Making it do so needs a mixin on `GarlicBlock`-like logic or a per-tick poll; not done (out of scope for a data task). Left as an open decision for the controller.

**Wolfsbane**: Werewolves has no wolfsbane tag, so `data/supernaturalbloodlines/recipe/wolfsbane_from_enchanted.json` (`enchanted:wolfsbane_flower` -> `werewolves:wolfsbane`) and `wolfsbane_from_werewolves.json` (back), shapeless 1:1, gated `part_enabled` + `mod_loaded werewolves`. Test `enchantedWolfsbaneConverts` crafts both ways with Werewolves and asserts the recipes are absent without it. The brief's item id `enchanted:wolfsbane` does not exist; the item is `enchanted:wolfsbane_flower`.

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
