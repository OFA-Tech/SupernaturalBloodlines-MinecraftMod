package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PieSweepTest {
    @Test
    void theSweepStartsAtTwelveAndRunsClockwise() {
        // a quarter covers the top right and nothing of the left half
        assertTrue(PieSweep.covered(5, -5, 0.25f));
        assertFalse(PieSweep.covered(-5, -5, 0.25f));
        assertFalse(PieSweep.covered(5, 5, 0.25f));
        // a half covers the whole right side, a three quarters adds the bottom left
        assertTrue(PieSweep.covered(5, 5, 0.5f));
        assertFalse(PieSweep.covered(-5, 5, 0.5f));
        assertTrue(PieSweep.covered(-5, 5, 0.75f));
        assertFalse(PieSweep.covered(-5, -5, 0.75f));
    }

    @Test
    void emptyAndFullAreNothingAndEverything() {
        assertFalse(PieSweep.covered(3, 3, 0f));
        assertTrue(PieSweep.covered(-3, -3, 1f));
    }

    @Test
    void theCoveredShareFollowsTheFraction() {
        for (float f : new float[] {0.25f, 0.5f, 0.75f}) {
            assertEquals(f, PieSweep.coveredShare(16, f), 0.04, "fraction " + f);
        }
    }
}
