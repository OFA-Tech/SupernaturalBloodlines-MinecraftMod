# Part: supernatural

Requires: `vampirism`. Optional: `enchanted` (rite tasks), `werewolves` (level steps and pack tasks); a task whose mod is missing is dropped from its rung. Package: `net.ofatech.supernaturalbloodlines.parts.supernatural`. Config key `supernatural`, config file `supernaturalbloodlines-supernatural.toml` (empty for now). Plan: `docs/superpowers/plans/2026-11-14-supernatural-system.md` (phase 12a). Spec: `docs/superpowers/specs/2026-11-14-supernatural-system-design.md`.

**Mixin budget: 1 of 3 used** (`RiteCompleteMixin`, see "The rite signal" below).

## What it holds

- **The race** (`core/supernatural/SupernaturalRace`): computed, never stored. `creature(BloodlineState, factionId)` is the first match of the spec table (Ghoul, Original hybrid, Original, Tribrid, Hybrid, Heretic, Hunter, Vampire, Werewolf, Human); `magic(BloodlineState)` is Witch or Siphoner or nothing. Tribrids and heretics have no magic race. No game classes.
- **The attachment** `supernaturalbloodlines:supernatural` (`SupernaturalState`, own codec, copied on death). Fields: `schema`, `rungs` (high-water mark per race), `race_since`, `sp`, `sp_earned`, `done`, `claimed`, `counters`, `lifetime` (counts that run from the moment the player has a race, for the "in total" tasks), `skills`, `quests`, `granted`, `backfill`. Maps are keyed by `SupernaturalRace.id()` (the lowercase enum name); a key this version does not know is kept and ignored.
- **Server only:** `schema`, `granted`, `backfill`. `SupernaturalState.clientView()` drops them; build every payload through it.
- **Access** (`core/supernatural/SupernaturalAccess`): `get`, `set` (stores, queues the sync, notifies listeners only when the state changed), `addListener`, `syncToSelf`. The sync (`SupernaturalSyncPayload`, to the owning player only) is sent once per server tick however often the state is set, and at login, respawn and dimension change. Known limit: a listener that calls `set` sees its change nested; there is no ordering queue like `BloodlineAccess` has.

## Ladders (data)

One file per race: `data/supernaturalbloodlines/supernatural/ladder/<race>.json`, where `<race>` is `SupernaturalRace.id()` (`human`, `ghoul`, `vampire`, `original`, `original_hybrid`, `hybrid`, `tribrid`, `heretic`, `witch`, `siphoner`, `hunter`, `werewolf`). `LadderReloadListener` loads them on every data reload (only the `supernaturalbloodlines` namespace). The listener is only registered while the part is on, so the files carry no `part_enabled` condition. `LadderTable.parse` takes a file whole or rejects it with one log line (`Ladder rejected: <race>: <reason>`); the other files are unaffected and a rejected race has no ladder. Ids must be unique across all files.

```json
{
    "rungs": [
        { "id": "L-ORI-0", "name": "Immortal", "sp": 0, "ep": 0 },
        { "id": "L-ORI-2", "name": "Bloodmaker", "sp": 3, "ep": 15000, "mods": [], "tasks": [
            { "id": "L-ORI-2a", "detector": "rite", "rite": "supernaturalbloodlines:original_blood", "role": "target", "mod": "enchanted" },
            { "id": "L-ORI-2b", "detector": "level", "faction": "vampirism:vampire", "n": 8 },
            { "id": "L-ORI-2c", "any_of": [
                { "id": "L-ORI-2c.1", "detector": "signal", "signal": "compel", "n": 3 },
                { "id": "L-ORI-2c.2", "detector": "state", "predicate": "moon_unbound" }
            ] }
        ] }
    ]
}
```

