package net.ofatech.supernaturalbloodlines.core.blood;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DailyUsesTest {
    @Test
    void theCountHoldsThroughTheDay() {
        assertEquals(5, DailyUses.usesToday(3, 5, 3 * 24000L));
        assertEquals(5, DailyUses.usesToday(3, 5, 3 * 24000L + 23999));
    }

    @Test
    void theCountIsZeroOnTheNextDay() {
        assertEquals(0, DailyUses.usesToday(3, 5, 4 * 24000L));
        assertEquals(0, DailyUses.usesToday(3, 99, 10 * 24000L));
    }

    @Test
    void aStoredDayInTheFutureIsNotToday() {
        assertEquals(0, DailyUses.usesToday(5, 5, 3 * 24000L));
    }

    @Test
    void theDayStartsAtTickZero() {
        assertEquals(0, DailyUses.dayOf(0));
        assertEquals(0, DailyUses.dayOf(23999));
        assertEquals(1, DailyUses.dayOf(24000));
    }
}
