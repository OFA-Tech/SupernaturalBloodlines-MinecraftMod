package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * A picture painted once by a pure {@code (x, y) -> ARGB} function into a texture, and drawn after that with one blit. The slots, the Blood Moon, its glow and the
 * Ghoul's moon were a fill per row (or per segment) every frame; this makes them one draw call. The newest {@link #LIMIT} textures are kept, so a moon that
 * changes with the progress or the window does not pile up. Client only, on the render thread.
 */
public final class BakedTexture {
    private static final int LIMIT = 96;
    private static final Map<String, ResourceLocation> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static int serial;

    private BakedTexture() {
    }

    /** The texture for {@code key}, painted by {@code painter} on the first ask (or after it was dropped). */
    public static ResourceLocation get(String key, int w, int h, IntBinaryOperator painter) {
        ResourceLocation id = CACHE.get(key);
        if (id != null) {
            return id;
        }
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = painter.applyAsInt(x, y);
                // NativeImage keeps ABGR
                image.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF));
            }
        }
        id = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "baked/" + (serial++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        CACHE.put(key, id);
        trim();
        return id;
    }

    private static void trim() {
        if (CACHE.size() <= LIMIT) {
            return;
        }
        Iterator<Map.Entry<String, ResourceLocation>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > LIMIT && it.hasNext()) {
            Map.Entry<String, ResourceLocation> oldest = it.next();
            Minecraft.getInstance().getTextureManager().release(oldest.getValue());
            it.remove();
        }
    }

    /** Draws a baked {@code w x h} picture at ({@code x}, {@code y}), 1 to 1. */
    public static void blit(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        g.blit(id, x, y, 0, 0, w, h, w, h);
    }

    /** The same, tinted by an ARGB colour (the picture is a white mask). */
    public static void blitTinted(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int argb) {
        g.setColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
        blit(g, id, x, y, w, h);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** Drops every texture (a resource reload, or the end of the world). */
    public static void clear() {
        for (ResourceLocation id : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
        CACHE.clear();
    }
}
