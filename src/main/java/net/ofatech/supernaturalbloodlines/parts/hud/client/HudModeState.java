package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;

/**
 * Which source the six slots show now. The start mode comes from the client {@code mode} setting; the {@code hud_mode} key (Caps Lock) toggles it
 * when two modes are on offer (Tensura installed, and Vampirism installed with a supernatural player), and remembers it between sessions.
 */
public final class HudModeState {
    private static final boolean TENSURA = ModList.get().isLoaded("tensura");
    private static final boolean VAMPIRISM = ModList.get().isLoaded("vampirism");
    private static SlotMode mode;

    private HudModeState() {
    }

    /** The modes this player is offered: Supernatural only while the player is supernatural ({@link HudStanding}); a plain human has Tensura mode or no HUD. */
    public static List<SlotMode> available() {
        return SlotMode.available(TENSURA, VAMPIRISM, HudStanding.supernatural());
    }

    /** The current mode, empty when neither mod is installed (no HUD). */
    public static Optional<SlotMode> current() {
        List<SlotMode> available = available();
        if (mode == null || !available.contains(mode)) {
            mode = SlotMode.start(HudClientConfig.mode().name(), available).orElse(null);
        }
        return Optional.ofNullable(mode);
    }

    private static long switchedAt = Long.MIN_VALUE;

    /** True for three seconds after the mode key was pressed: the panel with the other mode's slots stays up for that long. */
    public static boolean justSwitched() {
        var level = Minecraft.getInstance().level;
        return level != null && within(switchedAt, level.getGameTime());
    }

    /** Ticks since the mode key was pressed in this world, negative when it was not. */
    public static long sinceSwitch() {
        var level = Minecraft.getInstance().level;
        long d = level == null ? -1 : level.getGameTime() - switchedAt;
        return switchedAt == Long.MIN_VALUE || d < 0 ? -1 : d;
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

    /** The mode key: switches, shows the panel of the other mode for three seconds, and remembers it. Nothing changes with only one mode on offer. */
    public static void toggle() {
        SlotMode before = mode;
        var level = Minecraft.getInstance().level;
        if (level != null) {
            switchedAt = level.getGameTime();
        }
        HudActivity.touch();
        List<SlotMode> available = available();
        mode = SlotMode.next(current().orElse(SlotMode.TENSURA), available).orElse(null);
        // no action bar line: it was drawn over the mode pill at the same place on the screen; the pill and the panel already say which mode it is
        if (mode != null && available.size() > 1 && mode != before) {
            HudClientConfig.rememberMode(HudClientConfig.Mode.valueOf(mode.name()));
        }
    }
}
