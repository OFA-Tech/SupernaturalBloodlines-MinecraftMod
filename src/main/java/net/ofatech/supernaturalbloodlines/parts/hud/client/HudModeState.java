package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    /** The mode key: switches, says which mode it is now in the action bar, and remembers it. Nothing changes with only one mode installed. */
    public static void toggle() {
        SlotMode before = mode;
        mode = SlotMode.next(current().orElse(SlotMode.TENSURA), AVAILABLE).orElse(null);
        if (mode != null && AVAILABLE.size() > 1 && mode != before) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("supernaturalbloodlines.hud.mode." + mode.name().toLowerCase(Locale.ROOT)), false);
            HudClientConfig.rememberMode(HudClientConfig.Mode.valueOf(mode.name()));
        }
    }
}
