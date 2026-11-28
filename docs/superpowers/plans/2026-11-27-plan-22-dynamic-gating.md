# Plan 22 — Dynamic gating / optional mods (design)

**Date:** 2026-11-27
**Status:** Draft design (no code changes in this PR)
**Source audit:** `C:\Users\caiom\AppData\Local\Temp\opencode\optional-audit\REPORT.md` (main @ 4be03c3b)
**Owner rule:** #1236 — every other mod is optional; this mod never forces one.

## Audit summary

The read-only audit ran static scans on 1316 Java files, inspected all 380 data JSONs, and booted a dedicated server under 11 mod combinations (including zero-mod, single-mod, and full). Findings:

- **No crash-risk under-gating.** Every optional-mod reference is behind `isLoaded`, a per-part mixin plugin, or reflection. Zero `NoClassDefFound`/`NoSuchMethod`/mixin errors across all combos.
- **One real "forces a mod" path.** `neoforge.mods.toml` bounds seven optional mods to a minor line (`[x,y)`). FML refuses to start when such a mod is installed but out of range. `LinkedMods.warnUntested` already gives the soft warning, so the upper bounds add risk and no safety.
- **Granularity gaps.** Several features that only need one mod sit behind a part that requires two or three (HUD/Celestials with no mods, ARTIFACTS needing Enchanted for Vampirism-only items, SPECIES bundling witch/siphoner with Vampirism, curses gated on Vampirism).
- **Three test assumptions fail in valid configs.** `ItemModelCoverageGameTests`, `SupernaturalStateGameTests`, and `SeveranceGameTests` assert facts that are only true when specific parts are enabled, not when specific mods are present.
- **Client not boot-tested.** All client call sites are guarded by reading, but no client launch was performed.

## The eight fixes

### Fix 1 — Open version caps in neoforge.mods.toml

**Problem:** Seven optional mods have `[floor, ceiling)` ranges. An installed-but-newer mod stops FML from starting the game.

**Change:** In `src/main/templates/META-INF/neoforge.mods.toml`, change each `versionRange` to open-ended:

| modId | Current | New |
|---|---|---|
| tensura | `[2.0.1,2.1)` | `[2.0.1,)` |
| manascore_race | `[4.0.0,4.1)` | `[4.0.0,)` |
| vampirism | `[1.10.13,1.11)` | `[1.10.13,)` |
| werewolves | `[2.0.3,2.1)` | `[2.0.3,)` |
| enchanted | `[4.2.7,4.3)` | `[4.2.7,)` |
| curios | `[9.5,10)` | `[9.5,)` |
| geckolib | `[4.7,5)` | `[4.7,)` |

EC (`enhancedcelestials2core`) and OPAC (`openpartiesandclaims`) already use `[0,)`.

**Safety:** `LinkedMods.TESTED` continues to warn for untested versions. No behavioural change for in-range versions. Removes the only path where our mod prevents the game from starting.

**Files:** `src/main/templates/META-INF/neoforge.mods.toml` (7 lines).

**Test:** Boot with a newer-than-tested version of each mod (one at a time); confirm FML starts and `LinkedMods` logs a warning.

---

### Fix 2 — Fix three test assumptions + add CI combos

**Problem:** Three GameTests fail in valid mod configurations because they assert part-dependent facts using mod-presence checks.

**Changes:**

1. **`gametest/ItemModelCoverageGameTests.java:51`** — Replace `seen > 0 || !isLoaded("vampirism")` with a check that counts enabled item-registering parts (ARTIFACTS, SPECIES, BOSSES, SUPERNATURAL). If none are enabled, skip the assertion.

2. **`parts/supernatural/gametest/SupernaturalStateGameTests.java:76`** — The fake player gets human-rung SP (7) when SPECIES is off, not the expected 2. Pin the assertion to the actual rung floor when SPECIES is disabled, or skip the SP check when `!parts.contains(SPECIES)`.

3. **`parts/species/gametest/SeveranceGameTests.java:44,119`** — `SireBond.register()` runs only with Werewolves. Early `succeed()` when `!isLoaded("werewolves")` instead of building a HYBRID player and expecting a bond that cannot exist.

