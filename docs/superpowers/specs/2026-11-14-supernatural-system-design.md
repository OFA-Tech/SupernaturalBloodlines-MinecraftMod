# Plan 12: the supernatural race system and the hub (design, 2026-11-14)

Direction approved by the owner in conversation on 2026-11-14. `SPECIES-RULES.md` stays the source of truth for species behaviour: where this design and a rule disagree, the rule wins and this file gets patched.

**How to patch this file.** Every rung, task and skill has a stable id (`L-VAM-3`, `L-VAM-3b`, `S-HYB-2`, `Q-VAM-6`). Patch by id, as with `SPECIES-RULES.md`. Every value the designer invented is marked *(draft, owner to patch)*; in the content tables a whole table carries the mark once in its heading, and it covers every number and name in it.

Source tags used in the tables:

| Tag | Source | Detected through |
|---|---|---|
| **[SB]** | our own species task | our state, our events, NeoForge events (see "Detectors") |
| **[VMP]** | a Vampirism level step | `PlayerFactionEvent.FactionLevelChanged` and a level check |
| **[WW]** | a Werewolves level step | the same Vampirism faction event (Werewolves is a Vampirism faction) |
| **[ENC]** | an Enchanted rite | our rite factories (and optionally Enchanted's own rites, see "Constraints") |
| **[TEN]** | a Tensura state | `RaceAPI` / `IExistence`, read when a rung becomes active and on `RaceEvents` |

---

## Why

Today a player's supernatural life is spread over five mods and a dozen screens: Tensura's race and evolution screens, Vampirism's and Werewolves' skill trees and altars, Enchanted's rites, and our own commands (`/bloodline tree`, `/pack ...`). Our species are invisible except through chat messages, and everything a species grants (plan 05's layer) arrives at once and then nothing more happens.

The owner wants:
- a **supernatural race** next to the Tensura race, so every player (humans too) has a visible place in the supernatural world;
- a **ladder** per race, climbed by doing things the mods already track, paying our own skill points and Tensura EP;
- **one hub** that shows everything supernatural, lets the player act on our things and on Vampirism/Werewolves skills, and launches Tensura's and Enchanted's own screens unchanged.

## Goals

- A supernatural race for every player, derived from our own storage: Human, Ghoul, Vampire, Original, Hybrid, Original hybrid, Tribrid, Heretic, Witch, Siphoner, Hunter, Werewolf.
- A ladder per race. The plan 05 layer (skills, magicule/aura bonus) is spread over the rungs. The plan 05 apex race is the top rung where one exists.
- Three kinds of tasks: our species tasks, Vampirism/Werewolves level steps shown as tasks (their mechanics still run underneath), and Enchanted rites.
- Rewards in **our skill points (SP)** and **Tensura EP**. SP buy our species skills. Vampirism and Werewolves skills keep their own points.
- The vampire-feeding skill (S-VAM-6) with its unlock quest (Q-VAM-6), lifting plan 11's "biting a vampire never feeds you".
- The hub, with three themes (Grimoire default, Blood Moon, Tensura expansion) over one screen model, pages Path, Skills, Quests, Rites, Bloodline, Pack.
- Two entry points: a 5th tab in Tensura's menu, and our own key.
- Plan 13's Supernatural-mode assignment screen moves into the hub.
- Ladders and skill trees are **data** (datapack JSON), so the owner's patches don't need code.
- Existing players are back-filled without losing anything.

## Non-goals

- Changing Tensura's race storage. ManasCore keeps exactly one race; the Tensura race stays Tensura's (R3, R100).
- Replacing any mod's screen, skill tree, levelling or points. The hub reads them and calls their own server paths.
- New species, new rites or new turning routes. Every way into a race stays what `SPECIES-RULES.md` and plan 11 say.
- Active abilities for our skills. Every skill in this plan is a passive or a change to an existing rule; plan 13's HUD stays the place for actions.
- Per-race leaderboards, quests shared by a pack, or server-wide events.

## Anti-goals (a failure even if it "works")

- **Taking anything away.** No skill, layer grant, level, EP, rank or apex race a player has today may disappear on update or on a ladder change. A rung, once reached, is never lost.
- **Gating existing rules behind rungs.** A species' rule perks (R31's +20% attack, R42's thirst, R61's altar bonus, R81's mark, R91's rank perks) stay rule-level and apply from the moment you are that species. Only the plan 05 layer moves onto rungs.
- **Gating weaknesses.** Undead treatment, sun, holy damage, venom, wolfsbane apply from rung 0. A ladder never removes a weakness.
- **Skipping Tensura evolution.** EP rewards add EP; they never touch an evolution's requirements (R100). An apex rung only *offers* the apex race in Tensura's own evolution screen, which still needs its 2,000,000 EP.
- **A second source of truth.** The supernatural race is computed from state we already keep; it is never stored as its own field that could drift.
- **Ghouls progressing.** A Ghoul records no task progress, earns no SP and spends none until the turning is finished (plan 11).
- **Copying Tensura's art.** The Tensura theme draws Tensura's own textures by resource location at runtime; nothing of Tensura's is copied into the repo (Tensura is ARR).
- **Breaking when a mod is missing.** Whatever isn't installed simply isn't shown.

## Constraints

- **Mixin budget.** Each part has 3 mixins and every current part is full (`species`: AltarRange, RiteCancel, RiteCost; `species_tensura`: RaceInstance, RaceUtils, SireNaming; `vampirism_tensura`: RaceUtilsSun, VampirePlayerBite, VampireRaceSun). Plan 13 has its own new part. This plan adds **two new parts** with their own budgets:

| Part | Package | Required mods | Optional | Mixins planned |
|---|---|---|---|---|
| `supernatural` | `...parts.supernatural` | vampirism | werewolves, enchanted | 0, with 1 reserved: an `@Inject` on Enchanted's `Rite` completion (`require = 0`) so Enchanted's own rites count as "rites performed". Without it, only our rites count. |
| `supernatural_tensura` | `...parts.supernaturaltensura` | vampirism, tensura | werewolves | 0, with 1 reserved: the Tensura menu tab, only if `ScreenEvent.Init.Post` can't place it |

  Everything else is events (`FactionLevelChanged`, `BloodDrinkEvent`, `LivingDeathEvent`, `LivingEntityUseItemEvent.Finish`, `BlockEvent.EntityPlaceEvent`, `ScreenEvent.*`, `RaceEvents`) and our own listeners.
- **The 16-field codec cap.** `BloodlineState` is at 16 base fields plus the `Extra` group, and plan 11 adds three more to `Extra`. This plan adds **nothing** to `BloodlineState`. All new data goes in a **new attachment** `supernaturalbloodlines:supernatural` with its own codec (12 fields, under the cap).
- **Parts never import each other.** Shared hooks live in `shared/`:
  - `shared/supernatural/SupernaturalRace` (enum + `of(...)`, pure, in `core` if it needs no mod class);
  - `shared/supernatural/TaskSignals`: a tiny bus. Other parts fire `(player, signalId, amount)` (a compulsion, a siphon, a venom hit, a rite done, a naming); the `supernatural` part listens. Firing with no listener is a no-op.
  - `shared/supernatural/LadderQuery`: what the player's rungs grant (layer skills, bonus fractions, owned skills). `species_tensura`'s `SpeciesLayer` and `ApexOffer` read it; `species`, `artifacts` and `species_tensura` read owned skills through it for rule numbers.
  - `shared/hud/LoadoutAccess`: plan 13's 6-id list, so the hub can edit it without importing plan 13's part.
