# Original Bloodlines, Line Death, the Two Line-Breaking Rites and the Bloodline Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every vampire belongs to the line of one root Original; killing an Original with white oak kills the whole line; two witch rites free someone from a line; the hub's Bloodline page shows all of it in the three themes.

**Architecture:** A server-wide `LineIndex` (SavedData) stores `member -> root Original` for players and NPC mobs, so line death, the rites and offline players need no loaded data. Roots are plain UUIDs: a player Original's own UUID, or one of six fixed UUIDs for the NPC Originals (`NpcOriginal`, whose entities arrive with plan 17). Pure rules live in `core/bloodline/LineRules` (R120 to R127), the species part wires them to the game, and `HubSocial.Origin` carries the line to the hub, where one pure `LineTree` model feeds three theme renderers. The old tree-based `sireline_death` is replaced by `original_line_death`.

**Tech Stack:** Java 21, NeoForge 1.21.1, Vampirism, Enchanted 4.2.7 (rites), Werewolves, Tensura 2.0.1.5 (Tensura theme only), JUnit 5, GameTests. No mixins.

**Spec:** `docs/superpowers/specs/2026-11-16-bloodlines-bosses-design.md` sections 2 to 5 (copy of `C:\Users\caiom\.opencode\plan\2026-11-16-bloodlines-bosses-design.md`). Section 1 is `2026-11-16-severance-name.md`; the NPC werewolf packs and the bosses are other plans. **Visual reference for Tasks 12 to 14:** the Figma file `USFBf74c4I2BWDyfmRxFZO`, page "Bloodline v3" (being drawn now; read each theme's frame with `tools.figma.get_screenshot` / `get_design_context` after `create_session`, and compare the dev shots to it).

## Global Constraints

- Parts never import each other except the documented `supernatural_tensura` exception (it may import `supernatural`'s page kit; `species_tensura` may import `species.sireline`). The species part therefore finds the mountain ash block by registry id `supernaturalbloodlines:mountain_ash_line`, never by `MountainAsh`, and the supernatural part gets the line only through `shared/supernatural/HubSocial`.
- Every feature can be switched off per part with config switches; old saves load leniently and **every new codec field is optional** (`lenientOptionalFieldOf`).
- White oak is the only thing that kills an Original (R32); any other lethal damage puts them in the dying or desiccated state. True Alpha sparks and stacked sparks are not touched by this plan.
- Line death applies to NPCs and players, including hybrids ("Tyler would die with Klaus"), offline players at their next login with a chat message naming the Original; config `original_line_death`, default on. It replaces `sireline_death` (default off), which is removed.
- A player made Original by the rite is always the root of their own line, even if an Original sired them before. NPC Originals do not count toward `max_originals_per_world`. Once an Original is dead, **new NPC vampires never join their line**; existing members keep it. An Original boss who has not appeared yet still has a line.
- Every Vampirism vampire mob gets a line except bosses. A player with no traceable Original belongs to no line. A player turned by an NPC, or by a player in a line, inherits that line.
- Discovery is per player, never shared with their sired. `original_visibility` = `discovered` (default) | `always`, same style as `gene_visibility`. The Rite of Revelation also reveals the root Original.
- Warning when an Original of your line is desiccated: every online member gets a chat warning; undiscovered says "**the Original of your bloodline** lies desiccated", with no name. The login message names the Original only once discovered, else says it is unknown. The line-death message names the Original afterwards.
- **Rite of the Broken Line:** performer witch-type (witch, siphoner, heretic or tribrid); lunar eclipse; ingredients blood of **that** Original, white oak ash, and the target's own blood (a Sire's Blood-style bound bottle); the target leaves that Original's line and becomes the head of their own line, **not** an Original; their direct sire bond stays; everyone the target sired or turned stays in the Original's line. The rite fails with the wrong blood.
- **Rite of the Imprisoned Original (harder, much costlier):** the Original must be trapped inside a closed mountain ash circle at the rite's centre for the whole rite; a broken circle fails it; it frees the target **and their whole branch**, never the Original's whole line. Both rites also work against a **player** Original.
- Bloodline page, all three themes: the line above the player up to the crowned Original root (a "?" node until discovered; NPC ancestors also "?"; player ancestors always shown); a broken link above a free player with tooltip "Line broken from Klaus (Rite of the Broken Line)" and an open padlock on the portrait; descendants below with links running faintly past the player up to the Original; the selected node's details (name, species, sire, line status, actions Call / Severance / Broken Line); the gene vials kept; states You are an Original (root), Not a vampire, Sire unknown, In a line, Free; arrow keys move along the tree; narration names the break ("Tyler, your sire. Link above you broken: you are free of Klaus's line.").
- Grimoire: ink tree on the left page, break as torn or burned ink with a red margin note, wax padlock seal on the player, descendants' lines as faint dotted ink, details on the right page. Blood Moon: the Original as a portrait inside the moon, the line as a chain of portraits with broken glowing chain links at the break, a thin red thread from the descendants behind the player to the moon, details in the bottom bar. Tensura: hex nodes with teal connectors, a red dashed connector labelled "SEVERED", details in the right info pane.
- Patterns: per-part config in `SpeciesConfig`; `BloodlineRules`-style pure tables with R-numbers in `docs/superpowers/specs/SPECIES-RULES.md`; GameTests registered by the part in `SpeciesPart`; the 10-run matrix `C:\Users\caiom\AppData\Local\Temp\opencode\matrix13d.ps1` (`--no-daemon`); update `docs/parts/species.md`, `docs/balance.md`, `docs/hub-ui-kit.md`, the wiki and `CHANGELOG.md`. Rite power numbers and the 30 s duration are builder choices recorded in `docs/balance.md`.
- Shorthand used below: `GM` = `pwsh -NoProfile -File C:\Users\caiom\AppData\Local\Temp\opencode\gm.ps1 -TimeoutSec 900` followed by Gradle arguments. It runs the checkout hard-coded in the script; run it where the branch lives (or edit the path in a copy). A unit test run passes with `BUILD SUCCESSFUL`; a GameTest run passes with `All <n> required tests passed` and no `failed at` line.

## Config keys (all in `SpeciesConfig`, file `supernaturalbloodlines-species.toml`, COMMON)

| Key | Default | Meaning |
|---|---|---|
| `original_line_death` | `true` | white oak on an Original kills their line (replaces `sireline_death`, removed) |
| `original_visibility` | `discovered` | `discovered` or `always`: whether a player sees their root Original without the Rite of Revelation |
| `line_rites_enabled` | `true` | switches both rites off (they refuse with `line_rites_off`) |
| `npc_line_weights` | `["mikael=1","finn=1","elijah=1","kol=1","rebekah=1","klaus=1"]` | spawn weight of each NPC root for new NPC vampires; a bad entry is skipped, a weight below 0 counts as 0 |
| `imprisonment_ring_max_radius` | `8` (range 2 to 16) | the mountain ash ring must close within this many blocks of the rite's centre |
| `imprisonment_rite_seconds` | `30` (range 5 to 300) | how long the Original must stay trapped |

Test overrides follow `overrideSirelineDeath`: `overrideOriginalLineDeath(Boolean)`, `overrideOriginalVisibility(Boolean always)`, `overrideLineRites(Boolean)`, `overrideNpcLineWeights(List<String>)`, `overrideRingRadius(Integer)`, `overrideImprisonmentSeconds(Integer)`.

## Rule rows (new, `SPECIES-RULES.md`, section "Original bloodlines"; pure code in `LineRules`)

- **R120** Every vampire line has a root Original (a player Original, or one of Mikael, Finn, Elijah, Kol, Rebekah, Klaus). An Original is the root of their own line, including a player made Original by the rite (their branch moves with them).
- **R121** A child sired by a member inherits the member's root; a child sired by an Original gets that Original; a sire with no line gives no line.
- **R122** White oak on an Original kills every member of their line (players, NPCs, hybrids), never the root, never anyone freed. Offline players pay at login.
- **R123** A player sees their root only when it equals what they discovered, or under `original_visibility = always`.
- **R124** The line is warned once each time an Original becomes open to white oak (desiccated, dying, downed).
- **R125** Broken Line: refusals in order `not_witch`, `no_eclipse`, `no_blood` (fewer than two bloods), `wrong_blood` (no pair where one blood is the root of the other), `target_away`, `target_original`. Effect: the target becomes their own head, freed from that root.
- **R126** Imprisoned Original: refusals in order `not_witch`, `no_eclipse`, `no_blood`, `target_away`, `target_original`, `nobody_trapped`, `wrong_original`, `ring_open`. Effect: the target and everyone below them leave that root and follow the target.
- **R127** Someone who stops being supernatural leaves their line; someone with no recorded sire and no line stays out of lines.

## Review Focus

- A world from before this plan has sired players and creatures but an empty `LineIndex`: lines must be rebuilt from the recorded sires at login and at mob join, or the first white oak kill kills nobody. Pinned in Task 4 (`existingSireChainIsBackfilledAtLogin`).
- A hidden root must never reach a client or a chat line before discovery: the snapshot bytes, the warning, the login message. Pinned in Tasks 6, 7 and 10 (`snapshotNeverLeaksAHiddenRoot`, `warningHidesTheNameUntilDiscovered`, `loginMessageSaysUnknownUntilDiscovered`).
- Offline members: a death owed to an offline player must be dropped if they left the line before logging in (cured, freed), and a freed player must never be killed by their old Original. Pinned in Task 5 (`owedDeathIsDroppedWhenNoLongerAMember`, `freedMemberSurvives`).
- The anti-goals: the Imprisoned rite frees one branch only (Task 9 `imprisonedNeverFreesTheWholeLine`), an Original cannot break their own line and a freed head cannot be freed again from the same Original (Task 8 refusals), and a line warning must be silent when `original_line_death` is off (Task 7 `noWarningWhenLineDeathIsOff`).
- Rite edge cases: the target offline (no offline edits, refused `target_away`), the ring broken or the Original stepping out mid-rite (the rite fails and the ingredients are spent), the eclipse ending mid-rite, and an artifacts-off world where the ring block does not exist (always `ring_open`). Pinned in Tasks 8 and 9.

---

### Task 1: Pure rules, NPC roots and the ring fill

**Files:**
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/core/bloodline/NpcOriginal.java`
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/core/bloodline/LineRules.java`
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/core/bloodline/RingFill.java`
- Test: `src/test/java/net/ofatech/supernaturalbloodlines/core/bloodline/LineRulesTest.java`, `RingFillTest.java`

**Interfaces:**
- Produces:
  - `enum NpcOriginal { MIKAEL, FINN, ELIJAH, KOL, REBEKAH, KLAUS }` with `String id()` (lower case), `UUID uuid()` (`UUID.nameUUIDFromBytes(("supernaturalbloodlines:npc_original:" + id()).getBytes(UTF_8))`), `String translationKey()` (`"supernaturalbloodlines.original." + id()`), `static Optional<NpcOriginal> byUuid(UUID)`, `static Optional<NpcOriginal> byId(String)`.
  - `LineRules` (all static, no game classes): `enum State { ORIGINAL, NOT_VAMPIRE, SIRE_UNKNOWN, IN_LINE, FREE }`; `State state(boolean original, boolean vampire, boolean free, boolean hasRoot)`; `Optional<UUID> inheritedRoot(UUID sire, boolean sireIsOriginal, Optional<UUID> sireRoot)`; `boolean showsRoot(@Nullable UUID known, UUID root, boolean always)`; `Map<NpcOriginal,Integer> parseWeights(List<String>)`; `Optional<NpcOriginal> pickRoot(Map<NpcOriginal,Integer> weights, Set<NpcOriginal> dead, double roll)` (`roll` in [0,1)); `record BloodPair(UUID original, UUID target)`; `Optional<BloodPair> pairBloods(List<UUID> sources, Function<UUID,Optional<UUID>> rootOf)`; `record BrokenLineFacts(boolean witchType, boolean eclipse, int bloodCount, Optional<BloodPair> pair, boolean targetOnline, boolean targetIsOriginal)`; `Optional<String> brokenLineRefusal(BrokenLineFacts)`; `record ImprisonedFacts(boolean witchType, boolean eclipse, Optional<UUID> targetBlood, boolean targetOnline, boolean targetIsOriginal, Optional<UUID> targetRoot, Optional<UUID> trappedRoot, boolean ringClosed)`; `Optional<String> imprisonedRefusal(ImprisonedFacts)`; `String REFUSAL_PREFIX = "supernaturalbloodlines.refusal.line_"`. The refusal methods return the **full lang key** (`REFUSAL_PREFIX + "no_eclipse"` and so on).
  - `RingFill.enclosed(BiPredicate<Integer,Integer> wall, int cx, int cz, int maxRadius): boolean`: 4-neighbour flood fill from the centre cell; false when the centre is a wall or the fill reaches a cell with `|dx| > maxRadius` or `|dz| > maxRadius`.

- [ ] **Step 1: Write the failing tests**

`LineRulesTest`: `npcRootsAreStableAndDistinct` (six distinct UUIDs, `byUuid(KLAUS.uuid())` is `KLAUS`, `byUuid(UUID.randomUUID())` empty, `KLAUS.uuid()` equals the value computed from `"supernaturalbloodlines:npc_original:klaus"`); `stateTable` (`state(true,*,*,*)==ORIGINAL`, `state(false,false,*,*)==NOT_VAMPIRE`, `state(false,true,true,*)==FREE`, `state(false,true,false,true)==IN_LINE`, `state(false,true,false,false)==SIRE_UNKNOWN`); `childOfAnOriginalGetsTheOriginal` (`inheritedRoot(O, true, Optional.empty()) == Optional.of(O)`); `childOfAMemberGetsTheMembersRoot` (`inheritedRoot(M, false, Optional.of(O)) == Optional.of(O)`); `childOfALinelessSireGetsNothing` (`inheritedRoot(M, false, Optional.empty())` is empty); `showsRootRules` (`showsRoot(null, r, false)` false; `showsRoot(r, r, false)` true; `showsRoot(other, r, false)` false; `showsRoot(null, r, true)` true); `weightsParseLeniently` (`parseWeights(List.of("klaus=3","bad","kol=-2","nobody=1","finn=x"))` is `{KLAUS=3, KOL=0}`); `pickRootIsWeightedAndSkipsTheDead` (weights `{MIKAEL=1,KLAUS=3}`: `roll 0.0 -> MIKAEL`, `0.26 -> KLAUS`, `0.99 -> KLAUS`; with `dead={KLAUS}` every roll gives `MIKAEL`; with all weights 0 or all dead the result is empty); `pairBloodsFindsTheOriginalAndTheTarget` (sources `[T, O]`, `rootOf(T)=O`, `rootOf(O)=O` -> `BloodPair(O, T)`; sources `[O, O]` empty; sources `[T, X]` with `rootOf(X)=P` and `rootOf(T)=O` empty; one source empty); `brokenLineRefusalOrder` (each fact set that fails exactly one check returns that key; with two failing the earlier one in the order `not_witch, no_eclipse, no_blood, wrong_blood, target_away, target_original` wins; all good returns empty); `imprisonedRefusalOrder` (same for `not_witch, no_eclipse, no_blood, target_away, target_original, nobody_trapped, wrong_original, ring_open`; `wrong_original` when `trappedRoot` is present and differs from `targetRoot`).

`RingFillTest`: `squareRingEncloses` (wall = border of the square radius 3 around (0,0): `enclosed(wall,0,0,8)` true); `gapOpensIt` (remove one border cell: false); `diamondOfDiagonalBlocksEncloses` (walls at `|x|+|z|==3`: true); `ringLargerThanTheLimitIsOpen` (square radius 5, `maxRadius` 4: false); `centreOnAWallIsNotEnclosed`; `noWallsIsOpen`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `GM test --tests "net.ofatech.supernaturalbloodlines.core.bloodline.LineRulesTest" --tests "net.ofatech.supernaturalbloodlines.core.bloodline.RingFillTest"`
Expected: FAIL (compile error: classes not found).

- [ ] **Step 3: Implement the three classes to the signatures above**

`pickRoot`: sum the weights of NPC roots that are not dead and have weight > 0; `target = roll * sum`; walk the enum in declaration order. `pairBloods`: first ordered pair `(a, b)`, `a != b`, with `rootOf(b)` present and equal to `a`. `RingFill`: `ArrayDeque` BFS, `HashSet<Long>` of visited cells.

- [ ] **Step 4: Run the tests to verify they pass**

Run: same command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/ofatech/supernaturalbloodlines/core/bloodline src/test/java/net/ofatech/supernaturalbloodlines/core/bloodline
git commit -m "feat(species): pure line rules R120-R127, NPC Original roots, ring fill"
```

---

### Task 2: `knownLine` on the bloodline state

**Files:**
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/core/bloodline/BloodlineState.java` (record component, `NONE`, `Extra` codec, `CODEC` xmap, `Builder`)
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/core/BloodlineAccess.java` (the client view, line ~96, next to `vampireBloodUntil(0)`)
- Test: `src/test/java/net/ofatech/supernaturalbloodlines/core/bloodline/BloodlineStateTest.java`, `GeneViewTest.java`

**Interfaces:**
- Produces: `BloodlineState#knownLine(): @Nullable UUID` (the root Original this player has discovered; server only), `BloodlineState.Builder#knownLine(@Nullable UUID)`, codec key `known_line` (UUID, `lenientOptionalFieldOf`, absent = null). It is the 9th field of `Extra` (the group cap is 16).

- [ ] **Step 1: Write the failing tests**

`BloodlineStateTest.knownLineRoundTripsAndOldSavesLoad`: encode a state with `knownLine(U)` and decode: equal; decode a map without `known_line`: `knownLine()` is null. `GeneViewTest.knownLineNeverReachesAClient`: `BloodlineAccess.clientView(s, true).knownLine()` and `clientView(s, false).knownLine()` are both null.

- [ ] **Step 2: Run to verify failure**

Run: `GM test --tests "net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineStateTest" --tests "net.ofatech.supernaturalbloodlines.core.bloodline.GeneViewTest"` Expected: FAIL (compile).

- [ ] **Step 3: Add the field**

Append `@Nullable UUID knownLine` as the last record component; every `new BloodlineState(` in the file (the `NONE` constant, `BASE_CODEC` apply, `build()`) passes `null` or the builder value; fix every other compile error the same way (tests that construct the record directly). `BloodlineAccess.clientView` strips it with `.knownLine(null)` exactly where it strips `vampireBloodUntil`.

- [ ] **Step 4: Run to verify they pass**

Run: the same command, then `GM test` (whole unit suite). Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main src/test
git commit -m "feat(species): BloodlineState.knownLine, the root Original a player has discovered"
```

---

### Task 3: `LineIndex` and the mob attachment

**Files:**
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/parts/species/line/LineIndex.java`
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/parts/species/SpeciesRegistries.java` (two attachments, next to `SIRE`)
- Test: `src/test/java/net/ofatech/supernaturalbloodlines/parts/species/line/LineIndexTest.java` (mirror `SirelineIndexTest`)

**Interfaces:**
- Produces, on `LineIndex extends SavedData` (name `supernaturalbloodlines_lines`, stored on the overworld's data storage like `SirelineIndex`):
  - `static LineIndex get(MinecraftServer server)`, `static LineIndex load(CompoundTag, HolderLookup.Provider)`, `CompoundTag save(CompoundTag, HolderLookup.Provider)`.
  - `record Freed(UUID from, Kind kind)` with `enum Kind { BROKEN_LINE, IMPRISONED }`; `record Member(UUID id, boolean player)`.
  - `Optional<UUID> rootOf(UUID member)` (an Original's own entry is `rootOf(o) == o`); `Optional<Freed> freedOf(UUID member)`; `void setRoot(UUID member, UUID root, boolean player)` (clears any `Freed`); `void free(UUID member, UUID from, Freed.Kind kind)` (the member becomes their own root and keeps their `player` flag); `void remove(UUID member)`; `List<Member> membersOf(UUID root)` (insertion order, **never contains the root itself**); `void markDead(UUID root)`; `boolean isDead(UUID root)`; `Set<NpcOriginal> deadNpcRoots()`; `void addOwed(UUID member, UUID root)`; `Optional<UUID> takeOwed(UUID member)`.
- `SpeciesRegistries.LINE`: `AttachmentType<UUID>` serialized with `UUIDUtil.CODEC` (same shape as `SIRE`), the mob's mirror of its root. `SpeciesRegistries.ORIGINAL_ROOT`: `AttachmentType<String>` (a `NpcOriginal.id()`), set by plan 17 on a boss entity so rites and line death can tell which NPC Original an entity is; nothing sets it in this plan except tests.

- [ ] **Step 1: Write the failing tests** (`LineIndexTest`)

`setRootAndQuery` (`setRoot(a, o, true)`: `rootOf(a)==o`, `membersOf(o)==[Member(a,true)]`); `anOriginalsOwnEntryIsNotItsMember` (`setRoot(o, o, true)`: `rootOf(o)==o`, `membersOf(o)` empty); `freeMovesTheMemberToItsOwnRoot` (`free(a, o, BROKEN_LINE)`: `rootOf(a)==a`, `freedOf(a)==Freed(o, BROKEN_LINE)`, `membersOf(o)` no longer has `a`, `membersOf(a)` empty); `settingARootClearsFreed`; `removeForgetsEverything`; `deadRootsAndOwedRoundTrip` (save then load keeps entries, `player` flags, freed records, dead roots, owed; `takeOwed` returns the root once, then empty); `loadsAnEmptyTagLeniently` (`load(new CompoundTag(), null)` gives an empty index; a record missing `freed_kind` loads as not freed).

- [ ] **Step 2: Run to verify failure** (`GM test --tests "...LineIndexTest"`). Expected: FAIL (compile).

- [ ] **Step 3: Implement `LineIndex` and the attachments**

Keep a `Map<UUID, Entry>` (`root`, `player`, nullable `Freed`) and a derived `Map<UUID, LinkedHashSet<UUID>> byRoot` rebuilt on load; every mutator calls `setDirty()`. NBT: a list of `{id, root, player, freed_from?, freed_kind?}`, a `dead` list of UUIDs, an `owed` list of `{id, root}`; every key optional on load.

- [ ] **Step 4: Run to verify they pass.** Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main src/test
git commit -m "feat(species): LineIndex, the stored line of every vampire"
```

---

### Task 4: Assigning lines (inheritance, rite-made Originals, NPC vampires, backfill, cure)

**Files:**
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/parts/species/line/Lines.java` (the service and its listeners)
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/parts/species/line/NpcLines.java` (mob assignment, weights)
- Create: `src/main/resources/data/supernaturalbloodlines/tags/entity_type/line_exempt.json` (`{"values": ["vampirism:vampire_baron"]}`: Vampirism's boss-tier vampire; the spec says "bosses" and names none)
- Modify: `parts/species/SpeciesConfig.java` (`npc_line_weights` and its override), `parts/species/SpeciesPart.java` (`Lines.register()`, `NpcLines.register()`, the new GameTest class)
- Test: `src/main/java/net/ofatech/supernaturalbloodlines/parts/species/gametest/LineGameTests.java`

**Interfaces:**
- Consumes: `LineIndex` (Task 3), `LineRules` and `NpcOriginal` (Task 1), `SireRecorder.addListener((UUID sire, LivingEntity child) -> ...)`, `BloodlineAccess.addListener`, `SpeciesData.get(server).isOriginal(UUID)`, `OriginalRules.isOriginal(Species)`, `SirelineIndex` (`sireOf`, `childrenOf`), `Supernatural.is(Player)`.
- Produces (`Lines`, all static): `void register()`; `Optional<UUID> rootOf(MinecraftServer, UUID)` (reads `LineIndex`); `boolean isOriginalId(MinecraftServer, UUID)` (an online player's species, else `SpeciesData`, else `NpcOriginal.byUuid`); `void join(MinecraftServer, UUID root, LivingEntity child)` (index plus, for a mob, the `LINE` attachment); `void makeRoot(ServerPlayer)` (a rite-made Original: own root, and every recorded descendant that still follows the player's previous root, or has none, moves to the player; stops at descendants who are Originals); `Optional<UUID> heal(ServerPlayer)` (login backfill, see below); `void freeTarget(MinecraftServer, UUID target, UUID from, LineIndex.Freed.Kind kind)` and `void freeBranch(MinecraftServer, UUID target, UUID from)` (used by Tasks 8 and 9; `freeBranch` also moves every recorded descendant of `target` whose root is `from` to `target`, `LineIndex.setRoot(d, target, isPlayer)`).
- Produces (`NpcLines`): `static void useRoll(DoubleSupplier)` (tests; `null` resets), `static Optional<NpcOriginal> pick(MinecraftServer)` (uses the dead roots of `LineIndex` and `npc_line_weights`).

Behaviour (R120 to R121, R127):
- **Inheritance:** `SireRecorder` listener: `root = LineRules.inheritedRoot(sire, isOriginalId(sire), rootOf(sire))`; present: `join(...)` (overwrites a join-time random assignment).
- **Rite-made Original:** a `BloodlineAccess` listener: `!isOriginal(before.species()) && isOriginal(after.species())` calls `makeRoot`. A sired Original (state allows one) is therefore its own root.
- **Leaving:** the same listener and `PlayerFactionEvent.FactionLevelChanged`: when `!Supernatural.is(p) && !isOriginal(species)`, `LineIndex.remove(p)`.
- **Login heal** (`PlayerLoggedInEvent`, after `SireRecorder.sync`): an Original with no entry gets `makeRoot`; a player with a recorded sire and no entry climbs `SirelineIndex.sireOf` (cycle-safe, at most 64 steps, via `isOriginalId`) to the first Original, or to the first ancestor who has an entry, and joins that root; no Original found: no line.
- **NPC vampires:** `EntityJoinLevelEvent` on the server, `entity instanceof IVampire`, not a `Player`, not in `#supernaturalbloodlines:line_exempt`: if the index has the mob, mirror it to `LINE`; else if `LINE` is set, put it in the index; else `NpcLines.pick` a root and `join`. `LivingDeathEvent` (LOWEST, like `SireRecorder`) removes a dead mob from the index. A mob whose only recorded root is a dead NPC root keeps it.

- [ ] **Step 1: Write the failing GameTests** (`LineGameTests`, `@PrefixGameTestTemplate(false)`, `template = "empty"`; players from `TestPlayers.create(helper)`; the vampire mob is `BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("vampirism:vampire"))` spawned with `helper.spawn`)

`originalIsItsOwnRoot` (`makeRoot` on an Original: `rootOf(o)==o`); `siredPlayerInheritsTheOriginalsLine` (`SireRecorder.record(o, a)`: `rootOf(a)==o`); `lineFollowsTheSireDown` (`record(a, b)`: `rootOf(b)==o`); `siredByALinelessVampireGivesNoLine` (sire with no entry, not an Original: child has no entry); `playerMadeOriginalRootsAnOwnLineAndTakesTheBranch` (o sires a, a sires b; a becomes an Original: `rootOf(a)==a`, `rootOf(b)==a`, `rootOf(o)==o`, `membersOf(o)` no longer has `a` or `b`); `curedPlayerLeavesTheLine` (remove the species and faction: entry gone); `existingSireChainIsBackfilledAtLogin` (sire chain recorded in `SirelineIndex` and `BloodlineState` but an empty `LineIndex`, call `Lines.heal(b)`: `rootOf(b)==o`); `healWithNoOriginalAboveLeavesNoLine`; `npcVampireJoinsAWeightedLine` (`NpcLines.useRoll(() -> 0.0)` with default weights: the mob's `LINE` attachment equals `NpcOriginal.MIKAEL.uuid()` and the index agrees); `npcVampireNeverJoinsADeadRoot` (`LineIndex.markDead(MIKAEL.uuid())`, roll 0.0: the root is `FINN.uuid()`); `exemptTypeGetsNoLine` (spawn `vampirism:vampire_baron` when the id exists: no entry); `convertedCreatureTakesItsSiresLine` (record a sire for the mob through `SireRecorder.record(o, mob)`: `rootOf(mob)==o`, overwriting the random root); `deadMobLeavesTheIndex`. Each test resets `LineIndex` entries it made and `NpcLines.useRoll(null)`.

- [ ] **Step 2: Run to verify failure**

Run: `GM runGameTestServer` Expected: compile errors, then the tests above FAILED.

- [ ] **Step 3: Implement `Lines`, `NpcLines`, the tag and the config key** to the behaviour above. Register the listeners from `SpeciesPart` after `SireRecorder.register()`; `NpcLines` needs `IVampire` from Vampirism (the species part already requires it).

- [ ] **Step 4: Run to verify they pass**

Run: `GM runGameTestServer` Expected: `All <n> required tests passed`.

- [ ] **Step 5: Commit**

```bash
git add src/main
git commit -m "feat(species): vampires get a stored line, NPC vampires a weighted one"
```

---

### Task 5: Line death replaces `sireline_death`

**Files:**
- Move: `parts/species/sireline/SirelineDeath.java` to `parts/species/line/LineDeath.java` (`git mv`, keep the damage type id `supernaturalbloodlines:sireline` and its datapack JSON, change its death message text)
- Modify: `parts/species/SpeciesConfig.java` (remove `SIRELINE_DEATH`, `DEFAULT_SIRELINE_DEATH`, `overrideSirelineDeath`, `sirelineDeath()`; add `original_line_death` default `true`, `originalLineDeath()`, `overrideOriginalLineDeath(Boolean)`), `parts/species/SpeciesPart.java`, `parts/species/sireline/SirelineIndex.java` (remove `addPendingPlayer`/`takePendingPlayer` and their save key: players are owed through `LineIndex` now; an old save's key is ignored), `src/test/.../SirelineIndexTest.java` (drop the pending-player tests)
- Modify: `src/main/resources/assets/supernaturalbloodlines/lang/en_us.json`
- Rewrite: `parts/species/gametest/SirelineDeathGameTests.java` as `LineDeathGameTests.java` (keep its helper that drives a white oak kill)

**Interfaces:**
- Produces (`LineDeath`): `ResourceKey<DamageType> DAMAGE_TYPE` (unchanged); `void register()`; `void run(MinecraftServer server, UUID root)` (the one entry point; plan 17 calls it with `NpcOriginal.X.uuid()` when a boss Original dies to white oak); `static boolean slay(ServerPlayer)` (unchanged contract); `static void onLogin(ServerPlayer)`; `static void onJoin(EntityJoinLevelEvent)` (unchanged: a creature owed a death does not join).
- Lang keys (en_us): `supernaturalbloodlines.line.death` = `"%s has fallen to white oak. Everyone of their line dies with them."`; `supernaturalbloodlines.line.death_login` = `"%s of your bloodline fell to white oak while you were away. Their line dies with them."`; `supernaturalbloodlines.line.unknown_original` = `"The Original of your bloodline"`; `death.attack.sireline` = `"%1$s died with their Original's line"`.

Behaviour (R122):
- Trigger: `LivingDeathEvent` LOWEST, a `ServerPlayer` whose species is an Original, `OriginalRules.isWhiteOakKill(source)` and `SpeciesConfig.originalLineDeath()`: `run(server, p.getUUID())`.
- `run`: for each `Member` of `LineIndex.membersOf(root)`: an online player gets the named `line.death` message then `slay`; a `slay` that fails (something kept them alive) `addOwed(id, root)` and the member stays; an offline player `addOwed(id, root)`; a loaded mob is discarded, an unloaded one `SirelineIndex.addPendingMob(id)`. Every slain or discarded member leaves `LineIndex`. When the root is an NPC (`NpcOriginal.byUuid`), `markDead(root)`. A freed member is not in `membersOf(root)`, so they live; a member who is an Original is their own root, so they live.
- `slay` clears the dead player's `sire` as before (so they leave `SirelineIndex` through its listener).
- `onLogin` (HIGHEST): `takeOwed(p)`; still a member of that root (`rootOf == root`) and still `Supernatural.is`: send `line.death_login` (named only when `LineRules.showsRoot(knownLine, root, always)`, else `line.unknown_original`), then `slay`; a `slay` that fails re-adds the debt. No longer a member: the debt is dropped.
- An NPC root's name for a message is `Component.translatable(NpcOriginal.translationKey())` (Task 15 adds the six lang keys); a player root's is the profile name from `SireRecorder.nameOfId` (make it package-visible or public).

- [ ] **Step 1: Write the failing tests** (`LineDeathGameTests`)

`whiteOakKillOfAnOriginalKillsTheWholeLine` (O with online players A (sired by O) and B (sired by A), a vampire mob M and an offline member C in the index: the white oak kill; A and B `isDeadOrDying()` with `DAMAGE_TYPE`, M removed, C owed, all four gone from `LineIndex`, O's own entry stays); `hybridsDieWithTheLine` (a `Species.HYBRID` member dies); `freedMemberSurvives` (A freed with `BROKEN_LINE`: A alive, B (still O's) dead); `nonWhiteOakDeathDoesNothing` (an ordinary kill of O leaves everyone alive); `lineDeathOffKillsNobody` (`overrideOriginalLineDeath(false)`); `offlinePlayerPaysAtLogin` (`addOwed` then `LineDeath.onLogin(c)` with a `RecordingPlayer`: it got `line.death_login` and died); `loginMessageSaysUnknownUntilDiscovered` (knownLine null: the message contains `line.unknown_original`'s text and not the Original's name; knownLine = root: it contains the name); `owedDeathIsDroppedWhenNoLongerAMember` (`addOwed`, then `LineIndex.remove`: login kills nobody and the debt is gone); `npcRootDeathKillsItsLineAndMarksItDead` (`run(server, KLAUS.uuid())` with members: they die, `isDead(KLAUS.uuid())`); `oldSirelineDeathKeyIsGone` (the config spec has no `sireline_death` path: assert through `SpeciesConfig.SPEC.getValues().contains("sireline_death") == false`).

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer` Expected: compile errors, then FAILED.

- [ ] **Step 3: Implement** `LineDeath` (rewrite `run` to walk `LineIndex`, keep `slay`, `discard`, `onJoin`), the config change, the lang keys and the `SirelineIndex` cleanup. Update `SpeciesPart` imports and registration, and every remaining reference (`grep -rn "SirelineDeath\|sirelineDeath"` must return nothing).

- [ ] **Step 4: Run to verify they pass.** `GM test` then `GM runGameTestServer`. Expected: both pass.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(species): original_line_death replaces sireline_death"
```

---

### Task 6: Discovering your Original (Rite of Revelation extended)

**Files:**
- Modify: `parts/species/SpeciesConfig.java` (`original_visibility`, `originalVisibilityAlways()`, `overrideOriginalVisibility(Boolean)`)
- Modify: `parts/species/line/Lines.java` (`visibleRoot`, `reveal`)
- Modify: `parts/species/rite/RevelationRiteFactory.java` (the rite and `report`)
- Modify: `parts/species/gametest/RevelationGameTests.java` (adapt to the new `report` signature), create `parts/species/gametest/LineDiscoveryGameTests.java`
- Modify: `lang/en_us.json`

**Interfaces:**
- Produces (`Lines`): `Optional<UUID> visibleRoot(ServerPlayer p)` (root present and `LineRules.showsRoot(knownLine, root, SpeciesConfig.originalVisibilityAlways())`); `void reveal(ServerPlayer p)` (sets `knownLine` to the player's root through `BloodlineAccess.set`; no-op without a root); `Component rootName(MinecraftServer, UUID root)` (NPC: `translatable(NpcOriginal.translationKey())`, player: profile name).
- Changes `RevelationRiteFactory.report(BloodlineState s, @Nullable Component original): Component`: after the gene lines, one more line: `supernaturalbloodlines.gene.revelation.line` (`"Your bloodline's Original is %s."`) when `original != null`, else `supernaturalbloodlines.gene.revelation.no_line` (`"You belong to no Original's line."`). The rite calls `Lines.reveal(player)` after the genes and reports `rootName` of the root when there is one.

- [ ] **Step 1: Write the failing tests** (`LineDiscoveryGameTests`)

`revelationRevealsTheOriginalOfTheLine` (a member with a root and `knownLine` null: after the rite `visibleRoot` is the root and the player was sent `gene.revelation.line` with the Original's name); `revelationIsPerPlayerNotTheirSired` (the revealed player's sired still has `visibleRoot` empty); `revelationOnALinelessPlayerSaysNoLine`; `alwaysVisibilityShowsWithoutTheRite` (`overrideOriginalVisibility(true)`: `visibleRoot` present with `knownLine` null); `discoveryIsLostWhenTheRootChanges` (`knownLine` = O1, then the player is freed to be their own root: `visibleRoot` is the new root only if `knownLine` equals it, so empty); `revelationStillRevealsGenes` (the existing gene behaviour unchanged). Update the existing `RevelationGameTests` calls to `report(state, null)`.

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer`. Expected: compile errors, then FAILED.

- [ ] **Step 3: Implement** the config key (`defineInList("original_visibility", "discovered", List.of("discovered","always"))`, comment copied in style from `gene_visibility`), the `Lines` methods, the rite change and the lang keys.

- [ ] **Step 4: Run to verify they pass.** Expected: `All <n> required tests passed`.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(species): the Rite of Revelation reveals your Original, original_visibility"
```

---

### Task 7: Warning the line when an Original is open to white oak

**Files:**
- Create: `parts/species/line/LineWarning.java`
- Modify: `parts/species/SpeciesPart.java` (register, hook the server tick like `LunarEclipse.register()`), `lang/en_us.json`
- Test: `parts/species/gametest/LineWarningGameTests.java`

**Interfaces:**
- Consumes: `Downed.isDowned(Player)`, `VampirismAPI.vampirePlayer(p).isDBNO()`, the mob effect `supernaturalbloodlines:desiccated` looked up by id through `BuiltInRegistries.MOB_EFFECT` (it belongs to the artifacts part, no import), `Lines.visibleRoot`/`rootName`, `SpeciesConfig.originalLineDeath()`.
- Produces (`LineWarning`): `void register()`; `void tick(MinecraftServer)` (every 20 ticks scans online Original players); `void opened(MinecraftServer, UUID root)` and `void closed(UUID root)` (also the API plan 17 calls for an NPC Original who is desiccated or recovers); `void onLogin(ServerPlayer)`. A transient `Set<UUID>` latches roots that are open. Lang: `supernaturalbloodlines.line.warning_named` = `"%s of your bloodline lies desiccated. A white oak blow now would end your whole line."`; `supernaturalbloodlines.line.warning_hidden` = `"The Original of your bloodline lies desiccated. A white oak blow now would end your whole line."`.

Behaviour (R124): a root that becomes open (rising edge) sends each online member (`LineIndex.membersOf(root)` that is online, never the Original) one message, named when `showsRoot` for that member, else hidden. A root that stays open warns nobody again; `closed` (or the scan seeing the Original recover) re-arms it. A member who logs in while their root is open gets the message once. Nothing is sent when `original_line_death` is off.

- [ ] **Step 1: Write the failing tests**

`desiccatedOriginalWarnsOnlineMembers` (`opened(server, KLAUS.uuid())` with member A who discovered Klaus and member B who did not: A's message text contains "Klaus", B's contains "The Original of your bloodline lies desiccated" and does not contain "Klaus"); `warningHidesTheNameUntilDiscovered` (B's message has no `Klaus`); `warnsOncePerOpening` (`opened` twice: A got one message; `closed` then `opened`: two); `playerOriginalDownedWarnsMembers` (a player Original put in `Downed` state, `LineWarning.tick(server)`: member warned once, a second tick warns nobody, after the Original gets up and goes down again: warned again); `loginWhileOpenWarns` (`opened`, then `onLogin(c)`: `c` got one message); `noWarningWhenLineDeathIsOff` (`overrideOriginalLineDeath(false)`: nobody warned); `theOriginalIsNotWarned`.

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer`. Expected: FAILED / compile errors.

- [ ] **Step 3: Implement** `LineWarning` and register it.

- [ ] **Step 4: Run to verify they pass.** Expected: `All <n> required tests passed`.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(species): warn the line when their Original lies desiccated"
```

---

### Task 8: Rite of the Broken Line

**Files:**
- Create: `parts/species/rite/BrokenLineRiteFactory.java`
- Create: `src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite/rite_of_the_broken_line.json`
- Modify: `parts/species/SpeciesConfig.java` (`line_rites_enabled`), `parts/species/SpeciesPart.java` (`RiteFactoryRegistry.get().register(BrokenLineRiteFactory.ID, BrokenLineRiteFactory.CODEC)` next to the severance one), `lang/en_us.json`
- Test: `parts/species/gametest/LineRiteGameTests.java`

**Interfaces:**
- Consumes: `LineRules.pairBloods/brokenLineRefusal` (Task 1), `Lines.rootOf/freeTarget` (Task 4), `SeveranceRiteFactory.isWitchType(Species)`, `LunarEclipse.active(MinecraftServer)`, `SiresBloodItem.sourceOf(ItemStack)`, `SireCall.player(server, uuid)`.
- Produces: `BrokenLineRiteFactory` (`ID = supernaturalbloodlines:broken_line`, `CODEC = MapCodec.unit(...)`). Rite JSON: conditions `part_enabled species`; items `supernaturalbloodlines:sires_blood` x2, `supernaturalbloodlines:white_oak_ash` x1, `enchanted:spirit_of_otherwhere` x1; shapes `enchanted:medium_circle` -> `enchanted:ritual_chalk`; `power` **12000** (twice the Severance's 6000; recorded in `docs/balance.md` in Task 15); factory type `supernaturalbloodlines:broken_line`. Lang: rite name `rite.supernaturalbloodlines.rite_of_the_broken_line` = `Rite of the Broken Line`; `supernaturalbloodlines.rite.broken_line_done` = `The line is broken. %s no longer holds you.`; refusals `supernaturalbloodlines.refusal.line_not_witch` = `Only a witch, siphoner, heretic or tribrid can break a line.`, `line_no_eclipse` = `A line can only be broken under a lunar eclipse.`, `line_no_blood` = `The rite needs two bottles of blood: the Original's and the target's.`, `line_wrong_blood` = `These bloods do not belong to one line.`, `line_target_away` = `The target must be here, in the world.`, `line_target_original` = `An Original is the head of their own line.`, `line_rites_off` = `Line-breaking rites are switched off on this server.`, and for Task 9 `line_nobody_trapped` = `Nobody stands trapped at the centre.`, `line_wrong_original` = `The one trapped is not the Original of the target's line.`, `line_ring_open` = `The mountain ash circle is not closed.`.

Behaviour (R125): `onStart` follows `SeveranceRite.onStart`: the caster is `findEntity(params.caster)`; every refusal sends the key to the caster as an action-bar message and `return cancel()` (Enchanted returns the ingredients). Order: `line_rites_enabled` off gives `line_rites_off`; then `LineRules.brokenLineRefusal(facts)` with `witchType = isWitchType(species of caster)`, `eclipse = LunarEclipse.active(server)`, the blood sources of every consumed `sires_blood` stack, `pair = LineRules.pairBloods(sources, id -> Lines.rootOf(server, id))`, `targetOnline = SireCall.player(server, pair.target()) != null`, `targetIsOriginal = Lines.isOriginalId(server, pair.target())`. On success: `Lines.freeTarget(server, target, pair.original(), BROKEN_LINE)`, set the target's `knownLine` to the Original (they learn it), tell the target `broken_line_done` with the Original's name, return `false` (the rite is done at once). The target's `sire` and `severedSire` are untouched; their descendants keep their roots (Task 4's `freeTarget` moves only the target).

- [ ] **Step 1: Write the failing tests** (`LineRiteGameTests`; reuse `SeveranceRiteGameTests`' helpers (`caster`, `type`, `item`, `place`) by copying the few lines it needs; an eclipse is `LunarEclipse.start(server)` / `stop(server)` in try/finally; the rite is started the way `SeveranceRiteGameTests` starts severance)

`brokenLineDataLoads` (rite `rite_of_the_broken_line` exists, `getPower() == 12000`, items are two `sires_blood`, one `white_oak_ash`, one `spirit_of_otherwhere`); `brokenLineFreesTheTargetAndKeepsTheSire` (O sires T: after the rite `rootOf(T)==T`, `freedOf(T)==Freed(O, BROKEN_LINE)`, `BloodlineAccess.get(T).sire()==O`, `bondBroken()` false, `knownLine==O`); `brokenLineDoesNotMakeAnOriginal` (`OriginalRules.isOriginal(T species)` false, `LineIndex.membersOf(T)` empty unless T has sired); `brokenLineLeavesTheTargetsSiredInTheLine` (T sired U: `rootOf(U)==O` after); `brokenLineWorksAgainstAPlayerOriginal` (O is a player and its blood came from `SiresBlood.take` with a silver blade); `brokenLineWorksWithAnNpcOriginalsBlood` (`SiresBloodItem.of(NpcOriginal.KLAUS.uuid(), "Klaus")`, T in Klaus's line); `brokenLineRefusals` (each case leaves the index unchanged and gives the caster the exact key, checked with the `RecordingPlayer` caster): not witch, no eclipse, one blood only, two bloods of different lines (`wrong_blood`), target offline (`target_away`), target is an Original (`target_original`), `line_rites_enabled` off (`line_rites_off`); `aFreedHeadCannotBeFreedAgainFromTheSameOriginal` (T freed once, the same bloods again give `line_wrong_blood`); `lineDeathDoesNotReachTheFreed` (after the rite, `LineDeath.run(server, O)` kills U and leaves T alive).

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer`. Expected: FAILED / compile errors.

- [ ] **Step 3: Implement** the factory, JSON, config key, registration and lang keys. Add `line_rites_enabled`/`overrideLineRites`.

- [ ] **Step 4: Run to verify they pass.** Expected: `All <n> required tests passed`.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(species): Rite of the Broken Line"
```

---

### Task 9: Rite of the Imprisoned Original

**Files:**
- Create: `parts/species/line/Imprisonment.java` (the world checks)
- Create: `parts/species/rite/ImprisonedOriginalRiteFactory.java`
- Create: `src/main/resources/data/supernaturalbloodlines/enchanted/circle_magic/rite/rite_of_the_imprisoned_original.json`
- Modify: `parts/species/SpeciesConfig.java` (`imprisonment_ring_max_radius`, `imprisonment_rite_seconds` and overrides), `parts/species/SpeciesPart.java` (register the factory; register `LineRiteRingGameTests` only when `BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("supernaturalbloodlines:mountain_ash_line"))`), `lang/en_us.json`
- Test: `parts/species/gametest/LineRiteRingGameTests.java` (needs the artifacts block), data test in `LineRiteGameTests`

**Interfaces:**
- Consumes: `RingFill.enclosed`, `LineRules.imprisonedRefusal`, `Lines.freeBranch`, `SpeciesRegistries.ORIGINAL_ROOT` (Task 3), `RiteTargets`-style centre lookup.
- Produces (`Imprisonment`): `boolean ringClosed(ServerLevel, BlockPos centre, int maxRadius)` (wall at `(x, z)` when any of `y-1`, `y`, `y+1` holds the block `supernaturalbloodlines:mountain_ash_line`; the block missing from the registry means never closed); `Optional<UUID> trappedRoot(ServerLevel, BlockPos centre)` (living entities in `new AABB(centre).expandTowards(0, 1, 0)`: a `ServerPlayer` whose species is an Original gives their UUID; an entity with the `ORIGINAL_ROOT` attachment gives `NpcOriginal.byId(...).uuid()`; the nearest wins).
- Rite JSON: conditions `part_enabled species`; items `sires_blood` x1 (the target's own blood), `white_oak_ash` x3, `spirit_of_otherwhere` x2; shapes `enchanted:large_circle` -> `enchanted:ritual_chalk` (the ring lies outside the chalk); `power` **36000** (three times the Broken Line); factory `supernaturalbloodlines:imprisoned_original`. Lang: rite name `Rite of the Imprisoned Original`; `supernaturalbloodlines.rite.imprisoned_done` = `The branch is free. %s no longer holds you or yours.`; `supernaturalbloodlines.refusal.line_ring_broken` = `The circle was broken. The rite fails.`.

Behaviour (R126). `onStart`: same caster and refusal handling as Task 8; facts: `targetBlood` the single `sires_blood` source, `targetOnline` via `SireCall.player`, `targetRoot = Lines.rootOf`, `trappedRoot = Imprisonment.trappedRoot(level, getPos())`, `ringClosed = Imprisonment.ringClosed(level, getPos(), SpeciesConfig.ringRadius())`. A refusal at the start is `cancel()` (ingredients back). Success: remember `target`, `root`, `caster` and `ticksLeft = imprisonment_rite_seconds * 20` (persist them in `saveAdditional` / `loadAdditional`), return `true` (keeps running). `onTick`: every 10 ticks rebuild the facts and call `imprisonedRefusal`; any refusal (ring broken, the Original gone or replaced, the eclipse over, the caster gone) tells the caster `line_ring_broken`, and ends the rite with no effect (the ingredients are spent). At 0: `Lines.freeBranch(server, target, root)`, `knownLine` of the target set to the root, `imprisoned_done` to the target, finish. Verify with `javap -p -c` on `Rite.tick()`/`cancel()` (Enchanted 4.2.7, `net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite`) that returning `true`/`false` from `onTick` and calling `cancel()` mid-rite behave as stated, and adjust the return values if not.

- [ ] **Step 1: Write the failing tests**

In `LineRiteGameTests` (no ring needed): `imprisonedDataLoads` (power 36000, items as above, shape `large_circle`); `imprisonedCostsMoreThanBrokenLine` (36000 > 12000).
In `LineRiteRingGameTests` (builds a square ring of `mountain_ash_line` blocks of radius 6 around the circle's centre; players or a test entity with `ORIGINAL_ROOT = "klaus"` stand on the centre): `imprisonedFreesTargetAndBranch` (O sired T, T sired C and D: after `imprisonment_rite_seconds` (override to 5) the rite ends with `rootOf(T)==T`, `rootOf(C)==T`, `rootOf(D)==T`, `freedOf(T)==Freed(O, IMPRISONED)`, `freedOf(C)` empty, `knownLine(T)==O`); `imprisonedNeverFreesTheWholeLine` (O also sired S: `rootOf(S)==O` still, `membersOf(O)` contains S and not T, C, D, and `LineDeath.run(server, O)` kills S and spares T, C, D); `imprisonedWorksOnAPlayerOriginal`; `imprisonedWorksOnAnNpcOriginalEntity`; `imprisonedFailsWhenTheRingBreaksMidRite` (remove one ring block at half time: after the full time nothing moved and the caster got `line_ring_broken`); `imprisonedFailsWhenTheOriginalStepsOut` (teleport the Original off the centre mid-rite); `imprisonedFailsWhenTheEclipseEnds`; `imprisonedRefusals` (checked at the start, exact keys, ingredients kept): open ring (`line_ring_open`), nobody trapped (`line_nobody_trapped`), a trapped Original who is not the target's root (`line_wrong_original`), no eclipse, not witch, target offline, target is the Original.

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer`. Expected: FAILED / compile errors.

- [ ] **Step 3: Implement** `Imprisonment`, the factory (a `Rite` subclass like `SeveranceRite`), JSON, config keys, registration and lang keys.

- [ ] **Step 4: Run to verify they pass.** `GM runGameTestServer` Expected: `All <n> required tests passed`; then `GM runGameTestServer -PpartsOff=artifacts`: also passes (the ring tests are simply not registered).

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(species): Rite of the Imprisoned Original"
```

---

### Task 10: The line in the hub snapshot

**Files:**
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/shared/supernatural/HubSocial.java` (`Origin` record, `Social.origin`, `Social.NONE`)
- Modify: `parts/species/SpeciesHub.java` (fill it)
- Modify: `parts/supernatural/net/HubSnapshotPayload.java` (`Family.origin`, its codec; `of(ServerPlayer)`), `parts/supernatural/client/hub/DevDemo.java`, `parts/supernatural/gametest/HubGameTests.java`
- Test: `parts/supernatural/gametest/HubGameTests.java` (new tests), `src/test/.../core/bloodline/LineRulesTest.java` (state table, already in Task 1)

**Interfaces:**
- Produces (`HubSocial`): `record Origin(LineRules.State state, Optional<String> root, Optional<String> freedFrom, FreedKind freedKind, List<Boolean> ancestorHidden)` with `enum FreedKind { NONE, BROKEN_LINE, IMPRISONED }` and `Origin.NONE = new Origin(State.NOT_VAMPIRE, Optional.empty(), Optional.empty(), FreedKind.NONE, List.of())`. `root` present = the name to show, empty = draw "?"; an NPC Original's name is sent as `"npc:<id>"` (`NpcOriginal.id()`), the client translates it with `NpcOriginal`'s lang key. `freedFrom` follows the same convention and is only meaningful in `FREE`. `ancestorHidden` is parallel to `Family.ancestors()`: true for an ancestor who is not a player (drawn as "?"); a player ancestor is always shown. `Social` gains a last component `Origin origin`.
- `HubSnapshotPayload.Family` gains a last component `Origin origin` (6 components, the maximum of `StreamCodec.composite`), `Family.NONE` uses `Origin.NONE`. Codec: state as `stringUtf8(16)` (enum name), `ByteBufCodecs.optional(NAME)` for both names, kind as `stringUtf8(16)`, `ByteBufCodecs.BOOL.apply(ByteBufCodecs.list(8))` for `ancestorHidden` (capped like the other lists).
- `SpeciesHub.social(p)` builds the origin from the server state: `original = OriginalRules.isOriginal(species)`, `vampire = Supernatural.is(p)`, `free = LineIndex.freedOf(p).isPresent()`, `hasRoot`; `state = LineRules.state(...)`; `root` only when `Lines.visibleRoot(p)` is present (never otherwise); `freedFrom` present only when `knownLine` equals the freed record's `from`; `ancestorHidden` from the `SirelineIndex` node types (`"minecraft:player"` shown, anything else hidden).

- [ ] **Step 1: Write the failing tests** (`HubGameTests`)

`snapshotCarriesTheOrigin` (a member who discovered O: `HubSnapshotPayload.of(p).family().origin()` has state `IN_LINE`, `root == Optional.of(O's name)`); `snapshotNeverLeaksAHiddenRoot` (knownLine null, `overrideOriginalVisibility(false)`: `root` empty, and the encoded bytes of the payload (`STREAM_CODEC.encode` into a `FriendlyByteBuf`) contain neither O's name nor O's UUID bytes); `snapshotOfAFreePlayer` (state `FREE`, `freedKind == BROKEN_LINE`, `freedFrom` present only when discovered); `snapshotOfAnNpcRootUsesTheNpcPrefix` (`root == Optional.of("npc:klaus")`); `snapshotMarksNpcAncestorsHidden` (a creature ancestor in the index: `ancestorHidden` true at that position); `snapshotStatesTable` (an Original: `ORIGINAL`; a human: `NOT_VAMPIRE`; a vampire with no sire: `SIRE_UNKNOWN`); `snapshotRoundTrips` (encode then decode equals the original, with an empty `Origin.NONE` and a full one). Fix the two existing `Family` constructions in `HubGameTests` and `DevDemo` (pass `Origin.NONE`).

- [ ] **Step 2: Run to verify failure.** `GM runGameTestServer`. Expected: compile errors / FAILED.

- [ ] **Step 3: Implement** the records, the codec and `SpeciesHub`.

- [ ] **Step 4: Run to verify they pass.** Expected: `All <n> required tests passed`.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(hub): the snapshot carries the player's line (never a hidden root)"
```

---

### Task 11: The `LineTree` model (pure), keyboard walk, narration

**Files:**
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/shared/hub/kit/LineTree.java`
- Modify: `shared/hub/kit/FocusOrder.java` (`zoneOf`: ids starting `line:` belong to the stage zone, ordered root, ancestors, sire, you, descendants), `src/test/.../FocusWalkTest.java` (add the `line:` ids to `FRAME`)
- Test: `src/test/java/net/ofatech/supernaturalbloodlines/shared/hub/kit/LineTreeTest.java`

**Interfaces:**
- Consumes: `HubSocial.Origin`, `HubSocial.Kin`, `LineRules.State`.
- Produces (`LineTree`, pure, no Minecraft classes): `enum Kind { ROOT, ANCESTOR, SIRE, YOU, DESCENDANT }`; `record Node(String id, Kind kind, String name, boolean hidden, int depth)` (`name` is the raw name or `npc:<id>`; `hidden` means draw "?"); `enum LinkStyle { SOLID, BROKEN, FAINT }`; `record Link(String upper, String lower, LinkStyle style)`; `record Model(LineRules.State state, List<Node> nodes, List<Link> links, boolean padlock, int hiddenBelow)`; `static Model build(HubSocial.Origin origin, String you, Optional<String> sire, boolean severed, List<String> ancestors, List<HubSocial.Kin> below)`; `enum Key { UP, DOWN, LEFT, RIGHT }`; `static Optional<String> move(Model m, String at, Key key)`; `record Narr(String key, List<String> args)`; `static Narr narrate(Model m, Node n)`.
- Node ids: `line:root`, `line:anc:<i>` (0 is the farthest shown), `line:sire`, `line:you`, `line:down:<i>` (i from 0, only depth-0 sired, at most `MAX_BELOW = 3` shown; `hiddenBelow` counts the rest). Ancestors shown: at most the nearest 2, as the current page does.
- Shape rules: `ORIGINAL` has no `line:root`, `line:anc`, `line:sire` (you are the crown); `NOT_VAMPIRE` and `SIRE_UNKNOWN` have no root; `IN_LINE` has `line:root` (hidden when `origin.root()` is empty) then the ancestors then the sire then you, links all `SOLID` going down the chain, plus a `FAINT` link from every `line:down:<i>` to `line:root`; `FREE` has the same nodes (the root drawn as the ghost above, or none when the sire is unknown and the root is hidden) but the link from the node above you (`line:sire` or the last of the chain) to `line:you` is `BROKEN`, `padlock == true`, and the `FAINT` links to the root exist only when `origin.freedKind() == BROKEN_LINE` (an Imprisoned branch follows you, so its links are `SOLID` to you). `severed` (a Rite of Severance) draws the sire link `BROKEN` as today; in `FREE` the sire link is `BROKEN` as well.
- `move`: `UP` goes to the node above in the chain (a descendant goes to `line:you`), `DOWN` goes one step down the chain and from `line:you` to `line:down:0`, `LEFT`/`RIGHT` move between `line:down:<i>` siblings (no wrap); every other case returns empty.
- `narrate(model, node)`: keys `supernaturalbloodlines.hub.bloodline.narr.{root|root_hidden|ancestor|ancestor_hidden|sire|sire_free|you_original|you_free|you|descendant}` with `args = [displayName, rootName or "?"]`. Example pinned by a test: in `FREE` with sire `"Tyler"` and `freedFrom == Optional.of("npc:klaus")`, `narrate` of `line:sire` is `Narr("supernaturalbloodlines.hub.bloodline.narr.sire_free", ["Tyler", "npc:klaus"])`, whose en_us text is `"%1$s, your sire. Link above you broken: you are free of %2$s's line."`.

- [ ] **Step 1: Write the failing tests** (`LineTreeTest`)

`originalIsTheCrown` (`ORIGINAL`: nodes are `line:you` then descendants, `padlock` false, no root); `inLineHasTheChainAndFaintLinks` (root `"Klaus"`, ancestors `["A","B","C"]`, sire `"S"`, below `[Kin("D",0), Kin("E",0)]`: nodes `line:root, line:anc:0, line:anc:1, line:sire, line:you, line:down:0, line:down:1` (ancestors capped at the nearest 2: `["B","C"]`), the `line:down:*` links to `line:root` are `FAINT`); `hiddenRootIsAQuestionMark` (`root` empty: the root node has `hidden == true`); `npcAncestorsAreHidden` (`ancestorHidden=[true,false]`); `playerAncestorsAreAlwaysShown`; `freeBreaksTheLinkAboveYouAndShowsThePadlock` (`BROKEN` link `line:sire -> line:you`, `padlock` true); `freeByBrokenLineKeepsFaintLinks` and `freeByImprisonmentDoesNot` (the `FAINT` links are absent, the descendant links are `SOLID` to `line:you`); `severedSireLinkIsBroken`; `moreThanThreeBelowCountsTheRest` (5 sired: 3 nodes, `hiddenBelow == 2`); `arrowKeysWalkTheTree` (UP from `line:down:1` is `line:you`; UP again `line:sire`; UP again `line:anc:1`; UP from `line:root` empty; DOWN from `line:you` is `line:down:0`; RIGHT from `line:down:0` is `line:down:1`; RIGHT from the last is empty; LEFT from `line:down:0` is empty); `narrationNamesTheBreak` (the example above); `narrationNeverNamesAHiddenRoot` (hidden root: args contain `"?"` and never the name); `everyNodeIdIsUniqueAndHasAZone` (`FocusOrder.zoneOf(id)` is the stage zone for all).

- [ ] **Step 2: Run to verify failure.** `GM test --tests "net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTreeTest" --tests "net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.FocusWalkTest"` Expected: FAIL.

- [ ] **Step 3: Implement `LineTree`** and the `FocusOrder` zone.

- [ ] **Step 4: Run to verify they pass.** Same command. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(hub): LineTree model, walk and narration"
```

---

### Task 12: Bloodline page, Grimoire

**Files:**
- Create: `parts/supernatural/client/hub/page/GrimoireBloodlinePage.java`, `parts/supernatural/client/hub/page/BloodlineGenes.java` (the vials and the gene lines moved out of `BookBloodlinePage`, public static `draw(HubScreen.Frame, HubSnapshotPayload)` so Task 13 and Task 14 reuse them), `shared/hub/kit/LineTreeLayout.java` (pure; Grimoire function here, the other two in Tasks 13 and 14)
- Modify: `parts/supernatural/client/hub/theme/GrimoireTheme.java` (use the new page), `parts/supernatural/client/hub/DevDemo.java` (demo states: `-Dsupernaturalbloodlines.devshots.origin=original|none|unknown|inline|free`), `lang/en_us.json` (page text, tooltips, narration, the six `supernaturalbloodlines.original.<id>` names: Mikael, Finn, Elijah, Kol, Rebekah, Klaus), `docs/hub-ui-kit.md`
- Test: `src/test/.../shared/hub/kit/LineTreeLayoutTest.java`

**Interfaces:**
- Consumes: `LineTree` (Task 11), `BookKit` (`head`, `portrait`, `seal`, `small`, `leader`, `dropCap`), `Detail` kit, `HubScreen.Frame` (`region`, `target`, `detail`, `reserve`, `focused`, `screen().setPage`), `HubPage.RITES`, the hub's `HubPageView.keyPressed(int key, int modifiers)`.
- Produces: `LineTreeLayout.grimoire(Rect region, LineTree.Model m, int portrait): Map<String, Rect>` (node id to the rectangle of its portrait plus label; one column, top to bottom in chain order, descendants in a row under `line:you`, every rectangle inside `region`, `portrait` 14 or 11 as the current page picks for a short page). `GrimoireBloodlinePage implements HubPageView`.

Page content (all from the spec and the Figma "Bloodline v3" Grimoire frame): left page is the ink tree drawn with `BookKit.portrait` nodes and 1-pixel ink stems; `BROKEN` link as torn ink (a gap with two short ragged stubs) in `t.accent()` red and a red margin note beside it with the tooltip text `Line broken from %s (Rite of the Broken Line)` or `(Rite of the Imprisoned Original)`; `FAINT` links as dotted ink in `t.muted()`; a wax padlock seal (an open padlock, `BookKit.seal` style) on the `line:you` portrait when `padlock`; a hidden node is a portrait with a `?`. Right page: the selected node's `Detail` (name, species, sire, line status line from the five states, actions) and under it the gene vials from `BloodlineGenes.draw`. The actions are `call` (`BloodlinePage.command("bloodline call")`, enabled when `BloodlinePage.MAY_CALL` contains the player's race and they have sired), `severance` and `broken_line` (each opens `HubPage.RITES` through `f.screen().setPage`, enabled only when the selected node is `line:you` and the state allows it: Severance when `sire` is present and not severed, Broken Line in `IN_LINE`; a disabled one carries a `disabledReason`). Empty states keep `EmptyState.render` for `NOT_VAMPIRE`. Arrow keys call `LineTree.move` and focus the result; Tab order comes from `FocusOrder`; narration comes from `LineTree.narrate`.

- [ ] **Step 1: Write the failing layout test** (`LineTreeLayoutTest.grimoire...`)

For the region sizes used by the existing Grimoire layout tests and the states original, in-line (3 below), free: `everyNodeIsInsideItsRegion`, `noTwoNodesOverlap`, `chainOrderIsTopToBottom` (root above ancestors above sire above you), `descendantsShareARowUnderYou`, `shortPageUsesSmallPortraits`.

- [ ] **Step 2: Run to verify failure.** `GM test --tests "net.ofatech.supernaturalbloodlines.shared.hub.kit.LineTreeLayoutTest"` Expected: FAIL.

- [ ] **Step 3: Implement** the layout, the page, the shared genes helper, the theme wiring, the demo states and the lang keys. `BookBloodlinePage` stays until Task 14 removes its last user.

- [ ] **Step 4: Run to verify they pass, then look at it**

Run: `GM test` Expected `BUILD SUCCESSFUL`. Then the dev shots from `docs/hub-ui-kit.md` ("Dev screenshots (no clicking)"): `-Dsupernaturalbloodlines.devshots=grimoire:bloodline -Dsupernaturalbloodlines.devshots.origin=free` (and `inline`, `original`, `unknown`, `none`) at scales 2 and 4; compare each image to the Figma "Bloodline v3" Grimoire frame (fetched with `get_screenshot`): same layout, no clipped text, the red margin note and the padlock visible in `free`. Fix differences before committing.

- [ ] **Step 5: Commit**

```bash
git add -A src docs
git commit -m "feat(hub): Bloodline page in the Grimoire, with the line, the break and the padlock"
```

---

### Task 13: Bloodline page, Blood Moon

**Files:**
- Create: `parts/supernatural/client/hub/page/MoonBloodlinePage.java`
- Modify: `shared/hub/kit/LineTreeLayout.java` (`moon`), `parts/supernatural/client/hub/theme/BloodMoonTheme.java`, `lang/en_us.json` (only what differs), `docs/hub-ui-kit.md`
- Test: `LineTreeLayoutTest.moon...`

**Interfaces:**
- Produces: `LineTreeLayout.moon(Rect region, Rect bottomBar, LineTree.Model m): Map<String, Rect>` (the crown/root portrait centred in the moon disc at the top of the region; the chain hangs below as a vertical chain of portraits; descendants behind and below the player; the details live in `bottomBar`). `MoonBloodlinePage implements HubPageView`.

Page content (Blood Moon frame of the Figma): the root Original as a portrait inside the moon (`?` when hidden; for `ORIGINAL` the player is the moon's portrait); the line hangs as a chain of portraits joined by chain links; a `BROKEN` link is two half links with a red glow between them and the tooltip `Line broken from %s (Rite of the Broken Line)`; a thin red thread (1 px, `t.accent()` at 60 % alpha) runs from each descendant, behind the player portrait, to the moon (`FAINT` links); an open padlock badge on the player's portrait when `padlock`. Details (name, species, sire, line status, actions Call / Severance / Broken Line as `Detail` pills) go in the bottom bar through `f.detail(...)` exactly as `BookBloodlinePage(true)` did for Call. Gene vials via `BloodlineGenes.draw`. Keys, focus and narration as in Task 12.

- [ ] **Step 1: Write the failing layout tests** (`moon...`): `everyNodeIsInsideItsRegion`, `noOverlap`, `rootSitsInsideTheMoonDisc` (the root rect is inside the disc rect the theme's layout exposes), `detailsStayInTheBottomBar`.
- [ ] **Step 2: Run to verify failure.** `GM test --tests "...LineTreeLayoutTest"` Expected: FAIL.
- [ ] **Step 3: Implement** the layout, the page and the theme wiring.
- [ ] **Step 4: Run to verify they pass and look at it.** `GM test`; dev shots `blood_moon:bloodline` for all five origin states at scales 2 and 4, compared with the Figma Blood Moon frame (the glowing broken chain links and the red thread visible in `free`).
- [ ] **Step 5: Commit**

```bash
git add -A src docs
git commit -m "feat(hub): Bloodline page in the Blood Moon, the Original in the moon"
```

---

### Task 14: Bloodline page, Tensura

**Files:**
- Create: `parts/supernaturaltensura/client/TensuraBloodlinePage.java`
- Modify: `shared/hub/kit/LineTreeLayout.java` (`tensura`), `parts/supernaturaltensura/client/TensuraHubTheme.java` (use it), `lang/en_us.json` (`supernaturalbloodlines.hub.bloodline.severed_label` = `SEVERED`), `docs/hub-ui-kit.md`; delete `parts/supernatural/client/hub/page/BookBloodlinePage.java` and `BloodlinePage`'s now-unused tree code if nothing else uses it (keep `BloodlinePage.MAY_CALL`, `name`, `command`)
- Test: `LineTreeLayoutTest.tensura...`, `src/test/.../parts/supernaturaltensura/client/TensuraHubLayoutTest.java` (still green)

**Interfaces:**
- Produces: `LineTreeLayout.tensura(Rect stage, Rect infoPane, LineTree.Model m): Map<String, Rect>` (hex nodes on a vertical axis; every rectangle is the hex's bounding box and `Shapes.contains(Shape.HEX, size, x, y)` is the hit test). `TensuraBloodlinePage implements HubPageView` (the Tensura part may import the supernatural page kit: the documented exception).

Page content (Tensura frame): hex nodes (`Shapes`/`Bake`, as the Tensura skill tree draws them) with teal connectors (`TensuraHubTheme` accent); a `BROKEN` link is a red dashed connector labelled `SEVERED` (`severed_label`) with the tooltip `Line broken from %s (Rite of the Broken Line)`; `FAINT` links are thin dim teal; the padlock is a small open-lock glyph on the player's hex; hidden nodes are hexes with `?`. Details (name, species, sire, line status, actions) fill the right info pane through `f.detail(...)`; gene vials via `BloodlineGenes.draw` below. Keys, focus and narration as in Task 12.

- [ ] **Step 1: Write the failing layout tests** (`tensura...`): `everyHexIsInsideTheStage`, `noOverlap`, `hexHitTestMatchesTheDrawnShape`, `infoPaneKeepsItsWidth`.
- [ ] **Step 2: Run to verify failure.** `GM test --tests "...LineTreeLayoutTest"` Expected: FAIL.
- [ ] **Step 3: Implement** the layout, the page and the theme wiring; delete `BookBloodlinePage` and fix its imports in the three themes (all three now use their own page).
- [ ] **Step 4: Run to verify they pass and look at it.** `GM test` Expected `BUILD SUCCESSFUL`; dev shots `tensura:bloodline` for the five origin states at scales 2 and 4, compared with the Figma Tensura frame (the red dashed "SEVERED" connector in `free`). Also `GM runGameTestServer -PpartsOff=supernaturalTensura` passes.
- [ ] **Step 5: Commit**

```bash
git add -A src docs
git commit -m "feat(hub): Bloodline page in the Tensura theme, hexes and the SEVERED connector"
```

---

### Task 15: Docs, rules, balance, changelog and the whole matrix

**Files:**
- Modify: `docs/superpowers/specs/SPECIES-RULES.md` (R120 to R127 as listed above; the old sireline death sentence in the sireline section now points to R122), `docs/parts/species.md` (the line index, line death, discovery, warnings, both rites, the config table, the removed key, the NPC root API for plan 17: `NpcOriginal`, `LineDeath.run`, `LineWarning.opened/closed`, `SpeciesRegistries.ORIGINAL_ROOT`, `LineIndex`), `docs/balance.md` (Broken Line 12000 power, Imprisoned 36000 power and 30 s, ring radius 8, NPC weights equal, with the reasons: 2x and 6x the Severance's 6000), `docs/mixins.md` (one line under species: this plan adds none), `docs/hub-ui-kit.md` (done per task), `wiki/Sirelines.md`, `wiki/Rites.md`, `wiki/Rules-Reference.md`, `wiki/For-Server-Owners.md` (the new keys and the removed `sireline_death`), `wiki/Known-Limits.md` (a rite target must be online; NPC Originals arrive with the boss update; line membership of a world from before is rebuilt at login), `wiki/Items-And-Blocks.md` (the two rites' ingredients), a new `wiki/Original-Bloodlines.md` (what a line is, line death, how to find your Original, both rites, the page) and `wiki/_Sidebar.md`, `CHANGELOG.md` (Unreleased: Added, Changed, Removed `sireline_death`)

**Interfaces:** none.

- [ ] **Step 1: Make the doc edits.** Each wiki page states the same facts as the Global Constraints, in player language.
- [ ] **Step 2: Check nothing still mentions the old key.** Run: `git grep -n "sireline_death\|SirelineDeath" -- . ":!CHANGELOG.md" ":!docs/superpowers/plans" ":!docs/superpowers/specs"` Expected: no output.
- [ ] **Step 3: Run the whole matrix.** `pwsh -NoProfile -File C:\Users\caiom\AppData\Local\Temp\opencode\matrix13d.ps1`, then read `C:\Users\caiom\AppData\Local\Temp\opencode\matrix-13d.log`. Expected: 10 lines (`build`, `runGameTestServer`, `-PnoWerewolves -PnoCurios`, `-PnoLinkedMods`, `-PpartsOff=species`, `artifacts`, `speciesTensura`, `supernatural`, `supernaturalTensura`, `hud`), each `BUILD SUCCESSFUL` or `All <n> required tests passed`, none with `failed at`, last line `DONE`. The `-PpartsOff=species` run must not contain a `Line*` test, and `-PpartsOff=supernatural` must still pass (the species side builds the snapshot data through `HubSocial` only).
- [ ] **Step 4: Commit**

```bash
git add -A docs wiki CHANGELOG.md
git commit -m "docs: original bloodlines, line death and the line-breaking rites (R120-R127)"
```

---

## Self-review

- **Spec coverage:** section 2 (stored line, roots, NPC vampires and weights, inheritance, line death, offline login, config, replaces `sireline_death`, dead and missing roots, NPC Originals not counted, which mobs, discovery and `original_visibility`, warning and login message, hybrids) is Tasks 1 to 7; section 3 is Task 8; section 4 (including both rites against a player Original) is Task 9 and Task 8's `brokenLineWorksAgainstAPlayerOriginal`; section 5 (content, the five states, per-theme looks, keyboard, narration, the Figma reference) is Tasks 10 to 14; docs and matrix are Task 15. Anti-goals are tests in Tasks 5, 7, 8 and 9. "The Broken Line needs that Original's blood ... fails with the wrong blood" is `brokenLineRefusals`.
- **Types:** `LineIndex.Freed`/`Member`, `LineRules.State/BloodPair`, `HubSocial.Origin`, `LineTree.Model` are named alike in every task that uses them; `Lines.freeTarget/freeBranch` (Task 4) are what Tasks 8 and 9 call; `LineDeath.run(MinecraftServer, UUID)` and `LineWarning.opened/closed` are the entry points plan 17 uses.
- **Spec gaps decided here:** (1) roots are UUIDs, the six NPC roots have fixed `nameUUIDFromBytes` UUIDs, so blood bottles (`SiresBloodItem`, whose source is a UUID) and `LineIndex` need no new item or type; plan 17's boss blood drops must use `NpcOriginal.X.uuid()`, not the entity's UUID; (2) the stored line lives in a server-wide `LineIndex` (offline players, unloaded mobs), with a mob attachment as a mirror; the spec said "stored per vampire"; (3) a freed player's line is their own UUID with a `Freed` record, and a branch freed by imprisonment follows them; (4) the target of both rites must be online, found by their blood, with no consent step (as Severance has none); (5) the Imprisoned rite takes the target's blood only and finds the Original by who stands on the centre (a player Original, or an entity with the `ORIGINAL_ROOT` attachment that plan 17 sets), and plan 17 must also make Original boss entities stop at mountain ash; (6) "bosses" exempt from NPC lines is the tag `line_exempt`, shipped with `vampirism:vampire_baron` only; (7) "desiccated" for a player Original means the artifacts `desiccated` effect, Vampirism's dying state or our `Downed` state; (8) rite power 12000 and 36000, 30 s, ring radius 8 are builder numbers; (9) the freed target learns which Original they were freed from; (10) the old `sireline_death` key and `SirelineIndex`'s pending-player key are ignored, not migrated (the old rule was off by default).
- **Plan size:** 15 tasks for a spec of five sections; the page work is three tasks because each theme has its own look, layout and reviewer.
