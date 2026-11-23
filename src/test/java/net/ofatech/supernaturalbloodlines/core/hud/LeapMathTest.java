package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeapMathTest {
    @Test
    void theImpulseIsTheUnitDirectionTimesTheSpeedWithTheLift() {
        double[] v = LeapMath.impulse(3, 0, 4, 2.0, 0.5);
        assertEquals(1.2, v[0], 1e-9);
        assertEquals(0.5, v[1], 1e-9);
        assertEquals(1.6, v[2], 1e-9);
    }

    @Test
    void theLiftIsNeverUnderTheMinimum() {
        assertEquals(LeapMath.MIN_LIFT, LeapMath.impulse(1, 0, 0, 1, 0.0)[1], 1e-9);
        assertEquals(LeapMath.MIN_LIFT, LeapMath.impulse(1, 0, 0, 1, -3.0)[1], 1e-9);
        assertEquals(0.7, LeapMath.impulse(1, 0, 0, 1, 0.7)[1], 1e-9);
    }

    @Test
    void aTargetStraightAboveOrBelowIsAStraightUpPushNotNaN() {
        assertArrayEquals(new double[] {0.0, 0.5, 0.0}, LeapMath.impulse(0, 5, 0, 2.0, 0.5), 1e-9);
        assertArrayEquals(new double[] {0.0, LeapMath.MIN_LIFT, 0.0}, LeapMath.impulse(0, -5, 0, 2.0, 0.0), 1e-9);
    }

    @Test
    void theRangeIsInclusive() {
        assertTrue(LeapMath.inReach(12, 12));
        assertFalse(LeapMath.inReach(12.01, 12));
        assertTrue(LeapMath.inReach(0, 12));
    }

    @Test
    void theBlowLandsWithinTheRadius() {
        assertTrue(LeapMath.hits(2.5, LeapMath.HIT_RADIUS));
        assertFalse(LeapMath.hits(2.51, LeapMath.HIT_RADIUS));
        assertEquals(2.5, LeapMath.HIT_RADIUS);
    }

    @Test
    void theSpeedGrowsWithTheDistanceAndIsBounded() {
        assertTrue(LeapMath.speedFor(12) > LeapMath.speedFor(6));
        assertEquals(0.35, LeapMath.speedFor(0.0), 1e-9);
        assertEquals(2.0, LeapMath.speedFor(1000.0), 1e-9);
    }

    @Test
    void theLiftIsHigherForAHigherTargetAndBounded() {
        assertEquals(0.5, LeapMath.liftFor(0.0), 1e-9);
        assertEquals(0.5, LeapMath.liftFor(-4.0), 1e-9);
        assertTrue(LeapMath.liftFor(4.0) > 0.5);
        assertEquals(0.9, LeapMath.liftFor(100.0), 1e-9);
    }

    /** The leap really reaches: with vanilla's air drag (0.91) and gravity (0.08, drag 0.98) a leap from the ground at a target on the ground gets within the hit radius. */
    @Test
    void aLeapAtAnyDistanceInRangeReachesTheTarget() {
        for (double distance = 3.0; distance <= 12.0; distance += 1.0) {
            double[] v = LeapMath.impulse(distance, 0, 0, LeapMath.speedFor(distance), LeapMath.liftFor(0));
            double x = 0;
            double y = 0;
            double vx = v[0];
            double vy = v[1];
            boolean hit = false;
            for (int t = 1; t < LeapMath.FLIGHT_TICKS && !hit; t++) {
                x += vx;
                y += vy;
                vx *= 0.91;
                vy = (vy - 0.08) * 0.98;
                hit = LeapMath.hits(Math.hypot(distance - x, y), LeapMath.HIT_RADIUS);
                if (y <= 0 && vy < 0) {
                    break;
                }
            }
            assertTrue(hit, "a leap at " + distance + " blocks never came within " + LeapMath.HIT_RADIUS);
        }
    }

    @Test
    void aLandingEasesTheFirstBlocksOfAFallOnly() {
        assertEquals(0.0, LeapMath.fallAfterLeap(0.0), 1e-9);
        assertEquals(0.0, LeapMath.fallAfterLeap(LeapMath.FALL_FORGIVEN), 1e-9);
        assertEquals(2.0, LeapMath.fallAfterLeap(LeapMath.FALL_FORGIVEN + 2.0), 1e-9);
        assertEquals(92.0, LeapMath.fallAfterLeap(100.0), 1e-9);
    }

    @Test
    void aFlightEndsOnLandingOrWhenItsTimeRunsOut() {
        assertFalse(LeapMath.flightOver(3, false, true), "still on the ground it was thrown from");
        assertFalse(LeapMath.flightOver(5, true, false));
        assertTrue(LeapMath.flightOver(5, true, true));
        assertTrue(LeapMath.flightOver(LeapMath.FLIGHT_TICKS, false, false));
    }
}
