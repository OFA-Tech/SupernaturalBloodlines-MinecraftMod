package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvolutionBoostTest {
    @Test
    void aSpeciesPlayerGainsAQuarterMoreEp() {
        assertEquals(1250.0, 1000.0 * EvolutionBoost.factor(true, 1.25), 1e-9);
    }

    @Test
    void noSpeciesGainsExactlyWhatTensuraGives() {
        assertEquals(1000.0, 1000.0 * EvolutionBoost.factor(false, 1.25), 1e-9);
        assertEquals(1.0, EvolutionBoost.factor(false, 10.0), 0.0);
    }

    @Test
    void theConfigMultiplierIsTheFactor() {
        assertEquals(1.0, EvolutionBoost.factor(true, 1.0), 0.0);
        assertEquals(10.0, EvolutionBoost.factor(true, 10.0), 0.0);
        assertEquals(1.5, EvolutionBoost.factor(true, 1.5), 0.0);
    }
}
