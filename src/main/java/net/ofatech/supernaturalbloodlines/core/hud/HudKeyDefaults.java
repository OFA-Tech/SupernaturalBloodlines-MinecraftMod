package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Our HUD keys' defaults as plain data, so a unit test can compare them without the game. The values are GLFW key codes (copied here,
 * since core has no LWJGL) and the modifier's {@code KeyModifier} name. {@code HudKeys} (client) builds the key mappings from this.
 */
public final class HudKeyDefaults {
    public static final int GLFW_C = 67;
    public static final int GLFW_F = 70;
    public static final int GLFW_G = 71;
    public static final int GLFW_R = 82;
    public static final int GLFW_V = 86;
    public static final int GLFW_X = 88;
    public static final int GLFW_Z = 90;
    public static final int GLFW_HOME = 268;
    public static final int GLFW_CAPS_LOCK = 280;
    public static final int GLFW_LEFT_ALT = 342;
    public static final int GLFW_LEFT_BRACKET = 91;

    public record Key(String id, int glfwKey, String modifier) {
    }

    private static final int[] SLOT_KEYS = {GLFW_R, GLFW_F, GLFW_V, GLFW_Z, GLFW_X, GLFW_C};

    /** Slots 1 to 3 are the left column and 4 to 6 the right one. */
    public static final List<Key> SLOTS = slots();
    public static final Key HUD_MODE = new Key("key.supernaturalbloodlines.hud_mode", GLFW_CAPS_LOCK, "NONE");
    /** Registered by the supernatural part ({@code HubKeybinds}); listed so the clash test sees it. */
    public static final Key HUB = new Key("key.supernaturalbloodlines.hub", GLFW_HOME, "NONE");
    /** Held to open the skill wheel (owner decision, 2026-11-22). */
    public static final Key WHEEL = new Key("key.supernaturalbloodlines.wheel", GLFW_G, "NONE");
    public static final List<Key> ALL = all();

    private HudKeyDefaults() {
    }

    private static List<Key> slots() {
        List<Key> keys = new ArrayList<>();
        for (int i = 0; i < SLOT_KEYS.length; i++) {
            keys.add(new Key("key.supernaturalbloodlines.slot_" + (i + 1), SLOT_KEYS[i], "NONE"));
        }
        return List.copyOf(keys);
    }

    private static List<Key> all() {
        List<Key> keys = new ArrayList<>(SLOTS);
        keys.add(HUD_MODE);
        keys.add(HUB);
        keys.add(WHEEL);
        return List.copyOf(keys);
    }
}
