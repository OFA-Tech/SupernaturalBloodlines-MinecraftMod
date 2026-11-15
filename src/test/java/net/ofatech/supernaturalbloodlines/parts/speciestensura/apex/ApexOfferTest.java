package net.ofatech.supernaturalbloodlines.parts.speciestensura.apex;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Predicate;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import org.junit.jupiter.api.Test;

class ApexOfferTest {
    private static final Predicate<SupernaturalRace> OPEN = r -> true;

    private static List<String> offer(Species species, List<SupernaturalRace> races, String tensuraRace) {
        return ApexOffer.apexIdsFor(species, races, tensuraRace, OPEN);
    }

    @Test
    void offeredFromSaintTierWithTheSpecies() {
        assertEquals(List.of("primordial_vampire"), offer(Species.ORIGINAL, List.of(ORIGINAL), "tensura:elf_saint"));
        assertEquals(List.of("void_siphon"), offer(Species.HERETIC, List.of(HERETIC), "tensura:human_saint"));
    }

    @Test
    void notOfferedBelowSaintTierOrOutsideTheLine() {
        assertTrue(offer(Species.ORIGINAL, List.of(ORIGINAL), "tensura:enlightened_elf").isEmpty());
        assertTrue(offer(Species.ORIGINAL, List.of(ORIGINAL), "tensura:human").isEmpty());
        assertTrue(offer(Species.ORIGINAL, List.of(ORIGINAL), "tensura:vampire_overcomer").isEmpty());
        assertTrue(offer(Species.ORIGINAL, List.of(ORIGINAL), "supernaturalbloodlines:primordial_vampire").isEmpty());
    }

    @Test
    void notOfferedWithoutARace() {
        assertTrue(offer(Species.NONE, List.of(HUMAN), "tensura:human_saint").isEmpty());
        assertTrue(offer(Species.NONE, List.of(GHOUL), "tensura:vampire_lord").isEmpty());
    }

    @Test
    void aPotentialIsNotAHunterOfTheFive() {
        assertTrue(offer(Species.POTENTIAL, List.of(HUNTER), "tensura:human_saint").isEmpty());
        assertTrue(offer(Species.NONE, List.of(HUNTER), "tensura:human_saint").isEmpty());
        assertEquals(List.of("eternal_hunter"), offer(Species.HUNTER_OF_THE_FIVE, List.of(HUNTER), "tensura:human_saint"));
    }

    @Test
    void aHybridIsOfferedFromTheVampireLineAndFromBeastfolk() {
        assertEquals(List.of("moonblood_hybrid"), offer(Species.HYBRID, List.of(HYBRID), "tensura:vampire_lord"));
        assertEquals(List.of("moonblood_hybrid"), offer(Species.HYBRID, List.of(HYBRID), "tensura:spirit_beast"));
    }

    @Test
    void aTribridNotFromTheVampireLineAndAnOriginalHybridNotFromBeastfolk() {
        assertTrue(offer(Species.TRIBRID, List.of(TRIBRID), "tensura:vampire_lord").isEmpty());
        assertEquals(List.of("tribrid_ascendant"), offer(Species.TRIBRID, List.of(TRIBRID), "tensura:spirit_beast"));
        assertTrue(offer(Species.ORIGINAL_HYBRID, List.of(ORIGINAL_HYBRID), "tensura:spirit_beast").isEmpty());
        assertEquals(List.of("primordial_hybrid"), offer(Species.ORIGINAL_HYBRID, List.of(ORIGINAL_HYBRID), "tensura:vampire_lord"));
    }

    @Test
    void thePlainRacesAreOfferedTheirNewApex() {
        assertEquals(List.of("elder_vampire"), offer(Species.NONE, List.of(VAMPIRE), "tensura:vampire_lord"));
        assertEquals(List.of("elder_vampire"), offer(Species.NONE, List.of(VAMPIRE), "tensura:human_saint"));
        assertTrue(offer(Species.NONE, List.of(VAMPIRE), "tensura:spirit_beast").isEmpty());
        assertEquals(List.of("alpha_primeval"), offer(Species.NONE, List.of(WEREWOLF), "tensura:spirit_beast"));
        assertTrue(offer(Species.NONE, List.of(WEREWOLF), "tensura:beastfolk").isEmpty());
        assertTrue(offer(Species.NONE, List.of(WEREWOLF), "tensura:vampire_lord").isEmpty());
    }

    @Test
    void aWitchWerewolfIsOfferedBothApexesWhereBothLinesAllowThem() {
        assertEquals(List.of("alpha_primeval", "ancestral_witch"), offer(Species.WITCH, List.of(WEREWOLF, WITCH), "tensura:human_saint"));
        // the magic ladder stacks on the Beastfolk line of a werewolf
        assertEquals(List.of("alpha_primeval", "ancestral_witch"), offer(Species.WITCH, List.of(WEREWOLF, WITCH), "tensura:spirit_beast"));
        assertEquals(List.of("alpha_primeval", "void_siphon"), offer(Species.SIPHONER, List.of(WEREWOLF, SIPHONER), "tensura:divine_beast"));
        assertTrue(offer(Species.WITCH, List.of(HUMAN, WITCH), "tensura:spirit_beast").isEmpty());
        assertTrue(offer(Species.WITCH, List.of(WEREWOLF, WITCH), "tensura:vampire_lord").isEmpty());
        assertEquals(List.of("alpha_primeval", "void_siphon"), offer(Species.SIPHONER, List.of(WEREWOLF, SIPHONER), "tensura:human_saint"));
    }

    @Test
    void eachApexNeedsItsOwnTopRung() {
        Predicate<SupernaturalRace> onlyWerewolf = r -> r == WEREWOLF;
        assertEquals(List.of("alpha_primeval"),
                ApexOffer.apexIdsFor(Species.WITCH, List.of(WEREWOLF, WITCH), "tensura:human_saint", onlyWerewolf));
        assertTrue(ApexOffer.apexIdsFor(Species.HYBRID, List.of(HYBRID), "tensura:vampire_lord", onlyWerewolf).isEmpty());
    }
}