- **Optional mods** (each absent mod is handled the same way: its tasks, pages and launchers are not shown; a rung whose tasks are all from an absent mod is skipped):

| Absent | Effect |
|---|---|
| Tensura | No EP rewards (hidden), no [TEN] tasks, no apex rungs (each ladder ends one rung lower), no Tensura-skill grants, no Tensura tab or launcher, no Tensura theme (falls back to Grimoire). `supernatural_tensura` does not load. |
| Werewolves | Werewolf, Hybrid, Original hybrid and Tribrid can't exist (species rules). Pack page hidden; [WW] tasks never appear. |
| Enchanted | The `species` part is off, so only Human, Ghoul, Vampire, Hunter exist. Rites page and Enchanted launcher hidden; [ENC] tasks dropped. The Hunter ladder stops at L-HUN-2 (the Five need a rite). |
| Vampirism | Both parts off. No hub. |
| Plan 13's part | The Loadout tab is hidden. |

- **Performance.** No per-tick scans. Time and thirst tasks are checked every 200 ticks per online player, the way `RankSkills` already rechecks.
- **Data-first.** Ladders and skill trees are datapack JSON under `data/supernaturalbloodlines/supernatural/ladder/<race>.json` and `.../skill/<id>.json`, gated with `neoforge:conditions` per mod. Detectors and skill effects are a fixed set in code; JSON picks and tunes them. Rewards and costs are in JSON.

## Approaches

**How to store the supernatural race**

| Option | What it means | Verdict |
|---|---|---|
| **A. Derived race, separate progress attachment (chosen)** | The race is computed from `BloodlineState` (species, ghoul flag, witch layer) and the Vampirism faction. Ladder progress, SP and skills live in a new attachment. | Can't drift; no codec-cap pressure; the existing rules stay the only authority on what you are. |
| B. Store the race as a field | One more field to keep in step with every rule transition | Two sources of truth. Rejected. |
| C. Each race as a Vampirism faction | Rebuild forms, blood, levelling per race | Rejected already in the 2026-11-11 design. |

**How far the hub reaches** (owner: option C)

| Option | Verdict |
|---|---|
| A. Read-only overview | Too little: the owner wants to act from the hub. |
| B. Rebuild every mod's screen inside the hub | Breaks on every update of four mods. Rejected. |
| **C. Act on ours + Vampirism/Werewolves skills; launch Tensura and Enchanted (chosen)** | Our things and the two skill trees whose API exposes unlocking are in the hub; Tensura and Enchanted keep their own screens, opened from a launcher, Esc returns. |

**Where the ladder content lives**

| Option | Verdict |
|---|---|
| Hard-coded tables | Every owner patch is a code change. |
| **Datapack JSON with a fixed detector set (chosen)** | The owner (or a pack maker) patches numbers and tasks by id; code only grows when a new detector is needed. |

## Data model

### The supernatural race (computed, never stored)

`SupernaturalRace.of(state, factionId)`, first match wins:

| # | Condition | Race |
|---|---|---|
| 1 | `state.ghoul()` (plan 11) | GHOUL |
| 2 | species ORIGINAL_HYBRID | ORIGINAL_HYBRID |
| 3 | species ORIGINAL | ORIGINAL |
| 4 | species TRIBRID | TRIBRID |
| 5 | species HYBRID | HYBRID |
| 6 | species HERETIC | HERETIC |
| 7 | species SIPHONER | SIPHONER |
| 8 | species WITCH (also a witch-werewolf) | WITCH *(draft, owner to patch: a witch-werewolf could instead be a Werewolf with the witch layer)* |
| 9 | species POTENTIAL or HUNTER_OF_THE_FIVE, or faction `vampirism:hunter` | HUNTER |
| 10 | faction `vampirism:vampire` (Tensura vampire-line races in the faction included, R20) | VAMPIRE |
| 11 | faction `werewolves:werewolf` | WEREWOLF |
| 12 | anything else | HUMAN |

Hybrid-types are Werewolves-faction players with a vampire layer (R2): their ladders use **[WW]** steps and their vampire side comes from our skill tree, never from Vampirism vampire levels.

### The new attachment `supernaturalbloodlines:supernatural`

`SupernaturalState`, copied on death (R4: ordinary deaths keep everything).

| Field | Type | Synced | Meaning |
|---|---|---|---|
| `schema` | int | no | 0 = never migrated; 1 = this design |
| `rungs` | map race → int | yes | highest rung reached per race; kept when the race changes, so a returning race resumes |
| `race_since` | map race → long | yes | game time the player became that race (for "days as" tasks) |
| `sp` | int | yes | unspent skill points (one pool per player, carried across races) |
| `sp_earned` | int | yes | lifetime SP, for display and audits |
| `done` | set of task ids | yes | tasks whose condition is met |
| `claimed` | set of task ids | yes | tasks whose reward was paid |
| `counters` | map task id → long | yes | progress of counting tasks of the active rung; cleared when the task is done |
| `skills` | set of skill ids | yes | our skills bought |
| `quests` | set of quest ids | yes | skill quests finished (Q-VAM-6) |
| `granted` | set of strings | **no** | Tensura skills and attribute ids our skills granted, through `SkillOwners` source `supernatural_skill`. Only these are ever taken back. |
| `backfill` | list of strings | **no** | what migration gave, for `/supernaturalbloodlines debug supernatural` |

### Sync

- **S2C `SupernaturalSyncPayload`**: the synced fields, to the owning player only, on login, respawn, dimension change, and any change (coalesced to once per tick per player).
- **On hub open, S2C `HubSnapshotPayload`** (request/response, not kept in sync): what the hub shows about *other* players and world data: sire and sired names (`SirelineIndex`), pack members and ranks (`PackIndex`), the Five roster count, severance state. Only names, ranks and online flags; never other players' attachments.
- **Client reads directly** (no sync from us): Vampirism/Werewolves level, skills and points through their own synced client data; Tensura race, EP and magicules through Tensura's own client sync; Enchanted progress from what the client already has (else "open the book" only).
- **Server-only:** `granted`, `backfill`, counters' raw events, every rule decision. The client never decides a claim, a purchase or a rung.

### C2S actions (all validated on the server)

| Packet | Server checks |
|---|---|
| `ClaimTask(taskId)` | task belongs to the active rung of the current race; `done` and not `claimed`; not a Ghoul |
| `BuySkill(skillId)` | listed in the current race's tree; rung and prerequisite skill met; quest done if any; `sp >= cost`; not owned; not a Ghoul |
| `UnlockModSkill(factionSkillId)` | calls Vampirism's own skill-handler path (its points, parents and exclusions); refused for Ghouls (plan 11 gives them none anyway) |
| `SetLoadout(slot, actionId)` | forwarded to `LoadoutAccess` with plan 13's own checks |
| `OpenLauncher(target)` | client-only, no packet |

## Task system

### Kinds

| Kind | What completes it | Detector |
|---|---|---|
| **Species task [SB]** | a state or a counted event of ours | see "Detectors" |
| **Level step [VMP]/[WW]** | reaching level N in that faction | `PlayerFactionEvent.FactionLevelChanged`, plus a level check whenever a rung becomes active |
| **Rite [ENC]** | performing a rite (role *performer*: the player who started it) or being its *target* | our rite factories fire `TaskSignals` `rite:<id>:performer` / `rite:<id>:target` on success; Enchanted's own rites only through the reserved mixin |
| **Tensura state [TEN]** | race tier or naming | `RaceAPI` read on activation and on `RaceEvents.SET_RACE`; naming via `SireNaming` firing a signal |

