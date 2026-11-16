package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Panel tester: the duel's promote must move every member to the winner, not only the leader's own slot. */
class PackPromoteRepointTest {
    private static final UUID OLD = new UUID(0, 1);
    private static final UUID NEXT = new UUID(0, 2);
    private static final UUID MEMBER = new UUID(0, 3);

    @Test
    void promoteRepointsEveryMemberAndDemotesTheOldLeader() {
        PackIndex i = new PackIndex();
        i.join(OLD, MEMBER);
        i.join(OLD, NEXT);
        i.promote(OLD, NEXT);

        assertTrue(i.isLeader(NEXT), "the winner leads");
        assertFalse(i.isLeader(OLD), "the old leader is a member now");
        assertEquals(NEXT, i.leaderOf(OLD), "the old leader follows the winner");
        assertEquals(NEXT, i.leaderOf(MEMBER), "a member of the old pack did not follow the winner");
        assertEquals(NEXT, i.leaderOf(NEXT));
        assertEquals(Set.of(OLD, MEMBER), Set.copyOf(i.betasOf(NEXT)), "the winner's pack is not the old pack");
        assertTrue(i.betasOf(OLD).isEmpty(), "the old leader still has members");
    }
}