- **Rung:** `id`, `name` (English fallback; the hub reads lang keys), `sp` (skill points), `ep` (Tensura EP), `mods` and `parts` (the ladder ends before the first rung whose mod is missing or whose part, by config key, is off), `tasks`. Rung 0 is the race itself: no tasks, no rewards. Rewards are for the whole rung and are split across its top-level tasks when they are claimed: even shares, the remainder on the last task (3 SP over 2 tasks pays 1, then 2).
- **Task:** `id`, `detector`, `n` (count or threshold, default 1), `mod` and `part` (the task is dropped when that mod is missing or that part is off: a signal fired by a part that is off never comes), `total` (a lifetime count, counting only), and every other key is an arg the event must carry (`"any"` matches everything). An arg a detector does not know is kept and logged as a warning (`TaskSpec.Detector.allows`). An `any_of` task has member tasks (with ids of their own) instead of a detector, is done when one member is, and pays as one task. A rung left with no tasks is reached automatically with its whole reward.
- **Detectors** (the fixed set, `TaskSpec.Detector`) and their required args:

| Detector | Kind | Args | Event value |
|---|---|---|---|
| `level` | threshold | `faction` | the level now |
| `state` | threshold | `predicate` (`moon_unbound`, `emissary`, `rank_beta`, `rank_alpha` = ALPHA or TRUE_ALPHA, `beta_days`, `moon_control`, `defender_kills`, `mark_kills`, `siphoned_power`, `hunter_of_the_five`) | the number, or 1 when it holds |
| `sired` | threshold | | the sireline size |
| `days_as` | threshold | `race` | days as that race |
| `tensura_tier` | threshold | | 1 when at the apex tier |
| `feed` | counting | | amount |
| `kill` | counting | `category` (`prey`, `vampire`, `hunter`, `villager`) | amount |
| `rite` | counting | `rite` (id or `any`), `role` (`performer`, `target`), optional `other` | amount |
| `signal` | counting | `signal` | amount |
| `item_use` | counting | `item` | amount |
| `thirst_days` | counting | `min`; `n` is in days, the counter in ticks | ticks |
| `pack_days` | counting | `n` in days | ticks in the pack as a Beta, Alpha or True Alpha |

  Only the active rung of each of the player's ladders (creature and magic) records. A threshold task is done when the value reaches `n`; a counting task adds up. A Ghoul records and claims nothing.
- **In total.** A `total` task reads `lifetime[<race>/<detector>?<args>]`, which grows by every matching event while the player is that race, whatever rung is active (L-SIP-1a, 2b, 4a and L-HER-1b, 3c share the siphon count of their race; L-WIT-3b and 4b the rite count). Tasks that are not `total` count only while their rung is active (spec rule 1). When a rung becomes active, a total that is already enough completes at once.
- **Parts and mods.** `LadderTable.parse(files, modLoaded, partEnabled)`. Each task names the part whose code fires its signal: `species` (compel, sired, dying, venom, siphon, forced shift, the rank/state predicates, the Lineage, Original/Hybrid Blood, Severance rites), `artifacts` (Sun Ring, mountain ash, vervain tea, the Wards, the Cure), `vampirismEnchanted` (the faction rites, "prey"), `enchantedTensura` (Purification). With `-PpartsOff=species` a vampire's L-VAM-3 keeps only its level step and still pays the whole rung (`LadderDataGameTests.aVampireClimbsWhateverPartsAreOff`). The Hunter ladder ends at L-HUN-2 without `species`. Apex rungs need `mods: ["tensura"]` and `parts: ["supernaturalTensura"]`: **without Tensura (or with that part off) every ladder ends before its apex rung, because there are no apex races.**
- **Shares when a mod or part goes away between two claims.** Shares are computed from the rung as it is loaded now. If a task disappears after some of its rung was claimed, the tasks left split the whole total, so the remaining claims can pay more or less than the old shares; nothing already paid is taken back and a claimed task is never paid twice.
- **The one door.** `LadderRules.apply` is the only public way to record an event: it freezes both ladders for a Ghoul creature race, adds the lifetime counts, and moves every matching leaf. `record` and `reach` are package-private.
- **Client table.** `LadderTable.sources()` keeps the accepted files as read. `LadderNet` sends them in `LadderTablePayload` (JSON text by race id) on login and after every data reload (`OnDatapackSyncEvent` fires for both), and the client parses them with its own mods and parts into `LadderTable.clientCopy()`, which the hub and the claim screen read. The packets are registered `optional()`, so a client or server without the part still connects. A file that is not JSON is skipped with one warning. Not testable here without a real client: the codec round trip is (`LadderPayloadTest`).

