# Enchanted integration

Three parts link Enchanted: **vampirism_enchanted** (Vampirism and Enchanted), **enchanted_tensura** (Enchanted and Tensura), and the **species** and **artifacts** parts, which use Enchanted's circle magic for their rites. The full list of rites with ingredients is on [Rites](Rites). This page explains what each integration adds.

Rites are performed on an Enchanted **gold chalk** circle with an **altar** nearby for power. Power is paid when the rite starts (and, for some rites, each tick). If a rite refuses its target, Enchanted gives the ingredients back and the caster sees a message on the action bar.

## Altar power sources

More blocks give power to Enchanted's altars. The numbers are power per block, up to a limit of blocks of that kind.

| Block | Power | Limit | Part | Needs |
|---|---|---|---|---|
| Tensura Magic Ore, Deepslate Magic Ore | 6 | 20 | enchanted_tensura | Tensura |
| Tensura Magic Ore Block, Low/Medium/High Quality Magic Crystal Block, Low/High/Pure Magisteel Block, Mithril Block, Orichalcum Block, Adamantite Block, Hihiirokane Block | 10 | 10 | enchanted_tensura | Tensura |
| Condensed Magicule (this mod) | 30 | 16 | enchanted_tensura | Tensura |
| Blood Stone (this mod) | 25 | 16 | vampirism_enchanted | Vampirism |
| Vampirism Garlic, Vampire Orchid | 8 | 20 | vampirism_enchanted | Vampirism |
| Werewolves Wolfsbane, Wolf Berry Bush | 8 | 20 | vampirism_enchanted | Werewolves |

Plain stone adds nothing. Grass and dirt already count in Enchanted.

## Plants and blood

- **Enchanted garlic counts as Vampirism garlic in recipes** (the `c:crops/garlic` tag): garlic bread, garlic finder, purified garlic and the diffuser cores accept it, and the alchemical cauldron tints it. **The garlic crop itself does not hurt vampires, and the diffusers do not read the tag.** An Enchanted garlic crop is harmless to a vampire.
- **Wolfsbane conversion (needs Werewolves):** 1 Enchanted Wolfsbane Flower makes 1 Werewolves Wolfsbane (shapeless). 2 Werewolves Wolfsbane make 2 Enchanted flowers. (One Werewolves wolfsbane is already used by Werewolves' purple-dye recipe, so the way back needs two.)
- **Blood values:** an Enchanted Familiar Cat has blood 3 (like a cat). A Mandrake has blood 0.

## Curses (enchanted_tensura)

Both are applied with a **filled taglock** of the target on a medium circle of **nether chalk**, 2000 power. Both are lifted by Enchanted's remove-curse rite, and both are cleared when the cursed player dies. They stay and can be stacked like any Enchanted curse.

| Curse | Rite | Ingredients | Effect |
|---|---|---|---|
| Curse of Magicule Drain | `curse_magicule_drain` | filled taglock, Low Quality Magic Crystal, soul sand | slowly lowers magicules (about 0.5% of max magicule per second, more at higher strength), never below 0 |
| Curse of Skill Seal | `curse_skill_seal` | filled taglock, Medium Quality Magic Crystal, soul sand | one learned skill (chosen from the curse) cannot be activated or toggled on. It can always be switched off. A skill learned later may get sealed if nothing is sealed yet. The player sees "A curse seals that skill." |

Notes:

- Tensura has no "soul" item, so soul sand stands in for it.
- Switching `enchantedTensura` off **drops the saved curses of this mod** from players, and they do not come back if the part is switched on later.

## Rites that make or change things

See [Rites](Rites) for ingredients and power. In short:

- **Rite of Purification** and **Rite of Purification (Holy Water)** (enchanted_tensura): deal 8 holy damage to every undead within 8 blocks: Tensura-undead races, and mobs in the undead tag. Living things are spared. The damage type is `holy_purification` and counts as holy for Tensura.
- **Rite of Magicule Offering** (enchanted_tensura): the caster pays 5000 magicules and a Condensed Magicule block is dropped above the chalk. With too few magicules nothing is taken and the caster is told "You do not have enough magicules to make this offering."
- **Rite of Blood Offering** (vampirism_enchanted): 4 **full** Vampirism blood bottles and a stone make a Blood Stone. Empty or partly filled bottles do not count.
- **Rite of Cleansing Blood** (vampirism_enchanted): cures vampirism. Follows the bloodline rules for Originals and hybrids (see [Hybrids](Hybrids-And-Original-Hybrids)).
- **Rite of the Silver Moon** (vampirism_enchanted, needs Werewolves): cures lycanthropy, on a full-moon night.
- Everything for the species, hunters and artifacts: see [Rites](Rites).

The faction rites act on the **player standing on the centre block of the circle** (the closest player in the column on the chalk), or on the caster if nobody stands there. A rite that would change nothing, or whose new faction would refuse the player, is cancelled and the ingredients come back ("The rite would change nothing", "The rite cannot take hold: the new faction would not accept them.").

"Full moon" for rites means the night of the overworld full moon (phase 0 and the time of day between 12786 and 23216; noon of the full-moon day is refused). In the Nether and the End (no day/night) it is never a full moon.

## Poppets

Poppets are made at Enchanted's **spinning wheel** and must be **bound** to a player first (the usual Enchanted way). As in Enchanted, a poppet only protects against a **lethal** hit: a hit that is at least as big as your current health. Hits that would not kill you land normally and the poppet is not used. Each save costs one durability, and all three have 1.

| Poppet | Protects from (lethal hit) | Recipe (spinning wheel, 500 power) | Needs |
|---|---|---|---|
| Sun Poppet | Vampirism sun damage | Enchanted Poppet, Anointing Paste, Glowstone Dust | Vampirism, Enchanted |
| Silver Poppet | a silver weapon or tool (`werewolves:type/silver`), a silver bolt or other silver ammunition shot at you, or a werewolf bite | Enchanted Poppet, Anointing Paste, Silver Ingot | Werewolves |
| Spirit Poppet | Tensura soul damage (`soul_scatter`, `soul_consumed`, `synthesise`) | Enchanted Poppet, Medium Quality Magic Crystal, soul sand (needs Tensura) | Tensura |

The Silver Poppet does not exist without Werewolves. All textures of poppets and the blocks are placeholders. A real Tensura soul attack against a Spirit Poppet has not been checked in game yet (see [Known limits](Known-Limits)).

## EP data

The Enchanted Mandrake and Familiar Cat have Tensura EP data (magicule 300-500 and 600-900), so Tensura's analysis can read them. The numbers are first guesses.

## Switching these parts off

Block and items of a part vanish from a world when the part is switched off, and its rites and recipes are not loaded. See [For server owners](For-Server-Owners#switching-parts-off).
