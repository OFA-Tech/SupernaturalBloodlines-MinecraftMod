# Part `species` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Playable TVD-inspired species, built as layers on Vampirism / Werewolves factions and created through Enchanted rites. Delivered in this order:
1. Original (minimal)
2. hybrid
3. Original hybrid
4. witch / siphoner
5. heretic
6. tribrid

**Architecture:** `BloodlineRules` (foundation) decides every state change. This part supplies:
- **Triggers:** rites through plan 3's `FactionRiteFactory` (its `faction` field accepts `vampire|werewolf|hunter|none|vampire_or_werewolf`) and `RiteTargets.centrePlayerOrCaster(Rite, Rite.RiteParams, ServerLevel)`, plus new factories; items such as Original's blood, doppelgänger blood and white oak; death and drink events.
- **Effects of a state:** attributes, damage rules, the bite poison, blood thirst, and the witch altar bonus.

Every effect reads `BloodlineAccess.get(player)`, so the state is the single source of truth.

**Tech Stack:** As foundation, plus the Vampirism API, Enchanted API, Werewolves (optional: hybrid-type species only), and at most 2 mixins into Enchanted (witch bonus), each with `require = 0` and a config switch.

**Spec:** design section "4. `species`" and "Decisions after the builder check".

## Global Constraints

- Everything in foundation's Global Constraints.
- Only players whose Tensura race has `tensura:human_like`, or anyone when Tensura is absent, can take a species. Use `SpeciesEligibility.isHumanLike(ServerPlayer)`: if Tensura is loaded it calls the Tensura check through a small interface implemented in plan 5, otherwise it returns true.
- The part needs Vampirism + Enchanted. Hybrid-type species (HYBRID, ORIGINAL_HYBRID, TRIBRID) exist only when `werewolves` is loaded: their code lives in `parts.species.hybrid` (and is only touched behind `ModList.isLoaded("werewolves")`), and their rites are data-conditioned on `werewolves`. Without Werewolves, the rules refuse them with `…refusal.needs_werewolves` (add this check in `SpeciesContext`). They use the werewolf faction as their base. ORIGINAL and HERETIC use the vampire faction. WITCH and SIPHONER use no faction or the werewolf faction.
- `max_originals_per_world` defaults to 0 (unlimited). The transition time defaults to 24000 ticks.
- Generic names only:
  - items: "Original's Blood", "Doppelgänger Blood", "White Oak Stake"
  - rites: Rite of Immortality, Rite of the Moon, Rite of Ancestral Calling
- Plan 3 must be merged first: this part reuses `FactionRiteFactory` and `RiteTargets`. If plan 3 isn't merged, move those two classes into `core/rite/` in a small shared commit.

## Review Focus

- **A player who logs out mid-transition** must resume the same timer on login, not reset or skip it. Pinned by `transitionTimerSurvivesRelog`.
- **Two Originals feeding blood to the same werewolf** must make the last blood drunk before death the sire. Pinned by `lastOriginalBloodIsSire`.
- **An Original hit by a /kill command or void damage** must still die, because the "can't drop below half a heart" rule applies only to normal damage. Pinned by `voidAndKillBypassOriginalProtection`.
- **A hybrid hitting their sire with splash or area damage** must have it cancelled too, not only melee. Pinned by `sireProtectedFromIndirectDamage`.
- **The witch bonus mixin failing to apply** (an Enchanted update) must just turn the bonus off and log a warning, never crash. Pinned by the `require = 0` mixin config and the test `witchBonusDisabledWhenMixinMissing`, which uses the config switch to simulate it.

---

### Task 1: Discovery and shared species plumbing

**Files:**
- Create: `docs/parts/species.md` ("Hook points (verified)": Werewolves form API, `WerewolfPlayer` access, full-moon helper, the Vampirism blood drink events, Enchanted rite `entities`/`time` fields, and the altar range/cost code locations for the witch mixin), `parts/species/SpeciesEligibility.java`, `parts/species/SpeciesContext.java`, `parts/species/OutcomeExecutor.java`, `parts/species/SpeciesData.java` (ModConfigSpec section `species`)

