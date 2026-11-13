package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.MoonRule;
import org.junit.jupiter.api.Test;

class MoonRuleTest {
    @Test
    void fullMoonOnlyInPhaseZero() {
        assertTrue(MoonRule.isFullMoon(false, 0));
        for (int phase = 1; phase < 8; phase++) {
            assertFalse(MoonRule.isFullMoon(false, phase), "phase " + phase);
        }
    }

    @Test
    void fixedTimeDimensionsNeverHaveAFullMoon() {
        assertFalse(MoonRule.isFullMoon(true, 0));
    }
}
