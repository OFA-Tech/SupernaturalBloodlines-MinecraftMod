# Species rules (source of truth; the owner patches these by id)

Round 3 (2026-11-11): R3, R20, R32, R35, R42, R81–R84, R91, R95, R96, R100 and R101 were patched. R20a (2026-11-12) and R20b (2026-11-14) were added. Where the design or a plan disagrees, this file wins.

## General
- **R1.** Only players whose Tensura race is tagged `human_like` can take a show species or become one of the Five. Without Tensura, everyone qualifies.
- **R2.** Hybrid-types (hybrid, Original hybrid, tribrid) are in the Werewolves faction with a vampire layer. For every rule in this mod they count as **both** vampire and werewolf.
- **R3.** Species come only from rites, items and the events below. **The Tensura race is never replaced by a species** (R100). Species *unlock* apex evolutions that the player picks in Tensura's own evolution screen.
- **R4.** Ordinary deaths keep everything (species, rank, mark, sire), except where a rule below says otherwise.
- **R5.** No show names.

## Humans
- **R10.** The hidden werewolf gene: rolled once at first login (15%) and kept secret. The *Rite of Lineage (reveal)* shows it.
- **R11.** A human with the gene who kills a villager or a player becomes a **born werewolf** (Omega).
- **R12.** The *Rite of Lineage (awaken)* gives the gene (and awakens it for humans). Anyone with no faction, or a vampire or Original (see R24, R35), can be the target.
- **R13.** Resurrection Ring: 3 charges, and only against supernatural killers.

