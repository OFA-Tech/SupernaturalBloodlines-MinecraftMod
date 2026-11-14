package net.ofatech.supernaturalbloodlines.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * The red tint of a lunar eclipse, on the overworld's fog and horizon, and (plan 10) on the sky and the moon: a translucent
 * red disc over the sky dome and a dark orange moon, both drawn after the vanilla sky. Client only: nothing but the payload
 * handler in ModNetwork refers to this class. NeoForge 1.21.1 has no event for the sky dome's own colour, so the sky is tinted
 * by drawing over it instead of with a mixin.
 */
public final class EclipseClient {
    /** The sky tint (RGB), the same red as the fog, drawn over the sky at {@link #SKY_ALPHA}. */
    public static final int SKY_TINT = 0x8C0808;
    /** The owner's dark orange for the eclipse moon. */
    public static final int MOON_TINT = 0xB4501E;
    private static final float SKY_ALPHA = 0.55f;
    private static final float STRENGTH = 0.65f;
    private static final float[] RED = {0.55f, 0.03f, 0.03f};
    // vanilla draws the moon as a 40 x 40 quad at 100 blocks; its disc fills about 0.9 of the cell
    private static final float MOON_DISTANCE = 100f;
    private static final float MOON_RADIUS = 7f;
    private static final int DISC_STEPS = 48;
    private static boolean active;
    private static boolean listening;

    private EclipseClient() {}

    public static void set(boolean on) {
        active = on;
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFogColor e) -> {
                if (inEclipse()) {
                    e.setRed(mix(e.getRed(), RED[0]));
                    e.setGreen(mix(e.getGreen(), RED[1]));
                    e.setBlue(mix(e.getBlue(), RED[2]));
                }
            });
            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
                if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && inEclipse()) {
                    drawSkyAndMoon(e.getModelViewMatrix(), e.getPartialTick().getGameTimeDeltaPartialTick(false));
                }
            });
            NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> active = false);
        }
    }

    private static boolean inEclipse() {
        Minecraft mc = Minecraft.getInstance();
        return active && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
    }

    private static float mix(float from, float to) {
        return from + (to - from) * STRENGTH;
    }

    private static void drawSkyAndMoon(Matrix4f modelView, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // the vanilla sky disc: flat, 16 blocks up, 512 across, so the tint reaches the horizon
        disc(modelView, 16f, 512f, SKY_TINT, SKY_ALPHA);

        // the moon, where vanilla draws it (the sky turns with the time of day)
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(Minecraft.getInstance().level.getTimeOfDay(partialTick) * 360f));
        disc(pose.last().pose(), -MOON_DISTANCE, MOON_RADIUS, MOON_TINT, 1f);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /** A disc in the XZ plane at height {@code y}, centred on the origin. */
    private static void disc(Matrix4f matrix, float y, float radius, int rgb, float alpha) {
        float r = (rgb >> 16 & 255) / 255f;
        float g = (rgb >> 8 & 255) / 255f;
        float b = (rgb & 255) / 255f;
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, 0f, y, 0f).setColor(r, g, b, alpha);
        for (int i = 0; i <= DISC_STEPS; i++) {
            double angle = Math.PI * 2 * i / DISC_STEPS;
            builder.addVertex(matrix, (float) (radius * Math.cos(angle)), y, (float) (radius * Math.sin(angle))).setColor(r, g, b, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
}
