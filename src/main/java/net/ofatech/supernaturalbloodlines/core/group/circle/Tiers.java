package net.ofatech.supernaturalbloodlines.core.group.circle;

/** The circle's size tiers, 0 to 3; a triple that is not increasing is raised to the one before it. */
public final class Tiers {
    private Tiers() {
    }

    public static int of(int n, int t1, int t2, int t3) {
        int second = Math.max(t1, t2);
        int third = Math.max(second, t3);
        if (n >= third) {
            return 3;
        }
        if (n >= second) {
            return 2;
        }
        return n >= t1 ? 1 : 0;
    }
}