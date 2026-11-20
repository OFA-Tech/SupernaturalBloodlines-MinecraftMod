package net.ofatech.supernaturalbloodlines.core.hud;

/** A cooldown the server told the client about as (the tick it started, its length): what is left, for the slot's sweep and its seconds. Pure. */
public final class ActionCooldown {
    private ActionCooldown() {
    }

    /** Ticks left {@code now}; a clock that went back (a new world) or a finished cooldown is 0. */
    public static int left(long startedAt, int total, long now) {
        long passed = now - startedAt;
        return passed < 0 || passed >= total ? 0 : (int) (total - passed);
    }

    /** The tick a cooldown of {@code total} with {@code left} still to run was started at, for a cooldown the client learns about half way (a login). */
    public static long startedAt(long now, int total, int left) {
        return now - (total - Math.min(total, Math.max(0, left)));
    }

    /** The seconds to show: counted up, so a cooldown that has not ended never reads 0. */
    public static int seconds(int leftTicks) {
        return (leftTicks + 19) / 20;
    }

    /** The share of the cooldown left (1 just started, 0 over). */
    public static float fraction(int leftTicks, int total) {
        return total <= 0 || leftTicks <= 0 ? 0f : Math.min(1f, leftTicks / (float) total);
    }
}
