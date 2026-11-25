package net.ofatech.supernaturalbloodlines.core.group.circle;

/** What a circle adds to a rite: every member adds less than the one before. */
public final class Potency {
    private Potency() {
    }

    public static double of(int n, double max, double falloff) {
        if (n < 1) {
            return 1.0;
        }
        return 1.0 + max * (1.0 - Math.pow(falloff, n - 1));
    }
}