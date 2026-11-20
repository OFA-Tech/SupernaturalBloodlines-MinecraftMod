package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A development-only screenshot driver for the bosses, in the way of {@code ItemShots}: inert unless the JVM has
 * {@code -Dsupernaturalbloodlines.devshots.bosses=mikael} (Mikael from the front, the side, behind and desiccated) or {@code =klaus} (the man from the front
 * and behind, half way through the shift, and the wolf from the front and the side), into {@code <game dir>/screenshots/bossshots/<boss>.png}.
 * {@code .quit=true} leaves when done, {@code .world=New World} picks the world.
 */
public final class BossShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.bosses";
    private static final int WORLD_TICKS = 100;
    private static final int SETTLE_TICKS = 10;

    private final String boss;
    private final boolean quit = Boolean.getBoolean(PROPERTY + ".quit");
    private final String world = System.getProperty(PROPERTY + ".world", "New World");
    private Screen screen;
    private int worldTicks;
    private int wait;
    private boolean opened;
    private boolean done;

    private BossShots(String boss) {
        this.boss = boss;
    }

    public static void registerIfRequested() {
        String spec = System.getProperty(PROPERTY);
        if (spec == null || spec.isBlank()) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(new BossShots(spec.trim().toLowerCase(java.util.Locale.ROOT))::tick);
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
            screen = new PanelScreen(boss.equals("klaus"));
            mc.setScreen(screen);
        }
        if (++wait < SETTLE_TICKS) {
            return;
        }
        new File(mc.gameDirectory, "screenshots/bossshots").mkdirs();
        Screenshot.grab(mc.gameDirectory, "bossshots/" + boss + ".png", mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("BOSSSHOTS bossshots/{}.png", boss);
        done = true;
        if (quit) {
            mc.stop();
        }
    }

    /** One boss in a row of panels: each is a named entity at a yaw and a scale (a share of the screen height). */
    private record Panel(String name, LivingEntity entity, float yaw, float scale, float lift) {
    }

    private static final class PanelScreen extends Screen {
        private final boolean klaus;
        private List<Panel> panels;

        PanelScreen(boolean klaus) {
            super(Component.empty());
            this.klaus = klaus;
        }

        private List<Panel> build(Minecraft mc) {
            List<Panel> list = new ArrayList<>();
            if (klaus) {
                list.add(new Panel("the man, from the front", klaus(mc, BossForm.HUMAN, 0), 180f, 3.0f, 0f));
                list.add(new Panel("the man, from behind", klaus(mc, BossForm.HUMAN, 0), 0f, 3.0f, 0f));
                list.add(new Panel("shifting, half way", klaus(mc, BossForm.SHIFTING, ShiftRules.SHIFT_TICKS / 2 + 5), 160f, 4.6f, 0f));
                list.add(new Panel("the wolf, from the front", klaus(mc, BossForm.BEAST, ShiftRules.HOWL_TICKS), 150f, 4.6f, 0f));
                list.add(new Panel("the wolf, from the side", klaus(mc, BossForm.BEAST, ShiftRules.HOWL_TICKS), 90f, 7.0f, 0f));
            } else {
                String[] names = {"from the front", "from the side", "from behind", "desiccated"};
                float[] yaws = {180f, 90f, 0f, 0f};
                for (int i = 0; i < 4; i++) {
                    MikaelEntity m = MikaelEntity.create(mc.level);
                    m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BossItems.MIKAELS_STAKE.get()));
                    m.lookDesiccated(i == 3);
                    list.add(new Panel(names[i], m, yaws[i], i == 3 ? 5.5f : 2.6f, 0f));
                }
            }
            return list;
        }

        private static KlausEntity klaus(Minecraft mc, BossForm form, int ticks) {
            KlausEntity k = KlausEntity.create(mc.level);
            k.lookForm(form, ticks);
            return k;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            Minecraft mc = Minecraft.getInstance();
            if (panels == null) {
                panels = build(mc);
            }
            int w = width / panels.size();
            for (int i = 0; i < panels.size(); i++) {
                Panel p = panels.get(i);
                LivingEntity m = p.entity();
                m.setYRot(p.yaw());
                m.yRotO = p.yaw();
                m.yBodyRot = p.yaw();
                m.yBodyRotO = p.yaw();
                m.yHeadRot = p.yaw();
                m.yHeadRotO = p.yaw();
                g.drawString(font, p.name(), i * w + 4, 4, 0xFFFFFF, false);
                boolean flat = m instanceof NpcOriginalBoss b && b.isDesiccated();
                InventoryScreen.renderEntityInInventory(g, i * w + (flat ? w - 30 : w / 2), height / 2 + 30, height / p.scale(), new Vector3f(0f, m.getBbHeight() / 2f, 0f),
                        new Quaternionf().rotateZ((float) Math.PI), null, m);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}