package net.ofatech.supernaturalbloodlines.core.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocatorRulesTest {
    @Test
    void theBlurredPointIsInsideTheCellWithAStableOffsetAndNeverTheExactPosition() {
        LocatorRules.Pt a = LocatorRules.blur(1234.5, -777.5, 99L, 32);
        assertEquals(a, LocatorRules.blur(1234.5, -777.5, 99L, 32), "the same seed and the same cell give the same point");
        assertEquals(Math.floorDiv(1234, 32), Math.floorDiv((int) Math.floor(a.x()), 32), "inside the target's cell");
        assertEquals(Math.floorDiv(-778, 32), Math.floorDiv((int) Math.floor(a.z()), 32));
        assertNotEquals(1234.5, a.x(), "never the exact position");
        assertTrue(Math.abs(a.x() - 1234.5) <= 32 && Math.abs(a.z() + 777.5) <= 32);
    }

    @Test
    void stepsInsideOneCellDoNotMoveTheNeedle() {
        assertEquals(LocatorRules.blur(1230.0, -790.0, 5L, 32), LocatorRules.blur(1239.0, -785.0, 5L, 32));
        assertNotEquals(LocatorRules.blur(1230.0, -790.0, 5L, 32), LocatorRules.blur(1230.0, -790.0, 6L, 32), "another seed, another offset");
    }

    @Test
    void distanceIsWordsUnderFiftyAndHundredsAbove() {
        assertTrue(LocatorRules.distance(49).veryNear());
        assertFalse(LocatorRules.distance(50).veryNear());
        assertEquals(100, LocatorRules.distance(50).blocks());
        assertEquals(300, LocatorRules.distance(312).blocks());
        assertEquals(300, LocatorRules.distance(349).blocks());
        assertEquals(400, LocatorRules.distance(350).blocks());
        assertEquals(2000, LocatorRules.distance(1999).blocks());
    }

    // findable(enabled, online, alive, sameDimension, distanceSqr, range, cloaked, spectator, now, until)
    @Test
    void everyReasonThatTheyCannotBeFoundIsOneAnswer() {
        long now = 1000;
        long until = 5000;
        assertTrue(LocatorRules.findable(true, true, true, true, 100.0 * 100, 2000, false, false, now, until));
        assertFalse(LocatorRules.findable(false, true, true, true, 0, 2000, false, false, now, until), "setting off");
        assertFalse(LocatorRules.findable(true, false, true, true, 0, 2000, false, false, now, until), "offline");
        assertFalse(LocatorRules.findable(true, true, false, true, 0, 2000, false, false, now, until), "dead");
        assertFalse(LocatorRules.findable(true, true, true, false, 0, 2000, false, false, now, until), "another dimension");
        assertFalse(LocatorRules.findable(true, true, true, true, 2001.0 * 2001, 2000, false, false, now, until), "out of range");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, true, false, now, until), "cloaked");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, false, true, now, until), "creative or spectator");
        assertFalse(LocatorRules.findable(true, true, true, true, 0, 2000, false, false, 5000, until), "spent");
        assertTrue(LocatorRules.findable(true, true, true, true, 1e12, 0, false, false, now, until), "range 0 = the whole dimension");
        assertTrue(LocatorRules.findable(true, true, true, true, 2000.0 * 2000, 2000, false, false, now, until), "exactly at the range");
        assertTrue(LocatorRules.findable(true, true, true, true, 0, 2000, false, false, 4999, until), "the last tick");
    }
}
