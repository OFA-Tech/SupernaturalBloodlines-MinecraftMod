package net.ofatech.supernaturalbloodlines.core.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SusceptibilityRulesTest {
    @Test
    void theMultiplierRisesPerLevelAndIsCapped() {
        assertEquals(1.25, SusceptibilityRules.multiplier(0, 25, 200), 1e-9);
        assertEquals(1.50, SusceptibilityRules.multiplier(1, 25, 200), 1e-9);
        assertEquals(1.75, SusceptibilityRules.multiplier(2, 25, 200), 1e-9);
        assertEquals(2.00, SusceptibilityRules.multiplier(2, 100, 200), 1e-9, "capped at x2");
        assertEquals(1.0, SusceptibilityRules.multiplier(0, 0, 200), 1e-9);
        assertEquals(1.0, SusceptibilityRules.multiplier(2, 25, 100), 1e-9, "a cap of 100 is no extra damage");
    }

    @Test
    void aCapBelowOneHundredNeverLowersDamage() {
        assertEquals(1.0, SusceptibilityRules.multiplier(2, 25, 50), 1e-9);
        assertEquals(1.0, SusceptibilityRules.multiplier(0, -30, 200), 1e-9);
    }

    @Test
    void onlyARealBlowIsRaised() {
        assertEquals(12.5f, SusceptibilityRules.apply(10f, 1.25, false, false), 1e-6f);
        assertEquals(10f, SusceptibilityRules.apply(10f, 1.25, true, false), "bypasses invulnerability: /kill, void");
        assertEquals(10f, SusceptibilityRules.apply(10f, 1.25, false, true), "a probe");
        assertEquals(0f, SusceptibilityRules.apply(0f, 1.25, false, false));
        assertEquals(-1f, SusceptibilityRules.apply(-1f, 1.25, false, false));
    }
}
