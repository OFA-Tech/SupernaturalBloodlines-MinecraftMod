package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmsBonusTest {
    private static final double EPS = 1e-12;

    @Test
    void threePercentEach() {
        assertEquals(0.03, ArmsBonus.of(1, 3, 12), EPS);
        assertEquals(0.06, ArmsBonus.of(2, 3, 12), EPS);
        assertEquals(0.09, ArmsBonus.of(3, 3, 12), EPS);
    }

    @Test
    void capsAtTwelve() {
        assertEquals(0.12, ArmsBonus.of(4, 3, 12), EPS);
        assertEquals(0.12, ArmsBonus.of(5, 3, 12), EPS);
        assertEquals(0.12, ArmsBonus.of(20, 3, 12), EPS);
    }

    @Test
    void zeroBrothersIsNothing() {
        assertEquals(0, ArmsBonus.of(0, 3, 12), EPS);
        assertEquals(0, ArmsBonus.of(-1, 3, 12), EPS);
    }

    @Test
    void percentZeroIsOff() {
        assertEquals(0, ArmsBonus.of(4, 0, 12), EPS);
        assertEquals(0, ArmsBonus.of(4, 3, 0), EPS);
    }

    @Test
    void aCustomCapBelowTheSteps() {
        assertEquals(0.02, ArmsBonus.of(1, 3, 2), EPS);
    }

    private static boolean counts(boolean fits, boolean breaker, boolean online, boolean alive, boolean spectator, boolean sameDim, double distSq) {
        return ArmsBonus.counts(fits, breaker, online, alive, spectator, sameDim, distSq, 24);
    }

    @Test
    void exactlyAtTheRadiusCounts() {
        assertTrue(counts(true, false, true, true, false, true, 24.0 * 24.0));
    }

    @Test
    void justBeyondTheRadiusDoesNot() {
        assertFalse(counts(true, false, true, true, false, true, 24.0 * 24.0 + 0.001));
    }

    @Test
    void anotherDimensionDoesNot() {
        assertFalse(counts(true, false, true, true, false, false, 1));
    }

    @Test
    void aDeadOrSpectatorOrOfflineDoesNot() {
        assertFalse(counts(true, false, true, false, false, true, 1));
        assertFalse(counts(true, false, true, true, true, true, 1));
        assertFalse(counts(true, false, false, true, false, true, 1));
    }

    @Test
    void aBreakerOrANonFittingDoesNot() {
        assertFalse(counts(true, true, true, true, false, true, 1));
        assertFalse(counts(false, false, true, true, false, true, 1));
        assertTrue(counts(true, false, true, true, false, true, 1));
    }
}