package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReadyGlowTest {
    @Test
    void aSlotThatWasCoolingAndIsNotAnymoreHasJustBecomeReady() {
        assertTrue(ReadyGlow.justReady(0.3f, 0f));
        assertTrue(ReadyGlow.justReady(0.01f, 0f));
        assertFalse(ReadyGlow.justReady(0f, 0f), "ready all along");
        assertFalse(ReadyGlow.justReady(0.3f, 0.2f), "still cooling");
        assertFalse(ReadyGlow.justReady(0f, 0.5f), "a cooldown that began");
    }

    @Test
    void theGlowRunsOneLoopThenStops() {
        assertEquals(0f, ReadyGlow.alpha(-1, false));
        for (long age = 0; age < ReadyGlow.TICKS; age++) {
            float a = ReadyGlow.alpha(age, false);
            assertTrue(a >= 0.3f && a <= 0.6f, "tick " + age + " glows at " + a);
        }
        assertEquals(0f, ReadyGlow.alpha(ReadyGlow.TICKS, false));
        assertEquals(0f, ReadyGlow.alpha(10_000, false));
    }

    @Test
    void theGlowBreathesOverTheLoop() {
        float low = 1f;
        float high = 0f;
        for (long age = 0; age < ReadyGlow.TICKS; age++) {
            low = Math.min(low, ReadyGlow.alpha(age, false));
            high = Math.max(high, ReadyGlow.alpha(age, false));
        }
        assertTrue(high > low, "it is not one flat value");
    }

    @Test
    void reduceMotionSwitchesTheGlowOff() {
        for (long age = 0; age < ReadyGlow.TICKS; age++) {
            assertEquals(0f, ReadyGlow.alpha(age, true), "tick " + age);
        }
    }
}