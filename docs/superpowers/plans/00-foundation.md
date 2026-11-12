# Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A buildable, CI-checked NeoForge 1.21.1 mod skeleton with optional-part loading, the shared species attachment, and the pure `BloodlineRules` state machine that every part builds on.

**Architecture:** The main `@Mod` class asks `PartLoader` which parts are enabled (mods present and config switched on). It loads each part's init class by name, so classes that touch another mod are never loaded otherwise. All species transitions are decided by `BloodlineRules`, a pure Java class with no Minecraft or mod types in its signatures, unit-tested against the design's transition matrix.

**Tech Stack:** Java 21, NeoForge 21.1.x, ModDevGradle (mod generator template), JUnit 5, NeoForge GameTest, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-11-supernaturalbloodlines-design.md`

## Global Constraints

- Minecraft `1.21.1`, NeoForge `21.1.x` (pack runs 21.1.265), Java toolchain 21.
- Mod id `supernaturalbloodlines`, package `net.ofatech.supernaturalbloodlines`, repo `OFA-Tech/SupernaturalBloodlines-MinecraftMod`, license MIT.
- Linked mods are all `optional` in `neoforge.mods.toml`, with ranges:
  - tensura `[2.0.1,2.0.2)`
  - manascore_race `[4.0.0,4.1)`
  - vampirism `[1.10.13,1.11)`
  - werewolves `[2.0.3,2.1)`
  - enchanted `[4.2.7,4.3)`
- No linked mod's jar, code or assets in the repo. Dependencies come from Maven or CurseMaven only; compileOnly + runtime for dev.
- Classes that reference another mod live only in `parts.<part>` packages, and nothing outside a part imports them.
- No show character names (Klaus, Mikaelson, Petrova …) in any text.
- No secrets in the repo; CurseForge upload uses the `CURSEFORGE_TOKEN` GitHub secret.

## Review Focus

- **The game with zero linked mods installed** must start cleanly, with all parts reported "disabled (missing mods)" in the log. Pinned by the `PartLoaderTest.noModsMeansNoParts` test and by Task 6's manual run.
- **A config switch off for a part whose mods are present** must keep that part off. Pinned by `PartLoaderTest.configCanDisablePart`.
- **An unknown species string in saved NBT** (old or modded save) must load as `NONE`, not crash. Pinned by `BloodlineStateTest.unknownSpeciesDecodesToNone`.
- **A refused transition** (non-human-like race, Original cure) must leave the state unchanged and give a reason key. Pinned by the `BloodlineRulesTest.refusal*` tests.
- **Re-entrant sync** (system A → B → A on the same player) must stop after one hop. Pinned by `ReentrancyGuardTest.nestedCallIsBlocked`.

---

### Task 1: Repository from the mod generator template

**Files:**
- Create the whole template from https://neoforged.net/mod-generator/ with: Minecraft 1.21.1, ModDevGradle, mod id `supernaturalbloodlines`, name `Supernatural Bloodlines`, package `net.ofatech.supernaturalbloodlines`, Parchment enabled.
- Create: `LICENSE` (MIT; holder "Caio Manoel Bezerra de Araujo", plus a paragraph stating that the linked mods' content is not covered), `README.md`, `CHANGELOG.md`, `.editorconfig`, `.gitattributes` (copy their style from `D:\Development\OFA-TECH\ControlCore-HytaleMod`), and `.gitignore` (template's, plus `run/`, `build/`, `.gradle/`).
- Copy the design and `SPECIES-RULES.md` to `docs/superpowers/specs/`, and `PLAN.md` plus these plans to `docs/superpowers/plans/`.
- Modify `gradle.properties`:
  - `mod_license=MIT`
  - `mod_group_id=net.ofatech.supernaturalbloodlines`
  - `mod_authors=CaioMBA`
  - `mod_version=0.1.0`
  - `neo_version` = latest 21.1.x

- [ ] **Step 1:** Run `./gradlew build`. Expected: BUILD SUCCESSFUL, with `build/libs/supernaturalbloodlines-0.1.0.jar`.
- [ ] **Step 2:** `git init -b main`, commit "chore: NeoForge 1.21.1 template", then `gh repo create OFA-Tech/SupernaturalBloodlines-MinecraftMod --public --source . --push`.

### Task 2: Linked-mod dependencies

**Files:**
- Modify: `build.gradle` (repositories, dependencies, `neoForge.runs`), `gradle.properties` (version properties), and `src/main/templates/META-INF/neoforge.mods.toml` (optional dependency blocks).
- Create: `docs/dependencies.md`, a table of each coordinate and why.

**Interfaces:**
- Produces properties: `vampirism_version=1.21-1.10.13`, `werewolves_version=1.21-2.0.3.4`, `enchanted_version=4.2.7`, `tensura_file_id`, `manascore_file_id`, `architectury_version`.
- Repositories:
  - `https://maven.maxanier.de/releases`
  - `https://maven.paube.de/releases`
  - `https://maven.favouriteless.dev/releases`
  - `https://cursemaven.com` (content filter `curse.maven`)
  - Architectury's Maven
