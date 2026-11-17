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

## Before removing the mod: saved attribute modifiers

Some parts add **permanent attribute modifiers**. They are saved with the player, so **they stay after the jar is gone** (nothing is left to take them away): a player keeps the extra health, attack or speed for good. While the mod runs, switching a part off removes its modifiers at the next login. Before you remove the jar, do either of these:

1. **Switch every part off first** (`[parts]` in `supernaturalbloodlines-common.toml`, all `false`), restart, and let every player log in once. Core strips the modifiers of every switched-off part on login. Players who never come back keep theirs, so for them:
2. **Remove them by command**, per player (`<player>` or `@a` for everyone online). Each line is harmless if the modifier is not there ("no such modifier").

| Modifier id (`supernaturalbloodlines:...`) | Part | Attribute(s) | Command |
|---|---|---|---|
| `original` | species | max health, attack damage | `/attribute <player> minecraft:generic.max_health modifier remove supernaturalbloodlines:original` and the same with `minecraft:generic.attack_damage` |
| `hybrid` | species | movement speed, attack damage | `/attribute <player> minecraft:generic.movement_speed modifier remove supernaturalbloodlines:hybrid` and `minecraft:generic.attack_damage` |
| `hunter_mark` | species | attack damage, movement speed | `/attribute <player> minecraft:generic.attack_damage modifier remove supernaturalbloodlines:hunter_mark` and `minecraft:generic.movement_speed` |
| `wolf_rank` | species | attack damage, movement speed, max health | `/attribute <player> minecraft:generic.attack_damage modifier remove supernaturalbloodlines:wolf_rank`, then `minecraft:generic.movement_speed`, `minecraft:generic.max_health` |
| `pack_bonus` | species | attack damage | `/attribute <player> minecraft:generic.attack_damage modifier remove supernaturalbloodlines:pack_bonus` |
| `layer_magicule` | species_tensura | `tensura:max_magicule` | `/attribute <player> tensura:max_magicule modifier remove supernaturalbloodlines:layer_magicule` |
| `layer_aura` | species_tensura | `tensura:max_aura` | `/attribute <player> tensura:max_aura modifier remove supernaturalbloodlines:layer_aura` |
| `ep_boost` | species_tensura | `tensura:aura_gain`, `tensura:magicule_gain` | `/attribute <player> tensura:aura_gain modifier remove supernaturalbloodlines:ep_boost` and `tensura:magicule_gain` |
| `primeval_true_alpha` | species_tensura | max health, attack damage, movement speed | `/attribute <player> minecraft:generic.max_health modifier remove supernaturalbloodlines:primeval_true_alpha`, then `minecraft:generic.attack_damage`, `minecraft:generic.movement_speed` |
| `skill/s-hum-1`, `skill/s-vam-4`, `skill/s-wit-7` | supernatural | max health | `/attribute <player> minecraft:generic.max_health modifier remove supernaturalbloodlines:skill/s-hum-1` (and `s-vam-4`, `s-wit-7`) |
| `skill/s-hum-3`, `skill/s-vam-2` | supernatural | movement speed | `/attribute <player> minecraft:generic.movement_speed modifier remove supernaturalbloodlines:skill/s-hum-3` (and `s-vam-2`) |
| `skill/s-ori-1`, `skill/s-wol-7` | supernatural | attack damage | `/attribute <player> minecraft:generic.attack_damage modifier remove supernaturalbloodlines:skill/s-ori-1` (and `s-wol-7`) |
| `skill/s-wol-1` | supernatural | armor | `/attribute <player> minecraft:generic.armor modifier remove supernaturalbloodlines:skill/s-wol-1` |

The `skill/...` rows are the ladder skills that change an attribute in the shipped data. A datapack that adds more attribute skills adds more `skill/<id>` modifiers (the id is the skill id in lower case). `/attribute <player> <attribute> modifier remove <id>` is vanilla and works on offline players only through a tool that edits the player file; a player who is offline keeps the modifier until they log in with the mod still installed.

**Also before removing the mod:** apex races (see above), and on every client the hidden keys ([Key binds](Key-Binds)): every player who covered keys should start their client once with `hide_covered_keys = false` (or `hud_enabled = false`, or sit at the title screen) **before** the jar is deleted, because the restore code is in the jar. After that the keys stay unbound until the player binds them again (the old binds are in `config/supernaturalbloodlines-hud-keys.json`). The keys are only unbound while the player is in a world whose server has the HUD, so a server that turns the `hud` part off gives them back by itself.
## The ability HUD part

