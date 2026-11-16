package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.Minecraft;

/**
 * Whether the player is using the HUD: a slot key, the mode key, Alt, or a slot that is cooling down or active. After {@link #IDLE_TICKS} of none of these
 * the slots fade to {@link #FADED}, so the HUD does not stand out while it is not wanted.
 */
public final class HudActivity {
    public static final int IDLE_TICKS = 200;
    public static final float FADED = 0.4f;
    private static final long UNSET = Long.MIN_VALUE;
    private static long last = UNSET;

    private HudActivity() {
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** Forgets the last use: a new world starts with a full-strength HUD whatever its game time. */
    static void reset() {
        last = UNSET; // armed again by the first frame in the new world (the old level may still be set while this runs)
    }

    /** A key or the mode switch just happened. */
    public static void touch() {
        last = now();
    }

    /** 1 while the HUD is in use, {@link #FADED} after ten seconds of nothing. */
    public static float alpha(boolean busy) {
        if (net.ofatech.supernaturalbloodlines.shared.hud.DevHud.on) {
            return net.ofatech.supernaturalbloodlines.shared.hud.DevHud.idle ? FADED : 1f;
        }
        if (busy) {
            touch();
        }
        if (last == UNSET) {
            touch();
        }
        return alphaFor(idleTicks(last, now()));
    }

    /** Ticks since the last use; a use that is "in the future" (game time restarts in every world) or never happened counts as just now. Pure. */
    static long idleTicks(long last, long now) {
        return last == UNSET || now < last ? 0 : now - last;
    }

    /** The alpha {@code idleTicks} after the last use; pure. */
    public static float alphaFor(long idleTicks) {
        return idleTicks > IDLE_TICKS ? FADED : 1f;
    }
}
