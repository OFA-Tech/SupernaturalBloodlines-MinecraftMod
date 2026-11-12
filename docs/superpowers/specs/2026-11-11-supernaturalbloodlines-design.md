# SupernaturalBloodlines: design

Status: approved in conversation on 2026-11-11 (the shape and the one-mod decision). Details marked *my call* are proposals the builder may adjust, but should flag.

## Intent

The OFA Holy Packs modpack (NeoForge 1.21.1) runs Tensura: Reincarnated and Enchanted, and will bring back Vampirism + Werewolves. These mods ignore each other today:

- A player can be a Tensura vampire and a Vampirism vampire at the same time, as two unrelated systems.
- Enchanted's witchcraft knows nothing about vampires, werewolves or magicules.

The owner wants one mod that:

1. makes these mods work together;
2. adds playable supernatural species inspired by *The Vampire Diaries* / *The Originals*: hybrid, Original vampire and Original hybrid, witch / siphoner, heretic, tribrid;
3. gives each species a matching Tensura race.

It ships on CurseForge too, so it must also work when only some of the linked mods are installed.

## Goals

- One mod, `SupernaturalBloodlines`, built from optional **parts**. Each part switches on only when its mods are present.
- Data-first: tags, data maps and datapack registries before code. Then public APIs and events. Mixins only where nothing else works.
- Every feature can be checked in game and toggled in config.
- The modpack can use it on a dedicated server: the mod is required on both sides and synced.

## Non-goals

- No new werewolf transformation renderer. We reuse Werewolves' forms.
- No custom Vampirism skill-tree UI per species in the first release.
- No Forge / Fabric ports. No Minecraft versions other than 1.21.1.
- No other TVD species (dragons, sirens, travelers and so on) in this plan.

## Anti-goals (a failure even if it "works")

- Copying code, assets or JSON from Tensura (All Rights Reserved), Enchanted (All Rights Reserved), Vampirism or Werewolves.
- Using show character or family names (Klaus, Mikaelson, Petrova and so on) in content, items or text. Use generic names ("Original's blood", "doppelgänger blood").
- Crashing or erroring when any optional mod is absent.
- Two systems fighting over the same player state, for example double sun damage or infinite sync loops.
- Heavy per-tick work. The pack targets low-end PCs.

## Identity and conventions

