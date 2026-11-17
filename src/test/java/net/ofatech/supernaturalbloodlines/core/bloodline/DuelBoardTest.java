package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.DuelBoard.Refusal;
import org.junit.jupiter.api.Test;

class DuelBoardTest {
    private static final UUID LEADER = new UUID(0, 1);
    private static final UUID CHALLENGER = new UUID(0, 2);
    private static final UUID THIRD = new UUID(0, 3);

    private static boolean challenge(DuelBoard b, UUID c, UUID l, long now) {
        return b.challenge(c, l, now).isEmpty();
    }

    @Test
    void aChallengeIsAcceptedOnlyByTheLeaderItWasAimedAtAndOnlyOnTime() {
        DuelBoard b = new DuelBoard();
        assertTrue(challenge(b, CHALLENGER, LEADER, 100));
        assertTrue(b.accept(THIRD, 110).isEmpty(), "somebody else accepted");
        assertTrue(b.accept(LEADER, 100 + DuelBoard.TIMEOUT_TICKS + 1).isEmpty(), "accepted after it timed out");
        assertTrue(b.accept(LEADER, 100 + DuelBoard.TIMEOUT_TICKS).isPresent(), "accepted on the last tick");
    }

    @Test
    void aDuelIsNotOnUntilAcceptedAndSettlesOnceOnTheFirstDefeat() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        assertTrue(b.defeat(LEADER, 5).isEmpty(), "a challenge nobody accepted ended in a defeat");
        b.accept(LEADER, 10);
        DuelBoard.Result r = b.defeat(LEADER, 20).orElseThrow();
        assertEquals(CHALLENGER, r.winner());
        assertEquals(LEADER, r.loser());
        assertTrue(r.challengerWon());
        assertTrue(b.defeat(CHALLENGER, 21).isEmpty() && b.all().isEmpty(), "a settled duel settled again");
    }

    @Test
    void theLeaderWhoWinsIsTheWinner() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        assertFalse(b.defeat(CHALLENGER, 2).orElseThrow().challengerWon());
    }

    @Test
    void nobodyIsInTwoThingsAtOnce() {
        DuelBoard b = new DuelBoard();
        assertEquals(Optional.of(Refusal.BUSY), b.challenge(LEADER, LEADER, 0), "challenged themselves");
        assertTrue(challenge(b, CHALLENGER, LEADER, 0));
        assertEquals(Optional.of(Refusal.BUSY), b.challenge(THIRD, LEADER, 0), "a second challenge for the same leader");
        assertEquals(Optional.of(Refusal.BUSY), b.challenge(CHALLENGER, THIRD, 0), "the challenger challenged twice");
    }

    @Test
    void aDuelRunsOutAndAnyoneLeavingCallsItOff() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 50);
        assertTrue(b.expire(50 + DuelBoard.TIMEOUT_TICKS).isEmpty());
        assertEquals(1, b.expire(50 + DuelBoard.TIMEOUT_TICKS + 1).size());
        assertTrue(b.all().isEmpty());

        // a duel that was on and ran out bars the same two like a settled one (waiting it out must not save anybody)
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(CHALLENGER, LEADER, 10_000));

        b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        assertEquals(1, b.cancelInvolving(CHALLENGER).size());
        assertTrue(b.on(LEADER).isEmpty(), "the leader is still in a duel their challenger left");
        challenge(b, CHALLENGER, LEADER, 0);
        assertEquals(1, b.cancelInvolving(LEADER).size(), "a challenge nobody accepted yet is called off too");
    }

    @Test
    void aSettledDuelBarsTheSamePairForTenMinutesWhoeverAsks() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        b.defeat(LEADER, 100);
        long until = 100 + DuelBoard.PAIR_COOLDOWN_TICKS;
        assertEquals(Optional.of(Refusal.PAIR_COOLDOWN), b.challenge(CHALLENGER, LEADER, until - 1), "challenged again at once");
        assertEquals(Optional.of(Refusal.PAIR_COOLDOWN), b.challenge(LEADER, CHALLENGER, until - 1), "the other way round is the same pair");
        assertTrue(challenge(b, THIRD, LEADER, until - 1), "somebody else is not barred");
        b.cancelInvolving(THIRD);
        assertTrue(challenge(b, CHALLENGER, LEADER, until), "still barred after ten minutes");
    }

    @Test
    void aChallengeNobodyAnsweredBarsThatChallengerFromTheSameLeaderForFiveMinutes() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        long ran = DuelBoard.TIMEOUT_TICKS + 1;
        assertEquals(1, b.expire(ran).size());
        long until = ran + DuelBoard.RETRY_COOLDOWN_TICKS;
        assertEquals(Optional.of(Refusal.RETRY_COOLDOWN), b.challenge(CHALLENGER, LEADER, until - 1), "asked again at once");
        assertTrue(challenge(b, THIRD, LEADER, until - 1), "somebody else asked");
        b.cancelInvolving(THIRD);
        assertTrue(challenge(b, CHALLENGER, THIRD, until - 1), "the same challenger asked someone else");
        b.cancelInvolving(THIRD);
        assertTrue(challenge(b, CHALLENGER, LEADER, until), "still barred after five minutes");
    }

    @Test
    void decliningRemovesTheChallengeAndBarsRetriesTheSameWay() {
        DuelBoard b = new DuelBoard();
        assertTrue(b.decline(LEADER, 0).isEmpty(), "declined a challenge nobody made");
        challenge(b, CHALLENGER, LEADER, 0);
        assertTrue(b.decline(CHALLENGER, 5).isEmpty(), "the challenger declined their own challenge");
        assertTrue(b.decline(LEADER, 5).isPresent());
        assertTrue(b.all().isEmpty());
        assertEquals(Optional.of(Refusal.RETRY_COOLDOWN), b.challenge(CHALLENGER, LEADER, 6));
    }

    @Test
    void aGuardStopsOnlyTheWinnerAndOnlyForAMoment() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        b.defeat(LEADER, 100);
        assertTrue(b.guarded(LEADER, CHALLENGER, 100 + DuelBoard.GUARD_TICKS - 1), "the winner could finish the loser");
        assertFalse(b.guarded(LEADER, THIRD, 101), "a guard stopped somebody who is not the winner");
        assertFalse(b.guarded(CHALLENGER, LEADER, 101), "the winner is guarded too");
        assertFalse(b.guarded(LEADER, CHALLENGER, 100 + DuelBoard.GUARD_TICKS), "the guard never ends");
    }

    @Test
    void aDuelThatWasOnAndRanOutBarsTheSamePairLikeASettledOne() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 10);
        long out = 10 + DuelBoard.TIMEOUT_TICKS + 1;
        assertEquals(1, b.expire(out).size());
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(CHALLENGER, LEADER, out + 5), "waiting a duel out is a way to save a life");
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(LEADER, CHALLENGER, out + 5));
        assertTrue(challenge(b, CHALLENGER, LEADER, out + DuelBoard.PAIR_COOLDOWN_TICKS));
    }

    @Test
    void aChallengeThatRanOutIsSweptWhenSomebodyAsksAndStillReportedOnce() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        long after = DuelBoard.TIMEOUT_TICKS + 1;
        assertTrue(challenge(b, THIRD, LEADER, after), "the old challenge kept the leader busy");
        assertEquals(1, b.expire(after).size(), "the one whose challenge ran out must still be told");
        assertTrue(b.expire(after).isEmpty(), "it came twice");
        b.cancelInvolving(THIRD);
        assertEquals(Optional.of(Refusal.RETRY_COOLDOWN), b.challenge(CHALLENGER, LEADER, after + 1), "and waits before asking again");
    }

    @Test
    void theCooldownLeftIsTheLongerOfThePairAndTheRetryWait() {
        DuelBoard b = new DuelBoard();
        assertEquals(0, b.cooldownLeft(CHALLENGER, LEADER, 0));
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        b.defeat(LEADER, 100);
        assertEquals(DuelBoard.PAIR_COOLDOWN_TICKS - 50, b.cooldownLeft(CHALLENGER, LEADER, 150));
        assertEquals(DuelBoard.PAIR_COOLDOWN_TICKS - 50, b.cooldownLeft(LEADER, CHALLENGER, 150), "the pair is the same either way round");
        assertEquals(0, b.cooldownLeft(CHALLENGER, LEADER, 100 + DuelBoard.PAIR_COOLDOWN_TICKS + 1));
    }

    @Test
    void aBlowCountsOnlyFromTheOpponentOfADuelThatIsOnAndOnlyForTenSeconds() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.recordHit(LEADER, CHALLENGER, 5);
        assertFalse(b.hasCredit(LEADER, CHALLENGER, 6), "a blow in a challenge nobody accepted is no credit");
        b.accept(LEADER, 10);
        b.recordHit(LEADER, THIRD, 20);
        assertFalse(b.hasCredit(LEADER, CHALLENGER, 21), "a blow from a third player is nobody's credit");
        assertFalse(b.hasCredit(LEADER, THIRD, 21));
        b.recordHit(LEADER, CHALLENGER, 100);
        assertTrue(b.hasCredit(LEADER, CHALLENGER, 100 + DuelBoard.COMBAT_CREDIT_TICKS), "the last tick of the window");
        assertFalse(b.hasCredit(LEADER, CHALLENGER, 100 + DuelBoard.COMBAT_CREDIT_TICKS + 1), "one tick too late");
        assertFalse(b.hasCredit(CHALLENGER, LEADER, 101), "the credit is the victim's, not the attacker's");
    }

    @Test
    void theHitsAreForgottenWhenTheDuelEnds() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        b.recordHit(LEADER, CHALLENGER, 5);
        b.cancelInvolving(LEADER);
        assertFalse(b.hasCredit(LEADER, CHALLENGER, 6), "a blow of a duel that was called off counted in the next one");
        challenge(b, CHALLENGER, LEADER, 10_000_000);
        b.accept(LEADER, 10_000_001);
        b.recordHit(LEADER, CHALLENGER, 10_000_002);
        b.defeat(LEADER, 10_000_003);
        assertFalse(b.hasCredit(LEADER, CHALLENGER, 10_000_004), "a settled duel left its blows behind");
    }

    @Test
    void barringThePairStopsEitherOfThemChallenging() {
        DuelBoard b = new DuelBoard();
        b.bar(CHALLENGER, LEADER, 100);
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(CHALLENGER, LEADER, 101));
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(LEADER, CHALLENGER, 101));
        assertTrue(challenge(b, THIRD, LEADER, 101));
    }

    @Test
    void aBarredPairThatNeverFoughtIsToldItCalledTheDuelOff() {
        DuelBoard b = new DuelBoard();
        b.bar(CHALLENGER, LEADER, 100);
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), b.challenge(CHALLENGER, LEADER, 101));
        // a settled duel after it is called what it is
        DuelBoard c = new DuelBoard();
        challenge(c, CHALLENGER, LEADER, 0);
        c.accept(LEADER, 1);
        c.defeat(LEADER, 5);
        assertEquals(Optional.of(Refusal.PAIR_COOLDOWN), c.challenge(CHALLENGER, LEADER, 6));
        // an on duel that ran out was not fought to a defeat either
        DuelBoard d = new DuelBoard();
        challenge(d, CHALLENGER, LEADER, 0);
        d.accept(LEADER, 1);
        d.expire(1 + DuelBoard.TIMEOUT_TICKS + 1);
        assertEquals(Optional.of(Refusal.PAIR_CALLED_OFF), d.challenge(CHALLENGER, LEADER, 1 + DuelBoard.TIMEOUT_TICKS + 2));
    }

    @Test
    void noCooldownMeansZeroMinutesLeftNotAnOverflow() {
        DuelBoard b = new DuelBoard();
        assertEquals(0, b.cooldownLeft(CHALLENGER, LEADER, 0));
        assertEquals(0, b.cooldownLeft(CHALLENGER, LEADER, 1_000_000_000L), "a pair that was never barred has time left");
    }

    @Test
    void aPausedDuelHasNoClockAndResumesWithTheTimeItHadWhenTheDuelistLeft() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 10); // runs until 10 + TIMEOUT
        assertTrue(b.pause(CHALLENGER, 100, 1200), "the duel on was not paused");
        assertTrue(b.expire(10 + DuelBoard.TIMEOUT_TICKS + 5_000).isEmpty(), "a paused duel ran out of time");
        assertTrue(b.on(LEADER).isPresent(), "a paused duel is gone");
        // back after 600 ticks: the wait is not counted against the 2 minutes
        assertTrue(b.resume(CHALLENGER, 700).isPresent());
        assertTrue(b.expire(10 + DuelBoard.TIMEOUT_TICKS + 599).isEmpty(), "the wait was counted against the duel");
        assertEquals(1, b.expire(10 + DuelBoard.TIMEOUT_TICKS + 601).size(), "it never runs out after the resume");
    }

    @Test
    void aWindowThatRanOutIsReportedOnceAndCannotBeResumedAfterwards() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        b.pause(LEADER, 100, 200); // until 300
        assertTrue(b.expiredPauses(299).isEmpty(), "reported before the window ended");
        assertTrue(b.resume(LEADER, 300).isEmpty(), "resumed on the tick the window ended");
        assertEquals(1, b.expiredPauses(300).size());
        assertEquals(LEADER, b.expiredPauses(300).get(0).pausedBy());
        assertTrue(b.defeat(LEADER, 301).isPresent(), "a paused duel cannot be lost");
        assertTrue(b.expiredPauses(302).isEmpty(), "reported again after it was settled");
    }

    @Test
    void onlyOneDuelistCanBeAwayAndOnlyForADuelThatIsOn() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        assertFalse(b.pause(CHALLENGER, 5, 100), "a challenge nobody accepted was paused");
        b.accept(LEADER, 6);
        assertTrue(b.pause(CHALLENGER, 7, 100));
        assertFalse(b.pause(LEADER, 8, 100), "the second to leave was waited for too");
        assertTrue(b.resume(LEADER, 9).isEmpty(), "somebody who never left resumed it");
        assertFalse(b.pause(THIRD, 8, 100), "a third player paused a duel");
    }

    @Test
    void eachDuelistHasOneWaitPerDuelAndANewDuelStartsFresh() {
        DuelBoard b = new DuelBoard();
        challenge(b, CHALLENGER, LEADER, 0);
        b.accept(LEADER, 1);
        assertFalse(b.on(CHALLENGER).orElseThrow().pauseSpent(CHALLENGER));
        assertTrue(b.pause(CHALLENGER, 5, 100));
        assertTrue(b.on(CHALLENGER).orElseThrow().pauseSpent(CHALLENGER), "the wait was not counted");
        assertTrue(b.resume(CHALLENGER, 10).isPresent());
        assertFalse(b.pause(CHALLENGER, 20, 100), "a second wait was given to the same duelist");
        assertTrue(b.pause(LEADER, 21, 100), "the other duelist's own wait was taken from them");
        b.clear();
        challenge(b, CHALLENGER, LEADER, 1000);
        b.accept(LEADER, 1001);
        assertTrue(b.pause(CHALLENGER, 1002, 100), "a new duel did not give a new wait");
    }
}
