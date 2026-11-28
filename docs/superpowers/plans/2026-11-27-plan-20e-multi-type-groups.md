# Plan 20e: Multi-Type Groups (Design)

> **Status:** DESIGN ONLY. No src changes, no Gradle, no CI. This document grounds the build plan in `2026-11-27-plan-20e-build.md`.

**Goal:** Let a single group carry more than one type at once, derived from the leader's kinds. A hybrid/Original-hybrid leader gives pack+clan; a tribrid gives pack+clan+coven; a heretic or vampire-siphoner gives clan+coven; a witch-werewolf gives pack+coven; a witch-hunter gives coven+brotherhood; a plain human or hunter gives brotherhood alone. Members join if they fit **any** of the group's types, and receive only the perks of the types they personally fit. When the leader's kinds change, types are recomputed; when a member stops fitting every active type, they are removed with notice (no ban).

**Owner decisions recorded:** project memory #1235, #1240, #1248–#1256. Every rule below cites its source.

## 1. Architecture overview

### 1.1 What changes in `GroupIndex`

Today `GroupIndex` stores one type per group (`types: Map<UUID, String>`, `typeSince: Map<UUID, Long>`). Plan 20e replaces these with:

- `types: Map<UUID, Set<String>>` — the active type set for the group. Stored as a `ListTag` of strings, sorted alphabetically for deterministic NBT.
- `typeSince: Map<UUID, Map<String, Long>>` — per-type stamp. A type that leaves and returns gets a fresh stamp; dormant rows (ranks, charters, contests, votes) key off their own type id and already survive a type switch, so no new row is needed for them.
- `mainType: Map<UUID, String>` — the type shown to outsiders and used as the hub icon fallback under Reduce Motion. Computed deterministically from the active set (see §1.3); stored so an offline leader's group still has a stable main type.

The existing `KNOWN` set gains `MULTI_TYPES`, `TYPE_STAMPS`, `MAIN_TYPE`. Old single-type rows (`TYPES`, `TYPE_SINCE`) are migrated on load (§4). Unknown keys still round-trip through `extras`.

All other rows stay as they are. Ranks are already keyed `(member, type)` and dormant when the type is not active (`GroupIndex.rankOf` calls `GroupSwitch.dormant`). Charters, contests, votes and bans are keyed by group id and type-specific scope; they survive a type leaving and returning because nothing deletes them on a switch today (R280, R259, R245).

### 1.2 Type computation

A new pure class `core.group.MultiTypes` replaces `GroupTypes.of` for leaders:

```
MultiTypes.of(Leader l) -> Set<GroupType>
```

