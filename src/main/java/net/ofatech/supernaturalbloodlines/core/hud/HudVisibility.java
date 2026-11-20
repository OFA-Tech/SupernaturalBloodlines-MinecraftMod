package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * The client setting {@code hud_visibility}. AUTO shows the HUD while it is in use and fades it out after {@link #IDLE_TICKS} of nothing; ALWAYS keeps it
 * on screen; HIDDEN never draws it. Pure.
 */
public enum HudVisibility {
    AUTO,
    ALWAYS,
    HIDDEN;

    /** Ten seconds with no slot press, cooldown, running toggle, mode switch or Alt. */
    public static final int IDLE_TICKS = 200;
    /** The fade out takes this many ticks, the fade in a quarter of that: a key press must not wait for the HUD. */
    public static final int FADE_OUT_TICKS = 20;
    public static final int FADE_IN_TICKS = 5;

    /** The opacity the HUD is heading for {@code idleTicks} after the last use. */
    public float target(long idleTicks) {
        return switch (this) {
            case ALWAYS -> 1f;
            case HIDDEN -> 0f;
            case AUTO -> idleTicks > IDLE_TICKS ? 0f : 1f;
        };
    }

    /** {@code current} moved {@code ticks} game ticks toward {@code target}, so the HUD fades instead of jumping. */
    public static float approach(float current, float target, long ticks) {
        if (ticks <= 0 || current == target) {
            return current;
        }
        boolean in = target > current;
        float step = ticks / (float) (in ? FADE_IN_TICKS : FADE_OUT_TICKS);
        return in ? Math.min(target, current + step) : Math.max(target, current - step);
    }
}