**Option C for level steps.** A [VMP]/[WW] task is the mod's own level-up shown in our hub, with the mod's hint for how to do it ("Altar of Inspiration", "Altar of Infusion", "Hunter Table", the Werewolves altar). The player levels in that mod's own way; our task completes on the event. Nothing in that mod is skipped or replaced. The hub shows that mod's requirements text where its API or lang keys expose it, else our own hint line.

### Detectors (the fixed set)

| Detector | Source | Notes |
|---|---|---|
| `level(faction, n)` | `FactionLevelChanged`, level check | Werewolves uses Vampirism's faction API |
| `state(predicate)` | `BloodlineAccess` listener + check on activation | species, unbound, rank, `markKills`, `defenderKills`, `siphonedPower`, emissary, moon control (`shared/werewolves/MoonControl`) |
| `feed(human_like, n)` | `BloodDrinkEvent.PlayerDrinkBloodEvent`, and a signal from our Bite key / Tensura race feed (plan 13) | human-like = plan 05's list plus villagers and Vampirism human creatures (owner, 2026-11-14) |
| `kill(category, n)` | `LivingDeathEvent`, killer is the player | categories: `prey` (R81, existing check), `vampire`, `hunter`, `villager` |
| `rite(id, role, n)` | `TaskSignals` from rite factories | `any` matches every counted rite |
| `signal(id, n)` | `TaskSignals` from other parts | `compel`, `siphon_power`, `siphon_entity`, `venom_hit`, `dying_survived` (R32), `forced_shift_survived`, `vampire_bite_refused` (plan 11), `sire_named` (R101), `sun_ring_bound`, `mountain_ash_placed` |
| `item_use(item, n)` | `LivingEntityUseItemEvent.Finish` | vervain tea |
| `sired(n)` | `SirelineIndex` size, checked on activation and on its change | players or mobs |
| `days_as(race, n)` | game time − `race_since[race]`, checked every 200 ticks | in-game days of 24,000 ticks |
| `thirst_days(min, n)` | every 200 ticks, adds 200 to the counter while `thirst >= min` | hybrids |
| `tensura_tier(saint)` | `HumanLikeLine.isSaintOrHigher` on the current race | the same gate `ApexOffer` already uses |

### Rules

1. **Only the active rung's tasks track** (the rung after the highest reached). State and level detectors are re-checked the moment a rung becomes active, so a level reached early counts at once. Counting detectors (kills, feeds, rites, signals) only count while their task is active. *(draft, owner to patch)*
2. **Claim.** A done task shows a claim button. Claiming pays its share of the rung's rewards: the rung's SP and EP split evenly across its tasks, the remainder on the last. EP is added through `IExistence` (`markDirty()`) as a flat sum: no +25% boost, no evolution requirement touched.
3. **Rung up.** Claiming the last task of a rung reaches it: `rungs[race]++`, its grants apply, a toast shows, and the next rung's tasks activate.
4. **Any-of groups.** A rung may list tasks as "one of" (marked **or** in the tables). One done member completes the group; the group pays as one task.
5. **Missing mods.** A task whose source mod is absent is dropped from its rung. A rung left with no tasks is reached automatically with its rewards (without the EP when Tensura is absent).
6. **Ghouls are frozen.** While `ghoul`, no detector records, nothing can be claimed or bought, and the ladder shows the turning instead (L-GHO-0). EP itself keeps accumulating in Tensura (plan 11).
7. **Race changes.** The new race starts at its stored rung (0 the first time) and `race_since` is stamped on first entry. The old race's rung and grants stay recorded; its layer grants follow the species layer as today (they leave with the species), and its skills go dormant unless the new tree lists them. SP is never refunded or lost.
8. **Grants that need a state** (a layer on a species, the apex offer, a rank perk) are dormant while the state is missing and return with it. This is how R82 (a hunter losing the mark) and R91 (a cured True Alpha) stay true.
9. **Persistence.** Everything is in the attachment, saved with the player. Rite and kill signals are applied on the server thread at once; nothing is queued across restarts.

### What a rung grants

- **Layer slices.** Each species' plan 05 `LayerTable` entry is spread over the rungs: the skills one by one, and the magicule/aura bonus as a cumulative fraction. `SpeciesLayer.sync` asks `LadderQuery` for the granted slice instead of the whole `LayerSpec`. When the `supernatural` part is off, `LadderQuery` answers "the whole layer", which is today's behaviour. The **layer rung** of each race is the rung by which the whole plan 05 layer is granted.
- **Undead treatment** (`treatAsUndead`) and the R100 EP gain (+25%) are natures, not rewards: they apply from rung 0.
- **The apex offer.** The top rung of a race with a plan 05 apex makes `ApexOffer.apexIdFor` answer for that player (it now also requires the apex rung reached). The player still evolves in Tensura's evolution screen and still pays 2,000,000 EP.
- **SP and EP** through claims.
- **Unlocks** in the skill tree (a skill's "needs rung").

---

## Ladders

Columns: **Id**, **Rung**, **Tasks** (with source), **SP / EP** (rung total, split across its tasks), **Grants**. Rung 0 is reached on becoming the race and pays nothing. "Werewolves max level" and "Vampirism max level" mean the mod's own maximum (14 in the pinned versions; the builder confirms).

Layer skills are Tensura ids (`tensura:` omitted). Magicule/aura bonuses are cumulative totals at that rung.

### Human: L-HUM *(draft, owner to patch)*

No plan 05 layer, no apex. SP earned here carry into whatever the player becomes.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-HUM-0 | Human | (default) | 0 / 0 | the slot; Human tree |
| L-HUM-1 | Aware | a) drink vervain tea once [SB `item_use`]; b) survive a hit from a vampire or werewolf type (`kill`-side check: damaged by prey, still alive) [SB] | 2 / 1,000 | S-HUM-1 unlock |
| L-HUM-2 | Prepared | a) be the target of the Rite of Lineage (Reveal) [ENC]; b) kill 5 Vampirism vampire or Werewolves mobs [SB `kill`] | 3 / 3,000 | S-HUM-2, S-HUM-3 unlock |

### Ghoul: L-GHO

Frozen (owner rule). One rung that shows plan 11's turning as steps. Nothing to claim, no tree.

| Id | Rung | Steps shown (not claimable) | SP / EP | Grants |
|---|---|---|---|---|
| L-GHO-0 | Ghoul | 1) vampire blood in your system (plan 11 `vampireBloodUntil`); 2) die with it (transition starts, `turningTicksLeft`); 3) drink human-like blood (villagers count) before the timer ends | 0 / 0 | none. On completion the race becomes Vampire at L-VAM-0. |

### Vampire: L-VAM *(draft, owner to patch)*

Plain Vampirism vampires with no species, Tensura vampire-line races in the faction included. Plan 05 layer key `VAMPIRE`: `drain`, undead. **No plan 05 apex** (see "Could not ground"). Layer rung: 1.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-VAM-0 | Fledgling | (turning finished, or an existing vampire) | 0 / 0 | undead treatment |
| L-VAM-1 | Thirst | a) Vampirism level 2 [VMP]; b) drink human-like blood 10 times [SB `feed`] | 2 / 2,000 | `drain` (whole VAMPIRE layer) |
| L-VAM-2 | Night Walker | a) Vampirism level 4 (Altar of Inspiration) [VMP]; b) have a Sun Ring bound to you (Ward of Daylight, target) [ENC / SB `sun_ring_bound`] | 3 / 5,000 | — |
| L-VAM-3 | Compeller | a) Vampirism level 10 (R23) [VMP]; b) compel 3 villagers or lesser vampires [SB `compel`] | 4 / 15,000 | — |
| L-VAM-4 | Predator | a) Vampirism level 12 [VMP]; b) kill 10 hunters (mobs or players) [SB `kill`] | 4 / 30,000 | Q-VAM-6 opens |
| L-VAM-5 | Elder (top) | a) Vampirism max level [VMP]; b) 30 days as a vampire [SB `days_as`] | 5 / 60,000 | title "Elder" |

