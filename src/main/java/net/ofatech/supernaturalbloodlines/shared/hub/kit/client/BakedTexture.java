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
 * Ghoul's moon were a fill per row (or per segment) every frame; this makes them one draw call. The least recently used textures are dropped once the cache
 * holds more than {@link #MAX_PIXELS} pixels, so a moon that changes with the progress or the window does not pile up. Client only, on the render thread.
 */
public final class BakedTexture {
    /** About 4 MB of texture: a few full moons and every shape in use. */
    static final long MAX_PIXELS = 1_000_000L;

    /** A cache key that is not a string built every frame: the kind of picture and up to three numbers that make it differ. */
    public record Key(String kind, int a, int b, int c) {
        public static Key of(String kind, int a) {
            return new Key(kind, a, 0, 0);
        }

        public static Key of(String kind, int a, int b) {
            return new Key(kind, a, b, 0);
        }
    }

    private record Entry(ResourceLocation id, long pixels) {
    }

    private static final Map<Key, Entry> CACHE = new LinkedHashMap<>(64, 0.75f, true);
    private static final java.util.Set<Key> FAILED = new java.util.HashSet<>();
    private static long pixels;
    private static int serial;
    private static boolean warned;

    private BakedTexture() {
    }

    /** The texture for {@code key}, painted by {@code painter} on the first ask; null if it cannot be made (it is not tried again). */
    public static ResourceLocation get(Key key, int w, int h, IntBinaryOperator painter) {
        Entry cached = CACHE.get(key);
        if (cached != null) {
            return cached.id();
        }
        if (FAILED.contains(key) || w <= 0 || h <= 0) {
            return null;
        }
        NativeImage image = null;
        try {
            image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = painter.applyAsInt(x, y);
                    // NativeImage keeps ABGR
                    image.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF));
                }
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "baked/" + (serial++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            image = null; // the texture owns it now
            long size = (long) w * h;
            CACHE.put(key, new Entry(id, size));
            pixels += size;
            trim();
            return id;
        } catch (RuntimeException | LinkageError e) {
            if (image != null) {
                image.close();
            }
            FAILED.add(key);
            if (!warned) {
                warned = true;
                SupernaturalBloodlines.LOGGER.warn("Could not bake a hub texture ({}); drawing that picture the slow way from now on", key, e);
            }
            return null;
        }
    }

    private static void trim() {
        Iterator<Map.Entry<Key, Entry>> it = CACHE.entrySet().iterator();
        while (pixels > MAX_PIXELS && CACHE.size() > 1 && it.hasNext()) {
            Entry oldest = it.next().getValue();
            Minecraft.getInstance().getTextureManager().release(oldest.id());
            pixels -= oldest.pixels();
            it.remove();
        }
    }

    /** Draws a baked {@code w x h} picture at ({@code x}, {@code y}), 1 to 1. */
    public static void blit(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) {
        if (id == null) {
            return; // the picture could not be baked: nothing is drawn (the failure was logged once)
        }
        RenderSystem.enableBlend();
        g.blit(id, x, y, 0, 0, w, h, w, h);
    }

    /** Draws a baked {@code texW x texH} picture stretched to {@code w x h}. */
    public static void blitScaled(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int texW, int texH) {
        if (id == null) {
            return;
        }
        RenderSystem.enableBlend();
        g.blit(id, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
    }

    /** The same as {@link #blit}, tinted by an ARGB colour (the picture is a white mask). */
    public static void blitTinted(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int argb) {
        if (id == null) {
            return;
        }
        g.setColor(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
        blit(g, id, x, y, w, h);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** Drops every texture: a resource reload (the pictures may differ with a new pack) or a new game. */
    public static void clear() {
        for (Entry e : CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(e.id());
        }
        CACHE.clear();
        FAILED.clear();
        pixels = 0;
    }
}
