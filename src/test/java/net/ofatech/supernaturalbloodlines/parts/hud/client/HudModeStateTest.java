package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudModeStateTest {
    @Test
    void thePanelStaysForThreeSecondsAfterTheSwitchAndNoLonger() {
        assertTrue(HudModeState.within(1_000, 1_000));
        assertTrue(HudModeState.within(1_000, 1_059));
        assertFalse(HudModeState.within(1_000, 1_060));
        assertFalse(HudModeState.within(Long.MIN_VALUE, 5));
    }

    @Test
    void aYoungerWorldDoesNotKeepThePanelUp() {
        // game time restarts in every world: a switch at tick 90,000 and a world now at tick 10
        assertFalse(HudModeState.within(90_000, 10));
        assertFalse(HudModeState.within(90_000, 89_999));
    }
}
