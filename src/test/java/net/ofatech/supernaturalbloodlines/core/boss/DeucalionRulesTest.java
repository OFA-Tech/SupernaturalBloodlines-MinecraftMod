package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeucalionRulesTest {
    private static DeucalionRules.Target alpha(int sparks) {
        return new DeucalionRules.Target(true, true, sparks, true, false);
    }

    @Test
    void anAlphaWithEnoughStackedSparksIsHisTargetAndOneWithFewerIsNot() {
        assertTrue(DeucalionRules.eligible(alpha(3), 3), "exactly the minimum");
        assertTrue(DeucalionRules.eligible(alpha(4), 3));
        assertFalse(DeucalionRules.eligible(alpha(2), 3), "one short");
        assertTrue(DeucalionRules.eligible(alpha(1), 1));
        assertTrue(DeucalionRules.eligible(alpha(1), 0), "a minimum of zero still needs a spark");
        assertFalse(DeucalionRules.eligible(alpha(0), 0));
    }

    @Test
    void onlyAWerewolfAlphaInTheOverworldWhoIsNotASpectatorIsATarget() {
        assertFalse(DeucalionRules.eligible(new DeucalionRules.Target(false, true, 5, true, false), 3), "not a werewolf");
        assertFalse(DeucalionRules.eligible(new DeucalionRules.Target(true, false, 5, true, false), 3), "not an Alpha");
        assertFalse(DeucalionRules.eligible(new DeucalionRules.Target(true, true, 5, false, false), 3), "not the overworld");
        assertFalse(DeucalionRules.eligible(new DeucalionRules.Target(true, true, 5, true, true), 3), "a spectator");
    }

    @Test
    void heComesOnAFullMoonNightForSomebodyAndNeverWithoutBoth() {
        assertTrue(DeucalionRules.picks(true, true));
        assertFalse(DeucalionRules.picks(false, true));
        assertFalse(DeucalionRules.picks(true, false));
    }

    @Test
    void heShiftsBelowThreeQuartersOrWithHisTargetCloseOnANightAndOnlyOnceAsAMan() {
        assertTrue(DeucalionRules.shouldShift(BossForm.HUMAN, 0.74F, false, false), "below three quarters");
        assertFalse(DeucalionRules.shouldShift(BossForm.HUMAN, 0.75F, false, false), "exactly three quarters is not below");
        assertTrue(DeucalionRules.shouldShift(BossForm.HUMAN, 1.0F, true, true), "target close, at night");
        assertFalse(DeucalionRules.shouldShift(BossForm.HUMAN, 1.0F, true, false), "target close, by day");
        assertFalse(DeucalionRules.shouldShift(BossForm.HUMAN, 1.0F, false, true), "night, target far");
        assertFalse(DeucalionRules.shouldShift(BossForm.HUMAN, 0F, true, true), "dead");
        assertFalse(DeucalionRules.shouldShift(BossForm.SHIFTING, 0.1F, true, true), "already shifting");
        assertFalse(DeucalionRules.shouldShift(BossForm.BEAST, 0.1F, true, true), "the wolf does not change back");
    }

    @Test
    void theTargetIsCloseAtEightBlocksAndNotAtNine() {
        assertTrue(DeucalionRules.targetClose(64.0));
        assertFalse(DeucalionRules.targetClose(64.01));
    }

    @Test
    void theHitboxIsTheDemonWolfsFromTheSwapTick() {
        assertEquals(new ShiftRules.Size(0.6F, 1.95F), DeucalionRules.dimensionsFor(BossForm.HUMAN, 0));
        assertEquals(new ShiftRules.Size(0.6F, 1.95F), DeucalionRules.dimensionsFor(BossForm.SHIFTING, ShiftRules.SWAP_TICK - 1));
        assertEquals(new ShiftRules.Size(1.2F, 2.6F), DeucalionRules.dimensionsFor(BossForm.SHIFTING, ShiftRules.SWAP_TICK));
        assertEquals(new ShiftRules.Size(1.2F, 2.6F), DeucalionRules.dimensionsFor(BossForm.BEAST, 0));
    }

    @Test
    void heComesAgainOnlyWhenItIsSwitchedOnAndEnoughDaysHavePassed() {
        assertTrue(DeucalionRules.mayRespawn(true, 17, 10, 7), "exactly the days");
        assertFalse(DeucalionRules.mayRespawn(true, 16, 10, 7), "a day short");
        assertFalse(DeucalionRules.mayRespawn(false, 100, 10, 7), "switched off");
        assertFalse(DeucalionRules.mayRespawn(true, 100, -1, 7), "no death written down");
        assertTrue(DeucalionRules.mayRespawn(true, 11, 10, 0), "zero days counts as one");
        assertFalse(DeucalionRules.mayRespawn(true, 10, 10, 0));
    }
}
