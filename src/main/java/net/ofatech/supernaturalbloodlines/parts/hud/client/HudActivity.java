package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.Minecraft;
import net.ofatech.supernaturalbloodlines.core.hud.HudVisibility;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;

/**
 * Whether the player is using the HUD: a slot key, the mode key, Alt, or a slot that is cooling down or active. With {@code hud_visibility = AUTO}, after
 * {@link #IDLE_TICKS} of none of these the HUD fades out and stays gone until the next use; {@code ALWAYS} and {@code HIDDEN} ignore the use ({@link HudVisibility}).
 * The fade is the transition between the two.
 */
public final class HudActivity {
    public static final int IDLE_TICKS = HudVisibility.IDLE_TICKS;
    /** The opacity of the lines that stay while the slots are gone (the tracked task). */
    public static final float FADED = 0.4f;
    private static final long UNSET = Long.MIN_VALUE;
    private static long last = UNSET;
    private static float shown = 1f;
    private static long shownAt = UNSET;

    private HudActivity() {
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** Forgets the last use: a new world starts with a full-strength HUD whatever its game time. */
    static void reset() {
        last = UNSET; // armed again by the first frame in the new world (the old level may still be set while this runs)
        shown = 1f;
        shownAt = UNSET;
    }

    /** A key or the mode switch just happened. */
    public static void touch() {
        last = now();
    }

    /** The HUD's opacity this frame, 1 while it is in use (or always shown), 0 once it has faded out. */
    public static float alpha(boolean busy) {
        if (DevHud.on) {
            return DevHud.idle ? 0f : 1f;
        }
        if (busy || last == UNSET) {
            touch();
        }
        long now = now();
        float target = HudClientConfig.visibility().target(idleTicks(last, now));
        shown = HudVisibility.step(shown, shownAt != UNSET, shownAt, now, target);
        shownAt = now;
        return shown;
    }

    /** Ticks since the last use; a use that is "in the future" (game time restarts in every world) or never happened counts as just now. Pure. */
    static long idleTicks(long last, long now) {
        return last == UNSET || now < last ? 0 : now - last;
    }
}
