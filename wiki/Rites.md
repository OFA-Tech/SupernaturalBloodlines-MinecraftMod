# Rites

Every rite in this mod is an **Enchanted circle-magic rite**: you draw a chalk circle, throw the ingredients on the centre, link an **altar** with enough power, and a witch or anyone with the right chalk starts it. This page lists them all. "Power" is the altar power paid when the rite starts. Ways to get altar power are on [Enchanted integration](Enchanted-Integration#altar-power-sources).

- **Small circle** and **medium circle** are Enchanted's circle shapes. The chalk is Enchanted's **ritual chalk**, except the two curses, which need **nether chalk**.
- **Target:** the person who stands on the **centre block** of the circle (the closest player in the 1x2x1 column), or the **caster** if nobody does.
- A rite that **refuses** its target gives the ingredients back, takes no power, and shows the reason to the caster on the action bar.
- **Full moon** means the **night** of the overworld's full moon: moon phase 0 and the time of day between 12786 and 23216 (Werewolves' own full-moon window). Noon of the full-moon day is refused with "This rite can only be performed on a full-moon night." The Nether and the End never have one. This holds for the Rite of the Moon, the Silver Moon, Immortality and Severance.
- Ingredients **in bold** come from another mod. "Silver" is Werewolves' Silver Ingot.
- Costs are before the witch bonus (x0.75), Emissary bonus (x0.9) and siphoned power. See [Witches and siphoners](Witches-And-Siphoners).

## Quick table

