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
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.ToastPlace;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.DevHud;
import net.ofatech.supernaturalbloodlines.shared.hud.HudFaces;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.neoforged.fml.ModList;

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

    /** What is drawn of each line: the whole line, or its start and an ellipsis when it is wider than {@code width}. Never a silent cut. */
    public static List<String> fitted(List<Component> lines, int width, java.util.function.ToIntFunction<String> measure) {
        return lines.stream().map(c -> net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit.cut(c.getString(), width, measure)).toList();
    }

    /** The box: the kit's label in the HUD's look, under the status of Tensura and Vampirism when one of them is installed. */
    private static void draw(GuiGraphics g, Font font, List<Component> lines) {
        HudKit kit = HudKit.of(HudStyle.current());
        int h = lines.size() * 10 + 2 * PAD + 3;
        boolean corner = ModList.get().isLoaded("tensura") || ModList.get().isLoaded("vampirism");
        HudLayout.Box at = ToastPlace.place(g.guiWidth(), g.guiHeight(), W, h, corner);
        UiKit.draw(g, kit, Part.HUD_LABEL, at.x(), at.y(), at.w(), at.h());
        List<String> shown = fitted(lines, W - 2 * PAD - 4, font::width);
        for (int i = 0; i < shown.size(); i++) {
            int color = UiKit.labelOver(kit, Part.HUD_LABEL, i == 0 ? HudFaces.labelName(kit) : HudFaces.labelBody(kit));
            g.drawString(font, shown.get(i), at.x() + PAD + 2, at.y() + PAD + 2 + i * 10, color, false);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (DevHud.on && DevHud.toast && mc.player != null && mc.screen == null) {
            draw(e.getGuiGraphics(), mc.font, List.of(Component.literal("Klaus: Sire's call"), Component.literal("1:55 left"), Component.literal("Press H to answer")));
            return;
        }
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
        draw(g, font, lines);
    }
}

