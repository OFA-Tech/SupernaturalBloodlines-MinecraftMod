package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AltHoldTest {
    @Test
    void theDetailComesAfterOneSecondAndNotBefore() {
        assertFalse(AltHold.detailed(100, 100), "the moment Alt goes down");
        assertFalse(AltHold.detailed(100, 100 + AltHold.DETAIL_TICKS - 1));
        assertTrue(AltHold.detailed(100, 100 + AltHold.DETAIL_TICKS));
        assertTrue(AltHold.detailed(100, 5000));
    }

    @Test
    void withAltUpThereIsNoDetail() {
        assertFalse(AltHold.detailed(-1, 5000));
    }

    @Test
    void theHoldStartsWhenAltGoesDownKeepsItWhileHeldAndForgetsItWhenReleased() {
        long since = AltHold.since(-1, false, 50);
        assertEquals(-1, since);
        since = AltHold.since(since, true, 60);
        assertEquals(60, since, "down at 60");
        since = AltHold.since(since, true, 75);
        assertEquals(60, since, "still the same hold");
        since = AltHold.since(since, false, 90);
        assertEquals(-1, since, "released");
        assertEquals(100, AltHold.since(since, true, 100), "a new hold starts again");
    }

    @Test
    void aTapAndATapAgainNeverAddUpToAHold() {
        long since = AltHold.since(-1, true, 0);
        since = AltHold.since(since, false, 15);
        since = AltHold.since(since, true, 30);
        assertFalse(AltHold.detailed(since, 30 + AltHold.DETAIL_TICKS - 1));
    }
}
