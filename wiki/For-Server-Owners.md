# For server owners

Read this before you put the mod on a server with players you care about.

## Quick checklist

1. **Same mods, same versions, same jar** on the server and on every client. See [Installing](Installing).
2. **Same configs** on both sides: [see below](#common-configs-must-match).
3. **Back up the world** before you change a part switch, remove Tensura, or remove the mod.
4. Read the **apex race warning** before you remove anything.
5. Decide the **balance knobs** you want (list at the bottom) before players reach saint tier.

## Removing the mod or Tensura

> **Warning: reset players on an apex race first.**
>
> ManasCore (Tensura's race system) **throws an error** when a player is saved with a race that is **not registered**. That player **cannot load at all**. There is no fallback.

The apex races (Primordial Vampire, Primordial Hybrid, Moonblood Hybrid, Ancestral Witch, Void Siphon, Tribrid Ascendant, Eternal Hunter) are registered by **this mod**. So:

| You want to... | Do this |
|---|---|
| **Remove the mod** | **First** reset every player who picked an apex race, with **Tensura's race command**. Then stop the server and remove the jar. Players who are not reset cannot join. |
| **Remove Tensura** | The same load error happens for **every Tensura race**, not only ours. Tensura's own warnings apply. |
| **Turn off only `species_tensura`** | Safe. **The apex races stay registered** (this mod registers them whenever Tensura is installed), so players load. The apex race stays on the player; the layer does nothing. An Original **hunter** on `primordial_vampire` is **ejected from the hunters** (the vampire-type exemption needs the part). |
| **Roll back to an older build** (before `species_tensura`) | **First reset players on an apex race** with Tensura's race command, and **back up the world** before the first deploy. An older build also **forgets severances** ([Rite of Severance](Rite-Of-Severance)), so severed bonds come back. |
| **Take an apex race away** | Tensura's race command. There is no undo in this mod. Losing the *species* does **not** remove an apex race: its species-only skills go dormant, the race's own signature skill stays. |

Other things that cannot be rolled back in the game: mastery of a skill that was taken away and given back is lost.

## Switching parts off

Edit `[parts]` in `config/supernaturalbloodlines-common.toml` (see [Configuration](Configuration#core-supernaturalbloodlines-commontoml)) and **restart**. Do it on the server **and** the clients.

What a part switch does:

- Its classes, items, blocks, effects, rites, recipes, loot, tags and EP data are **not loaded**.
- Items and blocks it registered **vanish from the world** when chunks and inventories load (Minecraft drops unknown ids). That includes: Blood Stone and the poppets (vampirism_enchanted), Condensed Magicule and the Spirit Poppet (enchanted_tensura), and all species and artifact items. **Back up first.**
- A part that **builds on** another goes off with it: turn **vampirism_enchanted** off and **species**, **species_tensura** and **artifacts** go off too.
- The permanent attribute modifiers of a part that is off are **removed on login** (`original`, `hybrid`, `hunter_mark`, `wolf_rank`, `pack_bonus`, `layer_magicule`, `layer_aura`).
- **Turning `enchantedTensura` off drops the saved curses** of this mod (Magicule Drain, Skill Seal) from players. They don't return if you switch the part on again.
- A client without the same switches can't join (registry mismatch).

Do **not** try to switch off a part by deleting a data file or editing the jar: data loads through the part's condition, and a missing codec can stop the world from starting.

## Granted skills and links stay when a part is off

Switching a part off means *nothing runs*. It does **not** undo what already happened:

| What | After the part is off |
|---|---|
| Tensura **skills** granted by a species layer, werewolf level grants or rank skills | **stay** on the player. Nothing takes them back. |
| **Max magicule / aura bonuses** of the layer | **removed** on the player's next login (the modifiers are cleaned). |
| **Sire links** (Tensura owner) | **stay**, and so does the one-way friendly fire (a sired player can't hurt the sire). Clear them with Tensura's owner commands, or follow the steps below. |
| Players' **saved bloodline data** | stays in their player file. |
| **Apex races** | stay (see above). |

To **clean up skills first:** remove the species from the players (or set `layers` and `rank_skills` to empty and let everyone log in), then switch the part off.

To **remove sire links cleanly:** set `sire_subordinate_link_enabled = false` in `supernaturalbloodlines-species_tensura.toml`, have **everyone log in once**, **then** disable the part. The switch is read at login, on a bloodline change and on a faction change. With the part already off, nothing runs and the links stay until cleared by hand.

## COMMON configs must match

NeoForge **COMMON** configs are **not synced from the server**: each side reads its own file. Keep these equal on the server and on every client:

- **All of `[parts]`** (and the same mods installed).
- **`supernaturalbloodlines-species_tensura.toml`:** `layers`, `undead_treatment_enabled`, `apex_evolutions_enabled`, `evolution_progress_multiplier`. Tensura's undead checks and the evolution screen run **on the client too**, so a mismatch gives wrong screens (a missing apex option). `evolution_progress_multiplier` only matters on the server.
- **`moon_bound_werewolves`, `full_form_level`** in the common file.
- The other values (`max_originals_per_world`, `max_five`, chances, `sireline_death`) are used by the **server** only, but equal values avoid confusion.

If `apex_evolutions_enabled` differs, the client shows the apex race and the server **silently refuses the pick**, with nothing in the log.

A simple way: keep one `config/` folder in your modpack and ship it to everyone.

## Balance knobs

| Knob | Where | Default | Why you might change it |
|---|---|---|---|
| Undead treatment | `undead_treatment_enabled` | on | **Wider than holy damage**: a plain faction vampire goes from x1.5 to **x3** holy and light damage, and Tensura's other undead rules apply (infection immunity, Enervation on food...). Off keeps skills and bonuses but not this. |
| Evolution boost | `evolution_progress_multiplier` | 1.25 | a player with a species gains 25% more EP. It speeds up EP gain only: no evolution requirement is skipped. 1.0 turns it off. |
| Apex races | `apex_evolutions_enabled` | on | An apex race costs **2,000,000 EP** (the same as a divine human) and gives more max health than the divine tier. All values are placeholders. No apex race is bloodless: all of them can be bitten. |
| Sire naming | `sire_naming_enabled` | on | **Bypasses `playerNaming`** for a sire and their own sired: `playerNaming=false` does not beat it. Turn it off if your naming rule is meant to be absolute. Siring needs no consent; the Rite of Severance is the way out. |
| Severance | (rite) | n/a | A witch type can sever an **unwilling** player in the circle (no consent check), and a sired witch type can sever herself. A new sire replaces the old one, tree and sireline death included. |
| Sire link | `sire_subordinate_link_enabled` | on | A sired player can't hurt their sire or the sire's other sired. Also true for a bitten Beta and their Alpha. |
| Starved and Usurper | (Tensura skills) | n/a | They don't check for subordinates, so a sire with them can drain or rob a sired player. Nothing in this mod changes it. |
| Original cap | `max_originals_per_world` | 0 (no cap) | Originals are very hard to kill. Consider a cap. |
| The Five | `max_five` | 5 | |
| Sireline death | `sireline_death` | off | A white oak kill of one Original can wipe their whole line, offline players included. |
| Moon control | `moon_bound_werewolves` | on | Off: everyone shifts at will. |
| Eclipse | `lunar_eclipse_chance` | 3% | 0 turns eclipses off. One flag for the whole server. |
| Hidden gene | `latent_gene_chance` | 15% | rolled once per player at first login |
| True Alpha farming | (design) | | The 300 defender kills can be farmed at a mob spawner beside a villager. |

## Operators
- A bad entry in `layers` or `rank_skills` is **ignored**, logged at **ERROR** when the config loads, and every **operator (permission level 2)** is told in chat on login: "species_tensura: N invalid entries in the config were ignored (see the server log)."
- The debug commands are at **permission level 2**: see [Commands](Commands#supernaturalbloodlines-debug-permission-level-2).
- The log says whether each mixin hook is in place. Search it for `hooks in place`, `MISSING` or `hook is not in place`.

## Worlds with Accessories

The OFA pack runs **Accessories**, which provides a Curios compatibility layer. The Curios `ring` slot and the three rings through that layer are **not yet verified** in that pack. See [Known limits](Known-Limits).
