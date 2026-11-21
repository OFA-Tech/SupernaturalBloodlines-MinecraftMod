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
 * {@code -Dsupernaturalbloodlines.devshots.bosses=mikael} (Mikael from the front, the side, behind and desiccated) or {@code =elijah}, {@code =rebekah} (from the front, the side, behind and desiccated), {@code =deal} (Elijah's dialog) or {@code =klaus} (the man from the front
 * and behind, half way through the shift, and the wolf from the front and the side), into {@code <game dir>/screenshots/bossshots/<boss>.png}.
 * {@code .quit=true} leaves when done, {@code .world=New World} picks the world.
 */
public final class BossShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.bosses";
    private static final int WORLD_TICKS = 100;
    private static final int SETTLE_TICKS = 80;

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
        if (boss.equals("deal")) {
            dealTick(mc);
            return;
        }
        if (boss.equals("house")) {
            houseTick(mc);
            return;
        }
        if (screen == null) {
            if (mc.options.guiScale().get() != 2) {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                return;
            }
            screen = new PanelScreen(boss);
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

    /** The burned Hale house in the world: three pictures, {@code bossshots/house-<view>.png}, of a house placed with the real template on flat ground. */
    private static final String[][] HOUSE_VIEWS = {
        {"front", "tp @p 0.5 100 14.5 180 14"},
        {"corner", "tp @p 13.5 104 13.5 135 20"},
        {"inside", "tp @p 0.5 100 3.5 180 6"},
        {"cellar", "tp @p 0.5 96.2 0.5 90 12"}};
    private int houseStep = -1;
    private int houseWait;

    private static void command(net.minecraft.server.MinecraftServer server, String text) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withPermission(4), text));
    }

    private void houseTick(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) {
            done = true;
            return;
        }
        if (mc.options.guiScale().get() != 2) {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            return;
        }
        if (houseStep == -1) {
            // no HUD, no hand, no aura: a spectator sees the house and nothing of the mod's overlay; the template stands as the generator puts it, its floor layer in the ground's top block
            mc.options.hideGui = true;
            for (String c : new String[] {"gamemode spectator @p", "effect clear @p", "effect give @p minecraft:night_vision infinite 0 true", "time set 6000", "weather clear", "kill @e[type=!minecraft:player]", "fill -16 90 -16 16 97 16 minecraft:stone",
                    "fill -16 98 -16 16 98 16 minecraft:grass_block", "fill -16 99 -16 16 125 16 minecraft:air", "place template supernaturalbloodlines:hale_house -5 95 -4", HOUSE_VIEWS[0][1]}) {
                command(server, c);
            }
            houseStep = 0;
            houseWait = 0;
            return;
        }
        if (++houseWait < 160) {
            return;
        }
        String view = HOUSE_VIEWS[houseStep][0];
        new File(mc.gameDirectory, "screenshots/bossshots").mkdirs();
        Screenshot.grab(mc.gameDirectory, "bossshots/house-" + view + ".png", mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("BOSSSHOTS bossshots/house-{}.png", view);
        houseWait = 0;
        if (++houseStep >= HOUSE_VIEWS.length) {
            done = true;
            if (quit) {
                mc.stop();
            }
            return;
        }
        command(server, HOUSE_VIEWS[houseStep][1]);
    }

    private static final String[] DEAL_THEMES = {"grimoire", "blood_moon", "tensura"};
    private static final int[] DEAL_SCALES = {2, 4};
    private static final int DEAL_SETTLE_TICKS = 20;
    private int dealStep;

    /** The offer dialog in each hub theme at each GUI scale: one picture each, {@code bossshots/deal-<theme>-s<scale>.png}. */
    private void dealTick(Minecraft mc) {
        int scale = DEAL_SCALES[dealStep / DEAL_THEMES.length];
        String theme = DEAL_THEMES[dealStep % DEAL_THEMES.length];
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
            screen = null;
            return;
        }
        if (screen == null) {
            screen = ElijahDealScreen.demo(List.of("mikael", "finn", "kol", "rebekah", "klaus"), 7, theme);
            mc.setScreen(screen);
            wait = 0;
            return;
        }
        if (++wait < DEAL_SETTLE_TICKS) {
            return;
        }
        new File(mc.gameDirectory, "screenshots/bossshots").mkdirs();
        String name = "deal-" + theme + "-s" + scale;
        Screenshot.grab(mc.gameDirectory, "bossshots/" + name + ".png", mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("BOSSSHOTS bossshots/{}.png", name);
        screen = null;
        if (++dealStep >= DEAL_THEMES.length * DEAL_SCALES.length) {
            done = true;
            if (quit) {
                mc.stop();
            }
        }
    }

    /**
     * One panel of the sheet: a named entity at a yaw, with the size of what is drawn (the model, which is not the hitbox: the wolf's ears reach 2.1
     * blocks, its hitbox is 1.5) so every panel is fitted to its own cell and cannot spill into the next.
     */
    private record Panel(String name, LivingEntity entity, float yaw, float modelHeight, float modelWidth) {
    }

    private static final class PanelScreen extends Screen {
        private final String boss;
        private final float tilt = (float) Math.toRadians(Float.parseFloat(System.getProperty(PROPERTY + ".tilt", "0")));
        private List<Panel> panels;
        private int columns;
        private int rows;

        PanelScreen(String boss) {
            super(Component.empty());
            this.boss = boss;
        }

        private List<Panel> build(Minecraft mc) {
            List<Panel> list = new ArrayList<>();
            if (boss.equals("deucalion")) {
                columns = 4;
                rows = 2;
                int swap = ShiftRules.SWAP_TICK;
                list.add(new Panel("the man, glasses on", deucalion(mc, BossForm.HUMAN, 0), 180f, 1.95f, 1.3f));
                list.add(new Panel("the man, from the side", deucalion(mc, BossForm.HUMAN, 0), 90f, 1.95f, 1.3f));
                list.add(new Panel("shift, tick 5: no glasses", deucalion(mc, BossForm.SHIFTING, 5), 160f, 1.95f, 1.3f));
                list.add(new Panel("wolf, just after the swap", deucalion(mc, BossForm.SHIFTING, swap), 160f, 3.3f, 2.6f));
                list.add(new Panel("the wolf, from the front", deucalion(mc, BossForm.BEAST, ShiftRules.END_TICK), 180f, 3.3f, 2.6f));
                list.add(new Panel("the wolf, from the side", deucalion(mc, BossForm.BEAST, ShiftRules.END_TICK), 90f, 3.3f, 5.2f));
                list.add(new Panel("the wolf, from behind", deucalion(mc, BossForm.BEAST, ShiftRules.END_TICK), 0f, 3.3f, 2.6f));
                list.add(new Panel("the roar", deucalion(mc, BossForm.BEAST, swap + 20), 150f, 3.3f, 3.2f));
                return list;
            }
            if (boss.equals("peter")) {
                columns = 4;
                rows = 2;
                int swap = ShiftRules.SWAP_TICK;
                list.add(new Panel("burned, from the front", peter(mc, BossForm.HUMAN, 0, false), 180f, 1.95f, 1.3f));
                list.add(new Panel("whole, from the front", peter(mc, BossForm.HUMAN, 0, true), 180f, 1.95f, 1.3f));
                list.add(new Panel("man, just before the swap", peter(mc, BossForm.SHIFTING, swap - 1, true), 160f, 1.95f, 1.3f));
                list.add(new Panel("wolf, just after the swap", peter(mc, BossForm.SHIFTING, swap, true), 160f, 2.0f, 1.9f));
                list.add(new Panel("the wolf, from the front", peter(mc, BossForm.BEAST, ShiftRules.END_TICK, true), 180f, 2.0f, 1.9f));
                list.add(new Panel("the wolf, from the side", peter(mc, BossForm.BEAST, ShiftRules.END_TICK, true), 90f, 2.0f, 4.8f));
                list.add(new Panel("the wolf, from behind", peter(mc, BossForm.BEAST, ShiftRules.END_TICK, true), 0f, 2.0f, 1.9f));
                list.add(new Panel("the wolf, three quarters", peter(mc, BossForm.BEAST, ShiftRules.END_TICK, true), 135f, 2.0f, 3.6f));
                return list;
            }
            if (boss.equals("klaus")) {
                columns = 4;
                rows = 2;
                int swap = ShiftRules.SWAP_TICK;
                list.add(new Panel("the man, from the front", klaus(mc, BossForm.HUMAN, 0, false), 180f, 1.95f, 1.3f));
                list.add(new Panel("the man, from behind", klaus(mc, BossForm.HUMAN, 0, false), 0f, 1.95f, 1.3f));
                list.add(new Panel("man, just before the swap", klaus(mc, BossForm.SHIFTING, swap - 1, false), 160f, 1.95f, 1.3f));
                list.add(new Panel("wolf, just after the swap", klaus(mc, BossForm.SHIFTING, swap, false), 160f, 2.1f, 1.8f));
                list.add(new Panel("the wolf, from the front", klaus(mc, BossForm.BEAST, ShiftRules.END_TICK, false), 180f, 2.1f, 1.8f));
                list.add(new Panel("the wolf, from the side", klaus(mc, BossForm.BEAST, ShiftRules.END_TICK, false), 90f, 2.1f, 4.4f));
                list.add(new Panel("the wolf, from behind", klaus(mc, BossForm.BEAST, ShiftRules.END_TICK, false), 0f, 2.1f, 1.8f));
                list.add(new Panel("the wolf, desiccated", klaus(mc, BossForm.BEAST, ShiftRules.END_TICK, true), 140f, 1.3f, 4.0f));
                list.add(new Panel("the man, desiccated", klaus(mc, BossForm.HUMAN, 0, true), 135f, 0.6f, 2.2f));
                columns = 5;
            } else {
                columns = 4;
                rows = 1;
                String[] names = {"from the front", "from the side", "from behind", "desiccated"};
                float[] yaws = {180f, 90f, 0f, 135f};
                for (int i = 0; i < 4; i++) {
                    NpcOriginalBoss m = switch (boss) {
                        case "elijah" -> net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity.create(mc.level);
                        case "rebekah" -> net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity.create(mc.level);
                        case "kol" -> net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity.create(mc.level);
                        case "finn" -> net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity.create(mc.level);
                        default -> {
                            MikaelEntity mikael = MikaelEntity.create(mc.level);
                            mikael.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BossItems.MIKAELS_STAKE.get()));
                            yield mikael;
                        }
                    };
                    m.lookDesiccated(i == 3);
                    list.add(new Panel(names[i], m, yaws[i], i == 3 ? 0.6f : 1.95f, i == 3 ? 2.2f : 1.3f));
                }
            }
            return list;
        }

        private static net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity deucalion(Minecraft mc, BossForm form, int ticks) {
            var d = net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity.create(mc.level);
            d.lookForm(form, ticks);
            return d;
        }

        private static net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity peter(Minecraft mc, BossForm form, int ticks, boolean metMoon) {
            var p = net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity.create(mc.level);
            p.lookForm(form, ticks, metMoon);
            return p;
        }

        private static KlausEntity klaus(Minecraft mc, BossForm form, int ticks, boolean desiccated) {
            KlausEntity k = KlausEntity.create(mc.level);
            k.lookForm(form, ticks);
            k.lookDesiccated(desiccated);
            return k;
        }

        /** The animations run on the entity's own tick count, which nothing advances for an entity that is not in the level: advance it, so a held pose is reached. */
        @Override
        public void tick() {
            if (panels != null) {
                panels.forEach(p -> p.entity().tickCount++);
            }
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            Minecraft mc = Minecraft.getInstance();
            if (panels == null) {
                panels = build(mc);
            }
            // .hitbox=true draws the boxes as F3+B does; .tilt=<degrees> looks down on the body from above (a lying body is seen best that way)
            mc.getEntityRenderDispatcher().setRenderHitBoxes(Boolean.getBoolean(PROPERTY + ".hitbox"));
            int cw = width / columns;
            int ch = height / rows;
            for (int i = 0; i < panels.size(); i++) {
                Panel p = panels.get(i);
                int x0 = (i % columns) * cw;
                int y0 = (i / columns) * ch;
                LivingEntity m = p.entity();
                m.setYRot(p.yaw());
                m.yRotO = p.yaw();
                m.yBodyRot = p.yaw();
                m.yBodyRotO = p.yaw();
                m.yHeadRot = p.yaw();
                m.yHeadRotO = p.yaw();
                float scale = Math.min(0.78f * (ch - 16) / p.modelHeight(), 0.92f * cw / p.modelWidth());
                // the box centre is what is placed at the point, so lift or drop it to put the middle of what is drawn in the middle of the cell
                int cy = y0 + 16 + (ch - 16) / 2 + Math.round((p.modelHeight() - m.getBbHeight()) / 2f * scale);
                g.enableScissor(x0, y0, x0 + cw, y0 + ch);
                g.drawString(font, p.name(), x0 + 4, y0 + 4, 0xFFFFFF, false);
                InventoryScreen.renderEntityInInventory(g, x0 + cw / 2, cy, scale, new Vector3f(0f, m.getBbHeight() / 2f, 0f), new Quaternionf().rotateZ((float) Math.PI).rotateX(tilt), null, m);
                g.disableScissor();
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