**CI additions:** Add Gradle tasks/steps for:
- `-PnoLinkedMods` (zero-mod baseline, 30 tests, ~25 s)
- Single-mod runs via `-PaddonJars=<jar>` for Vampirism-only, Tensura-only, Enchanted-only (once CI can fetch jars from Maven/cache)

**Files:** Three test files above; CI config (location TBD — no `.github/workflows/` exists yet; may be external CI or need creation).

**Test:** Each previously-failing combo now passes. Full combo still passes (no regression).

---

### Fix 3 — HUD and CELESTIALS load only when useful

**Problem:** `Part.requiredMods` is AND-only. `List.of()` means "always enabled." HUD registers keybinds/config/payloads even with zero mods (empty wheel). CELESTIALS does nothing without EC.

**Design:** Add an `anyOf` field to `Part`:

```java
// Part.java
private final List<String> anyOfMods; // empty = no constraint (current behaviour for non-empty requiredMods)
```

Update `PartLoader.enabledParts` line 17:
```java
boolean modsOk = p.requiredMods().stream().allMatch(modLoaded)
    && (p.anyOfMods().isEmpty() || p.anyOfMods().stream().anyMatch(modLoaded));
```

Update `PartLoader.status` line 29 similarly.

**Part changes:**
- `HUD`: `requiredMods=[]`, `anyOfMods=["vampirism","tensura"]`
- `CELESTIALS`: `requiredMods=[]`, `anyOfMods=["enhancedcelestials2core"]`
- All other parts: `anyOfMods=[]` (no change)

**Mixin plugins unchanged** — they already call `PartLoader.enabledParts` and apply per-mixin mod maps.

**Files:** `core/Part.java`, `core/PartLoader.java`, `core/PartLoaderTest.java` (add anyOf cases).

**Test:** Boot with zero mods → HUD and CELESTIALS disabled. Boot with Vampirism only → HUD enabled. Boot with EC only → CELESTIALS enabled. Full combo → both enabled.

---

### Fix 4 — Split ARTIFACTS into Vampirism-only items + Enchanted rites

**Problem:** Ash Dagger, Sun Ring, Resurrection Ring, Vervain, The Cure, Mountain Ash, wolf artifacts, and Curios/inventory ring slots need only Vampirism (+Werewolves for wolf items, +Curios for slots). They are gated behind ARTIFACTS which requires Vampirism+Enchanted.

**Design:** Keep one `ARTIFACTS` part with `requiredMods=["vampirism"]`. Move Enchanted rite factories (bind-item, coven-forge, ward, locator rites) behind a new internal gate within the same part's `init`, or split into `ARTIFACTS_ENCHANTED` if the data-file count warrants it.

**Recommended approach (minimal):** Keep one part, gate internally.
- `ArtifactsPart.init` lines 48-67: wrap rite registration in `if (mods.isLoaded("enchanted"))`.
- Data files (~50 recipe/loot/biome/worldgen JSONs with `part_enabled: artifacts`): no change needed for Vampirism-only items. Rite-related data files add `"mod_loaded": "enchanted"` alongside existing `part_enabled`.
- Drop `ARTIFACTS -> VAMPIRISM_ENCHANTED` from `requiredParts()` (line 59). ARTIFACTS stands alone.
- `VampirismEnchantedPart` keeps its own rite targets/hooks; ARTIFACTS no longer reuses them.

**Files:** `core/Part.java` (drop requiredParts entry), `parts/artifacts/ArtifactsPart.java`, ~10 rite data JSONs (add `mod_loaded` condition).

**Test:** Boot Vampirism-only → items register, rites absent. Boot Vampirism+Enchanted → identical to today. Verify Curios slot fallback still works without Enchanted.

---

### Fix 5 — SUPERNATURAL needs no required mod

**Problem:** The hub shell, human ladder, Tensura ladder, skill points, quests, settings, and admin pages work without Vampirism. Only ~12 non-client files and 5 client pages touch Vampirism classes.

