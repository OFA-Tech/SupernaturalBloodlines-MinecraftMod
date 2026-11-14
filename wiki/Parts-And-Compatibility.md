# Parts and compatibility

A **part** is an optional feature set. A part is **on** when **every mod it needs is installed**, **its switch in the config is on**, and **every part it builds on is on**. The log prints one line per part at startup ("Part SPECIES: enabled" and so on). A disabled part's classes are never loaded, and its data (rites, recipes, loot, EP, altar blocks) is not loaded either.

Switch a part off in [`supernaturalbloodlines-common.toml`](Configuration#core-supernaturalbloodlines-commontoml), under `[parts]`, and restart. **The server and the clients need the same switches and the same linked mods.**

## Matrix: mods and parts

● needed · ○ optional extra that adds more · blank = not used

| | Vampirism | Enchanted | Tensura (+ManasCore) | Werewolves | Curios |
|---|:-:|:-:|:-:|:-:|:-:|
| **vampirism_tensura** | ● | | ● | ○ | |
| **enchanted_tensura** | ○ | ● | ● | | |
| **vampirism_enchanted** | ● | ● | | ○ | |
| **species** | ● | ● | | ○ | |
| **species_tensura** | ● | ● | ● | ○ | |
| **artifacts** | ● | ● | | ○ | ○ |

Parts that build on another part:

| Part | Needs the part |
|---|---|
| species | vampirism_enchanted |
| species_tensura | species |
| artifacts | vampirism_enchanted |

So switching **vampirism_enchanted** off also turns off **species**, **species_tensura** and **artifacts**. The log says `disabled (requires [VAMPIRISM_ENCHANTED])`.

## vampirism_tensura

- **Config key:** `vampirismTensura`. **Mods:** Vampirism, Tensura. Werewolves optional.
- **Does:** the Tensura race and the Vampirism faction stay in sync; sun burn; hunter and werewolf rules by Tensura race tags; holy damage x1.5; bloodless races can't be bitten; EP for Vampirism and Werewolves mobs; werewolf skill grants. See [Tensura integration](Tensura-Integration#vampirism-and-tensura-part-vampirism_tensura).
- **Config file:** `supernaturalbloodlines-vampirism_tensura.toml`.
- **Mixins (3):** `VampireRaceSunMixin`, `RaceUtilsSunMixin`, `VampirePlayerBiteMixin`.
- **Off:** nothing syncs: joining Vampirism no longer touches the Tensura race, and Tensura's own vampire burn is back for faction vampires.

## enchanted_tensura

- **Config key:** `enchantedTensura`. **Mods:** Enchanted, Tensura.
- **Does:** Tensura magic blocks power altars; two curses (Magicule Drain, Skill Seal); Rites of Purification and Magicule Offering; the Condensed Magicule block; the Spirit Poppet; EP for Enchanted mobs. See [Enchanted integration](Enchanted-Integration).
- **Mixins:** none. **Config file:** none.
- **Off:** the **block, item** and the two curses vanish from existing worlds and players. **Saved curses of this mod are dropped** from players, and don't come back if the part is turned on later. Back up the world first.

## vampirism_enchanted

- **Config key:** `vampirismEnchanted`. **Mods:** Vampirism, Enchanted. Werewolves optional.
- **Does:** altar power from Vampirism and Werewolves plants; Enchanted garlic works in Vampirism recipes; Rites of Cleansing Blood, the Silver Moon and Blood Offering; Blood Stone; Sun and Silver Poppets; wolfsbane conversion recipes; blood values for Enchanted mobs. It also holds the **faction rite** engine other parts use. See [Enchanted integration](Enchanted-Integration).
- **Mixins:** none. **Config file:** none.
- **Off:** turns off **species**, **species_tensura** and **artifacts** as well. Blood Stone and the poppets vanish from worlds.

## species

- **Config key:** `species`. **Mods:** Vampirism, Enchanted (and the vampirism_enchanted part). **Werewolves optional** (hybrids, tribrids, werewolf packs and all their rites need it).
- **Does:** all the species: [Originals](Vampires-And-Originals), [hybrids](Hybrids-And-Original-Hybrids), [witches and siphoners](Witches-And-Siphoners), [heretics and tribrids](Heretics-And-Tribrids), [Hunters of the Five](Hunters-Of-The-Five), [sirelines](Sirelines), and [werewolf packs](Werewolves-And-Packs); white oak; blood items; the Taser and Shocked; the commands.
- **Config file:** `supernaturalbloodlines-species.toml`.
- **Mixins (3, into Enchanted):** `AltarRangeMixin`, `RiteCostMixin`, `RiteCancelMixin`. They serve the witch bonus and paying with siphoned power. Applied only when Vampirism and Enchanted are installed and `parts.species` is on. `witch_bonus_enabled` turns the bonus off on purpose; if an Enchanted update makes a mixin miss, the bonus is simply missing and the log says so.
- **Access transformer:** `META-INF/accesstransformer.cfg` makes two `Entity` flag methods public, for the Hunter's client-only outline. It is not a mixin and is applied whether the part is on or not.
- **Off:** Originals lose their protection and bonuses (the permanent modifiers are removed on login: ids `original` and `hybrid`), the saved bloodline data stays on the player (core keeps it), and the species items and rites are gone. **Back up the world.**

## species_tensura

- **Config key:** `speciesTensura`. **Mods:** Vampirism, Enchanted, Tensura (and the species part). Werewolves optional.
- **Does:** the Tensura layer of each species (skills, magicule and aura bonus, undead treatment), the **seven apex races** and the evolution boost, **sires as Tensura owners**, sire naming, werewolf rank skills. See [Tensura integration](Tensura-Integration#the-species-layer-part-species_tensura).
- **Config file:** `supernaturalbloodlines-species_tensura.toml`.
- **Mixins (3, into Tensura and ManasCore):** `RaceUtilsMixin` (undead checks), `RaceInstanceMixin` (apex offer), `SireNamingMixin` (naming). Applied only when all the mods and both parts are on. The log says whether each hook is in place; if Tensura renames a method after an update, the hook is skipped and the rest keeps working.
- **Off:** nothing runs, **but the apex races stay registered** whenever Tensura is installed, because ManasCore throws for a saved race that is not registered. Players on an apex race stay on it, with no layer. Sire links already made stay too. See [For server owners](For-Server-Owners#granted-skills-and-links-stay-when-a-part-is-off).

## artifacts

- **Config key:** `artifacts`. **Mods:** Vampirism, Enchanted (and the vampirism_enchanted part). Werewolves optional (Moon Ring, wolfsbane strains). **Curios optional** (a `ring` slot).
- **Does:** the three rings, the Vampire Compass, the Hearing Device, the White Oak Ash Dagger and The Cure, vervain, mountain ash, wolfsbane strains and wolf lichen. See [Items and blocks](Items-And-Blocks).
- **Mixins:** none. **Config file:** none.
- **Off:** the items and blocks vanish from existing worlds. The Curios `ring` slot still shows on the player (Curios ignores load conditions for that file); it is harmless. A bitten non-Alpha werewolf can no longer shift at will (no Moon Ring). Mountain ash lines that were placed disappear.

## Always on (core)

- The **part switch** system, the `part_enabled` data condition, the player **bloodline state** (saved with the player and copied on death), the sync to the client, and the **debug commands**.
- **On login, core removes** the permanent attribute modifiers of every part that is **not** on (ids like `original`, `hybrid`, `hunter_mark`, `wolf_rank`, `layer_magicule`), so a part switched off does not leave stat changes on a player.
- **Apex races** are registered whenever Tensura is installed, even with `species_tensura` off.

## Mixins

The mod has **9 mixins** in 3 configs (a budget of 3 per part, each `require = 0`, each gated by its part's switch). The plugin for each config only applies it when all the part's mods are installed and its switch is on (read from the toml file at the start, since COMMON configs load after mixins).

| Part | Mixin | Where | Why |
|---|---|---|---|
| vampirism_tensura | `VampireRaceSunMixin` | Tensura `VampireRace.shouldBurn` | no second burn for a Vampirism vampire |
| vampirism_tensura | `RaceUtilsSunMixin` | Tensura `RaceUtils.isUnderSun` | no Weakness, Slowness or Mining Fatigue from Tensura in the sun for a faction vampire |
| vampirism_tensura | `VampirePlayerBiteMixin` | Vampirism `VampirePlayer.determineBiteType` | refuse a bite on a bloodless race |
| species | `AltarRangeMixin` | Enchanted `AltarBlockEntity` | witch altar range x1.5 |
| species | `RiteCostMixin` | Enchanted `GoldChalkBlockEntity` | witch cost x0.75, siphoned power |
| species | `RiteCancelMixin` | Enchanted `Rite.cancel` | a refused rite costs nothing |
| species_tensura | `RaceUtilsMixin` | Tensura `RaceUtils.isUndead`, `isAffectedByHolyExposure` | undead treatment |
| species_tensura | `RaceInstanceMixin` | ManasCore `ManasRaceInstance` | apex offer |
| species_tensura | `SireNamingMixin` | Tensura `RequestNamingKeyPacket` | sire naming |

If another mod you use also patches these methods, tell us (see [Troubleshooting](Troubleshooting)).
