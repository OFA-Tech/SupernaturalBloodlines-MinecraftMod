package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * The cooldown pie: which pixels of a slot the dark sweep covers. The sweep starts at twelve o'clock and runs clockwise, covering {@code fraction} of the
 * circle, so the dark part shrinks as the cooldown runs out. Pure, so the sweep is tested without a screen.
 */
public final class PieSweep {
    private PieSweep() {
    }

    /** Whether the point {@code (dx, dy)} (screen axes, y down, from the centre) is under a sweep of {@code fraction} (0 to 1). */
    public static boolean covered(double dx, double dy, float fraction) {
        if (fraction <= 0f) {
            return false;
        }
        if (fraction >= 1f) {
            return true;
        }
        if (dx == 0 && dy == 0) {
            return true;
        }
        // clockwise from the top: atan2(dx, -dy) is 0 at the top, rising to the right
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle <= fraction * 2 * Math.PI;
    }

    /** The share of an {@code n} by {@code n} square (centred on its middle) that a sweep of {@code fraction} covers, for tests. */
    public static double coveredShare(int n, float fraction) {
        int hit = 0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (covered(x + 0.5 - n / 2.0, y + 0.5 - n / 2.0, fraction)) {
                    hit++;
                }
            }
        }
        return hit / (double) (n * n);
    }
}
