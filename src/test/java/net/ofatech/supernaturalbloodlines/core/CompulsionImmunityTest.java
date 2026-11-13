package net.ofatech.supernaturalbloodlines.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompulsionImmunityTest {
    @BeforeEach
    @AfterEach
    void clear() {
        CompulsionImmunity.clearForTests();
    }

    @Test
    void nothingRegisteredMeansNoOneIsImmune() {
        assertFalse(CompulsionImmunity.isImmune(null));
    }

    @Test
    void registrationsAreOrCombined() {
        CompulsionImmunity.register(e -> false);
        assertFalse(CompulsionImmunity.isImmune(null));
        CompulsionImmunity.register(e -> true);
        assertTrue(CompulsionImmunity.isImmune(null));
    }

    @Test
    void aNullPredicateIsRefused() {
        assertThrows(NullPointerException.class, () -> CompulsionImmunity.register(null));
    }
}
