package net.ofatech.supernaturalbloodlines.parts.artifacts;

import net.neoforged.bus.api.EventPriority;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResurrectionRingRulesTest {
    /** Vampirism's death handler is HIGHEST, OriginalRules HIGH; TransitionHandler and GeneAwakening are LOW. */
    @Test
    void runsAfterTheSaversAndBeforeTheConsequences() {
        assertTrue(ResurrectionRing.PRIORITY.ordinal() > EventPriority.HIGH.ordinal());
        assertTrue(ResurrectionRing.PRIORITY.ordinal() < EventPriority.LOW.ordinal());
    }

    @Test
    void onlyHumansAreRevived() {
        for (Species s : Species.values()) {
            boolean human = s == Species.NONE || s == Species.POTENTIAL || s == Species.HUNTER_OF_THE_FIVE;
            assertEquals(human, ResurrectionRing.speciesAllowed(s), s.name());
        }
        assertFalse(ResurrectionRing.speciesAllowed(Species.HYBRID));
    }

    @Test
    void chargesNeverGoBelowZero() {
        assertEquals(2, ResurrectionRing.after(3));
        assertEquals(0, ResurrectionRing.after(1));
        assertEquals(0, ResurrectionRing.after(0));
    }
}
