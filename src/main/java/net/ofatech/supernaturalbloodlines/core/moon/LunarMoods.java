package net.ofatech.supernaturalbloodlines.core.moon;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one place that asks the {@link LunarSource}. A source that throws is answered with "no mood", never with an exception on a tick; after
 * {@link #MAX_FAILURES} failures in a row of one kind of call the source is dropped for good (and logged), so "no lunar mood from now on" is true.
 * The kinds are counted apart, because the server asks for the mood each tick and the client asks for the schedule: a good answer of one must not
 * hide the other one's failures.
 */
public final class LunarMoods {
    /** The numbers of the celestials config. {@link #OFF} changes nothing; it is what is used while the celestials part is off. */
    public record Tuning(double blueRiteCost, double superRiteCost, double bloodBite, boolean superBlueSkipsForcedShift, double bloodMoonPackChance) {
        public static final Tuning OFF = new Tuning(1, 1, 1, false, 1);
    }

    /** What is being asked of the source; each has its own failure count. */
    public enum Call {
        /** The running mood (server and client, every tick). */
        NOW,
        /** The schedule (the hub, once a second). */
        AHEAD,
        /** Starting or ending our event in Enhanced Celestials. */
        MIRROR
    }

    static final int MAX_FAILURES = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(LunarMoods.class);
    private static final AtomicInteger[] FAILURES = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
    /** One warning per kind of call: a broken Calendar read must not hide a broken mirror. */
    private static final AtomicBoolean[] WARNED = {new AtomicBoolean(), new AtomicBoolean(), new AtomicBoolean()};
    private static volatile LunarSource source = LunarSource.NONE;
    private static volatile Supplier<Tuning> tuning = () -> Tuning.OFF;

    /** The mood of one level at one game tick: it is asked for several times a tick (every rite's cost, the client's draw). */
    private record Cached(WeakReference<Level> level, long tick, LunarMood mood) {
    }

    private static volatile Cached cached;

    private LunarMoods() {
    }

    /** For tests: whether a failure of this kind was logged at warn since the source was set. */
    static boolean warned(Call kind) {
        return WARNED[kind.ordinal()].get();
    }

    public static void use(LunarSource value, Supplier<Tuning> numbers) {
        source = value == null ? LunarSource.NONE : value;
        tuning = numbers == null ? () -> Tuning.OFF : numbers;
        for (AtomicInteger f : FAILURES) {
            f.set(0);
        }
        for (AtomicBoolean w : WARNED) {
            w.set(false);
        }
        cached = null;
    }

    /** The source now in use ({@link LunarSource#NONE} when there is none, or after it failed for good). */
    public static LunarSource source() {
        return source;
    }

    /** Whether there is a source: false without Enhanced Celestials and after the source was dropped. */
    public static boolean active() {
        return source != LunarSource.NONE;
    }

    /** Forgets the mood of this tick: for a change this mod made itself (our eclipse starting in Enhanced Celestials). */
    public static void refresh() {
        cached = null;
    }

    /**
     * Runs {@code call}; its answer, or {@code fallback} when it throws. The first failure of each kind is logged at warn (with the exception), the others at debug. The
     * {@link #MAX_FAILURES}th failure in a row of this kind drops the source.
     */
    public static <T> T guarded(Call kind, Supplier<T> call, T fallback) {
        AtomicInteger failures = FAILURES[kind.ordinal()];
        try {
            T answer = call.get();
            failures.set(0);
            return answer;
        } catch (RuntimeException | LinkageError e) {
            if (WARNED[kind.ordinal()].compareAndSet(false, true)) {
                LOGGER.warn("The lunar source failed ({}): {}", kind, e.toString());
            } else {
                LOGGER.debug("The lunar source failed ({}): {}", kind, e.toString());
            }
            if (failures.incrementAndGet() >= MAX_FAILURES && source != LunarSource.NONE) {
                source = LunarSource.NONE;
                cached = null;
                LOGGER.warn("The lunar source failed {} times in a row, so there is no lunar mood from now on", MAX_FAILURES);
            }
            return fallback;
        }
    }

    public static LunarMood now(Level level) {
        if (level == null || source == LunarSource.NONE) {
            return LunarMood.NONE;
        }
        Cached c = cached;
        long tick = level.getGameTime();
        if (c != null && c.tick == tick && c.level.get() == level) {
            return c.mood;
        }
        LunarSource s = source;
        LunarMood mood = guarded(Call.NOW, () -> s.now(level), LunarMood.NONE);
        cached = new Cached(new WeakReference<>(level), tick, mood);
        return mood;
    }

    public static List<LunarSource.Upcoming> ahead(Level level, int days) {
        if (level == null || source == LunarSource.NONE) {
            return List.of();
        }
        LunarSource s = source;
        return guarded(Call.AHEAD, () -> s.ahead(level, days), List.of());
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
        LunarMood mood = now(level);
        return mood.isNone() ? 1 : mood.biteFactor(tuning.get().bloodBite());
    }

    /** The share of the full-moon NPC pack chance a Blood Moon night rolls at (R143); 1 without a mood. Only a Blood Moon changes it. */
    public static double packChance(Level level) {
        LunarMood mood = now(level);
        return mood.isNone() ? 1 : mood.packChance(tuning.get().bloodMoonPackChance());
    }

    public static boolean sparesForcedShift(Level level) {
        LunarMood mood = now(level);
        return !mood.isNone() && mood.sparesForcedShift(tuning.get().superBlueSkipsForcedShift());
    }

    /** True when the mood of {@code level} is our own eclipse drawn by Enhanced Celestials: the client then leaves the moon to it. */
    public static boolean ecDrawsEclipse(Level level) {
        return now(level).kind() == LunarMood.Kind.ECLIPSE;
    }
}
