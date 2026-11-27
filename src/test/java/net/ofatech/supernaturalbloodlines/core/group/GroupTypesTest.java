package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Fit;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;
import org.junit.jupiter.api.Test;

/** Spec 3.1: the derivation, row by row, and 3.2: the founding gate. */
class GroupTypesTest {
    private static GroupType of(Species s, boolean vampire, boolean werewolf, boolean hunter) {
        return GroupTypes.of(new Leader(s, vampire, werewolf, hunter, true));
    }

    @Test
    void aPlainWerewolfLeadsAPack() {
        assertEquals(GroupType.PACK, of(Species.NONE, false, true, false));
    }

    @Test
    void hybridTypesLeadPacksEvenThoughTheyAreAlsoVampires() {
        for (Species s : new Species[] {Species.HYBRID, Species.ORIGINAL_HYBRID, Species.TRIBRID}) {
            assertEquals(GroupType.PACK, of(s, true, false, false), s.name());
        }
    }

    @Test
    void aWerewolfWhoIsAlsoAVampireLeadsAPack() {
        assertEquals(GroupType.PACK, of(Species.NONE, true, true, false));
    }

    @Test
    void aVampireLeadsAClan() {
        assertEquals(GroupType.CLAN, of(Species.NONE, true, false, false));
    }

    @Test
    void anOriginalLeadsAClan() {
        assertEquals(GroupType.CLAN, of(Species.ORIGINAL, true, false, false));
    }

    @Test
    void anAugustineLeadsAClan() {
        assertEquals(GroupType.CLAN, of(Species.AUGUSTINE, true, false, false));
    }

    @Test
    void aVampireSiphonerLeadsAClan() {
        assertEquals(GroupType.CLAN, of(Species.SIPHONER, true, false, false));
    }

    @Test
    void aSiphonerWhoIsNotAVampireLeadsACoven() {
        assertEquals(GroupType.COVEN, of(Species.SIPHONER, false, false, false));
    }

    @Test
    void aWitchLeadsACoven() {
        assertEquals(GroupType.COVEN, of(Species.WITCH, false, false, false));
    }

    @Test
    void aHereticLeadsACovenEvenThoughRiteKindsCallItAVampire() {
        assertEquals(GroupType.COVEN, of(Species.HERETIC, true, false, false));
    }

    @Test
    void aPlainHumanLeadsABrotherhood() {
        assertEquals(GroupType.BROTHERHOOD, of(Species.NONE, false, false, false));
    }

    @Test
    void aRaceThatIsNotHumanLikeStillLeadsACoven() {
        assertEquals(GroupType.COVEN, GroupTypes.of(new Leader(Species.NONE, false, false, false, false)));
    }

    @Test
    void aVampireWithNoSpeciesStillLeadsAClan() {
        assertEquals(GroupType.CLAN, of(Species.NONE, true, false, false));
    }

    @Test
    void aHybridIsStillAPack() {
        assertEquals(GroupType.PACK, of(Species.HYBRID, false, false, false));
    }

    @Test
    void aWitchSiphonerHereticStillLeadACoven() {
        for (Species s : new Species[] {Species.WITCH, Species.SIPHONER, Species.HERETIC}) {
            assertEquals(GroupType.COVEN, of(s, false, false, false), s.name());
        }
    }

    @Test
    void theOrderIsPackHunterVampireHumanRest() {
        assertEquals(GroupType.PACK, of(Species.NONE, true, true, true), "werewolf first");
        assertEquals(GroupType.BROTHERHOOD, of(Species.NONE, true, false, true), "hunter before vampire");
        assertEquals(GroupType.CLAN, of(Species.NONE, true, false, false), "vampire before human");
        assertEquals(GroupType.BROTHERHOOD, of(Species.NONE, false, false, false), "human");
        assertEquals(GroupType.COVEN, GroupTypes.of(new Leader(Species.NONE, false, false, false, false)), "the rest");
        assertEquals(GroupType.BROTHERHOOD, GroupTypes.of(new Leader(Species.NONE, false, false, true, false)), "a hunter needs no human-like check");
    }

    @Test
    void aHumanStillCannotFound() {
        assertFalse(GroupTypes.canFound(GroupType.BROTHERHOOD, new Fit(false, false, false, false), false));
        assertTrue(GroupTypes.canFound(GroupType.BROTHERHOOD, new Fit(false, false, false, false), true));
    }

    @Test
    void aHunterLeadsABrotherhood() {
        assertEquals(GroupType.BROTHERHOOD, of(Species.NONE, false, false, true));
        assertEquals(GroupType.BROTHERHOOD, of(Species.HUNTER_OF_THE_FIVE, false, false, true));
        assertEquals(GroupType.BROTHERHOOD, of(Species.POTENTIAL, false, false, true));
    }

    @Test
    void aWitchInTheHunterFactionLeadsABrotherhood() {
        assertEquals(GroupType.BROTHERHOOD, of(Species.WITCH, false, false, true));
    }

    @Test
    void aHunterIsNeverAPackOrAClan() {
        for (Species s : Species.values()) {
            GroupType t = of(s, false, false, true);
            assertTrue(t == GroupType.BROTHERHOOD || t == GroupType.PACK, s.name() + " " + t);
            assertFalse(t == GroupType.CLAN, s.name());
        }
        assertEquals(GroupType.BROTHERHOOD, of(Species.NONE, true, false, true));
    }

    @Test
    void aWerewolfBeatsAHunter() {
        assertEquals(GroupType.PACK, of(Species.NONE, false, true, true));
    }

    @Test
    void canFoundFollowsTheType() {
        assertTrue(GroupTypes.canFound(GroupType.PACK, new Fit(true, false, false, false), false));
        assertTrue(GroupTypes.canFound(GroupType.CLAN, new Fit(false, true, false, false), false));
        assertTrue(GroupTypes.canFound(GroupType.COVEN, new Fit(false, false, true, false), false));
        assertTrue(GroupTypes.canFound(GroupType.BROTHERHOOD, new Fit(false, false, false, true), false));
        for (GroupType t : GroupType.values()) {
            for (GroupType fitOf : GroupType.values()) {
                if (t != fitOf) {
                    Fit only = new Fit(fitOf == GroupType.PACK, fitOf == GroupType.CLAN, fitOf == GroupType.COVEN, fitOf == GroupType.BROTHERHOOD);
                    assertFalse(GroupTypes.canFound(t, only, false), t + " founded by a " + fitOf);
                }
            }
        }
    }

    @Test
    void openFoundingLiftsTheGate() {
        Fit nobody = new Fit(false, false, false, false);
        for (GroupType t : GroupType.values()) {
            assertFalse(GroupTypes.canFound(t, nobody, false));
            assertTrue(GroupTypes.canFound(t, nobody, true));
        }
    }

    @Test
    void idsRoundTrip() {
        for (GroupType t : GroupType.values()) {
            assertEquals(t, GroupType.parse(t.id()).orElseThrow());
        }
        assertTrue(GroupType.parse("warband").isEmpty());
        for (GroupRank r : GroupRank.values()) {
            assertEquals(r, GroupRank.parse(r.id()));
        }
        assertEquals(GroupRank.NONE, GroupRank.parse("bogus"));
    }
}
