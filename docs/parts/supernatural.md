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
- **Client table (planned, phase 12a Task 4).** The hub and the claim screen need the ladders on the client. `LadderTable.sources()` keeps the accepted files as read; Task 4 sends them in their own payload on login and on `/reload`, and the client builds its table with `LadderTable.parse(sources, ...)` using its own mods and parts.
- **Max-level steps** are written as the number 14; `LadderDataGameTests.maxLevelStepsMatchTheFactions` checks it against `getHighestReachableLevel()` of the three factions.

`LadderRules` (pure, no game classes) holds the logic: `activeRung`, `activeTasks`, `record` / `reach` / `apply` (an event moves every matching leaf of the active rungs), `markDone`, `claim` (returns a `ClaimResult` with the new state, SP, EP, the rung reached and a `Refusal` whose `langKey()` is the message), `settle` (auto-reach of task-less rungs; call it when a race is first entered).

## Detectors and task signals (phase 12a Task 3)

`parts/supernatural/task/Detectors` registers the listeners; `TaskTracker` turns what they see into `TaskEvent`s, applies them with `LadderRules.apply`, and writes the state once. No timer runs more often than every 200 ticks per online player, and nothing scans the world.

| Detector | Where it comes from |
|---|---|
| `level` | `PlayerFactionEvent.FactionLevelChanged`, then a full re-read (below) |
| `state` | `StatePredicates` (pure) over `BloodlineState`, re-read on every `BloodlineAccess` change, on login, on a faction change and every 200 ticks; `emissary` through `EmissaryLines`, `moon_control` through `MoonControl` |
| `feed` | `BloodDrinkEvent.PlayerDrinkBloodEvent`, when the source is human-like: `shared/supernatural/HumanLikeBlood`, which the species part points at plan 11's `TurningHandler.isHumanLikeBlood` (without the species part only villagers and traders count) |
| `kill` | `LivingDeathEvent`, killer a player: `prey` (`Supernatural.isPrey`), `vampire`, `hunter` (Vampirism's faction of the victim), `villager` |
| `item_use` | `LivingEntityUseItemEvent.Finish` |
| `sired` | `shared/supernatural/SireCount`, which the species part points at the sireline index; read every 200 ticks and on the other re-reads |
| `days_as` | game time minus `race_since[race]`, every 200 ticks; the first time a race is seen, now is stamped (migration can set it from `turnedAt`) |
| `thirst_days`, `pack_days` | every 200 ticks, +200 ticks while thirst is at least `min`, or while in the pack as a Beta, Alpha or True Alpha |
| `signal`, `rite` | `shared/supernatural/TaskSignals`, fired by the other parts (below) |
| `tensura_tier` | not in this task: the Tensura tier of the player's line comes with the apex rung gate (12d) |

**Re-read.** `TaskTracker.recheck` reads the level, every state predicate, the sired count and the days as a race, and applies them as events. It runs on login, on a faction level change, on any `BloodlineAccess` listener change and whenever the rung of any race changes in the attachment, so a level reached early counts the moment its rung becomes active.

**Signals and where they are fired** (each is one `TaskSignals.fire` line; with the supernatural part off there is no listener and it does nothing):

| Signal | Fired from |
|---|---|
| `compel` | `Compulsion.choose` (a villager's discount or follow) and `Compulsion.bind` (a lesser vampire) |
| `siphon_power` (amount) / `siphon_entity` (attr `prey`) | `Siphoning.gain` / `Siphoning.siphonEntity` |
| `venom_hit` | `Venom.onDamaged`, when the venom is applied by a player |
| `dying_survived` | `Downed.tick`, when an Original gets up from the dying state (R32) |
| `forced_shift_survived` | `ForcedShift`, the last listener on a forced full-moon shift that was not cancelled |
| `sun_ring_bound` | `BindItemRiteFactory`, when the ring made is the Sun Ring |
| `mountain_ash_placed` | `MountainAshLineBlock.setPlacedBy` (the block is placed by the player; `BlockEvent.EntityPlaceEvent` is not used because it does not fire for every placement) |
| `vampire_bite_refused` | `GhoulGates.biteVampire` (plan 11); no ladder task uses it yet, the Q-VAM-6 quest does (12c) |
| `prey_hit_survived` | the supernatural part itself: `LivingDamageEvent.Post`, a player hit by a prey type and still alive |
| `rite:<id>:performer` / `:target` (attr `other`) | every rite factory of ours: `ancestral_calling`, `hybrid_blood`, `original_blood`, `rite_of_severance`, `rite_of_lineage_reveal` and `_awaken`, the faction rites by event name (`rite_of_the_moon`, `rite_of_the_five`, `rite_of_immortality`, `rite_of_cleansing_blood`, `rite_of_the_silver_moon`), `ward_of_daylight`, `ward_of_the_moon`, `rite_of_the_cure`, `purification`, `magicule_offering`. All in the `supernaturalbloodlines` namespace. The performer is the caster, the target is the player in the circle (the caster again when nobody stands there, then both roles fire for that player). |

**Settled in this task** (the open details of the Tasks 1 and 2 report):
- `prey_hit_survived` stays: the spec has no detector for "survive a hit", and a damage event is the only honest source.
- The `state` predicate ids are the ten in `StatePredicates.IDS`. `rank_alpha` is ALPHA or TRUE_ALPHA (L-WOL-4, L-WOL-5, L-TRI-3). `beta_days` is days since `betaSince` and only for a Beta, because `betaSince` is cleared on becoming an Alpha; L-HYB-4a ("days as a Beta or Alpha") therefore uses the `pack_days` counter instead.
- The `mod` and `part` tags are checked against the fire points in the table above (the part of the code that fires the signal is the part named in the data).
- **No mixin on Enchanted's `Rite` yet (decision for the owner).** Checked in the 4.2.7 jar: it has no event class for rites (its `*Events` classes are client, common, effects, poppets). The only common end point is `Rite.stop()`, which calls `onStop` and detaches the chalk; it is not a success signal (`cancel()` is the rite's own failure path and I did not trace whether a cancelled rite also reaches `stop()`), and `RiteType` keeps its `RiteFactory` in a private field with no getter, so the rite's id would need reflection. A mixin on `stop()` could therefore credit a cancelled rite. The reserved `supernatural` mixin stays unused (0 of 3) and only rites made by our own factories count, which the plan allows ("without the mixin, only our rites count"). Rites made with Enchanted's own factories (`enchanted:create_item`, `enchanted:apply_curse`: Ward of Return, Blood Offering, Hearing Device, Vampire Compass, Doppelgänger Blood, the two curses) do not count, and L-WIT-2a no longer lists Ward of Return. See the wiki's Known limits.

## Off

With the part off nothing reads or writes the attachment. Saved progress stays on the player and is back when the part is turned on again.
