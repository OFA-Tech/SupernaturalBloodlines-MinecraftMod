package net.ofatech.supernaturalbloodlines.core;

import org.lwjgl.glfw.GLFW;

/**
 * Development only: the one place that decides what the hidden-window screenshot mode ({@code -Dsupernaturalbloodlines.devshots.hidden=true})
 * may do, so the mixin, its plugin and the screenshot driver cannot disagree. No Minecraft class is touched here (the GLFW constants are
 * inlined by the compiler), so the mixin plugin can read it long before the game loads.
 */
public final class DevShotsGate {
    public static final String HIDDEN_PROPERTY = "supernaturalbloodlines.devshots.hidden";

    private DevShotsGate() {}

    public static boolean hidden() {
        return Boolean.getBoolean(HIDDEN_PROPERTY);
    }

    /** The GLFW window hints (hint, value) to set right before the window is created: none unless hidden mode is on. */
    public static int[][] windowHints(boolean hidden) {
        if (!hidden) {
            return new int[0][];
        }
        return new int[][] {
            {GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE},
            {GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE},
            {GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE},
            {GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE}
        };
    }

    /** A hidden window is created at the size of {@code --width/--height}; resizing it is a call on a window nobody should touch. */
    public static boolean mayResizeWindow(boolean hidden) {
        return !hidden;
    }

    /** GLFW ignores the call on an unfocused window; this makes sure it is never even made unless the window really is ours to move. */
    public static boolean mayMoveCursor(boolean hidden, boolean windowFocused) {
        return !hidden && windowFocused;
    }
}
