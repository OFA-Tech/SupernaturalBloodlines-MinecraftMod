# Tensura skill mastery: how it works, what is stuck, what is gated

Research only. Nothing here was run in a game or on a server. Every claim comes from `javap -p -c` of the jars, the pack's `config/` files, or the lang/data files. "Read" means the call is in the bytecode. Where I inferred something, it says so.

**Jars read** (copies in `mastery/jars`, class dumps in `mastery/dump_*.txt`, parsers in `mastery/*.py`):
ManasCore skill 4.0.0.2 (nested in `manascore-neoforge-4.0.0.2.jar`), Tensura 2.0.1.5, Ascension 3.1.0, Great Sage 0.0.7.1, FoxAblaze 1.0.6.8 (from `tensura-addons/`; the pack carries `foxboss`, not FoxAblaze Ultimate), Mysticism 2.2.1, Kumo Desu ga 2.0.3-HF2, Slime Throne Extras 2.1.4, Horizons 0.31, Infinity Stones 8.0.0-a6, TNO Extras 1.0.3, Rebalanced combined 3.2.17 (with its 24 nested rebalance jars), Skill Trainer 2.0.5, Magic Growth 1.0.6, Skill Obtain 1.1, Skill Books 2.2.3, Better Subs 1.2.4.
**Pack config read** (read-only): `OFA - Holy Packs (Client)/config/tensura/ability/*.toml`, `greatsage-common.toml`, `tensura/ascension-skills.toml`.

Short names: `Inst` = `ManasSkillInstance`, `TS` = `io.github.manasmods.tensura.ability.TensuraSkill`, `Skill` = `...tensura.ability.skill.Skill`.

---

## 1. How mastery works

### 1.1 ManasCore (`manascore-skill-neoforge-4.0.0.2`)

| Item | What the bytecode does |
|---|---|
| `Inst.masteryPoint` | a private `double`. `getMastery()` returns it. `setMastery(double)` writes it and calls `markDirty()`. **`setMastery` fires no event and no hook.** |
| `Inst.getMaxMastery()` | delegates to `ManasSkill.getMaxMastery()`. The ManasCore default is `100` (`bipush 100`). |
| `Inst.isMastered(e)` | delegates to `ManasSkill.isMastered(inst, e)`, which is `inst.getMastery() >= getMaxMastery()`. |
| `ManasSkill.addMasteryPoint(inst, e, double)` | returns if already mastered. Builds `min(mastery + amount, max)` in a `Changeable<Double>`. Fires **`SkillEvents.SKILL_MASTERY`** (`SkillMasteryEvent.master(inst, e, Changeable<Double>)`). If the result `isFalse()` it returns. Then `setMastery(new)`. Then, if now mastered, `inst.onSkillMastered(e)`. |
| `addMasteryPoint(inst, e)` | the same with `+1.0`. |
| Hooks on `Inst` | `onSkillMastered`, `onLearnSkill`, `onForgetSkill`, `onToggleOn`, `onToggleOff`, `canBeToggled`, `canTick`. |

### 1.2 Tensura 2.0.1.5 overrides (`TS.addMasteryPoint`)

Read in `TensuraSkill.addMasteryPoint(Inst, LivingEntity, double)`:

1. Returns if `inst.isTemporarySkill()`. **A temporary instance never gains mastery.**
2. Returns if already mastered.
3. **If `mastery < 0`, it does not add mastery.** It calls `addLearnPoint(inst, e, 0, getDefaultLearningIncrement(...))` and returns.
4. Otherwise it builds the `Changeable`, fires `SKILL_MASTERY` (veto or rewrite), clamps to `Skill.getMaxMastery()` and calls `setMastery`.
5. It then calls `SkillAPI.getSkillsFrom(e).markDirty()`.
6. If now mastered:
   - `inst.onSkillMastered(e)`;
   - for a `ServerPlayer`, `addMasteryStatistic(player)`. This awards stat `tensura:skill_mastered` and fires advancement trigger `SKILL_MASTERED` (`UNIQUE_SKILL_MASTERED` too for Uniques);
   - a gold chat line `tensura.skill.mastery` ("You have mastered the usage of %s.").
7. Otherwise a player gets the action-bar line `tensura.skill.mastery.point_added`.

