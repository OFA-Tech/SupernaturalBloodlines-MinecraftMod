package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PillVisibilityTest {
    @Test
    void thePillIsNotDrawnUntilTheModeKeyIsPressedOrAltIsHeld() {
        assertEquals(0f, PillVisibility.alpha(-1, false, false), "never switched, no Alt");
    }

    @Test
    void itShowsForAFewSecondsAfterASwitchThenFadesAway() {
        assertEquals(1f, PillVisibility.alpha(0, false, false));
        assertEquals(1f, PillVisibility.alpha(PillVisibility.HOLD_TICKS - 1, false, false));
        float mid = PillVisibility.alpha(PillVisibility.HOLD_TICKS + PillVisibility.FADE_TICKS / 2, false, false);
        assertTrue(mid > 0f && mid < 1f, "fading: " + mid);
        assertEquals(0f, PillVisibility.alpha(PillVisibility.HOLD_TICKS + PillVisibility.FADE_TICKS, false, false));
        assertEquals(0f, PillVisibility.alpha(10_000, false, false));
    }

    @Test
    void itShowsWhileAltIsHeldWhateverTheLastSwitch() {
        assertEquals(1f, PillVisibility.alpha(-1, true, false));
        assertEquals(1f, PillVisibility.alpha(10_000, true, false));
    }

    @Test
    void afterASwitchAnActionBarMessageHasThePlaceToItself() {
        assertEquals(0f, PillVisibility.alpha(0, false, true));
        assertEquals(0f, PillVisibility.alpha(30, false, true));
    }

    @Test
    void altShowsThePillEvenOverAnActionBarMessage() {
        assertEquals(1f, PillVisibility.alpha(-1, true, true));
        assertEquals(1f, PillVisibility.alpha(0, true, true));
    }
}
