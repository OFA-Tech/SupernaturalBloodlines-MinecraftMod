package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;
import org.junit.jupiter.api.Test;

class GroupLabelsTest {
    private static String key(Component c) {
        return ((TranslatableContents) c.getContents()).getKey();
    }

    private static final List<HubSocial.Member> ROWS = List.of(new HubSocial.Member("Dev", "leader", true), new HubSocial.Member("Rowan", "member", true));

    @Test
    void aLeaderIsNeverOfferedLeave() {
        assertFalse(GroupLabels.mayLeave(ROWS, "Dev"), "the server refuses a leader's leave, so the page must not offer it");
        assertTrue(GroupLabels.mayLeave(ROWS, "Rowan"));
        assertFalse(GroupLabels.mayLeave(List.of(), "Dev"), "nobody in a group, nothing to leave");
    }

    @Test
    void theSharedButtonsAlwaysTypeGroup() {
        // /pack does not exist in a world without Werewolves, and a pack's shared actions are the group tree's
        assertEquals("group", GroupLabels.root(GroupView.NONE));
        for (GroupType type : GroupType.values()) {
            assertEquals("group", GroupLabels.root(new GroupView(type, true, GroupView.MEMBER, "L", 0, List.of())), type.name());
        }
    }

    @Test
    void aProfileSaysItsOwnTypeNotTheViewers() {
        assertEquals("supernaturalbloodlines.hub.profile.fact.pack", key(GroupLabels.t(GroupType.PACK, "supernaturalbloodlines.hub.profile.fact.pack", "fact_pack")));
        assertEquals("supernaturalbloodlines.hub.group.fact_pack.clan", key(GroupLabels.t(GroupType.CLAN, "supernaturalbloodlines.hub.profile.fact.pack", "fact_pack")));
        assertEquals("supernaturalbloodlines.hub.group.fact_pack_days.brotherhood", key(GroupLabels.t(GroupType.BROTHERHOOD, "x", "fact_pack_days")));
    }

    @Test
    void aGroupMemberOfAPackKeepsThePackWords() {
        GroupView pack = new GroupView(GroupType.PACK, true, GroupView.MEMBER, "L", 0, List.of());
        assertEquals("supernaturalbloodlines.hub.pack.title", key(GroupLabels.t(pack, "supernaturalbloodlines.hub.pack.title", "title")));
        GroupView coven = new GroupView(GroupType.COVEN, true, GroupView.MEMBER, "L", 0, List.of());
        assertEquals("supernaturalbloodlines.hub.group.title.coven", key(GroupLabels.t(coven, "supernaturalbloodlines.hub.pack.title", "title")));
    }

    @Test
    void theRowsOfTheViewKeepTheirOrderForTheLedger() {
        GroupView v = new GroupView(GroupType.CLAN, true, GroupView.LEADER, "Dev", 1, List.of(
                new GroupView.Row("Dev", "leader", GroupRank.NONE, true, java.util.Optional.empty()),
                new GroupView.Row("Rowan", "day_walker", GroupRank.DAY_WALKER, true, java.util.Optional.empty())));
        assertEquals(List.of("Dev", "Rowan"), GroupLabels.members(v).stream().map(HubSocial.Member::name).toList());
    }
}
