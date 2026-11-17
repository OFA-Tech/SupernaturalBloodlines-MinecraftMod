package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class DevShotsGateTest {
    @AfterEach
    void clear() {
        System.clearProperty(DevShotsGate.HIDDEN_PROPERTY);
    }

    @Test
    void offByDefaultAndOnlyTrueTurnsItOn() {
        assertFalse(DevShotsGate.hidden());
        System.setProperty(DevShotsGate.HIDDEN_PROPERTY, "false");
        assertFalse(DevShotsGate.hidden());
        System.setProperty(DevShotsGate.HIDDEN_PROPERTY, "true");
        assertTrue(DevShotsGate.hidden());
    }

    @Test
    void hintsAreAppliedOnlyWhenHidden() {
        assertEquals(0, DevShotsGate.windowHints(false).length);
        assertArrayEquals(new int[][] {
            {GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE},
            {GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE},
            {GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE},
            {GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE}
        }, DevShotsGate.windowHints(true));
    }

    @Test
    void resizeIsSkippedWhenHidden() {
        assertTrue(DevShotsGate.mayResizeWindow(false));
        assertFalse(DevShotsGate.mayResizeWindow(true));
    }

    @Test
    void cursorMovesOnlyInAFocusedWindowWhenNotHidden() {
        assertTrue(DevShotsGate.mayMoveCursor(false, true));
        assertFalse(DevShotsGate.mayMoveCursor(false, false));
        assertFalse(DevShotsGate.mayMoveCursor(true, true));
        assertFalse(DevShotsGate.mayMoveCursor(true, false));
    }
}
