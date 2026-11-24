# Plan 19d: the other Tensura addons and the race links

> Branch `feature/plan-19d`, from main `65f9bf0c`. Spec: `docs/superpowers/specs/2026-11-22-tensura-evolution-rites-design.md` (section 8.4, section 9, Q6, Q7, D9). Engine: 19a (on main). 19b (Great Sage, FoxAblaze, the `retired` fact) and 19c (Blood Sovereign) are built in parallel on `feature/plan-19b` and `feature/plan-19c`; this plan keeps clear of their files and expects a rebase (see "Coordination").

**Goal:** four more Unique rites (Great Mage, King of Curses, Butcher, Imitator), and the race links of spec section 9 (Ascension's Bloodfiends are vampires, Mysticism's Direwolf and Spirit Wolves are werewolves, addon undead follow our undead rules, the Beastfolk evolution is found by id), with `LinkedMods` rows and the absent-addon proof.

**Mixin budget of the part stays 0 of 3 used.** Nothing here is a mixin.

## Decisions (owner, 2026-11-23; spec Q6, Q7, D9)

- **D1. Uniques:** `great_mage` and `king_of_curses` (Ascension), `butcher` (Mysticism), `imitator` (Slime Throne Extras). Nothing from Kumo Desu ga or Horizons.
- **D2. Races:** Ascension Bloodfiend = vampire; Mysticism `direwolf` and every `*_spirit_wolf` = werewolf; addon undead follow our undead rules; the Beastfolk evolution is found by id, never by position.
- **D3. Retired skills** (Rebalanced's `retired` tag: Azathoth, Beelzebub, Beelzebuth, Velgrynd, ...) are never granted. The engine fact is 19b's (E14); 19d adds the data guard and never names a retired skill.

## What was read (Task 0; recorded in `verified-hooks.md`, section "19d reads")

1. **No on/off switch for two of the four.** Ascension's `AddonSkillConfig` has `GREAT_MAGE_ENABLED` and `KING_OF_CURSES_ENABLED` (`ModConfigSpec.BooleanValue`, public static): the existing `unique.road:` probe reads them. Mysticism's `UniqueMysticismSkillConfig$Butcher` and Slime Throne Extras' `STExtrasUniqueSkillConfig$Imitator` are numbers, cooldowns and a list, no boolean: the road is open whenever the addon is loaded. That needs a probe id for "nothing to ask": `unique.no_switch`.
2. **The Bloodfiend chain** (bytecode of the five `...race.bloodfiend` classes, `getNextEvolutions`): `fledgling_bloodfiend` -> `kindred_bloodfiend` -> `blood_noble` -> `elder_bloodfiend` -> `progenitor_bloodfiend` (the last has no next). The third tier is `blood_noble`: the saint-equivalent, as `vampire_lord` is the third tier of Tensura's vampire line. `ApexLines.VAMPIRE` gets `blood_noble`, `elder_bloodfiend`, `progenitor_bloodfiend` (a set is "this tier or higher", as for `vampire_lord` and `divine_vampire`).
3. **Mysticism's wolves:** `direwolf` -> the fang races (`black_fang`, ...), `divine_wolf` -> the twelve spirit wolves (`magic`, `ocean`, `mountain`, `penumbra`, `gale`, `string`, `antumbra`, ...). The fang, frost, scorch, star, tempest and guitar lines are **not** linked (the decision names Direwolf and Spirit Wolf only; `divine_wolf` is not named either).
4. **The Beastfolk evolution Ascension adds** (`MixinBeastfolkRace`): `ascension:kitsune`, when the addon's kitsune switch is on. It is a branch of its own (kitsune, three, six, nine tails, divine kitsune), not our apex line: it is not mapped as a werewolf or a saint tier. Our own code never takes a position in an evolution list: `ApexOffer.merged` adds by `contains` and `ApexLines.BEASTFOLK` is keyed by id (a test pins both).
5. **Undead:** Tensura's `tensura:undead` holds ghoul, wight, wight_king, spirit_skeleton and divine_skeleton. **No installed addon (Ascension, Horizons, Kumo Desu ga, Mysticism, Slime Throne Extras, or any nested Rebalanced module) adds a race to it.** So "addon undead follow our undead rules" is not free: Ascension's `lich`, `elder_lich`, `lich_king` and `undead_skeleton` (the four the spec names) are not undead for the layer, the bite and holy damage today. The fix is data, in our own tag `supernaturalbloodlines:addon_undead` (optional entries), read by `RaceTags.isUndeadOrDaemon` next to `tensura:undead`. Tensura's own tag is not touched, so Tensura's mechanics are as they were. Other undead-looking Ascension races (`zombie`, `skeleton_warrior`, `mage_skeleton`, `death_knight`) are left out and listed for the owner.
6. **Retired tag:** the ids seen in the nested Rebalanced jars include `foxablazeultimate:azathoth`, `beelzebub`, `beelzebuth`, `velgrynd`, `greatsage:raphael`, `mysticism:embryo`; none of the four grants is in it (`trnightmare:imitator` is retired, `stextras:imitator` is not).

## Files

Create: `core/rite/RaceLinks.java` (pure: the id tables), `shared/species/TensuraRaceId.java` (the seam: the species part is Tensura-free), `data/supernaturalbloodlines/tags/manascore_race/races/addon_undead.json`, four rite files `evolve_great_mage|king_of_curses|butcher|imitator.json`, their `known`/`sealed` advancements and book entries (generated), tests below, `docs/parts/evolution_rites.md` section, this plan.
Modify: `Probes.java` (`unique.no_switch`), `RiteKinds.java` (an overload with the race id), `RiteUnlocks.kindsOf`, `ApexLines.java`, `RaceTags.java`, `SpeciesTensuraPart` (installs the seam), `RiteUnlockTable.java`, `LinkedMods.java`, `RitesBook.java`, `en_us.json`, `EvolutionRitesDataTest`, CHANGELOG, wiki, `SPECIES-RULES.md` (R340 and up).

## Tasks (TDD: each starts with a test that fails for the reason named, then the code, then the mutants)

### Task 1: the "no switch" probe
- Test (`ProbesTest`, pure): `noSwitchProbeIsKnownAlwaysOpenAndSatisfied` (`Probes.of("unique.no_switch")` is present, `missingHook()` empty, `roadOpen()` true, `satisfied(...)` true with a null target is not needed: only the first three), `anUnknownProbeIdIsStillNoProbe`. Red: `Probes.of` returns empty.
- Code: `UNIQUE_NO_SWITCH = "unique.no_switch"` mapped to the same own probe as `own.sage_great_sage`.
- Mutants: the id absent; `roadOpen` false.

### Task 2: the race tables (pure)
- `RaceLinksTest`: `theFiveBloodfiendsAreVampires` (exact set); `direwolfAndEverySpiritWolfAreWerewolves` against the real id list `docs/superpowers/research/tensura-addons/ids/mysticism_races.txt` (every id ending `_spirit_wolf` plus `mysticism:direwolf`, nothing else: the fang, frost, scorch, star, tempest, guitar and `divine_wolf` ids are **not** mapped); `noIdOutsideTheListsIsMapped` (every id of the Ascension, Mysticism, Horizons, Kumo Desu ga and Extras lists: mapped iff it is in a table); `aForeignNamespaceNeverMatchesTheSuffix` (`tensura:x_spirit_wolf`, `other:magic_spirit_wolf`); `emptyOrNullIsNothing`; `kindsOfRace` returns `{VAMPIRE}`, `{WEREWOLF}` or `{}`.
- `RiteKindsTest` (extend): `theRaceAddsTheKindAndKeepsTheSpecies` (a witch on `mysticism:direwolf` is `{WITCH, WEREWOLF}`; `Species.NONE` on `ascension:blood_noble` is `{VAMPIRE}`; a plain race adds nothing); `theOldOverloadIsUnchanged`.
- Mutants: a Bloodfiend dropped; the suffix rule without the namespace; `divine_wolf` added; the vampire table answering werewolf.

### Task 3: the seam and `kindsOf`
- Code: `TensuraRaceId.of(Player)` (default `""`, set by `SpeciesTensuraPart` to read `RaceAPI`); `RiteUnlocks.kindsOf` passes it to the new overload.
- GameTests (`RaceLinksGameTests`, species + speciesTensura, no addon needed: the ids are strings, the race is set with `RaceAPI.setRace` only when the race is registered, otherwise the seam is replaced by a fixed function the test installs): `aBloodfiendIsAVampireForTheRites` (kinds contain VAMPIRE), `aSpiritWolfIsAWerewolf`, `aHumanOnATensuraRaceGetsNothing`, `withNoSourceTheKindsAreTheOldOnes`.
- Addon GameTests (`RaceLinksAddonGameTests`, skipped when the addon is not loaded): `theRealBloodfiendChainIsTheOneThatWasRead` (each race's `getNextEvolutions` is the next id of the chain; the last has none), `theRealWolvesAreRegisteredAndMapped` (every mapped Mysticism id is in the race registry; no unmapped one is a spirit wolf), `aPlayerOnBloodNobleIsAVampireKindAndABloodfiendRiteStaysClosedToAHuman`.
- Mutants: the seam ignored in `kindsOf`; the seam consulted for a hunter only.

### Task 4: the apex lines and the Beastfolk evolution by id
- `ApexLinesTest` (extend): `bloodNobleAndHigherAreTheVampireApexTier` (the three ids qualify a vampire, a hybrid, an Original and an Augustine; `fledgling` and `kindred` do not; a werewolf does not); `theBeastfolkLineIsKeyedByIdAndKitsuneIsNotInIt`.
- `ApexOfferTest` (extend): `theApexIsFoundWhateverTheOrderOfTheList` (`merged` with a list where Ascension's kitsune comes first, last, or alone gives the same set); `merged` never writes into an immutable list (existing).
- Mutants: `blood_noble` missing; `kindred_bloodfiend` added; `ascension:kitsune` added to the Beastfolk set.

### Task 5: addon undead
- Tag file `addon_undead.json` (entries `required: false`: `ascension:lich`, `elder_lich`, `lich_king`, `undead_skeleton`); `RaceTags.ADDON_UNDEAD`; `isUndeadOrDaemon` reads it.
- Test: `AddonUndeadDataTest` (pure, on the file): the file lists exactly those four, every entry optional, every id is in the Ascension race list; no entry names a Tensura race; the Tensura tag file is not shipped by us. GameTest (`AddonUndeadGameTests`, Ascension loaded, else skipped): `aLichIsUndeadForTheRules` (`isUndeadOrDaemon` true, and a vampire bite refused as for any undead), `aBloodfiendIsNot`, `withoutTheTagTheRuleIsTensuras`. Without Ascension the file's entries are skipped and nothing changes (a GameTest with the addon absent: the tag is empty and loads).
- Mutants: the tag ignored in `isUndeadOrDaemon`; an entry made required (the datapack fails to load without Ascension: the absent-addon boot catches it).

### Task 6: the four rites
- Data tests (`EvolutionRitesDataTest`, extend): `thereAreTenRites` (the list gains `evolve_butcher`, `evolve_great_mage`, `evolve_imitator`, `evolve_king_of_curses`); `everyUniqueOfPlan19dNamesItsAddonItsProbeAndItsKinds` (a table: grants, requires_mod, probe id or `unique.no_switch`, subject kinds: only Butcher has `werewolf`); `noRecipeGrantsARetiredSkill` (every `grants` of every rite is outside the list of retired ids read in Task 0; mutant: add `foxablazeultimate:velgrynd` to a file); `allFourAreUniqueFreeAndKeepNothing` (tier unique, grant form default, nothing consumed, `ep_cost` positive: E15); `theUnlockRowsAreTheSpecs` (`RiteUnlockTable`: Great Mage and King of Curses `WITCH, SIPHONER, TRIBRID`; Butcher those and `WEREWOLF`; Imitator `SIPHONER, WITCH, TRIBRID`).
- Pure engine tests do not change (`EvolutionRulesTest` already covers the checks); one new row in `EvolutionRecipeCodecTest`: `aRecipeWithTheNoSwitchProbeDecodes`.
- Code: the four rite files, `RiteUnlockTable` rows, `RitesBook` entries (generated advancements and book files with `RITES_BOOK_WRITE=1`), lang (name and three description lines each, in lore words, never what anybody is), the `evolutions` category list.
- GameTests (`EvolutionOthersGameTests`, per addon, skipped without it): `aWitchEvolvesAWerewolfToButcher` (full-moon night, the target sneaks, the skill is learned, the EP is taken after the learn), `butcherRefusesAHumanWithTheSameLineAsAnyRefusal` (no oracle: the performer who is not the target hears only `rite.no_effect`; a target who is not a werewolf hears the reason), `aMysticismDirewolfCountsAsTheWerewolfOfTheRite` (the race link, end to end), `greatMageAndKingOfCursesFollowTheirSwitch` (the config value false: refused, nothing taken), `imitatorOpensWithNoSwitch`, `withTheAddonAbsentTheRiteDoesNotExist` (the existing `...ExistsOnlyWhereAscensionIs` pattern, per addon), `aSkillAlreadyHeldIsRefusedAndNothingIsTaken`, `theConsentRuleHolds` (a target who does not sneak: nothing changes).
- Mutants: the switch ignored; the subject kind of Butcher dropped; the EP taken before the learn; the unlock row of Butcher without `WEREWOLF`.

### Task 7: `LinkedMods`, docs, rules
- `LinkedMods.TESTED` rows `mysticism` 2.2, `tensurahorizons` 0.31, `tensura_kumodesu` 2.0.3, `stextras` 2.1 (series the jars were read at; a different version warns). Test: `LinkedModsTest.theRowsOfPlan19dAreTested` (a 2.2.1 Mysticism is tested, a 2.3 is untested).
- `verified-hooks.md` "19d reads", `docs/parts/evolution_rites.md` (the four rites, the race links, "Mixin budget: 0 of 3 used"), CHANGELOG, wiki (`Rites.md`, `Parts-And-Compatibility.md`, `Known-Limits.md`), `SPECIES-RULES.md` R340 to R345, the `docs/parts/species_tensura.md` note on the Bloodfiend apex tier.

### Task 8: the matrix
- `test`; the 12 GameTest runs of the repo plus `-PpartsOff=evolutionRites`; the addon-jar runs (`-PaddonJars=` Mysticism and Slime Throne Extras alone, then the pack set); the dedicated-server boots (pack set, then with Mysticism removed, then with Horizons and Mysticism removed). Mutants as above, through `run-mutants`.

## Coordination

- **19c** (Blood Sovereign) does not map the Bloodfiends: its D7 leaves it to this plan. Files both touch: `RitesBook.java`, `RiteUnlockTable.java` and its test, `EvolutionRitesDataTest` (the rite list), `en_us.json`, the `evolutions.json` category list, `docs/parts/evolution_rites.md`, CHANGELOG. Expect line conflicts in the lists; the resolution is always "both sets of names".
- **19b** adds the `retired` fact to `EvolutionRules.Facts` and `GrantForm.FREE`. 19d does not touch `EvolutionRules` or `EvolutionRiteFactory`.
- The protocol and the Admin rows do not change (no payload, no setting: the rites use the part's existing per-recipe switches).

## Self-review

- Spec 8.4 (four recipes) is Task 6; section 9 (four links) is Tasks 2 to 5; the outline's item 1 (the reads) is Task 0, 4 and 5 (recorded above); item 4 is Task 7; item 5 is Task 8.
- The finding that no addon uses `tensura:undead` is the one place the plan departs from the spec text ("nothing new"); the owner is told in the PR.
- The Bloodfiend apex tier is a reading of the chain order (third tier), not an id found in any addon text; the PR says so.
