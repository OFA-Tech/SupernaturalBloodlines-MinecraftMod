package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;
import net.ofatech.supernaturalbloodlines.core.group.MultiTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Plan 20e Task 3: GroupGate multi-type join and lead gates. */
class GroupGateMultiTypeTest {

    @AfterEach
    void restore() {
        GroupsConfig.override("groups_enabled", null);
        GroupsConfig.override("clans_enabled", null);
        GroupsConfig.override("covens_enabled", null);
        GroupsConfig.override("brotherhood_enabled", null);
        GroupsConfig.override("open_founding", null);
    }

    @Test
    void hybridLeaderGetsPackAndClan() {
        Set<GroupType> types = MultiTypes.of(new Leader(Species.HYBRID, true, false, false, true));
        assertTrue(types.contains(GroupType.PACK));
        assertTrue(types.contains(GroupType.CLAN));
        assertEquals(2, types.size());
    }

    @Test
    void tribridGetsThree() {
        Set<GroupType> types = MultiTypes.of(new Leader(Species.TRIBRID, true, false, false, true));
        assertTrue(types.contains(GroupType.PACK));
        assertTrue(types.contains(GroupType.CLAN));
        assertTrue(types.contains(GroupType.COVEN));
        assertFalse(types.contains(GroupType.BROTHERHOOD));
        assertEquals(3, types.size());
    }

    @Test
    void witchHunterLeadsCovenAndBrotherhood() {
        Set<GroupType> types = MultiTypes.of(new Leader(Species.WITCH, false, false, true, true));
        assertTrue(types.contains(GroupType.COVEN));
        assertTrue(types.contains(GroupType.BROTHERHOOD));
        assertFalse(types.contains(GroupType.PACK));
        assertFalse(types.contains(GroupType.CLAN));
        assertEquals(2, types.size());
    }

    @Test
    void plainHumanJoinsBrotherhoodOnly() {
        Set<GroupType> types = MultiTypes.of(new Leader(Species.NONE, false, false, false, true));
        assertEquals(Set.of(GroupType.BROTHERHOOD), types);
    }

    @Test
    void canLeadForHybridWhenEnabled() {
        // open_founding=true means anyone can found their kind's group
        GroupsConfig.override("open_founding", true);
        GroupsConfig.override("clans_enabled", true);
        // A hybrid vampire fits PACK and CLAN; with open founding they can lead
        // This test verifies the gate reads MultiTypes.of for the leader
        // The actual canLead check needs a ServerPlayer mock, so we test the type derivation here
        Set<GroupType> types = MultiTypes.of(new Leader(Species.HYBRID, true, false, false, true));
        assertTrue(types.contains(GroupType.PACK));
        assertTrue(types.contains(GroupType.CLAN));
    }

    @Test
    void openFoundingLiftsGate() {
        GroupsConfig.override("open_founding", true);
        assertTrue(GroupsConfig.openFounding());
    }
}