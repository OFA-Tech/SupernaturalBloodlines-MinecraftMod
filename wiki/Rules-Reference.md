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
| **R31** | Vampire faction, +20% attack, +4 max health. Can make Original's Blood (someone else's blood needs their consent: they sneak on the centre; without it the caster is told only that the rite would change nothing). |
| **R32** | Non-white-oak lethal damage puts the Original into **Vampirism's dying state** (or this mod's own downed state for an Original hybrid). A **white oak** hit kills instantly. Void and `/kill` still kill. |
| **R33** | The White Oak Ash Dagger desiccates an Original (30 minutes, until pulled out). It hurts any non-human who wields it. |
| **R34** | Only The Cure works on Originals. |
| **R35** | An **Original hybrid needs the gene**: a natural gene awakened by a kill (bound), or the gene from the Rite of Lineage (Awaken) (bound). The Rite of the Moon without the gene is refused. On a bound hybrid it breaks the curse. |
| **R36** | Replaced by R122 (line death, on by default; the old `sireline_death` switch is gone). |

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
| **R93** | **The spark:** stolen by killing an Alpha from outside the pack (the killer keeps their own and gains every spark the victim had: the stacked sparks); a kill inside the pack changes nothing; inherited by the strongest second Alpha, else the strongest Beta, when the Alpha leaves or is cured; given up to heal someone; earned (R91). |
| **R94** | **Pack:** 3 or more Betas near their online Alpha give +5% attack per Beta (up to +25%) and Regeneration to the Betas. **Roar:** buffs the pack and forces other werewolves out of wolf form. **Emissary:** a witch whose rites cost 10% less near the pack. **Anchor:** an item or a nearby player halves the forced full-moon shift. |
| **R95** | **Weaknesses.** Wolfsbane strains: common (Weakness and Nausea), yellow (damage over time), northern blue (lethal in 6,000 ticks unless cured with the ash of the same strain), wolf lichen (no healing and no transforming for 1,200 ticks). Silver. **Mountain ash** lines block vampires, werewolves, hybrids, Originals, heretics and tribrids; witches pass. Lunar eclipse: 3% of full moons, no powers until dawn. Electricity: lightning or a Taser stops forms and healing for 30 s. |
| **R96** | **Moon control** (`moon_bound_werewolves`, default on). Without control a werewolf transforms **only** when the full moon forces it. Control comes from a bound **Moon Ring** (half forms at will, full forms from Werewolves level 8), **hybrids, Original hybrids and tribrids** once unbound, **Alphas and True Alphas**, and **born werewolves with an anchor** (half only). The Moon Ring and the exemptions also stop the forced full-moon shift. |

## Tensura links

| Id | Rule |
|---|---|
| **R100** | **The race is kept and species are a layer on it:** Tensura skills, attributes and a magicule or aura bonus; Tensura treats vampire types as undead (holy and light damage x3, holy exposure and the rest); +25% EP gain (`evolution_progress_multiplier`, 1.25 by default; it speeds up EP gain only and does not touch the evolution progress or its requirements); and **one apex evolution** pickable at saint tier or higher in Tensura's evolution screen. Apex races keep the human-like tag (and undead for vampire types). Losing the species doesn't remove a chosen apex race, but its species-only skills go dormant. **Note:** the code makes Tensura treat the layer as undead in general, not only for holy damage and sunlight, and a plain Vampirism vampire too. |
| **R101** | A sire becomes their sired's **Tensura permanent owner**. Siring **names the sired** in Tensura **automatically**, with the sired's own current name (never a new one), when Tensura is installed and the part is on: the name is the compatibility link. It costs the sire Tensura's magicules and gives the sired Tensura's naming rewards (EP and stats); it is a LOW naming, so a sired **player** never gets the evolution step. `playerNaming=false` does not block it, and the sired's own naming toggle is not needed. It waits for the sire to be online and able to pay (tried at the link and at each login of either). Siring needs no consent. |
| **R101b** | The **Rite of Severance** breaks the whole sire bond of a sired player: no Tensura owner link, no sire naming exception, they can hurt their sire (R43 ends), no `/bloodline call`, and the sire's compelled creatures no longer spare them. **The sire stays recorded** (the tree shows it) and **sireline death still applies** (blood, not bond). A new sire replaces the old one and ends the severance. Performer: a witch, siphoner, heretic or tribrid at an altar, with the sired in the circle. Cost: **the night of a full moon**, 6000 power, **Sire's Blood** of the sired's own sire (taken with a silver blade and a glass bottle) and a **Spirit of Otherwhere**. Siring itself needs no consent. See [Rite of Severance](Rite-Of-Severance). |
| **R102** | Werewolf ranks grant Tensura skills, and the race is kept. |
| **R103** | Hunters and werewolves can't be undead or daemon races. **Exception:** an apex race of this mod, for vampire-type species only (Original, Original hybrid, hybrid, heretic, tribrid). |

