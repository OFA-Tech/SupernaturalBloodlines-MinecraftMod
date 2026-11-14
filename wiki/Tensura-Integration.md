# Tensura integration

Two parts link Tensura: **vampirism_tensura** (Vampirism and Tensura) and **species_tensura** (the species pages, plus Tensura). A third, **enchanted_tensura**, is covered in [Enchanted integration](Enchanted-Integration). See [Parts and compatibility](Parts-And-Compatibility) for which mods each part needs.

**The Tensura race is never replaced by a species.** A species is a *layer* on top of the race you already have.

## Vampirism and Tensura (part `vampirism_tensura`)

Needs Vampirism and Tensura. Werewolves is optional. Config file: `supernaturalbloodlines-vampirism_tensura.toml` (see [Configuration](Configuration#vampirism_tensura-supernaturalbloodlines-vampirism_tensuratoml)).

| Topic | What happens |
|---|---|
| Becoming a vampire (Vampirism) | You keep your Tensura race. Only a player whose race is exactly `tensura:human` becomes `tensura:vampire` on joining. An elf stays an elf. Not configurable. |
| Being a Tensura vampire | A Tensura race of the vampire line joins the Vampirism vampire faction at a **level floor**: `vampire` 1, `vampire_overcomer` 7, `vampire_lord` 7, `divine_vampire` the maximum. `ghoul` joins nothing. The floor never lowers a level you already have. |
| Cure | A cured player goes back to `cure_race` (default `tensura:human`), **only** if their race is in the vampire line. Any other race stays. |
| Sun | Only Vampirism's sun damage hurts a Vampirism vampire. Tensura's own vampire burn, and its Weakness, Slowness and Mining Fatigue in the sun, are switched off for them (two mixins). A bound [Sun Ring](Items-And-Blocks#rings-and-wards) also blocks Vampirism's sun damage. |
| Hunters | A hunter whose race is tagged `tensura:undead` or `tensura:daemon` cannot join the hunters. A hunter who becomes one leaves the hunters and is told so. |
| Werewolves | Races tagged `tensura:undead`, `tensura:daemon` or `tensura:no_blood` cannot become werewolves. |
| Werewolf race (R20b) | A player whose race is exactly `tensura:human` who becomes a werewolf becomes **`tensura:beastfolk`**. A werewolf on the Beastfolk line becomes a **`beast_lord`** as an Alpha and a **`spirit_beast`** as a True Alpha (Tensura's line is beastfolk, beast lord, spirit beast, divine beast). The race is never lowered, `divine_beast` stays a normal Tensura evolution and every other race is kept (an elf stays an elf). Players with a bloodline species are skipped (so a witch or siphoner werewolf on the human race stays human). A True Alpha who rejoins as a human goes straight to spirit beast. Only a join or a rank change after the update counts. Switch: `werewolf_beastfolk_enabled`. |
| Werewolf skill grants | Werewolf levels grant Tensura skills. Default: `tensura:godwolf_sense` at the maximum werewolf level. It is taken back if the level drops below it or the player stops being a werewolf (only if this mod granted it). |
| Holy damage | Vampirism holy water counts as Tensura holy damage. Tensura holy damage to a Vampirism vampire is multiplied by `tensura_holy_vs_vampire` (default 1.5), unless the species layer already makes Tensura treat them as undead (then Tensura's own x3 applies, and this multiplier is skipped so the two don't stack). |
| Bloodless races | A player whose race is tagged `tensura:no_blood` cannot be bitten, unless the race is also in our tag `supernaturalbloodlines:bitable` (the apex races) (the biter sees "There is no blood in them to drink."). Tensura bloodless mobs have blood 0 for Vampirism. |
| Existence points (EP) | Vampirism and Werewolves mobs have EP data so that Tensura's analysis can read them. The numbers are first guesses. |

Unified-vampire ids and levels can be changed with `race_level_floor`.

## The species layer (part `species_tensura`)

Needs Vampirism, Enchanted, Tensura, and the `species` part. Config file: `supernaturalbloodlines-species_tensura.toml`.

A species gives the player a **layer**:

- **Tensura skills**, learned **free of magicule cost** and tracked, so only skills this mod granted are ever taken back. A skill you already had is yours and is never taken. If two sources grant the same skill (for example the layer and your werewolf level), it stays until the last source lets go.
- **+% max magicule and max aura** (attribute modifiers `supernaturalbloodlines:layer_magicule` and `layer_aura`).
- **Undead treatment** in Tensura (when the layer says so).

The default layers (change them with `layers`; see [Configuration](Configuration#species_tensura-supernaturalbloodlines-species_tensuratoml)):

| Species | Skills | Max magicule | Max aura | Counts as undead |
|---|---|---|---|---|
| Original | blood_mist, drain, charm, self_regeneration | +20% | 0 | yes |
| Original hybrid | blood_mist, drain, charm, self_regeneration, beast_transformation, godwolf_sense | +25% | 0 | yes |
| Hybrid | drain, beast_transformation, self_regeneration | +10% | 0 | yes |
| Witch | none | +30% | +10% | no |
| Siphoner | drain | +10% | 0 | no |
| Heretic | drain, charm, self_regeneration | +20% | 0 | yes |
| Tribrid | drain, beast_transformation, self_regeneration, charm | +30% | 0 | yes |
| Hunter of the Five | none | 0 | +10% | no |
| Plain Vampirism vampire (`VAMPIRE`, no species) | drain | 0 | 0 | yes |

A skill with no namespace is a Tensura skill (`drain` is `tensura:drain`).

The layer is re-checked when your bloodline changes, on login and respawn, when your Vampirism level changes, and every 200 ticks. A Tensura evolution does not remove it.

**Losing a species:** its layer skills go dormant (they are forgotten) and come back if the species returns. **The mastery of those skills is lost**; they start fresh.

### Undead treatment and holy damage

When a layer says undead, Tensura treats the player as undead:

- holy and light damage **x3**
- holy exposure
- infection immunity
- Enervation on food, and the rest of what Tensura asks of its undead checks

A plain Vampirism vampire counts as well: they go from x1.5 to Tensura's x3 holy damage, plus infection immunity and Enervation from food (Tensura's undead rules). This is wider than "holy damage and sunlight", so it has its own switch: `undead_treatment_enabled`. Off: skills and bonuses stay, Tensura's undead checks are left alone, and the x1.5 multiplier of `tensura_holy_vs_vampire` returns.

Being undead does **not** by itself make a player bloodless. (The apex races below are undead but stay bitable.)

### Hunters and werewolves cannot be undead races

A Tensura undead or daemon race cannot be a hunter or a werewolf (see above). **The exception (R103):** an apex race of this mod, held by a vampire-type species (Original, Original hybrid, Hybrid, Heretic, Tribrid), may stay a hunter and become a werewolf. An Original with a Tensura **ghoul** race is ejected from the hunters like anyone else. With the `species_tensura` part off there is no exception.

### Who counts as human-like

Only players whose Tensura race is human-like can take a bloodline (R1). The `tensura:human_like` tag only has the five base races (human, dwarf, elf, merfolk, beastfolk), so this mod also accepts their evolved races (enlightened, saint, divine and so on, 15 more) and the apex races. A player with no Tensura race passes.

## Apex evolutions

From **saint tier or higher** of a human-like Tensura line, a player with a species can pick **one apex race** in Tensura's own evolution screen. Offer ids are `supernaturalbloodlines:<name>`.

| Species | Apex race | Signature skill (placeholder) | Undead |
|---|---|---|---|
| Original | Primordial Vampire | infinite_regeneration | yes |
| Original hybrid | Primordial Hybrid | ultraspeed_regeneration | yes |
| Hybrid | Moonblood Hybrid | godwolf_sense | yes |
| Witch | Ancestral Witch | magic_sense | no |
| Siphoner, Heretic | Void Siphon | gluttony | no |
| Tribrid | Tribrid Ascendant | ultra_instinct | yes |
| Hunter of the Five | Eternal Hunter | danger_sense | no |

- Tier-3 saint races: `human_saint`, `dwarf_saint`, `elf_saint`, `merfolk_saint`, `spirit_beast`. Tier 4: `divine_human`, `divine_dwarf`, `divine_elf`, `divine_fish`, `divine_beast`.
- An apex race needs **2,000,000 EP** (the same as Tensura's divine human) and can give more max health than the divine tier (Primordial Vampire 1000, against 980). **All names, stats, EP and signature skills are placeholders.**
- Apex races are human-like. The four vampire-type ones (Primordial Vampire, Primordial Hybrid, Moonblood Hybrid, Tribrid Ascendant) are also tagged `tensura:undead` (holy damage still applies). Tensura's `no_blood` includes undead, but every apex race is also in our tag `supernaturalbloodlines:bitable`, so **no apex race is bloodless for this mod**: they can be bitten. Tensura's own drain, blood mist and vampire feeding reach them too.
- **Losing the species does not remove a chosen apex race.** The species-only layer skills go dormant; the race's own signature skill stays.
- There is no icon for the apex races in the evolution screen (not checked in a client).
- Switch off: `apex_evolutions_enabled=false`. The apex races stay registered; there is just no option to pick them.

### The evolution boost

A player with a species gains `evolution_progress_multiplier` (default **1.25**, range 1 to 10) times the EP Tensura credits them, so they reach an evolution's EP requirement sooner. Set it to 1.0 to turn it off.

It speeds up **EP gain only**. The evolution requirements and their progress are Tensura's own, so no boss, item or awakening can be skipped. (The setting's name is from an older version, where it scaled the progress; the value is kept.) It works through Tensura's own aura and magicule gain, so it needs no extra mod hook.

> **Important:** keep `apex_evolutions_enabled` and `undead_treatment_enabled` equal on the server and on every client. The evolution screen is built on the client too. See [For server owners](For-Server-Owners#common-configs-must-match).

## Sires as Tensura owners, and naming

When someone is sired (see [Sirelines](Sirelines)), the sire becomes the **Tensura permanent owner** of the sired. Switch: `sire_subordinate_link_enabled` (default on).

- **Players** are linked while they have a sire **and** are still supernatural. A player who loses the species loses the link and gets it back with the species. A cure clears it.
- **Creatures** are linked when sired. A creature that is `ISubordinate` in Tensura is also tamed when the sire is online.
- A Tensura owner the player already has is never overwritten. The sire is still recorded, but the link and the naming exception do not apply.
- **Side effects of being a Tensura subordinate:**
  - A sired player **cannot hurt their sire or the sire's other sired** with a hit that goes through Tensura's hurt handler (a werewolf-bitten player's sire is the Alpha). The sire can still hurt them. Turn the link off to remove this.
  - Owner and sired count as allied, and a creature never targets its owner.
  - A demon lord sire's harvest festival also gifts the sired.
- Tensura's Starved and Usurper skills do not check for subordinates, so a sire can drain or rob a sired player with them. Nothing was changed.

### Sire naming (R101)

A sire may **name their own sired** through Tensura's naming menu, **even with Tensura's `playerNaming` game rule off**.

- The sired must have **switched their own naming on** (sneak with the naming key; Tensura's players are not nameable by default) and have no Tensura name yet.
- Only for the sire's own sired. Strangers and other sires are refused.
- It costs the sire magicules, as Tensura says, and gives Tensura's reward (EP and stats).
- **A sired player named this way never evolves** (the naming type is lowered to LOW). A sire who names at MEDIUM or HIGH is told once, in chat, that it grants EP and stats only. The sire still pays the cost they chose, and still risks the max-magicule loss.
- Switch: `sire_naming_enabled` (default on). **This bypasses `playerNaming` for the sire and their sired only.** Turn it off if your server rule is meant to be absolute.

- **The Rite of Severance** frees a sired player: the link is removed, and so is a Tensura owner a real naming set (only if it was the severed sire; someone else's owner and the name stay). A severed player is no longer the sire's to name. See [Rite of Severance](Rite-Of-Severance).

To remove the links cleanly: set `sire_subordinate_link_enabled=false`, have everyone log in once, **then** disable the part. With the part off nothing runs and the links stay.

**Config mismatch (known gap):** if `apex_evolutions_enabled` is on for a client and off for the server, the client shows the apex race and the **server silently refuses the pick**. Nothing is logged (telling the refusal apart needs a fourth mixin, over the budget). Keep the config equal.

## Werewolf rank skills

Needs Werewolves. Config key `rank_skills`.

| Rank | Skills (default) |
|---|---|
| Alpha | `tensura:beast_transformation`, `tensura:godwolf_sense` |
| True Alpha | those two plus `tensura:commander` |
| Born werewolf of Werewolves level 5 or more | `tensura:godwolf_sense` |
| Omega, Beta | none |

- The skills do not change the race (the race follows the Beastfolk line, see above).
- The rank skills need the werewolf faction: a True Alpha who is cured keeps the rank, dormant, and its skills come back when they rejoin the werewolves.
- Only skills this mod granted are taken back, and a skill another source still wants stays.

See [Werewolves and packs](Werewolves-And-Packs) for the ranks.

## Mixins in this integration

`vampirism_tensura` uses 3 mixins (sun burn, sun debuffs, bite refusal) and `species_tensura` uses 3 (undead checks, evolution list and progress, sire naming). Each has `require = 0`: if Tensura renames a method after an update, the hook is skipped, the log says so, and the rest keeps working. See [Parts and compatibility](Parts-And-Compatibility#mixins).

## Limits

- Mastery of a dormant layer skill is lost (see above).
- No race icon for the apex races.
- Not played in a real client yet: the evolution screen offer, naming through the real menu, and Alpha rank skills in the skill list. See [Known limits](Known-Limits).
