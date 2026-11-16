package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/** The ladder bookmarks hang in the page's header band to the left of the SP and EP margin note, never over it, at every window size. */
class LadderTabsTest {
    /** "6 SP  ·  1,028 EP" at 6 pixels a character, as the page measures it, and what PathPage leaves between the tabs and the note. */
    private static final int NOTE = 17 * 6;

    @Test
    void theBookmarksStayLeftOfTheMarginNoteAtEveryScale() {
        for (int[] s : new int[][] {{320, 240}, {426, 240}, {480, 270}, {640, 360}, {960, 540}}) {
            HubLayout l = GrimoireLayout.of(s[0], s[1], 8, 3);
            Rect main = l.region(HubLayout.SIDE);
            Rect note = l.chrome().counters();
            int noteLeft = note.right() - NOTE;
            for (int ladders : new int[] {2, 3}) {
                List<Rect> tabs = LadderTabs.layout(main.x(), main.y() - 17, main.w(), ladders);
                for (Rect t : tabs) {
                    assertFalse(t.overlaps(new Rect(noteLeft, note.y() - 4, NOTE, 17)), "x");
                    assertTrue(t.right() <= main.right(), s[0] + "x" + s[1] + " with " + ladders + " ladders: a bookmark " + t + " touches the note at " + noteLeft);
                    assertTrue(t.x() >= main.x() && t.w() >= 20, "a bookmark too small or off the page");
                }
                for (int i = 1; i < tabs.size(); i++) {
                    assertFalse(tabs.get(i - 1).overlaps(tabs.get(i)), "two bookmarks overlap");
                }
            }
        }
    }

    @Test
    void whenThereIsNoRoomForOneTabEachASingleTabTurnsTheLadders() {
        assertEquals(1, LadderTabs.layout(0, 0, 40, 2).size());
        assertEquals(2, LadderTabs.layout(0, 0, 120, 2).size());
        assertEquals(3, LadderTabs.layout(0, 0, 200, 3).size());
    }
}