**Design:**
- `SUPERNATURAL`: `requiredMods=[]`, `anyOfMods=["vampirism","tensura","werewolves"]` (loads when any content mod is present; pure-vanilla installs get nothing).
- Wrap the ~12 Vampirism-touching files in the existing adapter pattern (`isLoaded("vampirism")` + nested class or lambda guard). Files identified in audit section 4 row 20: Detectors, TaskTracker, Migration, SkillEffects, BloodEffects, ModSkillService, VampirismTweaks, 5 client pages.
- `SUPERNATURAL_TENSURA`: drop `vampirism` from `requiredMods`, keep `tensura`. Wrap `PrimevalRoarGrant` (the one file touching Vampirism) behind `isLoaded("vampirism")`.
- Update `requiredParts()`: `SUPERNATURAL_TENSURA -> SUPERNATURAL` stays (still needs the hub).

**Files:** `core/Part.java`, ~12 supernatural Java files (adapter wrapping), `parts/supernaturaltensura/` (1 file), `core/PartLoaderTest.java`.

**Test:** Boot Tensura-only → hub loads, Tensura ladder works, Vampirism pages absent. Boot Werewolves-only → hub loads, pack pages work. Boot Vampirism → identical to today.

---

### Fix 6 — Enchanted-only curses out of VAMPIRISM_ENCHANTED

**Problem:** Insanity, Plague, Susceptibility, Sun poppet, Silver poppet curses import only Enchanted (+Werewolves for silver poppet). They are gated behind VAMPIRISM_ENCHANTED which requires Vampirism.

**Design:** Create `ENCHANTED_CURSES` part with `requiredMods=["enchanted"]`. Move curse code from `parts/vampirismenchanted/curse/` to `parts/enchantedcurses/`. Silver poppet wraps Werewolves behind `isLoaded`.

**Alternative (smaller):** Keep code in place, change `VAMPIRISM_ENCHANTED.requiredMods` to `["enchanted"]`, rename to `ENCHANTED_CONTENT`. But this breaks the naming contract and confuses the Vampirism+Enchanted bridge. New part is cleaner.

**Files:** New `parts/enchantedcurses/EnchantedCursesPart.java`, move ~3 curse files, update `Part.java` enum, re-key ~5 curse data JSONs (`part_enabled: enchantedCurses`), update mixin config if curses have mixins (audit says no).

**Test:** Boot Enchanted-only → curses available. Boot Vampirism+Enchanted → identical to today. Boot Vampirism-only → curses absent (correct).

---

### Fix 7 — Split SPECIES into Vampirism core + Enchanted rites