## Vampires
- **R20.** You become one through Vampirism's normal infection. **The Tensura race is kept**: you get the vampire layer, and Tensura is told you're vampiric (see R100). Tensura's own vampire line (ghoul → vampire → overcomer/lord → divine) still joins the Vampirism vampire faction with a level floor (vampire 1, overcomer/lord 7, divine max). A cure resets the race to human **only** if the race is in Tensura's vampire line.
- **R20a (2026-11-12).** When a player with no bloodline species joins the Vampirism vampire faction and their Tensura race is **exactly** `tensura:human`, the race is set to `tensura:vampire`, automatically. Every other race is kept, as R20 says (an elf stays an elf). Not configurable.
- **R20b (2026-11-14).** Werewolves follow Tensura's Beastfolk line (beastfolk, beast_lord, spirit_beast, divine_beast, in the order of Tensura's own evolutions). A player with no bloodline species whose Tensura race is **exactly** `tensura:human` becomes `tensura:beastfolk` when they become a werewolf. A werewolf already on the line becomes `tensura:beast_lord` when they are an Alpha and `tensura:spirit_beast` when they are a True Alpha (also when they rejoin as one). The race is **never lowered**, `tensura:divine_beast` stays a normal Tensura evolution and every other race (an elf, a vampire-raced hybrid) is kept. Existing werewolves are not changed: only a join or a rank change after the update. **Rulings:** a True Alpha who rejoins as a `tensura:human` goes straight to `tensura:spirit_beast` (human to beastfolk to spirit beast in one step). A player with a bloodline species keeps their race, so a **witch or siphoner werewolf on the human race stays human** (the species layer owns their race). A vampire who becomes a werewolf is cured to human by the vampire sync first, and so ends on the Beastfolk line (`VampireSync` is registered before `WerewolfRaceSync`). Config `werewolf_beastfolk_enabled` (default on); off leaves every race alone.
- **R20c (2026-11-14, plan 11).** **The turning, the show's way.** Vampire blood in the system, then death, then human blood. Drinking vampire blood (a Ghoul biting a vampire, or an item in `#supernaturalbloodlines:vampire_blood`, by default `vampirism:vampire_blood_bottle`) marks the player for `vampire_blood_ticks` (default 24,000, `supernaturalbloodlines-species.toml`). Dying while marked puts them in the **turning** for `transition_ticks` (24,000 by default, the same setting as the hybrid transition) of online time: they respawn as they were and can drink. Drinking **human-like blood** in that time completes it: they join the Vampirism vampire faction at level 1 (the species rules do the rest, R61/R62), and a Ghoul (R20e) simply stops being one. Nothing is taken from them. Only someone in no faction, or a Ghoul, can be marked or turned: an existing vampire, werewolf or hunter is untouched. The only settings are the two timers.
- **R20d (2026-11-14, plan 11).** **Failing the turning, and where blood comes from.** If the clock runs out first, the player dies of `failed_transition` ("body could not bear the change", it bypasses a Totem of Undying) and comes back as they were: a human stays human, a Ghoul stays a Ghoul. Expired vampire blood, or a death with none, is a normal death. The hybrid transition (R40) wins: vampire blood does nothing to a player already in it, and a second turning never starts over a running one. **Human-like blood** is a villager or wandering trader, a Vampirism hunter, a player whose race is human-like (R1; witches and hunters count), or a Vampirism blood bottle. **Never** human blood: anything in the vampire faction (so a converted villager, and every vampire), a player of the vampire side by species (Original, Original hybrid, hybrid, tribrid, heretic), and a player in the werewolf faction. These exclusions hold with or without Tensura; Tensura only adds its human-like race list. Vampirism only lets a vampire bite, so a player who is not yet a Ghoul drinks the bottle (the click is taken over, like the hybrid transition's). Vampire blood, animals and other mobs do not feed the turning. If the player can no longer turn when it ends (they joined the hunters or the werewolves, or the vampires some other way during the turning), it **ends quietly**: no join, no kill, and the bottle is not taken over. If Vampirism or another mod refuses the join, the player is told (`turning.join_refused`), the turning is over and the bottle stays used up. A failed turning also says so in chat (`turning.failed`).
- **R20e (2026-11-14, plan 11).** **Ghouls.** Any Sanguinare that finishes (a bite, the Vampire Fang, a MedChair or a Totem) joins the Vampirism vampire faction at level 1 as before, but the player is also a **Ghoul** (`BloodlineState.ghoul`): they have the level 1 vampire (sun, thirst, weak to vampire weapons) and nothing more. They **cannot level up** (Vampirism's `FactionLevelChangePre` is cancelled for a new level above 1, which also stops the altars, `setFactionLevel` and the commands), the **Altar of Infusion and the Altar of Inspiration refuse the click** (no item is used), and each refusal says so in chat (`turning.ghoul_no_level`, `turning.ghoul_no_altar`). Vampirism's skill points cannot be lowered by a provider, so a Ghoul has the points of level 1 (the cap does the work). Only a Sanguinare that finishes **after** this update counts, only for a player in no faction at the moment of the join, and never for a vampire species: **existing vampires are not Ghouls**. A Ghoul stops being one only through the turning (R20c), or by leaving the vampire faction (a cure, a rite, a command): leaving also ends the turning that went with it (the clock and any vampire blood), because a cured Ghoul is fully human again. Nothing earned is taken. **With Tensura (species_tensura):** a Ghoul whose race is exactly `tensura:human` shows `tensura:ghoul`, and `tensura:vampire` once the turning is done (a Ghoul who leaves the vampires goes back to human, as a cure does, with or without vampirism_tensura); every other race is kept. **No Tensura evolution** goes through while a Ghoul (ManasCore's `SET_RACE` is refused for an evolution, with `turning.ghoul_no_evolve`); EP keeps accruing. Plain race changes (an admin command, a cure) are not evolutions and are not refused.
- **R20f (2026-11-14, plan 11).** **Biting a vampire** (a player or a mob) never fills the biter and costs them `bite_vampire_cost` of their own blood (default 1, 0 = free; `supernaturalbloodlines-species.toml`). This holds for every vampire, not only Ghouls. A later skill (plan 12) will lift it.
- **R21.** Sun damage comes only from Vampirism. A Sun Ring bound to the wearer blocks it.
- **R22.** Cured by the *Rite of Cleansing Blood* or The Cure.
- **R23.** From level 10, can compel villagers and lesser vampire mobs.
- **R24.** **Hybrid-awakening rule:** a vampire with the gene who kills a villager or a player becomes a **hybrid**, with their sire kept and their werewolf side **bound** (no wolf forms) until the *Rite of the Moon*. The Vampirism level carries over (capped) when the base faction changes.
- **R25.** A vampire who kills one of the Five gets the hunter's curse.

## Originals
- **R30.** *Rite of Immortality*: a human-like target with no faction, the night of a full moon (plan 10: noon of the full-moon day is refused), 12,000 power, a villager sacrificed, and a world cap (unlimited by default).
- **R31.** Vampire faction, +20% attack, +4 max health. Can make Original's Blood.
- **R32.** **Non-white-oak lethal damage** puts the Original into **Vampirism's own "dying" state**, with its give-up / wait screen (or our own copy of it if Vampirism has no hook). **A white oak hit kills instantly** and skips that state. Void and `/kill` still kill.
- **R33.** The White Oak Ash Dagger desiccates an Original until someone pulls it out, or for up to 30 minutes. It hurts any non-human who uses it.
- **R34.** Only The Cure works on Originals.
- **R35.** An **Original hybrid needs the gene**: either a natural gene awakened by a kill (R24 path, bound), or a gene given by the *Rite of Lineage (awaken)* on the Original, which makes them a bound Original hybrid. The *Rite of the Moon* is refused without the gene. On a bound Original hybrid, it breaks the curse. **The Rite of the Moon and the Rite of the Silver Moon, like the Rite of Immortality, need the night of a full moon (plan 10): noon of the full-moon day is refused.**
- **R36.** Sireline death is a config switch, off by default.

## Hybrids
- **R40.** A werewolf who dies with Original's Blood active starts a transition, 24,000 ticks of play time:
  - Doppelgänger blood makes them a hybrid, with that Original as their sire.
  - Any other blood makes them unstable, then they die and stay a werewolf.
  - If the timer runs out, they die and stay a werewolf.
- **R41.** R24 also makes a hybrid.
- **R42.** **Traits:**
  - speed and attack grow with age
  - blood thirst from 0 to 20: Weakness at 0, Strength at 15 or more
  - they shift form at will once unbound (R96)
- **R42v.** **Venom:**
  - **Werewolf, hybrid, Original hybrid and tribrid bites and attacks poison vampires.**
  - It's lethal unless cured: 1 damage every 40 ticks, plus hallucinations, and it kills a **non-Original** vampire after 12,000 ticks.
  - Originals only get Weakness II for that time.
  - **Only Hybrid's Blood cures it.** A rite makes it from a hybrid, Original hybrid or tribrid. Original's Blood does not cure it.
- **R43.** A hybrid can't hurt their sire. The sire can use `/bloodline call`.
- **R44.** Cleansing Blood turns them into a plain werewolf. The Cure turns them human.
- **R45.** A second dose of Original's Blood does nothing.

## Original hybrids
- **R50.** They follow the Original rules (R31–R34) and the hybrid rules (R42, R42v). Werewolves faction.

## Witches, siphoners, heretics
- **R60.** *Rite of Ancestral Calling*: 75% witch, 25% siphoner. Only for players with no faction or the werewolf faction.
- **R61.** Witch: altar range ×1.5 and rite cost ×0.75. Joining the vampires removes their magic.
- **R62.** Siphoner: drains up to 1,000 power from blocks, mobs and players. Joining the vampires makes them a heretic. A siphoner can't start the hybrid transition.
- **R63.** Heretic: vampire faction, keeps siphoning.
- **R64.** A witch-werewolf who completes the hybrid transition becomes a tribrid.
- **R65.** Siphoned power resets on death.

## Tribrids
- **R70.** Hybrid traits + witch bonuses.
- **R71.** Cleansing Blood turns them into a witch-werewolf. The Cure turns them human.

## Hunters of the Five (supernatural hunters)
- **R80.** *Rite of the Five*: 10,000 power, only for human-like Vampirism hunters, while fewer than 5 marked hunters exist (config).
- **R81.** **Prey** means vampires, werewolves, hybrids, Originals, heretics and tribrids. **Witches and siphoners are not prey**: they're nature's side.
  - The mark grows with each prey kill: +1% attack and +0.5% speed per kill, capped at +20% / +10%.
  - From 5 kills, nearby prey glows, visible only to that hunter.
  - Marked hunters are immune to compulsion.
- **R82.** A marked hunter killed by **prey** loses the mark. A random online eligible hunter becomes a potential, whose first prey kill makes them one of the Five.
- **R83.** **Hunter's curse:** any **prey** killer of a marked hunter (vampire *or* werewolf types) hallucinates until a potential activates. Humans and witches are exempt, and it doesn't stack.
- **R84.** Instinct: after 3 in-game days without a prey kill, Weakness until the next one.

## Werewolves (Teen Wolf layer)
- **R90.** **Born** (R11, R12, R24): half the full-moon forced-shift chance, and the four-legged form unlocks 5 levels sooner. **Bitten:** counts double for the pack bonus.
- **R91.** **Ranks:**
  - **Omega:** no pack, −10% attack and speed.
  - **Beta:** in a pack.
  - **Alpha:** red eyes, +20% attack, +4 max health, the Roar.
  - **True Alpha:** **no per-world limit, but very hard to earn**: max Werewolves level, 30 in-game days as a Beta, 300 defender kills, never killed an innocent, never stole a spark. Alpha perks +10%. **A True Alpha keeps the spark (2026-11-14):** it cannot be stolen, inherited or given up. Cured or leaving the werewolves, they stay True Alpha, dormant (no perks, no pack work) until they rejoin; their pack dissolves as for any Alpha and is not restored. **Cures too (plan 10 review):** the Cure and the Silver Moon (plain werewolf and hybrid) keep a True Alpha's rank, lineage and tracked rank skills (`clearedWolf`); every other rank is wiped as before. A cured plain Alpha now passes the spark or dissolves the pack like the leave path (the pack is handled when the wolf layer is cleared, before the faction is left). `SPARK_GIVEN_UP` is refused for a True Alpha, like `SPARK_LOST`.
  - Betas and Omegas have gold eyes. An innocent kill turns them blue for good.
- **R92.** Only Alphas turn humans, with a 10% chance the bite kills instead. Wild werewolf mobs make Omegas. A human turned by an Alpha joins that Alpha's pack and sireline.
- **R93.** **The spark:**
  - **Stolen** by killing the Alpha. Only a plain Alpha: killing a True Alpha is just a death.
  - **Inherited** by the strongest Beta when the Alpha leaves or is cured. Only a plain Alpha: a True Alpha who leaves just dissolves the pack.
  - **Given up** to heal someone. Not by a True Alpha.
  - **Earned:** see R91.
- **R94.** **Pack:**
  - With 3 or more Betas near their online Alpha: the Alpha gets +5% attack per Beta (up to +25%), and the Betas get Regeneration.
  - **Roar:** buffs the pack and forces other werewolves out of wolf form.
  - **Emissary:** a witch whose rites cost 10% less near the pack.
  - **Anchor:** an item or a nearby player halves the forced full-moon shift.
- **R95.** **Weaknesses:**
  - **Wolfsbane strains:**
    - **common** (purple: the existing Werewolves and Enchanted plant): Weakness and Nausea
    - **yellow** (uncommon): damage over time
    - **northern blue** (rare): lethal in 6,000 ticks unless cured with **ash of the same strain**
    - **wolf lichen**: no healing and no transforming for 1,200 ticks
  - Silver.
  - **Mountain ash** lines block **vampires, werewolves, hybrids, Originals, heretics and tribrids. Witches pass.**
  - Lunar eclipse: 3% of full moons, no powers until dawn.
  - Electricity: lightning or a Taser stops forms and healing for 30 s.
- **R96.** **Moon control** (config `moon_bound_werewolves`, default on):
  - **Without control**, a werewolf transforms **only when the full moon forces it** and can't shift at will.
  - **Control comes from:**
    - a bound **Moon Ring**: half transformations (human and survivalist forms) at will, plus full forms (beast, four-legged beast) from Werewolves level 8 (config)
    - **hybrids, Original hybrids and tribrids** (once unbound): as with the Moon Ring
    - **Alphas and True Alphas**: as with the Moon Ring
    - **born werewolves with a bound anchor**: half transformations only
  - The Moon Ring and the exemptions also stop the forced full-moon shift.

## Tensura links
- **R100.** **The race is kept and species are a layer on it.** Each species:
  - grants Tensura skills, attributes and an EP or magicule bonus
  - makes Tensura treat the player correctly: vampire types count as undead for holy damage and sunlight checks, and bloodless rules apply to Originals
  - speeds up the player's EP gain by 25% (config `evolution_progress_multiplier`, plan 10), through Tensura's own `AURA_GAIN` and `MAGICULE_GAIN` attributes. It never changes an evolution's requirements or progress: a boss, an item or an awakening can't be skipped
  - **unlocks one apex evolution** that human-like races can pick in Tensura's evolution screen once they reach a high stage of their own line:

| Species | Apex race (working names) |
|---|---|
| Original | `primordial_vampire` |
| Original hybrid | `primordial_hybrid` |
| hybrid | `moonblood_hybrid` |
| witch | `ancestral_witch` |
| siphoner / heretic | `void_siphon` |
| tribrid | `tribrid_ascendant` |
| marked hunter (one of the Five) | `eternal_hunter` |

  Apex races keep the `human_like` tag (and `undead` for vampire types), so every other rule still applies. Losing the species doesn't remove a chosen apex race, but its species-only skills go dormant.
  - **Apex cost (plan 10).** An apex race needs 2,000,000 EP, the same as a divine human.
  - **Apex races are bitable (plan 10).** No apex race is bloodless. All seven carry our race tag `supernaturalbloodlines:bitable`, and every bloodless check of this mod skips it: the vampire bite block (`FactionRaceRules.isBloodless`) and the bloodless part of the R103 werewolf-join check. The vampire-type ones stay `tensura:undead`, so holy damage and the other undead rules still apply. Tensura's own `RaceUtils.isBloodless` is answered false for them by a second injection in the existing species_tensura `RaceUtilsMixin`, so Tensura's drain, blood mist and VampireRace feeding reach apex races too.
- **R101.** A sire becomes their sired's Tensura permanent owner. **The sire can name their sired through Tensura's own naming menu** (players too, even with the server's player-naming setting off, but only for their own sired). It costs magicules and gives Tensura's naming rewards (EP, and an evolution step for mobs).
- **R101b (owner rite).** The **Rite of Severance** breaks the whole sire bond of a sired player. The sired is freed: no Tensura owner link, no sire naming exception, they can hurt their sire (R43 ends), they cannot be called with `/bloodline call`, and the sire's compelled creatures no longer spare them. **The sire stays recorded:** the family tree still shows it and the sireline death still applies (blood, not bond). A new sire replaces the old one and ends the severance. Performer: a witch-type (witch, siphoner, heretic or tribrid) at an Enchanted altar with the sired standing in the circle. Cost: full moon night only, 6000 altar power, Sire's Blood of the sired's own sire (a bottle of blood taken with a silver blade while holding a glass bottle in the off hand, bound to the one hit) and a rare reagent (`enchanted:spirit_of_otherwhere`). Siring itself needs no consent.
- **R102.** Werewolf ranks grant Tensura skills, and the race is kept.
- **R103.** Hunters and werewolves can't be undead or daemon races. Apex vampire-type races are an exception for vampire-type species only. A race with no blood (`tensura:no_blood`) can't be a werewolf either, unless it is bitable (an apex race, see R100).
