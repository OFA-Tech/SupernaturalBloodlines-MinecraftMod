package net.ofatech.supernaturalbloodlines.core.group.clan;

/** In-game midnight is day time 18000 (mod 24000). A contest ends at the first midnight at or after its minimum, read as a day time that may have jumped (a sleeping skip, /time set). */
public final class Midnight {
    public static final long TICK = 18_000;
    public static final long DAY = 24_000;

    private Midnight() {
    }

    /** The first midnight at or after {@code dayTime}. */
    public static long nextAtOrAfter(long dayTime) {
        return TICK + DAY * Math.ceilDiv(dayTime - TICK, DAY);
    }
}