# Plan 20a: the shared group base (packs, clans, covens and brotherhoods on one index): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generalise the pack system into groups with a type derived from the leader (pack, clan, coven, brotherhood): a new `GroupIndex` next to the untouched `PackIndex`, a type derivation with a lossless switch, generalised founding / invite / leave / kick / ally / absence / Open Parties and Claims link, a `/group` command tree (with `/clan`, `/coven` and `/sb group`), a group view for the hub, the Pack page parametrised per type, and Admin rows. Clan, coven and brotherhood **perks** are 20c, 20b and 20d; this plan ships none of them (the Brotherhood is a type, a gate, a label and a succession rule here, nothing more).

**Architecture:** Pure rules in `core.group`. `PackIndex` (the membership graph) keeps its class, SavedData name and NBT keys. `GroupIndex` (SavedData `supernaturalbloodlines_groups`) holds group ids, types, ranks and bans, and follows leaders through `PackIndex.onLeadershipMoved / onDissolved`. The pack stack's registration is split so the type-agnostic parts run without Werewolves. One new S2C payload carries the group view; actions stay typed commands sent by hub buttons.

**Tech Stack:** Java 21, NeoForge 1.21.1, Vampirism 1.10.13, Werewolves 2.0.3.4 (compile only, optional at run time), JUnit 5, the repo's GameTest runner.

**Spec:** [docs/superpowers/specs/2026-11-23-groups-design.md](../specs/2026-11-23-groups-design.md). Read it first (sections 2, 3, 4, 9, 10, 11); this plan does not repeat it. Lore: [research/groups-lore.md](../research/groups-lore.md).

Repo conventions that apply: one branch `feature/plan-20a-groups-base`, one PR, a `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/species.md` ticked, the R-rules copied into `docs/superpowers/specs/SPECIES-RULES.md` as **R240 to R249**, no narrating code comments, hub shots per `docs/safe-screenshots.md`.

## Global Constraints

- **No new Part.** Groups are the pack system generalised, inside `species`; `Part`, `requiredParts` and the 12-run matrix do not change. Packages: pure `net.ofatech.supernaturalbloodlines.core.group`; runtime `net.ofatech.supernaturalbloodlines.parts.species.group`; payload `net.ofatech.supernaturalbloodlines.parts.supernatural.net.GroupViewPayload`; client `parts.supernatural.client.GroupClient`.
- **`PackIndex` is not edited** except the one registration split in Task 3 (a method split, no field, no NBT key). `PackIndex.NAME` stays `supernaturalbloodlines_packs`. `HubSnapshotPayload` is not edited. Both are touched by `feature/pack-moon-control`.
- Rule ids R240 to R249 (spec section 4). Lang prefix `supernaturalbloodlines.group.`; the existing `supernaturalbloodlines.pack.*` keys stay.
- Config file `supernaturalbloodlines-groups.toml`, section `groups`, keys `groups_enabled` (true), `clans_enabled` (true), `covens_enabled` (true), `open_founding` (false), `max_members` (0 = unlimited). `AdminEntries.ALL.size()` becomes **(main's size at the time) + 5** (174 + 5 = 179 on main today, after plan 18f; add the pack-moon row and 19a's rows if they landed).
- Wire: new S2C `GroupViewPayload` on the existing optional registrar of `LadderNet`; `LadderNet.PROTOCOL` becomes **the integer after the one on `main` at merge time** (10 today, 11 on the pack-moon branch); `SnapshotIdsWireTest` follows. `HubRequests.Kind` is not extended in 20a.
- Founding gate (spec 3.2): PACK needs `isActiveAlpha` (unchanged), CLAN a vampire, COVEN a magic user (`WITCH, SIPHONER, HERETIC`), BROTHERHOOD a hunter (Vampirism hunter faction, Hunter of the Five, Potential); `open_founding` lifts it.
- Type table (spec 3.1) is the one in `GroupTypes.of`; every new test names its row.
- GameTest runs one at a time with `--max-workers=2`; restore `run/config` after any killed run.

## Review Focus

Inputs the spec implies and no happy path covers, most likely first. Each has a test in the task named in brackets.

