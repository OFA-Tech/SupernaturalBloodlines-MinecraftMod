package net.ofatech.supernaturalbloodlines.core.group.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PotencyTest {
    @Test
    void theTable() {
        int[] n = {1, 2, 3, 4, 5, 7, 10, 13};
        double[] want = {1.000, 1.200, 1.360, 1.488, 1.590, 1.738, 1.866, 1.931};
        for (int i = 0; i < n.length; i++) {
            assertEquals(want[i], Potency.of(n[i], 1.0, 0.8), 0.0006, "n=" + n[i]);
        }
    }

    @Test
    void strictlyIncreasingWithShrinkingSteps() {
        double previousStep = Double.MAX_VALUE;
        for (int n = 2; n <= 13; n++) {
            double step = Potency.of(n, 1.0, 0.8) - Potency.of(n - 1, 1.0, 0.8);
            assertTrue(step > 0 && step < previousStep, "n=" + n);
            previousStep = step;
        }
    }

    @Test
    void nBelowOneIsOne() {
        assertEquals(1.0, Potency.of(0, 1.0, 0.8));
        assertEquals(1.0, Potency.of(-4, 1.0, 0.8));
    }
}