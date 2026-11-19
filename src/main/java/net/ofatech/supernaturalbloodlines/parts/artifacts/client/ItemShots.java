package net.ofatech.supernaturalbloodlines.parts.artifacts.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactItems;
import net.ofatech.supernaturalbloodlines.parts.artifacts.RingPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A development-only screenshot driver for item art, in the way of the hub's {@code DevShots}: inert unless the JVM has
 * {@code -Dsupernaturalbloodlines.devshots.items=<kinds>} with {@code sheet} (every item of ours in the GUI, on the ground and in a hand),
 * {@code compass} (the Vampire Compass needle toward eight directions) and {@code worn} (the three rings on classic and slim arms, standing and
 * sneaking). {@code .tag=before|after} names the files, {@code .world=New World} picks the world, {@code .quit=true} leaves when done.
 * Pictures: {@code <game dir>/screenshots/itemshots/<tag>-<kind>[-n].png}. Run it with {@code runSafeShots} (hidden window).
 */
public final class ItemShots {
    private static final String PROPERTY = "supernaturalbloodlines.devshots.items";
    private static final int WORLD_TICKS = 100;
    private static final int SETTLE_TICKS = 8;
    private static final int CELL_W = 128;
    private static final int CELL_H = 52;

    private record Cell(String label, ItemStack stack) {
    }

    private final List<String> kinds;
    private final String tag = System.getProperty(PROPERTY + ".tag", "after");
    private final String world = System.getProperty(PROPERTY + ".world", "New World");
    private final boolean quit = Boolean.getBoolean(PROPERTY + ".quit");
    private final List<Screen> screens = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private int index;
    private int wait;
    private int worldTicks;
    private boolean opened;
    private boolean done;

    private ItemShots(List<String> kinds) {
        this.kinds = kinds;
    }

