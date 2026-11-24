package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import java.io.File;
import java.util.Set;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossForm;
import net.ofatech.supernaturalbloodlines.core.boss.ShiftRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BeastEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.BossNpc;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.PeterHaleEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A development-only driver for the shift (UI overhaul phase 7), inert unless the JVM has {@code -Dsupernaturalbloodlines.devshots.bosses=morph:klaus} (or
 * {@code deucalion}, {@code peter}, {@code beast}). It runs one boss through the whole shift in real time, one client tick at a time (a man for 10 ticks, then
 * the 40 ticks of the shift, then the howl), the way the game plays it: the pose and the body are what the renderer makes of the form and ticks, and the wolf's
 * GeckoLib clip starts at the swap by itself. It writes, into {@code <game dir>/screenshots/bossshots/}:
 * <ul>
 *   <li>{@code morph-<boss>-t<tick>.png}: a still at ticks 0, 6, 12, 19, 20, 26, 32, 39 and 44 of the shift;</li>
 *   <li>{@code morph-<boss>/%04d.png}: every rendered frame from 10 ticks before the shift to 60 ticks after its start, at 30 frames a second, for the clip.</li>
 * </ul>
 * {@code .quit=true} leaves when done, {@code .world=New World} picks the world. The boss bar is not drawn (the picture is a screen): the bar's chips are tested.
 */
final class MorphShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.bosses";
    private static final int WORLD_TICKS = 100;
    private static final int LEAD_IN = -10;
    private static final int LAST = 60;
    private static final Set<Integer> STILLS = Set.of(0, 6, 12, 19, 20, 26, 32, 39, 44);

    private final String boss;
    private final boolean quit = Boolean.getBoolean(PROPERTY + ".quit");
    private final String world = System.getProperty(PROPERTY + ".world", "New World");
    private MorphScreen screen;
    private int worldTicks;
    private boolean opened;
    private boolean finished;
    private int frames;
    private int settle;

    private MorphShots(String boss) {
        this.boss = boss;
    }

    static void register(String boss) {
        MorphShots shots = new MorphShots(boss);
        NeoForge.EVENT_BUS.addListener(shots::tick);
        NeoForge.EVENT_BUS.addListener(shots::frame);
        SupernaturalBloodlines.LOGGER.warn("MORPHSHOTS on: {} (development only)", boss);
    }

    private void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
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
            if (mc.options.guiScale().get() != 2 || mc.options.framerateLimit().get() != 30) {
                mc.options.guiScale().set(2);
                mc.options.framerateLimit().set(30); // the clip is encoded at 30 frames a second: render at that
                mc.resizeDisplay();
                return;
            }
            new File(mc.gameDirectory, "screenshots/bossshots/morph-" + boss).mkdirs();
            screen = new MorphScreen(boss);
            mc.setScreen(screen);
            return;
        }
        if (screen.done() && ++settle > 20) {
            SupernaturalBloodlines.LOGGER.warn("MORPHSHOTS {} done: {} frames", boss, frames);
            finished = true;
            if (quit) {
                mc.stop();
            }
        }
    }

    /** After every rendered frame: the still for this tick (once), and the frame for the clip. */
    private void frame(RenderFrameEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (screen == null || mc.screen != screen || screen.done()) {
            return;
        }
        int t = screen.t();
        if (t < LEAD_IN || t > LAST) {
            return;
        }
        Screenshot.grab(mc.gameDirectory, String.format("bossshots/morph-%s/%04d.png", boss, frames++), mc.getMainRenderTarget(), c -> { });
        if (STILLS.contains(t) && screen.firstFrameOf(t)) {
            Screenshot.grab(mc.gameDirectory, "bossshots/morph-" + boss + "-t" + t + ".png", mc.getMainRenderTarget(), c -> { });
        }
    }

    private static final class MorphScreen extends Screen {
        private final String boss;
        private LivingEntity entity;
        private int t = LEAD_IN - 1;
        private int lastStill = Integer.MIN_VALUE;

        MorphScreen(String boss) {
            super(Component.empty());
            this.boss = boss;
        }

        int t() {
            return t;
        }

        boolean done() {
            return t > LAST;
        }

        /** Whether this is the first frame drawn for this tick (so a still is taken once per tick, not once per frame). */
        boolean firstFrameOf(int tick) {
            if (lastStill == tick) {
                return false;
            }
            lastStill = tick;
            return true;
        }

        private LivingEntity make(Minecraft mc) {
            return switch (boss) {
                case "klaus" -> KlausEntity.create(mc.level);
                case "deucalion" -> DeucalionEntity.create(mc.level);
                case "peter" -> PeterHaleEntity.create(mc.level);
                default -> BeastEntity.create(mc.level);
            };
        }

        /** Puts the entity in the state the server would have it in at this tick of the story: the man, then the shift, then the beast with its howl. */
        private void pose(int tick) {
            BossForm form = tick < 0 ? BossForm.HUMAN : tick < ShiftRules.SHIFT_TICKS ? BossForm.SHIFTING : BossForm.BEAST;
            int ticks = tick < 0 ? 0 : Math.min(tick, ShiftRules.END_TICK);
            switch (entity) {
                case KlausEntity k -> k.lookForm(form, ticks);
                case DeucalionEntity d -> d.lookForm(form, ticks);
                case PeterHaleEntity p -> p.lookForm(form, ticks, true);
                case BeastEntity b -> {
                    b.setForm(form);
                    b.setShiftTicks(form == BossForm.BEAST ? ShiftRules.END_TICK : ticks);
                    // the Host's hat is on until the swap, as the director gives it
                    b.setItemSlot(EquipmentSlot.HEAD, tick < ShiftRules.SWAP_TICK ? new ItemStack(BossItems.SEBASTIEN_TRICORN.get()) : ItemStack.EMPTY);
                }
                default -> { }
            }
        }

        /** The entity is not in the level, so nothing advances its animation clock: advance it, and the story with it. */
        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (entity == null) {
                entity = make(mc);
            }
            t++;
            if (t <= LAST) {
                pose(Math.max(t, LEAD_IN));
            }
            entity.tickCount++;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            if (entity == null) {
                return;
            }
            float yaw = 160f;
            entity.setYRot(yaw);
            entity.yRotO = yaw;
            entity.yBodyRot = yaw;
            entity.yBodyRotO = yaw;
            entity.yHeadRot = yaw;
            entity.yHeadRotO = yaw;
            // the tallest body of the shift in blocks (Deucalion's wolf reaches 3.3 with its ears), so no head leaves the picture
            float modelHeight = switch (boss) {
                case "beast" -> 3.4f;
                case "deucalion" -> 3.5f;
                default -> 2.3f;
            };
            float scale = 0.72f * height / modelHeight;
            // the box centre is what is placed at the point: shift it so that the feet stay on one line whatever the height of the body
            int cy = height / 2 + 6 + Math.round((modelHeight - entity.getBbHeight()) / 2f * scale);
            InventoryScreen.renderEntityInInventory(g, width / 2, cy, scale,
                    new Vector3f(0f, entity.getBbHeight() / 2f, 0f), new Quaternionf().rotateZ((float) Math.PI), null, entity);
            String label = "tick " + t + "  " + (entity instanceof BossNpc ? entity.getClass().getSimpleName().replace("Entity", "") : "");
            g.drawString(font, label, 6, 6, 0xFFFFFF, false);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
