package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudVisibilityTest {
    @Test
    void autoHidesAfterTenIdleSecondsAndShowsAgainAtOnce() {
        assertEquals(1f, HudVisibility.AUTO.target(0));
        assertEquals(1f, HudVisibility.AUTO.target(HudVisibility.IDLE_TICKS));
        assertEquals(0f, HudVisibility.AUTO.target(HudVisibility.IDLE_TICKS + 1));
        assertEquals(1f, HudVisibility.AUTO.target(0), "any use resets the idle time to 0");
    }

    @Test
    void alwaysNeverHidesAndHiddenNeverShows() {
        for (long idle : new long[] {0, 200, 201, 1_000_000}) {
            assertEquals(1f, HudVisibility.ALWAYS.target(idle));
            assertEquals(0f, HudVisibility.HIDDEN.target(idle));
        }
    }

    @Test
    void theFadeIsTheTransitionNotAJump() {
        float a = HudVisibility.approach(1f, 0f, 1);
        assertTrue(a > 0f && a < 1f, "one tick: " + a);
        assertEquals(0f, HudVisibility.approach(1f, 0f, HudVisibility.FADE_OUT_TICKS));
        assertEquals(1f, HudVisibility.approach(0f, 1f, HudVisibility.FADE_IN_TICKS));
        assertEquals(0.5f, HudVisibility.approach(0.5f, 0.5f, 5));
        assertEquals(0.7f, HudVisibility.approach(0.7f, 0f, 0), "no time passed");
    }
}