### Original: L-ORI *(draft, owner to patch)*

Layer `ORIGINAL`: `blood_mist, drain, charm, self_regeneration`, magicule 0.20, undead. Apex `primordial_vampire`. Layer rung: 3. R31–R34 perks are rule-level from rung 0.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-ORI-0 | Immortal | (Rite of Immortality) | 0 / 0 | `drain`; undead treatment |
| L-ORI-1 | Eternal Hunger | a) Vampirism level 4 [VMP]; b) drink human-like blood 20 times [SB `feed`] | 3 / 5,000 | `self_regeneration`; magicule 0.05 |
| L-ORI-2 | Bloodmaker | a) make Original's Blood (Rite of Original's Blood, target) [ENC]; b) Vampirism level 8 [VMP] | 3 / 15,000 | `charm`; magicule 0.10 |
| L-ORI-3 | Unbroken | a) come back from Vampirism's dying state once (R32) [SB `dying_survived`]; b) compel 5 creatures [SB `compel`] | 4 / 30,000 | `blood_mist`; magicule 0.20 (whole layer) |
| L-ORI-4 | Progenitor | a) sire 3 (players or mobs) [SB `sired`]; b) Vampirism max level [VMP]; c) name a sired through Tensura's naming (R101) [TEN `sire_named`] | 5 / 60,000 | — |
| L-ORI-5 | Primordial (apex) | a) Tensura race saint or higher [TEN]; b) 60 days as an Original [SB `days_as`] | 5 / 100,000 | apex offer `primordial_vampire` |

### Original hybrid: L-OHY *(draft, owner to patch)*

Werewolves faction (R50), so [WW] steps. Layer `ORIGINAL_HYBRID`: `blood_mist, drain, charm, self_regeneration, beast_transformation, godwolf_sense`, magicule 0.25, undead. Apex `primordial_hybrid`. Layer rung: 3.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-OHY-0 | Bound | (R35 entry; bound until the Rite of the Moon) | 0 / 0 | `drain`, `self_regeneration`; undead treatment |
| L-OHY-1 | Unbound | a) complete the Rite of the Moon (target), done at once if not bound [ENC / SB `state`]; b) Werewolves level 4 [WW] | 3 / 10,000 | `beast_transformation`; magicule 0.08 |
| L-OHY-2 | Twin Nature | a) Werewolves level 8 (full forms, R96) [WW]; b) poison 3 vampires with venom (R42v) [SB `venom_hit`] | 4 / 20,000 | `charm`; magicule 0.15 |
| L-OHY-3 | Blood and Moon | a) make Hybrid's Blood (target) [ENC]; b) make Original's Blood (target) [ENC] | 4 / 30,000 | `blood_mist`, `godwolf_sense`; magicule 0.25 (whole layer) |
| L-OHY-4 | Two Lines | a) Werewolves max level [WW]; b) sire 3 [SB `sired`] | 5 / 60,000 | — |
| L-OHY-5 | Primordial hybrid (apex) | a) Tensura race saint or higher [TEN]; b) 60 days as an Original hybrid [SB] | 5 / 100,000 | apex offer `primordial_hybrid` |

### Hybrid: L-HYB *(draft, owner to patch)*

Werewolves faction plus a vampire layer (R2, owner rule). Layer `HYBRID`: `drain, beast_transformation, self_regeneration`, magicule 0.10, undead. Apex `moonblood_hybrid`. Layer rung: 3.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-HYB-0 | Awakened | (R24 bound, or R40 transition) | 0 / 0 | `drain`; undead treatment |
| L-HYB-1 | Unbound | a) Rite of the Moon (target), done at once if not bound [ENC / SB]; b) Werewolves level 3 [WW] | 2 / 5,000 | `beast_transformation` |
| L-HYB-2 | Thirst Mastered | a) keep thirst at 15+ for 3 in-game days in total (R42) [SB `thirst_days`]; b) Werewolves level 6 [WW] | 3 / 10,000 | `self_regeneration`; magicule 0.05 |
| L-HYB-3 | Venom | a) poison 3 vampires [SB `venom_hit`]; b) make Hybrid's Blood (target) [ENC]; c) Werewolves level 8 [WW] | 4 / 20,000 | magicule 0.10 (whole layer) |
| L-HYB-4 | Packbound | a) 7 days as a Beta or Alpha [SB `state` on `betaSince`/rank]; b) Werewolves level 12 [WW] | 4 / 40,000 | — |
| L-HYB-5 | Moonblood (apex) | a) Tensura race saint or higher [TEN]; b) Werewolves max level [WW] | 5 / 100,000 | apex offer `moonblood_hybrid` |

### Tribrid: L-TRI *(draft, owner to patch)*

Werewolves faction, hybrid traits plus witch bonuses (R70). Layer `TRIBRID`: `drain, beast_transformation, self_regeneration, charm`, magicule 0.30, undead. Apex `tribrid_ascendant`. Layer rung: 3.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-TRI-0 | Threefold | (R64) | 0 / 0 | `drain`; undead treatment |
| L-TRI-1 | Witch's Return | a) perform 3 rites [ENC `rite(any, performer)`]; b) Werewolves level 4 [WW] | 3 / 10,000 | `beast_transformation`; magicule 0.10 |
| L-TRI-2 | Venom and Craft | a) poison 3 vampires [SB `venom_hit`]; b) make Hybrid's Blood (target) [ENC] | 3 / 20,000 | `self_regeneration`; magicule 0.20 |
| L-TRI-3 | Emissary of Three | a) **or** group: be a pack's Emissary [SB `state`] **or** lead a pack (Alpha / True Alpha) [SB `state`]; b) Werewolves level 10 [WW] | 4 / 40,000 | `charm`; magicule 0.30 (whole layer) |
| L-TRI-4 | Ascending | a) Werewolves max level [WW]; b) **or** group: perform the Rite of Lineage (Awaken) **or** the Rite of Severance (performer) [ENC] | 5 / 60,000 | — |
| L-TRI-5 | Tribrid Ascendant (apex) | a) Tensura race saint or higher [TEN]; b) 60 days as a tribrid [SB] | 5 / 100,000 | apex offer `tribrid_ascendant` |

### Witch: L-WIT *(draft, owner to patch)*

No faction, or a witch-werewolf. Layer `WITCH`: no skills, magicule 0.30, aura 0.10. Apex `ancestral_witch`. Layer rung: 3. R61 bonuses are rule-level from rung 0.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-WIT-0 | Called | (Rite of Ancestral Calling) | 0 / 0 | magicule 0.05 |
| L-WIT-1 | Apprentice | a) perform 3 rites [ENC]; b) perform the Rite of Lineage (Reveal) on someone [ENC] | 2 / 5,000 | magicule 0.10 |
| L-WIT-2 | Ward-maker | a) perform any Ward rite (Daylight, the Moon, Return) [ENC]; b) place 16 mountain ash line blocks [SB `mountain_ash_placed`] | 3 / 10,000 | magicule 0.20; aura 0.05 |
| L-WIT-3 | Emissary | a) be named a pack's Emissary (R94) [SB `state`, Werewolves]; b) perform 10 rites in total [ENC] | 4 / 20,000 | magicule 0.30; aura 0.10 (whole layer) |
| L-WIT-4 | Coven-mother | a) **or** group: perform the Rite of Severance **or** the Rite of the Cure [ENC]; b) perform 25 rites in total [ENC] | 5 / 40,000 | — |
| L-WIT-5 | Ancestral (apex) | a) Tensura race saint or higher [TEN]; b) perform the Rite of Ancestral Calling on someone else [ENC] | 5 / 100,000 | apex offer `ancestral_witch` |

