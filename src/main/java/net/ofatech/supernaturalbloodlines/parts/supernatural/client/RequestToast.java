package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/**
 * A small box in the top left corner of the game screen for the newest open request: who, what and the seconds left, and the key that opens
 * Home. Drawn by the supernatural part itself, so it shows with the HUD off; it never takes focus and is gone while any screen is open
 * (the hub shows the request on Home). 8 px text, on a dark ground with a light text.
 */
@EventBusSubscriber(modid = SupernaturalBloodlines.MODID, value = Dist.CLIENT)
public final class RequestToast {
    public static final int PAPER = 0xE0181018;
    public static final int INK = 0xFFF2E8D8;
    public static final int MUTED = 0xFFC9BBA5;
    static final int X = 4;
    static final int Y = 4;
    static final int W = 150;
    static final int PAD = 4;

    private RequestToast() {}

    /** The words of the box, pure so a test can check them. */
    public static List<Component> lines(HubRequests.Request r, int seconds, Component key) {
        Component what = Component.translatable("supernaturalbloodlines.hub.request.kind." + r.kind().name().toLowerCase(java.util.Locale.ROOT));
        Component who = r.fromName().isEmpty() ? what : Component.translatable("supernaturalbloodlines.hub.toast.from", r.fromName(), what);
        return List.of(who, Component.translatable("supernaturalbloodlines.hub.toast.left", seconds), Component.translatable("supernaturalbloodlines.hub.toast.key", key));
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        List<HubRequests.Request> open = RequestsClient.requests();
        if (mc.screen != null || mc.player == null || open.isEmpty() || mc.options.hideGui) {
            return;
        }
        HubRequests.Request r = open.get(0);
        int left = RequestsClient.ticksLeft(r);
        if (left <= 0) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
        Font font = mc.font;
        List<Component> lines = lines(r, (left + 19) / 20, HubKeybinds.ANSWER.isUnbound() ? HubKeybinds.HUB.getTranslatedKeyMessage() : HubKeybinds.ANSWER.getTranslatedKeyMessage());
        int h = lines.size() * 10 + 2 * PAD - 1;
        g.fill(X, Y, X + W, Y + h, PAPER);
        for (int i = 0; i < lines.size(); i++) {
            for (var line : font.split(lines.get(i), W - 2 * PAD)) {
                g.drawString(font, line, X + PAD, Y + PAD + i * 10, i == 0 ? INK : MUTED, false);
                break;
            }
        }
    }
}

