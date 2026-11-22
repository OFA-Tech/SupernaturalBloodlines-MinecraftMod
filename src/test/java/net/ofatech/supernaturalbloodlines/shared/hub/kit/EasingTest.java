package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EasingTest {
    @Test
    void everyEasingStartsAtZeroAndEndsAtOne() {
        for (Easing e : Easing.values()) {
            assertEquals(0f, e.apply(0f), 1e-6, e + " start");
            assertEquals(1f, e.apply(1f), 1e-6, e + " end");
        }
    }

    @Test
    void timeOutsideTheRangeIsClamped() {
        for (Easing e : Easing.values()) {
            assertEquals(0f, e.apply(-3f), 1e-6, e + " before");
            assertEquals(1f, e.apply(7f), 1e-6, e + " after");
        }
    }

    @Test
    void easeOutArrivesFastAndSettlesOnce() {
        // halfway through, an ease-out is already past halfway; cubic further than quad; linear exactly half
        assertEquals(0.5f, Easing.LINEAR.apply(0.5f), 1e-6);
        assertEquals(0.75f, Easing.OUT_QUAD.apply(0.5f), 1e-6);
        assertEquals(0.875f, Easing.OUT_CUBIC.apply(0.5f), 1e-6);
        assertTrue(Easing.OUT_CUBIC.apply(0.25f) > Easing.OUT_QUAD.apply(0.25f));
    }

    @Test
    void easeOutNeverGoesBack() {
        for (Easing e : new Easing[] {Easing.OUT_QUAD, Easing.OUT_CUBIC}) {
            float last = 0f;
            for (int i = 0; i <= 100; i++) {
                float v = e.apply(i / 100f);
                assertTrue(v >= last, e + " went back at " + i);
                last = v;
            }
        }
    }

    @Test
    void stepsAreFourFramesAndNothingBetween() {
        assertEquals(0f, Easing.STEP4.apply(0.24f), 1e-6);
        assertEquals(1f / 3f, Easing.STEP4.apply(0.26f), 1e-6);
        assertEquals(2f / 3f, Easing.STEP4.apply(0.51f), 1e-6);
        assertEquals(1f, Easing.STEP4.apply(0.76f), 1e-6);
        assertEquals(1f, Easing.STEP4.apply(1f), 1e-6);
    }
}
