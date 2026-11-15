package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.junit.jupiter.api.Test;

class HudLayoutTest {
    /** Widths and heights of a 1280x720 to 1920x1080 window at GUI scale 4 and 2 (and 427 wide: 1280 at scale 3). */
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {320, 240}};

    @Test
    void sixSlotsInOrderLeftColumnThenRight() {
        Box[] b = HudLayout.slots(427, 240);
        assertEquals(6, b.length);
        assertTrue(b[0].x() < 427 / 2 && b[1].x() == b[0].x() && b[2].x() == b[0].x());
        assertTrue(b[3].x() > 427 / 2 && b[4].x() == b[3].x() && b[5].x() == b[3].x());
        assertTrue(b[0].y() < b[1].y() && b[1].y() < b[2].y());
        assertEquals(b[0].y(), b[3].y());
    }

    @Test
    void neverOverlapsTheHotbarTheOffhandSlotOrTheAttackIndicator() {
        for (int[] s : SCREENS) {
            Box hotbar = HudLayout.hotbar(s[0], s[1]);
            Box offhand = HudLayout.offhand(s[0], s[1]);
            Box attack = HudLayout.attackIndicator(s[0], s[1]);
            for (Box b : HudLayout.slots(s[0], s[1])) {
                assertFalse(b.overlaps(hotbar), b + " overlaps the hotbar on " + s[0] + "x" + s[1]);
                assertFalse(b.overlaps(offhand), b + " overlaps the offhand slot on " + s[0] + "x" + s[1]);
                assertFalse(b.overlaps(attack), b + " overlaps the attack indicator on " + s[0] + "x" + s[1]);
            }
        }
    }

    @Test
    void neverGoesOffScreen() {
        for (int[] s : SCREENS) {
            for (Box b : HudLayout.slots(s[0], s[1])) {
                assertTrue(b.x() >= 0 && b.y() >= 0 && b.right() <= s[0] && b.bottom() <= s[1], b + " is off a " + s[0] + "x" + s[1] + " screen");
            }
        }
    }

    @Test
    void slotsDoNotOverlapEachOther() {
        Box[] b = HudLayout.slots(320, 180);
        for (int i = 0; i < b.length; i++) {
            for (int j = i + 1; j < b.length; j++) {
                assertFalse(b[i].overlaps(b[j]), i + " overlaps " + j);
            }
        }
    }

    @Test
    void theColumnsStandOnTheBottomEdgeLikeTheHotbar() {
        Box[] b = HudLayout.slots(427, 240);
        assertEquals(240, b[2].bottom());
        assertEquals(240, b[5].bottom());
    }
}
