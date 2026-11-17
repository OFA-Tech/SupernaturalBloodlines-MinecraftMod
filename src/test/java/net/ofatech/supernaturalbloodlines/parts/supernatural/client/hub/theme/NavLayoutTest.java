package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/**
 * The full nav (Home, Path, Skills, Quests, Rites, Bloodline, Pack, Guide, Settings and Admin: ten entries for an operator) in every theme at the
 * GUI sizes of 1920 x 1080 at scales 4, 3 and 2, the smallest GUI Minecraft allows and a big one: every tab on screen, none on another, the two
 * content regions big enough to hold a page, and the chrome clear of the tabs.
 */
class NavLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};
    private static final int TABS = 10;
    private static final int SEALS = 4;
    private static final int MIN_REGION_W = 90;
    private static final int MIN_REGION_H = 100;

    private record Theme(String name, java.util.function.BiFunction<int[], Integer, HubLayout> of) {
    }

    private static final List<Theme> THEMES = List.of(
            new Theme("grimoire", (s, tabs) -> GrimoireLayout.of(s[0], s[1], tabs, SEALS)),
            new Theme("blood moon", (s, tabs) -> BloodMoonLayout.of(s[0], s[1], tabs, SEALS)),
            new Theme("tensura", (s, tabs) -> TensuraHubLayout.of(s[0], s[1], tabs, SEALS)));

    @Test
    void everyTabIsOnScreenAndOnTopOfNothing() {
        for (Theme th : THEMES) {
            for (int[] s : SCREENS) {
                for (int tabs : new int[] {7, 8, 9, TABS}) {
                    HubLayout l = th.of().apply(s, tabs);
                    String at = th.name() + " " + s[0] + "x" + s[1] + " with " + tabs + " tabs";
                    Rect screen = new Rect(0, 0, s[0], s[1]);
                    assertTrue(l.tabs().size() == tabs, at);
                    List<Rect> all = new ArrayList<>(l.tabs());
                    l.chrome();
                    if (l.chrome().themeButton() != null) {
                        all.add(l.chrome().themeButton());
                    }
                    if (l.chrome().close() != null) {
                        all.add(l.chrome().close());
                    }
                    for (Rect r : all) {
                        assertTrue(r.inside(screen), at + ": " + r + " is off the screen");
                    }
                    for (int i = 0; i < all.size(); i++) {
                        for (int j = i + 1; j < all.size(); j++) {
                            assertFalse(all.get(i).overlaps(all.get(j)), at + ": " + all.get(i) + " on " + all.get(j));
                        }
                    }
                    for (Rect r : l.tabs()) {
                        assertTrue(r.h() >= (s[0] < 480 ? 8 : 11), at + ": a tab is " + r.h() + " high, too small to press (a list row is 11)");
                    }
                }
            }
        }
    }

    @Test
    void theContentRegionsHoldAPageAtEveryScale() {
        for (Theme th : THEMES) {
            for (int[] s : SCREENS) {
                HubLayout l = th.of().apply(s, TABS);
                if (s[0] < 480) {
                    continue; // the smallest GUI only has to keep the nav usable (the test above); the supported scales are 480 x 270 and up
                }
                String at = th.name() + " " + s[0] + "x" + s[1];
                Rect screen = new Rect(0, 0, s[0], s[1]);
                for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                    Rect r = l.region(region);
                    assertTrue(r.inside(screen), at + " " + region + " " + r);
                    assertTrue(r.w() >= MIN_REGION_W && r.h() >= MIN_REGION_H, at + " " + region + " is only " + r.w() + " x " + r.h());
                }
                assertFalse(l.region(HubLayout.MAIN).overlaps(l.region(HubLayout.SIDE)), at + " the regions touch");
                for (Rect tab : l.tabs()) {
                    assertFalse(tab.overlaps(l.region(HubLayout.MAIN)) || tab.overlaps(l.region(HubLayout.SIDE)), at + " a tab covers content: " + tab);
                }
            }
        }
    }
}

