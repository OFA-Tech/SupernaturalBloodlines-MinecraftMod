package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules.Steal;
import net.ofatech.supernaturalbloodlines.core.boss.DeucalionRewardRules.Outcome;
import org.junit.jupiter.api.Test;

class DeucalionRewardRulesTest {
    @Test
    void aKillerWhoIsNoWerewolfGetsNothingWhateverTheLadderSays() {
        for (Steal s : Steal.values()) {
            var g = DeucalionRewardRules.sparks(false, s, false, 0, 4, 6);
            assertEquals(Outcome.NOT_A_WEREWOLF, g.outcome());
            assertEquals(0, g.sparksAfter());
        }
    }

    @Test
    void aTrueAlphaTakesNothingAndKeepsWhatTheyHave() {
        var g = DeucalionRewardRules.sparks(true, Steal.NEVER, true, 1, 4, 6);
        assertEquals(Outcome.NEVER, g.outcome());
        assertEquals(1, g.sparksAfter());
    }

    @Test
    void anOpenProtectedPathDestroysTheSparksAndChangesNothing() {
        var g = DeucalionRewardRules.sparks(true, Steal.PROTECTED, false, 0, 4, 6);
        assertEquals(Outcome.PROTECTED, g.outcome());
        assertEquals(0, g.sparksAfter());
        assertFalse(g.capped());
    }

    @Test
    void aWerewolfWhoWasNoAlphaTakesAllOfThemAndAnAlphaAddsThemToTheirOwn() {
        assertEquals(4, DeucalionRewardRules.sparks(true, Steal.TAKES, false, 0, 4, 6).sparksAfter());
        assertEquals(5, DeucalionRewardRules.sparks(true, Steal.TAKES, true, 1, 4, 6).sparksAfter());
        assertEquals(Outcome.GIVEN, DeucalionRewardRules.sparks(true, Steal.TAKES, true, 1, 4, 6).outcome());
    }

    @Test
    void theStackNeverGoesAboveTheCapAndTheKillerIsToldItWasCapped() {
        var g = DeucalionRewardRules.sparks(true, Steal.TAKES, true, 4, 4, 6);
        assertEquals(6, g.sparksAfter());
        assertTrue(g.capped());
        assertFalse(DeucalionRewardRules.sparks(true, Steal.TAKES, true, 2, 4, 6).capped(), "exactly the cap is not capped");
        assertTrue(DeucalionRewardRules.sparks(true, Steal.TAKES, false, 0, 7, 6).capped(), "a non-Alpha is capped too");
        assertFalse(DeucalionRewardRules.sparks(true, Steal.TAKES, false, 3, 4, 6).capped(), "the stored 3 of one who is no Alpha count for nothing");
        assertEquals(4, DeucalionRewardRules.sparks(true, Steal.TAKES, false, 3, 4, 6).sparksAfter());
    }

    @Test
    void aBossThatPaysNoSparksPaysNone() {
        assertEquals(Outcome.NEVER, DeucalionRewardRules.sparks(true, Steal.TAKES, true, 2, 0, 6).outcome());
    }

    @Test
    void theRoarIsForAWerewolf() {
        assertTrue(DeucalionRewardRules.givesRoar(true));
        assertFalse(DeucalionRewardRules.givesRoar(false));
    }
}
