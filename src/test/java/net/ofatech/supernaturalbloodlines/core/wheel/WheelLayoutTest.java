package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Cell;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Layout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Look;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Radial.Dir;
import org.junit.jupiter.api.Test;

class WheelLayoutTest {
    private static Layout at(int w, int h, int mods, int groups, int entries, int page) {
        return WheelLayout.compute(w, h, 4, Look.GRIMOIRE, mods, 0, groups, entries, page);
    }

    private static Layout open(int entries) {
        return WheelLayout.compute(480, 270, 4, Look.GRIMOIRE, 6, 3, 3, entries, 0);
    }

    private static int cx(Cell c) {
        return c.x() + c.w() / 2;
    }

    private static int cy(Cell c) {
        return c.y() + c.h() / 2;
    }

    private static void near(int x, int y, Cell c, String what) {
        assertTrue(Math.abs(cx(c) - x) <= 1 && Math.abs(cy(c) - y) <= 1, what + ": expected " + x + "," + y + " got " + cx(c) + "," + cy(c));
    }

    @Test
    void sixTwelveAndTwentyFourEntriesMatchTheMockup() {
        // the mockup's own picture (grimoire-1-open, ten powers): the first and last cell of the arc, and the cells at the ends of the inner ring
        List<Cell> ten = open(10).ring(Ring.OUTER);
        near(94, 217, ten.get(0), "10 entries, first");
        near(230, 217, ten.get(9), "10 entries, last");
        near(133, 32, ten.get(4), "10 entries, the lightning bolt at the top"); // bearing 204 + 31.2 * 4.5 = 344.4
        near(191, 32, ten.get(5), "10 entries, the locked heat wave at the top right");
        // six: 52 degrees each from 204: the cell at the bottom left, then up the left side
        List<Cell> six = open(6).ring(Ring.OUTER);
        near(80, 204, six.get(0), "6 entries, first");
        near(244, 204, six.get(5), "6 entries, last"); // bearing 130
        // twelve and 24 entries (a page of 12) have the same 26 degree cells
        List<Cell> twelve = open(12).ring(Ring.OUTER);
        List<Cell> pageOfTwentyFour = open(24).ring(Ring.OUTER);
        assertEquals(12, twelve.size());
        assertEquals(12, pageOfTwentyFour.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(twelve.get(i), pageOfTwentyFour.get(i), "cell " + i);
        }
        near(162 + (int) Math.round(107 * Math.sin(Math.toRadians(217))), 135 - (int) Math.round(107 * Math.cos(Math.toRadians(217))), twelve.get(0), "12 entries, first");
    }

    @Test
    void innerRingAndMiddleRingMatchTheMockup() {
        Layout l = open(10);
        List<Cell> inner = l.ring(Ring.INNER);
        assertEquals(6, inner.size());
        near(145, 105, inner.get(0), "inner, top left"); // the mock's blood icon at 580,420
        near(179, 105, inner.get(1), "inner, top right");
        near(196, 135, inner.get(2), "inner, right");
        // the open mod is the fourth (Tensura, bearing 150): its three labels fan 42 degrees apart round it at radius 69
        List<Cell> middle = l.ring(Ring.MIDDLE);
        assertEquals(3, middle.size());
        near(228, 157, middle.get(0), "Skills");
        near(196, 195, middle.get(1), "Magic");
        near(148, 203, middle.get(2), "Battlewill");
    }

    @Test
    void nothingLeavesTheScreenAt480x270() {
        for (int entries : new int[] {1, 6, 10, 12, 24, 40}) {
            for (int page = 0; page < 4; page++) {
                Layout l = open(entries).onPage(page);
                for (Ring r : Ring.values()) {
                    for (Cell c : l.ring(r)) {
                        assertTrue(c.x() >= 0 && c.y() >= 0 && c.x() + c.w() <= 480 && c.y() + c.h() <= 270, r + " " + c + " with " + entries);
                    }
                }
                for (Cell c : l.pageArrows()) {
                    assertTrue(c.x() >= 0 && c.y() >= 0 && c.x() + c.w() <= 480 && c.y() + c.h() <= 270, "arrow " + c);
                }
                Cell side = l.sidePanel();
                assertTrue(side.x() + side.w() <= 480 && side.y() + side.h() <= 270, "side panel " + side);
            }
        }
    }

