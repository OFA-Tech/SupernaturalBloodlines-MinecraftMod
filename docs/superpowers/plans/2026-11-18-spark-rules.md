# Spark rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Alpha and True Alpha become two exclusive paths, a Beta or Omega is warned (or protected) before a spark ends their path to True Alpha, a True Alpha holds full sparks by nature, healing destroys the spark, and an `alpha_primeval` who loses the spark falls back to the race before. Plus the leftovers of the PR #38 review.

**Architecture:** Pure rows first (`PackRules`, `ApexFallbackRules`, `WolfState` fields), then the glue (`SparkHandler`, `AlphaTrial`, `PackCommand`, `ApexFallback`), then the hub (Profile action, Settings toggle, the warning dialog), then docs. Every rule row has a unit test and every non-trivial one a GameTest; each test was checked to fail with its rule reverted (the mutants script, see Verification).

**Tech Stack:** Java 21, NeoForge 21.1.265, Minecraft 1.21.1, JUnit 5, GameTests.

## Owner rules (decided)

1. **Two paths.** Whoever ever held a normal spark (stolen, inherited, Alpha Trial, any road) can never be a True Alpha. R91 changes from "never stole" to "never held". Migration: anyone who is an Alpha now counts as having held one; True Alphas unchanged.
2. **Warning.** Every Beta/Omega with the path still open is warned before a normal spark: "Taking a spark ends your path to True Alpha for good", safe button "Keep my path" focused first.
3. **True Alpha has full sparks** while active (`max_stacked_sparks`), never steals, is never robbed; extras absorbed; dormant: off.
4. The full-spark bonus **stacks** with the `alpha_primeval` x1.5.
5. **Apex fallback:** an `alpha_primeval` who loses the spark, or a dormant True Alpha, goes back to the race before; it returns with the spark.
6. **Heal destroys the spark;** the healed player stays what they were; the leader's pack goes to the strongest other Alpha, else dissolves.
7. The PR #38 leftovers.

## Interpretations (not spelled out by the owner)

- **A kill cannot show a dialog:** the player setting "protect my True Alpha path" (`WolfState.protectPath`, default on, only matters while the path is open). On: the stolen spark is destroyed and the killer gets nothing. Off: the spark is taken (and the path ends).
- **A pack inheritance cannot ask either** (cure of the leader, leaving the werewolves): a Beta who protects an open path is passed over; with nobody left the pack dissolves.
- **Duel and absence** hand the lead only between Alphas who already hold a spark, so no one takes a spark there and no dialog is needed (pinned by a GameTest).
- **The Alpha Trial** is still a hook: `AlphaTrial.win(beta, warned)` refuses an unwarned Beta whose path is open. The trial's own UI (plan 15) shows `HubScreen.warnTrueAlpha` and passes `true`.
- **"Open path"** = werewolf Beta or Omega, no held spark, no blue eyes (an innocent kill already closes it).
- **A True Alpha victim** loses nothing, stacked or not: what they hold is theirs by nature.
- **An `alpha_primeval` from before the record existed** falls back to `tensura:spirit_beast` (the line that offers the apex).
- **Heal when the healer led and no other Alpha exists:** the pack dissolves and the healer is an Omega (the dialog says so).

## Tasks

- [x] **1. Pure rows.** `WolfState` gets `held_spark` (forced true for an Alpha or a thief, so old saves migrate), `protect_path`, `pre_apex`; `BloodlineRules.clearedWolf` keeps them through a cure. `PackRules`: `true_alpha_held_spark`, `true_alpha_never_steals`, `transferableSparks` 0 for a True Alpha, `pathOpen`, `stealBy`, `mayInherit`, `effectiveSparks`. New `ApexFallbackRules`. Tests: `PackRulesTest`, `ApexFallbackRulesTest`, `BloodlineRulesTest`.
- [x] **2. Glue.** `SparkHandler.steal` (never / protected / takes), `giveUp` without an heir, `heirOf` skips protected Betas; `AlphaTrial.win(beta, warned)`; `RankEffects`/`AlphaRoar`/`PackLeader` use `effectiveSparks`; `/pack protect on|off` (P16, P17); `ApexFallback` (species_tensura).
- [x] **3. Hub.** Profile self actions `protect_on`/`protect_off` (off asks first), a Settings toggle, the warning dialog with "Keep my path", heal texts.
- [x] **4. GameTests.** `SparkRulesGameTests` (steal protected/unprotected, trial, inheritance, duel, absence, True Alpha never steals, full sparks active/dormant, heal with and without a co-Alpha, spark history after a heal and a cure), `ApexWerewolfGameTests` (fallback, dormant True Alpha, evolved-on, legacy, stacking). Existing tests that relied on the old rules were moved to the new ones (their wolves do not protect their path; True Alpha numbers are the full-spark ones).
- [x] **5. Leftovers.** `Pickable.eligible()`; the "+N more" is plain text with its reason, recomputed every frame; the focus-ring doc is on `focusRing()`; `pack.alpha_offline` reads for an ally request too; `LabelFit.cut` returns a bare "…" when only it fits; `RateLimiterForgetTest`; `PackInvite` checks `getPlayerInfo(id)`; CHANGELOG line for the removed Call action.
- [x] **6. Docs.** CHANGELOG (with the upgrade note), wiki (Werewolves and packs, Commands), `docs/parts/species.md`, `SPECIES-RULES.md` (R91c, R93b, R93c).
- [x] **7. Screens and matrix.** Hidden safe shots of the warning dialog, the Profile and the Pack page in 3 themes at s2 and s4 (`runSafeShots` under `focus-watch`, never a visible client; the pictures are in `docs/superpowers/design/spark-rules/`); the 10-run matrix (all green: 1144 unit tests, 1070 GameTests, 689 / 17 / 350 / 959 / 972 / 944 / 1060 / 1003 in the variants); PR; green CI. Do not merge.

## Verification

- `gradlew test` and `runGameTestServer` green; the 10-run matrix (`build`, the full GameTests, `-PnoWerewolves -PnoCurios`, `-PnoLinkedMods`, and `-PpartsOff` for species, artifacts, speciesTensura, supernatural, supernaturalTensura, hud).
- Mutants (each fails a test when reverted): held spark no longer blocks True Alpha, True Alpha steals (rule row and handler), no protection, inheritance forces a spark, True Alpha without full sparks, True Alpha robbed, trial does not ask, heal gives an heir, no apex fallback, cure forgets the spark history.
