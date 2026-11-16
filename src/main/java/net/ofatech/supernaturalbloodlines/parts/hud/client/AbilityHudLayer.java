package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;

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
        HudSource src = HudSource.current();
        HudStyle.Style style = HudStyle.current();
        HudLayout.Box[] boxes = HudLayout.slots(g.guiWidth(), g.guiHeight());
        SlotView[] views = src.views();
        boolean alt = src.alt();
        boolean busy = alt || HudModeState.justSwitched();
        for (SlotView v : views) {
            busy |= v.cooldown() > 0f || v.active();
        }
        float alpha = HudActivity.alpha(busy);
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.25);
        float blood = src.blood();
        for (int i = 0; i < boxes.length; i++) {
            boolean bite = !views[i].isEmpty() && ClientLoadout.BITE_ICON.equals(views[i].icon());
            SlotArt.draw(g, mc.font, style, boxes[i], views[i], alpha, bite ? blood : -1f, pulse);
        }
        int previewTop = -1;
        SlotMode mode = src.mode();
        if (mode != null) {
            HudChrome.pill(g, mc.font, style, mode, g.guiWidth(), g.guiHeight(), alpha);
            SlotMode other = src.other();
            if ((alt || HudModeState.justSwitched()) && other != null) {
                previewTop = HudChrome.preview(g, mc.font, style, other, src.otherViews(other), boxes, Math.max(alpha, 0.9f));
            }
        }
        long moon = src.moonTicks();
        if (moon > 0) {
            HudChrome.moon(g, mc.font, style, moon, g.guiWidth(), pulse);
        }
        src.tracked().ifPresent(l -> HudChrome.tracker(g, mc.font, style, l, alpha, HudClientConfig.trackerAnchor(), src.bossBottom()));
        if (alt) {
            HudChrome.tooltips(g, mc.font, style, boxes, views, previewTop, alpha, src.cacheable());
        }
    }
}