### Siphoner: L-SIP *(draft, owner to patch)*

Layer `SIPHONER`: `drain`, magicule 0.10. Apex `void_siphon`. Layer rung: 2. Siphon totals are lifetime counters of ours (R65 still resets `siphonedPower` on death).

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-SIP-0 | Called | (Rite of Ancestral Calling, 25%) | 0 / 0 | `drain` |
| L-SIP-1 | Drawing | a) siphon 2,000 power in total [SB `siphon_power`]; b) perform 2 rites [ENC] | 2 / 5,000 | magicule 0.05 |
| L-SIP-2 | Leech | a) siphon from 5 mobs or players [SB `siphon_entity`]; b) siphon 10,000 in total [SB] | 3 / 10,000 | magicule 0.10 (whole layer) |
| L-SIP-3 | Full Vessel | a) hold the full 1,000 siphoned power at once (R62) [SB `state`]; b) perform 10 rites [ENC] | 4 / 20,000 | — |
| L-SIP-4 | Hollow | a) siphon 50,000 in total [SB]; b) **or** group: perform the Rite of Severance **or** the Rite of Purification [ENC] | 5 / 40,000 | — |
| L-SIP-5 | Void Siphon (apex) | a) Tensura race saint or higher [TEN]; b) 60 days as a siphoner [SB] | 5 / 100,000 | apex offer `void_siphon` |

### Heretic: L-HER *(draft, owner to patch)*

Vampire faction (R63), so [VMP] steps. Layer `HERETIC`: `drain, charm, self_regeneration`, magicule 0.20, undead. Apex `void_siphon` (shared with siphoners). Layer rung: 3.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-HER-0 | Heretic | (a siphoner joined the vampires) | 0 / 0 | `drain`; undead treatment |
| L-HER-1 | Blood Witch | a) Vampirism level 4 [VMP]; b) siphon 2,000 in total [SB] | 3 / 5,000 | `self_regeneration`; magicule 0.05 |
| L-HER-2 | Thief of Craft | a) siphon from 3 vampire-type mobs or players [SB `siphon_entity`]; b) drink human-like blood 15 times [SB `feed`] | 3 / 15,000 | `charm`; magicule 0.10 |
| L-HER-3 | Coven of Heretics | a) Vampirism level 10 [VMP]; b) compel 3 villagers [SB `compel`]; c) siphon 20,000 in total [SB] | 4 / 30,000 | magicule 0.20 (whole layer) |
| L-HER-4 | Dark Apostle | a) Vampirism max level [VMP]; b) perform 10 rites [ENC] | 5 / 60,000 | — |
| L-HER-5 | Void Siphon (apex) | a) Tensura race saint or higher [TEN]; b) 60 days as a heretic [SB] | 5 / 100,000 | apex offer `void_siphon` |

### Hunter: L-HUN *(draft, owner to patch)*

Vampirism hunters, the Five (POTENTIAL, HUNTER_OF_THE_FIVE) included. Layer `HUNTER_OF_THE_FIVE`: aura 0.10, only while marked. Apex `eternal_hunter`. Layer rung: 4. R81–R84 are rule-level.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-HUN-0 | Hunter | (join the hunter faction) | 0 / 0 | — |
| L-HUN-1 | Tracker | a) hunter level 3 [VMP]; b) kill 10 prey (R81) [SB `kill`] | 2 / 3,000 | — |
| L-HUN-2 | Slayer | a) hunter level 6 [VMP]; b) kill 25 prey [SB] | 3 / 8,000 | — |
| L-HUN-3 | Marked | a) become one of the Five (Rite of the Five as target, or a potential's first prey kill, R82) [ENC / SB `state`] | 4 / 20,000 | aura 0.05 (while marked) |
| L-HUN-4 | Five-tested | a) 20 mark kills (the R81 cap) [SB `state` `markKills`]; b) hunter level 10 [VMP] | 4 / 40,000 | aura 0.10 (whole layer, while marked) |
| L-HUN-5 | Eternal Hunter (apex) | a) Tensura race saint or higher [TEN]; b) hunter max level [VMP] | 5 / 100,000 | apex offer `eternal_hunter` (while marked) |

### Werewolf: L-WOL *(draft, owner to patch)*

Werewolves faction, no species. No `LayerTable` entry; rank skills stay `RankSkills` (Alpha, True Alpha, born level 5) and keep following the rank. The Beastfolk mapping (R20b) is shown on the Path page as it happens; the ladder never sets a Tensura race. **No plan 05 apex**: the top rung is True Alpha.

| Id | Rung | Tasks | SP / EP | Grants |
|---|---|---|---|---|
| L-WOL-0 | Omega | (bitten, born or wild) | 0 / 0 | — |
| L-WOL-1 | First Moon | a) Werewolves level 3 [WW]; b) live through a forced full-moon shift [SB `forced_shift_survived`] | 2 / 3,000 | — |
| L-WOL-2 | Beta | a) join a pack (rank BETA, R91) [SB `state`]; b) Werewolves level 5 [WW] | 3 / 8,000 | — |
| L-WOL-3 | Controlled | a) have moon control (bound Moon Ring, bound anchor, or Alpha rank; `MoonControl`) [SB `state`]; b) Werewolves level 8 [WW] | 3 / 15,000 | — |
| L-WOL-4 | Leader | a) **or** group: rank ALPHA or TRUE_ALPHA [SB `state`] **or** 15 days as a Beta [SB `state` `betaSince`]; b) 100 defender kills [SB `state` `defenderKills`] | 4 / 40,000 | — |
| L-WOL-5 | True Alpha (top) | a) rank TRUE_ALPHA (R91: max level, 30 days as a Beta, 300 defender kills, no innocent kill, no stolen spark; the hub shows each condition) [SB `state`] | 6 / 100,000 | title; True Alpha keeps the spark (R91): the rung stays and goes dormant with the rank when cured or leaving |

L-WOL-4's "or 15 days as a Beta" keeps the earned True Alpha path open: R91 forbids a stolen spark, so a Beta must be able to climb without becoming an Alpha first.

### Rung counts

| Race | Rungs | SP total | EP total |
|---|---|---|---|
| Human | 3 | 5 | 4,000 |
| Ghoul | 1 | 0 | 0 |
| Vampire | 6 | 18 | 112,000 |
| Original | 6 | 20 | 210,000 |
| Original hybrid | 6 | 21 | 220,000 |
| Hybrid | 6 | 18 | 175,000 |
| Tribrid | 6 | 20 | 230,000 |
| Witch | 6 | 19 | 175,000 |
| Siphoner | 6 | 19 | 175,000 |
| Heretic | 6 | 20 | 210,000 |
| Hunter | 6 | 18 | 171,000 |
| Werewolf | 6 | 18 | 166,000 |

The largest ladder pays about 11% of one apex evolution's 2,000,000 EP, so rewards help but never replace Tensura's own progression *(draft, owner to patch)*.

---

## Skills

### The model

