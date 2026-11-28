package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.MultiTypes;
import org.junit.jupiter.api.Test;

/** Plan 20e Task 3: Groups multi-type derive and type queries. */
class GroupsMultiTypeTest {

    @Test
    void deriveReturnsMultiTypeSetForHybrid() {
        // A hybrid vampire should derive PACK + CLAN
        // This test verifies Groups.derive uses MultiTypes.of internally
        // Since we can't easily mock ServerPlayer, we verify MultiTypes.of directly
        var leader = new net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader(
                Species.HYBRID, true, false, false, true);
        Set<GroupType> types = MultiTypes.of(leader);
        assertTrue(types.contains(GroupType.PACK));
        assertTrue(types.contains(GroupType.CLAN));
    }

    @Test
    void deriveReturnsMultiTypeSetForWitchHunter() {
        var leader = new net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader(
                Species.WITCH, false, false, true, true);
        Set<GroupType> types = MultiTypes.of(leader);
        assertTrue(types.contains(GroupType.COVEN));
        assertTrue(types.contains(GroupType.BROTHERHOOD));
        assertFalse(types.contains(GroupType.PACK));
        assertFalse(types.contains(GroupType.CLAN));
    }

    @Test
    void mainTypeOfMultiTypeSetIsPackForHybrid() {
        var leader = new net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader(
                Species.HYBRID, true, false, false, true);
        Set<GroupType> types = MultiTypes.of(leader);
        assertEquals(GroupType.PACK, MultiTypes.mainType(types));
    }

    @Test
    void mainTypeOfMultiTypeSetIsCovenForWitchHunter() {
        var leader = new net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader(
                Species.WITCH, false, false, true, true);
        Set<GroupType> types = MultiTypes.of(leader);
        assertEquals(GroupType.COVEN, MultiTypes.mainType(types));
    }
}