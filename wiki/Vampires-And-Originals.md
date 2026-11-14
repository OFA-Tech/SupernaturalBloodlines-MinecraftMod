# Vampires and Originals

Needs Vampirism and Enchanted. (Parts: `species` and `vampirism_enchanted`; see [Parts and compatibility](Parts-And-Compatibility).) Only players whose Tensura race is human-like can take a bloodline; without Tensura, everyone can (R1).

## Plain vampires

You become a vampire through **Vampirism's normal infection** (sanguinare). Everything about the vampire faction, levels and blood is Vampirism's own. This mod adds:

- **With Tensura:** you keep your Tensura race; a human becomes `tensura:vampire`. See [Tensura integration](Tensura-Integration).
- **Sun:** only Vampirism's sun damage applies. A bound [Sun Ring](Items-And-Blocks#rings-and-wards) blocks it, and a [Sun Poppet](Enchanted-Integration#poppets) saves you from a lethal sun hit.
- **Compulsion** from level 10. See [Sirelines](Sirelines#compulsion).
- **Cure:** the [Rite of Cleansing Blood](Rites#species-and-cures) or The Cure.
- **Sire:** the vampire who infected you is recorded as your **sire** (see [Sirelines](Sirelines)). Vampire mobs are not recorded.
- **Hunted by hunters:** vampires are prey for [Hunters of the Five](Hunters-Of-The-Five). Killing one of the Five brings the hunter's curse.
- **Werewolf gene:** a vampire who has the hidden werewolf gene and kills a villager or a player becomes a bound **hybrid** (needs Werewolves; see [Hybrids](Hybrids-And-Original-Hybrids)). The Vampirism level carries over (capped).

**Weaknesses of vampires** (and of every other vampire-type species): [mountain ash](Items-And-Blocks#mountain-ash) lines, [vervain](Items-And-Blocks#vervain), werewolf venom, the White Oak Ash Dagger, the Hearing Device.

## Originals

An Original is the first of their line: hard to kill and very strong.

### How to become one

Perform the **Rite of Immortality** on a medium circle (see [Rites](Rites#rite-of-immortality)):

- items: **White Oak Sapling, Golden Apple, Human Heart** (Vampirism)
- a **villager** standing in the circle is sacrificed
- **12,000** altar power, on a **full moon**
- the target (the player on the circle, or the caster) must be **human-like, with no faction and no species**
- refused if the world already has as many Originals as `max_originals_per_world` allows (default: unlimited, see [Configuration](Configuration#species-supernaturalbloodlines-speciestoml))

Without Werewolves this still works. The result is the vampire faction plus the Original species.

### What you get

- **+4 max health** and **+20% attack damage** (applies on top of weapon damage).
- **Lethal damage does not kill you**, unless it is from a **White Oak** weapon, the void, or `/kill`:
  - A vampire Original goes into **Vampirism's own dying state** (the *Give up / Wait* screen).
  - An Original **hybrid** goes into this mod's own **downed** state: 30 seconds on the ground, unable to move or act, then back up at half health. A screen offers **Give up** or **Wait**.
- You can make **Original's Blood** ([Rite of Original's Blood](Rites#species-and-cures)): a glass bottle on a small circle, 1000 power. Only Originals can do it; for anyone else the bottle comes back and the caster sees "Only an Original can do this."
- You can compel villagers and lesser vampires, and call your sired. See [Sirelines](Sirelines).
- With the Tensura part: skills `blood_mist`, `drain`, `charm`, `self_regeneration`, +20% max magicule, and **undead** treatment. See [Tensura integration](Tensura-Integration#the-species-layer-part-species_tensura).
- Tensura apex race: **Primordial Vampire** (needs saint tier or higher, 1,000,000 EP).

### Original's Blood and hybrids

Drinking Original's Blood gives the `Original's Blood` effect (5 minutes). A **werewolf** who **dies** with that effect starts the **hybrid transition**. The last Original's Blood a player drank before dying decides who becomes their sire. A second dose does nothing. See [Hybrids](Hybrids-And-Original-Hybrids#the-transition).

### How to kill an Original

- **White Oak Stake** (the only weapon that kills them at once; skips the dying state). Any weapon in the `supernaturalbloodlines:white_oak` item tag counts.
- Void damage and `/kill`.
- With the optional **sireline death** setting on, killing an Original with white oak also kills everyone they sired (and everyone those sired). See [Sirelines](Sirelines#sireline-death).
- **White Oak Ash Dagger:** a hit leaves the dagger in them as the *Desiccated* effect for up to **30 minutes**: they cannot move, attack or use items until another player pulls it out (right click), it times out, it is removed, or they die.

### How to cure an Original

**Only The Cure works.** The Rite of Cleansing Blood and the Rite of the Silver Moon are refused ("An Original cannot be cured this way."). To make The Cure you need a *desiccated* Original (see [Rites](Rites#rite-of-the-cure)). A desiccated Original cannot drink it; someone feeds it to them. Drinking it removes every bloodline layer and the faction.

### World limit

`max_originals_per_world` counts every Original and Original hybrid ever made, **even ones who never log in again**.

### Switching off

`parts.species` off: Originals stay as vampires in Vampirism, but lose the protection, the bonuses and the rites. See [For server owners](For-Server-Owners#switching-parts-off).
