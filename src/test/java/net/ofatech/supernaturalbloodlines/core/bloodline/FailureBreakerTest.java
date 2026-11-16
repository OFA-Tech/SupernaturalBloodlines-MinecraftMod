package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FailureBreakerTest {
    @Test
    void oneFailureDoesNotTripIt() {
        FailureBreaker b = new FailureBreaker(5);
        assertFalse(b.record(false));
        assertFalse(b.tripped());
    }

    @Test
    void aSuccessStartsTheCountAgain() {
        FailureBreaker b = new FailureBreaker(3);
        for (int i = 0; i < 20; i++) {
            b.record(false);
            b.record(false);
            b.record(true);
        }
        assertFalse(b.tripped(), "failures that never come three in a row never trip it");
    }

    @Test
    void consecutiveFailuresTripItOnceAndForGood() {
        FailureBreaker b = new FailureBreaker(3);
        assertFalse(b.record(false));
        assertFalse(b.record(false));
        assertTrue(b.record(false), "the call that trips it says so, so the caller logs once");
        assertTrue(b.tripped());
        assertFalse(b.record(false), "not again");
        b.record(true);
        assertTrue(b.tripped(), "and a later success does not bring it back");
    }
}