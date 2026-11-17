# UI-first audit (2026-11-17)

Owner rule: every command, config setting and gamerule that OUR mod adds must also work from the hub, so players never type commands. Admin-only actions and settings (debug commands, common/server config) show only to OPs (permission level >= 2) and are checked again on the server. Scope: our features only, not vanilla or other mods.

Audited at `main` 201c6d7, NeoForge 21.1.265, Minecraft 1.21.1. Read-only: no Gradle, no client.

## Totals

| Thing | Count | Hub already has it | Partly / broken | No UI |
|---|---|---|---|---|
| Brigadier command forms (3 roots + `/sb` aliases, see section 1) | 32 (20 player, 12 OP) | 8 | 2 | 22 |
| Config keys, COMMON | 60 | 0 | 0 | 60 |
| Config keys, CLIENT | 7 | 1 (`hub_theme`) | 1 (`mode`, by key only) | 5 |
| Config keys, SERVER | 0 | - | - | - |
| Gamerules we register | 0 | - | - | - |
| Data-only knobs (ladder/skill/tree/quest JSON, tags, data maps) | 8 families | 0 | 0 | 8 (datapack, out of live scope) |

`/sb` is a redirect alias for `/bloodline` and `/pack` only. The OP root `/supernaturalbloodlines` has no `/sb` alias and does not need one.

Hub today (`HubPage`): PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK, GUIDE, LOADOUT. No admin or settings page. Player buttons send the typed command with `connection.sendCommand` (`BloodlinePage.command`, `PackPage.command`), so Brigadier re-checks them on the server.

## 1. Commands

Source: `BloodlineCommand`, `PackCommand`, `EclipseCommand`, `DebugCommand`. "Perm" is the Brigadier gate; rules like "Alpha only" are checked inside the action and refuse with a chat message.

### `/bloodline` and `/sb bloodline` (part `species`)

| Id | Form | Perm | Hub today | Who should see it | Needed |
|---|---|---|---|---|---|
| B1 | `tree` (own) | 0 | Yes: Bloodline page tree (data from `HubSnapshotPayload`) | player | none |
| B2 | `tree <player>` | 0 for self, **2** for others (`BloodlineCommand` line 90) | No | **OP only** | Admin page: player picker, "Show bloodline tree" |
| B3 | `call` | 0 (Original, hybrid, tribrid) | Yes: Bloodline page `bloodline:call` stamp and the detail action `call` (`LinePageSupport` 128) | player | none |
| B4 | `accept <token>` | 0 | No. Only the chat `[Accept]` click (`SireCall` 143) | player | Hub "Requests" card: pending call for me, `[Accept]` |
| B5 | `compel <token> discount` | 0 | No. Only the chat click, token lives 5 s (`Compulsion` 202) | player | Hub card with a countdown, `[Discount]` |
| B6 | `compel <token> follow` | 0 | No. Same | player | Same card, `[Follow]` |
| B7 | `gene doppelganger <player> <bool>` | **2** | No | **OP only** | Admin debug action with player picker and toggle |

### `/pack` and `/sb pack` (part `species`, only with Werewolves)

