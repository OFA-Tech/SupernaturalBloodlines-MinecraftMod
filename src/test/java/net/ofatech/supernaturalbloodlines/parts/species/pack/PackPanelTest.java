package net.ofatech.supernaturalbloodlines.parts.species.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackLeadership;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import org.junit.jupiter.api.Test;

/** Panel tester (round 4): pure checks for two round-3 rules whose only other coverage is a game test. */
class PackPanelTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID M = new UUID(0, 2);
    private static final UUID B = new UUID(0, 3);
    private static final UUID B1 = new UUID(0, 4);
    private static final UUID B2 = new UUID(0, 5);

    @Test
    void joiningAnotherPackKeepsTheGraceOfTheOneWhoLeftByChoice() {
        long[] clock = {5_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            PackIndex i = new PackIndex();
            i.join(A, M);
            i.leaveVoluntarily(M);
            assertTrue(i.killsInPack(M, A, 10), "a moment after leaving the grace still holds");
            i.join(B, M);
            // round 4: joining a friend's pack must not free a kill on the one they left, or leaving and joining would be a loophole
            assertTrue(i.killsInPack(M, A, 10), "a join must not clear the leave: the grace holds until it ends");
            i.dissolve(B);
            assertTrue(i.killsInPack(M, A, 10), "and a dissolved friend's pack changes nothing about it");
            assertFalse(i.killsInPack(M, A, 0), "except with the switch off");
        } finally {
            PackIndex.overrideClock(null);
        }
    }

    @Test
    void theAbsentSuccessorFilterSkipsAnAlphaWhoIsNotHere() {
        PackIndex i = new PackIndex();
        i.join(A, B1);
        i.join(A, B2);
        i.setStrength(B1, PackLeadership.strength(Rank.ALPHA, 50));
        i.setStrength(B2, PackLeadership.strength(Rank.ALPHA, 10));
        assertEquals(B1, SparkHandler.coAlphaHeir(i, A), "without a filter the strongest second Alpha leads");
        assertEquals(B2, SparkHandler.coAlphaHeir(i, A, id -> id.equals(B2)), "the absent strongest one must be skipped");
        assertNull(SparkHandler.coAlphaHeir(i, A, id -> false), "nobody here means nobody takes the lead");
    }
}
