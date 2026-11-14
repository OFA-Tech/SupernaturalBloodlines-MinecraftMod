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
        assertRefused(PackRules.apply(thief, TRUE_ALPHA_EARNED, ctx()), thief, "true_alpha_stole_spark");
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
}
