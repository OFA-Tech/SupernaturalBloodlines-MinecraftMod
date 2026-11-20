package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KolRulesTest {
    private static KolRules.Facts hunter(int kills) {
        return new KolRules.Facts(true, kills, true, true, false);
    }

    @Test
    void heComesForAHunterWithEnoughKillsOnTheGroundInTheOverworld() {
        assertTrue(KolRules.eligible(hunter(12), 12));
        assertTrue(KolRules.eligible(hunter(20), 12));
        assertFalse(KolRules.eligible(hunter(11), 12), "one kill short");
        assertTrue(KolRules.eligible(hunter(3), 3), "the minimum is the setting");
        assertFalse(KolRules.eligible(hunter(0), 0), "a minimum of zero still needs a kill");
    }

    @Test
    void heDoesNotComeForTheWrongPlayerOrPlace() {
        assertFalse(KolRules.eligible(new KolRules.Facts(false, 20, true, true, false), 12), "not a hunter");
        assertFalse(KolRules.eligible(new KolRules.Facts(true, 20, false, true, false), 12), "not the overworld");
        assertFalse(KolRules.eligible(new KolRules.Facts(true, 20, true, false, false), 12), "in the air");
        assertFalse(KolRules.eligible(new KolRules.Facts(true, 20, true, true, true), 12), "a spectator");
    }

    @Test
    void heWithdrawsAfterAMinuteWithNobodyToHuntOrFightAndNotBefore() {
        assertTrue(KolRules.stranded(false, false));
        assertFalse(KolRules.stranded(true, false), "the prey is here");
        assertFalse(KolRules.stranded(false, true), "he is fighting");
        assertFalse(KolRules.withdraws(KolRules.WITHDRAW_AFTER - 1));
        assertTrue(KolRules.withdraws(KolRules.WITHDRAW_AFTER));
        assertEquals(1200, KolRules.WITHDRAW_AFTER);
    }

    @Test
    void heLeapsFromThreeToNineBlocksOnTheGroundAfterThePause() {
        assertTrue(KolRules.leaps(36, true, 0), "six blocks");
        assertTrue(KolRules.leaps(9, true, 0), "exactly three");
        assertTrue(KolRules.leaps(81, true, 0), "exactly nine");
        assertFalse(KolRules.leaps(8.9, true, 0), "too near");
        assertFalse(KolRules.leaps(81.1, true, 0), "too far");
        assertFalse(KolRules.leaps(36, false, 0), "in the air");
        assertFalse(KolRules.leaps(36, true, 1), "the pause is not over");
    }

    @Test
    void aGrimoirePaysItsPointsOncePerCopyAndNeverNegative() {
        assertEquals(5, KolRules.grimoireSp(false, 5));
        assertEquals(0, KolRules.grimoireSp(true, 5), "a copy that was read pays nothing");
        assertEquals(0, KolRules.grimoireSp(false, -3));
        assertEquals(0, KolRules.grimoireSp(false, 0));
    }
}