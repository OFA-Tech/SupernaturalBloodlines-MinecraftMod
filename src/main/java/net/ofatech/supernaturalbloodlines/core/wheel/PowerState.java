package net.ofatech.supernaturalbloodlines.core.wheel;

/**
 * What the player sees of a power right now. A cooldown is a stable {@code cooldownEnd} (a game time, 0 when ready) and its full length {@code cooldownTotal} in ticks, so a
 * running cooldown does not change the entry: the client counts down against its own clock ({@link #cooldownLeft}). {@code mode} is the current mode of {@code modeCount} (1 = no modes).
 */
public record PowerState(boolean on, long cooldownEnd, int cooldownTotal, LockReason lock, int mode, int modeCount) {
    public static final PowerState READY = new PowerState(false, 0L, 0, LockReason.NONE, 0, 1);

    public PowerState {
        java.util.Objects.requireNonNull(lock);
        cooldownEnd = Math.max(0L, cooldownEnd);
        cooldownTotal = cooldownEnd == 0L ? 0 : Math.max(1, cooldownTotal);
        modeCount = Math.max(1, modeCount);
        mode = Math.max(0, Math.min(mode, modeCount - 1));
    }

    public boolean locked() {
        return lock != LockReason.NONE;
    }

    /** Ticks of cooldown left at game time {@code now}. */
    public int cooldownLeft(long now) {
        return (int) Math.max(0L, Math.min(cooldownEnd - now, Integer.MAX_VALUE));
    }

    public PowerState withLock(LockReason newLock) {
        return new PowerState(on, cooldownEnd, cooldownTotal, newLock, mode, modeCount);
    }

    /** This state with a cooldown from a clock reading. */
    public PowerState withCooldown(CooldownClock.Reading r) {
        return new PowerState(on, r.end(), r.total(), lock, mode, modeCount);
    }
}
