# Rite of Severance Clears the Tensura Name Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When the Rite of Severance cuts a sired player's bond and the severed sire is the player's Tensura owner, also clear the player's Tensura "named" status, keeping the EP and stats the naming gave.

**Architecture:** No mixin. Tensura's `IExistence` (`TensuraStorages.getExistenceFrom(entity)`) holds the name as a plain nullable `String` with `setName(String)`; `SubordinateLink.onChange` already reacts to the severance and clears the owner. The change adds the name clear to that one place and fixes an ordering problem there (below). One new switch in the species_tensura config.

**Tech Stack:** Java 21, NeoForge 1.21.1, Tensura 2.0.1.5 (`io.github.manasmods.tensura.storage.ep.IExistence`), GameTests.

**Spec:** `docs/superpowers/specs/2026-11-16-bloodlines-bosses-design.md` section 1 (copy of `C:\Users\caiom\.opencode\plan\2026-11-16-bloodlines-bosses-design.md`).

## Research result (done, `javap -p -c` on `curse.maven:tensura-reincarnated-643695:9476869`, Tensura 2.0.1.5)

- **No mixin is needed.** `IExistence` has `String getName()` and `void setName(String)`; `ExistenceStorage.setName` is a bare `this.name = name` (no null check, no event). `ExistenceStorage.save` writes `name` only when non-null and **removes the key when it is null**, `load` reads it only when present, so `setName(null)` is saved, loaded and synced correctly after `markDirty()`. The existing tests already use `existence(p).setName(null)` in `reset`.
- "Named" in Tensura is exactly `getName() != null` (`RequestNamingMenuPacket.canName` refuses with `already_named` on that; `SireNaming.tryName` reads the same). Clearing the field makes `canName` pass again for any namer who passes the owner check (`getPermanentOwner()` must be null or the namer). The owner is already cleared by `SubordinateLink`.
- EP and stats from a naming live elsewhere (`namingReward` raises the base `MAX_MAGICULE`/`MAX_AURA` attribute values), so clearing the name keeps them. Nothing in Tensura reverts them when the name goes.
- **Mixin budget unchanged:** species_tensura stays at 2 of 3 mixins (`docs/mixins.md`).

## The ordering problem this plan must fix (found while reading `SubordinateLink`)

`onChange` runs `unlink(p)` first, which clears the owner when the link is ours (the normal case: the auto-naming path stores `linked_sire`), and only then `freeFrom(p, severedSire)`, which tests `severedSire.equals(owner)`. For a normal sired player the owner is already null by then, so a name clear placed in `freeFrom` would never run. The test `severanceFreesTheOwnerAndTheNameStays` covers exactly that normal path. The plan therefore reads "the severed sire is the owner" **before** `unlink`.

## Global Constraints

- Parts never import each other except the documented `supernatural_tensura` exception (species_tensura already imports `parts.species.sireline.*`; that stays as is).
- Every feature can be switched off per part with a config switch; the new switch is in `supernaturalbloodlines-species_tensura.toml` and defaults to `true`.
- The name clears only when the severed sire is the player's Tensura permanent owner (spec section 1). An owner who is not the severed sire is left alone, as today, and so is the name.
- EP and stats from the naming are kept.
- Tests live in the part and are registered by the part (`SirelineTensuraGameTests` already is). The 10-run matrix script is `C:\Users\caiom\AppData\Local\Temp\opencode\matrix13d.ps1` (`--no-daemon`). It runs the checkout hard-coded in `gm.ps1` (`D:\Development\OFA-TECH\SupernaturalBloodlines-MinecraftMod`), so run it where the branch is checked out, or edit that path in a copy.

## Review Focus

- The severed sire is **not** the owner (someone else owns the player): owner and name stay. Pinned in Task 1 by `severanceKeepsAnUnrelatedOwnerAndItsName`.
- The link is ours (`linked_sire` stored, the normal path) versus an owner Tensura's own naming set (no stored link): both must clear the name. Pinned by `severanceFreesTheOwnerAndClearsTheName` and `severanceClearsTheOwnerANamingSetAndTheName`.
- The old sire must not re-name a severed player at the next login (`SireNaming.retryFor`), and the severance must not fire a second time on an unrelated bloodline change while the bond stays broken (that would wipe a name another namer gave afterwards). Pinned by `severedPlayerKeepsALaterNameFromAnotherNamer`.
- Switch off: `severance_clears_name = false` restores today's behaviour (owner cleared, name stays). Pinned by `severanceNameClearSwitchOffKeepsTheName`.
- Re-siring after a severance names the player again (the sire pays Tensura's cost, the sired gets Tensura's LOW reward again). That is the point of the spec ("someone else can name them later"), and the rite's full-moon night, 6000 power and reagent are what limits farming it. Not changed here; stated in the CHANGELOG and docs so the owner can veto.

