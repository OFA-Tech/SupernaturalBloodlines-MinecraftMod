# Plan 21: TVD rites (invitation ward, locator, two curses): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship three things from the owner's approved list, as ideas only (Bewitchment is All Rights Reserved: no code, art or text): **(21a)** a vampire **invitation ward** in two forms (an area ward at an altar, and a smaller door ward) with an invite list, spoken and hub invitations, expulsion on revoke and the dead-owner rule; **(21b)** a **locator** rite that finds a player through a taglock or a sealed blood bottle and reads on the Vampire Compass in a third mode; **(21c)** the **Curse of Insanity** and the **Curse of Susceptibility**, built like the two Tensura curses.

**Architecture:** Pure rules in new `core.ward`, `core.locator` and `core.curse` packages (JUnit, no game classes). The ward is a record in a SavedData `WardIndex` (`parts.artifacts.ward`), enforced by one event listener every 5 ticks and shown through one new S2C payload `WardViewPayload`. The locator is a rite factory in `species` plus a `shared.species.LocatorTargets` bridge that the artifacts part's compass reads (the same pattern as `DoppelgangerIndex`). The curses are `Curse.Type`s in `parts.vampirismenchanted.curse`, applied by Enchanted's own `enchanted:apply_curse` data factory.

**Tech Stack:** Java 21, NeoForge 1.21.1 (21.1.265), Vampirism 1.10.13, Werewolves 2.0.3.4, Enchanted 4.2.7, Open Parties and Claims 0.33.0 (reflection only, optional), JUnit 5, the repo's GameTest runner.

**Spec:** [docs/superpowers/specs/2026-11-23-tvd-rites-design.md](../specs/2026-11-23-tvd-rites-design.md). Read it first (sections 2 to 5 and 9); this plan does not repeat it. Research: [research/bewitchment-tvd-rites.md](../research/bewitchment-tvd-rites.md).

