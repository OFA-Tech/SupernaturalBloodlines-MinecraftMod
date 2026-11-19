package net.ofatech.supernaturalbloodlines.core.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Fails if forget() is a no-op: the player's used place would still refuse them after a rejoin. */
class RateLimiterForgetTest {
    private static final UUID A = new UUID(3, 3);

    @Test
    void forgetFreesAPlayersPlaceSoTheyAreNotRefusedAfterRejoin() {
        RateLimiter r = new RateLimiter();
        assertTrue(r.tryAcquire(A, 100, 1));
        assertFalse(r.tryAcquire(A, 100, 1), "the second press in the same second is refused");
        r.forget(A);
        assertTrue(r.tryAcquire(A, 100, 1), "after forget the place is free");
    }
}