## Original bloodlines (R120 to R127)

| Id | Rule |
|---|---|
| **R120** | Every vampire belongs to the **line of one Original**, stored server-wide: a player Original's own UUID, or one of six fixed NPC roots (Mikael, Finn, Elijah, Kol, Rebekah, Klaus). An Original is the root of their own line and never a member of it. A player made Original is the root of their own line even if an Original sired them; the branch below them moves with them. |
| **R121** | A child of an Original joins that Original's line; a child of a member joins the member's root; a child of a sire with no line has none. A player turned by an NPC inherits the NPC's line. Vampirism vampire creatures get a weighted NPC root when they first appear (not the types in `#line_exempt`); an un-sired one keeps it on the creature alone, a sired one is a member of the line. A cured player leaves it. |
| **R122** | **Line death** (`original_line_death`, on by default): killing an Original or Original hybrid with white oak kills every vampire and player in their line (never a converted animal or villager). Only a melee white-oak blow by someone other than the Original counts: the Original's own white oak, or a death with no killer, does nothing. Offline players pay at login, once, if still in the line and still supernatural. Someone freed by a rite survives. |
| **R123** | A player sees the Original of their line as **?** until they discover it (`original_visibility`: `discovered` or `always`). The Rite of Revelation (which also reveals genes) discovers it; discovery is per player. Player ancestors are visible, NPC ancestors and an NPC sire are **?**. |
| **R165** | Being hunted by Klaus (a doppelgänger player, `klaus_hunts_players`) discovers the doppelgänger gene for the target, and the message says why. The howl is heard by the target alone. |
| **R166** | A plain mountain ash line does not hold Klaus or Mikael; one laid by a pack Emissary does. |
| **R167** | Elijah's deal: one per player ever; an oath that holds through its last day; broken by a blow to Elijah or to any NPC Original but Mikael while it binds; then he is hostile for good and makes no more deals. The offer lists every NPC Original but Elijah, the same for everybody, so it never gives away which line the player is in. A tamed animal or a minion strikes for its owner, unless the Original was already after that owner (then it is defending them); any other mob's blow never breaks it. |
| **R168** | The Elixir saves from any death but a line death, `/kill`, the white oak end of an Original, a death that is part of turning into something (vampire blood still running, Original blood, an unstable hybrid transition) and the void, once; the Resurrection Ring goes first. |
| **R169** | Rebekah answers a Cure made at the rite (not a given one) after `rebekah_delay_seconds`, only while she is unseen or lost, and hunts whoever cast it. |
| **R170** | Elijah comes once a day to a player who knows an Original or holds an NPC Original's blood; he fights only after a broken oath or when hit. |
| **R172** | The Rite of Cloaking hides its performer from the Vampire Compass and from a hunter's sense for `kol_cloak_minutes`; a new cloak never shortens a running one. |
| **R173** | Kol comes by night, once a night, to a Hunter of the Five with at least `kol_min_mark` kills on the ground in the overworld; fast, with a leap; he withdraws after a minute with nobody to hunt. |
| **R174** | Kol's Grimoire pays `kol_grimoire_sp` skill points once per copy; the Cloaking rite needs a witch type and a read grimoire and gives it back. |
| **R175** | Finn comes under a lunar eclipse, one roll a night, to a witch type or a vampire; a finished ritual puts his next visit a week away. |
| **R176** | Finn's ritual hurts nobody. Hurt him for a fifth of his health or pull him 24 blocks from the ring and he fights instead. |
| **R177** | The Linking Spell is a witch-type rite: exactly two living creatures inside the chalk (5.5 blocks, feet within 2), never an NPC Original or a boss or one already linked; a player (a player Original too) only if they sneak in the circle with a lead in the main hand and PvP is on. Damage is shared in the performer's name. Every refusal is the same message at the same cost, so it never names an Original. Damage is shared once, never in a loop; it ends with a death, a dimension change or its time; linked players are told at both ends. |
| **R178** | Deucalion comes on a full moon night, once per world, for a player Alpha who stacks at least `deucalion_min_sparks` (3; a True Alpha counts as holding the full stack), with `deucalion_pack_alphas` Alphas of his own; he drops to fours below three quarters of his health or with his target within 8 blocks at night, and never changes back; his glasses go with the shift. He may come again only with `deucalion_respawn` on. |
| **R179** | The player who does the most damage to Deucalion (a tie goes to the last blow; a pet's damage is its owner's) takes `deucalion_sparks` (4) by the rules of a stolen spark: a True Alpha takes nothing (but learns the Roar), an open protected path to True Alpha destroys them, the stack never goes above `max_stacked_sparks`, and somebody who is no werewolf gets nothing. The game tells whoever is paid nothing why. His pack gives no weak spark and no drops. A mob's kill pays nothing. |
| **R180** | The Demon Wolf Roar (S-WOL-11, learned from Deucalion): on the Alpha Roar (from its slot, its key or /pack roar), with a cooldown of its own (2 minutes), the other **player** Alphas within 32 blocks who are not in your pack are forced to shift, except where PvP is off, in spectator mode or on your own team. A Beta is not. |
| **R181** | Peter Hale waits in a burned Hale house (a one-piece jigsaw, `structure/hale_house.nbt`, in forests and taigas, spacing 40, `peter_house_enabled` and `peter_house_chance` read for each house). **Many houses, one Peter**: he is made when a player in the overworld stands within 24 blocks of a piece of any house and the index says he has not been seen (or was lost); every other house stays empty. He is a burned man until he has met a full moon, then a whole one; at half health, or on a full moon night with `peter_feral_full_moon` on, he shifts into the Alpha (a quadruped, 1.4 by 1.7) for good. As the wolf on a full moon night he is **feral**: he takes anything alive within 24 blocks that is not a boss or a player who cannot be hurt, through walls too. He pounces at a target 3 to 4.3 blocks away, **only while he can see it** (never through a wall or a floor), standing still while the animation plays and ending where its root bone does; the pounce lands only on a target he still sees. The player who calls him is told once, on the action bar and in chat, that the house is not empty (never his name). `/locate structure supernaturalbloodlines:hale_house` finds a house; its chest holds a book about the fire. Needs Werewolves. |
| **R182** | Peter's death pays once (`boss_reward_on`): the Memory Claws lie where he fell for anybody (never burn, never despawn), and his one Alpha spark goes through `SparkHandler.grantFromBoss` to **the player who did the most damage to him** (a tie to the last blow, a pet's damage its owner's, a mob's or the environment's nobody's), by the rules of a stolen spark: a True Alpha takes none, an open protected path destroys it, the stack never passes `max_stacked_sparks`, and a player who is no werewolf is told so. A mob's hit never counts as a white oak kill, and he is no Original, so no line is ever touched. `peter_respawn` (off) lets him be found again `peter_respawn_days` after he died. The reward is paid at the last priority of the death event, so anything that cancels the death (a totem, the Elixir) leaves him alive and unpaid. |
| **R184** | The Beast of Gévaudan's encounter: the Host, the Beast, the Spirit, the escape. The Beast collapses at a quarter of its health or inside a closed ring of mountain ash; the Spirit has `beast_spirit_seconds`; an escape returns it once at half health, a second one is for good; a Host is made twice in a world at most. |
| **R185** | The Host is invulnerable but to the void and `/kill`, and his death fails the night; only the Valet Pike hurts the Spirit, and a death of it without a pike blow is an escape. |
| **R186** | The Roar: after a 40-tick rear-up everyone in range is shaken (Weakness II and Darkness) except Alphas, True Alphas, creative and spectating players and the bosses; a hostile mob of no pack runs. |
| **R187** | The Beast heals 2 a second unless wolfsbane is on it, takes 0.6 of what shots do and a heart at most from a wolfsbane pulse, is stunned for a second at most by a Hearing Device, and cannot be taken below 1 health by blows. |
| **R188** | The Valet Pike is made by a witch type on a full moon night from the bottled blood of an NPC Original, a Hunter of the Five, an Original, a hybrid, a tribrid, an Alpha, or an advanced vampire or werewolf (rung 4 up); a plain player's is not enough. Every refusal is the same message at the same cost. It is never dropped. |
| **R189** | A blow of the Valet Pike on the Spirit ends the Beast for good; nothing else touches the Spirit. |
| **R190** | The Rite of the Frequency (a werewolf, a full moon night, the pike given back, a Hearing Device and an echo shard) brings the Host near the caster once, or makes the waiting Host change now; it needs Werewolves and the Beast switched on. |
| **R191** | The Mantle of the Beast: for a shifted werewolf type who wears it, every two minutes it weakens and slows the enemies within 6 blocks for 20 seconds; for anyone else it does nothing. It goes once to the player who did the most damage to the Beast over the whole encounter; no spark, no pike. |
| **R183** | The Memory Claws: right-click another **player** to be given a **snapshot** of their sire and genes as they know them at that moment. It is written into the user's own record (never the pack's) and shown on that player's Profile page; the target's later discoveries never reach it, and a second use takes a new snapshot. What it shows is what the discovery rules already show: the sire as the Bloodline page shows it (never an NPC Original or a creature) and the genes **that player has discovered themselves**; nothing is discovered by it and no line is named. The target must **agree on purpose**: sneak, with a lead in the main hand, **looking at the user** (the look within about 25 degrees of their eyes, with a clear line), where PvP is on, and the user must be allowed to hurt them (no teams). Where PvP is off the claws say so. The target is told on the action bar and in chat. Consent is asked before anything hidden is read, every refusal says the same words and costs nothing, a non-player is refused with its own line, and it never works on the user. 12 uses; a 30-second cooldown that the item keeps itself (the wait says the seconds left, whoever the target is). |
| **R171** | The True Cure is the same change as The Cure; an Original drinks it with no rite and their line is untouched. |
| **R124** | When an Original lies open to white oak (desiccated, in the dying state or downed), every online member of the line is warned once per opening. A member who has not discovered the Original is never told the name. |
| **R125** | **Rite of the Broken Line** (witch-type, lunar eclipse): frees only the target, who becomes the head of their own line (not an Original) and keeps their sire bond; the people they sired stay in the line. Wrong blood, a wrong Original and an open ring all give the same plain refusal, "The rite fails." |
| **R126** | **Rite of the Imprisoned Original** (harder, lunar eclipse): the Original must be brought down (downed, in the dying state or desiccated) and stay inside a closed mountain ash circle for the whole rite; it frees the target's whole branch, never the Original's whole line. |
| **R127** | `line_rites_enabled` switches both rites. A freed player's record says which rite freed them. |

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
