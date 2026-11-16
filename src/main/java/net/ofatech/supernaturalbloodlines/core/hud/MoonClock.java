package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;

/**
 * When the full moon forces a werewolf's shift, from the world clock alone, by the same window the rites and the lunar eclipse use ({@link MoonRule}: the
 * phase-0 day, after 12786 and before 23216, and never in a dimension with a fixed time). The client cannot know whether the player has moon control (a Moon
 * Ring, Moon Calm, free_will, an exempt rank, or {@code moon_bound_werewolves} turned off on the server), so the warning is shown to every werewolf. Pure.
 */
public final class MoonClock {
    private MoonClock() {
    }

    /**
     * @param phase the world's moon phase today (0 is full)
     * @return the ticks until the night of a full moon begins, 0 while it is that night, or -1 when today is not a full moon, the dimension has a fixed
     *         time, or the night has passed
     */
    public static long ticksUntilForcedShift(boolean hasFixedTime, int phase, long dayTime) {
        if (!MoonRule.isFullMoon(hasFixedTime, phase)) {
            return -1;
        }
        long timeOfDay = Math.floorMod(dayTime, 24_000L);
        if (timeOfDay <= MoonRule.NIGHT_START) {
            return MoonRule.NIGHT_START + 1 - timeOfDay;
        }
        return timeOfDay < MoonRule.NIGHT_END ? 0 : -1;
    }

    /** {@code "1:40"} for 2000 ticks. */
    public static String clock(long ticks) {
        long s = Math.max(0, ticks) / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }
}
