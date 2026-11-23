package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns "ticks left", which changes every tick (every second for Tensura), into a stable end time and the full length, so a running cooldown does not change what is sent to the
 * wheel: the client counts down from {@code end} on its own clock. One clock per player. Pure.
 * <p>
 * {@link #observe} keeps the first end it saw and keeps it while a later reading agrees with it within {@code grain} ticks (the rounding of the source: 20 for Tensura, which counts
 * whole seconds); a reading that disagrees is a new cooldown. A reading of 0 clears it.
 */
public final class CooldownClock {
    /** The end game time (0 when ready) and the full length in ticks. */
    public record Reading(long end, int total) {
        public static final Reading READY = new Reading(0L, 0);
    }

    private record Entry(long end, int total) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    /**
     * @param knownTotal the full length when the source knows it, else 0 (then the longest left seen when the cooldown began is used)
     */
    public synchronized Reading observe(String key, long now, int leftTicks, int grain, int knownTotal) {
        if (leftTicks <= 0) {
            entries.remove(key);
            return Reading.READY;
        }
        long computed = now + leftTicks;
        Entry e = entries.get(key);
        if (e == null || Math.abs(computed - e.end) > Math.max(1, grain)) {
            e = new Entry(computed, Math.max(leftTicks, knownTotal));
            entries.put(key, e);
        } else if (knownTotal > e.total) {
            e = new Entry(e.end, knownTotal);
            entries.put(key, e);
        }
        return new Reading(e.end, e.total);
    }

    /** A press just started a cooldown of this length: the exact end and full length (the best the source can say). */
    public synchronized void notePress(String key, long now, int fullTicks) {
        if (fullTicks > 0) {
            entries.put(key, new Entry(now + fullTicks, fullTicks));
        }
    }

    public synchronized void clear() {
        entries.clear();
    }
}