| Id | Form | Perm | Hub today | Who should see it | Needed |
|---|---|---|---|---|---|
| P1 | `invite <player>` | 0 (leader) | No. Pack page only prints the hint "type /pack invite" (`invite_hint`, `book.pack.invite`) | player (leader) | Online-player picker plus Invite button; replaces the hint |
| P2 | `accept <token>` | 0 | **Broken**: button `pack:accept` (`PackPage` 62, `BookPackPage` 267/288) sends `pack accept` with no token, but the command needs `<token>` (`PackCommand` 63-65). Not run in a client; read from the code | player | Fix (see plan task 2): tokenless accept resolves the player's own open invitation |
| P3 | `leave` | 0 | Yes: Pack page `pack:leave` (two presses on `PackPage`, confirm dialog in the Grimoire) | player | none |
| P4 | `kick <player>` | 0 (leader) | Yes: Grimoire `BookPackPage` link and detail action | player (leader) | none |
| P5 | `ally invite <player>` | 0 (leader) | No (hint text only) | player (leader) | Picker plus button |
| P6 | `ally accept <token>` | 0 | No (chat click `PackCommand` 324 only) | player (leader) | Requests card |
| P7 | `ally remove <player>` | 0 (leader) | Yes: `pack:ally_end` with confirm | player (leader) | none |
| P8 | `challenge <leader>` | 0 (second Alpha) | Yes (`BookPackPage` 242/296) | player | none |
| P9 | `challenge accept` | 0 | Yes (`duel_accept`) | player | none |
| P10 | `challenge decline` | 0 | Yes (`duel_decline`) | player | none |
| P11 | `heal <player>` | 0 (Alpha) | No | player (Alpha) | Picker (within 4 blocks) plus Heal with confirm (gives up the spark) |
| P12 | `info` | 0 (member) | Partly: member list with ranks, emissary and ally rows from the snapshot; "your anchor" is not shown | player | Add anchor line to Pack page |
| P13 | `emissary [player]` | 0 (leader) | No | player (leader) | Member picker, Set and Clear |
| P14 | `roar` | 0 (Alpha) | Key bind only (`key.supernaturalbloodlines.alpha_roar`, `PackKeybinds`) | player (Alpha) | Pack page Roar button with cooldown text |
| P15 | `anchor [player]` | 0 (werewolf) | No | player (werewolf) | Picker, Set and Clear |

### `/supernaturalbloodlines debug ...` (root `.requires(hasPermission(2))`, no `/sb` alias)

`DebugCommand` is core, always on. `EclipseCommand` is registered by `LunarEclipse` (needs `species` plus Werewolves).

| Id | Form | Perm | Hub today | Who should see it | Needed |
|---|---|---|---|---|---|
| D1 | `debug get <player>` | 2 | No | OP only | Admin debug: shows bloodline state |
| D2 | `debug set <player> <species>` | 2 | No | OP only | Species picker (`Species` enum) |
| D3 | `debug event <player> <event>` | 2 | No | OP only | Event picker (`BloodlineEvent` enum, 23 values) |
| D4 | `debug supernatural <player> show` | 2 | No | OP only | Admin debug |
| D5 | `debug supernatural <player> backfill` | 2 | No | OP only | Admin debug |
| D6 | `debug supernatural <player> reset` | 2 | No | OP only | Admin debug with confirm (destroys ladder progress) |
| D7 | `debug supernatural <player> sp <n>` | 2, n in -1,000,000..1,000,000 | No | OP only | Number control |
| D8 | `debug supernatural <player> rung <race> <n>` | 2, n in 0..20 | No | OP only | Race picker (`SupernaturalRace`) plus number |
| D9 | `debug eclipse start` | 2 | No | OP only | Admin debug |
| D10 | `debug eclipse stop` | 2 | No | OP only | Admin debug |

Counts: B 7 + P 15 + D 10 = 32. Covered: B1, B3, P3, P4, P7, P8, P9, P10 = 8. Partly or broken: P2, P12 = 2. None: 22.

## 2. Config keys

All of ours use `ModConfigSpec` with `ModConfig.Type.COMMON` (files in `config/`, not synced) or `CLIENT`. No `SERVER` spec. No key calls `worldRestart()` or `gameRestart()` (grep over `src`), so every `ValueSpec.restartType()` is `NONE`.

Live column: **live** = the code calls `.get()` at the point of use (not cached by us), so a `set` plus `save` takes effect on the next use. **restart** = read once at startup. **live (client copy!)** = the key is a COMMON key the client reads from its own file.

Hub today is "No" for every COMMON key. The only config UI is `hub_theme` (theme button) and `mode` (key).

