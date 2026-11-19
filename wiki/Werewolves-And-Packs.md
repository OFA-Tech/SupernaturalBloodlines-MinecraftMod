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

### Genes

There are two hidden genes, and a player can carry both.

| Gene | Chance | Rolled | Does |
|---|---|---|---|
| werewolf | `latent_gene_chance`, 15% | once, at first login | can awaken (above) |
| doppelgänger | `doppelganger_gene_chance`, 5% | once, at the first login after it exists (so players from before the update are rolled on their next login) | nothing by itself. A [Vampire Compass](Items-And-Blocks) in its second mode and the [Rite of Doppelgänger Blood](Rites#species-and-cures) look for it. |

- **Unknown until discovered.** With `gene_visibility = discovered` (the default) a player sees each of their genes as *Unknown*. The werewolf gene is discovered by the Rite of Lineage (Reveal), by awakening, or by the [Rite of Revelation](Rites#rite-of-revelation); the doppelgänger gene by the Rite of Revelation or a successful Rite of Doppelgänger Blood with their taglock. `always` shows a player their own genes. Nobody ever sees another player's genes, and the server does not send an undiscovered gene to the client at all.
- **Changing `gene_visibility`** takes effect the next time the player's bloodline is synced (at login, or when it changes).
- A player who was already an awakened born werewolf (or a bound hybrid made by the gene) before this update counts as having discovered the werewolf gene.
- Operators can set the doppelgänger gene with `/bloodline gene doppelganger <player> <true|false>` ([Commands](Commands)).

## Ranks

| Rank | How | Perks |
|---|---|---|
| **Omega** | no pack: a wild bite, leaving a pack, or losing the spark | **-10%** attack and movement speed |
| **Beta** | in an Alpha's pack | pack bonus and Regeneration when the pack is gathered |
| **Alpha** | the spark | **+20%** attack, **+4** max health, red eyes, the Roar |
| **True Alpha** | earned the hard way | the Alpha perks x1.1, and **full sparks by nature** (see [Two paths](#two-paths-alpha-and-true-alpha)): **+30.8%** attack and **+6.16** max health at the default cap of 5 |

Perks only apply while in the werewolf faction (a cured True Alpha keeps the rank, dormant: no attribute perks and no Tensura rank skills until they rejoin). During a lunar eclipse the positive perks stop; the Omega penalty does not.

### Eyes

Gold for Beta and Omega, **red** for Alpha and True Alpha, **blue** after killing an innocent (a villager that wasn't a wandering trader, or a player who didn't hit you in the last 10 seconds before dying). Blue eyes block True Alpha for good, and an Alpha with an innocent kill keeps red eyes.

Eyes show as two glowing dust particles at eye height every half second while a werewolf is in a wolf form. The render layer on the model is not done.

## Turning people (bites)

- **Only Alphas turn humans** (config `only_alphas_turn`, default on; off gives Werewolves' own rule where any werewolf's bite infects). An Alpha's bite makes the victim a **Beta of their pack** (and records the Alpha as their sire) with `alpha_bite_death_chance` (default **10%**) of killing them instead ("... body rejected the Alpha's bite").
- A **Beta's or Omega's** bite does **not** infect.
- A **wild werewolf mob** makes an **Omega** (no pack, no death roll, no sire).
- A Beta bitten again by another Alpha, or invited by one, **stays in their pack** until `/pack leave`.
- If the biter lost the spark before the bite took, the bitten becomes an Omega with no pack (the sire is still recorded).
- Not every player can be bitten into a werewolf: vampires, hunters, Originals, heretics and hunters of the Five are refused.

## The Alpha spark

| How | What happens |
|---|---|
| **Stolen** | a werewolf kills an **Alpha from outside their pack** (never a True Alpha's, see below, and **a True Alpha never steals**: the Alpha they kill keeps the spark): they take **every spark the victim had** and the victim becomes an **Omega** (the [stacked sparks](#stacked-sparks) of the Deucalion factor). An Alpha who steals **keeps their own spark and gains the victim's on top**; a werewolf who was no Alpha becomes one. They take the spark and **not the pack**: the strongest other Alpha leads it, and with none it dissolves. **A kill inside the pack, outside a duel, changes nothing at all**: no spark, no lead, no rank, no blue eyes and no gene awakening, whoever kills whom (Beta, second Alpha or leader). **Leaving the pack just before the kill gains nothing**: someone who left it **by choice** (with `/pack leave`, or by leaving its party) in the last `left_pack_grace_minutes` (10) still counts as a member. A kick gets no such grace. **If the killer is a Beta or Omega whose path to True Alpha is still open and who protects it** (the default, see [Two paths](#two-paths-alpha-and-true-alpha)), the spark is **destroyed instead**: they get nothing and keep the path. Ordinary deaths (a mob, a fall, lava, a player who is no werewolf) keep rank and spark; **an ordinary death of the leader keeps them leading**. |
| **Inherited** | the leader leaves the werewolves or is cured: the pack goes to the **strongest second Alpha** first (online or not); with none, the spark goes to the **highest-level online Beta** (even one who protects an open True Alpha path: protection covers kills only, and the spark closes their path); with nobody left the pack **dissolves** and its members are told (now, or at their next login). A second Alpha who is cured stays in the pack as a Packmate. |
| **Given up** | `/pack heal <player>`: an **Alpha** (not a True Alpha) within 4 blocks of a player gives the spark up to fully heal them and cure hybrid venom and wolfsbane (all strains). **The spark is destroyed**: nobody inherits it and the healed player stays what they were (a Beta stays a Beta). The Alpha becomes a Beta of the pack (an Omega if the pack dissolves). If they led, the strongest other Alpha leads the pack; with no other Alpha it dissolves. The healer held a spark, so their path to True Alpha is closed. |
| **Earned** | a Beta becomes a **True Alpha**, who keeps the spark for good |

### Two paths: Alpha and True Alpha

**Alpha and True Alpha are exclusive.** Anyone who has **ever held a normal spark can never become a True Alpha**, however they got it: stolen, inherited, won in the Alpha Trial, or any other road. A True Alpha is earned only as a Beta or Omega who never held one. Whoever is an Alpha when a world is updated counts as having held a spark, and so does an ex-Alpha the pack index still has a tombstone for (marked at their next login); True Alphas are unchanged. **Limit:** an ex-Alpha who lost the spark before this version and has no tombstone in the pack index is not caught: they count as never having held one, and the history cannot be recovered because it was never recorded.

**The warning.** Every Beta or Omega whose path is still open is to see "**Taking a spark ends your path to True Alpha for good**" before an action that would give them a normal spark: that is the Alpha Trial, whose dialog arrives with the trial itself (plan 15; the dialog and the server check are ready). The safe button, "**Keep my path**", is focused first. A kill never asks. A pack inheritance or a duel or absence hand-over needs no dialog: duels and absences only move the lead between Alphas who already hold a spark, and an inheritance gives the spark to the strongest Beta even if they protect their path (it closes the path; protection covers kills only).

**Protect my True Alpha path.** A kill cannot ask first, so each player has the setting **Protect my True Alpha path** (on by default while the path is open): a kill that would steal a spark destroys it instead. It covers kills only: a pack inheritance still gives the spark to the strongest Beta, and that closes their path. Turn it off on your own Profile ("Stop protecting my path") or in Settings; both ask first, and the typed command is `/pack protect off confirm`. A protected kill of someone who was not fighting you still turns your eyes blue, which closes the path on its own, and says so. It does nothing once the path is closed (a held spark, or blue eyes).

### Apex fallback

An **Alpha Primeval** who loses the Alpha spark (stolen, given away, cured), or a True Alpha who goes dormant, goes back to the race they had before Alpha Primeval (and is not offered Alpha Primeval again until they hold a spark) (it is remembered when they evolve; a player from before this version goes to Spirit Beast). The apex skills stay locked until they hold a spark again (or the True Alpha is active again): then the race and its skills come back. A wolf who never held a spark, and one who evolved on by their own choice, are left alone. The True Alpha's full-spark bonus **stacks** with the Alpha Primeval x1.5.

### Becoming a True Alpha

Checked every minute (1200 ticks), for Betas in the werewolf faction:

- the **maximum Werewolves level**
- **30 in-game days** as a Beta
- **300 defender kills**: a hostile mob killed while it was targeting a villager or a pack mate
- **no innocent kill** (blue eyes)
- **never held a spark** (stolen, inherited, won in the Alpha Trial or any other road; see [Two paths](#two-paths-alpha-and-true-alpha))

There is **no limit** on True Alphas per world. When it happens: "Your eyes burn red: you have earned the spark. You are a True Alpha."

A True Alpha **keeps the spark**: it cannot be stolen, inherited or given up. If they are cured or leave the werewolves they stay True Alpha, dormant (no attack or health perk, no `/pack invite`, `heal` or `emissary`). Their pack goes to their strongest **second Alpha** (and dissolves only when they have none), and is **not** restored when they rejoin. Rejoining the werewolves brings the perks back. The Cure and the Rite of the Silver Moon do not take the True Alpha rank either. A plain Alpha who is cured passes the spark on like one who leaves.

### Tensura race

With Tensura, a human who becomes a werewolf becomes a Tensura **beastfolk**. As an Alpha they become a **beast lord**, as a True Alpha a **spirit beast**, and the race is never lowered. Other races are kept. **A cure (or any other way out of the werewolves) puts back the race this mod replaced** (a human is human again), but only while the race is still the one this mod set: a race you evolved to yourself stays. See [Tensura integration](Tensura-Integration). Switch: `werewolf_beastfolk_enabled`.

## Packs and `/pack`

| Command | Does |
|---|---|
| `/pack invite <player>` | the leader invites an online player who is in no pack. They get a clickable **[Accept]** that lasts 60 s. |
| `/pack kick <player>` | the leader removes a member (online or not) |
| `/pack ally invite <player>` / `accept <token>` / `remove <player>` | packs ally through their leaders |
| `/pack accept <token>` | what the **[Accept]** button runs |
| `/pack leave` | leave your pack (a Beta becomes an Omega). The leader cannot leave. |
| `/pack info` | show your pack |
| `/pack heal <player>` | give up the spark to heal (it is destroyed) |
| `/pack protect on\|off` | protect your path to True Alpha (hub: Profile, Settings) |
| `/pack roar` | the Alpha Roar |
| `/pack emissary [player]` | name a member of your pack (a witch, siphoner, heretic or tribrid) as your Emissary (no name clears it) |
| `/pack anchor [player]` | name your anchor (no name clears it) |

A werewolf with a pack already must leave it before another Alpha's bite or invitation can move them.

### Mixed packs and Alpha Packs

- **Anyone can join a pack.** A player who is not a werewolf joins as a **Packmate**: nothing about them changes, and they have every perk of the pack (the pack bonus counts them, the Roar buffs them, the Emissary's discount reaches them).
- **There is no limit** on the size of a pack.
- **A pack can have several Alphas, and one leader.** An Alpha who accepts another Alpha's invitation keeps the spark and becomes a **second Alpha** of that pack. **A joining Alpha never takes over**, however strong: the sitting leader stays leader, and a level-up or a rank does not move the lead. Only the leader invites and kicks.
- **Who leads next.** The **strongest Alpha** (a True Alpha before a plain Alpha, then the higher Werewolves level, and on a tie whoever was in the pack first) is only **the successor**: it leads when the leader leaves the werewolves, is cured, loses the spark or gives it up.
- **Leadership duel.** Otherwise the lead changes hands in a duel: a second Alpha uses `/pack challenge <leader>` (or the **Challenge <leader>** action on the leader's row in the hub, after a confirmation), and the leader answers with `/pack challenge accept` or `/pack challenge decline` (the chat message has an **[Accept]** button, and the hub shows **Accept the challenge** and **Decline** to the leader). Both must be in the same dimension and within **64 blocks** when it is asked and when it is accepted, and an accepted duel is **called off, and the pair barred for 10 minutes, if one of them walks out of that range or into another dimension**. They fight to the first defeat: a blow of the other's that kills them is the defeat, and so is a death by **lava, a fall, a mob or a pet only if the other had hit them in the last 10 seconds of the duel** (the server keeps the tick of the blow itself). **If they die of something else** (nobody had hit them, or long ago) **or fall into the void, the duel is off and the death stands.** A defeat is turned into **1 HP**; after one by the world they also get 5 seconds of Fire Resistance and, standing in lava, are moved to the nearest spot they can see from where they stand (never through a wall). For 5 seconds the winner cannot hurt the loser. **The winner leads the pack. Nobody loses the spark** and it is no steal. **Losing the connection while the duel is on starts a wait, not a defeat**: the duel is paused (its 2 minutes stop), the other one is told "%s disconnected. They have 60 seconds to return before they forfeit", and if the duelist is back in time both are told and it goes on. If the wait runs out it is a forfeit, as if they had logged out: the other one wins and leads if they were the challenger, and the one who did not come back is told how it ended at their next login. The wait is `duel_reconnect_seconds` (60 by default; 0 is the old instant forfeit). **Each duelist has a window of their own** (their disconnect plus `duel_reconnect_seconds`) and the duel stays paused while either of them is away, so leaving right after the other one wins nothing. Somebody who is still away when their own window ends forfeits (unless the other is away with time left: that one is waited for first). If both windows end with both still away, **nobody wins**: the duel is called off, the pair cannot duel again for 10 minutes, and both read it at their next login. Once both are back it resumes. Each duelist gets **one** wait per duel: a second disconnect of the same duelist is a forfeit at once. A server that is stopping settles nothing. You cannot remove (`/pack kick`) the player you are dueling, and leaving the pack's party in the middle of a duel only takes effect after it. The same two cannot duel again for **10 minutes**, and a challenger whose challenge ran out or was declined cannot ask the same leader again for **5**. It is called off, with a message saying why, after **2 minutes**, when the pack is not the one it was asked in any more, or when **another player** kills either one (that death stands).
- **Only the leader invites and kicks.** (The party of Open Parties and Claims, below, has no admins: everyone but its owner is a plain member.)
- When the leader loses the spark (stolen from outside, given up, cured, or leaves the werewolves) the **strongest other Alpha takes the pack** before any Beta is made an Alpha. **An ordinary death of the leader (a mob, a fall, lava, a player who is not a werewolf) changes nothing: they keep leading.** And a werewolf who kills a member of their own pack, the leader included, changes nothing either: the lead moves only in a duel.
- **Told.** A thief is told how many sparks they hold (and that the cap stopped some), the victim who took it (at their next login if away), and a cure or heal says its stacked sparks are gone. A refused duel challenge says how many minutes to wait.
- **A leader who stays away.** Offline for `absent_leader_days` real days (default **7**, 0 = off), the leader loses the lead to the strongest second Alpha (online or not) the next time the server checks (when anyone logs in, and every 10 seconds). **The successor must be here** (online, or seen within the same days): if every other Alpha is away too, nothing changes, and a pack whose leader is away with **no second Alpha keeps its leader**. A server that was off is not an absence, and a restart does not erase one: only the gap is forgiven. **They keep their spark and stay a second Alpha**, can challenge for the lead when they are back, and are told at their next login. The pack is told and the Open Parties and Claims party moves too.
- **Allied packs** (`/pack ally`): their members cannot hurt each other and the Roar does not make them submit. **That includes Packmates**: a Packmate of one pack cannot hurt, and cannot be hurt by, anyone in an allied pack (a Packmate has every perk of the pack, and this is one). They share no pack bonus and no Roar buff.
- **The Alpha Trial** (plan 15) is not in the game yet: `AlphaTrial.win` is the hook it will use, so until then the only way to a second Alpha is an invitation to an Alpha.
- **Open Parties and Claims.** With Open Parties and Claims (or its Team Claims fork, same mod id) installed and `opac_link_enabled` on, a pack gets **a party of its own**: the leader owns it and everyone else is a plain member. It is **never a party anyone made by hand**: a leader who already has one keeps it and the pack makes no party; a player who is already in another party is left out of ours and **stays in the pack**. Someone who **leaves the party (or is removed from it) leaves the pack, and is told so: "You left the party, so you left the pack. A werewolf Beta becomes an Omega."** Only the owner, who is the leader, can remove people from it. Someone who leaves the pack leaves the party. A party its owner ended is not made again until someone joins or leaves. When leadership moves, the new leader gets a new party, our members leave the old one, and the old owner's party is left as it is. Without the mod nothing of this exists.
- **Cures and bites.** A cured Beta stays in the pack as a **Packmate**. A bite from a second Alpha puts the bitten in the **leader's** pack (the biter is still the sire); a Packmate of that pack becomes a Beta of it; someone who already runs with another pack is bitten but **not moved**. A Packmate who becomes a werewolf any other way becomes a Beta of the pack they are in.
- **Hunters of the Five.** A kill between two members of one pack gives no mark and no Hunter's Curse, in either direction.
- **Told.** Everyone a change takes out of a pack, or moves to a new leader, is told: in chat now, or at their next login (a short list is kept for someone who stays away). A leader the party link could not add because they are in another party is told once. An invitation to a leader who already has a pack says that their pack comes along. **Only a member of the pack can be named an Emissary** (invite them first, as a Packmate), and only the leader can name one; an Emissary who leaves or is removed is nobody's Emissary any more.

### Pack bonus

Every 5 seconds the game counts an **online Alpha's** Betas within **64 blocks** in the same dimension. With **3 or more**:

- the Alpha gets **+5% attack per Beta counted** (up to 5, so up to +25%); **bitten Betas count twice**
- those Betas get **Regeneration I**

The bonus stacks multiplicatively with the Alpha's +20% (1.20 x 1.25). An offline or distant Alpha keeps the pack but not the bonus. The eclipse suppresses it.

### Stacked sparks

The **Deucalion factor**: an Alpha who kills an Alpha from outside their pack keeps their own spark and **gains the victim's on top**, so one wolf can hold several.

| | |
|---|---|
| **Count** | every Alpha has 1 spark; a steal adds **all** the victim's to the killer's, up to `max_stacked_sparks` (default **5**, 1 turns it off). A werewolf who was no Alpha becomes one with the victim's count (the first spark is the rank, the rest are stacked). |
| **Each spark past the first** | **+10%** to the Alpha rank's attack and health bonuses (the True Alpha's ×1.1 on top) and **one tier of Strength** for the pack from the Alpha Roar (Strength I with one spark, II with two, up to V). |
| **A True Alpha** | never stacks (they cannot steal) and nothing is taken from them: they count as holding `max_stacked_sparks` while active (full bonus and the top Roar tier), and as one spark while dormant. Extras stacked before the rule are absorbed into the full count. |
| **Lost** | the cure, `/pack heal`, losing the faction and being robbed of the spark drop every stacked spark together with the rank. |
| **Shown** | "Alpha ×3" on the Alpha's row in every theme, in the Pack page's rank line and in the Bloodline page. |

### Alpha Roar

Key bind **Alpha Roar** (unbound by default, category *Supernatural Bloodlines*) or `/pack roar`. **60 second** cooldown. Alpha and True Alpha only, not during an eclipse ("The eclipse swallows your roar.").

- **Pack Betas within 32 blocks:** Strength I (a tier higher for each [stacked spark](#stacked-sparks)) and Speed I for 30 s, and may shift at will for those 30 s.
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
| Werewolves' own **Free Will** skill (`werewolves:free_will`) | **full** |

- **Half control:** human and survivalist forms at will.
- **Full control:** also the beast forms, but only from Werewolves level `full_form_level` (default **8**); below it, half.
- **Free Will** is the skill Werewolves itself sells for controlling the full moon; before this mod's gate it let its owner leave a form, so it counts as full control here too (an earlier build cancelled their shifts).
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
- The overworld's **fog, horizon and sky turn red**, and the **moon turns dark orange** (client side: a tinted disc over the sky and a dark orange disc over the moon, drawn after the vanilla sky; no mixin).
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

## Wild packs and the weaker spark (plan 15b)
- **Wild packs.** On a **full-moon night** (a lunar eclipse counts as one, and an Enhanced Celestials **Blood Moon** on another night at half the chance), once per player per night, a pack of wild werewolves led by an Alpha of the Werewolves mod, and sometimes a lone wolf, appears 24 to 48 blocks from you, in the dark and away from villages. They leave at dawn when nobody is near. `npc_packs_enabled` turns it off.
- **What they hunt:** a vampire first, then a hunter, then another player, then prey (sheep, cows, pigs, chickens, rabbits, goats). They hunt together and are **feral** (stronger and faster) in the dark on a full-moon night. A Wolf Spirit never hurts its owner, their pack or an allied pack.
- **The weaker spark.** If you are a werewolf and you kill an Alpha of the Werewolves mod (a wild pack's or a forest one), a weaker spark takes root in you at once. It grows while you are online (3 days); until then it counts for nothing, is not part of your spark count and cannot be stolen. When grown it becomes a normal spark: an Omega or Beta becomes an Alpha, and the path to True Alpha closes. If your path is **open and protected** the spark is destroyed instead and your path is kept. You can hold two at a time and take one every three days. A cure, or becoming a True Alpha, loses it. Your own profile and the Pack page say when it will be ready; nobody else sees it.
- **Moonblood Surge** (`/pack surge`, a werewolf in a pack): the pack within 24 blocks gets a buff; you pay with Weakness and Slowness for 30 s. **Wolf Spirits** (`/pack spirits`, an Alpha): three spirit wolves for three minutes. Both are on the Home page, are refused in a lunar eclipse, and have a cooldown.

## Brothers and pack wolves (plan 15d)
- **The Werewolf Brother rite** (a small circle, a moonstone, raw meat and a silver nugget). **Any werewolf** may use it on a wild werewolf beast that stands in the circle and has been **wounded to half its health or less**: beaten, not lured. Never an Alpha, a trial boss or a wolf that already has a pack. **2 Brothers per werewolf, 6 per pack.** Find it on the Rites page, or press **Recruit** on the Pack page.
- **What a Brother does.** Follow (stays near you, comes to you from far away), Guard (holds its post and takes up what hurts you) or Rest (stays put, only fights back); it can **hunt** on quiet nights (up to three animals near its post, never a tamed, named or baby one) and **sort a chest** by its post. Every order is a button on the **Pack page**; click a wolf for its profile. It counts **half a Beta** toward the pack bonus (capped), gets the pack's Regeneration, the Roar and the Surge, and **never takes a spark**.
- **When you are away.** A Brother keeps its bond and rests where it stands while you are offline or dead (it does not hunt or tidy then), and follows again when you are back. It is lost only if it dies or you release it. **The pack's leader can give your Brother orders and turn Hunt and Sort on or off, but only you can release it.**
- **Chests.** A Brother only uses a plain chest, trapped chest or barrel that you could open yourself (a claim that stops you keeps it out) near its post; it never touches a furnace or a hopper and never drops what does not fit: the drops stay where the animal fell. Hunt and Sort need a post: give it Guard or Rest first.
- **A full pack.** If a player joins your pack and it then holds more than 6 Brothers, the newest ones rest and count for nothing until there is room.
- **Peace.** A pack's own wolves and its allies never attack each other.
- **With Tensura.** Every subordinate of a pack member (of a Brother too, one step only) joins the pack as a member, keeps Tensura's own orders, gets the pack's gifts and counts a quarter of a Beta, at most 6 per pack (`tensura_subordinates_join`).
