package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.Rect;
import org.junit.jupiter.api.Test;

class GrimoireLayoutTest {
    // the smallest GUI Minecraft allows, GUI scale 4 at 1920x1080, scale 2 at 1280x720 and 1920x1080
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};

    @Test
    void theBookAndItsRibbonsFitEveryScreen() {
        for (int[] s : SCREENS) {
            Rect screen = new Rect(0, 0, s[0], s[1]);
            HubLayout l = GrimoireLayout.of(s[0], s[1], 6, 3);
            String at = s[0] + "x" + s[1];
            assertTrue(l.frame().inside(screen), at);
            assertEquals(6, l.tabs().size(), at);
            for (Rect r : l.tabs()) {
                assertTrue(new Rect(r.x(), r.y(), r.w() + 4, r.h()).inside(screen), at + " ribbon " + r); // a selected ribbon is 4 wider
                assertTrue(r.bottom() <= l.frame().bottom(), at + " ribbon below the book");
            }
        }
    }

    @Test
    void thePagesAreTwinsEitherSideOfTheSpine() {
        for (int[] s : SCREENS) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], 6, 3);
            assertEquals(l.region(HubLayout.MAIN).w(), l.region(HubLayout.SIDE).w());
            assertEquals(l.region(HubLayout.MAIN).h(), l.region(HubLayout.SIDE).h());
            assertEquals(l.region(HubLayout.MAIN).y(), l.region(HubLayout.SIDE).y());
            assertFalse(GrimoireLayout.page(l.region(HubLayout.MAIN)).overlaps(GrimoireLayout.page(l.region(HubLayout.SIDE))));
            assertTrue(GrimoireLayout.page(l.region(HubLayout.MAIN)).inside(l.frame()));
            assertTrue(GrimoireLayout.page(l.region(HubLayout.SIDE)).inside(l.frame()));
            assertTrue(l.region(HubLayout.MAIN).w() >= 110, "a page too narrow to read at " + s[0]);
        }
    }

    @Test
    void ribbonsDoNotTouchEachOther() {
        HubLayout l = GrimoireLayout.of(320, 240, 6, 3);
        for (int i = 1; i < l.tabs().size(); i++) {
            assertFalse(l.tabs().get(i).overlaps(l.tabs().get(i - 1)));
        }
    }

    @Test
    void ribbonsStayBelowTheToasts() {
        for (int[] s : SCREENS) {
            assertTrue(GrimoireLayout.of(s[0], s[1], 6, 3).tabs().get(0).y() >= GrimoireLayout.TOAST_CLEAR);
        }
    }

    @Test
    void sealsSitUnderTheRibbonsOnTheSmallestScreen() {
        HubLayout l = GrimoireLayout.of(320, 240, 6, 3);
        Rect screen = new Rect(0, 0, 320, 240);
        for (Rect s : l.seals()) {
            assertTrue(s.inside(screen), "seal off screen " + s);
            for (Rect r : l.tabs()) {
                assertFalse(s.overlaps(r), "seal " + s + " on ribbon " + r);
            }
        }
        for (int i = 1; i < l.seals().size(); i++) {
            assertFalse(l.seals().get(i).overlaps(l.seals().get(i - 1)));
        }
    }

    @Test
    void theBookStopsGrowingOnABigScreen() {
        HubLayout l = GrimoireLayout.of(1920, 1080, 4, 0);
        assertEquals(GrimoireLayout.MAX_W, l.frame().w());
        assertEquals(GrimoireLayout.MAX_H, l.frame().h());
    }
}