---

### Task 1: Clear the name on severance

**Files:**
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/parts/speciestensura/SpeciesTensuraConfig.java` (new key, getter, test override; same shape as `SIRE_NAMING` / `sireNaming()` / `overrideSireNaming`)
- Modify: `src/main/java/net/ofatech/supernaturalbloodlines/parts/speciestensura/SubordinateLink.java` (`onChange`, `freeFrom`, class javadoc)
- Test: `src/main/java/net/ofatech/supernaturalbloodlines/parts/speciestensura/gametest/SirelineTensuraGameTests.java` (two existing tests change, four are new)

**Interfaces:**
- Produces: `SpeciesTensuraConfig.severanceClearsName(): boolean` (key `severance_clears_name`, default `true`) and `SpeciesTensuraConfig.overrideSeveranceClearsName(Boolean value)` (tests; `null` goes back to the config).
- Consumes: `BloodlineAccess.BloodlineChange(before, after)`, `BloodlineState.bondBroken()` and `boundSire()`, `TensuraStorages.getExistenceFrom(LivingEntity): IExistence`.

- [ ] **Step 1: Change the two existing severance tests and add four (they must fail first)**

In `SirelineTensuraGameTests` (helpers `namer`, `hybrid`, `online`, `reset`, `existence`, `said`, `playerNaming`/`restore` already exist):

1. Rename `severanceFreesTheOwnerAndTheNameStays` to `severanceFreesTheOwnerAndClearsTheName`. Keep its flow; replace the name assertion with `helper.assertTrue(existence(sired).getName() == null, "the name survived the severance: " + existence(sired).getName())`. Keep "the sire stays recorded" and the "new sire links again" parts. Update its javadoc ("...and the Tensura name goes with it").
2. Rename `severanceClearsTheOwnerANamingSet` to `severanceClearsTheOwnerANamingSetAndTheName`. Replace `"Ward".equals(existence(sired).getName())` after the severance with `existence(sired).getName() == null`.
3. New `severanceKeepsAnUnrelatedOwnerAndItsName`: `existence(ownedSired).setPermanentOwner(other.getUUID()); existence(ownedSired).setName("Taken");` record the sire, sever (`severedSire(sire.getUUID())`), assert owner is still `other` and name is still `"Taken"`. (Move the "unrelated owner" half out of the old test 2 into this one.)
4. New `severanceNameClearSwitchOffKeepsTheName`: `SpeciesTensuraConfig.overrideSeveranceClearsName(false)` in a try/finally with `null`; sire, sever; assert owner is `null` **and** `own.equals(existence(sired).getName())`.
5. New `severedPlayerKeepsALaterNameFromAnotherNamer`: with `playerNaming(helper, true)` (restore in finally): sire, sever, assert name null. Call `SubordinateLink.onLogin(sired, List.of(sire, sired))` and assert the name is still null (the old sire does not re-name). Then `RequestNamingMenuPacket.name(sired, other, NamingType.LOW, "Second")` with `other` an online namer (use the `namer(helper)` helper), assert name `"Second"` and owner `other`. Then `BloodlineAccess.set(sired, BloodlineAccess.get(sired).toBuilder().thirst(10).build())` and assert name is still `"Second"` and owner still `other`.
6. New `severanceKeepsEpAndStats`: sire, record the sired's `TensuraAttributes.MAX_MAGICULE` base value (read it the way `autoNamingCostsTheSireAndRewardsTheSiredWithoutEvolving` does) after the naming; sever; assert the base value is unchanged and still above its pre-naming value.

- [ ] **Step 2: Run the game tests to verify the new and changed tests fail**

Run: `pwsh -NoProfile -File C:\Users\caiom\AppData\Local\Temp\opencode\gm.ps1 -TimeoutSec 900 runGameTestServer`
Expected: compile error for `overrideSeveranceClearsName` first; after adding the config stubs only, FAILED for tests 1, 2 and 5 (`the name survived the severance`).

- [ ] **Step 3: Add the config switch in `SpeciesTensuraConfig`**

`SEVERANCE_CLEARS_NAME` `BooleanValue` defined next to `SIRE_NAMING` as `"severance_clears_name"`, default `true`, comment: the Rite of Severance also clears the player's Tensura name when the severed sire is their Tensura owner, so someone else can name them later; EP and stats from the naming are kept; off keeps the name. Add `private static volatile Boolean severanceClearsNameOverride`, `public static void overrideSeveranceClearsName(Boolean value)` and `public static boolean severanceClearsName()` mirroring `sireNaming()`.

- [ ] **Step 4: Fix `SubordinateLink.onChange` and `freeFrom`**

Signature changes: `private static void freeFrom(ServerPlayer p, UUID severedSire)` becomes `private static void freeFrom(ServerPlayer p)`.

New `onChange` order: (1) compute `UUID severed = change.after().bondBroken() ? change.before().boundSire() : null;` and `boolean ownedBySevered = severed != null && severed.equals(TensuraStorages.getExistenceFrom(p).getPermanentOwner());` **before anything else**; (2) the existing `unlink(p)` when `boundSire` changed; (3) `if (ownedBySevered) freeFrom(p);` (4) `sync(p)`.

`freeFrom(p)`: `existence.setPermanentOwner(null)` (idempotent when `unlink` already cleared it), then `if (SpeciesTensuraConfig.severanceClearsName()) existence.setName(null)`, then `existence.markDirty()`. Update the javadoc of `freeFrom` and of the class: the name goes with the owner when the severed sire was the owner; an owner that is not the severed sire stays, and so does the name.

- [ ] **Step 5: Run the game tests to verify they pass**

Run: `pwsh -NoProfile -File C:\Users\caiom\AppData\Local\Temp\opencode\gm.ps1 -TimeoutSec 900 runGameTestServer`
Expected: `All <n> required tests passed` and no `failed at` line; the six tests above appear in the run.

- [ ] **Step 6: Mutation check**

Temporarily move step (3) of `onChange` back after `unlink(p)` with the owner re-read inside `freeFrom` (the old order). Run the game tests: `severanceFreesTheOwnerAndClearsTheName` must fail, `severanceClearsTheOwnerANamingSetAndTheName` must pass. Revert.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/net/ofatech/supernaturalbloodlines/parts/speciestensura
git commit -m "feat(species_tensura): the Rite of Severance clears the Tensura name"
```

