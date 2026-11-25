package net.ofatech.supernaturalbloodlines.core.group.clan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.GroupRank;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules.Holder;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules.Outcome;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules.Refusal;
import org.junit.jupiter.api.Test;

class ContestRulesTest {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final UUID LEADER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID WALKER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private static Optional<Refusal> challenge(GroupRank rank, int days, int members, boolean leaderOnline, boolean liveCharter, boolean pending, long cooldownUntil) {
        return ContestRules.mayChallenge(rank, days, members, leaderOnline, liveCharter, pending, cooldownUntil, 100 * DAY, 7, 3);
    }

    @Test
    void aDayWalkerWithTheDaysMayChallenge() {
        assertEquals(Optional.empty(), challenge(GroupRank.DAY_WALKER, 7, 3, true, true, false, 0));
        assertEquals(Optional.empty(), challenge(GroupRank.DAY_WALKER, 30, 9, true, true, false, 100 * DAY), "a cooldown that ends exactly now is over");
    }

    @Test
    void eachRefusalInItsOrder() {
        assertEquals(Optional.of(Refusal.NOT_DAY_WALKER), challenge(GroupRank.NONE, 7, 3, true, true, false, 0));
        assertEquals(Optional.of(Refusal.NOT_DAY_WALKER), challenge(GroupRank.ELDER, 7, 3, true, true, false, 0));
        assertEquals(Optional.of(Refusal.TOO_NEW), challenge(GroupRank.DAY_WALKER, 6, 3, true, true, false, 0));
        assertEquals(Optional.of(Refusal.LEADER_AWAY), challenge(GroupRank.DAY_WALKER, 7, 3, false, true, false, 0));
        assertEquals(Optional.of(Refusal.NO_CHARTER), challenge(GroupRank.DAY_WALKER, 7, 3, true, false, false, 0));
        assertEquals(Optional.of(Refusal.TOO_FEW), challenge(GroupRank.DAY_WALKER, 7, 2, true, true, false, 0));
        assertEquals(Optional.of(Refusal.ALREADY), challenge(GroupRank.DAY_WALKER, 7, 3, true, true, true, 0));
        assertEquals(Optional.of(Refusal.COOLDOWN), challenge(GroupRank.DAY_WALKER, 7, 3, true, true, false, 100 * DAY + 1));
        // two at once answer the first in the list
        assertEquals(Optional.of(Refusal.NOT_DAY_WALKER), challenge(GroupRank.NONE, 0, 1, false, false, true, 200 * DAY));
        assertEquals(Optional.of(Refusal.TOO_NEW), challenge(GroupRank.DAY_WALKER, 0, 1, false, false, true, 200 * DAY));
        assertEquals(Optional.of(Refusal.LEADER_AWAY), challenge(GroupRank.DAY_WALKER, 7, 1, false, false, true, 200 * DAY));
        assertEquals(Optional.of(Refusal.NO_CHARTER), challenge(GroupRank.DAY_WALKER, 7, 1, true, false, true, 200 * DAY));
        assertEquals(Optional.of(Refusal.TOO_FEW), challenge(GroupRank.DAY_WALKER, 7, 1, true, true, true, 200 * DAY));
        assertEquals(Optional.of(Refusal.ALREADY), challenge(GroupRank.DAY_WALKER, 7, 3, true, true, true, 200 * DAY));
    }

    private static Holder holder(CharterState state, UUID id, boolean online, boolean alive, GroupRank rank, boolean inClan) {
        return new Holder(state, id, online, alive, rank, inClan);
    }

