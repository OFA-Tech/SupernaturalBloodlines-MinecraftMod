package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DuelBoardTest {
    private static final UUID LEADER = new UUID(0, 1);
    private static final UUID CHALLENGER = new UUID(0, 2);
    private static final UUID THIRD = new UUID(0, 3);

    @Test
    void aChallengeIsAcceptedOnlyByTheLeaderItWasAimedAtAndOnlyOnTime() {
        DuelBoard b = new DuelBoard();
        assertTrue(b.challenge(CHALLENGER, LEADER, 100));
        assertTrue(b.accept(THIRD, 110).isEmpty(), "somebody else accepted");
        assertTrue(b.accept(LEADER, 100 + DuelBoard.TIMEOUT_TICKS + 1).isEmpty(), "accepted after it timed out");
        assertTrue(b.accept(LEADER, 100 + DuelBoard.TIMEOUT_TICKS).isPresent(), "accepted on the last tick");
    }

    @Test
    void aDuelIsNotOnUntilAcceptedAndSettlesOnceOnTheFirstDefeat() {
        DuelBoard b = new DuelBoard();
        b.challenge(CHALLENGER, LEADER, 0);
        assertTrue(b.defeat(LEADER).isEmpty(), "a challenge nobody accepted ended in a defeat");
        b.accept(LEADER, 10);
        DuelBoard.Result r = b.defeat(LEADER).orElseThrow();
        assertEquals(CHALLENGER, r.winner());
        assertEquals(LEADER, r.loser());
        assertTrue(r.challengerWon());
        assertTrue(b.defeat(CHALLENGER).isEmpty() && b.all().isEmpty(), "a settled duel settled again");

        b.challenge(CHALLENGER, LEADER, 20);
        b.accept(LEADER, 21);
        assertFalse(b.defeat(CHALLENGER).orElseThrow().challengerWon(), "the leader who wins is the leader still");
    }

    @Test
    void nobodyIsInTwoThingsAtOnce() {
        DuelBoard b = new DuelBoard();
        assertFalse(b.challenge(LEADER, LEADER, 0), "challenged themselves");
        assertTrue(b.challenge(CHALLENGER, LEADER, 0));
        assertFalse(b.challenge(THIRD, LEADER, 0), "a second challenge for the same leader");
        assertFalse(b.challenge(CHALLENGER, THIRD, 0), "the challenger challenged twice");
    }

    @Test
    void aDuelRunsOutAndAnyoneLeavingCallsItOff() {
        DuelBoard b = new DuelBoard();
        b.challenge(CHALLENGER, LEADER, 0);
        b.accept(LEADER, 50);
        assertTrue(b.expire(50 + DuelBoard.TIMEOUT_TICKS).isEmpty());
        assertEquals(1, b.expire(50 + DuelBoard.TIMEOUT_TICKS + 1).size());
        assertTrue(b.all().isEmpty());

        b.challenge(CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        assertEquals(1, b.cancelInvolving(CHALLENGER).size());
        assertTrue(b.on(LEADER).isEmpty(), "the leader is still in a duel their challenger left");
        b.challenge(CHALLENGER, LEADER, 0);
        assertEquals(1, b.cancelInvolving(LEADER).size(), "a challenge nobody accepted yet is called off too");
    }
}