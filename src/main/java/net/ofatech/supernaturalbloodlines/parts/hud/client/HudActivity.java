package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.Minecraft;

/**
 * Whether the player is using the HUD: a slot key, the mode key, Alt, or a slot that is cooling down or active. After {@link #IDLE_TICKS} of none of these
 * the slots fade to {@link #FADED}, so the HUD does not stand out while it is not wanted.
 */
public final class HudActivity {
    public static final int IDLE_TICKS = 200;
    public static final float FADED = 0.4f;
    private static long last;

    private HudActivity() {
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** Forgets the last use: a new world starts with a full-strength HUD whatever its game time. */
    static void reset() {
        last = now();
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
        long idle = now() - last;
        return alphaFor(idle < 0 ? 0 : idle); // a world younger than the last use (game time restarts) counts as just used
    }

    /** The alpha {@code idleTicks} after the last use; pure. */
    public static float alphaFor(long idleTicks) {
        return idleTicks > IDLE_TICKS ? FADED : 1f;
    }
}
