package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The ring of blood when a pack changed the strip's size: the same share as an outline round the slot. */
class FrameOutlineTest {
    private static Set<Integer> pixels(List<int[]> segments, int w) {
        Set<Integer> out = new HashSet<>();
        for (int[] r : segments) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    out.add(y * w + x);
                }
            }
        }
        return out;
    }

    @Test
    void noBloodIsNoSegmentAndAFullRingIsEveryPixelOfTheOutlineOnce() {
        assertTrue(FrameStrip.outlineSegments(28, 28, 0f).isEmpty());
        List<int[]> all = FrameStrip.outlineSegments(28, 28, 1f);
        int ring = 2 * 28 + 2 * 28 - 4;
        assertEquals(ring, pixels(all, 28).size(), "every pixel of the outline is lit");
        int area = 0;
        for (int[] r : all) {
            area += r[2] * r[3];
        }
        assertEquals(ring, area, "and none is lit twice");
    }

    @Test
    void theRingFillsClockwiseFromTheTopMiddleAndGrowsWithTheShare() {
        int total = 2 * 28 + 2 * 28 - 4;
        int before = 0;
        for (float share = 0.1f; share <= 1f; share += 0.1f) {
            int lit = pixels(FrameStrip.outlineSegments(28, 28, share), 28).size();
            assertTrue(lit >= before, "it never shrinks as the share grows");
            before = lit;
        }
        // a sixteenth is the right half of the top edge (14 of its pixels, from the middle to the corner)
        Set<Integer> start = pixels(FrameStrip.outlineSegments(28, 28, 10f / total), 28);
        for (int x = 14; x < 24; x++) {
            assertTrue(start.contains(x), "the top edge from its middle, pixel " + x);
        }
        assertTrue(!start.contains(13), "and not left of the middle");
        // a quarter has gone down the right edge: its last lit pixel is on the right edge
        Set<Integer> quarter = pixels(FrameStrip.outlineSegments(28, 28, 0.25f), 28);
        assertTrue(quarter.contains(10 * 28 + 27), "the right edge has been reached");
        assertTrue(!quarter.contains(27 * 28 + 10), "the bottom edge has not");
    }

    @Test
    void aRingOfBloodStaysInsideItsBox() {
        for (float share : new float[] {0.2f, 0.5f, 0.9f, 1f}) {
            for (int[] r : FrameStrip.outlineSegments(28, 28, share)) {
                assertTrue(r[0] >= 0 && r[1] >= 0 && r[0] + r[2] <= 28 && r[1] + r[3] <= 28, "a piece is outside the box");
            }
        }
    }
}