**Problem:** SPECIES bundles Vampirism-only content (sirelines, hunters' mark/sense/instinct, originals, turning, ghouls, downed, Augustines, blood bottles, Force-feed, Rip Heart, Neck Snap, doppelganger genes, Werewolf packs/sparks/duels/NPC packs) with Enchanted-only content (rite factories, witch/siphoner types, covens/groups/circles). This blocks witch/siphoner on Enchanted-only and blocks Vampirism species features without Enchanted.

**Design:** Two parts.

**`SPECIES` (Vampirism-only):**
- `requiredMods=["vampirism"]`
- Content: sirelines, hunter mark/sense/instinct/compulsion-immunity, originals, turning, ghouls, downed, Augustines, blood bottles, Force-feed, Rip Heart, Neck Snap, doppelganger genes, hybrid/tribrid (Werewolves behind existing `isLoaded`), Werewolf packs/sparks/duels/NPC packs/ranks/anchors/moon/eclipse.
- Mixins: none (the three species mixins are Enchanted-only; move to SPECIES_RITES).

**`SPECIES_RITES` (Enchanted):**
- `requiredMods=["enchanted"]`
- `requiredParts=[SPECIES]` — wait, no. Witch/siphoner should work WITHOUT Vampirism (Fix 8). So: `requiredMods=["enchanted"]`, no requiredParts.
- Content: rite factories in `species/rite/`, `witch/` types (AncestralCallingRiteFactory, Siphoning, WitchAltarBonus), covens/groups/circles, Alpha Trial/Brother/Lineage rite factories.
- Mixins: the three species mixins (move from `species.mixins.json` to `species_rites.mixins.json`).

**Data file re-keying (~40 files):**
- Files that serve Vampirism-only content: keep `part_enabled: species`.
- Files that serve Enchanted rites/witch: change to `part_enabled: speciesRites`.
- Files that serve both (e.g., a recipe that uses a Vampirism item AND an Enchanted rite): add a second condition block or duplicate the entry with both part conditions. Audit section 4 row 27 identifies these.
- Script: write a one-shot `datarefs.ps1` variant that lists every JSON with `part: species`, cross-references against the sub-package of its source code, and outputs the target part. Manual review of the output before bulk-edit.

**StalePartModifiers:** Update owner map so modifiers from the old SPECIES part migrate to the correct new part on first load.

**Downstream rewiring:**
- `BOSSES.requiredParts`: `Set.of(SPECIES)` stays (NPC Originals are Vampirism).
- `SPECIES_TENSURA.requiredParts`: change from `SPECIES` to `Set.of(SPECIES, SPECIES_RITES)` if it uses both, or just `SPECIES` if Tensura bridge is Vampirism-only. Audit says SPECIES_TENSURA touches Vampirism+Enchanted+Tensura → needs both.
- `EVOLUTION_RITES.requiredParts`: change from `SPECIES_TENSURA` to include `SPECIES_RITES` (witch types live there after Fix 8).

**Files:** `core/Part.java` (new enum value, update requiredParts), new `parts/speciesrites/SpeciesRitesPart.java`, move ~30 Java files, new mixin config `species_rites.mixins.json`, update `species.mixins.json` (remove 3 entries), re-key ~40 data JSONs, update `StalePartModifiers` owner map, update `PartLoaderTest`.

**Test matrix:**
- Vampirism-only → species features work, rites absent.
- Enchanted-only → witch/siphoner/covens work, species features absent.
- Vampirism+Enchanted → identical to today.
- Full combo → identical to today.
- Save migration: load a save made on old SPECIES, verify StalePartModifiers migrates attachments.

---

### Fix 8 — Witch/siphoner and evolution rites without Vampirism

**Problem:** After Fix 7, witch/siphoner types live in SPECIES_RITES (Enchanted-only). EVOLUTION_RITES currently requires `vampirism+enchanted+tensura` but its non-test code touches only Enchanted+Tensura+ManasCore. The Vampirism requirement comes from needing witch types (now in SPECIES_RITES) and the performer being a witch type.

**Design:**
- `EVOLUTION_RITES`: change `requiredMods` to `["enchanted","tensura"]`. Change `requiredParts` to `Set.of(SPECIES_RITES)` (for witch types). Remove `vampirism`.
- The "performer must be a witch type" check already lives in SPECIES_RITES after Fix 7, so no additional gate needed.
- Addon probes (Ascension, Mysticism, S-T Extras, Great Sage, FoxAblaze, Horizons, Kumodesu) remain reflection-based with `mod_loaded` in rite JSON — no change.

**Files:** `core/Part.java` (update EVOLUTION_RITES entry), `parts/evolutionrites/EvolutionRitesPart.java` (verify no Vampirism imports remain; audit says clean).

**Test:** Boot Enchanted+Tensura (no Vampirism) → evolution rites available, witch performer works. Boot full combo → identical to today.

---

## Migration and rollback safety

### Save compatibility
- **Attachment keys:** `SupernaturalState`, `HUD_LOADOUT`, and part-specific attachments are keyed by part configKey. Splitting SPECIES → SPECIES + SPECIES_RITES means old saves have `species` keys. `StalePartModifiers` must map `species` → `species` OR `speciesRites` based on the modifier's owner class. Add a migration pass in `StalePartModifiers.init` that runs once per save.
- **Data-pack overrides:** Packs that override `part_enabled: species` JSONs will need updating. Document this in the changelog. Provide a grep command for pack authors: `grep -rl '"part": "species"' data/`.
- **Config keys:** `supernaturalbloodlines-common.toml` has per-part switches. New parts (`ENCHANTED_CURSES`, `SPECIES_RITES`) get new keys with defaults matching old behaviour (enabled). Old `species` switch controls the new `SPECIES` part; add a note that `speciesRites` is independent.

### Rollback
- Every fix is independent except 8 depends on 7. Fixes 1-6 can ship in any order or revert individually.
- Fix 7 (SPECIES split) is the highest-risk change. Ship it in a release that includes the StalePartModifiers migration. If reverted, the migration is harmless (maps to a part that exists again).
- Version cap opening (Fix 1) is zero-risk to revert.

### Modpack impact
- **Existing packs with all mods:** No behavioural change. Every split preserves today's behaviour when all mods are present.
- **Packs missing one mod:** Gain content they previously couldn't access (the whole point). No loss.
- **Packs with out-of-range mod versions:** Fix 1 unblocks them. Previously FML refused to start; now the game starts with a warning.

---

## Testing strategy

### Per-fix proof matrix

| Fix | Zero-mod | Vamp-only | Tens-only | Ench-only | Vamp+Ench | Vamp+Tens | Ench+Tens | Full |
|-----|----------|-----------|-----------|-----------|-----------|-----------|-----------|------|
| 1 (caps) | — | newer jar | newer jar | newer jar | — | — | — | — |
| 2 (tests) | ✓ pass | ✓ pass | ✓ pass | ✓ pass | ✓ pass | ✓ pass | ✓ pass | ✓ pass |
| 3 (HUD/Cel) | ✗ disabled | HUD on | HUD on | ✗ | HUD on | HUD on | HUD on | both on |
| 4 (artifacts) | — | items ✓ | — | — | items+rites | items ✓ | — | identical |
| 5 (supernatural) | ✗ | identical | hub+ladder | ✗ | identical | identical | ✗ | identical |
| 6 (curses) | — | ✗ | — | curses ✓ | identical | ✗ | curses ✓ | identical |
| 7 (species split) | — | species ✓ | — | rites ✓ | identical | species ✓ | rites ✓ | identical |
| 8 (evo rites) | — | ✗ | ✗ | ✗ | ✗ | ✗ | evo ✓ | identical |

### Boot combos to add to CI
1. `-PnoLinkedMods` (zero-mod baseline)
2. `-PaddonJars=<vampirism.jar>` (Vampirism-only)
3. `-PaddonJars=<tensura+manascore+architectury+geckolib+sbl+terrablender>` (Tensura-only)
4. `-PaddonJars=<enchanted+stateobserver+modopedia+geckolib>` (Enchanted-only)
5. Existing CI combo (`-PnoWerewolves -PnoCurios`) stays.
6. Full combo stays.

### Client testing
The audit did not boot a client. Plan 22 implementation should include at least one client boot per combo category (zero-mod, single-mod, full) to verify HUD rendering, mixin application, and keybind registration.

---

## Files changed (summary)

| File | Fixes |
|------|-------|
| `src/main/templates/META-INF/neoforge.mods.toml` | 1 |
| `src/main/java/.../core/Part.java` | 3, 4, 5, 6, 7, 8 |
| `src/main/java/.../core/PartLoader.java` | 3 |
| `src/main/java/.../core/PartLoaderTest.java` | 3, 5, 7 |
| `src/main/java/.../core/StalePartModifiers.java` | 7 |
| `src/main/java/.../core/EnabledParts.java` | 3 |
| `src/main/java/.../parts/artifacts/ArtifactsPart.java` | 4 |
| `src/main/java/.../parts/supernatural/*.java` (~12 files) | 5 |
| `src/main/java/.../parts/supernaturaltensura/*.java` (1 file) | 5 |
| `src/main/java/.../parts/enchantedcurses/EnchantedCursesPart.java` (new) | 6 |
| `src/main/java/.../parts/speciesrites/SpeciesRitesPart.java` (new) | 7 |
| `src/main/java/.../parts/evolutionrites/EvolutionRitesPart.java` | 8 |
| `src/main/resources/META-INF/services/` or mixin configs | 6, 7 |
| `src/test/java/.../gametest/ItemModelCoverageGameTests.java` | 2 |
| `src/test/java/.../parts/supernatural/gametest/SupernaturalStateGameTests.java` | 2 |
| `src/test/java/.../parts/species/gametest/SeveranceGameTests.java` | 2 |
| ~50 data JSONs (recipes, loot, altars, rites) | 4, 6, 7 |
| CI config (location TBD) | 2 |