# Plan 24 — Optimization Round Design

**Date:** 2026-11-28
**Status:** Draft
**Scope:** Server tick time, allocation rate, heap pressure, save-file size, client hub/wheel rendering
**Owner-chosen method:** Spark profiler on dedicated server + Holy Packs client, scripted test world, five scenes, before/after comparison

---

## 1. Profiling Methodology

### 1.1 Environment

- Dedicated server: `java -jar forge-*-server.jar` with `-Xmx4G -Xms4G -XX:+UseZGC`
- Client: Holy Packs modpack + SupernaturalBloodlines jar (the build under test)
- Test world: fresh superflat, `/gamerule doDaylightCycle false`, seed fixed, pre-generated chunks in a 64×64 radius
- Player count for multiplayer scenes: 4 fake players via `/player <name> spawn` (Carpet or similar)

### 1.2 Spark Commands

```bash
# Start profiling before each scene
/spark profiler start --thread * --interval 10

# Run the scene script for exactly 60 seconds (1200 ticks)
# ...scene commands...

# Stop and export
/spark profiler stop --format html --upload false
/spark heapsummary
/spark gcmonitor start   # leave running across all scenes
```

### 1.3 Five Scenes

| # | Scene | Setup | Duration | Key metrics |
|---|-------|-------|----------|-------------|
| 1 | Idle | Server running, no players online | 60 s | Baseline tick ms, GC pauses |
| 2 | Boss fight | 4 players vs Kol Shadow boss, active combat | 60 s | Tick p99, FightLedger allocs |
| 3 | Full-moon NPC packs | `/time set 14000`, 20 NPC packs loaded, moon rules active | 60 s | PackMoonRules tick %, NpcSubordinates allocs |
| 4 | Hub + Wheel open | 1 player with hub screen open, wheel catalogue built every 20 ticks | 60 s | Client frame ms, WheelCatalogue.build allocs |
| 5 | Multiplayer groups | 4 players in 2 covens + 2 packs, group sweeps active | 60 s | GroupSwitcher.sweep ms, CovenRegistry.sweep ms |

### 1.4 Measurement Protocol

1. Record **before** baseline: run all five scenes, capture spark HTML + heap summary + GC log.
2. Apply one fix at a time (or one task group).
3. Record **after**: same five scenes, same duration, same seed.
4. Compare: tick time (mean + p99), allocation rate (MB/s from GC log), heap live set (from heap summary), save file size (bytes on disk after `/save-all`).
5. A fix is accepted only when it improves at least one metric without regressing any other by more than 5%.

### 1.5 Save File Size Measurement

After each full scene cycle:
```bash
/save-all
wc -c world/data/supernaturalbloodlines_groups.dat
wc -c world/data/supernaturalbloodlines_packs.dat
```
Target: both files under 256 KB after 200 simulated test runs (current: ~12 MB combined).

---

## 2. Identified Hot Spots & Proposed Fixes

### 2.1 GroupIndex Save Bloat (TOP PRIORITY)

**File:** `parts/species/group/GroupIndex.java` (928 lines)
**Evidence from code:**
- 14 separate maps serialized as flat NBT lists (`ids`, `types`, `typeSince`, `ranks`, `channel`, `consent`, `votes`, `bans`, `cooldowns`, `charters`, `contests`, `marks`, `curses`, `breakers`)
- Each entry uses a `CompoundTag` with a UUID key stored as two longs via `NbtUtils.createUUID` — 16 bytes of key overhead per entry minimum
- `extras` map preserves unknown keys forever (forward-compat, but also accumulates garbage from removed features)
- `prune(PackIndex)` only removes ranks of players no longer in any pack; does NOT remove:
  - `channel` rows for players no longer in a pack
  - `consent` rows for players no longer in a pack
  - `marks`, `curses`, `breakers` for players who left the server permanently
  - `cooldowns` entries whose `until` has passed
  - `bans` scopes that are empty after `pruneBansQuietly` (the inner map is cleaned but the outer scope key stays if another scope exists)
- `save()` writes ALL maps unconditionally, even empty ones
- No compaction or defragmentation on load

