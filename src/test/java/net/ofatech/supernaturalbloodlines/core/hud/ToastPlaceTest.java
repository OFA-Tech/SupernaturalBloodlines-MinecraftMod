package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.shared.hud.HudPlacement;
import org.junit.jupiter.api.Test;

class ToastPlaceTest {
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}};
    /** The status that Tensura and Vampirism draw in the top left: about 140 wide and 70 tall. */
    private static final Box STATUS = new Box(6, TrackerAnchor.TOP, TrackerAnchor.CORNER_W, TrackerAnchor.CORNER_H);

    private static Box place(int[] s, boolean status, TrackerAnchor anchor, Box tracker, int boss) {
        return ToastPlace.place(s[0], s[1], 150, 41, status, anchor, tracker, boss);
    }

    @Test
    void byDefaultTheToastKeepsTheCornerAndStandsUnderTheStatusOnlyWhenThereIsOne() {
        for (int[] s : SCREENS) {
            Box plain = place(s, false, TrackerAnchor.TOP_LEFT, null, 0);
            assertEquals(ToastPlace.X, plain.x());
            assertEquals(ToastPlace.Y, plain.y());
            Box under = place(s, true, TrackerAnchor.TOP_LEFT, null, 0);
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
                            Box toast = place(s, status, toastAt, tracker, 0);
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
    void aToastAtTheTopCentreStandsUnderTheBossBars() {
        for (int[] s : SCREENS) {
            for (int boss : new int[] {0, 30, 60, 90}) {
                Box toast = place(s, false, TrackerAnchor.TOP_CENTER, null, boss);
                assertTrue(toast.y() >= boss + 4 || toast.bottom() == s[1], s[0] + "x" + s[1] + " boss bars to " + boss + ": the toast is at " + toast.y());
            }
        }
        assertTrue(place(SCREENS[2], false, TrackerAnchor.TOP_CENTER, null, 60).y() > place(SCREENS[2], false, TrackerAnchor.TOP_CENTER, null, 0).y(), "and moves with them");
    }

    @Test
    void bothAboveTheModePillTheToastStandsAboveTheTrackerAndOffThePillTheColumnsAndTheHotbar() {
        for (int[] s : SCREENS) {
            for (int trackerH : new int[] {TrackerAnchor.height(1), TrackerAnchor.height(2)}) {
                int[] at = TrackerAnchor.ABOVE_MODE_PILL.place(s[0], s[1], 150, trackerH, 0);
                Box tracker = new Box(at[0], at[1], 150, trackerH);
                Box toast = place(s, false, TrackerAnchor.ABOVE_MODE_PILL, tracker, 0);
                String what = s[0] + "x" + s[1] + " " + toast + " over " + tracker;
                assertFalse(toast.overlaps(tracker), what);
                assertTrue(toast.bottom() <= tracker.y(), "above it, not under it: " + what);
                for (int textW : new int[] {60, 130, 200}) {
                    assertFalse(toast.overlaps(HudLayout.pill(s[0], s[1], textW)), "on the mode pill " + what);
                    assertFalse(toast.overlaps(HudLayout.pill(s[0], s[1], textW, true)), "on the lifted mode pill " + what);
                }
                for (Box slot : HudLayout.slots(s[0], s[1])) {
                    assertFalse(toast.overlaps(slot), "on a slot " + slot + ": " + what);
                }
                assertFalse(toast.overlaps(HudLayout.hotbar(s[0], s[1])), "on the hotbar " + what);
            }
        }
    }

    @Test
    void theToastDoesNotMoveWhenThereIsNothingInItsWay() {
        int[] at = TrackerAnchor.ABOVE_MODE_PILL.place(480, 270, 150, 22, 0);
        Box tracker = new Box(at[0], at[1], 150, 22);
        assertEquals(ToastPlace.place(480, 270, 150, 41, false, TrackerAnchor.TOP_LEFT, null, 0), ToastPlace.place(480, 270, 150, 41, false, TrackerAnchor.TOP_LEFT, tracker, 0));
    }

    @Test
    void theToastIsKeptOnTheScreenWhateverItsSize() {
        Box b = ToastPlace.place(100, 80, 150, 41, true, TrackerAnchor.TOP_LEFT, null, 0);
        assertTrue(b.x() >= 0 && b.y() >= 0 && b.bottom() <= 80);
    }

    @Test
    void aNewFrameForgetsTheLastOnesTrackerAndBossBars() {
        HudPlacement.trackerBox(new Box(1, 2, 3, 4));
        HudPlacement.bossBottom(77);
        HudPlacement.beginFrame();
        assertNull(HudPlacement.trackerBox());
        assertEquals(0, HudPlacement.bossBottom());
    }
}
