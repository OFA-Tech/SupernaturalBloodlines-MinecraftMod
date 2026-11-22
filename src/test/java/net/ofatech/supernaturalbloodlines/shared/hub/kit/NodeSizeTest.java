package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NodeSizeTest {
    @Test
    void aRoomyRingGetsTheKitsNodeAndACrowdedOneKeepsTheSmallShape() {
        assertEquals(NodeSize.BIG, NodeSize.of(90, 6));
        assertEquals(NodeSize.SMALL, NodeSize.of(90, 20), "20 nodes round 90 px are 28 px apart");
        assertEquals(NodeSize.BIG, NodeSize.of(60, 1));
    }

    @Test
    void aSmallRingKeepsTheSmallShapeWhateverTheCount() {
        assertEquals(NodeSize.SMALL, NodeSize.of(NodeSize.MIN_RADIUS - 1, 2));
        assertEquals(NodeSize.SMALL, NodeSize.of(26, 1));
    }

    @Test
    void aBigNodeAlwaysLeavesRoomForTheNamesBetweenNeighbours() {
        for (int radius = 20; radius <= 200; radius += 5) {
            for (int count = 1; count <= 24; count++) {
                if (NodeSize.of(radius, count) == NodeSize.BIG && count > 1) {
                    double chord = 2 * radius * Math.sin(Math.PI / count);
                    assertTrue(chord >= NodeSize.BIG + NodeSize.NAME_ROOM, "radius " + radius + " count " + count + ": chord " + chord);
                }
            }
        }
    }
}