## Claims, SP and EP (phase 12a Task 4)

- `LadderService.claim(player, taskId)` runs `LadderRules.claim` and applies its result: SP go into the state (`sp`, `spEarned`), EP go through `shared/supernatural/EpReward` (a settable hook; no-op by default; `supernaturalTensura/TensuraEpReward` adds `getEP() + ep` to `TensuraStorages.getExistenceFrom(p)` and calls `markDirty()`: flat, no +25% boost), the player gets a chat line (`claim.paid`, `claim.rung_reached`). A refusal sends its own lang key (`claim.ghoul_frozen`, `unknown_task`, `not_active`, `not_done`, `already_claimed`) and changes nothing.
- The only client action is `ClaimTaskPayload` (a task id of at most 64 characters). The server decides everything: the id is only a name, an unknown one is refused (`anInventedTaskIdIsRefused`).
- A rung left with no tasks (its mod or part is off) is reached on its own, with its SP and EP, by `TaskTracker` after every event (`LadderRules.settle`); a Ghoul is not paid.
- `shared/supernatural/LadderQuery` is what other parts ask: `layerSlice(player)` (empty = the whole layer) and `rung(player, race)`. Without this part the answers are "the whole layer" and 0.

## The rite signal (one mixin on Enchanted's `Rite`)

Enchanted 4.2.7 has no event for a rite that worked (its `*Events` classes are client, common, effects, poppets). The reliable point is `Rite.start()`: it calls `stop()` only when `onStart` returned false (the rite is finished), and a rite that refuses calls `cancel()` first, so a `@Unique` flag set at the head of `cancel()` tells the two apart. Both hooks are `require = 0`; the plugin checks in `postApply` that both landed and logs `RiteCompleteMixin hook in place` or a warning. It is applied only when Enchanted is installed and the part is on; the switch is `rite_complete_signal` in `supernaturalbloodlines-supernatural.toml` (the plan's `supernatural.mixin.rite_complete` name was not used: that file is already the part's). `RiteSignal` fires the datapack rite id for the caster and the target through `TaskSignals.rite`, so it is the single signal for every rite, Enchanted's own types and ours (our factories fire nothing any more). A rite that keeps running and ends in `tick()` is not signalled; none of the rites the ladders ask for does that. Checked with real rites in `RiteSignalGameTests`: an Enchanted type counts once, one of ours counts once, a cancelled rite counts for nothing, and the switch turns it off.
## What the tasks count, exactly (notes from the Task 3 review)

