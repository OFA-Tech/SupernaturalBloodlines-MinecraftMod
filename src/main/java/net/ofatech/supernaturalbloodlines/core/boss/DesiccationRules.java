package net.ofatech.supernaturalbloodlines.core.boss;

/** R152: a desiccated Original returns after {@code original_return_days} in-game days. Days are {@code dayTime / 24000}. */
public final class DesiccationRules {
    public static final long DAY = 24000L;

    private DesiccationRules() {
    }

    public static long dayOf(long dayTime) {
        return Math.floorDiv(dayTime, DAY);
    }

    public static long returnDay(long today, int days) {
        return today + Math.max(1, days);
    }

    public static boolean due(long today, long returnDay) {
        return today >= returnDay;
    }

    /** Days left, never negative, for the Admin state line. */
    public static long daysLeft(long today, long returnDay) {
        return Math.max(0, returnDay - today);
    }
}