package net.ofatech.supernaturalbloodlines.core.group.clan;

/** An exile is a ban of one clan, stored as an end time (wall-clock millis) in the group index. */
public final class ExileRules {
    private static final long DAY = 24L * 60 * 60 * 1000;

    private ExileRules() {
    }

    public static long until(long nowMillis, int days) {
        return nowMillis + days * DAY;
    }

    /** True while the ban lasts; at {@code until} itself it is over. */
    public static boolean bars(long until, long nowMillis) {
        return until > nowMillis;
    }
}