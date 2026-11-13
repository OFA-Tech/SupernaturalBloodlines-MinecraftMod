package net.ofatech.supernaturalbloodlines.parts.species.original;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Give up / Wait choice of a downed player. Client only: nothing but the payload handler in SpeciesPart refers to
 * this class, and it only runs when a {@link DownedPayload} arrives, so a dedicated server never loads it.
 */
public final class DownedScreen extends Screen {
    private DownedScreen() {
        super(Component.translatable("supernaturalbloodlines.downed.title"));
    }

    public static void show(boolean downed) {
        Minecraft mc = Minecraft.getInstance();
        if (downed) {
            if (!(mc.screen instanceof DownedScreen)) {
                mc.setScreen(new DownedScreen());
            }
        } else if (mc.screen instanceof DownedScreen) {
            mc.setScreen(null);
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("supernaturalbloodlines.downed.give_up"), b -> {
            PacketDistributor.sendToServer(DownedGiveUpPayload.INSTANCE);
            onClose();
        }).bounds(width / 2 - 105, height / 2, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("supernaturalbloodlines.downed.wait"), b -> onClose())
                .bounds(width / 2 + 5, height / 2, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 30, 0xFF5555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
