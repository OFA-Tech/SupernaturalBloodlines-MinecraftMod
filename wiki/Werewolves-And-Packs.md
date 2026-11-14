# Werewolves and packs

**Needs Werewolves**, apart from the Shocked effect and the Taser (any supernatural), and the sireline parts. Everything here is part `species` (plus `artifacts` for mountain ash, wolfsbane and the Moon Ring, and `species_tensura` for rank skills).

Werewolves (the mod) gives players the werewolf faction, forms and levels. This mod adds **lineage**, **ranks**, **packs**, **moon control** and **weaknesses** on top.

## Born and bitten

| | Born | Bitten |
|---|---|---|
| **How** | the hidden gene awakens, or the Rite of Lineage (Awaken) | an Alpha's bite, or a wild werewolf's bite |
| **Starts as** | Omega | Beta of the Alpha's pack (Alpha's bite), or Omega (wild bite) |
| **Full-moon forced shift** | spared on **half** of the nights | normal |
| **Pack bonus** | counts once | counts **twice** |
| **Tensura rank skill** | `godwolf_sense` at Werewolves level 5 (Tensura part) | none |

> The old plan said a born wolf also unlocks the four-legged form 5 levels sooner. **That is not possible**: Werewolves has no unlock for it that a player can reach. See [Known limits](Known-Limits).

## The hidden gene

- Every player rolls the **hidden werewolf gene** once, at first login (`latent_gene_chance`, default **15%**). It is secret.
- **Rite of Lineage (Reveal)** tells you if you have it ("The bones agree: you carry the werewolf gene." / "The bones are silent."). Small circle, 3000 power: Bone, Glowstone Dust, Paper.
- **Rite of Lineage (Awaken)** gives the gene, and awakens it (medium circle, 9000 power: Moonstone, 3 Bones). Who it works on:

| Target | Result |
|---|---|
| no faction | gene, **born** werewolf, **Omega**, joins the werewolves |
| vampire or Original | a **bound hybrid** or Original hybrid, with the gene |
| hunter | refused: "A hunter cannot take a werewolf's lineage." |
| werewolf | refused: "A werewolf already has their lineage; the rite cannot give another." |
| a heretic, or a Hunter of the Five | refused ("Your bloodline cannot take a werewolf's lineage this way.") |

- **A human with the gene who kills a villager or a player** becomes a born werewolf (an Omega). A vampire with the gene becomes a bound hybrid; a hunter's gene stays latent.

## Ranks

| Rank | How | Perks |
|---|---|---|
| **Omega** | no pack: a wild bite, leaving a pack, or losing the spark | **-10%** attack and movement speed |
| **Beta** | in an Alpha's pack | pack bonus and Regeneration when the pack is gathered |
| **Alpha** | the spark | **+20%** attack, **+4** max health, red eyes, the Roar |
| **True Alpha** | earned | the Alpha perks x1.1 (**+22%**, **+4.4**) |

Perks only apply while in the werewolf faction (a cured True Alpha keeps the rank, dormant: no attribute perks and no Tensura rank skills until they rejoin). During a lunar eclipse the positive perks stop; the Omega penalty does not.

### Eyes

Gold for Beta and Omega, **red** for Alpha and True Alpha, **blue** after killing an innocent (a villager that wasn't a wandering trader, or a player who didn't hit you in the last 10 seconds before dying). Blue eyes block True Alpha for good, and an Alpha with an innocent kill keeps red eyes.

Eyes show as two glowing dust particles at eye height every half second while a werewolf is in a wolf form. The render layer on the model is not done.

## Turning people (bites)

- **Only Alphas turn humans.** An Alpha's bite makes the victim a **Beta of their pack** (and records the Alpha as their sire) with `alpha_bite_death_chance` (default **10%**) of killing them instead ("... body rejected the Alpha's bite").
- A **Beta's or Omega's** bite does **not** infect.
- A **wild werewolf mob** makes an **Omega** (no pack, no death roll, no sire).
- A Beta bitten again by another Alpha, or invited by one, **stays in their pack** until `/pack leave`.
- If the biter lost the spark before the bite took, the bitten becomes an Omega with no pack (the sire is still recorded).
- Not every player can be bitten into a werewolf: vampires, hunters, Originals, heretics and hunters of the Five are refused.

## The Alpha spark

