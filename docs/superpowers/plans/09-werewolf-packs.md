# Werewolf Bloodlines and Packs (Teen Wolf) Implementation Plan — part `species`, needs Werewolves

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Werewolves get a lineage (born or bitten) and a rank (Omega, Beta, Alpha, True Alpha), with eye colours, packs, an Alpha Roar, Alpha-only turning, the hidden gene, the Rite of Lineage, anchors, the lunar eclipse and electricity weakness.

**Architecture:** `parts.species.hybrid` already gates on Werewolves; this adds `parts.species.pack`, which is also touched only behind `ModList.isLoaded("werewolves")`. Every state change goes through foundation's pure `PackRules` on `BloodlineState.wolf`. A world `PackIndex` (SavedData) holds the packs. Effects (attributes, roar, eclipse) read the state. A keybind plus a C2S payload triggers the roar.

**Tech Stack:** As foundation, plus the Vampirism API (faction, `ActionEvent`), Werewolves, and Enchanted (Rite of Lineage).

**Spec:** design section "9. Werewolf bloodlines and packs".

## Global Constraints

- Everything in foundation's Global Constraints.
- Config defaults:
  - `latent_gene_chance` 0.15
  - `alpha_bite_death_chance` 0.10
  - `lunar_eclipse_chance` 0.03
  - True Alpha (R91, **no world cap**): max Werewolves level, 30 in-game days as a Beta (720,000 ticks), 300 defender kills, never killed an innocent, never stole a spark
  - `moon_bound_werewolves` true, `full_form_level` 8 (R96)
- Ordinary deaths keep rank and spark. Only an Alpha killed by a werewolf player loses the spark.
- The roar keybind defaults to unbound (category "Supernatural Bloodlines"). The OFA pack's `OFA-notes/apply_keymap.py` gets a binding later (not part of this repo).

## Review Focus

- **An Alpha killed by their own Beta** steals correctly. The rest of the pack goes to the new Alpha; the old Alpha becomes an Omega and leaves the pack. Pinned by `betaKillingOwnAlphaTakesPack`.
- **Pack data with an offline Alpha:** Betas keep the pack, but the bonus needs the Alpha online and near. Pinned by `bonusNeedsAlphaOnlineAndNear`.
- **The gene awakens while the player is a vampire** (the Klaus rule): they become a bound hybrid, switching to the werewolf faction with the vampire layer kept. A hunter's gene stays latent. Pinned by `vampireWithGeneBecomesBoundHybrid` and `geneDoesNotAwakenAsHunter`.
- **Eclipse plus a server restart mid-eclipse:** it must end at dawn and not persist forever. Pinned by `eclipseEndsAtDawnAfterReload`.
- **A Beta bitten again by another Alpha** must not switch packs silently. They stay until they `/pack leave`. Pinned by `secondBiteDoesNotSwitchPack`.

---

### Task 1: Discovery, hidden gene and Rite of Lineage

**Files:**
- Create: `docs/parts/species.md` "Werewolf hook points" section, recording:
  - how the lupus sanguinem effect records its source
  - `MobEffectEvent.Applicable` with a source entity
  - the Werewolves form action ids
  - `ActionEvent.ActionActivatedEvent` cancel behaviour
  - the four-legged form unlock skill
  - the full-moon forced-shift location
- Create: `parts/species/pack/GeneHandler.java`, `parts/species/pack/PackAccess.java` (helpers: `static WolfState wolf(Player)` and `static void applyPack(ServerPlayer, PackEvent, PackContext)`, which store through `BloodlineAccess.set`), and the rites `rite_of_lineage_reveal.json` (power 3000; the factory `supernaturalbloodlines:lineage` with `mode: reveal` prints the gene result to the target) and `rite_of_lineage_awaken.json` (power 9000, `mode: awaken`).

**Interfaces:**
- **On first login** (`geneRolled == false`): roll `latentGene` with `latent_gene_chance` and set `geneRolled = true`.
- **On `LivingDeathEvent`**, when the killer is a player with `latentGene`, no faction, and the victim is a villager or a player: apply GENE_AWAKENED, then `joinFaction(werewolf)`.
- **Klaus rule (R24 / R35b):** when the killer is in the **vampire** faction (plain vampire or ORIGINAL) and has `latentGene`, apply `BloodlineEvent.GENE_AWAKENED_AS_VAMPIRE` instead, then `OutcomeExecutor.run` (leave vampire → join werewolf; species HYBRID or ORIGINAL_HYBRID, bound). Set lineage BORN.
- **While `hybridCurseBound`:** Werewolves form actions are cancelled (`ActionEvent.ActionActivatedEvent`) and the message `supernaturalbloodlines.hybrid_curse.bound` is shown. The *Rite of the Moon* clears it. Hunters, vampires and werewolves all treat the player as a hybrid (R2).
- **Other factions:** a hunter with the gene doesn't awaken (the gene stays latent).