    @Test
    void theWheelIsCentredInABiggerGui() {
        Layout big = at(960, 540, 6, 3, 10, 0);
        Layout small = open(10);
        assertEquals(small.ring(Ring.OUTER).get(0).x() + 240, big.ring(Ring.OUTER).get(0).x());
        assertEquals(small.ring(Ring.OUTER).get(0).y() + 135, big.ring(Ring.OUTER).get(0).y());
        assertEquals(small.sidePanel().x() + 240, big.sidePanel().x());
    }

    @Test
    void noCellsOverlap() {
        for (int entries : new int[] {1, 5, 6, 10, 12, 24}) {
            for (int groups : new int[] {0, 3}) {
                Layout l = at(480, 270, 6, groups, entries, 0);
                List<Cell> all = new java.util.ArrayList<>();
                for (Ring r : Ring.values()) {
                    all.addAll(l.ring(r));
                }
                for (int i = 0; i < all.size(); i++) {
                    for (int j = i + 1; j < all.size(); j++) {
                        Cell a = all.get(i);
                        Cell b = all.get(j);
                        int ox = Math.min(a.x() + a.w(), b.x() + b.w()) - Math.max(a.x(), b.x());
                        int oy = Math.min(a.y() + a.h(), b.y() + b.h()) - Math.max(a.y(), b.y());
                        boolean overlap = ox > 0 && oy > 0;
                        // a group label may touch the corner of a ring cell beside it by 2 px (as in the mockup); nothing else may touch
                        boolean label = a.h() == WheelLayout.MIDDLE_H || b.h() == WheelLayout.MIDDLE_H;
                        assertFalse(overlap && !(label && Math.min(ox, oy) <= 2), a + " overlaps " + b + " with " + entries + " entries");
                    }
                }
            }
        }
    }

    @Test
    void middleLabelsOfRealWidthsStayApart() {
        Layout l = open(10).withLabelWidths(new int[] {36, 40, 54});
        List<Cell> middle = l.ring(Ring.MIDDLE);
        assertEquals(54, middle.get(2).w());
        assertEquals(36, middle.get(0).w());
        Cell a = middle.get(1);
        Cell b = middle.get(2);
        assertFalse(a.x() < b.x() + b.w() && b.x() < a.x() + a.w() && a.y() < b.y() + b.h() && b.y() < a.y() + a.h(), a + " overlaps " + b);
        // a width outside 36 to 54 is brought into it
        assertEquals(54, open(10).withLabelWidths(new int[] {99, 99, 99}).ring(Ring.MIDDLE).get(0).w());
        assertEquals(36, open(10).withLabelWidths(new int[] {1, 1, 1}).ring(Ring.MIDDLE).get(0).w());
    }

    @Test
    void hitReturnsTheRingAndCellOrEmpty() {
        Layout l = open(10);
        Cell o = l.ring(Ring.OUTER).get(3);
        assertEquals(Optional.of(new WheelLayout.Hit(Ring.OUTER, 3)), l.hit(cx(o), cy(o)));
        Cell i = l.ring(Ring.INNER).get(2);
        assertEquals(Optional.of(new WheelLayout.Hit(Ring.INNER, 2)), l.hit(i.x(), i.y()));
        assertEquals(Optional.of(new WheelLayout.Hit(Ring.INNER, 2)), l.hit(i.x() + i.w() - 1, i.y() + i.h() - 1));
        assertTrue(l.hit(i.x() + i.w(), i.y() + i.h()).isEmpty() || l.hit(i.x() + i.w(), i.y() + i.h()).get().ring() != Ring.INNER);
        Cell m = l.ring(Ring.MIDDLE).get(1);
        assertEquals(Optional.of(new WheelLayout.Hit(Ring.MIDDLE, 1)), l.hit(cx(m), cy(m)));
        assertTrue(l.hit(162, 135).isEmpty(), "the hub is no cell");
        assertTrue(l.hit(-5, -5).isEmpty());
        assertTrue(l.hit(470, 260).isEmpty());
    }

    @Test
    void oneGroupHasNoMiddleRing() {
        assertTrue(at(480, 270, 6, 0, 10, 0).ring(Ring.MIDDLE).isEmpty());
        assertTrue(at(480, 270, 6, 1, 10, 0).ring(Ring.MIDDLE).isEmpty());
    }

