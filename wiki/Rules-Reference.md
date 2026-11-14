# Rules reference

The species rules, with their ids (the "R" numbers used in the code, the changelog and the issue tracker). This is a readable summary: where the plans and the code disagreed, **the code wins**, and the notes below say where. Page links point to the full explanation.

## General

| Id | Rule |
|---|---|
| **R1** | Only players whose Tensura race is human-like can take a bloodline or become one of the Five. Without Tensura everyone qualifies. |
| **R2** | Hybrid types (hybrid, Original hybrid, tribrid) are in the werewolf faction with a vampire layer. For every rule they count as **both** vampire and werewolf. |
| **R3** | Species come only from rites, items and the events below. **The Tensura race is never replaced by a species** (R100). Species unlock apex evolutions. |
| **R4** | Ordinary deaths keep everything (species, rank, mark, sire), except where a rule says otherwise. |
| **R5** | No show names. |

## Humans

| Id | Rule |
|---|---|
| **R10** | The hidden werewolf gene: rolled once at first login (15%) and kept secret. The Rite of Lineage (Reveal) shows it. |
| **R11** | A human with the gene who kills a villager or a player becomes a **born** werewolf (Omega). |
| **R12** | The Rite of Lineage (Awaken) gives the gene. With no faction: a born Omega. A vampire or Original: a bound hybrid or Original hybrid (R24, R35). Hunters and werewolves are refused. |
| **R13** | Resurrection Ring: 3 charges, only against supernatural killers. |

## Vampires

| Id | Rule |
|---|---|
| **R20** | You become one through Vampirism's infection. The Tensura race is **kept**, and you get the vampire layer. Tensura's vampire line joins the faction at a level floor. A cure resets the race to human **only** if it is in that line. |
| **R20a** | A player with no species who joins the vampires with the race exactly `tensura:human` becomes `tensura:vampire`. Every other race is kept. Not configurable. |
| **R20b** | Werewolves follow Tensura's Beastfolk line: an exact `tensura:human` becomes `tensura:beastfolk` when they become a werewolf; on the line an Alpha is a `beast_lord` and a True Alpha a `spirit_beast`. Never lowered; other races kept. Config `werewolf_beastfolk_enabled`. |
| **R21** | Sun damage comes only from Vampirism. A bound Sun Ring blocks it. |
| **R22** | Cured by the Rite of Cleansing Blood or The Cure. |
| **R23** | From level 10, can compel villagers and lesser vampire mobs. |
| **R24** | **Hybrid-awakening rule:** a vampire with the gene who kills a villager or a player becomes a **bound hybrid**, keeping their sire. The Vampirism level carries over (capped). |
| **R25** | A vampire who kills one of the Five gets the hunter's curse. |

## Originals

| Id | Rule |
|---|---|
| **R30** | Rite of Immortality: human-like, no faction, full moon, 12,000 power, a villager sacrificed, a world cap (unlimited by default). |
| **R31** | Vampire faction, +20% attack, +4 max health. Can make Original's Blood. |
| **R32** | Non-white-oak lethal damage puts the Original into **Vampirism's dying state** (or this mod's own downed state for an Original hybrid). A **white oak** hit kills instantly. Void and `/kill` still kill. |
| **R33** | The White Oak Ash Dagger desiccates an Original (30 minutes, until pulled out). It hurts any non-human who wields it. |
| **R34** | Only The Cure works on Originals. |
| **R35** | An **Original hybrid needs the gene**: a natural gene awakened by a kill (bound), or the gene from the Rite of Lineage (Awaken) (bound). The Rite of the Moon without the gene is refused. On a bound hybrid it breaks the curse. |
| **R36** | Sireline death is a config switch, **off** by default. |

## Hybrids

| Id | Rule |
|---|---|
| **R40** | A werewolf who dies with Original's Blood starts a transition (24,000 ticks of play): Doppelgänger Blood makes them a hybrid with that Original as sire; any other blood makes them unstable and then they die; running out of time kills them. |
| **R41** | R24 also makes a hybrid. |
| **R42** | Traits: speed and attack grow over 50 in-game days, blood thirst 0 to 20 (Weakness at 0, Strength at 15+), shifting at will once unbound (R96). |
| **R42v** | **Venom:** werewolf, hybrid, Original hybrid and tribrid hits poison vampires: 1 damage every 40 ticks plus hallucinations, and it kills a non-Original vampire after 12,000 ticks. Originals only get Weakness II. **Only Hybrid's Blood cures it.** |
| **R43** | A hybrid can't hurt their sire. The sire can use `/bloodline call`. |
| **R44** | Cleansing Blood turns them into a plain werewolf. The Cure turns them human. |
| **R45** | A second dose of Original's Blood does nothing. |

## Original hybrids

| Id | Rule |
|---|---|
| **R50** | They follow the Original rules (R31 to R34) and the hybrid rules (R42, R42v). Werewolf faction. |

## Witches, siphoners, heretics

| Id | Rule |
|---|---|
| **R60** | Rite of Ancestral Calling: 75% witch, 25% siphoner. Only for players with no faction or the werewolf faction. |
| **R61** | Witch: altar range x1.5 and rite cost x0.75. Joining the vampires removes their magic. |
| **R62** | Siphoner: drains up to 1,000 power from blocks, mobs and players. Joining the vampires makes them a heretic. A siphoner can't start the hybrid transition. |
| **R63** | Heretic: vampire faction, keeps siphoning. |
| **R64** | A witch-werewolf who completes the hybrid transition becomes a tribrid. |
| **R65** | Siphoned power resets on death. |

## Tribrids

| Id | Rule |
|---|---|
| **R70** | Hybrid traits plus the witch bonuses. |
| **R71** | Cleansing Blood turns them into a witch-werewolf. The Cure turns them human. |

