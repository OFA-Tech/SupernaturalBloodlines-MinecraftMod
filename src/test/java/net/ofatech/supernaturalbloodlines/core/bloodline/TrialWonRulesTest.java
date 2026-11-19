package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Weak;
import org.junit.jupiter.api.Test;

/** R147: what winning the Alpha Trial does to the wolf layer, and the mark of the boss. */
class TrialWonRulesTest {
    private static final String REFUSAL = "supernaturalbloodlines.refusal.";

    private static WolfState wolf(Lineage l, Rank r) {
        return WolfState.NONE.toBuilder().lineage(l).rank(r).build();
    }

    private static PackOutcome won(WolfState w) {
        return PackRules.apply(w, PackEvent.TRIAL_WON, PackContext.defaults());
    }

    @Test
    void aBetaBecomesAnAlphaWithOneSparkHeldForGood() {
        WolfState beta = wolf(Lineage.BITTEN, Rank.BETA).toBuilder().packAlpha(UUID.randomUUID()).betaSince(5).build();
        WolfState after = won(beta).state();
        assertEquals(Rank.ALPHA, after.rank());
        assertEquals(1, after.sparks());
        assertTrue(after.heldSpark());
        assertEquals(null, after.packAlpha(), "an Alpha has no Alpha; the pack is kept by the index");
    }

    @Test
    void anOmegaBecomesAnAlphaToo() {
        WolfState after = won(wolf(Lineage.BORN, Rank.OMEGA)).state();
        assertEquals(Rank.ALPHA, after.rank());
        assertTrue(after.heldSpark());
    }

    @Test
    void winningClosesThePathToTrueAlphaForGood() {
        WolfState open = wolf(Lineage.BITTEN, Rank.OMEGA);
        assertTrue(PackRules.pathOpen(open));
        WolfState after = won(open).state();
        assertFalse(PackRules.pathOpen(after));
        // even as a Beta again with every condition met, the spark that was held refuses True Alpha
        WolfState beta = after.toBuilder().rank(Rank.BETA).betaSince(1).defenderKills(5).build();
        PackContext c = new PackContext(UUID.randomUUID(), 14, 1 + 30L * 24000, 14, 30L * 24000, 1, true);
        assertEquals(REFUSAL + "true_alpha_held_spark", PackRules.apply(beta, PackEvent.TRUE_ALPHA_EARNED, c).refusalKey());
    }

    @Test
    void theWeakerSparksAreGoneButTheDayStays() {
        WolfState beta = wolf(Lineage.BITTEN, Rank.BETA).toBuilder().weak(new Weak(2, 700, 9, 70000)).build();
        assertEquals(new Weak(0, 0, 9), won(beta).state().weak());
    }

    @Test
    void anAlphaATrueAlphaAHumanAndNoRankAreRefusedAndUnchanged() {
        for (WolfState w : new WolfState[] {wolf(Lineage.BITTEN, Rank.ALPHA), wolf(Lineage.BITTEN, Rank.TRUE_ALPHA), WolfState.NONE, wolf(Lineage.NONE, Rank.BETA),
                wolf(Lineage.BORN, Rank.NONE)}) {
            PackOutcome o = won(w);
            assertTrue(o.refused(), w.toString());
            assertEquals(REFUSAL + "trial_no", o.refusalKey());
            assertEquals(w, o.state());
        }
    }

    @Test
    void theBossMarkSurvivesTheCodecAndEveryFieldIsOptional() {
        UUID id = UUID.randomUUID();
        TrialMark m = new TrialMark(id, 7, 24, 1, 120);
        assertEquals(m, TrialMark.CODEC.parse(JsonOps.INSTANCE, TrialMark.CODEC.encodeStart(JsonOps.INSTANCE, m).getOrThrow()).getOrThrow());
        TrialMark empty = TrialMark.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(null, empty.challenger());
        assertEquals(0, empty.phase());
        assertEquals(new TrialMark(id, 7, 24, 2, 120), m.withPhase(2));
        assertEquals(new TrialMark(id, 7, 24, 1, 0), m.withAway(0));
    }
}