### 2.1 `supernaturalbloodlines-common.toml` (`core/Config`, no custom file name)

| Id | Key | Type, range, default | Live | Who |
|---|---|---|---|---|
| C1-C9 | `parts.<enchantedTensura, vampirismEnchanted, vampirismTensura, species, speciesTensura, artifacts, supernatural, supernaturalTensura, hud>` | boolean, true | **restart** (`EarlySwitches` reads the toml before config load; mixin plugins and part registration use it) | OP only, marked "Restart the server" |
| C10 | `werewolves.full_form_level` | int 1..100, 8 | live (`MoonControl` 102) | OP |
| C11 | `werewolves.moon_bound_werewolves` | boolean, true | live (`MoonControl` 115) | OP |

### 2.2 `supernaturalbloodlines-vampirism_tensura.toml` (`VampireSyncConfig`)

| Id | Key | Type, range | Live | Who |
|---|---|---|---|---|
| V1 | `cure_race` | string, must parse as a `ResourceLocation` | live | OP |
| V2 | `race_level_floor` | list of `"race=level"` (level >= 1 or -1) | live (`mapping()` parses per call) | OP, list editor, validated with `VampireMapping.parse` |
| V3 | `werewolf_skill_grants` | list of `"level=skillId"` | live | OP, list editor, `SkillGrantSpec.parse` |
| V4 | `werewolf_beastfolk_enabled` | boolean, true | live | OP |
| V5 | `tensura_holy_vs_vampire` | double 0.0..100.0, 1.5 | live (`HolyDamageBridge` 47) | OP |

### 2.3 `supernaturalbloodlines-supernatural.toml` (`SupernaturalConfig`)

| Id | Key | Type, range | Live | Who |
|---|---|---|---|---|
| S1 | `rite_complete_signal` | boolean, true | live | OP |
| S2 | `vampire_feed_blood` | int 1..20, 3 | live (`BloodEffects` 83) | OP |

### 2.4 `supernaturalbloodlines-hud.toml` (`HudConfig`, COMMON)

| Id | Key | Type, range | Live | Who |
|---|---|---|---|---|
| H1 | `rate_limit_per_second` | int 1..100, 10 | live (`HudNet` 70) | OP |
| H2 | `bite_reach` | double 1.0..16.0, 4.0 | live (`BiteService` 121, `FactionActions` 81) | OP |
| H3 | `mixins.next_preset_active` | boolean, true | live (read when the check runs) | OP |
| H4 | `mixins.tensura_feed_fills_blood` | boolean, true | live | OP |
| H5 | `mixins.hide_in_controls_mixin` | boolean, true | **reserved, does nothing** (its own comment) | hide; allow-list entry `hidden` with a reason |

### 2.5 `supernaturalbloodlines-artifacts.toml` (`ArtifactsConfig`)

| Id | Key | Type, range | Live | Who |
|---|---|---|---|---|
| A1 | `artifacts.compass_doppelganger_range` | int 1..30,000,000, 512 | live (`VampireCompassItem` 148) | OP |

### 2.6 `supernaturalbloodlines-species.toml` (`SpeciesConfig`, all under `species.`)