- [ ] **Step 1:** Write the GameTests:
  - `geneRolledOnceOnFirstLogin` (seeded)
  - `geneAwakensOnVillagerKill`
  - `geneDoesNotAwakenAsHunter`
  - `vampireWithGeneBecomesBoundHybrid`
  - `originalWithGeneBecomesBoundOriginalHybrid`
  - `boundHybridCannotShift`
  - `riteRevealsGene`
  - `riteAwakensWithoutGene`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(packs): hidden gene and rite of lineage".

### Task 2: Alpha-only turning and packs

**Files:**
- Create: `parts/species/pack/AlphaBite.java`, `parts/species/pack/PackIndex.java`, `parts/species/pack/PackCommand.java` (`/pack invite <player>`, `accept`, `leave`, `info`, `emissary <player>`, `anchor <player>`)

**Interfaces:**
- **`MobEffectEvent.Applicable` for lupus sanguinem:** when the source is a player werewolf whose rank isn't ALPHA or TRUE_ALPHA, set the result to DO_NOT_APPLY.
  - A wild Werewolves mob source is allowed: remember `BITTEN_BY_WILD`.
  - An Alpha player source is allowed: remember the Alpha's UUID in an attachment `pending_alpha`.
- **On Werewolves faction join** (`FactionLevelChanged`):
  - With `pending_alpha`: roll `alpha_bite_death_chance`. On a hit, the player dies (damage type `supernaturalbloodlines:rejected_bite`) and leaves the faction. Otherwise apply BITTEN_BY_ALPHA, add the player to the Alpha's pack, and record the sire (plan 8 `SireRecorder`).
  - Wild source: BITTEN_BY_WILD.
- **`PackIndex`:**
  - `Optional<UUID> alphaOf(UUID)`
  - `List<UUID> betasOf(UUID alpha)`
  - `void join(UUID alpha, UUID beta)`
  - `void leave(UUID)`
  - `void transfer(UUID oldAlpha, UUID newAlpha)`

- [ ] **Step 1:** Write the GameTests:
  - `betaBiteDoesNotInfect`
  - `alphaBiteInfectsAndJoinsPack`
  - `alphaBiteCanKill` (chance 1.0)
  - `wildBiteMakesOmega`
  - `inviteAcceptLeave`
  - `secondBiteDoesNotSwitchPack`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(packs): alpha-only turning and pack index".

### Task 3: Spark, True Alpha, ranks and eyes

**Files:**
- Create: `parts/species/pack/SparkHandler.java`, `parts/species/pack/RankEffects.java`, `parts/species/pack/DefenderKills.java`, `core/network/EyeColorPayload.java` (S2C: player id → `GOLD`, `BLUE` or `RED`), and the client handler `client/EyeColorClient.java`

**Interfaces:**
- **Steal:** on `LivingDeathEvent`, when the victim is ALPHA or TRUE_ALPHA and the killer is a werewolf player, apply KILLED_ALPHA to the killer and LEFT_PACK to the victim (Omega), then `PackIndex.transfer` per the design.
- **Inherit:** when an Alpha leaves the Werewolves faction or is cured, apply SPARK_INHERITED to the highest-level Beta.
- **Give up:** `/pack heal <player>` (Alpha only, target within 4 blocks) fully heals the target, removes `hybrid_venom` and Werewolves wolfsbane effects, and applies SPARK_GIVEN_UP.
- **Innocent kill:** a killed villager, or a player whose last hurt target wasn't the killer, applies KILLED_INNOCENT.
- **`DefenderKills`:** a hostile mob killed while its target was a pack member or a villager applies DEFENDER_KILL. Every 1200 ticks, try TRUE_ALPHA_EARNED with the R91 thresholds. No cap.
- **`RankEffects`:** attribute modifiers `supernaturalbloodlines:wolf_rank` per the design table, reapplied on rank change and login. Born wolves halve the forced full-moon shift chance (same hook as the Moon Ring).
- **Eyes:** send `EyeColorPayload` on change and to trackers. The client shows glowing eye particles (2 dust particles at eye height every 10 ticks while in a Werewolves form or roaring). A render layer on Werewolves' model is a stretch goal: record it in the docs if skipped.

- [ ] **Step 1:** Write the GameTests:
  - `killingAlphaStealsSpark`
  - `betaKillingOwnAlphaTakesPack`
  - `curedAlphaPassesSpark`
  - `healGivesUpSpark`
  - `villagerKillTurnsEyesBlue`
  - `trueAlphaAwakensWhenEligible`
  - `rankModifiersApplied`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(packs): spark, true alpha, ranks, eyes".

### Task 4: Pack bonus, Alpha Roar, Emissary, anchors, moon control