| How | What happens |
|---|---|
| **Stolen** | a werewolf player kills an **Alpha** (never a True Alpha: killing one is just a death): they take the spark and the pack (a Beta who kills their own Alpha leads it afterwards; a killer who already leads a pack adopts the Betas). The old Alpha becomes an **Omega**. Ordinary deaths keep rank and spark. |
| **Inherited** | an **Alpha** leaves the werewolves or is cured: the spark goes to the **highest-level online Beta**. With nobody online the pack **dissolves** and its Betas become Omegas at next login. |
| **Given up** | `/pack heal <player>`: an **Alpha** (not a True Alpha) within 4 blocks of a player gives the spark away to fully heal them and cure hybrid venom and wolfsbane (all strains). The Alpha stays in the pack as a Beta of the heir. |
| **Earned** | a Beta becomes a **True Alpha**, who keeps the spark for good |

### Becoming a True Alpha

Checked every minute (1200 ticks), for Betas in the werewolf faction:

- the **maximum Werewolves level**
- **30 in-game days** as a Beta
- **300 defender kills**: a hostile mob killed while it was targeting a villager or a pack mate
- **no innocent kill** (blue eyes)
- **never stole a spark**

There is **no limit** on True Alphas per world. When it happens: "Your eyes burn red: you have earned the spark. You are a True Alpha."

A True Alpha **keeps the spark**: it cannot be stolen, inherited or given up. If they are cured or leave the werewolves they stay True Alpha, dormant (no attack or health perk, no `/pack invite`, `heal` or `emissary`). Their pack dissolves as for any Alpha and is **not** restored. Rejoining the werewolves brings the perks back.

### Tensura race

With Tensura, a human who becomes a werewolf becomes a Tensura **beastfolk**. As an Alpha they become a **beast lord**, as a True Alpha a **spirit beast**, and the race is never lowered. Other races are kept. See [Tensura integration](Tensura-Integration). Switch: `werewolf_beastfolk_enabled`.

## Packs and `/pack`

| Command | Does |
|---|---|
| `/pack invite <player>` | an Alpha invites an online werewolf with no pack who isn't an Alpha. They get a clickable **[Accept]** that lasts 60 s. |
| `/pack accept <token>` | what the **[Accept]** button runs |
| `/pack leave` | leave your pack (becomes an Omega). An Alpha cannot leave. |
| `/pack info` | show your pack |
| `/pack heal <player>` | give up the spark to heal |
| `/pack roar` | the Alpha Roar |
| `/pack emissary [player]` | name your Emissary (no name clears it) |
| `/pack anchor [player]` | name your anchor (no name clears it) |

A werewolf with a pack already must leave it before another Alpha's bite or invitation can move them.

### Pack bonus

Every 5 seconds the game counts an **online Alpha's** Betas within **64 blocks** in the same dimension. With **3 or more**:

- the Alpha gets **+5% attack per Beta counted** (up to 5, so up to +25%); **bitten Betas count twice**
- those Betas get **Regeneration I**

The bonus stacks multiplicatively with the Alpha's +20% (1.20 x 1.25). An offline or distant Alpha keeps the pack but not the bonus. The eclipse suppresses it.

### Alpha Roar

Key bind **Alpha Roar** (unbound by default, category *Supernatural Bloodlines*) or `/pack roar`. **60 second** cooldown. Alpha and True Alpha only, not during an eclipse ("The eclipse swallows your roar.").

- **Pack Betas within 32 blocks:** Strength I and Speed I for 30 s, and may shift at will for those 30 s.
- **Other werewolves within 16 blocks:** drop their form and get Slowness II for 5 s ("X's roar forces your wolf down.").
- The roarer's eyes glow for 30 s.

### Emissary

A named **witch, siphoner, heretic or tribrid** pays **10% less** for rites while the Alpha or a Beta of the pack is within 16 blocks (after their own x0.75). Their mountain ash lines are the Emissary's: they also stop mobs in the `#supernaturalbloodlines:supernatural_mobs` tag (only the Vampirism ghost). A witch can be the Emissary of **one** Alpha only.

### Anchors

An **anchor** halves the forced full-moon shift. It is either:

- a player the wolf named with `/pack anchor <player>` and who is within 16 blocks (the main path), or
- an item of the `#supernaturalbloodlines:anchor` tag held or worn (ships with the Recovery Compass, Clock and Compass).

