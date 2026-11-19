package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;

/**
 * Delayed awakening (R142): a latent werewolf gene that a kill triggers awakens at a later dusk, not at once. Times are the overworld's
 * {@code dayTime}. Pure.
 */
public final class AwakeningRules {
    private static final long DAY = 24000L;

    private AwakeningRules() {
    }

    /**
     * The dayTime the awakening is due: the {@code nights}th dusk strictly after {@code now} (a kill at dusk or in the night waits for the next one).
     * 0 nights is now.
     */
    public static long due(long now, int nights) {
        if (nights <= 0) {
            return now;
        }
        long thisDusk = Math.floorDiv(now, DAY) * DAY + MoonRule.NIGHT_START;
        long first = thisDusk > now ? thisDusk : thisDusk + DAY;
        return first + (nights - 1) * DAY;
    }

    /**
     * Whether it is time. Also when the clock was set back so far that the due time is more than a day beyond what a fresh kill would wait
     * (a {@code /time set} must not freeze an awakening for ever) and when the setting went down to 0.
     */
    public static boolean isDue(long now, long due, int nights) {
        return nights <= 0 || now >= due || due - now > (nights + 1L) * DAY;
    }
}
