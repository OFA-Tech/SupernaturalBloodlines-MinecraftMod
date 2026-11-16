package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CallGuardTest {
    private static int boom() {
        throw new IllegalStateException("the party mod threw");
    }

    @Test
    void aSuccessClearsTheCountSoScatteredFailuresNeverTrip() {
        AtomicInteger trips = new AtomicInteger();
        CallGuard g = new CallGuard(3, t -> trips.incrementAndGet());
        for (int i = 0; i < 30; i++) {
            assertFalse(g.call(CallGuardTest::boom).ok());
            assertFalse(g.call(CallGuardTest::boom).ok());
            assertTrue(g.call(() -> 1).ok());
        }
        assertFalse(g.off());
        assertEquals(0, trips.get());
    }

    @Test
    void consecutiveFailuresTurnItOffAndTheWarningComesOnce() {
        AtomicInteger trips = new AtomicInteger();
        AtomicInteger made = new AtomicInteger();
        CallGuard g = new CallGuard(3, t -> trips.incrementAndGet());
        for (int i = 0; i < 3; i++) {
            g.call(CallGuardTest::boom);
        }
        assertTrue(g.off());
        assertEquals(1, trips.get());
        for (int i = 0; i < 5; i++) {
            assertFalse(g.call(() -> made.incrementAndGet()).ok());
        }
        assertEquals(0, made.get(), "a call was made after it was off");
        assertEquals(1, trips.get(), "the warning came again");
    }

    @Test
    void aNullAnswerIsAnAnswerNotAFailure() {
        CallGuard g = new CallGuard(2, t -> { });
        for (int i = 0; i < 10; i++) {
            CallGuard.Answer<String> a = g.call(() -> null);
            assertTrue(a.ok());
            assertNull(a.value());
        }
        assertFalse(g.off());
    }

    @Test
    void aLinkageErrorCountsLikeAnyFailure() {
        AtomicInteger trips = new AtomicInteger();
        CallGuard g = new CallGuard(1, t -> trips.incrementAndGet());
        g.call(() -> {
            throw new NoSuchMethodError("the API changed");
        });
        assertTrue(g.off());
        assertEquals(1, trips.get());
    }
}