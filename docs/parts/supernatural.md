# Part: supernatural

Requires: `vampirism`. Optional: `enchanted` (rite tasks), `werewolves` (level steps and pack tasks); a task whose mod is missing is dropped from its rung. Package: `net.ofatech.supernaturalbloodlines.parts.supernatural`. Config key `supernatural`, config file `supernaturalbloodlines-supernatural.toml` (empty for now). Plan: `docs/superpowers/plans/2026-11-14-supernatural-system.md` (phase 12a). Spec: `docs/superpowers/specs/2026-11-14-supernatural-system-design.md`.

**Mixin budget: 0 of 3 used.** One is reserved for an `@Inject` on Enchanted's `Rite` completion (phase 12a Task 3, `require = 0`, config switch), added only if Enchanted fires no event.

## What it holds

- **The race** (`core/supernatural/SupernaturalRace`): computed, never stored. `creature(BloodlineState, factionId)` is the first match of the spec table (Ghoul, Original hybrid, Original, Tribrid, Hybrid, Heretic, Hunter, Vampire, Werewolf, Human); `magic(BloodlineState)` is Witch or Siphoner or nothing. Tribrids and heretics have no magic race. No game classes.
- **The attachment** `supernaturalbloodlines:supernatural` (`SupernaturalState`, own codec, copied on death). Fields: `schema`, `rungs` (high-water mark per race), `race_since`, `sp`, `sp_earned`, `done`, `claimed`, `counters`, `skills`, `quests`, `granted`, `backfill`. Maps are keyed by `SupernaturalRace.id()` (the lowercase enum name); a key this version does not know is kept and ignored.
- **Server only:** `schema`, `granted`, `backfill`. `SupernaturalState.clientView()` drops them; build every payload through it.
- **Access** (`core/supernatural/SupernaturalAccess`): `get`, `set` (stores, queues the sync, notifies listeners only when the state changed), `addListener`, `syncToSelf`. The sync (`SupernaturalSyncPayload`, to the owning player only) is sent once per server tick however often the state is set, and at login, respawn and dimension change. Known limit: a listener that calls `set` sees its change nested; there is no ordering queue like `BloodlineAccess` has.

## Ladders (data)

One file per race: `data/supernaturalbloodlines/supernatural/ladder/<race>.json`, where `<race>` is `SupernaturalRace.id()` (`human`, `ghoul`, `vampire`, `original`, `original_hybrid`, `hybrid`, `tribrid`, `heretic`, `witch`, `siphoner`, `hunter`, `werewolf`). `LadderReloadListener` loads them on every data reload (only the `supernaturalbloodlines` namespace; `neoforge:conditions` are honoured, the shipped files carry `part_enabled` for `supernatural`). `LadderTable.parse` takes a file whole or rejects it with one log line (`Ladder rejected: <race>: <reason>`); the other files are unaffected and a rejected race has no ladder. Ids must be unique across all files.

```json
{
    "rungs": [
        { "id": "L-ORI-0", "name": "Immortal", "sp": 0, "ep": 0 },
        { "id": "L-ORI-2", "name": "Bloodmaker", "sp": 3, "ep": 15000, "mods": [], "tasks": [
            { "id": "L-ORI-2a", "detector": "rite", "rite": "supernaturalbloodlines:original_blood", "role": "target", "mod": "enchanted" },
            { "id": "L-ORI-2b", "detector": "level", "faction": "vampirism:vampire", "n": 8 },
            { "id": "L-ORI-2c", "any_of": [
                { "id": "L-ORI-2c.1", "detector": "signal", "signal": "compel", "n": 3 },
                { "id": "L-ORI-2c.2", "detector": "state", "predicate": "moon_unbound" }
            ] }
        ] }
    ]
}
```

- **Rung:** `id`, `name` (English fallback; the hub reads lang keys), `sp` (skill points), `ep` (Tensura EP), `mods` (the ladder ends before the first rung whose mod is missing), `tasks`. Rung 0 is the race itself: no tasks, no rewards. Rewards are for the whole rung and are split across its top-level tasks when they are claimed: even shares, the remainder on the last task (3 SP over 2 tasks pays 1, then 2).
- **Task:** `id`, `detector`, `n` (count or threshold, default 1), `mod` (dropped when that mod is missing), and every other key is an arg the event must carry (`"any"` matches everything). An `any_of` task has member tasks (with ids of their own) instead of a detector, is done when one member is, and pays as one task. A rung left with no tasks is reached automatically with its whole reward.
- **Detectors** (the fixed set, `TaskSpec.Detector`) and their required args:

| Detector | Kind | Args | Event value |
|---|---|---|---|
| `level` | threshold | `faction` | the level now |
| `state` | threshold | `predicate` (`moon_unbound`, `emissary`, `rank_beta`, `rank_alpha` = ALPHA or TRUE_ALPHA, `beta_days`, `moon_control`, `defender_kills`, `mark_kills`, `siphoned_power`, `hunter_of_the_five`) | the number, or 1 when it holds |
| `sired` | threshold | | the sireline size |
| `days_as` | threshold | `race` | days as that race |
| `tensura_tier` | threshold | | 1 when at the apex tier |
| `feed` | counting | | amount |
| `kill` | counting | `category` (`prey`, `vampire`, `hunter`, `villager`) | amount |
| `rite` | counting | `rite` (id or `any`), `role` (`performer`, `target`), optional `other` | amount |
| `signal` | counting | `signal` | amount |
| `item_use` | counting | `item` | amount |
| `thirst_days` | counting | `min`; `n` is in days, the counter in ticks | ticks |

  Only the active rung of each of the player's ladders (creature and magic) records. A threshold task is done when the value reaches `n`; a counting task adds up. A Ghoul records and claims nothing.
- **Signals the data names** (each fired from code in Task 3): `compel`, `siphon_power`, `siphon_entity` (with `prey` = `true` for a vampire- or werewolf-type), `venom_hit`, `dying_survived`, `forced_shift_survived`, `sun_ring_bound`, `mountain_ash_placed`, `prey_hit_survived`. The spec's `vampire_bite_refused` belongs to the skill quest, not to a ladder.
- **Max-level steps** are written as the number 14; `LadderDataGameTests.maxLevelStepsMatchTheFactions` checks it against `getHighestReachableLevel()` of the three factions.

`LadderRules` (pure, no game classes) holds the logic: `activeRung`, `activeTasks`, `record` / `reach` / `apply` (an event moves every matching leaf of the active rungs), `markDone`, `claim` (returns a `ClaimResult` with the new state, SP, EP, the rung reached and a `Refusal` whose `langKey()` is the message), `settle` (auto-reach of task-less rungs; call it when a race is first entered).

## Off

With the part off nothing reads or writes the attachment. Saved progress stays on the player and is back when the part is turned on again.
