package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReduceMotionTest {
    @AfterEach
    void motionBackOn() {
        ReduceMotion.install(() -> false);
    }

    @Test
    void motionIsOnUntilTheSettingSaysOtherwise() {
        assertFalse(ReduceMotion.on());
        ReduceMotion.install(() -> true);
        assertTrue(ReduceMotion.on());
    }

    @Test
    void withReduceMotionOnEveryTweenIsFinishedInTheFirstFrame() {
        for (Easing e : Easing.values()) {
            for (int duration : new int[] {Motion.PAGE_TURN, Motion.PAGE_CHANGE, Motion.PRESS_DOWN, Motion.UNLOCK, Motion.BOSS_HEALTH}) {
                assertEquals(1f, Motion.progress(100, 100, duration, e, true), 1e-6, e + " " + duration);
                // and the same tween with motion on has not started: the setting is what changes it
                assertEquals(0f, Motion.progress(100, 100, duration, e, false), 1e-6, e + " " + duration + " (motion on)");
            }
        }
    }

    @Test
    void aStateChangeStillLandsAtOnceWithReduceMotion() {
        AnimClock clock = new AnimClock();
        clock.progress("tab", "off", 10, 4, Easing.OUT_QUAD, true);
        assertEquals(1f, clock.progress("tab", "on", 11, 4, Easing.OUT_QUAD, true), 1e-6);
        AnimClock moving = new AnimClock();
        moving.progress("tab", "off", 10, 4, Easing.OUT_QUAD, false);
        assertEquals(0f, moving.progress("tab", "on", 11, 4, Easing.OUT_QUAD, false), 1e-6);
    }

    @Test
    void thePositionLandsOnTheTargetAndTheGlowIsHeld() {
        assertEquals(4, Motion.texels(0, 4, Motion.progress(0, 0, Motion.PAGE_CHANGE, Easing.OUT_QUAD, true)));
        for (long tick = 0; tick < 90; tick++) {
            assertEquals(Motion.GLOW_HELD, Motion.glowPercent(tick, true), "tick " + tick);
        }
    }

    @Test
    void theHudHaloBreathesWithMotionOnAndIsHeldAtItsBrightestWithItOff() {
        float low = 1f;
        float high = 0f;
        for (long tick = 0; tick < 100; tick++) {
            float p = Motion.pulse(tick, false);
            low = Math.min(low, p);
            high = Math.max(high, p);
            assertEquals(1f, Motion.pulse(tick, true), 1e-6, "tick " + tick);
        }
        assertTrue(low < 0.1f && high > 0.9f, "the halo moves from " + low + " to " + high);
        assertEquals(0.5f, Motion.pulse(0, false), 1e-6);
    }}