    @Test
    void threeGroupsHaveAMiddleRing() {
        assertEquals(3, at(480, 270, 6, 3, 10, 0).ring(Ring.MIDDLE).size());
        assertEquals(2, at(480, 270, 6, 2, 10, 0).ring(Ring.MIDDLE).size());
    }

    @Test
    void fortyEntriesAreFourPages() {
        int[] sizes = {12, 12, 12, 4};
        for (int p = 0; p < 4; p++) {
            Layout l = at(480, 270, 6, 3, 40, p);
            assertEquals(4, l.pageCount());
            assertEquals(sizes[p], l.ring(Ring.OUTER).size(), "page " + p);
            assertEquals(p, l.page());
            assertEquals(p * 12, l.entryIndex(0));
        }
        // a short last page keeps the 26 degree cells of the others
        assertEquals(at(480, 270, 6, 3, 40, 0).ring(Ring.OUTER).get(2), at(480, 270, 6, 3, 40, 3).ring(Ring.OUTER).get(2));
    }

    @Test
    void exactlyTwelveEntriesIsOnePageAndNoArrows() {
        Layout l = open(12);
        assertEquals(1, l.pageCount());
        assertTrue(l.pageArrows().isEmpty());
        assertEquals(2, open(13).pageCount());
        assertEquals(2, open(13).pageArrows().size());
    }

    @Test
    void pageClampsAtTheFirstAndLastPage() {
        assertEquals(0, at(480, 270, 6, 3, 40, -3).page());
        assertEquals(3, at(480, 270, 6, 3, 40, 99).page());
        assertEquals(0, open(5).onPage(7).page());
    }

    @Test
    void arrowKeysReachEveryCellOfEveryRing() {
        for (int groups : new int[] {0, 2, 3}) {
            Layout l = at(480, 270, 6, groups, 10, 0);
            Set<String> seen = new HashSet<>();
            ArrayDeque<Ring> ringQ = new ArrayDeque<>();
            ArrayDeque<Integer> idxQ = new ArrayDeque<>();
            seen.add("INNER0");
            ringQ.add(Ring.INNER);
            idxQ.add(0);
            while (!ringQ.isEmpty()) {
                Ring r = ringQ.poll();
                int i = idxQ.poll();
                for (Dir d : Dir.values()) {
                    Ring target = l.ringAfter(r, d);
                    int next = l.neighbour(r, i, d);
                    if (next < 0 || target == null) {
                        continue;
                    }
                    if (seen.add(target.name() + next)) {
                        ringQ.add(target);
                        idxQ.add(next);
                    }
                }
            }
            int total = 6 + (groups >= 2 ? groups : 0) + 10;
            assertEquals(total, seen.size(), "reached with " + groups + " groups: " + seen);
        }
    }

    @Test
    void leftPastTheLastOuterCellTurnsThePage() {
        Layout first = at(480, 270, 6, 3, 40, 0);
        Layout middle = at(480, 270, 6, 3, 40, 1);
        Layout last = at(480, 270, 6, 3, 40, 3);
        assertEquals(WheelLayout.TURN_NEXT, middle.neighbour(Ring.OUTER, 11, Dir.RIGHT));
        assertEquals(WheelLayout.TURN_PREVIOUS, middle.neighbour(Ring.OUTER, 0, Dir.LEFT));
        assertEquals(0, first.neighbour(Ring.OUTER, 0, Dir.LEFT), "no page before the first");
        assertEquals(3, last.neighbour(Ring.OUTER, 3, Dir.RIGHT), "no page after the last");
        assertEquals(5, first.neighbour(Ring.OUTER, 4, Dir.RIGHT));
        assertEquals(5, first.neighbour(Ring.INNER, 0, Dir.LEFT), "the inner ring wraps");
    }

