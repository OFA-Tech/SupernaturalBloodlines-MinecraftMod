# Part: supernatural

Requires: `vampirism`. Optional: `enchanted` (rite tasks), `werewolves` (level steps and pack tasks); a task whose mod is missing is dropped from its rung. Package: `net.ofatech.supernaturalbloodlines.parts.supernatural`. Config key `supernatural`, config file `supernaturalbloodlines-supernatural.toml` (empty for now). Plan: `docs/superpowers/plans/2026-11-14-supernatural-system.md` (phase 12a). Spec: `docs/superpowers/specs/2026-11-14-supernatural-system-design.md`.

**Mixin budget: 0 of 3 used.** One is reserved for an `@Inject` on Enchanted's `Rite` completion (phase 12a Task 3, `require = 0`, config switch), added only if Enchanted fires no event.

## What it holds

- **The race** (`core/supernatural/SupernaturalRace`): computed, never stored. `creature(BloodlineState, factionId)` is the first match of the spec table (Ghoul, Original hybrid, Original, Tribrid, Hybrid, Heretic, Hunter, Vampire, Werewolf, Human); `magic(BloodlineState)` is Witch or Siphoner or nothing. Tribrids and heretics have no magic race. No game classes.
- **The attachment** `supernaturalbloodlines:supernatural` (`SupernaturalState`, own codec, copied on death). Fields: `schema`, `rungs` (high-water mark per race), `race_since`, `sp`, `sp_earned`, `done`, `claimed`, `counters`, `skills`, `quests`, `granted`, `backfill`. Maps are keyed by `SupernaturalRace.id()` (the lowercase enum name); a key this version does not know is kept and ignored.
- **Server only:** `schema`, `granted`, `backfill`. `SupernaturalState.clientView()` drops them; build every payload through it.
- **Access** (`core/supernatural/SupernaturalAccess`): `get`, `set` (stores, queues the sync, notifies listeners only when the state changed), `addListener`, `syncToSelf`. The sync (`SupernaturalSyncPayload`, to the owning player only) is sent once per server tick however often the state is set, and at login, respawn and dimension change. Known limit: a listener that calls `set` sees its change nested; there is no ordering queue like `BloodlineAccess` has.

## Off

With the part off nothing reads or writes the attachment. Saved progress stays on the player and is back when the part is turned on again.
