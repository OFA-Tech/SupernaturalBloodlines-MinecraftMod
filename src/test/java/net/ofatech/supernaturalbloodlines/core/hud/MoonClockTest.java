package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import org.junit.jupiter.api.Test;

class MoonClockTest {
    @Test
    void theShiftComesWhenTheRulesNightOfAFullMoonBegins() {
        assertEquals(MoonRule.NIGHT_START + 1, MoonClock.ticksUntilForcedShift(false, 0, 0));
        assertEquals(2_000, MoonClock.ticksUntilForcedShift(false, 0, MoonRule.NIGHT_START + 1 - 2_000));
        assertEquals(2_000, MoonClock.ticksUntilForcedShift(false, 0, 8L * 24_000 + MoonRule.NIGHT_START + 1 - 2_000));
        assertEquals(0, MoonClock.ticksUntilForcedShift(false, 0, MoonRule.NIGHT_START + 1));
        assertEquals(0, MoonClock.ticksUntilForcedShift(false, 0, MoonRule.NIGHT_END - 1));
    }

    @Test
    void itAgreesWithTheRuleForEveryTickOfTheDay() {
        // the warning is 0 exactly while MoonRule says the night is on: the HUD and the server cannot disagree
        for (long t = 0; t < 24_000; t++) {
            assertEquals(MoonRule.isFullMoonNight(false, 0, t), MoonClock.ticksUntilForcedShift(false, 0, t) == 0, "tick " + t);
        }
    }

    @Test
    void otherPhasesTheMorningAfterAndFixedTimeDimensionsHaveNoShift() {
        assertEquals(-1, MoonClock.ticksUntilForcedShift(false, 0, MoonRule.NIGHT_END));
        for (int phase = 1; phase < 8; phase++) {
            assertEquals(-1, MoonClock.ticksUntilForcedShift(false, phase, 5_000), "phase " + phase);
        }
        assertEquals(-1, MoonClock.ticksUntilForcedShift(true, 0, 5_000), "the Nether and the End report phase 0 forever");
    }

    @Test
    void theClockIsMinutesAndSeconds() {
        assertEquals("1:40", MoonClock.clock(2_000));
        assertEquals("0:05", MoonClock.clock(100));
        assertEquals("0:00", MoonClock.clock(-3));
    }
}
