package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Flat;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SpritePresence;

/**
 * Draws the art kit: a theme's sprite by {@link Part} (the nine-slice and tiling come from the sprite's own {@code .mcmeta}, vanilla's {@code blitSprite}), and
 * the shared icons. One texture blit each, never a fill per pixel. A sprite a resource pack removed is drawn flat in the theme's colours, so a screen stays usable.
 * Client only.
 */
public final class UiKit {
    private static final SpritePresence PRESENCE = new SpritePresence(UiKit::probe);

    private UiKit() {
    }

    public static ResourceLocation id(String sprite) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, sprite);
    }

    private static boolean probe(String sprite) {
        return !Minecraft.getInstance().getGuiSprites().getSprite(id(sprite)).contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    /** Forget which sprites exist: the resource packs changed. */
    public static void reload() {
        PRESENCE.clear();
    }

    public static boolean has(String sprite) {
        return PRESENCE.has(sprite);
    }

    public static void draw(GuiGraphics g, HubTheme theme, Part part, Rect r) {
        draw(g, theme, part, r.x(), r.y(), r.w(), r.h());
    }

    /** {@code part} of the theme's kit stretched, sliced or tiled over the box; flat colours when its sprite is missing. */
    public static void draw(GuiGraphics g, HubTheme theme, Part part, int x, int y, int w, int h) {
        draw(g, theme, part, x, y, w, h, 1f);
    }

    public static void draw(GuiGraphics g, HubTheme theme, Part part, int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        String sprite = KitSprites.of(theme.kit(), part.sprite());
        if (!has(sprite)) {
            flat(g, theme, part.flat(), x, y, w, h);
            return;
        }
        int m = part.margin();
        blend(g, alpha);
        g.blitSprite(id(sprite), x - m, y - m, w + 2 * m, h + 2 * m);
        reset(g, alpha);
    }

    private static void blend(GuiGraphics g, float alpha) {
        if (alpha < 1f) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, alpha);
        }
    }

    private static void reset(GuiGraphics g, float alpha) {
        if (alpha < 1f) {
            g.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** What a part looks like without its sprite: the theme's own colours, one fill and an outline, so it stays readable. */
    public static void flat(GuiGraphics g, HubTheme theme, Flat kind, int x, int y, int w, int h) {
        switch (kind) {
            case NONE -> {
            }
            case SURFACE -> {
                g.fill(x, y, x + w, y + h, theme.muted());
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, theme.paper());
            }
            case ACCENT -> {
                g.fill(x, y, x + w, y + h, theme.paper());
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, theme.accent());
            }
            case TRACK -> g.fill(x, y, x + w, y + h, theme.muted() & 0xFFFFFF | 0x80000000);
            case LINE -> g.fill(x, y + h / 2, x + w, y + h / 2 + 1, theme.muted());
            case RING -> {
                g.fill(x, y, x + w, y + 2, theme.focusColor());
                g.fill(x, y + h - 2, x + w, y + h, theme.focusColor());
                g.fill(x, y, x + 2, y + h, theme.focusColor());
                g.fill(x + w - 2, y, x + w, y + h, theme.focusColor());
            }
        }
    }

    /** The kit's focus ring round {@code r}: two pixels outside it. */
    public static void focus(GuiGraphics g, HubTheme theme, Rect r) {
        draw(g, theme, Part.FOCUS, r.x() - 3, r.y() - 3, r.w() + 6, r.h() + 6);
    }

    /**
     * The icon {@code name} in a {@code box} square at ({@code x}, {@code y}): the 16 px or the 32 px picture, whole or halved, centred ({@link KitSprites#iconPlan}).
     * The picture keeps its own colours; only the alpha of {@code argb} is used (a locked or resting icon is dimmed, never recoloured).
     * Returns false when neither sprite exists, so the caller can fall back.
     */
    public static boolean icon(GuiGraphics g, String name, int x, int y, int box, int argb) {
        KitSprites.IconPlan plan = KitSprites.iconPlan(box);
        String sprite = KitSprites.icon(plan.sprite(), name);
        if (!has(sprite)) {
            return false;
        }
        int alpha = argb >>> 24;
        float a = alpha == 0 ? 1f : alpha / 255f;
        blend(g, a);
        g.blitSprite(id(sprite), x + (box - plan.draw()) / 2, y + (box - plan.draw()) / 2, plan.draw(), plan.draw());
        reset(g, a);
        return true;
    }

    public static boolean has(String kitName, Part part) {
        return has(KitSprites.of(kitName, part.sprite()));
    }
}
