package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.LeadByMark.Candidate;
import org.junit.jupiter.api.Test;

class LeadByMarkTest {
    private static final UUID LEADER = new UUID(0, 100);

    private static UUID id(int n) {
        return new UUID(0, n);
    }

    private static Candidate c(int n, int mark) {
        return new Candidate(id(n), mark, true, true, false, 10);
    }

    @Test
    void aStrictlyHigherOnlineMarkTakesIt() {
        assertEquals(Optional.of(id(1)), LeadByMark.pick(LEADER, 2, List.of(c(1, 3))));
    }

    @Test
    void aTieKeepsTheLeader() {
        assertEquals(Optional.empty(), LeadByMark.pick(LEADER, 2, List.of(c(1, 2))));
    }

    @Test
    void anOfflineMemberNeverTakesIt() {
        assertEquals(Optional.empty(), LeadByMark.pick(LEADER, 0, List.of(new Candidate(id(1), 20, false, true, false, 10))));
    }

    @Test
    void aCodeBreakerNeverTakesIt() {
        assertEquals(Optional.empty(), LeadByMark.pick(LEADER, 0, List.of(new Candidate(id(1), 20, true, true, true, 10))));
    }

    @Test
    void aMemberWhoDoesNotFitNeverTakesIt() {
        assertEquals(Optional.empty(), LeadByMark.pick(LEADER, 0, List.of(new Candidate(id(1), 20, true, false, false, 10))));
    }

    @Test
    void theHighestOfSeveralTakesIt() {
        assertEquals(Optional.of(id(2)), LeadByMark.pick(LEADER, 1, List.of(c(1, 3), c(2, 9), c(3, 5))));
    }

    @Test
    void twoTiedAtTheTopTheLongestInTheGroupTakesIt() {
        Candidate older = new Candidate(id(5), 4, true, true, false, 5);
        Candidate newer = new Candidate(id(1), 4, true, true, false, 9);
        assertEquals(Optional.of(id(5)), LeadByMark.pick(LEADER, 1, List.of(newer, older)));
        assertEquals(Optional.of(id(5)), LeadByMark.pick(LEADER, 1, List.of(older, newer)));
    }

    @Test
    void thenTheLowerId() {
        Candidate low = new Candidate(id(1), 4, true, true, false, 5);
        Candidate high = new Candidate(id(9), 4, true, true, false, 5);
        assertEquals(Optional.of(id(1)), LeadByMark.pick(LEADER, 1, List.of(high, low)));
        assertEquals(Optional.of(id(1)), LeadByMark.pick(LEADER, 1, List.of(low, high)));
    }

    @Test
    void aLeaderWhoDroppedToZeroIsPassedByAnyOnlineMark() {
        assertEquals(Optional.of(id(1)), LeadByMark.pick(LEADER, 0, List.of(c(1, 1))));
    }

    @Test
    void aHumanLeaderIsPassedByTheFirstMarkedHunter() {
        assertEquals(Optional.of(id(2)), LeadByMark.pick(LEADER, 0, List.of(c(1, 0), c(2, 1))));
    }

    @Test
    void nobodyHigherChangesNothing() {
        assertTrue(LeadByMark.pick(LEADER, 5, List.of(c(1, 3), c(2, 5))).isEmpty());
    }

    @Test
    void noCandidatesIsEmpty() {
        assertTrue(LeadByMark.pick(LEADER, 0, List.of()).isEmpty());
    }

    @Test
    void theLeaderIsNeverItsOwnSuccessor() {
        assertTrue(LeadByMark.pick(LEADER, 0, List.of(new Candidate(LEADER, 9, true, true, false, 1))).isEmpty());
    }
}