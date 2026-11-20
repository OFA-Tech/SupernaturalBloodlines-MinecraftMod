package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.ModeMemory;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;

/**
 * Which source the six slots show now. The start mode comes from the client {@code mode} setting; the {@code hud_mode} key (Caps Lock) toggles it
 * when two modes are on offer (Tensura installed, and Vampirism installed with a supernatural player), and remembers it between sessions.
 */
public final class HudModeState {
    private static final boolean TENSURA = ModList.get().isLoaded("tensura");
    private static final boolean VAMPIRISM = ModList.get().isLoaded("vampirism");
    private static final ModeMemory MEMORY = new ModeMemory();

    private HudModeState() {
    }

    /** The modes this player is offered: Supernatural only while the player is supernatural ({@link HudStanding}); a plain human has Tensura mode or no HUD. */
    public static List<SlotMode> available() {
        return SlotMode.available(TENSURA, VAMPIRISM, HudStanding.supernatural());
    }

    /** The current mode, empty when no mode is on offer (no HUD). Reading it never changes the player's choice ({@link ModeMemory}). */
    public static Optional<SlotMode> current() {
        return MEMORY.current(HudClientConfig.mode().name(), available());
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
        net.ofatech.supernaturalbloodlines.parts.hud.net.ActionCooldowns.clear();
    }

    /**
     * The mode key: switches, shows the panel of the other mode for three seconds, says the new mode to the narrator, and remembers the choice (here and in
     * the client setting). With one mode on offer it does nothing, except for a human with Tensura, who is told when Supernatural mode unlocks.
     */
    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        List<SlotMode> available = available();
        if (!ModeMemory.canToggle(available)) {
            if (ModeMemory.lockedHint(TENSURA, VAMPIRISM, HudStanding.supernatural())) {
                mc.gui.setOverlayMessage(Component.translatable("supernaturalbloodlines.hud.mode.locked"), false);
            }
            return;
        }
        var level = mc.level;
        if (level != null) {
            switchedAt = level.getGameTime();
        }
        HudActivity.touch();
        SlotMode next = SlotMode.next(current().orElse(SlotMode.TENSURA), available).orElse(null);
        MEMORY.choose(next);
        if (next != null) {
            // the pill is not narrated, so the switch is said once to the narrator
            mc.getNarrator().sayNow(Component.translatable("supernaturalbloodlines.hud.mode." + next.name().toLowerCase(Locale.ROOT)));
            HudClientConfig.rememberMode(HudClientConfig.Mode.valueOf(next.name()));
        }
    }
}
