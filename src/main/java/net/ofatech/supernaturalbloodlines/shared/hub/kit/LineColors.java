package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;

/** The colours of the Bloodline page that are not the theme's text colours: a crown, a ring or a thread must still be 3:1 or more against what it is drawn on. Pure. */
public final class LineColors {
    public static final double MIN_RATIO = 3.0;
    public static final int GOLD = 0xFFC9962B;
    public static final int MOON_RED = 0xFFE05A5A;
    public static final int MOON_THREAD = 0xFF8A2F3A;
    public static final int MOON_RING = 0xFF6A3A44;
    /** The lighter end of the Blood Moon's sky (the darker end is easier for a light colour). */
    public static final int MOON_SKY_TOP = 0xFF2A0B14;
    public static final int MOON_SKY_BOTTOM = 0xFF07040A;

    private LineColors() {
    }

    /** {@code color}, or the nearest shade of it (towards black or white, whichever reads better on {@code bg}) that is {@code min}:1 or more against {@code bg}. */
    public static int atLeast(int color, int bg, double min) {
        if (Contrast.ratio(color, bg) >= min) {
            return color;
        }
        int target = Contrast.ratio(0xFFFFFFFF, bg) >= Contrast.ratio(0xFF000000, bg) ? 0xFFFFFFFF : 0xFF000000;
        for (int step = 1; step <= 20; step++) {
            int mixed = mix(color, target, step * 5);
            if (Contrast.ratio(mixed, bg) >= min) {
                return mixed;
            }
        }
        return target;
    }

    private static int mix(int a, int b, int percent) {
        int out = 0xFF000000;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            out |= (ca + (cb - ca) * percent / 100) << shift;
        }
        return out;
    }

    /** The crown and the ring of the crowned Original, on the theme's paper. */
    public static int gold(HubTheme t) {
        return atLeast(GOLD, t.paper(), MIN_RATIO);
    }
}
