package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent.*;
import static org.junit.jupiter.api.Assertions.*;

class PackRulesTest {
    private static final UUID ALPHA = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final String REFUSAL = "supernaturalbloodlines.refusal.";
    private static final long DAYS_30 = 30L * 24000;

    private static PackContext ctx() {
        return new PackContext(ALPHA, 10, 1_000_000, 10, DAYS_30, 300, true);
    }

    private static WolfState wolf(Lineage l, Rank r) {
        return WolfState.NONE.toBuilder().lineage(l).rank(r).build();
    }

    /** A bitten beta that meets every True Alpha condition. */
    private static WolfState eligible() {
        return wolf(Lineage.BITTEN, Rank.BETA).toBuilder().packAlpha(ALPHA).betaSince(1_000_000 - DAYS_30)
                .defenderKills(300).build();
    }

    private static void assertRefused(PackOutcome o, WolfState unchanged, String reason) {
        assertTrue(o.refused());
        assertEquals(REFUSAL + reason, o.refusalKey());
        assertEquals(unchanged, o.state());
    }

    @Test
    void geneAwakensBornOmega() {
        PackOutcome o = PackRules.apply(WolfState.NONE.toBuilder().latentGene(true).build(), GENE_AWAKENED, ctx());
        assertFalse(o.refused());
        assertEquals(Lineage.BORN, o.state().lineage());
        assertEquals(Rank.OMEGA, o.state().rank());
        assertTrue(o.state().latentGene());
    }

    @Test
    void geneWithoutLatentIsRefused() {
        assertRefused(PackRules.apply(WolfState.NONE, GENE_AWAKENED, ctx()), WolfState.NONE, "no_gene");
    }

    @Test
    void geneOnExistingWerewolfDoesNothing() {
        WolfState w = wolf(Lineage.BITTEN, Rank.BETA).toBuilder().latentGene(true).build();
        assertEquals(w, PackRules.apply(w, GENE_AWAKENED, ctx()).state());
    }

    @Test
    void riteAwakensEvenWithoutGene() {
        PackOutcome o = PackRules.apply(WolfState.NONE, RITE_OF_LINEAGE_AWAKEN, ctx());
        assertFalse(o.refused());
        assertEquals(Lineage.BORN, o.state().lineage());
        assertEquals(Rank.OMEGA, o.state().rank());
        assertTrue(o.state().latentGene());
        assertTrue(o.state().geneRolled());
    }

    @Test
    void riteOnWerewolfOnlyGivesTheGene() {
        WolfState w = wolf(Lineage.BITTEN, Rank.ALPHA);
        WolfState r = PackRules.apply(w, RITE_OF_LINEAGE_AWAKEN, ctx()).state();
        assertEquals(Lineage.BITTEN, r.lineage());
        assertEquals(Rank.ALPHA, r.rank());
        assertTrue(r.latentGene());
    }

    @Test
    void alphaBiteMakesBittenBetaInPack() {
        PackOutcome o = PackRules.apply(WolfState.NONE, BITTEN_BY_ALPHA, ctx());
        assertEquals(Lineage.BITTEN, o.state().lineage());
        assertEquals(Rank.BETA, o.state().rank());
        assertEquals(ALPHA, o.state().packAlpha());
        assertEquals(1_000_000, o.state().betaSince());
    }

    @Test
    void alphaBiteNeedsAnAlpha() {
        assertRefused(PackRules.apply(WolfState.NONE, BITTEN_BY_ALPHA,
                new PackContext(null, 0, 0, 10, DAYS_30, 300, false)), WolfState.NONE, "no_alpha");
    }

    @Test
    void biteOnExistingWerewolfDoesNothing() {
        WolfState w = wolf(Lineage.BORN, Rank.OMEGA);
        assertEquals(w, PackRules.apply(w, BITTEN_BY_ALPHA, ctx()).state());
        assertEquals(w, PackRules.apply(w, BITTEN_BY_WILD, ctx()).state());
    }

