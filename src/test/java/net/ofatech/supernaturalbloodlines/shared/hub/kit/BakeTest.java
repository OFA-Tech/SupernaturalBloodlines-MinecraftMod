package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntBinaryOperator;
import org.junit.jupiter.api.Test;

class BakeTest {
    private static int alpha(int argb) {
        return argb >>> 24;
    }

    private static int lum(int argb) {
        return (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
    }

    @Test
    void theMoonIsPaleInTheMiddleOpaqueInsideAndFadesInTheHalo() {
        int r = 40;
        int size = Bake.moonSize(r);
        IntBinaryOperator m = Bake.moon(r, Bake.STEPS);
        int middle = m.applyAsInt(size / 2, size / 2);
        int rim = m.applyAsInt(size / 2 + r - 2, size / 2);
        assertEquals(255, alpha(middle));
        assertTrue(lum(middle) > lum(rim), "pale in the middle, red at the rim");
        int near = alpha(m.applyAsInt(size / 2 + r + 3, size / 2));
        int far = alpha(m.applyAsInt(size / 2 + r + 20, size / 2));
        assertTrue(near > far && far > 0, "the halo is fainter farther out: " + near + " then " + far);
        assertEquals(0, alpha(m.applyAsInt(0, 0)), "the corner is clear");
    }

    @Test
    void theLitPartRisesWithTheProgress() {
        int r = 40;
        int size = Bake.moonSize(r);
        int bottom = size / 2 + r - 4;
        int top = size / 2 - r + 6;
        // empty (the floor is 10 percent): the top is the dark unlit disc, the bottom is lit
        IntBinaryOperator low = Bake.moon(r, 0);
        IntBinaryOperator full = Bake.moon(r, Bake.STEPS);
        assertTrue(lum(low.applyAsInt(size / 2, top)) < lum(full.applyAsInt(size / 2, top)), "the top is unlit at low progress");
        assertEquals(lum(low.applyAsInt(size / 2, bottom)), lum(full.applyAsInt(size / 2, bottom)), 40);
    }

    @Test
    void theProgressIsAFewDozenStepsSoNearbyValuesShareATexture() {
        assertEquals(Bake.step(0.501f), Bake.step(0.505f));
        assertEquals(0, Bake.step(-1f));
        assertEquals(Bake.STEPS, Bake.step(7f));
    }

    @Test
    void theGlowIsStrongestInTheMiddleAndNothingOutside() {
        IntBinaryOperator g = Bake.glow(100);
        assertTrue(alpha(g.applyAsInt(100, 100)) > alpha(g.applyAsInt(100, 190)));
        assertEquals(0, alpha(g.applyAsInt(0, 0)));
    }

    @Test
    void theGreyMoonIsDimAndRound() {
        IntBinaryOperator m = Bake.greyMoon(30);
        assertEquals(255, alpha(m.applyAsInt(30, 30)));
        assertEquals(0, alpha(m.applyAsInt(0, 0)));
        for (float d = 0; d <= 1f; d += 0.1f) {
            assertTrue(Bake.greyValue(d) / 255.0 >= 0.38 && Bake.greyValue(d) / 255.0 <= 0.56);
        }
    }
}
