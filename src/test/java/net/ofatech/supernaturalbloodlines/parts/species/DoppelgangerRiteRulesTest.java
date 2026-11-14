package net.ofatech.supernaturalbloodlines.parts.species;

import org.junit.jupiter.api.Test;

import static net.ofatech.supernaturalbloodlines.parts.species.rite.DoppelgangerRiteRules.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DoppelgangerRiteRulesTest {
    @Test
    void doppelgangerModeAcceptsOnlyDoppelgangers() {
        assertNull(refusal(false, true, true));
        assertNull(refusal(false, true, false)); // the gene is enough; being human-like is not asked
        assertEquals(NOT_DOPPELGANGER, refusal(false, false, true));
        assertEquals(NOT_DOPPELGANGER, refusal(false, false, false));
    }

    @Test
    void humanModeAcceptsAnyHumanLike() {
        assertNull(refusal(true, false, true));
        assertNull(refusal(true, true, true));
        assertEquals(NOT_HUMAN_LIKE, refusal(true, false, false));
        assertEquals(NOT_HUMAN_LIKE, refusal(true, true, false));
    }

    @Test
    void everyRefusalKeyIsUnderTheGeneNamespace() {
        for (String k : new String[]{NO_OWNER, OWNER_AWAY, NOT_DOPPELGANGER, NOT_HUMAN_LIKE}) {
            assertEquals(true, k.startsWith("supernaturalbloodlines.gene."), k);
        }
    }
}
