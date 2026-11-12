package net.ofatech.supernaturalbloodlines.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Stops a sync that bounces back to the same player on the same channel (A -> B -> A) after one hop. */
public final class ReentrancyGuard {
    private static final ThreadLocal<Set<String>> ACTIVE = ThreadLocal.withInitial(HashSet::new);

    private ReentrancyGuard() {
    }

    /** Runs the action unless this player and channel is already running on this thread; returns whether it ran. */
    public static boolean run(UUID player, String channel, Runnable action) {
        String key = player + "/" + channel;
        Set<String> active = ACTIVE.get();
        if (!active.add(key)) {
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            active.remove(key);
        }
    }
}