**Files:**
- Create: `parts/species/pack/PackBonus.java`, `parts/species/pack/AlphaRoar.java`, `client/PackKeybinds.java` (a `KeyMapping` "key.supernaturalbloodlines.alpha_roar", unbound), `core/network/RoarPayload.java` (C2S), and `parts/species/pack/Anchors.java` (with the item tag `supernaturalbloodlines:anchor`)

**Interfaces:**
- **`PackBonus`:** every 100 ticks per online Alpha, count the Betas within 64 blocks (bitten count double). If the count is ≥ 3 (raw, not doubled), the Alpha gets attack `+0.05 × min(doubledCount, 5)` and the Betas get Regeneration I for 120 ticks.
- **`AlphaRoar.roar(ServerPlayer)`:** ALPHA or TRUE_ALPHA only, 1200-tick cooldown, effects as in the design. Forcing non-pack wolves out of their form uses Werewolves `switchForm(NONE)` (or the deactivation action from the Task 1 notes).
- **Emissary:** `PackIndex` stores the emissary UUID. `WitchAltarBonus.costMultiplier` (plan 4) multiplies a further ×0.9 when the caster is an emissary and a pack member is within 16 blocks.
- **Anchors:** at the forced full-moon shift, if an anchor item is held or worn (`ArtifactSlots.isWorn`, or the inventory check when the artifacts part is off) or the anchor player is within 16 blocks, halve the chance.
- **`MoonControl` (R96)** (`parts/species/pack/MoonControl.java`):
  - `enum Control { NONE, HALF, FULL }`, and `static Control of(ServerPlayer p)`.
  - FULL-capable when any of these hold: a bound Moon Ring is worn; a hybrid-type that isn't bound; ALPHA or TRUE_ALPHA. FULL needs Werewolves level ≥ `full_form_level`, else HALF.
  - A BORN werewolf with an anchor bound gets HALF.
  - Otherwise NONE.
- **Form gate** (`ActionEvent.ActionActivatedEvent`), when `moon_bound_werewolves` is on:
  - Werewolf form actions are cancelled unless the full moon is forcing the shift, or the control allows that form: HALF allows the human and survivalist forms; FULL also allows beast and four-legged beast.
  - Players with control (any non-NONE) skip the forced full-moon shift.

- [ ] **Step 1:** Write the GameTests:
  - `bonusNeedsThreeBetasNear`
  - `bonusNeedsAlphaOnlineAndNear`
  - `roarBuffsPackAndSubmitsOthers`
  - `roarCooldown`
  - `emissaryCheapensRites`
  - `anchorHalvesForcedShift`
  - `moonBoundWolfCannotShiftAtWill`
  - `alphaShiftsAtWill`
  - `bornWithAnchorHalfOnly`
  - `unboundHybridShiftsAtWill`
  - `controlSkipsForcedShift`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS). Commit "feat(packs): pack bonus, roar, emissary, anchors".

### Task 5: Lunar eclipse and electricity

**Files:**
- Create: `parts/species/pack/LunarEclipse.java` (SavedData `eclipseNight: long`, set at dusk of a full-moon night by the chance roll, cleared at dawn and on load if past dawn), `core/network/EclipsePayload.java` plus the client sky tint (`ViewportEvent.ComputeFogColor` and the sky colour event, red tint while active), `parts/species/pack/Electricity.java`, the effect `shocked`, and the item `taser` (durability 16, repaired with redstone in an anvil)

**Interfaces:**
- **During the eclipse:** cancel `ActionEvent.ActionActivatedEvent` for Werewolves form actions on werewolf-faction players, remove the rank modifiers, and suppress the pack bonus. Announce through `supernaturalbloodlines.eclipse.start` and `.end`.
- **Electricity:** on `EntityStruckByLightningEvent`, and for players within 3 blocks of a strike (`LightningBolt` tick hook via `EntityJoinLevelEvent` + position scan), apply `shocked` for 600 ticks. A taser hit on a supernatural applies `shocked`. While `shocked`, form actions are cancelled and natural regeneration stops (`LivingHealEvent` cancelled when the source is natural regen; Task 1 notes how to tell).

- [ ] **Step 1:** Write the GameTests:
  - `eclipseBlocksForms`
  - `eclipseEndsAtDawnAfterReload`
  - `lightningShocksWerewolf`
  - `taserShocksSupernatural`
  - `shockedBlocksRegen`
- [ ] **Step 2:** Run them (FAIL), implement, run again (PASS).
- [ ] **Step 3:** Two-client manual check: a pack of 1 Alpha + 3 Betas, the roar, eyes, and an eclipse (`/supernaturalbloodlines debug eclipse start`, added here). Tick the pack section of `docs/parts/species.md`. Update `CHANGELOG.md`. Commit "feat(packs): lunar eclipse and electricity".
