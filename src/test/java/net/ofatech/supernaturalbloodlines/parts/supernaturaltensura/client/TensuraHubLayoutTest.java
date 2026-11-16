package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client;

import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TensuraHubLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {426, 240}, {640, 360}, {960, 540}};

    @Test
    void theWindowItsListsAndPanelsFitWithoutOverlap() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 6, 3);
            Rect frame = l.frame();
            assertTrue(frame.inside(new Rect(0, 0, s[0], s[1])), at);
            Rect nav = TensuraHubLayout.nav(frame);
            Rect main = TensuraHubLayout.panel(l.region(HubLayout.MAIN));
            Rect side = TensuraHubLayout.panel(l.region(HubLayout.SIDE));
            assertFalse(nav.overlaps(main) || nav.overlaps(side) || main.overlaps(side), at);
            assertTrue(main.inside(frame) && side.inside(frame), at);
            List<Rect> rows = new ArrayList<>(l.tabs());
            rows.addAll(l.seals());
            for (int i = 0; i < rows.size(); i++) {
                assertTrue(rows.get(i).inside(nav), at + " row out of the list: " + rows.get(i));
                for (int j = i + 1; j < rows.size(); j++) {
                    assertFalse(rows.get(i).overlaps(rows.get(j)), at + ": the page and mod lists run into each other");
                }
            }
            assertTrue(l.region(HubLayout.MAIN).w() >= 80 && l.region(HubLayout.SIDE).w() >= 90, at + " panels too narrow");
            // the toasts are drawn at the top right: the lists are on the left, clear of them
            assertFalse(new Rect(s[0] - 160, 0, 160, 32).overlaps(nav), at);
        }
    }

    @Test
    void itStopsGrowingOnABigScreen() {
        assertEquals(TensuraHubLayout.MAX_W, TensuraHubLayout.of(1920, 1080, 6, 3).frame().w());
    }

    @Test
    void theThemeButtonFitsEveryScreenAndTouchesNothing() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = TensuraHubLayout.of(s[0], s[1], 6, 3);
            Rect b = l.chrome().themeButton();
            org.junit.jupiter.api.Assertions.assertNotNull(b, at + " has no theme button");
            assertTrue(b.inside(new Rect(0, 0, s[0], s[1])), at + " button off screen " + b);
            assertTrue(b.w() >= 14 && b.h() >= 12, at + " button too small to hit");
            for (Rect r : l.tabs()) {
                assertFalse(b.overlaps(r), at + " button on a tab");
            }
            for (Rect r : l.seals()) {
                assertFalse(b.overlaps(r), at + " button on a seal");
            }
            for (Rect r : l.regions().values()) {
                assertFalse(b.overlaps(r), at + " button on a region " + r);
            }
        }
    }
}