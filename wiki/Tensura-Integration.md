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

### The layer, rung by rung

With the supernatural system on, your species layer arrives step by step as you climb your race's ladder, instead of all at once. An Original, for example, starts with Drain, gets Self-Regeneration and a first share of the extra magicule at the first rung, Charm at the second, and the whole layer (Blood Mist and the full 20% magicule) at the third. Being counted as undead and the +25% EP gain apply from the start. Nothing already granted is ever taken back, and a player who was already a species when the system arrived keeps the whole layer. The rows are in the part docs (`docs/parts/species_tensura.md`, "Layer by rung").

Rungs also pay Tensura EP when they are claimed: a flat sum straight into your EP, with no boost and no evolution requirement touched.
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

## Ghouls (part `species_tensura`)

A player who becomes a **Ghoul** (see [Vampires and Originals](Vampires-And-Originals#ghouls)) and whose Tensura race is exactly `tensura:human` shows the **Ghoul** race. When they finish the turning they become `tensura:vampire`; every other race (an elf, say) is kept throughout. While you are a Ghoul **no Tensura evolution goes through**: the evolution screen may seem to accept it, but your race doesn't change and chat says to complete your turning first. EP keeps adding up. Leaving the vampires sets a Ghoul back to human, like any cure.
## Apex evolutions

From the **third tier** of a Tensura line (saint tier for the human-like lines), a player with a species (or a plain vampire or werewolf) can pick **one apex race** in Tensura's own evolution screen. Offer ids are `supernaturalbloodlines:<name>`. With the supernatural ladders on, you also need the top rung of your race's ladder.

| Species | Apex race | Signature skill (placeholder) | Undead |
|---|---|---|---|
| Original | Primordial Vampire | infinite_regeneration | yes |
| Original hybrid | Primordial Hybrid | ultraspeed_regeneration | yes |
| Hybrid | Moonblood Hybrid | godwolf_sense | yes |
| Witch | Ancestral Witch | magic_sense | no |
| Siphoner, Heretic | Void Siphon | gluttony | no |
| Tribrid | Tribrid Ascendant | ultra_instinct | yes |
| Hunter of the Five | Eternal Hunter | danger_sense | no |
| Plain Vampire | Elder Vampire | self_regeneration | yes |
| Plain Werewolf | Primeval Alpha (`alpha_primeval`) | godwolf_sense | no |

| Tensura line | Offered from (tier 3, then tier 4) | For |
|---|---|---|
| Human, dwarf, elf, merfolk | `human_saint`, `dwarf_saint`, `elf_saint`, `merfolk_saint`; `divine_human`, `divine_dwarf`, `divine_elf`, `divine_fish` | every race with an apex race |
| Vampire (vampire, overcomer, lord, divine) | `vampire_lord`; `divine_vampire` (the overcomer is the tier below) | vampire, Original, hybrid, Original hybrid, heretic |
| Beastfolk | `spirit_beast`; `divine_beast` | werewolf, hybrid, tribrid |

A witch- or siphoner-werewolf may be offered two apex races. A **True Alpha** (not dormant) on Primeval Alpha has the apex bonuses x1.5 and is granted the skill Primeval Roar: the Alpha Roar with Roar's Echo's cooldown, twice the range, and +10% attack for 30 s for the Betas it reaches (draft numbers).
- An apex race needs **2,000,000 EP** (the same as Tensura's divine human) and can give more max health than the divine tier (Primordial Vampire 1000, against 980). **All names, stats, EP and signature skills are placeholders.**
- Apex races are human-like. The five vampire-type ones (Primordial Vampire, Primordial Hybrid, Moonblood Hybrid, Tribrid Ascendant, Elder Vampire) are also tagged `tensura:undead` (holy damage still applies). Tensura's `no_blood` includes undead, but every apex race is also in our tag `supernaturalbloodlines:bitable`, so **no apex race is bloodless for this mod**: they can be bitten. Tensura's own drain, blood mist and vampire feeding reach them too.
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

**Siring names the sired player in Tensura, automatically.** The name is the sired player's **own current name**, never a new one: it only makes the two mods compatible (Tensura's naming link). Siring itself needs no consent, and neither does this.

- It uses Tensura's own naming, so the sire **pays Tensura's magicule cost** and the sired gets **Tensura's EP and stat reward**, as for a real LOW naming. LOW never gives an evolution, so a sired player is never evolved by their sire.
- **`playerNaming=false` does not block it**, and the sired does not need to turn their own naming on. The rule and the toggle are lifted for that one call and given back; everybody else is still bound by the rule.
- It happens when the sire link is made (siring, or the species arriving), and is tried again whenever the sired or the sire **logs in**. It needs the **sire online** and able to pay; a sire who is offline, or short of magicule, leaves the sired unnamed until the next login of either. Tensura's other checks stay (a sired with more EP than the sire cannot be named).
- A sired who already has a Tensura name, or a Tensura owner who is not the sire, is left alone. A sired player the Rite of Severance freed keeps the name.
- There is no menu exception for a sire any more; Tensura's naming menu follows Tensura's rules (`playerNaming`, the target's toggle) like for anyone. A sire's hand naming of their own sired is still forced to LOW, so a sire never evolves their sired.
- Tensura's naming also fully heals the named player, removes harmful effects, makes them glow for 2 seconds and clears their tracked evolution (also when it happens at a later login). Players named by hand before this update keep their old name; a later account-name change is not followed. A sired player with more EP than the sire is never named, and the sire sees Tensura's refusal at each login.
- Switch: `sire_naming_enabled` (default on). It needs `sire_subordinate_link_enabled` (the owner link is what Tensura's naming reads). Off: siring names nobody.

- **The Rite of Severance** frees a sired player: the link is removed, and so is a Tensura owner a real naming set (only if it was the severed sire; someone else's owner and the name stay). A severed player keeps the name and gets no new one from the old sire. See [Rite of Severance](Rite-Of-Severance).

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

`vampirism_tensura` uses 3 mixins (sun burn, sun debuffs, bite refusal) and `species_tensura` uses 2 (undead checks, evolution list). Each has `require = 0`: if Tensura renames a method after an update, the hook is skipped, the log says so, and the rest keeps working. See [Parts and compatibility](Parts-And-Compatibility#mixins).

## Limits

- Mastery of a dormant layer skill is lost (see above).
- No race icon for the apex races.
- Not played in a real client yet: the evolution screen offer, the automatic sire naming at a real login, and Alpha rank skills in the skill list. See [Known limits](Known-Limits).