| Rite | Circle | Power | Needs | Part |
|---|---|---|---|---|
| [Rite of Immortality](#rite-of-immortality) | medium | 12,000 | | species |
| [Rite of Original's Blood](#species-and-cures) | small | 1,000 | | species |
| [Rite of Doppelgänger Blood](#species-and-cures) | small | 6,000 | Werewolves | species |
| [Rite of Hybrid's Blood](#species-and-cures) | small | 1,000 | Werewolves | species |
| [Rite of the Moon](#rite-of-the-moon) | medium | 15,000 | Werewolves | species |
| [Rite of Ancestral Calling](#rite-of-ancestral-calling) | small | 4,000 | | species |
| [Rite of the Five](#rite-of-the-five) | medium | 10,000 | | species |
| [Rite of Lineage (Reveal)](#rite-of-lineage) | small | 3,000 | Werewolves | species |
| [Rite of Lineage (Awaken)](#rite-of-lineage) | medium | 9,000 | Werewolves | species |
| [Rite of Cleansing Blood](#species-and-cures) | medium | 4,000 | | vampirism_enchanted |
| [Rite of the Silver Moon](#species-and-cures) | medium | 4,000 | Werewolves | vampirism_enchanted |
| [Rite of Blood Offering](#altar-blocks) | small | 1,000 | | vampirism_enchanted |
| [Rite of Purification](#rite-of-purification) | medium | 3,000 | Tensura | enchanted_tensura |
| [Rite of Purification (Holy Water)](#rite-of-purification) | medium | 3,000 | Tensura, Vampirism | enchanted_tensura |
| [Rite of Magicule Offering](#altar-blocks) | small | 0 | Tensura | enchanted_tensura |
| [Curse of Magicule Drain](#curses) | medium (nether chalk) | 2,000 | Tensura | enchanted_tensura |
| [Curse of Skill Seal](#curses) | medium (nether chalk) | 2,000 | Tensura | enchanted_tensura |
| [Ward of Daylight](#artifact-rites) | medium | 6,000 | | artifacts |
| [Ward of the Moon](#artifact-rites) | medium | 6,000 | Werewolves | artifacts |
| [Ward of Return](#artifact-rites) | medium | 8,000 | | artifacts |
| [Rite of the Cure](#rite-of-the-cure) | medium | 20,000 | | artifacts |
| [Rite of the Vampire Compass](#artifact-rites) | medium | 2,000 | | artifacts |
| [Rite of the Hearing Device](#artifact-rites) | medium | 3,000 | | artifacts |
| [Rite of Severance](#rite-of-severance) | medium | 6,000 | | species |

A rite listed with "Werewolves" is not loaded without it (its data file has a Werewolves condition).

## Rite of Immortality

Makes an **Original**. See [Vampires and Originals](Vampires-And-Originals#originals).

| | |
|---|---|
| Circle, power | medium, **12,000** |
| Items | **White Oak Sapling**, **Golden Apple**, **Human Heart** (Vampirism) |
| Also | a **villager** standing in the circle is sacrificed |
| Condition | **full moon** |
| Target | human-like, **no faction, no species**; not over the world's Original cap |
| Result | an Original, in the vampire faction |

## Species and cures

| Rite | Circle, power | Items | Target and conditions | Result |
|---|---|---|---|---|
| **Rite of Original's Blood** | small, 1,000 | Glass Bottle | the target is an **Original** (Original hybrid too) | **Original's Blood** (drinkable) |
| **Rite of Doppelgänger Blood** | small, 6,000 | **filled Taglock** (Enchanted), 2 Diamonds, Amethyst Shard | Werewolves | **Doppelgänger Blood** |
| **Rite of Hybrid's Blood** | small, 1,000 | Glass Bottle | the target is a **hybrid, Original hybrid or tribrid** ("Only a hybrid can do this.") | **Hybrid's Blood** (cures werewolf venom) |
| **Rite of Cleansing Blood** | medium, 4,000 | **Garlic** (Vampirism), **Holy Water Bottle** (Vampirism, normal), Golden Apple | the target should be a vampire; works on heretics, hybrids and tribrids too. **Refused on Originals.** | cures vampirism; hybrid becomes a plain werewolf, tribrid a witch, heretic a siphoner |
| **Rite of the Silver Moon** | medium, 4,000 | 2 **Silver Ingots**, 1 **Wolfsbane** (Werewolves) | **full-moon night**; a werewolf; Werewolves. **Refused on Originals.** | cures lycanthropy; hybrid and tribrid become plain vampires |

For the cure results see [Hybrids](Hybrids-And-Original-Hybrids#ending-it-cures) and [Heretics and Tribrids](Heretics-And-Tribrids).

**Refusals you may see:** "The one in the circle does not belong to the right faction for this rite." / "The rite would change nothing." / "The rite cannot take hold: the new faction would not accept them." / "This rite can only be performed on a full-moon night." / "An Original cannot be cured this way."

## Rite of the Moon

Makes an **Original hybrid**, or breaks a hybrid's curse. See [Hybrids](Hybrids-And-Original-Hybrids#original-hybrids). Needs Werewolves.

| | |
|---|---|
| Circle, power | medium, **15,000** |
| Items | **Moonstone**, **Original's Blood** (the item) |
| Also | a **wolf** and a **villager** in the circle are sacrificed |
| Condition | **full moon** |
| Target | a vampire or werewolf |
| Result | an **Original with the gene** becomes an Original hybrid (unbound, born, Omega). Without the gene: "You do not carry the werewolf gene." A **bound hybrid** of any hybrid type becomes unbound. Any other target is refused. |

## Rite of Ancestral Calling

Makes a witch (75%) or siphoner (25%). See [Witches and siphoners](Witches-And-Siphoners#how-to-become-one-rite-of-ancestral-calling).

| | |
|---|---|
| Circle, power | small, **4,000** |
| Items | Bone, Amethyst Shard, Lapis Lazuli |
| Target | human-like, no species; no faction or the werewolf faction (not a vampire, not a hunter) |

## Rite of the Five

Makes a **Hunter of the Five**. See [Hunters of the Five](Hunters-Of-The-Five).

| | |
|---|---|
| Circle, power | medium, **10,000** |
| Items | 5 Candles, Iron Ingot, Book, **filled Taglock** |
| Target | a human-like Vampirism **hunter** with no species. Refused when the Five are full. |

## Rite of Lineage

Needs Werewolves. See [Werewolves and packs](Werewolves-And-Packs#the-hidden-gene).

| Mode | Circle, power | Items | What it does |
|---|---|---|---|
| **Reveal** | small, 3,000 | Bone, Glowstone Dust, Paper | tells the target whether they carry the gene. Changes nothing. |
| **Awaken** | medium, 9,000 | Moonstone, 3 Bones | gives the gene and awakens it (see the table on the werewolf page) |

## Rite of Purification

| | |
|---|---|
| Circle, power | medium, **3,000** |
| Items (normal) | 2 Gold Ingots, Glowstone Dust |
| Items (Holy Water) | Gold Ingot, **Holy Water Bottle** (Vampirism) |
| Result | **8 holy damage** to every undead within **8 blocks**: Tensura races tagged undead and mobs in the undead tag. Living things are spared. The damage counts as holy for Tensura. |

## Altar blocks

| Rite | Circle, power | Items | Result |
|---|---|---|---|
| **Rite of Blood Offering** | small, 1,000 | 4 **full** Vampirism Blood Bottles, Stone | a **Blood Stone** (altar power 25, up to 16) |
| **Rite of Magicule Offering** | small, **0** | Low Quality Magic Crystal (Tensura), Amethyst Shard | **costs the caster 5000 magicules**; a **Condensed Magicule** block drops above the chalk (altar power 30, up to 16). With too few magicules nothing is taken and the ingredients return. |

## Curses

On a medium circle of **nether chalk**, 2,000 power. The curse falls on the player whose **filled taglock** is used. See [Enchanted integration](Enchanted-Integration#curses-enchanted_tensura).

| Rite | Items |
|---|---|
| **Curse of Magicule Drain** | filled Taglock, Low Quality Magic Crystal, Soul Sand |
| **Curse of Skill Seal** | filled Taglock, Medium Quality Magic Crystal, Soul Sand |

## Artifact rites

All on a medium circle. See [Items and blocks](Items-And-Blocks#rings-and-wards).

| Rite | Power | Items | Result |
|---|---|---|---|
| **Ward of Daylight** | 6,000 | **Sun Ring**, 2 Sunflowers, 4 Glowstone Dust | binds the ring to the **vampire** on the circle |
| **Ward of the Moon** | 6,000 | **Moon Ring**, 2 Amethyst Shards, 2 Silver Ingots (Werewolves) | binds the ring to the **werewolf** on the circle |
| **Ward of Return** | 8,000 | Totem of Undying, 4 Gold Ingots, Diamond | a **Resurrection Ring** |
| **Rite of the Vampire Compass** | 2,000 | Compass, Vervain | a **Vampire Compass** |
| **Rite of the Hearing Device** | 3,000 | Note Block, 2 Iron Ingots, Amethyst Shard | a **Hearing Device** |

A ward refuses (and returns the ingredients) if the wrong faction stands on the circle, the ring is missing ("The rite has nothing to bind."), or it is already bound ("That is already bound to someone.").

## Rite of the Cure

| | |
|---|---|
| Circle, power | medium, **20,000** |
| Items | 2 **White Oak Ash**, 4 **Vervain**, **Enchanted Golden Apple** |
| Condition | a **desiccated Original** (hit with the White Oak Ash Dagger) within **6 blocks** of the circle ("The rite needs a desiccated Original in the circle.") |
| Result | **The Cure** |

## Rite of Severance

Breaks a sired player's whole sire bond. Full details on [Rite of Severance](Rite-Of-Severance).

| | |
|---|---|
| Circle, power | medium, **6,000** |
| Items | **Sire's Blood** (of the target's own sire), **Spirit of Otherwhere** (Enchanted) |
| Performer | a **witch, siphoner, heretic or tribrid** |
| Condition | the **night of a full moon** (full-moon day and time of day 12786 to 23216; noon is refused) |
| Target | the sired player on the centre (else the caster). No consent check. |
| Result | the bond is broken; the sire stays recorded and sireline death still applies |

Note: every rite that says "full moon" needs the night of the full moon, like this one.
