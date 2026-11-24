package net.ofatech.supernaturalbloodlines.core.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class InsanityRulesTest {
    @Test
    void theDelayShortensWithStrengthAndStaysInsideTwoThirdsToFourThirds() {
        Random r = new Random(7);
        for (int s = 0; s <= 2; s++) {
            long base = 30L * 20 / (s + 1);
            for (int i = 0; i < 200; i++) {
                long d = InsanityRules.nextDelay(30, s, r::nextDouble);
                assertTrue(d >= base * 2 / 3 && d <= base * 4 / 3, s + ": " + d);
            }
        }
    }

    @Test
    void theEdgesOfTheRandomAreTheEdgesOfTheWindow() {
        assertEquals(400, InsanityRules.nextDelay(30, 0, () -> 0.0));
        assertEquals(800, InsanityRules.nextDelay(30, 0, () -> 1.0));
        assertEquals(600, InsanityRules.nextDelay(30, 0, () -> 0.5));
        assertEquals(300, InsanityRules.nextDelay(30, 1, () -> 0.5), "one level in: half the delay");
    }

    @Test
    void aZeroIntervalIsAtLeastOneSecond() {
        assertTrue(InsanityRules.nextDelay(0, 0, () -> 0.5) >= 20);
        assertTrue(InsanityRules.nextDelay(1, 2, () -> 0.0) >= 20, "never under a second whatever the level");
    }
}
