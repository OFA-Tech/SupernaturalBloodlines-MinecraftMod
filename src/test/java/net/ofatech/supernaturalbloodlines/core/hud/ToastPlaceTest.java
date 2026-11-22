package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.junit.jupiter.api.Test;

class ToastPlaceTest {
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}};
    /** The status that Tensura and Vampirism draw in the top left: about 140 wide and 70 tall. */
    private static final Box STATUS = new Box(6, TrackerAnchor.TOP, TrackerAnchor.CORNER_W, TrackerAnchor.CORNER_H);

    @Test
    void byDefaultTheToastKeepsTheCornerAndStandsUnderTheStatusOnlyWhenThereIsOne() {
        for (int[] s : SCREENS) {
            Box plain = ToastPlace.place(s[0], s[1], 150, 41, false, TrackerAnchor.TOP_LEFT, null);
            assertEquals(ToastPlace.X, plain.x());
            assertEquals(ToastPlace.Y, plain.y());
            Box under = ToastPlace.place(s[0], s[1], 150, 41, true, TrackerAnchor.TOP_LEFT, null);
            assertFalse(under.overlaps(STATUS), s[0] + "x" + s[1] + ": " + under + " on the status");
            assertTrue(under.y() >= STATUS.bottom());
        }
    }

    @Test
    void theToastNeverCoversTheTrackedTasksPillWhateverTheCornersOfBoth() {
        for (int[] s : SCREENS) {
            for (TrackerAnchor toastAt : TrackerAnchor.values()) {
                for (TrackerAnchor trackerAt : TrackerAnchor.values()) {
                    for (int trackerH : new int[] {TrackerAnchor.height(1), TrackerAnchor.height(2)}) {
                        int[] at = trackerAt.place(s[0], s[1], 150, trackerH, 0);
                        Box tracker = new Box(at[0], at[1], 150, trackerH);
                        for (boolean status : new boolean[] {false, true}) {
                            Box toast = ToastPlace.place(s[0], s[1], 150, 41, status, toastAt, tracker);
                            String what = s[0] + "x" + s[1] + " toast " + toastAt + " tracker " + trackerAt + " status " + status + ": " + toast + " on " + tracker;
                            assertFalse(toast.overlaps(tracker), what);
                            assertTrue(toast.x() >= 0 && toast.y() >= 0 && toast.right() <= s[0] && toast.bottom() <= s[1], "on the screen: " + what);
                            if (status && toastAt == TrackerAnchor.TOP_LEFT) {
                                assertFalse(toast.overlaps(STATUS), "and off the status: " + what);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void theToastDoesNotMoveWhenThereIsNothingInItsWay() {
        int[] at = TrackerAnchor.ABOVE_MODE_PILL.place(480, 270, 150, 22, 0);
        Box tracker = new Box(at[0], at[1], 150, 22);
        assertEquals(ToastPlace.place(480, 270, 150, 41, false, TrackerAnchor.TOP_LEFT, null), ToastPlace.place(480, 270, 150, 41, false, TrackerAnchor.TOP_LEFT, tracker));
    }

    @Test
    void theToastIsKeptOnTheScreenWhateverItsSize() {
        Box b = ToastPlace.place(100, 80, 150, 41, true, TrackerAnchor.TOP_LEFT, null);
        assertTrue(b.x() >= 0 && b.y() >= 0 && b.bottom() <= 80);
    }
}