Repo conventions that apply: one branch per sub-plan (`feature/plan-21a-ward`, `feature/plan-21c-curses`, `feature/plan-21b-locator`), one PR each, a `CHANGELOG.md` entry with a "Rolling back" paragraph, `docs/parts/<part>.md` ticked, the R-rules copied into `docs/superpowers/specs/SPECIES-RULES.md` as **R300 to R329**, no narrating code comments, hub shots per `docs/safe-screenshots.md` under the owner-game rule, every heavy run through `gt-slot.ps1` with `--max-workers=2` and a hard timeout, worktrees under `D:\Development\OFA-TECH\.worktrees\SupernaturalBloodlines\`, absolute paths.

## Global Constraints

- **Rules R300 to R329** (spec section 0): 21a R300-R314, 21b R315-R319, 21c R320-R324, shared R325-R329. If another plan took any of them by merge time, shift this block (as 20a's plan says to) and say so in the PR.
- **No oracle (R325), hidden Originals (R326), consent (R327)** are acceptance criteria, not notes. Each has a named test in the task that builds the thing (see Review Focus). A refusal is **one message per rite and costs the same**; the needle has **one** unfindable state.
- **No new Part.** Ward and locator-compass in `artifacts`, locator rite in `species`, curses in `vampirismEnchanted`. `Part`, `requiredParts` and the 12-run matrix do not change. (`ARTIFACTS` and `SPECIES` both require `VAMPIRISM_ENCHANTED`, verified in `Part.java`; `ARTIFACTS` does not require `SPECIES`, so the compass never imports a species class.)
- **Config:** 23 keys (spec section 9), each with an `AdminEntries` row and a `Config` spec entry that `AdminCoverageTest` knows. `AdminEntries.ALL.size()` becomes **(main's size at merge time) + 13 / + 5 / + 5** per sub-plan (178 today; check `AdminEntriesTest` on the branch after merging main).
- **Wire:** one new S2C payload `WardViewPayload` on `LadderNet`'s optional registrar; `LadderNet.PROTOCOL` becomes **the integer after the one on `main` at merge time** (11 today, so 12); `SnapshotIdsWireTest` pins the new payload's golden bytes and the protocol together. `HubSnapshotPayload`, `HallucinationPayload` and `ModNetwork` are **not** edited.
- **Commands:** `/ward invite <player>`, `/ward uninvite <player>`, `/ward drop <ward>`, `/ward info`; rows `W1`-`W4` in `AdminCommands` (check no clash first, Task 0); `CommandCoverageGameTests` must stay green.
- **Lang prefix** `supernaturalbloodlines.ward.`, `.locator.`, `.curse.`; `HubLangCoverageTest` and the rites-book tests must stay green.
- **Tests that can fail:** every task starts with a failing test and ends with the mutation list written in its last step. A task is not done until each listed mutant is run and killed (the pack-moon plan's `mutate.ps1` pattern: one text replacement, run the narrow test or the GameTest run, `git checkout --` the file).
- **GameTest runs** one at a time; restore `run/config` after any killed run; `focus-watch.ps1` is always given `-MaxSec 900`.

## Review Focus

Inputs the spec implies and no happy path covers, most likely first. Each has a test in the task named in brackets.

1. **The refusal oracle.** A ward rite for an owner who is a vampire, a human and an Original must succeed or fail identically; a locator on a villager's taglock, an NPC Original's bottle and an unknown bottle must give the same message at the same cost. [Task 6 `wardRiteIsIdenticalForEveryOwnerSpecies`, Task 15 `locatorRefusalsAreOneMessageAtOneCost`]
2. **A non-sneaking player on the circle's centre** cancels the ward rite and gives everything back. [Task 6 `aStandingPlayerOnTheCentreCancelsTheWard`]
3. **A dead owner with the ward in an unloaded chunk.** The ward must stand down without the chunk loading. [Task 8 `ownerDeathFallsWardsInUnloadedChunks`]
4. **A vampire boxed into the ward** (one-wide corridor, a roof) is not pushed forever. [Task 5 `aBoxedInVampireIsTeleportedOutAfterTheTimeout`]
5. **A hybrid co-owner confusion**: a hybrid who is *invited* must not be pushed by a second ward they are not invited to, and one who is the owner of the first is not blocked by it. [Task 5 `invitationIsPerWard`]
6. **The compass in a chest keeps its attunement and loses its needle.** [Task 14 `aChestedCompassKeepsTheAttunementOnly`]
7. **The same needle for every reason.** Offline, dead, other dimension, out of range, cloaked, expired and creative must yield the same tracker value. [Task 13 `everyReasonLooksTheSame`, Task 14 `theNeedleSpinsTheSameForEveryReason`]
8. **Two curses on one player and a death.** Both end at death, neither survives a respawn, and a second application does not stack. [Task 11 `bothCursesEndAtDeathAndDoNotStack`]
9. **Susceptibility and the damage probe.** `DamageProbe.active()` must see the original amount. [Task 11 `susceptibilityNeverRaisesAProbe`]
10. **A server without Open Parties and Claims, with it, and with a broken API.** The claim check must not exist, must work, and must fail open to "no check" with one warning respectively. [Task 8 `claimGuardFailsOpen`]

## File structure

Create (main, `src/main/java/net/ofatech/supernaturalbloodlines/`):
- `core/ward/WardRules.java`, `WardTable.java`, `WardSpeech.java`: pure.
- `core/locator/LocatorRules.java`: pure.
- `core/curse/SusceptibilityRules.java`, `InsanityRules.java`: pure.
- `parts/artifacts/ward/WardIndex.java` (SavedData), `WardEnforcer.java`, `WardRiteFactory.java`, `WardCommand.java`, `WardChat.java`, `WardView.java`, `ClaimGuard.java`.
- `parts/supernatural/net/WardViewPayload.java`; client: `parts/supernatural/client/hub/page/WardCard.java`.
- `shared/species/LocatorTargets.java` (the bridge).
- `parts/species/rite/LocatorRiteFactory.java`, `parts/species/locator/LocatorAttunement.java`.
- `parts/vampirismenchanted/curse/InsanityCurse.java`, `SusceptibilityCurse.java`, `PlagueCurses.java` (registration, named like `Curses` of the Tensura part).

Create (data): `data/supernaturalbloodlines/enchanted/circle_magic/rite/` `rite_of_the_ward.json`, `rite_of_the_door_ward.json`, `rite_of_the_locator.json`, `rite_of_the_locator_<item>.json` (one per blood item Task 0 proves is stamped), `curse_insanity.json`, `curse_susceptibility.json`.

Modify: `parts/artifacts/MountainAshRules.java` (extract `pushBack`), `parts/artifacts/item/CompassMode.java` and `VampireCompassItem.java`, `parts/artifacts/ArtifactItems.java` (two components), `core/rite/RiteUnlockTable.java`, `shared/admin/AdminEntries.java`, `AdminCommands.java`, `parts/supernatural/net/LadderNet.java`, the Path page, lang, `SPECIES-RULES.md`, `docs/parts/*.md`, `wiki/*`, `CHANGELOG.md`.

Tests mirror the packages under `src/test/java/...`; GameTests: `parts/artifacts/gametest/WardGameTests`, `parts/species/gametest/LocatorGameTests`, `parts/vampirismenchanted/gametest/PlagueCurseGameTests`.

---

## Task 0: Preflight (no feature code)

**Files:** a scratch note `C:\Users\caiom\AppData\Local\Temp\opencode\plan21\preflight.md` (not committed) and, if any answer changes the spec, the spec itself.

- [ ] **Step 1: Prove each UNVERIFIED item of spec section 1, one line of evidence each.**
  - U1 `ServerChatEvent` thread: `javap -p` the dispatch in `ServerGamePacketListenerImpl` (or read the NeoForge sources jar in the Gradle cache); note whether `server.execute` is needed. The ward code uses `server.execute` regardless.
  - U3 which blood items are stamped: `Select-String` every `BloodStamp.write(` / `writeSeal(` caller; list the items. This fixes the list of `rite_of_the_locator_<item>` files.
  - U4 does `vampirismEnchanted` have a config class: `Select-String -Path ...\parts\vampirismenchanted -Pattern 'ModConfigSpec'`.
  - U5 the `HallucinationPayload` spy setter's name (read the last 40 lines of the class).
  - U6 `AdminCommands` row ids that start with `W`: `Select-String -Pattern 'row\("W'`.
  - Also: the exact id of a moonstone item (spec 5.3), and the vampire-blood item id for the Susceptibility rite.
- [ ] **Step 2: Write the answers into the spec** (section 1's table: replace `UNVERIFIED` by the evidence, or change the design line that the evidence breaks) and commit `docs: plan 21 preflight`.

---

# 21a: the invitation ward

## Task 1: The pure ward rules

**Files:** create `core/ward/WardRules.java`, `core/ward/WardSpeech.java`; test `src/test/java/.../core/ward/WardRulesTest.java`, `WardSpeechTest.java`.

- [ ] **Step 1: Write the failing tests.**

```java
package net.ofatech.supernaturalbloodlines.core.ward;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WardRulesTest {
    @Test
    void anUninvitedVampireIsBlockedAndEveryoneElseIsNot() {
        assertTrue(WardRules.blocks(true, true, true, false, false, false, false, false));
        assertFalse(WardRules.blocks(true, true, false, false, false, false, false, false), "a human is never blocked");
        assertFalse(WardRules.blocks(true, true, true, false, false, false, true, false), "the owner");
        assertFalse(WardRules.blocks(true, true, true, false, false, false, false, true), "an invited one");
        assertFalse(WardRules.blocks(true, true, true, false, false, true, false, false), "creative or spectator");
        assertFalse(WardRules.blocks(false, true, true, false, false, false, false, false), "ward_enabled off");
        assertFalse(WardRules.blocks(true, false, true, false, false, false, false, false), "a fallen ward");
    }

    @Test
    void werewolvesAreBlockedOnlyWhenTheSettingSaysSo() {
        assertFalse(WardRules.blocks(true, true, false, true, false, false, false, false));
        assertTrue(WardRules.blocks(true, true, false, true, true, false, false, false));
        assertFalse(WardRules.blocks(true, true, false, false, true, false, false, false), "a human with the setting on");
    }

    @Test
    void theBoxHasHalfSizePlusOneCellsOnEachSideOfTheCentreCell() {
        WardRules.Box b = new WardRules.Box(100, 64, 100, 2);
        assertTrue(b.contains(98.0, 64.0, 98.0));
        assertTrue(b.contains(102.99, 66.99, 102.99));
        assertFalse(b.contains(97.99, 64, 100));
        assertFalse(b.contains(103.0, 64, 100));
        assertFalse(b.contains(100, 61.99, 100));
    }

    @Test
    void theNearestFaceIsTheWayOut() {
        WardRules.Box b = new WardRules.Box(0, 64, 0, 10);
        WardRules.Dir west = WardRules.pushDirection(b, -9.0, 0.5);
        assertEquals(-1.0, west.x(), 1e-9);
        assertEquals(0.0, west.z(), 1e-9);
        WardRules.Dir south = WardRules.pushDirection(b, 0.5, 10.0);
        assertEquals(0.0, south.x(), 1e-9);
        assertEquals(1.0, south.z(), 1e-9);
        assertEquals(WardRules.pushDirection(b, 0.5, 0.5), WardRules.pushDirection(b, 0.5, 0.5), "the dead centre is deterministic");
    }

    @Test
    void theExitPointIsJustOutsideTheNearestFace() {
        WardRules.Box b = new WardRules.Box(0, 64, 0, 10);
        WardRules.Point p = WardRules.exitPoint(b, -9.0, 0.5);
        assertTrue(p.x() < -10.0 && !b.contains(p.x(), 64, p.z()));
        assertEquals(0.5, p.z(), 1e-9);
    }

    @Test
    void theInviteListHasACapAndNoDuplicates() {
        Set<UUID> list = new HashSet<>();
        UUID a = UUID.randomUUID();
        assertEquals(WardRules.InviteResult.ADDED, WardRules.invite(list, a, 2));
        assertEquals(WardRules.InviteResult.ALREADY, WardRules.invite(list, a, 2));
        assertEquals(WardRules.InviteResult.ADDED, WardRules.invite(list, UUID.randomUUID(), 2));
        assertEquals(WardRules.InviteResult.FULL, WardRules.invite(list, UUID.randomUUID(), 2));
        assertEquals(2, list.size());
    }

    @Test
    void theRadiusIsClamped() {
        assertEquals(1, WardRules.clampRadius(-5));
        assertEquals(10, WardRules.clampRadius(10));
        assertEquals(24, WardRules.clampRadius(99));
    }
}
```

```java
class WardSpeechTest {
    @Test
    void onlyTheWholeMessageIsThePhrase() {
        assertTrue(WardSpeech.matches("come in", "come in"));
        assertTrue(WardSpeech.matches("  Come in! ", "come in"));
        assertTrue(WardSpeech.matches("COME IN...", "come in"));
        assertFalse(WardSpeech.matches("please come in", "come in"));
        assertFalse(WardSpeech.matches("come in and sit", "come in"));
        assertFalse(WardSpeech.matches("", "come in"));
    }
}
```

- [ ] **Step 2: Run them to see them fail** (they do not compile: that is the failure). Command, through the slot, log under `C:\Users\caiom\AppData\Local\Temp\opencode\plan21\`:

```powershell
& C:\Users\caiom\AppData\Local\Temp\opencode\gt-slot.ps1 -Name "plan21 t1 red" -Run { & .\gradlew.bat test --tests '*WardRulesTest' --tests '*WardSpeechTest' --no-daemon --max-workers=2 --console=plain *> C:\Users\caiom\AppData\Local\Temp\opencode\plan21\t1-red.log }
```

- [ ] **Step 3: Write `WardRules` and `WardSpeech`.**

```java
public final class WardRules {
    public static final int MAX_RADIUS = 24;
    public enum Kind { AREA, DOOR }
    public enum InviteResult { ADDED, ALREADY, FULL }
    public record Dir(double x, double z) {}
    public record Point(double x, double z) {}

    /** The cube of {@code half} cells on each side of the centre cell. */
    public record Box(int cx, int cy, int cz, int half) {
        public boolean contains(double x, double y, double z) {
            return x >= cx - half && x < cx + half + 1 && y >= cy - half && y < cy + half + 1 && z >= cz - half && z < cz + half + 1;
        }
        double minX() { return cx - half; }
        double maxX() { return cx + half + 1; }
        double minZ() { return cz - half; }
        double maxZ() { return cz + half + 1; }
    }

    private WardRules() {}

    public static boolean blocks(boolean enabled, boolean standing, boolean vampiric, boolean wolfLike, boolean blocksWolves,
                                 boolean creativeOrSpectator, boolean owner, boolean invited) {
        if (!enabled || !standing || creativeOrSpectator || owner || invited) {
            return false;
        }
        return vampiric || blocksWolves && wolfLike;
    }

    public static Dir pushDirection(Box b, double x, double z) {
        double w = x - b.minX(), e = b.maxX() - x, n = z - b.minZ(), s = b.maxZ() - z;
        double min = Math.min(Math.min(w, e), Math.min(n, s));
        if (min == w) return new Dir(-1, 0);
        if (min == e) return new Dir(1, 0);
        if (min == n) return new Dir(0, -1);
        return new Dir(0, 1);
    }

    public static Point exitPoint(Box b, double x, double z) {
        Dir d = pushDirection(b, x, z);
        double margin = 1.0;
        if (d.x() < 0) return new Point(b.minX() - margin, z);
        if (d.x() > 0) return new Point(b.maxX() + margin, z);
        if (d.z() < 0) return new Point(x, b.minZ() - margin);
        return new Point(x, b.maxZ() + margin);
    }

    public static InviteResult invite(java.util.Set<java.util.UUID> list, java.util.UUID id, int max) {
        if (list.contains(id)) return InviteResult.ALREADY;
        if (list.size() >= max) return InviteResult.FULL;
        list.add(id);
        return InviteResult.ADDED;
    }

    public static int clampRadius(int r) {
        return Math.max(1, Math.min(MAX_RADIUS, r));
    }
}
```

```java
public final class WardSpeech {
    private WardSpeech() {}

