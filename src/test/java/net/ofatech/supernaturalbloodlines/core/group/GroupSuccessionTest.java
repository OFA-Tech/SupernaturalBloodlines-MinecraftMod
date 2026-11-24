package net.ofatech.supernaturalbloodlines.core.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.group.GroupSuccession.Candidate;
import org.junit.jupiter.api.Test;

/** R244 per type. */
class GroupSuccessionTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);

    private static Candidate c(UUID id, GroupRank rank, long joined, boolean here, int mark) {
        return new Candidate(id, rank, joined, here, mark);
    }

    @Test
    void aClanTakesTheLongestDayWalkerWhoIsHere() {
        List<Candidate> m = List.of(c(A, GroupRank.DAY_WALKER, 50, true, -1), c(B, GroupRank.DAY_WALKER, 10, true, -1), c(C, GroupRank.NONE, 1, true, -1));
        assertEquals(Optional.of(B), GroupSuccession.heir(GroupType.CLAN, m));
    }

    @Test
    void aNightWalkerIsNeverAnHeir() {
        assertTrue(GroupSuccession.heir(GroupType.CLAN, List.of(c(A, GroupRank.NONE, 1, true, -1), c(B, GroupRank.ELDER, 1, true, -1))).isEmpty());
    }

    @Test
    void anAbsentHeirIsSkipped() {
        List<Candidate> m = List.of(c(A, GroupRank.DAY_WALKER, 50, true, -1), c(B, GroupRank.DAY_WALKER, 10, false, -1));
        assertEquals(Optional.of(A), GroupSuccession.heir(GroupType.CLAN, m));
        assertTrue(GroupSuccession.heir(GroupType.CLAN, List.of(c(A, GroupRank.DAY_WALKER, 50, false, -1), c(B, GroupRank.DAY_WALKER, 10, false, -1))).isEmpty());
    }

    @Test
    void aCovenTakesTheLongestElder() {
        List<Candidate> m = List.of(c(A, GroupRank.ELDER, 50, true, -1), c(B, GroupRank.ELDER, 10, true, -1), c(C, GroupRank.DAY_WALKER, 1, true, -1));
        assertEquals(Optional.of(B), GroupSuccession.heir(GroupType.COVEN, m));
    }

    @Test
    void aPackIsLeftToTheSparkRule() {
        assertTrue(GroupSuccession.heir(GroupType.PACK, List.of(c(A, GroupRank.NONE, 1, true, 9))).isEmpty());
    }

    @Test
    void aBrotherhoodTakesTheHighestOnlineMark() {
        List<Candidate> m = List.of(c(A, GroupRank.NONE, 1, true, 3), c(B, GroupRank.NONE, 9, true, 7), c(C, GroupRank.NONE, 5, true, 5));
        assertEquals(Optional.of(B), GroupSuccession.heir(GroupType.BROTHERHOOD, m));
    }

    @Test
    void anOfflineMarkNeverInherits() {
        List<Candidate> m = List.of(c(A, GroupRank.NONE, 1, true, 3), c(B, GroupRank.NONE, 9, true, -1));
        assertEquals(Optional.of(A), GroupSuccession.heir(GroupType.BROTHERHOOD, m));
        assertTrue(GroupSuccession.heir(GroupType.BROTHERHOOD, List.of(c(B, GroupRank.NONE, 9, true, -1))).isEmpty());
    }

    @Test
    void tiesGoToTheLongestInThenTheLowerId() {
        assertEquals(Optional.of(B), GroupSuccession.heir(GroupType.BROTHERHOOD, List.of(c(A, GroupRank.NONE, 9, true, 4), c(B, GroupRank.NONE, 2, true, 4))));
        assertEquals(Optional.of(A), GroupSuccession.heir(GroupType.CLAN, List.of(c(B, GroupRank.DAY_WALKER, 5, true, -1), c(A, GroupRank.DAY_WALKER, 5, true, -1))));
    }
}
