package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.hud.MoonClock;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.VampireBlood;
import net.ofatech.supernaturalbloodlines.parts.hud.client.werewolves.WerewolfCheck;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/** The HUD's data from the running game. Bite's blood and the moon warning are worked out once a tick (by {@link HudSlots}), not once a frame. */
final class LiveHud implements HudSource {
    /** The warning shows from five minutes before the forced shift. */
    static final long MOON_WARNING_TICKS = 6_000;

    private static float blood = -1f;
    private static long moonTicks;

    /** Called once a client tick, with the player (null outside a world). */
    static void tick(Minecraft mc) {
        Player p = mc.player;
        blood = -1f;
        moonTicks = 0;
        if (p == null || mc.level == null) {
            return;
        }
        boolean vampirism = ModList.get().isLoaded("vampirism");
        if (vampirism && VampireBlood.isVampire(p)) {
            blood = Math.max(0f, Math.min(1f, VampireBlood.level(p) / 20f));
        }
        if (vampirism && ModList.get().isLoaded("werewolves") && WerewolfCheck.isWerewolf(p)) {
            long ticks = MoonClock.ticksUntilForcedShift(mc.level.dimensionType().hasFixedTime(), mc.level.getMoonPhase(), mc.level.getDayTime());
            moonTicks = ticks > 0 && ticks <= MOON_WARNING_TICKS ? ticks : 0;
        }
    }

    @Override
    public SlotView[] views() {
        return HudSlots.views();
    }

    @Override
    public SlotMode mode() {
        return HudModeState.current().orElse(null);
    }

    @Override
    public SlotMode other() {
        SlotMode mode = mode();
        return mode == null || HudModeState.available().size() < 2 ? null : SlotMode.next(mode, HudModeState.available()).orElse(null);
    }

    @Override
    public SlotView[] otherViews(SlotMode other) {
        return HudSlots.preview(other);
    }

    @Override
    public boolean alt() {
        Minecraft mc = Minecraft.getInstance();
        return net.minecraft.client.gui.screens.Screen.hasAltDown() && mc.screen == null;
    }

    @Override
    public long sinceSwitch() {
        return HudModeState.sinceSwitch();
    }

    @Override
    public boolean actionBar() {
        return Minecraft.getInstance().gui.overlayMessageTime > 0;
    }

    @Override
    public float blood() {
        return blood;
    }

    @Override
    public long moonTicks() {
        return moonTicks;
    }

    @Override
    public Optional<TrackerAccess.Line> tracked() {
        return TrackerAccess.available() ? TrackerAccess.line() : Optional.empty();
    }

    @Override
    public int bossBottom() {
        return BossBars.bottom();
    }
}