**Interfaces:**
- **`interface HumanLikeCheck { boolean isHumanLike(ServerPlayer p); }`**, with `SpeciesEligibility.setCheck(HumanLikeCheck)` (plan 5 installs the Tensura one; the default returns true).
- **`static BloodlineContext SpeciesContext.of(ServerPlayer p, @Nullable UUID bloodSource)`:** reads the faction from `VampirismAPI.factionPlayerHandler`, `humanLike` from `SpeciesEligibility`, the Originals count from `SpeciesData` (a `SavedData` counting ORIGINAL + ORIGINAL_HYBRID players), and the config.
- **`static void OutcomeExecutor.run(ServerPlayer p, Outcome o)`:**
  - JOIN_VAMPIRE → `setFactionAndLevel(vampire, max(1, current))`
  - JOIN_WEREWOLF → the same for werewolf
  - LEAVE_FACTION → `leaveFaction(false)`
  - KILL_PLAYER → `p.hurt(damageSources().source(supernaturalbloodlines:failed_transition), Float.MAX_VALUE)`
  - A refusal sends the key as a chat message.
  - All of it runs inside `ReentrancyGuard.run(uuid, "species", …)`.

- [ ] **Step 1:** Write the GameTests:
  - `executorJoinsVampire`
  - `executorRefusalSendsMessage`
  - `nonHumanLikeRefusedWhenCheckSaysNo`: install a stub `HumanLikeCheck` returning false.
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species): context, eligibility, outcome executor".

### Task 2: Originals (minimal) and Original's Blood

**Files:**
- Create:
  - `parts/species/original/OriginalRules.java`: listeners
  - `parts/species/item/OriginalBloodItem.java`
  - `parts/species/item/WhiteOak.java`: a white oak log/planks set, plus `white_oak_stake` (sword-like) and the item tag `supernaturalbloodlines:white_oak`
  - `data/supernaturalbloodlines/enchanted/circle_magic/rite/immortality.json`, using `supernaturalbloodlines:faction_rite` with event RITE_OF_IMMORTALITY and no faction requirement. Add the codec value `faction: "none"` to plan 3's factory.
  - the rite that makes Original's blood: `original_blood.json`, a `create_item` with an `entities` condition that the target is an Original, done through a new factory `supernaturalbloodlines:original_blood` that checks `BloodlineAccess.get(target).species()` is ORIGINAL or ORIGINAL_HYBRID, then gives the item with the `supernaturalbloodlines:blood_source` component set to the target's UUID
  - the effect `original_blood` (MobEffect, 5 min)
- Immortality rite contents:
  - full moon via the `time` field
  - power 12000
  - entities: 1 villager as the sacrifice
  - items: white oak sapling, a golden apple, Vampirism human heart (or a fallback item from the Task 1 notes)

**Interfaces:**
- **Drinking `OriginalBloodItem`:** applies the effect `original_blood`, storing the source UUID in a `supernaturalbloodlines:blood_source` attachment on the drinker (last one wins).
- **Original protection (R32)** (`LivingIncomingDamageEvent`, LOWEST priority):
  - Lethal damage to an ORIGINAL or ORIGINAL_HYBRID from a source that isn't white oak (weapon not tagged `supernaturalbloodlines:white_oak`), the void, `minecraft:generic_kill` or `#minecraft:bypasses_invulnerability` puts them into **Vampirism's dying (DBNO) state**, with its give-up / wait screen. Task 1 records the Vampirism API for this. If Vampirism exposes no hook, implement our own `downed` state: no movement or attacks, a client screen with Give up / Wait, revive after 600 ticks.
  - **White oak lethal damage kills instantly**, bypassing Vampirism's dying state (cancel or skip it for that death).
  - Originals get +20% attack damage and +4 max health (attribute modifiers with id `supernaturalbloodlines:original`).

- [ ] **Step 1:** Write the GameTests:
  - `immortalityMakesOriginalVampire`
  - `immortalityRefusedWhenCapReached` (cap 1)
  - `originalGoesDownOnNonOakLethalDamage`
  - `whiteOakKillsOriginalInstantly` (no dying state)
  - `voidAndKillBypassOriginalProtection`
  - `originalBloodRecordsSource`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species): Originals, white oak, Original's blood".

### Task 3: Hybrid transition

**Files:**
- Create:
  - `parts/species/hybrid/TransitionHandler.java`: death, respawn, tick, login
  - `parts/species/item/DoppelgangerBloodItem.java`
  - the rite `doppelganger_blood.json`: factory `create_item` with a taglock of a player + 2 diamonds + an amethyst shard, power 6000
  - the effect `unstable`
  - the damage type `failed_transition`

