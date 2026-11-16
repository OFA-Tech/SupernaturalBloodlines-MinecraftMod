package net.ofatech.supernaturalbloodlines.shared.hub.kit.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.hub.Contrast;
import net.ofatech.supernaturalbloodlines.shared.hub.HubTheme;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ConfirmLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Counters;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Detail;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.DetailLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.EmblemData;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.ModelDrag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.SourceTag;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TickRing;

/**
 * The kit's widgets, drawn with a theme's own colours: the pure kit classes say where, this puts the pixels there. Client only; nothing
 * here picks a colour of its own except the focus ring, the pack roles and the model.
 */
public final class KitDraw {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, Icons.TEXTURE);
    private static final int ATLAS_W = Icons.COLUMNS * Icons.CELL;
    private static final int ATLAS_H = Icons.ROWS * Icons.CELL;
    private static final Component STAY = Component.translatable("supernaturalbloodlines.hub.confirm.stay");

    private KitDraw() {
    }

    /** An icon tinted {@code color} at {@code size} pixels (16 draws it 1:1; 8, 12 and 24 are clean multiples or halves). */
    public static void icon(GuiGraphics g, Icons icon, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        g.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >>> 24) == 0 ? 255 : (color >>> 24)) / 255f);
        g.blit(ATLAS, x, y, size, size, icon.column() * Icons.CELL, icon.row() * Icons.CELL, Icons.CELL, Icons.CELL, ATLAS_W, ATLAS_H);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** A one pixel line (Bresenham): ink strokes of a sigil, cords. */
    public static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /** A filled shape of {@code size} pixels at ({@code x}, {@code y}). */
    public static void shape(GuiGraphics g, Shapes.Shape shape, int x, int y, int size, int color) {
        if (size <= 0) {
            return;
        }
        // one blit of a baked white mask, tinted; a fill per row only if a texture cannot be made (no GL context)
        try {
            var mask = BakedTexture.get("shape/" + shape + "/" + size, size, size, (px, py) -> Shapes.contains(shape, size, px, py) ? 0xFFFFFFFF : 0);
            BakedTexture.blitTinted(g, mask, x, y, size, size, color);
        } catch (RuntimeException noTexture) {
            for (Shapes.Run run : Shapes.runs(shape, size)) {
                g.fill(x + run.x(), y + run.y(), x + run.x() + run.w(), y + run.y() + run.h(), color);
            }
        }
    }

    /** A shape with a 1 pixel border, and an icon in the middle (12 px inside 20 or less, 16 px above). */
    public static void medallion(GuiGraphics g, Shapes.Shape shape, Rect r, int fill, int border, Icons icon, int iconColor) {
        int size = Math.min(r.w(), r.h());
        int x = r.x() + (r.w() - size) / 2;
        int y = r.y() + (r.h() - size) / 2;
        shape(g, shape, x, y, size, border);
        shape(g, shape, x + 1, y + 1, size - 2, fill);
        if (icon != null) {
            int is = size <= 20 ? 12 : 16;
            icon(g, icon, x + (size - is) / 2, y + (size - is) / 2, is, iconColor);
        }
    }

    public static void tickRing(GuiGraphics g, int cx, int cy, int radius, int count, float fraction, int on, int off) {
        for (TickRing.Tick t : TickRing.ticks(cx, cy, radius, count, fraction)) {
            g.fill(t.x() - 1, t.y() - 1, t.x() + 1, t.y() + 1, t.on() ? on : off);
        }
    }

    /** Rank pips; the width they take. */
    public static int pips(GuiGraphics g, int x, int y, int total, int filled, int on, int off) {
        for (int i = 0; i < total; i++) {
            g.fill(x + i * 6, y, x + i * 6 + 4, y + 4, i < filled ? on : off);
        }
        return Math.max(0, total * 6 - 2);
    }

    /** A source tag chip (SB, VMP, WW, ENC, TEN) in the theme's colour for the mod; its width. */
    public static int sourceTag(GuiGraphics g, Font font, HubTheme theme, SourceTag tag, int x, int y) {
        int bg = theme.sourceColor(tag.source());
        int w = tagWidth(font, tag);
        g.fill(x, y, x + w, y + 10, bg);
        g.drawString(font, tag.label(), x + 3, y + 1, Contrast.bestText(bg), false);
        return w;
    }

    public static int tagWidth(Font font, SourceTag tag) {
        return font.width(tag.label()) + 5;
    }

    /** The keyboard focus ring, one pixel outside {@code r}, two pixels wide. */
    public static void focusRing(GuiGraphics g, Rect r, int color) {
        int x1 = r.x() - 2;
        int y1 = r.y() - 2;
        int x2 = r.right() + 2;
        int y2 = r.bottom() + 2;
        g.fill(x1, y1, x2, y1 + 2, color);
        g.fill(x1, y2 - 2, x2, y2, color);
        g.fill(x1, y1, x1 + 2, y2, color);
        g.fill(x2 - 2, y1, x2, y2, color);
    }

    /** A small square button with an icon: the theme switch, the close. */
    public static void button(GuiGraphics g, HubTheme theme, Rect r, Icons icon, boolean hot, boolean focused) {
        int bg = hot ? theme.accent() : theme.paper();
        int fg = hot ? theme.paper() : theme.accent();
        g.fill(r.x(), r.y(), r.right(), r.bottom(), theme.accent());
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, bg);
        int is = Math.min(12, Math.min(r.w(), r.h()) - 2);
        icon(g, icon, r.x() + (r.w() - is) / 2, r.y() + (r.h() - is) / 2, is, fg);
        if (focused) {
            focusRing(g, r, theme.focusColor());
        }
    }

    /**
     * The live player model: vanilla's own renderer, turned by {@link ModelDrag}. {@code r} is the box it fills; the model's feet sit at the
     * bottom of it.
     */
    public static void model(GuiGraphics g, Rect r, LivingEntity entity, ModelDrag drag) {
        int scale = Math.max(8, Math.round(r.h() * 0.43f));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, r.x(), r.y(), r.right(), r.bottom(), scale, 0.0625f,
                drag.lookX(r.x() + r.w() / 2f), r.y() + r.h() * 0.4f, entity);
    }

    /** The default emblem: a tick ring (the ladder's progress) round the live model. {@code r} is the square the ring fits. */
    public static void emblem(GuiGraphics g, HubTheme theme, Rect r, EmblemData data) {
        int cx = r.x() + r.w() / 2;
        int cy = r.y() + r.h() / 2;
        int radius = Math.min(r.w(), r.h()) / 2 - 2;
        tickRing(g, cx, cy, radius, 36, data.progress(), theme.accent(), theme.muted());
        int inner = (int) (radius * 1.35);
        model(g, new Rect(cx - inner / 2, cy - inner / 2 - 2, inner, inner), data.entity(), data.drag());
    }

    /** The default counters: SP over EP, right aligned in {@code r}. */
    public static void counters(GuiGraphics g, Font font, HubTheme theme, Rect r, Counters c) {
        String sp = c.spText();
        g.drawString(font, sp, r.right() - font.width(sp), r.y(), theme.accent(), false);
        String ep = c.epText();
        if (ep != null) {
            g.drawString(font, ep, r.right() - font.width(ep), r.y() + 10, theme.muted(), false);
        }
    }

    // ---- the detail ----

    /** The height the text of {@code d} takes at width {@code w}: a theme sizes its bar or pane from it. */
    public static int detailHeight(Font font, int w, Detail d) {
        return flow(null, font, null, new Rect(0, 0, w, 10_000), d) ;
    }

    /**
     * Registers the targets of {@code d}'s actions in {@code rects} (one rectangle per action, in order), with the tooltip, the confirm
     * routing and the focus ring. A theme draws the buttons itself in its own style and calls this for the behaviour.
     */
    public static void detailActions(UiContext ctx, HubTheme theme, List<Rect> rects, Detail d) {
        for (int i = 0; i < d.actions().size() && i < rects.size(); i++) {
            Detail.Action a = d.actions().get(i);
            Rect b = rects.get(i);
            String id = "detail:" + a.id();
            List<Component> tip = !a.enabled() && a.disabledReason() != null ? List.of(a.label(), a.disabledReason()) : List.of(a.label());
            Runnable run = !a.enabled() ? null : a.confirm() == null ? a.run()
                    : () -> ctx.confirm(a.confirm().title(), a.confirm().body(), STAY, a.label(), a.run());
            ctx.target(id, b, tip, run);
            if (ctx.focused(id)) {
                focusRing(ctx.g(), b, theme.focusColor());
            }
        }
    }

    /**
     * The default look of a detail in {@code r}: the text on top (clipped above the actions) and the actions stacked at the bottom. A
     * theme that styles its own calls {@link #detailHeight}, {@link DetailLayout} and {@link #detailActions}.
     */
    public static void detail(UiContext ctx, HubTheme theme, Rect r, Detail d) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        Rect text = DetailLayout.textArea(r, d.actions().size());
        g.enableScissor(text.x(), text.y(), text.right(), text.bottom());
        flow(g, font, theme, text, d);
        g.disableScissor();
        List<Rect> rects = DetailLayout.stack(r, d.actions().size());
        for (int i = 0; i < rects.size(); i++) {
            Detail.Action a = d.actions().get(i);
            Rect b = rects.get(i);
            boolean hot = a.enabled() && (ctx.hovered(b) || ctx.focused("detail:" + a.id()));
            int fill = a.primary() ? (a.enabled() ? theme.accent() : theme.muted()) : theme.paper();
            int label = a.primary() ? Contrast.bestText(fill) : a.enabled() ? theme.ink() : theme.muted();
            g.fill(b.x(), b.y(), b.right(), b.bottom(), a.enabled() ? theme.accent() : theme.muted());
            g.fill(b.x() + 1, b.y() + 1, b.right() - 1, b.bottom() - 1, hot && !a.primary() ? theme.good() : fill);
            g.drawString(font, a.label(), b.x() + (b.w() - font.width(a.label())) / 2, b.y() + 3,
                    hot && !a.primary() ? Contrast.bestText(theme.good()) : label, false);
        }
        detailActions(ctx, theme, rects, d);
    }

    /** Draws (when {@code g} is not null) or only measures the text of a detail; the y it ends at, from the top of {@code r}. */
    private static int flow(GuiGraphics g, Font font, HubTheme theme, Rect r, Detail d) {
        int y = r.y();
        if (d.kicker() != null) {
            if (g != null) {
                g.drawString(font, d.kicker(), r.x(), y, theme.muted(), false);
            }
            y += 10;
        }
        int x = r.x();
        if (d.icon() != null) {
            if (g != null) {
                icon(g, d.icon(), x, y - 1, 12, theme.accent());
            }
            x += 15;
        }
        int tagW = d.source() == null ? 0 : tagWidth(font, d.source());
        boolean tagBelow = d.source() != null && x + font.width(d.title()) + 4 + tagW > r.right();
        for (FormattedCharSequence part : font.split(d.title(), Math.max(10, r.right() - x))) {
            if (g != null) {
                g.drawString(font, part, x, y, theme.accent(), false);
            }
            y += 11;
        }
        if (d.source() != null) {
            if (tagBelow) {
                if (g != null) {
                    sourceTag(g, font, theme, d.source(), r.x(), y - 1);
                }
                y += 12;
            } else if (g != null) {
                int lastLineW = font.width(d.title());
                sourceTag(g, font, theme, d.source(), x + Math.min(lastLineW, r.right() - x) + 4, y - 12);
            }
        }
        if (d.subtitle() != null) {
            if (g != null) {
                g.drawString(font, d.subtitle(), r.x(), y, theme.muted(), false);
            }
            y += 10;
        }
        if (d.pips() > 0) {
            if (g != null) {
                pips(g, r.x(), y + 1, d.pips(), d.pipsFilled(), theme.accent(), theme.muted());
            }
            y += 9;
        }
        for (Component line : d.body()) {
            for (FormattedCharSequence part : font.split(line, r.w())) {
                if (g != null) {
                    g.drawString(font, part, r.x(), y, theme.ink(), false);
                }
                y += 9;
            }
            y += 2;
        }
        for (Detail.Row row : d.rows()) {
            int rx = r.x();
            if (row.source() != null) {
                if (g != null) {
                    sourceTag(g, font, theme, row.source(), rx, y - 1);
                }
                rx += tagWidth(font, row.source()) + 3;
            }
            for (FormattedCharSequence part : font.split(row.label(), Math.max(10, r.right() - rx))) {
                if (g != null) {
                    g.drawString(font, part, rx, y, row.complete() ? theme.good() : theme.ink(), false);
                }
                y += 9;
            }
            if (g != null && row.total() > 0) {
                int bw = r.w();
                g.fill(r.x(), y, r.x() + bw, y + 4, theme.muted());
                g.fill(r.x(), y, r.x() + (int) (bw * Math.min(1.0, (double) row.done() / row.total())), y + 4, theme.good());
            }
            y += 7;
        }
        for (Detail.Field f : d.fields()) {
            if (g != null) {
                g.drawString(font, f.label(), r.x(), y, theme.muted(), false);
            }
            y += 9;
            for (FormattedCharSequence part : font.split(f.value(), r.w())) {
                if (g != null) {
                    g.drawString(font, part, r.x(), y, theme.ink(), false);
                }
                y += 9;
            }
            y += 2;
        }
        if (d.reward() != null) {
            for (FormattedCharSequence part : font.split(d.reward(), r.w())) {
                if (g != null) {
                    g.drawString(font, part, r.x(), y, theme.accent(), false);
                }
                y += 9;
            }
        }
        return y - r.y();
    }

    // ---- the confirm dialog ----

    /** A confirm dialog over the whole screen: a dim, a panel with a title and wrapped text, the safe button (left) and the confirm button (right). */
    public static void confirm(UiContext ctx, HubTheme theme, int screenW, int screenH, Component title, Component body, Component safe, Component yes,
                               boolean confirmFocused, ConfirmLayout l) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        Rect panel = l.panel();
        g.fill(0, 0, screenW, screenH, 0xB0000000);
        g.fill(panel.x() - 1, panel.y() - 1, panel.right() + 1, panel.bottom() + 1, theme.accent());
        g.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), theme.paper());
        g.drawString(font, title, panel.x() + ConfirmLayout.PAD, panel.y() + ConfirmLayout.PAD, theme.accent(), false);
        Rect text = l.text();
        int y = text.y();
        int lines = 0;
        for (FormattedCharSequence part : font.split(body, text.w())) {
            if (++lines > l.maxLines()) {
                break;
            }
            g.drawString(font, part, text.x(), y, theme.ink(), false);
            y += ConfirmLayout.LINE;
        }
        dialogButton(g, font, theme, l.safe(), safe, false, !confirmFocused);
        dialogButton(g, font, theme, l.confirm(), yes, true, confirmFocused);
    }

    private static void dialogButton(GuiGraphics g, Font font, HubTheme theme, Rect r, Component label, boolean primary, boolean focused) {
        int fill = primary ? theme.accent() : theme.paper();
        g.fill(r.x(), r.y(), r.right(), r.bottom(), theme.accent());
        g.fill(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, fill);
        g.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + 4, primary ? Contrast.bestText(fill) : theme.ink(), false);
        if (focused) {
            focusRing(g, r, theme.focusColor());
        }
    }
}
