package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrackerAnchorTest {
    private static final int[][] SCREENS = {{480, 270}, {427, 240}, {320, 240}, {640, 360}, {960, 540}};
    /** Tensura's and Vampirism's status in the top left corner: avatar, bars and stats, about 120 x 70. */
    private static final HudLayout.Box OTHER_MODS_CORNER = new HudLayout.Box(0, 0, TrackerAnchor.CORNER_W, 70);

    @Test
    void theDefaultIsTheTopCentreAndClearOfTheOtherModsCorner() {
        for (int[] s : SCREENS) {
            int[] p = TrackerAnchor.TOP_CENTER.place(s[0], s[1], 150, 0);
            HudLayout.Box pill = new HudLayout.Box(p[0], p[1], 150, TrackerAnchor.HEIGHT);
            assertFalse(pill.overlaps(OTHER_MODS_CORNER), s[0] + "x" + s[1] + ": the pill is on the other mods' status");
            // centred when the screen is wide enough, else just right of the other mods' corner
            assertEquals(Math.max(s[0] / 2 - 75, TrackerAnchor.CORNER_W + 4), p[0], 1);
        }
    }

    @Test
    void itMovesDownUnderTheBossBars() {
        int[] p = TrackerAnchor.TOP_CENTER.place(480, 270, 150, 57);
        assertTrue(p[1] >= 57 + 4, "the pill is on the boss bars: y=" + p[1]);
    }

    @Test
    void everyAnchorStaysOnScreenAndOffTheSlots() {
        for (int[] s : SCREENS) {
            HudLayout.Box[] slots = HudLayout.slots(s[0], s[1]);
            for (TrackerAnchor a : TrackerAnchor.values()) {
                int[] p = a.place(s[0], s[1], 150, 0);
                HudLayout.Box pill = new HudLayout.Box(p[0], p[1], 150, TrackerAnchor.HEIGHT);
                assertTrue(p[0] >= 0 && p[1] >= 0 && pill.right() <= s[0] && pill.bottom() <= s[1], a + " leaves a " + s[0] + "x" + s[1] + " screen");
                for (HudLayout.Box slot : slots) {
                    assertFalse(pill.overlaps(slot), a + " covers a slot on " + s[0] + "x" + s[1]);
                }
            }
        }
    }
}
