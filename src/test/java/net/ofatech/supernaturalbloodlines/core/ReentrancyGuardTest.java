package net.ofatech.supernaturalbloodlines.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ReentrancyGuardTest {
    @Test
    void nestedCallIsBlocked() {
        UUID p = UUID.randomUUID();
        AtomicBoolean inner = new AtomicBoolean(true);
        assertTrue(ReentrancyGuard.run(p, "sync", () ->
                inner.set(ReentrancyGuard.run(p, "sync", () -> fail("inner action must not run")))));
        assertFalse(inner.get());
    }

    @Test
    void differentPlayersRunIndependently() {
        AtomicBoolean inner = new AtomicBoolean();
        ReentrancyGuard.run(UUID.randomUUID(), "sync", () ->
                inner.set(ReentrancyGuard.run(UUID.randomUUID(), "sync", () -> { })));
        assertTrue(inner.get());
    }

    @Test
    void differentChannelsRunIndependently() {
        UUID p = UUID.randomUUID();
        AtomicBoolean inner = new AtomicBoolean();
        ReentrancyGuard.run(p, "a", () -> inner.set(ReentrancyGuard.run(p, "b", () -> { })));
        assertTrue(inner.get());
    }

    @Test
    void guardReleasedAfterException() {
        UUID p = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () ->
                ReentrancyGuard.run(p, "sync", () -> { throw new IllegalStateException("boom"); }));
        assertTrue(ReentrancyGuard.run(p, "sync", () -> { }));
    }
}
