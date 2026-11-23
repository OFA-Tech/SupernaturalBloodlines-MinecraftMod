package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChangeGateTest {
    @Test
    void sameHashSendsNothingDifferentSends() {
        ChangeGate g = new ChangeGate();
        assertTrue(g.offer(7), "the first one always goes");
        assertFalse(g.offer(7));
        assertFalse(g.offer(7));
        assertTrue(g.offer(8));
        assertFalse(g.offer(8));
        assertTrue(g.offer(7), "going back is a change too");
    }

    @Test
    void resetSendsAgain() {
        ChangeGate g = new ChangeGate();
        g.offer(1);
        g.reset();
        assertTrue(g.offer(1));
    }
}
