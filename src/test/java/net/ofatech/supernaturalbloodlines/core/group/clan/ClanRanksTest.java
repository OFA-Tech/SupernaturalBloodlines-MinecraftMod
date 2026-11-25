package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanRanks.Refusal;
import org.junit.jupiter.api.Test;

class ClanRanksTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    void autoPromotionIsDueAfterTheDays() {
        assertTrue(ClanRanks.autoPromotionDue(0, 14 * DAY, 14, false));
        assertTrue(ClanRanks.autoPromotionDue(0, 20 * DAY, 14, false));
    }

    @Test
    void oneMillisecondEarlierIsNot() {
        assertFalse(ClanRanks.autoPromotionDue(0, 14 * DAY - 1, 14, false));
    }

    @Test
    void zeroDaysMeansLeaderOnly() {
        assertFalse(ClanRanks.autoPromotionDue(0, 1000 * DAY, 0, false));
    }

    @Test
    void aDemotedMemberIsNeverAutoPromoted() {
        assertFalse(ClanRanks.autoPromotionDue(0, 1000 * DAY, 14, true));
    }

    @Test
    void onlyTheLeaderPromotesAndDemotes() {
        assertEquals(Optional.empty(), ClanRanks.mayPromote(GroupRank.NONE, true, GroupRank.NONE, false, true));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayPromote(GroupRank.DAY_WALKER, false, GroupRank.NONE, false, true));
        assertEquals(Optional.empty(), ClanRanks.mayDemote(GroupRank.NONE, true, GroupRank.DAY_WALKER, false, true));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayDemote(GroupRank.DAY_WALKER, false, GroupRank.DAY_WALKER, false, true));
    }

    @Test
    void aNightWalkerCannotPromote() {
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayPromote(GroupRank.NONE, false, GroupRank.NONE, false, true));
    }

    @Test
    void cannotPromoteTheLeaderOrAnOutsider() {
        assertEquals(Optional.of(Refusal.IS_LEADER), ClanRanks.mayPromote(GroupRank.NONE, true, GroupRank.NONE, true, true));
        assertEquals(Optional.of(Refusal.NOT_A_MEMBER), ClanRanks.mayPromote(GroupRank.NONE, true, GroupRank.NONE, false, false));
        assertEquals(Optional.of(Refusal.IS_LEADER), ClanRanks.mayDemote(GroupRank.NONE, true, GroupRank.NONE, true, true));
        assertEquals(Optional.of(Refusal.NOT_A_MEMBER), ClanRanks.mayDemote(GroupRank.NONE, true, GroupRank.DAY_WALKER, false, false));
    }

    @Test
    void promotingADayWalkerAgainIsRefused() {
        assertEquals(Optional.of(Refusal.ALREADY_DAY_WALKER), ClanRanks.mayPromote(GroupRank.NONE, true, GroupRank.DAY_WALKER, false, true));
    }

    @Test
    void demotingANightWalkerIsRefused() {
        assertEquals(Optional.of(Refusal.NOT_A_DAY_WALKER), ClanRanks.mayDemote(GroupRank.NONE, true, GroupRank.NONE, false, true));
    }
}
