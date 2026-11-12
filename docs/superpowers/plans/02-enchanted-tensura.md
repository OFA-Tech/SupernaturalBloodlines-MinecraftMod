# Part `enchanted_tensura` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enchanted's witchcraft and Tensura's magic work together:
- Tensura magic blocks power the altar.
- Two curses hit magicules and skills.
- A poppet guards against spiritual damage.
- A purification rite and a magicule offering rite.
- Enchanted's mobs get EP.

**Architecture:** Data first. Altar power, rites and EP are JSON under `src/main/resources/data/…`, each file gated with `neoforge:conditions` on `enchanted` + `tensura`. Code is in `parts.enchantedtensura`: two `Curse`s registered with `CurseManager`, two `RiteFactory` codecs registered with `RiteFactoryRegistry` from the part's init (which runs in the mod constructor, before datapacks load), and one poppet item.

**Tech Stack:** As foundation, plus the Enchanted API (`net.favouriteless.enchanted.api`), ManasCore skill API, and Tensura `TensuraStorages` / `TensuraDamageHelper`.

**Spec:** design section "1. `enchanted_tensura`".

## Global Constraints

- Everything in foundation's Global Constraints.
- Rite JSON lives at `data/supernaturalbloodlines/enchanted/circle_magic/rite/<name>.json`. Altar power lives at `data/tensura/enchanted/altar/block/<block>.json` (the block's namespace).
- Rites, curses and poppets use only Enchanted's API package, plus `circle_magic.rites.Rite` where a factory must return one.
- New damage types for this part have ids containing `holy` so Tensura treats them as holy.

## Review Focus

- **Magicule Drain when the player has 0 magicules** must stop at 0, never negative. Pinned by `MagiculeDrainCurseTest.neverBelowZero`.
- **Skill Seal when the player has no skills** must do nothing and not crash. Pinned by `skillSealWithNoSkillsIsNoop`.
- **A Magicule Offering when the caster lacks the magicules** must fail the rite with a message and take nothing. Pinned by `offeringFailsWithoutMagicules`.
- **Purification near players of non-undead races** must not hurt them. Pinned by `purificationSparesLiving`.
- **Curses removed on death or by Enchanted's remove-curse rite** must restore everything (e.g. the sealed skill becomes usable). Pinned by `removingSkillSealRestoresSkill`.

---

### Task 1: Discovery notes

**Files:**
- Create: `docs/parts/enchanted_tensura.md`, a section "Hook points (verified)".

- [ ] **Step 1:** From the Tensura jar (`javap`, zip listing), record:
  - the magic-related block ids: magic ore, crystal, magisteel and similar blocks
  - the `IExistence` getters and setters, and the magicule max attribute
  - the `SkillEvents.ACTIVATE_SKILL` signature, and how to interrupt it
  - the `tensura:is_spiritual` damage-type tag
- [ ] **Step 2:** From the Enchanted sources (branch `1.21.1`), record:
  - the `Rite` base class constructor and lifecycle (start, tick, end, fail)
  - `BaseRiteParams` / `RiteParams`
  - how the rite finds its caster and centre
  - `PoppetColour` values
  - `PlayerPoppetItem`'s constructor
  - the list of Enchanted entity ids
- [ ] **Step 3:** Commit "docs: enchanted_tensura hook points".

### Task 2: Data — altar power and EP

**Files:**
- Create: `data/tensura/enchanted/altar/block/<id>.json` for each magic block from Task 1. Values: `{"power": 6, "limit": 20}` for ores and `{"power": 10, "limit": 10}` for crystal/magisteel blocks, each file with `"neoforge:conditions": [{"type":"neoforge:mod_loaded","modid":"enchanted"}]`.
- Create: `data/tensura/entity_existence/supernaturalbloodlines_enchanted_<mob>.json` for each Enchanted mob, with min/max magicule set by mob strength (record the chosen numbers in the docs file).

- [ ] **Step 1:** Write the GameTest `magicBlocksPowerAltar`: build an Enchanted altar with 10 Tensura magic ore blocks nearby. Assert the altar's max power is higher than with 10 stone blocks.
- [ ] **Step 2:** Run it (FAIL), add the data, run again (PASS).
- [ ] **Step 3:** Check in game: Tensura's analyze skill on an Enchanted mob shows EP. Commit "feat(enchanted_tensura): altar power and EP data".

### Task 3: Curses — Magicule Drain and Skill Seal

**Files:**
- Create: `parts/enchantedtensura/curse/MagiculeDrainCurse.java`, `parts/enchantedtensura/curse/SkillSealCurse.java`, `parts/enchantedtensura/curse/Curses.java`
- Create the rites: `data/supernaturalbloodlines/enchanted/circle_magic/rite/curse_magicule_drain.json` and `curse_skill_seal.json`, using Enchanted's `enchanted:apply_curse` factory.
  - Items: a taglock plus `tensura` magic crystal, plus a soul item from Task 1.
  - Shape: `enchanted:medium_circle` with nether chalk.
  - Power: 2000.
- Test: GameTests `EnchantedTensuraGameTests`

**Interfaces:**
- **`Curses.register()`**, called from `EnchantedTensuraPart.init`, registers:
  - `new Curse.Type<>(id("magicule_drain"), MagiculeDrainCurse::new, MapCodec.unit(MagiculeDrainCurse::new))`
  - the same for `id("skill_seal")`
- **`MagiculeDrainCurse.tick(ServerPlayer, int strength, long age)`:** every 20 ticks, subtracts `max(1, maxMagicule * 0.005 * (strength + 1))` from the magicules, clamped at 0, then `markDirty()`.
- **`SkillSealCurse`:**
  - `onInitialise` picks one learned skill id (deterministic per curse instance) and stores it in the curse's codec-serialised field.
  - A `SkillEvents.ACTIVATE_SKILL` listener interrupts activation of that skill while the curse is present.
  - `onRemove` clears it.

- [ ] **Step 1:** Write the GameTests:
  - `MagiculeDrainCurseTest.neverBelowZero`
  - `drainReducesMagicules`
  - `skillSealBlocksChosenSkill`
  - `skillSealWithNoSkillsIsNoop`
  - `removingSkillSealRestoresSkill`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Check in game: perform the curse rite on a second player (taglock), and confirm the effects. Commit "feat(enchanted_tensura): magicule drain and skill seal curses".

### Task 4: Rites — Purification and Magicule Offering

**Files:**
- Create: `parts/enchantedtensura/rite/PurificationRiteFactory.java`, `parts/enchantedtensura/rite/MagiculeOfferingRiteFactory.java`, `data/supernaturalbloodlines/damage_type/holy_purification.json`, and the two rite JSONs.

**Interfaces:**
- **`PurificationRiteFactory`** (type `supernaturalbloodlines:purification`, codec field `damage: float`, default 8):
  - On start, it damages every living entity within 8 blocks of the centre for which `RaceUtils.isUndead` is true, or that has the `minecraft:undead` entity tag.
  - The damage type is `supernaturalbloodlines:holy_purification`.
  - The rite JSON uses gold-based ingredients and holy water if Vampirism is loaded (a second JSON with a condition), with power 3000.
- **`MagiculeOfferingRiteFactory`** (type `supernaturalbloodlines:magicule_offering`, codec fields `magicules: double` (default 5000) and `count: int` (default 1)):
  - On start, it checks the caster's `IExistence.getMagicule() >= magicules`, subtracts them (`markDirty()`), and drops `count` × `supernaturalbloodlines:condensed_magicule` at the centre.
  - The rite JSON has `power: 0`.
- **New block `condensed_magicule`:** a plain block with a placeholder texture. Its altar power comes from data, `data/supernaturalbloodlines/enchanted/altar/block/condensed_magicule.json` = `{"power": 30, "limit": 16}`. Turning magicules into a placeable power block needs no change to Enchanted's power API, which can only consume.

- [ ] **Step 1:** Write the GameTests:
  - `purificationHurtsUndeadRace`
  - `purificationSparesLiving`
  - `offeringFailsWithoutMagicules`
  - `offeringConsumesMagicules`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(enchanted_tensura): purification and magicule offering rites".

### Task 5: Spirit Poppet

**Files:**
- Create: `parts/enchantedtensura/item/SpiritPoppetEffect.java` (implements `PlayerPoppetEffect`: `protectsAgainst` = source has the tag `tensura:is_spiritual`; `protect` = cancel the damage), `parts/enchantedtensura/item/PartItems.java` (DeferredRegister, item `spirit_poppet` = `new PlayerPoppetItem(PoppetColour.<chosen>, new SpiritPoppetEffect(), props.durability(…))`), a recipe, the lang entry, and a placeholder texture.

- [ ] **Step 1:** Write the GameTest `spiritPoppetBlocksSpiritualDamage`: a player holding the poppet in inventory, hit by a spiritual damage source. Assert no damage and that the poppet is used.
- [ ] **Step 2:** Run it (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Check every feature in game, and tick `docs/parts/enchanted_tensura.md`. Run `-PnoLinkedMods`. Expected: no errors. Update `CHANGELOG.md`. Commit "feat(enchanted_tensura): spirit poppet".
