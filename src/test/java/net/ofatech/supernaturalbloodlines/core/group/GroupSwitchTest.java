package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** R241: a switch is planned only when the stored and the derived type differ, and a row of another type is dormant. */
class GroupSwitchTest {
    @Test
    void equalTypesPlanNothing() {
        for (GroupType t : GroupType.values()) {
            assertTrue(GroupSwitch.plan(t, t).isEmpty());
        }
    }

    @Test
    void aChangePlansTheTwoTypes() {
        var plan = GroupSwitch.plan(GroupType.CLAN, GroupType.COVEN).orElseThrow();
        assertEquals(GroupType.CLAN, plan.from());
        assertEquals(GroupType.COVEN, plan.to());
    }

    @Test
    void dormantMeansNotTheActiveType() {
        assertTrue(GroupSwitch.dormant(GroupType.COVEN, GroupType.CLAN));
        assertFalse(GroupSwitch.dormant(GroupType.CLAN, GroupType.CLAN));
        assertFalse(GroupSwitch.dormant(GroupType.CLAN, null));
    }
}
