package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A small cache for what a screen works out every frame from the same inputs (where the names on a ring go, how a name wraps). The key holds every input, so a
 * changed language, size or datum is a different key and never a stale answer; when it grows past {@code max} entries it starts over, so it cannot grow for good. Pure.
 */
public final class Memo<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final int max;
    private int misses;

    public Memo(int max) {
        this.max = max;
    }

    public V get(K key, Supplier<V> compute) {
        V v = map.get(key);
        if (v == null) {
            misses++;
            if (map.size() >= max) {
                map.clear();
            }
            v = compute.get();
            map.put(key, v);
        }
        return v;
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }

    /** How many times {@code compute} ran: what a test counts. */
    public int misses() {
        return misses;
    }
}