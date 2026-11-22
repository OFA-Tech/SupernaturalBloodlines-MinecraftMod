package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.junit.jupiter.api.Test;

/** The panels over the columns are as tall as the room above them, and the right one keeps clear of the moon warning. */
class HudPanelRoomTest {
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}};

    @Test
    void aLineIsTenPixelsAndTheFrameEight() {
        assertEquals(0, HudLayout.panelLines(8));
        assertEquals(3, HudLayout.panelLines(38));
        assertEquals(3, HudLayout.panelLines(47));
        assertEquals(0, HudLayout.panelLines(-20));
    }

    @Test
    void theRightPanelKeepsEightPixelsFromTheMoonWarningAtEveryScreenWithOrWithoutTheModePanel() {
        for (int[] s : SCREENS) {
            Box moon = HudLayout.moonBox(s[0]);
            Box column = HudLayout.slots(s[0], s[1])[3];
            for (int head : new int[] {-1, 1, 2}) {
                int stackTop = head < 0 ? column.y() : HudLayout.preview(s[0], s[1], head).y();
                int lines = HudLayout.panelLines(HudLayout.panelRoom(true, s[0], stackTop, false));
                int h = 8 + 10 * lines;
                int y = HudLayout.rightPanelTop(s[0], stackTop, h);
                assertTrue(y >= moon.bottom() + HudLayout.MOON_CLEAR, s[0] + "x" + s[1] + " head " + head + ": the panel at " + y + " is within 8 px of the moon warning (to " + moon.bottom() + ")");
                if (lines > 0) {
                    assertTrue(y + h <= stackTop - 6 + 0, s[0] + "x" + s[1] + " head " + head + ": the panel ends at " + (y + h) + " over " + stackTop);
                }
            }
        }
    }

    @Test
    void withoutTheClearanceAFullHeightPanelWouldTouchTheMoon() {
        int[] s = SCREENS[0];
        Box moon = HudLayout.moonBox(s[0]);
        int stackTop = HudLayout.slots(s[0], s[1])[3].y();
        // the premise: a panel of the height the whole space above the column allows reaches the moon warning's text
        int tall = 8 + 10 * HudLayout.panelLines(stackTop - 6 - 2);
        assertTrue(stackTop - tall - 6 < moon.bottom() + HudLayout.MOON_CLEAR, "a panel that ignored the moon would be at " + (stackTop - tall - 6));
        assertTrue(HudLayout.rightPanelTop(s[0], stackTop, tall) >= moon.bottom() + HudLayout.MOON_CLEAR);
    }

    @Test
    void theLeftPanelFitsBetweenTheTopOfTheScreenAndItsColumnWithTheLiftOfThePillToo() {
        for (int[] s : SCREENS) {
            Box column = HudLayout.slots(s[0], s[1])[0];
            for (boolean lifted : new boolean[] {false, true}) {
                int lines = HudLayout.panelLines(HudLayout.panelRoom(false, s[0], column.y(), lifted));
                int h = 8 + 10 * lines;
                int y = HudLayout.leftTooltipTop(column.y(), h, lifted);
                assertTrue(y >= 2, s[0] + "x" + s[1] + ": on the screen");
                if (lines > 0) {
                    assertTrue(y + h <= column.y() - 6, s[0] + "x" + s[1] + " lifted " + lifted + ": the panel is over its column");
                    assertFalse(new Box(0, y, s[0], h).overlaps(new Box(0, column.y(), s[0], column.h())), "off the slots");
                }
            }
        }
    }
}
