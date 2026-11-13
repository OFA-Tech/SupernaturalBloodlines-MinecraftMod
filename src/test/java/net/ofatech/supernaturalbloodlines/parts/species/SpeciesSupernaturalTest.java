package net.ofatech.supernaturalbloodlines.parts.species;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeciesSupernaturalTest {
    @Test
    void humanSideSpeciesAreNotSupernatural() {
        assertFalse(SpeciesSupernatural.isSupernatural(Species.NONE));
        assertFalse(SpeciesSupernatural.isSupernatural(Species.POTENTIAL));
        assertFalse(SpeciesSupernatural.isSupernatural(Species.HUNTER_OF_THE_FIVE));
        assertTrue(SpeciesSupernatural.isSupernatural(Species.WITCH));
        assertTrue(SpeciesSupernatural.isSupernatural(Species.HYBRID));
    }

    @Test
    void witchesAndSiphonersAreNeverVampiricOrPrey() {
        assertFalse(SpeciesSupernatural.isVampiric(Species.WITCH));
        assertFalse(SpeciesSupernatural.isVampiric(Species.SIPHONER));
        for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID, Species.HERETIC}) {
            assertTrue(SpeciesSupernatural.isVampiric(s), s.name());
        }
        // every species is classified: a new one must be decided here
        for (Species s : Species.values()) {
            assertEquals(s != Species.NONE && s != Species.POTENTIAL && s != Species.HUNTER_OF_THE_FIVE && s != Species.WITCH && s != Species.SIPHONER,
                    SpeciesSupernatural.isVampiric(s), s.name());
        }
    }
}