---

### Task 2: Docs, rules, changelog, matrix

**Files:**
- Modify: `docs/parts/species_tensura.md` (the "Severance." bullet around line 188, the R101b paragraph around line 232, the test list around line 193 with the two renamed tests and four new ones, the checklist: add "Severance clears the Tensura name")
- Modify: `docs/mixins.md` (one sentence under species_tensura: the name clear needs no mixin, `IExistence.setName(null)`; budget stays 2 of 3)
- Modify: `docs/superpowers/specs/SPECIES-RULES.md` (R101b: the severance also clears the sired's Tensura name when the severed sire was their owner; EP and stats stay; config `severance_clears_name`)
- Modify: `wiki/Rite-Of-Severance.md`, `wiki/Tensura-Integration.md` (what the player sees: named status is gone, EP kept, another namer can name them)
- Modify: `CHANGELOG.md` (under Unreleased, "Changed": the sentence from the spec, the config key, and the re-siring note from Review Focus)

**Interfaces:** none.

- [ ] **Step 1: Make the doc edits above**

Each doc states the same three facts: the condition (the severed sire is the Tensura owner), the effect (name cleared, EP and stats kept), the switch (`severance_clears_name`, default on, in `supernaturalbloodlines-species_tensura.toml`).

- [ ] **Step 2: Run the whole matrix**

Run: `pwsh -NoProfile -File C:\Users\caiom\AppData\Local\Temp\opencode\matrix13d.ps1` then read `C:\Users\caiom\AppData\Local\Temp\opencode\matrix-13d.log`.
Expected: 10 lines (`build`, `runGameTestServer`, `-PnoWerewolves -PnoCurios`, `-PnoLinkedMods`, `-PpartsOff=species`, `artifacts`, `speciesTensura`, `supernatural`, `supernaturalTensura`, `hud`), each ending in `BUILD SUCCESSFUL` or `All <n> required tests passed`, none with `failed at`, and the final line `DONE`. The `-PpartsOff=speciesTensura` and `-PnoLinkedMods` runs must not mention the new tests (they are part-registered).

- [ ] **Step 3: Commit**

```bash
git add docs wiki CHANGELOG.md
git commit -m "docs: the Rite of Severance clears the Tensura name (R101b)"
```

---

## Self-review

- **Spec coverage:** section 1, bullet 1 (clear when the severed sire is the Tensura owner, via the existing `SubordinateLink.freeFrom` path) is Task 1 step 4; bullet 2 (EP and stats kept) is `severanceKeepsEpAndStats`; bullet 3 (other owner left alone) is `severanceKeepsAnUnrelatedOwnerAndItsName`. "Research before PR 1" is the section above. No gaps.
- **Types:** `freeFrom(ServerPlayer)`, `severanceClearsName()`, `overrideSeveranceClearsName(Boolean)` are named the same in Tasks 1 and 2.
- **Decisions made without the spec:** the new switch `severance_clears_name` (spec says every feature can be switched off); the clear is also gated on the owner, not on the name's origin (a name with the severed sire as owner can only have come from that sire or from our auto-naming, because `canName` refuses a namer other than the owner).
