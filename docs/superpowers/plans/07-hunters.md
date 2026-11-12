# Hunters (the Five) Implementation Plan — part `species`

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vampirism hunters can become one of the Five through a witch's rite, grow a hunter's mark with each **prey** kill (vampires and werewolf types; witches aren't prey, R81), pass the mark to a potential when killed by prey, and curse the prey that killed them.

**Architecture:** Lives in `parts.species.hunter`. It uses foundation's rules for every change: `RITE_OF_THE_FIVE`, `KILLED_PREY`, `KILLED_BY_PREY`, `CHOSEN_AS_POTENTIAL`, `KILLED_MARKED_HUNTER`, `HUNTER_CURSE_LIFTED`. A world `FiveRoster` (`SavedData`) tracks the members. The perks are attribute modifiers, a client-only glow, and the curse's client hallucinations through one small S2C payload.

**Tech Stack:** As foundation, plus the Vampirism API and Enchanted (the rite through plan 3's `FactionRiteFactory`).

**Spec:** design section "7. Hunters".

## Global Constraints

- Everything in foundation's Global Constraints.
- `max_five` is a config value, default 5. Only human-like players (plan 4's `SpeciesEligibility`) in the Vampirism hunter faction qualify.
- No show character names. The rite is *Rite of the Five*, the mark is *Hunter's Mark*, the curse is *Hunter's Curse*.
- Depends on plan 4, Task 1 (`SpeciesContext`, `OutcomeExecutor`) and the core `Supernatural` registry (defined in plan 6, Task 3; if plan 6 isn't merged, create `core/Supernatural.java` first in a shared commit).

## Review Focus

- **A member of the Five leaving the hunter faction or quitting the server** must free their roster slot when they leave the faction, but not when they log out. Pinned by `leavingHunterFactionFreesSlot` and `logoutKeepsSlot`.
- **No eligible hunters online when a mark is lost:** the potential choice must wait and retry when an eligible hunter joins, not be lost. Pinned by `potentialChosenLaterWhenNoneOnline`.
- **Two curses on the same killer** (two marked hunters killed) must not stack into constant hallucinations. One flag, the same intensity. Pinned by `curseDoesNotStack`.
- **A human killing a marked hunter** must not be cursed. Pinned by foundation's `vampireKillingMarkedHunterIsCursed`, plus the GameTest `humanKillerNotCursed`.
- **Glow visible only to the hunter:** other players must not see the glowing vampires. Pinned by a manual two-client check in Task 3.

---

### Task 1: Roster and the Rite of the Five

**Files:**
- Create: `parts/species/hunter/FiveRoster.java` (SavedData: `Set<UUID> members`, `Set<UUID> pendingPotentialChoice`), `parts/species/hunter/HunterEvents.java`, and the rite `data/supernaturalbloodlines/enchanted/circle_magic/rite/rite_of_the_five.json` (`faction_rite`, event RITE_OF_THE_FIVE, faction hunter, power 10000; items: 5 candles, a silver ingot or iron fallback, a blank book, and the target's taglock)
- Modify: `SpeciesContext.of` (fills `fiveCount` from the roster and `maxFive` from config), and `OutcomeExecutor` (on a HUNTER_OF_THE_FIVE result, adds to the roster; on leaving that species, removes from it)

**Interfaces:**
- `static FiveRoster FiveRoster.get(ServerLevel overworld)`, with `int size()`, `boolean add(UUID)`, `void remove(UUID)` and `boolean contains(UUID)`.

- [ ] **Step 1:** Write the GameTests:
  - `riteOfTheFiveMarksHunter`
  - `riteRefusedWhenFiveFull` (max_five 1)
  - `leavingHunterFactionFreesSlot`
  - `logoutKeepsSlot`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(hunters): roster and rite of the five".

### Task 2: Mark, potentials and curse

**Files:**
- Create: `parts/species/hunter/MarkTracker.java`, `parts/species/hunter/PotentialChooser.java`, `parts/species/hunter/HunterCurse.java`, `core/network/HallucinationPayload.java` (S2C: `type` enum PHANTOM, WHISPER, DARKNESS), and the client handler `client/HallucinationClient.java` (it spawns a client-only phantom entity, which is a vanilla zombie rendered with the Vampirism hunter skin if available, else a plain zombie, for 5 s; plays a whisper sound; applies a Darkness pulse)

**Interfaces:**
- **`LivingDeathEvent`** (server):
  - **victim is prey** (`Supernatural.isPrey`: vampire or werewolf types, not witches) and **the killer is a player**: KILLED_PREY on the killer. If that made a POTENTIAL into HUNTER_OF_THE_FIVE, apply HUNTER_CURSE_LIFTED to every online player with `hunterCurse` (and mark offline ones in a `liftedCurses` set on the roster, applied at login).
  - **victim is HUNTER_OF_THE_FIVE** and **the killer is prey**: KILLED_BY_PREY on the victim, KILLED_MARKED_HUNTER on the killer (vampire or werewolf types), then `PotentialChooser.choose(server)`.
- **`PotentialChooser.choose(MinecraftServer)`:** picks a random online player who is human-like, in the Vampirism hunter faction and has species NONE, then applies CHOSEN_AS_POTENTIAL (state POTENTIAL). If nobody qualifies, it sets `pendingPotentialChoice` and retries on `PlayerLoggedInEvent` and on hunter-faction join.
- **`HunterCurse`:** every 600 ± 200 ticks for each online player with `hunterCurse`, send one random `HallucinationPayload`.
- **Mark perks:** on a `markKills` change or login, reapply the modifiers `supernaturalbloodlines:hunter_mark`: attack damage `+0.01 × min(markKills,20)` (multiply base) and speed `+0.005 × min(markKills,20)`.

- [ ] **Step 1:** Write the GameTests:
  - `markGrowsOnVampireKill`
  - `markGrowsOnWerewolfKill`
  - `witchKillDoesNotCount`
  - `markedHunterKilledByVampireLosesMark`
  - `werewolfKillerIsCursed`
  - `potentialChosenFromOnlineHunters`
  - `potentialChosenLaterWhenNoneOnline`
  - `potentialFirstKillLiftsCurse`
  - `curseDoesNotStack`
  - `humanKillerNotCursed`
  - `markPerksScaleAndCap` (25 kills → +20% / +10%)
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(hunters): mark, potentials, curse".

### Task 3: Instinct, vampire sense, compulsion immunity, checks

**Files:**
- Create: `parts/species/hunter/HunterInstinct.java`, `core/network/GlowPayload.java` (S2C, the entity ids to glow for 40 ticks), and the client handler `client/HunterSenseClient.java` (a per-entity client glow via the `ClientTickEvent` outline override, only on the receiving client)

**Interfaces:**
- **Instinct:** every 1200 ticks for HUNTER_OF_THE_FIVE players, if `gameTime - lastVampireKill > 72000` (3 days, counting any prey kill) and no allied prey is involved (skip the check when the player is on a scoreboard team with a vampire), apply Weakness I for 1300 ticks.
- **Sense** (`markKills >= 5`): every 40 ticks, send a `GlowPayload` with the **prey** entities within 24 blocks (no witches).
- **Compulsion immunity:** `static boolean HunterRules.immuneToCompulsion(Player)` returns true for POTENTIAL and HUNTER_OF_THE_FIVE. Plan 8's compulsion checks this together with `VervainRules.protects`.

- [ ] **Step 1:** Write the GameTests:
  - `instinctWeakensIdleHunter`
  - `instinctResetByKill`
  - `hunterImmuneToCompulsion`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Two-client manual check: the glow is visible only to the hunter, and the hallucinations show only for the cursed player. Tick the hunter section of `docs/parts/species.md`. Update `CHANGELOG.md`. Commit "feat(hunters): instinct, sense, compulsion immunity".
