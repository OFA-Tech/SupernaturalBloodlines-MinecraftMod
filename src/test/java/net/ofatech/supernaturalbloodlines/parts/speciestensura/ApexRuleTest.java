package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class ApexRuleTest {
    @Test
    void vampireTypeSpeciesWithAnApexRaceAreExempt() {
        for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.HERETIC, Species.TRIBRID}) {
            assertTrue(ApexRule.isApexVampire(s, "supernaturalbloodlines"), s + " with an apex race");
        }
    }

    @Test
    void aTensuraRaceIsNeverExempt() {
        assertFalse(ApexRule.isApexVampire(Species.ORIGINAL, "tensura"));
        assertFalse(ApexRule.isApexVampire(Species.TRIBRID, "minecraft"));
    }

    @Test
    void otherSpeciesAreNeverExempt() {
        for (Species s : new Species[] {Species.NONE, Species.WITCH, Species.SIPHONER, Species.POTENTIAL, Species.HUNTER_OF_THE_FIVE}) {
            assertFalse(ApexRule.isApexVampire(s, "supernaturalbloodlines"), s + " must not be exempt");
        }
    }
}
