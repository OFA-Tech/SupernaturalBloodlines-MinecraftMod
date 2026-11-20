package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesiccationRulesTest {
    @Test
    void aDayIsTwentyFourThousandTicksAndNegativeTimeFloors() {
        assertEquals(0, DesiccationRules.dayOf(23999));
        assertEquals(1, DesiccationRules.dayOf(24000));
        assertEquals(-1, DesiccationRules.dayOf(-1));
    }

    @Test
    void theReturnDayIsTodayPlusTheDaysAndAtLeastOne() {
        assertEquals(15, DesiccationRules.returnDay(10, 5));
        assertEquals(11, DesiccationRules.returnDay(10, 0));
    }

    @Test
    void dueOnTheReturnDayNotBefore() {
        assertFalse(DesiccationRules.due(14, 15));
        assertTrue(DesiccationRules.due(15, 15));
        assertTrue(DesiccationRules.due(20, 15));
    }

    @Test
    void daysLeftNeverGoesNegative() {
        assertEquals(3, DesiccationRules.daysLeft(12, 15));
        assertEquals(0, DesiccationRules.daysLeft(20, 15));
    }
}
