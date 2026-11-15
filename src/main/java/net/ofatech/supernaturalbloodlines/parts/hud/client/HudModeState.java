package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import java.util.Optional;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;

/**
 * Which source the six slots show now. The start mode comes from the client {@code mode} setting; the {@code hud_mode} key (Alt + Caps Lock) toggles it
 * when both Tensura and Vampirism are installed. 13b Task 4 adds the action bar line and remembers it between sessions.
 */
public final class HudModeState {
    private static final List<SlotMode> AVAILABLE = SlotMode.available(ModList.get().isLoaded("tensura"), ModList.get().isLoaded("vampirism"));
    private static SlotMode mode;

    private HudModeState() {
    }

    public static List<SlotMode> available() {
        return AVAILABLE;
    }

    /** The current mode, empty when neither mod is installed (no HUD). */
    public static Optional<SlotMode> current() {
        if (mode == null || !AVAILABLE.contains(mode)) {
            mode = SlotMode.start(HudClientConfig.mode().name(), AVAILABLE).orElse(null);
        }
        return Optional.ofNullable(mode);
    }

    public static void toggle() {
        mode = SlotMode.next(current().orElse(SlotMode.TENSURA), AVAILABLE).orElse(null);
    }
}
