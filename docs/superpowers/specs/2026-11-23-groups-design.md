# Groups design (plan 20): packs, clans, covens and brotherhoods on one base

Status: design for the owner to review. Documents only; no code was written and no Gradle ran. Date 2026-11-23, written against `origin/main` (3a6df244, after plan 18f).
Lore and sources: [research/groups-lore.md](../research/groups-lore.md) (read it for the show and film material; this file does not repeat it).
Plans: [20a base](../plans/2026-11-23-plan-20a-groups-base.md) (full step-by-step TDD plan, with the outlines of 20b covens, 20c clans and 20d brotherhoods at its end, as plan 19a does for 19b to 19d).

## 0. Decisions this design starts from (owner-approved) and what it adds

Owner-approved (copied from the memory entry "Plan 20"):

- D1 **Shared base.** The pack system is generalised (members, leader, invites, kicks, allies, challenge, 7-day absence, the Open Parties and Claims link). The Pack page becomes a Group page, themed per type.
- D2 **Type from the leader.** Werewolf-like (werewolf, hybrid, tribrid) is a pack; vampire-like (vampire, Augustine, vampire-siphoner) is a clan; witch-like (the rest) is a coven. If the leader changes kind, the type and the perks switch.
- D3 **Coven.** Circle link (linked members split a rite's power and EP cost equally; anyone who cannot pay their share cannot link; each extra member adds potency with diminishing returns); a per-witch channel cap (nosebleed, then damage); if the caster dies mid-rite everyone linked takes heavy damage and weakness (Admin "lethal" option); size tiers 3 / 7 / 13 for grand rites; Regent, Elders and a Shunning vote.
- D4 **Clan.** Shared feeding (feeding near clanmates gives them a little blood); a sire-line bonus; Night Walker to Day Walker rank with cheaper coven-crafted Sun Rings; Game of Kings (whoever holds the clan's Charter item at midnight leads); no Garden, exile only.
- D5 **Pack.** As today, plus the in-flight "hybrid leader gives members moon control" (branch `feature/pack-moon-control`). Covens can craft Moonlight Rings.
- D6 **Order.** After plan 19a's rite engine: 20a shared base, 20b covens, 20c clans.
- D7 **Brotherhood (owner addition, 2026-11-23).** Hunters get a fourth group type, the Brotherhood (the Five, from The Vampire Diaries, with Teen Wolf's Argent hunters and their Code). A hunter leader makes a brotherhood. Perks: shared mark (hunting together), the Code, the Hunter's Curse on a brother's killer, leadership by the highest mark. Plan 20d, section 8b; its four questions were decided on PR #83 (16.2).
- D8 **Decided on PR #83 (owner approved all six picks of the first review).** Q1 members pay 70% of the altar bill from a per-witch channel meter, the altar pays 30%; Q2 no existing rite becomes grand, 20b adds the gate and three new grand rites; Q3 edge kinds as in 3.1 and 3.2; Q4 rings are the existing items through coven rites; Q5 the Charter contest rules of 7.6; Q6 type-specific data stays dormant on a switch. They are written into the sections they concern; section 16 keeps them as the record.

Assumptions this design makes where the decisions are silent (each is a switch, or is a question in section 16):

- A1 Every existing group is a pack, and stays one until its leader's kind says otherwise (section 3).
- A2 A hybrid-type leader is a **pack** leader even though R2 says hybrids are also vampires: the pack check comes first.
- A3 Founding a group needs the leader to be a "fit" kind (an active Alpha for a pack, a vampire for a clan, a magic user for a coven); `open_founding` lifts that.
- A4 Rule ids: **R240 to R299** are reserved here (20a R240-R249, 20b R250-R269, 20c R270-R289, 20d R290-R299). Plan 19a takes "the next free" after R214 and is assumed to stop before R240; if it does not, shift this block (one search and replace).
- A5 A player in the Vampirism hunter faction is hunter-like whatever their species (a witch who joined the hunters leads a brotherhood, not a coven); Hunter of the Five and Potential are hunter-like by species. Hunters and werewolves are already exclusive (`PackAccess.blocked`, R103), and a vampire cannot be in the hunter faction, so the hunter test never competes with the vampire test.

## 1. What exists today (read from the code, main e423c757; main has since gained plan 18f, which touches blood but not the pack stack)

The pack stack is in `parts/species/pack` (about 35 files with `party/`) and only registers when Werewolves is installed (`SpeciesPart` lines ~210-245).

| Piece | What it is | Group relevance |
|---|---|---|
| `PackIndex` (SavedData `supernaturalbloodlines_packs`, overworld storage) | Membership graph: `members` (beta to leader, insertion order = join order), emissaries, anchors, former Alphas, dissolved, strengths, allies (stored once per pair), OPAC mirrors/parties, notices, sparks, last seen, joined at, alive at. No group id: a group is "a leader UUID that is the value of at least one member". | Already type-agnostic data. This is the membership graph the groups reuse unchanged. |
| `PackIndex.register()` | A `BloodlineAccess` listener that syncs `WolfState.packAlpha` with the index, plus login and logout (last seen, notices, dissolved to Omega). | Login/logout/last-seen are needed by every type; the wolf sync is Werewolves-only. Must be split (20a task 3). |
| `PackIndex.onLeadershipMoved(old, new)`, `onDissolved(leader)` | Listener lists; whatever is kept "per leader" follows the lead. | The hook the new group store uses to follow leaders. |
| `PackCommand` (`/pack`, `/sb pack`) | invite, accept, leave, kick, ally invite/accept/remove, challenge (+accept/decline), heal, protect, info, emissary, roar, surge, spirits, npc ..., anchor. Every action is a static method returning a `Component` error or null; invitations use `CallRequests` tokens (single use, 1 minute). Gate is `isActiveAlpha` (werewolf and Alpha rank). | Gate and wording must generalise; werewolf-only actions stay under `/pack`. |
| `PackDuel` + `core/bloodline/DuelBoard` | Leadership duel: a second Alpha challenges, the leader accepts, first defeat wins, 2 min, cooldowns. Pure bookkeeping in `DuelBoard`. | Pack only. Clan uses Game of Kings, coven uses a vote. |
| `PackAbsence` | Leader offline `absent_leader_days` (7, 0 = off) loses the lead to `SparkHandler.coAlphaHeir(...)`; last-seen kept by wall clock, shifted by server downtime. | Generalise the heir pick per type. |
| `party/PartyLink`, `OpacBackend`, `core/bloodline/PartySyncRules` | Mirrors a pack into an Open Parties and Claims party the pack made itself; config `opac_link_enabled`. | Type-agnostic already; only registration and the duel check need touching. |
| `AllyProtection`, `PackIndex.ally/alliedPlayers` | Allied leaders; allied packs cannot hurt each other. | Allow across types. |
| `witch/RiteCost` + `mixin/species/RiteCostMixin` (into Enchanted's `GoldChalkBlockEntity`) | The one rite-cost hook: witch x0.75, Emissary x0.9, skills, lunar mood; siphoned power with `begin / commit / discard`. | The coven link hooks in here. No new mixin. |
| Hub | `HubPage.PACK` (Group.PEOPLE). Flat theme: `PackPage`. Themed: `BookPackPage` (styles BOOK, MOON, TENSURA) in `GrimoireTheme`, `BloodMoonTheme`, `TensuraHubTheme`. Pack data to the client rides `HubSnapshotPayload.pack` (`HubSocial.Member(name, rank, online, id)`). Requests ride `HubRequestsPayload`. Buttons send the typed command (`PackPage.command`). `ProfilePage` shows `Fact.PACK`, `PACK_DAYS`, `ANCHOR` and builds `ProfileActions.Viewer`. | Section 10. |
| Existing items | `sun_ring` (4 gold + glowstone), `moon_ring` (silver + amethyst, Werewolves only), bound by the rites `ward_of_daylight` (6000 power, `bind_item`) and `ward_of_the_moon`. R21, R96. | Section 8 (rings). |

In-flight work that touches the same files (branch `feature/pack-moon-control`, uncommitted in its worktree): `PackIndex` (+ `hybrid_like` set), `HubSnapshotPayload` (+ `moonGift`, 8 fields), `HubSocial` (+ `moonGift` predicate), `LadderNet.PROTOCOL` 10 to 11, `AdminEntries` (+1), `SpeciesConfig`, `SPECIES-RULES.md` R96 "the pack's gift". Section 14 (risks) covers the collision.

## 2. Data model and migration

### 2.1 Principle: do not touch the pack file

`PackIndex` stays the membership graph, with its class name, its SavedData name `supernaturalbloodlines_packs` and every NBT key unchanged. The generalisation adds a second store that holds only what a type needs:

`GroupIndex` (new SavedData `supernaturalbloodlines_groups`, overworld storage, `data_version` 1):

| Key (NBT list, one compound per row) | Type | Meaning | Who writes |
|---|---|---|---|
| `ids` | leader UUID to group UUID | Stable id of the group; follows the leader when the lead moves (listener). | 20a |
| `types` | group UUID to `PACK/CLAN/COVEN` | The type last derived. Compared with the live derivation to detect a switch. | 20a |
| `type_since` | group UUID to long | Wall-clock millis of the last switch (hub shows it). | 20a |
| `ranks` | member UUID to string | Only non-default ranks: clan `day_walker`, coven `elder`. A pack has none (its ranks are in `WolfState`). | 20a (clan/coven fill it) |
| `bans` | (group UUID, player UUID) to until-millis | Exile (clan) and Shunning (coven) re-join bans. | 20b/20c |
| `votes` | list of vote rows | Open coven votes (regent, shun, elder). | 20b |
| `channel` | player UUID to (value, at-tick) | The per-witch channel meter; regenerates lazily from the tick stamp, so there is no per-tick work. | 20b |
| `charters` | group UUID to charter row | Charter id, holder, state, forged at, last sighting. | 20c |
| `contests` | group UUID to contest row | A Game of Kings in progress. | 20c |
| `marks` | member UUID to int | The last mark (kills) seen online for each brotherhood member; lets the lead follow the mark (R291) and draws the map (R297). | 20d |
| `curses` | player UUID to until-millis | When a brother's-killer curse with no Potential owed ends (R295). | 20d |
| `code_breakers` | player UUID to (until-millis, strike times) | The Code-breaker timer and the strikes (R294). | 20d |

Everything is keyed by **UUID** and every list entry is read in its own try/catch (a damaged row costs that row, as `PackIndex.skipped` does).

Not stored, on purpose: perks. They are computed live from the group's current type and the member's own state, as R96's pack gift is ("live, never stored for the member"). That is what makes a type switch lossless and instant.

### 2.2 Migration without loss

1. `PackIndex` is **not rewritten**: no new key, no renamed key. A world that has run 20a and is rolled back to the old jar loads its packs exactly as before (the old jar never reads `supernaturalbloodlines_groups.dat`).
2. First load of `GroupIndex` (no file, `data_version` 0): for every leader in `PackIndex.leaders()` create a group id and store type `PACK`. This is correct by A1: every group that exists today was made by `/pack invite` with a werewolf Alpha, so it is a pack. Offline leaders are never re-derived at this step (their facts are unknown), which is why the seed is a constant and not a derivation.
3. After the seed, a group's type only changes by the live derivation (section 3) when the leader's facts can be read (online, or the `BloodlineAccess` listener fires). A leader who is offline keeps the stored type.
4. Groups born later get an id when `Groups.of(leader)` is first asked (lazy `getOrCreate`) and are also swept on login and every 200 ticks (`PackAbsence` cadence).
5. A member who leaves, is kicked or whose group dissolves loses their `ranks` row at that moment (the same places that call `PackIndex.leave/kick/dissolve`); a row for someone no longer in the group is also ignored on read and pruned in the 200-tick sweep.

Tests that prove it (20a task 2): a fixture NBT written by the *current* `PackIndex.save` (all lists populated, including the hybrid-like list of the pack-moon branch when present) loads into the new code and re-saves with every `PackIndex` list holding the same rows (sets compared as sets, `members` in order because order is join order; the maps are hash maps, so bytes are not compared); a seeded `GroupIndex` has one `PACK` row per leader; a leader moved by `promote/transfer` keeps the group id; a dissolved pack drops its group; damaged rows are skipped; unknown keys are kept in a round trip (forward compatibility).

## 3. Type derivation and what happens on a leader change

### 3.1 The derivation (pure, `core/group/GroupTypes.of`)

Inputs, all things the leader's state already says: `Species`, "is a Vampirism vampire", "is a werewolf" (Werewolves faction or a wolf lineage, the same fact `RiteKinds` uses), "is a hunter" (the Vampirism hunter faction, `BaseFaction.HUNTER`, or species `HUNTER_OF_THE_FIVE` / `POTENTIAL`).

Order (first match wins):

1. werewolf, or species in `HYBRID, ORIGINAL_HYBRID, TRIBRID` -> **PACK**
2. a hunter -> **BROTHERHOOD** (D7, A5)
3. a vampire and species in `NONE, ORIGINAL, AUGUSTINE, SIPHONER` -> **CLAN** (this is "vampire, Augustine, vampire-siphoner"; an Original is a vampire; a siphoner who is also a vampire is the vampire-siphoner)
4. everything else (witch, siphoner who is not a vampire, heretic, human) -> **COVEN**

Notes. A heretic is a witch-vampire; the decision's "the rest" puts it in the coven, and the doc follows the decision (confirmed on PR #83, Q3). A pack leader who is a plain werewolf Alpha is rule 1. A hybrid is both vampire and werewolf (R2) and rule 1 catches it first (A2).

### 3.2 Who may found a group (A3)

Deriving a type for any leader is separate from being allowed to lead. Founding (the first successful `invite`) needs a "fit" leader: PACK an active Alpha (unchanged `isActiveAlpha`), CLAN a vampire-like, COVEN a magic user (`WITCH, SIPHONER, HERETIC`), BROTHERHOOD a hunter (`Fit.hunter`: hunter faction, Hunter of the Five or Potential). `groups.open_founding` (default false) removes the gate so anyone leads the type their kind derives. An existing group whose leader stops being "fit" is not dissolved: it keeps going and only the founding gate is about new groups.

### 3.3 When the derivation runs

- the `BloodlineAccess` listener for a leader (species, faction or wolf layer changed),
- `PackIndex.promote / transfer / join of a leader` (the new leader's kind may differ: this is the `onLeadershipMoved` callback),
- the leader's login and the 200-tick sweep (heals a missed event).

### 3.4 What a switch does (R241)

`GroupIndex.switchType(group, from, to)`:

- **Nobody is kicked and nothing in `PackIndex` moves.** A werewolf Beta in a clan is still a member; their `WolfState` is theirs.
- Perks of the old type stop at once (they are read live), perks of the new type start at once.
- Type-specific stored rows (`ranks`, `votes`, `charters`, `contests`, `marks`, `curses`, `code_breakers`, pack emissary/anchors/sparks in `PackIndex`) are **kept dormant, never deleted**: a leader who becomes a werewolf again gets the pack back as it was. Dormant rows are hidden in the hub and ignored by rules. The prune sweep never deletes a row of a dormant type.
- In-flight type events are cancelled with no winner: a Game of Kings contest (the Charter becomes inert, section 7.6), an open coven vote, a forming circle, a pack duel (the existing `PackDuel` rules already cancel on a pack change).
- Every member gets one notice (`PackIndex.addNotice`, so offline members hear it at login): "Your group is now a <type>: <leader> is a <kind>". Said once per change, not per check.
- `type_since` is stamped; the hub shows "since".
- The OPAC party is untouched (it follows leadership, not type).

Abuse check: switching kind costs the leader real things (a Cure, a rite, an item); perks are not banked, so there is nothing to farm by flipping. No debounce is needed; the notice is the only per-switch output.

## 4. The shared base: what becomes generic (rules R240-R249)

- **R240 Groups.** A group is a leader plus members (the `PackIndex` graph). Its type is derived from the leader (section 3). One player is in at most one group, as one pack today. No size limit by default (`max_members`, 0 = unlimited, Admin row).
- **R241 Type switch.** Section 3.4.
- **R242 Invite / accept / leave / kick.** As R94b: only the leader invites and kicks; an invitation is a single-use token valid one minute (`CallRequests`); a leader who joins another group brings their members. The gate is `GroupGate.canLead` (3.2). A player already in a group cannot be invited. Leaving by choice keeps the kill-grace rule of `left_pack_grace_minutes` for packs; clans and covens have no kill rule, so no grace.
- **R243 Allies.** Leaders of any two groups can ally, across types (a coven can ally a pack). Allied members cannot hurt each other (`AllyProtection`, now type-blind). Allies share nothing else.
- **R244 Leader absence.** A leader offline `absent_leader_days` (7, 0 = off) loses the lead at the next check. The heir by type: **pack** the strongest second Alpha (unchanged `coAlphaHeir`); **clan** the Day Walker who has been in the clan longest and is here; **coven** the Elder who has been in the coven longest and is here (the Elder's own start date is not stored; the join time is the order); **brotherhood** the **online** member with the highest mark (the mark of an offline member is only the last one seen, so they never inherit), ties by longest in the brotherhood. "Here" is the existing rule (online or seen within the window). No heir means nothing changes. The old leader stays as an ordinary member of the new leader (a pack: second Alpha; a clan: Day Walker; a coven: Elder; a brotherhood: a Brother). Each of the four has a test with the clock pinned (`PackIndex.overrideClock`).
- **R245 Challenge.** The one "challenge for the lead" button and the one `/<type> challenge` command route by type: pack = the duel (unchanged), clan = a Game of Kings (R272), coven = a call for a new Regent (R261), brotherhood = none, the mark decides (R291). A type never gets another type's challenge.
- **R246 OPAC link.** Any group is a party of Open Parties and Claims when it is installed and `opac_link_enabled` is on: the leader owns it, others are plain members, leaving the party leaves the group. Unchanged logic, registered whether or not Werewolves is installed.
- **R247 Notices.** Everything a member must hear and may miss (kick, new leader, type switch, ban, vote result) is a notice kept for the next login (8 kept; the "you lost the lead" notice is never dropped), as `PackIndex.addNotice`.
- **R248 Wording.** Chat and hub text names the type: "pack", "clan", "coven" (lang keys `supernaturalbloodlines.group.<what>.<type>`); the existing `supernaturalbloodlines.pack.*` keys stay and still serve packs.
- **R249 Commands.** `/group <sub>` is the shared tree; `/clan` and `/coven` are aliases of it (Brigadier redirect), and `/pack` keeps working as before and also redirects the shared subcommands. Subcommands that only mean something to one type (`/pack heal`, `roar`, `surge`, `spirits`, `npc`, `anchor`, `emissary`, `protect`; `/coven link ...`, `/coven vote`; `/clan charter ...`) answer `wrong_type` on another. `/sb group` is the `/sb` form.

## 5. Pack perks (unchanged, plus the in-flight gift)

Packs keep R90-R96 exactly (Alpha ranks, spark, roar, pack bonus, Emissary, anchor, duel, weaknesses, moon control). Plan 20 adds no pack perk. What changes for packs: they appear on the Group page, invite/kick/ally go through the shared gate, and the in-flight R96 gift is read from the group type (`PACK` and the **current leader** hybrid-like). When `feature/pack-moon-control` lands first, 20a task 6 re-points its `hybrid_like` check at `GroupTypes` only if that is simpler; otherwise it is left alone. The gift is not re-implemented.

## 6. Coven (20b; rules R250-R269)

### 6.1 Roles

Regent (the leader, elected by Elders, never hereditary), Elders (appointed), members. Lore: Regent elected by the Elders, Elders appointed by Elders, shunning before the Elders, a coven of 13 as the ideal, 3 as the minimum ([lore](../research/groups-lore.md) 1.1, 1.2, 1.5, 5.1).

- **R250 Elders.** The Regent appoints an Elder; if the coven has Elders already a majority of them must agree (a vote, R259). A candidate needs `elder_min_days` (3) in the coven. At most one Elder per `members_per_elder` (4) members, at least 1 once the coven has 3 members. An Elder keeps the rank until removed (by the Regent with a majority of Elders) or until they leave.
- **R259 Votes.** One vote object at a time per kind per coven; electorate = Regent + Elders; a yes/no per elector; passes on a strict majority of the electorate **once everyone has voted or `vote_hours` (24 real hours) passed** (non-voters count as no); an electorate of fewer than 3 people means the Regent decides alone for appoint/remove/shun (and no Regent election can be called). Votes are saved (`votes`), survive restarts, and are void on a type switch or when the subject leaves.
- **R260 Shunning.** An Elder or the Regent calls a shun vote on a member (never on the Regent). If it passes the member is removed with the reason "shunned", loses any rank, cannot join **any coven** for `shun_days` (7), and is told. A shunned witch keeps their own magic; they only lose coven benefits (lore 1.5). The Elders' "back turned" walk is a cosmetic and is not in 20b (section 12).
- **R261 Regent election.** An Elder may call "no confidence" naming an Elder as the nominee. It passes by R259; the nominee becomes the leader through `PackIndex.promote(old, nominee)`, the old Regent becomes an Elder. One election per `election_cooldown_days` (7). A Regent who is absent 7 days loses the lead by R244 without a vote.

### 6.2 The circle link (R252-R258): how it hooks into the rite engine

**Which engine.** The rite engine of 19a is a set of Enchanted `RiteFactory` records (`supernaturalbloodlines:evolution`, the Rite of Mastery factory) whose EP is taken in one place: `EvolutionRiteFactory.grant` and the mastery apply spend `EnergyHelper.setBaseMaxEP(target, ... - epCost)` after the learn is confirmed. The altar side of every rite, 19a's and the older ones, goes through `RiteCost.begin / commit / discard` (called from `RiteCostMixin` in Enchanted's `GoldChalkBlockEntity`). The link plugs into **both seams** and adds no mixin:

| Seam | Today | The link adds |
|---|---|---|
| Altar power: `RiteCost.begin(level, caster, chalk, holder, base, start, type)` returns the altar's bill | witch x0.75, Emissary, skills, lunar mood; siphoned power is staged in a `ThreadLocal` and paid in `commit()`, dropped in `discard()` | After the existing chain gives `A`, a `CircleLink.plan(...)` (pure) cuts the altar's bill to `A - memberPool` and stages the members' payments in the same `Pending` object; they are charged in `commit()`, so a rite that cannot start (altar cannot pay) or cancels itself costs the members nothing. |
| EP: 19a's `grant` and mastery apply | One payer: the target | A new `RiteEp.charge(rite, target, total)` is the only place EP leaves a player; when a link is staged it splits `total` among the linked members, otherwise it charges the target exactly as before. Done as 20b task 1 (a refactor of two call sites, with a source-scan test that no rite factory calls `setBaseMaxEP` directly). |

**Forming a circle (R252).** A circle belongs to one chalk (dimension + block position) and one anchor (the caster). The anchor (a coven member who stands within the circle: 5.5 blocks flat, 2 up or down of the chalk, the radius `LinkingRiteFactory` already uses) presses **Open circle** (`/coven link open`). Other coven members within 32 blocks get a request card **Join the circle** (`CIRCLE_CALL`, single-use token, 60 s); accepting is consent. Nobody can be linked against their will. A member can **Leave** (`/coven link leave`) any time before the rite starts. Each member sets how much they will take: **consent level** `none` (default: only what their channel meter holds), `nosebleed`, `damage`. A circle holds up to 13 members; a player is in one circle at a time; the circle closes after 5 minutes idle, on a type switch, on the anchor's logout or death before the rite starts, or when the anchor presses Close.

**The plan at rite start (R253).** `RiteCost.begin(start=true)` finds the circle of this chalk, drops everyone who is no longer inside the radius, online, alive and at half health, and runs the pure plan:

- `N` = members left (the anchor counts). `N = 1` is a solo cast: nothing changes from today.
- Altar bill after the existing discounts: `A`. Member pool: `memberPool = ceil(A x link_member_share)` (default **0.70**). The altar pays `A - memberPool`.
- Each member's power share: `base = floor(memberPool / N)`, and the first `memberPool mod N` members (the anchor first, then join order) pay `base + 1`, so the shares add up exactly to `memberPool`.
- EP: the rite's EP cost `E` (19a: `ep_cost` of a recipe, or the Mastery tier cost), split the same way (floor, remainder to the first members). When `E` is 0 nothing is split. A member whose base Max EP would fall below `ep_keep_percent` (10%) of what they have **cannot link**: they are dropped.
- **Channel meter.** Each witch-type member (`WITCH, SIPHONER, HERETIC, TRIBRID`) has a meter with capacity `C = channel_cap` (default **3000** power); any other member has `C x channel_other_factor` (0.5). It refills `C` per `channel_regen_minutes` (10) and is stored as (value, tick) so it costs nothing while idle. Overdraw `d = max(0, s - meter)`:

| Overdraw | Result |
|---|---|
| `d = 0` | clean; the meter drops by `s` |
| `0 < d <= 0.5 C` | **nosebleed**: Nausea I 10 s, Weakness I 30 s, blood particles; needs consent `nosebleed` or `damage` |
| `0.5 C < d <= C` | **damage**: `floor(10 x (d - 0.5 C) / (0.5 C))` HP, at most 10, never below 1 HP, plus the nosebleed effects; needs consent `damage` |
| `d > C` | **cannot pay: cannot link.** The member is dropped before anything is charged |

- A dropped member is told why ("your share is too heavy: add members or rest"); the plan is recomputed without them, repeated until stable (at most 13 passes, then it is a pure function of the final set). If fewer than 2 remain it is a solo cast. A member whose consent is lower than their needed overdraw is dropped the same way. This is what "anyone who cannot pay their share cannot link" means in play, and why a 36 000-power rite needs a big circle: N = 3 gives 6300 each (over the cap, dropped), N = 13 gives 1453 or 1454.
- **Per-tick power** of a rite that keeps running (`start = false`) stays on the altars (a stated shortcut; upgrade only if rites with long durations show a need).

Worked numbers (witch-type, meter full at 3000, consent `none`):

| Rite | A (after witch x0.75) | N | member pool (70%) | share each | altar pays | result |
|---|---|---|---|---|---|---|
| 6 000 power (e.g. Severance) | 4 500 | 1 | 0 | 0 | 4 500 | solo, as today |
| 6 000 | 4 500 | 3 | 3 150 | 1 050 | 1 350 | all clean |
| 20 000 (Cure) | 15 000 | 3 | 10 500 | 3 500 | 4 500 | over the cap by 500: each needs consent `nosebleed` or is dropped |
| 20 000 | 15 000 | 7 | 10 500 | 1 500 | 4 500 | clean |
| 36 000 (Imprisoned Original) | 27 000 | 13 | 18 900 | 1 453 or 1 454 | 8 100 | clean |

**Potency (R254).** `m(N) = 1 + P x (1 - r^(N-1))`, `P = potency_max_bonus` (1.0), `r = potency_falloff` (0.8). Values: N=1 1.000, 2 1.200, 3 1.360, 4 1.488, 5 1.590, 7 1.738, 10 1.866, 13 1.931. Each extra member adds less (+0.200, +0.160, +0.128 ...). `CovenPotency.scale(base)` is the one function a rite calls to scale a knob that is a duration, a radius or a count (20b wires the Linking Spell's `link_seconds` and the three ward durations; the evolution rites are all-or-nothing and take no potency: their link benefit is the cost split). Potency is read from the live window of the chalk, so a rite that is not linked gets 1.0.

**Size tiers (R255).** Tier 1 at `N >= 3`, tier 2 at `N >= 7`, tier 3 at `N >= 13` (`tier1_members`, `tier2_members`, `tier3_members`, Admin rows). A **grand rite** declares `min_circle` in its factory block (a field of our own `GrandRiteFactory` base; the factory reads `CircleLink.current(chalk).size()` in `onStart` and cancels with a refusal when the circle is smaller). No existing rite becomes grand; a solo caster can still do everything they can do today. Which rites are grand: decided on PR #83 (Q2), none of the existing ones.

**Failure on caster death (R256).** From the moment the rite starts (`commit()`), the circle is **active** for the rite's duration, or `link_window_seconds` (30) for an instant rite. If the anchor dies in that window (`LivingDeathEvent` on the anchor's UUID, any cause):

- every linked member takes `link_death_damage_percent` (60) of their max health, never below 1 HP,
- plus Weakness II and Mining Fatigue I for 5 minutes, their channel meter goes to 0, and they cannot be linked again for `link_shaken_minutes` (30),
- a still-running Enchanted rite of that chalk is stopped (`RiteManager` / `Rite.stop()`: **unverified**, section 13),
- with the Admin option `link_death_lethal` (default false) the members die instead (the lore's Aja/Gemini rule).

Not a penalty case: the anchor logging out (the link is released, the rite goes on) and a member dying (that member leaves the circle, nobody else is affected). A restart forgets circles (they are in memory): the penalty cannot outlive the server, and a circle never persists a debt.

### 6.3 What else a coven gets

- **Moonlight Rings (R262).** A coven rite forges the existing Moon Ring (`moon_ring`), already bound to the person at the centre, for half the silver. Needs tier 1 (3 linked) and the receiver in a pack. Decided on PR #83 (Q4).
- Out of scope for 20b (parked, section 12): celestial battery, consecration bank, Great Harvest, Fete de Cadeau, night watch, Merge succession, the forbidden rite.

## 7. Clan (20c; rules R270-R289)

### 7.1 Ranks

Leader (title in lang: "Head of the Clan"), **Day Walker**, **Night Walker** (lore 2.2). A new member is a Night Walker. The leader promotes (`/clan promote <p>`), or a Night Walker is promoted automatically after `daywalker_days` (14 real days in the clan, 0 = leader only). A Day Walker may challenge for the Charter (R272), may bind a coven-forged Sun Ring at the clan discount (R273), and is a successor candidate (R244).

### 7.2 Shared feeding (R274) and the Vampirism hook

Hook: `de.teamlapen.vampirism.api.event.BloodDrinkEvent.PlayerDrinkBloodEvent` (verified, section 13). It is fired by `VampirePlayer.drinkBlood(int, float, boolean, IDrinkBloodContext)` before the blood is added; its `getAmount()` is the final amount, `getBloodSource().getEntity()` is the victim (empty for a bottle or a block), and `e.getVampire().getRepresentingEntity()` is the feeder. SB already listens to it (`BloodEffects`, `Detectors`, `VervainRules`) on `NeoForge.EVENT_BUS`.

Rule. Listener at `EventPriority.LOWEST` (after Keen Fangs, Vervain and the other adjustments, so it sees the amount the feeder really gets). When all hold, each clanmate gets a little blood:

- the feeder is in a clan; the event has a **living-entity source** and `getAmount() > 0` (bottles, blocks and `none()` contexts never share: no loops, no bottle farming),
- the source is not a member of the same clan (two clanmates biting each other farm nothing),
- the mate is online, alive, in the same dimension within `feed_share_radius` (16) blocks, a Vampirism vampire (`VampirismAPI.vampirePlayer(mate)` is present), and below max blood,
- `share = clamp(floor(amount x feed_share_percent / 100), 1, feed_share_max)` (default 10%, at least 1, at most 2),
- the mate's received total in the last 60 s is under `feed_share_cap_per_minute` (4), and the same feeder has not fed this mate within 100 ticks.

The mate gets the blood through `IVampire.drinkBlood(share, 0f, IDrinkBloodContext.none())`. **That call fires `BloodDrinkEvent` again** (verified in the bytecode), so a `ThreadLocal` guard marks the listener as "already sharing" and the listener also refuses any event whose source is empty. The feeder pays nothing and is unchanged. The victim is untouched (no extra drain). A shared feed does not count for tasks: `Detectors.onDrinkBlood` ignores events with no living source (**to confirm** in 20c task 1; if it does not, the shared call is wrapped in the same guard Detectors checks).

### 7.3 Sire-line bonus (R275)

For each online clanmate within 32 blocks who is in the same sireline as you (your sire, someone you sired, or anyone under the same sireline root: read from the existing sireline index, **API to be read in 20c task 1**), +3% attack damage, up to +15% (`sireline_bonus_percent`, `sireline_bonus_max_percent`). Live, never stored, same style as the pack bonus (`PackBonus`). The sire bond itself (obedience, lore 2.1) and the sireline-death rule are out of scope.

### 7.4 Rings (R273)

Day Walker is about the sun. A **coven-forged Sun Ring** (a rite performed by a coven circle with tier 1, output the existing `sun_ring` with a `coven_forged` component) binds through `ward_of_daylight` at `sun_ring_discount_percent` (50%) of its power, only for a Day Walker of a clan. Everyone else binds a plain Sun Ring at the full 6000 as today. Decided on PR #83 (Q4).

### 7.5 Exile (R276), no Garden

A leader may `/clan exile <p>`: the member is removed like a kick, loses Day Walker, and cannot rejoin **that clan** for `exile_days` (7). An ordinary `kick` has no ban. Members may always leave. There is no entombing, no desiccation and no Garden.

### 7.6 Game of Kings and the Charter (R270-R272, R277-R279)

Lore: any member can challenge; the leader must accept; whoever holds the Charter at midnight leads (Strix, lore 2.1).

**The item.** `supernaturalbloodlines:clan_charter`, max stack 1, not in any creative tab and not craftable. The server's `charters` row is the truth; the item is only a token carrying `charter_id` and `group_id`.

**Created.** A clan with at least `charter_min_members` (3) members: the leader presses **Forge the Charter** (`/clan charter forge`) holding the forge cost (config item list, default a written book and a gold block; taken on success). One live Charter per clan.

**Held.** The row records `holder` (a player UUID), `state` (`HELD`, `STORED`, `DROPPED`, `LOST`) and the last sighting. A scan every 40 ticks over online clan members' main inventory, hotbar and offhand sets `HELD`. A Charter in a chest, shulker, ender chest, item frame or any non-player container is `STORED` and **counts for nobody**.

**Protected (non-griefable).**

- Only a member of the owning clan can pick it up: `ItemEntityPickupEvent.Pre` is denied for anyone else (**event signature unverified**, section 13; the fallback is a 40-tick sweep that removes it from a non-member's inventory and drops it back to its last position).
- The item entity is invulnerable (fire, lava, cactus, explosions), never despawns, and a void fall marks the Charter `LOST` instead of destroying evidence.
- Duplicates (creative, commands, dupe bugs): one `charter_id` may exist once; the scan keeps the stack that matches the row's holder, turns every other copy into dust with a message.
- A non-member who somehow holds it (admin give) is stripped by the same sweep.
- A holder who dies drops it where they die; only clan members can take it.
- Outsiders can never hold, steal, destroy or carry the Charter out of reach; an insider can, and that is the game.

**Challenge (R271, R272).** A Day Walker with at least 7 days in the clan presses **Challenge for the Charter** (`/clan challenge`) when: the leader is online, the clan has a live Charter and at least 3 members, and no contest ran in the last `contest_cooldown_days` (7). The leader gets a request card **Accept** (the leader cannot refuse: the lore says must accept); if they do not answer in `contest_accept_minutes` (10) it starts anyway. The whole clan is told. The contest runs until the next in-game midnight in the Overworld (day time 18000), or at most `contest_max_hours` (1 real hour) if time does not advance (daylight cycle off, a sleeping skip is caught because the check is "day time passed 18000 since the contest began").

**Resolve.** At the deadline the server reads the row: if the holder is an **online Day Walker or the leader** holding the Charter in their own inventory, they lead (`PackIndex.promote(old, new)`, then `GroupIndex`, notices, `PartyLink.leadershipChanged`); the old leader becomes a Day Walker. If it is stored, dropped, held by a Night Walker or offline, **nobody changes** (the leader keeps the lead, the cooldown starts). A holder in another dimension counts (they are online).

**Lost or destroyed (R278).** `LOST` is set when the dropped entity is removed or falls into the void, or when no sighting happened for `charter_lost_days` (7 real days) while no holder is on record, or when the recorded holder has been offline for `absent_leader_days`. A `LOST` Charter: the leader may **Reforge** (`/clan charter reforge`) after 24 h, for the same cost, which gives a new `charter_id` and makes every old copy inert (dust on sight). Offline holder: the row stays `HELD (offline)`; their inventory is not touched; a contest cannot be won by an offline holder (section above), so "log out with it" wins nothing.

**Type switch (R241).** The Charter is dormant while the group is not a clan: inert, tooltip "dormant", contest void, no sweep.

**What cannot happen.** A contest never starts without the leader online; the Charter never leaves clan members' hands; two Charters never coexist; a destroyed Charter never leaves the clan leaderless (R244 and Reforge); nobody wins by being offline; nobody wins by stashing it.

## 8. Rings, in one place

| Item | Today | After |
|---|---|---|
| Sun Ring | 4 gold + glowstone, bound by `ward_of_daylight` (6000) | Same, plus the coven-forged variant at 50% bind power for a Day Walker (20c) |
| Moon Ring ("Moonlight Ring") | silver + amethyst, Werewolves only, bound by `ward_of_the_moon` | Same, plus a coven rite that forges it pre-bound for half the silver (20b) |

Both new paths are rites with a circle of at least 3 linked (tier 1). They are an alternative, never a replacement: every current recipe and bind keeps working. Whether "Moonlight Ring" should be a new item instead was Q4 and the owner kept the existing items.

## 8b. Brotherhood (20d; rules R290-R299)

Owner addition (D7). Sources: lore section 8 (the Five, the Hunter's Mark and Curse, Kol, Mikael, the Argents and their Code). This section builds on the hunter code that exists; it changes none of R80-R84 or R173 for anyone who is not in a brotherhood.

### 8b.1 What exists (read from the code, main 3a6df244)

| Piece | What it does today | Used by the brotherhood |
|---|---|---|
| `Species.HUNTER_OF_THE_FIVE` (marked) and `POTENTIAL`; `FiveRoster` (SavedData: the Five, potentials, cursed, lifted curses, pending potential choices) | At most `max marked hunters` (5, config, R80) exist; a Potential becomes marked at their first prey kill (R82). | The type test, the mark reads. |
| `MarkTracker` (`parts/species/hunter`) | A prey kill (`Supernatural.isPrey`, with witches and siphoners excluded, R81) grows `markKills` (+1% attack, +0.5% speed per kill, capped at `CAP` 20); a marked hunter killed by prey loses the mark and the world owes a Potential (`PotentialChooser.owe`). A kill inside one pack counts for nothing. | Leadership reads `markKills`; nothing in it changes. |
| `HunterCurse` | A prey killer of a marked hunter gets the saved flag `hunterCurse` (R83), one hallucination (`HallucinationPayload`) every 600 +- 200 ticks, lifted for everyone by a Potential's first kill (`liftAll`, offline players at login). A second curse changes nothing. | Reused for any brother (R295). |
| `HunterInstinct` | R84 Weakness after 3 days without a prey kill; from 5 kills prey within 24 blocks (32 with Hunter's Eye) is sent to that hunter as a glow (`GlowPayload.send(player, entityIds)`), never a cloaked one (`CloakAccess`). | Shared sight (R293) sends the same payload to brothers. |
| `HunterRules`, `CompulsionImmunity` | Marked hunters and Potentials cannot be compelled. | Unchanged. |
| Kol (R173, `KolRules`, `BossDirector`) | Comes by night for a marked hunter with `kol_min_mark` kills; leaps; withdraws after a minute with nobody to hunt. | A warning line (R296). |
| `SparkHandler.isInnocent(killer, victim)` | A villager, or a player who did not hit the killer within the last 200 ticks. Used for the werewolf's innocent kill. | The Code's "innocent" (R294), plus "not prey". |
| `HunterEvents.register()` | Runs without Werewolves (`SpeciesPart` line 178). | The brotherhood needs no Werewolves. |

Not in the code: a tattoo item, a map item, the Cure road. The Mark is stored as a number (`markKills`); "the map" is a display (R297).

### 8b.2 The rules

- **R290 The Brotherhood.** A group whose leader is a hunter (spec 3.1, A5) is a brotherhood. A hunter founds one by inviting (a Hunter of the Five, a Potential or any Vampirism hunter; `open_founding` lifts the gate for anyone whose kind derives BROTHERHOOD, which is only hunters). Anyone who is **not prey** (R81) may be invited and may stay: humans, witches, siphoners and other hunters, as the Argents had human kin. **Prey cannot join** (`prey_member` refusal, vampires, werewolves, hybrids, Originals, heretics, tribrids); a member who becomes prey is released at the next 200-tick sweep with a notice (`released_prey`), the one place a type-driven rule removes someone, and only because they became what the Brotherhood hunts. Leader title in lang: "First Brother" (Alexander led the Five); the others are Brothers; a Potential is shown as "Potential". Invite, kick, leave, allies, absence and the party link are the shared base (R242-R246). There is no `challenge` (R245).
- **R291 Leadership by the highest mark** (`lead_by_mark`, default on; decided, Q7). The leader is the member with the **highest mark**; the founder leads until a member's mark is strictly higher. Checked when a member's `markKills` changes (the `BloodlineAccess` listener that `MarkTracker` already uses), when the leader's mark drops (R82 resets it), at login and in the 200-tick sweep. The takeover goes through `PackIndex.promote(old, new)` (so allies, the party and the emissary rules follow as for any lead change), tells everyone (R247), and the old leader stays a Brother. Rules: the new leader must be **online** now, not a Code-breaker (R294) and at least a Potential; a tie keeps the current leader; the marks are `GroupIndex.marks` (last seen online per member), so an offline member's old mark can lift them above a leader only at the moment they are online to be checked. Only Hunters of the Five have a mark and at most 5 exist, so a brotherhood led by a plain hunter is led by that hunter until a marked hunter in it passes their mark of 0.
- **R292 Brothers in arms** (the hunting-together bonus; `arms_*`). Each **other** brother who is a hunter (hunter faction), online, in the same dimension within `arms_radius` (24) blocks, gives +`arms_percent` (3)% attack damage, up to +`arms_max_percent` (12)%. A live **transient** attribute modifier (never saved, refreshed every 20 ticks, removed when alone), multiplying with the mark's own bonus. Code-breakers neither give nor get it. No progress is shared: the mark is each hunter's own record (see R297 and Q4).
- **R293 Shared sight.** A brother with the prey sense (5 or more kills, R81) shares what they sense: every other brother who is a hunter and **lacks the sense** (under 5 kills) within `sight_radius` (48) blocks of the sensor gets the same glow for that pass (`GlowPayload.send`); cloaked prey stays hidden (`CloakAccess`); the sense still never shows witches or siphoners. A Code-breaker gives and gets nothing.
- **R294 The Code** ("We hunt those who hunt us"; `code_enabled`). A brother who kills an **innocent** breaks the Code. Innocent is `SparkHandler.isInnocent` and **not prey**: a villager, or a player who is not prey and did not hit them in the last 10 seconds. (Prey, hostile mobs and an attacker in a fight are never innocent.) Each break: the brother's mark loses `code_mark_penalty_percent` (25)% of its kills, at least 1 when they have any (through a new pure `BloodlineEvent.CODE_BROKEN` in the bloodline rules, so the rules own the mark and `MarkTracker.sync` follows); they are a **Code-breaker** for `code_breaker_hours` (24 real hours): no R292 or R293 given or received, no lead (R291) and no heir (R244); one line to the brotherhood. Strikes: `code_strikes` (3) breaks within `code_strikes_days` (7) **expel** them (removed with the reason "broke the Code" and barred from that brotherhood for `code_exile_days` (7)). Two breaks on the same victim within a minute count once. The Code is a punishment only; nothing rewards hunting prey (the mark already does, R81).
- **R295 The Hunter's Curse reaches every brother** (`curse_extends_enabled`). A prey killer (R83: humans and witches are exempt) of **any brother**, marked or not, gets the curse by the existing path, `HunterCurse.curse`: the saved flag, the same hallucinations. **If the killer already carries it nothing changes** (no second curse, no new timer, as R83). A curse put on by a brother who was not marked, so owes no Potential and would never be lifted by one, ends after `brotherhood_curse_hours` (48 real hours, stored as an until-time in `GroupIndex.curses`, checked at login and in the sweep); the existing lift (a Potential's first kill) ends it at once as before. A killer inside the same brotherhood (impossible for prey) is exempt like a same-pack kill. A brother killed by a non-prey (a human, the world) curses nobody.
- **R296 Kol's shadow.** When Kol comes for a brother (R173), every other online brother gets one line naming the target (`kol_warning`). Kol's rules, his eligibility and his withdrawal are unchanged; the warning is the whole rule.
- **R297 The Map** (display only). The Mark is drawn on the Brotherhood page as the tattoo of the show: the Grand Sigil appears at 1 kill, Amara and Qetsiyah at 4, the ring and pentagram at 8, Silas at 12, the five swords at 16 and the whole map at 20 (`MarkTracker.CAP`). A brother sees their own mark grow; when **any** brother has 20 the map is shown complete to every brother ("completing one completes all"), taken from `GroupIndex.marks`, never from another player's state. It does nothing else; the Cure road is parked (section 12).
- **R298 (reserved)** Mikael's call: not in 20d, see section 12.
- **R299 (reserved).**

### 8b.3 Perks by the owner's four picks

| Owner pick | Rule | Choice made |
|---|---|---|
| Shared mark | R292, R293, R297 | The **hunting-together bonus and shared sight**, not shared progress: sharing kills would inflate Kol's trigger and the cap of 20 and make the Five's rarity (at most 5 marked) meaningless. The map is shared as a picture. |
| The Code | R294 | **Punish** killing the innocent (mark loss, a day without the group's perks, three strikes expel). No reward for prey-only hunting. |
| Hunter's Curse | R295 | Reuse R83 for any brother, non-stacking, with a 48-hour end for a curse nobody can lift. |
| Leadership | R291 | The highest mark leads; the founder leads until passed. |

Seeds left out: the Argents' honour suicide (rejected, lore 8.7), Mikael's call, the Cure road, a reward for hunting only prey, the Bestiary, silver bullets (section 12).

### 8b.4 Brotherhood and the other types

Type switch: a hunter leader who stops being hunter-like (leaves the faction) switches the group to the type their new kind derives (a human leads a coven, 3.1); members stay, Brotherhood rows (`marks`, `curses`, `code_breakers`, strikes) stay dormant (Q6). A group of any other type whose leader joins the hunters becomes a brotherhood the same way. Allies are type-blind (R243): a brotherhood may ally a pack. Prey members of an allied group are not in the brotherhood, so shared sight never reaches them.
## 9. Config and Admin rows (UI-first rule)

New file `supernaturalbloodlines-groups.toml`, section `groups`, registered by `SpeciesPart` (no new Part: groups are the pack system generalised, so the part matrix is unchanged). New `AdminEntries` section `"groups"`; `AdminCoverageTest.CONFIGS` gets the file; every key below gets a row, an `en_us` key (`supernaturalbloodlines.hub.admin.key.supernaturalbloodlines-groups.toml:<key>`) and a section title. `AdminEntries.ALL.size()` is 174 on main (after plan 18f); each plan states its own new total.

| Plan | Keys (default) | Rows |
|---|---|---|
| 20a | `groups_enabled` (true), `clans_enabled` (true), `covens_enabled` (true), `open_founding` (false), `max_members` (0) | 5 |
| 20b | `link_enabled` (true), `link_member_share` (0.70), `channel_cap` (3000), `channel_other_factor` (0.5), `channel_regen_minutes` (10), `potency_max_bonus` (1.0), `potency_falloff` (0.8), `tier1_members` (3), `tier2_members` (7), `tier3_members` (13), `link_window_seconds` (30), `link_death_damage_percent` (60), `link_death_lethal` (false), `link_shaken_minutes` (30), `ep_keep_percent` (10), `elder_min_days` (3), `members_per_elder` (4), `vote_hours` (24), `election_cooldown_days` (7), `shun_days` (7), `moonlight_ring_enabled` (true) | 21 |
| 20c | `charter_enabled` (true), `charter_min_members` (3), `contest_cooldown_days` (7), `contest_accept_minutes` (10), `contest_max_hours` (1), `charter_lost_days` (7), `daywalker_days` (14), `exile_days` (7), `feed_share_enabled` (true), `feed_share_percent` (10), `feed_share_max` (2), `feed_share_radius` (16), `feed_share_cap_per_minute` (4), `sireline_bonus_percent` (3), `sireline_bonus_max_percent` (15), `sun_ring_discount_percent` (50) | 16 |
| 20d | `brotherhood_enabled` (true), `lead_by_mark` (true), `arms_percent` (3), `arms_max_percent` (12), `arms_radius` (24), `sight_share_enabled` (true), `sight_radius` (48), `code_enabled` (true), `code_mark_penalty_percent` (25), `code_breaker_hours` (24), `code_strikes` (3), `code_strikes_days` (7), `code_exile_days` (7), `curse_extends_enabled` (true), `brotherhood_curse_hours` (48), `kol_warning_enabled` (true) | 16 |

`opac_link_enabled`, `absent_leader_days` and `left_pack_grace_minutes` stay in the species file and apply to every type. A switch that is off removes the feature, not the data: `clans_enabled = false` makes no new clan and no clan perks, existing clans keep their rows and members.

## 10. Hub UI (UI-first)

Rule: **every command has a button; every config key has an Admin row; nothing needs typing.** A hub button sends the typed command (as `PackPage.command` does), so Brigadier and the action re-check on the server. The page id stays `HubPage.PACK` so saved layouts and the wheel do not break; its label follows the player's type: "Pack", "Clan", "Coven", or "Groups" for a player whose kind has no group yet.

### 10.1 Structure per theme

The three themes already split into the flat `PackPage` (shared fallback) and the themed `BookPackPage` (Grimoire = BOOK, Blood Moon = MOON, Tensura = TENSURA). 20a does not fork a page per type; it parametrises them with a `GroupView` (section 11) so the layout is common and the type decides: title and noun, member row ranks, the actions column, the ring art role set, and the extra section.

| Region | Pack (as today) | Clan | Coven |
|---|---|---|---|
| Header | Pack, "your rank" | Clan, rank (Head / Day Walker / Night Walker), "since" | Coven, rank (Regent / Elder / Member), size tier badge |
| Members list | Alpha, Beta, Packmate, emissary, ally rows, sparks, NPC wolves | Head, Day Walkers, Night Walkers, allies; sireline mark | Regent, Elders, members, allies; channel meter for you |
| Ring art (`PackRing` roles) | wolf ring (existing) | court table (Day Walkers on the inner seats) | circle of 13 candles, linked members lit |
| Extra section | wolves, moon gift line, Roar | Charter card (holder, state, contest), shared-feed line | Circle card (open/join/leave, consent, share preview), votes card |
| Theme | per existing style | colour set per theme, same layout | colour set per theme, same layout |

Brotherhood (20d): header "Brotherhood", rank (First Brother / Brother / Potential); member rows with a pip row for the mark (kills out of 20, own and the leader's only); the **Map card** (the tattoo drawing by regions, R297); the **Code card** (the saying "We hunt those who hunt us", your strikes, the Code-breaker timer); a line for the hunting-together bonus now (+x%); a Kol warning toast; ring art: five swords around a pentagram (the five seats, the leader's seat marked).

Art: the ring variants need two new ring sprites per theme (three with the Brotherhood's). They are an art task (the UI art kit pipeline, `docs/superpowers/plans/2026-11-21-ui-overhaul-phase2-art-kit.md`); until then the existing ring is drawn with the new roles tinted, so nothing blocks on art (shipped, flagged "placeholder art" in the PR).

### 10.2 Command to button table (the coverage test reads this)

| Command | Button (page, section) | Who sees it |
|---|---|---|
| `/group invite <p>` | Invite (picker, `PackInvite`) | leader |
| `/group accept [token]` | Accept (Requests card and Group page) | invited |
| `/group leave` | Leave (two presses) | member |
| `/group kick <p>` | Kick (member row detail) | leader |
| `/group ally invite|accept|remove` | Ally (picker, card, End) | leader |
| `/group info` | the page itself | member |
| `/pack challenge ...` | Challenge / Accept / Decline | pack: second Alpha / leader |
| `/clan challenge`, accept | Challenge for the Charter / Accept | clan: Day Walker / leader |
| `/coven regent <p>` | Call for a new Regent (Elder picker) | coven Elder |
| `/clan promote <p>`, `/clan demote <p>` | Promote / Demote | clan leader |
| `/clan exile <p>` | Exile (confirm) | clan leader |
| `/clan charter forge|reforge` | Forge / Reforge the Charter | clan leader |
| `/coven elder add|remove <p>` | Appoint / Remove Elder | Regent |
| `/coven shun <p>` | Call a shunning vote (confirm) | Elder, Regent |
| `/coven vote yes|no` | Yes / No on the vote card | electorate |
| `/coven link open|close|leave|join`, `consent <level>` | Open / Close / Leave / Join circle; consent selector | coven members |
| pack-only (`heal`, `protect`, `roar`, `surge`, `spirits`, `npc ...`, `anchor`, `emissary`) | unchanged | pack |
| (the Brotherhood has no command of its own: `/brotherhood` is one more alias of the shared tree; marks, the Code, the map and the bonus are shown, never typed; its Admin rows are in section 9) | the Map and Code cards | brotherhood |

`GroupUiCoverageTest` (20a) walks the `GroupCommand` tree and fails for any literal that has no button id registered in `GroupButtons` (the same idea as `AdminCoverageTest`). New request kinds (`CIRCLE_CALL`, `GROUP_VOTE`, `CHARTER_CONTEST`, appended after `DUEL` so ordinals stay stable) show on Home as cards and the corner toast like the existing four.

### 10.3 Profile

`ProfilePage` shows the group as `Fact.GROUP` (was `PACK`; the old value keeps its wire name for old clients, section 11) with the type noun; `ProfileActions.Viewer` gains the viewer's `GroupType` and rank, so the Profile of a clanmate offers Promote / Exile and of a coven member Appoint Elder / Call a shunning vote, exactly when the buttons would show on the group page.

### 10.4 Safe screenshots

Per `docs/safe-screenshots.md`: hidden hub shots for each of the four types in each of the three themes (12) plus the Charter, circle, Map and Code cards, none with a live account name. The visual pass is part of 20a's last task for the shared page and of 20b/20c for their cards.

## 11. Network and protocol

No new C2S payload: actions are typed commands (the pattern the pack page uses). One new S2C payload and one enum extension:

- `GroupViewPayload` (S2C, optional channel on `LadderNet`'s registrar, client handler named only on the client like its neighbours). Sent where `HubSnapshotPayload` and `HubRequestsPayload` are sent (on `HubSnapshotRequestPayload` with the same 20-tick cooldown, and on `HubRequests.changed`). Content: group type (byte), viewer's rank (byte), leader name, `since` days, member rows (name, rank string as today plus a one-letter group rank, online, optional UUID; capped at 64), then a type block: coven (Elders flags, tier, circle state, the viewer's meter and consent and a share preview, the open vote with counts only), clan (Charter state, holder name, contest line, sireline mates count), brotherhood (the viewer's own kills, the leader's kills, whether any brother has 20, the viewer's strikes and Code-breaker time, the current bonus percent; never another brother's kills), pack (nothing new). It carries **nothing about people outside the viewer's group** and never another player's meter, consent or vote choice.
- Why a new payload and not more fields on `HubSnapshotPayload`: that record already has 8 fields written by hand ("more than `StreamCodec.composite` takes"), and the pack-moon branch is adding a ninth concern to it. A separate payload keeps the two branches independent.
- `HubRequests.Kind` gains `CIRCLE_CALL`, `GROUP_VOTE`, `CHARTER_CONTEST` at the end.
- `LadderNet.PROTOCOL`: raise by one at merge time (10 on main today, 11 on the pack-moon branch); the plan says "next integer after main's at merge", and `SnapshotIdsWireTest` is updated with it. The channel is optional, so a mismatch hides the hub's group data but does not refuse the connection (documented behaviour of `LadderNet`).
- Bounds and hostile sizes: every list capped in the codec; a decode of an oversize count fails the packet cleanly (tests: round trip, max size, count over the cap).
- Server authority: the client decides nothing. Ranks, shares, votes, Charter state are computed on the server from `GroupIndex`/`PackIndex`; the client only draws and sends commands.
- Admin: Admin rows ride the existing `AdminSnapshotPayload`; `AdminEntries.ALL.size() <= AdminSnapshotPayload.MAX_ROWS` is already tested and must still hold (174 + 5 + 21 + 16 + 16 = 232, before the pack-moon row and 19a's 7 to 12: check `MAX_ROWS` in 20a task 7; raise it with the protocol bump if not).

## 12. Out of scope (parked, from the lore seeds)

Celestial battery (C4), Great Harvest (C5), consecration bank (C8), forbidden rite (C10), Merge succession (C11), Fete de Cadeau (C12), night watch (C13), the Elder's Chain (V9), Aliquid sub sole (V4), initiation trial (V5), blood party and Fight Night (V6), sire bond (V8), Council and Death Dealers (V10), the pack seeds P1-P10, the cosmetic "backs turned" shunning walk; for the Brotherhood: sharing mark progress, the Cure road (the map leading to the Cure, R297 is only the picture), a reward for hunting only prey, Mikael's call (telling hunters whom Mikael hunts would reveal a private hunt), the Bestiary, forged silver bullets, the honour suicide of the Argents (rejected). Lists are in the lore file section 6; each can be its own later plan on this base.

## 13. Verification: hooks read with javap, and what is not verified

Read from the jars in the Gradle cache (`~\.gradle\caches\modules-2`):

| Fact | Jar | Result |
|---|---|---|
| `BloodDrinkEvent` is an abstract `Event` with `getVampire()`, `getAmount()`, `getSaturation()`, `getBloodSource()`, `setAmount(int)`, `setSaturationModifier(float)`; subclasses `PlayerDrinkBloodEvent(IVampirePlayer, int, float, boolean useRemaining, IDrinkBloodContext)` and `EntityDrinkBloodEvent` | Vampirism 1.21-1.10.13 api | verified. Not cancellable (no `ICancellableEvent`). |
| `IDrinkBloodContext`: `getEntity()`, `getStack()`, `getBlockState()`, `getBlockPos()` (all `Optional`), `static none()` | same | verified |
| `VampirePlayer.drinkBlood(int, float, boolean, IDrinkBloodContext)` fires `VampirismEventFactory.fireVampirePlayerDrinkBloodEvent`, then `bloodStats.addBlood(getAmount(), getSaturation())`, then `handleSpareBlood` only if `useRemaining` | Vampirism 1.21-1.10.13 (class) | verified in bytecode |
| `IVampire.drinkBlood(int, float, IDrinkBloodContext)` and the 4-arg form are public defaults; `useBlood(int, boolean)` | api | verified. **A call on a mate re-fires the event.** |
| `de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION` | Werewolves 1.21-2.0.3.4 | verified |
| Enchanted `RiteType` has `getPower()`, `getTickPower()`; `Rite` has `onStart(RiteParams) boolean`, `onTick`, `onStop`, `cancel()`, `start()`, `stop()`, `tick()`; `Rite.RiteParams` has `consumedItems`, `caster`, `target`; `GoldChalkBlockEntity implements PowerConsumer` with `tryConsumePower(int)` and static `serverTick`; `RiteManager` has static `addRite`, `removeRite`, `getRiteAt` | Enchanted 4.2.7 | verified (signatures only) |
| `RiteCost.begin/commit/discard` and `RiteCostMixin` into `GoldChalkBlockEntity` exist; `MixinHooksGameTests` asserts the hook landed | repo main | verified by reading |

**Not verified (flagged, each has a task that checks it first):**

1. `RiteManager.removeRite` / `Rite.stop()` is enough to abort a running rite on the anchor's death (only signatures were read). 20b task 6 proves it in a GameTest or drops that line of R256.
2. `RiteCost.begin(start = true)` is called exactly once per rite start; the siphon `Pending` design assumes it. 20b task 2 adds a GameTest that counts calls.
3. `ItemEntityPickupEvent.Pre` can deny a pickup in this NeoForge (21.1.x). 20c task 4 reads it; the sweep fallback is written either way.
4. The sireline index API for "same sireline" (the code lives in `parts/species/sireline` and `parts/species/line`; names were not read). 20c task 1.
5. `Detectors.onDrinkBlood` ignores events without a living source, and the forced feed / give blood of plan 18f (`core/blood/ForceFeedRules`, `GiveBloodGameTests`; merged after this was read) either carries no living-entity context or is excluded from sharing. 20c task 1.
6. Curios slots (rings worn in curio slots) do not count as "held" for the Charter; only the main inventory, hotbar and offhand are scanned. Decision, not a hook; a test pins it.
7. Plan 19a is **not merged**: `feature/plan-19a` (212d84ce) is local only (no `origin/feature/plan-19a`), 6 commits ahead of main. The two EP call sites named here come from reading that local branch (`EvolutionRiteFactory.grant`, mastery apply) and may move.
8. Sun-damage and moon-control checks used by the ring paths are existing code (R21, R96) and are not changed.
9. Brotherhood (read in the repo, not javap: they are our own classes): `MarkTracker`, `HunterCurse`, `FiveRoster`, `HunterInstinct`, `GlowPayload.send`, `KolRules`, `SparkHandler.isInnocent` exist as described in 8b.1 (read on main 3a6df244). **Unverified:** a client ignores a glow id for an entity it has not loaded (so a brother far from the prey loses nothing); `Supernatural.isPrey` is public and usable from the group package (`MarkTracker.isPrey` is package-private); `BossDirector` has a single place where Kol's target is chosen for the warning; the `BloodlineRules` event table can take `CODE_BROKEN` without touching the saved codec (a new enum value, ordinals appended). 20d task 1 reads each.

## 14. Risks

| # | Risk | Likelihood | Mitigation |
|---|---|---|---|
| 1 | Merge collision with `feature/pack-moon-control` (`PackIndex`, `HubSnapshotPayload`, `LadderNet`, `HubSocial`, `AdminEntries`, `SpeciesConfig`, `BookPackPage`, `PackPage`, `SPECIES-RULES.md`) | high | 20a does not edit `PackIndex` or `HubSnapshotPayload` (new `GroupIndex`, new `GroupViewPayload`); it rebases on whichever lands first; protocol is "next after main's". Task 0 of 20a is a rebase-and-recount step. |
| 2 | 19a moves the EP spend sites 20b refactors | medium | 20b starts after 19a merges (D6); its task 1 re-reads the two sites; the source-scan test fails loudly if a third site appears. |
| 3 | A coven link makes expensive rites cheap enough to break the economy | medium | Members pay 70% in a body-bound meter; the altar still pays 30%; shares are a hard gate; every number is an Admin row; worked table in 6.2 is a test fixture. |
| 4 | Charter edge cases (dupes, void, unloaded chunks, crashes) | medium | Server row is the truth; duplicates become dust; LOST + Reforge always recovers; contests only with the leader online and an online holder; the GameTest list in 20c covers each. |
| 5 | Shared feeding exploits (alt farm, loops) | medium | Living-source only, not between clanmates as victims, per-mate per-minute cap, re-entrancy guard, radius. |
| 6 | Registration split breaks no-Werewolves boots | medium | Matrix run 2 (`-PnoWerewolves`) gets a new GameTest "a coven forms and the absence rule runs without Werewolves"; run before anything else in 20a. |
| 7 | Type derivation flaps for a hybrid cured and re-bitten | low | Perks are live and nothing is deleted; one notice per change. |
| 8 | Hub art (two ring sprites per theme x 2 types) is not ready | medium | Placeholder tint of the existing ring ships; art is a separate task. |
| 9 | UI regression in the three themes | medium | The pages are parametrised, not forked; pack screenshots must be pixel-identical before/after (hidden shots); `PackUiTest`, `PackPanelTest` and `PackRingTest` keep passing unchanged. |
| 10 | `AdminSnapshotPayload.MAX_ROWS` | low | Checked in 20a task 7. |
| 11 | Save size: `votes` and `channel` grow | low | Channel rows are dropped when full and idle (value = capacity); votes die after 7 days. |
| 12 | The Code punishes by mistake (a villager killed by a splash, a player who was fighting a brother but not within 10 s) | medium | `isInnocent`'s fight window, prey and hostile mobs excluded, two breaks on one victim within a minute count once, three strikes (not one) expel, all numbers are Admin rows, `code_enabled` switches it off. |
| 13 | Leadership by mark is a takeover by anyone who gets a mark | medium | Only 5 marked hunters exist; the takeover needs the new leader online, strictly higher, no Code-breaker; marks are server-recorded, not client data. |
| 14 | A marked hunter joins a brotherhood only to take it over | low | Joining needs the leader's invite (R242); the old leader stays a Brother; one `kick` by the new leader is allowed like any leader. |
| 15 | The curse timer for a non-marked brother's killer is a new lift path next to `FiveRoster`'s | low | It only ends a flag the same code sets; `GroupIndex.curses` is dormant data (Q6); a test covers "a Potential's kill still ends it at once". |
| 16 | Plan 18f / 20d overlap on `BloodlineRules` events | low | Events are appended; the codec test of the event enum runs. |

## 15. Tests and failure modes

Per plan (each plan lists its own, with a mutation table); the cross-cutting list:

- **Pure (JUnit):** type derivation table (with the hunter rows); `LeadByMark` (strictly higher takes it, tie keeps, offline member cannot take it, Code-breaker cannot, a mark reset passes the lead on); `ArmsBonus` (count, cap, distance, dimension, Code-breaker); `CodeRules` (innocent table, penalty 25% with a minimum of 1, the strike window, one break per victim per minute); `CurseExtend` (already cursed changes nothing, a non-marked brother's curse gets an end time, a Potential's kill ends all); `MapRegions` (thresholds 1/4/8/12/16/20, completion from any brother); switch plan; heir by type; ban window arithmetic; circle plan (the worked table in 6.2 as fixtures; shares add up; drop-and-recompute converges; consent levels; EP floor); potency curve values and monotonic diminishing returns; tier thresholds; vote tally (strict majority, non-voters count no, electorate under 3); shared-feed share maths and caps; Charter state machine (every transition, duplicates, LOST, reforge); contest resolution table; codec round trips and hostile sizes.
- **GameTests:** the registration without Werewolves; a hunter founds a brotherhood and prey cannot join; a marked member passes the founder and takes the lead (and the old leader stays); the Code (an innocent kill costs the mark and starts the breaker timer, the third strike expels); a prey kills a brother and is cursed, a second prey killer changes nothing, the 48-hour end; shared sight reaches a brother without the sense; Kol's warning; a coven and a clan invite/kick/leave/ally; absence by type; type switch with no member lost; OPAC link for a clan with the fake backend; a linked rite charges members only on `commit` and nothing on `discard`; the anchor's death penalty (and lethal option, and logout not penalised); shared feeding with a real bite; the Charter contest end to end with two players; each Charter protection.
- **Failure modes that must not throw:** leader missing from the profile cache; a member row for a UUID that never logged in; a chalk replaced mid-circle; a dimension change mid-circle; two circles on one chalk; a vote whose subject left; a contest whose challenger left; `GroupIndex` load with a half-written list; the group id of a dissolved pack; the Charter row naming a group that no longer exists (dropped with one warning).

## 16. Owner questions and decisions

### 16.1 Decided (the first review, owner approved all six picks on PR #83)

1. **Q1 What pays a coven's power share:** members pay 70% of the altar bill from a per-witch channel meter (cap 3000, refills in 10 minutes); the altar still pays 30%; EP is split exactly as decided (6.2).
2. **Q2 Grand rites:** none of the existing rites becomes grand and 19a's rites are not coupled to tiers (they only get the cost split); 20b ships the `min_circle` gate and three new grand rites, one per tier: Fete des Benedictions (3), Night Watch (7), Great Harvest (13, items and XP, never kills).
3. **Q3 Edge kinds:** an Original leads a clan; Original hybrid and tribrid lead packs; heretic, human and potential derive as covens, and a coven still needs a magic user to found unless `open_founding` is on; a heretic who is a vampire does not make a clan. (A hunter now derives BROTHERHOOD, A5.)
4. **Q4 Rings:** no new items. A coven circle of 3 or more forges the existing Moon Ring pre-bound for half the silver, and a Sun Ring that binds at 50% power for a Day Walker. Every present recipe and bind is unchanged.
5. **Q5 The Charter contest:** a Day Walker with 7 days in the clan may challenge; the leader must accept; one contest per 7 days; the holder at midnight must be online and a Day Walker or the leader; the Charter lives only with clan members.
6. **Q6 A type switch** keeps type-specific data dormant and restores it when the leader returns to the old kind (nothing is deleted).

### 16.2 Decided: plan 20d, Brotherhood (the owner approved all four picks on PR #83)

1. **Q7 A higher mark takes the lead at once.** The moment a member who is online has a strictly higher mark than the leader, the lead moves (the old leader stays a Brother); the founder leads until then; a tie keeps the leader (R291).
2. **Q8 The Code punishes only.** An innocent kill costs 25% of the mark (at least 1 kill) and a day as a Code-breaker (no group perks, no lead); the third break in a week expels for a week; nothing rewards prey-only hunting (R294).
3. **Q9 A non-marked brother's curse ends after 48 real hours,** or at once when a Potential awakens as today (R295).
4. **Q10 Shared mark is the hunting-together bonus plus shared sight** (+3% attack per brother within 24 blocks, up to +12%); the map is a shared picture; kills are never shared (R292, R293, R297).