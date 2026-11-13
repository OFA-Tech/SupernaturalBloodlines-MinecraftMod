package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per player, per key (a block position): when the key may be used again. Expired keys are dropped on the next use. */
public final class SiphonCooldown<K> {
    private final Map<UUID, Map<K, Long>> readyAt = new HashMap<>();

    /** True and starts the cooldown when the key is ready at {@code now}. */
    public boolean tryUse(UUID player, K key, long now, long cooldown) {
        Map<K, Long> mine = readyAt.computeIfAbsent(player, id -> new HashMap<>());
        mine.values().removeIf(t -> t <= now);
        if (mine.containsKey(key)) {
            return false;
        }
        mine.put(key, now + cooldown);
        return true;
    }

    public void forget(UUID player) {
        readyAt.remove(player);
    }

    public int size(UUID player) {
        Map<K, Long> mine = readyAt.get(player);
        return mine == null ? 0 : mine.size();
    }
}