Rules (owner decisions #1248–#1253):

| Leader facts | Active types |
|---|---|
| werewolf OR hybrid-type species (HYBRID, ORIGINAL_HYBRID) AND vampire | PACK, CLAN |
| tribrid (TRIBRID species) | PACK, CLAN, COVEN |
| vampire AND magic-user species (SIPHONER, HERETIC) | CLAN, COVEN |
| werewolf AND magic-user species (WITCH) | PACK, COVEN |
| hunter AND magic-user species (WITCH) | COVEN, BROTHERHOOD |
| hunter alone, OR plain human | BROTHERHOOD |
| vampire alone (NONE, ORIGINAL, AUGUSTINE, ASCENSION_BLOODFIEND) | CLAN |
| magic-user alone (WITCH, SIPHONER, HERETIC) | COVEN |
| werewolf alone | PACK |

Ascension Bloodfiends count as vampire-like (#1254): `Species.ASCENSION_BLOODFIEND` joins the vampire branch above.

`mainType` is the first in canonical order PACK > CLAN > COVEN > BROTHERHOOD that is active. Deterministic, stable, and matches the "first match wins" spirit of today's `GroupTypes.of`.

`Groups.derive(ServerPlayer)` returns `Set<GroupType>` now. `Groups.typeOf(ServerPlayer)` and `Groups.typeOfGroup(ServerLevel, UUID)` return the **main type** for callers that need one answer (hub labels, admin rows, legacy commands). New `Groups.typesOf(...)` and `Groups.typesOfGroup(...)` return the full set.

### 1.3 Join, leave, remove

**Join gate (`GroupGate.canInvite` / `canLead`):** A player may join if they fit **any** active type of the group. The per-type fit rules are:

- PACK: everyone fits.
- CLAN: vampire-like (vampire flag true, or species in NONE/ORIGINAL/AUGUSTINE/SIPHONER/ASCENSION_BLOODFIEND).
- COVEN: witch-like (species in WITCH/SIPHONER/HERETIC).
- BROTHERHOOD: human or hunter (`BrotherhoodFit.fits` or hunter faction/species).

A shunned witch is still barred from every coven-type group even if the group also carries pack or clan (#1250): the shun check runs against the coven type specifically, not the group's main type.

An exiled player is barred from the specific group (exile scope is the group id, unchanged).

**Member stops fitting:** When the leader's types change (kind change, lead move, login, 200-tick sweep), every member is checked against the new active set. A member who fits **none** is removed via `PackMembers.departed` with a notice (`group.type_lost.<type>` where `<type>` is the type they last fit). No ban, no exile. Offline members get a queued notice. There is a 30-second grace period after an upgrade before removal fires, so a member who logs in during the window can re-fit by changing their own kind (e.g., turning). The grace is wall-clock, stored in a transient map in `GroupSwitcher`, not persisted.

**Perks:** Each perk system reads the member's personal fit, not the group's type set. Today's code already does this: `CovenPotency` checks `isMagicUser`, `BrotherhoodArms` checks `BrotherhoodFit.fits`, `ClanRanks` checks vampire-like. No change needed except that the gate that decides whether a perk *applies at all* now reads `Groups.typesOfGroup` instead of `Groups.typeOfGroup`. Specifically:

- Circle link: active if COVEN is in the group's types AND the anchor is witch-like.
- Charter / contest: active if CLAN is in the group's types.
- Brotherhood mark lead / Code / arms / sight: active if BROTHERHOOD is in the group's types.
- Pack duel / alpha succession: active if PACK is in the group's types.

### 1.4 Leadership succession across types

Every type's succession rule runs independently. Whichever produces a new leader **first** wins; then types are recomputed from the new leader's kinds.

Order of evaluation in `PackAbsence.check`:

1. PACK: `SparkHandler.coAlphaHeir` (unchanged).
2. CLAN: longest-serving Day Walker who is here.
3. COVEN: longest-serving Elder who is here.
4. BROTHERHOOD: highest online mark.

If multiple types produce candidates, the first non-empty in the above order wins. This is deterministic and matches the canonical main-type order. After promotion, `GroupSwitcher.refresh(newLeader)` recomputes the type set from the new leader's kinds; dormant rows of types that leave are kept, and rows of types that return are restored exactly (running contests/votes/circles/duels are voided by the existing `GroupSwitcher.onSwitched` hooks).

The brotherhood's lead-by-mark (`BrotherhoodLead`) also triggers a recompute when it fires.

### 1.5 Hub view changes

**Payload:** `GroupViewPayload` gains a `Set<GroupType> types` field and a `GroupType mainType` field. The existing `type` field stays and carries `mainType` for backwards compatibility (old clients see the main type only). Protocol bump required.

**Icon cycling:** The hub client cycles through the active type icons every 2 seconds. Under Reduce Motion (Minecraft's accessibility setting, read via `Minecraft.getInstance().options.enableAccessibility()` or equivalent), only the main type's icon is shown. The cycle order is canonical (PACK > CLAN > COVEN > BROTHERHOOD).

**Outsiders vs members:** An outsider (not in the group) sees only the main type in the group list and profile. A member sees all active types. The payload already carries `hasGroup` and the viewer's rank; the client uses the viewer's membership to decide which field to render.

**Labels:** `GroupLabels.noun` takes the main type. A new `GroupLabels.nouns(Set<GroupType>)` returns a comma-separated list for the member view (e.g., "Pack & Clan"). Lang keys: `supernaturalbloodlines.hub.group.noun.multi` with `{0}` = the list.

### 1.6 Coven NPC circles

Witch-like Tensura subordinates of members may stand in coven circles and add potency. They are capped by `circle_npcs_per_member` (default 1). The subordinate must be:

- Owned by a member of the group.
- Witch-like (the subordinate's species, not the owner's).
- Within the circle radius.

`CovenPotency` already counts members; it gains a second pass that counts eligible NPCs up to `members * circle_npcs_per_member`. The NPC does not pay EP or channel meter; it only adds potency.

### 1.7 Config keys

New keys in `supernaturalbloodlines-groups.toml`, section `groups`:

| Key | Type | Default | Comment |
|---|---|---|---|
| `multi_type_groups` | bool | true | Master switch. Off: groups behave as in 20a-20d (one type from main type only). Existing multi-type groups keep their members but only the main type is active. |
| `circle_npcs_per_member` | int | 1 | Max witch-like Tensura subordinates per member in a coven circle. 0 = no NPCs. |
| `type_lost_grace_seconds` | int | 30 | Seconds after a type-set change before a member who fits none is removed. 0 = immediate. |

Admin rows: 3 new entries. `AdminEntries.ALL.size()` becomes (current on main) + 3. Record the exact baseline in the build plan's Task 0.

### 1.8 Rules allocation

R363–R375:

- R363: A group's active types are derived from the leader's kinds.
- R364: A player may join a group if they fit any of its active types.
- R365: A member receives only the perks of the types they personally fit.
- R366: When a member fits no active type, they are removed with notice after the grace period; no ban.
- R367: Ascension Bloodfiends are vampire-like for group purposes.
- R368: A witch-hunter fits both coven and brotherhood.
- R369: Leadership succession evaluates every active type; the first non-empty candidate in canonical order wins.
- R370: When a type returns, its dormant rows are restored exactly; running contests, votes, circles and duels are voided.
- R371: Outsiders see only the main type; members see all active types.
- R372: The hub icon cycles through active type icons every 2 s; Reduce Motion shows the main type only.
- R373: Witch-like Tensura subordinates of members may stand in coven circles and add potency, capped by `circle_npcs_per_member`.
- R374: A shunned witch is barred from every group that carries the coven type, even if the group also carries other types.
- R375: On upgrade from single-type to multi-type, members who fit none of the new types are removed once with notice after the grace period.

## 2. Migration from single-type to multi-type

### 2.1 Data migration (on load)

When `GroupIndex.load` encounters the old `TYPES` / `TYPE_SINCE` keys (single string per group), it:

1. Reads the single type as a singleton set.
2. Writes it into `MULTI_TYPES` and `TYPE_STAMPS`.
3. Sets `MAIN_TYPE` to that type.
4. Drops the old keys (they are not in `KNOWN` anymore, so they would otherwise round-trip as extras).

This is lossless: a group that was CLAN stays {CLAN} with the same stamp.

### 2.2 Runtime upgrade

When `multi_type_groups` flips from false to true (config reload or server restart), the next `GroupSwitcher.sweep` recomputes every online leader's type set. Members who fit none get the grace-period notice. No bulk removal; the sweep is the same 200-tick one that already runs.

### 2.3 Downgrade

When `multi_type_groups` flips to false, every group keeps its members but only the main type is active. Dormant rows of other types are preserved. Flipping back restores them.

## 3. Interaction with existing 20a–20d code

| System | Change | Risk |
|---|---|---|
| `GroupSwitcher.refresh` | Returns `Optional<SetChange>` instead of `Optional<Switch>`. Hooks receive the old and new sets. | Low: hooks already cancel in-flight events; the set change carries the same info. |
| `GroupGate.canLead` | Reads `MultiTypes.of` instead of `GroupTypes.of`. The founding gate checks that the leader fits at least one type they would create. | Low: the gate is already permissive for leaders with members. |
| `GroupSuccession.heir` | Takes `Set<GroupType>` instead of `GroupType`. Evaluates each active type in canonical order. | Medium: must preserve today's behaviour for single-type groups. Covered by tests. |
| `PackAbsence.check` | Calls the multi-type heir. | Same as above. |
| `BrotherhoodLead` | Triggers recompute on mark change. | Low: already triggers `GroupSwitcher.refresh`. |
| `CovenGovernance` (shun) | Shun bars coven-type groups specifically. | Low: scope is already `type:coven`. |
| `ClanDiscipline` (exile) | Unchanged: scope is group id. | None. |
| `CircleLink` / `CovenPotency` | Counts NPC subordinates. | Medium: NPC counting must not double-count members. |
| `GroupViewPayload` | New fields. Protocol bump. | Low: old clients see main type. |
| `AdminEntries` | 3 new rows. | Low: within budget. |
| `GroupButtons.IDS` | No new buttons; the existing controls work for multi-type groups. | None. |

## 4. Edge cases

1. **Leader goes offline mid-sweep.** Offline leaders are never switched (today's rule). Their group keeps whatever type set it had. When they log in, `refresh` runs and recomputes.
2. **Two types produce heirs on the same tick.** Canonical order breaks the tie deterministically.
3. **A member fits a type that leaves and returns before the grace period ends.** Grace is reset on each type-set change; the member stays.
4. **A circle is open when COVEN leaves the type set.** The existing `GroupSwitcher.onSwitched` hook closes it. Members are told.
5. **A contest is running when CLAN leaves.** Voided by the same hook. Charter row is kept (dormant).
6. **A vote is open when COVEN leaves.** Cancelled by the same hook.
7. **NPC subordinate's owner leaves the group.** The NPC is dropped from the circle on the next potency computation (lazy, like member drops today).
8. **Reduce Motion toggled mid-session.** Client re-reads the setting on the next render tick; no server change.
9. **Old client connects to a multi-type server.** Sees main type only (payload compatibility). Cannot issue multi-type-specific commands (there are none; all actions go through existing typed commands).
10. **`open_founding` with multi-type.** Lifts the per-type founding gate as today; the leader still gets the types their kinds produce.

## 5. Open questions and recommended picks

See `2026-11-27-plan-20e-build.md` §Open Questions.