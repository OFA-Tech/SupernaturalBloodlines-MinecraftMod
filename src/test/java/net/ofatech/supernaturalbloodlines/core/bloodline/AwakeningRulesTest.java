package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import org.junit.jupiter.api.Test;

/** R142, row by row. */
class AwakeningRulesTest {
    private static final long DUSK = MoonRule.NIGHT_START;

    @Test
    void zeroNightsIsNow() {
        assertEquals(5000, AwakeningRules.due(5000, 0));
        assertEquals(5000, AwakeningRules.due(5000, -1));
    }

    @Test
    void aKillByDayWaitsForTodaysDusk() {
        assertEquals(DUSK, AwakeningRules.due(1000, 1));
        assertEquals(DUSK, AwakeningRules.due(0, 1));
    }

    @Test
    void aKillAtDuskOrByNightWaitsForTomorrowsDusk() {
        assertEquals(24000 + DUSK, AwakeningRules.due(DUSK, 1), "exactly at dusk is not after it");
        assertEquals(24000 + DUSK, AwakeningRules.due(20000, 1));
        assertEquals(24000 + DUSK, AwakeningRules.due(23999, 1));
    }

    @Test
    void theDayNumberCarriesThrough() {
        assertEquals(24000L * 9 + DUSK, AwakeningRules.due(24000L * 9 + 100, 1));
        assertEquals(24000L * 10 + DUSK, AwakeningRules.due(24000L * 9 + 20000, 1));
    }

    @Test
    void moreNightsAreMoreDays() {
        assertEquals(DUSK + 24000, AwakeningRules.due(1000, 2));
        assertEquals(DUSK + 48000, AwakeningRules.due(1000, 3));
    }

    @Test
    void notDueBeforeItsTimeAndDueAfter() {
        assertFalse(AwakeningRules.isDue(DUSK - 1, DUSK, 1));
        assertTrue(AwakeningRules.isDue(DUSK, DUSK, 1));
        assertTrue(AwakeningRules.isDue(DUSK + 5000, DUSK, 1));
    }

    @Test
    void aClockSetFarBackDoesNotFreezeIt() {
        long due = AwakeningRules.due(24000L * 100, 1);
        assertTrue(AwakeningRules.isDue(0, due, 1));
        assertFalse(AwakeningRules.isDue(24000L * 100, due, 1));
    }

    @Test
    void turningTheSettingOffReleasesItAtOnce() {
        assertTrue(AwakeningRules.isDue(0, 24000L * 3, 0));
    }
}
