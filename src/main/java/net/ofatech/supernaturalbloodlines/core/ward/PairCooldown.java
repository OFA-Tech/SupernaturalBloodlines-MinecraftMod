package net.ofatech.supernaturalbloodlines.core.ward;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A cooldown per ordered pair (owner, guest): an invitation, or a withdrawal, counts as a use. A clock that went backwards frees the pair. */
public final class PairCooldown {
    private final Map<String, Long> last = new HashMap<>();

    private static String key(UUID a, UUID b) {
        return a + ">" + b;
    }

    /** Whether the pair may be used now; when it may, the time is taken. */
    public synchronized boolean tryUse(UUID a, UUID b, long now, long cooldown) {
        Long before = last.get(key(a, b));
        if (before != null && now >= before && now - before < cooldown) {
            return false;
        }
        last.put(key(a, b), now);
        return true;
    }

    public synchronized void clear() {
        last.clear();
    }
}
