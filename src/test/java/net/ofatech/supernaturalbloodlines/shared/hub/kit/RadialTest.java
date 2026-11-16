package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RadialTest {
    @Test
    void theRingStartsAtTheTopAndEveryNodeIsOnTheRadius() {
        List<Radial.Node> n = Radial.ring(100, 100, 50, 12);
        assertEquals(new Radial.Node(100, 50), n.get(0));
        for (Radial.Node node : n) {
            double d = Math.hypot(node.x() - 100, node.y() - 100);
            assertTrue(Math.abs(d - 50) <= 1, "off the ring: " + node);
        }
        assertEquals(new Radial.Node(150, 100), n.get(3));
    }

    @Test
    void arrowsWalkAroundTheRing() {
        List<Radial.Node> n = Radial.ring(100, 100, 50, 12);
        assertEquals(1, Radial.neighbour(n, 0, Radial.Dir.RIGHT));
        assertEquals(11, Radial.neighbour(n, 0, Radial.Dir.LEFT));
        int below = Radial.neighbour(n, 3, Radial.Dir.DOWN);
        assertTrue(n.get(below).y() > n.get(3).y(), "down moves down");
        int above = Radial.neighbour(n, 3, Radial.Dir.UP);
        assertTrue(n.get(above).y() < n.get(3).y(), "up moves up");
    }

    @Test
    void anArrowWithNothingThatWayStaysPut() {
        List<Radial.Node> n = List.of(new Radial.Node(10, 10), new Radial.Node(10, 40));
        assertEquals(0, Radial.neighbour(n, 0, Radial.Dir.UP));
        assertEquals(0, Radial.neighbour(n, 0, Radial.Dir.LEFT));
        assertEquals(1, Radial.neighbour(n, 0, Radial.Dir.DOWN));
        assertEquals(0, Radial.neighbour(List.of(new Radial.Node(1, 1)), 0, Radial.Dir.RIGHT));
        assertEquals(-1, Radial.neighbour(List.of(), 0, Radial.Dir.RIGHT));
    }
}
