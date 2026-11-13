package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static org.junit.jupiter.api.Assertions.*;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import org.junit.jupiter.api.Test;

class ApexOfferTest {
    @Test
    void offeredFromSaintTierWithTheSpecies() {
        assertEquals("primordial_vampire", ApexOffer.apexIdFor(Species.ORIGINAL, "tensura:elf_saint").orElseThrow());
        assertEquals("void_siphon", ApexOffer.apexIdFor(Species.HERETIC, "tensura:divine_beast").orElseThrow());
    }

    @Test
    void notOfferedBelowSaintTierOrOutsideTheLine() {
        assertTrue(ApexOffer.apexIdFor(Species.ORIGINAL, "tensura:enlightened_elf").isEmpty());
        assertTrue(ApexOffer.apexIdFor(Species.ORIGINAL, "tensura:human").isEmpty());
        assertTrue(ApexOffer.apexIdFor(Species.ORIGINAL, "tensura:vampire_lord").isEmpty());
        assertTrue(ApexOffer.apexIdFor(Species.ORIGINAL, "supernaturalbloodlines:primordial_vampire").isEmpty());
    }

    @Test
    void notOfferedWithoutASpecies() {
        assertTrue(ApexOffer.apexIdFor(Species.NONE, "tensura:human_saint").isEmpty());
        assertTrue(ApexOffer.apexIdFor(Species.POTENTIAL, "tensura:human_saint").isEmpty());
    }
}
