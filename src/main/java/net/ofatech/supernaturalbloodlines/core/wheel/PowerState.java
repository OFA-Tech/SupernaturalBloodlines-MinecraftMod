package net.ofatech.supernaturalbloodlines.core.wheel;

/** What the player sees of a power right now. {@code cooldownLeft} and {@code cooldownTotal} are ticks; {@code mode} is the current mode of {@code modeCount} (1 = no modes). */
public record PowerState(boolean on, int cooldownLeft, int cooldownTotal, LockReason lock, int mode, int modeCount) {
    public static final PowerState READY = new PowerState(false, 0, 0, LockReason.NONE, 0, 1);

    public PowerState {
        java.util.Objects.requireNonNull(lock);
        cooldownLeft = Math.max(0, cooldownLeft);
        cooldownTotal = Math.max(cooldownLeft, cooldownTotal);
        modeCount = Math.max(1, modeCount);
        mode = Math.max(0, Math.min(mode, modeCount - 1));
    }

    public boolean locked() {
        return lock != LockReason.NONE;
    }
}
