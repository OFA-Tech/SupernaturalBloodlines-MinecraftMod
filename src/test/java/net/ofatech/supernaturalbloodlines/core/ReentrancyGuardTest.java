package net.ofatech.supernaturalbloodlines.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReentrancyGuardTest {
    @Test
    void nestedCallIsBlocked() {
        UUID p = UUID.randomUUID();
        AtomicBoolean innerRan = new AtomicBoolean();
        boolean[] inner = new boolean[1];
        boolean outer = ReentrancyGuard.run(p, "sync", () ->
                inner[0] = ReentrancyGuard.run(p, "sync", () -> innerRan.set(true)));
        assertTrue(outer);
        assertFalse(inner[0]);
        assertFalse(innerRan.get());
    }

    @Test
    void differentPlayersRunIndependently() {
        boolean[] inner = new boolean[1];
        ReentrancyGuard.run(UUID.randomUUID(), "sync", () ->
                inner[0] = ReentrancyGuard.run(UUID.randomUUID(), "sync", () -> { }));
        assertTrue(inner[0]);
    }

    @Test
    void differentChannelsRunIndependently() {
        UUID p = UUID.randomUUID();
        boolean[] inner = new boolean[1];
        ReentrancyGuard.run(p, "a", () -> inner[0] = ReentrancyGuard.run(p, "b", () -> { }));
        assertTrue(inner[0]);
    }

    @Test
    void guardReleasedAfterException() {
        UUID p = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () ->
                ReentrancyGuard.run(p, "sync", () -> { throw new IllegalStateException("boom"); }));
        assertTrue(ReentrancyGuard.run(p, "sync", () -> { }));
    }
}