- **One catalogue, many trees.** A skill has one id and one cost. A race's tree lists catalogue ids with a "needs rung" for that tree and an optional prerequisite skill. A skill bought once is owned in every tree that lists it; it is **active** only while the current race's tree lists it, dormant otherwise.
- **Effect kinds** (fixed in code, tuned in JSON):
  - **ATTR**: an attribute modifier with our id, removed when dormant.
  - **RULE**: a number one of our own rules reads through `LadderQuery.owns(player, id)` (compulsion range, siphon cap, rite cost, ...).
  - **TENSURA**: a Tensura skill granted through `SkillOwners` source `supernatural_skill`, taken back only if we granted it. Hidden without Tensura.
  - **BLOOD**: a change to Vampirism blood on feeding.
- A tree's SP total roughly matches its ladder's SP total, so a player who climbs the whole ladder can buy most of the tree but carries little spare *(draft, owner to patch)*.

### Catalogue *(draft, owner to patch)*

| Id | Name | Cost | Kind | Effect |
|---|---|---|---|---|
| S-HUM-1 | Survivor | 1 | ATTR | +2 max health while your race is Human |
| S-HUM-2 | Vervain Blood | 2 | RULE | vervain tea lasts 20 minutes instead of 10 (artifacts) |
| S-HUM-3 | Swift Feet | 2 | ATTR | +5% movement speed while Human or Hunter |
| S-VAM-1 | Keen Fangs | 1 | BLOOD | +1 Vampirism blood per human-like feed |
| S-VAM-2 | Night Stride | 2 | ATTR | +5% movement speed at night |
| S-VAM-3 | Silver Tongue | 2 | RULE | compulsion range 4 → 6 blocks; villager discount 20% → 30% |
| S-VAM-4 | Hardened | 3 | ATTR | +2 max health |
| S-VAM-5 | Blood Mist | 3 | TENSURA | `tensura:blood_mist` (for a tree whose layer doesn't already grant it) |
| S-VAM-6 | **Predator's Thirst** (the vampire-feeding skill) | 4 + Q-VAM-6 | BLOOD | see below |
| S-VAM-7 | Elder's Skin | 4 | RULE | Vampirism sun damage −25% (stacks with nothing: a bound Sun Ring already blocks it, R21) |
| S-ORI-1 | Old Blood | 2 | ATTR | +10% attack (a separate modifier from R31's) |
| S-ORI-2 | Bloodmaker's Yield | 1 | RULE | the Rite of Original's Blood fills 2 bottles |
| S-ORI-3 | Sire's Call | 3 | RULE | `/bloodline call` cooldown 10 → 5 minutes |
| S-ORI-4 | Primordial Mind | 5 | RULE | +0.05 magicule bonus on top of the layer (Tensura) |
| S-HYB-1 | Steady Thirst | 2 | RULE | blood thirst falls half as fast (R42) |
| S-HYB-2 | Venom Fang | 3 | RULE | a venom hit (R42v) also gives Slowness I for 5 s |
| S-WOL-1 | Thick Hide | 1 | ATTR | +1 armor |
| S-WOL-2 | Moon Calm | 2 | RULE | forced full-moon shift chance −25% (no effect with moon control) |
| S-WOL-3 | Godwolf Sense | 2 | TENSURA | `tensura:godwolf_sense` (shared with rank skills through `SkillOwners`) |
| S-WOL-4 | Wolfsbane Tolerance | 2 | RULE | common wolfsbane effects last half as long (R95; other strains unchanged) |
| S-WOL-5 | Pack Heart | 3 | RULE | as an Alpha, your pack bonus needs 2 Betas instead of 3 (R94) |
| S-WOL-6 | Roar's Echo | 3 | RULE | Roar cooldown 60 → 40 s |
| S-WOL-7 | Moon Heart | 4 | ATTR | +10% attack on full-moon nights |
| S-WIT-1 | Steady Circle | 2 | RULE | your rites cost ×0.95 on top of R61 |
| S-WIT-2 | Wide Circle | 2 | RULE | altar range ×1.5 → ×1.75 (R61) |
| S-WIT-3 | Ashwarden | 2 | RULE | mountain ash lines you placed can't be broken by vampire or werewolf types |
| S-WIT-4 | Emissary's Bond | 3 | RULE | as an Emissary, the pack rite discount is 20% instead of 10% (R94) |
| S-WIT-5 | Magic Sense | 3 | TENSURA | `tensura:magic_sense` |
| S-WIT-6 | Ancestral Voice | 4 | RULE | the Rites of Lineage cost half power when you perform them |
| S-WIT-7 | Coven's Ward | 4 | ATTR + RULE | +2 max health; +0.05 aura bonus (Tensura) |
| S-SIP-1 | Deep Draw | 2 | RULE | siphon cap 1,000 → 1,500 (R62) |
| S-SIP-2 | Quick Draw | 2 | RULE | siphon rate ×1.5 |
| S-SIP-3 | Life Siphon | 3 | RULE | heal 1 health per 100 power siphoned from a mob or player |
| S-SIP-4 | Spent Wisely | 3 | RULE | rite costs paid from siphoned power ×0.9 |
| S-SIP-5 | Void Hunger | 4 | RULE | siphoning a vampire- or werewolf-type also gives it Weakness I for 5 s |
| S-HUN-1 | Steady Aim | 2 | RULE | +5% damage against prey (R81) |
| S-HUN-2 | Hunter's Eye | 2 | RULE | the Five's prey sense radius 24 → 32 blocks (while marked) |
| S-HUN-3 | Patient Hunter | 3 | RULE | the R84 instinct starts after 5 days instead of 3 |
| S-HUN-4 | Danger Sense | 3 | TENSURA | `tensura:danger_sense` |
| S-HUN-5 | Mark Mastery | 4 | RULE | mark caps +20% / +10% → +25% / +12.5% (R81, while marked) |

40 catalogue skills.

### S-VAM-6 Predator's Thirst and its quest

The owner's vampire-feeding skill (no show names in game, R5).

- **Without it** (plan 11, behaviour 4): biting a vampire, player or mob, gives no blood and costs a little of your own.
- **With it:** a bite on a vampire **feeds you and drains the victim**:
  - you gain Vampirism blood: the victim's `vampirism:entity_blood` value if above 0, else `vampire_feed_blood` (config, default 3) *(draft, owner to patch)*;
  - a vampire player loses the same amount of Vampirism blood; a vampire mob takes 2 damage and Weakness I for 10 s *(draft, owner to patch)*;
  - it goes through plan 13's Bite key row 2 (Vampirism blood drinking) and plan 11's refusal hook, which asks `LadderQuery.owns(p, "S-VAM-6")` before refusing;
  - venom is unchanged: a hybrid-type biter still poisons a vampire victim (R42v);
  - an Original victim is drained too; it never dies of the drain (only white oak kills, R32).
- **Q-VAM-6 "Hunter of Hunters"** opens at L-VAM-4 (or the matching rung in the other trees below). Tasks *(draft, owner to patch)*:
  - a) kill 10 vampires, Vampirism mobs or players [SB `kill`];
  - b) bite a vampire and be refused 3 times (plan 11's penalty) [SB `vampire_bite_refused`];
  - c) drink human-like blood 25 times [SB `feed`].
  Finishing the quest pays nothing; it lets you buy S-VAM-6 for 4 SP.
- **Which trees list it:** the owner placed it in the vampire tree. This design also lists it, with the same quest, in every vampire-type tree (Original, Original hybrid, Hybrid, Tribrid, Heretic), since R2 makes them all vampires; one purchase covers all *(draft, owner to patch)*.

### Trees per race *(draft, owner to patch)*

"Needs" is the rung of that race's own ladder.

**Human (3 skills, 5 SP):** S-HUM-1 (needs L-HUM-1), S-HUM-2 (L-HUM-2), S-HUM-3 (L-HUM-2).

