# Part: evolution_rites

**Mixin budget: 0 of 3 used.**

Requires: **vampirism, enchanted and tensura**; needs the `speciesTensura` part (and so `species`). Plan: `docs/superpowers/plans/2026-11-22-plan-19a-evolution-engine.md`; spec: `docs/superpowers/specs/2026-11-22-tensura-evolution-rites-design.md` (rules E1 to E16, decisions D1 to D15). Research and the verified signatures: `docs/superpowers/research/tensura-addons/`. This is phase **19a**: the engine, our own chains and the Rite of Mastery. 19b (Great Sage and FoxAblaze, the Raphael merge), 19c (Ascension's Blood Sovereign) and 19d (the other addons, race links) come later.

A witch-type performer (witch, siphoner, heretic, tribrid) opens a road to a skill that **already exists** (Tensura or an addon) for the player on the circle, who agrees by sneaking (or for herself). Nothing is invented: no skill, race or item of ours. Uniques and below need a full-moon night, an Ultimate a lunar eclipse. Every refusal about the target goes through `RiteTargets.refuseAbout`: the performer hears "The rite would change nothing." whatever the target is (E5).

## What 19a ships

- [x] The part, `supernaturalbloodlines-evolution_rites.toml`, 15 Admin rows, the part switch row.
- [x] The engine: `core.evolution` (`Tier`, `EvolutionRecipe`, `EvolutionRecipeCodec`, `EvolutionRules`, `MasteryRules`: pure), `parts.evolutionrites.rite.EvolutionRiteFactory` (factory `supernaturalbloodlines:evolution`: the rite file's `factory` block is the recipe), `probe/` (`AddonProbe`, `Hooks`, `Probes`, `UniqueRoadProbe`).
- [x] **Sage to Great Sage** (`evolve_great_sage`, Unique, full moon): needs a mastered `tensura:sage`; Tensura's own `SkillHelper.learnSkill`, so its price and vetoes apply; Sage stays; `ep_cost` 100,000 of base Max EP, spent only after the learn is confirmed.
- [x] **Deus Sanguis** (`evolve_deus_sanguis`, Unique, full moon, only with Ascension): the target is vampire-kind; the addon's own `AddonSkillConfig.DEUS_SANGUIS_ENABLED` switch decides whether the road is open; `ep_cost` 200,000. Its rite file and its two unlock advancements carry `neoforge:mod_loaded ascension`, so without the addon there is no rite, nothing known, nothing sealed, and no book entry.
- [x] **The Rite of Mastery** (four rite files `mastery_common|extra|unique|ultimate`, factory `supernaturalbloodlines:mastery`): masters the one skill the target has chosen, of the file's tier. EP is spent last. See "Rite of Mastery".
- [x] Book: category "Evolution Rites", six sealed entries unlocked by kind (witch, siphoner, tribrid; a vampire also learns Deus Sanguis).
- [x] 19b: the Raphael merge, the dormant Raphael rite, free grants and the retired-skill check.
- [ ] 19c: `blood_sovereign`, `GrantForm.PERMANENT`. 19d: the other Uniques and the race links.

## The Raphael merge (19b)

With FoxAblaze installed, an `UNLOCK_SKILL` listener redirects Great Sage's Raphael to FoxAblaze's. Either Raphael counts as held; Great Sage's own gates already accept both. The redirect respects `raphaelTrulyUnique` and checks the owners of both Raphael and Ciel; FoxAblaze registers the owner when its skill is learned. Login converts an existing Great Sage Raphael only after the replacement is learned. No mixin is used.

Great Sage's own evolution forgets Great Sage and Degenerate before asking to learn Raphael. On a refused redirect, the merge tries to restore any missing prerequisites at default mastery. The owner accepted the loss of their old mastery. Other addons can still veto a restore or stop the original learn before the redirect runs.

The Rite of Raphael stays closed while Great Sage's `CapstoneGate.OBTAINABLE` is false (including 0.0.7.1). When the road opens, the rite still checks Great Sage's own eligibility gate. It takes mastered Great Sage and Degenerate only after Raphael is learned.

## The performer who left (E5, plan 19a panel)

Enchanted hands a rite only the performer's id. Nine factories (`Faction`, `BindItem`, `Lineage`, `Severance`, `HybridBlood`, `AncestralCalling`, `OriginalBlood`, `Evolution`, `Mastery`) used "the caster, or else the one on the circle", so a performer who logged out left the target standing in for them, and a rite that asks the target's consent only when performer and target differ then changed an unwilling player. They now call `RiteTargets.performer` (pure rule: `core.rite.PerformerRule`): the performer if present, else the target ONLY when the target is the performer, else the rite cancels (which gives the ingredients back). `CasterFallbackScanTest` fails if any `*RiteFactory` brings the old line back; GameTests cover the Augustine, Great Sage and Mastery rites with a witch target who does not sneak.

## Rite of Mastery (E16)

| Tier | Night | EP spent (default) | Sacrifice (default; a config list per tier) |
|---|---|---|---|
| Common (a resistance, an intrinsic, a common skill) | full moon | 100,000 | 5 diamond blocks, 5 echo shards |
| Extra | full moon | 300,000 | 5 netherite ingots, 20 echo shards |
| Unique | full moon | 750,000 | 5 nether stars, 5 netherite ingots |
| Ultimate | lunar eclipse | 2,000,000 | 5 nether stars, 5 dragon's breath, 10 netherite ingots |

- "EP" is the target's **base Max EP** (`EnergyHelper.getBaseMaxEP`, `setBaseMaxEP`). The rite refuses before taking anything unless the base Max EP is strictly more than the cost. The EP is taken after the mastery is confirmed, so a refusal or a veto costs nothing.
- **Choosing the skill:** the hub's Skills page has a **Mastery** tab (shown when this part is installed) that lists the player's skills with their tier and Choose, Clear and Show buttons; they run `bloodline mastery choose <skill>`, `clear` and (no argument) show (rows B11 to B13 of `AdminCommands`). The server validates the choice when it is made and again when the rite runs. There is no default skill. No payload and no protocol change: the tab uses the hub's command rows. The supernatural part reaches the picker only through `shared.hub.MasteryAccess` (parts never import each other); this part installs the provider on the client.
- **How mastery is applied** (`MasteryApply`): `setMastery(0)` if the skill is still in learning, then `addMasteryPoint(max - current)`, so `SKILL_MASTERY`, `onSkillMastered` (a skill's own evolution), the statistic and the advancement all happen. If afterwards the skill is not mastered (a listener vetoed it, as Ascension's sealed skills do) the old mastery is restored and the rite is refused: the research's "set it anyway" fallback is **not** used. A **resistance** has no mastery: the rite finishes its learning (Tensura's own `addLearnPoint(inst, target, 0, points)`: the same path as the learning points in play, which fires `SkillLearningEvent` and `UNLOCK_SKILL` (so a veto applies) and runs `onLearnSkill`, the toggle and the chat line. A veto at either restores the old mastery AND the base Max EP Tensura's own listener may already have charged).
- **Refused, to the target, with the reason:** no choice, not held, lent (temporary), already mastered or finished, the wrong tier for this file, in `mastery_refused_skills` (default: Ascension's System skills, whose mastery that addon rewrites from the System level), in Rebalanced's `tensura_rebalanced:retired` tag, not enough EP, on cooldown, vetoed. The performer who is not the target hears "The rite would change nothing." for all of them.
- **The sacrifice is config.** The four rite files name only the item **kinds** (Enchanted needs each kind thrown on the circle to start the rite, and takes one stack of each); `mastery_sacrifice_<tier>` (`item:count` lines) says how many. In `onStart`, before anything about the target is read, the rite adds what Enchanted took to what lies loose on the circle (within 8 blocks, `Sacrifice.plan`): if that is short of the config it cancels and tells **only the performer** (nothing about the target; Enchanted gives back what it took, the loose items are never touched); if Enchanted took more than the count, the surplus is dropped back on the circle. A kind the rite file names but the config does not list counts as 1.
- **Cooldown:** one rite per target per `mastery_cooldown_days` (7) of game time (24,000 ticks a day, `getGameTime`), set only after a success.
- The point of it: `tensura:sage` has no mastery source at all, so Sage to Great Sage was unreachable; `tensura:anti_skill`'s toggle needs it mastered and nothing masters it; a resistance stops at 0 forever; Kumo's Determination only gains while the player is below half health.

## Settings (`supernaturalbloodlines-evolution_rites.toml`, every key has an Admin row, section "Evolution rites")

| Key | Default | Meaning |
|---|---|---|
| `rites_enabled` | true | all evolution rites; off: they say they are closed |
| `ultimate_rites_enabled` | true | the eclipse rites |
| `disabled_recipes` | empty | recipe ids that are off (for example `great_sage`) |
| `announce_ultimates` | false | one server line when an Ultimate is granted (names the player) |
| `mastery_enabled` | true | the Rite of Mastery |
| `mastery_ep_common`, `_extra`, `_unique`, `_ultimate` | 100000, 300000, 750000, 2000000 | base Max EP each tier spends |
| `mastery_cooldown_days` | 7 | days between two rites for one player |
| `mastery_refused_skills` | the three Ascension System skills | skill ids the rite refuses |
| `mastery_sacrifice_<tier>` (4 keys) | the owner's counts above | `item:count` lines taken from the circle |

The item counts are config too (`mastery_sacrifice_common|extra|unique|ultimate`, each with an Admin row). A server that wants a new KIND (not just a count) must also change the rite file by datapack: the circle only starts for the kinds the file names. `EvolutionRitesDataTest` fails if a rite file and the default sacrifice name different kinds.

## What vanishes with the part off

The six rite files are conditioned on `part_enabled evolutionRites` (and Deus Sanguis on `mod_loaded ascension`), and so are the 12 unlock advancements (the book generator copies each rite's conditions onto them): with the part off there is no rite, nothing known or sealed, and the book shows neither entry. `RiteUnlocks` only counts rites whose advancements are in the datapacks (`RiteUnlocks.present`), so the species part's own checks, `/bloodline rites` and the fallback that teaches everything are unaffected. The hub's Mastery tab appears only when the part's client installs `MasteryAccess`. A skill already granted or mastered stays: it is an ordinary Tensura skill (rolling back loses nothing).

## Verified hooks used (Tensura 2.0.1.5, ManasCore skill 4.0.0.2)

`SkillHelper.learnSkill(LivingEntity, ManasSkill)`, `SkillUtils.hasSkill/isSkillMastered`, `EnergyHelper.getBaseMaxEP/setBaseMaxEP`, `Skill.getType()`, `ManasSkillInstance.isMastered/addMasteryPoint(LivingEntity,double)/setMastery/onLearnSkill/isTemporarySkill`, `SkillAPI.getSkillsFrom/getSkillRegistry/getSkillRegistryKey`, `SkillEvents.SKILL_MASTERY`, Ascension 3.1.0 `com.simplygray.tensuraaddon.AddonSkillConfig.DEUS_SANGUIS_ENABLED`. `EvolutionHookProbeGameTests.everyListedHookResolves` looks each up on the run classpath and names a missing one.

**Not verified at runtime:** the `tensura_rebalanced:retired` tag lookup (the pack's Rebalanced is not loadable in the dev run; the code never throws and treats an absent registry or tag as "not retired"); Skill Books' price hooks on `learnSkill`; Tensura's own price for Great Sage is read by a control learn in the test, not copied.

## Manual checks (not claimed by tests)

A real eclipse rite on a client with a real addon; the look of the six book entries and the Mastery tab; Skill Books pricing on `great_sage`; Ascension's Deus Sanguis in a real server with the jar.

## Found while building (19a), for the next phases

- **Mysticism closes Great Sage's learn.** `mysticism.handler.AbilityHandler` listens to `SkillEvents.UNLOCK_SKILL` and returns `interruptFalse` (message `mysticism.soulEnergy.no_skill`) when the player's Soul Energy cannot pay for any new skill; `RestrictedHandler` does the same for the "restricted learnable" tag from Max Aura. The rite keeps that rule (E1: the addon's own requirement), so a vetoed learn is refused, costs nothing, and the target is told (`not_ready`). The Mastery of Sage still works; the learn is up to Mysticism. The GameTests learn a control skill first (`EvoTestSupport.learnOpen`) and expect the refusal where Mysticism is installed.
- **FoxAblaze's single-owner rule is the game rule `raphaelTrulyUnique`** (one per ultimate: `beelzebubTrulyUnique`, `veldoraTrulyUnique`, ...), read from `FoxAblazeGameRules`; the owner list is Tensura's `ITrulyUnique` (`TensuraStorages.getUniqueStorageFrom(level)`). Verified in `UltimateSkillProtector` bytecode; 19b reads it.
- **Ascension's jar `ascension-3.1.0.jar` declares mod version 1.0.0** in its `neoforge.mods.toml` (the dependency block shows `ascensioncore` 1.0.0). `LinkedMods` tests against what the loader reports.
- **Tensura Trainer 2.0.5 crashes a dedicated server** on mod construction (`com.antigravity.tensuratrainer.NeoForgeTrainerClient` loads a client class: "Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER"). Not ours; a pack must not put it on a server. The boot with the pack set therefore leaves it out.
- `-PpartsOff=` feeds only `runGameTestServer`. A dedicated-server boot with the part off needs `evolutionRites = false` written to `run/config/supernaturalbloodlines-common.toml` by hand (restore it after).
- Gradle's `runServer` does not forward stdin: drive the console over RCON (`enable-rcon=true`).

## Booting with the addon jars

`./gradlew runGameTestServer -PaddonJars=<folder>` puts every jar of the folder on the run classpath (`docs/compat/tensura-addons.md`). The research lists the jars and their SHA-256 (`docs/superpowers/research/tensura-addons/README.md`). Pass: it reaches "Done" (dedicated server, `runServer`), the log has `EvolutionRitesPart enabled`, no `Registry loading errors`, `Unknown type supernaturalbloodlines:evolution` or stack trace from this mod.
