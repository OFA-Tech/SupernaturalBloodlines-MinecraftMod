package net.ofatech.supernaturalbloodlines.parts.hud.client.tensura;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.manasmods.tensura.handler.client.OverlayHandler;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.BloodSpotRule;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.VampireBlood;

/**
 * A Vampirism vampire's blood in Tensura's food spot. Tensura's food number is hidden through its own in-memory {@code decorations.food.render} flag
 * ({@link TensuraHudFlags}, put back exactly as the player had it), Vampirism's blood bar layer is skipped ({@link VampireBlood#registerBarHiding}), and
 * we draw our drop and the number where Tensura draws its own, with Tensura's own placement: {@code OverlayHandler.transform(7, scale, left)} (the
 * private routine that every decoration uses, reached through a private lookup) is applied to our pose, then the icon is at 0,0 (9 x 9) and the number
 * at {@code left ? -2 - width : 11}, 1, as in {@code renderFoodDecoration}. If Tensura's private pieces cannot be reached (an update renamed them),
 * {@link #ready()} is false, nothing is hidden and both original displays stay. Linked to Tensura and Vampirism: reached only when both are loaded.
 */
public final class BloodInFoodSpot {
    private static final int FOOD_DECO = OverlayHandler.FOOD_DECO;
    private static final ResourceLocation FULL = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/blood_full.png");
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/blood_empty.png");

    private static final VarHandle POSE;
    private static final MethodHandle TRANSFORM;
    private static BloodSpotRule.Show show = BloodSpotRule.Show.TENSURA_FOOD;
    /** Set once a draw has thrown: the originals stay for the rest of the session (the rule is re-evaluated every tick and would turn it back on). */
    private static volatile boolean broken;

    static {
        VarHandle pose = null;
        MethodHandle transform = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(OverlayHandler.class, MethodHandles.lookup());
            pose = lookup.findStaticVarHandle(OverlayHandler.class, "poseStack", PoseStack.class);
            transform = lookup.findStatic(OverlayHandler.class, "transform", MethodType.methodType(void.class, int.class, float.class, boolean.class));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            SupernaturalBloodlines.LOGGER.warn("HUD: Tensura's food decoration cannot be reached ({}); the blood stays in Vampirism's bar", e.toString());
        }
        POSE = pose;
        TRANSFORM = transform;
    }

    private BloodInFoodSpot() {
    }

    public static boolean ready() {
        return POSE != null && TRANSFORM != null && !broken;
    }

    public static boolean hidesVampirismBar() {
        return show.hidesVampirismBar();
    }

    public static boolean hidesTensuraFood() {
        return show.hidesTensuraFood();
    }

    public static void register() {
        VampireBlood.registerBarHiding(BloodInFoodSpot::hidesVampirismBar);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            LocalPlayer p = Minecraft.getInstance().player;
            show = p == null ? BloodSpotRule.Show.TENSURA_FOOD
                    : BloodSpotRule.decide(true, true, HudClientConfig.bloodInFoodSpot(), VampireBlood.isVampire(p), ready());
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> show = BloodSpotRule.Show.TENSURA_FOOD);
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (show == BloodSpotRule.Show.BLOOD_IN_FOOD_SPOT && mc.player != null && !mc.options.hideGui && !mc.player.isSpectator()) {
                draw(e.getGuiGraphics(), mc, VampireBlood.level(mc.player));
            }
        });
    }

    private static void draw(GuiGraphics g, Minecraft mc, int blood) {
        PoseStack stack = g.pose();
        Object before = POSE.get();
        POSE.set(stack);
        stack.pushPose();
        try {
            boolean left = OverlayHandler.isLeftSide(FOOD_DECO);
            TRANSFORM.invoke(FOOD_DECO, OverlayHandler.getScaleById(FOOD_DECO), left);
            String text = Integer.toString(Math.max(0, blood));
            g.blit(blood <= 0 ? EMPTY : FULL, 0, 0, 0, 0, 9, 9, 9, 9);
            g.drawString(mc.font, text, left ? -2 - mc.font.width(text) : 11, 1, 0xFFFFFF);
        } catch (Throwable t) {
            show = BloodSpotRule.Show.VAMPIRISM_BAR; // never half draw twice: fall back to the originals
            if (!broken) {
                broken = true;
                SupernaturalBloodlines.LOGGER.warn("HUD: drawing the blood in Tensura's food spot failed; the originals are back for this session", t);
            }
        } finally {
            stack.popPose();
            POSE.set(before);
        }
    }
}
