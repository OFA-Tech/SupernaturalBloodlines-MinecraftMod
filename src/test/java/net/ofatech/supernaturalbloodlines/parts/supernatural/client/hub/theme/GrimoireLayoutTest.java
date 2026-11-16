package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

class GrimoireLayoutTest {
    // the smallest GUI Minecraft allows, GUI scale 4 at 1920x1080 (480 x 270), scale 3 and 2 at 1920x1080, and a big screen
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};
    private static final int TABS = 8;
    private static final int SEALS = 4;

    private static List<Rect> everything(HubLayout l) {
        List<Rect> all = new ArrayList<>(l.tabs());
        all.addAll(l.seals());
        all.add(l.chrome().themeButton());
        all.add(l.chrome().close());
        return all;
    }

    @Test
    void theBookItsTabsAndItsSealsFitEveryScreen() {
        for (int[] s : SCREENS) {
            Rect screen = new Rect(0, 0, s[0], s[1]);
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            String at = s[0] + "x" + s[1];
            assertTrue(l.frame().inside(screen), at);
            assertEquals(TABS, l.tabs().size(), at);
            for (Rect r : l.tabs()) {
                assertTrue(new Rect(r.x(), r.y(), r.w() + 4, r.h()).inside(screen), at + " tab " + r); // a selected tab is 4 wider
                assertTrue(r.bottom() <= l.frame().bottom(), at + " tab below the book");
            }
            for (Rect r : l.seals()) {
                assertTrue(r.inside(screen), at + " seal off screen " + r);
                assertTrue(r.y() >= l.frame().bottom(), at + " the seals hang below the book");
            }
            assertTrue(l.chrome().themeButton().inside(screen) && l.chrome().close().inside(screen), at);
        }
    }

    @Test
    void nothingOnTheEdgesTouchesAnythingElse() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            List<Rect> all = everything(l);
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    assertFalse(all.get(i).overlaps(all.get(j)), s[0] + "x" + s[1] + ": " + all.get(i) + " on " + all.get(j));
                }
            }
        }
    }

    @Test
    void thePagesAreTwinsEitherSideOfTheSpine() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            assertEquals(l.region(HubLayout.MAIN).w(), l.region(HubLayout.SIDE).w());
            assertEquals(l.region(HubLayout.MAIN).h(), l.region(HubLayout.SIDE).h());
            assertEquals(l.region(HubLayout.MAIN).y(), l.region(HubLayout.SIDE).y());
            assertFalse(GrimoireLayout.page(l.region(HubLayout.MAIN)).overlaps(GrimoireLayout.page(l.region(HubLayout.SIDE))));
            assertTrue(GrimoireLayout.page(l.region(HubLayout.MAIN)).inside(l.frame()));
            assertTrue(GrimoireLayout.page(l.region(HubLayout.SIDE)).inside(l.frame()));
            assertTrue(l.region(HubLayout.MAIN).w() >= 110, "a page too narrow to read at " + s[0]);
            assertTrue(l.region(HubLayout.MAIN).h() >= 130, "a page too short at " + s[1]);
        }
    }

    @Test
    void theMarginNoteSitsInTheLeftPagesHeaderAndOverNoContent() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            Rect counters = l.chrome().counters();
            assertNotNull(counters);
            Rect page = GrimoireLayout.page(l.region(HubLayout.MAIN));
            assertTrue(counters.inside(page), s[0] + "x" + s[1]);
            assertFalse(counters.overlaps(l.region(HubLayout.MAIN)), "the margin note is over the text");
            assertTrue(counters.h() >= 9, "room for a line of text");
        }
    }

    @Test
    void thePageNumbersSitUnderTheTextInsideThePage() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            Rect content = l.region(HubLayout.MAIN);
            Rect page = GrimoireLayout.page(content);
            Rect number = GrimoireLayout.number(page);
            assertTrue(number.inside(page), s[0] + "x" + s[1]);
            assertFalse(number.overlaps(content), "the number is over the text");
        }
    }

    @Test
    void tabsDoNotTouchEachOtherAndStayBelowTheToasts() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], TABS, SEALS);
            assertTrue(l.tabs().get(0).y() >= GrimoireLayout.TOAST_CLEAR);
            for (int i = 1; i < l.tabs().size(); i++) {
                assertFalse(l.tabs().get(i).overlaps(l.tabs().get(i - 1)));
            }
        }
    }

    @Test
    void theBookStopsGrowingOnABigScreen() {
        HubLayout l = GrimoireLayout.of(1920, 1080, 4, 0);
        assertEquals(GrimoireLayout.MAX_W, l.frame().w());
        assertEquals(GrimoireLayout.MAX_H, l.frame().h());
    }

    @Test
    void theThemeButtonFitsEveryScreenAndTouchesNothing() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            HubLayout l = GrimoireLayout.of(s[0], s[1], 6, 3);
            Rect b = l.chrome().themeButton();
            assertNotNull(b, at + " has no theme button");
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
