package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GlowShapeTest {
    private static Set<Integer> pixels(List<int[]> rects, int size) {
        Set<Integer> out = new HashSet<>();
        for (int[] r : rects) {
            assertTrue(r[0] >= 0 && r[1] >= 0 && r[0] + r[2] <= size && r[1] + r[3] <= size, "a piece is outside the box");
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    assertTrue(out.add(y * size + x), "a pixel is drawn twice");
                }
            }
        }
        return out;
    }

    @Test
    void aSquareGlowIsTheWholeSquareAndOnePixelAroundIt() {
        assertEquals(16 * 16, pixels(GlowShape.fill(false, 16), 16).size());
        assertEquals(4 * 18 - 4, pixels(GlowShape.outline(false, 18), 18).size());
    }

    @Test
    void aRoundGlowIsADiscWhoseCornersAreEmpty() {
        Set<Integer> disc = pixels(GlowShape.fill(true, 16), 16);
        assertFalse(disc.contains(0), "the corner");
        assertFalse(disc.contains(15), "the corner");
        assertFalse(disc.contains(15 * 16), "the corner");
        assertTrue(disc.contains(8 * 16 + 8) && disc.contains(8), "the middle and the top of the edge");
        double area = Math.PI * 8 * 8;
        assertTrue(Math.abs(disc.size() - area) < area * 0.1, disc.size() + " pixels for a disc of area " + area);
    }

    @Test
    void theRoundLineIsTheEdgeOfTheDiscAndNothingInsideIt() {
        Set<Integer> line = pixels(GlowShape.outline(true, 18), 18);
        Set<Integer> disc = pixels(GlowShape.fill(true, 18), 18);
        assertTrue(disc.containsAll(line), "the line is on the disc");
        assertFalse(line.contains(9 * 18 + 9), "the middle is empty");
        assertTrue(line.contains(9) || line.contains(8), "the top of the circle is there");
        assertTrue(line.size() < disc.size() / 2, "a line, not a body");
        assertFalse(line.contains(0), "and the square's corner is not");
    }
}