**Ghoul:** no tree (frozen).

**Vampire (7 skills, 19 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-VAM-1 Keen Fangs | 1 | L-VAM-1 | — |
| S-VAM-2 Night Stride | 2 | L-VAM-2 | — |
| S-VAM-3 Silver Tongue | 2 | L-VAM-3 | — |
| S-VAM-4 Hardened | 3 | L-VAM-3 | S-VAM-2 |
| S-VAM-5 Blood Mist | 3 | L-VAM-4 | — |
| S-VAM-6 Predator's Thirst | 4 | L-VAM-4 + Q-VAM-6 | S-VAM-1 |
| S-VAM-7 Elder's Skin | 4 | L-VAM-5 | S-VAM-4 |

**Original (8 skills, 21 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-VAM-1 Keen Fangs | 1 | L-ORI-1 | — |
| S-ORI-1 Old Blood | 2 | L-ORI-1 | — |
| S-ORI-2 Bloodmaker's Yield | 1 | L-ORI-2 | — |
| S-VAM-3 Silver Tongue | 2 | L-ORI-2 | — |
| S-VAM-4 Hardened | 3 | L-ORI-3 | S-ORI-1 |
| S-ORI-3 Sire's Call | 3 | L-ORI-4 | S-VAM-3 |
| S-VAM-6 Predator's Thirst | 4 | L-ORI-3 + Q-VAM-6 | S-VAM-1 |
| S-ORI-4 Primordial Mind | 5 | L-ORI-5 | S-VAM-4 |

**Original hybrid (8 skills, 21 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-VAM-1 Keen Fangs | 1 | L-OHY-1 | — |
| S-ORI-1 Old Blood | 2 | L-OHY-1 | — |
| S-HYB-1 Steady Thirst | 2 | L-OHY-2 | — |
| S-HYB-2 Venom Fang | 3 | L-OHY-2 | — |
| S-VAM-3 Silver Tongue | 2 | L-OHY-3 | — |
| S-ORI-3 Sire's Call | 3 | L-OHY-4 | S-VAM-3 |
| S-VAM-6 Predator's Thirst | 4 | L-OHY-3 + Q-VAM-6 | S-VAM-1 |
| S-WOL-7 Moon Heart | 4 | L-OHY-5 | S-HYB-2 |

**Hybrid (8 skills, 19 SP).** The hybrid's vampire side lives here (plan 13: a hybrid's HUD actions are Werewolves actions; its vampire abilities come from this tree).

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-VAM-1 Keen Fangs | 1 | L-HYB-1 | — |
| S-WOL-1 Thick Hide | 1 | L-HYB-1 | — |
| S-HYB-1 Steady Thirst | 2 | L-HYB-2 | — |
| S-VAM-2 Night Stride | 2 | L-HYB-2 | — |
| S-HYB-2 Venom Fang | 3 | L-HYB-3 | — |
| S-VAM-3 Silver Tongue | 2 | L-HYB-3 | — |
| S-VAM-6 Predator's Thirst | 4 | L-HYB-4 + Q-VAM-6 | S-VAM-1 |
| S-WOL-7 Moon Heart | 4 | L-HYB-5 | S-HYB-2 |

**Tribrid (7 skills, 20 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-WIT-1 Steady Circle | 2 | L-TRI-1 | — |
| S-HYB-1 Steady Thirst | 2 | L-TRI-1 | — |
| S-WIT-2 Wide Circle | 2 | L-TRI-2 | S-WIT-1 |
| S-HYB-2 Venom Fang | 3 | L-TRI-2 | — |
| S-WIT-4 Emissary's Bond | 3 | L-TRI-3 | — |
| S-VAM-6 Predator's Thirst | 4 | L-TRI-3 + Q-VAM-6 | — |
| S-WOL-7 Moon Heart | 4 | L-TRI-5 | S-HYB-2 |

**Witch (7 skills, 20 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-WIT-1 Steady Circle | 2 | L-WIT-1 | — |
| S-WIT-2 Wide Circle | 2 | L-WIT-1 | — |
| S-WIT-3 Ashwarden | 2 | L-WIT-2 | — |
| S-WIT-4 Emissary's Bond | 3 | L-WIT-3 | — |
| S-WIT-5 Magic Sense | 3 | L-WIT-3 | — |
| S-WIT-6 Ancestral Voice | 4 | L-WIT-4 | S-WIT-1 |
| S-WIT-7 Coven's Ward | 4 | L-WIT-5 | S-WIT-3 |

**Siphoner (6 skills, 17 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-SIP-1 Deep Draw | 2 | L-SIP-1 | — |
| S-SIP-2 Quick Draw | 2 | L-SIP-1 | — |
| S-SIP-3 Life Siphon | 3 | L-SIP-2 | S-SIP-2 |
| S-WIT-5 Magic Sense | 3 | L-SIP-3 | — |
| S-SIP-4 Spent Wisely | 3 | L-SIP-3 | S-SIP-1 |
| S-SIP-5 Void Hunger | 4 | L-SIP-4 | S-SIP-3 |

**Heretic (7 skills, 19 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-VAM-1 Keen Fangs | 1 | L-HER-1 | — |
| S-SIP-1 Deep Draw | 2 | L-HER-1 | — |
| S-SIP-3 Life Siphon | 3 | L-HER-2 | — |
| S-VAM-3 Silver Tongue | 2 | L-HER-3 | — |
| S-VAM-4 Hardened | 3 | L-HER-3 | — |
| S-VAM-6 Predator's Thirst | 4 | L-HER-3 + Q-VAM-6 | S-VAM-1 |
| S-SIP-5 Void Hunger | 4 | L-HER-4 | S-SIP-3 |

**Hunter (7 skills, 18 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-HUM-2 Vervain Blood | 2 | L-HUN-1 | — |
| S-HUN-1 Steady Aim | 2 | L-HUN-1 | — |
| S-HUM-3 Swift Feet | 2 | L-HUN-2 | — |
| S-HUN-2 Hunter's Eye | 2 | L-HUN-3 | — |
| S-HUN-3 Patient Hunter | 3 | L-HUN-3 | — |
| S-HUN-4 Danger Sense | 3 | L-HUN-4 | S-HUN-2 |
| S-HUN-5 Mark Mastery | 4 | L-HUN-5 | S-HUN-1 |

**Werewolf (7 skills, 17 SP):**

| Skill | Cost | Needs | After |
|---|---|---|---|
| S-WOL-1 Thick Hide | 1 | L-WOL-1 | — |
| S-WOL-2 Moon Calm | 2 | L-WOL-1 | — |
| S-WOL-3 Godwolf Sense | 2 | L-WOL-2 | — |
| S-WOL-4 Wolfsbane Tolerance | 2 | L-WOL-3 | — |
| S-WOL-5 Pack Heart | 3 | L-WOL-4 | — |
| S-WOL-6 Roar's Echo | 3 | L-WOL-4 | S-WOL-5 |
| S-WOL-7 Moon Heart | 4 | L-WOL-5 | S-WOL-1 |

### Vampirism and Werewolves skills in the hub

- The Skills page has a tab per faction tree the player has (Vampirism vampire or hunter, or Werewolves). It draws that mod's tree from its own client data and spends **that mod's** skill points.
- Unlocking sends `UnlockModSkill`; the server calls the faction player's own skill handler (`ISkillHandler`: can-enable check, then enable), so parents, exclusions, points and refunds are exactly the mod's own.
- A wax seal (Grimoire) or rail button (others) still opens the mod's own skill screen for anything we don't draw (resets, refund items).

---

## The hub

### One screen model

`HubScreen` holds the state (current page, selected rung/skill/task) and a `HubTheme` that supplies layout and drawing. Pages are components that ask the theme for regions. Switching theme in the client config rebuilds the screen without losing the page. Client config `hub_theme`: `GRIMOIRE` (default), `BLOOD_MOON`, `TENSURA` (falls back to Grimoire without Tensura).

### Pages

| Page | Shows | Does | Hidden when |
|---|---|---|---|
| **Path** | your story: race, Tensura race (and R20/R20a/R20b mappings as they happened), how you got here; the ladder with reached rungs and what each granted; SP / EP totals; the apex offer state | — | never |
| **Skills** | our tree (owned, buyable, locked with reason, dormant); Vampirism/Werewolves trees; **Loadout** tab (plan 13's 6 slots) | buy our skills; unlock their skills; assign HUD slots | Loadout without plan 13; faction trees without a faction |
| **Quests** | the active rung's tasks with progress, source tag and the mod's hint; skill quests (Q-VAM-6) | claim | — |
| **Rites** | rite tasks; the rites you can perform or be the target of, with power and items (from our rite JSON); witch bonuses in effect | launcher: Enchanted's book | without Enchanted |
| **Bloodline** | sire, sired, the family tree (`/bloodline tree` data), bond / severance, the gene once revealed (R10), Tensura EP and evolution progress (live) | launcher: Tensura's screen; `/bloodline call` button where allowed (R43) | — |
| **Pack** | rank and eyes, pack members and who is near, anchor, emissary, spark state, True Alpha conditions, eclipse warning | `/pack` actions the commands already allow (invite, accept, leave) | without Werewolves, or for non-wolf races with no pack |

A Ghoul sees Path (with L-GHO-0's steps) and Bloodline; every other page shows "Complete your turning".

### Themes

| | Grimoire (default) | Blood Moon | Tensura expansion |
|---|---|---|---|
| Frame | an open book | dark full screen | Tensura's own frame style, wider; Tensura's textures by resource location, never copied |
| Navigation | ribbon bookmarks, one per page | left rail of pages | Tensura-style side tabs |
| Path | left page: story and ladder (rungs as chapters, reached ones stamped); right page: next rung's tasks | the moon fills with progress to the next rung; rungs as phases around it | ladder as Tensura's evolution-style list |
| Quests | right page, claim buttons as wax stamps | below the moon | list panel |
| Skills | two pages: tree left, details right | tree centre, details on the right rail | Tensura skill-list style |
| Launchers | wax seals at the book's edge (Tensura, Enchanted, Vampirism, Werewolves) | right rail buttons, with SP / EP counters | buttons in Tensura's style |

Every theme: keyboard navigation (Tab, arrows, Enter), tooltips that read the same text as the visuals, and readable at GUI scale 4.

### Launchers and returning

Opening a launcher remembers the hub page. When the launched screen closes (Esc) and nothing else opened, the hub reopens on that page. Launched screens are never changed.

## Entry points

1. **Tensura menu tab.** A 5th right-side tab under Tensura's three "coming soon" tabs, added with `ScreenEvent.Init.Post` on Tensura's menu screen (class matched by name, so no hard link). If the screen class or the tab positions aren't found, no tab is added and a warning is logged once. The reserved mixin in `supernatural_tensura` is only for the case where the event can't place it.
2. **Our key** `key.supernaturalbloodlines.hub`, in our key category. The default comes from plan 13's keymap (suggested `J`) *(draft, owner to patch)*.

## Migration

Runs once per player on login when `schema == 0`, server side, inside the usual re-entrancy guard. It only adds.

1. **Race** is computed as usual. `race_since[race]` = `BloodlineState.turnedAt` when set, else now.
2. **Proven tasks.** For each rung of that race in order, a task is marked done (and claimed) if current state proves it:
   - level steps from the current faction level;
   - state tasks (species, unbound, rank, `markKills`, `defenderKills`, emissary, moon control, sired count, Tensura tier);
   - entry rites proven by the species (an Original did the Rite of Immortality; an unbound hybrid did the Rite of the Moon; one of the Five had the Rite of the Five or a potential's kill).
   - Counting tasks with no history (feeds, rites performed, kills beyond the counters we have) are **not** proven.
3. **Rung** = the highest of:
   - the last rung whose tasks are all proven, in order;
   - the **layer rung** for any player with a species, so every layer skill and bonus they hold today stays (no `SpeciesLayer` take-back);
   - the **rank floor**: Alpha → L-WOL-4, True Alpha → L-WOL-5 (dormant if not a werewolf now), and the same rungs' pack tasks for hybrid-types;
   - the **apex floor**: a player whose Tensura race is already one of our apex races gets that ladder's apex rung.
4. **Rewards.** SP for every back-filled rung is added to `sp` (so they can buy skills at once). EP is **not** paid for back-filled rungs, since they already earned it playing *(draft, owner to patch)*.
5. **Skills** start empty; nothing they own today is a skill of ours.
6. Ghoul flags are untouched (plan 11: existing vampires are not Ghouls).
7. `backfill` records what was given; `schema = 1`.

A datapack change that moves a task to a different rung never lowers anyone's rung: `rungs` is a high-water mark.

## Testing

- **Unit (no game classes):** `SupernaturalRace.of` for every row; the ladder JSON parser (bad ids rejected with a log line, like `LayerTable`); claim, rung-up and any-of rules; Ghoul freeze; migration floors; layer slicing (each race's slices add up to its `LayerTable` entry).
- **GameTests:** a level step completes on `FactionLevelChanged`; a rite signal completes a rite task; claim pays SP and EP; buying a skill applies and dormancy removes its modifier; S-VAM-6 feeding vs the plan 11 refusal; migration of an Original, an Alpha and a True Alpha keeps every layer skill; missing-mods runs per part.
- **Manual:** each theme at GUI scale 2 and 4; Tensura tab; launcher return with Esc.

## Left to the builder

- The exact Vampirism skill-handler calls for `UnlockModSkill` in 1.21-1.10.13, and whether Werewolves' tree uses the same handler.
- Each mod's max level and where its level-up requirements can be read for the hint text.
- Tensura's menu screen class and tab positions; whether `Init.Post` is enough.
- How to open Enchanted's book and Tensura's screens from code; fall back to a "use the item" hint where there's no entry point.
- The return-to-hub hook (`ScreenEvent.Closing` plus a next-tick open, or similar).
- Where each `TaskSignals` fire goes in the existing parts (compulsion, siphon, venom, dying state, forced shift, sun ring binding, mountain ash, naming, plan 11 bite refusal).
- Whether Enchanted's `Rite` has a clean completion point for the reserved mixin, or whether only our rites count.
- Art: placeholders first (book, moon, ribbons, seals); final art can use the fal skills.
- Debug commands under `/supernaturalbloodlines debug supernatural` (set rung, give SP, reset, show backfill).
- The ladder/skill JSON schema, keeping ids exactly as in this file.

## Could not ground

- **No plan 05 apex for Vampire, Werewolf, Human or Ghoul.** Their top rungs (Elder, True Alpha) are titles; Tensura's own vampire and Beastfolk lines stay Tensura's.
- **Witch-werewolves** are placed in the Witch race (row 8); no rule says which ladder they climb.
- **Detectors not yet in code:** `forced_shift_survived`, `sun_ring_bound`, `mountain_ash_placed`, `vampire_bite_refused` (plan 11), `dying_survived`, lifetime siphon totals. Each is one `TaskSignals` call in code that already exists or is planned.
- **Every number** (SP, EP, counts, days, skill effects) is the designer's.
