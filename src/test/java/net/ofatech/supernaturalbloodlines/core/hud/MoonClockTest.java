package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoonClockTest {
    @Test
    void theShiftComesWhenTheNightOfAFullMoonDayBegins() {
        assertEquals(13_000, MoonClock.ticksUntilForcedShift(0));
        assertEquals(2_000, MoonClock.ticksUntilForcedShift(11_000));
        assertEquals(2_000, MoonClock.ticksUntilForcedShift(8L * 24_000 + 11_000));
        assertEquals(0, MoonClock.ticksUntilForcedShift(13_000));
        assertEquals(0, MoonClock.ticksUntilForcedShift(22_999));
    }

    @Test
    void otherDaysAndTheMorningAfterHaveNoShift() {
        assertEquals(-1, MoonClock.ticksUntilForcedShift(23_000));
        assertEquals(-1, MoonClock.ticksUntilForcedShift(24_000 + 11_000));
        for (int day = 1; day < 8; day++) {
            assertEquals(-1, MoonClock.ticksUntilForcedShift(day * 24_000L + 5_000), "day " + day);
        }
    }

    @Test
    void theClockIsMinutesAndSeconds() {
        assertEquals("1:40", MoonClock.clock(2_000));
        assertEquals("0:05", MoonClock.clock(100));
        assertEquals("0:00", MoonClock.clock(-3));
    }
}