## Hunters of the Five

| Id | Rule |
|---|---|
| **R80** | Rite of the Five: 10,000 power, only for human-like Vampirism hunters, while fewer than 5 marked hunters exist (config). |
| **R81** | **Prey** = vampires, werewolves, hybrids, Originals, heretics, tribrids. **Witches and siphoners are not prey.** The mark grows per prey kill (+1% attack, +0.5% speed, capped at +20% and +10%). From 5 kills nearby prey glows, visible only to that hunter. Marked hunters are immune to compulsion. |
| **R82** | A marked hunter killed by **prey** loses the mark. A random online eligible hunter becomes a potential, whose first prey kill makes them one of the Five. |
| **R83** | **Hunter's curse:** any prey killer of a marked hunter hallucinates until a potential activates. Humans and witches are exempt. It doesn't stack. |
| **R84** | Instinct: after 3 in-game days without a prey kill, Weakness until the next one. |

## Werewolves

| Id | Rule |
|---|---|
| **R90** | **Born:** half the full-moon forced-shift chance. (The old "four-legged form 5 levels sooner" is **not possible**, see [Known limits](Known-Limits).) **Bitten:** counts double for the pack bonus. |
| **R91** | **Ranks.** Omega: no pack, -10% attack and speed. Beta: in a pack. Alpha: red eyes, +20% attack, +4 max health, the Roar. **True Alpha:** no per-world limit, very hard to earn (max Werewolves level, 30 in-game days as a Beta, 300 defender kills, never killed an innocent, never stole a spark); Alpha perks +10%. Betas and Omegas have gold eyes; an innocent kill turns them blue for good. |
| **R92** | Only Alphas turn humans, with a 10% chance the bite kills instead. Wild werewolf mobs make Omegas. A human turned by an Alpha joins that Alpha's pack and sireline. |
| **R93** | **The spark:** stolen by killing the Alpha; inherited by the strongest Beta when the Alpha leaves or is cured; given up to heal someone; earned (R91). |
| **R94** | **Pack:** 3 or more Betas near their online Alpha give +5% attack per Beta (up to +25%) and Regeneration to the Betas. **Roar:** buffs the pack and forces other werewolves out of wolf form. **Emissary:** a witch whose rites cost 10% less near the pack. **Anchor:** an item or a nearby player halves the forced full-moon shift. |
| **R95** | **Weaknesses.** Wolfsbane strains: common (Weakness and Nausea), yellow (damage over time), northern blue (lethal in 6,000 ticks unless cured with the ash of the same strain), wolf lichen (no healing and no transforming for 1,200 ticks). Silver. **Mountain ash** lines block vampires, werewolves, hybrids, Originals, heretics and tribrids; witches pass. Lunar eclipse: 3% of full moons, no powers until dawn. Electricity: lightning or a Taser stops forms and healing for 30 s. |
| **R96** | **Moon control** (`moon_bound_werewolves`, default on). Without control a werewolf transforms **only** when the full moon forces it. Control comes from a bound **Moon Ring** (half forms at will, full forms from Werewolves level 8), **hybrids, Original hybrids and tribrids** once unbound, **Alphas and True Alphas**, and **born werewolves with an anchor** (half only). The Moon Ring and the exemptions also stop the forced full-moon shift. |

## Tensura links

| Id | Rule |
|---|---|
| **R100** | **The race is kept and species are a layer on it:** Tensura skills, attributes and a magicule or aura bonus; Tensura treats vampire types as undead (holy and light damage x3, holy exposure and the rest); a bonus to evolution progress (x1.25 by default); and **one apex evolution** pickable at saint tier or higher in Tensura's evolution screen. Apex races keep the human-like tag (and undead for vampire types). Losing the species doesn't remove a chosen apex race, but its species-only skills go dormant. **Note:** the code makes Tensura treat the layer as undead in general, not only for holy damage and sunlight, and a plain Vampirism vampire too. |
| **R101** | A sire becomes their sired's **Tensura permanent owner**. The sire can **name** their sired through Tensura's own menu (players too, even with `playerNaming` off, but only their own sired and only if the sired turned their own naming on). It costs magicules and gives Tensura's naming rewards (EP and stats); a named sired **player** never gets the evolution step (the sire is told). `sire_naming_enabled` bypasses the `playerNaming` rule for that pair only. Siring needs no consent. |
| **R101b** | The **Rite of Severance** breaks the whole sire bond of a sired player: no Tensura owner link, no sire naming exception, they can hurt their sire (R43 ends), no `/bloodline call`, and the sire's compelled creatures no longer spare them. **The sire stays recorded** (the tree shows it) and **sireline death still applies** (blood, not bond). A new sire replaces the old one and ends the severance. Performer: a witch, siphoner, heretic or tribrid at an altar, with the sired in the circle. Cost: **the night of a full moon**, 6000 power, **Sire's Blood** of the sired's own sire (taken with a silver blade and a glass bottle) and a **Spirit of Otherwhere**. Siring itself needs no consent. See [Rite of Severance](Rite-Of-Severance). |
| **R102** | Werewolf ranks grant Tensura skills, and the race is kept. |
| **R103** | Hunters and werewolves can't be undead or daemon races. **Exception:** an apex race of this mod, for vampire-type species only (Original, Original hybrid, hybrid, heretic, tribrid). |

## Apex races (R100)

| Species | Apex race |
|---|---|
| Original | Primordial Vampire |
| Original hybrid | Primordial Hybrid |
| Hybrid | Moonblood Hybrid |
| Witch | Ancestral Witch |
| Siphoner, heretic | Void Siphon |
| Tribrid | Tribrid Ascendant |
| Hunter of the Five | Eternal Hunter |

Details are on [Tensura integration](Tensura-Integration#apex-evolutions).
