package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.RingLabels.Anchor;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.RingLabels.Request;
import org.junit.jupiter.api.Test;

class RingLabelsTest {
    private static final Rect PAGE = new Rect(0, 0, 400, 300);

    @Test
    void aNameWithNothingInItsWayIsPlacedWhereItWasAskedFor() {
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 40, 9, Anchor.LEFT)), List.of(), PAGE, 12, 2);
        assertEquals(new Rect(100, 50, 40, 9), out.get(0));
    }

    @Test
    void twoNeighboursThatWouldRunTogetherAreCutAndNeverTouch() {
        // two names under two faces 16 px apart, each 50 wide: "Lydia" and "Stiles" ran into "LydiaStiles"
        List<Rect> out = RingLabels.place(List.of(new Request(100, 120, 50, 9, Anchor.CENTER), new Request(116, 120, 50, 9, Anchor.CENTER)), List.of(), PAGE, 12, 2);
        assertNotNull(out.get(0));
        assertNotNull(out.get(1), "the second is cut, not lost, while 12 px fit");
        assertFalse(out.get(0).inset(-1).overlaps(out.get(1)), out.get(0) + " and " + out.get(1));
        assertTrue(out.get(1).w() < 50 || out.get(0).w() < 50, "one of them had to give way");
    }

    @Test
    void aNameThatWouldCoverAFaceOrTheLineUnderTheModelStopsShort() {
        Rect face = new Rect(120, 48, 20, 20);
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 60, 9, Anchor.LEFT)), List.of(face), PAGE, 8, 2);
        assertNotNull(out.get(0));
        assertTrue(out.get(0).right() <= face.x(), "it ends before the face: " + out.get(0));
    }

    @Test
    void aNameThatCannotFitEvenAtTheMinimumIsLeftOutForTheTooltip() {
        Rect wall = new Rect(98, 40, 60, 40);
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 60, 9, Anchor.LEFT)), List.of(wall), PAGE, 12, 2);
        assertNull(out.get(0));
    }

    @Test
    void aNameAtThePageEdgeSlidesInsideItAndANameNeverLeavesThePage() {
        List<Rect> out = RingLabels.place(List.of(new Request(395, 50, 40, 9, Anchor.LEFT), new Request(2, 80, 40, 9, Anchor.RIGHT)), List.of(), PAGE, 12, 2);
        for (Rect r : out) {
            assertNotNull(r);
            assertTrue(r.inside(PAGE), r.toString());
        }
    }

    @Test
    void everyPlacedNameClearsEveryOtherWhateverTheOrderOrTheSpacing() {
        for (int step = 6; step <= 40; step += 2) {
            List<Request> reqs = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) {
                reqs.add(new Request(60 + i * step, 100, 44, 9, i % 2 == 0 ? Anchor.CENTER : Anchor.LEFT));
            }
            List<Rect> out = RingLabels.place(reqs, List.of(), PAGE, 10, 2);
            for (int i = 0; i < out.size(); i++) {
                for (int j = i + 1; j < out.size(); j++) {
                    if (out.get(i) != null && out.get(j) != null) {
                        assertFalse(out.get(i).overlaps(out.get(j)), "step " + step + ": " + out.get(i) + " and " + out.get(j));
                    }
                }
            }
        }
    }

    @Test
    void theNamesOfASkillRingNeverCoverANodeTheModelOrEachOtherAtAnyRingSize() {
        for (int radius = 40; radius <= 130; radius += 10) {
            for (int count = 4; count <= 12; count++) {
                int size = NodeSize.of(radius, count);
                int cx = 200;
                int cy = 150;
                List<Radial.Node> ring = Radial.ring(cx, cy, radius, count);
                List<Request> reqs = new java.util.ArrayList<>();
                List<Rect> blocked = new java.util.ArrayList<>();
                blocked.add(new Rect(cx - radius * 9 / 20 - 2, cy - radius * 9 / 20 - 2, radius * 9 / 10 + 4, radius * 9 / 10 + 4));
                for (Radial.Node n : ring) {
                    blocked.add(new Rect(n.x() - size / 2 - 2, n.y() - size / 2 - 2, size + 4, size + 4));
                }
                for (Radial.Node n : ring) {
                    int dx = n.x() - cx;
                    boolean beside = Math.abs(dx) * 2 > radius;
                    int w = 66;
                    int h = 18;
                    int y = beside ? n.y() - h / 2 : n.y() >= cy ? n.y() + size / 2 + 2 : n.y() - size / 2 - 2 - h;
                    reqs.add(beside ? new Request(dx > 0 ? n.x() + size / 2 + 3 : n.x() - size / 2 - 3, y, w, h, dx > 0 ? Anchor.LEFT : Anchor.RIGHT) : new Request(n.x(), y, w, h, Anchor.CENTER));
                }
                List<Rect> out = RingLabels.place(reqs, blocked, new Rect(0, 0, 400, 300), 24, 4);
                for (int i = 0; i < out.size(); i++) {
                    Rect a = out.get(i);
                    if (a == null) {
                        continue;
                    }
                    for (Rect b : blocked) {
                        assertFalse(a.overlaps(b), "radius " + radius + " count " + count + ": name " + i + " " + a + " covers " + b);
                    }
                    for (int j = i + 1; j < out.size(); j++) {
                        if (out.get(j) != null) {
                            assertFalse(a.overlaps(out.get(j)), "radius " + radius + " count " + count + ": names " + i + " and " + j + " overlap");
                        }
                    }
                }
            }
        }
    }
    @Test
    void aNameUnderAFaceSlidesToTheFreeSideBeforeItIsCut() {
        // "Lydia" under a face 30 px from "Stiles's": both fit at their full width when one stands to the side of its face
        List<Rect> out = RingLabels.place(List.of(new Request(100, 120, 50, 9, Anchor.CENTER), new Request(130, 120, 50, 9, Anchor.CENTER)), List.of(), PAGE, 12, 2);
        assertNotNull(out.get(0));
        assertNotNull(out.get(1));
        assertEquals(50, out.get(0).w());
        assertEquals(50, out.get(1).w(), "it slid, it was not cut: " + out.get(1));
        assertFalse(out.get(0).inset(-1).overlaps(out.get(1)), out.get(0) + " and " + out.get(1));
    }
    @Test
    void aTwoLineNameThatDoesNotFitAboveTheBottomEdgeIsKeptAsOneLineBeforeItIsDropped() {
        // a name under the lowest node: its second line would stand below the page
        List<Rect> out = RingLabels.place(List.of(new Request(100, 285, 50, 18, Anchor.CENTER)), List.of(), PAGE, 12, 2);
        assertNotNull(out.get(0), "the name is not lost");
        assertEquals(9, out.get(0).h(), "one line");
        assertTrue(out.get(0).inside(PAGE), out.get(0).toString());
    }

    @Test
    void aShortNameThatFitsWholeIsKeptEvenWhenItIsNarrowerThanTheMinimumForACutName() {
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 20, 9, Anchor.LEFT)), List.of(), PAGE, 22, 2);
        assertNotNull(out.get(0), "Liam fits in 20 px: the 22 px minimum is for names that are cut");
        assertEquals(20, out.get(0).w());
    }

    @Test
    void aNameThatHasToBeCutStillStopsAtTheMinimum() {
        Rect wall = new Rect(110, 40, 80, 40);
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 60, 9, Anchor.LEFT)), List.of(wall), PAGE, 22, 2);
        assertNull(out.get(0), "only 10 px are free: under the 22 px minimum it is left to the tooltip");
    }

    @Test
    void aNameIsNotMovedOrShortenedWhenItFitsWhereItWasAsked() {
        List<Rect> out = RingLabels.place(List.of(new Request(100, 50, 50, 18, Anchor.CENTER)), List.of(), PAGE, 12, 2);
        assertEquals(18, out.get(0).h());
        assertEquals(50, out.get(0).w());
    }}
