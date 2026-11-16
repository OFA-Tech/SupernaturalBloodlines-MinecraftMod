package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Panel tester (round 3): an unanswered challenge that already ran out must not keep the leader busy before the sweep. */
class DuelBoardPanelTest {
    private static final UUID CHALLENGER = new UUID(0, 2);
    private static final UUID LEADER = new UUID(0, 1);
    private static final UUID THIRD = new UUID(0, 3);

    @Test
    void anExpiredUnansweredChallengeDoesNotBlockTheLeaderBeforeTheSweep() {
        DuelBoard b = new DuelBoard();
        assertTrue(b.challenge(CHALLENGER, LEADER, 0).isEmpty());
        long afterTimeout = DuelBoard.TIMEOUT_TICKS + 1;
        // no expire() yet (PackDuel.check runs every 20 ticks): the old challenge has run out and must not count as busy
        assertTrue(b.challenge(THIRD, LEADER, afterTimeout).isEmpty(), "the leader was busy with a challenge that had already run out");
    }
}
