package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class TensuraHubLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {426, 240}, {480, 270}, {640, 360}, {960, 540}};

    @Test
    void theListTheStageAndThePaneFitWithoutOverlap() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 8, 4);
            Rect frame = l.frame();
            assertTrue(frame.inside(new Rect(0, 0, s[0], s[1])), at);
            Rect nav = TensuraHubLayout.nav(frame);
            Rect stage = l.region(TensuraHubLayout.FULL);
            Rect pane = l.chrome().detail();
            assertNotNull(pane, at);
            assertFalse(nav.overlaps(stage) || nav.overlaps(pane) || stage.overlaps(pane), at);
            assertTrue(stage.inside(frame) && pane.inside(frame) && nav.inside(frame), at);
            assertTrue(stage.w() >= 90 && pane.w() >= 110, at + " the stage or the pane is too narrow");
            List<Rect> rows = new ArrayList<>(l.tabs());
            rows.addAll(l.seals());
            for (int i = 0; i < rows.size(); i++) {
                assertTrue(rows.get(i).inside(nav), at + " row out of the list: " + rows.get(i));
                assertTrue(rows.get(i).h() >= 11, at + " row too short");
                for (int j = i + 1; j < rows.size(); j++) {
                    assertFalse(rows.get(i).overlaps(rows.get(j)), at + ": the page and mod lists run into each other");
                }
            }
            // the toasts are drawn at the top right: the lists are on the left, clear of them
            assertFalse(new Rect(s[0] - 160, 0, 160, 32).overlaps(nav), at);
        }
    }

    @Test
    void theGapBetweenTheColumnsHoldsTheScrollBarWithAirOnBothSides() {
        for (int[] s : SCREENS) {
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 8, 4);
            int gap = l.region(HubLayout.SIDE).x() - l.region(HubLayout.MAIN).right();
            // the bar is drawn one pixel off the main column, 7 wide: a pixel of air must be left before the side column
            assertTrue(gap >= 1 + net.ofatech.supernaturalbloodlines.shared.hub.kit.KitMetrics.SCROLL_W + 1, s[0] + "x" + s[1] + " gap " + gap);
        }
    }

    @Test
    void theTwoColumnsSplitTheStage() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 8, 4);
            Rect stage = l.region(TensuraHubLayout.FULL);
            Rect main = TensuraHubLayout.panel(l.region(HubLayout.MAIN));
            Rect side = TensuraHubLayout.panel(l.region(HubLayout.SIDE));
            assertFalse(main.overlaps(side), at);
            assertTrue(main.x() >= stage.x() - 4 && side.right() <= stage.right() + 4, at);
        }
    }

    @Test
    void itStopsGrowingOnABigScreen() {
        assertEquals(TensuraHubLayout.MAX_W, TensuraHubLayout.of(1920, 1080, 8, 4).frame().w());
    }

    @Test
    void theTitleBarHoldsTheStatsTheThemeButtonAndTheCloseMarkApart() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 8, 4);
            Rect b = l.chrome().themeButton();
            Rect c = l.chrome().close();
            Rect n = l.chrome().counters();
            assertNotNull(b, at);
            assertTrue(b.inside(new Rect(0, 0, s[0], s[1])) && c.inside(new Rect(0, 0, s[0], s[1])), at + " off screen");
            assertTrue(b.w() >= 14 && b.h() >= 12, at + " button too small to hit");
            assertFalse(b.overlaps(c) || n.overlaps(b) || n.overlaps(c), at + " the title bar's parts overlap");
            assertTrue(n.w() >= 60, at + " no room for the stats line");
            for (Rect r : l.tabs()) {
                assertFalse(b.overlaps(r) || n.overlaps(r), at + " the title bar's parts are on a tab");
            }
            for (Rect r : l.regions().values()) {
                assertFalse(b.overlaps(r) || n.overlaps(r), at + " the title bar's parts are on a region " + r);
            }
            // every page of the window fits in its list at the smallest screen, with the rows tall enough to read
            assertEquals(8, l.tabs().size());
            assertEquals(4, l.seals().size());
        }
    }
}
