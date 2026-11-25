package net.ofatech.supernaturalbloodlines.core.group.clan;

/** In-game midnight is day time 18000 (mod 24000). It is reached by crossing it between two reads, not by landing on it: a sleeping skip and /time set jump over it. */
public final class Midnight {
    public static final long TICK = 18_000;
    public static final long DAY = 24_000;

    private Midnight() {
    }

    /** Whether some {@code t} with {@code prev < t <= now} has {@code t mod 24000 == 18000}; a step that does not move forward is never a crossing. */
    public static boolean crossed(long prevDayTime, long nowDayTime) {
        return nowDayTime > prevDayTime && Math.floorDiv(nowDayTime - TICK, DAY) > Math.floorDiv(prevDayTime - TICK, DAY);
    }

    /** The first midnight at or after {@code dayTime}. */
    public static long nextAtOrAfter(long dayTime) {
        return TICK + DAY * Math.ceilDiv(dayTime - TICK, DAY);
    }
}