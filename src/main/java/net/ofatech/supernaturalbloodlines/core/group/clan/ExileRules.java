package net.ofatech.supernaturalbloodlines.core.group.clan;

/** An exile is a ban of one clan, stored as an end time (wall-clock millis) in the group index. */
public final class ExileRules {

    private ExileRules() {
    }

    /** "7 days, until 2026-12-01 14:03 UTC" (UTC, so the line is the same wherever it is read; one day is "1 day"). */
    public static String words(int days, long untilMillis) {
        String when = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(untilMillis));
        return days + (days == 1 ? " day" : " days") + ", until " + when + " UTC";
    }

    public static long until(long nowMillis, int days) {
        return nowMillis + days * ClanTime.DAY;
    }

    /** True while the ban lasts; at {@code until} itself it is over. */
    public static boolean bars(long until, long nowMillis) {
        return until > nowMillis;
    }
}