- Dependencies:
  - `compileOnly "de.teamlapen.vampirism:Vampirism:${vampirism_version}:api"`, plus the full jar as `localRuntime`
  - Werewolves, Enchanted (`net.favouriteless.enchanted:enchanted-neoforge-1.21.1`) and its stateobserver / modopedia / geckolib, the same way
  - Tensura and ManasCore as `compileOnly` + `localRuntime "curse.maven:<slug>-<projectId>:<fileId>"`. Look the ids up on CurseForge for tensura-neoforge-2.0.1.5 and manascore-neoforge-4.0.0.2, plus their runtime deps (architectury, geckolib, smartbrainlib).
  - Use ManasCore's nested modules for compile; if CurseMaven exposes only the outer jar, add a `jarJar`-aware extraction task or `compileOnly files(zipTree(...))`, and note the choice in `docs/dependencies.md`.

- [ ] **Step 1:** Add the dependencies, then run `./gradlew compileJava --refresh-dependencies`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2:** Add a throwaway compile check in `src/test/java/.../DependencyCompileCheck.java` that references `de.teamlapen.vampirism.api.VampirismAPI.class`, `net.favouriteless.enchanted.api.curses.CurseManager.class`, `io.github.manasmods.manascore.race.api.RaceAPI.class` and `io.github.manasmods.tensura.storage.TensuraStorages.class` (adjust to the real package). Run `./gradlew compileTestJava`. Expected: success. Then delete the file.
- [ ] **Step 3:** Run `./gradlew runClient` once. Expected: the title screen shows, with Vampirism, Werewolves, Enchanted and Tensura in the mod list. Close the client.
- [ ] **Step 4:** Commit "build: linked mods as optional compile/runtime deps".

### Task 3: Part loader

**Files:**
- Create: `src/main/java/net/ofatech/supernaturalbloodlines/core/Part.java`, `core/PartLoader.java`, `core/Config.java`
- Modify: `SupernaturalBloodlines.java` (the `@Mod` class)
- Create, for each part: `parts/<package>/<Name>Part.java`, a class with `public static void init(IEventBus modBus)` that only logs "enabled" for now:
  - `parts.vampirismtensura.VampirismTensuraPart`
  - `parts.enchantedtensura.EnchantedTensuraPart`
  - `parts.vampirismenchanted.VampirismEnchantedPart`
  - `parts.species.SpeciesPart`
  - `parts.speciestensura.SpeciesTensuraPart`
  - `parts.artifacts.ArtifactsPart`
- Test: `src/test/java/net/ofatech/supernaturalbloodlines/core/PartLoaderTest.java`

**Interfaces:**
- Produces `enum Part { ENCHANTED_TENSURA, VAMPIRISM_ENCHANTED, VAMPIRISM_TENSURA, SPECIES, SPECIES_TENSURA, ARTIFACTS }`, with:
  - `List<String> requiredMods()`
  - `String initClass()`
  - `String configKey()`
  - `Set<Part> requiredParts()` (SPECIES_TENSURA requires SPECIES)
- Required mods per part:

