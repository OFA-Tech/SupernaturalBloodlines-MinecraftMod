package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per player, per key (a block position): when the key may be used again. Expired keys are dropped on the next use. */
public final class SiphonCooldown<K> {
    private final Map<UUID, Map<K, Long>> readyAt = new HashMap<>();

    /** True and starts the cooldown when the key is ready at {@code now}. */
    public boolean tryUse(UUID player, K key, long now, long cooldown) {
        if (isCooling(player, key, now)) {
            return false;
        }
        readyAt.computeIfAbsent(player, id -> new HashMap<>()).put(key, now + cooldown);
        return true;
    }

    /** Is the key still cooling down at {@code now}? Drops what has run out. */
    public boolean isCooling(UUID player, K key, long now) {
        Map<K, Long> mine = readyAt.get(player);
        if (mine == null) {
            return false;
        }
        mine.values().removeIf(t -> t <= now);
        return mine.containsKey(key);
    }

    public void forget(UUID player) {
        readyAt.remove(player);
    }

    public int size(UUID player) {
        Map<K, Long> mine = readyAt.get(player);
        return mine == null ? 0 : mine.size();
    }
}
