package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A development-only screenshot driver for the bosses, in the way of {@code ItemShots}: inert unless the JVM has
 * {@code -Dsupernaturalbloodlines.devshots.bosses=mikael}. It opens the world and photographs Mikael from the front, the side, behind and desiccated, into
 * {@code <game dir>/screenshots/bossshots/mikael.png}. {@code .quit=true} leaves when done, {@code .world=New World} picks the world.
 */
public final class BossShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.bosses";
    private static final int WORLD_TICKS = 100;
    private static final int SETTLE_TICKS = 10;

    private final boolean quit = Boolean.getBoolean(PROPERTY + ".quit");
    private final String world = System.getProperty(PROPERTY + ".world", "New World");
    private Screen screen;
    private int worldTicks;
    private int wait;
    private boolean opened;
    private boolean done;

    private BossShots() {
    }

    public static void registerIfRequested() {
        String spec = System.getProperty(PROPERTY);
        if (spec == null || spec.isBlank()) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(new BossShots()::tick);
        SupernaturalBloodlines.LOGGER.warn("BOSSSHOTS on: {} (development only)", spec);
    }

    private void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (done) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            if (mc.screen instanceof TitleScreen && !opened && mc.getWindow() != null) {
                opened = true;
                mc.createWorldOpenFlows().openWorld(world, () -> mc.setScreen(new TitleScreen()));
            }
            return;
        }
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.getTutorial().setStep(TutorialSteps.NONE);
        if (worldTicks++ < WORLD_TICKS) {
            return;
        }
        if (screen == null) {
            if (mc.options.guiScale().get() != 2) {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                return;
            }
            screen = new MikaelScreen();
            mc.setScreen(screen);
        }
        if (++wait < SETTLE_TICKS) {
            return;
        }
        new File(mc.gameDirectory, "screenshots/bossshots").mkdirs();
        Screenshot.grab(mc.gameDirectory, "bossshots/mikael.png", mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("BOSSSHOTS bossshots/mikael.png");
        done = true;
        if (quit) {
            mc.stop();
        }
    }

    private static final class MikaelScreen extends Screen {
        private MikaelEntity[] mikaels;

        MikaelScreen() {
            super(Component.empty());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            Minecraft mc = Minecraft.getInstance();
            if (mikaels == null) {
                mikaels = new MikaelEntity[4];
                for (int i = 0; i < mikaels.length; i++) {
                    mikaels[i] = MikaelEntity.create(mc.level);
                    mikaels[i].setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BossItems.MIKAELS_STAKE.get()));
                    mikaels[i].lookDesiccated(i == 3);
                }
            }
            String[] names = {"from the front", "from the side", "from behind", "desiccated"};
            float[] yaws = {180f, 90f, 0f, 0f};
            int w = width / 4;
            for (int i = 0; i < 4; i++) {
                MikaelEntity m = mikaels[i];
                m.setYRot(yaws[i]);
                m.yRotO = yaws[i];
                m.yBodyRot = yaws[i];
                m.yBodyRotO = yaws[i];
                m.yHeadRot = yaws[i];
                m.yHeadRotO = yaws[i];
                g.drawString(font, names[i], i * w + 4, 4, 0xFFFFFF, false);
                InventoryScreen.renderEntityInInventory(g, i * w + (i == 3 ? w - 30 : w / 2), height / 2 + 20, i == 3 ? height / 5.5f : height / 2.6f, new Vector3f(0f, m.getBbHeight() / 2f, 0f),
                        new Quaternionf().rotateZ((float) Math.PI), null, m);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
