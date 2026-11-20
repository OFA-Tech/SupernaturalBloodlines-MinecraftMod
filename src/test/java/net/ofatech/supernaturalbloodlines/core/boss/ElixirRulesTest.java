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
        assertFalse(ElixirRules.saves(ElixirRules.Kind.TURNING, false), "a death that is part of becoming something is not undone");
        assertFalse(ElixirRules.saves(ElixirRules.Kind.VOID, false), "out of the world, it would only drop them again");
    }

    @Test
    void aDeathIsClassifiedByTheFirstThingThatAppliesInOrder() {
        // lineDeath, killCommand, originalEnd, turning, outOfWorld
        assertEquals(ElixirRules.Kind.OTHER, ElixirRules.classify(false, false, false, false, false));
        assertEquals(ElixirRules.Kind.LINE_DEATH, ElixirRules.classify(true, false, false, false, false));
        assertEquals(ElixirRules.Kind.LINE_DEATH, ElixirRules.classify(false, false, true, false, false), "the white oak end of an Original");
        assertEquals(ElixirRules.Kind.KILL_COMMAND, ElixirRules.classify(false, true, false, false, false));
        assertEquals(ElixirRules.Kind.TURNING, ElixirRules.classify(false, false, false, true, false));
        assertEquals(ElixirRules.Kind.VOID, ElixirRules.classify(false, false, false, false, true));
        assertEquals(ElixirRules.Kind.LINE_DEATH, ElixirRules.classify(true, true, true, true, true), "a line death outranks everything");
        assertEquals(ElixirRules.Kind.KILL_COMMAND, ElixirRules.classify(false, true, false, true, true), "the kill command outranks turning and the void");
        assertEquals(ElixirRules.Kind.TURNING, ElixirRules.classify(false, false, false, true, true), "turning outranks the void");
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