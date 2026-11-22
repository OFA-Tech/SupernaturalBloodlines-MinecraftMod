package net.ofatech.supernaturalbloodlines.core.blood;

/** The day rule of a per-day limit: counted in world (game) time, a day is 24000 ticks, so sleeping or {@code /time} does not reset it. */
public final class DailyUses {
    public static final long DAY_TICKS = 24000L;

    private DailyUses() {
    }

    public static long dayOf(long gameTime) {
        return Math.floorDiv(gameTime, DAY_TICKS);
    }

    /** The uses counted on {@code storedDay}, as they stand at {@code gameTime}: none once another day has begun. */
    public static int usesToday(long storedDay, long storedUses, long gameTime) {
        return storedDay == dayOf(gameTime) ? (int) Math.max(0L, Math.min(Integer.MAX_VALUE, storedUses)) : 0;
    }
}
