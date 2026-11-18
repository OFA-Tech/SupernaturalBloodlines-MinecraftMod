package net.ofatech.supernaturalbloodlines.shared.supernatural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HubPresenceTest {
    private static final UUID P = new UUID(5, 5);
    private final long[] now = {1_000_000};

    @AfterEach
    void reset() {
        HubPresence.overrideClock(System::currentTimeMillis);
        HubPresence.forget(P);
    }

    @Test
    void anOpenHubStopsCountingAfterAMinuteWithoutASnapshotRequestButAskingKeepsIt() {
        HubPresence.overrideClock(() -> now[0]);
        HubPresence.set(P, true);
        assertTrue(HubPresence.isOpen(P));
        now[0] += HubPresence.STALE_MS - 1;
        assertTrue(HubPresence.isOpen(P));
        HubPresence.touch(P);
        now[0] += HubPresence.STALE_MS - 1;
        assertTrue(HubPresence.isOpen(P), "a snapshot request keeps it open");
        now[0] += 2;
        assertFalse(HubPresence.isOpen(P), "a crashed client's open flag expires");
    }

    @Test
    void touchingAPlayerWhoseHubIsNotOpenDoesNotOpenIt() {
        HubPresence.touch(P);
        assertFalse(HubPresence.isOpen(P));
        HubPresence.set(P, true);
        HubPresence.set(P, false);
        assertFalse(HubPresence.isOpen(P));
    }
}