**Proposed fix:**
1. **Deep prune on load and periodically:** Add `deepPrune(PackIndex packs, long now)` that removes stale entries from `channel`, `consent`, `marks`, `curses`, `breakers` for UUIDs not in `packs` and not in `ids.values()`. Call it from `GroupSwitcher.sweep()` (already runs every 200 ticks) and once during `load()`.
2. **Skip empty collections in save():** Guard each `tag.put(NAME, list)` with `if (!list.isEmpty())`. This alone cuts the base size of an idle world from ~2 KB of empty list headers to zero.
3. **Compact UUID storage:** For `ids`, `typeSince`, `marks`, `curses` — replace the per-entry CompoundTag pattern with parallel `LongArrayTag` pairs (key-high, key-low, value...). Saves ~40% per entry. Keep the old format readable in `load()` for one version.
4. **Bound `extras`:** Log and drop unknown keys older than `DATA_VERSION`. The current forward-compat is correct in principle but should emit a warning so operators notice stale data.
5. **Increment `DATA_VERSION` to 2** to signal the compacted format.

**Regression test:** Existing `GroupIndexTest` + new `GroupIndexSaveSizeTest` that seeds 500 fake groups, saves, asserts byte size < threshold, loads back, asserts all data round-trips.

### 2.2 Per-Tick Listener Overhead

**Files surveyed:**
- `GroupSwitcher.java` — sweep every 200 ticks iterates ALL leaders via `packs.leaders()`, calls `refresh()` on each online leader, then `groups.prune(packs)`
- `CovenRegistry.java` — sweep every 200 ticks calls `CircleLink.sweep()` + `CovenGovernance.sweep()`
- `HunterCurse.java` — tick every server tick (line 36: `ServerTickEvent.Post`)
- `HunterInstinct.java` — tick every server tick (line 68)
- `FightLedger.java` — event-driven (not tick-based), but `record()` prunes at 4096 entries with `removeIf` on ConcurrentHashMap keySet — O(n) scan
- `DefenderKills.java` — tick listener
- `LocatorNotices.java` — tick listener
- `Links.java` (boss) — tick listener
- `CloakEvents.java` — tick listener
- `LineWarning.java` — tick listener
- `NpcSubordinates.java` — tick listener
- `AugustineRules.java` — clears VICTIM_BEFORE every tick

**Observations:**
- At least 10 listeners fire on EVERY `ServerTickEvent.Post`. Most guard with `tickCount % N == 0` but still pay the lambda dispatch cost.
- `HunterCurse.tick()` and `HunterInstinct.tick()` run every single tick with no modulo guard — they iterate over online players each time.
- `FightLedger.record()` does a full-map `removeIf` when size hits 4096. Under sustained combat this can stall.

**Proposed fixes:**
1. **Consolidate tick listeners into a single dispatcher.** One `ServerTickEvent.Post` listener that checks `tickCount % period` and fans out to registered tasks. Eliminates 8+ NeoForge event bus dispatches per tick. New class: `TickScheduler` with `register(int period, Runnable task)`.
2. **Guard HunterCurse/HunterInstinct with early-exit.** If no online player has the hunter species, skip the entire tick body. Cache the check result and invalidate on login/logout/bloodline change.
3. **FightLedger: time-bucketed pruning.** Replace the size-triggered `removeIf` with a ring buffer of 10 one-second buckets. Prune only the oldest bucket per call. Worst case: O(bucket_size) instead of O(total).
4. **AugustineRules VICTIM_BEFORE.clear():** Only clear when non-empty. Trivial but eliminates a HashMap.clear() call on 99.9% of ticks.

**Regression test:** Existing GameTests cover behavior. Add `TickSchedulerTest` verifying task ordering and period correctness. Add `FightLedgerPruneBenchmark` (unit test with assertion on max iteration count).

### 2.3 Hub Rendering (Client-Side)

**File:** `parts/supernatural/client/hub/HubScreen.java`
**Evidence from code:**
- `render()` rebuilds `targets` list every frame (line 271: `targets = f.targets`)
- `HubView.of(player)` is called every frame (line 198) — reads a dozen player state fields
- `layout = theme.layout(width, height, visible, launchers.size())` recalculated every frame (line 209)
- Tab loop draws each tab with tooltip construction (lines 216–242): allocates `ArrayList<Component>` per tab per frame
- Seal/launcher loop (lines 247–264): similar per-frame allocation
- Scrollbar drawing iterates `maxScroll` entries (lines 272–283)
- `viewOf(page, view)` may construct a new page view object depending on theme cast (line 325–333)

**Proposed fixes:**
1. **Cache layout.** Only recalculate when `width`, `height`, `visible.size()`, or `launchers.size()` changes. Store last dimensions and compare. Layout is deterministic given these inputs.
2. **Pool tooltip lists.** Reuse a single `ArrayList<Component>` per tab slot across frames, clearing before use. Or use `List.of()` for static tooltips (most tabs have fixed text).
3. **Cache HubView.** Invalidate only on bloodline change, group change, or permission change (listen to existing events). The current comment on line 199 acknowledges the problem ("building one reads a dozen things") but doesn't cache.
4. **Defer target list construction.** Build targets lazily during input handling rather than eagerly every render frame. The `lastView` pattern (line 338) already hints at this.

