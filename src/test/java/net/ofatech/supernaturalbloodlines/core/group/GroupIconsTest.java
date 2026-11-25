package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroupIconsTest {
    @Test
    void eachTypeHasItsOwnIcon() {
        assertEquals("pack", GroupIcons.name(GroupType.PACK, true));
        assertEquals("coven", GroupIcons.name(GroupType.COVEN, true));
        assertEquals("clan", GroupIcons.name(GroupType.CLAN, true));
        assertEquals("brotherhood", GroupIcons.name(GroupType.BROTHERHOOD, true));
    }

    @Test
    void someoneInNoGroupKeepsThePaw() {
        for (GroupType type : GroupType.values()) {
            assertEquals("pack", GroupIcons.name(type, false), type.name());
        }
    }

    @Test
    void everyTypeIsCovered() {
        assertEquals(4, GroupType.values().length, "a new group type needs an icon here");
    }
}