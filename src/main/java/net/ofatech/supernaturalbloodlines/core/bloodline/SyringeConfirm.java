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

    private record Armed(UUID target, long until) {
    }

    private final Map<UUID, Armed> armed = new HashMap<>();

    /**
     * {@code user} uses the syringe on {@code target} at {@code now}. Returns true when this use confirms an earlier one (the arming is spent);
     * false when it arms (or re-arms: another target, or a first use that ran out).
     */
    public boolean confirm(UUID user, UUID target, long now) {
        armed.values().removeIf(a -> now > a.until());
        Armed before = armed.get(user);
        if (before != null && before.target().equals(target)) {
            armed.remove(user);
            return true;
        }
        armed.put(user, new Armed(target, now + ARM_TICKS));
        return false;
    }

    public void clear() {
        armed.clear();
    }
}
