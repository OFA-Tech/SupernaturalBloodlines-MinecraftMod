package net.ofatech.supernaturalbloodlines.shared.tensura;

import static net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.HumanLikeLine;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexSpec;
import org.junit.jupiter.api.Test;

class ApexLinesTest {
    @Test
    void saintRulesAreUnchanged() {
        for (String id : new String[] {"tensura:human_saint", "tensura:divine_human", "tensura:elf_saint", "tensura:divine_dwarf", "tensura:divine_fish"}) {
            for (SupernaturalRace r : new SupernaturalRace[] {VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, TRIBRID, HERETIC, WITCH, SIPHONER, HUNTER, WEREWOLF}) {
                assertTrue(ApexLines.atApexTier(r, id), r + " on " + id);
            }
            assertFalse(ApexLines.atApexTier(HUMAN, id));
            assertFalse(ApexLines.atApexTier(GHOUL, id));
        }
        assertFalse(ApexLines.atApexTier(ORIGINAL, "tensura:enlightened_elf"));
        assertFalse(ApexLines.atApexTier(ORIGINAL, "tensura:elf"));
    }

    @Test
    void aHybridQualifiesOnBothTensuraLines() {
        for (String id : new String[] {"tensura:vampire_lord", "tensura:divine_vampire", "tensura:spirit_beast", "tensura:divine_beast"}) {
            assertTrue(ApexLines.atApexTier(HYBRID, id), id);
        }
    }

    @Test
    void aTribridIsNotOfferedFromTheVampireLineButFromBeastfolk() {
        assertFalse(ApexLines.atApexTier(TRIBRID, "tensura:vampire_lord"));
        assertFalse(ApexLines.atApexTier(TRIBRID, "tensura:divine_vampire"));
        assertTrue(ApexLines.atApexTier(TRIBRID, "tensura:spirit_beast"));
    }

    @Test
    void anOriginalHybridIsNotOfferedFromBeastfolk() {
        assertTrue(ApexLines.atApexTier(ORIGINAL_HYBRID, "tensura:vampire_lord"));
        assertFalse(ApexLines.atApexTier(ORIGINAL_HYBRID, "tensura:spirit_beast"));
        assertFalse(ApexLines.atApexTier(ORIGINAL_HYBRID, "tensura:divine_beast"));
    }

    @Test
    void vampireTypesAreOfferedFromTheVampireLine() {
        for (SupernaturalRace r : new SupernaturalRace[] {VAMPIRE, ORIGINAL, HYBRID, ORIGINAL_HYBRID, HERETIC}) {
            assertTrue(ApexLines.atApexTier(r, "tensura:vampire_lord"), r.name());
            assertTrue(ApexLines.atApexTier(r, "tensura:divine_vampire"), r.name());
            assertFalse(ApexLines.atApexTier(r, "tensura:vampire_overcomer"), r + " on the tier below the lord");
            assertFalse(ApexLines.atApexTier(r, "tensura:vampire"), r.name());
        }
        for (SupernaturalRace r : new SupernaturalRace[] {WEREWOLF, TRIBRID, WITCH, SIPHONER, HUNTER, HUMAN, GHOUL}) {
            assertFalse(ApexLines.atApexTier(r, "tensura:vampire_lord"), r.name());
        }
    }

    @Test
    void werewolfTypesAreOfferedFromBeastfolkAtSpiritBeast() {
        for (SupernaturalRace r : new SupernaturalRace[] {WEREWOLF, HYBRID, TRIBRID}) {
            assertFalse(ApexLines.atApexTier(r, "tensura:beastfolk"), r + " below the tier");
            assertFalse(ApexLines.atApexTier(r, "tensura:beast_lord"), r + " below the tier");
            assertTrue(ApexLines.atApexTier(r, "tensura:spirit_beast"), r.name());
            assertTrue(ApexLines.atApexTier(r, "tensura:divine_beast"), r.name());
        }
        for (SupernaturalRace r : new SupernaturalRace[] {VAMPIRE, ORIGINAL, ORIGINAL_HYBRID, HERETIC, WITCH, SIPHONER, HUNTER}) {
            assertFalse(ApexLines.atApexTier(r, "tensura:spirit_beast"), r.name());
        }
    }

    @Test
    void anApexRaceOfOursIsPastTheTier() {
        assertTrue(ApexLines.atApexTier(ORIGINAL, "supernaturalbloodlines:primordial_vampire"));
        assertTrue(ApexLines.atApexTier(WEREWOLF, "supernaturalbloodlines:alpha_primeval"));
        assertFalse(ApexLines.atApexTier(HUMAN, "supernaturalbloodlines:alpha_primeval"));
        // but an apex race has no further apex to be offered
        assertFalse(ApexLines.qualifies(ORIGINAL, "supernaturalbloodlines:primordial_vampire"));
        assertTrue(ApexLines.qualifies(ORIGINAL, "tensura:elf_saint"));
    }

    @Test
    void unknownRacesAreNeverAtTheTier() {
        assertFalse(ApexLines.atApexTier(ORIGINAL, "tensura:slime"));
        assertFalse(ApexLines.atApexTier(ORIGINAL, "somemod:human_saint"));
        assertFalse(ApexLines.atApexTier(ORIGINAL, ""));
    }

    @Test
    void theLinesCoverTheOldSaintListAndOurNamespaceMatchesTheApexRaces() {
        Set<String> all = new HashSet<>(ApexLines.HUMAN_LIKE);
        all.addAll(ApexLines.BEASTFOLK);
        assertEquals(HumanLikeLine.SAINT_OR_HIGHER, all);
        assertEquals(ApexSpec.RACE_NAMESPACE + ":", ApexLines.OUR_NAMESPACE);
    }
}
