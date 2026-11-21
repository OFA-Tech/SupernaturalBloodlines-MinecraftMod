package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The stateful part of the HUD fade, as a pure step: what {@code HudActivity.alpha} does each frame. */
class HudVisibilityStepTest {
    @Test
    void autoFadesOutOverTwentyTicksAndComesBackInFive() {
        float shown = 1f;
        long t = 1000;
        // idle: the target is 0, one frame per tick
        for (int i = 0; i < HudVisibility.FADE_OUT_TICKS; i++) {
            float next = HudVisibility.step(shown, true, t, t + 1, 0f);
            assertTrue(next < shown, "it fades at tick " + i);
            shown = next;
            t++;
        }
        assertEquals(0f, shown, 1e-6, "gone after FADE_OUT_TICKS");
        for (int i = 0; i < HudVisibility.FADE_IN_TICKS; i++) {
            shown = HudVisibility.step(shown, true, t, t + 1, 1f);
            t++;
        }
        assertEquals(1f, shown, 1e-6, "back after FADE_IN_TICKS");
    }

    @Test
    void theFirstFrameOfAWorldOrAClockThatWentBackJumpsStraightToTheTarget() {
        assertEquals(0f, HudVisibility.step(1f, false, Long.MIN_VALUE, 10, 0f), "first frame");
        assertEquals(0f, HudVisibility.step(1f, true, 90_000, 10, 0f), "a younger world");
        assertEquals(1f, HudVisibility.step(0f, true, 90_000, 10, 1f));
    }

    @Test
    void aFrameThatChangesNothingKeepsTheOpacityAndASkippedFrameCatchesUp() {
        assertEquals(0.5f, HudVisibility.step(0.5f, true, 100, 100, 0f));
        assertEquals(0.5f, HudVisibility.step(0.5f, true, 100, 105, 0.5f));
        assertEquals(0f, HudVisibility.step(0.5f, true, 100, 160, 0f), "a long pause (a screen) does not leave it half faded");
    }

    @Test
    void hiddenAndAlwaysIgnoreUseAndAutoFollowsIt() {
        assertEquals(0f, HudVisibility.HIDDEN.target(0));
        assertEquals(1f, HudVisibility.ALWAYS.target(1_000_000));
        assertEquals(1f, HudVisibility.AUTO.target(HudVisibility.IDLE_TICKS));
        assertEquals(0f, HudVisibility.AUTO.target(HudVisibility.IDLE_TICKS + 1));
    }
}
