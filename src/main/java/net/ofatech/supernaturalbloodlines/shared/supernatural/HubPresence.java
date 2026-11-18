package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Which players have the hub open right now, as their client said. The species part reads it (a request made to someone with the hub open
 * waits longer) and the supernatural part, which owns the hub, writes it. An open hub keeps asking for snapshots; one that has not asked for
 * {@link #STALE_MS} (a crashed client, a lost "closed") stops counting as open. Nothing here is trusted for anything but a timeout.
 */
public final class HubPresence {
    public static final long STALE_MS = 60_000;

    private static final Map<UUID, Long> OPEN = new ConcurrentHashMap<>();
    private static volatile LongSupplier clock = System::currentTimeMillis;

    private HubPresence() {}

    public static void overrideClock(LongSupplier c) {
        clock = c;
    }

    public static void set(UUID player, boolean open) {
        if (open) {
            OPEN.put(player, clock.getAsLong());
        } else {
            OPEN.remove(player);
        }
    }

    /** An open hub asked for its snapshot: it is still there. Does nothing for a player whose hub is not marked open. */
    public static void touch(UUID player) {
        OPEN.computeIfPresent(player, (id, at) -> clock.getAsLong());
    }

    public static boolean isOpen(UUID player) {
        Long at = OPEN.get(player);
        return at != null && clock.getAsLong() - at <= STALE_MS;
    }

    public static void forget(UUID player) {
        OPEN.remove(player);
    }
}

