package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The index's own rules, without a world. */
class PackIndexTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID M = new UUID(0, 4);

    @Test
    void anAllianceIsMadeBothWaysOnceAndNeverWithYourself() {
        PackIndex i = new PackIndex();
        assertFalse(i.ally(A, A), "allied with themselves");
        assertTrue(i.ally(A, B));
        assertFalse(i.ally(A, B), "the same alliance twice");
        assertFalse(i.ally(B, A), "the same alliance from the other side");
        assertTrue(i.alliesOf(A).contains(B) && i.alliesOf(B).contains(A), "not both ways");
        assertTrue(i.unally(B, A));
        assertTrue(i.alliesOf(A).isEmpty() && i.alliesOf(B).isEmpty());
    }

    @Test
    void onlyALeaderAllies() {
        PackIndex i = new PackIndex();
        i.join(A, M);
        assertFalse(i.ally(M, B), "a member allied");
        assertFalse(i.ally(B, M), "allied with a member");
        assertTrue(i.ally(A, B));
        assertTrue(i.alliedPlayers(M, B) && i.alliedPlayers(B, M), "a member is covered by their pack's alliance");
        assertFalse(i.alliedPlayers(A, M), "the same pack is not allied with itself");
    }

    @Test
    void samePackNeedsTwoPlayersInOnePack() {
        PackIndex i = new PackIndex();
        assertFalse(i.samePack(A, A));
        assertFalse(i.samePack(A, B), "two strangers");
        i.join(A, M);
        assertTrue(i.samePack(A, M) && i.samePack(M, A));
        assertFalse(i.samePack(M, M));
        assertFalse(i.samePack(M, B));
        assertTrue(i.isLeader(A) && !i.isLeader(M) && i.isLeader(B));
    }

    @Test
    void aLeaderWhoJoinsBringsTheirMembersAndTheirAlliesGoWithTheLeadership() {
        PackIndex i = new PackIndex();
        i.join(B, M);
        i.ally(B, C);
        assertTrue(i.join(A, B));
        assertEquals(List.of(M, B), i.betasOf(A), "the members come along, the leader after them");
        assertTrue(i.alliesOf(B).isEmpty(), "a member has no alliance of their own");
        // the leadership moves to B: the alliance of the pack goes with it
        i.ally(A, C);
        i.promote(A, B);
        assertTrue(i.alliesOf(B).contains(C) && i.alliesOf(A).isEmpty());
        assertTrue(i.alliedPlayers(A, C), "the old leader is still in the allied pack");
    }
}