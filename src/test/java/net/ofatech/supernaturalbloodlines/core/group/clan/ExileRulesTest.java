package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExileRulesTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    void banArithmetic() {
        assertEquals(1_000 + 7 * DAY, ExileRules.until(1_000, 7));
        assertEquals(1_000 + DAY, ExileRules.until(1_000, 1));
    }

    @Test
    void theBanEndsExactlyAtUntil() {
        long until = ExileRules.until(0, 7);
        assertTrue(ExileRules.bars(until, until - 1));
        assertFalse(ExileRules.bars(until, until));
        assertFalse(ExileRules.bars(until, until + 1));
    }

    @Test
    void noBanRecordedNeverBarsAtAnyTime() {
        for (long now : new long[] {0, 5, 7 * DAY, Long.MAX_VALUE}) {
            assertFalse(ExileRules.bars(0, now), "until 0 means none; now " + now);
        }
        assertTrue(ExileRules.bars(10, 5));
        assertTrue(ExileRules.bars(Long.MAX_VALUE, Long.MAX_VALUE - 1));
    }

    @Test
    void theWordsSayTheDaysAndTheEndInUtc() {
        assertEquals("7 days, until 1970-01-08 00:00 UTC", ExileRules.words(7, 7 * DAY));
        assertEquals("1 day, until 1970-01-02 00:00 UTC", ExileRules.words(1, DAY));
    }
}