| Part | Required mods |
|---|---|
| `ENCHANTED_TENSURA` | enchanted, tensura |
| `VAMPIRISM_ENCHANTED` | vampirism, enchanted |
| `VAMPIRISM_TENSURA` | vampirism, tensura |
| `SPECIES` | vampirism, enchanted (hybrid-type species check `werewolves` inside the part) |
| `SPECIES_TENSURA` | vampirism, enchanted, tensura |
| `ARTIFACTS` | vampirism, enchanted (moon ring checks `werewolves`, slots check `curios`, inside the part) |

- Produces `static Set<Part> PartLoader.enabledParts(Predicate<String> modLoaded, Predicate<Part> configEnabled)`, and `static void PartLoader.initAll(Set<Part> parts, IEventBus bus)`, which uses `Class.forName(initClass).getMethod("init", IEventBus.class)`.
- Produces `Config.COMMON` (`ModConfigSpec`) with `parts.<configKey>` booleans, default `true`.

- [ ] **Step 1:** Write the tests:
  - `noModsMeansNoParts`: `enabledParts(id -> false, p -> true)` is empty.
  - `allModsAllParts`: `enabledParts(id -> true, p -> true)` equals all 6.
  - `configCanDisablePart`: with all mods present and VAMPIRISM_TENSURA off, the result has no VAMPIRISM_TENSURA.
  - `speciesTensuraNeedsSpecies`: with all mods present and SPECIES off, SPECIES_TENSURA is absent.
  - `missingWerewolvesKeepsSpecies`: werewolves absent → SPECIES and ARTIFACTS still present.
- [ ] **Step 2:** Run `./gradlew test --tests "*PartLoaderTest"`. Expected: FAIL (classes missing).
- [ ] **Step 3:** Implement `Part`, `PartLoader`, `Config` and the five stub part classes. In the `@Mod` constructor: register the config, compute the parts with `ModList.get()::isLoaded`, log each part as `enabled` / `disabled (missing: …)` / `disabled (config)`, then call `initAll`. The config is read at construction, which is acceptable for COMMON configs loaded early. If not readable yet, defer `initAll` to `FMLCommonSetupEvent` and note why.
- [ ] **Step 4:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 5:** Commit "feat: optional part loader".

### Task 4: Bloodline state and rules (pure core)

**Files:**
- Create: `core/bloodline/WolfState.java`, `core/bloodline/PackRules.java` (+ `PackEvent`, `PackContext`, `PackOutcome`), `core/bloodline/Species.java`, `core/bloodline/BaseFaction.java`, `core/bloodline/BloodlineState.java`, `core/bloodline/BloodlineEvent.java`, `core/bloodline/BloodlineContext.java`, `core/bloodline/Outcome.java`, `core/bloodline/BloodlineRules.java`
- Test: `src/test/java/.../core/bloodline/BloodlineRulesTest.java`, `BloodlineStateTest.java`

**Interfaces:**
- **Enums:**
  - `enum Species { NONE, ORIGINAL, ORIGINAL_HYBRID, HYBRID, WITCH, SIPHONER, HERETIC, TRIBRID, POTENTIAL, HUNTER_OF_THE_FIVE }`
  - `enum BaseFaction { NONE, VAMPIRE, WEREWOLF, HUNTER }`
  - `enum BloodlineEvent { RITE_OF_IMMORTALITY, DIED_WITH_ORIGINAL_BLOOD, DRANK_DOPPELGANGER_BLOOD, DRANK_OTHER_BLOOD, TRANSITION_TIMEOUT, UNSTABLE_DEATH, JOINED_VAMPIRE, RITE_OF_THE_MOON, RITE_OF_CLEANSING_BLOOD, RITE_OF_THE_SILVER_MOON, LEFT_BASE_FACTION, ANCESTRAL_CALLING_WITCH, ANCESTRAL_CALLING_SIPHONER, DIED, DRANK_THE_CURE, GENE_AWAKENED_AS_VAMPIRE, RITE_OF_THE_FIVE, KILLED_PREY, KILLED_BY_PREY, RITE_OF_LINEAGE_AWAKEN, CHOSEN_AS_POTENTIAL, KILLED_MARKED_HUNTER, HUNTER_CURSE_LIFTED }`
