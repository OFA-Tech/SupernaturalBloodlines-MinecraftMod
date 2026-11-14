# Part: supernatural

Requires: `vampirism`. Optional: `enchanted` (rite tasks), `werewolves` (level steps and pack tasks); a task whose mod is missing is dropped from its rung. Package: `net.ofatech.supernaturalbloodlines.parts.supernatural`. Config key `supernatural`, config file `supernaturalbloodlines-supernatural.toml` (empty for now). Plan: `docs/superpowers/plans/2026-11-14-supernatural-system.md` (phase 12a). Spec: `docs/superpowers/specs/2026-11-14-supernatural-system-design.md`.

**Mixin budget: 0 of 3 used.** One is reserved for an `@Inject` on Enchanted's `Rite` completion (phase 12a Task 3, `require = 0`, config switch), added only if Enchanted fires no event.

## What it holds

- **The race** (`core/supernatural/SupernaturalRace`): computed, never stored. `creature(BloodlineState, factionId)` is the first match of the spec table (Ghoul, Original hybrid, Original, Tribrid, Hybrid, Heretic, Hunter, Vampire, Werewolf, Human); `magic(BloodlineState)` is Witch or Siphoner or nothing. Tribrids and heretics have no magic race. No game classes.
- **The attachment** `supernaturalbloodlines:supernatural` (`SupernaturalState`, own codec, copied on death). Fields: `schema`, `rungs` (high-water mark per race), `race_since`, `sp`, `sp_earned`, `done`, `claimed`, `counters`, `lifetime` (counts that run from the moment the player has a race, for the "in total" tasks), `skills`, `quests`, `granted`, `backfill`. Maps are keyed by `SupernaturalRace.id()` (the lowercase enum name); a key this version does not know is kept and ignored.
- **Server only:** `schema`, `granted`, `backfill`. `SupernaturalState.clientView()` drops them; build every payload through it.
- **Access** (`core/supernatural/SupernaturalAccess`): `get`, `set` (stores, queues the sync, notifies listeners only when the state changed), `addListener`, `syncToSelf`. The sync (`SupernaturalSyncPayload`, to the owning player only) is sent once per server tick however often the state is set, and at login, respawn and dimension change. Known limit: a listener that calls `set` sees its change nested; there is no ordering queue like `BloodlineAccess` has.

## Ladders (data)

One file per race: `data/supernaturalbloodlines/supernatural/ladder/<race>.json`, where `<race>` is `SupernaturalRace.id()` (`human`, `ghoul`, `vampire`, `original`, `original_hybrid`, `hybrid`, `tribrid`, `heretic`, `witch`, `siphoner`, `hunter`, `werewolf`). `LadderReloadListener` loads them on every data reload (only the `supernaturalbloodlines` namespace). The listener is only registered while the part is on, so the files carry no `part_enabled` condition. `LadderTable.parse` takes a file whole or rejects it with one log line (`Ladder rejected: <race>: <reason>`); the other files are unaffected and a rejected race has no ladder. Ids must be unique across all files.

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

- **Rung:** `id`, `name` (English fallback; the hub reads lang keys), `sp` (skill points), `ep` (Tensura EP), `mods` and `parts` (the ladder ends before the first rung whose mod is missing or whose part, by config key, is off), `tasks`. Rung 0 is the race itself: no tasks, no rewards. Rewards are for the whole rung and are split across its top-level tasks when they are claimed: even shares, the remainder on the last task (3 SP over 2 tasks pays 1, then 2).
- **Task:** `id`, `detector`, `n` (count or threshold, default 1), `mod` and `part` (the task is dropped when that mod is missing or that part is off: a signal fired by a part that is off never comes), `total` (a lifetime count, counting only), and every other key is an arg the event must carry (`"any"` matches everything). An arg a detector does not know is kept and logged as a warning (`TaskSpec.Detector.allows`). An `any_of` task has member tasks (with ids of their own) instead of a detector, is done when one member is, and pays as one task. A rung left with no tasks is reached automatically with its whole reward.
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
| `pack_days` | counting | `n` in days | ticks in the pack as a Beta, Alpha or True Alpha |

  Only the active rung of each of the player's ladders (creature and magic) records. A threshold task is done when the value reaches `n`; a counting task adds up. A Ghoul records and claims nothing.