    @Test
    void wildBiteMakesBittenOmega() {
        PackOutcome o = PackRules.apply(WolfState.NONE, BITTEN_BY_WILD, ctx());
        assertEquals(Lineage.BITTEN, o.state().lineage());
        assertEquals(Rank.OMEGA, o.state().rank());
        assertNull(o.state().packAlpha());
    }

    @Test
    void joinedPackMakesBeta() {
        PackOutcome o = PackRules.apply(wolf(Lineage.BORN, Rank.OMEGA), JOINED_PACK, ctx());
        assertEquals(Rank.BETA, o.state().rank());
        assertEquals(ALPHA, o.state().packAlpha());
        assertEquals(1_000_000, o.state().betaSince());
        assertEquals(Lineage.BORN, o.state().lineage());
    }

    @Test
    void joinedPackRefusals() {
        WolfState omega = wolf(Lineage.BORN, Rank.OMEGA);
        assertRefused(PackRules.apply(WolfState.NONE, JOINED_PACK, ctx()), WolfState.NONE, "not_werewolf");
        assertRefused(PackRules.apply(omega, JOINED_PACK, new PackContext(null, 0, 0, 10, DAYS_30, 300, true)), omega, "no_alpha");
        assertRefused(PackRules.apply(omega, JOINED_PACK, new PackContext(ALPHA, 0, 0, 10, DAYS_30, 300, false)), omega, "no_pack");
    }

    @Test
    void killingAlphaStealsSpark() {
        PackOutcome o = PackRules.apply(wolf(Lineage.BITTEN, Rank.BETA).toBuilder().packAlpha(ALPHA).betaSince(5).build(), KILLED_ALPHA, ctx());
        assertEquals(Rank.ALPHA, o.state().rank());
        assertNull(o.state().packAlpha());
        assertTrue(o.state().stoleSpark());
        assertEquals(0, o.state().betaSince());
    }

    @Test
    void killingAlphaNeedsAWerewolf() {
        assertRefused(PackRules.apply(WolfState.NONE, KILLED_ALPHA, ctx()), WolfState.NONE, "not_werewolf");
    }

    @Test
    void sparkInheritedMakesAlphaWithoutStealing() {
        PackOutcome o = PackRules.apply(eligible(), SPARK_INHERITED, ctx());
        assertEquals(Rank.ALPHA, o.state().rank());
        assertNull(o.state().packAlpha());
        assertFalse(o.state().stoleSpark());
    }

    @Test
    void innocentKillTurnsEyesBlue() {
        assertTrue(PackRules.apply(wolf(Lineage.BORN, Rank.OMEGA), KILLED_INNOCENT, ctx()).state().killedInnocent());
    }

    @Test
    void trueAlphaNeedsEveryCondition() {
        PackOutcome ok = PackRules.apply(eligible(), TRUE_ALPHA_EARNED, ctx());
        assertFalse(ok.refused());
        assertEquals(Rank.TRUE_ALPHA, ok.state().rank());

        WolfState blue = eligible().toBuilder().killedInnocent(true).build();
        assertRefused(PackRules.apply(blue, TRUE_ALPHA_EARNED, ctx()), blue, "true_alpha_blue_eyes");

        assertRefused(PackRules.apply(eligible(), TRUE_ALPHA_EARNED,
                new PackContext(ALPHA, 9, 1_000_000, 10, DAYS_30, 300, true)), eligible(), "true_alpha_level");

        WolfState early = eligible().toBuilder().betaSince(1_000_000 - DAYS_30 + 1).build();
        assertRefused(PackRules.apply(early, TRUE_ALPHA_EARNED, ctx()), early, "true_alpha_beta_days");
        WolfState neverBeta = eligible().toBuilder().betaSince(0).build();
        assertRefused(PackRules.apply(neverBeta, TRUE_ALPHA_EARNED, ctx()), neverBeta, "true_alpha_beta_days");

        WolfState fewKills = eligible().toBuilder().defenderKills(299).build();
        assertRefused(PackRules.apply(fewKills, TRUE_ALPHA_EARNED, ctx()), fewKills, "true_alpha_defender_kills");

        WolfState thief = eligible().toBuilder().stoleSpark(true).build();
        assertRefused(PackRules.apply(thief, TRUE_ALPHA_EARNED, ctx()), thief, "true_alpha_held_spark");
    }