**Regression test:** Existing `HubLangCoverageTest`, `PortraitFitTest`, `NewPagesContrastTest`. Add `HubLayoutCacheTest` asserting layout identity when dimensions haven't changed.

### 2.4 Skill Wheel Catalogue Building

**File:** `parts/hud/wheel/WheelCatalogue.java`
**Evidence from code:**
- `build(ServerPlayer)` iterates all installed `PROVIDERS`, collects entries, sorts, applies overlap/dedup grouping
- Called from `WheelCataloguePayload` sync (triggered by `HudConfig.wheelCatalogueIntervalTicks()`, default likely 20 ticks based on config name)
- `withGroups()` creates multiple intermediate lists: `cands`, `shown`, `out`, plus `Dedup.apply()` internals
- `coverNote()` builds translatable components with string concatenation per covered power
- Sort comparator uses `source().ordinal()` — stable but allocates comparator wrapper

**Proposed fixes:**
1. **Revision-based dirty tracking.** Add a `revision` counter incremented only when providers install/remove or player state changes (bloodline, rank, cooldown). Skip `build()` entirely when revision hasn't changed since last sync. The payload already carries a `revision` field (line 13 of `WheelCataloguePayload.java`) — wire it up.
2. **Pre-sorted provider output.** Require providers to return entries in source order. Eliminate the sort when all providers comply. Fall back to sort only when a provider violates the contract (log once).
3. **Reuse Cand list.** Clear and reuse across builds instead of allocating new ArrayLists.

**Regression test:** Existing `WheelCatalogueGameTests`, `WheelOverlapGameTests`, `WheelTensuraGameTests`. Add `WheelCatalogueRevisionTest` asserting no rebuild when revision is unchanged.

### 2.5 Rite Circle Validation

**File:** `parts/species/group/circle/CircleLink.java`
**Evidence from code:**
- `sweep()` runs every 200 ticks, iterates all sessions, calls `prune()` on each
- `prune()` iterates session members and checks pack membership via `PackIndex.inPack()` + `leaderOf()`
- `CircleQuery.Source.sizeAt()` is called during rite casting — reads block state around chalk position

**Assessment:** Circle validation is event-driven (on cast), not per-tick. The sweep is already throttled to 200 ticks. **Low priority** unless spark shows it as a hotspot. Monitor but don't preemptively optimize.

**Proposed action:** Profile first. If hot, cache `sizeAt` results keyed by BlockPos with invalidation on block change. Otherwise, no change.

---

## 3. Regression Test Strategy

Each fix gets a three-layer defense:

| Layer | What it proves | Example |
|-------|---------------|---------|
| Unit test | Logic correctness in isolation | `GroupIndexSaveSizeTest`: 500 groups → save → assert bytes < 64 KB → load → assert all 500 round-trip |
| GameTest | In-game behavior unchanged | Existing `KolShadowGameTests`, `WheelUseGameTests`, etc. must pass unmodified |
| Benchmark gate | Performance didn't regress | JMH microbenchmark or timed GameTest asserting wall-clock upper bound |

### Specific regression tests per fix:

1. **GroupIndex deep prune:** New `GroupIndexDeepPruneTest` — seed stale entries, call deepPrune, assert only valid entries remain. Existing `GroupIndexDataSafetyTest` must still pass.
2. **TickScheduler:** New `TickSchedulerOrderingTest` — register tasks at periods 1, 5, 20, 200; simulate 1000 ticks; assert execution counts match expectations.
3. **FightLedger ring buffer:** New `FightLedgerPruneDistributionTest` — insert 8192 records over simulated time, assert no single prune call scans more than 1024 entries.
4. **Hub layout cache:** New `HubLayoutCacheInvalidationTest` — assert same layout object returned for same dimensions, different object after resize.
5. **Wheel revision:** New `WheelCatalogueSkipBuildTest` — assert `builds()` doesn't increment when revision is unchanged.

### CI integration:

- All existing ~3,300 unit tests and ~2,800 GameTests must pass.
- Add a `perf-check` Gradle task that runs the benchmark gate tests and fails if any exceed their threshold.
- Save file size assertion added to the existing data safety test suite.

---

## 4. Acceptance Criteria

