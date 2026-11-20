package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * How visible the mode pill is. It is not drawn all the time (too much clutter): it shows for {@link #HOLD_TICKS} after a mode switch, then fades out over
 * {@link #FADE_TICKS}, and it shows while Alt is held, whatever else is on screen (it then lifts above an action bar message, see {@link HudLayout#pill}). After a switch it gives way to an action bar message, which stands on the same pixels.
 * Pure.
 */
public final class PillVisibility {
    public static final int HOLD_TICKS = 60;
    public static final int FADE_TICKS = 20;

    private PillVisibility() {
    }

    /**
     * @param ticksSinceSwitch ticks since the mode key was pressed, negative when it never was in this world
     * @param alt              Alt is held
     * @param actionBarMessage an action bar message is on screen
     * @return 0 (not drawn) to 1 (full)
     */
    public static float alpha(long ticksSinceSwitch, boolean alt, boolean actionBarMessage) {
        if (alt) {
            return 1f; // asked for on purpose: it moves out of the message's way ({@link HudLayout#pill}) instead of giving way
        }
        if (actionBarMessage) {
            return 0f;
        }
        if (ticksSinceSwitch < 0 || ticksSinceSwitch >= HOLD_TICKS + FADE_TICKS) {
            return 0f;
        }
        return ticksSinceSwitch < HOLD_TICKS ? 1f : 1f - (ticksSinceSwitch - HOLD_TICKS) / (float) FADE_TICKS;
    }
}
