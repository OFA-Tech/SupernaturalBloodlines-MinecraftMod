package net.ofatech.supernaturalbloodlines.core.group.clan;

/** The wall-clock units every clan rule counts in: one place, so a day is the same length everywhere. */
public final class ClanTime {
    public static final long HOUR = 60L * 60 * 1000;
    public static final long DAY = 24 * HOUR;

    private ClanTime() {
    }

    /** A wait said in a unit the reader can use: "45 s", "12 min", "5 h" (rounded up, so a wait is never said shorter than it is). */
    public static String span(long millis) {
        if (millis < 60_000) {
            return Math.max(0, (millis + 999) / 1000) + " s";
        }
        if (millis < HOUR) {
            return (millis + 59_999) / 60_000 + " min";
        }
        return (millis + HOUR - 1) / HOUR + " h";
    }
}
