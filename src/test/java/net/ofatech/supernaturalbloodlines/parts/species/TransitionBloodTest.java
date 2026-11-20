package net.ofatech.supernaturalbloodlines.parts.species;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.hybrid.TransitionBlood;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransitionBloodTest {
    @Test
    void defaultModeTakesOnlyAnOriginalHybrid() {
        assertTrue(TransitionBlood.accepts(false, Species.ORIGINAL_HYBRID.name()));
        assertFalse(TransitionBlood.accepts(false, Species.ORIGINAL.name()));
    }

    @Test
    void originalModeTakesAnyOriginal() {
        assertTrue(TransitionBlood.accepts(true, Species.ORIGINAL_HYBRID.name()));
        assertTrue(TransitionBlood.accepts(true, Species.ORIGINAL.name()));
    }

    @Test
    void noOtherSpeciesEverStartsIt() {
        for (Species s : Species.values()) {
            if (s == Species.ORIGINAL || s == Species.ORIGINAL_HYBRID) {
                continue;
            }
            assertFalse(TransitionBlood.accepts(false, s.name()), s + " in the default mode");
            assertFalse(TransitionBlood.accepts(true, s.name()), s + " in original mode");
        }
        assertFalse(TransitionBlood.accepts(true, "DRAGON"));
        assertFalse(TransitionBlood.accepts(false, ""));
    }

    @Test
    void aBottleFromBeforeTheUpdateIsAcceptedUnderEitherSetting() {
        assertTrue(TransitionBlood.accepts(false, null));
        assertTrue(TransitionBlood.accepts(true, null));
    }
}