`addMasteryPoint(inst, e)` with no amount uses `getDefaultMasteryIncrement` = the `ABILITY_MASTERY_GAIN` attribute value (the pack's attributefix leaves its range at 0..1024).

**Max mastery** (`Skill.getMaxMastery()`, cached, switch on `SkillType` ordinal):

| Type | Config key | Default (pack config identical) |
|---|---|---|
| RESISTANCE, INTRINSIC, COMMON | `masteryIntrinsic` | **100** |
| EXTRA | `masteryExtra` | **500** |
| UNIQUE | `masteryUnique` | **1000** |
| ULTIMATE | `masteryUltimate` | **10000** |

`masteryUniqueSin = 1500` exists in config, but `Skill.getMaxMastery` does not read it. Instead the sins override `getMaxMastery`. 189 of the 520 registered skill and magic classes override it, including Envy, Greed, Lust, Sloth, Wrath, Pride and Gluttony, all Arts and Magics, and the addon skills listed in the analysis.

**Gain sources** (`AbilityConfig.Mastery`, pack values equal the defaults: `masteryPoint 1`, `masteryHitMultiplier 2`, `masteryKillMultiplier 2`, `masteryHoldTick 60`, `masteryActivateTime 10`):

| Source | Where (read) | Amount |
|---|---|---|
| Per hit | `handler.DamagingHandler` (damage lambda): when `DamageSource.tensura$getAbilityInstance() != null`, the target is not in `EP_DROP_EXCLUDED`, the target is not a summoned spawn, and the skill is not mastered | `addMasteryPoint(e, ABILITY_MASTERY_GAIN * 2)` |
| Per kill | `handler.DeathHandler` (death lambda), same ability-source test | `GAIN * 2` |
| Held skills | each skill's `onHeld`: every `masteryHoldTick` (60) ticks held, about 3 s | `addMasteryPoint(e)` |
| Toggled skills | each skill's `onTick`: a tag counter `activatedTimes` and `% masteryActivateTime (10) == 0`, about 0.5 s | `addMasteryPoint(e)` |
| Press / cast | `onPressed`, `onRelease`, magic `getCastingTime`, `onTouchEntity`, `onDamageEntity` | per skill |
| Magic and Battlewill | the same pattern; they also feed the stat counters `MAGIC_MASTERED` and `BATTLEWILL_MASTERED` that Sage checks | |

Counts over the 520 registered skill classes whose own class chain calls an `addMasteryPoint`-family method, by trigger method (a skill may have several): `onPressed` 70, `getCastingTime`+`onRelease` 80, `onHeld` 55+, `onTick` 45+, `onTouchEntity` 11, `onDamageEntity` 5. The rest is in `mastery/table.json`.

### 1.3 Negative mastery = "still learning", for any skill, not only resistances

- `getAcquirementMastery()` = `-AbilityConfig.Learning.learningPointRequirement` (pack config: **100**). A skill granted in learning form starts at `-100`.
- `addLearnPoint(inst, e, mode, double)` only works while `mastery < 0`. It checks `EnergyHelper.isOutOfEnergy` (a learning cost), fires `TensuraSkillEvents.SKILL_LEARNING`, adds the points, and caps at **0**. It adds `min..max` bonus (pack: 0..4) plus the `ABILITY_LEARNING_GAIN` attribute.
- On reaching `>= 0` it fires **`SkillEvents.UNLOCK_SKILL`** (a listener may veto, and the mastery is restored), then `onLearnSkill`, a `tensura.skill.acquire_learning` message and the level-up sound.
- A resistance learns through **`ResistSkill`** (`onTakenDamage` / `onBeingDamaged` while `mastery < 0`):
  - one hit above `getDamageAmountForLearning()` adds a learn point; or
  - the NBT counter `damagePoint` (sum of damage) reaches `damageAmount * damagePointMultiplier (10.0)`, which adds a learn point.
  - Tiers (`resistance_config.toml`, pack equals default):

| Tier | Resistances | Hit damage | Learn points |
|---|---|---|---|
| default | everything not listed | 5.0 | 100 |
| Easy | corrosion, poison, paralysis | 5.0 | 350 |
| Medium | cold, heat, darkness, earth, flame, light, spatial, water, wind, gravity, holy attack, electricity, pain, thermal fluctuation | 15.0 | 500 |
| Hard | physical attack, spiritual attack, abnormal condition, magic | 20.0 | 700 |

- Rebalanced (in `ProgressionConfig`) adds `resistance_learning.intervalTicks` and `maxPointsPerInterval`, a per-player, per-resistance cooldown. See section 2.5.
- So "negative mastery means a resistance is still learning" is true **and** it also means any other skill granted in learning form is not yet unlocked (`canBeToggled` of Sage, Great Sage, Chant Annulment, Commander and Ultra Instinct is `mastery >= 0`).

### 1.4 What happens when a skill becomes mastered (`onSkillMastered` overrides)

Read in Tensura 2.0.1.5. **Most overrides are skill evolutions:** `SkillHelper.learnSkill(e, Evolved.createLearningInstance(e))`, so the new skill arrives at **negative mastery** and needs learn points. The skill must not be a sub-instance (`isSubInstance` check).

| Mastered skill | Result | Extra condition (read) |
|---|---|---|
| Predator | Gluttony, with `setMastery(Gluttony.getAcquirementMastery)` | only if the player does not hold Starved (`hasSkill(STARVED)` is checked) |
| Analyst | Law Manipulation (free instance, tag `NoMagiculeCost`) | none |
| Researcher | Godly Craftsman (`setMastery(acquirementMastery)`, plus stored enchantments moved) | none |
| Earth / Flame / Gravity / Lightning / Sound / Spatial / Water / Weather / Wind Manipulation | the matching Domination, as a learning instance (Flame also grants Heat Wave) | `EnergyHelper.getBaseMaxEP >= <skill>.dominationEpAcquirement` (config). **Under that EP nothing happens, and the hook runs only once.** |
| Mysticism Darkness / Ice / Light Manipulation | the matching Domination | same EP check |
| Hero Haki | Sacred Haki | `IExistence.isTrueHero()` |
| Haki | Mortal Fear | none |
| Magic Sense | Universal Perception | `hasSkillFully` check |
| Danger Sense | Sense Soundwave | `hasSkill` check |
| Spatial Motion | Spatial Manipulation | none |
| Warp Portal, Gate | Spatial Motion | none |
| Ranged Barrier | Multilayer Barrier | none |
| Telepathy | Thought Communication | none |
| Water Current Control | Hydraulic Propulsion | none |
| Arts chain | Aura Slash to Heavy Slash, Five Petals Thrust to Eight Petals Flash, Ogre Sword Guillotine to Ogre Sword Cannon, Magic Bullet to Maximum Magic Bullet to Death March Dance, Formhide to Haze | none |
| Create Lesser Undead to Greater, Curse to Curse Bind | | race check (`RaceAPI.getRaceFrom`) |
| Horizons Composite Body, Indomitable | toggle-state refresh only | none |

The Tensura `/ability mastery` command and Great Sage's `AnalysisManager.master` both call `onSkillMastered` by hand, so evolutions fire on those paths too.

**Toggle unlocks:** many skills return `isMastered` from `canBeToggled`. See the list in section 3.2 (Corrosion, Poison, Paralysis, Steel Strength, Anti Skill, Berserk, Cook, Fighter, Kumo's Determination, and so on).

### 1.5 The safe server-side way to set a skill to mastered

Two verified patterns exist. Best is the first, with the second as fallback.

**A. Go through `addMasteryPoint`, so every hook fires** (this is what the Rebalanced `GatesConfig` string says its own awards do: "own addMasteryPoint, so SKILL_MASTERY fires and onSkillMastered still runs"):

```java
// server thread; inst = SkillAPI.getSkillsFrom(player).getSkill(skill).orElse(null)
// (Skills#getSkill(ManasSkill) returns Optional<ManasSkillInstance>; read in the Tensura mastery command)
if (inst == null || inst.isTemporarySkill() || inst.isMastered(player)) return;
if (inst.getMastery() < 0) {
    // learning state: TS.addMasteryPoint would route to addLearnPoint instead (see 1.2 step 3).
    // Tensura's own command and Great Sage skip this by calling setMastery directly. Either:
    //  (a) ((TensuraSkill) inst.getSkill()).addLearnPoint(inst, player, 0, -inst.getMastery());   // fires SKILL_LEARNING + UNLOCK_SKILL + onLearnSkill, but charges learning cost and can be vetoed
    //  (b) inst.setMastery(0);                                                                      // no UNLOCK_SKILL/onLearnSkill
}
inst.addMasteryPoint(player, inst.getMaxMastery() - inst.getMastery());
```

This fires `SKILL_MASTERY`, `onSkillMastered` (evolutions), the `skill_mastered` stat and advancement, the chat line, and `markDirty`.

**B. Fallback if a `SKILL_MASTERY` listener vetoed or scaled it** (check `inst.isMastered(player)` afterwards). This is exactly Great Sage's `AnalysisManager.master` and Tensura's `AbilityMasteryCommand.master`:

```java
boolean was = inst.isMastered(player);
inst.setMastery(inst.getMaxMastery());
if (!was) {
    try { inst.onSkillMastered(player); } catch (RuntimeException ex) { /* Great Sage logs and goes on */ }
    if (inst.getSkill() instanceof TensuraSkill ts && player instanceof ServerPlayer sp) ts.addMasteryStatistic(sp); // stat + advancement (the command does this)
}
SkillAPI.getSkillsFrom(player).markDirty();      // command path
// Great Sage also calls SkillStorage.updateSkill(inst, true) to push the instance to the client
```

Great Sage's version also calls its own `learnModes(player, inst)` afterwards, to open locked modes.

**Known traps:**

- **A raw `setMastery(max)` alone fires nothing.** FoxAblaze's Wisdom Crystal and Better Subs' `grantSkill(..., mastered=true)` do exactly that, so they never evolve anything.
- **`SKILL_MASTERY` has listeners.** Tensura's `AbilityHandler` registers some. Ascension's `ModifierEffects` cancels (`interruptFalse`) when a skill is "sealed" (message `ascension.modifier.msg.sealed`). Ascension skills `AllSeeingTruth`, `EyeOfMind`, `GodOfFortune`, `GreatMage`, `TimelessMage`, `TrashGamer` and `TalentEffects` register mastery hooks. Mysticism's `AbilityHandler` and `SubjugatorHandler` do too. I did not trace what each one rewrites, and I did not verify listener order.
- **`onSkillMastered` is one-shot.** For the Manipulation to Domination row, an EP shortfall at that moment means no Domination, and nothing retries. A rite that masters such a skill below the EP threshold loses the evolution.
- **Ascension System skills are re-synced.** `PlayerSystem.syncMastery` rewrites `the_system`, `shadow_necromancer` and `shadow_monarch` from the player's System level through `setMastery` (constants `400`, `10`, `40`, `600`; `SYSTEM_MAX_LEVEL` config). A forced mastery on these gets overwritten. See 2.1.
- **Max mastery can change.** `getMaxMastery()` is cached per skill object (`Skill.cachedMaxMastery`) and individual skills override it. Compare against `inst.getMaxMastery()` at call time, never a literal.
- **Ascension Ultimates arrive at mastery `-1`** (Rebalanced route text: "grants the Ultimate at mastery -1"). Treat `-1` as learning and use the learning branch above.

---

## 2. Skills that are hard or impossible to master

Method: for every registered skill class (520 across the jars; 355 Tensura including the Arts and Magics, 56 Ascension, 57 Mysticism, 19 Horizons, 14 Kumo, 9 TNO, 7 Slime Throne Extras, 2 Great Sage, 1 Infinity), I walked the class chain for calls named `*astery*` or `*LearnPoint*`, then searched every other class in every jar for a call in a method that references the skill's registry field. "No source" below means **none of that found**. The central per-hit and per-kill handler can still feed a skill that deals ability damage, but none of the skills below deal damage.

### 2.1 No mastery source at all (confirmed after the event-class cross-check)

| Skill id | Type | Reason and evidence | Does it matter? |
|---|---|---|---|
| `tensura:sage` | EXTRA | `SageSkill` has no mastery call, and no other class references `ExtraSkills.SAGE` with one. Max 500, never reached | Sage's own acquisition checks Magic and Battlewill mastered counts, not Sage's mastery. Tensura has no Sage to Great Sage class (verified earlier), so nothing is blocked in Tensura itself |
| `tensura:chant_annulment` | EXTRA | same; `canBeToggled` is `mastery >= 0`, so it works unmastered | cosmetic |
| `tensura:ultra_instinct` | EXTRA | same | cosmetic |
| `tensura:commander` | UNIQUE | same; `canBeToggled` is `mastery >= 0` | cosmetic |
| **`tensura:anti_skill`** | UNIQUE | no source anywhere; **`AntiSkill.canBeToggled` returns `isMastered`**. `ExtraSkills`/`UniqueSkills` references to it are in `Creator`, `Degenerate`, `ResistSkill`, `Titanification`, `Usurper`, TNO `AllRounder`, none of them grants mastery | **toggle is permanently locked** |
| `tensura:godly_craftsman` | UNIQUE | mastery comes only from `ResearcherEnchantingMenu.addSkillMastery` (the Researcher enchanting menu) | needed mastered for Elite's gate |
| `tensura:researcher` | UNIQUE | its only mastery call is `setMastery` inside its own `onSkillMastered`; real gain is the enchanting menu | masters to Godly Craftsman |
| `tensura:spatial_manipulation` | EXTRA | no own call. Indirect: `RequestSpatialActionPacket.handleWarp/handlePortal` add mastery to the *spatial movement* instance (Spatial Motion, Gate, Warp Portal) | works only through warping |
| `greatsage:raphael`, `greatsage:venuzdonoa` | ULTIMATE / UNIQUE | no mastery call in the whole Great Sage jar except `AnalysisManager.master` (which masters the analysed skill, see 4.2) | Great Sage's own Raphael is **never mastered by play** |
| `infinitystones:immortality` | ULTIMATE | no source | n/a |
| `ascension:the_system`, `ascension:shadow_necromancer`, `ascension:shadow_monarch` | UNIQUE/UNIQUE/ULTIMATE | no `addMasteryPoint`; **mastery is rewritten from the System level** by `PlayerSystem.syncMastery`; max overridden (`400`, ?, `100`). Rebalanced text: Monarch "needs levels 101-200 to finish its native mastery progression" | forced mastery is overwritten |
| `kumodesu:spider_movement`, `egg_laying`, `humanification`, `taboo` | INTRINSIC | no source | passive |
| `kumodesu:weapon_creation`, `wondrous_weapon_creation`, `height_of_occultism` | UNIQUE | no source | |
| `mysticism:gravity_flux` | EXTRA | no source | |
| `mysticism:magisteel_body`, `relapse` | INTRINSIC | no source | |
| `mysticism:fourth_wall`, `scholar`, `schrodinger` | UNIQUE | no source | |
| **`mysticism:reducer`** | UNIQUE | no source, but `ReducerPurityEdgeEffect` tests `isMastered` on Reducer | **the mastered bonus is unreachable** |
| `stextras:imitator` | UNIQUE | no source | |
| `horizons:inheritor`, `last_word`, `storyteller` | UNIQUE | no own call; gained through event classes (`InheritorSelectionManager`, `LastWordEvents.onLivingDeath`, `StorytellerEffectEvents` and others), so they are reachable but only through those events | see 2.3 |
| `horizons:composite_body` | INTRINSIC | no source found; its `onSkillMastered` only refreshes toggle state | |
| `tnoextras:looter` | UNIQUE | via `LooterManager.addLooterMasteryPoint` only | reachable, indirect |

### 2.2 Resistances and nullifications (46 registered: 44 Tensura, 2 Ascension)

- **Evidence:** `ResistSkill` (the base class of all 44) has `addLearnPoint` and `setMastery`, and no `addMasteryPoint`. The per-resist subclasses add nothing. After learning ends (mastery caps at 0), **mastery stays at 0 forever**. Max is 100 (type RESISTANCE falls in the `masteryIntrinsic` branch). **No Tensura resistance can ever reach `isMastered`.** Rebalanced's `MixinResistSkill` and `MixinResistSkillEvolve` also read `getMastery`, not `isMastered`.
- **Huge damage:** a resistance needs a hit above its tier's damage, or accumulated damage of `tier * 10`, and 100 / 350 / 500 / 700 learn points.
  - Default tier: a 5.0-damage hit, 100 points.
  - Hard tier (physical, spiritual, abnormal condition, magic): a **20.0**-damage hit, **700** points; summed damage route needs 200 damage per point.
  - Rebalanced paces the points (per player and resistance, minimum ticks between increments).
- **Mastery gain:** none after the first learn, because nothing in the chain calls `addMasteryPoint`. `AbilityMasteryCommand` may set a resistance above 0 (<= 100) manually.
- **Passive Ascension resists:** `ascension:blood_resistance`, `blood_nullification` are in the "rare-only" trigger class (`onTakenDamage`).

### 2.3 Mastery needs a rare condition or state

| Skill | Condition (read) | Evidence |
|---|---|---|
| `kumodesu:determination` (max from override), `perseverance` (200), `resolve` (50) | `canTick` is true only when the skill is toggled **or in a slot**, **and** `getHealth < getMaxHealth * CONFIG.hpMultiplier` (Determination default 0.5). Mastery is added in `onTick` only. **`canBeToggled` returns `isMastered`**, so the toggle stays locked until you have been below half HP while the skill sat in a slot for enough ticks. | `DeterminationSkill.canTick/onTick/canBeToggled`, `PerseveranceSkill.canTick`, `ResolveSkill.canTick`; same shape for the three |
| `tensura:degenerate` | one mastery point per **block converted** in `onPressed` (lambdas `$3`, `$5`), after `SKILL_GRIEF_PRE` is not vetoed. Boss areas and any grief-blocking claim stop it | `DegenerateSkill.lambda$onPressed$3`. **This is the Raphael prerequisite** |
| `tensura:self_regeneration` | the mastery call sits behind a `getHealth < max` branch: only gains while injured | `SelfRegenerationSkill.onTick` (the call follows a `dcmpg ... ifge` health test) |
| `tensura:corrosion`, `poison`, `paralysis` (COMMON), `asc:prickly_hands`, `kumodesu:deadly_poison`, `deadly_spider_poison`, `fatal_spider_poison`, `mysticism:lethal_poison`, `bullet_punch`, `spark`, `tensura:sound_manipulation`, `sound_domination`, `divine_ki_release` | gain only in `onTouchEntity` / `onDamageEntity` / `onTakenDamage`: the skill must be toggled **and land a hit**. Their toggle lang says "toggleable when mastered"; the first mastery comes from press use | `lists.py` "rare-only" class |
| `tensura:fighter`, `tensura:cook` | `onDamageEntity` / `onTouchEntity` only | same |
| `tensura:survivor` | `onTakenDamage` adds learn points; mastery in `onTick` | `SurvivorSkill` |
| `tensura:merciless` | `onHeld` every 60 ticks, and the `isOutOfEnergy` check stops it when out of magicule | `MercilessSkill.onHeld` |
| `tensura:mathematician` | `onTick` only (toggled) | |
| `ascension:blood_manipulation`, `blood_frenzy`, `toxic_skin`, `blood_domination` | `onTick` / `onDamageEntity` / `onBeingDamaged`. Blood Domination's `canUnlock` needs **mastered Blood Manipulation** | `BloodDominationSkill.canUnlock` |
| `tensura:magic_jamming`, `godwolf_sense`, `heavenly_eye`, `magic_sense`, ... (about 45 toggles) | `onTick` while toggled, 1 point per 10 ticks | ordinary |

### 2.4 Mastery gated by a disabled or closed feature

| Feature | Evidence |
|---|---|
| **Great Sage Raphael capstone** (`greatsage:raphael`) | `server.CapstoneGate.OBTAINABLE = false` and `CapstoneGate.available(player)` is `return false`. `lines()` still lists mastered `tensura:great_sage` and mastered `tensura:degenerate` as requirements. (From `plan-19/.../verified-hooks.md`, bytecode, re-read in this session's dump for `CapstoneGate.mastered`.) |
| **Ascension Awakening** | game rule `doAscensionUltimate` (`AddonGameRules.DO_ASCENSION_ULTIMATE`), the altar pattern, a cooldown tag `ascension:last_awakening_tick`, `AWAKENING_MIN_EP` (5,000,000 per the summary), True Hero or True Demon Lord. Each pair needs the Unique **mastered** (`TensuraSkill.isMastered` at `prepare` offset 379). Apocalypse Bird also needs the "Apocalypse Bird" switch in the Ascension skill config. |
| **FoxAblaze Raphael** | `fusionRequireSpecialRace`, single owner (game rule `RAPHAEL_TRULY_UNIQUE`), `requireMasteredRaphael` config for Azathoth |
| **Elite (`elitetensura`) Ultimates** | `requirementsEnabled`; disabling it bypasses the mastery check (Rebalanced route text) |
| **Sage-tree features in Great Sage** | `requireGreatSage` game rule; analysis of resistances is permanently closed (config comment) |

### 2.5 Rebalanced 3.2.17: what it changes about mastery

All read in bytecode and route text; **none of the keys below are in the pack config files I read**, so they run on defaults.

- **`MixinResistanceLearningPacing`** and `ProgressionConfig.resistance_learning` (`intervalTicks`, `maxPointsPerInterval`; defaults look like 20 and 1 from the constant order, inferred): a minimum tick gap between damage-learning points per player and resistance, to stop regeneration or multi-hit farming. "Damage requirements and tier totals remain native."
- **`MixinResistSkill`, `DefenceQuery.isDormantResistance`, `MixinAbilitySelectionScreenHideResistance`:** resistances with `mastery < 0` are "dormant" and hidden in the menu.
- **`resistancesKeepDamageBar`** (`GatesConfig`): a Resistance or Nullification ALWAYS starts at minus its own tier requirement (350 / 500 / 700), whatever mod granted it, so a finished Nullification is not handed over by an addon race line.
- **`LearningMigration`:** if `learningPointRequirement = 0` in `ability_config.toml`, new skills start at mastery 0 and are usable at once, and the migration zeroes old non-resistance skills stuck below 0 ("nothing fills them any more for non-resistances"). **The client instance config I read has `learningPointRequirement = 100`**, so this pack instance is in the other state. Check the real pack config before relying on either.
- **Mastery-bonus caps:** `MixinAllSeeingTruthMasteryCap`, `MixinFortuneMasteryCap`, `MixinBestiaPassiveCaps` cap the *effects* of mastery (`truthMasteryMultiplierBonusCap`, `fortuneFlatMasteryBonusCap`, `bestiaLearningMasteryBonusCap`), not the points.
- **`MixinGravityFlightMastery.getMaxMastery`** changes Gravity Flight's max; `GravityFlight.handlePress` and `MixinBloodFrenzyUpkeep` add their own `addMasteryPoint` (so they fire `SKILL_MASTERY`).
- **`SupersedeEvents.mirrorMastery`:** mirrors a successor's mastery onto the predecessor (Haki and Mortal Fear case, via Tensura's own `addMasteryPoint`, so the hooks run).
- **`MixinUniqueGrantLearning`, `UniqueRerolls.use` (`setMastery`), `BossBalance.apply` (`setMastery`), `PostAwakeningConversion`, `MixinResistSkillEvolve`:** reset or convert mastery on rerolls, boss grants and awakening (resistance to nullification only when `getMastery >= 0`, inferred from the `getMastery` reads).
- **Ascension route text:** Ultimates are granted "at mastery -1", with no operator grant in the route.

---

## 3. Mastery-gated evolutions and unlocks (what a mastery rite would shortcut)

### 3.1 Skill evolutions and fusions

| Result | Requires mastered | Other conditions | Source |
|---|---|---|---|
| **Raphael** (`foxablazeultimate:raphael`) | `tensura:great_sage` + `tensura:degenerate` | Hero Egg / True Hero / Demon Lord Seed / True Demon Lord (`fusionRequireSpecialRace`), single owner, no EP check; automatic in `RaphaelFusionTickHandler` | `RaphaelFusion.tryFuse`, FoxAblaze lang text |
| **Raphael** (`greatsage:raphael`) | `tensura:great_sage` + `tensura:degenerate` | Max EP >= 500,000, Max Magicule >= 50,000, True Hero or True Demon Lord, all growth-tree nodes, **plus `CapstoneGate.available` = false (closed)** | `CapstoneGate.lines` |
| **Beelzebub** | Gluttony + Merciless | race flag | FoxAblaze lang |
| **Uriel** | Fox Raphael owned (need not be mastered) + mastered Infinity Prison | | Rebalanced route |
| **Azathoth** | mastered Fox Raphael (default `requireMasteredRaphael`) | True Hero or True Demon Lord | Rebalanced route |
| **Shub-Niggurath** | mastered Raphael or Ciel, Uriel, Elite Beelzebuth | True Demon Lord, >= 20 mastered non-negative skills, 2,000,000 Max EP, 1,000,000 MP | Rebalanced route |
| **Fortuna** | Mathematician + Usurper | True Hero or True Demon Lord | route |
| **Hell Sniper** | Sniper | race flag | route |
| **Mammon** | Greed | race flag | route |
| **Satanael** | Wrath | an owned mob killed by another entity | route |
| **Samael** | Poison + Merciless (permanent, non-sub-instance) | 700,000 MP | route |
| **Asmodeus** | Lust | | handler fields (`AsmodeusAcquisitionHandler` refs `LUST`) |
| **Veldora** | a wind Manipulation or Domination | | route |
| **Velgrynd** | mastery check present, ids not traced | | `VelgryndAcquisitionHandler` |
| **Ascension Ultimates** (11 pairs, altar + catalyst, 5,000,000 Max EP, True Hero or True Demon Lord, 2 h cooldown) | the Unique: `great_mage`->`timeless_mage` (all Aspectual schools learned), `sealer`->`one_who_seals` (Suppression Stone, 400k), `dragon_slayer`->`slayer_of_dragons` (30 Dragon Essences), `bubble_majin`->`evil_majin` (50 Majin Candies), `imprisoned_jester`->`unbound_jester` (1M spirit damage), `deus_sanguis`->`blood_sovereign`, `eye_of_mind`->`all_seeing_truth`, `king_of_curses`->`ruler_of_death`, `trash_gamer`->`god_of_fortune`, **all three** `punishing_bird`+`judgement_bird`+`big_bird`->`apocalypse_bird`, `shadow_necromancer`->`shadow_monarch` (System level) | gate checked in `AwakeningRitual.prepare` with `TensuraSkill.isMastered`; the ritual consumes the Unique | `ids/ULTIMATES_SUMMARY.txt`, Rebalanced `ultimate_ascension.json` |
| **Blood Domination** | Blood Manipulation | | `BloodDominationSkill.canUnlock` |
| **Elite Tensura** ultimates | Dragon Skin (Crocodile Skin), Gluttony + Merciless (Beelzebuth), Earth Wall + Stone Shot, Godly Craftsman + Creator, Survivor + Gourmet, Great Sage + Godly Craftsman, Voidedge | `requirementsEnabled`, EP and awakening checks | Rebalanced `ultimate_elitetensura.json` |
| **Mysticism** | Mithril Strength <- Steel Strength; Exoskeleton <- Dragon Skin; Lethal Poison <- Poison; Tenacity <- Infinite + Ultraspeed Regeneration; Captivator <- spiritual resist/nullification; Kyurem / Reshiram / Zekrom <- each other and thermal/cold resists; Melancholy <- gravity and molecular manipulation; Domination skills <- Manipulation | | field refs in classes that call `isMastered` (**pairing per skill not traced**) |
| **Slime Throne Extras** | Darkness / Light Domination <- Manipulation; `ElementalSoulRequirements.getMissingRequiredDominations` | | `checkAcquiringRequirement` bytecode |
| **Cultivation Great Sage** | master "Cultivation Great Sage" + watch Neidan | | Rebalanced `update_20261114_mortal_cultivation.json` |

**Predator**, **Analyst**, **Researcher** and the Manipulation line (section 1.4) are the Tensura-native chains. Predator also needs "Starved mastered" in Elite's wording ("own Starved and master Predator, or own Predator when Starved is mastered; implemented in both mastery callbacks").

### 3.2 Toggle and feature unlocks tied to `isMastered`

Tensura: Corrosion, Poison, Paralysis, Steel Strength, Fighter, Cook, Anti Skill, Berserk, Berserker, Chosen One, Envy, Engorger, Gourmand, Gourmet, Black Flame, Dragon Ear, Eye of Truth, Falsifier, Godwolf Sense, all Manipulation and Domination skills and others (`canBeToggled`, 40+). Magic `canLearnSkill` (Fire Ball, Chain Explosion, Full Recovery, Demon Dominate, Demon Marionette, Create Greater Undead, Curse Bind) needs the lesser spell mastered (lang `tensura.skill.learn_points.failed_mastery`: "%s requires mastery over %s to be learnt").
Kumo: Determination, Perseverance, Resolve, Deadly Poison, Deadly Spider Poison, Fatal Spider Poison (`canBeToggled`). Slime Throne Extras: Blacksmith, Darkness Domination. TNO: Seducer, Looter.

### 3.3 Races

`EvolutionRequirement$AbilityRequirement(ManasSkill, boolean mastered)` uses `SkillUtils.isSkillMastered` when the flag is true. Constructed in Tensura (Orc Disaster: mastered `starved`), Ascension (Ender Dragonewt: `space_transform`), Mysticism (about 60 wolf, insect and wyrm races), and Horizons races (`WyvernRace`: Body Armor, Magic Sense, Rapid, Scale Armor, Self and Ultraspeed Regeneration). Per-race flag values were not fully extracted: the extraction script (`evreq.py`) found the constructor calls, but for Mysticism's races it could not recover the literal.

---

## 4. What already exists to buy or force mastery

### 4.1 Nothing sells it in the shop sense. Nearest things, with cost

| Mechanism | Cost and conditions | Hooks fire? |
|---|---|---|
| Tensura `/tensura ability mastery` family (`AbilityMasteryCommand.master`, `masteryAll`, `masteryAllSkill`, `masteryAllMagic`, `masteryAllBattlewill`, `masteryRandom`) | operator command, free; refuses a value above `getMaxMastery` ("mastery.above_max"); toggles off a toggled skill when lowering | **yes**: `SKILL_MASTERY` event, `setMastery`, `addMasteryStatistic`, `onSkillMastered` |
| **Great Sage analysis** (`AnalysisManager.start` / `master`) | time `analysisTicks 1200 * classFactor` (intrinsic 2, common 2, extra 3, unique 5, ultimate 8, so 2 to 8 minutes), **x`latentTimeFactor 3`** if the skill is at negative mastery, **+175% magicule** (`latentMagiculeExtra 0.75`) for the same case, **25% fail** (`analysisFailChance`, magicules lost), + `modeTimeExtra 0.5` per locked mode; needs Great Sage or a Raphael (`requireGreatSage` rule); **resistances are excluded** | **yes**: `setMastery(max)`, `onSkillMastered` (exceptions caught), `learnModes`, `SkillStorage.updateSkill(inst, true)` |
| FoxAblaze **Wisdom Crystal** (item, eaten) | drop only: loot modifiers on `supermassive_slime` (80%), Rimuru ogre fight, Luminous Valentine. Adds a ratio of max mastery (`WISDOM_TARGET_MASTERY_GAIN_RATIO 0.1`, Fortuna 0.05) to the target skill, plus a 12,000-tick buff | **no**: raw `setMastery` (`WisdomCrystalItem.addMasteryByRatio`) |
| Tensura Better Subs `IExpandedSkillAcquisition.grantSkill(entity, skill, mastered)` | the owner pays the skill's `getAcquiringMagiculeCost`; works on **subordinates** | **no**: raw `setMastery(getMaxMastery)` on a default instance |
| Tensura Researcher enchanting menu (`ResearcherEnchantingMenu.addSkillMastery`) | menu action for Researcher / Godly Craftsman | `addMasteryPoint` path |
| Mysticism `BookStorageMenu.upgradeBook` | calls `addMasteryPoint`; book upgrade | `addMasteryPoint` path |
| Rebalanced `UniqueRerolls.use`, `BossBalance.apply` | `setMastery` on reroll items and boss grants | raw |
| Ascension `ImageTrainingSkill` | summon a training clone that ignores resistances; 15-min cooldown; gives a little Max EP; `ImageTrainingClone.grantBaselineSkill` sets baseline mastery on the clone | n/a |

### 4.2 Not mastery sellers

- **Skill Trainer 2.0.5** (`tensura_trainer`, author DevilLord): **client-side AFK auto-trainer only.** `TrainerTicker.clientTick` sends ManasCore `RequestSkillActivationPacket`, `RequestSkillReleasePacket`, `RequestSkillTogglePacket` and Tensura `RequestAbilityModeChangePacket` on a timer; it has a `trainMasteredSkills` switch and an auto-disable on death. No cost, no direct mastery write. It trains only what the skills already pay for.
- **Magic Growth 1.0.6:** environmental magicule growth, magic ore, and mob upgrades. **Nothing on skill mastery.**
- **Skill Obtain 1.1:** `handler.SkillObtain` learns skills on mob death by config chance. No mastery call.
- **Skill Books 2.2.3:** reflection grant helpers that preserve an existing instance's mastery. No mastery sale.

---

## 5. Open items (not verified)

1. What each `SKILL_MASTERY` listener does (Tensura `AbilityHandler`, Ascension `ModifierEffects` / `TalentEffects` / six skill hooks, Mysticism `AbilityHandler` / `SubjugatorHandler`), and their order. Only the Ascension "sealed" veto was read.
2. Whether `SkillHelper.learnSkill(LivingEntity, Inst)` charges the acquiring magicule itself for an evolved learning instance (the plan's open item 1 stays open).
3. The pack's real `learningPointRequirement` (client instance says 100; Rebalanced is written for 0).
4. Per-race `AbilityRequirement` flags for Mysticism, and the per-skill pairing of Mysticism's and Slime Throne Extras' mastery checks.
5. FoxAblaze's Velgrynd and Asmodeus exact requirements (only the referenced skill fields were read).
6. The Horizons skills `inheritor`, `last_word`, `storyteller` are reachable only via event classes; how often those events fire was not measured.
7. No runtime test: every result is from bytecode and config text.

## Appendix: reproducing

- `mastery/jars`: copies of the jars. `mastery/dump_<jar>.txt`: `javap -p -c -constants` of every class. `mastery/table.json`: skill id, class, type, mastery-call methods, overrides. `mastery/classes.json`: all parsed classes.
- Helper scripts: `parse.py`, `registry2.py` (skill id to class via bootstrap methods), `combine.py`, `cond.py` (conditions before a mastery call), `meth.py`, `refs.py`, `gates.py`, `clsfields.py`, `tgates.py`.
