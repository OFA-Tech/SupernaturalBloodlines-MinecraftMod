package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import org.junit.jupiter.api.Test;

class HudLayoutTest {
    /** Widths and heights of a 1280x720 to 1920x1080 window at GUI scale 4 and 2 (and 427 wide: 1280 at scale 3). */
    private static final int[][] SCREENS = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {320, 240}, {213, 120}, {240, 135}, {256, 144}, {171, 96}};

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
    void aNarrowWindowLiftsAColumnThatDoesNotFitAboveTheHotbarStrip() {
        // 854 x 480 at GUI scale 4 is 213 x 120: the offhand slot itself is off screen, so both columns are lifted
        Box[] b = HudLayout.slots(213, 120);
        assertTrue(b[0].x() >= 0 && b[5].right() <= 213);
        assertTrue(b[2].bottom() <= 120 - HudLayout.HOTBAR_STRIP);
        assertTrue(b[5].bottom() <= 120 - HudLayout.HOTBAR_STRIP);
    }

    @Test
    void theColumnsStandJustAboveTheBottomEdge() {
        Box[] b = HudLayout.slots(427, 240);
        assertEquals(240 - HudLayout.BOTTOM, b[2].bottom());
        assertEquals(240 - HudLayout.BOTTOM, b[5].bottom());
    }

    @Test
    void allThreeSlotsOfBothColumnsFitAboveTheBottomEdgeAtEveryGuiScale() {
        // 1920 x 1080 at GUI scale 2, 3 and 4, 1280 x 720 at 2 and 3, and the smallest the design allows
        for (int[] s : new int[][] {{960, 540}, {640, 360}, {480, 270}, {640, 360}, {427, 240}, {320, 240}}) {
            for (Box b : HudLayout.slots(s[0], s[1])) {
                assertTrue(b.y() >= 0 && b.bottom() <= s[1] - HudLayout.BOTTOM, b + " is cut by the bottom of a " + s[0] + "x" + s[1] + " screen");
            }
        }
    }

    /** GUI sizes of a 1280x720 and a 1920x1080 window at GUI scales 4, 3 and 2. */
    private static final int[][] SCALES = {{320, 180}, {427, 240}, {480, 270}, {640, 360}, {960, 540}};

    @Test
    void thePillThePanelAndTheSlotsNeverShareAPixelAtGuiScales2To4() {
        for (int[] s : SCALES) {
            Box[] slots = HudLayout.slots(s[0], s[1]);
            Box hotbar = HudLayout.hotbar(s[0], s[1]);
            for (int textW : new int[] {60, 130, 200}) {
                Box pill = HudLayout.pill(s[0], s[1], textW);
                assertFalse(pill.overlaps(hotbar), "pill on the hotbar " + s[0] + "x" + s[1]);
                for (Box slot : slots) {
                    assertFalse(pill.overlaps(slot), "pill " + pill + " on " + slot + " " + s[0] + "x" + s[1] + " text " + textW);
                }
                for (int head : new int[] {1, 2}) {
                    Box panel = HudLayout.preview(s[0], s[1], head);
                    assertFalse(pill.overlaps(panel), "pill " + pill + " on the panel " + panel + " " + s[0] + "x" + s[1] + " text " + textW);
                    for (Box slot : slots) {
                        assertFalse(panel.overlaps(slot), "panel on " + slot + " " + s[0] + "x" + s[1]);
                    }
                    assertTrue(panel.x() >= 0 && panel.right() <= s[0] && panel.y() >= 0, "panel off screen " + panel + " " + s[0] + "x" + s[1]);
                }
            }
        }
    }

    @Test
    void theActionBarLineStandsOnThePill() {
        // why the pill gives way (PillVisibility): at every size the two share pixels
        for (int[] s : SCALES) {
            assertTrue(HudLayout.pill(s[0], s[1], 130).overlaps(HudLayout.actionBar(s[0], s[1], 80)), s[0] + "x" + s[1]);
        }
    }

    @Test
    void theLiftedPillClearsTheMessageTheSlotsThePanelAndTheLeftTooltipAtGuiScales2To4() {
        for (int[] s : SCALES) {
            Box[] slots = HudLayout.slots(s[0], s[1]);
            for (int textW : new int[] {60, 130, 200}) {
                Box pill = HudLayout.pill(s[0], s[1], textW, true);
                assertTrue(pill.x() >= 0 && pill.right() <= s[0] && pill.y() >= 0, "on screen " + pill + " " + s[0] + "x" + s[1]);
                for (int msgW : new int[] {40, 120, 220}) {
                    assertFalse(pill.overlaps(HudLayout.actionBar(s[0], s[1], msgW)), "the message " + msgW + " " + s[0] + "x" + s[1]);
                }
                for (Box slot : slots) {
                    assertFalse(pill.overlaps(slot), "slot " + slot + " " + s[0] + "x" + s[1]);
                }
                for (int head : new int[] {1, 2}) {
                    assertFalse(pill.overlaps(HudLayout.preview(s[0], s[1], head)), "panel " + s[0] + "x" + s[1] + " text " + textW);
                }
                for (int h : new int[] {40, 80}) {
                    int top = HudLayout.leftTooltipTop(slots[0].y(), h, true);
                    assertTrue(top + h <= pill.y() - 2, "the tooltip stands on the pill: " + top + "+" + h + " vs " + pill + " " + s[0] + "x" + s[1]);
                }
            }
        }
    }

    @Test
    void anUnliftedPillStillSitsOnTheMessageWhichIsWhyItLifts() {
        for (int[] s : SCALES) {
            assertTrue(HudLayout.pill(s[0], s[1], 130, false).overlaps(HudLayout.actionBar(s[0], s[1], 80)));
            assertTrue(HudLayout.pill(s[0], s[1], 130, true).y() < HudLayout.pill(s[0], s[1], 130, false).y());
        }
    }
}