| Id | Key | Type, range, default | Live | Who |
|---|---|---|---|---|
| X1 | `max_originals_per_world` | int 0..10,000, 0 | live | OP |
| X2 | `transition_ticks` | int 20..24,000,000, 24000 | live | OP |
| X3 | `vampire_blood_ticks` | int 20..24,000,000, 24000 | live | OP |
| X4 | `bite_vampire_cost` | int 0..20, 1 | live | OP |
| X5 | `max_five` | int 1..10,000, 5 | live | OP |
| X6 | `only_alphas_turn` | boolean, true | live | OP |
| X7 | `max_stacked_sparks` | int 1..20, 5 | live | OP |
| X8 | `duel_reconnect_seconds` | int 0..600, 60 | live | OP |
| X9 | `left_pack_grace_minutes` | int 0..1440, 10 | live | OP |
| X10 | `absent_leader_days` | int 0..3650, 7 | live | OP |
| X11 | `opac_link_enabled` | boolean, true | live | OP |
| X12 | `witch_bonus_enabled` | boolean, true | live | OP |
| X13 | `siphoner_chance` | double 0.0..1.0, 0.25 | live | OP |
| X14 | `original_line_death` | boolean, true | live | OP |
| X15 | `original_visibility` | one of `discovered`, `always` | live | OP |
| X16 | `line_rites_enabled` | boolean, true | live | OP |
| X17 | `npc_line_weights` | list of `"name=weight"` | live | OP, list editor |
| X18 | `imprisonment_ring_max_radius` | int 2..16, 8 | live | OP |
| X19 | `imprisonment_rite_seconds` | int 5..300, 30 | live | OP |
| X20 | `latent_gene_chance` | double 0.0..1.0, 0.15 | live (rolled at first login) | OP |
| X21 | `alpha_bite_death_chance` | double 0.0..1.0, 0.10 | live | OP |
| X22 | `lunar_eclipse_chance` | double 0.0..1.0, 0.03 | live | OP |
| X23 | `doppelganger_gene_chance` | double 0.0..1.0, 0.05 | live | OP |
| X24 | `doppelganger_villager_chance` | double 0.0..1.0, 0.005 | live | OP |
| X25 | `gene_visibility` | one of `discovered`, `always` | live | OP |
| X26 | `doppelganger_rite_taglock` | one of `doppelganger`, `human` | live | OP |
| X27 | `hybrid_transition_blood` | one of `original_hybrid`, `original` | live | OP |

### 2.7 `supernaturalbloodlines-species_tensura.toml` (`SpeciesTensuraConfig`)

| Id | Key | Type, range | Live | Who |
|---|---|---|---|---|
| T1 | `layers` | list of `"SPECIES=skill,skill\|mag\|aura\|undead"` | live (`Cached` re-parses when the list instance changes); **client copy** | OP, list editor, validated with `LayerTable.parse` |
| T2 | `rank_skills` | list of `"RANK=skill,skill"` | live | OP, list editor, `RankSkillTable.parse` |
| T3 | `undead_treatment_enabled` | boolean, true | live; **client copy**: Tensura's undead checks also run on each client's own file | OP, warn "also set it on clients" |
| T4 | `apex_evolutions_enabled` | boolean, true | live; **client copy** (evolution screen is built on the client) | OP, same warning |
| T5 | `evolution_progress_multiplier` | double 1.0..10.0, 1.25 | live (refreshed within 10 s) | OP |
| T6 | `sire_subordinate_link_enabled` | boolean, true | live | OP |
| T7 | `sire_naming_enabled` | boolean, true | live | OP |
| T8 | `severance_clears_name` | boolean, true | live | OP |
| T9 | `resire_naming_reward` | boolean, true | live | OP |

### 2.8 CLIENT configs (each player's own, never sent to the server)

| Id | File, key | Type | Hub today | Who |
|---|---|---|---|---|
| U1 | `hub-client.toml` `hub_theme` | enum GRIMOIRE, BLOOD_MOON, TENSURA | **Yes**: theme button, menu of looks (`HubClientConfig.setTheme`) | player |
| U2 | `hud-client.toml` `hud_enabled` | boolean | No | player |
| U3 | `hide_covered_keys` | boolean | No | player |
| U4 | `hide_tensura_skill_hud` | boolean | No | player |
| U5 | `blood_in_food_spot` | boolean | No | player |
| U6 | `mode` | enum AUTO, TENSURA, SUPERNATURAL | Partly: the mode key writes it (`rememberMode`) | player |
| U7 | `tracker_anchor` | enum TOP_CENTER, TOP_RIGHT, TOP_LEFT, ABOVE_MODE_PILL | No | player |