    /** The whole message, trimmed, lower-cased, without trailing . , ! ? and spaces, is exactly {@code phrase}. */
    public static boolean matches(String raw, String phrase) {
        String t = raw.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[.,!?\\s]+$", "");
        return !t.isEmpty() && t.equals(phrase);
    }
}
```

- [ ] **Step 4: Run green**, then **mutation checks** (each: replace, run the two tests, expect a failure, restore): `owner || invited` -> `owner`; `x < cx + half + 1` -> `x <= cx + half + 1`; `min == w` order swapped with `min == e`; `list.size() >= max` -> `list.size() > max`; `replaceAll("[.,!?\\s]+$", "")` -> `""`; `clampRadius` upper bound 24 -> 25; `vampiric || blocksWolves && wolfLike` -> `vampiric || wolfLike`.
- [ ] **Step 5: Commit** `feat(ward): pure ward rules (R302, R305)`.

## Task 2: The ward table (pure) and the saved index

**Files:** create `core/ward/WardTable.java` (pure: add, drop, invite, uninvite, fall by owner, count by owner), `parts/artifacts/ward/WardIndex.java` (SavedData named `supernaturalbloodlines_wards`); tests `WardTableTest` and a GameTest `wardIndexSurvivesASaveAndLoad`.

- [ ] **Step 1: Failing tests** for the pure table (JUnit): adding a ward returns an id (`[0-9a-f]{8}`); `countByOwner` caps at `max`; `invite` and `uninvite` go through `WardRules.invite`; `fallAllOf(owner)` marks only that owner's wards, is idempotent, and `standing(id)` becomes false; `drop(id, requester)` works only for the owner and returns false for anyone else (the generic reply, R325); an owner's ward ids never appear in another owner's `view`.
- [ ] **Step 2: Implement `WardTable`** as an in-memory model holding `Map<String, Ward>` with `Ward(id, kind, dimension, cx, cy, cz, half, owner, invited, fallen)`; no Minecraft types (the dimension is a String).
- [ ] **Step 3: `WardIndex extends SavedData`** wraps a `WardTable`; NBT: one list tag `wards`, each a compound (`id`, `kind`, `dim`, `x`, `y`, `z`, `half`, `owner`, `invited` list of UUID, `fallen`). `WardIndex.get(ServerLevel)` on the overworld's data storage. The `UUID` list codec is the one `PackIndex` uses (copy its `writeIds/readIds` pattern; do not edit `PackIndex`).
- [ ] **Step 4: GameTest** `wardIndexSurvivesASaveAndLoad`: build an index with two wards, `save(new CompoundTag(), registries)`, `load`, compare: ids, owners, invited sets, fallen flag. `newIndexHasNothing`: a fresh index has no wards (the "old world" case).
- [ ] **Step 5: Mutants:** drop `invited` from `save`; drop `fallen` from `load`; `countByOwner` counting fallen wards as standing (a fallen ward should not count against the cap: decide in the test, then mutate it).
- [ ] **Step 6: Commit** `feat(ward): the ward table and its saved index (R300)`.

## Task 3: Config and Admin rows for the ward

**Files:** `parts/artifacts/ArtifactsConfig.java` (13 keys, spec section 9), `shared/admin/AdminEntries.java` (13 `artifacts(...)` lines), tests `AdminEntriesTest` (count), `AdminCoverageTest` (already walks specs).

- [ ] **Step 1: Failing test:** `AdminEntriesTest` expects `main's size + 13`; run it red.
- [ ] **Step 2:** add the keys with comments that say what a server owner gets; add test overrides (`overrideWardRadius(Integer)` etc.) in the same style as `SpeciesConfig.overrideAbsentLeaderDays`.
- [ ] **Step 3:** green; `AdminSnapshotPayload.MAX_ROWS` still holds the new count (the existing assertion says so); if not, raise it in Task 9 with the protocol bump.
- [ ] **Step 4: Mutants:** a key missing from `AdminEntries` (killed by `AdminCoverageTest`); a default flipped (killed by a config-default test you add: `wardDefaults()` asserting each default from the spec table).
- [ ] **Step 5: Commit** `feat(ward): ward settings and Admin rows (R328)`.

