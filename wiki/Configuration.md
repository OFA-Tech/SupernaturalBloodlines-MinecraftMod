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
| `hud` | `true` | [hud](Ability-HUD) | none (Tensura, Vampirism and Werewolves are optional extras) |
| `celestials` | `true` | [celestials](Enhanced-Celestials) | none (Enhanced Celestials 2 is optional; without it the part does nothing) |

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
| `max_stacked_sparks` | `5` | 1 to 20 | The most [Alpha sparks](Werewolves-And-Packs#stacked-sparks) one werewolf can hold (an Alpha who kills an Alpha from outside their pack takes the victim's on top of their own). Each past the first adds 10% to the Alpha bonuses and a tier to the Roar's Strength. 1 turns it off. **Lowering it clamps the effects at once**; a holder above the new cap drops to it at their next steal. |
| `duel_reconnect_seconds` | `60` | 0 to 600 | A duelist who loses their connection during a [leadership duel](Werewolves-And-Packs#mixed-packs-and-alpha-packs) has this many seconds to come back: the duel is paused (its 2 minute clock stops) and the other one is told. After that they forfeit it. **0 is the old rule: a disconnect is an instant forfeit.** A server that is stopping settles nothing. |
| `left_pack_grace_minutes` | `10` | 0 to 1440 | Someone who left a pack **by choice**, with `/pack leave` or by leaving its party, in the last this many minutes still counts as a member of it for what a kill does: no spark steal, no blue eyes, no gene awakening, no Hunter mark. Leaving just before a kill gains nothing, and neither does joining another pack. **Someone who was kicked, and the members of a pack that dissolved, get no grace.** Kept in memory: a restart forgets it. 0 turns it off. |
| `absent_leader_days` | `7` | 0 to 3650 | A pack leader offline this many real days loses the lead to the strongest second Alpha **who is here** (online, or seen within the same days), who keeps their spark and stays a second Alpha. The time the server was off does not count and a restart does not erase an absence. 0 turns it off. |
| `opac_link_enabled` | `true` | | With Open Parties and Claims (or Team Claims) installed: a pack gets a party of its own and leaving the party leaves the pack. Only a party the pack made itself is ever touched. **Upgrading:** packs that exist already get their party on the first run, so set this to `false` **before** upgrading to opt out. Turning it off later stops the link but **does not remove** the parties it made. |
| `only_alphas_turn` | `true` | | A werewolf player below Alpha cannot infect a human (R92, the Teen Wolf rule). **Off:** Werewolves' own rule: any werewolf's bite can infect (it then records no pack and no sire). |
| `witch_bonus_enabled` | `true` | | Witches, heretics and tribrids get **altar range x1.5** and **rite power cost x0.75**. Siphoned power is always usable. |
| `siphoner_chance` | `0.25` | 0.0 to 1.0 | Chance that the Rite of Ancestral Calling makes a **siphoner** instead of a witch. |
| `original_line_death` | `true` | | When an Original is killed with white oak, everyone in their line dies with them (offline players on their next login). Replaces `sireline_death`. |
| `original_visibility` | `discovered` | `discovered`, `always` | Whether a player sees the Original of their line (`?` until the Rite of Revelation) or always. |
| `line_rites_enabled` | `true` | | Switches the Rite of the Broken Line and the Rite of the Imprisoned Original on or off. |
| `npc_line_weights` | all `1` | `name=weight` | How likely each Original is the root of a new NPC vampire's line. |
| `imprisonment_ring_max_radius` | `8` | 2 to 16 | The mountain ash circle of the Imprisoned rite must close within this many blocks. |
| `imprisonment_rite_seconds` | `30` | 5 to 300 | How long the Original must stay trapped. |
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
| `undead_treatment_enabled` | `true` | **Plainly:** on, a vampire takes **x3** holy damage from Tensura and gets Tensura's undead rules (infection immunity, Enervation from food); off, they take **x1.5** (the `tensura_holy_vs_vampire` setting) and none of those. Tensura treats a player whose layer says undead as undead (holy and light damage x3, holy exposure, infection immunity, Enervation on food...). A plain Vampirism vampire counts too, so `tensura_holy_vs_vampire` has no effect on it. **Off:** layers still grant skills and bonuses, and Tensura's undead and holy-exposure answers are "no" for **our apex races as well** (they are in Tensura's `tensura:undead` race tag through a data file, and a tag cannot carry a load condition in this NeoForge, so the answers are switched in code). A mod that reads that tag directly, or Tensura's `no_blood` race tag (which includes `#tensura:undead`; Tensura's glow-colour check reads it), still sees the apex races there. To take them out of the tag for good, add a datapack with `data/tensura/tags/manascore_race/races/undead.json` set to `"replace": true` and Tensura's own list: `tensura:ghoul`, `tensura:wight`, `tensura:wight_king`, `tensura:spirit_skeleton`, `tensura:divine_skeleton`. |
| `apex_evolutions_enabled` | `true` | Offer the species' apex race in Tensura's evolution screen. Off: no apex option; species keep working. **If it differs between client and server, the client shows the option and the server silently refuses the pick** (nothing is logged). |
| `evolution_progress_multiplier` | `1.25` (1.0 to 10.0) | A player with a species gets this multiple of the EP Tensura credits them (**EP gain only**: evolution requirements are never touched). The name is old; the value is kept from earlier versions. Server side only. **1.0 turns it off.** |
| `sire_subordinate_link_enabled` | `true` | A sired creature or player becomes the sire's Tensura subordinate. A sired player then cannot hurt the sire or the sire's other sired (one way); owner and sired count as allied; a demon lord sire's harvest festival gifts them. **Off:** no link is made or removed. |
| `sire_naming_enabled` | `true` | Siring **names the sired player in Tensura automatically**, with their own current name (no new name). The sire pays Tensura's magicule cost, the sired gets Tensura's EP and stats, **never** a race evolution (LOW). `playerNaming=false` and the sired's own naming toggle do not block it. Tried at the link and at the next login of the sired or the sire (the sire must be online and able to pay). Needs `sire_subordinate_link_enabled`. Off: siring names nobody. |
| `severance_clears_name` | `true` | The Rite of Severance also clears the sired player's Tensura name when the severed sire is their Tensura owner (the EP and stats the naming gave stay), so someone else can name them. Off: the owner goes and the name stays. |
| `resire_naming_reward` | `true` | A sire naming a player whose name a severance cleared pays Tensura's reward again. Off: the re-naming still names the player and costs the sire as usual, but gives the sired no EP and stats a second time. |

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

## hud: `supernaturalbloodlines-hud.toml` and `supernaturalbloodlines-hud-client.toml`

Common (server):

| Key | Default | What it does |
|---|---|---|
| `rate_limit_per_second` | `10` | Most HUD payloads one player may send per second. |
| `bite_reach` | `4.0` | How far (blocks) the [Bite action](Ability-HUD#the-bite-action) reaches, from 1 up to 16. Vampirism's own drinking is also limited to the player's interaction range (3), so more only helps the other bites. |
| `[mixins]` `next_preset_active` | `true` | With Tensura, a skill in the next preset (the right column) can be fired. **It applies to every player on the server**: with it on, the skills in preset N+1 count as equipped for passives and for activation, so a player has **6 equipped skills instead of 3** (the same thing Tensura Slots does today). Off: that stops, and Tensura refuses a skill in the next preset again (unless Tensura Slots is installed). Read each time a check runs, so no restart. |
| `[mixins]` `tensura_feed_fills_blood` | `true` | With Tensura and Vampirism, using Tensura's vampire race ability on a creature also fills your Vampirism blood (and takes it from the victim), and a thirsty vampire can use it even when healthy. Off: Tensura's ability works exactly as before and gives no blood. |
| `[mixins]` `hide_in_controls_mixin` | `true` | Reserved: guards a mixin that does not exist yet. |

Client (each player's own): `hud_enabled` (`true`), `hide_covered_keys` (`true`), `hide_tensura_skill_hud` (`true`), `blood_in_food_spot` (`true`), `tracker_anchor` (`TOP_CENTER`), `mode` (`AUTO`, `TENSURA` or `SUPERNATURAL`). `hide_covered_keys` is live: it unbinds and hides the [covered keys](Key-Binds#covered-keys-and-how-to-get-them-back) and, when turned off, gives your recorded binds back (the record is `config/supernaturalbloodlines-hud-keys.json`). `hud_enabled` turns the whole ability HUD (slots, keys, the blood in Tensura's food spot) on or off; `hide_tensura_skill_hud` hides Tensura's own skill HUD while ours is shown; `blood_in_food_spot` shows a vampire's blood in Tensura's food spot (see [Ability HUD](Ability-HUD)); `mode` picks the mode the HUD starts in.

## celestials: `supernaturalbloodlines-celestials.toml`

Only does anything with [Enhanced Celestials 2](Enhanced-Celestials). A factor of `1` turns that effect off. All of them are on the hub's Admin page (section Celestials).

| Key | Default | What it does |
|---|---|---|
| `blood_moon_bite_bonus` | `1.25` | A vampire's bite gives this many times the blood on a Blood Moon (1 to 5). |
| `blood_moon_pack_chance_factor` | `0.5` | A Blood Moon night that is not a full moon spawns wild werewolf packs at this share of the full-moon chance (0 to 1). |
| `blue_moon_rite_cost` | `0.8` | Altar power cost of every rite on a Blue Moon, as a factor (0.1 to 1). It never allows a full-moon rite on another night. |
| `super_moon_rite_cost` | `0.75` | Cost of the Rite of the Moon, the Silver Moon and Immortality on a super moon, on top of the Blue Moon factor (0.1 to 1). |
| `super_blue_skips_forced_shift` | `true` | A Super Blue Moon does not force the Werewolves full-moon shift on anybody. |

## bosses: `supernaturalbloodlines-bosses.toml`

Needs the species part and GeckoLib. All of them are on the hub's Admin page (section Bosses). Only Mikael is in the game so far; the other `<boss>_enabled` switches are there for the next phases and do nothing yet.

| Key | Default | What it does |
|---|---|---|
| `boss_check_seconds` | `5` | How often the boss director looks at the world (1 to 120). |
| `boss_bars_enabled` | `true` | A boss bar for players within 64 blocks of a boss. |
| `original_return_days` | `5` | In-game days a desiccated NPC Original stays down before they get up again (1 to 100). Their blood drops at most once per window. |
| `npc_original_stake_needs_open` | `true` | White oak kills an NPC Original only while they are desiccated. Off: it kills a standing one too. |
| `boss_reward_on` | `first_defeat` | When a boss pays its reward, once: `first_defeat` (brought down or killed) or `death` (only when it dies). |
| `mikael_enabled`, `klaus_enabled`, `elijah_enabled`, `kol_enabled`, `rebekah_enabled`, `finn_enabled`, `deucalion_enabled`, `peter_hale_enabled`, `beast_of_gevaudan_enabled` | `true` | Whether that boss can appear. |
| `boss_health_factor` | `1.0` | Every boss's health is multiplied by this (0.1 to 10). |
| `boss_damage_factor` | `1.0` | Every boss's attack damage is multiplied by this (0.1 to 10). |
| `mikael_min_rung` | `3` | The lowest rung of the vampire ladder (0 to 6) at which Mikael hunts a vampire player. |
| `mikael_night_chance` | `0.15` | The chance, once each night, that Mikael comes for a vampire player of that rung. |

## supernatural (client): `supernaturalbloodlines-hub-client.toml`

A **CLIENT** config: each player picks their own, the server never reads it. It is the one file you may edit while the game runs; an open hub picks the change up at once and stays on its page.

| Key | Default | What it does |
|---|---|---|
| `hub_theme` | `GRIMOIRE` | The [hub](Supernatural-Hub)'s look. `GRIMOIRE`: an open book. `BLOOD_MOON`: a dark screen with a red moon that fills with your ladder. `TENSURA`: Tensura's menu style, made wider; needs Tensura and the `supernaturalTensura` part, otherwise the Grimoire is shown. |

## Part data and the load condition

All data a part ships (rites, recipes, loot, tags, EP, altar blocks) is loaded only when its part is on and its mods are installed (the condition `supernaturalbloodlines:part_enabled`). Switching a part off in `[parts]` removes its data too. See [For server owners](For-Server-Owners#switching-parts-off).

### Moon rules (plan 15a)

| Key | Default | Values | What it does |
|---|---|---|---|
| `full_moon_sleep` | `off` | `off`, `werewolves`, `everyone` | Who cannot use a bed in the Overworld from dusk on a full-moon night. Admin > Species. |
| `npc_packs_enabled` | `true` | | Wild NPC werewolf packs roam on full-moon nights. |
| `npc_pack_chance` | `0.35` | 0 to 1 | Chance per player per full-moon night that a pack spawns near them (a Blood Moon night rolls at `blood_moon_pack_chance_factor` of it). |
| `npc_lone_wolf_chance` | `0.5` | 0 to 1 | The same, for a lone wolf, apart from the pack. |
| `npc_pack_alpha_chance` | `0.7` | 0 to 1 | Chance a pack has an Alpha (the one that can drop a weaker spark). |
| `npc_pack_min_size`, `npc_pack_max_size` | `3`, `5` | 1 to 8 | Wolves in a pack, not counting the Alpha (it leads only as many as the Werewolves mod lets it). |
| `npc_max_packs_per_night` | `3` | 0 to 20 | Most packs one dimension spawns in a night. |
| `npc_pack_dawn_despawn` | `true` | | Wild wolves with no player within 24 blocks leave at dawn. |
| `feral_attack_factor`, `feral_speed_factor` | `1.2`, `1.1` | 1 to 3, 1 to 2 | A wild werewolf in the dark on a full-moon night. 1 = no change. |
| `npc_alpha_spark_enabled` | `true` | | Killing an Alpha of the Werewolves mod gives a werewolf a weaker spark. |
| `weak_spark_grow_days` | `3` | 1 to 30 | Online days a weaker spark takes to grow. |
| `weak_spark_max_held` | `2` | 1 to 5 | Weaker sparks one player may hold. |
| `weak_spark_cooldown_days` | `3` | 0 to 30 | Game days between two weaker sparks for one player. |
| `moonblood_surge_enabled`, `wolf_spirits_enabled` | `true` | | The two pack abilities. |
| `npc_recruit_enabled` | `true` | | The Werewolf Brother rite works. |
| `max_npc_members_per_player`, `max_npc_members_per_pack` | `2`, `6` | 0 to 8, 0 to 24 | Brothers one werewolf may keep, and a whole pack. |
| `npc_hunt_trips`, `npc_chest_sort` | `true` | | A Brother with Hunt on hunts on quiet nights; a Brother with Sort on tidies the chest by its post. |
| `npc_hunt_period_minutes` | `10` | 1 to 120 | Minutes between two hunting trips of one Brother. |
| `npc_weight_brother`, `npc_weight_tensura` | `0.5`, `0.25` | 0 to 1 | What a Brother and a Tensura subordinate count toward the pack bonus (a Beta is 1). |
| `npc_bonus_share` | `0.4` | 0 to 1 | The most of the bonus the wolves may give, as a share of its cap. |
| `tensura_subordinates_join`, `max_tensura_members_per_pack` | `true`, `6` | 0 to 24 | In `supernaturalbloodlines-species_tensura.toml`: the subordinates of pack members join the pack, up to the cap. |
| `delayed_awakening_nights` | `1` | 0 to 3 | A human's werewolf gene awakens at the next dusk (N dusks later) after the kill, not at once. 0 = at once. Admin > Species. |
