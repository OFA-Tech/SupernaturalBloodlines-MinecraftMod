package net.ofatech.supernaturalbloodlines.core.moon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
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
    void anyNightIsTheNightWindowInADimensionWithADay() {
        assertTrue(MoonRule.isNight(false, 13000));
        assertTrue(MoonRule.isNight(false, 24000L * 3 + 18000));
        assertFalse(MoonRule.isNight(false, 6000), "noon");
        assertFalse(MoonRule.isNight(false, MoonRule.NIGHT_START));
        assertFalse(MoonRule.isNight(false, MoonRule.NIGHT_END));
        assertFalse(MoonRule.isNight(true, 13000), "a fixed-time dimension has no night");
    }

    @Test
    void aRiteThatDoesNotNeedTheFullMoonStillNeedsANight() {
        assertTrue(MoonRule.nightFor(true, false, 0, 13000));
        assertFalse(MoonRule.nightFor(true, false, 3, 13000), "the full moon asked for, another phase");
        assertTrue(MoonRule.nightFor(false, false, 3, 13000), "no moon asked for: a night of any phase");
        assertFalse(MoonRule.nightFor(false, false, 3, 6000), "no moon asked for is not no night asked for");
        assertFalse(MoonRule.nightFor(false, false, 0, 6000), "noon on the full-moon day");
        assertFalse(MoonRule.nightFor(false, true, 3, 13000));
    }

    @Test
    void fixedTimeDimensionsNeverHaveAFullMoon() {
        assertFalse(MoonRule.isFullMoon(true, 0));
    }
}