    public static void registerIfRequested() {
        String spec = System.getProperty(PROPERTY);
        if (spec == null || spec.isBlank()) {
            return;
        }
        ItemShots shots = new ItemShots(Arrays.stream(spec.split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT)).toList());
        NeoForge.EVENT_BUS.addListener(shots::tick);
        SupernaturalBloodlines.LOGGER.warn("ITEMSHOTS on: {} (development only)", spec);
    }

    private static List<Cell> allItems() {
        List<Cell> cells = new ArrayList<>();
        BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals(SupernaturalBloodlines.MODID)).sorted().forEach(k -> {
            Item item = BuiltInRegistries.ITEM.get(k);
            cells.add(new Cell(k.getPath(), new ItemStack(item)));
        });
        return cells;
    }

    private static List<Cell> compassCells(Minecraft mc) {
        List<Cell> cells = new ArrayList<>();
        // the needle is drawn from the player's yaw to the target, so eight targets around the player give eight frames
        int[][] dirs = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        for (int[] d : dirs) {
            ItemStack stack = new ItemStack(ArtifactItems.VAMPIRE_COMPASS.get());
            BlockPos at = mc.player.blockPosition().offset(d[0] * 200, 0, d[1] * 200);
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(mc.level.dimension(), at)), false));
            cells.add(new Cell("target " + d[0] + "," + d[1], stack));
        }
        return cells;
    }

    private void build(Minecraft mc) {
        for (String kind : kinds) {
            switch (kind) {
                case "sheet" -> {
                    List<Cell> all = allItems();
                    int perPage = (mc.getWindow().getGuiScaledWidth() / CELL_W) * ((mc.getWindow().getGuiScaledHeight() - 4) / CELL_H);
                    for (int p = 0; p * perPage < all.size(); p++) {
                        screens.add(new SheetScreen(all.subList(p * perPage, Math.min(all.size(), (p + 1) * perPage)), 40f, true));
                        names.add("sheet-" + (p + 1));
                    }
                }
                case "compass" -> {
                    screens.add(new SheetScreen(compassCells(mc), 72f, false));
                    names.add("compass");
                }
                case "worn" -> {
                    for (String ring : RingPose.RINGS) {
                        if (ring.equals("moon_ring") && ArtifactItems.MOON_RING == null) {
                            continue;
                        }
                        screens.add(new WornScreen(ring));
                        names.add("worn-" + ring);
                    }
                }
                default -> SupernaturalBloodlines.LOGGER.error("ITEMSHOTS: unknown kind {}", kind);
            }
        }
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
        if (screens.isEmpty() && index == 0) {
            if (mc.options.guiScale().get() != 2) {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                return;
            }
            build(mc);
        }
        if (index >= screens.size()) {
            done = true;
            SupernaturalBloodlines.LOGGER.warn("ITEMSHOTS done");
            if (quit) {
                mc.stop();
            }
            return;
        }
        if (wait == 0) {
            mc.setScreen(screens.get(index));
        }
        if (++wait < SETTLE_TICKS) {
            return;
        }
        new File(mc.gameDirectory, "screenshots/itemshots").mkdirs();
        String name = "itemshots/" + tag + "-" + names.get(index) + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), c -> { });
        SupernaturalBloodlines.LOGGER.warn("ITEMSHOTS {}", name);
        index++;
        wait = 0;
    }

    /** One item model in one display context at a pixel size (1 block = {@code size} px), the way {@code GuiGraphics.renderItem} draws a GUI icon. */
    private static void drawModel(GuiGraphics g, ItemStack stack, ItemDisplayContext ctx, float cx, float cy, float size) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 150f);
        ps.scale(size, -size, size);
        if (ctx == ItemDisplayContext.GROUND) {
            ps.mulPose(Axis.XP.rotationDegrees(-15f));
            ps.mulPose(Axis.YP.rotationDegrees(35f));
        } else if (ctx != ItemDisplayContext.GUI) {
            ps.mulPose(Axis.YP.rotationDegrees(-30f));
        }
        boolean flat = !model.usesBlockLight();
        if (flat) {
            Lighting.setupForFlatItems();
        } else {
            Lighting.setupFor3DItems();
        }
        mc.getItemRenderer().render(stack, ctx, false, ps, g.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, model);
        g.flush();
        Lighting.setupFor3DItems();
        ps.popPose();
    }

    private static final class SheetScreen extends Screen {
        private final List<Cell> cells;
        private final float size;
        private final boolean withLabel;

        SheetScreen(List<Cell> cells, float size, boolean withLabel) {
            super(Component.empty());
            this.cells = cells;
            this.size = size;
            this.withLabel = withLabel;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            int cols = Math.max(1, width / CELL_W);
            for (int i = 0; i < cells.size(); i++) {
                Cell c = cells.get(i);
                int x = (i % cols) * CELL_W;
                int y = 4 + (i / cols) * CELL_H;
                if (withLabel) {
                    g.drawString(font, font.plainSubstrByWidth(c.label(), CELL_W - 4), x + 2, y, 0xFFFFFF, false);
                    drawModel(g, c.stack(), ItemDisplayContext.GUI, x + 22, y + 28, size * 0.5f);
                    drawModel(g, c.stack(), ItemDisplayContext.GROUND, x + 64, y + 28, size * 0.9f);
                    drawModel(g, c.stack(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, x + 106, y + 28, size * 0.9f);
                } else {
                    int w = Math.max(width / 4, 1);
                    x = (i % 4) * w;
                    y = 8 + (i / 4) * (height / 2);
                    g.drawString(font, c.label(), x + 4, y, 0xFFFFFF, false);
                    drawModel(g, c.stack(), ItemDisplayContext.GUI, x + w / 2f, y + 50, size);
                    drawModel(g, c.stack(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, x + w / 2f, y + 120, size);
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    /** Classic and slim players, each seen from the side that shows one arm, standing (top) and sneaking (bottom), wearing the ring in both slots. */
    private static final class WornScreen extends Screen {
        private final String ring;
        private final RemotePlayer[] players = new RemotePlayer[4];
        private final boolean[] slim = {false, false, true, true};
        private boolean equipped;

        WornScreen(String ring) {
            super(Component.empty());
            this.ring = ring;
        }

        private static UUID uuidFor(boolean slim) {
            for (long i = 1; ; i++) {
                UUID u = new UUID(i * 7919L, i * 104729L);
                if ((DefaultPlayerSkin.get(u).model() == PlayerSkin.Model.SLIM) == slim) {
                    return u;
                }
            }
        }

        private void equip(Minecraft mc) {
            Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, ring));
            for (int i = 0; i < players.length; i++) {
                players[i] = new RemotePlayer(mc.level, new GameProfile(uuidFor(slim[i]), ""));
                if (ModList.get().isLoaded("curios")) {
                    WornCurios.equip(players[i], new ItemStack(item));
                }
            }
            equipped = true;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, width, height, 0xFF2B2B33);
            Minecraft mc = Minecraft.getInstance();
            if (!equipped) {
                equip(mc);
            }
            int w = width / 4;
            int h = height / 3;
            // rows: the whole player standing, then the hand close up standing and sneaking
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                    RemotePlayer p = players[col];
                    p.setPose(row == 2 ? Pose.CROUCHING : Pose.STANDING);
                    p.setShiftKeyDown(row == 2);
                    // columns: classic right arm, classic left arm, slim right arm, slim left arm
                    float yaw = 180f + (col % 2 == 0 ? -90f : 90f);
                    p.setYRot(yaw);
                    p.yRotO = yaw;
                    p.yBodyRot = yaw;
                    p.yBodyRotO = yaw;
                    p.yHeadRot = yaw;
                    p.yHeadRotO = yaw;
                    p.setXRot(0f);
                    p.xRotO = 0f;
                    int cx = col * w + w / 2;
                    int cy = row * h + h / 2 + (row == 0 ? 6 : 0);
                    Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
                    g.enableScissor(col * w, row * h, (col + 1) * w, (row + 1) * h);
                    if (row == 0) {
                        InventoryScreen.renderEntityInInventory(g, cx, cy, h / 2.2f, new Vector3f(0f, p.getBbHeight() / 2f, 0f), pose, null, p);
                    } else {
                        // standing: the hand is 12/16 of a block above the feet; sneaking it swings forward, so the frame is wider
                        InventoryScreen.renderEntityInInventory(g, cx, cy, row == 1 ? 330f : 150f, new Vector3f(0f, row == 1 ? 0.75f : 0.5f, 0f), pose, null, p);
                    }
                    g.disableScissor();
                    g.drawString(font, (slim[col] ? "slim " : "classic ") + (col % 2 == 0 ? "right" : "left") + (row == 2 ? " sneaking" : ""), col * w + 4, row * h + 2, 0xFFFFFF, false);
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    /** Links Curios: loaded only when Curios is. Local to the client player object, nothing is sent. */
    private static final class WornCurios {
        static void equip(LivingEntity e, ItemStack ring) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(e).ifPresent(inv -> inv.getStacksHandler("ring").ifPresent(h -> {
                if (h.getSlots() < 2) {
                    h.grow(2 - h.getSlots());
                }
                h.getStacks().setStackInSlot(0, ring.copy());
                h.getStacks().setStackInSlot(1, ring.copy());
            }));
        }
    }
}
