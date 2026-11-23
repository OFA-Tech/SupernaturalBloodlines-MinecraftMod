package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FrameStrip;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitColors;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitFaces;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Flat;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.NineSlicePlan;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SpritePresence;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the art kit: a theme's sprite by {@link Part}, and the shared icons. A nine-slice is drawn by {@link NineSlicePlan} (the edges tiled, a solid middle one
 * fill, so a panel is tens of blits and not thousands); a tile or a plain sprite by vanilla's {@code blitSprite}. A sprite a resource pack removed is drawn flat
 * in the theme's colours, so a screen stays usable. Everything that can be looked up once is kept until the packs reload. Client only.
 */
public final class UiKit {
    private static final SpritePresence PRESENCE = new SpritePresence(UiKit::probe);
    private static final Map<String, Entry[]> PARTS = new HashMap<>();
    private static final Map<String, Icon> ICONS_16 = new HashMap<>();
    private static final Map<String, Icon> ICONS_32 = new HashMap<>();
    private static final Map<String, String> SKILLS = new HashMap<>();
    private static final java.util.Set<String> WARNED = new java.util.HashSet<>();

    /** A theme sprite: its id, whether it exists, and how to cut it when it is a nine-slice. */
    private record Entry(ResourceLocation id, boolean present, @Nullable Slice slice) {
    }

    private record Slice(ResourceLocation atlas, int x, int y, int atlasW, int atlasH, int w, int h, int l, int t, int r, int b, @Nullable Integer middle) {
    }

    private record Icon(@Nullable ResourceLocation id, boolean present) {
    }

    private UiKit() {
    }

