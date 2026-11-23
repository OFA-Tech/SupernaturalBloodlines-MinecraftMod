package net.ofatech.supernaturalbloodlines.core.curse;

import java.util.function.DoubleSupplier;

/** The Curse of Insanity's timing as a pure rule (R320). */
public final class InsanityRules {
    private static final long MIN_TICKS = 20;

    private InsanityRules() {
    }

    /**
     * Ticks until the next hallucination: the interval shortens with the strength ({@code seconds / (strength + 1)}) and varies by a third either
     * way, never under a second.
     */
    public static long nextDelay(int seconds, int strength, DoubleSupplier random) {
        double base = Math.max(1, seconds) * 20.0 / (strength + 1);
        long ticks = (long) (base * (2.0 / 3 + random.getAsDouble() * 2.0 / 3) + 1e-9);
        return Math.max(MIN_TICKS, ticks);
    }
}
