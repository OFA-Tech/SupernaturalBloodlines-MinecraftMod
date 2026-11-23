package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * The motion rules of the style guide as numbers: durations in client ticks (20 a second), and the pure maths that turns a start time into a progress, a
 * whole-texel offset or a glow band. Everything takes {@code reduce} (the {@code reduce_motion} setting): with it on, a change is already finished and a
 * pulse is held, so a state change still lands in one frame. Information (the cooldown sweep) does not call this at all. Pure.
 */
public final class Motion {
    /** Used now: the Grimoire's chosen tab slides out in this many ticks. */
    public static final int PAGE_CHANGE = 4;
    // The rest of the event table of the style guide, here for the phase that animates it (each is read by that phase's code and test):
    /** Phase 5: the Grimoire's page turn. */
    public static final int PAGE_TURN = 6;
    /** Phase 4: a slot pressed (down, then up). */
    public static final int PRESS_DOWN = 2;
    public static final int PRESS_UP = 3;
    /** Phase 5: a skill unlocked or a quest claimed. */
    public static final int UNLOCK = 12;
    /** Phase 6: a boss bar's health easing. */
    public static final int BOSS_HEALTH = 10;
    /** Phase 6: a form chip of a boss bar being lit. */
    public static final int CHIP_SWAP = 4;
    /** Phase 5: tooltips appear after a delay and fade in. */
    public static final int TOOLTIP_DELAY = 10;
    public static final int TOOLTIP_FADE = 3;
    /** Phase 4: the ready glow of a slot, one loop in ticks. */
    public static final int GLOW_LOOP = 30;

    /** The alpha bands of the ready glow over one loop (32, 50, 60, 50 percent); reduce motion holds the brightest. Phase 4 draws the slot glow with it. */
    private static final int[] GLOW_BANDS = {32, 50, 60, 50};
    public static final int GLOW_HELD = 60;

    private Motion() {
    }

    /** How far a tween that began at {@code start} has come at {@code now} (both in ticks), eased, from 0 to 1. Reduce motion: always 1. */
    public static float progress(double start, double now, int duration, Easing easing, boolean reduce) {
        if (reduce || duration <= 0) {
            return 1f;
        }
        return easing.apply((float) ((now - start) / duration));
    }

    /** A position between two whole texels, in a whole texel: nothing of pixel art is ever drawn at a fraction. */
    public static int texels(int from, int to, float progress) {
        return from + Math.round((to - from) * progress);
    }

    /** The HUD's breathing halo, 0 to 1, at {@code tick}: a sine of 0.25 radians a tick. With reduce motion it is held at its brightest. */
    public static float pulse(long tick, boolean reduce) {
        if (reduce) {
            return 1f;
        }
        return 0.5f + 0.5f * (float) Math.sin(tick * 0.25);
    }

    /** The glow of a ready target in percent alpha at {@code tick}. */
    public static int glowPercent(long tick, boolean reduce) {
        if (reduce) {
            return GLOW_HELD;
        }
        int phase = (int) Math.floorMod(tick, (long) GLOW_LOOP);
        return GLOW_BANDS[phase * GLOW_BANDS.length / GLOW_LOOP];
    }
}