| Item | Value |
|---|---|
| Repo | `OFA-Tech/SupernaturalBloodlines-MinecraftMod`, public. Naming follows `OFA-Tech/RegistryCleanupTool-MinecraftMod` and `ControlCore-HytaleMod`. |
| Local path | `D:\Development\OFA-TECH\SupernaturalBloodlines-MinecraftMod` |
| Mod id | `supernaturalbloodlines` |
| Package | `net.ofatech.supernaturalbloodlines` |
| License | MIT, for our code only; state that the linked mods' content is not covered |
| Template | NeoForge mod generator (https://neoforged.net/mod-generator/), ModDevGradle, as RegistryCleanupTool uses |
| Repo extras | ControlCore's setup: `.github/workflows/build.yml` and `release.yml`, `README.md`, `CHANGELOG.md`, `docs/`, `.editorconfig` |
| Minecraft / NeoForge | 1.21.1, NeoForge 21.1.x (the pack runs 21.1.265) |

## Linked mods: versions, artifacts, hook points (researched 2026-11-11)

| Mod | Version | Gradle source | Notes |
|---|---|---|---|
| Vampirism | 1.21-1.10.13 (`vampirism`, needs `teamlapenlib`) | `https://maven.maxanier.de/releases`, `de.teamlapen.vampirism:Vampirism:1.21-1.10.13`, `api` classifier for compileOnly | Source branch `version/1.21/latest` |
| Werewolves | 1.21-2.0.3.4 (`werewolves`) | `https://maven.paube.de/releases`, `de.teamlapen.werewolves:Werewolves:1.21-2.0.3.4` | Branch `1.21`; needs Vampirism `[1.10.0-beta.2,1.11.0)` |
| Enchanted | 4.2.7 (`enchanted`) | `https://maven.favouriteless.dev/releases`, `net.favouriteless.enchanted:enchanted-neoforge-1.21.1:4.2.7` | Branch `1.21.1`; also needs stateobserver 1.6.3, modopedia 1.1.10, geckolib 4.7 |
| Tensura: Reincarnated | 2.0.1.5 (`tensura`) | CurseMaven (`curse.maven:<slug>-<projectId>:<fileId>`); the builder looks up the IDs | Closed source; read via `javap`; not obfuscated |
| ManasCore | 4.0.0.2 (`manascore_race`, `manascore_skill`, `manascore_storage`, nested in the jar) | CurseMaven | GPLv3; compileOnly only, never bundle |
| Architectury | 13.0.8+ | its Maven | ManasCore events are Architectury `Event`s |

### Vampirism API (`de.teamlapen.vampirism.api`)
- `VampirismAPI.factionPlayerHandler(Player)` returns `IFactionPlayerHandler`, with:
  - `getCurrentFaction()`, `getCurrentLevel()`
  - `canJoin`, `joinFaction`, `setFactionAndLevel`, `leaveFaction(boolean)`
- `VampirismAPI.factionRegistry()` provides `getFactionByID`, `getFaction(Entity)` and `createPlayableFaction`.
- Events in `api.event.PlayerFactionEvent`:
  - `CanJoinFaction`, with behaviour ALLOW / DENY / ONLY_WHEN_NO_FACTION
  - `FactionLevelChangePre` (cancellable)
  - `FactionLevelChanged`
- Other events: `BloodDrinkEvent.PlayerDrinkBloodEvent` and `EntityDrinkBloodEvent`, `ActionEvent.ActionActivatedEvent`.
- Data:
  - data map `vampirism:entity_blood`, `{"blood": N}`, where 0 means not biteable
  - `entity_converter`, `item_blood`
  - `data/<ns>/vampirism/no_sun_damage.json`
  - `ISundamageRegistry` for sun rules in code
- Infection is `SanguinareEffect` (internal). Garlic and holy water are tags under `vampirism:`; there is a `holy_water` damage type.
- One faction per player.

### Werewolves
- Faction `werewolves:werewolf` (`WReference.WEREWOLF_FACTION`).
- Forms: `WerewolfForm` (static list) and `WerewolfPlayer.get(p).getForm()` / `switchForm`.
- `Helper.isFullMoon(Level)`. Infection: `LupusSanguinemEffect`, which ends in `joinFaction` and is gated by `CanJoinFaction`.
- Silver: item tag `werewolves:type/silver` and the `ISilverItem` marker.
- Damage-type tags: `werewolves:werewolf_fur_immune` and `bite`.
- Size changes go through `EntityEvent.Size`.

### Enchanted API (`net.favouriteless.enchanted.api`)
- **Rites:**
  - JSON at `data/<ns>/enchanted/circle_magic/rite/<name>.json`, with fields `items`, `shapes`, `power`, `tick_power`, `time`, `weather`, `dimensions`, `entities` and `factory`.
  - Built-in factories include `apply_curse`, `remove_curse`, `command`, `create_item` and `summon_entity`.
  - Custom factory: `RiteFactoryRegistry.get().register(id, MapCodec)`. It must run before the datapack loads; Enchanted itself does it in its mod constructor.
- **Curses:** `CurseManager.get().register(new Curse.Type<>(id, Supplier, MapCodec))`. Players only, stored per UUID.
- **Altar power:**
  - per block: `data/<blockNs>/enchanted/altar/block/<block>.json`, `{"limit":N,"power":N}`
  - per tag: `altar/tag/<tag>.json`
  - upgrades: `altar/upgrade/*.json`
  - code: `PowerProvider` / `PowerConsumer` / `PowerHelper`
- **Poppets:** `PlayerPoppetEffect` (`protectsAgainst(DamageSource)`, `protect(Player)`) passed into `new PlayerPoppetItem(PoppetColour, effect, props)`. We register the item ourselves.
- **Brewing API is a stub in 4.2.7.** Don't rely on it; brews are kettle recipes (`enchanted:kettle`).
- **Enchanted's own plants:** `enchanted:garlic` and `enchanted:wolfsbane` duplicate the plants in Vampirism and Werewolves.

### Tensura / ManasCore
- **Race:**
  - Read with `RaceAPI.getRaceFrom(LivingEntity)` (gives `Races`), then `getRace()` (an `Optional<ManasRaceInstance>`).
  - Change with `setRace(...)` and `evolveRace(...)`.
- **Race events:** `RaceEvents.SET_RACE`, `POST_INIT`, and others. These are Architectury events returning `EventResult`, with `Changeable` holders.
- **Skills:**
  - Grant or remove with `SkillAPI.getSkillsFrom(entity)`, `learnSkill` / `forgetSkill`, or `SkillHelper.learnSkill`.
  - Events: `SkillEvents.UNLOCK_SKILL` and `REMOVE_SKILL`.
- **Race registry** `manascore_race:races`: Architectury `Registrar`. Tensura races extend `TensuraRace extends ManasRace`.
- **Tensura's vampire line:**
  - Stages: `tensura:ghoul` → `vampire` → `vampire_overcomer` / `vampire_lord` → `divine_vampire`.
  - `VampireRace` has `shouldBurn` (driven by `RaceUtils.isUnderSun`) and its settings in `config.race.VampireConfig`.
  - Tensura has no werewolf race.
- **EP and magicules:**
  - `TensuraStorages.getExistenceFrom(entity)` returns `IExistence`, with get/set for EP, aura and magicule, plus `markDirty()`.
  - EP for a mob type: `data/tensura/entity_existence/<name>.json`.
- **Holy damage:** `TensuraDamageHelper.isHoly(source)` is true when the damage message id contains "holy" or "divine", among other checks. So `vampirism:holy_water` probably already counts as holy damage for Tensura (to verify).
- **Tags a datapack can extend:**
  - race tags: `tensura:undead`, `no_blood`, `spiritual`, `daemon`, `human_like`, `divine`
  - entity tags: `tensura:no_blood`, `spiritual`
  - damage-type tags: `tensura:is_spiritual`, etc.
- **How existing Tensura addons in the pack do it:** NeoForge events, `TensuraStorages`, `SkillAPI` / `RaceAPI`, and few mixins. `tensura_rebalance_core` uses the ManasCore events.

## Approaches considered

1. **Three separate compat mods, plus a 4th species mod.** Rejected. The species and the unified vampire touch every mod pair, so shared rules would be split across repos that must change together, and the Tensura one would depend on the Enchanted one.
2. **Species inside the compat mods.** Rejected for the same coupling reason.
3. **One mod with optional parts (chosen).** One version and one place for the shared player-state rules. Parts load only when their mods are present, so partial installs still work. Parallel sessions work in separate parts (packages) on separate branches.

Within the species part, species are **layers on existing factions**, not new Vampirism factions. Making each one a full Vampirism faction would mean rebuilding werewolf forms and blood handling for each, which is the most work and the most fragile. Revisit only if players want per-species skill-tree menus.

## Architecture

### Part loading
- The main `@Mod` class checks `ModList.get().isLoaded(...)` for each part's mods, plus a config toggle, and only then calls `<Part>.init(modBus)`.
- **Hard rule:** classes that reference another mod's classes live only inside that part's package and are never touched unless the part is enabled. This avoids `NoClassDefFoundError`.
- Parts:

| Part | Package | Required mods |
|---|---|---|
| `enchanted_tensura` | `...parts.enchantedtensura` | enchanted, tensura |
| `vampirism_enchanted` | `...parts.vampirismenchanted` | vampirism, enchanted (werewolves optional) |
| `vampirism_tensura` | `...parts.vampirismtensura` | vampirism, tensura (werewolves optional) |
| `species` | `...parts.species` | vampirism, enchanted (hybrid-type species only with werewolves) |
| `species_tensura` | `...parts.speciestensura` | species part active, and tensura |
| `artifacts` | `...parts.artifacts` | vampirism, enchanted (moon ring with werewolves; Curios optional) |

- **Data files are gated per part** with `neoforge:conditions` (`neoforge:mod_loaded`, combined with `and`) on every JSON that names another mod's ids.
- **`neoforge.mods.toml`:** all linked mods are `optional`, with version ranges pinned to the researched lines:
  - tensura `[2.0.1,2.0.2)`
  - manascore `[4.0.0,4.1)`
  - vampirism `[1.10.13,1.11)`
  - werewolves `[2.0.3,2.1)`
  - enchanted `[4.2.7,4.3)`
  - Widen each range only after testing a newer version.

### Shared player state
- A NeoForge data attachment `supernaturalbloodlines:species`. It is serialisable, synced to the client, copied on death or respawn only where the rules say so, and holds:
  - `species`: NONE, HYBRID, ORIGINAL, ORIGINAL_HYBRID, WITCH, SIPHONER, HERETIC, TRIBRID
  - `sire` (UUID or null)
  - `transition` state and timer
  - `siphoned_power`
- **One "authority" helper** (`BloodlineRules`) decides every transition. It is plain Java, easy to unit test, and has no mod classes in its signature. Parts translate mod events into calls to it.
- **Loop guard:** a re-entrancy flag per player while one system changes the other, e.g. a Tensura `setRace` triggered by a faction change must not trigger a faction change back.

### Config (NeoForge `ModConfigSpec`)
- `common`: part on/off switches, the level ↔ race mapping, skill grant lists, and species tuning numbers.
- `server`: values synced to the client.
- No secrets, no network calls.

## Parts, features and rules

### 1. `enchanted_tensura` (Enchanted + Tensura)
- **Altar power from Tensura magic blocks** (data). The builder lists Tensura's magic ore / crystal / magisteel block ids from the jar.
- **Curses** (code):
  - *Magicule Drain*: slowly lowers magicules while active.
  - *Skill Seal*: one random learned skill can't be activated while active. Use a `SkillEvents.ACTIVATE_SKILL` interrupt.
- **Rites** (data, with a custom factory where needed):
  - *Rite of Purification*: holy damage in an area to Tensura-undead races and `minecraft:undead`. The damage type id contains "holy".
  - *Magicule Offering*: converts the caster's magicules into the power the rite needs. Custom factory, or a PowerProvider shim.
- **Spirit Poppet** (code): protects against damage in `tensura:is_spiritual`.
- **EP for Enchanted mobs** (data): `entity_existence` entries.

### 2. `vampirism_enchanted` (Vampirism + Enchanted, Werewolves optional)
- **Plant parity** (data):
  - Enchanted garlic is accepted where Vampirism garlic is: tags, `c:crops/garlic`.
  - Enchanted wolfsbane counts as Werewolves' wolfsbane: tags and recipe ingredient.
  - Altar power from Vampirism and Werewolves plants and blocks.
- **Rites:**
  - *Rite of Cleansing Blood*: cures vampirism by leaving the faction.
  - *Rite of the Silver Moon*: cures lycanthropy; only when Werewolves is installed.
  - *Blood Offering*: uses Vampirism blood bottles as power.
  - All use a custom factory that calls `IFactionPlayerHandler`.
- **Poppets:**
  - *Silver Poppet*: protects against silver and werewolf bites, when Werewolves is installed.
  - *Sun Poppet*: absorbs one sun-damage tick burst for vampires.
- **Blood values for Enchanted mobs** (data map).

### 3. `vampirism_tensura` (Vampirism + Tensura, Werewolves optional)
- **Unified vampire.** Vampirism owns sun, blood, levels and actions; Tensura owns skills, evolution and EP.
  - **Vampirism → Tensura:** joining the vampire faction (sanguinare finished) sets the Tensura race to the configured `join_race`, default `tensura:vampire`.
  - **Tensura → Vampirism:** a Tensura race in the vampire line joins the vampire faction at the mapped level. Default mapping (*my call*):
    - `ghoul` is the pre-vampire stage, with no faction.
    - `vampire` is level 1, or keeps the current level if higher.
    - `vampire_overcomer` and `vampire_lord` are at least level 7.
    - `divine_vampire` is the maximum level.
  - **Leaving the faction (cure)** sets the race to `cure_race`, default `tensura:human`.
  - **Sun burn:** Tensura's own vampire burn is disabled for faction vampires. Prefer a Tensura config option if one exists; otherwise one mixin on `VampireRace.shouldBurn`.
- **Hunters:** `CanJoinFaction` returns DENY for hunters whose race has the tags `tensura:undead` or `daemon`. A hunter whose race becomes undead or daemon leaves the hunter faction, with a chat message (*my call*).
- **Werewolves** (when installed):
  - Joining is denied for the race tags `tensura:undead`, `daemon` and `no_blood`.
  - Werewolf levels grant configured Tensura skills; default: `tensura:godwolf_sense` at max level (*my call*).
- **Holy damage both ways:**
  - Verify that `vampirism:holy_water` counts as Tensura holy damage.
  - Make Tensura holy damage hurt Vampirism vampires as holy water does.
- **Bloodless races can't be bitten:** an event cancel for players whose race has the tag `tensura:no_blood`, and blood 0 for Tensura bloodless mobs (data map).
- **EP for Vampirism and Werewolves mobs** (data `entity_existence`).

### 4. `species` (Vampirism + Enchanted; hybrid, Original hybrid and tribrid need Werewolves too)

Layers on existing factions. Show-inspired rules, generic names.

| Species | Base faction | Layer adds | How you become one (Enchanted rites and items, *my call*) |
|---|---|---|---|
| Original vampire | vampire | Only white-oak weapons kill; stronger; its blood ("Original's blood" item, made by an Original at a rite) sires hybrids | *Rite of Immortality* on a player with no faction: white oak, sun, a heart-of-the-tree item, a sacrifice |
| Original hybrid | werewolf + Original (see decisions below) | Werewolf forms usable; strongest | An Original completes the *Rite of the Moon* (moonstone, full moon, sacrifices) |
| Hybrid | werewolf | Vampire traits: blood drinking, speed, no sun damage, shift at will, no full-moon pain, bite poisons vampires; *sired* to the Original whose blood made it | A werewolf dies with Original's blood in their system → transition state → must drink doppelgänger blood before the timer ends. Drinking ordinary blood → unstable → dies (show rule) |
| Witch | none or werewolf | Bigger altar range and cheaper rites; can cast without an altar at low power | *Rite of Ancestral Calling* (*my call*) |
| Siphoner | none or werewolf | No own power; siphons power from magic blocks, players, mobs and (with Tensura) magicules to fuel rites | Born of the same rite with a "siphoner" outcome, or a configured chance |
| Heretic | vampire | Siphoner who turned: keeps siphoning and can siphon their own vampire nature | A siphoner who completes the vampire transition |
| Tribrid | werewolf (+ hybrid layer + witch layer) | Witch + hybrid; top tier | A witch who completes the hybrid transition becomes a tribrid instead of losing magic |

- **Rule:** a plain witch who becomes a vampire loses her magic (show rule); only siphoners become heretics.

#### Decisions after the builder check (owner = decided by the owner; others are *my call*)

- **Who can take a species (owner):** only players whose Tensura race has the tag `tensura:human_like`, which includes the vampire line, or players with no Tensura race when Tensura is absent. Other races are refused with a message. Species are reached only through our rites, never through Tensura's evolution screens, so no evolution links are needed.
- **Originals (owner):** any player with no faction can do the *Rite of Immortality* in survival. It is costly:
  - high altar power
  - rare ingredients
  - full-moon night
  - a sacrifice: the rite's `entities` field requires specific living mobs in the circle
  - The config value `max_originals_per_world` defaults to 0, meaning unlimited.
- **Hybrid-type species use the werewolf faction as their base.** This covers the hybrid, Original hybrid and tribrid.
  - Werewolves' forms only work for werewolf-faction players, so the vampire side is our layer, not Vampirism's vampire system.
  - The Original hybrid is therefore werewolf faction + Original layer + hybrid traits, not vampire-based.
- **Vampire traits on a werewolf base are a small, separate set:**
  - speed and strength attributes, scaled by age (time since turning)
  - *blood thirst*: drinking Vampirism blood bottles or biting mobs gives buffs; going without gives a mild debuff
  - a hybrid bite that poisons Vampirism vampires (our effect)
  - No full Vampirism blood or sun system. Werewolves don't burn anyway.
- **Mixins:** 0–2 is a guideline, not a hard cap. Mixins into Enchanted (witch altar range and rite cost) and into Tensura (vampire sun burn) are acceptable with `require = 0` and a config switch. Each one is listed in `docs/mixins.md` with its reason.
- **Rite targeting:** a rite's custom factory acts on the player standing on the circle's centre block, falling back to the caster. "Sacrifices" are mobs listed in the rite's `entities` field, consumed by the rite.
- **Transition matrix** (`BloodlineRules`; each line is a unit test):

| Current state | Event | Result |
|---|---|---|
| no faction, human_like | Rite of Immortality | Original vampire (joins vampire faction + Original layer) |
| werewolf | dies with the *Original's Blood* effect active | respawns in *transition* (attachment timer, default 20 min of play) |
| transition | drinks doppelgänger blood | hybrid; `sire` = the Original whose blood it was |
| transition | drinks any other blood | *unstable* effect → dies when it ends → back to plain werewolf, transition cleared |
| transition | timer runs out | dies → plain werewolf |
| witch + werewolf | completes the hybrid transition | tribrid |
| siphoner + werewolf | completes the hybrid transition | refused at the start of transition (message); stays a siphoner werewolf |
| witch | joins the vampire faction (sanguinare) | becomes a plain vampire, loses the witch layer |
| siphoner | joins the vampire faction | heretic |
| Original vampire | completes the Rite of the Moon | Original hybrid |
| vampire (plain) with the hidden gene | kills a villager or player (the Klaus rule) | hybrid with the werewolf side **bound** (no wolf forms); joins the werewolf faction, keeps the vampire layer; sire unchanged |
| Original with the hidden gene | kills a villager or player | Original hybrid, bound |
| bound hybrid / Original hybrid | completes the Rite of the Moon | unbound (wolf forms allowed) |
| hybrid / Original hybrid / tribrid | Rite of Cleansing Blood | loses the vampire traits → plain werewolf (+ witch layer for a tribrid → witch werewolf) |
| Original (any) | cure rites | refused: Originals can't be cured |
| any vampire/werewolf/species layer | drinks The Cure | everything removed, leaves faction (the only cure for Originals) |
| human_like Vampirism hunter | Rite of the Five, roster < max_five | HUNTER_OF_THE_FIVE, markKills 0 |
| HUNTER_OF_THE_FIVE | killed by a supernatural attacker | plain hunter (NONE); a random eligible hunter becomes POTENTIAL |
| POTENTIAL | first vampire/hybrid/Original kill | HUNTER_OF_THE_FIVE; every cursed killer has `hunterCurse` lifted |
| HUNTER_OF_THE_FIVE | kills a vampire/hybrid/Original | markKills + 1 |
| vampire, hybrid or Original | kills a HUNTER_OF_THE_FIVE | `hunterCurse` = true |
| hybrid | a second Original's Blood | no effect |
| any species | leaves its base faction any other way (command, Vampirism UI) | the species layer is removed to match |

- **Death and respawn:** species, sire and Original status persist. The transition state persists through the death that starts it. `siphoned_power` resets on death.
- **Originals and white oak:** damage that isn't from the `supernaturalbloodlines:white_oak` item tag can't take an Original below half a heart. A white-oak kill is a normal death and respawn. New items: white oak stake and white-oak-tipped weapons; white oak wood comes from a rare tree, or a rite-transformed oak.
- **Sire bond:** a hybrid can't damage its sire (the damage is cancelled). The sire can call the hybrid to their position through a rite. No death chain: an Original's death doesn't kill its hybrids.
- **Unified vampire priority:** Tensura evolution drives the race. A Vampirism level is only raised to the race's minimum level (a floor), never lowered by it. Vampirism level-ups never evolve the Tensura race. Leaving the vampire faction sets `cure_race`.
- **Doppelgänger blood** comes from a rare "doppelgänger" villager variant or a rite. *My call:* a rite that needs a taglock of a player plus rare ingredients.
- **Build order:**
  1. Originals and Original's blood (minimal). The hybrid depends on it.
  2. Hybrid.
  3. Original hybrid.
  4. Witch / siphoner.
  5. Heretic.
  6. Tribrid.

### 5. `species_tensura` (species part + Tensura)
- Register our own races in `manascore_race:races`:
  - `supernaturalbloodlines:original_vampire`
  - `original_hybrid`
  - `hybrid`
  - `witch`
  - `siphoner`
  - `heretic`
  - `tribrid`
- Classes extend `TensuraRace` (runtime use, no copying).
- Intrinsic skills come from Tensura's existing ones, e.g. `blood_mist`, `drain`, `beast_transformation`, `self_regeneration`, `godwolf_sense` and `charm`. The builder picks the final lists.
- **Sync both ways**, like the unified vampire. The species layer wins over the plain vampire mapping.
- Race tags (`tensura:undead` and so on) set per race, so the other rules apply automatically.

### 6. `artifacts` (Vampirism + Enchanted; Werewolves and Curios optional)

The owner approved these items. Witches make the enchanted ones with rites.

**Rings and Curios**
- Rings are Curios `ring` items, tagged `curios:ring`. Register them through `CuriosApi.registerCurio` only when Curios is loaded.
- In the OFA pack, the Accessories compat layer shows Curios items in Accessories slots.
- Without Curios, a ring works from anywhere in the player's inventory.
- Check whether a ring is equipped through one helper, `ArtifactSlots.isWorn(player, item)`.

| Item (generic name) | Effect | How it's made |
|---|---|---|
| **Sun Ring** | No Vampirism sun damage while worn. Bound to one vampire; for anyone else it does nothing. | Rite *Ward of Daylight*: lapis block + gold ring + the vampire's taglock + power 5000. Binds the vampire's UUID into a data component. |
| **Moon Ring** | (Werewolves) The wearer isn't forced to transform at full moon and keeps control. | Rite *Ward of the Moon*: kyanite stand-in (amethyst) + silver + taglock + power 5000. |
| **Resurrection Ring** | A human (no vampire or werewolf faction, no species except a hunter or potential) killed by a supernatural attacker (Vampirism vampire or werewolf faction, a species, or a Tensura race tagged daemon or undead) cancels death: back to full health, Regeneration, 1 charge used. 3 charges. | Rite *Ward of Return*, power 8000. |
| **Vervain** | A plant (crop) and a tea. Held, carried in the offhand, or drunk as tea (10 min effect): Vampirism vampires and hybrids take burn damage when they bite you or hit you barehanded, and you're immune to compulsion. Vampires who eat it take damage. Grows near white oak or from seeds in loot. | Crop + kettle recipe. |
| **White Oak Ash Dagger** | Hitting an Original applies *Desiccated*: they can't move or act and look grey until the dagger is pulled. Another player right-clicks to pull it; the effect has a long timeout (default 30 min). If a vampire, werewolf or species player hits with it, the wielder also takes heavy damage (show rule: only humans can use it). | Silver dagger + white oak ash (from burnt white oak) via the kettle or a rite. |
| **The Cure** | One use. Drinking it removes every vampire, werewolf and species layer, including Originals, and leaves the faction. Sets `BloodlineEvent.DRANK_THE_CURE`. Very rare: a rite needing a desiccated Original nearby and power 20000. | Rite *Rite of the Cure*. |
| **Vampire Compass** | Points to the nearest vampire, hybrid or Original within 64 blocks. | Crafted compass + vervain + witch rite (power 2000). |
| **Hearing Device** | Use it to stun every vampire, werewolf and species mob or player within 5 blocks for 5 s (Slowness V + Nausea). It must be rewound (60 s cooldown). | Crafted + rite (power 3000). |

### 7. Hunters, modeled on the show's Five (in the `species` part)

- **Layer on Vampirism's hunter faction.** New species values:
  - `POTENTIAL`: a hunter who has been chosen
  - `HUNTER_OF_THE_FIVE`: a marked hunter
  - Only human-like players (the same eligibility as the other species) who are in the Vampirism hunter faction.
- ***Rite of the Five*** (power 10000):
  - Binds the target as one of the Five while fewer than `max_five` (config, default 5) exist in the world.
  - The world keeps a `FiveRoster` (`SavedData`) of member UUIDs.
- **The mark:** `markKills` counts vampire, hybrid and Original kills.
  - Perks scale with `min(markKills, 20)`: +1% attack damage and +0.5% speed per kill, capped at +20% / +10%.
  - Immune to compulsion.
  - Vampires within 24 blocks glow for the hunter only (client-side glow). Unlocked at 5 kills.
- **Losing the mark:** a marked hunter killed by a supernatural attacker loses the mark (back to a plain hunter).
  - A potential is then chosen at random from online, human-like Vampirism hunters without a mark, and becomes `POTENTIAL`.
  - The potential becomes one of the Five on their first vampire kill. Natural deaths keep the mark (*my call*: Minecraft deaths are frequent).
- **The hunter's curse:** the vampire, hybrid or Original who killed a marked hunter gets `hunterCurse = true`.
  - Periodic hallucinations: phantom hunter mobs only that player sees, Nausea, Darkness pulses, ghostly whispers.
  - It lifts when any potential becomes one of the Five.
  - Humans are exempt.
- **The hunter instinct:** a marked hunter who hasn't killed a vampire in 3 in-game days gets Weakness I until their next kill.
  - Allied vampires (same team or party) don't trigger it.
  - The full show behaviour of forcing attacks on friends is skipped as too disruptive.

### 8. Sirelines and bloodline perks (in the `species` part; Tensura link in `species_tensura`)

- **Who counts as sired:** anyone a player turns. Recorded as `sire` in the bloodline state (players) or in an entity attachment (mobs), plus a world-level `SirelineIndex` (`SavedData`, sire → set of sired UUIDs, with entity type and last known name):
  - a player infected by a player-vampire's bite through Vampirism sanguinare (discovery: how Vampirism records the infection source)
  - creatures converted by a player (Vampirism converted creatures)
  - hybrids made with an Original's blood
- **Commands:**
  - `/bloodline tree [player]` shows the sireline.
  - `/bloodline call` (needs Original or hybrid status, 10 min cooldown) teleports online sired players who accept a prompt, and pulls sired mobs that are loaded.
- **Compulsion** (Originals, Original hybrids, hybrids, plus plain Vampirism vampires at level ≥ 10), on villagers and Vampirism lesser vampire mobs. Not possible on hunters of the Five, vervain users, or players. Right-click while sneaking with an empty hand on a target in eye contact (within 4 blocks, looking at each other):
  - Villagers get a 20% trade discount for 1 in-game day, or follow for 5 min.
  - Lesser vampire mobs become sired and follow the player like Tensura subordinates when Tensura is installed, or just stay non-hostile without it.
- **Sire bond** (planned already): the sired can't harm the sire.
- **Sireline death:** config `sireline_death` (default **false**). When true, a permanently "killed" Original (white oak kill) kills every sired vampire in the index: players die once, mobs are removed.
- **Tensura link** (`species_tensura`, owner approved): when a sire is recorded and Tensura is present, set `IExistence.setPermanentOwner(sireUUID)` on the sired entity (players included; `playerNaming` isn't needed for this link) and `markDirty()`. Tensura then gives ally checks, kill credit to the sire, and Harvest Festival subordinate evolution through its own systems. The link is removed when the sire bond breaks (cure, or leaving the species).

### 9. Werewolf bloodlines and packs, modeled on Teen Wolf (in `species`, needs Werewolves)

Applies to every Werewolves-faction player, hybrids and tribrids included. It is stored in `BloodlineState.wolf` (a `WolfState`), and the pure `PackRules` (core) decides every change.

- **Lineage:**
  - `BORN`: the curse is inherited. Born wolves have better full-moon control (the forced-shift chance is halved) and can unlock Werewolves' four-legged beast form 5 levels earlier.
  - `BITTEN`: turned by an Alpha. Bitten wolves get a bigger pack bond bonus.
- **How you become born** (owner: both ways):
  - **Hidden gene:** each player is rolled once on first login (`latent_gene_chance`, default 0.15), and the result stays hidden. The gene awakens on their first villager or player kill: they become a born werewolf and join the Werewolves faction.
  - ***Rite of Lineage*** (a witch rite, power 9000): reveals whether the target has the gene. With an extra ingredient set, it awakens a bloodline on purpose: the target becomes a born werewolf even without the gene.
- **Ranks:**

| Rank | Eyes | Notes |
|---|---|---|
| `OMEGA` | gold | No pack. −10% attack and speed. |
| `BETA` | gold | In a pack. |
| `ALPHA` | red | Leads a pack. +20% attack and +4 max health. Has the Alpha Roar. Only Alphas can turn humans. |
| `TRUE_ALPHA` | red | An Alpha by merit. Alpha perks + 10%. At most `max_true_alphas` per world (default 1). |

  - Eyes turn **blue** if the wolf ever killed an innocent: a villager, or a player who wasn't attacking them. Blue eyes block True Alpha.
- **Alpha spark:**
  - **Stolen:** a werewolf player who kills an Alpha takes the spark. The old Alpha becomes an Omega; their pack's Betas become the new Alpha's pack, or Omegas if the new Alpha already has a pack.
  - **Inherited:** when an Alpha leaves the faction, is cured, or gives up the spark, it passes to the pack's highest-level Beta.
  - **Ordinary deaths keep the spark** (*my call*: Minecraft deaths are frequent).
  - **Earned (True Alpha):** a Beta or Omega with gold eyes, Werewolves level ≥ 10, at least 7 in-game days as a Beta, and at least 50 "defender kills" (hostile mobs killed while they were targeting a pack member or a villager) awakens as a True Alpha. Only while fewer than `max_true_alphas` exist.
  - **Giving up the spark (healing):** an Alpha can sacrifice the spark to fully heal a player and cure their wolfsbane or hybrid venom. The Alpha drops to Beta in their own pack, and the spark passes on as above.
- **Turning people** (owner: Alphas only):
  - Player werewolves infect others with lupus sanguinem only if they are ALPHA or TRUE_ALPHA.
  - An Alpha's bite has `alpha_bite_death_chance` (default 0.10) of killing the target instead of turning them, rolled when the infection completes.
  - Werewolves' wild werewolf mobs can still infect, counting as wild Alphas: bitten lineage, Omega rank.
  - A human bitten by an Alpha player joins that Alpha's pack as a Beta and is recorded as the Alpha's sired (sireline, section 8).
- **Packs:**
  - `PackIndex` is SavedData: Alpha → Betas. Betas also join by accepting an Alpha's invite (`/pack invite`, `/pack accept`, `/pack leave`).
  - **Pack bonus:** while at least 3 Betas are online within 64 blocks of their Alpha, the Alpha gets +5% attack per Beta (capped at +25%) and each Beta gets Regeneration I. Bitten Betas count double toward the Alpha's bonus.
  - **Alpha Roar** (keybind, 60 s cooldown):
    - Pack Betas within 32 blocks get Strength I + Speed I for 30 s, and their Werewolves form is allowed even when blocked.
    - Non-pack werewolves within 16 blocks are forced out of their form and get Slowness II for 5 s ("submit").
  - **Emissary:** an Alpha can name one witch, siphoner or heretic as the pack's Emissary (`/pack emissary <player>`). The Emissary's rites cost 10% less near pack members, and pack members get the Emissary's mountain ash lines strengthened (see `artifacts`).
- **Anchors:** a werewolf can bind an anchor at full moon to reduce the forced full-moon shift (−50% chance). An anchor is either a held or worn item tagged `supernaturalbloodlines:anchor` (Curios `charm` slot supported), or a named player within 16 blocks set with `/pack anchor <player>`. It uses the same hook as the Moon Ring.
- **Weaknesses** (owner approved all three):
  - **Mountain ash** (in `artifacts`): a dust line block crafted from ash-tree bark (a new mountain ash sapling and tree). Supernatural entities (`Supernatural.is`) can't cross it: it's a collision shape for them only, and projectiles they shoot pass through. An Emissary's line also blocks their spells or rites from crossing (*stretch*, record if skipped).
  - **Lunar eclipse:** each full-moon night has `lunar_eclipse_chance` (default 0.03) of being an eclipse.
    - A server-wide announcement goes out, and the client gets a red sky tint.
    - Werewolves (and hybrids' werewolf side) lose rank perks and can't use Werewolves form actions. Cancel `ActionEvent.ActionActivatedEvent` for werewolf form actions.
    - It lasts until dawn.
  - **Electricity:** a lightning strike within 3 blocks, or a hit from a hunter's **Taser** item (crafted, rechargeable with redstone), gives the *Shocked* effect for 30 s: no form actions, no natural regeneration, and Slowness I.
- **Tensura** (owner: as skills, the race is kept):
  - Rank changes grant or remove Tensura skills from the config map. Defaults: ALPHA → `beast_transformation`, `godwolf_sense`; TRUE_ALPHA → the Alpha skills + the commander skill found in discovery.
  - Born lineage → `godwolf_sense` at Werewolves level ≥ 5.
  - Skills are only removed if this mod granted them (tracked in the attachment).

### 10. Round-3 patches (override earlier sections)

`docs/superpowers/specs/SPECIES-RULES.md` (rules R1–R103) is the source of truth for species behaviour. Where it differs from the sections above, follow it. Summary of the changes:

- **Tensura races are never replaced** (R3, R20, R100). Species are a layer: Tensura skills, attributes, EP bonus, "treated as undead" where it matters, an evolution-requirement bonus, and one **apex evolution** per species that human-like races unlock in Tensura's evolution screen. The unified vampire keeps the race; only Tensura's own vampire line still syncs into the Vampirism faction.
- **R32:** an Original takes Vampirism's dying state on non-oak lethal damage; white oak kills instantly.
- **R35:** an Original hybrid needs the gene (natural, or given by the *Rite of Lineage*).
- **R42v:** werewolf-type bites poison vampires. It's lethal to non-Originals after 12,000 ticks, and only Hybrid's Blood cures it.
- **R81–R84:** hunters' prey is vampires and werewolves; witches aren't prey.
- **R91:** no True Alpha cap, much harder requirements.
- **R95:** wolfsbane strains; mountain ash blocks vampire and werewolf types, not witches.
- **R96:** werewolves are moon-bound unless they have control: a Moon Ring, hybrid-types, Alphas, or born werewolves with an anchor (half forms). Full forms come from level 8.
- **R101:** a sire can name their sired through Tensura's naming.

## Constraints

- **Java 21, NeoForge 21.1.x, Minecraft 1.21.1.** The dev machine has Java 25; Gradle needs toolchain 21.
- **No ARR jars or code in the repo.** Dependencies come only from Maven or CurseMaven.
- **Server and client both need the mod.** The mod's own entries go into registries, so a mismatch must give a clear error.
- **Performance:** no per-tick scans of all players or entities beyond Vampirism's and Werewolves' own ticks. Siphoning and transitions use timers.
- **Secrets:** none in the repo. CI uses GitHub secrets for CurseForge upload (`CURSEFORGE_TOKEN`).

## Testing and verification

- **Unit tests** (JUnit) for `BloodlineRules`: every transition, the mapping table, and loop-guard cases.
- **NeoForge GameTests** where a world is needed: faction ↔ race sync, rites firing, curses ticking.
- **Run configurations:** `runClient` and `runServer` with all linked mods as `localRuntime` / `runtimeOnly` from Maven / CurseMaven, plus a "missing mods" run per part to prove nothing crashes.
- **Each feature gets an in-game check** in a test world with commands (`/supernaturalbloodlines debug …`) to set state quickly.
- **Final check:** inside the OFA Holy Packs instance and its dedicated server copy.

## Left to the builder

- Exact CurseMaven project and file ids for Tensura and ManasCore.
- Tensura block ids for altar power, Enchanted mob ids, and the Tensura vampire config option name.
- Item, block and texture art: placeholder first; final art can use the fal skills.
- Final numbers (damage, timers, power costs) through config with sensible defaults.
- Whether `vampirism:holy_water` already counts as Tensura holy damage (verify, then decide).

## Risks

- **Tensura's four-part version** may break on update. Pin the version range, and isolate Tensura calls in the two Tensura parts.
- **Werewolf size changes and Tensura bat or transformation sizing** can clash. Test early in the species part.
- **Several mixins on the same Player / LivingEntity methods** across Vampirism, Tensura and Enchanted. Keep ours to 0–2, with `require = 0` where possible.
- **Licensing:**
  - MIT covers our code. ManasCore is GPLv3 and compileOnly, never bundled.
  - Enchanted's README allows "additional content via the Enchanted API".
  - Tensura is ARR: runtime references only.
- **Show IP:** generic names only; describe the inspiration, without claiming affiliation.
