package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.NarrationGate.Say;
import net.ofatech.supernaturalbloodlines.parts.bosses.client.NarrationGate.State;
import org.junit.jupiter.api.Test;

class NarrationGateTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    /** Plays a list of (tick, set, focus) through the gate the way the client does, and returns what was said at each step. */
    private static Say[] play(Object[][] steps) {
        State state = null;
        Say[] said = new Say[steps.length];
        for (int i = 0; i < steps.length; i++) {
            long now = (long) steps[i][0];
            @SuppressWarnings("unchecked")
            Set<UUID> set = (Set<UUID>) steps[i][1];
            UUID focus = (UUID) steps[i][2];
            said[i] = NarrationGate.decide(state, set, focus, now);
            state = NarrationGate.after(state, said[i], set, focus, now);
        }
        return said;
    }

    @Test
    void theRowIsSaidOnceWhenItFirstAppearsAndNotAgainWhileNothingChanges() {
        Say[] said = play(new Object[][] {{0L, Set.of(A, B), A}, {1L, Set.of(A, B), A}, {200L, Set.of(A, B), A}, {5000L, Set.of(A, B), A}});
        assertEquals(Say.ROW, said[0]);
        assertEquals(Say.NONE, said[1]);
        assertEquals(Say.NONE, said[2]);
        assertEquals(Say.NONE, said[3]);
    }

    @Test
    void aBossComingOrGoingSaysTheWholeRowAtOnceEvenInsideTheHold() {
        Say[] said = play(new Object[][] {{0L, Set.of(A, B), A}, {5L, Set.of(A, B, C), A}, {6L, Set.of(A, C), A}});
        assertEquals(Say.ROW, said[0]);
        assertEquals(Say.ROW, said[1], "a boss came");
        assertEquals(Say.ROW, said[2], "a boss went");
    }

    @Test
    void aChangeOfFocusAloneSaysJustTheFocusAndNotWithinTheHold() {
        long t = NarrationGate.MIN_HOLD;
        Say[] said = play(new Object[][] {{0L, Set.of(A, B), A}, {t - 1, Set.of(A, B), B}, {t, Set.of(A, B), B}, {t + 1, Set.of(A, B), B}});
        assertEquals(Say.ROW, said[0]);
        assertEquals(Say.NONE, said[1], "inside the hold: not said");
        assertEquals(Say.FOCUS, said[2], "when the hold is over and the focus still differs: only the focus");
        assertEquals(Say.NONE, said[3], "and not again");
    }

    @Test
    void aFocusThatFlipsBackBeforeTheHoldIsOverIsNeverSaid() {
        Say[] said = play(new Object[][] {{0L, Set.of(A, B), A}, {10L, Set.of(A, B), B}, {20L, Set.of(A, B), A}, {500L, Set.of(A, B), A}});
        assertEquals(Say.NONE, said[1]);
        assertEquals(Say.NONE, said[2], "it is the focus that was last said");
        assertEquals(Say.NONE, said[3]);
    }

    @Test
    void aFlipEveryTickIsSaidAtMostOncePerHold() {
        Object[][] steps = new Object[121][];
        for (int i = 0; i <= 120; i++) {
            steps[i] = new Object[] {(long) i, Set.of(A, B), i % 2 == 0 ? A : B};
        }
        Say[] said = play(steps);
        assertEquals(Say.ROW, said[0]);
        long lastAt = 0;
        int spoken = 1;
        for (int i = 1; i <= 120; i++) {
            if (said[i] != Say.NONE) {
                assertEquals(Say.FOCUS, said[i], "tick " + i);
                assertTrue(i - lastAt >= NarrationGate.MIN_HOLD, "tick " + i + " was said only " + (i - lastAt) + " ticks after the last");
                lastAt = i;
                spoken++;
            }
        }
        assertTrue(spoken >= 3 && spoken <= 1 + 120 / NarrationGate.MIN_HOLD, "a flip every tick was said " + spoken + " times in 120 ticks");
    }

    @Test
    void aChangeOfBothTheSetAndTheFocusIsOneWholeRowNotTwoSayings() {
        Say[] said = play(new Object[][] {{0L, Set.of(A, B), A}, {1L, Set.of(B, C), B}});
        assertEquals(Say.ROW, said[1]);
    }

    @Test
    void noBossesNothingToSayAndTheStateIsForgottenSoTheNextFightIsSaidAgain() {
        assertEquals(Say.NONE, NarrationGate.decide(null, Set.of(), null, 0));
        assertNull(NarrationGate.after(new State(Set.of(A), A, 0), Say.NONE, Set.of(), null, 5));
        Say[] said = play(new Object[][] {{0L, Set.of(A), A}, {10L, Set.of(), null}, {20L, Set.of(A), A}});
        assertEquals(Say.ROW, said[2], "the boss came back after the row was gone");
    }
}
