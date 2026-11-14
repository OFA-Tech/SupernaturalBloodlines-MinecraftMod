package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

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

    /** Dimensions with a fixed time (the Nether, the End) report phase 0 forever, which is not a full moon. */
    public static boolean isFullMoon(boolean hasFixedTime, int phase) {
        return !hasFixedTime && phase == 0;
    }
}