- **L-HYB-2a ("thirst 15+ for 3 days in total")** is summed only while the rung is active, because `thirst_days` cannot be a lifetime count (a `total` on it is rejected when the file is read).
- **A bite is one feed.** Each bite that drinks counts as one `feed`, however much blood it took.
- **PvP counts for `prey_hit_survived`.** A hit on a player by a vampire or werewolf player counts like a hit by a mob of those types. That is accepted.
- **Prey tasks need `vampirismEnchanted`.** "Prey" (vampire and werewolf types, never witches or siphoners) is defined by that part, so every `kill category=prey` and `siphon_entity prey=true` task carries `"part": "vampirismEnchanted"` (`LadderDataTest`). L-HER-2a asks for vampire-type victims only and uses `vampiric=true` instead, so a werewolf does not count.
- **`dying_survived` has two fire points** (the Downed state of hybrids and Vampirism's dying state of Originals), so one event can come from either; a task must not use `n > 1` for it without a guard (the shipped L-ORI-3a uses `n = 1`).
- **Mountain ash** is counted in a LOWEST-priority `EntityPlaceEvent` listener that skips cancelled placements, not in `setPlacedBy`.
- **FakePlayers do not climb ladders** (deployers, turrets). The GameTest server turns this off for its own FakePlayers only (`SupernaturalAccess.trackFakePlayers`).
## Off

With the part off nothing reads or writes the attachment. Saved progress stays on the player and is back when the part is turned on again.

## Migration (phase 12a Task 5)

`Detectors.onLogin` runs `Migration.runIfNeeded` first, then the first read of the state, so the race is stamped from `turnedAt` and not from "now" (a long-time vampire keeps their days: `aLongTimeVampireKeepsTheirDaysAfterTheUpdate`). The rules are pure (`core/supernatural/MigrationRules`, `MigrationRulesTest`), run once (`schema` 0 to 1) and only add:

1. `race_since` = `turnedAt` of the current species, else now (the creature race and the magic race both).
2. Tasks the state proves are marked done: `level`, `state`, `sired`, `days_as`. A count (feeds, rites, kills, venom) has no history and is never proven.
3. The rung goes up while every task of the active rung is proven (their tasks are marked claimed), then up to the highest floor: the **layer rung** of the species (every layer skill and bonus a player holds today stays), **Alpha or True Alpha rank to L-WOL-5**, the **apex floor** (the top rung of the ladder of the apex race the player is, such as `primordial_vampire`; clipped to the ladder, so without Tensura it is the last rung there). A True Alpha who is not a werewolf now keeps L-WOL-5 dormant; an Alpha who is cured does not (only the True Alpha rank is the R91 proof kept past the cure).
4. The SP of every rung given are paid into the pool (`sp` and `spEarned`), no EP (they earned it playing). A rung left with no tasks is reached on its own with its reward.
5. `backfill` lists the rungs it gave. A Ghoul gets the schema and nothing else.

Debug commands (permission 2): `debug supernatural <player> show | backfill | reset | sp <n> | rung <race> <n>`. `reset` puts the state back to "never migrated" so the next login migrates again; `rung` sets the high-water mark with no reward.

## Skills (phase 12c, Task 1)

- **Catalogue and trees (data).** `data/supernaturalbloodlines/supernatural/skill/<id>.json` (id in lower case, `s-vam-4.json`) and `.../tree/<race>.json`. 40 buyable skills and one granted-only skill (S-WOL-8 Primeval Roar: `granted_only`, cost 0, never in a tree; its grant belongs to phase 12d), 11 trees (a Ghoul has none). `SkillReloadListener` loads both folders on every data reload; like the ladders it exists only while the part is on. `SkillTable.parse` takes a skill or a tree file whole or rejects it with one log line (`Skill data rejected: ...`), never a crash.
- **A skill:** `id`, `name` (English fallback), `cost`, `kinds` (`attr`, `rule`, `tensura`, `blood`; S-WIT-7 is `attr` and `rule`), and what the kinds need: `attribute` (`id`, `operation` = `add_value`, `add_multiplied_base`, `add_multiplied_total`, `amount`, `when` = `always`, `night`, `full_moon_night`), `tensura_skill` (a TENSURA skill is hidden without Tensura, and so is every tree entry after it) and `params`, the numbers a RULE or BLOOD skill gives. A rule asks `LadderQuery.value(player, "S-VAM-3", "range", 4)`: the number from the JSON when the player owns the skill, else its own base. The base numbers stay in the rules' code.
- **A tree:** `skills: [{ skill, rung, after?, quest? }]`. `rung` is the rung of that race's own ladder; `after` must be listed earlier in the same tree; `quest` is a quest id (Q-VAM-6, phase 12c Task 4).
- **Owned and active.** `skills` in the state is what was bought; a skill is **active** only while the tree of the player's creature or magic race lists it (`SkillRules.active`), dormant otherwise. Nothing bought is ever lost. `LadderQuery.owns(player, id)` is true only for owned and active. A Ghoul has no active skill.
- **Buying.** `BuySkillPayload(skillId)` (at most 64 characters) goes to `SkillService.buy`, which runs `SkillRules.buy` against the server's state: not a Ghoul, a known skill, not granted-only, not owned, listed in a tree of the player's, the tree's rung reached, `after` owned, the quest done, enough SP. Each refusal has its own lang key (`supernatural.skill.refuse.*`). A skill listed in both trees of a witch-werewolf is bought once (the first tree whose requirements are met is enough); a skill listed in two races' trees (S-VAM-1 for a Vampire and an Original) stays owned when the race changes. The shipped trees share no skill between a creature tree and a magic tree, so a witch-werewolf owns two disjoint sets today.
- **ATTR effects** (`SkillEffects`): one permanent modifier per skill with our id `supernaturalbloodlines:skill/<id>`, set when the skill is active and its `when` holds, removed when not. It runs on login, respawn, a faction or species change, a change of skills, and every 200 ticks (night and full moon). The night is 13000 to 23000 ticks of the day; the full moon is Werewolves' own window (after 12786 and before 23216 on the phase-0 day). A fixed-time dimension has neither.
- **TENSURA effects** (`supernaturalTensura/TensuraSkillEffects`): granted through `SkillOwners` source `supernatural_skill`, tracked in `granted`. A skill the player had before is theirs and never forgotten; one shared with the species layer or the werewolf grants stays while any source wants it. A data reload that loses the whole catalogue changes nothing.
- **Known limit:** the client does not get the skill table yet (the hub, phase 12e, needs it); the purchase is server-checked.

## Skill rules (phase 12c, Task 2)

A RULE skill is a number one of our rules reads through `LadderQuery.value(player, skillId, key, base)`: the `params` number of the skill JSON when the player owns it (owned and active), else the rule's own base. The rule keeps its behaviour and its tests; a skill adds nothing else. GameTests compare the same player with and without the skill (`SkillRuleGameTests`, `WerewolfSkillRuleGameTests` in species, `ArtifactsSkillRuleGameTests`, `ArtifactsWerewolfSkillRuleGameTests` in artifacts, `SpeciesTensuraSkillRuleGameTests` in species_tensura; each is registered only when the part is on and `supernatural` is on); the pure rows are in `SkillRuleMathTest`.

| Skill | Where | What |
|---|---|---|
| S-HUM-2 | `artifacts/VervainRules.teaTicks` | after the tea is drunk, the `vervain` effect is replaced by a 24,000 tick one |
| S-VAM-3 | `species/sireline/Compulsion` | eye-contact reach 4 to 6; villager discount: see SPECIES-RULES.md, "Skills" (capped by the villager at +25 reputation) |
| S-VAM-7 | `species/ElderSkin` | `vampirism:sun_damage` x0.75 in `LivingIncomingDamageEvent`; a bound Sun Ring cancels it first |
| S-ORI-2 | `species/rite/OriginalBloodRiteFactory` | 2 bottles (the target Original's skill) |
| S-ORI-3 | `species/sireline/SireCall.cooldownTicks` | 12,000 to 6,000 ticks |
| S-ORI-4, S-WIT-7 | `speciestensura/SpeciesLayer.sync` | +0.05 on `layer_magicule` / `layer_aura`; the layer re-syncs when skills change |
| S-HYB-1 | `species/hybrid/BloodThirst.fallTicks` | thirst falls every 2,400 ticks |
| S-HYB-2 | `species/hybrid/Venom` | Slowness I for 100 ticks on the hit that applies the venom |
| S-WOL-2 | `shared/werewolves/ForcedShift.moonCalmSpares` | a second per-night coin (`MoonControl.sparedOn`, independent of the born wolf's) spares 25% of the nights |
| S-WOL-4 | `artifacts/werewolves/WolfsbaneStrains.commonTicks` | common strain 600 to 300 ticks |
| S-WOL-5 | `species/pack/PackBonus.betasNeeded` | 3 to 2 Betas |
| S-WOL-6 | `species/pack/AlphaRoar.cooldown` | 1,200 to 800 ticks |
| S-WIT-1, S-WIT-4, S-WIT-6, S-SIP-4 | `species/witch/RiteCost.begin` | the caster's multipliers: x0.95, Emissary discount 10% to 20%, x0.5 on the Rites of Lineage, siphoned power covers the cost at x0.9. Order: witch (round up), Emissary (round up), then the skills (round up) |
| S-WIT-2 | `species/witch/WitchAltarBonus.rangeFor` | altar range x1.75 instead of x1.5 (the widest witch at the altar) |
| S-SIP-1, -2, -3, -5 | `species/witch/Siphoning` | cap 1,000 to 1,500; 50 to 75 power per siphon; heal 1 per 100 power taken from a creature; Weakness I for 5 s on a vampire or werewolf type |
| S-HUN-1 | `species/hunter/SteadyAim` | x1.05 on damage to prey, `LivingIncomingDamageEvent` |
| S-HUN-2, S-HUN-3 | `species/hunter/HunterInstinct.range / idleTicks` | sense radius 24 to 32; instinct after 5 days |
| S-HUN-5 | `species/hunter/MarkTracker.perKill` | the mark at the cap of 20 kills gives +25% attack and +12.5% speed (per kill 1.25% and 0.625%) |

- **No new mixin.** Only the existing `RiteCostMixin` (species, still 3 of 3) now also reads the circle's rite type through a `@Shadow` of `GoldChalkBlockEntity.type`, so Ancestral Voice can tell a Rite of Lineage from another rite; the lookup is the same registry key `RiteSignal` uses.
- **S-WIT-3 Ashwarden has no effect**: a mountain ash line is already unbreakable for vampire and werewolf types whoever placed it, so there is no number to change. The skill stays in the catalogue and in the Witch tree (S-WIT-7 comes after it); owner to patch.
- **Not tested end to end:** S-WIT-4 (an Emissary near the pack needs a full pack set-up); its multiplier is covered by the `RiteCost.split` rows and the wiring of `RiteCost.begin` by the S-WIT-1 and S-WIT-6 tests.

## Blood skills (phase 12c, Task 3)

- **Keen Fangs (S-VAM-1)** (`skill/BloodEffects`): a listener on Vampirism's `BloodDrinkEvent.PlayerDrinkBloodEvent` at `LOW` adds the skill's `extra_blood` (1) to the amount of a feed from a human-like victim (`HumanLikeBlood`, plan 11's check when the species part is on, else villagers and traders). The amount of the event is settable and is what the vampire's blood level receives, so nothing is added after the drink. It runs after the Vervain rule, which sets the amount to 0 for a protected victim; a feed that gave 0 stays 0. A dormant skill (the player's race tree does not list it) adds nothing. Test: `BloodSkillGameTests.keenFangsAddsBlood`.
- Predator's Thirst (S-VAM-6) is the other BLOOD skill (phase 12c Task 4).

## Blood skills (phase 12c, Task 3)

- **Keen Fangs (S-VAM-1)** (`skill/BloodEffects`): a listener on Vampirism's `BloodDrinkEvent.PlayerDrinkBloodEvent` at `LOW` adds the skill's `extra_blood` (1) to the amount of a feed from a human-like victim (`HumanLikeBlood`, plan 11's check when the species part is on, else villagers and traders). The amount of the event is settable and is what the vampire's blood level receives, so nothing is added after the drink. It runs after the Vervain rule, which sets the amount to 0 for a protected victim; a feed that gave 0 stays 0. A dormant skill (the player's race tree does not list it) adds nothing. Test: `BloodSkillGameTests.keenFangsAddsBlood`.
- Predator's Thirst (S-VAM-6) is the other BLOOD skill (phase 12c Task 4).