**Interfaces:**
- **On `LivingDeathEvent`:** a werewolf with the `original_blood` effect gets `BloodlineEvent.DIED_WITH_ORIGINAL_BLOOD` applied, with the context's `bloodSource` = the attachment UUID. It's marked to respawn in transition (the state persists through `copyOnDeath`).
- **Server tick:** while `inTransition`, every 20 ticks of online time decrement `transitionTicksLeft` by 20. At 0, apply TRANSITION_TIMEOUT and execute it.
- **Drinking:**
  - doppelgänger blood → DRANK_DOPPELGANGER_BLOOD
  - a Vampirism blood bottle or a bite while in transition → DRANK_OTHER_BLOOD, which adds the `unstable` effect (60 s). When it ends, apply UNSTABLE_DEATH and execute (kill).

- [ ] **Step 1:** Write the GameTests:
  - `werewolfDyingWithOriginalBloodEntersTransition`
  - `doppelgangerBloodCompletesHybrid`
  - `otherBloodMakesUnstableThenKills`
  - `transitionTimesOut`
  - `transitionTimerSurvivesRelog`: simulate save/load of the attachment mid-transition
  - `lastOriginalBloodIsSire`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species): hybrid transition".

### Task 4: Hybrid traits and sire bond

**Files:**
- Create: `parts/species/hybrid/HybridTraits.java`, `parts/species/hybrid/BloodThirst.java`, `parts/species/hybrid/SireBond.java`, the effect `hybrid_venom`.

**Interfaces:**
- **Attributes** (applied on state change and on login; removed when the species is lost), modifier id `supernaturalbloodlines:hybrid`:
  - `+0.02` movement speed and `+2` attack damage, both scaled by age: `min(1, ticksSinceTurned / 1_200_000)`. Use the `BloodlineState.turnedAt` field from foundation.
- **Hybrids shift form at will** and have no full-moon pain. Read the Werewolves form API notes from Task 1:
  - If Werewolves exposes forced full-moon transformation via `WerewolfPlayer` ticking, cancel it for hybrids through its event or an action-cooldown hook.
  - If it can't be cancelled without a mixin, accept the forced shift and note it in `docs/parts/species.md`. Don't add a third mixin.
- **`BloodThirst`:** a value from 0 to 20 (the `BloodlineState.thirst` field from foundation). It rises from drinking blood bottles or biting mobs with the Werewolves bite action (`entity_blood` > 0), and falls by 1 every 1200 ticks. At 0: Weakness I. At ≥ 15: Strength I.
- **Venom (R42v):** bites and melee attacks from **any werewolf-faction player or werewolf mob** (plain werewolves, hybrids, Original hybrids, tribrids) on a Vampirism vampire apply `hybrid_venom`:
  - 1 damage every 40 ticks, plus a hallucination payload every 600 ticks (reuse plan 7's `HallucinationPayload`).
  - After 12,000 ticks it kills a **non-Original** vampire (damage type `supernaturalbloodlines:venom`). Originals and Original hybrids instead get Weakness II for its duration.
  - Milk doesn't remove it. **Only Hybrid's Blood cures it.** Original's Blood doesn't.
  - New item `hybrid_blood`, made by the rite `hybrid_blood.json` (a factory like `original_blood`, which checks that the target is HYBRID, ORIGINAL_HYBRID or TRIBRID). Drinking it removes the venom.
- **`SireBond`:** `LivingIncomingDamageEvent`. If the attacker, or the owner of a projectile or area cloud, is a hybrid whose `sire` equals the target's UUID, cancel the damage.

- [ ] **Step 1:** Write the GameTests:
  - `hybridGetsSpeedAndDamage`
  - `hybridTraitsRemovedOnCure`
  - `thirstFallsAndWeakens`
  - `venomPoisonsVampire`
  - `plainWerewolfBiteAlsoPoisons`
  - `venomKillsNonOriginalAfterTimer`
  - `originalOnlyWeakenedByVenom`
  - `onlyHybridBloodCuresVenom` (Original's Blood and milk don't)
  - `sireProtectedFromIndirectDamage`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species): hybrid traits, thirst, venom, sire bond".

### Task 5: Original hybrid

