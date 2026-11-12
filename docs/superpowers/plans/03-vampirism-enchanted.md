# Part `vampirism_enchanted` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vampirism and Werewolves work with Enchanted's witchcraft:
- Garlic and wolfsbane are shared.
- Rites cure vampirism and lycanthropy.
- A blood offering gives altar power.
- Silver and sun poppets.
- Enchanted's mobs get blood values.

**Architecture:** Data first: tags, altar power, blood data maps and rite JSON. The code is in `parts.vampirismenchanted`:
- a shared `FactionRiteFactory`, which plan 4 reuses
- two poppet effects
- a Werewolves-only nested package `parts.vampirismenchanted.werewolves`

**Tech Stack:** As foundation, plus the Vampirism API, Enchanted API, and Werewolves (optional).

**Spec:** design section "2. `vampirism_enchanted`".

## Global Constraints

- Everything in foundation's Global Constraints.
- Cure rites refuse Originals (any species ORIGINAL / ORIGINAL_HYBRID) using `BloodlineRules`. Plan 4 adds the species effects; this part only calls `BloodlineAccess.applyEvent` and acts on the Outcome.

## Review Focus

- **The cure rite on a player not in that faction** must fail the rite with a message, and the ingredients are not lost. Pinned by `cureRiteOnNonVampireFails`.
- **The cure rite on a hybrid or tribrid**, once plan 4 exists, must follow `BloodlineRules` (vampire traits removed, still a werewolf). Pinned by `cureRiteRespectsBloodlineOutcome`, using a stub state set via the debug API.
- **Werewolves absent:** the Silver Moon rite and the silver poppet must not be registered or crash. Pinned by the Task 5 clean run with `-PnoWerewolves`.
- **Enchanted garlic in Vampirism garlic recipes and garlic diffusers.** Players expect it to work wherever garlic is used. Pinned by `enchantedGarlicCountsAsGarlic`.
- **The Sun Poppet** must only absorb sun damage, never all damage. Pinned by `sunPoppetIgnoresOtherDamage`.

---

### Task 1: Discovery notes

**Files:**
- Create: `docs/parts/vampirism_enchanted.md`, a section "Hook points (verified)".

- [ ] **Step 1:** Record:
  - Vampirism's sun damage type id, garlic tags (`c:crops/garlic`, `vampirism:…`) and blood bottle item ids
  - the `item_blood` / `entity_blood` data map formats and their file locations
  - Werewolves' wolfsbane item/block ids and silver tags
  - Enchanted's `enchanted:garlic` / `enchanted:wolfsbane` item and block ids
  - the `Rite` lifecycle (same as plan 2, Task 1; reuse those notes if done)
- [ ] **Step 2:** Commit "docs: vampirism_enchanted hook points".

### Task 2: Data — plant parity, altar power, blood values

**Files:**
- Create:
  - tag `data/c/tags/item/crops/garlic.json`, adding `enchanted:garlic`
  - the Vampirism garlic item tag (id from Task 1), adding `enchanted:garlic`
  - Werewolves' wolfsbane tag (if any; otherwise a recipe conversion shapeless 1:1 both ways), adding `enchanted:wolfsbane`
  - altar power for Vampirism and Werewolves plant blocks: `data/vampirism/enchanted/altar/block/*.json` and `data/werewolves/enchanted/altar/block/*.json`
  - `entity_blood` entries for Enchanted's mobs
- Every file has the matching `neoforge:mod_loaded` conditions.

- [ ] **Step 1:** Write the GameTests:
  - `enchantedGarlicCountsAsGarlic`: the item has the Vampirism garlic tag, and a vampire next to a garlic-placed block gets the garlic effect if placeable.
  - `vampirismPlantsPowerAltar`
  - `enchantedMobsHaveBlood`
- [ ] **Step 2:** Run them (FAIL), add the data, run again (PASS). Commit "feat(vampirism_enchanted): plant parity, altar power, blood data".

### Task 3: Shared `FactionRiteFactory` and the cure rites

**Files:**
- Create: `parts/vampirismenchanted/rite/FactionRiteFactory.java`, `parts/vampirismenchanted/rite/RiteTargets.java`, and the rite JSONs `cleansing_blood.json` and `silver_moon.json` (the latter conditioned on `werewolves`).
- Test: GameTests `VampirismEnchantedGameTests`

**Interfaces:**
- **`RiteTargets.centrePlayerOrCaster(Rite rite, ServerLevel level)`:** returns `Optional<ServerPlayer>`, the player standing on the circle's centre block, else the caster. Plan 4 reuses this.
- **`FactionRiteFactory`** (type `supernaturalbloodlines:faction_rite`):
  - Codec fields: `event: BloodlineEvent`, and `faction: "vampire" | "werewolf"` (the faction the target must be in).
  - On start:
    1. Resolve the target.
    2. Check the faction (else fail with `supernaturalbloodlines.rite.wrong_faction`).
    3. Call `BloodlineAccess.applyEvent(target, event, ctx)`.
    4. If the result is refused, fail with its key.
    5. Otherwise run the Outcome actions. `LEAVE_FACTION` → `IFactionPlayerHandler.leaveFaction(false)`. Then the plain cure: leave the faction when the state species is NONE.
  - Plan 4 registers further `BloodlineEvent` values through the same factory.
- **Rites:**
  - `cleansing_blood`: event RITE_OF_CLEANSING_BLOOD, faction vampire, power 4000, garlic + holy water + golden apple.
  - `silver_moon`: event RITE_OF_THE_SILVER_MOON, faction werewolf, needs a full moon (the `time` field), silver ingredients.

- [ ] **Step 1:** Write the GameTests:
  - `cleansingCuresPlainVampire`
  - `silverMoonCuresPlainWerewolf`
  - `cureRiteOnNonVampireFails`
  - `cureRiteRespectsBloodlineOutcome`: set an ORIGINAL via `BloodlineAccess.set`; expect a refusal with `…refusal.original_cure` and the player still a vampire.
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(vampirism_enchanted): faction rite factory and cure rites".

### Task 4: Blood Offering, Sun Poppet, Silver Poppet

**Files:**
- Create:
  - The Blood Offering rite JSON only, no Java: Enchanted's `enchanted:create_item` factory turns 4 Vampirism blood bottles + a stone item into `supernaturalbloodlines:blood_stone`. That block's altar power is data, `data/supernaturalbloodlines/enchanted/altar/block/blood_stone.json` = `{"power": 25, "limit": 16}`. The block is registered in `PartItems`, the same "power block" approach as plan 2's condensed magicule.
  - `parts/vampirismenchanted/item/SunPoppetEffect.java`: protects against Vampirism's sun damage type
  - `parts/vampirismenchanted/werewolves/SilverPoppetEffect.java`: protects against damage from items tagged `werewolves:type/silver` and the `werewolves:bite` damage tag
  - `parts/vampirismenchanted/item/PartItems.java`: the two poppet items; the silver poppet is registered only if werewolves is loaded
  - recipes, lang entries and placeholder textures

- [ ] **Step 1:** Write the GameTests:
  - `bloodOfferingPowersAltar`
  - `sunPoppetAbsorbsSunDamage`
  - `sunPoppetIgnoresOtherDamage`
  - `silverPoppetBlocksSilver` (werewolves only)
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).

### Task 5: Checks

- [ ] **Step 1:** Check every feature in game and tick `docs/parts/vampirism_enchanted.md`. Run with `-PnoLinkedMods` and `-PnoWerewolves`. Expected: no errors from this mod.
- [ ] **Step 2:** Update `CHANGELOG.md`. Commit "feat(vampirism_enchanted): poppets and blood offering".