1. A leader is offline at the first load: the seed must still call the group a pack, never derive from facts it cannot read. [Task 2: `seedNeverReadsAnOfflineLeader`]
2. A world with no Werewolves: `PackIndex`'s login/logout, `PackAbsence` and `PartyLink` must run there, or a coven's leader never "leaves" and the absence rule is dead. [Task 3: `GroupNoWerewolvesGameTests.aCovenRecordsLastSeen`]
3. A werewolf Beta whose `WolfState.packAlpha` still names a leader who is now a clan's head: nobody is kicked, the index and the wolf state stay in step. [Task 5: `aSwitchLosesNobody`, `aWolfBetaSurvivesTheirLeadersSwitch`]
4. A leader dissolves a pack and invites again the same minute: the new group gets a new id, the old rows are gone, no stale rank leaks. [Task 2: `aDissolvedGroupLeavesNoRows`]
5. A type switch and a pack duel on the same tick: the duel is cancelled by the existing pack rules, the switch still notifies once. [Task 5: `aSwitchDuringADuel`]
6. Two groups ally across types, then one switches type: the alliance survives (allies are leader-to-leader). [Task 6: `allianceSurvivesATypeSwitch`]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`):
- `core/group/GroupType.java`, `GroupTypes.java`, `GroupRank.java`, `GroupSwitch.java`, `GroupSuccession.java`, `GroupWire.java` (the pure size bounds shared by codec and tests): pure.
- `parts/species/group/GroupIndex.java`, `Groups.java` (facade: type of a player's group, noun key, lookups), `GroupGate.java`, `GroupCommand.java`, `GroupSwitcher.java`, `GroupText.java`, `GroupViews.java`, `GroupsConfig.java`, `GroupsRegistry.java` (the unconditional registration).
- `parts/supernatural/net/GroupViewPayload.java`; `parts/supernatural/client/GroupClient.java`.
- `parts/supernatural/client/hub/page/GroupLabels.java`, `GroupButtons.java`.
- `parts/species/gametest/GroupNoWerewolvesGameTests.java`, `GroupInviteGameTests.java`, `GroupTypeSwitchGameTests.java`, `GroupAbsenceGameTests.java`.
- `docs/parts/species.md` (extend), `wiki/Groups.md`.

Create (test, `src/test/java/net/ofatech/supernaturalbloodlines/`): `core/group/GroupTypesTest.java`, `GroupSwitchTest.java`, `GroupSuccessionTest.java`; `parts/species/group/GroupIndexTest.java`, `GroupTextTest.java`, `GroupsConfigTest.java`, `GroupUiCoverageTest.java`; `parts/supernatural/net/GroupViewWireTest.java`.

Modify: `parts/species/SpeciesPart.java` (registration split, HubSocial source install, config registration), `parts/species/pack/PackIndex.java` (registration method split only), `parts/species/pack/PackAbsence.java` (heir by type), `parts/species/pack/PackCommand.java` (gates call `GroupGate`; wording through `GroupText`), `parts/species/pack/AllyProtection.java` (type-blind), `parts/species/pack/party/PartyLink.java` (nothing but the duel check stays pack-guarded), `shared/admin/AdminEntries.java`, test `shared/admin/AdminEntriesTest.java`, `AdminCoverageTest.java`, `parts/supernatural/net/LadderNet.java`, test `SnapshotIdsWireTest.java`, `parts/supernatural/client/hub/page/PackPage.java`, `BookPackPage.java`, `ProfilePage.java`, the three theme classes only if a label hook is needed, `en_us.json`, `CHANGELOG.md`, `SPECIES-RULES.md`.

---

### Task 0: Baseline and rebase (no code)

- [ ] **Step 1:** `git fetch origin`; branch `feature/plan-20a-groups-base` from `origin/main`. If `feature/pack-moon-control` has merged, note the new `LadderNet.PROTOCOL`, the new `AdminEntries.ALL.size()` (update the expected counts in this plan's Global Constraints in your PR notes) and the `hybrid_like` list in `PackIndex`. If it has not, **do not copy its changes**.
- [ ] **Step 2: Baseline run** `./gradlew test` and the species GameTests `./gradlew runGameTestServer` once on the clean branch; record the counts in the PR. Every later "unchanged" claim is measured against them.
- [ ] **Step 3: Hidden hub shots of the Pack page** in the three themes (Grimoire, Blood Moon, Tensura), with a pack of 3 online and 1 offline, saved outside the repo as the "before" set for Task 9.

---

### Task 1: The pure type rules

**Files:**
- Create: `core/group/GroupType.java`, `GroupTypes.java`, `GroupRank.java`.
- Test: `core/group/GroupTypesTest.java`.

**Interfaces:**
- Produces: `enum GroupType { PACK, CLAN, COVEN, BROTHERHOOD; String id(); static Optional<GroupType> parse(String) }` (`id()` is lower case: `pack`, `clan`, `coven`, the NBT and lang form).
- Produces: `enum GroupRank { NONE, DAY_WALKER, ELDER; String id(); static GroupRank parse(String) }` (a row is stored only for a non-`NONE` rank).
- Produces: `GroupTypes.Leader` record `(Species species, boolean vampire, boolean werewolf, boolean hunter)`; `static GroupType of(Leader)` (spec 3.1, in that order); `record Fit(boolean activeAlpha, boolean vampire, boolean magicUser, boolean hunter)`; `static boolean canFound(GroupType type, Fit fit, boolean openFounding)` (PACK `activeAlpha`, CLAN `vampire`, COVEN `magicUser`, BROTHERHOOD `hunter`; `openFounding` true means yes).

- [ ] **Step 1: Write the failing tests** (`GroupTypesTest`, one `@Test` each; the name is the table row):
  - `aPlainWerewolfLeadsAPack` (`NONE`, `werewolf = true`).
  - `hybridTypesLeadPacksEvenThoughTheyAreAlsoVampires` (`HYBRID`, `ORIGINAL_HYBRID`, `TRIBRID`, each with `vampire = true`).
  - `aWerewolfWhoIsAlsoAVampireLeadsAPack` (`NONE`, both true).
  - `aVampireLeadsAClan` (`NONE`, `vampire`), `anOriginalLeadsAClan`, `anAugustineLeadsAClan`, `aVampireSiphonerLeadsAClan` (`SIPHONER`, `vampire`).
  - `aSiphonerWhoIsNotAVampireLeadsACoven`, `aWitchLeadsACoven`.
  - `aHereticLeadsACovenEvenThoughRiteKindsCallItAVampire` (`HERETIC`, `vampire = true`; pins the decided Q3).
  - `aHumanIsCovenType` (`NONE`, no faction).
  - `aHunterLeadsABrotherhood` (the hunter faction with `NONE`; `HUNTER_OF_THE_FIVE`; `POTENTIAL`), `aWitchInTheHunterFactionLeadsABrotherhood` (A5), `aHunterIsNeverAPackOrAClan`, `aWerewolfBeatsAHunter` (both true, which cannot happen in play: the order is pinned).
  - `canFoundFollowsTheType`: PACK needs `activeAlpha`, CLAN `vampire`, COVEN `magicUser`, BROTHERHOOD `hunter`, each alone is not enough for the others.
  - `openFoundingLiftsTheGate`.
  - `idsRoundTrip`: `parse(t.id()) == t`, an unknown string is empty.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupTypesTest'`. Expected: compile error.
- [ ] **Step 3: Implement** the three types. No game classes, no logging, no state.
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(groups): the pure group type rules`.

---

### Task 2: `GroupIndex` and the migration

**Files:**
- Create: `parts/species/group/GroupIndex.java`.
- Test: `parts/species/group/GroupIndexTest.java`.

**Interfaces:**
- Consumes: `PackIndex` (`leaders()`, `leaderOf`, `betasOf`, `pack`, `onLeadershipMoved`, `onDissolved`, `load/save` for the fixture), `GroupType`, `GroupRank`.
- Produces: `GroupIndex extends SavedData`, NAME `supernaturalbloodlines_groups`, `data_version` 1; `static GroupIndex get(ServerLevel)` (overworld storage, like `PackIndex.get`) which also sets a static `live` reference, cleared on `ServerStoppedEvent` (the follow listeners have no server in hand).
- Produces: `UUID groupOf(UUID leader)` (get or create), `Optional<UUID> existingGroupOf(UUID leader)`, `GroupType typeOf(UUID group)` (default `PACK`), `void setType(UUID group, GroupType, long now)`, `long typeSince(UUID group)`, `GroupRank rankOf(UUID member)`, `void setRank(UUID member, GroupRank)`, `void clearRanks(UUID member)`, `void seed(PackIndex)` (one `PACK` row per leader, only when `data_version` is 0), `void follow(UUID oldLeader, UUID newLeader)`, `void drop(UUID leader)`, `static void register()` (installs the two `PackIndex` listeners), `static GroupIndex load(CompoundTag, HolderLookup.Provider)`, `save`.
- Rules: every list row read in its own try/catch with one `LOGGER.warn` per damaged row (copy `PackIndex.skipped`); unknown top-level keys are kept in a `CompoundTag extras` and written back; a row of a type this build does not know is kept as-is.

- [ ] **Step 1: Write the failing tests** (`GroupIndexTest`; a `PackIndex` is built with `new PackIndex()` and `join`, as `PackIndexTest` does):
  - `seedMakesOnePackPerLeader`: two leaders (one with 2 members, one with 1) give two groups, both `PACK`.
  - `seedNeverReadsAnOfflineLeader` (Review Focus 1): the seed takes only the index; there is no species argument, so it cannot flip a group. Asserted by calling `seed` with an index whose leaders have no profile and no state.
  - `seedRunsOnce`: a second `seed` after `data_version` is 1 adds nothing (a leader made after the seed gets its row from `groupOf`, not from `seed`).
  - `packIndexFileIsUntouched`: a fixture `CompoundTag` produced by `PackIndex.save` with every list populated (members, emissaries, anchors, former, dissolved, mirrors, parties, ended, notices, sparks, last_seen, joined_at, alive_at, strengths, allies) loads into `PackIndex.load`, and saving it again gives lists with the same rows (sets as sets, `members` in order). The test also builds a `GroupIndex`, seeds it and saves it, and asserts the `PackIndex` tag was not changed by that.
  - `idFollowsTheLeaderOnPromote`: `promote(old, next)` then `follow(old, next)`; `existingGroupOf(next)` is the id `groupOf(old)` had; `existingGroupOf(old)` is empty.
  - `idFollowsTheLeaderOnTransfer`.
  - `aDissolvedGroupLeavesNoRows` (Review Focus 4): ranks of members, the id, the type; a new group made by the same leader afterwards has a different id.
  - `ranksAreForgottenWhenAMemberLeaves`: `clearRanks`.
  - `aDamagedRowCostsOnlyThatRow`: a `ranks` list with one row of the wrong tag type and two good rows loads two.
  - `unknownKeysSurviveARoundTrip`: a tag with an extra top-level list and an extra row type loads and saves with them.
  - `aTypeOfAnotherBuildIsKeptDormant`: type `"warband"` in a row loads, `typeOf` answers `PACK`, and `save` writes `"warband"` back.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupIndexTest'`. Expected: compile error.
