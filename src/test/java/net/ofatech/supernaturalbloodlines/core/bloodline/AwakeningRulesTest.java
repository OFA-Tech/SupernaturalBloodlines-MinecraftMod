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
        assertFalse(AwakeningRules.isDue(DUSK - 1, DUSK, 1, 0, 0));
        assertTrue(AwakeningRules.isDue(DUSK, DUSK, 1, 0, 0));
        assertTrue(AwakeningRules.isDue(DUSK + 5000, DUSK, 1, 0, 0));
    }

    @Test
    void aClockSetFarBackDoesNotFreezeIt() {
        long due = AwakeningRules.due(24000L * 100, 1);
        assertTrue(AwakeningRules.isDue(0, due, 1, 0, 0));
        assertFalse(AwakeningRules.isDue(24000L * 100, due, 1, 0, 0));
    }

    @Test
    void aFrozenClockStillReleasesItAfterTheNightsOfGameTime() {
        long frozen = 1000; // doDaylightCycle=false: the day time never moves, the dusk is never reached
        long due = AwakeningRules.due(frozen, 1);
        assertFalse(AwakeningRules.isDue(frozen, due, 1, 50_000 + 23_999, 50_000), "a tick short of a day");
        assertTrue(AwakeningRules.isDue(frozen, due, 1, 50_000 + 24_000, 50_000), "a whole day of game time");
        assertFalse(AwakeningRules.isDue(frozen, AwakeningRules.due(frozen, 2), 2, 50_000 + 24_000, 50_000), "two nights need two days");
        assertTrue(AwakeningRules.isDue(frozen, AwakeningRules.due(frozen, 2), 2, 50_000 + 48_000, 50_000), "two days");
    }

    @Test
    void theGameTimeNeverReleasesItBeforeTheDuskOfAMovingClock() {
        for (int nights = 1; nights <= 3; nights++) {
            for (long killedAt = 0; killedAt < 24000; killedAt += 250) {
                long due = AwakeningRules.due(killedAt, nights);
                long wait = due - killedAt;
                assertTrue(wait <= nights * 24000L, "the game-time fallback (" + nights * 24000L + ") would come before the dusk (" + wait + " away) for a kill at " + killedAt);
            }
        }
    }

    @Test
    void thePendingQueueSurvivesItsCodecAndAnOldFieldIsOptional() {
        var codec = AwakeningRules.Pending.CODEC;
        var json = codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, new AwakeningRules.Pending(12786, 777)).getOrThrow();
        assertEquals(new AwakeningRules.Pending(12786, 777), codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
        var bare = new com.google.gson.JsonObject();
        bare.addProperty("due", 5);
        assertEquals(new AwakeningRules.Pending(5, 0), codec.parse(com.mojang.serialization.JsonOps.INSTANCE, bare).getOrThrow());
        assertFalse(AwakeningRules.Pending.NONE.waiting());
        assertTrue(new AwakeningRules.Pending(1, 0).waiting());
    }

    @Test
    void turningTheSettingOffReleasesItAtOnce() {
        assertTrue(AwakeningRules.isDue(0, 1000, 0, 0, 0), "a due time just ahead, with the setting at 0");
        assertFalse(AwakeningRules.isDue(0, 1000, 1, 0, 0), "the same with one night: not yet");
    }
}
