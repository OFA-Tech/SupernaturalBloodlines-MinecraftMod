package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;

/**
 * Which source the six slots show now. The start mode comes from the client {@code mode} setting; the {@code hud_mode} key (Caps Lock) toggles it
 * when both Tensura and Vampirism are installed, says the new mode in the action bar and remembers it between sessions.
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

    private static long switchedAt = Long.MIN_VALUE;

    /** True for three seconds after the mode key was pressed: the panel with the other mode's slots stays up for that long. */
    public static boolean justSwitched() {
        var level = Minecraft.getInstance().level;
        return level != null && within(switchedAt, level.getGameTime());
    }

    /**
     * Whether {@code now} is within three seconds after {@code switchedAt}. Game time restarts in every world, so a world that is younger than the last switch
     * gives a negative difference, which is not "just switched".
     */
    static boolean within(long switchedAt, long now) {
        long d = now - switchedAt;
        return switchedAt != Long.MIN_VALUE && d >= 0 && d < 60;
    }

    /** Forgets the last switch and the last use (logout): the next world starts with no panel and a full-strength HUD. */
    public static void reset() {
        switchedAt = Long.MIN_VALUE;
        HudActivity.reset();
    }

    /** The mode key: switches, says which mode it is now in the action bar, and remembers it. Nothing changes with only one mode installed. */
    public static void toggle() {
        SlotMode before = mode;
        var level = Minecraft.getInstance().level;
        if (level != null) {
            switchedAt = level.getGameTime();
        }
        HudActivity.touch();
        mode = SlotMode.next(current().orElse(SlotMode.TENSURA), AVAILABLE).orElse(null);
        if (mode != null && AVAILABLE.size() > 1 && mode != before) {
            Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("supernaturalbloodlines.hud.mode." + mode.name().toLowerCase(Locale.ROOT)), false);
            HudClientConfig.rememberMode(HudClientConfig.Mode.valueOf(mode.name()));
        }
    }
}
