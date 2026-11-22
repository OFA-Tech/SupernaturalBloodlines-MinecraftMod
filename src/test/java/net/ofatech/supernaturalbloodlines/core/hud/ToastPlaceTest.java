package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.junit.jupiter.api.Test;

class ToastPlaceTest {
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};
    /** The status that Tensura and Vampirism draw in the top left: about 140 wide and 70 tall. */
    private static final Box STATUS = new Box(6, TrackerAnchor.TOP, TrackerAnchor.CORNER_W, TrackerAnchor.CORNER_H);

    @Test
    void withoutTheOtherModsStatusTheToastKeepsTheCorner() {
        for (int[] s : SCREENS) {
            Box b = ToastPlace.place(s[0], s[1], 150, 41, false);
            assertEquals(ToastPlace.X, b.x());
            assertEquals(ToastPlace.Y, b.y());
        }
    }

    @Test
    void withItTheToastStandsUnderTheStatusAndNeverOnIt() {
        for (int[] s : SCREENS) {
            Box b = ToastPlace.place(s[0], s[1], 150, 41, true);
            assertFalse(b.overlaps(STATUS), s[0] + "x" + s[1] + ": " + b + " on the status");
            assertTrue(b.y() >= STATUS.bottom(), s[0] + "x" + s[1] + ": under it");
            assertTrue(b.x() >= 0 && b.right() <= s[0] && b.bottom() <= s[1], "and on the screen");
        }
    }

    @Test
    void theToastIsKeptOnTheScreenWhateverItsSize() {
        Box b = ToastPlace.place(100, 80, 150, 41, true);
        assertTrue(b.x() >= 0 && b.y() >= 0 && b.bottom() <= 80);
    }

    @Test
    void theTrackerKeepsTheSameCornerClear() {
        int[] at = TrackerAnchor.TOP_LEFT.place(480, 270, 100, 22, 0);
        assertEquals(TrackerAnchor.TOP + TrackerAnchor.CORNER_H, at[1]);
    }
}