- **In total.** A `total` task reads `lifetime[<race>/<detector>?<args>]`, which grows by every matching event while the player is that race, whatever rung is active (L-SIP-1a, 2b, 4a and L-HER-1b, 3c share the siphon count of their race; L-WIT-3b and 4b the rite count). Tasks that are not `total` count only while their rung is active (spec rule 1). When a rung becomes active, a total that is already enough completes at once.
- **Parts and mods.** `LadderTable.parse(files, modLoaded, partEnabled)`. Each task names the part whose code fires its signal: `species` (compel, sired, dying, venom, siphon, forced shift, the rank/state predicates, the Lineage, Original/Hybrid Blood, Severance rites), `artifacts` (Sun Ring, mountain ash, vervain tea, the Wards, the Cure), `vampirismEnchanted` (the faction rites, "prey"), `enchantedTensura` (Purification). With `-PpartsOff=species` a vampire's L-VAM-3 keeps only its level step and still pays the whole rung (`LadderDataGameTests.aVampireClimbsWhateverPartsAreOff`). The Hunter ladder ends at L-HUN-2 without `species`. Apex rungs need `mods: ["tensura"]` and `parts: ["supernaturalTensura"]`: **without Tensura (or with that part off) every ladder ends before its apex rung, because there are no apex races.**
- **Shares when a mod or part goes away between two claims.** Shares are computed from the rung as it is loaded now. If a task disappears after some of its rung was claimed, the tasks left split the whole total, so the remaining claims can pay more or less than the old shares; nothing already paid is taken back and a claimed task is never paid twice.
- **The one door.** `LadderRules.apply` is the only public way to record an event: it freezes both ladders for a Ghoul creature race, adds the lifetime counts, and moves every matching leaf. `record` and `reach` are package-private.
- **Client table.** `LadderTable.sources()` keeps the accepted files as read. `LadderNet` sends them in `LadderTablePayload` (JSON text by race id) on login and after every data reload (`OnDatapackSyncEvent` fires for both), and the client parses them with its own mods and parts into `LadderTable.clientCopy()`, which the hub and the claim screen read. The packets are registered `optional()`, so a client or server without the part still connects. A file that is not JSON is skipped with one warning. Not testable here without a real client: the codec round trip is (`LadderPayloadTest`).

## Claims, SP and EP (phase 12a Task 4)

- `LadderService.claim(player, taskId)` runs `LadderRules.claim` and applies its result: SP go into the state (`sp`, `spEarned`), EP go through `shared/supernatural/EpReward` (a settable hook; no-op by default; `supernaturalTensura/TensuraEpReward` adds `getEP() + ep` to `TensuraStorages.getExistenceFrom(p)` and calls `markDirty()`: flat, no +25% boost), the player gets a chat line (`claim.paid`, `claim.rung_reached`). A refusal sends its own lang key (`claim.ghoul_frozen`, `unknown_task`, `not_active`, `not_done`, `already_claimed`) and changes nothing.
- The only client action is `ClaimTaskPayload` (a task id of at most 64 characters). The server decides everything: the id is only a name, an unknown one is refused (`anInventedTaskIdIsRefused`).
- A rung left with no tasks (its mod or part is off) is reached on its own, with its SP and EP, by `TaskTracker` after every event (`LadderRules.settle`); a Ghoul is not paid.
- `shared/supernatural/LadderQuery` is what other parts ask: `layerSlice(player)` (empty = the whole layer) and `rung(player, race)`. Without this part the answers are "the whole layer" and 0.

## Off

With the part off nothing reads or writes the attachment. Saved progress stays on the player and is back when the part is turned on again.
