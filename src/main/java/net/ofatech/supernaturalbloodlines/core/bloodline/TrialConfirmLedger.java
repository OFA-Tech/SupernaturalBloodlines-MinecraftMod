package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server's memory of the True Alpha warning for the Alpha Trial (R147). The rite is refused once and the player is sent the warning; their answer
 * ({@code confirm}) only counts if a warning was sent to them a moment ago, and then lets the rite start within two minutes. The client's word is the
 * click, but the server checks that it asked first. Times are game ticks. Kept in memory: a restart asks again.
 */
public final class TrialConfirmLedger {
    /** A warning waits this long (5 minutes) for its answer. */
    public static final long WARN_TICKS = 6000;
    /** An answer lets the rite start this long (2 minutes). */
    public static final long CONFIRM_TICKS = 2400;

    private final Map<UUID, Long> warned = new HashMap<>();
    private final Map<UUID, Long> confirmed = new HashMap<>();

    public synchronized void warn(UUID p, long now) {
        warned.put(p, now + WARN_TICKS);
    }

    /** The player pressed "take the spark": true when a warning was sent to them and has not run out. */
    public synchronized boolean confirm(UUID p, long now) {
        Long until = warned.remove(p);
        if (until == null || now > until) {
            return false;
        }
        confirmed.put(p, now + CONFIRM_TICKS);
        return true;
    }

    public synchronized boolean valid(UUID p, long now) {
        Long until = confirmed.get(p);
        return until != null && now <= until;
    }

    /** The rite started: the answer is used up. */
    public synchronized void consume(UUID p) {
        confirmed.remove(p);
        warned.remove(p);
    }

    public synchronized void clear() {
        warned.clear();
        confirmed.clear();
    }
}
