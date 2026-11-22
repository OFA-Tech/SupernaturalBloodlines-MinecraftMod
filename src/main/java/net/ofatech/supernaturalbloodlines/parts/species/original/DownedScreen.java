package net.ofatech.supernaturalbloodlines.parts.species.original;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Wait / Give up choice of a downed player. Client only: nothing but the payload handler in SpeciesPart refers to this class, and it only runs when a
 * {@link DownedPayload} arrives, so a dedicated server never loads it. Wait comes first and has the focus; Give up asks twice (the first click arms it for
 * 3 s). In the turning the screen says what Wait and Give up do, with the seconds that are left.
 */
public final class DownedScreen extends Screen {
    private static final int ARM_TICKS = 60;
    private static final int PAD = 6;
    private static final int GAP = 8;

    private final int mode;
    private int ticksLeft;
    private int armed;
    private Button wait;
    private Button giveUp;
    private MultiLineTextWidget body;
    private int shownSeconds = -1;
    private int top;
    private int bottom;

    private DownedScreen(int mode, int ticksLeft) {
        super(Component.translatable(mode == Downed.MODE_TURNING ? "supernaturalbloodlines.downed.turning.title" : "supernaturalbloodlines.downed.title"));
        this.mode = mode;
        this.ticksLeft = ticksLeft;
    }

    public static void show(boolean downed, int mode, int ticksLeft) {
        Minecraft mc = Minecraft.getInstance();
        if (downed) {
            if (mc.screen instanceof DownedScreen open) {
                open.ticksLeft = ticksLeft; // the server sends it again now and then: the countdown is put right
            } else {
                mc.setScreen(new DownedScreen(mode, ticksLeft));
            }
        } else if (mc.screen instanceof DownedScreen) {
            mc.setScreen(null);
        }
    }

    private int seconds() {
        return (ticksLeft + 19) / 20;
    }

    @Override
    protected void init() {
        armed = 0;
        int textWidth = Math.min(width - 40, 300);
        boolean hasBody = mode == Downed.MODE_TURNING;
        body = new MultiLineTextWidget(hasBody ? bodyText() : Component.empty(), font).setMaxWidth(textWidth).setColor(0xDDDDDD).setCentered(true);
        shownSeconds = seconds();
        // the room is the height of the longest the text gets (a full countdown, two digits), so the countdown never reflows the text into the buttons
        int bodyHeight = hasBody ? Math.max(body.getHeight(), new MultiLineTextWidget(bodyText(Downed.TICKS / 20), font).setMaxWidth(textWidth).getHeight()) : 0;
        boolean sideBySide = width >= 280;
        int buttonsHeight = sideBySide ? 20 : 44;
        int total = font.lineHeight + GAP + bodyHeight + GAP + buttonsHeight;
        top = (height - total) / 2;
        bottom = top + total;
        int y = top + font.lineHeight + GAP;
        if (hasBody) {
            body.setPosition((width - textWidth) / 2, y);
            addRenderableWidget(body);
        }
        y += bodyHeight + GAP;
        int bw = sideBySide ? 130 : 200;
        wait = Button.builder(Component.translatable("supernaturalbloodlines.downed.wait"), b -> onClose())
                .bounds(sideBySide ? width / 2 - bw - 5 : width / 2 - bw / 2, y, bw, 20).build();
        giveUp = Button.builder(Component.translatable("supernaturalbloodlines.downed.give_up"), b -> pressedGiveUp())
                .bounds(sideBySide ? width / 2 + 5 : width / 2 - bw / 2, sideBySide ? y : y + 24, bw, 20).build();
        addRenderableWidget(wait);
        addRenderableWidget(giveUp);
        setInitialFocus(wait);
    }

    private Component bodyText() {
        return bodyText(seconds());
    }

    private static Component bodyText(int seconds) {
        return Component.translatable("supernaturalbloodlines.downed.turning.body", seconds);
    }

    private void pressedGiveUp() {
        if (armed > 0) {
            PacketDistributor.sendToServer(DownedGiveUpPayload.INSTANCE);
            onClose();
        } else {
            armed = ARM_TICKS;
            giveUp.setMessage(Component.translatable("supernaturalbloodlines.downed.give_up_sure"));
        }
    }

    @Override
    public void tick() {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
        if (mode == Downed.MODE_TURNING && seconds() != shownSeconds) {
            shownSeconds = seconds();
            body.setMessage(bodyText());
        }
        if (armed > 0 && --armed == 0) {
            giveUp.setMessage(Component.translatable("supernaturalbloodlines.downed.give_up"));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // a dark backing, so the text reads over any world
        int half = Math.min(width - 8, 320) / 2;
        graphics.fill(width / 2 - half, top - PAD, width / 2 + half, bottom + PAD, 0xC0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, top, 0xFF5555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