    @Test
    void trueAlphaIsRefusedForANonWerewolf() {
        WolfState human = eligible().toBuilder().lineage(Lineage.NONE).rank(Rank.NONE).build();
        assertRefused(PackRules.apply(human, TRUE_ALPHA_EARNED, ctx()), human, "not_werewolf");
    }

    @Test
    void trueAlphaIsRefusedWhenTheLevelCapWasNotSupplied() {
        PackContext noCap = new PackContext(ALPHA, 0, 1_000_000, 0, DAYS_30, 300, true);
        assertRefused(PackRules.apply(eligible(), TRUE_ALPHA_EARNED, noCap), eligible(), "true_alpha_level");
        assertRefused(PackRules.apply(eligible(), TRUE_ALPHA_EARNED,
                new PackContext(ALPHA, 99, 1_000_000, PackContext.defaults().maxLevel(), DAYS_30, 300, true)),
                eligible(), "true_alpha_level");
    }

    @Test
    void innocentKillOnAHumanDoesNotTurnEyesBlue() {
        assertEquals(WolfState.NONE, PackRules.apply(WolfState.NONE, KILLED_INNOCENT, ctx()).state());
    }

    @Test
    void sparkGivenUpDropsToBeta() {
        WolfState alpha = wolf(Lineage.BITTEN, Rank.ALPHA);
        PackOutcome o = PackRules.apply(alpha, SPARK_GIVEN_UP, ctx());
        assertEquals(Rank.BETA, o.state().rank());
        assertEquals(ALPHA, o.state().packAlpha());
        assertEquals(1_000_000, o.state().betaSince());
        WolfState trueAlpha = wolf(Lineage.BITTEN, Rank.TRUE_ALPHA);
        assertRefused(PackRules.apply(trueAlpha, SPARK_GIVEN_UP, ctx()), trueAlpha, "true_alpha_keeps_spark");
        WolfState beta = wolf(Lineage.BITTEN, Rank.BETA);
        assertEquals(beta, PackRules.apply(beta, SPARK_GIVEN_UP, ctx()).state());
    }

    @Test
    void defenderKillCounts() {
        WolfState w = wolf(Lineage.BITTEN, Rank.BETA).toBuilder().defenderKills(4).build();
        assertEquals(5, PackRules.apply(w, DEFENDER_KILL, ctx()).state().defenderKills());
    }

    @Test
    void leavingPackMakesOmega() {
        PackOutcome o = PackRules.apply(eligible(), LEFT_PACK, ctx());
        assertEquals(Rank.OMEGA, o.state().rank());
        assertNull(o.state().packAlpha());
        assertEquals(0, o.state().betaSince());
        assertEquals(Lineage.BITTEN, o.state().lineage());
    }

    @Test
    void leavingPackDoesNotDemoteAnAlpha() {
        WolfState alpha = wolf(Lineage.BORN, Rank.ALPHA);
        assertEquals(alpha, PackRules.apply(alpha, LEFT_PACK, ctx()).state());
    }

    @Test
    void sparkLostMakesAnAlphaAnOmegaWithNoPack() {
        WolfState alpha = wolf(Lineage.BITTEN, Rank.ALPHA).toBuilder().defenderKills(7).build();
        WolfState out = PackRules.apply(alpha, SPARK_LOST, ctx()).state();
        assertEquals(Rank.OMEGA, out.rank());
        assertNull(out.packAlpha());
        assertEquals(0, out.betaSince());
        assertEquals(Lineage.BITTEN, out.lineage());
        assertEquals(7, out.defenderKills());
        WolfState beta = wolf(Lineage.BITTEN, Rank.BETA);
        assertEquals(beta, PackRules.apply(beta, SPARK_LOST, ctx()).state());
    }

