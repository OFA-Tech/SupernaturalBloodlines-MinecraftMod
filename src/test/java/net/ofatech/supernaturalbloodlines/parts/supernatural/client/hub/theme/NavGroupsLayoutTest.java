package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme;

import static net.ofatech.supernaturalbloodlines.shared.hub.HubPage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.NavGroups;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.junit.jupiter.api.Test;

/** The nav with its group gaps and headings, for the pages an operator, a player and a Ghoul see, at 1920 x 1080 scales 4, 3, 2, the smallest GUI and a big one. */
class NavGroupsLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {480, 270}, {640, 360}, {960, 540}};
    private static final List<List<HubPage>> SETS = List.of(
            List.of(HOME, PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK, GUIDE, SETTINGS, ADMIN),
            List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, GUIDE, SETTINGS),
            List.of(HOME, PATH, GUIDE, SETTINGS));

    private static List<HubLayout> layouts(int[] s, List<HubPage> pages) {
        return List.of(GrimoireLayout.of(s[0], s[1], pages, 4), BloodMoonLayout.of(s[0], s[1], pages, 4), TensuraHubLayout.of(s[0], s[1], pages, 4));
    }

    @Test
    void everyTabHeadingAndLauncherIsOnScreenAndOnTopOfNothing() {
        for (int[] s : SCREENS) {
            for (List<HubPage> pages : SETS) {
                int k = 0;
                for (HubLayout l : layouts(s, pages)) {
                    String at = "theme " + k++ + " " + s[0] + "x" + s[1] + " " + pages.size() + " pages";
                    Rect screen = new Rect(0, 0, s[0], s[1]);
                    assertEquals(pages.size(), l.tabs().size(), at);
                    List<Rect> all = new ArrayList<>(l.tabs());
                    all.addAll(l.headings());
                    if (s[0] >= 480) { // the smallest GUI only has to keep the nav usable; the launchers need 480 x 270
                        all.addAll(l.seals());
                    }
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
                        assertTrue(r.h() >= (s[0] < 480 ? 8 : 11), at + ": a tab is " + r.h() + " high");
                        assertFalse(r.overlaps(l.region(HubLayout.MAIN)) || r.overlaps(l.region(HubLayout.SIDE)), at + ": a tab covers content");
                    }
                }
            }
        }
    }

    @Test
    void tabsKeepTheirOrderAndAGroupChangeLeavesRoom() {
        for (int[] s : SCREENS) {
            List<HubPage> pages = SETS.get(0);
            int[] gaps = NavGroups.gaps(pages);
            for (HubLayout l : layouts(s, pages)) {
                for (int i = 1; i < pages.size(); i++) {
                    Rect a = l.tabs().get(i - 1);
                    Rect b = l.tabs().get(i);
                    boolean column = l.tabs().get(0).x() == l.tabs().get(1).x();
                    int between = column ? b.y() - a.bottom() : 0;
                    assertTrue(b.y() >= a.bottom(), s[0] + "x" + s[1] + ": tab " + i + " is not below tab " + (i - 1));
                    if (gaps[i] > 0) {
                        int plain = i > 1 && gaps[i - 1] == 0 ? l.tabs().get(i - 1).y() - l.tabs().get(i - 2).bottom() : 0;
                        assertTrue(between > plain, s[0] + "x" + s[1] + ": no gap before " + pages.get(i));
                    }
                }
            }
        }
    }

    @Test
    void tensuraHeadingsAreOneForEachGroupAndSitOverTheirFirstRow() {
        HubLayout l = TensuraHubLayout.of(960, 540, SETS.get(0), 4);
        var heads = NavGroups.headings(SETS.get(0));
        assertEquals(heads.size(), l.headings().size());
        for (int i = 0; i < heads.size(); i++) {
            Rect h = l.headings().get(i);
            Rect row = l.tabs().get(heads.get(i).tab());
            assertTrue(h.bottom() <= row.y() && row.y() - h.bottom() <= 2, "heading " + heads.get(i).group() + " is not over its row");
        }
    }

    @Test
    void whenHeadingsWouldSqueezeTheRowsTensuraKeepsPlainGaps() {
        HubLayout l = TensuraHubLayout.of(320, 240, SETS.get(0), 4);
        assertTrue(l.headings().isEmpty());
        for (Rect r : l.tabs()) {
            assertTrue(r.h() >= 11, "a row is " + r.h());
        }
    }
}

