package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BloodMoonLayoutTest {
    // the smallest GUI, GUI scale 3 and 2 at 1280x720, and a big screen
    private static final int[][] SCREENS = {{320, 240}, {426, 240}, {640, 360}, {960, 540}};

    @Test
    void everythingFitsAndNothingOverlaps() {
        for (int[] s : SCREENS) {
            String at = s[0] + "x" + s[1];
            Rect screen = new Rect(0, 0, s[0], s[1]);
            HubLayout l = BloodMoonLayout.of(s[0], s[1], 6, 3);
            Rect moon = l.region(BloodMoonLayout.MOON);
            Rect main = BloodMoonLayout.card(l.region(HubLayout.MAIN));
            Rect side = BloodMoonLayout.card(l.region(HubLayout.SIDE));
            List<Rect> all = new ArrayList<>(List.of(moon, main, side));
            all.addAll(l.tabs());
            all.addAll(l.seals());
            for (int i = 0; i < all.size(); i++) {
                assertTrue(all.get(i).inside(screen), at + " off screen: " + all.get(i));
                for (int j = i + 1; j < all.size(); j++) {
                    assertFalse(all.get(i).overlaps(all.get(j)), at + ": " + all.get(i) + " on " + all.get(j));
                }
            }
            assertTrue(l.region(HubLayout.MAIN).w() >= 100 && l.region(HubLayout.SIDE).w() >= 80, at + " cards too narrow to read");
            assertTrue(l.region(HubLayout.MAIN).h() >= 80, at + " cards too short");
        }
    }

    @Test
    void theRailsStayBelowTheToasts() {
        HubLayout l = BloodMoonLayout.of(320, 240, 6, 3);
        assertTrue(l.tabs().get(0).y() >= GrimoireLayout.TOAST_CLEAR && l.seals().get(0).y() >= GrimoireLayout.TOAST_CLEAR);
        for (int[] s : SCREENS) {
            HubLayout at = BloodMoonLayout.of(s[0], s[1], 6, 3);
            Rect counters = BloodMoonLayout.counters(at.region(BloodMoonLayout.MOON));
            assertFalse(counters.overlaps(new Rect(s[0] - 160, 0, 160, 32)), s[0] + "x" + s[1] + ": the counters are under the toasts");
            assertTrue(counters.inside(at.region(BloodMoonLayout.MOON)));
        }
    }
}
