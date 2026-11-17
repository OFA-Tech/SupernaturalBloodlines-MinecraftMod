package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules.State;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.FreedKind;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.Kin;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial.Origin;
import org.junit.jupiter.api.Test;

class LineTreeLayoutTest {
    private static List<Kin> kids(int n) {
        List<Kin> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new Kin("K" + i, 1));
        }
        return out;
    }

    private static List<LineTree.Model> models() {
        Origin inLine = new Origin(State.IN_LINE, Optional.of("Klaus"), Optional.empty(), FreedKind.NONE, List.of(false, false));
        Origin free = new Origin(State.FREE, Optional.empty(), Optional.of("Klaus"), FreedKind.BROKEN_LINE, List.of(true, false));
        return List.of(
                LineTree.build(new Origin(State.ORIGINAL, Optional.empty(), Optional.empty(), FreedKind.NONE, List.of()), "Me", Optional.empty(), false, List.of(), kids(3)),
                LineTree.build(inLine, "Me", Optional.of("S"), false, List.of("B", "A"), kids(3)),
                LineTree.build(free, "Me", Optional.of("S"), false, List.of("B", "A"), kids(3)),
                LineTree.build(new Origin(State.SIRE_UNKNOWN, Optional.empty(), Optional.empty(), FreedKind.NONE, List.of()), "Me", Optional.empty(), false, List.of(), List.of()));
    }

    private static final List<Rect> GRIMOIRE_REGIONS = List.of(new Rect(10, 10, 200, 230), new Rect(10, 10, 170, 150), new Rect(0, 0, 230, 260));

    private static void insideAndApart(Rect region, Map<String, Rect> rects, int expected) {
        assertEquals(expected, rects.size());
        List<Rect> all = new ArrayList<>(rects.values());
        for (Rect r : all) {
            assertTrue(r.x() >= region.x() && r.y() >= region.y() && r.right() <= region.right() && r.bottom() <= region.bottom(), r + " is outside " + region);
        }
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                assertFalse(all.get(i).overlaps(all.get(j)), all.get(i) + " overlaps " + all.get(j));
            }
        }
    }

    @Test
    void everyNodeIsInsideItsRegionAndNoTwoOverlap() {
        for (Rect region : GRIMOIRE_REGIONS) {
            for (LineTree.Model m : models()) {
                insideAndApart(region, LineTreeLayout.grimoire(region, m, LineTreeLayout.portraitFor(region, m)), m.nodes().size());
            }
        }
    }

    @Test
    void chainOrderIsTopToBottomAndTheSiredShareARowUnderYou() {
        Rect region = GRIMOIRE_REGIONS.get(0);
        LineTree.Model m = models().get(1);
        Map<String, Rect> r = LineTreeLayout.grimoire(region, m, 14);
        List<String> chain = List.of("line:root", "line:anc:0", "line:anc:1", "line:sire", "line:you");
        for (int i = 0; i + 1 < chain.size(); i++) {
            assertTrue(r.get(chain.get(i)).bottom() <= r.get(chain.get(i + 1)).y(), chain.get(i) + " is not above " + chain.get(i + 1));
        }
        int row = r.get("line:down:0").y();
        for (String kid : List.of("line:down:1", "line:down:2")) {
            assertEquals(row, r.get(kid).y());
        }
        assertTrue(r.get("line:you").bottom() <= row);
    }

    @Test
    void shortPageUsesSmallPortraits() {
        LineTree.Model m = models().get(1);
        assertEquals(14, LineTreeLayout.portraitFor(new Rect(0, 0, 200, 260), m));
        assertTrue(LineTreeLayout.portraitFor(new Rect(0, 0, 200, 150), m) < 14);
    }

    @Test
    void moonNodesAreInsideAndApartAndOffTheBar() {
        Rect region = new Rect(40, 10, 360, 190);
        Rect bar = new Rect(48, 204, 344, 38);
        for (LineTree.Model m : models()) {
            Map<String, Rect> r = LineTreeLayout.moon(region, bar, m);
            for (Rect n : r.values()) {
                assertFalse(n.overlaps(bar), n + " is on the bar");
                assertTrue(n.x() >= region.x() && n.right() <= region.right() && n.y() >= region.y() && n.bottom() <= region.bottom(), n + " outside");
            }
            List<Rect> all = new ArrayList<>(r.values());
            for (int i = 0; i < all.size(); i++) {
                for (int j = i + 1; j < all.size(); j++) {
                    assertFalse(all.get(i).overlaps(all.get(j)));
                }
            }
        }
    }

    @Test
    void rootSitsInsideTheMoonDisc() {
        Rect region = new Rect(40, 10, 360, 190);
        Rect bar = new Rect(48, 204, 344, 38);
        Rect disc = LineTreeLayout.moonDisc(region);
        Rect root = LineTreeLayout.moon(region, bar, models().get(1)).get("line:root");
        assertTrue(root.x() >= disc.x() && root.right() <= disc.right() && root.y() >= disc.y() && root.bottom() <= disc.bottom());
        // an Original is the moon's portrait
        Rect you = LineTreeLayout.moon(region, bar, models().get(0)).get("line:you");
        assertTrue(you.x() >= disc.x() && you.right() <= disc.right() && you.y() >= disc.y() && you.bottom() <= disc.bottom());
    }

    @Test
    void tensuraHexesAreInsideTheStageAndOffThePane() {
        Rect stage = new Rect(100, 30, 250, 200);
        Rect pane = new Rect(358, 30, 126, 200);
        for (LineTree.Model m : models()) {
            Map<String, Rect> r = LineTreeLayout.tensura(stage, pane, m);
            insideAndApart(stage, r, m.nodes().size());
            assertTrue(r.values().stream().noneMatch(n -> n.overlaps(pane)));
        }
        Rect wide = new Rect(100, 30, 420, 230);
        Map<String, Rect> r = LineTreeLayout.tensura(wide, new Rect(528, 30, 150, 230), models().get(1));
        assertTrue(r.values().stream().allMatch(n -> n.right() <= wide.x() + wide.w() * 60 / 100), "the genes' room on the right was taken");
    }
}
