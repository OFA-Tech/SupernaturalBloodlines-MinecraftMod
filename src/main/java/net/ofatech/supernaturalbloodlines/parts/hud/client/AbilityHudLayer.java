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
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;

/**
 * The two columns of three slots, drawn above the hotbar: the slot's icon, its key in the corner, the cooldown as a dark fill from the top with the
 * seconds, an outline while it is active, half transparent when it cannot be used. Reads the views that {@link HudSlots} built this tick; builds nothing
 * but the tooltip lists while Alt is held. Hidden with the GUI, in spectator mode, with {@code hud_enabled} off, and when neither Tensura nor Vampirism is installed.
 */
public final class AbilityHudLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "ability_hud");
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation ACTIVE = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/slot_active.png");

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
        HudLayout.Box[] boxes = HudLayout.slots(g.guiWidth(), g.guiHeight());
        SlotView[] views = HudSlots.views();
        for (int i = 0; i < boxes.length; i++) {
            draw(g, mc.font, boxes[i], views[i]);
        }
        if (Screen.hasAltDown() && mc.screen == null) {
            tooltips(g, mc.font, boxes, views);
        }
    }

    private static void draw(GuiGraphics g, Font font, HudLayout.Box b, SlotView v) {
        g.blitSprite(HOTBAR, 182, 22, 0, 0, b.x(), b.y(), HudLayout.SLOT, HudLayout.SLOT);
        if (!v.isEmpty()) {
            ClientIcons.draw(g, v.icon(), b.x() + 3, b.y() + 3, 16, v.greyed() ? 0.4f : 1f);
            if (v.cooldown() > 0f) {
                g.fill(b.x() + 3, b.y() + 3, b.x() + 19, b.y() + 3 + Math.round(16 * v.cooldown()), 0xB0000000);
                if (v.cooldownSeconds() > 0) {
                    g.drawCenteredString(font, Integer.toString(v.cooldownSeconds()), b.x() + 11, b.y() + 7, 0xFFFFFFFF);
                }
            }
            if (v.active()) {
                g.blit(ACTIVE, b.x(), b.y(), 0, 0, HudLayout.SLOT, HudLayout.SLOT, HudLayout.SLOT, HudLayout.SLOT);
            }
        }
        String key = v.keyLabel();
        if (!key.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(b.x() + 2, b.y() + 2, 0);
            g.pose().scale(0.7f, 0.7f, 1f);
            g.drawString(font, key.length() > 3 ? key.substring(0, 3) : key, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
    }

    /** One small panel per filled slot, beside its column: the name (and the mode), then the cooldown or why it cannot be used. */
    private static void tooltips(GuiGraphics g, Font font, HudLayout.Box[] boxes, SlotView[] views) {
        for (int i = 0; i < boxes.length; i++) {
            SlotView v = views[i];
            if (v.isEmpty()) {
                continue;
            }
            List<Component> lines = new ArrayList<>(3);
            lines.add(Component.literal(v.detail() == null ? v.name() : v.name() + ": " + v.detail()));
            if (v.greyed()) {
                lines.add(Component.translatable(v.greyedReason()).withStyle(net.minecraft.ChatFormatting.RED));
            } else if (v.cooldownSeconds() > 0) {
                lines.add(Component.translatable("supernaturalbloodlines.hud.slot.cooldown", v.cooldownSeconds()).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            int width = 0;
            for (Component line : lines) {
                width = Math.max(width, font.width(line));
            }
            HudLayout.Box b = boxes[i];
            boolean right = i >= 3;
            // renderComponentTooltip puts the panel 12 right of the point and 12 above it, with 4 of padding
            int x = right ? b.x() - 4 - 8 - width - 12 : b.right() + 4 - 12;
            g.renderComponentTooltip(font, lines, x, b.y() + 12);
        }
    }
}
