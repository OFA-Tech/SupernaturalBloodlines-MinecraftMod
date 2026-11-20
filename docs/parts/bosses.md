# Part: bosses

Requires: **vampirism and enchanted** (the species part's lines, which the NPC Originals root) and **GeckoLib** (Enchanted already requires it, so it costs nobody anything). Needs the `species` part. Plan: `docs/superpowers/plans/2026-11-19-bosses.md`. This is phase **17b**: the NPC Original base and **Mikael**. Klaus, Elijah, Kol, Rebekah, Finn and the werewolf bosses come in 17c to 17h; their switches (`<boss>_enabled`) already exist and do nothing yet.

**Mixin budget: 0 of 3** (see `docs/mixins.md`).

## What it adds

- The first **entity types** of the mod: `supernaturalbloodlines:mikael`. A plain `PathfinderMob` (not a Vampirism entity: no sun, no bite, no conversion, no Vampirism AI), never despawns, has a boss bar for players within 64 blocks. Server and client load the same classes; nothing under `parts/bosses` except `client/` names a client class (`BossServerSafetyTest` reads every import).
- **`BossIndex`** (SavedData `supernaturalbloodlines_bosses`, in the overworld's data storage): for each boss its state (`UNSEEN`, `ACTIVE`, `OPEN`, `DEAD`, `LOST`), the entity's uuid, its last dimension and place, the day it returns, whether its reward was paid, how many times it came, the day its blood may drop again, the last night rolled, and who it hunts. A bad entry is skipped, not the file.
- **`BossDirector`**: one `LevelTickEvent.Post` hook on the overworld, every `boss_check_seconds`. It marks a boss **lost** when its entity is gone from a loaded chunk for two checks in a row (an unloaded chunk says nothing), and rolls for Mikael. No vanilla spawn rules, no biome modifier. Every refusal is one table, `SpawnRules`, in this order: `disabled`, `needs_mod`, `dead`, `present`, `cooling`.
- **The NPC Original base** (`NpcOriginalBoss`): carries `ORIGINAL_ROOT` (so the line rules, `LineDeath`, `LineWarning`, the Imprisoned Original rite and Revelation treat it like a player Original) and `NPC_OPEN` (the Imprisoned Original rite's "brought down"). What a blow does is `DefeatRules` (R151), played by `NpcOriginalEvents`.
- **Mikael**: black clothes, his white oak stake in his hand. Once a night, with the chance `mikael_night_chance`, he comes for one vampire player of at least `mikael_min_rung`, on the ground, in the overworld, outside a mountain ash ring, 24 to 40 blocks away. The target is told ("A hunter in black is watching you", with his name only when they have discovered him, R123). He feeds on the vampires he hits (a quarter of the damage, R156). Peaceful: he never comes.
- **Mikael's Stake** (`mikaels_stake`): his reward and weapon. In `#supernaturalbloodlines:white_oak`, never breaks.

## The rules

| Rule | What | Test |
|---|---|---|
| R150 | The states of a boss. `DEAD` is final; only `FORGET` leaves it. A lost boss may be brought back. | `BossRulesTest` |
| R151 | White oak kills an NPC Original only while they are open (`npc_original_stake_needs_open`, on by default); on a standing one it hurts but cannot take the last health. Anything else lethal, and the Ash Dagger, desiccates a standing one (health 1, no AI, open, the line warned, their blood dropped). An open one cannot be hurt below 1 by anything but white oak. `/kill` and the void end them. | `DefeatRulesTest`, `NpcOriginalGameTests` |
| R152 | A desiccated Original gets up after `original_return_days` in-game days (at least 1), with full health, and the line is warned again the next time. The check is the entity's own, so one that was in an unloaded chunk gets up when it loads. | `DesiccationRulesTest`, `NpcOriginalGameTests` |
| R153 | A boss pays its reward once (`boss_reward_on`: `first_defeat` = on desiccation or death, `death` = only on death). A return does not pay again; a lost boss that comes back does not either. | `RewardRulesTest`, `NpcOriginalGameTests` |
| R154 | The refusals of the director, in order. | `SpawnRulesTest` |
| R155 | Who Mikael hunts: a vampire of at least the rung, at night (ticks 13000 to 22999), in the overworld, on the ground, outside an ash ring; one roll a night, below the chance. | `MikaelRulesTest`, `MikaelGameTests` |
| R156 | He heals a quarter of the damage he deals to a vampire. | `MikaelRulesTest` |
| R157 | A white oak death is for good: the index says `DEAD`, the line dies with them (`LineDeath`, if `original_line_death` is on), they never come back until an operator resets them. | `NpcOriginalGameTests` |

White oak working only while the Original is open is the rule the plan named; the stake and the Ash Dagger are the two tools of the story: the dagger opens them, the stake ends them.

## Where it is in the hub

- **Admin page, section Bosses** (operators only): a boss picker and the five commands as buttons: Spawn here (X1), State (X2), Desiccate (X3), Return now (X4), Forget (X5, asks first). The settings (`boss_check_seconds`, `boss_bars_enabled`, `original_return_days`, `npc_original_stake_needs_open`, `boss_reward_on`, `mikael_enabled`, and the other eight `<boss>_enabled`, `boss_health_factor`, `boss_damage_factor`, `mikael_min_rung`, `mikael_night_chance`) are on the same page with the other settings (`AdminEntries`; `AdminCoverageTest` fails when a key has no row).
- **Guide**: a chapter "The bosses", last, shown when the part is on.
- **Home, "The world now"**: a line when a boss is out there or down within 128 blocks (`WorldNowHook`, the snapshot's `bossNear`; the hub protocol is now 6).

## Commands

`/supernaturalbloodlines boss <boss> spawn|state|desiccate|return|reset` (permission 2). `<boss>` suggests only the bosses that have landed. `spawn` refuses with the reason from `SpawnRules` (a boss that is already out there is not made twice).

## Layout

- `core/boss/` (pure): `BossId`, `BossState`, `BossRules`, `DefeatRules`, `DesiccationRules`, `RewardRules`, `BossFacts`, `SpawnRules`, `BossWeight`, `MikaelRules`.
- `parts/bosses/`: `BossesPart`, `BossesConfig`, `BossRegistries` (entity types, attributes), `BossItems`.
- `parts/bosses/director/`: `BossIndex`, `BossDirector`, `BossBars`. `entity/`: `BossNpc`, `NpcOriginalBoss`, `MikaelEntity`, `NpcOriginalEvents`, `ai/NightHuntGoal`. `command/BossCommand`. `client/`: `BossRenderers`, `HumanoidBossRenderer` (player model with `textures/entity/boss/<id>.png`; a desiccated one lies flat), `BossShots`.
- Hooks elsewhere: `SpeciesRegistries.NPC_OPEN` and `Imprisonment.isDown`, `WorldNowHook.setBossNear`, `GuideChapters`, `AdminPage`/`AdminEntries`/`AdminCommands`.

## Tests

- Pure: `BossIdTest`, `BossRulesTest`, `DefeatRulesTest`, `DesiccationRulesTest`, `RewardRulesTest`, `SpawnRulesTest`, `MikaelRulesTest`, `BossServerSafetyTest`, and the guide, admin and wire tests that changed with the part. Each was shown to fail with its rule reverted (a script of mutants, listed in the PR).
- GameTests: `NpcOriginalGameTests` (15), `MikaelGameTests` (15), `BossServerGameTests` (3), `BossesPartOffGameTests` (1: off leaves no entity, item or command).
- **Dedicated server (U6)**: the GameTest run is a dedicated server (`FMLEnvironment.dist == DEDICATED_SERVER`, no client classes), and `BossServerGameTests` spawns Mikael, ticks him, saves and loads him there.
- Hidden screenshots: `-Dsupernaturalbloodlines.devshots.bosses=mikael` (`BossShots`), with `runSafeShots`: Mikael from the front, the side, behind and desiccated. See `docs/safe-screenshots.md`.