| Metric | Before (estimate) | Target | Measurement |
|--------|-------------------|--------|-------------|
| Server tick mean (idle) | ~15 ms | ≤ 10 ms | Spark profiler, scene 1 |
| Server tick p99 (boss fight) | ~45 ms | ≤ 25 ms | Spark profiler, scene 2 |
| Allocation rate (combat) | ~12 MB/s | ≤ 6 MB/s | GC monitor log |
| Heap live set (steady state) | ~800 MB | ≤ 500 MB | Spark heap summary |
| GroupIndex save size (200 runs) | ~6 MB | ≤ 256 KB | `wc -c` after `/save-all` |
| PackIndex save size (200 runs) | ~6 MB | ≤ 256 KB | `wc -c` after `/save-all` |
| Hub frame time (open) | ~8 ms | ≤ 4 ms | Client spark / debug overlay |
| Wheel catalogue build rate | Every 20 ticks | On-demand (revision) | `WheelCatalogue.builds()` counter |
| Test suite wall clock | Current baseline | ≤ 95% of baseline | CI timing |

A fix that meets its target but causes ANY other metric to regress by >5% is rejected.

---

## 5. Build Plan — Task Breakdown (Ordered by Impact)

Tasks are ordered by expected impact on the acceptance criteria. Each task is independently mergeable with its own regression tests.

### Phase 1: Save Data (Highest Impact — Fixes the 12 MB Problem)

| # | Task | Effort | Impact | Files |
|---|------|--------|--------|-------|
| 1.1 | GroupIndex: skip empty collections in save() | Small | High | `GroupIndex.java` |
| 1.2 | GroupIndex: deepPrune for channel/consent/marks/curses/breakers | Medium | High | `GroupIndex.java`, `GroupSwitcher.java` |
| 1.3 | GroupIndex: compact UUID storage for ids/typeSince/marks/curses | Medium | High | `GroupIndex.java` |
| 1.4 | GroupIndex: bound extras, increment DATA_VERSION | Small | Medium | `GroupIndex.java` |
| 1.5 | PackIndex: audit and apply same save-size fixes | Medium | High | `PackIndex.java` |
| 1.6 | Save size regression tests | Small | Gate | New test files |

### Phase 2: Tick Listeners (Server Performance)

| # | Task | Effort | Impact | Files |
|---|------|--------|--------|-------|
| 2.1 | TickScheduler: consolidated tick dispatch | Medium | High | New `TickScheduler.java`, refactor 8+ listeners |
| 2.2 | HunterCurse/HunterInstinct: early-exit when no hunters online | Small | Medium | `HunterCurse.java`, `HunterInstinct.java` |
| 2.3 | FightLedger: time-bucketed pruning | Medium | Medium | `FightLedger.java` |
| 2.4 | AugustineRules: conditional clear | Trivial | Low | `AugustineRules.java` |
| 2.5 | Tick scheduler + ledger regression tests | Small | Gate | New test files |

### Phase 3: Client Rendering (Frame Time)

| # | Task | Effort | Impact | Files |
|---|------|--------|--------|-------|
| 3.1 | HubScreen: cache layout calculation | Small | Medium | `HubScreen.java` |
| 3.2 | HubScreen: cache HubView with invalidation | Medium | Medium | `HubScreen.java`, `HubView.java` |
| 3.3 | HubScreen: pool tooltip lists | Small | Low | `HubScreen.java` |
| 3.4 | WheelCatalogue: revision-based dirty tracking | Medium | High | `WheelCatalogue.java`, provider interfaces |
| 3.5 | Client rendering regression tests | Small | Gate | New test files |

### Phase 4: Verification & Polish

| # | Task | Effort | Impact | Files |
|---|------|--------|--------|-------|
| 4.1 | Full spark profile: before/after all fixes | Small | Validation | Docs update |
| 4.2 | Perf-check Gradle task for CI | Small | Gate | `build.gradle` |
| 4.3 | Update acceptance criteria with actual numbers | Small | Docs | This document |

---

## 6. Out of Scope

- Rite circle validation optimization (profile first, see §2.5)
- Network protocol compression (separate plan)
- Mod compatibility testing beyond Holy Packs (owner's chosen test environment)
- Client-side particle/render optimizations outside the hub
- Database migration tooling for save format changes (handled by DATA_VERSION + backward-compatible load)

---

## 7. Risks

- **Save format change (§1.3):** Must maintain backward-compatible loading for one version. Test with real 12 MB save files from production servers.
- **TickScheduler consolidation (§2.1):** Changing listener ordering could expose latent race conditions. Mitigate by preserving relative ordering within each period bucket.
- **HubView caching (§3.2):** Missing an invalidation trigger would show stale data. Mitigate by over-invalidating (any bloodline/group/permission event) rather than under-invalidating.
- **FightLedger ring buffer (§2.3):** Bucket boundary could split a fight record across buckets. Mitigate by using wall-clock alignment, not tick alignment.