## Task 4: Extract Ashwarden's push (refactor, behaviour unchanged)

**Files:** `parts/artifacts/MountainAshRules.java`; existing tests (`MountainAshGameTests` and the Ashwarden tests) are the safety net.

- [ ] **Step 1: Run the existing mountain ash GameTests green first** (record the count).
- [ ] **Step 2:** extract `static void pushBack(LivingEntity entity, Vec3 awayFrom)` that does exactly what lines 66 to 69 do (the horizontal vector from `awayFrom`, normalised, `ASHWARDEN_PUSH` 0.6 and 0.2 up, `hurtMarked`), and call it from `ashwarden`. The weakness effect stays in `ashwarden`.
- [ ] **Step 3:** make it package-visible plus `@VisibleForTesting public static` wrapper `MountainAsh.pushBack`, because the ward is in a sibling package.
- [ ] **Step 4:** the same GameTests green and the same count. A new tiny GameTest `pushBackIsTheOldImpulse` asserts the delta movement for a known entity and position equals the old formula (0.6 horizontal, 0.2 vertical). **Mutant:** change 0.6 to 0.5: killed by that test.
- [ ] **Step 5: Commit** `refactor(mountain-ash): one push-back for the ash line and the ward`.

## Task 5: Enforcement (the push, the timeout, the door)

**Files:** `parts/artifacts/ward/WardEnforcer.java`, `parts/artifacts/ward/WardPredicates.java` (gathers the booleans for `WardRules.blocks`); GameTests `WardGameTests`.

