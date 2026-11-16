package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/** The floating bar's buttons at every width: the title keeps its room, the primary button stays, the rest move behind "...", and nothing overlaps. */
class BarPlanTest {
    /** Quests: Hide finished, Track on HUD, Claim (primary), 6 px per character. */
    private static final List<Integer> QUESTS = List.of(13 * 6, 12 * 6, 5 * 6);

    private static Rect bar(int guiWidth) {
        HubLayout l = BloodMoonLayout.of(guiWidth, guiWidth * 9 / 16 < 240 ? 240 : guiWidth * 9 / 16, 8, 3);
        return l.chrome().detail();
    }

    private static void assertSound(BarPlan p, Rect bar, int titleW, String at) {
        List<Rect> all = new ArrayList<>();
        for (Rect r : p.pills()) {
            if (r != null) {
                all.add(r);
            }
        }
        if (p.overflow() != null) {
            all.add(p.overflow());
        }
        for (int i = 0; i < all.size(); i++) {
            assertTrue(all.get(i).inside(bar), at + ": a button leaves the bar " + all.get(i));
            assertTrue(all.get(i).x() >= bar.x() + BarPlan.PAD + p.title(), at + ": a button runs into the title");
            for (int j = i + 1; j < all.size(); j++) {
                assertFalse(all.get(i).overlaps(all.get(j)), at + ": two buttons overlap");
            }
        }
        assertTrue(p.title() >= Math.min(titleW, BarPlan.MIN_TITLE), at + ": the title has no room");
    }

    @Test
    void everythingFitsOnAWideScreen() {
        Rect b = bar(960);
        BarPlan p = BarPlan.of(b, QUESTS, 2, 30 * 6);
        assertSound(p, b, 30 * 6, "960");
        assertNull(p.overflow());
        assertTrue(p.hidden().isEmpty());
    }

    @Test
    void aNarrowScreenKeepsTheTitleAndThePrimaryAndMovesTheRestBehindTheOverflowButton() {
        Rect b = bar(320);
        BarPlan p = BarPlan.of(b, QUESTS, 2, 34 * 6);
        assertSound(p, b, 34 * 6, "320");
        assertNotNull(p.pills().get(2), "the primary button must stay");
        assertNotNull(p.overflow(), "the secondary buttons need the overflow button");
        assertEquals(List.of(0, 1), p.hidden());
        assertNull(p.pills().get(0));
        assertTrue(p.title() >= BarPlan.MIN_TITLE);
    }

    @Test
    void theSameAtEveryWidthFromTheSmallestUp() {
        for (int w : new int[] {320, 360, 426, 480, 640, 960}) {
            Rect b = bar(w);
            for (int primary : new int[] {-1, 0, 2}) {
                BarPlan p = BarPlan.of(b, QUESTS, primary, 34 * 6);
                assertSound(p, b, 34 * 6, w + " primary " + primary);
                if (primary >= 0) {
                    assertNotNull(p.pills().get(primary), w + ": the primary button is gone");
                }
            }
        }
    }
}
