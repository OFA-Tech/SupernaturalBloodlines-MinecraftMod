package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvolutionBoostTest {
    @Test
    void multipliesTheProgress() {
        assertEquals(50.0F, EvolutionBoost.boost(40.0F, 1.25), 1e-4F);
        assertEquals(0.0F, EvolutionBoost.boost(0.0F, 1.25), 1e-4F);
    }

    @Test
    void neverPassesOneHundred() {
        assertEquals(100.0F, EvolutionBoost.boost(80.0F, 1.25), 1e-4F);
        assertEquals(100.0F, EvolutionBoost.boost(99.0F, 10.0), 1e-4F);
        assertEquals(100.0F, EvolutionBoost.boost(100.0F, 1.25), 1e-4F);
    }

    @Test
    void neverLowersTheProgress() {
        assertEquals(40.0F, EvolutionBoost.boost(40.0F, 0.5), 1e-4F);
        assertEquals(100.0F, EvolutionBoost.boost(100.0F, 0.5), 1e-4F);
    }
}