    public static ResourceLocation id(String sprite) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, sprite);
    }

    private static boolean probe(String sprite) {
        // an invalid path throws ResourceLocationException: the presence cache turns that into "not there"
        return !Minecraft.getInstance().getGuiSprites().getSprite(id(sprite)).contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    /** Forget everything looked up: the resource packs changed. */
    public static void reload() {
        PRESENCE.clear();
        PARTS.clear();
        ICONS_16.clear();
        ICONS_32.clear();
        SKILLS.clear();
        STRIPS.clear();
        IDS.clear();
        WARNED.clear();
    }

    public static boolean has(String sprite) {
        return PRESENCE.has(sprite);
    }

    public static boolean has(String kitName, Part part) {
        return entry(kitName, part).present();
    }

    private static Entry entry(String kit, Part part) {
        Entry[] row = PARTS.computeIfAbsent(kit, k -> new Entry[Part.values().length]);
        Entry e = row[part.ordinal()];
        if (e == null) {
            String path = KitSprites.of(kit, part.sprite());
            ResourceLocation id = id(path);
            boolean present = has(path);
            e = new Entry(id, present, present && KitSprites.SCALED.contains(part) ? slice(id, path) : null);
            row[part.ordinal()] = e;
        }
        return e;
    }

    /** How a nine-slice sprite is cut, or null when it is not one (a tile and a plain sprite go through vanilla). */
    @Nullable
    private static Slice slice(ResourceLocation id, String path) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites().getSprite(id);
        if (!(Minecraft.getInstance().getGuiSprites().getSpriteScaling(sprite) instanceof GuiSpriteScaling.NineSlice ns)) {
            return null;
        }
        int w = ns.width();
        int h = ns.height();
        if (!NineSlicePlan.sizesMatch(sprite.contents().width(), sprite.contents().height(), w, h)) {
            return null; // a hi-res pack: vanilla's blitSprite draws it
        }
        var b = ns.border();
        int atlasW = Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
        int atlasH = Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
        return new Slice(sprite.atlasLocation(), sprite.getX(), sprite.getY(), atlasW, atlasH, w, h, b.left(), b.top(), b.right(), b.bottom(), middle(path, w, h, b));
    }

    /** The one colour the middle of the picture is (read once from the pack's PNG), or null when it is not one colour. */
    @Nullable
    private static Integer middle(String path, int w, int h, GuiSpriteScaling.NineSlice.Border b) {
        ResourceLocation png = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/sprites/" + path + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(png); NativeImage img = NativeImage.read(in)) {
            if (img.getWidth() != w || img.getHeight() != h) {
                return null;
            }
            int[] argb = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int abgr = img.getPixelRGBA(x, y);
                    argb[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                }
            }
            return NineSlicePlan.solidMiddle(argb, w, h, b.left(), b.top(), b.right(), b.bottom());
        } catch (IOException | RuntimeException unreadable) {
            if (WARNED.add(path)) {
                SupernaturalBloodlines.LOGGER.warn("Could not read the picture of the kit sprite {} ({}): its middle is drawn tile by tile", path, unreadable.toString());
            }
            return null;
        }
    }

    public static void draw(GuiGraphics g, KitColors theme, Part part, Rect r) {
        draw(g, theme, part, r.x(), r.y(), r.w(), r.h(), 1f);
    }

    /** {@code part} of the theme's kit stretched, sliced or tiled over the box; flat colours when its sprite is missing. */
    public static void draw(GuiGraphics g, KitColors theme, Part part, int x, int y, int w, int h) {
        draw(g, theme, part, x, y, w, h, 1f);
    }

    public static void draw(GuiGraphics g, KitColors theme, Part part, int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Entry e = entry(theme.kit(), part);
        if (!e.present()) {
            flat(g, theme, part.flat(), x, y, w, h);
            return;
        }
        int m = part.margin();
        blend(g, alpha);
        if (e.slice() != null) {
            Slice s = e.slice();
            for (NineSlicePlan.Op op : NineSlicePlan.of(s.w(), s.h(), s.l(), s.t(), s.r(), s.b(), x - m, y - m, w + 2 * m, h + 2 * m, s.middle())) {
                if (op.fill()) {
                    g.fill(op.x(), op.y(), op.x() + op.w(), op.y() + op.h(), op.color()); // the shader colour set by blend() tints a fill too
                } else {
                    g.blit(s.atlas(), op.x(), op.y(), op.w(), op.h(), (float) (s.x() + op.u()), (float) (s.y() + op.v()), op.w(), op.h(), s.atlasW(), s.atlasH());
                }
            }
        } else {
            g.blitSprite(e.id(), x - m, y - m, w + 2 * m, h + 2 * m);
        }
        reset(g, alpha);
    }

    /** Blending is on and the normal blend function set for every kit blit, faded or not: another layer may have left it off or changed. Only the tint is skipped at full alpha. */
    private static void blend(GuiGraphics g, float alpha) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        if (alpha < 1f) {
            g.setColor(1f, 1f, 1f, alpha);
        }
    }

    private static void reset(GuiGraphics g, float alpha) {
        if (alpha < 1f) {
            g.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** What a part looks like without its sprite: the theme's own colours, one fill and an outline, so it stays readable. */
    public static void flat(GuiGraphics g, KitColors theme, Flat kind, int x, int y, int w, int h) {
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

    /** The colour of the label on a kit button of {@code theme}: read on the picture when there is one, on the flat colours that replace it when there is not. */
    public static int label(KitColors theme, Part button) {
        return entry(theme.kit(), button).present() ? KitFaces.labelOn(faceOf(theme, button), theme.ink())
                : KitFaces.labelOn(KitFaces.flatFace(button.flat(), theme.paper(), theme.accent(), theme.muted()), theme.ink());
    }

    /** {@code preferred} for text on {@code part} while its picture is there; on the flat colours that replace a missing picture, white or black when {@code preferred} would not read on them. */
    public static int labelOver(KitColors theme, Part part, int preferred) {
        if (entry(theme.kit(), part).present()) {
            return KitFaces.knows(part) ? KitFaces.labelOn(faceOf(theme, part), preferred) : preferred;
        }
        return KitFaces.labelOn(KitFaces.flatFace(part.flat(), theme.paper(), theme.accent(), theme.muted()), preferred);
    }

    /** What a button or surface is in the pack in use: its picture's solid middle when it has one (a repainted sprite), else the shipped art's colour. */
    private static int faceOf(KitColors theme, Part part) {
        Slice s = entry(theme.kit(), part).slice();
        return KitFaces.resolveFace(KitFaces.face(theme.kit(), part), s == null ? null : s.middle());
    }

    /** The focus ring round {@code r}: the kit's picture (two pixels outside it), or the theme's focus colour in a two pixel frame when the picture is gone. */
    public static void focus(GuiGraphics g, KitColors theme, Rect r) {
        if (entry(theme.kit(), Part.FOCUS).present()) {
            draw(g, theme, Part.FOCUS, r.x() - 3, r.y() - 3, r.w() + 6, r.h() + 6);
        } else {
            flat(g, theme, Flat.RING, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4);
        }
    }

    /** A strip's place in the atlas; {@code NO_STRIP} for one that is missing. */
    private record Strip(@Nullable ResourceLocation atlas, int x, int y, int atlasW, int atlasH, int w, int h) {
    }

    private static final Strip NO_STRIP = new Strip(null, 0, 0, 0, 0, 0, 0);
    private static final Map<String, Strip> STRIPS = new HashMap<>();
    private static final Map<String, ResourceLocation> IDS = new HashMap<>();

    private static Strip strip(String sprite) {
        return STRIPS.computeIfAbsent(sprite, s -> {
            if (!has(s)) {
                return NO_STRIP;
            }
            TextureAtlasSprite sp = Minecraft.getInstance().getGuiSprites().getSprite(id(s));
            int atlasW = Math.round(sp.contents().width() / (sp.getU1() - sp.getU0()));
            int atlasH = Math.round(sp.contents().height() / (sp.getV1() - sp.getV0()));
            return new Strip(sp.atlasLocation(), sp.getX(), sp.getY(), atlasW, atlasH, sp.contents().width(), sp.contents().height());
        });
    }

    /**
     * Frame {@code frame} of a strip of {@code frames} square frames of {@code size} (the cooldown pie, the ring of blood) at ({@code x}, {@code y}), faded by
     * {@code alpha}. False when the strip is missing or is not that many frames of that size (a hi-res pack): the caller draws its stand-in.
     */
    public static boolean frame(GuiGraphics g, String sprite, int frame, int size, int frames, int x, int y, float alpha) {
        Strip s = strip(sprite);
        if (s == NO_STRIP) {
            return false;
        }
        if (!FrameStrip.holds(s.w(), s.h(), size, frames)) {
            if (WARNED.add(sprite)) {
                SupernaturalBloodlines.LOGGER.warn("{} is {} x {}, not {} frames of {} px (a resource pack?): the HUD draws its plain stand-in", sprite, s.w(), s.h(), frames, size);
            }
            return false;
        }
        if (frame < 0 || frame >= frames) {
            return false;
        }
        blend(g, alpha);
        g.blit(s.atlas(), x, y, size, size, (float) s.x(), (float) (s.y() + FrameStrip.top(frame, size)), size, size, s.atlasW(), s.atlasH());
        reset(g, alpha);
        return true;
    }

    /** A sprite of the kit that is not a theme's own (the shared ones) at its own size, faded; false when it is missing. */
    public static boolean shared(GuiGraphics g, String sprite, int x, int y, int w, int h, float alpha) {
        if (!has(sprite)) {
            return false;
        }
        blend(g, alpha);
        g.blitSprite(IDS.computeIfAbsent(sprite, UiKit::id), x, y, w, h);
        reset(g, alpha);
        return true;
    }

    private static Icon icon(int size, String name) {
        return (size == 16 ? ICONS_16 : ICONS_32).computeIfAbsent(name, n -> {
            String path = KitSprites.icon(size, n);
            return KitSprites.validName(path) && has(path) ? new Icon(id(path), true) : new Icon(null, false);
        });
    }

    /** The name of a skill's icon: its own picture, the "unknown" rune for a skill of another mod or one without a picture. */
    public static String skillIconName(String skillId) {
        return SKILLS.computeIfAbsent(skillId, s -> KitSprites.skillIcon(s, UiKit::has));
    }

    /**
     * The icon {@code name} in a {@code box} square at ({@code x}, {@code y}): the 16 px or the 32 px picture, whole or halved, centred ({@link KitSprites#iconPlan}).
     * The picture keeps its own colours; only the alpha of {@code argb} is used (a locked or resting icon is dimmed, never recoloured).
     * Returns false when neither sprite exists, so the caller can fall back.
     */
    public static boolean icon(GuiGraphics g, String name, int x, int y, int box, int argb) {
        KitSprites.IconPlan plan = KitSprites.iconPlan(box);
        Icon icon = icon(plan.sprite(), name);
        if (!icon.present()) {
            return false;
        }
        int alpha = argb >>> 24;
        float a = alpha == 0 ? 1f : alpha / 255f;
        blend(g, a);
        g.blitSprite(icon.id(), x + (box - plan.draw()) / 2, y + (box - plan.draw()) / 2, plan.draw(), plan.draw());
        reset(g, a);
        return true;
    }
}
