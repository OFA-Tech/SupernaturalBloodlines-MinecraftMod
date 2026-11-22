package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AugustineBloodRulesTest {
    private static final double FROM_VAMPIRE = 3.0;
    private static final double FROM_OTHER = 1.0 / 3.0;

    @Test
    void aVampireGivesTriple() {
        assertEquals(9, AugustineBloodRules.scale(3, true, FROM_VAMPIRE, FROM_OTHER));
        assertEquals(3, AugustineBloodRules.scale(1, true, FROM_VAMPIRE, FROM_OTHER));
    }

    @Test
    void anyoneElseGivesAThirdButNeverNothingFromABiteThatGaveSomething() {
        assertEquals(1, AugustineBloodRules.scale(3, false, FROM_VAMPIRE, FROM_OTHER));
        assertEquals(1, AugustineBloodRules.scale(1, false, FROM_VAMPIRE, FROM_OTHER));
        assertEquals(3, AugustineBloodRules.scale(10, false, FROM_VAMPIRE, FROM_OTHER));
        assertEquals(2, AugustineBloodRules.scale(5, false, FROM_VAMPIRE, FROM_OTHER));
    }

    @Test
    void nothingStaysNothing() {
        assertEquals(0, AugustineBloodRules.scale(0, true, FROM_VAMPIRE, FROM_OTHER), "a refused bite stays refused");
        assertEquals(0, AugustineBloodRules.scale(0, false, FROM_VAMPIRE, FROM_OTHER));
        assertEquals(0, AugustineBloodRules.scale(-4, true, FROM_VAMPIRE, FROM_OTHER));
    }

    @Test
    void aZeroFactorGivesNothing() {
        assertEquals(0, AugustineBloodRules.scale(5, true, 0, FROM_OTHER));
        assertEquals(0, AugustineBloodRules.scale(5, false, FROM_VAMPIRE, 0));
    }

    @Test
    void aHugeAmountDoesNotOverflow() {
        assertTrue(AugustineBloodRules.scale(Integer.MAX_VALUE, true, 10, FROM_OTHER) > 0);
    }
}
