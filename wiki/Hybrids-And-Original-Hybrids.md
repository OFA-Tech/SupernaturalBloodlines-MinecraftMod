# Hybrids and Original hybrids

**Needs Werewolves**, as well as Vampirism and Enchanted. Without Werewolves a hybrid type can't be made: the rites are not loaded and anything that would make one is refused ("This bloodline needs the Werewolves mod.").

A hybrid is a player in the **werewolf faction** with a **vampire layer**. For every rule in this mod a hybrid counts as **both** a vampire and a werewolf (R2).

There are three hybrid types:

| Type | Made from | See |
|---|---|---|
| **Hybrid** | a werewolf who completes the transition, or a vampire who awakens the gene | below |
| **Original hybrid** | an Original with the werewolf gene | [Original hybrids](#original-hybrids) |
| **Tribrid** | a witch-werewolf who completes the transition | [Heretics and Tribrids](Heretics-And-Tribrids) |

## The transition

A werewolf can become a hybrid by dying the right way.

1. An **Original** makes **Original's Blood** ([Rites](Rites#species-and-cures)) and the werewolf **drinks** it. This gives the `Original's Blood` effect (5 minutes).
2. The werewolf **dies** with the effect active. The transition starts: they respawn (the timer survives a respawn and a relog) with **24,000 ticks of online play time** (20 minutes; config `transition_ticks`). The Original whose blood they last drank is their **sire**.
3. During the transition they must drink **Doppelgänger Blood**. That completes it: they become a hybrid.

| What happens | Result |
|---|---|
| Drink **Doppelgänger Blood** | become a hybrid (a **tribrid** if they are a witch). The sire is recorded. |
| Drink **any other blood** (Vampirism blood bottles) | the transition turns **unstable** (the `Unstable` effect, 1 minute). When it runs out they **die** and stay a werewolf. |
| **Time runs out** | they die and stay a werewolf. |
| Drink Original's Blood again | nothing (a second dose does nothing, R45) |
| A **siphoner** | can't start the transition ("A siphoner cannot become a hybrid.") |

The death at the end of a transition ("Name's body could not bear the change") cannot be stopped by a Totem of Undying or Resistance.

**Doppelgänger Blood** is made with the **Rite of Doppelgänger Blood** (small circle, 6000 power: a filled taglock, 2 diamonds, an amethyst shard; the taglock only has to be filled, it doesn't matter whose). See [Rites](Rites#species-and-cures).

## Other ways to become a hybrid

- **The gene kill (R24).** A **vampire** or **Original** with the hidden werewolf gene who kills a **villager** or a **player** becomes a hybrid or an Original hybrid. They keep their sire. Their werewolf side is **bound** until the Rite of the Moon. The Vampirism level carries over (capped at the werewolf maximum). A heretic is refused (no hybrids from siphoners). Wandering traders don't count.
- **Rite of Lineage (Awaken)** on a vampire or an Original gives the gene *and* awakens it: a bound hybrid or Original hybrid. See [Werewolves and packs](Werewolves-And-Packs#the-hidden-gene).

### Bound hybrids

A hybrid made by the gene kill or the lineage rite is **bound**: no wolf forms at all ("The curse binds your wolf: you cannot shift until the Rite of the Moon."). The **Rite of the Moon** breaks the curse. It is refused on an unbound hybrid. See [Rites](Rites#rite-of-the-moon).

## What a hybrid gets

- **Speed and attack that grow with age.** +0.02 movement speed and +2 attack damage, reached over **50 in-game days** (1,200,000 ticks) after turning. A hybrid with no recorded turning time counts as fully grown.
- **Blood thirst** (0 to 20): a hybrid cannot drink Vampirism's bottles the normal way, so a right-click on a blood bottle gives **+5 thirst** (only below 20) and gives a glass bottle back. Thirst falls by 1 every 1200 ticks (1 minute).
  - **0:** Weakness I
  - **15 or more:** Strength I
- **Shift at will.** Once unbound, a hybrid shifts form whenever they want, and the forced full-moon shift is cancelled. See [moon control](Werewolves-And-Packs#moon-control).
- **Venom as a weapon.** A melee hit or bite of a werewolf, hybrid, Original hybrid or tribrid on a **vampire** poisons them (see below).
- **Sire bond.** A hybrid **cannot hurt their sire**, directly, by projectile, area cloud or pet. The sire can use `/bloodline call` on them. See [Sirelines](Sirelines).
- With the Tensura part: skills `drain`, `beast_transformation`, `self_regeneration`, +10% max magicule, undead treatment. Tensura apex race: **Moonblood Hybrid**.

## Werewolf venom

A werewolf-type hit on a vampire applies the `Werewolf Venom` effect once (a second bite does not reset it):

- **10 minutes** (12,000 ticks).
- Every 2 seconds: **1 damage** that never takes the last half-heart, and every 30 seconds a hallucination (and Nausea II).
- At the end it **kills** a non-Original vampire. A Totem of Undying, Resistance and Vampirism's dying state do not save them.
- **Originals and Original hybrids** only get Weakness II for those 10 minutes.
- **Milk does not cure it.** Only **Hybrid's Blood** does.

**Hybrid's Blood** is made with the **Rite of Hybrid's Blood**: a glass bottle on a small circle (1000 power) with a **hybrid, Original hybrid or tribrid** standing on the circle. Original's Blood does *not* cure venom. You drink it only while poisoned, and you get the glass bottle back.

## Weaknesses

Mountain ash, wolfsbane strains, silver (as a werewolf), electricity (the Taser, lightning), a lunar eclipse (no form actions until dawn), and everything that hurts vampires. See [Werewolves and packs](Werewolves-And-Packs#weaknesses) and [Items and blocks](Items-And-Blocks).

## Ending it (cures)

| Cure | Hybrid | Tribrid |
|---|---|---|
| **Rite of Cleansing Blood** | becomes a plain **werewolf** | becomes a **witch** (still a werewolf, with the witch layer) |
| **Rite of the Silver Moon** | becomes a plain **vampire** | becomes a plain **vampire** (loses the witch layer) |
| **The Cure** | becomes **human** | becomes **human** |

Leaving the werewolf faction by any other means (a command, Vampirism's screens) also turns a hybrid into a human.

## Original hybrids

An Original hybrid has **both** the Original rules (+4 health, +20% attack, can't die except to white oak, void or `/kill`) and the hybrid rules (age traits, thirst, venom, sire bond). They are in the **werewolf faction**, so lethal damage puts them in this mod's **downed** state (30 seconds, **Give up / Wait**) instead of Vampirism's dying state.

**How to become one:** the **Rite of the Moon** on an **Original who carries the werewolf gene** (R35):

- medium circle, **15,000** power, **full moon**
- items: a **Moonstone** and an **Original's Blood**
- a **wolf** and a **villager** in the circle are sacrificed
- the target must be a vampire or werewolf (an Original)
- without the gene it is refused ("You do not carry the werewolf gene."). The gene comes from a natural roll, or from the **Rite of Lineage (Awaken)**, which makes a bound Original hybrid at once.

The result is unbound, a born Omega, and in the werewolf faction.

**Cures:** none of the rites works. **Only The Cure** (R34).

Tensura skills (default layer): `blood_mist`, `drain`, `charm`, `self_regeneration`, `beast_transformation`, `godwolf_sense`, +25% max magicule, undead. Tensura apex race: **Primordial Hybrid**.

## Without Werewolves

A hybrid type is never made. The Rites of Doppelgänger Blood, Hybrid's Blood, the Moon and Lineage don't exist. Originals, witches, heretics and hunters work as normal.