- [ ] **Step 3: Implement `GroupIndex`.** `save/load` follow `PackIndex`'s shape (lists of `{key, value}` compounds, `NbtUtils.createUUID`). `follow`/`drop` are the bodies of the two `PackIndex` listeners; `register()` adds them once (guard with a static flag, as the other `register` methods do) and they act on the `live` instance (no-op when null, which is the unit-test case).
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS.
- [ ] **Step 5: Wire the seed.** `GroupIndex.get` calls `seed(PackIndex.get(level))` when `data_version` is 0 and marks dirty. A GameTest is not needed: `seedMakesOnePackPerLeader` covers it, and Task 3's GameTest loads a real index.
- [ ] **Step 6: Commit** `feat(groups): the group index, its seed and the leader follow`.

---

### Task 3: Register the type-agnostic pack stack without Werewolves

**Files:**
- Create: `parts/species/group/GroupsRegistry.java`, `parts/species/gametest/GroupNoWerewolvesGameTests.java`.
- Modify: `parts/species/SpeciesPart.java` (lines ~205-335: the `ModList.get().isLoaded("werewolves")` blocks), `parts/species/pack/PackIndex.java` (split `register()` only), `parts/species/pack/party/PartyLink.java` (registration only), `parts/species/pack/PackAbsence.java` (registration only).

**Interfaces:**
- Produces: `PackIndex.registerSession()` (the `PlayerLoggedInEvent` / `PlayerLoggedOutEvent` listeners: last seen, notices, dissolved) and `PackIndex.registerWolf()` (the `BloodlineAccess` wolf-sync listener); `PackIndex.register()` calls both, so Werewolves worlds are unchanged. `GroupsRegistry.registerAlways()` calls `registerSession()`, `PackAbsence.register()`, `PartyLink.register()`, `GroupIndex.register()`, the `GroupCommand` registration (Task 4) and the `HubSocial` source install, with **no** Werewolves check; `SpeciesPart` calls it once.
- Everything Werewolves-only stays behind the existing check: `registerWolf`, `PackDuel`, `PackCommand`'s werewolf subcommands, the spark and roar handlers, `AlphaTrial`.