    @Test
    void resolveTable() {
        Outcome change = ContestRules.resolve(holder(CharterState.HELD, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER);
        assertEquals(Optional.of(WALKER), change.newLeader());
        assertEquals("", change.reason());
        assertEquals("leader_holds", ContestRules.resolve(holder(CharterState.HELD, LEADER, true, true, GroupRank.NONE, true), LEADER).reason());
        assertEquals("not_day_walker", ContestRules.resolve(holder(CharterState.HELD, WALKER, true, true, GroupRank.NONE, true), LEADER).reason());
        assertEquals("offline", ContestRules.resolve(holder(CharterState.HELD, WALKER, false, true, GroupRank.DAY_WALKER, true), LEADER).reason());
        assertEquals("not_held", ContestRules.resolve(holder(CharterState.STORED, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER).reason());
        assertEquals("not_held", ContestRules.resolve(holder(CharterState.DROPPED, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER).reason());
        assertEquals("not_held", ContestRules.resolve(holder(CharterState.LOST, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER).reason());
        assertEquals("dead", ContestRules.resolve(holder(CharterState.HELD, WALKER, true, false, GroupRank.DAY_WALKER, true), LEADER).reason());
        assertEquals("left", ContestRules.resolve(holder(CharterState.HELD, WALKER, true, true, GroupRank.DAY_WALKER, false), LEADER).reason());
        for (Outcome o : new Outcome[] {ContestRules.resolve(holder(CharterState.STORED, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER),
                ContestRules.resolve(holder(CharterState.HELD, null, false, false, GroupRank.NONE, false), LEADER)}) {
            assertEquals(Optional.empty(), o.newLeader());
        }
    }

    @Test
    void aStoredCharterThatTheHeadLastHeldIsForfeitedToAnOnlineLivingDayWalkerChallenger() {
        Holder stored = holder(CharterState.STORED, null, false, false, GroupRank.NONE, false);
        Holder challenger = holder(CharterState.HELD, WALKER, true, true, GroupRank.DAY_WALKER, true);
        Outcome won = ContestRules.resolve(stored, LEADER, challenger, true);
        assertEquals(Optional.of(WALKER), won.newLeader());
        assertTrue(won.forfeit());
        assertEquals("forfeit_challenger_away", ContestRules.resolve(stored, LEADER, holder(CharterState.HELD, WALKER, false, false, GroupRank.DAY_WALKER, true), true).reason());
        assertEquals("forfeit_challenger_away", ContestRules.resolve(stored, LEADER, holder(CharterState.HELD, WALKER, true, false, GroupRank.DAY_WALKER, true), true).reason());
        assertEquals("forfeit_challenger_away", ContestRules.resolve(stored, LEADER, holder(CharterState.HELD, WALKER, true, true, GroupRank.NONE, true), true).reason());
        assertEquals("forfeit_challenger_away", ContestRules.resolve(stored, LEADER, holder(CharterState.HELD, WALKER, true, true, GroupRank.DAY_WALKER, false), true).reason());
        assertEquals("not_held", ContestRules.resolve(stored, LEADER, challenger, false).reason(), "a Charter somebody else stored is no forfeit");
        assertEquals("not_held", ContestRules.resolve(holder(CharterState.DROPPED, null, false, false, GroupRank.NONE, false), LEADER, challenger, true).reason(), "only STORED is a forfeit");
        assertEquals("not_held", ContestRules.resolve(holder(CharterState.LOST, null, false, false, GroupRank.NONE, false), LEADER, challenger, true).reason());
        assertFalse(ContestRules.resolve(holder(CharterState.HELD, WALKER, true, true, GroupRank.DAY_WALKER, true), LEADER, challenger, true).forfeit(), "a held Charter is the ordinary win");
    }

    @Test
    void theWireNumberOfARefusalRoundTripsAndZeroIsNone() {
        assertEquals(0, Refusal.wire(Optional.empty()));
        assertEquals(Optional.empty(), Refusal.fromWire(0));
        for (Refusal r : Refusal.values()) {
            assertEquals(Optional.of(r), Refusal.fromWire(Refusal.wire(Optional.of(r))), r.name());
        }
        assertEquals(Refusal.values().length, Refusal.wire(Optional.of(Refusal.values()[Refusal.values().length - 1])), "the largest wire number is values().length");
        assertEquals(Optional.empty(), Refusal.fromWire(Refusal.values().length + 1), "a number past the end is nothing, never a crash");
        assertEquals(Optional.empty(), Refusal.fromWire(-1));
    }

    @Test
    void everyReasonHasItsOwnWord() {
        for (String reason : new String[] {"not_held", "left", "leader_holds", "offline", "dead", "not_day_walker", "forfeit_challenger_away"}) {
            assertTrue(ContestRules.REASONS.contains(reason), reason);
        }
        assertEquals(7, ContestRules.REASONS.size());
    }

    @Test
    void dueAtTheNextMidnightAfterTheMinimum() {
        // started 17 990: the 2 minute minimum puts the earliest at 20 390, so the end is the midnight at 42 000
        assertEquals(42_000, ContestRules.endsAt(17_990, 2));
        assertFalse(ContestRules.due(17_990, 18_000, 0, 1000, 2, 1), "the midnight 10 ticks after the start is too soon");
        assertFalse(ContestRules.due(17_990, 41_999, 0, 1000, 2, 1));
        assertTrue(ContestRules.due(17_990, 42_000, 0, 1000, 2, 1));
        assertTrue(ContestRules.due(17_990, 60_000, 0, 1000, 2, 1), "a jump over it also ends it");
    }

    @Test
    void dueAtTheFirstMidnightWhenFarFromIt() {
        assertEquals(18_000, ContestRules.endsAt(6_000, 2));
        assertFalse(ContestRules.due(6_000, 17_999, 0, 1000, 2, 1));
        assertTrue(ContestRules.due(6_000, 18_000, 0, 1000, 2, 1));
        assertEquals(18_000, ContestRules.endsAt(15_600, 2), "15 600 + 2 400 lands exactly on midnight");
        assertEquals(18_000, ContestRules.endsAt(6_000, 0));
    }

    @Test
    void dueByWallClockWhenTheTimeStands() {
        long hour = 60L * 60 * 1000;
        assertFalse(ContestRules.due(6_000, 6_000, 0, hour - 1, 2, 1));
        assertTrue(ContestRules.due(6_000, 6_000, 0, hour, 2, 1));
        assertFalse(ContestRules.due(6_000, 6_000, 0, 2 * hour - 1, 2, 2));
    }

    @Test
    void aTimeSetBackwardsDoesNotEndIt() {
        assertFalse(ContestRules.due(30_000, 5_000, 0, 1000, 2, 1));
    }

    @Test
    void autoStartAtTheDeadline() {
        assertFalse(ContestRules.autoStart(1000, 999));
        assertTrue(ContestRules.autoStart(1000, 1000));
        assertTrue(ContestRules.autoStart(1000, 5000));
    }
}
