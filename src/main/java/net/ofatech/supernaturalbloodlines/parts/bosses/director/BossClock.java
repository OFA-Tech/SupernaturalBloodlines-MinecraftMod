package net.ofatech.supernaturalbloodlines.parts.bosses.director;

import net.minecraft.server.level.ServerLevel;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * R152: the days a desiccated Original stays down are counted in game time ({@code getGameTime() / 24000}), not in the time of day: sleeping through a
 * night, or {@code /time set}, does not shorten or lengthen them. (Mikael's "once a night" still uses the time of day: it is about the night.)
 */
public final class BossClock {
    private static volatile long testOffset;

    private BossClock() {
    }

    public static long today(ServerLevel level) {
        return DesiccationRules.dayOf(level.getGameTime() + testOffset);
    }

    /** Tests: moves the clock by this many ticks without touching the world's time; 0 puts it back. */
    @VisibleForTesting
    public static void offsetForTests(long ticks) {
        testOffset = ticks;
    }
}
