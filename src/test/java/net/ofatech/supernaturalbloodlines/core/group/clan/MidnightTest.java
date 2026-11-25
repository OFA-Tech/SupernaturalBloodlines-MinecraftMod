package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MidnightTest {
    @Test
    void anOrdinaryTickBeforeMidnightIsNot() {
        assertFalse(Midnight.crossed(17_990, 17_991));
        assertFalse(Midnight.crossed(17_998, 17_999));
    }

    @Test
    void theTickThatLandsOnMidnightIs() {
        assertTrue(Midnight.crossed(17_999, 18_000));
        assertTrue(Midnight.crossed(18_000 + 24_000 - 1, 18_000 + 24_000));
    }

    @Test
    void exactlyAtMidnightTwiceIsOnce() {
        assertTrue(Midnight.crossed(17_999, 18_000));
        assertFalse(Midnight.crossed(18_000, 18_001));
        assertFalse(Midnight.crossed(18_000, 18_000));
    }

    @Test
    void aSleepingSkipAcrossMidnightIs() {
        assertTrue(Midnight.crossed(13_000, 24_000));
        assertTrue(Midnight.crossed(0, 24_000));
    }

    @Test
    void aJumpOverSeveralDaysIs() {
        assertTrue(Midnight.crossed(1_000, 100_000));
        assertTrue(Midnight.crossed(19_000, 43_000));
    }

    @Test
    void aJumpBackwardsIsNot() {
        assertFalse(Midnight.crossed(24_000, 5_000));
        assertFalse(Midnight.crossed(100_000, 17_000));
    }

    @Test
    void noMovementIsNot() {
        assertFalse(Midnight.crossed(18_000, 18_000));
        assertFalse(Midnight.crossed(0, 0));
    }

    @Test
    void nextMidnightAfter() {
        assertEquals(18_000, Midnight.nextAtOrAfter(0));
        assertEquals(18_000, Midnight.nextAtOrAfter(18_000));
        assertEquals(42_000, Midnight.nextAtOrAfter(18_001));
        assertEquals(42_000, Midnight.nextAtOrAfter(20_390));
        assertEquals(18_000, Midnight.nextAtOrAfter(-5_000));
        assertEquals(18_000 + 24_000 * 5, Midnight.nextAtOrAfter(18_000 + 24_000 * 4 + 1));
    }
}
