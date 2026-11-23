package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.AltHold;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Motion;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReadyGlow;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ReduceMotion;
import net.ofatech.supernaturalbloodlines.core.hud.PillVisibility;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
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
        return HudClientConfig.hudEnabled() && HudClientConfig.visibility() != net.ofatech.supernaturalbloodlines.core.hud.HudVisibility.HIDDEN && mc.player != null && !mc.options.hideGui && !mc.player.isSpectator() && HudModeState.current().isPresent();
    }

    /** The tick Alt went down, or -1 while it is up: the panels follow a second later. */
    private long altSince = -1;
    private final ReadyGlow.Tracker glowTracker = new ReadyGlow.Tracker(Loadout.SLOTS);
    private final float[] cooldowns = new float[Loadout.SLOTS];
    private final boolean[] eligible = new boolean[Loadout.SLOTS];
    private final float[] glow = new float[Loadout.SLOTS];

    /** What each slot glows with: a slot that turns ready glows once; a mode switch or a change of loadout starts the tracking over. */
    private float[] glows(SlotMode mode, SlotView[] views, long time) {
        boolean reduce = ReduceMotion.on();
        int context = mode == null ? 0 : mode.ordinal() + 1;
        for (int i = 0; i < glow.length; i++) {
            SlotView v = views[i];
            eligible[i] = !v.isEmpty() && !v.greyed();
            cooldowns[i] = eligible[i] ? v.cooldown() : 0f;
            context = 31 * context + (v.isEmpty() ? 0 : v.icon().hashCode() * 17 + v.name().hashCode());
        }
        glowTracker.update(cooldowns, eligible, context, time, reduce, glow);
        if (net.ofatech.supernaturalbloodlines.shared.hud.DevHud.on && net.ofatech.supernaturalbloodlines.shared.hud.DevHud.glow && !reduce) {
            glow[net.ofatech.supernaturalbloodlines.shared.hud.DevHud.human ? 0 : 3] = 0.6f; // the screenshots: one slot is ready, held at a fixed glow
        }
        return glow;
    }
    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (Minecraft.getInstance().screen instanceof net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.WheelScreen) {
            return; // the wheel lists the same powers: the six slots would only sit under its rings
        }
        net.ofatech.supernaturalbloodlines.shared.hud.HudPlacement.toastAnchor(HudClientConfig.toastAnchor());
        if (!shown()) {
            glowTracker.reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        HudSource src = HudSource.current();
        net.ofatech.supernaturalbloodlines.shared.hud.HudPlacement.bossBottom(src.bossBottom());
        HudStyle.Style style = HudStyle.current();
        HudLayout.Box[] boxes = HudLayout.slots(g.guiWidth(), g.guiHeight());
        SlotView[] views = src.views();
        boolean alt = src.alt();
        long nowTick = mc.level == null ? 0 : mc.level.getGameTime();
        altSince = AltHold.since(altSince, alt, nowTick);
        // a dev shot holds Alt for good: it says for itself whether the second has passed
        boolean detail = net.ofatech.supernaturalbloodlines.shared.hud.DevHud.on ? net.ofatech.supernaturalbloodlines.shared.hud.DevHud.detail : AltHold.detailed(altSince, nowTick);
        boolean busy = alt || HudModeState.justSwitched();
        for (SlotView v : views) {
            busy |= v.cooldown() > 0f || v.active();
        }
        float alpha = HudActivity.alpha(busy);
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        float pulse = Motion.pulse(time, ReduceMotion.on());
        float[] glow = glows(src.mode(), views, time);
        int previewTop = -1;
        if (alpha > 0.01f) {
            float blood = src.blood();
            for (int i = 0; i < boxes.length; i++) {
                boolean bite = !views[i].isEmpty() && ClientLoadout.BITE_ICON.equals(views[i].icon());
                SlotArt.draw(g, mc.font, style, SlotShape.of(src.mode()), boxes[i], views[i], alpha, bite ? blood : -1f, pulse, glow[i]);
            }
            SlotMode mode = src.mode();
            if (mode != null) {
                // the pill is not drawn all the time: after a mode switch, and while Alt is held
                float pillAlpha = PillVisibility.alpha(src.sinceSwitch(), alt, src.actionBar());
                if (pillAlpha > 0f) {
                    HudChrome.pill(g, mc.font, style, mode, g.guiWidth(), g.guiHeight(), Math.max(alpha, 0.9f) * pillAlpha, alt && src.actionBar());
                }
                SlotMode other = src.other();
                if ((alt || HudModeState.justSwitched()) && other != null) {
                    previewTop = HudChrome.preview(g, mc.font, style, other, src.otherViews(other), boxes, Math.max(alpha, 0.9f));
                }
            }
        }
        long moon = src.moonTicks();
        if (moon > 0) {
            HudChrome.moon(g, mc.font, style, moon, g.guiWidth(), pulse);
        }
        src.tracked().ifPresent(l -> HudChrome.tracker(g, mc.font, style, l, Math.max(alpha, HudActivity.FADED), HudClientConfig.trackerAnchor(), src.bossBottom()));
        if (alt && alpha > 0.01f) {
            HudChrome.labels(g, mc.font, style, boxes, views, previewTop, alpha, src.cacheable(), src.actionBar(), detail);
        }
    }
}