- **`record BloodlineState(...)`** with fields:
  - `Species species`
  - `boolean witchLayer`
  - `@Nullable UUID sire`
  - `boolean inTransition`
  - `boolean unstable`
  - `long transitionTicksLeft`
  - `@Nullable UUID pendingSire`
  - `int siphonedPower`
  - `long turnedAt` (game time the current species began; 0 = unset)
  - `int thirst` (0–20, default 20)
  - `@Nullable String previousRace` (unused since R100 keeps the race; keep the field for save compatibility, always null)
  - `int markKills` (hunter's mark), `boolean hunterCurse`, `long lastVampireKill` (game time; hunter instinct)
  - `boolean hybridCurseBound` (R24a: werewolf side sealed until the Rite of the Moon)
  - `WolfState wolf` (Teen Wolf layer, plan 9): `record WolfState(Lineage lineage, Rank rank, boolean geneRolled, boolean latentGene, boolean killedInnocent, @Nullable UUID packAlpha, long betaSince, int defenderKills, Set<String> grantedSkills)`, with `enum Lineage { NONE, BORN, BITTEN }`, `enum Rank { NONE, OMEGA, BETA, ALPHA, TRUE_ALPHA }` and `WolfState.NONE`. It lives in `core/bloodline/WolfState.java` and is included in `BloodlineState.CODEC` (field name `wolf`; missing → `WolfState.NONE`).
  - `static final BloodlineState NONE`
  - The last three fields are used by plans 4 and 5. They are defined here so parallel sessions don't edit this shared record. Rules set `turnedAt` from `BloodlineContext.gameTime` (add `long gameTime` to the context) whenever the species changes.
  - `static Codec<BloodlineState> CODEC`. Species is encoded by name, and an unknown name decodes to `NONE`.
- **`record BloodlineContext(...)`** with fields:
  - `BaseFaction faction`
  - `boolean humanLike`
  - `int originalsInWorld`
  - `int maxOriginals` (0 = unlimited)
  - `@Nullable UUID bloodSource`
  - `long transitionTicks` (default 24000)
  - `long gameTime`, `int fiveCount`, `int maxFive` (default 5)
- **`record Outcome(BloodlineState state, List<Action> actions, @Nullable String refusalKey)`**
  - `enum Action { JOIN_VAMPIRE, JOIN_WEREWOLF, LEAVE_FACTION, KILL_PLAYER }`
  - `boolean refused()`
- **`static Outcome BloodlineRules.apply(BloodlineState s, BloodlineEvent e, BloodlineContext c)`**. Refusal keys use the form `supernaturalbloodlines.refusal.<reason>`.

- [ ] **Step 1:** Write `BloodlineRulesTest`, one test per row of the design's transition matrix, with exact expectations:
  - `immortalityMakesOriginal`: state NONE, faction NONE, humanLike → species ORIGINAL, actions [JOIN_VAMPIRE].
  - `refusalImmortalityNotHumanLike`: humanLike=false → refused, key `supernaturalbloodlines.refusal.not_human_like`, state unchanged.
  - `refusalImmortalityHasFaction`: faction WEREWOLF → refused, `…refusal.has_faction`.
  - `refusalImmortalityCapReached`: maxOriginals=1, originalsInWorld=1 → refused, `…refusal.original_cap`.
  - `werewolfDiesWithOriginalBloodStartsTransition`: faction WEREWOLF, bloodSource=U → inTransition, transitionTicksLeft=24000, pendingSire=U.
  - `siphonerCannotStartHybridTransition`: SIPHONER + WEREWOLF → refused, `…refusal.siphoner_hybrid`.
  - `doppelgangerBloodCompletesHybrid`: inTransition, pendingSire=U → HYBRID, sire=U, inTransition=false.
  - `witchCompletingTransitionBecomesTribrid`: species WITCH, witchLayer=true → TRIBRID.
  - `otherBloodMakesUnstable`: inTransition → unstable=true.
  - `unstableDeathRevertsToWerewolf`: UNSTABLE_DEATH → species unchanged from before the transition, inTransition=false, unstable=false.
  - `timeoutKillsAndReverts`: TRANSITION_TIMEOUT → actions [KILL_PLAYER], transition cleared.
  - `witchJoiningVampireLosesMagic`: WITCH + JOINED_VAMPIRE → NONE, witchLayer=false.
  - `siphonerJoiningVampireBecomesHeretic`: SIPHONER + JOINED_VAMPIRE → HERETIC.
  - `originalRiteOfMoonNeedsGene` (R35): ORIGINAL with `latentGene` true + RITE_OF_THE_MOON → ORIGINAL_HYBRID (unbound), actions [LEAVE_FACTION, JOIN_WEREWOLF]. Without the gene → refused, `…refusal.no_gene`.
  - `lineageAwakenOnOriginalMakesBoundOriginalHybrid` (R35): ORIGINAL + RITE_OF_LINEAGE_AWAKEN → ORIGINAL_HYBRID, bound. The same event on a plain vampire → HYBRID, bound. On faction NONE → no species change (`PackRules` handles the born werewolf).
  - `cleansingHybridLeavesWerewolf`: HYBRID + RITE_OF_CLEANSING_BLOOD → NONE, sire=null.
  - `cleansingTribridKeepsWitch`: TRIBRID → WITCH, witchLayer=true.
  - `refusalOriginalCannotBeCured`: ORIGINAL and ORIGINAL_HYBRID + cleansing or silver moon → refused, `…refusal.original_cure`.
  - `secondOriginalBloodNoEffectOnHybrid`: HYBRID + DIED_WITH_ORIGINAL_BLOOD → no change, no actions.
  - `leavingBaseFactionDropsLayer`: HYBRID + LEFT_BASE_FACTION → NONE. HERETIC + LEFT_BASE_FACTION → SIPHONER.
  - `deathResetsSiphonedPower`: SIPHONER with siphonedPower 50 + DIED → siphonedPower 0, species kept.
  - `ancestralCallingOnlyNoFactionOrWerewolf`: faction VAMPIRE → refused, `…refusal.vampire_no_magic`.
  - `cureRemovesEverythingEvenOriginal`: ORIGINAL_HYBRID + DRANK_THE_CURE → NONE, sire null, actions [LEAVE_FACTION].
  - `riteOfTheFiveMarksHunter`: faction HUNTER, humanLike, fiveCount 4 → HUNTER_OF_THE_FIVE, markKills 0.
  - `refusalFiveFull`: fiveCount 5 → refused, `…refusal.five_full`. A faction other than HUNTER → `…refusal.not_hunter`.
  - `markGrowsOnPreyKill`: HUNTER_OF_THE_FIVE + KILLED_PREY → markKills +1, lastVampireKill = gameTime. "Prey" = vampires and werewolf types; witches aren't prey (R81). The caller decides prey through `Supernatural.isPrey`.
  - `potentialActivatesOnFirstKill`: POTENTIAL + KILLED_PREY → HUNTER_OF_THE_FIVE, markKills 1.
  - `markLostToPreyKill`: HUNTER_OF_THE_FIVE + KILLED_BY_PREY → NONE, markKills 0.
  - `naturalDeathKeepsMark`: HUNTER_OF_THE_FIVE + DIED → unchanged mark.
  - `preyKillingMarkedHunterIsCursed`: state ORIGINAL + KILLED_MARKED_HUNTER → hunterCurse true. A plain werewolf (faction WEREWOLF, species NONE) → true. A human (faction NONE) or a WITCH → no curse.
  - `curseLifts`: HUNTER_CURSE_LIFTED → hunterCurse false.
  - `vampireGeneAwakensBoundHybrid` (R24): species NONE, faction VAMPIRE, context `latentGene` true (add `boolean latentGene` to `BloodlineContext`) + GENE_AWAKENED_AS_VAMPIRE → HYBRID, hybridCurseBound true, sire unchanged, actions [LEAVE_FACTION, JOIN_WEREWOLF].
  - `originalGeneAwakensBoundOriginalHybrid` (R35b): ORIGINAL → ORIGINAL_HYBRID, bound.
  - `refusalGeneMissing`: latentGene false → refused, `…refusal.no_gene`.
  - `moonRiteUnbindsCurse`: a bound HYBRID or ORIGINAL_HYBRID + RITE_OF_THE_MOON → hybridCurseBound false, species unchanged.
- [ ] **Step 2:** Write `BloodlineStateTest`:
  - `roundTripsThroughCodec`: every field survives `CODEC` encode/decode with `JsonOps`.
  - `unknownSpeciesDecodesToNone`: JSON `{"species":"DRAGON"}` decodes to species NONE.
  - `speciesChangeStampsTurnedAt`: an Immortality outcome with `gameTime` 500 → `turnedAt` 500.
- Add the GameTest `listenerNotifiedOnSpeciesChangeOnly` to Task 5: setting the same species twice notifies once.
- [ ] **Step 2b:** Write `PackRulesTest` for the pure `core/bloodline/PackRules.java`:
  - Signature: `static PackOutcome apply(WolfState w, PackEvent e, PackContext c)`
  - `enum PackEvent { GENE_AWAKENED, RITE_OF_LINEAGE_AWAKEN, BITTEN_BY_ALPHA, BITTEN_BY_WILD, JOINED_PACK, LEFT_PACK, KILLED_ALPHA, SPARK_INHERITED, SPARK_GIVEN_UP, TRUE_ALPHA_EARNED, KILLED_INNOCENT, DEFENDER_KILL }`
  - `record PackContext(@Nullable UUID otherAlpha, int werewolfLevel, long gameTime, int maxLevel, long betaDaysTicks, int defenderKillsNeeded, boolean otherAlphaHasPack)`
  - `record PackOutcome(WolfState state, @Nullable String refusalKey)`
  - Tests:
    - `geneAwakensBornOmega`: latentGene → lineage BORN, rank OMEGA.
    - `geneWithoutLatentIsRefused`
    - `riteAwakensEvenWithoutGene`
    - `alphaBiteMakesBittenBetaInPack`: packAlpha = otherAlpha, betaSince = gameTime.
    - `wildBiteMakesBittenOmega`
    - `killingAlphaStealsSpark`: rank ALPHA, packAlpha null, stoleSpark true.
    - `innocentKillTurnsEyesBlue`: killedInnocent true.
    - `trueAlphaNeedsEveryCondition` (R91, no cap): each missing condition is refused with `…refusal.true_alpha_<reason>`, where reason is `blue_eyes`, `level`, `beta_days`, `defender_kills` or `stole_spark`. Add `boolean stoleSpark` to `WolfState`, set by KILLED_ALPHA. All met → TRUE_ALPHA.
    - `sparkGivenUpDropsToBeta`
    - `defenderKillCounts`
    - `leavingPackMakesOmega`
- [ ] **Step 3:** Run `./gradlew test --tests "*Bloodline*" --tests "*PackRules*"`. Expected: FAIL.
- [ ] **Step 4:** Implement the classes. `BloodlineRules.apply` is a `switch` on the event. Keep it free of Minecraft classes, except that `Codec` (DFU) is allowed in `BloodlineState`.
- [ ] **Step 5:** Run `./gradlew test`. Expected: PASS.
- [ ] **Step 6:** Commit "feat: bloodline state machine".

### Task 5: Species attachment, sync and re-entrancy guard

**Files:**
- Create: `core/attachment/ModAttachments.java`, `core/network/BloodlineSyncPayload.java`, `core/network/ModNetwork.java`, `core/ReentrancyGuard.java`, `core/BloodlineAccess.java`
- Test: `src/test/java/.../core/ReentrancyGuardTest.java`, and the GameTest `src/main/java/.../gametest/BloodlineGameTests.java`

**Interfaces:**
- **`ModAttachments.BLOODLINE`:** `DeferredHolder<AttachmentType<?>, AttachmentType<BloodlineState>>`, serialized with `BloodlineState.CODEC` and `copyOnDeath()`. The rules decide what resets, via the `DIED` event.
- **`BloodlineAccess`:**
  - `static BloodlineState get(Player p)`
  - `static void set(ServerPlayer p, BloodlineState s)`, which stores the state and sends a `BloodlineSyncPayload` to the player and to everyone tracking them
  - `static Outcome applyEvent(ServerPlayer p, BloodlineEvent e, BloodlineContext c)`, which applies the rules, stores the result, and returns the Outcome. It does not execute faction actions; the parts do that.
  - `static void addListener(BiConsumer<ServerPlayer, BloodlineChange> l)`, with `record BloodlineChange(BloodlineState before, BloodlineState after)`. `set` and `applyEvent` notify the listeners after storing, only when the species changed. Plans 4 and 5 use this.
- **`ReentrancyGuard`:** `static boolean run(UUID player, String channel, Runnable action)`. Returns false and skips the action when the same player and channel is already running.

- [ ] **Step 1:** Write `ReentrancyGuardTest`:
  - `nestedCallIsBlocked`: an inner `run` for the same uuid and channel returns false.
  - `differentPlayersRunIndependently`
  - `guardReleasedAfterException`: after the action throws, the next `run` returns true.
- [ ] **Step 2:** Run it. Expected: FAIL. Implement it with a `ThreadLocal<Set<String>>`. Run it again. Expected: PASS.
- [ ] **Step 3:** Implement the attachment, the payload (`StreamCodec` via `ByteBufCodecs.fromCodec(BloodlineState.CODEC)` or an explicit codec), and `BloodlineAccess`. Register the payload on `RegisterPayloadHandlersEvent`, with the client handler storing the state in the local player's attachment.
- [ ] **Step 4:** Write the GameTest `bloodlineSurvivesSaveLoad`: give a mock server player species WITCH, serialize the player NBT, read it back, and assert WITCH. Add an `@GameTestHolder("supernaturalbloodlines")` holder and enable the `gameTestServer` run in `build.gradle`.
- [ ] **Step 5:** Run `./gradlew runGameTestServer`. Expected: "All 1 required tests passed".
- [ ] **Step 6:** Commit "feat: bloodline attachment with client sync".

### Task 6: Debug command, docs, CI and a clean-start check

**Files:**
- Create: `core/command/DebugCommand.java`, `.github/workflows/build.yml`, `.github/workflows/release.yml`, `docs/parts.md`, `docs/mixins.md` (an empty table: mixin | target | why | config switch), and `docs/parts/<part>.md` for each part (a feature checklist copied from the design).
- Modify: `README.md` (what it is, the parts table with required mods, install, MIT and the "not affiliated with the shows" note), and `src/main/resources/assets/supernaturalbloodlines/lang/en_us.json` (all refusal keys from Task 4).

**Interfaces:**
- Produces the commands, all at permission level 2:
  - `/supernaturalbloodlines debug get <player>`, which prints the BloodlineState
  - `/supernaturalbloodlines debug set <player> <species>`
  - `/supernaturalbloodlines debug event <player> <event>`, which runs `BloodlineAccess.applyEvent` with a context built from the player
- `build.yml` runs `./gradlew build test` with Java 21 on push and pull requests. `release.yml`, on a tag `v*`, builds the jar and attaches it to a GitHub release. Its CurseForge upload step is guarded by `if: secrets.CURSEFORGE_TOKEN != ''`.

- [ ] **Step 1:** Implement the command. In `runClient`, run `/supernaturalbloodlines debug set @s WITCH`, then `debug get @s`. Expected: it prints WITCH, and it survives a relog.
- [ ] **Step 2:** Clean-start check: temporarily switch every linked mod in `build.gradle` from `localRuntime` to compile-only (a `-PnoLinkedMods` flag), run `runClient`, and check `logs/latest.log`. Expected: 6 lines of "disabled (missing: …)" and no errors from `supernaturalbloodlines`. Keep the flag for later parts' checks.
- [ ] **Step 3:** Push. Expected: the build workflow is green on GitHub.
- [ ] **Step 4:** Commit "chore: debug command, docs, CI" and tag `v0.1.0-alpha`.
