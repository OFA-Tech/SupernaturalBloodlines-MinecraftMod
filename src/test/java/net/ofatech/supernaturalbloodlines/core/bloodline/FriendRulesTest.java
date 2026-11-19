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
}