The `hud` part is mostly client side (the slots, the keys, the blood in Tensura's food spot), but **the server needs it too**: bites and the slot triggers are checked and done on the server, and the loadout is stored on the player. A client without the mod cannot send or receive its packets and is simply skipped. It adds two small mixins with Tensura (a skill in the next preset can be fired, and Tensura's vampire race ability can feed a thirsty vampire); both can be switched off in `supernaturalbloodlines-hud.toml` (`next_preset_active`, `tensura_feed_fills_blood`). Switching the part off puts every hidden key back the next time a client starts. See [Ability HUD](Ability-HUD) and [Configuration](Configuration).

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
- The other values (`max_originals_per_world`, `max_five`, chances, `original_line_death`) are used by the **server** only, but equal values avoid confusion.

If `apex_evolutions_enabled` differs, the client shows the apex race and the server **silently refuses the pick**, with nothing in the log.

A simple way: keep one `config/` folder in your modpack and ship it to everyone.

## Balance knobs

| Knob | Where | Default | Why you might change it |
|---|---|---|---|
| Undead treatment | `undead_treatment_enabled` | on | **Wider than holy damage**: a plain faction vampire goes from x1.5 to **x3** holy and light damage, and Tensura's other undead rules apply (infection immunity, Enervation on food...). Off keeps skills and bonuses but not this. |
| Evolution boost | `evolution_progress_multiplier` | 1.25 | a player with a species gains 25% more EP. It speeds up EP gain only: no evolution requirement is skipped. 1.0 turns it off. |
| Apex races | `apex_evolutions_enabled` | on | An apex race costs **2,000,000 EP** (the same as a divine human) and gives more max health than the divine tier. Every value is in `docs/balance.md`. No apex race is bloodless: all of them can be bitten. |
| Sire naming | `sire_naming_enabled` | on | Siring **names the sired player in Tensura automatically** (their own name; the sire pays the magicule cost, the sired gets EP and stats). **`playerNaming=false` does not block it.** Turn it off if your naming rule is meant to be absolute. Siring needs no consent; the Rite of Severance is the way out. |
| Severance clears the name | `severance_clears_name` | on | The Rite of Severance also clears the sired player's Tensura name when the severed sire was their owner (EP and stats stay). |
| Re-siring reward | `resire_naming_reward` | on | Off: naming a player again after a severance gives them no EP and stats reward (the sire still pays). |
| Severance | (rite) | n/a | A witch type can sever an **unwilling** player in the circle (no consent check), and a sired witch type can sever herself. A new sire replaces the old one, tree and sireline death included. |
| Sire link | `sire_subordinate_link_enabled` | on | A sired player can't hurt their sire or the sire's other sired. Also true for a bitten Beta and their Alpha. |
| Starved and Usurper | (Tensura skills) | n/a | They don't check for subordinates, so a sire with them can drain or rob a sired player. Nothing in this mod changes it. |
| Original cap | `max_originals_per_world` | 0 (no cap) | Originals are very hard to kill. Consider a cap. |
| The Five | `max_five` | 5 | |
| Line death | `original_line_death` | on | A white oak kill of one Original kills their whole line, offline players included. Replaces `sireline_death`. |
| Moon control | `moon_bound_werewolves` | on | Off: everyone shifts at will. |
| Eclipse | `lunar_eclipse_chance` | 3% | 0 turns eclipses off. One flag for the whole server. |
| Hidden gene | `latent_gene_chance` | 15% | rolled once per player at first login |
| True Alpha farming | (design) | | The 300 defender kills can be farmed at a mob spawner beside a villager. |

## Upgrading to line death

Line death is now ON by default, offline players included. Set `original_line_death = false` to keep the old behaviour. `sireline_death` is ignored. A server that had `sireline_death` in `supernaturalbloodlines-species.toml` is told once in the log when it starts. Offline players who owed a death to the old rule still pay it at their next login (as a death of their new line, or dropped if no Original is recorded above them).

Line death only takes vampires and players: a converted animal or villager is never killed or removed by it. The Original's own white oak does not start it (see [Original Bloodlines](Original-Bloodlines)).

## Operators
- A bad entry in `layers` or `rank_skills` is **ignored**, logged at **ERROR** when the config loads, and every **operator (permission level 2)** is told in chat on login: "species_tensura: N invalid entries in the config were ignored (see the server log)."
- The debug commands are at **permission level 2**: see [Commands](Commands#supernaturalbloodlines-debug-permission-level-2).
- The log says whether each mixin hook is in place. Search it for `hooks in place`, `MISSING` or `hook is not in place`.

## Worlds with Accessories

The OFA pack runs **Accessories**, which provides a Curios compatibility layer. The Curios `ring` slot and the three rings through that layer are **not yet verified** in that pack. See [Known limits](Known-Limits).
