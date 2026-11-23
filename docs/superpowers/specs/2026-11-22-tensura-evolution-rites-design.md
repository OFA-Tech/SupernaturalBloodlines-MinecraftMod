# Tensura evolution rites (plan 19): design

Status: design only (2026-11-22). Docs only: no game code, no Gradle run, no game launch. Branch `docs/plan-19`. Research: [docs/superpowers/research/tensura-addons/](../research/tensura-addons/README.md) (jar versions with SHA-256, id lists, [verified-hooks.md](../research/tensura-addons/verified-hooks.md)). Build plans: [19a](../plans/2026-11-22-plan-19a-evolution-engine.md) in full, 19b, 19c and 19d as outlines at its end.

The owner decisions below are binding (project memories #1213 and #1214, 2026-11-21/22; the owner accepted all 21 picks). Where the research makes one of them hard to keep, the spec says so and puts a question in section 14 with a recommended answer; it never quietly changes a decision.

## 1. Intent

The Tensura addons add many skills, and most of them have no road at all: a Unique such as Deus Sanguis exists only as a roll in Tensura's reincarnation menu. A witch in this mod is a person who works rites at an altar. The idea: a witch-type performer, on a rare night, evolves a consenting target (or herself) toward a skill that **already exists**, in a dramatic rite with the book, a circle and an altar. It gives our species a story with the Tensura content that is already installed, without us writing a single skill.

## 2. Goals, non-goals, anti-goals

Goals
- One shared rite engine, one data recipe per chain (owner pick).
- Every recipe keeps the addon's own requirements exactly, even when the pack's Rebalanced changes them (section 5, E2).
- Rare and dramatic: full moon for Uniques and below, a lunar eclipse for Ultimates.
- Each addon is optional: a recipe whose addon is missing does not exist (no rite, no book entry, no unlock).
- No oracle: a caster cannot learn what a stranger on the circle is, holds or lacks (section 7).

Non-goals
- Inventing a skill, an ultimate, a race or a stat of ours (decision 1).
- Replacing an addon's own road. The addon's road stays and still works; a rite is an extra road.
- A server-wide "who owns what" browser.
- Changing anything in an addon (no mixin into an addon, no edit of its config).

Anti-goals (a failure even if it works)
- A rite that is easier than the addon's road, or that skips one of its checks.
- A rite that consumes a skill the addon's own road would keep.
- A refusal message that tells a caster something about a player who has not agreed.
- A world that fails to start because an addon is missing, renamed or updated.

## 3. Owner decisions (binding)

| # | Decision | Where it lands |
|---|---|---|
| D1 | Rites only open new roads to **existing** skills of Tensura and its addons. No ultimates of our own. | E1 |
| D2 | Rites are rare and dramatic, but keep the same requirements as the addon's own path. | E2 |
| D3 | Ultimates need an eclipse; Uniques and below need a full moon. | E3 |
| D4 | Witch-type casters only; the target consents by sneaking. | E4 |
| D5 | The rites start sealed in the book. Ultimates are lore-gated; the rest unlock by kind. | E7 |
| D6 | A rite consumes skills only where the addon's own path does. | E8 |
| D7 | FoxAblaze's single-owner rule is respected. Either Raphael counts, and FoxAblaze's is preferred. | E9 |
| D8 | Our chains: Sage -> Great Sage, a vampire rite to Ascension's Deus Sanguis, and werewolf/witch rites only where an addon has a fitting unique. No Starved -> Gluttony. | section 8 |
| D9 | Race mapping: Ascension Bloodfiend = vampire; Mysticism Direwolf and Spirit Wolf = werewolf; addon undead follow our undead rules; the Beastfolk evolution is picked by id. | section 9 |
| D10 | One data-driven recipe per chain, run by a shared rite engine. | section 6 |
| D11 | Testing must include a real server boot with all the addon jars. | section 13 |
| D12 | Build order: 19a engine + our own chains, 19b Great Sage + FoxAblaze, 19c Ascension, 19d the others + race links. | section 12 |

## 4. What the research found (the facts the design stands on)

1. **There is no Sage -> Great Sage road in Tensura 2.0.1.5.** Great Sage is a Unique in the reincarnation pool; no class links it to Sage (verified-hooks.md, Tensura). The chain of D8 is ours, and Tensura has no requirement for it to keep.
2. **No addon Unique has a road either** (Deus Sanguis, Great Mage, Butcher, ...): Ascension, Mysticism, Kumo Desu ga, Slime Throne Extras and Horizons add them to the reincarnation pool and nothing else. For a Unique, "the addon's own requirement" is only the addon's own on/off switch where one exists (`DEUS_SANGUIS_ENABLED`).
3. **Only Ascension has a manual road to Ultimates**: the Awakening Altar (a multiblock plus an Ultimate Catalyst) with a full list of checks. A rite cannot call it (it needs the altar's block entity); a rite repeats its checks by asking Ascension's own public helpers and config values.
4. **Raphael is closed or automatic.** Great Sage's `greatsage:raphael` is unobtainable in 0.0.7.1 (`CapstoneGate.available` is a constant `false`). FoxAblaze's Raphael (and most of its other ultimates) fuses **by itself** from a tick or event handler as soon as the conditions hold. A rite on top of FoxAblaze's Raphael has nobody to serve (section 8.3, question Q1).
5. **The pack's Rebalanced rewrites obtain routes** (retired FoxAblaze skills, a replaced Shub prerequisite, MP prices). A number copied into our data goes stale. So a recipe asks the installed addon at runtime.
6. **FoxAblaze's single-owner rule is Tensura's own registry** (`ITrulyUnique`), switched by FoxAblaze's game rule, and it covers Raphael and Ciel together. Great Sage keeps no owner registry; it only refuses a player who already holds either Raphael.

## 5. Rules

The rules are numbered E1 to E14 here so they do not collide with the R-numbers of `SPECIES-RULES.md`; the first build phase copies them there under the next free R-numbers.

**E1. Existing skills only.** A recipe names a skill id that an installed mod registers. No skill, item, race or attribute of ours is added by this plan. (Our rite JSONs, a part, config, lang and book entries are not skills.)

**E2. The addon's requirements, exactly, asked at runtime.** A recipe has a `probe`: a small class that asks the installed addon (by reflection, see section 6.3) whether this target meets the addon's own requirements for this skill, using the addon's own helper where one is public and its **live** config value otherwise. Our own conditions (E3, E4, ingredients, altar power) are added; none of them replaces or relaxes an addon condition. If a probe cannot find the hook it needs (a version change), the recipe is **unavailable** and a single warning names the missing class or method; it never grants by guessing.

**E3. The moon.** Tier decides the night. `ULTIMATE` needs a lunar eclipse (`LunarEclipse.active(server)`, which is only ever true on a full-moon night; the hub already shows it). `UNIQUE` and `EXTRA` need a full-moon night (`MoonRule.isFullMoonNight(hasFixedTime, moonPhase, dayTime)`, the same check as Severance). The tier is a recipe field, and a game test compares it with the skill's real `Skill.getType()` (a skill that passes both `ULTIMATE` and `UNIQUE`, like `mammon`, is an Ultimate).

**E4. Who works it.** The performer must be witch-type (`SeveranceRiteFactory.isWitchType`: witch, siphoner, heretic, tribrid). The target is the player on the centre block (`RiteTargets.centrePlayerOrCaster`) or the performer. A target who is not the performer must sneak on the centre (`RiteTargets.consentAnswer`, new ask key `supernaturalbloodlines.rite.sneak_to_evolve`).

**E5. No oracle.** Section 7.

**E6. Grants.** The skill is learned on the **target** with Tensura's own call, `SkillHelper.learnSkill`. A recipe says which form: `default` (the plain price and veto, our Sage -> Great Sage), `free` (a `TensuraSkillInstance` with the `NoMagiculeCost` tag, as Great Sage's and FoxAblaze's own grants do) or `permanent` (`learnSkill(entity, skill, -1)`, as Ascension's altar does). A form is added to the engine when the first recipe needs it (19a: `default`; 19b: `free`; 19c: `permanent`). `learnSkill` returning `false` (a listener vetoed it: not enough magicules, a pack hook, Skill Books' price hooks) is a refusal; nothing is consumed.

**E7. The book.** Every evolution rite is a gated rite of `RiteUnlockTable` (sealed in the book until known, "???" decoy entry, hint line). Ultimates unlock only by lore; Uniques and below unlock by kind (section 10).

**E8. Consumption.** `consumes` lists the skills the **addon's own path** forgets, nothing else: Raphael (Great Sage and Degenerate, in both addons), Blood Sovereign (Deus Sanguis), and the ten other Ascension pairs. A recipe with an empty list (our Sage -> Great Sage, every Unique) consumes nothing. Order: learn first, forget after a confirmed learn, so a vetoed learn never costs a skill.

**E9. Single owner.** For a recipe whose skill belongs to FoxAblaze's Raphael/Ciel family (`foxablazeultimate:raphael`, `foxablazeultimate:ciel`): if the game rule `RAPHAEL_TRULY_UNIQUE` is on and Tensura's `ITrulyUnique` of the overworld says another player owns either id, the rite refuses with the generic answer (section 7). A player who already holds either Raphael (`foxablazeultimate:raphael` or `greatsage:raphael`) gets no other one (Great Sage's own rule). Great Sage keeps no registry, so an owner of `greatsage:raphael` elsewhere is not detectable; this is written in the book page and the docs.

**E10. Present, available, enabled.** A recipe works only when (a) its `requires_mod` is loaded (`ModList.isLoaded`), (b) its probe found every hook, (c) the road is open (a probe may say "closed in this version", as Great Sage's Raphael is), (d) the server settings allow it (section 11). A recipe that fails a-c is treated as absent everywhere (section 10.3); one that fails d is visible and refuses with a plain line.

**E11. Atomic, on the server thread, instant.** A rite does its whole effect in `onStart` (like Severance) and returns `false`; every failure is `cancel()` (Enchanted gives the ingredients back) and nothing has changed. A skill that a third mod (Skill Obtain, Skill Books) gave the target in the meantime is seen at grant time: the grant checks "already holds" again.

**E12. Performing teaches.** A completed rite calls `RiteSignal.completed` as every rite does, so the performer learns the rite (`RiteLearned`). A refusal does not.

**E13. Parts and addons are optional.** The code is a new part (section 12.1). It has no compile dependency on any addon; addon classes are only reached through the probes.

**E14. The Rebalanced `retired` skills.** A recipe never grants a skill in the Rebalanced `retired` skill tag (`tensura_rebalanced:retired`, seen in the nested jars' data) when that tag exists; it is checked by tag, never by id (question Q7).

## 6. How it is built

### 6.1 Approaches considered

| | A. One rite factory per skill | B. One engine, recipes in data, requirements copied into the data | **C. One engine, recipes in data, requirements asked of the addon (chosen)** | D. Patch the addon (mixins) so its own road accepts our rite |
|---|---|---|---|---|
| Matches D10 | no | yes | yes | no |
| Drift with Rebalanced or an addon update | none, but ~25 classes | **yes: the numbers are copied** | none for what a probe asks; a hook change makes the recipe unavailable and loud | none |
| Cost | high | low | low engine plus one probe per addon (4) | high, brittle, breaks "do not change an addon" |
| Risk | code volume | wrong in the pack | reflection on four addons | any addon update |

C: owner pick D10 plus the finding in section 4 (5). One more reason for C over B: for the Ascension ultimates the check list is long (EP, status, mastery, cooldown, extra gate), and Rebalanced changes it.

### 6.2 The parts

A new part `evolutionRites` (config key `parts.evolutionRites`), package `net.ofatech.supernaturalbloodlines.parts.evolutionrites`, `requiredMods` `vampirism`, `enchanted`, `tensura`; `requiredParts` `SPECIES_TENSURA` (which brings `SPECIES` and `VAMPIRISM_ENCHANTED`: `SeveranceRiteFactory.isWitchType`, `RiteTargets`, `LunarEclipse`, `RiteUnlocks`). Pure rules go in `core.evolution` (no game classes). Data carries `{"type": "supernaturalbloodlines:part_enabled", "part": "evolutionRites"}` plus `neoforge:mod_loaded` for the addon: the `circle_magic/rite` registry fails the whole load for a factory that is not registered (reproduced for `enchantedTensura`, `docs/parts/enchanted_tensura.md`). `docs/parts/evolution_rites.md` is the part's checklist.

### 6.3 The recipe (the rite's own `factory` block)

One rite file per recipe, `data/supernaturalbloodlines/enchanted/circle_magic/rite/evolve_<id>.json`: Enchanted's own fields (`items`, `shapes`, `power`, conditions) and the factory `supernaturalbloodlines:evolution`. The recipe is the factory's codec, so the datapack registry loads it and a datapack can change the ingredients but cannot invent a recipe the engine has no probe for.

```json
{
  "neoforge:conditions": [
    { "type": "supernaturalbloodlines:part_enabled", "part": "evolutionRites" },
    { "type": "neoforge:mod_loaded", "modid": "ascension" }
  ],
  "items": [ { "id": "ascension:ultimate_catalyst", "count": 1 }, { "id": "enchanted:spirit_of_otherwhere", "count": 1 } ],
  "shapes": { "enchanted:large_circle": "enchanted:otherwhere_chalk" },
  "power": 20000,
  "factory": {
    "type": "supernaturalbloodlines:evolution",
    "recipe": "blood_sovereign",
    "tier": "ultimate",
    "grants": "ascension:blood_sovereign",
    "grant_form": "permanent",
    "consumes": [ "ascension:deus_sanguis" ],
    "needs": [ { "skill": "ascension:deus_sanguis", "mastered": true } ],
    "requires_mod": "ascension",
    "probe": "ascension.awakening",
    "subject_kinds": [ "VAMPIRE" ]
  }
}
```

(`subject_kinds`: the kinds the **target** must be, our rule; empty = any. It is checked on the target's state, so it answers only through `refuseAbout`.) The example is illustrative; the ingredient list of each recipe is the builder's call within E5.

Code layout (names are the plan's; section "File structure" of the 19a plan):

- `core.evolution`: `Tier` (`ULTIMATE`, `UNIQUE`, `EXTRA` and the night each needs), `EvolutionRecipe` (a record of the fields above), `EvolutionRules` (pure: the order of checks, the `Reason` enum, `Facts`), `GrantForm`.
- `parts.evolutionrites.rite.EvolutionRiteFactory` (the `RiteFactory`, codec, the rite body).
- `parts.evolutionrites.probe`: `AddonProbe` (interface), `Hooks` (cached `MethodHandle` lookup that returns "missing hook X" instead of throwing), `Probes` (the lookup by probe id, with the own probe of our chain) and one class per addon (`GreatSageProbe`, `FoxAblazeProbe`, `AscensionProbe`; Mysticism, Kumo, Slime Throne and Horizons need none: their uniques use the generic `unique.road` probe, which only checks the addon's switch where one exists).
- `parts.evolutionrites.EvolutionRitesConfig`, `EvolutionRitesPart`.

### 6.4 The order of checks in `onStart`

1. The target (`centrePlayerOrCaster`) and the performer; either missing -> `cancel()` silently (as the other rites).
2. Settings and presence (E10 a-d) -> plain refusal to the performer (it says nothing about the target).
3. The performer is witch-type -> `refusal.evolution.not_witch`.
4. The night (E3) -> `rite.needs_full_moon` (existing key) or `rite.needs_eclipse` (new).
5. Consent (E4) -> `RiteTargets.consentAnswer`: the performer hears `rite.no_effect` whatever the target is, the target is asked to sneak.
6. **Everything about the target**, in one block, through `RiteTargets.refuseAbout`: subject kind, holds the skill already, holds `needs`, mastered, the probe, single owner, retired skill. One answer for the performer (`no_effect`); the target hears the reason (section 7).
7. Learn on the target (`E6`); on `false`, as step 6 (reason `not_ready`), nothing consumed.
8. Forget `consumes` (E8); tell the target `evolution.done`, the performer `evolution.done_caster`; `RiteSignal.completed`; if `announce_ultimates` and tier is ULTIMATE, one server line (default off).

## 7. Refusals (no oracle leaks)

`RiteTargets.refuseAbout(caster, target, key)` does the split already: a performer who is not the target gets `supernaturalbloodlines.rite.no_effect`, the target gets `key`; a performer who is the target gets `key`. The rule for this plan: **anything that depends on the target goes through `refuseAbout`; only what depends on the performer or the world may be said to the performer.**

| Check | Depends on | Who hears what | Key (new unless noted) |
|---|---|---|---|
| rite or recipe switched off, addon missing, hook missing, road closed | the server | performer: plain | `supernaturalbloodlines.refusal.evolution.closed` |
| not witch-type | the performer | performer | `supernaturalbloodlines.refusal.evolution.not_witch` |
| wrong night | the world | performer | `supernaturalbloodlines.rite.needs_full_moon` (existing), `supernaturalbloodlines.rite.needs_eclipse` |
| target is not sneaking | the target's input only | performer: `rite.no_effect`; target: asked | `supernaturalbloodlines.rite.sneak_to_evolve` |
| target of the wrong kind, lacks the source skill, not mastered, below an addon threshold, wrong status, cooldown, game rule of the addon off | the target | performer: `no_effect`; target: the reason | `supernaturalbloodlines.refusal.evolution.not_ready` (one generic key for every addon requirement; the book page lists the requirements, so the reason is the book's, not a live reading) |
| already holds the skill | the target | performer: `no_effect`; target: told | `supernaturalbloodlines.refusal.evolution.already` |
| single-owner taken by another player | the world's secret | performer: `no_effect`; target: the **same** `not_ready` | (no separate key: it must not tell the target that someone else holds Raphael) |
| a listener vetoed the learn | the target | as `not_ready` | |

Same cost and same timing for every refusal (`cancel()`, the ingredients back). A GameTest asserts that the performer's messages for a human, a vampire, and a player holding the skill are identical (`rite.no_effect` with the target not sneaking, and with the target sneaking), and the unit test pins that the single-owner and the other addon refusals map to the same key.

## 8. The recipes

"Source" is the road the addon or Tensura has now. "Requirement kept" is what the probe asks, live. Moon: F = full moon, E = eclipse. Unlock: the kinds that know the rite from being one (section 10) or the lore.

### 8.1 19a: the engine and our own chain

| Recipe | Grants (tier) | Source today | Requirement kept | Consumes | Moon | Unlock |
|---|---|---|---|---|---|---|
| `great_sage` | `tensura:great_sage` (Unique) | Tensura 2.0.1.5: reincarnation pool only; no road (research 4.1) | Tensura has none. **Ours (Q2):** target holds `tensura:sage` and it is mastered; the learn goes through plain `SkillHelper.learnSkill` so Tensura's own price and vetoes apply (its `mpAcquirement` default is 75000.0, and the pack's Skill Books may rescale it) | nothing (Sage stays) | F | WITCH, SIPHONER, TRIBRID |

Probe: `own.sage_great_sage` (reads `SkillUtils.hasSkill`, `Inst.isMastered`).

### 8.2 19c: Ascension

| Recipe | Grants (tier) | Source today | Requirement kept | Consumes | Moon | Unlock |
|---|---|---|---|---|---|---|
| `deus_sanguis` | `ascension:deus_sanguis` (Unique) | reincarnation pool only (`additionalUniqueSkills`) | `AddonSkillConfig.DEUS_SANGUIS_ENABLED`. **Ours:** target is vampire-kind (our vampire species, or an Ascension Bloodfiend race, section 9) | nothing | F | WITCH, SIPHONER, TRIBRID, VAMPIRE |
| `blood_sovereign` | `ascension:blood_sovereign` (Ultimate) | Awakening Altar, `ULTIMATE_CATALYST` | `doAscensionUltimate` true; Max EP at or above `AddonConfig.AWAKENING_MIN_EP` (5,000,000 default); `IExistence.isTrueHero() \|\| isTrueDemonLord()`; holds and has mastered `ascension:deus_sanguis`; `AWAKENING_COOLDOWN_MINUTES` (120 default) since `ascension:last_awakening_tick`; this pair has no extra gate. The catalyst is a rite ingredient (the altar consumes it) | `ascension:deus_sanguis` | E | lore |

Other Ascension pairs (research `ULTIMATES_SUMMARY.txt`): `great_mage -> timeless_mage` (all aspectual magics: `hasAllAspectualMagics`), `sealer -> one_who_seals` (`hasSuppressionStoneOver400k`, consumed by `consumeSuppressionStoneOver400k`), `dragon_slayer -> slayer_of_dragons` (`hasDragonEssences30` and its consume), `bubble_majin -> evil_majin` (`hasEatenEnoughMajinCandies`), `imprisoned_jester -> unbound_jester` (`hasReachedUnboundJesterThreshold`), `eye_of_mind -> all_seeing_truth`, `king_of_curses -> ruler_of_death`, `trash_gamer -> god_of_fortune`, three birds -> `apocalypse_bird` (consumes all three; `hasOtherBirdsMastered` is private), `shadow_necromancer -> shadow_monarch` (the "System" job gate, not traced). **Not in 19c unless Q5 says so.** The probe is written for the whole table (it reads the pair from the addon's own table), so adding a pair later is one rite file and one lang block.

### 8.3 19b: Great Sage and FoxAblaze, and the Raphael finding

| Recipe | Grants (tier) | Source today | Requirement kept | Consumes | Moon | Unlock |
|---|---|---|---|---|---|---|
| `raphael` | FoxAblaze's `foxablazeultimate:raphael` if FoxAblaze is loaded (D7 "preferred"), else `greatsage:raphael` (Ultimate) | FoxAblaze: **automatic** fusion on a tick (`RaphaelFusion.tryFuse`). Great Sage: `CapstoneGate`, **closed in 0.0.7.1** (`available` is a constant `false`) | FoxAblaze: mastered `tensura:great_sage` and `tensura:degenerate`; none of Raphael, Ciel held; the race flag Hero Egg, True Hero, Demon Lord Seed or True Demon Lord when `fusionRequireSpecialRace`; single-owner (E9). Great Sage: Max EP 500000, Max Magicule 50000, the two mastered, True Hero or True Demon Lord, every payable node, `available()` | `tensura:great_sage`, `tensura:degenerate` (both addons do) | E | lore |

**Raphael and the automatic roads.** With FoxAblaze loaded, a player who meets those conditions is fused by FoxAblaze within a tick or two, so the rite has nobody left to evolve; one who does not meet them is refused by the same conditions. A FoxAblaze Raphael rite therefore opens no new road. The same holds for FoxAblaze's other ultimates whose handlers fire by themselves (Beelzebuth, Satanael, Fortuna, Hell Sniper, Mammon, Samael, Veldora, Asmodeus, Azathoth, Velgrynd, Ciel; Uriel and Shub are Synthesise-mode picks). The plan therefore keeps the engine's FoxAblaze **single-owner reader** and the Raphael row but marks the row dormant until the owner answers Q1 (recommended: Raphael only as Great Sage's road, opened by the probe the day Great Sage's `available` can be `true`; no FoxAblaze rows).

### 8.4 19d: the others

Candidates for "an addon has a fitting unique" (D8). None has a road today; each is a Unique, F, consumes nothing, unlock by kind. The owner picks (Q6).

| Recipe | Grants | Fit | Requirement kept | Unlock |
|---|---|---|---|---|
| `great_mage` | `ascension:great_mage` | witch magic ("learn and master everything far faster... copy magic off any spell circle") | the addon's switch if it has one | WITCH, SIPHONER, TRIBRID |
| `king_of_curses` | `ascension:king_of_curses` | curses (we have curse rites) | as above | WITCH, SIPHONER, TRIBRID |
| `butcher` | `mysticism:butcher` | "your thirst for the hunt": werewolf | none | WITCH, SIPHONER, TRIBRID, WEREWOLF |
| `imitator` | `stextras:imitator` | a siphoner takes what is cast | none | SIPHONER, WITCH, TRIBRID |

Not recipes: Kumo Desu ga's uniques (Determination, Perseverance, Resolve, Weapon Creation: spider/ogre flavour, no fit), Horizons' uniques (Edict, Fortune, Inheritor, Storyteller, ...: no fit found), Mysticism's `embryo` (acquisition not found), the Horizons Sovereign awakening (a race state).

## 9. Race links (D9, 19d)

The links are data and one table, `ApexLines`-style, and change what counts as a vampire or a werewolf for `RiteKinds` and the apex offer. All ids are in the id lists.

| Link | Ids | Rule |
|---|---|---|
| Ascension Bloodfiend = vampire | `ascension:fledgling_bloodfiend`, `kindred_bloodfiend`, `elder_bloodfiend`, `progenitor_bloodfiend`, `blood_noble` | count as vampire-kind for `RiteKinds.of(...)` and the `subject_kinds` check; which of them is the saint-equivalent tier of `ApexLines.VAMPIRE` is read from `getNextEvolutions` in 19d task 1 (not guessed) |
| Mysticism Direwolf and Spirit Wolf = werewolf | `mysticism:direwolf` and every id of `mysticism_races.txt` ending in `_spirit_wolf` | werewolf-kind; the Mysticism fang and guitar-wolf lines are **not** included (the decision names Direwolf and Spirit Wolf only) |
| Addon undead follow our undead rules | Ascension's `lich`, `elder_lich`, `lich_king`, `undead_skeleton`, and any other race an addon puts in the tag | nothing new: the rule is `RaceTags.UNDEAD` (Tensura's `tensura:undead`, which an addon fills), so a race the addon puts in that tag is undead for the layer, the bite and holy damage already; the work is a test that lists the addon races carrying the tag |
| Beastfolk evolution by id | the id of the Beastfolk evolution Ascension's `MixinBeastfolkRace` adds, and Tensura's `spirit_beast`, `divine_beast` | `ApexLines.BEASTFOLK` stays keyed by id; an evolution list from `getNextEvolutions` is searched **by id**, never by position |

## 10. Book, unlocks and the rest of the mod

### 10.1 The book

A new category `evolutions` ("Evolution Rites", icon `minecraft:nether_star`) in `RitesBook.CATS`; one real entry per rite and one "???" decoy (existing design: `known/<rite>` and `sealed/<rite>` advancements, `RiteUnlockTable.GATED`, regenerate with `RITES_BOOK_WRITE=1 ./gradlew test --tests '*RitesBookTest'`). The real entry says in lore words what the rite needs, never what any player is. A lore-locked entry carries no `assigned_items`. Entry text names the addon's skill, the night, who works it, and the requirements the addon states (they are public in the addon).

### 10.2 Unlocking

`RiteUnlockTable.GATED` gets one row per evolution recipe (the table stays the single source; a data test checks each rite file's `unlock` against it):
- Unique and below: `kind(...)` with the performer kinds (WITCH, SIPHONER, TRIBRID) plus the subject kind where the recipe has one (VAMPIRE for Deus Sanguis, WEREWOLF for Butcher): a witch learns to work it, and the vampire learns that such a rite exists (Q3).
- Ultimate: `lore(...)` with one lore id, source in Q4 (recommended: `finn`, Finn's Notes, "the rites of the eclipse", no new item).

### 10.3 Absent addons

`RiteUnlocks.reconcile` and `fix` treat a recipe that fails E10 a-c like no recipe: neither `known` nor `sealed` is awarded (and `sealed` is revoked), so the book shows neither entry and `/bloodline rites list` omits it. Three layers, each tested: (1) the rite JSON has `neoforge:mod_loaded` so Enchanted never registers it; (2) the recipe's `requires_mod` is checked with `ModList.isLoaded` (a `Predicate<String>` in the pure rules, so a unit test can say "not loaded"); (3) the unlock filter above. The addons are **not** declared in `neoforge.mods.toml` (an out-of-range optional mod stops FML, `LinkedMods` doc); their tested series go into `LinkedMods.TESTED` instead: `greatsage` 0.0.7, `foxablazeultimate` 1.0.6, `ascension` 3.1, `mysticism` 2.2, `tensurahorizons` 0.31, `tensura_kumodesu` 2.0.3, `stextras` 2.1, so another version warns.

### 10.4 Other touch points

- `/bloodline rites list|learn|forget` already reads `RiteUnlockTable.choices()`: nothing new except that it hides absent recipes.
- The hub keeps no rite page; the Guide gets one chapter paragraph ("Evolution rites", the two nights, the consent) and the Admin page the rows of section 11. No hub wire change (`LadderNet.PROTOCOL` unchanged).
- Ladders and quests: none of the new rites is a ladder task, so the "performer must know the rite" data test is unaffected.

## 11. Settings (every setting has an Admin row)

New file `config/supernaturalbloodlines-evolution_rites.toml` (`EvolutionRitesConfig.FILE`, `SPEC`), section `evolutionRites` in `AdminEntries.SECTIONS` ("Evolution rites"), listed in `AdminCoverageTest.CONFIGS`. `parts.evolutionRites` gets its row from the existing loop over `Part.values()`.

| Key | Kind | Default | Meaning | Admin row |
|---|---|---|---|---|
| `parts.evolutionRites` | BOOL (restart) | true | the part | `hub.admin.key.supernaturalbloodlines-common.toml:parts.evolutionRites` |
| `rites_enabled` | BOOL | true | all evolution rites; off = `evolution.closed` | `...-evolution_rites.toml:rites_enabled` |
| `ultimate_rites_enabled` | BOOL | true | the eclipse (Ultimate) rites | `...:ultimate_rites_enabled` |
| `disabled_recipes` | LIST | empty | recipe ids that are off (a server removes one skill from the game) | `...:disabled_recipes` |
| `announce_ultimates` | BOOL | false | one server line when an Ultimate is granted (it names the player: off by default, E5) | `...:announce_ultimates` |

`AdminEntriesTest` count 156 becomes 161 (the part row and four keys); the lang adds `hub.admin.section.evolutionRites` and one `hub.admin.key.<file>:<key>` per row. `AdminCoverageTest` fails the build for a key without a row, so the rows cannot be forgotten.

## 12. Phases

1. **19a engine + our own chain.** The part, config and admin rows, the pure rules, the factory and `AddonProbe` with the hook probe, the unlock/book integration, `great_sage`, GameTests, the 12-run matrix and a server boot with the addon jars. Full plan: [19a](../plans/2026-11-22-plan-19a-evolution-engine.md).
2. **19b Great Sage + FoxAblaze.** `GreatSageProbe` and `FoxAblazeProbe` (single-owner reader), `raphael` per Q1, the `free` grant form, the road-closed test, and the retired-skill check (E14) with its source.
3. **19c Ascension.** `AscensionProbe`, `deus_sanguis`, `blood_sovereign` (and more pairs per Q5), the `permanent` form, the catalyst ingredient.
4. **19d the others and the race links.** `great_mage`, `king_of_curses`, `butcher`, `imitator` per Q6, the four links of section 9, the LinkedMods series.

Each phase ends with its matrix and a server boot with the jars; none changes the wire.

## 13. Testing

- **Pure tests** (JUnit, no game): `EvolutionRulesTest` (order and every reason), `TierTest` (night per tier), recipe codec round trip, the table-vs-rite-file test, a Facts-based test that every target-dependent reason maps to the same performer message.
- **Data tests** (file walk): every `evolve_*.json` has `part_enabled` and, for an addon recipe, `mod_loaded`; an Ultimate recipe's night is the eclipse; every `consumes` skill id is also a `needs` skill id (a rite never consumes what it did not require); every recipe has a `RiteUnlockTable` row, both advancement files, a real and a decoy entry; no decoy page names the skill or a circle; every lang key of section 7 exists.
- **GameTests** (the repo's runner): the hook probe class (resolves each signature of verified-hooks.md that the phase uses, fails with the missing name); the rite end to end on a FakePlayer pair for `great_sage` (success, each refusal of section 7, ingredients back, consent, self-cast, a third mod granting the skill between the check and the grant); absent addon (the unlock filter); the admin switches.
- **The matrix**: the repo's 12 GameTest runs (`default`; `-PnoWerewolves -PnoCurios`; `-PnoLinkedMods`; `-PpartsOff=` each of `species`, `artifacts`, `speciesTensura`, `supernatural`, `supernaturalTensura`, `hud`, `celestials`, `bosses`; `species,artifacts`), plus a 13th for the new part alone: `-PpartsOff=evolutionRites`. The class skips itself with its part off or `-PnoLinkedMods`.
- **A real server boot with all the addon jars** (D11): the dedicated server with Tensura 2.0.1.5 and the jars of the research table (Ascension, Ascension Core, Great Sage, FoxAblaze, Mysticism, Horizons, Kumo Desu ga, Slime Throne Extras) and, in a second boot, the pack's skill-acquisition addons (Skill Obtain, Skill Books, Skill Trainer, Rebalanced). Pass: it reaches "Done", the log has our `EvolutionRitesPart enabled`, no `Registry loading errors`, the hook probe prints every hook found (or the missing one named), `/bloodline rites list` runs. A boot **without** each addon and with the part off passes too. Rite behaviour with a real addon is a manual release check (below).
- **Mutation targets** (the plan lists each with the test that must fail): the night (`ULTIMATE` using the full-moon rule), consent skipped, witch check skipped, a target-dependent refusal said to the performer, single-owner skipped, `consumes` forgotten before the learn, `consumes` forgetting too much, `requires_mod` ignored, the unlock filter ignoring a missing addon, the probe returning `true` on a missing hook, `disabled_recipes` ignored.
- **Manual release checks** (not claimed by tests): a real eclipse rite with a real addon on a client; the circle and the book entry look; Skill Books pricing on `great_sage`.

## 14. Open questions for the owner (with a recommended answer)

The seven are repeated in the plan summary. None blocks 19a.

- **Q1. Raphael.** FoxAblaze fuses it by itself, so a rite adds no road; Great Sage's is closed in 0.0.7.1. *Recommend:* no FoxAblaze Raphael rite and no FoxAblaze rows; keep the single-owner reader and one dormant `raphael` row that opens by itself if Great Sage ever ships `available() == true` (then it grants Great Sage's, and FoxAblaze's only if the owner wants a ceremony for it).
- **Q2. Where an addon has no road** (Sage -> Great Sage, every Unique): what is "the addon's requirement"? *Recommend:* only the addon's own switch, our subject-kind rule, a mastered `tensura:sage` for `great_sage`, and Tensura's own price through plain `learnSkill` (no free tag). No invented EP gates.
- **Q3. Unlock by kind** (D5): by the performer's kinds only, or also the subject's? *Recommend:* both (a vampire learns that the rite exists and can ask a witch).
- **Q4. Where do Ultimate rites' lore come from?** *Recommend:* Finn's Notes (`finn`, already "the rites of the eclipse"): no new item; the addon's own gates keep it hard.
- **Q5. Ascension scope in 19c.** *Recommend:* `deus_sanguis` and `blood_sovereign` only; the other ten pairs are one rite file each later.
- **Q6. 19d fitting uniques.** *Recommend:* `great_mage` and `king_of_curses` (witch), `butcher` (werewolf), `imitator` (siphoner); nothing from Kumo Desu ga or Horizons.
- **Q7. Rebalanced's retired skills.** *Recommend:* never grant a skill in its `retired` tag (E14), so Azathoth, Beelzebuth and Velgrynd can never come from a rite while the pack retires them.

## 15. Left to the builder

Ingredients and altar power per recipe (within E5: no ingredient names the target's state; suggested power 9000 for Uniques, 20000 for Ultimates, as Severance is 6000 and the Broken Line 12000); the exact lang text; the circle size; the hint wording; whether the probes share one reflection helper or one per addon; the GameTest fixtures. Everything else above is decided.
