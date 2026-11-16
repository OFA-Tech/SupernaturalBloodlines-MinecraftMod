package net.ofatech.supernaturalbloodlines.parts.hud.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudInputRulesTest {
    @Test
    void theModeKeyCountsOnTheTickItGoesDownWithAModifierHeld() {
        assertTrue(HudInput.modePressedWithModifier(false, true, false, true), "the press");
        assertFalse(HudInput.modePressedWithModifier(false, true, true, true), "still held");
        assertFalse(HudInput.modePressedWithModifier(false, false, true, true), "the release");
        assertFalse(HudInput.modePressedWithModifier(false, true, false, false), "no modifier: the mapping fires by itself");
        assertFalse(HudInput.modePressedWithModifier(true, true, false, true), "a mapping with its own modifier fires by itself");
    }

    @Test
    void aSlotKeyIgnoresCtrlAndShiftButNotAlt() {
        assertTrue(HudInput.slotAllowed(false, false), "plain");
        assertFalse(HudInput.slotAllowed(true, false), "Ctrl + Z is undo, not slot 4");
        assertFalse(HudInput.slotAllowed(false, true), "Shift + R");
        assertFalse(HudInput.slotAllowed(true, true));
    }
}