- [ ] **Step 1: Failing GameTests** (fake players; `Supernatural.isVampiric` is decided by the faction and species the test sets, as `MountainAshGameTests` does):
  - `anUninvitedVampireIsPushedOutAndAHumanIsNot`: ward AREA radius 3; a vampire inside is moved out (position outside the box after the check and delta movement away); a human is untouched; the pushed vampire got exactly one line `ward.cannot_enter`.
  - `anInvitedVampireAndTheOwnerAreLeftAlone`.
  - `invitationIsPerWard`: invited to A, standing in B (a different owner's): pushed by B only.
  - `aBoxedInVampireIsTeleportedOutAfterTheTimeout`: roof and walls so the push cannot clear; after `ward_expel_seconds` the entity stands outside (`WardRules.exitPoint` then the nearest standable block).
  - `aDoorWardCancelsTheDoorForAnUninvitedVampire`: `RightClickBlock` on the door cell is cancelled for them and not for a human; the same line is sent.
  - `aVampireMobIsBlockedWhenAffectsMobsIsOn`, and not when it is off.
  - `werewolvesAreNotBlockedByDefault`.
  - `aFallenWardBlocksNobody`, `ward_enabled = false blocks nobody`.
  - `creativeAndSpectatorAreNeverBlocked`.
  - `theCheckOnlyRunsInLoadedChunks`: a ward in an unloaded chunk causes no chunk load (assert `level.hasChunk` is unchanged).
- [ ] **Step 2: Implement.** `WardEnforcer.register()` adds an `EntityTickEvent.Post` listener for `LivingEntity` (cadence `tickCount % 5 == 0`, as Ashwarden); for each loaded ward of the entity's level whose box contains the entity, ask `WardRules.blocks`; if so `MountainAsh.pushBack(entity, centre)` toward `WardRules.pushDirection`, count ticks inside in an in-memory map (cleared on leaving, logout and death), and after `ward_expel_seconds * 20` ticks teleport to `exitPoint` (find a standable `y`: scan down then up 3 blocks; if none, leave them and log once at DEBUG). The door form also registers a `PlayerInteractEvent.RightClickBlock` listener. Messages use `displayClientMessage(..., true)`.
- [ ] **Step 3: Green; mutants:** drop the `owner` exception (killed by `anInvitedVampireAndTheOwnerAreLeftAlone`); remove the timeout branch (killed by the boxed-in test); remove the door listener (killed); `tickCount % 5` -> `% 1` (a cost test: assert the per-tick query count stays under a bound with 3 wards, using a counter in `WardEnforcer`).
- [ ] **Step 4: Commit** `feat(ward): enforce the ward (R302, R303)`.

## Task 6: The ward rite (owner, consent, claims, one refusal)

**Files:** `parts/artifacts/ward/WardRiteFactory.java` (`RiteFactory`, id `ward`, MapCodec with `mode` `area|door`); register in the artifacts part next to `BindItemRiteFactory`; data `rite_of_the_ward.json` (medium circle, `enchanted:ritual_chalk`, power 5,000; items: 4 `mountain_ash_dust`, 1 `minecraft:iron_ingot`, 1 `enchanted:taglock`? no: **no taglock** (the owner is the one on the centre); one `supernaturalbloodlines:vampire_compass`-free list: use `minecraft:amethyst_shard` x2 for flavour), `rite_of_the_door_ward.json` (small circle, power 2,500, 2 mountain ash dust, 1 iron nugget x4); both conditioned on `part_enabled artifacts`; `RiteUnlockTable` marks both `RiteKind.WITCH`.

- [ ] **Step 1: Failing GameTests:**
  - `aWitchWardsHerOwnHomeWithNobodyOnTheCentre`: the caster is the owner; the index has one standing ward with her on the list.
  - `aSneakingPlayerOnTheCentreOwnsTheWard`: the sneaking player is the owner; the caster is on the list.
  - `aStandingPlayerOnTheCentreCancelsTheWard`: a non-sneaking player there: no ward, the ingredients are back, the caster is told `ward.cancelled`.
  - `onlyAWitchTypeCanRaiseIt`: a plain player gets the one generic refusal.
  - `wardRiteIsIdenticalForEveryOwnerSpecies`: the same rite with the owner a human, a vampire, an Original and a hybrid: the same outcome, the same message key and the same ingredients spent. **This is the no-oracle test (R325); it must fail if any branch looks at the owner's species.**
  - `theOwnerCapIsTheGenericRefusal`: the 4th ward for `ward_max_per_owner` 3 gives the same message and cost as any other refusal.
  - `aDoorWardMarksTheNearestDoorWithinFour`: the door cell is the ward's centre, half-size 1; none within 4 is the generic refusal.
  - `claimedGroundIsRefusedWithTheGenericMessage` (a fake `ClaimGuard` returning "somebody else's"), `unclaimedAndOwnClaimAndPartyClaimAreAllowed`.
- [ ] **Step 2: Implement.** `onStart`: caster must be a witch type (`SeveranceRiteFactory.isWitchType`); find the entity on the centre (the existing helper the Linking rite uses); owner = the sneaking one or the caster; cancel and refund if a standing player is there; ask `ClaimGuard` (Task 8 provides the real one; here a stub interface installed by the test); on any failure call one `refuse(caster)`: the same message, ingredients spent (the exception is the cancel for a standing player, which refunds, as R147). `onTick`/`onStop` do nothing.
- [ ] **Step 3: Green; mutants:** a species check in `onStart` (killed by the identical-for-every-species test); refund on the generic refusal (killed by the cost assertion); skip the sneak test (killed by the standing-player test); owner = always the caster (killed by the sneaking-owner test).
- [ ] **Step 4: Rite data tests:** `RiteUnlocksDataTest` (every rite file has its advancement pair; add the pair `rites/known/rite_of_the_ward` and the others by the existing generator), `RitesBookTest` (the entry is generated; the lang labels exist).
- [ ] **Step 5: Commit** `feat(ward): the Rite of the Ward and the door ward (R300, R301, R327)`.

## Task 7: Invitations: the command, the spoken phrase, the messages

**Files:** `WardCommand.java` (`/ward invite|uninvite|drop|info`, registered like `PackCommand`), `WardChat.java` (`ServerChatEvent`), rows `W1`-`W4` in `AdminCommands`, lang.

- [ ] **Step 1: Failing tests** (pure + GameTest):
  - pure `WardSpeechTest` already covers the matcher; add `WardTargetingTest` for `WardChat.pick(...)` as a pure function over `(looked-at id, distance, LOS)`, `(candidates with distances)`: looked-at within 8 with line of sight wins; else nearest within 5; none when nobody; never the owner.
  - GameTests: `ownerSaysComeInAndTheNearestIsInvited` (the message is **not** cancelled, the invitee is told, the owner told), `saysItNearNobodyAndNothingHappens` (no message at all), `aNonOwnerSayingItDoesNothing`, `ownerOutsideEightBlocksOfTheBoxDoesNothing`, `spokenInvitesOffDoesNothing`, `uninviteByCommandAndByTheHubRowRemoveTheName`, `inviteCommandRespectsTheCap` (the generic "list is full"), `dropByAnotherPlayerIsRefusedWithTheGenericReply`, `commandsOnlyWorkOnYourOwnWards`.
  - `CommandCoverageGameTests` green (rows `W1`-`W4` exist; `info` is `Ui.PAGE`).
- [ ] **Step 2: Implement.** The chat handler reads `getRawText()`, matches `WardSpeech.matches(raw, "come in")` and `"get out"`, then `server.execute(() -> ...)` (U1) to pick the target and call the same service method the command calls, so the two paths cannot differ. Messages: `ward.invited_you` (to the invitee, with the owner's name), `ward.you_invited` (to the owner, with the invitee's name), `ward.not_welcome` (to a withdrawn one), `ward.list_full`, `ward.dropped`. None of them mentions species.
- [ ] **Step 3: Green; mutants:** cancel the chat event (killed by the not-cancelled test); skip the owner-distance check; pick the farthest instead of the nearest; the command accepting a ward id of someone else (killed by `commandsOnlyWorkOnYourOwnWards`).
- [ ] **Step 4: Commit** `feat(ward): invitations by chat and command (R305, R306, R309)`.

## Task 8: Death, revoke and the claim check

**Files:** `WardEnforcer` (revoke expulsion), `WardLifecycle.java` (owner death, login), `ClaimGuard.java` (reflection like `OpacBackend`: `Class.forName` of the names read with `javap` from 0.33.0; a `CallGuard`-style fail counter that turns it off after 5 failures with one warning).

- [ ] **Step 1: Failing GameTests:**
  - `ownerDeathFallsEveryWardAtOnce`; `ownerDeathFallsWardsInUnloadedChunks` (a ward whose chunk is not loaded: after the owner's `LivingDeathEvent` the index says fallen without loading it); `fallsOnDeathOffKeepsTheWard`; `aFallenWardNeverComesBack` (the owner respawns: still fallen).
  - `uninvitingAVampireInsideThrowsThemOut` (with `ward_expel_on_revoke` true, the next enforcement tick pushes them), `…false` leaves them alone until they leave and re-enter; `theThrownOutOneIsToldOnce`.
  - `claimGuardFailsOpen`: a guard whose reflection throws five times is off, the rite is not refused for it, one warning is logged.
  - `noOpacMeansNoCheck`; `opacPartyMemberMayWardOnTheOwnersClaim` (with the fake guard).
- [ ] **Step 2: Implement; run the real `ClaimGuard` against the pack's OPAC jar in a manual check** (not CI: OPAC is optional): note the result in the PR.
- [ ] **Step 3: Mutants:** the death listener ignoring offline-chunk wards (killed by the unloaded test); `ward_falls_on_owner_death` ignored; the revoke push removed; the guard throwing on the first failure instead of the fifth.
- [ ] **Step 4: Commit** `feat(ward): the dead-owner rule, revoke and the claim check (R304, R307, R308)`.

## Task 9: The hub: payload, protocol, the Path card

**Files:** `WardViewPayload`, `WardView` (server builder), `LadderNet` (register + `PROTOCOL`), `WardCard` (client), the Path page, lang, `SnapshotIdsWireTest`, `HubLangCoverageTest`.

- [ ] **Step 1: Failing tests:** `WardViewPayloadWireTest` (round trip; empty view; the maximum ward count and name lengths; a hostile client cannot send it); `SnapshotIdsWireTest` golden bytes for the payload and the protocol together (it fails first because the number has not moved); `HubLangCoverageTest` for the new keys; a GameTest `aViewHoldsOnlyTheViewersOwnWardsAndTheirWelcomes` (another player's ward, invite list and coordinates are never in the viewer's payload: assert on the bytes).
- [ ] **Step 2: Implement.** `WardView.of(ServerPlayer)`: the viewer's own wards (id, kind, dimension, **coordinates** for the owner, invited names with online flags, fallen flag) and the **names** of owners whose standing ward invited the viewer. Payload size bounds as in `GroupWire` (cap wards 8, invited 16, names 16 chars).
- [ ] **Step 3: The Path page card** draws: kind, place, the invite list with a Withdraw button each (`/ward uninvite`), an Invite button per online player from the snapshot's people list (`/ward invite`), and **Drop** with the confirm dialog (`ProfileActions`' danger pattern); the "Homes that welcome you" line. A player with no ward and no welcome sees nothing new.
- [ ] **Step 4: Contrast and layout:** each text colour on every theme at 4.5:1 (extend `ThemeContrastTest` with the card's colours); the card at the smallest GUI scale fits (the page's layout test).
- [ ] **Step 5: Hidden shot under the owner-game rule:** `runSafeShots` through the slot, `focus-watch.ps1 -JavaOnly -MaxSec 900`, the demo property `supernaturalbloodlines.devshots.demo.ward=true` (add it to `DevDemo`), three themes, scale 3; read each image before the PR. **Mutants:** put another player's ward in the view (killed by the bytes test); skip the protocol bump (killed by the golden test).
- [ ] **Step 6: Commit** `feat(ward): the Path card and the ward view payload (R310, protocol N+1)`.

## Task 10: 21a docs and the full run

- [ ] **Step 1:** copy R300-R314 and R325-R329 (the ones this sub-plan builds) into `SPECIES-RULES.md`; `docs/parts/artifacts.md` (ticked, with the file list), `wiki/Witches-And-Siphoners.md` (the rites), `wiki/Configuration.md` (13 keys), `wiki/For-Server-Owners.md`, `wiki/Rules-Reference.md`, `wiki/Troubleshooting.md` ("a vampire cannot enter my base"), `CHANGELOG.md` with a **Rolling back** paragraph (the index is a new SavedData file and can stay; a server that downgrades loses only the wards).
- [ ] **Step 2: Run `test` and all 12 GameTest runs** (default; `-PnoWerewolves -PnoCurios`; `-PnoLinkedMods`; `-PpartsOff=` species, artifacts, speciesTensura, supernatural, supernaturalTensura, hud, celestials, bosses, `species,artifacts`) through the slot with `--max-workers=2` and a hard timeout; every result in a log under `plan21\`.
- [ ] **Step 3:** push, PR, wait for green CI, do not merge. The PR body lists the decisions (spec section 11) as the owner approved them.

---

# 21c: the two curses

## Task 11: The curse rules and classes

**Files:** `core/curse/SusceptibilityRules.java`, `core/curse/InsanityRules.java`, `parts/vampirismenchanted/curse/{InsanityCurse,SusceptibilityCurse,PlagueCurses}.java`; tests `SusceptibilityRulesTest`, `InsanityRulesTest`, `PlagueCurseGameTests`.

- [ ] **Step 1: Failing pure tests.**

```java
class SusceptibilityRulesTest {
    @Test
    void theMultiplierRisesPerLevelAndIsCapped() {
        assertEquals(1.25, SusceptibilityRules.multiplier(0, 25, 200), 1e-9);
        assertEquals(1.50, SusceptibilityRules.multiplier(1, 25, 200), 1e-9);
        assertEquals(1.75, SusceptibilityRules.multiplier(2, 25, 200), 1e-9);
        assertEquals(2.00, SusceptibilityRules.multiplier(2, 100, 200), 1e-9, "capped at x2");
        assertEquals(1.0, SusceptibilityRules.multiplier(0, 0, 200), 1e-9);
    }

    @Test
    void onlyARealBlowIsRaised() {
        assertEquals(12.5f, SusceptibilityRules.apply(10f, 1.25, false, false), 1e-6f);
        assertEquals(10f, SusceptibilityRules.apply(10f, 1.25, true, false), "bypasses invulnerability: /kill, void");
        assertEquals(10f, SusceptibilityRules.apply(10f, 1.25, false, true), "a probe");
        assertEquals(0f, SusceptibilityRules.apply(0f, 1.25, false, false));
        assertEquals(-1f, SusceptibilityRules.apply(-1f, 1.25, false, false));
    }
}

class InsanityRulesTest {
    @Test
    void theDelayShortensWithStrengthAndStaysInsideTwoThirdsToFourThirds() {
        java.util.Random r = new java.util.Random(7);
        for (int s = 0; s <= 2; s++) {
            long base = 30L * 20 / (s + 1);
            for (int i = 0; i < 200; i++) {
                long d = InsanityRules.nextDelay(30, s, r::nextDouble);
                assertTrue(d >= base * 2 / 3 && d <= base * 4 / 3, s + ": " + d);
            }
        }
    }

    @Test
    void aZeroIntervalIsAtLeastOneSecond() {
        assertTrue(InsanityRules.nextDelay(0, 0, () -> 0.5) >= 20);
    }
}
```

- [ ] **Step 2: Run red; implement the two rule classes** (`multiplier = Math.min(1 + extra * (strength + 1) / 100.0, maxPct / 100.0)`; `apply(amount, m, bypassesInvulnerability, probe)` returns `amount` unless `amount > 0 && !bypass && !probe`; `nextDelay(seconds, strength, random)` = `max(20, base * (2/3 + random * 2/3))` with `base = max(1, seconds) * 20 / (strength + 1)`).
- [ ] **Step 3: Failing GameTests** (`PlagueCurseGameTests`, the Tensura curses' tests are the model):
  - `susceptibilityRaisesTheBlowByTheCurseLevel` (post a `LivingIncomingDamageEvent` for 10 damage on a cursed fake player at levels 0, 1, 2: 12.5, 15, 17.5; an uncursed player 10);
  - `susceptibilityNeverRaisesAProbe` (inside `DamageProbe`: unchanged);
  - `susceptibilityIgnoresAKillAndTheVoid` (damage type tag `bypasses_invulnerability`);
  - `aCancelledBlowStaysCancelled`;
  - `insanitySendsOneHallucinationPerDelay` (through the spy hook, U5; fake clock: advance `delay - 1` ticks: none, `delay`: one; the kind is one of the three);
  - `insanityNeverSendsToAPlayerWithoutAChannelButStillCounts`;
  - `bothCursesEndAtDeathAndDoNotStack` (apply each twice: one instance; a death removes both; a respawned player has none);
  - `removeCurseRiteLiftsBoth` (the `enchanted:remove_curse` factory path as the Tensura curses' test does);
  - `insanityTimerRestartsOnLogin`.
- [ ] **Step 4: Implement.** `InsanityCurse` (counter in memory like `MagiculeDrainCurse`; `tick` reads `InsanityRules.nextDelay`; sends `HallucinationPayload.send(target, Kind.random(target.getRandom()))`; if it returns false do the existing server-side stand-in the Hunter's Curse uses). `SusceptibilityCurse` (empty `tick`; the multiplier lives in a `LivingIncomingDamageEvent` listener at `EventPriority.LOW`, `receiveCanceled = false`, that finds the victim's non-removed `SusceptibilityCurse` instance through `CurseManager.get().getCurses(...)` and reads its strength from the instance). `PlagueCurses.register()` registers both types with `CurseManager`, adds the death listener (LOWEST, `removeCurse`) and the damage listener, called from `VampirismEnchantedPart` next to the existing registrations.
- [ ] **Step 5: Mutants:** the `bypass` condition removed; the probe condition removed; the cap removed; `Kind.random` replaced by a fixed kind (killed by a test that sees all three over many runs); the death listener removed; strength read as 0 always; the listener at HIGH priority with `receiveCanceled` true (killed by the cancelled-blow test).
- [ ] **Step 6: Commit** `feat(curses): Curse of Insanity and Curse of Susceptibility (R320 to R324)`.

## Task 12: The curse rites, config, Admin rows, docs and the full run

- [ ] **Step 1: Failing data tests:** `RiteUnlocksDataTest` and `RiteUnlockTableTest` expect `curse_insanity` and `curse_susceptibility` in `CURSES` (so `taughtBy(CURSES)` is the four), `RitesBookTest` the entries and unique labels `(I)` to `(IV)` and the Tensura-only note on the two old ones only; `AdminEntriesTest` +5; `AdminCoverageTest` knows the new config file if U4 says one is needed.
- [ ] **Step 2:** the two rite JSONs (spec 5.3; the ingredient ids from Task 0), config keys (spec section 9), Admin rows, lang (`supernaturalbloodlines.curse.insanity.*`, `...susceptibility.*` with the one-line tooltip "takes +N% damage").
- [ ] **Step 3: GameTests** `theRiteAppliesTheCurseThroughATaglock` (both), `aRiteWithTheSettingOffDoesNothingAndGivesBack`? Decide with the owner: **the setting off removes the rite file's effect by the existing `part_enabled`-style condition on a config value**; if Enchanted's data conditions cannot read a config, the factory is ours (a thin wrapper that checks the setting then delegates to `ApplyCurseFactory`). The task picks whichever Task 0 shows works and writes the test for it.
- [ ] **Step 4:** docs (SPECIES-RULES R320-R324, `docs/parts/vampirism_enchanted.md`, wiki Witches-And-Siphoners, Configuration, Rules-Reference, CHANGELOG with Rolling back: remove the two curse types and Enchanted drops unknown curses by id on load: **verify by javap or a test** that a player data with an unknown curse type does not crash the load; if it does, the plan adds a tolerant fallback and the CHANGELOG says so).
- [ ] **Step 5: Run `test` and all 12 GameTest runs**, push, PR, wait for green CI, do not merge.

---

# 21b: the locator

## Task 13: The pure locator rules

**Files:** `core/locator/LocatorRules.java`; test `LocatorRulesTest`.

- [ ] **Step 1: Failing tests.**

```java
class LocatorRulesTest {
    @Test
    void theBlurredPointIsTheCentreOfTheCellWithAStableOffset() {
        LocatorRules.Pt a = LocatorRules.blur(1234.0, -777.0, 99L, 32);
        LocatorRules.Pt again = LocatorRules.blur(1234.0, -777.0, 99L, 32);
        assertEquals(a, again, "the same seed and the same cell give the same point");
        assertEquals(Math.floorDiv(1234, 32), Math.floorDiv((int) a.x(), 32), "inside the target's cell");
        assertEquals(Math.floorDiv(-777, 32), Math.floorDiv((int) a.z(), 32));
        assertNotEquals(1234.0, a.x(), "never the exact position");
        assertTrue(Math.abs(a.x() - 1234.0) <= 32 && Math.abs(a.z() + 777.0) <= 32);
    }

    @Test
    void stepsInsideOneCellDoNotMoveTheNeedle() {
        assertEquals(LocatorRules.blur(1230.0, -770.0, 5L, 32), LocatorRules.blur(1239.0, -765.0, 5L, 32));
    }

    @Test
    void distanceIsWordsNotNumbersUnderAHundred() {
        assertEquals(LocatorRules.Distance.VERY_NEAR, LocatorRules.distance(49));
        assertEquals(100, LocatorRules.distance(50).blocks());
        assertEquals(300, LocatorRules.distance(312).blocks());
        assertEquals(300, LocatorRules.distance(349).blocks());
        assertEquals(400, LocatorRules.distance(350).blocks());
    }

    @Test
    void everyReasonLooksTheSame() {
        long now = 1000, until = 5000;
        assertTrue(LocatorRules.findable(true, true, true, true, 100 * 100, 2000, false, false, now, until));
        assertFalse(LocatorRules.findable(false, true, true, true, 0, 2000, false, false, now, until), "setting off");
        assertFalse(LocatorRules.findable(true, false, true, true, 0, 2000, false, false, now, until), "offline");
        assertFalse(LocatorRules.findable(true, true, false, true, 0, 2000, false, false, now, until), "dead");
        assertFalse(LocatorRules.findable(true, true, true, false, 0, 2000, false, false, now, until), "another dimension");
        assertFalse(LocatorRules.findable(true, true, true, true, 2001.0 * 2001, 2000, false, false, now, until), "out of range");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, true, false, now, until), "cloaked");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, false, true, now, until), "creative or spectator");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, false, false, 5000, until), "spent");
        assertTrue(LocatorRules.findable(true, true, true, true, 1e12, 0, false, false, now, until), "range 0 = the whole dimension");
    }
}
```

- [ ] **Step 2: Implement.** `findable(enabled, online, alive, sameDimension, distanceSqr, range, cloaked, spectator, now, until)` returns one boolean (that is the "one state" of R325); `blur(x, z, seed, cell)` = the cell's centre plus a seeded offset in `[-cell/4, cell/4)` on each axis; `distance(blocks)` returns a record `Distance(boolean veryNear, int blocks)` with the constant `VERY_NEAR`, rounding half up to the nearest 100.
- [ ] **Step 3: Mutants:** the `cloaked` term removed; `until` compared with `>` instead of `>=`; the blur returning the exact position; the rounding to 10; `range == 0` meaning "nothing". Each killed.
- [ ] **Step 4: Commit** `feat(locator): pure locator rules (R316, R325)`.

## Task 14: The compass mode, the components and the bridge

**Files:** `CompassMode` (cycle), `VampireCompassItem` (`update` for `LOCATOR`), `ArtifactItems` (data components `locator_target` UUID and `locator_until` long, synced as nothing for the UUID: a client must not learn who is tracked, **sync only a boolean "attuned"** through a third component), `shared/species/LocatorTargets.java` (interface installed by the species part), `CompassPrivacy` (tracker cleared as today; the components stay), tests `CompassModeTest` (cycle), `LocatorCompassGameTests`.

- [ ] **Step 1: Failing tests:** `CompassModeTest.toggledCyclesThroughThree`; GameTests:
  - `theNeedleSpinsTheSameForEveryReason`: for offline, dead, other dimension, out of range, cloaked, creative, spent: the compass's `LodestoneTracker` is **equal** in all seven cases (and equal to the empty tracker), which is the R325 test;
  - `aFindableTargetGivesABlurredPointNotTheExactOne`;
  - `aChestedCompassKeepsTheAttunementOnly` (close a container holding it: the tracker target is gone, `locator_target` and `locator_until` remain);
  - `aServerWithoutTheSpeciesPartSkipsTheThirdMode` (the cycle goes `VAMPIRES -> DOPPELGANGERS -> VAMPIRES`);
  - `theTooltipNeverNamesTheTarget` (the tooltip lines contain "?" and the distance words, never the name or the UUID, and a **client-side** copy of the stack has no UUID: assert on the synced component set).
- [ ] **Step 2: Implement.** `CompassMode.toggled()` becomes a cycle that skips `LOCATOR` when `LocatorTargets.installed()` is false; `VampireCompassItem.update` for `LOCATOR` reads the two components, asks `LocatorTargets.find(server, uuid)` for `(online, alive, dimension, position, cloaked, spectator)` and runs `LocatorRules.findable` and `blur`; **the only outputs are a `GlobalPos` or an empty tracker**. Tooltip uses `LocatorRules.distance`.
- [ ] **Step 3: Mutants:** the tracker set to the exact position (killed by the blur test); the UUID synced to the client (killed by the tooltip/component test); `CompassPrivacy` clearing the components (killed by the chest test); a distinct tracker for the cloaked case (killed by the same-for-every-reason test).
- [ ] **Step 4: Commit** `feat(locator): the compass's third mode (R316, R318)`.

