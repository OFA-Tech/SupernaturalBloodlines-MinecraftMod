package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players have the hub open right now, as their client said. The species part reads it (a request made to someone with the hub open
 * waits longer) and the supernatural part, which owns the hub, writes it. Pure; nothing here is trusted for anything but a timeout.
 */
public final class HubPresence {
    private static final Set<UUID> OPEN = ConcurrentHashMap.newKeySet();

    private HubPresence() {}

    public static void set(UUID player, boolean open) {
        if (open) {
            OPEN.add(player);
        } else {
            OPEN.remove(player);
        }
    }

    public static boolean isOpen(UUID player) {
        return OPEN.contains(player);
    }

    public static void forget(UUID player) {
        OPEN.remove(player);
    }
}
