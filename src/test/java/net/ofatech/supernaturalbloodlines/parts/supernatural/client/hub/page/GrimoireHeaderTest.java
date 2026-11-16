package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/**
 * The points line is the left page's margin note and the ladder bookmarks hang in the right page's header band: they must never share pixels, nor may the
 * bookmarks leave the band onto the page's text, at any GUI size a player can have.
 */
class GrimoireHeaderTest {
    private static final int[][] SIZES = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1280, 720}};

    @Test
    void theBookmarksNeverTouchThePointsLineOrThePageText() {
        for (int[] size : SIZES) {
            HubLayout l = GrimoireLayout.of(size[0], size[1], 8, 3);
            Rect side = l.region(HubLayout.SIDE);
            Rect counters = l.chrome().counters();
            for (int ladders = 2; ladders <= 3; ladders++) {
                List<Rect> tabs = LadderTabs.layout(side.x(), side.y() - LadderTabs.BAND, side.w(), ladders);
                for (Rect tab : tabs) {
                    String at = size[0] + "x" + size[1] + " with " + ladders + " ladders";
                    assertFalse(tab.overlaps(counters), "the bookmarks cover the points line at " + at);
                    assertTrue(tab.bottom() <= side.y(), "the bookmarks run onto the page's text at " + at);
                    assertTrue(tab.y() >= side.y() - 19, "the bookmarks leave the top of the page at " + at);
                    assertTrue(tab.x() >= side.x() && tab.right() <= side.right(), "the bookmarks leave the page's width at " + at);
                }
            }
        }
    }
}
