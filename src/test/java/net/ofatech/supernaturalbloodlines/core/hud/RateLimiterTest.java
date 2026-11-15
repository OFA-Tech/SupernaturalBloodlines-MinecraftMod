package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimiterTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void allowsTheLimitPerSecondAndRefusesTheRest() {
        RateLimiter r = new RateLimiter();
        for (int i = 0; i < 10; i++) {
            assertTrue(r.tryAcquire(A, 100, 10), "press " + i);
        }
        assertFalse(r.tryAcquire(A, 100, 10), "the 11th in the same second");
        assertFalse(r.tryAcquire(A, 119, 10), "still the same second");
    }

    @Test
    void aNewSecondStartsFresh() {
        RateLimiter r = new RateLimiter();
        for (int i = 0; i < 11; i++) {
            r.tryAcquire(A, 100, 10);
        }
        assertTrue(r.tryAcquire(A, 120, 10), "20 ticks later");
    }

    @Test
    void playersAreCountedApart() {
        RateLimiter r = new RateLimiter();
        for (int i = 0; i < 11; i++) {
            r.tryAcquire(A, 5, 10);
        }
        assertTrue(r.tryAcquire(B, 5, 10));
    }

    @Test
    void aRefusalCountsLikeAnythingElse() {
        RateLimiter r = new RateLimiter();
        assertTrue(r.tryAcquire(A, 0, 1));
        for (int i = 0; i < 50; i++) {
            assertFalse(r.tryAcquire(A, 1, 1), "a flood of refused presses never gets in");
        }
    }

    @Test
    void aServerClockThatWentBackwardsDoesNotLockAPlayerOut() {
        RateLimiter r = new RateLimiter();
        assertTrue(r.tryAcquire(A, 1000, 1));
        assertFalse(r.tryAcquire(A, 1000, 1));
        assertTrue(r.tryAcquire(A, 5, 1), "a restarted tick counter");
    }

    @Test
    void forgetDropsAPlayer() {
        RateLimiter r = new RateLimiter();
        assertTrue(r.tryAcquire(A, 0, 1));
        r.forget(A);
        assertTrue(r.tryAcquire(A, 0, 1));
    }
}
