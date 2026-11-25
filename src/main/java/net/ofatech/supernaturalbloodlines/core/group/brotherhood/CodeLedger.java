package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Two breaks by one killer on one victim within a minute are one break. In memory only. A clock that goes backwards forgets nothing. */
public final class CodeLedger {
    static final long WINDOW_MILLIS = 60_000L;

    private record Pair(UUID breaker, UUID victim) {
    }

    private final Map<Pair, Long> last = new HashMap<>();

    public boolean countsOnce(UUID breaker, UUID victim, long nowMillis) {
        for (Iterator<Long> it = last.values().iterator(); it.hasNext();) {
            if (nowMillis - it.next() >= WINDOW_MILLIS) {
                it.remove();
            }
        }
        Pair key = new Pair(breaker, victim);
        if (last.containsKey(key)) {
            return false;
        }
        last.put(key, nowMillis);
        return true;
    }
}