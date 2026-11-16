package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The undead switch is read on the client too, where the species_tensura config may not be loaded: that must mean "on" (Tensura's own answers for the layers). */
class UndeadSwitchDefaultTest {
    @Test
    void anUnloadedConfigMeansTheTreatmentIsOn() {
        SpeciesTensuraConfig.overrideUndeadTreatment(null);
        assertTrue(SpeciesTensuraConfig.undeadTreatment());
    }
}
