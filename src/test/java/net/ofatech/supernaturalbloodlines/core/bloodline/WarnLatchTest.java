package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WarnLatchTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void warnsOncePerOpeningAndRearmsWhenClosed() {
        WarnLatch latch = new WarnLatch();
        assertTrue(latch.open(A));
        assertFalse(latch.open(A), "still open: no second warning");
        assertTrue(latch.isOpen(A));
        latch.close(A);
        assertFalse(latch.isOpen(A));
        assertTrue(latch.open(A), "a new opening warns again");
    }

    @Test
    void rootsAreIndependentAndClearForgetsAll() {
        WarnLatch latch = new WarnLatch();
        assertTrue(latch.open(A));
        assertTrue(latch.open(B));
        latch.close(B);
        assertTrue(latch.isOpen(A));
        latch.clear();
        assertFalse(latch.isOpen(A));
        latch.close(A); // closing what is not open is fine
    }
}
