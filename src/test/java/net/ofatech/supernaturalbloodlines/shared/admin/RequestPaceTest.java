package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RequestPaceTest {
    @Test
    void withoutASnapshotItAsksOnceAndThenWaits() {
        assertTrue(RequestPace.due(0, 5000, false, 5000));
        for (long t = 5001; t < 6500; t += 16) {
            assertFalse(RequestPace.due(5000, t, false, t), "asked again at " + t);
        }
        assertTrue(RequestPace.due(5000, 6499, false, 6500));
    }

    @Test
    void anOpenPageWithASnapshotDoesNotAskAndAReopenedOneDoes() {
        assertFalse(RequestPace.due(0, 9000, true, 9016));
        assertTrue(RequestPace.due(0, 1000, true, 9016));
    }

    @Test
    void anOpenPageKeepsAskingEveryTenSeconds() {
        assertFalse(RequestPace.due(5000, 14_990, true, 14_999));
        assertTrue(RequestPace.due(5000, 15_000, true, 15_001), "a long-open page never refreshed");
        assertFalse(RequestPace.due(14_000, 15_000, true, 15_001), "the 1.5 s floor was broken");
    }
}

