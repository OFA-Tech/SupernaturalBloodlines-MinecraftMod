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
    void aBanIsPerScopeNotPerPlayer() {
        assertFalse(ExileRules.bars(0, 5), "no ban recorded (until 0) never bars");
        assertTrue(ExileRules.bars(10, 5));
    }
}
