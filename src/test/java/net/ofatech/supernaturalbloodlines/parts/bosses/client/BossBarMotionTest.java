package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.Motion;
import org.junit.jupiter.api.Test;

class BossBarMotionTest {
    @Test
    void aNewHealthEasesInOverTheDurationAndLandsOnTheTarget() {
        BossBarMotion.Tween t = new BossBarMotion.Tween(1f);
        assertEquals(1f, t.value(100, false), 1e-6f, "at rest at the first health");
        t.target(0.4f, 100, false);
        float start = t.value(100, false);
        float mid = t.value(100 + Motion.BOSS_HEALTH / 2.0, false);
        float end = t.value(100 + Motion.BOSS_HEALTH, false);
        assertEquals(1f, start, 1e-6f, "it begins where it was");
        assertTrue(mid < 1f && mid > 0.4f, "half way through it is between: " + mid);
        assertEquals(0.4f, end, 1e-6f);
        assertEquals(0.4f, t.value(100 + 5 * Motion.BOSS_HEALTH, false), 1e-6f, "and stays");
    }

    @Test
    void easingOutMovesMoreInTheFirstHalfThanTheSecond() {
        BossBarMotion.Tween t = new BossBarMotion.Tween(1f);
        t.target(0f, 0, false);
        float firstHalf = 1f - t.value(Motion.BOSS_HEALTH / 2.0, false);
        float secondHalf = t.value(Motion.BOSS_HEALTH / 2.0, false) - t.value(Motion.BOSS_HEALTH, false);
        assertTrue(firstHalf > secondHalf, firstHalf + " against " + secondHalf);
    }

    @Test
    void withReduceMotionTheFillIsAtItsHealthAtOnce() {
        BossBarMotion.Tween t = new BossBarMotion.Tween(1f);
        t.target(0.25f, 50, true);
        assertEquals(0.25f, t.value(50, true), 1e-6f, "no easing: it is already there");
        BossBarMotion.Tween u = new BossBarMotion.Tween(1f);
        u.target(0.25f, 50, false);
        assertTrue(u.value(50, false) > 0.9f, "the same change without reduce motion has barely begun");
    }

    @Test
    void aTargetThatChangesMidwayStartsFromWhereTheFillIs() {
        BossBarMotion.Tween t = new BossBarMotion.Tween(1f);
        t.target(0f, 0, false);
        float where = t.value(Motion.BOSS_HEALTH / 2.0, false);
        t.target(1f, Motion.BOSS_HEALTH / 2.0, false);
        assertEquals(where, t.value(Motion.BOSS_HEALTH / 2.0, false), 1e-6f, "no jump");
        assertEquals(1f, t.value(Motion.BOSS_HEALTH / 2.0 + Motion.BOSS_HEALTH, false), 1e-6f);
    }

    @Test
    void theSameTargetAgainDoesNotRestartTheEasing() {
        BossBarMotion.Tween t = new BossBarMotion.Tween(1f);
        t.target(0.5f, 0, false);
        float before = t.value(5, false);
        t.target(0.5f, 5, false);
        assertEquals(before, t.value(5, false), 1e-6f);
        assertEquals(0.5f, t.goal(), 0f);
    }

    @Test
    void theShiftingFillBreathesAndWithReduceMotionIsHeldAtFull() {
        int lo = 100;
        int hi = 0;
        for (long tick = 0; tick < 64; tick++) {
            int p = BossBarMotion.shiftFillPercent(tick, false);
            lo = Math.min(lo, p);
            hi = Math.max(hi, p);
            assertEquals(100, BossBarMotion.shiftFillPercent(tick, true), "held at full, whatever the tick");
        }
        assertTrue(lo < 90 && hi >= 99, "it breathes: " + lo + " to " + hi);
    }

    @Test
    void theSeamFlashesAndWithReduceMotionThereIsNoFlash() {
        int lo = 100;
        int hi = 0;
        for (long tick = 0; tick < 64; tick++) {
            int p = BossBarMotion.seamPercent(tick, false);
            lo = Math.min(lo, p);
            hi = Math.max(hi, p);
            assertEquals(0, BossBarMotion.seamPercent(tick, true), "no flash at tick " + tick);
        }
        assertTrue(lo < 55 && hi > 90, "it flashes: " + lo + " to " + hi);
    }

    @Test
    void aChipIsLitOverFourTicksAndInOneFrameWithReduceMotion() {
        assertEquals(0f, BossBarMotion.chipFade(10, 10, false), 1e-6f);
        float mid = BossBarMotion.chipFade(10, 12, false);
        assertTrue(mid > 0f && mid < 1f, "" + mid);
        assertEquals(1f, BossBarMotion.chipFade(10, 14, false), 1e-6f);
        assertEquals(1f, BossBarMotion.chipFade(10, 10, true), 1e-6f, "reduce motion: lit at once");
        assertEquals(1f, BossBarMotion.chipFade(Double.NEGATIVE_INFINITY, 10, false), "a chip first seen is at rest");
    }
}