## Task 15: The locator rite

**Files:** `parts/species/rite/LocatorRiteFactory.java` (`RiteFactory`, id `locator`, codec with `source` `taglock|blood`), `parts/species/locator/LocatorAttunement.java`, the species part's `LocatorTargets` installation, rite JSONs `rite_of_the_locator.json` and `rite_of_the_locator_<item>.json` per Task 0, lang, `RiteUnlockTable` (`RiteKind.WITCH`).

- [ ] **Step 1: Failing GameTests:**
  - `aTaglockAttunesTheCompassToItsOwner`: the returned compass has `locator_target` = the owner and `locator_until` = now + `locator_minutes * 1200`; the vampire compass is given back, not consumed;
  - `aSealedBloodBottleAttunesItToTheSource` (each stamped bottle item from Task 0);
  - `locatorRefusalsAreOneMessageAtOneCost`: a villager's taglock, an NPC Original's bottle, an unstamped bottle, an offline taglock owner, a plain (non-witch) caster, `locator_enabled = false`: **one** message key and the same spent ingredients every time (this is the R325 test);
  - `theTargetIsToldOnceWithNoNameAndAgainAtTheEnd` (`locator_tell_target` on; off: nothing; the two messages carry no name, place or species);
  - `anOriginalsBloodNeverNamesThem` (the compass tooltip, the rite messages and the book page show "?"; the secret-kind label rule of R210);
  - `theAttunementEndsAtTheTime` (the needle spins after `locator_until`, the target is told "The search for you has ended." only when the setting is on);
  - `aSecondAttunementReplacesTheFirst`.
