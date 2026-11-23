package net.ofatech.supernaturalbloodlines.core.wheel;

import java.util.Set;

/**
 * Which on-screen layers are not drawn while the wheel is open: every one but the world's own overlays. The wheel fills the screen, and the HUD of any mod (ours, Tensura's and
 * Vampirism's status, the hotbar, the boss bars, the chat) would sit under its rings and pills. A layer of a mod added later is hidden too, with no list to keep up. Pure.
 */
public final class WheelHud {
    /** Drawn even while the wheel is open: the vignette, frost and pumpkin overlays, and the debug screen a developer asked for. */
    public static final Set<String> KEPT = Set.of("minecraft:camera_overlays", "minecraft:debug_overlay");

    private WheelHud() {
    }

    public static boolean hides(boolean wheelOpen, String layerId) {
        return wheelOpen && !KEPT.contains(layerId);
    }
}