    @Test
    void sparkLostStillDemotesPlainAlpha() {
        assertEquals(Rank.OMEGA, PackRules.apply(wolf(Lineage.BORN, Rank.ALPHA), SPARK_LOST, ctx()).state().rank());
    }

    @Test
    void sparkLostRefusedForTrueAlpha() {
        WolfState trueAlpha = wolf(Lineage.BITTEN, Rank.TRUE_ALPHA).toBuilder().defenderKills(7).build();
        assertRefused(PackRules.apply(trueAlpha, SPARK_LOST, ctx()), trueAlpha, "true_alpha_keeps_spark");
    }

    @Test
    void aNewTrueAlphaLeavesTheirOldPack() {
        WolfState out = PackRules.apply(eligible(), TRUE_ALPHA_EARNED, ctx()).state();
        assertEquals(Rank.TRUE_ALPHA, out.rank());
        assertNull(out.packAlpha());
        assertEquals(0, out.betaSince());
    }

    // ---- stacked Alpha sparks (plan 14) ----

    @Test
    void aVictimsSparksAreAllTakenExceptATrueAlphasOwn() {
        assertEquals(1, PackRules.transferableSparks(1, false));
        assertEquals(3, PackRules.transferableSparks(3, false));
        assertEquals(0, PackRules.transferableSparks(1, true), "a True Alpha with none stacked has nothing to take");
        assertEquals(0, PackRules.transferableSparks(3, true), "a True Alpha's count is theirs by nature: nothing is taken, stacked or not");
        assertEquals(0, PackRules.transferableSparks(0, true), "a count that is not there is one spark");
    }

    @Test
    void aKillersSparksAddUpToTheCapAndANonAlphaStartsFromNothing() {
        assertEquals(2, PackRules.sparksAfterSteal(1, true, 1, 5));
        assertEquals(5, PackRules.sparksAfterSteal(3, true, 4, 5), "past the cap");
        assertTrue(PackRules.capped(3, true, 4, 5), "3 and 4 more is 7: the cap stopped two");
        assertTrue(!PackRules.capped(3, true, 2, 5), "exactly the cap is not capped");
        assertTrue(!PackRules.capped(3, false, 4, 5), "the 3 of one who is no Alpha count for nothing: 4 is under 5");
        assertTrue(PackRules.capped(0, false, 6, 5), "a non-Alpha is capped too");
        assertTrue(!PackRules.capped(0, true, 0, 5) && PackRules.capped(9, true, 0, 5), "nothing taken: capped only when they were over already");
        assertEquals(3, PackRules.sparksAfterSteal(1, false, 3, 5), "a non-Alpha takes all three: the rank spark and two stacked");
        assertEquals(1, PackRules.sparksAfterSteal(1, true, 1, 1), "a cap of 1 turns stacking off");
        assertEquals(1, PackRules.sparksAfterSteal(0, false, 0, 5));
    }

    @Test
    void everySparkPastTheFirstAddsTenPercentAndOneRoarTierUpToTheCap() {
        assertEquals(1.0, PackRules.sparkScale(1, 5), 1e-9);
        assertEquals(1.2, PackRules.sparkScale(3, 5), 1e-9);
        assertEquals(1.4, PackRules.sparkScale(9, 5), 1e-9, "the cap stops it");
        assertEquals(1.0, PackRules.sparkScale(0, 5), 1e-9);
        assertEquals(0, PackRules.roarTier(1, 5));
        assertEquals(2, PackRules.roarTier(3, 5));
        assertEquals(4, PackRules.roarTier(9, 5));
    }