**Files:**
- Create: the rite `rite_of_the_moon.json`, using `faction_rite` with event RITE_OF_THE_MOON (it needs the gene, R35: `SpeciesContext` fills `latentGene` from `BloodlineState.wolf.latentGene`; it also unbinds a bound hybrid). The faction check accepts vampire (Original) or werewolf (bound hybrid):
  - full moon
  - power 15000
  - entities: 1 wolf + 1 villager as sacrifices
  - items: moonstone placeholder item `supernaturalbloodlines:moonstone` (a new item, found in loot or crafted with amethyst + Werewolves silver), plus Original's blood

- [ ] **Step 1:** Write the GameTests:
  - `originalWithGeneCompletingMoonRiteBecomesOriginalHybrid`: assert species ORIGINAL_HYBRID, the werewolf faction, the hybrid traits and the Original protection all apply
  - `moonRiteRefusedWithoutGene`
  - `lineageAwakenOnOriginalGivesBoundOriginalHybrid`
  - `moonRiteUnbindsBoundHybrid`
- [ ] **Step 2:** Run it (FAIL), implement, run again (PASS). Commit "feat(species): Original hybrid".

### Task 6: Witch and siphoner

**Files:**
- Create:
  - the rite `ancestral_calling.json`: factory `supernaturalbloodlines:ancestral_calling`, which applies ANCESTRAL_CALLING_SIPHONER with a config chance `siphoner_chance` (default 0.25), else ANCESTRAL_CALLING_WITCH
  - `parts/species/witch/WitchAltarBonus.java`
  - the mixin `mixin/enchanted/AltarRangeMixin.java` (+ cost) in `supernaturalbloodlines.enchanted.mixins.json`, `require = 0`, gated by config `witch_bonus_enabled`
  - `parts/species/witch/Siphoning.java`
  - an entry in `docs/mixins.md`

**Interfaces:**
- **Witch bonus:** while a witch, heretic or tribrid is within the altar's area, altar range is ×1.5 and rite power costs ×0.75. The mixin multiplies the values it reads, calling `WitchAltarBonus.rangeMultiplier(level, pos)` and `costMultiplier(caster)`.
- **Siphoning:**
  - Sneak + use an empty hand on a block with altar-power data, or on a living entity, siphons power: +50 to `siphonedPower` (cap 1000). Blocks get a 30 s per-block cooldown. Entities take 2 damage (magic).
  - Siphoners, heretics and tribrids pay rite costs from `siphonedPower` first, through the same cost hook.
  - A siphoner who isn't near an altar can use siphoned power as their only source.
- **Witch who joins the vampire faction:** listen to `FactionLevelChanged`. Joined vampire → JOINED_VAMPIRE (the witch loses magic, the siphoner becomes a heretic).

- [ ] **Step 1:** Write the GameTests:
  - `ancestralCallingMakesWitchOrSiphoner` (seeded random)
  - `witchCheapensRite`
  - `witchBonusDisabledWhenMixinMissing`: config off → normal cost
  - `siphonerDrainsBlock`
  - `siphonerPaysFromSiphonedPower`
  - `witchJoiningVampireLosesMagic`
  - `siphonerJoiningVampireBecomesHeretic`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(species): witches and siphoners".

### Task 7: Heretic and tribrid wiring, then checks

**Files:**
- Modify: `TransitionHandler` (a witch completing the transition becomes a tribrid; a siphoner is refused at transition start, both through the rules), `HybridTraits` (applies to TRIBRID and ORIGINAL_HYBRID too), and `WitchAltarBonus` / `Siphoning` (apply to HERETIC and TRIBRID).

- [ ] **Step 1:** Write the GameTests:
  - `hereticKeepsSiphoning`
  - `witchWerewolfCompletingTransitionBecomesTribrid`
  - `tribridHasHybridTraitsAndWitchBonus`
  - `siphonerWerewolfCannotStartTransition`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Run with `-PnoWerewolves`: Originals, witches and heretics work, and the hybrid rites are absent. Then play through in game: Original → make Original's blood → turn a werewolf friend (a second client) into a hybrid → Original hybrid → witch → tribrid. Tick `docs/parts/species.md`, and run `-PnoLinkedMods` (species reports disabled; no errors).
- [ ] **Step 4:** Update `CHANGELOG.md`. Commit "feat(species): heretic and tribrid".
