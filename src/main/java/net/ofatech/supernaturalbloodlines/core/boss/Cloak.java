package net.ofatech.supernaturalbloodlines.core.boss;

/**
 * R172: the Rite of Cloaking hides its performer from the Vampire Compass and from a Hunter of the Five's sense until a game time. {@code 0} is "never
 * cloaked". A new cloak never shortens a longer one that is still running. Pure.
 */
public final class Cloak {
    public static final long NONE = 0L;
    private static final long TICKS_PER_MINUTE = 1200L;

    private Cloak() {
    }

    /** The game time a cloak made now runs until: at least one minute, whatever the setting says. */
    public static long until(long now, int minutes) {
        return now + Math.max(1, minutes) * TICKS_PER_MINUTE;
    }

    /** A cloak made now when {@code current} is still running: the later of the two ends. */
    public static long renewed(long current, long now, int minutes) {
        return Math.max(current, until(now, minutes));
    }

    public static boolean active(long until, long now) {
        return until > now;
    }
}
