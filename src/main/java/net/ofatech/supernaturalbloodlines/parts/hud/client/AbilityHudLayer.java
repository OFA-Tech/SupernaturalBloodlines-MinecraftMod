package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.VampireBlood;
import net.ofatech.supernaturalbloodlines.parts.hud.client.werewolves.WerewolfCheck;
import net.ofatech.supernaturalbloodlines.core.hud.MoonClock;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.neoforged.fml.ModList;
import java.util.Optional;

/**
 * The two columns of three slots, drawn above the hotbar: the slot's icon, its key in the corner, the cooldown as a dark fill from the top with the
 * seconds, an outline while it is active, half transparent when it cannot be used. Reads the views that {@link HudSlots} built this tick; builds nothing
 * but the tooltip lists while Alt is held. Hidden with the GUI, in spectator mode, with {@code hud_enabled} off, and when neither Tensura nor Vampirism is installed.
 */
public final class AbilityHudLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ability_hud");

    public static void register(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.HOTBAR, ID, new AbilityHudLayer());
    }

    /** True while the HUD is on screen; Tensura's own skill HUD is hidden exactly then. */
    public static boolean shown() {
        Minecraft mc = Minecraft.getInstance();
        return HudClientConfig.hudEnabled() && mc.player != null && !mc.options.hideGui && !mc.player.isSpectator() && HudModeState.current().isPresent();
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (!shown()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        HudStyle.Style style = HudStyle.current();
        HudLayout.Box[] boxes = HudLayout.slots(g.guiWidth(), g.guiHeight());
        SlotView[] views = HudDemo.on() ? HudDemo.views() : HudSlots.views();
        boolean alt = (Screen.hasAltDown() && mc.screen == null) || HudDemo.alt();
        boolean busy = alt || HudModeState.justSwitched();
        for (SlotView v : views) {
            busy |= v.cooldown() > 0f || v.active();
        }
        float alpha = HudActivity.alpha(busy);
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.25);
        float blood = bloodShare();
        for (int i = 0; i < boxes.length; i++) {
            boolean bite = !views[i].isEmpty() && ClientLoadout.BITE_ICON.equals(views[i].icon());
            SlotArt.draw(g, mc.font, style, boxes[i], views[i], alpha, bite ? blood : -1f, pulse);
        }
        SlotMode mode = HudDemo.on() ? HudDemo.mode() : HudModeState.current().orElse(null);
        if (mode != null) {
            HudChrome.pill(g, mc.font, style, mode, g.guiWidth(), g.guiHeight(), alpha);
            if ((alt || HudModeState.justSwitched()) && (HudDemo.on() || HudModeState.available().size() > 1)) {
                SlotMode other = HudDemo.on() ? SlotMode.TENSURA : SlotMode.next(mode, HudModeState.available()).orElse(mode);
                HudChrome.preview(g, mc.font, style, other, HudDemo.on() ? HudDemo.otherViews() : HudSlots.preview(other), boxes, Math.max(alpha, 0.9f));
            }
        }
        long moon = moonTicks(mc);
        if (moon > 0 && moon <= MOON_WARNING_TICKS) {
            HudChrome.moon(g, mc.font, style, moon, g.guiWidth(), pulse);
        }
        if (HudDemo.on() || TrackerAccess.available()) {
            Optional<TrackerAccess.Line> line = HudDemo.on() ? Optional.of(HudDemo.tracked()) : TrackerAccess.line();
            line.ifPresent(l -> HudChrome.tracker(g, mc.font, style, l, alpha));
        }
        if (alt) {
            HudChrome.tooltips(g, mc.font, style, boxes, views, mode != null && (HudDemo.on() || HudModeState.available().size() > 1), alpha);
        }
    }

    /** The warning shows from five minutes before the forced shift. */
    private static final long MOON_WARNING_TICKS = 6_000;

    /** The share (0 to 1) of the blood ring round Bite, or a negative number when the player has no blood to show. */
    private static float bloodShare() {
        if (HudDemo.on()) {
            return HudDemo.blood();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ModList.get().isLoaded("vampirism") && VampireBlood.isVampire(mc.player)) {
            return Math.max(0f, Math.min(1f, VampireBlood.level(mc.player) / 20f));
        }
        return -1f;
    }

    /** Ticks until the full moon forces a werewolf's shift (more than 0), from the world clock; 0 when there is no warning to give. */
    private static long moonTicks(Minecraft mc) {
        if (HudDemo.on()) {
            return HudDemo.moonTicks();
        }
        if (mc.player == null || mc.level == null || !ModList.get().isLoaded("werewolves") || !ModList.get().isLoaded("vampirism") || !WerewolfCheck.isWerewolf(mc.player)) {
            return 0;
        }
        return Math.max(0, MoonClock.ticksUntilForcedShift(mc.level.getDayTime()));
    }
}
