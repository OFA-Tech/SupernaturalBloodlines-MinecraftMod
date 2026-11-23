package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * The portraits ({@code textures/gui/boss/<name>.png}) as plain textures: each one's size is read once (a resource pack may draw them larger), and a picture that
 * is not a square multiple of 24 is not drawn at all, never stretched. Client only.
 */
final class BossPortraitTex {
    private record Tex(ResourceLocation id, int w, int h, boolean usable) {
    }

    private static final Map<String, Tex> TEXTURES = new HashMap<>();

    private BossPortraitTex() {
    }

    static void reload() {
        TEXTURES.clear();
    }

    private static Tex tex(String name) {
        Tex t = TEXTURES.get(name);
        if (t == null) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/boss/" + name + ".png");
            int w = 0;
            int h = 0;
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(id); NativeImage img = NativeImage.read(in)) {
                w = img.getWidth();
                h = img.getHeight();
            } catch (IOException | RuntimeException unreadable) {
                SupernaturalBloodlines.LOGGER.warn("Could not read the boss portrait {} ({}): its card is drawn without it", id, unreadable.toString());
            }
            boolean ok = BossPortraits.usable(w, h);
            if (w > 0 && !ok) {
                SupernaturalBloodlines.LOGGER.warn("The boss portrait {} is {} x {}, not a square multiple of 24 (a resource pack?): its card is drawn without it", id, w, h);
            }
            t = new Tex(id, w, h, ok);
            TEXTURES.put(name, t);
        }
        return t;
    }

    /** The whole of portrait {@code name} in its 24 x 24 window at ({@code x}, {@code y}). */
    static void draw(GuiGraphics g, String name, int x, int y) {
        Tex t = tex(name);
        if (t.usable()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(t.id(), x, y, BossBarLayout.ART, BossBarLayout.ART, 0f, 0f, t.w(), t.h(), t.w(), t.h());
        }
    }

    /** A shift: the human on the left of the seam and the wolf on its right, each cut off by the scissor so there are two blits and no per-row work. */
    static void drawSeam(GuiGraphics g, String human, String wolf, int x, int y, int seamX) {
        g.enableScissor(x, y, x + seamX, y + BossBarLayout.ART);
        draw(g, human, x, y);
        g.disableScissor();
        g.enableScissor(x + seamX, y, x + BossBarLayout.ART, y + BossBarLayout.ART);
        draw(g, wolf, x, y);
        g.disableScissor();
    }
}
