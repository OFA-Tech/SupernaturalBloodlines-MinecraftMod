package net.ofatech.supernaturalbloodlines.shared.hub.kit;

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

/**
 * The kit's widgets, drawn with a theme's own colours: the pure kit classes say where, this puts the pixels there. Client only; nothing
 * here picks a colour of its own except the focus ring, the pack roles and the model.
 */
public final class KitDraw {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, Icons.TEXTURE);
    private static final int ATLAS_W = Icons.COLUMNS * Icons.CELL;
    private static final int ATLAS_H = Icons.ROWS * Icons.CELL;

    private KitDraw() {
    }

    /** An icon tinted {@code color} at {@code size} pixels (16 draws it 1:1; 8, 12 and 24 are clean multiples or halves). */
    public static void icon(GuiGraphics g, Icons icon, int x, int y, int size, int color) {
        RenderSystem.enableBlend();
        g.setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >>> 24) == 0 ? 255 : (color >>> 24)) / 255f);
        g.blit(ATLAS, x, y, size, size, icon.column() * Icons.CELL, icon.row() * Icons.CELL, Icons.CELL, Icons.CELL, ATLAS_W, ATLAS_H);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** A filled shape of {@code size} pixels at ({@code x}, {@code y}). */
    public static void shape(GuiGraphics g, Shapes.Shape shape, int x, int y, int size, int color) {
        Shapes.Span[] rows = Shapes.spans(shape, size);
        for (int row = 0; row < rows.length; row++) {
            g.fill(x + rows[row].x(), y + row, x + rows[row].x() + rows[row].w(), y + row + 1, color);
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
        int w = font.width(tag.label()) + 5;
        g.fill(x, y, x + w, y + 10, bg);
        g.drawString(font, tag.label(), x + 3, y + 1, Contrast.bestText(bg), false);
        return w;
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

    /** The emblem: a tick ring (the ladder's progress) round the live model. {@code r} is the square the ring fits. */
    public static void emblem(GuiGraphics g, HubTheme theme, Rect r, float progress, LivingEntity entity, ModelDrag drag) {
        int cx = r.x() + r.w() / 2;
        int cy = r.y() + r.h() / 2;
        int radius = Math.min(r.w(), r.h()) / 2 - 2;
        tickRing(g, cx, cy, radius, 36, progress, theme.accent(), theme.muted());
        int inner = (int) (radius * 1.35);
        model(g, new Rect(cx - inner / 2, cy - inner / 2 - 2, inner, inner), entity, drag);
    }

    /** The shared detail: title, source tag, pips, wrapped text, fields, reward, and the actions stacked at the bottom of {@code r}. */
    public static void detail(UiContext ctx, HubTheme theme, Rect r, Detail d) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        g.enableScissor(r.x(), r.y(), r.right(), r.bottom());
        int y = r.y();
        g.drawString(font, d.title(), r.x(), y, theme.accent(), false);
        int x = r.x() + font.width(d.title()) + 4;
        if (d.source() != null) {
            sourceTag(g, font, theme, d.source(), Math.min(x, r.right() - 28), y - 1);
        }
        y += 11;
        if (d.subtitle() != null) {
            g.drawString(font, d.subtitle(), r.x(), y, theme.muted(), false);
            y += 10;
        }
        if (d.pips() > 0) {
            pips(g, r.x(), y + 1, d.pips(), d.pipsFilled(), theme.accent(), theme.muted());
            y += 9;
        }
        for (Component line : d.body()) {
            for (FormattedCharSequence part : font.split(line, r.w())) {
                g.drawString(font, part, r.x(), y, theme.ink(), false);
                y += 9;
            }
            y += 2;
        }
        for (Detail.Field f : d.fields()) {
            g.drawString(font, f.label(), r.x(), y, theme.muted(), false);
            y += 9;
            for (FormattedCharSequence part : font.split(f.value(), r.w())) {
                g.drawString(font, part, r.x(), y, theme.ink(), false);
                y += 9;
            }
            y += 2;
        }
        if (d.reward() != null) {
            for (FormattedCharSequence part : font.split(d.reward(), r.w())) {
                g.drawString(font, part, r.x(), y, theme.accent(), false);
                y += 9;
            }
        }
        g.disableScissor();
        int by = r.bottom();
        for (int i = d.actions().size() - 1; i >= 0; i--) {
            Detail.Action a = d.actions().get(i);
            by -= 16;
            Rect b = new Rect(r.x(), by, r.w(), 14);
            String id = "detail:" + a.id();
            boolean hot = a.enabled() && (ctx.hovered(b) || ctx.focused(id));
            int fill = a.primary() ? (a.enabled() ? theme.accent() : theme.muted()) : theme.paper();
            int text = a.primary() ? Contrast.bestText(fill) : a.enabled() ? theme.ink() : theme.muted();
            g.fill(b.x(), b.y(), b.right(), b.bottom(), a.enabled() ? theme.accent() : theme.muted());
            g.fill(b.x() + 1, b.y() + 1, b.right() - 1, b.bottom() - 1, hot && !a.primary() ? theme.good() : fill);
            g.drawString(font, a.label(), b.x() + (b.w() - font.width(a.label())) / 2, b.y() + 3, hot && !a.primary() ? Contrast.bestText(theme.good()) : text, false);
            ctx.target(id, b, List.of(a.label()), a.enabled() ? a.run() : null);
            if (ctx.focused(id)) {
                focusRing(g, b, theme.focusColor());
            }
        }
    }

    /** A confirm dialog over the whole screen: a dim, a panel with a title and wrapped text, Cancel (the safe one) and Confirm. */
    public static void confirm(UiContext ctx, HubTheme theme, int screenW, int screenH, Component title, Component body, Component cancel, Component yes,
                               boolean confirmFocused, Rect cancelRect, Rect confirmRect, Rect panel, Rect text) {
        GuiGraphics g = ctx.g();
        Font font = ctx.font();
        g.fill(0, 0, screenW, screenH, 0xB0000000);
        g.fill(panel.x() - 1, panel.y() - 1, panel.right() + 1, panel.bottom() + 1, theme.accent());
        g.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), theme.paper());
        g.drawString(font, title, panel.x() + ConfirmLayout.PAD, panel.y() + ConfirmLayout.PAD, theme.accent(), false);
        int y = text.y();
        for (FormattedCharSequence part : font.split(body, text.w())) {
            if (y + 9 > text.bottom()) {
                break;
            }
            g.drawString(font, part, text.x(), y, theme.ink(), false);
            y += 9;
        }
        dialogButton(g, font, theme, cancelRect, cancel, false, !confirmFocused);
        dialogButton(g, font, theme, confirmRect, yes, true, confirmFocused);
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
