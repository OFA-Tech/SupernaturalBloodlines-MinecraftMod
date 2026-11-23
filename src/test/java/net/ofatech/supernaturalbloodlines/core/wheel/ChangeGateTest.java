package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChangeGateTest {
    /** Two values that are different but always hash the same. */
    private record Same(int n) {
        @Override
        public int hashCode() {
            return 1;
        }
    }

    @Test
    void sameListSendsNothingDifferentSends() {
        ChangeGate g = new ChangeGate();
        assertTrue(g.offer(List.of("a")), "the first one always goes");
        assertFalse(g.offer(List.of("a")));
        assertFalse(g.offer(List.of("a")));
        assertTrue(g.offer(List.of("b")));
        assertFalse(g.offer(List.of("b")));
        assertTrue(g.offer(List.of("a")), "going back is a change too");
    }

    @Test
    void aHashCollisionDoesNotHideAChange() {
        ChangeGate g = new ChangeGate();
        assertTrue(g.offer(List.of(new Same(1))));
        assertTrue(g.offer(List.of(new Same(2))), "different lists with the same hash code must still be sent");
        assertFalse(g.offer(List.of(new Same(2))));
    }

    @Test
    void resetSendsAgain() {
        ChangeGate g = new ChangeGate();
        g.offer(List.of(1));
        g.reset();
        assertTrue(g.offer(List.of(1)));
    }
}
