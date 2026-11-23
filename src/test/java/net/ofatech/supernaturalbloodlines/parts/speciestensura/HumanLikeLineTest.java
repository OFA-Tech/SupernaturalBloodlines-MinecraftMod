package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HumanLikeLineTest {
    @Test
    void saintTierIsPartOfTheEvolvedRaces() {
        assertTrue(HumanLikeLine.EVOLVED.containsAll(HumanLikeLine.SAINT_OR_HIGHER));
        assertEquals(18, HumanLikeLine.EVOLVED.size());
        assertEquals(13, HumanLikeLine.SAINT_OR_HIGHER.size());
    }

    /** Tensura Horizons' rung between saint and divine is past the saint tier: still offered the apex race, still on the line. */
    @Test
    void theHorizonsRungBetweenSaintAndDivineIsPastTheSaintTier() {
        for (String id : new String[] {"tensurahorizons:ascendant_elf", "tensurahorizons:runeforged_dwarf", "tensurahorizons:tide_sovereign"}) {
            assertTrue(HumanLikeLine.isSaintOrHigher(id), id);
            assertTrue(HumanLikeLine.isEvolvedMember(id), id);
            assertTrue(net.ofatech.supernaturalbloodlines.shared.tensura.ApexLines.HUMAN_LIKE.contains(id), id);
        }
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
