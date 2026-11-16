package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes.Run;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes.Shape;
import org.junit.jupiter.api.Test;

/** A slot's shape costs as few fills as it can, and is worked out once: the HUD draws six slots with three shapes each, every frame. */
class ShapesCacheTest {
    @Test
    void aSquareIsOneFill() {
        Run[] runs = Shapes.runs(Shape.SQUARE, 22);
        assertEquals(1, runs.length);
        assertEquals(new Run(0, 0, 22, 22), runs[0]);
    }

    @Test
    void theSameShapeAndSizeIsTheSameInstance() {
        assertSame(Shapes.spans(Shape.ROUND, 22), Shapes.spans(Shape.ROUND, 22));
        assertSame(Shapes.runs(Shape.HEX, 22), Shapes.runs(Shape.HEX, 22));
        assertFalse(Shapes.runs(Shape.HEX, 22) == Shapes.runs(Shape.HEX, 20));
    }

    @Test
    void theRunsCoverExactlyTheSpans() {
        for (Shape s : Shape.values()) {
            for (int size : new int[] {8, 20, 22, 31}) {
                int fromRuns = 0;
                for (Run r : Shapes.runs(s, size)) {
                    fromRuns += r.w() * r.h();
                }
                int fromSpans = 0;
                for (Shapes.Span sp : Shapes.spans(s, size)) {
                    fromSpans += sp.w();
                }
                assertEquals(fromSpans, fromRuns, s + " " + size);
            }
        }
    }

    @Test
    void aHexagonIsFewerFillsThanItHasRows() {
        // the sloping rows differ by a pixel, but the straight middle is one fill
        assertTrue(Shapes.runs(Shape.HEX, 22).length < 22);
    }

    @Test
    void containsFollowsTheSpans() {
        assertTrue(Shapes.contains(Shape.ROUND, 22, 11, 11));
        assertFalse(Shapes.contains(Shape.ROUND, 22, 0, 0));
        assertFalse(Shapes.contains(Shape.SQUARE, 22, 22, 5));
    }
}
