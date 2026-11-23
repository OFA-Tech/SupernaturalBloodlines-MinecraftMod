package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.wheel.Annulus.Shape;
import net.ofatech.supernaturalbloodlines.core.wheel.Annulus.Span;
import org.junit.jupiter.api.Test;

class AnnulusTest {
    private static Set<Long> pixels(List<Span> spans) {
        Set<Long> out = new HashSet<>();
        for (Span s : spans) {
            for (int x = s.x0(); x <= s.x1(); x++) {
                assertTrue(out.add(((long) s.y() << 32) | (x & 0xFFFFFFFFL)), "pixel twice: " + x + "," + s.y());
            }
        }
        return out;
    }

    private static void agrees(Shape shape, double r0, double r1, double a0, double a1) {
        Set<Long> spans = pixels(Annulus.wedge(shape, r0, r1, a0, a1));
        int r = (int) Math.ceil(r1) + 2;
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                boolean in = Annulus.contains(shape, r0, r1, a0, a1, x, y);
                assertEquals(in, spans.contains(((long) y << 32) | (x & 0xFFFFFFFFL)), shape + " " + r0 + ".." + r1 + " " + a0 + ".." + a1 + " at " + x + "," + y);
            }
        }
    }

    @Test
    void aCircleBandHasTheRightArea() {
        int count = pixels(Annulus.band(Shape.CIRCLE, 52, 86)).size();
        double area = Math.PI * (86 * 86 - 52 * 52);
        assertTrue(Math.abs(count - area) < area * 0.02, count + " vs " + area);
    }

    @Test
    void spansAgreeWithThePointRuleForCircleHexagonAndWedges() {
        for (Shape s : new Shape[] {Shape.CIRCLE, Shape.polygon(6)}) {
            agrees(s, 52, 86, 0, 360);
            agrees(s, 19, 49, 0, 360);
            agrees(s, 52, 86, 330, 330 + 60);
            agrees(s, 88, 126, 100, 160);
            agrees(s, 52, 86, 200, 380);
            agrees(s, 0, 17, 0, 360);
        }
    }

    @Test
    void aHexagonHasAVertexAtTheTop() {
        List<Span> hex = Annulus.band(Shape.polygon(6), 0, 100);
        Span top = hex.get(0);
        assertTrue(top.y() <= -99 && top.x1() - top.x0() < 12, "the top row of the hexagon is a vertex: " + top);
        // and it is widest at 50 above the centre (the vertices at bearing 60 and 120 are at up = 50, x = 87)
        int widest = 0;
        for (Span s : hex) {
            widest = Math.max(widest, s.x1() - s.x0() + 1);
        }
        assertTrue(widest >= 170 && widest <= 176, "widest " + widest);
    }

    @Test
    void aWedgeOfMoreThanHalfARingIsRefusedAndAWholeRingIsNot() {
        assertThrows(IllegalArgumentException.class, () -> Annulus.wedge(Shape.CIRCLE, 10, 20, 0, 200));
        assertTrue(Annulus.wedge(Shape.CIRCLE, 10, 20, 0, 360).size() > 0);
        assertTrue(Annulus.wedge(Shape.CIRCLE, 10, 20, 0, 180).size() > 0);
    }

    @Test
    void anEmptyBandHasNoSpans() {
        assertTrue(Annulus.band(Shape.CIRCLE, 50, 50).isEmpty());
        assertTrue(Annulus.wedge(Shape.CIRCLE, 10, 20, 10, 10).isEmpty());
    }
}
