package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import net.minecraft.client.Minecraft;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.AnimClock;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Easing;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Motion;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;

/** The motion rules on the client tick: one clock for the screens, and the {@code reduce_motion} setting read for every call. Client only. */
public final class UiMotion {
    private static final AnimClock CLOCK = new AnimClock();

    private UiMotion() {
    }

    /** The client tick count: every motion runs on it (20 a second), so it is the same on a slow or a fast machine. */
    public static long tick() {
        return Minecraft.getInstance().gui.getGuiTicks();
    }

    /** The eased progress (0 to 1) of the element {@code id} changing into {@code state}; 1 when it is at rest or motion is reduced. */
    public static float progress(String id, Object state, int duration, Easing easing) {
        return CLOCK.progress(id, state, tick(), duration, easing, ReduceMotion.on());
    }

    /** A slide of up to {@code texels} whole texels while {@code on}, back to 0 when it goes off: the offset to draw at now. */
    public static int slide(String id, boolean on, int texels, int duration, Easing easing) {
        float p = progress(id, on, duration, easing);
        return on ? Motion.texels(0, texels, p) : Motion.texels(texels, 0, p);
    }

    /** The alpha percent of a ready glow now (held at its brightest with reduce motion on). */
    public static int glowPercent() {
        return Motion.glowPercent(tick(), ReduceMotion.on());
    }

    /** Forget every element: a screen that closes starts the next one at rest. */
    public static void clear() {
        CLOCK.clear();
    }
}
