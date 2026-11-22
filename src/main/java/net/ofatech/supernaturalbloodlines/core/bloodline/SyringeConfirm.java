package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wes's syringe on another player needs two uses (R202): the first arms it for {@link #ARM_TICKS} and nothing else happens, the second on the same player
 * before it runs out does the injection. So a stray click never changes anyone, and the target is told in between. Pure; the clock is passed in.
 */
public final class SyringeConfirm {
    public static final long ARM_TICKS = 5 * 20;
    /** A second use sooner than this after the arming is the held use key repeating, not a decision: it only arms again. */
    public static final long MIN_TICKS = 10;

    private record Armed(UUID target, long armedAt) {
    }

    private final Map<UUID, Armed> armed = new HashMap<>();

    /**
     * {@code user} uses the syringe on {@code target} at {@code now}. Returns true when this use confirms an earlier one on the same player made between
     * {@link #MIN_TICKS} and {@link #ARM_TICKS} ago (the arming is spent); false when it arms (or arms again: another target, a first use that ran out, or a use so soon
     * after the last that it is only the use key held down).
     */
    public boolean confirm(UUID user, UUID target, long now) {
        armed.values().removeIf(a -> now - a.armedAt() > ARM_TICKS);
        Armed before = armed.get(user);
        if (before != null && before.target().equals(target) && now - before.armedAt() >= MIN_TICKS) {
            armed.remove(user);
            return true;
        }
        armed.put(user, new Armed(target, now));
        return false;
    }

    public void clear() {
        armed.clear();
    }
}
