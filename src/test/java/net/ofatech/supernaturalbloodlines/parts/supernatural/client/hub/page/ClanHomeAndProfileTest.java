package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.junit.jupiter.api.Test;

/** The contest request card on Home (one answer, no decline) and the actions of the head on a clanmate's profile. */
class ClanHomeAndProfileTest {
    private static List<String> ids(List<ClanCards.Act> acts) {
        return acts.stream().map(ClanCards.Act::id).toList();
    }

    @Test
    void aCharterContestRequestHasOneAnswerAndNoDecline() {
        var answers = HomePage.answers(HubRequests.Kind.CHARTER_CONTEST);
        assertEquals(1, answers.size());
        assertEquals("C12", answers.get(0).rowId());
        assertTrue(answers.get(0).primary() && !answers.get(0).usesToken());
        assertTrue(answers.stream().noneMatch(a -> a.label().equals("decline")));
    }

    @Test
    void promoteDemoteAndExileAppearForTheHeadOnTheRightRanksOnly() {
        assertEquals(List.of("promote", "exile"), ids(ClanCards.actionsFor(GroupView.LEADER, "member", false, true)));
        assertEquals(List.of("demote", "exile"), ids(ClanCards.actionsFor(GroupView.LEADER, "day_walker", false, true)));
        assertEquals("C13", ClanCards.actionsFor(GroupView.LEADER, "member", false, true).get(0).row());
        assertEquals("C14", ClanCards.actionsFor(GroupView.LEADER, "day_walker", false, true).get(0).row());
        assertEquals("C15", ClanCards.actionsFor(GroupView.LEADER, "member", false, true).get(1).row());
    }

    @Test
    void exileAsksFirstAndPromotionDoesNot() {
        var acts = ClanCards.actionsFor(GroupView.LEADER, "member", false, true);
        assertTrue(!acts.get(0).confirm() && acts.get(1).confirm());
    }

    @Test
    void nobodyElseGetsAny() {
        for (byte rank : new byte[] {GroupView.MEMBER, GroupView.DAY_WALKER, GroupView.ELDER, GroupView.NOT_IN}) {
            assertTrue(ClanCards.actionsFor(rank, "member", false, true).isEmpty(), "rank " + rank);
        }
    }

    @Test
    void nothingForTheHeadsOwnRowAnAllyOrAPersonWithNoId() {
        assertTrue(ClanCards.actionsFor(GroupView.LEADER, "leader", false, true).isEmpty());
        assertTrue(ClanCards.actionsFor(GroupView.LEADER, "member", true, true).isEmpty(), "exile on the head's own row");
        assertTrue(ClanCards.actionsFor(GroupView.LEADER, "ally", false, true).isEmpty());
        assertTrue(ClanCards.actionsFor(GroupView.LEADER, "member", false, false).isEmpty());
    }
}
