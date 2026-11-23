# Verified hooks (plan 19)

Each row was read in the class file with `javap -p -c -constants` (bytecode), in the jar and version named. "Read" means the call or constant is in the bytecode; nothing here was run in a game. A hook with a version change must be read again: the plan's first task of each phase re-runs the checks in this file against the jar in the Gradle cache (see "How the plan re-checks").

Short names: `Skill` = `io.github.manasmods.manascore.skill.api.ManasSkill`, `Inst` = `ManasSkillInstance`, `SS` = `io.github.manasmods.manascore.skill.impl.SkillStorage` (from `SkillAPI.getSkillsFrom(LivingEntity)`), `IExistence` = `io.github.manasmods.tensura.storage.ep.IExistence`.

## Tensura 2.0.1.5 (`tensura-neoforge-2.0.1.5.jar`)

| Hook | Signature | Notes |
|---|---|---|
| Learn a skill (the API every addon uses) | `io.github.manasmods.tensura.ability.SkillHelper`: `static boolean learnSkill(LivingEntity, Skill)`; `(LivingEntity, Skill, int)`; `(LivingEntity, Skill, int, MutableComponent)`; `(LivingEntity, Inst)`; `(LivingEntity, Inst, int)`; `(LivingEntity, Inst, int, MutableComponent)` | The `int` is the instance's remove time (`-1` = permanent; Ascension passes `-1`). The `Skill` forms call `createDefaultInstance()` and the instance form. Inside it, fires `SkillEvents.UNLOCK_SKILL` (a listener may veto: that is where Tensura's price/veto and the pack's Skill Books hooks act), reads the instance tag `NoMagiculeCost`, and calls `onLearnSkill`. Returns whether the skill was learned. |
| The free grant the addons use | `TensuraSkillInstance(Skill)` -> `getOrCreateTag().putBoolean("NoMagiculeCost", true)` -> `SkillHelper.learnSkill(LivingEntity, Inst)` | Great Sage (`RaphaelEvolutionService.finish`, `SkillGift.grant`) and FoxAblaze (`RaphaelFusion.doGrant`, also tag `GiftedInstance`) do exactly this. Ours: `shared.tensura.SkillOwners.grant(ServerPlayer, Skill)` (ManasCore's `SkillStorage.learnSkill`, `NoMagiculeCost` plus our `supernaturalbloodlines:granted`). |
| Forget a skill | `SS.forgetSkill(ResourceLocation)`, `SS.forgetSkill(ResourceLocation, MutableComponent)`, `SS.forgetSkill(Skill)` | Ascension: `(id, null)`; Great Sage: `(id)`; FoxAblaze: `(Skill)`. |
| Hold / master | `io.github.manasmods.tensura.ability.SkillUtils.hasSkill(LivingEntity, Skill)`, `SkillUtils.isSkillMastered(LivingEntity, Skill)`; `TensuraSkill.isMastered(Inst, LivingEntity)`; `Inst.isMastered(LivingEntity)`; `Inst.getMastery()` | FoxAblaze uses `SkillUtils`, Great Sage `Inst.isMastered`, Ascension `TensuraSkill.isMastered`. |
| True Hero / True Demon Lord | `TensuraStorages.getExistenceFrom(LivingEntity): IExistence`; `IExistence.isTrueHero()`, `isTrueDemonLord()`, `isHeroEgg()`, `isDemonLordSeed()`, `isBlessed()`, `getMagicule()`, `getEP()`, `getSoulPoints()` | All booleans, no event needed. A race flag, not a race id (the Rebalanced notes say the same). |
| Max EP and Max Magicule | `io.github.manasmods.tensura.util.EnergyHelper.getMaxEP(LivingEntity): double`, `getMaxMagicule(LivingEntity): double` | What Great Sage, Ascension and Horizons compare. |
| Truly unique (single owner) registry | `TensuraStorages.getUniqueStorageFrom(Level): io.github.manasmods.tensura.storage.unique.ITrulyUnique`; `ITrulyUnique.hasSkill(ResourceLocation): boolean`, `ITrulyUnique.getOwner(ResourceLocation): UUID` (nullable) | FoxAblaze reads it on `MinecraftServer.overworld()`. |
| Skill type | `io.github.manasmods.tensura.ability.skill.Skill.getType(): Skill$SkillType` (`EXTRA`, `UNIQUE`, `ULTIMATE`, ...); a skill class may pass several types (see `ids/skilltypes.txt`) | `GreatSageSkill` is a `UNIQUE`; `SageSkill` is an `EXTRA`. |
| Sage and Great Sage | `ability.skill.extra.SageSkill.checkAcquiringRequirement(Player, double)`: needs the custom stats `TensuraStats.MAGIC_MASTERED` and `BATTLEWILL_MASTERED` and `ExtraSkillConfig$Sage.abilityMastered` against the mastery. `ability.skill.unique.GreatSageSkill` has the config `UniqueSkillConfig$GreatSage` (`mpAcquirement` default 75000.0, `masteryPoint` 9.0) and no `checkAcquiringRequirement` of its own. | **No class of the jar turns Sage into Great Sage**: only `UniqueSkills`, `GreatSageSkill`, `ReincarnationConfig$Skills`, two screens and the language provider mention `great_sage`. Great Sage is a reincarnation-pool unique in Tensura 2.0.1.5. |
| Race evolution requirement (for the race links) | `io.github.manasmods.tensura.race.template.EvolutionRequirement` with nested `AbilityRequirement`, `AwakenRequirement`, `BossRequirement`, `EPRequirement`, `ItemCarryingRequirement`, `ItemConsumeRequirement`, `NamedRequirement`, `PhysicalBodyRequirement` | Not used by the skill rites. |
| Race tags | ManasCore race tags `tensura:undead`, `tensura:daemon`, `tensura:beastfolk` (ours: `shared.tensura.RaceTags`) | Unchanged. |

## Great Sage 0.0.7.1 (`greatsage-0.0.7.1.jar`, package `dev.dedek.greatsage`)

| Hook | Signature | Notes |
|---|---|---|
| Raphael's gate | `server.CapstoneGate`: `static List<String> lines(ServerPlayer)`, `static boolean eligible(ServerPlayer)`, `static boolean available(ServerPlayer)`; constants `OBTAINABLE = false`, `EP_REQUIRED = 500000.0`, `MAGICULE_REQUIRED = 50000.0` | `lines` builds `"name|have|need"` strings: Max EP, Max Magicule, mastered `tensura:great_sage`, mastered `tensura:degenerate`, True Hero or True Demon Lord (`IExistence`), and the count of unlocked payable `SageNode`s. `eligible` is true when no line is short. **`available` is `return false`.** |
| Who calls it | `SageProgress` calls `CapstoneGate.available(player)`, then `RaphaelEvolutionService.begin(player)` | Read in `SageProgress`'s constants. |
| The evolution | `server.RaphaelEvolutionService.begin(ServerPlayer)`; `tick`, `witness`, `running`, `finish` | `begin` has **no** eligibility check: it returns if the player is already evolving or `GreatSageGameRules.holdsRaphael`, grants `tensura:great_sage` and `tensura:degenerate` if missing, and starts a 1270-tick ritual. `finish` forgets both (`SS.forgetSkill(ResourceLocation)`) and learns `greatsage:raphael` (free instance, `NoMagiculeCost`). |
| Either Raphael counts | `GreatSageGameRules.ADDON_RAPHAELS` = [`greatsage:raphael`, `foxablazeultimate:raphael`]; `static boolean holdsRaphael(LivingEntity)` | Checks both. Great Sage keeps no owner registry (no `ITrulyUnique` reference anywhere in the jar). |
| Game rules | `GreatSageGameRules`: `requireGreatSage`, `sageProgression`, `sageInterface`, `sageShaderRendering`, `sageReincarnation`, `worldVoice`, `announcementsRequireGreatSage` | `NEED_GREAT_SAGE`, `REQUIRE_GREAT_SAGE`, `SAGE_PROGRESSION`, ... keys. |
| Great Sage growth | `SageNode` enum (`GREAT_SAGE`, `SLOT_2..4`, `BUDGET_1..5`, `RANK_*`, `REACH_*`, `CEILING_*`, `STOMACH_*`, `NERVE_*`, `PRECISION_*`, `RECOVERY`, `CAPSTONE`); `SageProgress.unlocked(ServerPlayer): Set<String>` | The tree does not grant `tensura:great_sage`; it needs it (`requireGreatSage`). |

## FoxAblaze 1.0.6.8 (`foxablaze-1.0.6.8.jar`, package `com.foxablazeultimate`)

| Hook | Signature | Notes |
|---|---|---|
| Raphael fusion | `ability.skill.raphael.RaphaelFusion.tryFuse(LivingEntity)`, called from `RaphaelFusionTickHandler` on `PlayerTickEvent.Post` | Aborts unless: the player holds neither `foxablazeultimate:raphael` nor `ciel` and `CielState.isAwakened` is false; `SkillUtils.isSkillMastered` for `tensura:great_sage` and `tensura:degenerate`; `isEligibleRace`; and not `isOwnedByOther` when the rule is on. Then a ceremony (`PENDING`) and `doGrant` (forget both, learn a free instance with tags `NoMagiculeCost` and `GiftedInstance`). **No EP check.** The persistent-data flag `FoxAblazeUltimateRaphaelInactivityReclaimed` can also stop it. |
| Race flag | `RaphaelFusion.isEligibleRace` (private): true when `RaphaelConfig.Raphael.fusionRequireSpecialRace` is false; else `IExistence.isHeroEgg() \|\| isTrueHero() \|\| isDemonLordSeed() \|\| isTrueDemonLord()` | Read. |
| **Single-owner check** | game rule `FoxAblazeGameRules.RAPHAEL_TRULY_UNIQUE` (read on `player.level().getGameRules()`); `isOwnedByOther(ServerPlayer, ResourceLocation)` = `ITrulyUnique` of `server.overworld()`: `hasSkill(id)` and `getOwner(id) != null` and the owner is not this player, for the Raphael id **or the Ciel id** | The Raphael/Ciel family is one slot. |
| Other acquisitions | event or tick handlers: `AsmodeusAcquisitionHandler`, `AzathothAcquisitionHandler`, `FortunaAcquisitionHandler`, `HellSniperAcquisitionHandler`, `MammonAcquisitionHandler`, `SamaelAcquisitionHandler`, `SatanaelAcquisitionHandler`, `VeldoraAcquisitionHandler`, `VelgryndAcquisitionHandler`, `BeelzebubFusionTickHandler`, `CielAwakeningHandler`, `ShubNiggurathEvolution`, `YogSothothEvolution` | Class names and the Rebalanced route text only; the handler bodies were **not** read, except Raphael's. Uriel and Shub are Raphael Synthesise-mode evolutions (route text). |
| Other config | one `config.*Config` class per skill (`RaphaelConfig`, `CielConfig`, ...), `FoxAblazeUltimateConfig` | `RaphaelConfig.Raphael` has `fusionRequireSpecialRace`, `fusionDeclarationDelayTicks`, `fusionGrantDelayTicks`, `fusionGrantMastery`. |

## Ascension 3.1.0 (`ascension-3.1.0.jar`, package `com.simplygray.tensuraaddon`)

| Hook | Signature | Notes |
|---|---|---|
| The road | `handler.AwakeningRitual.prepare(AwakeningAltarBlockEntity, ServerPlayer): Prepared` and `complete(AwakeningAltarBlockEntity, ServerPlayer, Prepared \| List<Prepared>)` | Both need the altar block entity (level, position, effects), so a rite cannot call them. |
| The table | `AwakeningRitual.AWAKENINGS` (private static `List<Awakening>`); `Awakening` has `requiredUnique`, `grantedUltimate` (both `ResourceLocation`), `label`, `extraGate: Predicate<ServerPlayer>`, `extraGateMsgKey`, `onPrepareSuccess: Consumer` | Read in `<clinit>`; the 11 pairs are in `ids/ULTIMATES_SUMMARY.txt`. |
| What `prepare` checks (in order) | game rule `AddonGameRules.DO_ASCENSION_ULTIMATE` (`ascension.awakening.disabled_by_gamerule`); the altar pattern (`bad_pattern`); the cooldown `AddonConfig.AWAKENING_COOLDOWN_MINUTES` against `ascension:last_awakening_tick`; `EnergyHelper.getMaxEP >= AddonConfig.AWAKENING_MIN_EP` (`requires_ep`); `IExistence.isTrueDemonLord() \|\| isTrueHero()` (`requires_status`); `SkillUtils.hasSkill(requiredUnique)` and `TensuraSkill.isMastered`; the per-awakening `extraGate`; the ultimate not held | The ultimate catalyst: `AwakeningRitual.isCatalyst(ItemStack)`, item `AddonItems.ULTIMATE_CATALYST`. |
| What `complete` does | `SkillHelper.learnSkill(player, ultimate, -1)`; `SS.forgetSkill(requiredUnique, null)`; Apocalypse Bird also forgets the other two birds; EP bonus (`AddonConfig.AWAKENING_BONUS`, `EpUtil.grantMaxEp`), witness bonus, the cooldown tag | Read. |
| Public gate helpers (static, public) | `hasSuppressionStoneOver400k(ServerPlayer)`, `consumeSuppressionStoneOver400k`, `hasEatenEnoughMajinCandies`, `hasReachedUnboundJesterThreshold`, `hasDragonEssences30`, `consumeDragonEssences30`, `hasAllAspectualMagics`, `isCatalyst(ItemStack)`, `forcedAwakenings(ServerPlayer): List<Prepared>` (every pair whose Unique is held and ultimate is not), `describeProgress(ServerPlayer, ResourceLocation): List<Component>`, `getRegisteredUltimateIds()` | `hasOtherBirdsMastered` is private. Shadow Monarch's "level 40 job" gate is in the table's lambda and was not traced. |
| **Deus Sanguis** | `skill.DeusSanguisSkill` (`UNIQUE`); config `AddonSkillConfig.DEUS_SANGUIS_ENABLED` (and `DEUS_SANGUIS_*` tunables); listed in `AddonConfig.ADDITIONAL_UNIQUE_SKILLS` (a `defineListAllowEmpty` of 9 Unique ids, the reincarnation pool) and in the Codex | **No acquisition code besides the reincarnation pool** (`mixin.MixinReincarnationMenu`). It is not granted by the Bloodfiend races. Its Ultimate: `ascension:deus_sanguis -> ascension:blood_sovereign`. |
| Races | `race.bloodfiend`: `FledglingBloodfiendRace`, `KindredBloodfiendRace`, `ElderBloodfiendRace`, `ProgenitorBloodfiendRace`, `BloodNobleRace`; mixins `MixinBeastfolkRace`, `MixinDragonewtRace`, `MixinWightRace` | Beastfolk is mixed into (adds an evolution); the id of the added one was not extracted (19d task 1). |

## Mysticism 2.2.1, Horizons 0.31, Kumo Desu ga 2.0.3-HF2, Slime Throne Extras 2.1.3 / 2.1.4

| Addon | Read | Result |
|---|---|---|
| Mysticism | `ability.skill.unique.ButcherSkill` (extends Tensura's `Skill`, `UNIQUE`), `ability.skill.ultimate.EmbryoSkill` (extends `MysticSkill`), `ability.SkillEvolutionRequirement` (`getProgress`, `getRequirementComponent`), class list | Uniques have no acquisition code of their own; `MixinReincarnationMenu` adds them to the pool. Embryo: the way to get it was **not found** (it has no handler class). |
| Horizons | `awakening.SovereignAwakening.tryAwaken(ServerPlayer)`, `skill.SkillAcquisitionPolicy.blocked(ResourceLocation)` | `tryAwaken` is a race awakening: needs a Demon Lord Seed or Hero Egg (`IExistence`), Max EP, aura/magicule percentages (0.9 of 100.0 constants), grants `tensurahorizons:true_sovereign` advancement and a Sovereign's Authority skill with `SkillHelper.learnSkill(LivingEntity, Inst, int, MutableComponent)`. Not a skill road for our rites. |
| Kumo Desu ga | class list only | Only `mixin.ReincarnationScreenAccessor` touches reincarnation. No evolution/ritual classes. |
| Slime Throne Extras | class list only | `mixin.tensura.menu.ReincarnationMenuMixin`, `net.C2S.SkillUnlockPacket`, FTB Quests `ReincarnationEssenceReward`. |

## The skill-acquisition addons (pack)

See the table in [README.md](README.md). Verified: Skill Obtain's handler (`handler.SkillObtain.init`: `EntityEvent.LIVING_DEATH` + config list `skillObtain` of `mob|skill,...|chance` -> `SkillHelper.learnSkill(LivingEntity, Skill)`); Skill Books' transformer class list and that its grants go by reflection through `SkillHelper`/`SkillAPI` (`SkillGrantCompat`, `SkillGrantFlow`) and its price patches by ASM (`SkillCostTransformer.patchNative`, `patchMysticism`). The Rebalanced `skill_numbers_obtain/ultimate_*.json` route tables were read for Great Sage, FoxAblaze and Ascension.

## Not verified (open)

1. Whether `SkillHelper.learnSkill(LivingEntity, Skill)` charges Tensura's acquiring magicule price itself or leaves it to an `UNLOCK_SKILL` listener, and the exact rule (`mpAcquirement` 75000.0 for Great Sage): the call chain was read only down to `SkillEvents.UNLOCK_SKILL`. It decides the price of the Sage -> Great Sage rite (19a task 1 runs it in a GameTest).
2. The bodies of FoxAblaze's other handlers (only Raphael's was read), and Satanael's/Veldora's "kill" triggers.
3. Ascension's Shadow Monarch gate ("The System", level 40 job), the private `hasOtherBirdsMastered`, the exact `Prepared`/`Awakening` field types beyond those in the constructor call, and what `EpUtil.grantMaxEp` does to a target that did not use the altar.
4. How Ascension's `MixinBeastfolkRace` adds its evolution (id), and Mysticism's wolf evolution ids (`direwolf`, spirit wolves) as `ManasRace.getNextEvolutions` lists: needed for the race links (19d).
5. How Mysticism's `mysticism:embryo` is obtained.
6. Everything at runtime: no addon was launched; the jar reads say what the code does, a real server boot with the jars is a plan task.
7. Skill Trainer in depth, and Infinity Stones, Re:Zero, Magicule Engineering and NEB (hashed only).
8. Rebalanced 3.2.17's rest: the nested rebalance jars other than Great Sage, FoxAblaze, Ascension, Mysticism and core, and the effect of `tensura_rebalance_core` on `SkillHelper.learnSkill`.

## How the plan re-checks

19a task 1 ("hook probe") is a GameTest class that, with the jars on the run classpath, resolves each signature above by reflection and fails with the missing name. It is the executable form of this file for the hooks the plan uses.