- [ ] **Step 2: Implement.** `LocatorRiteFactory.create` returns a `Rite` whose `onStart` reads the consumed items (`TaglockOwner.uuid(params.consumedItems)` or `BloodStamp.read`), applies the refusal table of spec 4.1 with **one** `refuse()`, builds the output compass with the components, drops it at the altar, and tells the target. `LocatorTargets` (species side) answers `find` from `server.getPlayerList()`, `CloakAccess.cloaked`, `isCreative/isSpectator`.
- [ ] **Step 3: Green; mutants:** a species check in the refusal path (killed by the one-message test); the target told with a name (killed by the no-name test); the needle given the target's real name in the tooltip; the attunement never expiring; the offline-owner case giving a different message.
- [ ] **Step 4: Rite data tests** (`RiteUnlocksDataTest`, `RitesBookTest`; unique labels for the locator pages), **commit** `feat(locator): the Rite of the Locator (R315, R317, R318, R319)`.

## Task 16: 21b hub, config, docs and the full run

- [ ] **Step 1:** the five config keys and `AdminEntries` rows (+5), `AdminEntriesTest`; the compass's third-mode hint line in `supernaturalbloodlines.compass.hint`; the Path page shows nothing new for the locator (the compass is the surface), but the **Guide** gets a short chapter for witches ("The Locator", gated on the species part), and `HubLangCoverageTest` stays green.
- [ ] **Step 2:** docs: R315-R319 into `SPECIES-RULES.md`, `docs/parts/species.md` and `artifacts.md`, `wiki/Items-And-Blocks.md` (the compass's third mode), `wiki/Witches-And-Siphoners.md`, Configuration, Rules-Reference, CHANGELOG with Rolling back (the two components are ignored by an older jar; an attuned compass becomes an ordinary compass).
- [ ] **Step 3: Run `test` and all 12 GameTest runs**; hidden shot of the compass tooltip in the three themes under the owner-game rule (`-MaxSec 900`); push, PR, wait for green CI, do not merge.

---

## Task 17: Closing the plan

- [ ] **Step 1:** after the three PRs merge, one docs-only commit sets the plan's status to done, copies the owner's answers to the six questions into the spec (section 11), and lists any R-id that moved.
- [ ] **Step 2:** a last grep across the repo for the strings `Bewitchment`, `moriyashiine` and any Bewitchment prose phrase: the only hits allowed are in `docs/superpowers/research/` and this plan and spec (the clean-room note).
