package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.BossFocus.Candidate;
import org.junit.jupiter.api.Test;

class BossFocusTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final long NEVER = BossFocus.NEVER;

    private static Candidate c(UUID id, double dist, long lastHit) {
        return new Candidate(id, dist * dist, lastHit);
    }

    @Test
    void noBossesNoFocus() {
        assertNull(BossFocus.choose(List.of(), null, 0, 100));
        assertNull(BossFocus.choose(List.of(), A, 0, 100));
    }

    @Test
    void withoutHitsTheNearestBossIsTheFocus() {
        assertEquals(B, BossFocus.choose(List.of(c(A, 30, NEVER), c(B, 10, NEVER), c(C, 20, NEVER)), null, 0, 100));
    }

    @Test
    void theBossHitLastBeatsTheNearest() {
        assertEquals(A, BossFocus.choose(List.of(c(A, 50, 90), c(B, 5, NEVER)), null, 0, 100));
        assertEquals(C, BossFocus.choose(List.of(c(A, 50, 80), c(B, 5, 70), c(C, 60, 95)), null, 0, 100), "the most recent of several");
    }

    @Test
    void aHitOlderThanFifteenSecondsIsForgottenAndNearestRulesAgain() {
        assertEquals(A, BossFocus.choose(List.of(c(A, 50, 100), c(B, 5, NEVER)), null, 0, 100 + BossFocus.HIT_WINDOW), "exactly on the edge it still counts");
        assertEquals(B, BossFocus.choose(List.of(c(A, 50, 100), c(B, 5, NEVER)), null, 0, 100 + BossFocus.HIT_WINDOW + 1), "one tick after it does not");
    }

    @Test
    void aHitOnAnotherBossSwitchesAtOnceEvenInsideTheTwoSecondHold() {
        // A became the focus at tick 100; at tick 105 the player hits B
        assertEquals(B, BossFocus.choose(List.of(c(A, 5, 90), c(B, 40, 105)), A, 100, 105));
    }

    @Test
    void theCurrentBossStaysWhileItIsStillTheOneHitLast() {
        assertEquals(A, BossFocus.choose(List.of(c(A, 50, 200), c(B, 5, 150)), A, 100, 210));
    }

    @Test
    void withoutAHitASwitchWaitsTheTwoSecondHoldAndNeedsABossClearlyNearer() {
        List<Candidate> nearerB = List.of(c(A, 20, NEVER), c(B, 10, NEVER));
        assertEquals(A, BossFocus.choose(nearerB, A, 100, 100 + BossFocus.MIN_HOLD - 1), "inside the hold");
        assertEquals(B, BossFocus.choose(nearerB, A, 100, 100 + BossFocus.MIN_HOLD), "after it");
        assertEquals(A, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 9, NEVER)), A, 100, 1000), "a little nearer is not enough");
        assertEquals(A, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 8.1, NEVER)), A, 100, 1000), "19 % nearer is not enough");
        assertEquals(B, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 7.9, NEVER)), A, 100, 1000), "21 % nearer is");
    }

    @Test
    void aCurrentBossThatIsGoneIsReplacedAtOnceWithoutWaitingTheHold() {
        assertEquals(B, BossFocus.choose(List.of(c(B, 10, NEVER), c(C, 30, NEVER)), A, 100, 101));
        assertEquals(C, BossFocus.choose(List.of(c(B, 10, 50), c(C, 30, 99)), A, 100, 101), "by the hit rule when there is one");
    }

    @Test
    void aTieOnTheSameTickKeepsTheCurrentFocusWhateverItsId() {
        // B is the current focus and has the higher id: two hits on the same tick, and two bosses at the same distance, do not take the focus from it
        assertEquals(B, BossFocus.choose(List.of(c(A, 10, 90), c(B, 10, 90)), B, 50, 100), "the same hit tick");
        assertEquals(B, BossFocus.choose(List.of(c(B, 10, 90), c(A, 10, 90)), B, 50, 100), "the same hit tick, other order");
        assertEquals(B, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 10, NEVER)), B, 0, 1000), "the same distance");
        assertEquals(A, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 10, NEVER)), A, 0, 1000));
        assertEquals(A, BossFocus.choose(List.of(c(B, 10, 90), c(A, 10, 90)), C, 0, 100), "no current among them: the lower id, as before");
    }

    @Test
    void anUnknownDistanceIsFarAndTiesAreBrokenTheSameWayEveryTime() {
        assertEquals(B, BossFocus.choose(List.of(new Candidate(A, BossFocus.UNKNOWN, NEVER), c(B, 40, NEVER)), null, 0, 0));
        assertEquals(A, BossFocus.choose(List.of(c(B, 10, NEVER), c(A, 10, NEVER)), null, 0, 0), "equal distances: the lower id, whatever the order");
        assertEquals(A, BossFocus.choose(List.of(c(A, 10, NEVER), c(B, 10, NEVER)), null, 0, 0));
        assertEquals(A, BossFocus.choose(List.of(c(B, 10, 50), c(A, 10, 50)), null, 0, 60), "equal hits too");
    }
}
