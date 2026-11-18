package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The one place that asks the {@link LunarSource}. A source that throws is answered with "no mood" (and logged once), never with an exception on a tick. */
public final class LunarMoods {
    private static final Logger LOGGER = LoggerFactory.getLogger(LunarMoods.class);
    /** The numbers of the celestials config. {@link #OFF} changes nothing; it is what is used while the celestials part is off. */
    public record Tuning(double blueRiteCost, double superRiteCost, double bloodBite, double bloodPackChance, double superBossWeight, boolean superBlueSkipsForcedShift) {
        public static final Tuning OFF = new Tuning(1, 1, 1, 1, 1, false);
    }

    private static volatile LunarSource source = LunarSource.NONE;
    private static volatile Supplier<Tuning> tuning = () -> Tuning.OFF;
    private static volatile boolean warned;

    private LunarMoods() {
    }

    public static void use(LunarSource value, Supplier<Tuning> numbers) {
        source = value == null ? LunarSource.NONE : value;
        tuning = numbers == null ? () -> Tuning.OFF : numbers;
        warned = false;
    }

    /** The altar cost factor of a rite on this level now. 1 without a mood. */
    public static double riteCost(Level level, boolean fullMoonRite) {
        Tuning t = tuning.get();
        return now(level).riteCost(fullMoonRite, t.blueRiteCost(), t.superRiteCost());
    }

    /** What a vampire's bite gives, as a factor. */
    public static double biteFactor(Level level) {
        return now(level).biteFactor(tuning.get().bloodBite());
    }

    /** NPC packs (not built yet): the chance factor tonight. */
    public static double packChanceFactor(Level level) {
        return now(level).packChanceFactor(tuning.get().bloodPackChance());
    }

    /** Full-moon bosses (not built yet): the spawn weight factor tonight. */
    public static double bossWeight(Level level) {
        return now(level).bossWeight(tuning.get().superBossWeight());
    }

    public static boolean sparesForcedShift(Level level) {
        return now(level).sparesForcedShift(tuning.get().superBlueSkipsForcedShift());
    }

    public static LunarMood now(Level level) {
        if (level == null) {
            return LunarMood.NONE;
        }
        try {
            return source.now(level);
        } catch (RuntimeException | LinkageError e) {
            warn(e);
            return LunarMood.NONE;
        }
    }

    public static List<LunarSource.Upcoming> ahead(Level level, int days) {
        if (level == null) {
            return List.of();
        }
        try {
            return source.ahead(level, days);
        } catch (RuntimeException | LinkageError e) {
            warn(e);
            return List.of();
        }
    }

    /** True when the mood of {@code level} is our own eclipse drawn by Enhanced Celestials: the client then leaves the moon to it. */
    public static boolean ecDrawsEclipse(Level level) {
        return now(level).kind() == LunarMood.Kind.ECLIPSE;
    }

    private static void warn(Throwable e) {
        if (!warned) {
            warned = true;
            LOGGER.warn("The lunar source failed, so there is no lunar mood from now on: {}", e.toString());
        }
    }
}
