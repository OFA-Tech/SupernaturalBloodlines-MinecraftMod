package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HumanLikeLineTest {
    @Test
    void saintTierIsPartOfTheEvolvedRaces() {
        assertTrue(HumanLikeLine.EVOLVED.containsAll(HumanLikeLine.SAINT_OR_HIGHER));
        assertEquals(15, HumanLikeLine.EVOLVED.size());
        assertEquals(10, HumanLikeLine.SAINT_OR_HIGHER.size());
    }

    @Test
    void enlightenedIsNotSaintTier() {
        assertTrue(HumanLikeLine.isEvolvedMember("tensura:enlightened_elf"));
        assertFalse(HumanLikeLine.isSaintOrHigher("tensura:enlightened_elf"));
        assertFalse(HumanLikeLine.isSaintOrHigher("tensura:beast_lord"));
        assertTrue(HumanLikeLine.isSaintOrHigher("tensura:spirit_beast"));
        assertFalse(HumanLikeLine.isEvolvedMember("tensura:vampire"));
    }
}