    @Test
    void theRankAndEveryStackedSparkGoTogether() {
        WolfState stacked = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.ALPHA).sparks(4).build();
        assertEquals(1, PackRules.apply(stacked, PackEvent.SPARK_LOST, PackContext.defaults()).state().sparks());
        assertEquals(Rank.OMEGA, PackRules.apply(stacked, PackEvent.SPARK_LOST, PackContext.defaults()).state().rank());
        assertEquals(1, PackRules.apply(stacked, PackEvent.SPARK_GIVEN_UP, new PackContext(UUID.randomUUID(), 0, 5, 0, 0, 0, true)).state().sparks());
        WolfState beta = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.BETA).sparks(1).build();
        assertEquals(1, PackRules.apply(beta, PackEvent.SPARK_INHERITED, PackContext.defaults()).state().sparks(), "an heir gets one spark");
        WolfState truth = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.TRUE_ALPHA).sparks(3).build();
        assertTrue(PackRules.apply(truth, PackEvent.SPARK_LOST, PackContext.defaults()).refused(), "a True Alpha's spark is never lost");
        assertTrue(PackRules.apply(truth, PackEvent.SPARK_GIVEN_UP, PackContext.defaults()).refused());
    }

    @Test
    void aWolfStateWithNoSparkCountIsOneSparkAndTheCountSurvivesTheCodec() {
        WolfState w = WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(Rank.ALPHA).sparks(3).build();
        var encoded = WolfState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, w).getOrThrow();
        assertEquals(3, WolfState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow().sparks());
        var old = encoded.getAsJsonObject();
        old.remove("sparks");
        assertEquals(1, WolfState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow().sparks(), "an old save has one spark");
        old.addProperty("sparks", -4);
        assertEquals(1, WolfState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow().sparks(), "a bad count is one");
        assertEquals(1, WolfState.NONE.sparks());
    }

    // ---- plan 18: two separate paths, the spark history and the warning ----

    @Test
    void holdingASparkByAnyRoadClosesThePathForGood() {
        WolfState inherited = PackRules.apply(eligible(), SPARK_INHERITED, ctx()).state();
        assertTrue(inherited.heldSpark(), "an inherited spark is a held one");
        WolfState trial = PackRules.apply(eligible(), SPARK_INHERITED, ctx()).state().toBuilder().rank(Rank.OMEGA).build();
        assertRefused(PackRules.apply(trial.toBuilder().betaSince(1_000_000 - DAYS_30).defenderKills(300).build(), TRUE_ALPHA_EARNED, ctx()),
                trial.toBuilder().betaSince(1_000_000 - DAYS_30).defenderKills(300).build(), "true_alpha_held_spark");
        WolfState stolen = PackRules.apply(eligible(), KILLED_ALPHA, ctx()).state();
        assertTrue(stolen.heldSpark() && stolen.stoleSpark());
        WolfState held = eligible().toBuilder().heldSpark(true).build();
        assertRefused(PackRules.apply(held, TRUE_ALPHA_EARNED, ctx()), held, "true_alpha_held_spark");
        assertEquals(Rank.TRUE_ALPHA, PackRules.apply(eligible(), TRUE_ALPHA_EARNED, ctx()).state().rank(), "who never held one still earns it");
    }

    @Test
    void anAlphaAlwaysCountsAsHavingHeldASparkSoAnOldSaveMigrates() {
        assertTrue(wolf(Lineage.BITTEN, Rank.ALPHA).heldSpark(), "an Alpha of a world from before the field");
        assertTrue(wolf(Lineage.BITTEN, Rank.ALPHA).toBuilder().rank(Rank.OMEGA).build().heldSpark(), "and it outlives the spark");
        assertFalse(wolf(Lineage.BITTEN, Rank.TRUE_ALPHA).heldSpark(), "an existing True Alpha is unchanged");
        assertFalse(eligible().heldSpark());
        assertTrue(wolf(Lineage.BITTEN, Rank.OMEGA).toBuilder().stoleSpark(true).build().heldSpark(), "a thief held it");
        var old = com.google.gson.JsonParser.parseString("{\"lineage\":\"BITTEN\",\"rank\":\"ALPHA\"}");
        WolfState decoded = WolfState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow();
        assertTrue(decoded.rank() == Rank.ALPHA && decoded.heldSpark() && decoded.protectPath() && decoded.preApex().isEmpty(), "the defaults of a save with none of the new fields: " + decoded);
    }

    @Test
    void aTrueAlphaNeverStealsAndNothingIsTakenFromOne() {
        WolfState truth = wolf(Lineage.BITTEN, Rank.TRUE_ALPHA);
        assertRefused(PackRules.apply(truth, KILLED_ALPHA, ctx()), truth, "true_alpha_never_steals");
        assertEquals(PackRules.Steal.NEVER, PackRules.stealBy(truth));
        assertEquals(0, PackRules.transferableSparks(5, true));
    }

    @Test
    void theWarningAndTheProtectionLookAtTheSamePathOpenRule() {
        WolfState beta = eligible();
        assertTrue(PackRules.pathOpen(beta) && PackRules.pathOpen(beta.toBuilder().rank(Rank.OMEGA).build()));
        assertFalse(PackRules.pathOpen(beta.toBuilder().heldSpark(true).build()), "a held spark closed it");
        assertFalse(PackRules.pathOpen(beta.toBuilder().killedInnocent(true).build()), "blue eyes closed it");
        assertFalse(PackRules.pathOpen(wolf(Lineage.BITTEN, Rank.ALPHA)) || PackRules.pathOpen(wolf(Lineage.BITTEN, Rank.TRUE_ALPHA)), "an Alpha has no path to warn");
        assertFalse(PackRules.pathOpen(WolfState.NONE), "a human has none");
    }

    @Test
    void aKillWithAnOpenProtectedPathDestroysTheSparkInsteadOfGivingIt() {
        WolfState beta = eligible();
        assertEquals(PackRules.Steal.PROTECTED, PackRules.stealBy(beta), "on by default while open");
        assertEquals(PackRules.Steal.TAKES, PackRules.stealBy(beta.toBuilder().protectPath(false).build()), "their choice");
        assertEquals(PackRules.Steal.TAKES, PackRules.stealBy(beta.toBuilder().heldSpark(true).build()), "a closed path has nothing to protect");
        assertEquals(PackRules.Steal.TAKES, PackRules.stealBy(wolf(Lineage.BITTEN, Rank.ALPHA)), "an Alpha stacks");
    }

    @Test
    void aTrueAlphaHoldsTheFullCountWhileActiveAndThePlainOneWhileDormant() {
        assertEquals(5, PackRules.effectiveSparks(Rank.TRUE_ALPHA, 1, 5, true));
        assertEquals(5, PackRules.effectiveSparks(Rank.TRUE_ALPHA, 3, 5, true), "stacked extras are absorbed, not added");
        assertEquals(1, PackRules.effectiveSparks(Rank.TRUE_ALPHA, 1, 5, false), "dormant: the bonus is off");
        assertEquals(3, PackRules.effectiveSparks(Rank.ALPHA, 3, 5, true), "an Alpha has what they took");
        assertEquals(1, PackRules.effectiveSparks(Rank.ALPHA, 0, 5, true));
        assertEquals(1.4, PackRules.sparkScale(PackRules.effectiveSparks(Rank.TRUE_ALPHA, 1, 5, true), 5), 1e-9, "full bonus");
        assertEquals(4, PackRules.roarTier(PackRules.effectiveSparks(Rank.TRUE_ALPHA, 1, 5, true), 5), "top Roar tier");
    }

    @Test
    void healingGivesTheSparkUpWithNoHeirAndANonAlphaStaysWhatTheyWere() {
        WolfState alpha = wolf(Lineage.BITTEN, Rank.ALPHA).toBuilder().sparks(3).build();
        WolfState gave = PackRules.apply(alpha, SPARK_GIVEN_UP, new PackContext(ALPHA, 0, 5, 0, 0, 0, true)).state();
        assertEquals(Rank.BETA, gave.rank());
        assertEquals(1, gave.sparks());
        assertTrue(gave.heldSpark(), "they held it");
        WolfState patient = eligible();
        assertEquals(patient, PackRules.apply(patient, SPARK_GIVEN_UP, ctx()).state(), "the healed one gets no spark and stays a Beta");
    }
}
