package net.ofatech.supernaturalbloodlines.core.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ElixirRulesTest {
    @Test
    void itSavesFromEveryDeathButALineDeathAndTheKillCommand() {
        assertTrue(ElixirRules.saves(ElixirRules.Kind.OTHER, false));
        assertFalse(ElixirRules.saves(ElixirRules.Kind.LINE_DEATH, false));
        assertFalse(ElixirRules.saves(ElixirRules.Kind.KILL_COMMAND, false));
    }

    @Test
    void theRingGoesFirst() {
        assertFalse(ElixirRules.saves(ElixirRules.Kind.OTHER, true), "a death the ring saves is left to the ring");
    }

    @Test
    void theHealIsAShareOfTheMaximumNeverZeroAndNeverMore() {
        assertEquals(10F, ElixirRules.healTo(20F, 0.5), 1e-6);
        assertEquals(20F, ElixirRules.healTo(20F, 1.0), 1e-6);
        assertEquals(20F, ElixirRules.healTo(20F, 3.0), 1e-6);
        assertEquals(1F, ElixirRules.healTo(20F, 0.0), 1e-6);
    }
}