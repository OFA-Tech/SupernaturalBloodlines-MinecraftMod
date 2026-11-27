package net.ofatech.supernaturalbloodlines.core.moon;

/** Pure, so a unit test can reach it without Minecraft. */
public final class MoonRule {
    private MoonRule() {
    }

    /** Werewolves' own full-moon window (its Helper.isFullMoon): the time of day is after 12786 and before 23216 on the phase-0 day. The lunar eclipse uses the same window. */
    public static final long NIGHT_START = 12786;
    public static final long NIGHT_END = 23216;

    /** A full moon that is up: the phase-0 day, and the time of day inside the night window (noon on the full-moon day does not count). */
    public static boolean isFullMoonNight(boolean hasFixedTime, int phase, long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        return isFullMoon(hasFixedTime, phase) && timeOfDay > NIGHT_START && timeOfDay < NIGHT_END;
    }

    /** Any night, full moon or not: the time of day inside the night window, in a dimension that has a day. */
    public static boolean isNight(boolean hasFixedTime, long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        return !hasFixedTime && timeOfDay > NIGHT_START && timeOfDay < NIGHT_END;
    }

    /**
     * The night a rite wants: a full-moon night when its file says {@code requires_full_moon}, else any night. A running lunar eclipse counts as a
     * full moon (R358), inside the same night window.
     */
    public static boolean nightFor(boolean needsFullMoon, boolean hasFixedTime, int phase, long dayTime, boolean eclipse) {
        if (!needsFullMoon) {
            return isNight(hasFixedTime, dayTime);
        }
        return isFullMoonNight(hasFixedTime, phase, dayTime) || eclipseNight(hasFixedTime, dayTime, eclipse);
    }

    /** The one eclipse rule (R358): a running lunar eclipse counts only inside the night window, in a dimension with a day. */
    public static boolean eclipseNight(boolean hasFixedTime, long dayTime, boolean eclipse) {
        return eclipse && isNight(hasFixedTime, dayTime);
    }

    /** Dimensions with a fixed time (the Nether, the End) report phase 0 forever, which is not a full moon. */
    public static boolean isFullMoon(boolean hasFixedTime, int phase) {
        return !hasFixedTime && phase == 0;
    }
}
