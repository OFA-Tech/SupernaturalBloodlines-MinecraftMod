package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GroupsConfigTest {
    @AfterEach
    void restore() {
        for (String k : new String[] {"groups_enabled", "clans_enabled", "covens_enabled", "open_founding", "max_members"}) {
            GroupsConfig.override(k, null);
        }
    }

    @Test
    void defaultsAnswerBeforeTheFileLoads() {
        assertTrue(GroupsConfig.groupsEnabled() && GroupsConfig.clansEnabled() && GroupsConfig.covensEnabled());
        assertFalse(GroupsConfig.openFounding());
        assertEquals(0, GroupsConfig.maxMembers());
    }

    @Test
    void overrideRestores() {
        GroupsConfig.override("open_founding", true);
        GroupsConfig.override("max_members", 5);
        assertTrue(GroupsConfig.openFounding());
        assertEquals(5, GroupsConfig.maxMembers());
        GroupsConfig.override("open_founding", null);
        GroupsConfig.override("max_members", null);
        assertFalse(GroupsConfig.openFounding());
        assertEquals(0, GroupsConfig.maxMembers());
    }

    @Test
    void aNegativeMaxMembersIsClamped() {
        GroupsConfig.override("max_members", -3);
        assertEquals(0, GroupsConfig.maxMembers());
    }
}
