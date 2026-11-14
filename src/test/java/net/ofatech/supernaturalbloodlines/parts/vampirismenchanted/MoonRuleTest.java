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
    void fullMoonNightNeedsTheNightWindowToo() {
        assertTrue(MoonRule.isFullMoonNight(false, 0, 13000));
        assertTrue(MoonRule.isFullMoonNight(false, 0, 192000L + 18000));
        assertFalse(MoonRule.isFullMoonNight(false, 0, 6000), "noon on the full-moon day");
        assertFalse(MoonRule.isFullMoonNight(false, 0, 0), "the morning of the full-moon day");
        assertFalse(MoonRule.isFullMoonNight(false, 0, MoonRule.NIGHT_START));
        assertFalse(MoonRule.isFullMoonNight(false, 0, MoonRule.NIGHT_END));
        assertFalse(MoonRule.isFullMoonNight(false, 3, 13000), "a night with another phase");
        assertFalse(MoonRule.isFullMoonNight(true, 0, 13000), "a fixed-time dimension");
    }

    @Test
    void fixedTimeDimensionsNeverHaveAFullMoon() {
        assertFalse(MoonRule.isFullMoon(true, 0));
    }
}