- [ ] **Step 1: Read** `SpeciesPart` lines 130-340 and list, in the PR, which registrations move and which stay. `HubSocial.set(...)` (the pack rows of the hub) must be installed whether or not Werewolves is loaded; if its source reads a werewolf class, split the werewolf rows out of it behind the check.
- [ ] **Step 2: Write the failing test** `GroupNoWerewolvesGameTests` (runs only under `-PnoWerewolves`, skips itself otherwise, as the other Werewolves tests do the other way round): `aCovenRecordsLastSeen` (a witch leads, a human is a member; the member's logout event stores a last-seen time; Review Focus 2), `aCovenLeaderWhoStaysAwayIsSeenAsAbsent` (pin the clock, move 7 days; `PackAbsence.check` finds the leader absent and, with no heir, changes nothing and does not throw), `theHubSourceIsInstalled` (`HubSocial.of(member).pack()` lists the coven).
- [ ] **Step 3: Run** `./gradlew runGameTestServer -PnoWerewolves -PnoCurios`. Expected: FAIL (nothing registered).
- [ ] **Step 4: Implement** the split and `GroupsRegistry`. Run the same command: PASS. Then run the Task 0 baseline GameTests **with** Werewolves: counts equal the baseline.
- [ ] **Step 5: Commit** `refactor(groups): the type-agnostic pack stack registers without Werewolves`.

---

### Task 4: The gate, the shared actions and the command tree

**Files:**
- Create: `parts/species/group/Groups.java`, `GroupGate.java`, `GroupText.java`, `GroupCommand.java`, test `GroupTextTest.java`, `gametest/GroupInviteGameTests.java`.
- Modify: `parts/species/pack/PackCommand.java` (every `isActiveAlpha(...)` gate on invite, kick, ally invite/accept/remove becomes `GroupGate.canLead`; wording through `GroupText`), `pack/AllyProtection.java` (no type test), lang.

**Interfaces:**
- Produces: `Groups.typeOf(ServerPlayer)` (the type of the group the player is in, else the type their own kind would lead, from `GroupTypes.of` with the live facts: `BloodlineAccess.get(p).species()`, `VampirismSupernatural.isVampire(p)` (use the existing vampire check `RiteKinds` callers use), `VampirismSupernatural.isWerewolf(p) || wolf lineage != NONE`); `Groups.typeOfGroup(ServerLevel, UUID leader)`.
- Produces: `GroupGate.canLead(ServerPlayer): Optional<String>` (empty = allowed, else the refusal key without prefix: `not_alpha`, `not_fit`, `group_off`, `full`): applies `GroupsConfig` switches (`groups_enabled`, per-type switch), `max_members`, `GroupTypes.canFound(...)` with `Fit(activeAlpha = PackCommand.isActiveAlpha, vampire, magicUser = species in WITCH, SIPHONER, HERETIC, hunter = BaseFaction.HUNTER or species HUNTER_OF_THE_FIVE / POTENTIAL)`.
- Produces: `GroupText.key(GroupType, String base)` = `supernaturalbloodlines.group.<base>.<type>`; `GroupText.error(GroupType, base, args...)` with a fallback to the existing `supernaturalbloodlines.pack.<base>` for a pack; `GroupText.sharedBases()` = the list of bases the shared actions use (`invited`, `invite`, `invite_merge`, `accept`, `accept_hover`, `joined`, `joined_leader`, `left`, `kicked`, `kicked_ok`, `ally_invite`, `ally_invited`, `ally_made`, `ally_ended`, `not_leader`, `not_member`, `self`, `target_has_pack` as `target_in_group`, `has_pack` as `in_group`).
- Produces: `GroupCommand.register(dispatcher)`: `/group`, `/clan`, `/coven`, `/brotherhood` (the same node, Brigadier `redirect`), `/sb group`; literals `invite <player>`, `accept [token]`, `leave`, `kick <name>`, `ally invite|accept|remove`, `info`, `challenge ...` (routes by type; 20a answers `wrong_type` for CLAN and COVEN until 20b/20c). `/pack` keeps its whole tree and is registered as before.

- [ ] **Step 1: Write the failing tests.**
  - `GroupTextTest` (data test, reads `en_us.json`): for every base in `sharedBases()`, keys exist for `pack` (the old key), `clan`, `coven` and `brotherhood`; no `clan`/`coven`/`brotherhood` key is a copy of the pack text containing the word "pack".
  - `GroupInviteGameTests`: `aWitchInvitesAndAHumanJoinsACoven` (type `COVEN`, `Groups.typeOf(member)` `COVEN`), `aVampireFoundsAClan`, `aHunterFoundsABrotherhoodAndAHumanJoins`, `aVampireCannotFoundABrotherhood`, (the refusal of prey is 20d, R290), `aHumanCannotFoundAGroup` (refusal `not_fit`), `openFoundingLetsAHumanLeadACoven` (`GroupsConfig.override("open_founding", true)`), `anAlphaStillFoundsAPackExactlyAsBefore` (the existing `WerewolfPackGameTests` expectations, re-asserted through the new gate), `aClanCannotBeFoundedWhenClansAreOff`, `maxMembersRefusesTheInviteAtTheCap`, `kickAndLeaveWorkInACoven`, `allyAcrossTypes` (a pack leader and a coven leader ally; `alliedPlayers` true), `alliedMembersCannotHurtEachOtherAcrossTypes` (damage cancelled both ways), `aWolfOnlyLiteralAnswersWrongTypeInACoven` (`/pack anchor` is a pack action; through `/coven` the shared literals only), `slashPackStillWorks`, `slashSbGroupIsTheSameTree`.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupTextTest'` (expected: FAIL, no keys) and `./gradlew runGameTestServer` (expected: FAIL).
- [ ] **Step 3: Implement** `Groups`, `GroupGate`, `GroupText`, `GroupCommand`; replace the gates in `PackCommand` (leave every method signature and every pack message key as is; a pack's player sees today's text). In `AllyProtection` remove the "is a pack" test if there is one; allies are leader-to-leader and type-blind.
- [ ] **Step 4: Lang.** `supernaturalbloodlines.group.<base>.<type>` for clan, coven and brotherhood (and `pack` too for the new bases that have no old key: `joined_leader`, `in_group`, `target_in_group`, `wrong_type`, `not_fit`, `group_off`, `full`). Plain words, no show names (R5).
- [ ] **Step 5: Run** both commands. Expected: PASS; the baseline pack GameTests are unchanged.
- [ ] **Step 6: Commit** `feat(groups): one gate, one wording layer and the /group command tree`.

---

### Task 5: The type switch

**Files:**
- Create: `core/group/GroupSwitch.java`, `parts/species/group/GroupSwitcher.java`, test `core/group/GroupSwitchTest.java`, `gametest/GroupTypeSwitchGameTests.java`.
- Modify: `GroupsRegistry.java` (listeners), `GroupIndex.java` if a helper is missing.

**Interfaces:**
- Produces (pure): `GroupSwitch.plan(GroupType stored, GroupType derived): Optional<Switch>` with `record Switch(GroupType from, GroupType to)`; empty when equal. `GroupSwitch.dormant(GroupType active, GroupType rowType): boolean` (a row is dormant unless its type is the active one).
- Produces: `GroupSwitcher.refresh(ServerPlayer leader)`: derive (`Groups.typeOf`), compare with `GroupIndex.typeOf(group)`, apply the switch (spec 3.4): `setType`, one `PackIndex.addNotice` per member (`supernaturalbloodlines.group.type_changed` with the new noun as the argument; online members hear it now through `PackMembers.tell`), cancel in-flight group events (20a has none; the hook method `GroupSwitcher.onSwitched` is a list other plans append to). Called from the `BloodlineAccess` listener of a leader, `PackIndex.onLeadershipMoved` (new leader), login, and the 200-tick sweep.
- **Never** deletes a row; never calls `PackIndex.kick/leave/dissolve`.

- [ ] **Step 1: Write the failing tests.**
  - `GroupSwitchTest`: `equalTypesPlanNothing`, `aChangePlansTheTwoTypes`, `dormantMeansNotTheActiveType`.
  - `GroupTypeSwitchGameTests`: `aHybridLeaderWhoBecomesAHumanSwitchesToACoven` (use `debug set <player> <species>` equivalents through `BloodlineAccess`), `aSwitchLosesNobody` (Review Focus 3: members of the index and every `WolfState.packAlpha` identical before and after), `aWolfBetaSurvivesTheirLeadersSwitch`, `aSwitchIsToldOnce` (run the sweep five times: one notice per member; an offline member has one waiting notice), `switchBackRestoresDormantRows` (a rank row written while the group was a clan is invisible to `GroupText`/view while it is a coven and is back after the switch back), `aSwitchDuringADuel` (Review Focus 5), `anOfflineLeaderIsNotSwitched` (the sweep leaves an offline leader's group at the stored type), `typeSinceIsStamped`.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupSwitchTest'` then `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** Keep `GroupSwitcher` free of Werewolves classes (it reads the leader through `Groups.typeOf`).
- [ ] **Step 4: Run** the Step 2 commands. Expected: PASS.
- [ ] **Step 5: Mutants** for this task (M5, M6, M13 of Task 10).
- [ ] **Step 6: Commit** `feat(groups): a leader who changes kind switches the group's type`.

---

### Task 6: Absence by type, the lead moving, the party link

**Files:**
- Create: `core/group/GroupSuccession.java`, test `core/group/GroupSuccessionTest.java`, `gametest/GroupAbsenceGameTests.java`.
- Modify: `parts/species/pack/PackAbsence.java` (`check` and `depose`), `pack/party/PartyLink.java` (only if a pack-only call remains outside the duel check).

**Interfaces:**
- Produces: `GroupSuccession.Candidate(UUID id, GroupRank rank, long joinedAt, boolean here, int mark)` (`mark` is -1 for a candidate who is not online); `static Optional<UUID> heir(GroupType type, List<Candidate> members)`: CLAN the `DAY_WALKER` who is `here` with the smallest `joinedAt`; COVEN the `ELDER` who is `here` with the smallest `joinedAt`; BROTHERHOOD the candidate who is `here` **and online** (`mark >= 0`) with the highest `mark`, ties the smallest `joinedAt`; PACK is always empty here (the pack path keeps `SparkHandler.coAlphaHeir`, called by `PackAbsence`); ties by UUID order.
- `PackAbsence.check` picks the heir by `Groups.typeOfGroup`: PACK as today, CLAN/COVEN through `GroupSuccession.heir`. `depose` for CLAN/COVEN: `PackIndex.promote(old, heir)`, then `GroupIndex.setRank(old, DAY_WALKER | ELDER)`, `clearRanks(heir)`, `PartyLink.leadershipChanged`, the same three notices (`absent_lead_now`, `absent_leader`, `absent_you`); `SparkHandler.repoint` only for PACK.

- [ ] **Step 1: Write the failing tests.**
  - `GroupSuccessionTest`: `aClanTakesTheLongestDayWalkerWhoIsHere`, `aNightWalkerIsNeverAnHeir`, `anAbsentHeirIsSkipped` (the next one is taken; `here = false` for all gives empty), `aCovenTakesTheLongestElder`, `aPackIsLeftToTheSparkRule`, `aBrotherhoodTakesTheHighestOnlineMark`, `anOfflineMarkNeverInherits`, `ties by id`.
  - `GroupAbsenceGameTests` (clock pinned with `PackIndex.overrideClock`, `SpeciesConfig.overrideAbsentLeaderDays(7)` as the existing absence tests do): `aClanLeaderAbsentSevenDaysLosesTheLeadToADayWalker`, `aCovenLeaderAbsentSevenDaysLosesTheLeadToAnElder`, `aBrotherhoodLeaderAbsentLosesTheLeadToTheHighestMark` (the old leader stays a Brother), `noHeirChangesNothing`, `theOldLeaderStaysAMemberWithTheNextRank`, `allianceSurvivesATypeSwitch` (Review Focus 6), `aClanIsMirroredIntoThePartyToo` (`PartyLink.setBackend` fake, the existing `PartyLink` tests' fixture), `thePackAbsenceTestsStillPass` (run the existing pack tests unchanged).
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupSuccessionTest'` then `./gradlew runGameTestServer`. Expected: FAIL.
- [ ] **Step 3: Implement.** Step 4: run, expected PASS. Step 5: **Mutant** M9.
- [ ] **Step 6: Commit** `feat(groups): leader absence picks the heir of the group's type`.

---

### Task 7: Config, Admin rows and the part-off switches

**Files:**
- Create: `parts/species/group/GroupsConfig.java`, test `GroupsConfigTest.java`.
- Modify: `parts/species/SpeciesPart.java` (register the config like the other files of the part), `shared/admin/AdminEntries.java`, tests `AdminEntriesTest.java` (expected size), `AdminCoverageTest.java` (`CONFIGS`), lang.

**Interfaces:**
- Produces: `GroupsConfig.FILE = "supernaturalbloodlines-groups.toml"`, `SPEC`; `static boolean groupsEnabled()`, `clansEnabled()`, `covensEnabled()`, `openFounding()`, `int maxMembers()`; `static void override(String key, Object value)` (test hook, `null` restores, as `BossesConfig.override`). Before the config loads, the defaults answer.

- [ ] **Step 1: Write the failing tests.** `AdminEntriesTest` size +5; `AdminCoverageTest.CONFIGS` gets `new Known(GroupsConfig.FILE, GroupsConfig.SPEC, true, "GroupsConfig.java")`; `GroupsConfigTest`: `defaultsAnswerBeforeTheFileLoads`, `overrideRestores`, `aNegativeMaxMembersIsClamped` (range 0 to 100000).
- [ ] **Step 2: Run** `./gradlew test --tests '*AdminEntriesTest' --tests '*AdminCoverageTest' --tests '*GroupsConfigTest'`. Expected: compile error.
- [ ] **Step 3: Implement** the config (copy `BossesConfig`'s shape), `AdminEntries.GROUPS_FILE`, append `"groups"` to `SECTIONS`, five `SettingEntry.of(GROUPS_FILE, "groups", key, BOOL/INT)` rows; lang: `supernaturalbloodlines.hub.admin.section.groups` ("Groups") and one `supernaturalbloodlines.hub.admin.key.supernaturalbloodlines-groups.toml:<key>` per key.
- [ ] **Step 4: Check the bound.** Assert (or raise) `AdminEntries.ALL.size() <= AdminSnapshotPayload.MAX_ROWS` for the **final** count of plans 20a-20c (about 216 + the pack-moon row + 19a's rows). If `MAX_ROWS` is lower, raise it in this task and note that the protocol number in Task 8 covers it.
- [ ] **Step 5: Run** the Step 2 command. Expected: PASS (the coverage test fails by name if a key has no row).
- [ ] **Step 6: Commit** `feat(groups): the groups config and its Admin rows`.

---

### Task 8: The group view payload

**Files:**
- Create: `core/group/GroupWire.java`, `parts/species/group/GroupViews.java`, `parts/supernatural/net/GroupViewPayload.java`, `parts/supernatural/client/GroupClient.java`, test `parts/supernatural/net/GroupViewWireTest.java`.
- Modify: `parts/supernatural/net/LadderNet.java` (register, send, `PROTOCOL`), test `SnapshotIdsWireTest.java`.

**Interfaces:**
- Produces: `GroupWire.MAX_MEMBERS = 64`, `MAX_NAME = 32`, `MAX_RANK = 16`.
- Produces: `GroupViewPayload(GroupType type, boolean hasGroup, byte viewerRank, String leaderName, int sinceDays, List<Row> members)` with `record Row(String name, String rank, GroupRank groupRank, boolean online, Optional<UUID> id)`; `STREAM_CODEC` written out (two lists and strings bounded by `GroupWire`); `static GroupViewPayload of(ServerPlayer)` (calls `GroupViews`, which reads `PackIndex`, `GroupIndex`, `HubSocial.of(p).pack()` for the existing pack rows so ranks stay the ones the hub already draws).
- Produces: `GroupClient.receive(GroupViewPayload, IPayloadContext)` stores a static `view()`; `GroupClient.clear()` on disconnect (the client's `ClientPlayerNetworkEvent.LoggingOut`, as the other client caches do).
- `LadderNet`: `registrar.playToClient(GroupViewPayload.TYPE, GroupViewPayload.STREAM_CODEC, FMLEnvironment.dist.isClient() ? GroupClient::receive : (payload, ctx) -> { })`; sent next to `sendRequests(p)` in the snapshot handler and from `HubRequests.changed`'s hook, guarded by `p.connection.hasChannel(GroupViewPayload.TYPE)`.

- [ ] **Step 1: Write the failing tests** (`GroupViewWireTest`): `roundTrip` (a coven view with 3 rows), `anEmptyViewRoundTrips` (`hasGroup = false`), `maxMembersRoundTrips` (64 rows), `aCountOverTheCapIsRejected` (hand-built buffer with 65 as the count: the decoder throws `DecoderException`, which the connection turns into a clean disconnect of that one packet, not a crash), `aLongNameIsRejected`, `onlyTheViewersOwnGroupIsCarried` (structural: `of(p)` for a player in group A lists only A's names; a leader of group B never appears), `theViewNeverCarriesAMeterOrAVote` (reflection over the record components: no component named `meter`, `consent`, `vote`; 20b adds its own fields in a separate record that the same test is extended to cover). `SnapshotIdsWireTest` expects the new `PROTOCOL`.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupViewWireTest' --tests '*SnapshotIdsWireTest'`. Expected: compile error.
- [ ] **Step 3: Implement** all; set `PROTOCOL` to main's value plus one and say so in the CHANGELOG ("the hub channel changed: update client and server together").
- [ ] **Step 4: Run** the Step 2 command. Expected: PASS. **Mutant** M11.
- [ ] **Step 5: Commit** `feat(groups): the group view payload and its client cache`.

---

### Task 9: The hub (UI-first), the three themes, the Profile

**Files:**
- Create: `client/hub/page/GroupLabels.java`, `GroupButtons.java`, test `GroupUiCoverageTest.java`.
- Modify: `PackPage.java`, `BookPackPage.java`, `ProfilePage.java` (+ the profile `Fact` for the group label), the theme classes only if a tab-label hook is needed, `en_us.json`.

**Interfaces:**
- Produces: `GroupLabels.noun(GroupView|GroupType)` (lang keys `supernaturalbloodlines.hub.group.noun.pack|clan|coven|brotherhood|none`), `GroupLabels.tab(GroupViewPayload)` used by the hub's tab strip for `HubPage.PACK` (the enum constant and its saved layouts do not change), `GroupLabels.rank(GroupType, GroupRank, String wolfRank)`.
- Produces: `GroupButtons.IDS`: `Map<String, String>` from a `GroupCommand` literal path (`invite`, `accept`, `leave`, `kick`, `ally invite`, `ally accept`, `ally remove`, `info`, `challenge`) to the button id on the page (`group:invite`, `group:accept`, `group:leave`, `group:kick`, `group:ally_invite`, ...). The 20b and 20c plans append their literals.
- The pages read `GroupClient.view()`: title and noun from the type, member rows from `members` (rank text from `GroupLabels.rank`), the actions column: Accept (Requests card and page), Leave (two presses), Invite (`PackInvite` picker, shown to a leader or a player who could found), Kick (member detail), Ally (picker, End with confirm). The wolf-only sections (wolves, moon gift line, Roar, Challenge/duel, heal, anchor, emissary, protect) are drawn only when `type == PACK`. `Fact.PACK` is shown as "Group" with the type noun; its wire name does not change.
- The ring (`PackRing`) keeps its roles; for CLAN and COVEN 20a draws it with the leader and members roles tinted by `GroupLabels` (placeholder art, noted in the PR; the two new sprites per theme are the art task of 20b/20c).

- [ ] **Step 1: Write the failing test** `GroupUiCoverageTest`: build the `GroupCommand` tree in a plain dispatcher (the way `AdminCommands` tests do), collect every literal path under `group`, and assert each is a key of `GroupButtons.IDS` or in the explicit "no button needed" set (`info`, shown by the page itself); also assert no id in `IDS` is used twice and every id has a lang key. `PackUiTest`, `PackPanelTest`, `PackRingTest` run **unchanged** and must pass.
- [ ] **Step 2: Run** `./gradlew test --tests '*GroupUiCoverageTest' --tests '*PackUiTest' --tests '*PackPanelTest' --tests '*PackRingTest'`. Expected: compile error / FAIL.
- [ ] **Step 3: Implement** the labels, the buttons and the page changes. Pack rows, ranks, spark marks and the wolves block must render exactly as before for a pack.
- [ ] **Step 4: Hidden hub shots** (`docs/safe-screenshots.md`): the Pack page of Task 0 again in the three themes (compare: pixel-identical for a pack apart from the tab label), then the Clan, Coven and Brotherhood versions of the page with 3 members in each theme, an empty page for a player with no group (shows the founding hint of their kind), the Profile of a clanmate and of a covenmate. Save outside the repo; attach to the PR.
- [ ] **Step 5: Run** the Step 2 command. Expected: PASS. **Mutant** M12.
- [ ] **Step 6: Commit** `feat(groups): the Pack page becomes the Group page, themed per type`.

---

### Task 10: Rules, docs, changelog, the matrix and the mutation pass

**Files:** Modify `docs/superpowers/specs/SPECIES-RULES.md`, `docs/parts/species.md`, `CHANGELOG.md`, `wiki/Werewolves-And-Packs.md`, `wiki/Rules-Reference.md`, `wiki/Configuration.md`; create `wiki/Groups.md`.

- [ ] **Step 1:** `SPECIES-RULES.md`: a new section "Groups (plan 20, owner 2026-11-23)" with R240 to R249 (spec section 4 verbatim in rule form), placed after "Werewolves (Teen Wolf layer)", and a one-line pointer from R94b ("groups generalise this: R240-R249"). If the pack-moon branch merged, R96 is untouched.
- [ ] **Step 2:** `docs/parts/species.md`: a "Groups" checklist (ticked items of 20a), the settings table, "what vanishes with the switch" (`groups_enabled = false`: no new clan or coven, existing rows kept).
- [ ] **Step 3:** `CHANGELOG.md`: the entry, with **"Rolling back"**: delete `supernaturalbloodlines_groups.dat` (packs are untouched: `supernaturalbloodlines_packs.dat` is never rewritten by this plan); the hub channel number changed.
- [ ] **Step 4:** `./gradlew test` (all unit tests). Expected: PASS.
- [ ] **Step 5: The matrix**, one run at a time with `--max-workers=2`, each `./gradlew runGameTestServer` plus: 1 none; 2 `-PnoWerewolves -PnoCurios` (**the run that matters most here**); 3 `-PnoLinkedMods`; 4 `-PpartsOff=species` (groups vanish with the part, nothing throws); 5 `-PpartsOff=artifacts`; 6 `-PpartsOff=speciesTensura`; 7 `-PpartsOff=supernatural` (the payload is not registered; the species part still runs); 8 `-PpartsOff=supernaturalTensura`; 9 `-PpartsOff=hud`; 10 `-PpartsOff=celestials`; 11 `-PpartsOff=bosses`; 12 `-PpartsOff=species,artifacts`. Record the counts; they equal the baseline plus the new tests.
- [ ] **Step 6: The mutation pass.** For each row: apply the change, run the named test, confirm it FAILS, revert. Record each in the PR.

| # | Mutant | Test that must fail |
|---|---|---|
| M1 | the vampire check before the werewolf check in `GroupTypes.of` | `hybridTypesLeadPacksEvenThoughTheyAreAlsoVampires`, `aWerewolfWhoIsAlsoAVampireLeadsAPack` |
| M2 | `seed` derives a type from facts | `seedNeverReadsAnOfflineLeader`, `seedMakesOnePackPerLeader` |
| M3 | `GroupIndex` writes into a `PackIndex` key | `packIndexFileIsUntouched` |
| M4 | `follow` drops the id | `idFollowsTheLeaderOnPromote`, `idFollowsTheLeaderOnTransfer` |
| M5 | the switch deletes the old type's rows | `switchBackRestoresDormantRows` |
| M6 | the switch calls `PackIndex.leave` for members who do not fit | `aSwitchLosesNobody`, `aWolfBetaSurvivesTheirLeadersSwitch` |
| M7 | `registerSession` left inside the Werewolves guard | `aCovenRecordsLastSeen` (run 2) |
| M8 | `canFound` ignores `openFounding` / the type | `openFoundingLetsAHumanLeadACoven`, `aHumanCannotFoundAGroup`, `canFoundFollowsTheType` |
| M9 | `heir` ignores `here` | `anAbsentHeirIsSkipped` |
| M10 | alliance refused between different types | `allyAcrossTypes`, `allianceSurvivesATypeSwitch` |
| M11 | the codec accepts a count over the cap | `aCountOverTheCapIsRejected` |
| M12 | a command literal with no button | `GroupUiCoverageTest` |
| M13 | the sweep notices every pass | `aSwitchIsToldOnce` |
| M14 | the `/pack` tree loses a shared literal | `slashPackStillWorks` |
| M15 | the hunter test placed after the coven fallback in `GroupTypes.of` | `aHunterLeadsABrotherhood`, `aWitchInTheHunterFactionLeadsABrotherhood` |
| M16 | the brotherhood heir ignores `online` (an offline stale mark inherits) | `anOfflineMarkNeverInherits` |

- [ ] **Step 7: Open the PR** with the matrix counts, the mutation table with results, the before/after shots and the list of what moved out of the Werewolves guard.

---

## Self-review

- Spec coverage: section 2 (data, migration) is Task 2; 3 (type, switch) Tasks 1 and 5; 4 (R240-R249) Tasks 4, 6 and 10; 9 (config) Task 7; 10 (hub) Task 9; 11 (protocol) Task 8. Sections 5, 6, 7 and 8 are the pack (unchanged), 20b, 20c and the rings, below.
- Types: `GroupTypes.of`'s `Leader` and `Fit` are used by name in Task 4; `GroupIndex.groupOf/typeOf/setType/rankOf/setRank/clearRanks/follow/drop/seed` are defined in Task 2 and consumed in Tasks 5 and 6; `GroupSuccession.Candidate` is defined and consumed in Task 6; `GroupButtons.IDS` is defined in Task 9 and extended by 20b and 20c.
- Proportion: bodies are left to the signatures and the tests; the decisions the builder cannot make alone are the numbers and the order of checks, and those are in the spec.
- Not done here, on purpose: any clan, coven or brotherhood perk, any new rite, any item, the `CIRCLE_CALL`, `GROUP_VOTE` and `CHARTER_CONTEST` request kinds, the two ring sprites per theme.

---

## Outline: 20b Covens (**superseded: the full plan is [2026-11-24-plan-20b-covens.md](2026-11-24-plan-20b-covens.md)**; this outline is kept as the record. The owner decided Q1, Q2 and Q4 on PR #83)

Gate: 19a merged (the two EP sites exist on `main`); 20a merged; spec questions Q1 (power payment: members 70%, altar 30%), Q2 (grand rites: three new ones, none of the existing), Q4 (rings: existing items through coven rites) decided on PR #83. Branch `feature/plan-20b-coven`. Rules R250 to R269. Mixin budget: **0 new** (the link rides the existing `RiteCostMixin`). Admin rows: +21.
1. **Re-read the two EP sites** of 19a (`EvolutionRiteFactory.grant`, the mastery apply) and replace the direct `EnergyHelper.setBaseMaxEP` spends with one `RiteEp.charge(rite, target, total)`; a source-scan test (`NoDirectEpSpendTest`) fails if any rite factory calls `setBaseMaxEP` outside `RiteEp`. Behaviour identical until a circle exists (the whole 19a GameTest set passes unchanged).
2. **Pure circle rules** (`core/group/circle`): `CirclePlan.plan(altarBill, epTotal, members, shareFraction)` with drop-and-recompute, consent levels, EP keep floor; `Potency.of(n)`; `Tiers.of(n)`; `Channel.value(stored, atTick, now, cap, regen)`. The worked table of the spec (section 6.2) is the test fixture, plus: shares add up exactly, the recompute converges in at most 13 passes, a member over the hard cap is dropped, consent `none` drops a nosebleed-zone member, potency is strictly increasing with strictly decreasing steps, tier thresholds are config.
3. **Circle sessions** (in memory, `parts/species/group/circle/CircleLink`): `/coven link open|close|join|leave|consent`, the `CIRCLE_CALL` request kind (appended to `HubRequests.Kind`, protocol bump), join by token, single circle per player, 13 max, closes on idle, anchor logout, type switch; the circle card in the view payload (a **separate** record, the viewer's own meter, consent and share preview only).
4. **The cost hook**: extend `RiteCost.begin/commit/discard` with the circle plan in the same `Pending` object as the siphoned power; a GameTest counts `begin(start = true)` calls per rite start (**verifies spec 13 item 2**), then: members are charged on `commit` only, `discard` and an altar that cannot pay charge nobody, a solo cast is byte-for-byte today's bill, a 20 000-power rite at N = 3 with consent `none` drops members and recomputes, EP is split through `RiteEp.charge` (a Mastery Unique rite at N = 5).
5. **Channel meter and strain** (`GroupIndex.channel`, effects): nosebleed and damage zones exactly as the spec table; never below 1 HP outside the lethal option; regen is lazy.
6. **The window and the death rule**: window length, anchor death (`LivingDeathEvent`), members' damage/weakness/meter reset/shaken, `link_death_lethal`; logout is not a penalty; a member's death only removes that member. **Prove** `RiteManager.removeRite` / `Rite.stop()` aborts a running Enchanted rite (**spec 13 item 1**) or delete that sentence of R256 and say so in the PR.
7. **Governance**: Elders, votes (saved), shun, Regent election, bans (`GroupIndex.bans`), `GROUP_VOTE` request, `/coven elder|shun|regent|vote`, absence heir by Elder already in 20a; pure `VoteTally` tests (strict majority, non-voters count no, electorate under 3, expiry, subject left).
8. **`GrandRiteFactory`** with `min_circle` and the three grand rites of Q2; `CovenPotency.scale` wired into the Linking Spell's `link_seconds` and the ward durations; the Moonlight Ring rite (Q4).
9. **Hub**: the Circle card (open/join/leave, consent selector, share preview with the worst case), the votes card, Elder marks, tier badge, the coven ring art role set; `GroupButtons.IDS` grows, `GroupUiCoverageTest` still passes; hidden shots in the three themes.
10. Admin rows (21), lang, `SPECIES-RULES.md` R250 to R269, CHANGELOG with "Rolling back" (config switch `link_enabled`; shares already paid are gone, they never persist a debt), matrix 1 to 12, mutants (member share not split, altar paid in full when linked, charge before commit, death penalty skipped, consent ignored, a dropped member still charged, potency not diminishing, tier off by one, vote majority off by one).

## Outline: 20c Clans (the full plan is [2026-11-24-plan-20c-clans.md](2026-11-24-plan-20c-clans.md); the outline below is kept as written, the plan corrects it)

Gate: 20a merged (20b is needed only for the coven-forged Sun Ring in step 8); Q5 (the Charter contest) decided on PR #83. Branch `feature/plan-20c-clan`. Rules R270 to R289. Mixin budget: **0**. Admin rows: +16.
1. **Read before building:** the sireline index API (`parts/species/sireline`, `parts/species/line`) for "same sireline"; `Detectors.onDrinkBlood` and whether a `none()`-context shared feed counts for tasks (**spec 13 items 4, 5**); the NeoForge 21.1.x `ItemEntityPickupEvent.Pre` signature (**item 3**); `VampirismAPI.vampirePlayer(Player)` for a non-vampire (returns null or an empty optional?).
2. **Pure rules** (`core/group/clan`): `CharterState` machine (`HELD`, `STORED`, `DROPPED`, `LOST`), `Contest.resolve(holder, leader, ranks, here)`, `FeedShare.of(amount, percent, max)` with the per-mate window, `SirelineBonus.of(count, percent, max)`, `Promotion.due(joinedAt, now, days)`. Tests: every transition, duplicates, reforge, resolution table (stored, dropped, Night Walker, offline holder, holder is the leader), caps.
3. **Ranks and discipline**: `/clan promote|demote|exile`, the `daywalker_days` auto-promotion in the 200-tick sweep, `GroupIndex.bans` for exile, heir by Day Walker already in 20a.
4. **The Charter item and its protection**: `clan_charter` item (stack 1, component `charter_id`/`group_id`), forge and reforge commands and buttons, the 40-tick scan, the pickup denial (or the sweep fallback), the invulnerable no-despawn entity, duplicates to dust, void and removal to `LOST`, non-member strip; GameTests per protection (a non-member cannot pick it up, a duplicate becomes dust, lava does not destroy it, a void fall marks it lost and Reforge works, a stack in a chest counts for nobody, an offline holder wins nothing).
5. **Game of Kings**: `/clan challenge`, the `CHARTER_CONTEST` request (appended kind), accept or auto-accept, the midnight resolution (`dayTime >= 18000` crossing, plus `contest_max_hours` fallback), `PackIndex.promote` and `PartyLink.leadershipChanged`; an end-to-end two-player GameTest, one with the leader offline (not allowed), one with a type switch mid-contest (void, no winner).
6. **Shared feeding**: the `BloodDrinkEvent.PlayerDrinkBloodEvent` listener at `LOWEST`, the re-entrancy `ThreadLocal`, the caps, the "source is a clanmate" exclusion; a GameTest with a real `drinkBlood` (the `BloodSkillGameTests.from(source)` helper is the model) proving the mate gets `share`, the feeder is unchanged, a bottle never shares, a second event is not produced by the share, and the caps hold.
7. **Sire-line bonus**: a live attack-damage modifier like `PackBonus` (refresh every 20 ticks, removed when alone), capped; tests for count, cap, dimension, distance.
8. **The coven-forged Sun Ring** (Q4; *plan 20b now builds the rite itself as `coven_sun_ring` behind `sun_ring_day_walker_only`, default false; 20c only gives the rank a meaning and flips that default after the owner agrees*): the rite (needs 20b's circle, tier 1), the `coven_forged` component, the `ward_of_daylight` power discount in `RiteCost.begin` only for a Day Walker of a clan; the plain recipe and the plain bind unchanged (test).
9. **Hub**: the Charter card (state, holder, contest line, Forge/Reforge/Challenge/Accept), rank marks and Promote/Demote/Exile on the member detail and on the Profile, the shared-feed line, the court-table ring role set; `GroupButtons.IDS` grows; hidden shots.
10. Admin rows (16), lang, `SPECIES-RULES.md` R270 to R289, CHANGELOG with "Rolling back" (`charter_enabled` off makes every Charter inert; no item is deleted), matrix 1 to 12, mutants (a contest resolves for a Night Walker, an offline holder wins, a chest counts as held, a duplicate survives, a bottle shares blood, the share loops, a clanmate victim shares, the cap ignored, exile without a ban, a Charter works while the group is a coven).

## Outline: 20d Brotherhood (a plan of its own is written when 20a is merged; the owner decided the four 20d questions of spec 16.2 on PR #83)

Gate: 20a merged (20b and 20c are not needed: nothing here uses a circle, a Charter or a sireline); spec 16.2 Q7-Q10 decided on PR #83 (mark leads at once; the Code punishes only; 48-hour curse end; bonus and sight, no shared kills). Branch `feature/plan-20d-brotherhood`. Rules R290 to R299 (spec 8b). Mixin budget: **0**. Admin rows: +16. The Brotherhood needs no Werewolves (`HunterEvents.register()` runs without it).
1. **Read before building** (spec 13 item 9): `Supernatural.isPrey` visibility from the group package; that a client ignores a glow id for an entity it has not loaded; where `BossDirector` picks Kol's target (for the warning); that a new `BloodlineEvent.CODE_BROKEN` appended to the enum keeps the saved codec and `BloodlineRules` tables intact (the event enum's own test); `SparkHandler.isInnocent` is package-private in `parts/species/pack`: expose a public `Innocence.of(killer, victim)` that both the werewolf rule and the Code call, with the werewolf tests unchanged.
2. **Pure rules** (`core/group/brotherhood`): `LeadByMark.next(leader, members)` (strictly higher, online, not a Code-breaker, ties keep, a reset mark passes the lead on), `ArmsBonus.of(count, percent, max)`, `CodeRules` (`breaks(victimKind, fighting, prey)`, `penalty(kills, percent)` with the minimum of 1, strike window, one break per victim per minute), `CurseExtend.endAt(now, hours, owesPotential)`, `MapRegions.revealed(kills)` and `complete(bestKills)`. Every row of spec 8b is a test; the tables are fixtures.
3. **The type's runtime**: `GroupIndex.marks`, `curses`, `code_breakers` (spec 2.1) with their load/save and dormant-row tests; `Brotherhood.invite` gate (`prey_member` refusal), the 200-tick sweep that releases a member who became prey (`released_prey`), a mark listener that records `marks` and calls `LeadByMark`; the takeover through `PackIndex.promote` + `PartyLink.leadershipChanged`, GameTests: the founder leads, a marked member passes them, the old leader stays, a tie keeps, an offline member cannot take it, a mark reset (R82) passes it on.
4. **Brothers in arms and shared sight**: a transient attack modifier refreshed every 20 ticks (never saved, removed when alone), the glow share through `GlowPayload.send` for brothers without the sense, cloak respected; GameTests with 3 brothers at 20 and 30 blocks, across dimensions, with a Code-breaker, and with a witch member (gets nothing, gives nothing).
5. **The Code**: `Innocence` + `CODE_BROKEN` through the bloodline rules (mark loss, `MarkTracker.sync` follows), the breaker timer, strikes, expulsion with the ban through `GroupIndex.bans`, notices; GameTests: a villager kill and an uninvolved player kill each cost the mark, a prey kill and a fight never do, the third strike expels, the breaker cannot take the lead.
6. **The Hunter's Curse for any brother**: a hook next to `MarkTracker.killedMarkedHunter` for a non-marked brother's death (`HunterCurse.curse(killer)` exists), the 48-hour end in `GroupIndex.curses`, "already cursed changes nothing", a Potential's first kill still ends everything; GameTests for each, including a human killer (no curse, R83) and a brother killed by the world.
7. **Kol's warning** (one line to the other brothers) and the **Map and Code cards** in the hub (the tattoo drawn by regions from the viewer's own kills and a completion flag, the saying, strikes, the breaker timer, the bonus line), the Brotherhood ring art role set (placeholder tint until the art task), `GroupViewPayload` brotherhood block (own kills, leader's kills, completion flag, strikes, bonus) with its wire tests (never another brother's kills), `GroupButtons` unchanged (no command of its own) and `GroupUiCoverageTest` still passing, hidden shots in the three themes.
8. Admin rows (16), lang (`supernaturalbloodlines.group.*.brotherhood`, the Code lines, the map regions), `SPECIES-RULES.md` R290 to R299 with a pointer from R80-R84, CHANGELOG with "Rolling back" (`brotherhood_enabled` off: groups of that type behave as the base with no perks and no mark-led lead; no row is deleted; the new bloodline event is unused), matrix 1 to 12, mutants (the lead moves on a tie, an offline member takes the lead, a Code-breaker gets the bonus, a prey kill breaks the Code, the penalty is 0 when the mark is 1, the curse stacks, a non-marked brother's curse never ends, shared sight shows a cloaked or witch target, a prey member is allowed in).

> **Plan 20b (covens) is built** (`2026-11-24-plan-20b-covens.md`): the coven rows of `GroupButtons`, `CovenCommand` and the circle are drawn and wired there; the spec corrections are at the end of `specs/2026-11-23-groups-design.md`.