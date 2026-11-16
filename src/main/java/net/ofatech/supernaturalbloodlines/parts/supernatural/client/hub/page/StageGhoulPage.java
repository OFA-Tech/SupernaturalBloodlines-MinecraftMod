package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubPageView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.HubScreen;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;

/**
 * A Ghoul's Path for the Blood Moon and Tensura: a grey moon with the time left to finish turning ("14 : 20"), and in the bar or the info pane what to do
 * (drink human blood) with a button to the Guide. Every other page is locked for them (tabs dimmed with a lock). The clock is the turning's own ticks,
 * synced with the bloodline state.
 */
public final class StageGhoulPage implements HubPageView {
    private static final String KEY = PageKit.HUB + "ghoul.";

    /** {@code ticks} as {@code "14 : 20"} (minutes and seconds); more than 99 minutes shows 99 : 59. */
    static String clock(int ticks) {
        int s = Math.min(99 * 60 + 59, Math.max(0, ticks) / 20);
        return String.format("%02d : %02d", s / 60, s % 60);
    }

    @Override
    public void render(HubScreen.Frame f) {
        HubTheme t = f.theme();
        BookKit.moonHead(f, Component.translatable(KEY + "title"), Component.translatable(KEY + "sub"));
        Rect stage = f.region(BookKit.MOON);
        int top = f.begin(BookKit.MOON);
        int r = Math.max(24, Math.min(stage.w(), stage.bottom() - top) / 2 - 14);
        int cx = stage.x() + stage.w() / 2;
        int cy = top + (stage.bottom() - top) / 2;
        disc(f.g, cx, cy, r);
        int ticks = f.view.bloodline().turningTicksLeft();
        String clock = clock(ticks);
        int scale = r >= 90 ? 3 : 2;
        var pose = f.g.pose();
        pose.pushPose();
        pose.translate(cx - f.font().width(clock) * scale / 2f, cy - 4 * scale - 4, 0);
        pose.scale(scale, scale, 1f);
        f.g.drawString(f.font(), clock, 0, 0, 0xFF0A0A0A, false);
        pose.popPose();
        Component left = Component.translatable(KEY + "left");
        f.g.drawString(f.font(), left, cx - f.font().width(left) / 2, cy + 4 * scale + 2, 0xFF0A0A0A, false);
        f.end(stage.bottom());
        f.detail(Detail.of(Component.translatable(KEY + "bar")).body(Component.translatable(KEY + "bar_text"))
                .action("guide", Component.translatable(KEY + "guide"), true, true, () -> f.screen().setPage(HubPage.GUIDE)).build());
    }

    /** The grey of the moon at {@code d} (0 in the middle, 1 at the rim): about 55 percent luminance in the middle to 38 at the rim, a drained moon. */
    static int value(float d) {
        return net.ofatech.supernaturalbloodlines.shared.hub.kit.Bake.greyValue(d);
    }

    /** A dim grey moon, a little paler in the middle: one baked picture per radius. */
    static void disc(GuiGraphics g, int cx, int cy, int r) {
        int side = 2 * r;
        if (side > 0) {
            net.ofatech.supernaturalbloodlines.shared.hub.kit.client.BakedTexture.blit(g, net.ofatech.supernaturalbloodlines.shared.hub.kit.client.BakedTexture.get("ghoul/" + r, side, side,
                    net.ofatech.supernaturalbloodlines.shared.hub.kit.Bake.greyMoon(r)), cx - r, cy - r, side, side);
        }
    }
}
