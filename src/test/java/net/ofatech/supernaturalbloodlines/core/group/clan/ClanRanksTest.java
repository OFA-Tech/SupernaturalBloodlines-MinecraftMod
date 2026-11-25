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
    void exileFollowsTheSameLeaderAndMemberChecks() {
        assertEquals(Optional.empty(), ClanRanks.mayExile(true, false, true));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayExile(false, false, true));
        assertEquals(Optional.of(Refusal.IS_LEADER), ClanRanks.mayExile(true, true, true));
        assertEquals(Optional.of(Refusal.NOT_A_MEMBER), ClanRanks.mayExile(true, false, false));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayExile(false, true, false), "the caller is checked first");
    }

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
        assertEquals(Optional.empty(), ClanRanks.mayPromote(true, GroupRank.NONE, false, true));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayPromote(false, GroupRank.NONE, false, true));
        assertEquals(Optional.empty(), ClanRanks.mayDemote(true, GroupRank.DAY_WALKER, false, true));
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayDemote(false, GroupRank.DAY_WALKER, false, true));
    }

    @Test
    void aNightWalkerCannotPromote() {
        assertEquals(Optional.of(Refusal.NOT_LEADER), ClanRanks.mayPromote(false, GroupRank.NONE, false, true));
    }

    @Test
    void cannotPromoteTheLeaderOrAnOutsider() {
        assertEquals(Optional.of(Refusal.IS_LEADER), ClanRanks.mayPromote(true, GroupRank.NONE, true, true));
        assertEquals(Optional.of(Refusal.NOT_A_MEMBER), ClanRanks.mayPromote(true, GroupRank.NONE, false, false));
        assertEquals(Optional.of(Refusal.IS_LEADER), ClanRanks.mayDemote(true, GroupRank.NONE, true, true));
        assertEquals(Optional.of(Refusal.NOT_A_MEMBER), ClanRanks.mayDemote(true, GroupRank.DAY_WALKER, false, false));
    }

    @Test
    void promotingADayWalkerAgainIsRefused() {
        assertEquals(Optional.of(Refusal.ALREADY_DAY_WALKER), ClanRanks.mayPromote(true, GroupRank.DAY_WALKER, false, true));
    }

    @Test
    void demotingANightWalkerIsRefused() {
        assertEquals(Optional.of(Refusal.NOT_A_DAY_WALKER), ClanRanks.mayDemote(true, GroupRank.NONE, false, true));
    }
}