    @Test
    void upAndDownStepBetweenRingsToTheNearestCell() {
        Layout l = open(12);
        assertEquals(Ring.MIDDLE, l.ringAfter(Ring.INNER, Dir.UP));
        assertEquals(Ring.OUTER, l.ringAfter(Ring.MIDDLE, Dir.UP));
        assertEquals(Ring.OUTER, l.ringAfter(Ring.OUTER, Dir.UP));
        assertEquals(Ring.INNER, l.ringAfter(Ring.INNER, Dir.DOWN));
        assertEquals(Ring.MIDDLE, l.ringAfter(Ring.OUTER, Dir.DOWN));
        assertEquals(Ring.OUTER, at(480, 270, 6, 0, 12, 0).ringAfter(Ring.INNER, Dir.UP), "no middle ring: straight out");
        // no middle ring: the first inner cell (bearing 330) goes straight to the outer cell nearest that bearing (the 26 degree cells sit at 217 + 26 i)
        int nearer = 0;
        double best = 1e9;
        for (int i = 0; i < 12; i++) {
            double d = Math.abs(((217 + 26 * i) % 360 - 330 + 540) % 360 - 180);
            if (d < best) {
                best = d;
                nearer = i;
            }
        }
        assertEquals(nearer, at(480, 270, 6, 0, 12, 0).neighbour(Ring.INNER, 0, Dir.UP));
        assertEquals(0, l.neighbour(Ring.OUTER, 0, Dir.UP), "the outer ring has nothing further out: it stays");
    }

    @Test
    void emptyListHasNoCells() {
        Layout l = open(0);
        assertTrue(l.ring(Ring.OUTER).isEmpty());
        assertEquals(1, l.pageCount());
        assertTrue(l.pageArrows().isEmpty());
        assertEquals(-1, l.neighbour(Ring.OUTER, 0, Dir.LEFT));
        Layout none = WheelLayout.compute(480, 270, 4, Look.GRIMOIRE, 0, 0, 0, 0, 0);
        assertTrue(none.ring(Ring.INNER).isEmpty());
        assertTrue(none.hit(162, 135).isEmpty());
    }

    @Test
    void fewerThanSixEntriesKeepTheCellsTogetherAndCentred() {
        List<Cell> three = open(3).ring(Ring.OUTER);
        // 52 degrees each, centred in the arc: the middle one is straight up
        near(162, 135 - 107, three.get(1), "the middle of three is at the top");
        assertEquals(1, open(1).ring(Ring.OUTER).size());
        near(162, 135 - 107, open(1).ring(Ring.OUTER).get(0), "a single entry is at the top");
    }

    @Test
    void pickerAndModeListStayOnScreen() {
        Layout l = open(10);
        for (int i = 0; i < 10; i++) {
            Cell p = l.pickerAnchor(i);
            assertEquals(204, p.w());
            assertEquals(64, p.h());
            assertTrue(p.x() >= 0 && p.x() + p.w() <= 480 && p.y() >= 0 && p.y() + p.h() <= 270, "picker " + p);
            Cell m = l.modeListAnchor(i);
            assertEquals(62, m.w());
            assertEquals(46, m.h());
            assertTrue(m.x() >= 0 && m.x() + m.w() <= 480 && m.y() >= 0 && m.y() + m.h() <= 270, "mode list " + m);
        }
        // the mockup: the picker of the top cell sits right under it, the list of a left cell goes to its left and of a right cell to its right
        Cell top = l.ring(Ring.OUTER).get(4);
        assertEquals(top.y() + top.h() + 7, l.pickerAnchor(4).y());
        assertTrue(l.modeListAnchor(0).x() + 62 <= l.ring(Ring.OUTER).get(0).x());
        assertTrue(l.modeListAnchor(9).x() >= l.ring(Ring.OUTER).get(9).x() + 36);
    }

    @Test
    void theTensuraLookHasAHexagonAndTheOthersACircle() {
        assertEquals(6, WheelLayout.compute(480, 270, 4, Look.TENSURA, 6, 3, 3, 10, 0).shape().sides());
        assertEquals(0, WheelLayout.compute(480, 270, 4, Look.GRIMOIRE, 6, 3, 3, 10, 0).shape().sides());
        assertEquals(0, WheelLayout.compute(480, 270, 4, Look.BLOOD_MOON, 6, 3, 3, 10, 0).shape().sides());
    }

    @Test
    void fewerModsSpreadAroundTheInnerRing() {
        List<Cell> three = at(480, 270, 3, 0, 4, 0).ring(Ring.INNER);
        assertEquals(3, three.size());
        // three mods: 120 degrees apart, the first at -60 (the top left)
        near(162 + (int) Math.round(34 * Math.sin(Math.toRadians(-60))), 135 - (int) Math.round(34 * Math.cos(Math.toRadians(-60))), three.get(0), "first of three");
        near(162 + (int) Math.round(34 * Math.sin(Math.toRadians(60))), 135 - (int) Math.round(34 * Math.cos(Math.toRadians(60))), three.get(1), "second of three");
    }
}
