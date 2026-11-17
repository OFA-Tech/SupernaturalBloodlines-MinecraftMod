package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership.Candidate;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

class PackLeadershipTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID BETA = new UUID(0, 4);

    private static Candidate alpha(UUID id, int level) {
        return new Candidate(id, PackLeadership.strength(Rank.ALPHA, level));
    }

    @Test
    void aTrueAlphaBeatsAPlainAlphaOfAnyLevel() {
        List<Candidate> c = List.of(alpha(A, 14), new Candidate(B, PackLeadership.strength(Rank.TRUE_ALPHA, 1)));
        assertEquals(Optional.of(B), PackLeadership.pick(c, A));
    }

    @Test
    void theHigherLevelLeadsAmongPlainAlphas() {
        assertEquals(Optional.of(C), PackLeadership.pick(List.of(alpha(A, 5), alpha(B, 7), alpha(C, 12)), A));
    }

    @Test
    void aTieKeepsTheSittingLeaderWhoeverJoinedFirst() {
        List<Candidate> c = List.of(alpha(A, 9), alpha(B, 9));
        assertEquals(Optional.of(B), PackLeadership.pick(c, B));
        assertEquals(Optional.of(A), PackLeadership.pick(c, A));
        assertEquals(Optional.of(A), PackLeadership.pick(c, null), "no sitting leader: the first to join");
    }

    @Test
    void aBetaOrAPackmateNeverLeads() {
        assertEquals(Optional.empty(), PackLeadership.pick(List.of(new Candidate(BETA, PackLeadership.strength(Rank.BETA, 14)), new Candidate(C, 0)), A));
        assertFalse(PackLeadership.isAlpha(PackLeadership.strength(Rank.OMEGA, 14)));
        assertTrue(PackLeadership.isAlpha(PackLeadership.strength(Rank.ALPHA, 0)));
    }

    @Test
    void theLevelIsClampedSoItNeverReachesTheNextRank() {
        assertTrue(PackLeadership.strength(Rank.ALPHA, 5000) < PackLeadership.strength(Rank.TRUE_ALPHA, 0));
        assertEquals(PackLeadership.strength(Rank.ALPHA, 0), PackLeadership.strength(Rank.ALPHA, -3));
    }
}
