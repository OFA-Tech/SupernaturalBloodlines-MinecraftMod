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
| [Rite of Revelation](#rite-of-revelation) | small | 1,000 | | species |
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
| [Rite of the Cursed Dagger](#artifact-rites) | medium | 20,000 | | artifacts, species |
| [Rite of Severance](#rite-of-severance) | medium | 6,000 | | species |
| [Rite of Cloaking](#kols-and-esthers-grimoires) | medium | 4,000 | | bosses |
| [Linking Spell](#kols-and-esthers-grimoires) | medium | 5,000 | | bosses |
| [Rite of the Valet Pike](Werewolves-And-Packs#the-beast-of-gévaudan) | medium | 7,000 | full moon | bosses |
| [Rite of the Frequency](Werewolves-And-Packs#the-beast-of-gévaudan) | medium | 8,000 | full moon | bosses |

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
| **Rite of Original's Blood** | small, 1,000 | Glass Bottle | the target is an **Original** (Original hybrid too). Someone else's blood needs their consent: they **sneak on the centre**; otherwise the caster only sees "The rite would change nothing." and the bottle comes back, whoever stands there | **Original's Blood** (drinkable) |
| **Rite of Doppelgänger Blood** | small, 6,000 | **filled Taglock** (Enchanted), 2 Diamonds, Amethyst Shard | Werewolves. The taglock must be of a **doppelgänger** villager or a player with the doppelgänger gene (`doppelganger_rite_taglock`, or any human-like villager or player with `human`). The owner must be nearby (a villager) or online (a player). A player owner learns whether they carry the gene. **A refused rite tells the caster that the owner is not a doppelgänger** (or not human-like), so it can be used to test a player's taglock: the owner is not told. | **Doppelgänger Blood** |
| **Rite of Hybrid's Blood** | small, 1,000 | Glass Bottle | the target is a **hybrid, Original hybrid or tribrid**. Someone else's blood needs their consent: they **sneak on the centre**; otherwise the caster only sees "The rite would change nothing." and the bottle comes back, whoever stands there ("Only a hybrid can do this." is shown only to a caster who is their own target) | **Hybrid's Blood** (cures werewolf venom) |
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

## Rite of Revelation

Small circle, 1,000 power, one **filled Taglock** (Enchanted) of the subject. It makes **every gene the owner was rolled for** known to them (carried or not; the werewolf gene is skipped when it was never rolled, for example without Werewolves) and tells them, and only them, which they carry. See [genes](Werewolves-And-Packs#genes).

- Only a **player's** taglock works ("The Rite of Revelation needs a player's own taglock."), and the player must be **online**. An offline owner, an unfilled taglock or a villager's taglock is refused with the reason and the taglock comes back.
- The caster learns nothing except that the rite worked; with someone else's taglock only the owner gets the message.
- Needs only the species part (not Werewolves).
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
| **Rite of the Cursed Dagger** | 20,000 | White Oak Ash Dagger, 2 White Oak Ash, Echo Shard (needs species too) | a **Cursed White Oak Ash Dagger**, which also desiccates an Original hybrid. Acts on nobody on the circle. The book keeps it sealed until you are a witch, a siphoner or a tribrid. |

A ward refuses (and returns the ingredients) if the wrong faction stands on the circle, the ring is missing ("The rite has nothing to bind."), or it is already bound ("That is already bound to someone.").

## Rite of the Cure

| | |
|---|---|
| Circle, power | medium, **20,000** |
| Items | 2 **White Oak Ash**, 4 **Vervain**, **Enchanted Golden Apple** |
| Condition | a **desiccated Original** (hit with the White Oak Ash Dagger) within **6 blocks** of the circle ("The rite needs a desiccated Original in the circle.") |
| Result | **The Cure** |
| After | The caster is told "The rite is done. Something old has noticed." A minute later (`rebekah_delay_seconds`) **Rebekah** comes for them, from 12 to 48 blocks away; a mountain ash line laid by a pack Emissary holds her. A Cure that was given or dropped does not call her. |

## Rite of Severance

Breaks a sired player's whole sire bond. Full details on [Rite of Severance](Rite-Of-Severance).

| | |
|---|---|
| Circle, power | medium, **6,000** |
| Items | **Sire's Blood** (of the target's own sire), **Spirit of Otherwhere** (Enchanted) |
| Performer | a **witch, siphoner, heretic or tribrid** |
| Condition | the **night of a full moon** (full-moon day and time of day 12786 to 23216; noon is refused) |
| Target | the sired player on the centre (else the caster). No consent check. |
| Result | the bond is broken; the sire stays recorded and line death still applies |

Note: every rite that says "full moon" needs the night of the full moon, like this one.

## Rite of the Broken Line and Rite of the Imprisoned Original

Witch-type performer, lunar eclipse. The Broken Line frees one player from an Original's line; the Imprisoned Original frees a player and their whole branch, with the Original, brought down (downed, dying or desiccated), held inside a closed mountain ash circle for 30 seconds. Neither rite asks the target: a witch can free an unwilling player. See [Original Bloodlines](Original-Bloodlines).

## Kol's and Esther's grimoires

Two rites that need a boss's reward as a key. The grimoire is **given back** when the rite works.

**Rite of Cloaking** (medium circle, 4,000 power): Kol's Grimoire (read once, for its skill points), 2 phantom membrane, 1 ender pearl. A witch, siphoner, heretic or tribrid performs it. For `kol_cloak_minutes` (30) you are left out of the Vampire Compass and of a Hunter of the Five's sense. You are told when the cloak fades; dying ends it. A plain player is told only a witch type can work it, and an unread grimoire is refused.

**Linking Spell** (medium circle, 5,000 power): Esther's Grimoire, 2 lead, 2 amethyst shards. Only a witch type can work it. Exactly two living creatures stand inside the chalk (it is 11 wide: within 5.5 blocks of the altar, feet within 2 blocks of it); you count if you stand in it, so you may link yourself to a creature. Neither may be one of the NPC Originals, a boss, or linked already. **A player, including a player Original, agrees by sneaking inside the circle with a lead in their main hand** (and PvP must be on): sneaking alone, or a lead alone, is not a yes. For `link_seconds` (60) half the damage one of them takes (`link_damage_share`) is dealt to the other in your name, once, never back again; a player's own hit cooldown is respected. It ends when either dies, leaves the dimension, or its time runs out, and a linked player is told at both ends.

**Every refusal is the same**: "The link will not hold." It costs the lead, the amethyst and the altar's power, and the grimoire always comes back. That is deliberate, so a refusal never tells you who is a boss, whether somebody did not agree, or whether a creature was linked already. (A performer who is not a witch type is told so and gets everything back.) A partner who is not loaded owes a creature's share (never a player's) until it loads, and then takes at most half its health.

## Wards, the locator and two more curses (plan 21)

| Rite | Who | What |
|---|---|---|
| **Rite of the Ward** | a witch, siphoner, heretic or tribrid | Raises a ward on the altar. A vampire who is not invited is pushed out (`ward_radius`, 10). Whoever sneaks on the centre owns it; a vampire cannot own one. |
| **Rite of the Door Ward** | the same | The small form: one door, trapdoor or gate within four blocks stays shut to an uninvited vampire. |
| **Rite of the Locator** (taglock, Original's Blood, Hybrid's Blood) | the same | Attunes a **Vampire Compass** to somebody for 30 minutes. See [Items and Blocks](Items-And-Blocks). |
| **Curse of Insanity** | anyone, with a taglock, a fermented spider eye and soul sand | Hallucinations every half minute or so. Needs Vampirism and Enchanted. |
| **Curse of Susceptibility** | anyone, with a taglock, a vampire blood bottle and soul sand | 25% more damage taken per strength level plus one, never more than double. |

The **ward** falls when its owner dies or becomes a vampire; invite people on the Path page (*Your ward*), with `/ward invite <player>`, or by saying "come in" (and "get out" to withdraw) near it. A ward never says what anybody is: every refusal is the same line. The **locator**'s needle spins the same way for every reason somebody cannot be found, and the one looked for is told "Somebody is looking for you." once. Both curses end when the target dies.

