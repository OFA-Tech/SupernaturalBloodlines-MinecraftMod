package net.ofatech.supernaturalbloodlines.core;

import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupernaturalTest {
    private static final Predicate<Entity> YES = e -> true;
    private static final Predicate<Entity> NO = e -> false;

    @BeforeEach
    @AfterEach
    void clear() {
        Supernatural.clearForTests();
    }

    @Test
    void nothingRegisteredMeansNothingMatches() {
        assertFalse(Supernatural.is(null));
        assertFalse(Supernatural.isVampiric(null));
        assertFalse(Supernatural.isPrey(null));
    }

    @Test
    void registrationsAreOrCombined() {
        Supernatural.register(NO, NO, NO);
        assertFalse(Supernatural.is(null));
        Supernatural.register(YES, NO, NO);
        Supernatural.register(NO, NO, NO);
        assertTrue(Supernatural.is(null));
        assertFalse(Supernatural.isVampiric(null));
        assertFalse(Supernatural.isPrey(null));
    }

    @Test
    void aNullPredicateIsRefusedAndRegistersNothing() {
        assertThrows(NullPointerException.class, () -> Supernatural.register(YES, YES, null));
        assertFalse(Supernatural.is(null));
    }

    @Test
    void thePredicatesAreIndependent() {
        Supernatural.register(NO, YES, NO);
        assertFalse(Supernatural.is(null));
        assertTrue(Supernatural.isVampiric(null));
        Supernatural.register(NO, NO, YES);
        assertTrue(Supernatural.isPrey(null));
    }
}
