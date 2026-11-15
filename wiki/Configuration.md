# Configuration

Four config files, all NeoForge **COMMON** configs. They are in the `config/` folder of the game (or the server). A part's file is only made when that part is on.

| File | Part | What it holds |
|---|---|---|
| [`supernaturalbloodlines-common.toml`](#core-supernaturalbloodlines-commontoml) | core | the **part switches**, moon control |
| [`supernaturalbloodlines-vampirism_tensura.toml`](#vampirism_tensura-supernaturalbloodlines-vampirism_tensuratoml) | vampirism_tensura | Tensura races, levels, skills, holy damage |
| [`supernaturalbloodlines-species.toml`](#species-supernaturalbloodlines-speciestoml) | species | Originals, hunters, hybrids, packs, eclipse |
| [`supernaturalbloodlines-species_tensura.toml`](#species_tensura-supernaturalbloodlines-species_tensuratoml) | species_tensura | species layers, apex races, sire links |
| [`supernaturalbloodlines-supernatural.toml`](#supernatural-supernaturalbloodlines-supernaturaltoml) | supernatural | whether a finished rite counts for ladder tasks |
| [`supernaturalbloodlines-hub-client.toml`](#supernatural-client-supernaturalbloodlines-hub-clienttoml) | supernatural (client only) | the hub's look |

**COMMON configs are not synced** by NeoForge: the server and each client read their own file. Keep them **equal**, especially the part switches and the species_tensura options. See [For server owners](For-Server-Owners#common-configs-must-match).

Edit a file while the game is closed. Values the game cannot use (out of range, wrong type) are reset to the default by NeoForge. A bad entry in the two **list** options of species_tensura is ignored, logged at ERROR and told to operators in chat on login.

## Core: `supernaturalbloodlines-common.toml`

### `[parts]` (restart required)

Switch a part off to keep it disabled even when its mods are installed. All default `true`.

| Key | Default | Part | Needs mods |
|---|---|---|---|
| `enchantedTensura` | `true` | [enchanted_tensura](Enchanted-Integration) | Enchanted, Tensura |
| `vampirismEnchanted` | `true` | [vampirism_enchanted](Enchanted-Integration) | Vampirism, Enchanted |
| `vampirismTensura` | `true` | [vampirism_tensura](Tensura-Integration#vampirism-and-tensura-part-vampirism_tensura) | Vampirism, Tensura |
| `species` | `true` | species (all the species pages) | Vampirism, Enchanted, and the vampirismEnchanted part |
| `speciesTensura` | `true` | [species_tensura](Tensura-Integration#the-species-layer-part-species_tensura) | Vampirism, Enchanted, Tensura, and the species part |
| `artifacts` | `true` | artifacts ([items](Items-And-Blocks)) | Vampirism, Enchanted, and the vampirismEnchanted part |

The switches are read straight from the file early at startup (the config system loads too late for mixins), so a missing file, a missing key, or a bad value means **on**.

### `[werewolves]`

| Key | Default | Range | Does |
|---|---|---|---|
| `full_form_level` | `8` | 1 to 100 | Werewolves level from which **moon control** (Moon Ring, hybrids, Alphas) also allows the full forms (beast and four-legged beast). Below it only the half forms. |
| `moon_bound_werewolves` | `true` | | A werewolf shifts **only** when the full moon forces it, unless something gives them moon control (Moon Ring, hybrid, Alpha, anchor). **Off:** everyone shifts at will. |

## vampirism_tensura: `supernaturalbloodlines-vampirism_tensura.toml`

"Tensura <-> Vampirism sync. Joining Vampirism never changes the Tensura race." (Except a plain human: see [Tensura integration](Tensura-Integration).)

| Key | Default | Does |
|---|---|---|
| `cure_race` | `"tensura:human"` | The race set when a cured player's current race is in the Tensura vampire line. Must be a valid id. |
| `race_level_floor` | `["tensura:vampire=1", "tensura:vampire_overcomer=7", "tensura:vampire_lord=7", "tensura:divine_vampire=-1"]` | `"race=level"`: a Tensura vampire-line race raises the Vampirism level to **at least** `level` (capped at the maximum). `-1` is the maximum vampire level. A race **not listed does not join the faction** (so `tensura:ghoul` is not listed on purpose). A bad entry is skipped with one warning. |
| `werewolf_skill_grants` | `["-1=tensura:godwolf_sense"]` | `"level=skillId"`: a werewolf at that **Werewolves level or higher** is given the Tensura skill, and loses it when the level drops or they leave the faction. `-1` is the maximum werewolf level. |
| `werewolf_beastfolk_enabled` | `true` | A player whose race is exactly `tensura:human` becomes `tensura:beastfolk` when they become a werewolf; an Alpha on the Beastfolk line becomes a `beast_lord`, a True Alpha a `spirit_beast`. Never lowers a race, and every other race is kept. **Off:** werewolves keep their race. Only a join or a rank change after the change counts. Needs Werewolves. |
| `tensura_holy_vs_vampire` | `1.5` (0 to 100) | Tensura **holy** damage (not Vampirism's holy water) to a Vampirism vampire is multiplied by this. **No effect** while species_tensura makes Tensura treat that vampire as undead (`undead_treatment_enabled`, on by default): Tensura then applies its own x3 and this is skipped so the two don't stack. |

## species: `supernaturalbloodlines-species.toml`

All under `[species]`.

| Key | Default | Range | Does |
|---|---|---|---|
| `max_originals_per_world` | `0` | 0 to 10000 | How many Originals (Original and Original hybrid) the world allows. **0 = unlimited.** Counts Originals who no longer log in. |
| `transition_ticks` | `24000` | 20 to 24,000,000 | Play time a werewolf has to drink Doppelgänger Blood after dying with Original's Blood, in ticks. The same time a player has in the turning to drink human blood. |
| `bite_vampire_cost` | `1` | 0 to 20 | How much of their own blood a vampire loses when biting a vampire (a bite on a vampire never feeds). |
| `vampire_blood_ticks` | `24000` | 20 to 24,000,000 | How long vampire blood stays in a player's system, in game ticks. Dying while it is there starts the turning. |
| `max_five` | `5` | 1 to 10000 | How many Hunters of the Five the world allows. Lowering it later removes nobody: the Five can stay above it until marks are lost, and no new Rite of the Five works until there are fewer than this. |
| `witch_bonus_enabled` | `true` | | Witches, heretics and tribrids get **altar range x1.5** and **rite power cost x0.75**. Siphoned power is always usable. |
| `siphoner_chance` | `0.25` | 0.0 to 1.0 | Chance that the Rite of Ancestral Calling makes a **siphoner** instead of a witch. |
| `sireline_death` | `false` | | When an Original is killed with white oak, everyone they sired, and everyone those sired, dies with them (offline players on their next login). |
| `latent_gene_chance` | `0.15` | 0.0 to 1.0 | Chance that a player carries the hidden werewolf gene, rolled **once at first login**. Changing it does not re-roll players who already logged in. |
| `doppelganger_gene_chance` | `0.05` | 0.0 to 1.0 | Chance that a player carries the hidden doppelgänger gene, rolled **once** at the first login after it exists. Changing it does not re-roll players who were already rolled. It can sit next to the werewolf gene. |
| `gene_visibility` | `discovered` | `discovered` or `always` | `discovered`: a player sees each gene as Unknown until the Rite of Lineage (Reveal), the Rite of Revelation, a Doppelgänger Blood rite with their taglock, or a werewolf awakening finds it. `always`: a player always sees their own genes. Never shown to other players. |
| `doppelganger_villager_chance` | `0.005` | 0.0 to 1.0 | Chance that a villager is a **doppelgänger**, rolled **once**, the first time the villager is loaded (new spawns, breeding, village villagers, and old villagers after the update). Unlimited per world. It looks and trades like any villager. |
| `doppelganger_rite_taglock` | `doppelganger` | `doppelganger` or `human` | Whose taglock the Rite of Doppelgänger Blood accepts. `doppelganger`: a doppelgänger villager or a player with the gene. `human`: any human-like villager or player (the old behaviour). The owner must be nearby or online either way. |
| `alpha_bite_death_chance` | `0.10` | 0.0 to 1.0 | Chance that an Alpha's bite **kills** the human instead of turning them. |
| `hybrid_transition_blood` | `original_hybrid` | `original_hybrid` or `original` | Whose **Original's Blood** starts the hybrid transition. `original_hybrid`: only blood taken from an **Original hybrid** (as in the show). `original`: blood from any Original or Original hybrid. The bottle records its source's species when the rite fills it. A bottle made before this setting existed is accepted either way. A refused drink keeps the bottle and tells the player. |
| `lunar_eclipse_chance` | `0.03` | 0.0 to 1.0 | Chance that a full-moon night is a **lunar eclipse**: werewolves lose their form actions, rank perks and pack bonus until dawn. |

## artifacts: `supernaturalbloodlines-artifacts.toml`

All under `[artifacts]`.

| Key | Default | Range | Does |
|---|---|---|---|
| `compass_doppelganger_range` | `512` | 1 to 30,000,000 | How far the [Vampire Compass](Items-And-Blocks) in Doppelgängers mode finds a doppelgänger **villager**. Only villagers in loaded chunks are found, so a larger value does not look further than the server keeps loaded. Players with the gene are found anywhere in the dimension, whatever this says. |
## species_tensura: `supernaturalbloodlines-species_tensura.toml`

"The Tensura layer of each species. The race is never replaced (R100)." **COMMON: keep it equal on the server and the clients**, especially `layers`, `undead_treatment_enabled` and `apex_evolutions_enabled`.

| Key | Default | Does |
|---|---|---|
| `layers` | see below | `"SPECIES=skill,skill\|maxMagiculeBonus\|maxAuraBonus\|treatAsUndead"`. Bonuses are fractions (0.2 = +20%). `SPECIES` is a species name or `VAMPIRE` (a Vampirism vampire with no species). A skill without a namespace is a Tensura skill. **An entry replaces the default of the same key.** |
| `rank_skills` | see below | `"RANK=skill,skill"` with RANK `ALPHA`, `TRUE_ALPHA` or `BORN_LEVEL5` (a born werewolf of level 5). Werewolves only. The race never changes; only skills this mod granted are taken back. An entry replaces the default of its key. |
| `undead_treatment_enabled` | `true` | **Plainly:** on, a vampire takes **x3** holy damage from Tensura and gets Tensura's undead rules (infection immunity, Enervation from food); off, they take **x1.5** (the `tensura_holy_vs_vampire` setting) and none of those. Tensura treats a player whose layer says undead as undead (holy and light damage x3, holy exposure, infection immunity, Enervation on food...). A plain Vampirism vampire counts too, so `tensura_holy_vs_vampire` has no effect on it. **Off:** layers still grant skills and bonuses, and Tensura's undead checks are left alone. |
| `apex_evolutions_enabled` | `true` | Offer the species' apex race in Tensura's evolution screen. Off: no apex option; species keep working. **If it differs between client and server, the client shows the option and the server silently refuses the pick** (nothing is logged). |
| `evolution_progress_multiplier` | `1.25` (1.0 to 10.0) | A player with a species gets this multiple of the EP Tensura credits them (**EP gain only**: evolution requirements are never touched). The name is old; the value is kept from earlier versions. Server side only. **1.0 turns it off.** |
| `sire_subordinate_link_enabled` | `true` | A sired creature or player becomes the sire's Tensura subordinate. A sired player then cannot hurt the sire or the sire's other sired (one way); owner and sired count as allied; a demon lord sire's harvest festival gifts them. **Off:** no link is made or removed. |
| `sire_naming_enabled` | `true` | Siring **names the sired player in Tensura automatically**, with their own current name (no new name). The sire pays Tensura's magicule cost, the sired gets Tensura's EP and stats, **never** a race evolution (LOW). `playerNaming=false` and the sired's own naming toggle do not block it. Tried at the link and at the next login of the sired or the sire (the sire must be online and able to pay). Needs `sire_subordinate_link_enabled`. Off: siring names nobody. |

### Default `layers`

```toml
layers = [
  "ORIGINAL=blood_mist,drain,charm,self_regeneration|0.20|0|true",
  "ORIGINAL_HYBRID=blood_mist,drain,charm,self_regeneration,beast_transformation,godwolf_sense|0.25|0|true",
  "HYBRID=drain,beast_transformation,self_regeneration|0.10|0|true",
  "WITCH=|0.30|0.10|false",
  "SIPHONER=drain|0.10|0|false",
  "HERETIC=drain,charm,self_regeneration|0.20|0|true",
  "TRIBRID=drain,beast_transformation,self_regeneration,charm|0.30|0|true",
  "HUNTER_OF_THE_FIVE=|0|0.10|false",
  "VAMPIRE=drain|0|0|true"
]
```

Valid species keys: `ORIGINAL`, `ORIGINAL_HYBRID`, `HYBRID`, `WITCH`, `SIPHONER`, `HERETIC`, `TRIBRID`, `HUNTER_OF_THE_FIVE`, `VAMPIRE`. A hunter *potential* has no layer.

### Default `rank_skills`

```toml
rank_skills = [
  "ALPHA=tensura:beast_transformation,tensura:godwolf_sense",
  "TRUE_ALPHA=tensura:beast_transformation,tensura:godwolf_sense,tensura:commander",
  "BORN_LEVEL5=tensura:godwolf_sense"
]
```

## supernatural: `supernaturalbloodlines-supernatural.toml`

| Key | Default | What it does |
|---|---|---|
| `rite_complete_signal` | `true` | A rite that finished counts for ladder tasks (the hook on Enchanted's rites). `false`: no rite counts for any task. |

The ladders themselves are datapack JSON (`data/supernaturalbloodlines/supernatural/ladder/`), not config.

## supernatural (client): `supernaturalbloodlines-hub-client.toml`

A **CLIENT** config: each player picks their own, the server never reads it. It is the one file you may edit while the game runs; an open hub picks the change up at once and stays on its page.

| Key | Default | What it does |
|---|---|---|
| `hub_theme` | `GRIMOIRE` | The [hub](Supernatural-Hub)'s look. `GRIMOIRE`: an open book. `BLOOD_MOON`: a dark screen with a red moon that fills with your ladder. `TENSURA`: Tensura's menu style, made wider; needs Tensura and the `supernaturalTensura` part, otherwise the Grimoire is shown. |

## Part data and the load condition

All data a part ships (rites, recipes, loot, tags, EP, altar blocks) is loaded only when its part is on and its mods are installed (the condition `supernaturalbloodlines:part_enabled`). Switching a part off in `[parts]` removes its data too. See [For server owners](For-Server-Owners#switching-parts-off).
