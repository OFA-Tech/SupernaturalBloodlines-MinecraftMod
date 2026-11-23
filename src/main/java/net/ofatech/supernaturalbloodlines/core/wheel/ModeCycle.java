package net.ofatech.supernaturalbloodlines.core.wheel;

/** The mode an Alt click or Alt scroll goes to: the next (or previous) unlocked one, round the list. */
public final class ModeCycle {
    private ModeCycle() {
    }

    /** The mode after {@code current}, skipping locked ones; {@code current} when it is the only unlocked one; -1 when none is unlocked. */
    public static int next(int current, boolean[] unlocked, boolean backwards) {
        int n = unlocked.length;
        if (n == 0) {
            return -1;
        }
        int step = backwards ? -1 : 1;
        int from = current < 0 || current >= n ? (backwards ? 0 : n - 1) : current;
        for (int i = 1; i <= n; i++) {
            int candidate = Math.floorMod(from + step * i, n);
            if (unlocked[candidate]) {
                return candidate;
            }
        }
        return -1;
    }
}
