# Tensura addons: research for plan 19 (evolution rites)

Researched 2026-11-22 for `docs/superpowers/specs/2026-11-22-tensura-evolution-rites-design.md`. Nothing here is built. No jar and no extracted or decompiled code is in the repo: this folder holds id lists, the jar versions with their SHA-256, and notes written in our own words. The signatures in [verified-hooks.md](verified-hooks.md) were read with `javap -p -c -constants` from the jars listed below.

## Jars read

Two places: the research folder `%TEMP%\opencode\tensura-addons\` (call it **R**) and the owner's CurseForge pack `OFA - Holy Packs (Client)\mods` (call it **P**; read-only, jars were copied out, never written into). A jar with the same name in both places has the same hash.

| Jar | Mod id | Version | Where | SHA-256 |
|---|---|---|---|---|
| `tensura-neoforge-2.0.1.5.jar` | `tensura` | 2.0.1.5 (the version this mod pins, memory #736) | P | `e89a4c8d3c67082ce4c21e8f0bf53098cf4cccf1b26499aa1ebb7b38b5540549` |
| `ascension-3.1.0.jar` | `ascension` (package `com.simplygray.tensuraaddon`) | 3.1.0 | R, P | `3cb52c66568101c971154ab6a63591605a1cf96ab657743d38ab3958aec8213e` |
| `ascensioncore-1.0.0.jar` | `ascensioncore` | 1.0.0 | P | `6c604f25303b35202a0159e84ee8016ae156e306a6c9cd1f9acf1ca3d5e45c59` |
| `greatsage-0.0.7.1.jar` | `greatsage` (package `dev.dedek.greatsage`) | 0.0.7.1 | R, P | `0858b617197f1c71d6abce85973a0240cf2745250a3dae33bf0369498bea7061` |
| `foxablaze-1.0.6.8.jar` | `foxablazeultimate` (package `com.foxablazeultimate`) | 1.0.6.8 | R, P | `e15488b5ceffaf0964b64cce51c7ce152b101ed92eef638f57e52a61d5b89d84` |
| `mysticism-2.2.1.jar` / `mysticism-neoforge-2.2.1.jar` | `mysticism` (package `io.github.Memoires.mysticism`) | 2.2.1 | R, P | `61e98fe4666cb2baa35396dca7d4e1bb7e41069f2ddfc4af9799b2bb5eb442d3` |
| `horizons-0.31.jar` / `Tensura_Horizons-0.31.jar` | `tensurahorizons` (package `com.scyvix.tensurahorizons`) | 0.31 | R, P | `1c761a2bc36ace5eb64b2b9fe35bc1250d7bfae55f58848232f5bc98181f9ae8` |
| `kumodesu-2.0.3-HF2.jar` / `tensura_kumodesu-neoforge-2.0.3-1.21.1-HOTFIX2.jar` | `tensura_kumodesu` | 2.0.3-1.21.1-HOTFIX2 | R, P | `55ab3e756a53ca4c2b20d4a5cda24868a844bffb58a3fa27d0e8c1cd1cca373f` |
| `slimethrone-2.1.3.jar` | `stextras` (Slime Throne Extras) | 2.1.3 | R only | `bb0e12f4b1dc19352988993a6b0b332b8244f36f912b385874802f33530be10c` |
| `SlimeThroneExtras-neoforge-2.1.4.jar` | `stextras` | 2.1.4 | P only | `4267665f83a03a6427af356b7cd956e795a61526fec8ecd0a7701f705c0a2649` |
| `tensura_skillobtain-neoforge-1.1.jar` | `tensura_skillobtain` (Skill Obtain) | 1.1 | P | `5bc6999d396fe767d6eb235fb4aa0bb32aad9793ed83c4cafebc410791c6e5ef` |
| `tensura_skill_books-neoforge-2.2.3.jar` | `tensura_skill_books` | 2.2.3 | P | `0972e6bef15162c8fd41756c0a311527f717a803052a2f7cf2603f918f84f56a` |
| `tensura-trainer-neoforge-2.0.5.jar` | `tensura_trainer` | 2.0.5 (mods.toml has `${file.jarVersion}` unfilled) | P | `e254fdf76dde1d7fb089e4c3fbda2f11c8d6abd05f9b9d5b0e4199526d78c0f4` |
| `tensura_rebalanced_combined-3.2.17.jar` | none (a dependency locator: `META-INF/neoforge.mods.toml` is absent; nested jars under `META-INF/rebalance/`) | 3.2.17 | P | `5600a62b99cf947026d462a71be5c41906f7b2a0a2bb78fde05e812dcfcfea5f` |
| `tensura_magic_growth-1.0.6.jar` | `tensura_magic_growth` | 1.0.6 | P | `cfcc49a77fbb14c4276a6c2073250de3968d9928c90caf8f91d5ac54b80d342a` |
| `tno_tensura_extras-1.0.3.jar` | `tno_tensura_extras` ("Tensura Extra") | 1.0.3 | P | `001c4f7c33e71375e8088b0e805b7f5fba7a70c43a914e63a6cf73527c7072cd` |
| `tensurauniquemonsters-neoforge-1.0.2.jar` | `tr_unique_monsters` | 1.0.2 | P | `db73f98391ae96d9a267d2ee63c128e497b887ac00c74da9618cd6e0a271eea3` |
| `infinity-stones-tensura-8.0.0-alpha.6.jar` | `infinity` | 8.0.0-alpha.6 | P (hashed, not read) | `d1c587eece27dc16892a3b0730ef3db85530bb1b40d8739551a7e03bf1034004` |
| `rezero_tensura-2.0.0.jar` | `rezero_tensura` | 2.0.0 | P (hashed, not read) | `74c2115071a9d0e7bf10c978b04f25e45163fea07399d8a14801d2b7d465f4a4` |
| `tensura_magicule_engineering-1.3.jar` | `tensura_magicule_engineering` | 1.3 | P (hashed, not read) | `aa3b858f9db233e2a6f7ae5fac4186d24a9e4c202445f2adb64875b65bafaa14` |
| `tensura_neb-neoforge-2.0.0.3.jar` | `tensura_neb` | 2.0.0.3 | P (hashed, not read) | `99ce6e9fd6737278182e055a3fb98b5013a5c53cc7a62700078daa21e158778c` |

Required dependencies of the addons (from each `META-INF/neoforge.mods.toml`; matters for a server boot): every addon needs Tensura 2.0.x, ManasCore 4.0.x and Architectury 13; Kumo Desu ga also GeckoLib, SmartBrainLib and TerraBlender (the dev run already has them); **Ascension also needs `ascensioncore` 1.0.0** (a separate jar, hashed above); FoxAblaze needs Tensura 2.0.1.5 or newer and GeckoLib 4.7.5; Horizons lists Mysticism and Ascension as optional (`[2.1.2,)` and any); Slime Throne Extras lists FTB Quests as optional; Tensura Extra lists FTB Chunks, Iron Chests and Lootr as optional. So the eight jars Ascension, Ascension Core, Great Sage, FoxAblaze, Mysticism, Horizons, Kumo Desu ga and Slime Throne Extras need nothing beyond what the dev run already has. Skill Obtain, Skill Books and Skill Trainer need only Tensura, ManasCore and Architectury. Rebalanced is an `IDependencyLocator` jar (`META-INF/services/net.neoforged.neoforgespi.locating.IDependencyLocator`); whether it works on the Gradle dev run classpath was not tried.

Nested in Rebalanced 3.2.17 (read): `tensura_greatsage_rebalanced-1.1.2`, `tensura_foxablaze_rebalanced-1.0.4`, `tensura_ascension_rebalanced-1.2.3`, `tensura_mysticism_rebalanced-1.2.3`, `tensura_rebalance_core-1.3.13` (the other nested jars were listed, not opened).

## Id lists (this folder, `ids/`)

| File | What |
|---|---|
| `ascension_skills.txt`, `ascension_races.txt` | `id | name | category | description` for Ascension |
| `foxablaze_skills.txt` | FoxAblaze skill ids with an inferred `SkillType` |
| `foxablaze_desc.txt` | FoxAblaze's own in-game description text (acquisition paragraphs) |
| `mysticism_skills.txt`, `mysticism_races.txt` | Mysticism |
| `tensurahorizons_skills.txt`, `tensurahorizons_races.txt` | Tensura Horizons |
| `tensura_kumodesu_skills.txt`, `tensura_kumodesu_races.txt` | Kumo Desu ga |
| `stextras_skills.txt`, `stextras_races.txt` | Slime Throne Extras (2.1.3 lists) |
| `skilltypes.txt` | the `Skill$SkillType` set each skill class passes to Tensura's `Skill` constructor |
| `ULTIMATES_SUMMARY.txt` | Ascension's 11 Unique -> Ultimate pairs and the two ultimates of other addons |

There is no id list for Great Sage: it registers two skills, `greatsage:raphael` and `greatsage:venuzdonoa` (class names `RaphaelSkill`, `VenuzdonoaSkill` in `GreatSageSkills`).

## What each addon does that matters here

**Ascension 3.1.0.** The one addon with a real, manual Unique -> Ultimate road: the **Awakening Altar** (a multiblock) plus an **Ultimate Catalyst** item, behind the game rule `doAscensionUltimate`. Requirements (read in `AwakeningRitual.prepare` and `Awakening`): Max EP at or above `AddonConfig.AWAKENING_MIN_EP` (5,000,000 by default), a True Hero or True Demon Lord (`IExistence`), the required Unique held and mastered, a personal cooldown (`ascension:last_awakening_tick`, `AWAKENING_COOLDOWN_MINUTES`) and a per-ultimate extra gate (see the table in `ULTIMATES_SUMMARY.txt`). On success it learns the ultimate with `SkillHelper.learnSkill(entity, skill, -1)` (the `int` is the instance's remove time, `-1` = permanent; the skill then goes through Tensura's own learning state, as the pack's Rebalanced notes) and forgets the Unique (`SkillStorage.forgetSkill(id, null)`); Apocalypse Bird also forgets the other two birds. Uniques themselves (Deus Sanguis among them) have **no Ascension road at all**: they are added to Tensura's reincarnation pool by the config list `additionalUniqueSkills` (read by `MixinReincarnationMenu`). The Bloodfiend races (`fledgling`, `kindred`, `elder`, `progenitor`, `blood_noble`) do not grant Deus Sanguis.

**Great Sage 0.0.7.1.** A growth tree (`SageNode`) and presentation layer on top of Tensura's `tensura:great_sage`; it registers `greatsage:raphael`. Raphael's gate (`CapstoneGate`) needs 500,000 Max EP, 50,000 Max Magicule, mastered Great Sage and Degenerate, a True Hero or True Demon Lord and every payable tree node, but `CapstoneGate.available(player)` is a constant `false` and `OBTAINABLE = false` in this version: **nobody can reach `greatsage:raphael` in survival**. The pack's Rebalanced says the same in its own words. The mod keeps no single-owner registry; `GreatSageGameRules.ADDON_RAPHAELS` lists both Raphaels, so holding either blocks starting the evolution.

**FoxAblaze 1.0.6.8.** Fifteen ultimates, each with an event or tick driven acquisition handler (`*AcquisitionHandler`, `RaphaelFusionTickHandler`, `BeelzebubFusionTickHandler`, `CielAwakeningHandler`): they fire by themselves when the conditions hold, there is no button. Raphael is fused from mastered Great Sage and Degenerate (race flag Hero Egg, True Hero, Demon Lord Seed or True Demon Lord, no EP check). Its single-owner rule is Tensura's own `ITrulyUnique` registry, switched by FoxAblaze's game rule `RAPHAEL_TRULY_UNIQUE`, and it covers Raphael **and** Ciel.

**Mysticism 2.2.1.** Many Uniques (Butcher, Gardener, Dreamer, ...), the ultimate `mysticism:embryo` (acquisition not found in code; its `SkillEvolutionRequirement` class is a UI progress helper) and race evolution (`EvolutionHandler`, `MysticismEvolutionRequirement`). Uniques come from the reincarnation pool (`MixinReincarnationMenu`). Races include `direwolf` and the `*_spirit_wolf` line.

**Tensura Horizons 0.31.** Requires Mysticism and Ascension. Its awakening (`SovereignAwakening.tryAwaken`) is a race state (a "True Sovereign" flag and Sovereign's Authority), not a skill road. Uniques are reincarnation-pool skills. `SkillAcquisitionPolicy.blocked(id)` blocks one skill (`anathema`).

**Kumo Desu ga 2.0.3-HF2, Slime Throne Extras 2.1.3/2.1.4.** Uniques and races only; no awakening, evolution or ritual class; uniques come from the reincarnation menu (`ReincarnationMenuMixin` / `ReincarnationScreenAccessor`). `stextras` also has `SkillUnlockPacket` (a UI unlock) and an FTB Quests reward.

## The skill-acquisition addons of the pack

| Addon | What it does | Effect on the rites |
|---|---|---|
| Skill Obtain 1.1 | `LIVING_DEATH` handler: a config list `mob | skill,... | chance` makes a killer learn a skill with `SkillHelper.learnSkill(entity, skill)`. | None on the hooks. It can hand a player a skill (even a Unique or Ultimate if the server lists one) outside any rite: the engine must never assume "does not hold the skill" from a past check, and checks again at grant time. |
| Skill Books 2.2.3 | Study-book items that teach a skill (reflection into `SkillHelper`/`SkillAPI`, `SkillGrantFlow`), and **bytecode transformers** (`SkillCostTransformer`, `SkillCostHooks`) that patch the EP and soul-energy price lines of Tensura's and Mysticism's *acquisition* code and refund them. | None on `SkillHelper.learnSkill`'s signature. A rite that grants through the plain acquisition price (`SkillHelper.learnSkill(entity, skill)`) may be priced by these hooks: this is the owner's pack behaviour and is correct, but the spec must not hard-code Tensura's `mpAcquirement` numbers. |
| Skill Trainer 2.0.5 | Client screens, a queue and a ticker (`TrainerTicker`, `PacketHelper`): trains skills for the player. | None found. It speeds mastery, which is a requirement, not a hook. Not read in depth. |
| Rebalanced 3.2.17 | A bundle of rebalance jars, one per addon, with mixins and data that **rewrite obtain routes**. | **Big.** See below. |
| Magic Growth 1.0.6 | Magic ore growth, magicule chunk influence; no skill code. | None. |
| Tensura Extra 1.0.3 | Mobs, items and mixins (`DespairTimeMagicMixin`, alignment fields); no skill-grant code found. | None. |

**Rebalanced rewrites the ultimates' routes.** Its `skill_numbers_obtain/ultimate_*.json` tables and mixins (read for Great Sage 1.1.2, FoxAblaze 1.0.4 and Ascension 1.2.3) say, in the pack: `greatsage:raphael` is unobtainable (the same constant); FoxAblaze's `azathoth`, `beelzebuth` and `velgrynd` are *retired* (blacklisted, `MixinFoxBeelzebubFusionRetired`); Shub-Niggurath's Beelzebuth prerequisite is replaced by `elitetensura:beelzebuth` (`MixinShubRetainedPrerequisite`); Ciel needs a mastered FoxAblaze Raphael and a free or own Raphael/Ciel ownership family; Fortuna, Hell Sniper, Mammon and Samael take MP payments and consume or retain named prerequisites (Mathematician and Usurper, Sniper, Greed, Poison and Merciless); Veldora and Satanael have kill-based handlers; Uriel is a Raphael Synthesise-mode evolution. Consequence for the design: **a rite must ask the installed addon (or read its live config), never copy a number into our data**, or it will drift from what the pack's players see. It also means several FoxAblaze roads are automatic or UI-driven, so a rite adds nothing to them (see the spec, section "Raphael and the automatic roads").

## Mastery

[mastery.md](mastery.md) (2026-11-23, from the owner's second research pass, copied unchanged): how mastery works, which skills cannot be mastered by play, which evolutions and toggles it gates, what already forces it, and the safe way to apply it. It is the basis of the Rite of Mastery (spec section 8.5).

## How to repeat

```
javap -p -c -constants -cp <jar> <class.name>        # JDK 25's javap reads the Java 21 class files
jar tf <jar>                                         # class and entry lists
```

The nested Rebalanced jars were read with `System.IO.Compression.ZipFile` from the outer jar.
