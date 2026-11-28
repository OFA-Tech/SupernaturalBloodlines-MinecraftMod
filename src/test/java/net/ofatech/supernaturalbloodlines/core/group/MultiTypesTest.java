package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;
import org.junit.jupiter.api.Test;

/** Plan 20e §1.2: multi-type computation from leader facts, row by row. */
class MultiTypesTest {
    private static Set<GroupType> of(Species s, boolean vampire, boolean werewolf, boolean hunter) {
        return MultiTypes.of(new Leader(s, vampire, werewolf, hunter, true));
    }

    @Test
    void aPlainWerewolfLeadsPackOnly() {
        assertEquals(Set.of(GroupType.PACK), of(Species.NONE, false, true, false));
    }

    @Test
    void hybridVampireLeadsPackAndClan() {
        for (Species s : new Species[] {Species.HYBRID, Species.ORIGINAL_HYBRID}) {
            assertEquals(Set.of(GroupType.PACK, GroupType.CLAN), of(s, true, false, false), s.name());
        }
    }

    @Test
    void tribridLeadsPackClanAndCoven() {
        assertEquals(Set.of(GroupType.PACK, GroupType.CLAN, GroupType.COVEN), of(Species.TRIBRID, true, false, false));
    }

    @Test
    void vampireMagicUserLeadsClanAndCoven() {
        for (Species s : new Species[] {Species.SIPHONER, Species.HERETIC}) {
            assertEquals(Set.of(GroupType.CLAN, GroupType.COVEN), of(s, true, false, false), s.name());
        }
    }

    @Test
    void werewolfWitchLeadsPackAndCoven() {
        assertEquals(Set.of(GroupType.PACK, GroupType.COVEN), of(Species.WITCH, false, true, false));
    }

    @Test
    void witchHunterLeadsCovenAndBrotherhood() {
        assertEquals(Set.of(GroupType.COVEN, GroupType.BROTHERHOOD), of(Species.WITCH, false, false, true));
    }

    @Test
    void hunterAloneLeadsBrotherhood() {
        assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.NONE, false, false, true));
        assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.HUNTER_OF_THE_FIVE, false, false, true));
        assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.POTENTIAL, false, false, true));
    }

    @Test
    void plainHumanLeadsBrotherhood() {
        assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.NONE, false, false, false));
    }

    @Test
    void vampireAloneLeadsClan() {
        for (Species s : new Species[] {Species.NONE, Species.ORIGINAL, Species.AUGUSTINE}) {
            assertEquals(Set.of(GroupType.CLAN), of(s, true, false, false), s.name());
        }
    }

    @Test
    void magicUserAloneLeadsCoven() {
        for (Species s : new Species[] {Species.WITCH, Species.SIPHONER, Species.HERETIC}) {
            assertEquals(Set.of(GroupType.COVEN), of(s, false, false, false), s.name());
        }
    }

    @Test
    void mainTypeIsDeterministic() {
        assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of(GroupType.PACK, GroupType.CLAN)));
        assertEquals(GroupType.CLAN, MultiTypes.mainType(Set.of(GroupType.CLAN, GroupType.COVEN)));
        assertEquals(GroupType.COVEN, MultiTypes.mainType(Set.of(GroupType.COVEN, GroupType.BROTHERHOOD)));
        assertEquals(GroupType.BROTHERHOOD, MultiTypes.mainType(Set.of(GroupType.BROTHERHOOD)));
    }

    @Test
    void emptySetGivesPack() {
        assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of()));
    }

    @Test
    void packFitsEveryone() {
        assertTrue(MultiTypes.fits(GroupType.PACK, new Leader(Species.NONE, false, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.PACK, new Leader(Species.WITCH, true, false, false, true)));
    }

    @Test
    void clanFitsVampireLike() {
        assertTrue(MultiTypes.fits(GroupType.CLAN, new Leader(Species.NONE, true, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.CLAN, new Leader(Species.ORIGINAL, true, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.CLAN, new Leader(Species.AUGUSTINE, true, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.CLAN, new Leader(Species.SIPHONER, true, false, false, true)));
        assertFalse(MultiTypes.fits(GroupType.CLAN, new Leader(Species.WITCH, false, false, false, true)));
    }

    @Test
    void covenFitsWitchLike() {
        assertTrue(MultiTypes.fits(GroupType.COVEN, new Leader(Species.WITCH, false, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.COVEN, new Leader(Species.SIPHONER, false, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.COVEN, new Leader(Species.HERETIC, false, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.COVEN, new Leader(Species.TRIBRID, false, false, false, true)));
        assertFalse(MultiTypes.fits(GroupType.COVEN, new Leader(Species.NONE, true, false, false, true)));
    }

    @Test
    void brotherhoodFitsHumanOrHunter() {
        assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, new Leader(Species.NONE, false, false, false, true)));
        assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, new Leader(Species.NONE, false, false, true, true)));
        assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, new Leader(Species.HUNTER_OF_THE_FIVE, false, false, true, true)));
        assertFalse(MultiTypes.fits(GroupType.BROTHERHOOD, new Leader(Species.NONE, true, false, false, true)));
    }

    @Test
    void witchHunterFitsBothCovenAndBrotherhood() {
        Leader wh = new Leader(Species.WITCH, false, false, true, true);
        assertTrue(MultiTypes.fits(GroupType.COVEN, wh));
        assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, wh));
    }
}