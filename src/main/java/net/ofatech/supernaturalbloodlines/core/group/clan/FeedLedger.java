package net.ofatech.supernaturalbloodlines.core.group.clan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** What each mate has taken from shares in the last minute, and when each feeder last fed each mate. In memory only; the caller clears it when the server stops. */
public final class FeedLedger {
    private static final long WINDOW_MILLIS = 60_000;

    private record Take(long at, int amount) {
    }

    private record Pair(UUID feeder, UUID mate) {
    }

    private final Map<UUID, ArrayDeque<Take>> takes = new HashMap<>();
    private final Map<Pair, Long> lastTick = new HashMap<>();
    private long newest = Long.MIN_VALUE;

    /** The share really granted: trimmed to what the minute's cap leaves; 0 when the same feeder fed this mate less than {@code spacingTicks} ago or the clock went backwards. */
    public int grant(UUID feeder, UUID mate, int share, long nowMillis, long nowTick, int capPerMinute, int spacingTicks) {
        if (nowMillis < newest || share <= 0) {
            return 0;
        }
        newest = nowMillis;
        takes.values().forEach(q -> q.removeIf(t -> nowMillis - t.at() >= WINDOW_MILLIS));
        takes.values().removeIf(ArrayDeque::isEmpty);
        Long last = lastTick.get(new Pair(feeder, mate));
        if (last != null && nowTick - last < spacingTicks) {
            return 0;
        }
        ArrayDeque<Take> queue = takes.computeIfAbsent(mate, k -> new ArrayDeque<>());
        int used = queue.stream().mapToInt(Take::amount).sum();
        int granted = Math.min(share, Math.max(0, capPerMinute - used));
        if (granted > 0) {
            queue.add(new Take(nowMillis, granted));
            lastTick.put(new Pair(feeder, mate), nowTick);
        } else if (queue.isEmpty()) {
            takes.remove(mate);
        }
        return granted;
    }

    public void clear() {
        takes.clear();
        lastTick.clear();
        newest = Long.MIN_VALUE;
    }
}