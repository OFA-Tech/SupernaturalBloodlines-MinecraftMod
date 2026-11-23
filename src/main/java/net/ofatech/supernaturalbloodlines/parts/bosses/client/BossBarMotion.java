package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.ofatech.supernaturalbloodlines.shared.hub.kit.Easing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Motion;

/**
 * The motion of a boss bar, each part taking {@code reduce} (the {@code reduce_motion} setting): with it on, the fill is already at its health, the shifting fill
 * and the seam do not move, and a chip is lit in one frame. The form, the colours and the numbers change either way: they are information. Pure.
 */
public final class BossBarMotion {
    /** The shifting fill breathes between this share of full and full: not lower, or the Grimoire's gold would wash into its parchment track (3:1 is held at this floor, {@code BossFacesTest}). */
    private static final int PULSE_FLOOR = 85;
    private static final int PULSE_SPAN = 15;
    /** The seam of a shift flashes between this and full (percent alpha). */
    private static final int SEAM_FLOOR = 40;
    private static final int SEAM_SPAN = 60;

    private BossBarMotion() {
    }

    /** The alpha percent of the gold fill now: it breathes, and with reduce motion it is held at full. */
    public static int shiftFillPercent(long tick, boolean reduce) {
        return PULSE_FLOOR + Math.round(PULSE_SPAN * Motion.pulse(tick, reduce));
    }

    /** The alpha percent of the seam's line now: it flashes, and with reduce motion there is no flash (0). */
    public static int seamPercent(long tick, boolean reduce) {
        return reduce ? 0 : SEAM_FLOOR + Math.round(SEAM_SPAN * Motion.pulse(tick, false));
    }

    /** How far a chip has come in being lit (0 to 1) {@code now}, having begun at {@code since}; 1 with reduce motion. */
    public static float chipFade(double since, double now, boolean reduce) {
        return Motion.progress(since, now, Motion.CHIP_SWAP, Easing.OUT_QUAD, reduce);
    }

    /** A health fill easing from where it was to where it is going; start one with the first health and it begins at rest. */
    public static final class Tween {
        private float from;
        private float to;
        private double since = Double.NEGATIVE_INFINITY;

        public Tween(float initial) {
            this.from = initial;
            this.to = initial;
        }

        /** The health the fill is going to now; a new target starts from wherever the fill is. */
        public void target(float target, double now, boolean reduce) {
            if (target == to) {
                return;
            }
            from = value(now, reduce);
            to = target;
            since = now;
        }

        public float value(double now, boolean reduce) {
            return from + (to - from) * Motion.progress(since, now, Motion.BOSS_HEALTH, Easing.OUT_CUBIC, reduce);
        }

        public float goal() {
            return to;
        }
    }
}
