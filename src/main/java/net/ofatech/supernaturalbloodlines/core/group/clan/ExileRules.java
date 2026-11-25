package net.ofatech.supernaturalbloodlines.core.group.clan;

/** An exile is a ban of one clan, stored as an end time (wall-clock millis) in the group index. */
public final class ExileRules {

    private ExileRules() {
    }

    /** "2026-12-01 14:03 UTC": UTC, so the line is the same wherever it is read. The days are a number of their own, said by the lang file. */
    public static String untilText(long untilMillis) {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(untilMillis)) + " UTC";
    }

    /** Whole days a ban still lasts, a started day counting as a day; 0 when it is over. */
    public static int daysLeft(long untilMillis, long nowMillis) {
        return untilMillis <= nowMillis ? 0 : (int) Math.min(Integer.MAX_VALUE, (untilMillis - nowMillis + ClanTime.DAY - 1) / ClanTime.DAY);
    }

    public static long until(long nowMillis, int days) {
        return nowMillis + days * ClanTime.DAY;
    }

    /** True while the ban lasts; at {@code until} itself it is over. */
    public static boolean bars(long until, long nowMillis) {
        return until > nowMillis;
    }
}