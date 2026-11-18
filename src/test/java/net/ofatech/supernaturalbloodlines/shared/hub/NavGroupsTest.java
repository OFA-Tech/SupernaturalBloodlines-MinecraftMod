package net.ofatech.supernaturalbloodlines.shared.hub;

import static net.ofatech.supernaturalbloodlines.shared.hub.HubPage.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NavGroupsTest {
    private static final List<HubPage> OP = List.of(HOME, PATH, SKILLS, QUESTS, RITES, BLOODLINE, PACK, GUIDE, SETTINGS, ADMIN);

    @Test
    void aGapWhereTheGroupChangesAndAWiderOneBeforeAdmin() {
        assertArrayEquals(new int[] {0, 1, 0, 0, 1, 1, 0, 1, 1, 2}, NavGroups.gaps(OP));
    }

    @Test
    void aHiddenPageLeavesNoEmptyGroupAndHomeHasNoHeading() {
        List<HubPage> noRitesNoAdmin = List.of(HOME, PATH, SKILLS, QUESTS, BLOODLINE, GUIDE, SETTINGS);
        assertArrayEquals(new int[] {0, 1, 0, 0, 1, 1, 1}, NavGroups.gaps(noRitesNoAdmin));
        assertEquals(List.of(Group.ME, Group.PEOPLE, Group.HELP, Group.SYSTEM), NavGroups.headings(noRitesNoAdmin).stream().map(NavGroups.Heading::group).toList());
        assertEquals(1, NavGroups.headings(noRitesNoAdmin).get(0).tab());
    }

    @Test
    void anOperatorGetsTheOperatorHeadingOverAdmin() {
        var last = NavGroups.headings(OP).get(NavGroups.headings(OP).size() - 1);
        assertEquals(Group.OPERATOR, last.group());
        assertEquals(OP.indexOf(ADMIN), last.tab());
    }
}