Counts: COMMON = 11 + 5 + 2 + 5 + 1 + 27 + 9 = 60. CLIENT = 7. Total 67. Of the 60, one is reserved (H5) and nine need a restart (C1-C9); five are string lists (V2, V3, X17, T1, T2); four are string-enum lists (X15, X25, X26, X27); one is a validated string (V1).

## 3. Gamerules

None registered. `GameRules.register` has no hit in `src`. The only `GameRules` use is a read of Tensura's `TensuraGameRules.PLAYER_NAME` (`SireNaming` 90), which is Tensura's rule, out of scope. The plan still builds the edit path for a gamerule entry, with zero entries today, and the registry test fails if one is added without an entry.

## 4. Data-only knobs

These are datapack JSON under `src/main/resources/data`. A server owner changes them by shipping a datapack and running `/reload`; they are not runtime values, so they have no hub control and are listed only so nobody thinks they were missed.

| Knob | Where | UI |
|---|---|---|
| Ladders (12 files), quests (1), trees (11), skills (41) | `data/supernaturalbloodlines/supernatural/{ladder,quest,tree,skill}` read by `LadderReloadListener`, `SkillReloadListener`, synced as `LadderTablePayload` and `SkillTablePayload` | none; Admin page shows a read-only line "N ladders, M skills loaded" and a Reload note |
| Tags: `entity_type/line_exempt`, `supernatural_mobs`; `item/anchor`, `blood_drinks`, `silver_blades`, `silver_ingots`, `vampire_blood`, `white_oak`, `wolfsbane/common`; `manascore_race/races/bitable` | `data/supernaturalbloodlines/tags` | none |
| Tensura race tags `human_like`, `undead` | `data/tensura/tags/manascore_race/races` | none |
| Altar blocks, circle magic, recipes, loot, worldgen, damage types | `data/*` | none, content not settings |
| `part_enabled` condition | `PartEnabledCondition`, follows C1-C9 | none |

## 5. Findings that shape the plan

1. **Probable defect, P2.** The hub's `[Accept]` button runs `pack accept` with no token, and `/pack accept` only exists as `accept <token>`. It should answer "incorrect argument". Fix it as part of this plan; check it in a client first.
2. **Config `set` does not validate.** `ConfigValue.set(T)` writes to the loaded config and, for `RestartType.NONE`, updates the cache. It does not fire events and does not write the file (source: `ModConfigSpec.java` 1264-1273). Validation must come from `getSpec().test(value)` (public; includes the range) before `set`.
3. **Saving.** `ModConfigSpec.save()` calls `LoadedConfig.save()`, which writes the file and then fires `ModConfigEvent.Reloading` under the config lock (verified with `javap -c` on FML loader 4.0.45). `ConfigWarning` already listens to `Reloading`, so list edits will log and warn ops through the same path.
4. **Restart.** In NeoForge 21.1.265 `RestartType` is `NONE`, `WORLD`, `GAME`; only `NONE` refreshes the cache on `set` and `afterReload`. We declare none, so every key is `NONE` to NeoForge. The real restart cases are ours: C1-C9 are read by `EarlySwitches` straight from the toml before any spec loads. Mark exactly those "Restart the server (or the game, in single player)".
5. **Client copies.** COMMON files are per JVM, not synced. A change made on a dedicated server by an OP reaches the server's copy only. T1, T3 and T4 are documented as needing the same value on clients. Open question for the owner (see plan).
6. **No config screen.** The mod registers no `IConfigScreenFactory`, so nothing in the mod list opens a config either.
7. **Permission checks already in code.** `hasPermission(2)` on the debug root and `gene`; `ConfigWarning.OP_LEVEL = 2`. The plan adds one shared pure `AdminAccess` for the new payload.
8. **Hub key `1` to `8`.** `FocusOrder.pageForKey` jumps to pages 1 to 8. The plan adds Settings (key 9) and Admin (10th, tab or click only).
