package net.ofatech.supernaturalbloodlines.core.moon;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one place that asks the {@link LunarSource}. A source that throws is answered with "no mood", never with an exception on a tick; after
 * {@link #MAX_FAILURES} failures in a row the source is dropped for good (and logged once), so "no lunar mood from now on" is true.
 */
public final class LunarMoods {
    /** The numbers of the celestials config. {@link #OFF} changes nothing; it is what is used while the celestials part is off. */
    public record Tuning(double blueRiteCost, double superRiteCost, double bloodBite, double bloodPackChance, double superBossWeight, boolean superBlueSkipsForcedShift) {
        public static final Tuning OFF = new Tuning(1, 1, 1, 1, 1, false);
    }

    static final int MAX_FAILURES = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(LunarMoods.class);
    private static volatile LunarSource source = LunarSource.NONE;
    private static volatile Supplier<Tuning> tuning = () -> Tuning.OFF;
    private static volatile int failures;

    private LunarMoods() {
    }

    public static void use(LunarSource value, Supplier<Tuning> numbers) {
        source = value == null ? LunarSource.NONE : value;
        tuning = numbers == null ? () -> Tuning.OFF : numbers;
        failures = 0;
    }

    /** The source now in use ({@link LunarSource#NONE} when there is none, or after it failed for good). */
    public static LunarSource source() {
        return source;
    }

    /** Runs {@code call}; its answer, or {@code fallback} when it throws. The {@link #MAX_FAILURES}th failure in a row drops the source. */
    static <T> T guarded(Supplier<T> call, T fallback) {
        try {
            T answer = call.get();
            failures = 0;
            return answer;
        } catch (RuntimeException | LinkageError e) {
            if (++failures >= MAX_FAILURES && source != LunarSource.NONE) {
                source = LunarSource.NONE;
                LOGGER.warn("The lunar source failed {} times in a row, so there is no lunar mood from now on: {}", MAX_FAILURES, e.toString());
            }
            return fallback;
        }
    }

    public static LunarMood now(Level level) {
        return level == null ? LunarMood.NONE : guarded(() -> source.now(level), LunarMood.NONE);
    }

    public static List<LunarSource.Upcoming> ahead(Level level, int days) {
        return level == null ? List.of() : guarded(() -> source.ahead(level, days), List.of());
    }

    /**
     * The altar cost factor of a rite on this level now. 1 without a mood; {@code fullMoonRite} is only asked when there is one, so a game without
     * Enhanced Celestials never looks the rite up.
     */
    public static double riteCost(Level level, BooleanSupplier fullMoonRite) {
        LunarMood mood = now(level);
        if (mood.isNone()) {
            return 1;
        }
        Tuning t = tuning.get();
        return mood.riteCost(fullMoonRite.getAsBoolean(), t.blueRiteCost(), t.superRiteCost());
    }

    public static double riteCost(Level level, boolean fullMoonRite) {
        return riteCost(level, () -> fullMoonRite);
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

    /** True when the mood of {@code level} is our own eclipse drawn by Enhanced Celestials: the client then leaves the moon to it. */
    public static boolean ecDrawsEclipse(Level level) {
        return now(level).kind() == LunarMood.Kind.ECLIPSE;
    }
}
