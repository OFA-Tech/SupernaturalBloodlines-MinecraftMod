package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * When the full moon forces a werewolf's shift, from the world clock alone: the moon is full on the days whose number is a multiple of eight, and the forced
 * shift comes when that day's night begins (tick 13000 of the day). Pure, so the HUD's warning is tested without a world.
 */
public final class MoonClock {
    public static final int DAY = 24_000;
    public static final int NIGHT_BEGINS = 13_000;
    /** The night ends (the sun is up again) at tick 23000 of the day. */
    public static final int NIGHT_ENDS = 23_000;

    private MoonClock() {
    }

    /**
     * @return the ticks until the night of a full moon begins, 0 while it is that night, or -1 when today is not a full moon day or its night has passed
     */
    public static long ticksUntilForcedShift(long dayTime) {
        long day = Math.floorDiv(dayTime, DAY);
        long tod = Math.floorMod(dayTime, DAY);
        if (Math.floorMod(day, 8) != 0) {
            return -1;
        }
        if (tod < NIGHT_BEGINS) {
            return NIGHT_BEGINS - tod;
        }
        return tod < NIGHT_ENDS ? 0 : -1;
    }

    /** {@code "1:40"} for 2000 ticks. */
    public static String clock(long ticks) {
        long s = Math.max(0, ticks) / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }
}
