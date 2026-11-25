package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SightRulesTest {
    private static boolean gets(boolean fits, boolean breaker, boolean hasSense, boolean online, boolean sameDim, double distSq) {
        return SightRules.receives(fits, breaker, hasSense, online, sameDim, distSq, 48);
    }

    @Test
    void aSenseHolderGivesToAFittingBrotherWithoutIt() {
        assertTrue(SightRules.gives(true, false, true));
        assertTrue(gets(true, false, false, true, true, 100));
    }

    @Test
    void aBrotherWithTheSenseReceivesNothing() {
        assertFalse(gets(true, false, true, true, true, 100));
    }

    @Test
    void aBreakerGivesAndGetsNothing() {
        assertFalse(SightRules.gives(true, true, true));
        assertFalse(gets(true, true, false, true, true, 100));
    }

    @Test
    void aNonFittingGivesAndGetsNothing() {
        assertFalse(SightRules.gives(false, false, true));
        assertFalse(gets(false, false, false, true, true, 100));
    }

    @Test
    void aGiverWithoutTheSenseGivesNothing() {
        assertFalse(SightRules.gives(true, false, false));
    }

    @Test
    void exactlyAtTheRadiusReceives() {
        assertTrue(gets(true, false, false, true, true, 48.0 * 48.0));
    }

    @Test
    void beyondTheRadiusDoesNot() {
        assertFalse(gets(true, false, false, true, true, 48.0 * 48.0 + 0.001));
    }

    @Test
    void anotherDimensionDoesNot() {
        assertFalse(gets(true, false, false, true, false, 1));
    }

    @Test
    void anOfflineBrotherDoesNot() {
        assertFalse(gets(true, false, false, false, true, 1));
    }

    @Test
    void theRecipientIsNeverInItsOwnIds() {
        assertEquals(List.of(4, 6), SightRules.idsFor(List.of(4, 5, 6), 5));
        assertEquals(List.of(4, 5), SightRules.idsFor(List.of(4, 5), 9));
    }

    @Test
    void emptyIdsStayEmpty() {
        assertEquals(List.of(), SightRules.idsFor(List.of(), 5));
        assertEquals(List.of(), SightRules.idsFor(List.of(5), 5));
    }
}