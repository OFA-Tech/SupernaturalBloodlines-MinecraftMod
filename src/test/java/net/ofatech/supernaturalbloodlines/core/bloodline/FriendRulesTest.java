package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** R144: who never hurts whom. */
class FriendRulesTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void theSamePackAreFriends() {
        assertTrue(FriendRules.friends(A, A, false));
    }

    @Test
    void alliedPacksAreFriends() {
        assertTrue(FriendRules.friends(A, B, true));
    }

    @Test
    void strangersAreNot() {
        assertFalse(FriendRules.friends(A, B, false));
    }

    @Test
    void aSideWithNoPackIsNobodysFriend() {
        assertFalse(FriendRules.friends(null, A, true));
        assertFalse(FriendRules.friends(A, null, true));
        assertFalse(FriendRules.friends(null, null, false));
    }

    /** An NPC side is named by its owner's pack leader: a Brother of the leader, a Brother of a Beta and a Tensura subordinate of the same pack share one. */
    @Test
    void aPacksOwnWolvesAndSubordinatesAreFriendsOfEachOtherAndOfItsAllies() {
        UUID leader = A;
        assertTrue(FriendRules.friends(leader, leader, false), "a Brother of the leader and a Brother of a Beta");
        assertTrue(FriendRules.friends(leader, B, true), "a wolf of the pack and a wolf of an allied pack");
        assertFalse(FriendRules.friends(leader, B, false), "a wolf of another pack is not a friend");
        assertFalse(FriendRules.friends(null, leader, true), "a wild wolf is nobody's friend, even of an allied pack");
    }
}