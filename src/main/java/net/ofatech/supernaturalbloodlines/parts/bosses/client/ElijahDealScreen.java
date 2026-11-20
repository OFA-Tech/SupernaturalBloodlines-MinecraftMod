package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealChoicePayload;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload;

/**
 * Elijah''s offer (R167): a small dialog, not a hub page. One button for each Original whose blood he may give, and "No deal". It sends the position of the
 * button, nothing else; the server decides what the position means.
 */
public final class ElijahDealScreen extends Screen {
    private static final int BUTTON_W = 160;
    private static final int BUTTON_H = 20;
    private final DealOfferPayload offer;

    private ElijahDealScreen(DealOfferPayload offer) {
        super(Component.translatable("supernaturalbloodlines.boss.elijah.title"));
        this.offer = offer;
    }

    public static void open(DealOfferPayload offer) {
        Minecraft.getInstance().setScreen(new ElijahDealScreen(offer));
    }

    /** A screen for the screenshots: no network, the same widgets. */
    public static Screen demo(List<String> originals, int days) {
        return new ElijahDealScreen(new DealOfferPayload(0, days, originals));
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_W) / 2;
        int y = height / 2 - 20;
        for (int i = 0; i < offer.originals().size(); i++) {
            int answer = i;
            addRenderableWidget(Button.builder(Component.translatable("supernaturalbloodlines.original." + offer.originals().get(i)), b -> answer(answer))
                    .bounds(x, y + i * (BUTTON_H + 4), BUTTON_W, BUTTON_H).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("supernaturalbloodlines.boss.elijah.no_deal"), b -> answer(-1))
                .bounds(x, y + offer.originals().size() * (BUTTON_H + 4) + 8, BUTTON_W, BUTTON_H).build());
    }

    private void answer(int index) {
        if (offer.entityId() != 0) {
            PacketDistributor.sendToServer(new DealChoicePayload(offer.entityId(), index));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        int cx = width / 2;
        int top = height / 2 - 20 - 38;
        g.drawCenteredString(font, title, cx, top, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("supernaturalbloodlines.boss.elijah.offer", offer.days()), cx, top + 14, 0xC8C8D0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}