package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShapesTest {
    @Test
    void everyShapeIsSymmetricAndInsideItsBox() {
        for (Shapes.Shape shape : Shapes.Shape.values()) {
            for (int size : new int[] {8, 14, 16, 22, 32}) {
                Shapes.Span[] rows = Shapes.spans(shape, size);
                assertEquals(size, rows.length);
                for (int y = 0; y < size; y++) {
                    assertTrue(rows[y].x() >= 0 && rows[y].x() + rows[y].w() <= size, shape + " " + size + " row " + y);
                    assertEquals(rows[y].w(), rows[size - 1 - y].w(), shape + " " + size + " is not symmetric at row " + y);
                    assertEquals((size - rows[y].w()) / 2, rows[y].x(), "centred " + shape + " " + size + " row " + y);
                }
            }
        }
    }

    @Test
    void theHexagonHasAPointAndAFullMiddle() {
        Shapes.Span[] hex = Shapes.spans(Shapes.Shape.HEX, 16);
        assertTrue(hex[0].w() <= 4, "the apex is narrow");
        assertEquals(16, hex[8].w());
        assertEquals(16, hex[5].w(), "full width from a quarter down");
        for (int y = 1; y < 4; y++) {
            assertTrue(hex[y].w() >= hex[y - 1].w(), "widens toward the middle");
        }
    }

    @Test
    void theRoundShapeIsNarrowAtTheTopAndWideInTheMiddle() {
        Shapes.Span[] round = Shapes.spans(Shapes.Shape.ROUND, 16);
        assertTrue(round[0].w() < round[3].w() && round[3].w() < round[7].w());
        assertEquals(16, Shapes.spans(Shapes.Shape.SQUARE, 16)[0].w());
    }
}
