package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;

/**
 * Delayed awakening (R142): a latent werewolf gene that a kill triggers awakens at a later dusk, not at once. {@code dayTime} is the overworld's
 * clock, which {@code doDaylightCycle=false} freezes, so the wait is also counted on the game time ({@code gameTime}), which never stops. Pure.
 */
public final class AwakeningRules {
    private static final long DAY = 24000L;

    /** What waits for a player: the {@code dayTime} it is due and the {@code gameTime} it was queued. {@link #NONE} (due 0) is nothing. */
    public record Pending(long due, long queuedAt) {
        public static final Pending NONE = new Pending(0, 0);
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("due").forGetter(Pending::due),
                Codec.LONG.optionalFieldOf("queued_at", 0L).forGetter(Pending::queuedAt)).apply(i, Pending::new));

        public boolean waiting() {
            return due > 0;
        }
    }

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
     * Whether it is time. Also when the setting went down to 0; when the clock was set back so far that the due time is more than a day beyond what
     * a fresh kill would wait (a {@code /time set} must not freeze an awakening for ever); and when the clock does not move at all: the dusk is
     * never reached, so {@code nights} whole days of game time since the kill are enough (never sooner than the dusk would have been).
     */
    public static boolean isDue(long now, long due, int nights, long gameTime, long queuedAt) {
        return nights <= 0 || now >= due || due - now > (nights + 1L) * DAY || gameTime - queuedAt >= nights * DAY;
    }
}
