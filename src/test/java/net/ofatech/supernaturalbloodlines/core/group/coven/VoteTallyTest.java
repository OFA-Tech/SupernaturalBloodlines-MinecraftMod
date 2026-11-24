package net.ofatech.supernaturalbloodlines.core.group.coven;

import static net.ofatech.supernaturalbloodlines.core.group.coven.VoteTally.Result.FAILED;
import static net.ofatech.supernaturalbloodlines.core.group.coven.VoteTally.Result.PASSED;
import static net.ofatech.supernaturalbloodlines.core.group.coven.VoteTally.Result.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoteTallyTest {
    @Test
    void aStrictMajorityPasses() {
        assertEquals(PASSED, VoteTally.result(3, 2, 1, true, false));
        assertEquals(PASSED, VoteTally.result(4, 3, 1, true, false));
    }

    @Test
    void aTieFails() {
        assertEquals(FAILED, VoteTally.result(4, 2, 2, true, false));
    }

    @Test
    void nonVotersCountAsNoAfterTheTime() {
        assertEquals(FAILED, VoteTally.result(5, 2, 0, false, true));
        assertEquals(PASSED, VoteTally.result(5, 3, 0, false, true));
    }

    @Test
    void itWaitsUntilEveryoneVotedOrTheTimeIsUp() {
        assertEquals(PENDING, VoteTally.result(3, 2, 0, false, false));
        assertEquals(PASSED, VoteTally.result(3, 2, 1, true, false));
        assertEquals(PASSED, VoteTally.result(3, 2, 0, false, true));
    }

    @Test
    void anElectorateOfTwoIsTheRegentAlone() {
        assertFalse(VoteTally.needsVote(1));
        assertFalse(VoteTally.needsVote(2));
        assertTrue(VoteTally.needsVote(3));
    }

    @Test
    void noElectionWithAnElectorateUnderThree() {
        assertFalse(VoteTally.mayCallElection(2));
        assertTrue(VoteTally.mayCallElection(3));
    }

    @Test
    void anExpiredEmptyVoteFails() {
        assertEquals(FAILED, VoteTally.result(3, 0, 0, false, true));
    }
}