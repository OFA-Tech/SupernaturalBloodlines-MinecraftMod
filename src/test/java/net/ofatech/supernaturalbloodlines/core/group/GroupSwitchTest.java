package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroupSwitchTest {
    @Test
    void equalTypesPlanNothing() {
        for (GroupType t : GroupType.values()) {
            assertTrue(GroupSwitch.plan(t, t).isEmpty());
        }
    }

    @Test
    void aChangePlansTheTwoTypes() {
        GroupSwitch.Switch s = GroupSwitch.plan(GroupType.CLAN, GroupType.COVEN).orElseThrow();
        assertEquals(GroupType.CLAN, s.from());
        assertEquals(GroupType.COVEN, s.to());
    }

    @Test
    void dormantMeansNotTheActiveType() {
        assertFalse(GroupSwitch.dormant(GroupType.CLAN, GroupType.CLAN));
        assertTrue(GroupSwitch.dormant(GroupType.COVEN, GroupType.CLAN));
        assertFalse(GroupSwitch.dormant(GroupType.COVEN, null));
    }
}