A non-born anchored wolf is spared on half of the nights. A **born** wolf with an anchor gets half moon control instead (below).

## Moon control

Config `moon_bound_werewolves` (default **on**). **Off:** everyone shifts at will.

On, a werewolf transforms **only when the full moon forces it** and cannot shift at will, unless they have **moon control**:

| Source | Control |
|---|---|
| **Moon Ring** bound to you | **full** |
| **Alpha** or **True Alpha** | **full** |
| **Hybrid**, **Original hybrid**, **tribrid** (unbound) | **full** |
| **Born** werewolf with an **anchor** | **half** |

- **Half control:** human and survivalist forms at will.
- **Full control:** also the beast forms, but only from Werewolves level `full_form_level` (default **8**); below it, half.
- Control also **skips the forced full-moon shift**. Without control you see "You cannot shift at will: only the full moon moves your wolf."
- The roar gives a short permit to shift.
- Switching a form **off** is never blocked.
- With the **artifacts part off**, a bitten (non-Alpha) wolf has no way to shift at will: only the full moon moves it (no Moon Ring).

## Weaknesses

### Wolfsbane

Eaten, brewed or coated on a weapon. Only affects werewolf types (werewolf faction, hybrids, Original hybrids, tribrids). Sources and recipes: [Items and blocks](Items-And-Blocks#wolfsbane-and-wolf-lichen).

| Strain | Effect |
|---|---|
| **Common** (purple; Werewolves' and Enchanted's plant) | Weakness I and Nausea, 30 s |
| **Yellow** (uncommon) | 1 damage every 2 s for 60 s, never kills |
| **Northern blue** (rare) | 1 damage every 5 s, **dies after 5 minutes** unless cured with **ash of the same strain** |
| **Wolf lichen** | no natural regeneration and no form changes (except back to human) for 60 s |

### Silver

The Werewolves silver items. A [Silver Poppet](Enchanted-Integration#poppets) protects once against a lethal silver hit or bite.

### Mountain ash

Lines of dust stop vampires, werewolves, hybrids, Originals, heretics and tribrids. Witches pass. See [Items and blocks](Items-And-Blocks#mountain-ash).

### Lunar eclipse

Each full-moon night (the Werewolves full-moon window, 12786 to 23216 of the day) has a **3%** chance (`lunar_eclipse_chance`, rolled once per night when someone is online) of being an **eclipse**, until dawn.

- The server announces the start and the end: "A lunar eclipse darkens the moon. The wolves lose their strength until dawn."
- The overworld's **fog and horizon turn red**. (The zenith colour is not changed: NeoForge 1.21.1 has no event for it.)
- Werewolves lose **every form action** (the full moon's forced shift too, and a form already on is dropped within a second): "The eclipse holds your wolf down until dawn."
- The Alpha perks (the Omega penalty stays) and the **pack bonus** are suppressed. Nobody can roar.
- It is one flag for the **whole server**, whatever the dimension. It is stored as one night number and is never active past dawn, so a restart cannot make it last.
- Operators: `/supernaturalbloodlines debug eclipse start|stop`.

### Electricity

- A **lightning strike** on a supernatural, or within **3 blocks** of one, gives **Shocked** for 30 s: Slowness I, **no natural regeneration**, **no Werewolves form actions** (a form already on is dropped).
- The **Taser** (durability 16, repaired with redstone in an anvil; crafted from a lightning rod over redstone over an iron ingot) shocks a supernatural it hits. **Every hit spends a charge**, even on a non-supernatural.
- A visual-only lightning bolt shocks nobody.
- Shocked works without Werewolves; only the form lock needs it. A **vampire's own blood regeneration is not blocked** (Shocked only slows them). A mod that heals through the food bar is blocked too.

## Tensura skills by rank

See [Tensura integration](Tensura-Integration#werewolf-rank-skills): Alpha and True Alpha get Tensura skills; born wolves of level 5 get `godwolf_sense`.

## Design points the owner may still change

- **Farming defender kills:** the 300 defender kills count any hostile mob that targeted a villager or a pack mate, so a player can farm them at a mob spawner beside a villager. Possible fixes: a daily cap, or ignoring spawner mobs.
- **A bitten wolf with the artifacts part off** has no way to shift at will.
- The Taser spends a charge on any hit.

See [Known limits](Known-Limits) for the rest.
