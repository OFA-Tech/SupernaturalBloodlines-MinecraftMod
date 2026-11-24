package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.Annulus;
import net.ofatech.supernaturalbloodlines.core.wheel.Annulus.Span;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPicker;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Cell;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Layout;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelLayout.Ring;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelModel;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPalette;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudKeys;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelPanelText;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.TextCache;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;

/**
 * Draws the wheel as the approved W3 mockups show it: the three ring bands (filled spans of {@link Annulus}, a hexagon in the Tensura look), the cells, chips, panel and
 * glow from the art kit, the text from the palette. State is never colour alone: on is a halo and the ON pill, a cooldown a sweep and its seconds, locked a grey icon, a
 * dashed outline, a lock and the reason, hover corner brackets, the open path a caret and a lit label. Client only; draws what {@link Frame} holds and keeps nothing.
 */
final class WheelRenderer {
    /** Everything one frame needs. {@code power} is the entry under the pointer or focus, {@code slots} the picker's slots when it is open. */
    record Frame(Layout layout, WheelModel<WheelEntry> model, WheelModel.Open open, List<WheelEntry> page, WheelLayout.Hit hover, WheelEntry power, boolean alt, WheelPalette pal,
                 HudKit kit, long gameTime, Picker picker, java.util.function.Function<WheelEntry, String> slotOf) {
    }

    record Picker(WheelEntry entry, Cell anchor, List<SlotPicker.Slot> slots, List<WheelEntry> holds, int hoverSlot, boolean none) {
    }

    private record SpanKey(int sides, int r0, int r1, int a0, int a1) {
    }

    private static final Map<SpanKey, List<Span>> SPANS = new HashMap<>();
    private WheelRenderer() {
    }

    private static List<Span> spans(Annulus.Shape shape, double r0, double r1, double a0, double a1) {
        if (SPANS.size() > 512) {
            SPANS.clear();
        }
        SpanKey key = new SpanKey(shape.sides(), (int) Math.round(r0 * 10), (int) Math.round(r1 * 10), (int) Math.round(a0 * 10), (int) Math.round(a1 * 10));
        return SPANS.computeIfAbsent(key, k -> Annulus.wedge(shape, r0, r1, a0, a1));
    }
    private static void fill(GuiGraphics g, List<Span> spans, int cx, int cy, int color) {
        for (Span s : spans) {
            g.fill(cx + s.x0(), cy + s.y(), cx + s.x1() + 1, cy + s.y() + 1, color);
        }
    }

    static String text(String key, Object... args) {
        return WheelStrings.text(key, args);
    }

    private static int w(Font font, String s) {
        return WheelPanelText.width(font, s, font::width);
    }

    private static String cut(Font font, String s, int width) {
        return TextCache.cut(font, s, width, font::width);
    }

    private static List<String> wrap(Font font, String s, int width, int lines) {
        return TextCache.wrap(font, s, width, lines, font::width);
    }
    static void draw(GuiGraphics g, Font font, Frame f) {
        Layout l = f.layout();
        WheelPalette pal = f.pal();
        int cx = l.centreX();
        int cy = l.centreY();
        var shape = l.shape(Ring.INNER);
        var outerShape = l.shape(Ring.OUTER);
        // the bands
        fill(g, spans(outerShape, 88, 126, 0, 360), cx, cy, pal.outerBand());
        fill(g, spans(shape, 19, 49, 0, 360), cx, cy, pal.innerBand());
        List<Cell> inner = l.ring(Ring.INNER);
        List<Cell> middle = l.ring(Ring.MIDDLE);
        List<Cell> outer = l.ring(Ring.OUTER);
        if (!middle.isEmpty()) {
            fill(g, spans(shape, 52, 86, 0, 360), cx, cy, pal.middleTrack());
        }
        // the lit wedges of the open path and the hovered power
        int open = Math.max(0, Math.min(f.open().mod(), inner.size() - 1));
        if (inner.size() >= 2) {
            double half = 180.0 / inner.size();
            double b = l.bearing(Ring.INNER, open);
            fill(g, spans(shape, 19, 49, b - half, b + half), cx, cy, pal.innerLit());
        }
        int openGroup = Math.max(0, Math.min(f.open().group(), middle.size() - 1));
        if (!middle.isEmpty()) {
            double b = l.bearing(Ring.MIDDLE, openGroup);
            fill(g, spans(shape, 52, 86, b - WheelLayout.FAN_STEP / 2, b + WheelLayout.FAN_STEP / 2), cx, cy, pal.middleLit());
        }
        if (f.hover() != null && f.hover().ring() == Ring.OUTER && f.hover().index() < outer.size()) {
            double b = l.bearing(Ring.OUTER, f.hover().index());
            double half = l.outerSegment() / 2;
            fill(g, spans(outerShape, 88, 126, b - half, b + half), cx, cy, pal.outerLit());
        }
        // the dividers, the rim and the hub
        for (int i = 0; i <= outer.size(); i++) {
            double b = Math.toRadians(l.outerStart() + l.outerSegment() * i);
            KitDraw.line(g, cx + (int) Math.round(88 * Math.sin(b)), cy - (int) Math.round(88 * Math.cos(b)), cx + (int) Math.round(126 * Math.sin(b)),
                    cy - (int) Math.round(126 * Math.cos(b)), pal.line());
            if (f.kit() != HudKit.BLOOD_MOON) {
                int edge = f.kit() == HudKit.GRIMOIRE ? 3 : 2;
                UiKit.draw(g, f.kit(), Part.WHEEL_EDGE, cx + (int) Math.round(126 * Math.sin(b)) - edge / 2, cy - (int) Math.round(126 * Math.cos(b)) - edge / 2, edge, edge);
            }
        }
        fill(g, spans(outerShape, 125, 126.5, 0, 360), cx, cy, pal.rim());
        fill(g, spans(outerShape, 87, 88, 0, 360), cx, cy, pal.line());
        fill(g, spans(shape, 51, 52, 0, 360), cx, cy, pal.innerLine());
        fill(g, spans(shape, 0, 17, 0, 360), cx, cy, pal.hub());
        fill(g, spans(shape, 16, 17, 0, 360), cx, cy, pal.innerLine());
        fill(g, spans(shape, 0, 9, 0, 360), cx, cy, pal.seal());
        String glyph = WheelStrings.key(HudKeys.WHEEL);
        g.drawString(font, glyph, cx - w(font, glyph) / 2, cy - 4, pal.hintInk(), false);
        // the carets of the open path
        if (!inner.isEmpty()) {
            caret(g, f, cx, cy, l.bearing(Ring.INNER, open), 50);
        }
        if (!middle.isEmpty()) {
            caret(g, f, cx, cy, l.bearing(Ring.MIDDLE, openGroup), 87);
        }
        innerCells(g, font, f, inner, open);
        middleCells(g, font, f, middle, openGroup);
        outerCells(g, font, f, outer);
        pageControls(g, font, f);
        panel(g, font, f);
        hints(g, font, f);
        if (f.alt()) {
            modeLists(g, font, f);
        }
        if (f.picker() != null) {
            picker(g, font, f);
        }
    }

    private static void caret(GuiGraphics g, Frame f, int cx, int cy, double bearing, int r) {
        double a = Math.toRadians(bearing);
        int x = cx + (int) Math.round(r * Math.sin(a));
        int y = cy - (int) Math.round(r * Math.cos(a));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) bearing));
        UiKit.draw(g, f.kit(), Part.WHEEL_CARET, -3, -2, 7, 5);
        g.pose().popPose();
    }

    static String iconName(PowerSource s) {
        return switch (s) {
            case BLOODLINE -> "bloodline";
            case VAMPIRISM -> "tag_vmp";
            case WEREWOLVES -> "tag_ww";
            case TENSURA -> "tag_ten";
            case INFINITY -> "infinity_stones";
            case GREAT_SAGE -> "great_sage";
        };
    }

    static String modName(PowerSource s) {
        return text("mod." + s.name().toLowerCase(Locale.ROOT));
    }

    private static void innerCells(GuiGraphics g, Font font, Frame f, List<Cell> inner, int open) {
        for (Cell c : inner) {
            boolean on = c.index() == open;
            UiKit.draw(g, f.kit(), on ? Part.SLOT_ACTIVE : Part.SLOT, c.x() + 3, c.y() + 3, 22, 22);
            UiKit.icon(g, iconName(f.model().mods().get(c.index())), c.x() + 6, c.y() + 6, 16, 0);
            if (f.hover() != null && f.hover().ring() == Ring.INNER && f.hover().index() == c.index()) {
                corners(g, c.x() - 2, c.y() - 2, c.w() + 4, c.h() + 4, 4, bracket(f));
            }
        }
    }

    private static void middleCells(GuiGraphics g, Font font, Frame f, List<Cell> middle, int open) {
        if (middle.isEmpty()) {
            return;
        }
        var groups = f.model().groups(f.model().mods().get(Math.max(0, Math.min(f.open().mod(), f.model().mods().size() - 1))));
        for (Cell c : middle) {
            boolean on = c.index() == open;
            UiKit.draw(g, f.kit(), on ? Part.CHIP_ON : Part.CHIP, c.x(), c.y(), c.w(), c.h());
            String label = cut(font, text("group." + groups.get(c.index()).name().toLowerCase(Locale.ROOT)), c.w() - 4);
            g.drawString(font, label, c.x() + (c.w() - w(font, label)) / 2, c.y() + 3, on ? f.pal().onPill() : f.pal().ink(), false);
        }
    }

    private static void outerCells(GuiGraphics g, Font font, Frame f, List<Cell> outer) {
        for (Cell c : outer) {
            if (c.index() >= f.page().size()) {
                continue;
            }
            WheelEntry e = f.page().get(c.index());
            boolean on = e.state().on();
            boolean locked = e.state().locked();
            int left = e.state().cooldownLeft(f.gameTime());
            boolean cooling = left > 0;
            if (on) {
                UiKit.draw(g, f.kit(), Part.CELL_GLOW_52, c.x(), c.y(), 36, 36);
            }
            UiKit.draw(g, f.kit(), on ? Part.WHEEL_CELL_36_ACTIVE : Part.WHEEL_CELL_36, c.x(), c.y(), 36, 36);
            ClientIcons.draw(g, e.icon(), c.x() + 2, c.y() + 2, 32, locked || cooling ? 0.45f : 1f);
            if (cooling) {
                int frame = Math.max(1, Math.min(32, Math.round(32f * left / Math.max(1, e.state().cooldownTotal()))));
                UiKit.frame(g, KitSprites.WHEEL_SWEEP, frame, 32, 33, c.x() + 2, c.y() + 2, 1f);
            }
            if (locked) {
                UiKit.shared(g, KitSprites.WHEEL_LOCK_OUTLINE, c.x() - 3, c.y() - 3, 42, 42, 1f);
                KitDraw.icon(g, Icons.LOCK, c.x() + 21, c.y() + 21, 14, 0xFFFFFFFF); // the sprite keeps its own gold: WheelContrastTest checks it on every cell
            }
            if (on) {
                String t = text("on");
                int w = w(font, t) + 6;
                UiKit.draw(g, f.kit(), Part.CHIP_ON, c.x() + 36 - w + 2, c.y() - 7, w, 11);
                g.drawString(font, t, c.x() + 36 - w + 5, c.y() - 4, f.pal().onPill(), false);
            }
            if (cooling) {
                String t = (left + 19) / 20 + " s";
                int w = w(font, t) + 8;
                UiKit.draw(g, f.kit(), Part.CHIP, c.x() + 18 - w / 2, c.y() + 37, w, 11);
                g.drawString(font, t, c.x() + 18 - w(font, t) / 2, c.y() + 40, f.pal().ink(), false);
            } else if (e.modeNames().size() > 1) {
                String t = cut(font, WheelStrings.of(e.modeName()), 52);
                int w = w(font, t) + 18;
                UiKit.draw(g, f.kit(), Part.CHIP, c.x() + 18 - w / 2, c.y() + 37, w, 11);
                UiKit.shared(g, KitSprites.MODE_MARK, c.x() + 18 - w / 2 + 5, c.y() + 40, 7, 7, 1f);
                g.drawString(font, t, c.x() + 18 - w / 2 + 14, c.y() + 40, f.pal().ink(), false);
            }
            if (f.hover() != null && f.hover().ring() == Ring.OUTER && f.hover().index() == c.index()) {
                corners(g, c.x() - 4, c.y() - 4, 44, 44, 6, bracket(f));
            }
        }
    }

    /** Four corner brackets round a rectangle. */
    private static int bracket(Frame f) {
        return f.kit() == HudKit.GRIMOIRE ? f.pal().line() : f.kit().accent();
    }

    private static void corners(GuiGraphics g, int x, int y, int w, int h, int arm, int color) {
        g.fill(x, y, x + arm, y + 2, color);
        g.fill(x, y, x + 2, y + arm, color);
        g.fill(x + w - arm, y, x + w, y + 2, color);
        g.fill(x + w - 2, y, x + w, y + arm, color);
        g.fill(x, y + h - 2, x + arm, y + h, color);
        g.fill(x, y + h - arm, x + 2, y + h, color);
        g.fill(x + w - arm, y + h - 2, x + w, y + h, color);
        g.fill(x + w - 2, y + h - arm, x + w, y + h, color);
    }

    private static void pageControls(GuiGraphics g, Font font, Frame f) {
        Layout l = f.layout();
        List<Cell> arrows = l.pageArrows();
        if (arrows.isEmpty()) {
            return;
        }
        for (Cell a : arrows) {
            UiKit.draw(g, f.kit(), Part.BUTTON_NORMAL, a.x(), a.y(), a.w(), a.h());
            UiKit.icon(g, a.index() == 0 ? "arrow_left" : "arrow_right", a.x() + 3, a.y() + 2, 16, 0);
        }
        for (Cell d : l.pageDots()) {
            UiKit.draw(g, f.kit(), d.index() == l.page() ? Part.PIP_ON : Part.PIP_OFF, d.x(), d.y(), 5, 5);
        }
        String t = (l.page() + 1) + "/" + l.pageCount();
        Cell lab = l.pageLabel();
        g.drawString(font, t, lab.x() + (lab.w() - w(font, t)) / 2, lab.y(), f.pal().ink(), false);
    }

    private static void panel(GuiGraphics g, Font font, Frame f) {
        Cell p = f.layout().sidePanel();
        WheelPalette pal = f.pal();
        UiKit.draw(g, f.kit(), Part.PANEL, p.x(), p.y(), p.w(), p.h());
        int x = p.x() + 9;
        int w = p.w() - 18;
        int y = p.y() + 9;
        int rows = p.y() + p.h() - 9;
        WheelEntry e = f.power();
        var mods = f.model().mods();
        PowerSource source = e != null ? e.ref().source() : mods.isEmpty() ? null : mods.get(Math.max(0, Math.min(f.open().mod(), mods.size() - 1)));
        if (e == null) {
            String head = source == null ? text("hint.open") : modName(source);
            g.drawString(font, WheelStrings.bold(cut(font, head, w)), x, y, pal.ink(), false);
            helpLines(g, font, f, x, rows - 4 * 10);
            return;
        }
        boolean modes = e.modeNames().size() > 1;
        String where = modName(source) + (e.group() == net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup.MAIN || f.model().groups(source).isEmpty() ? ""
                : " · " + text("group." + e.group().name().toLowerCase(Locale.ROOT)));
        WheelPanelText.Lines t = WheelPanelText.lines(font, font::width, w, p.y() + 49, rows - 6 * 10, WheelStrings.of(e.name()), where, WheelStrings.of(e.coverNote()), text("kind." + e.kind().name().toLowerCase(Locale.ROOT)),
                WheelDescriptions.of(e), modes ? text("panel.mode", e.state().mode() + 1, e.modeNames().size(), WheelStrings.of(e.modeName())) : "",
                e.state().locked() ? reason(e.state().lock()) : "");
        g.drawString(font, WheelStrings.bold(t.name()), x, y, pal.ink(), false);
        y += 11;
        g.drawString(font, t.where(), x, y, pal.muted(), false);
        y += 12;
        for (String line : t.cover()) {
            g.drawString(font, line, x, y - 2, pal.muted(), false); // what else this power stands for (rule C), under its name
            y += 10;
        }
        UiKit.draw(g, f.kit(), Part.DIVIDER, x, y, w, 3);
        y += 6;
        g.drawString(font, WheelStrings.bold(t.kind()), x, y, pal.ink(), false);
        y += 11;
        for (String line : t.description()) {
            g.drawString(font, line, x, y, pal.ink(), false);
            y += 10;
        }
        if (!t.description().isEmpty()) {
            y += 2;
        }
        for (String line : t.mode()) {
            g.drawString(font, line, x, y, pal.muted(), false);
            y += 10;
        }
        if (e.state().locked()) {
            int bh = WheelPanelText.lockBoxHeight(t.lock().size());
            UiKit.draw(g, f.kit(), KitSprites.CARD, x - 2, y, w + 4, bh);
            KitDraw.icon(g, Icons.LOCK, x + 2, y + 12, 14, pal.warn());
            g.drawString(font, WheelStrings.bold(text("locked")), x + 20, y + 4, pal.warn(), false);
            int ly = y + 14;
            for (String line : t.lock()) {
                g.drawString(font, line, x + 20, ly, pal.ink(), false);
                ly += 10;
            }
        }
        // the rows: where it is slotted and how well it is known
        g.drawString(font, text("panel.slot"), x, rows - 6 * 10, pal.muted(), false);
        g.drawString(font, slotText(e, f), x + 46, rows - 6 * 10, pal.ink(), false);
        g.drawString(font, text("panel.mastery"), x, rows - 5 * 10, pal.muted(), false);
        if (e.mastery() >= 0f) {
            int bw = w - 46 - 28;
            UiKit.draw(g, f.kit(), Part.MASTERY_TRACK, x + 46, rows - 5 * 10 + 1, bw, 6);
            UiKit.draw(g, f.kit(), e.mastery() >= 1f ? Part.MASTERY_FILL_MASTERED : Part.MASTERY_FILL, x + 46, rows - 5 * 10 + 1, Math.max(1, Math.round(bw * Math.min(1f, e.mastery()))), 6);
            String pct = Math.round(Math.min(1f, e.mastery()) * 100) + "%";
            g.drawString(font, pct, x + w - w(font, pct), rows - 5 * 10, pal.ink(), false);
        } else {
            g.drawString(font, "-", x + 46, rows - 5 * 10, pal.ink(), false);
        }
        helpLines(g, font, f, x, rows - 4 * 10);
    }
    private static String slotText(WheelEntry e, Frame f) {
        String s = f.slotOf().apply(e);
        return s == null || s.isEmpty() ? text("panel.slot_none") : s;
    }

    static String reason(LockReason r) {
        return text("lock." + r.name().toLowerCase(Locale.ROOT));
    }

    private static void helpLines(GuiGraphics g, Font font, Frame f, int x, int y) {
        boolean paged = f.page().size() > 0 && f.layout().pageCount() > 1;
        // a stone power with a second option takes the closing row's place (G and Esc are on the G hint already)
        boolean dual = f.power() != null && net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity.InfinityCompat.isDual(f.power());
        List<WheelPanelText.HelpRow> rows = WheelPanelText.help(font, s -> font.width(WheelStrings.bold(s)),
                List.of(text("help.click"), text("help.right"), paged ? text("help.scroll") : text("help.alt"), dual ? text("help.dual_key") : text("help.close_keys")),
                List.of(text("help.click_use"), text("help.right_slot"), paged ? text("help.scroll_page") : text("help.alt_mode"), dual ? text("help.dual_what") : text("help.close")));
        for (WheelPanelText.HelpRow row : rows) {
            g.drawString(font, WheelStrings.bold(row.key()), x, y, f.pal().ink(), false);
            g.drawString(font, row.what(), x + row.whatX(), y, f.pal().muted(), false);
            y += 10;
        }
    }
    private static void hints(GuiGraphics g, Font font, Frame f) {
        Cell h = f.layout().gHint();
        chip(g, font, f, h, WheelStrings.key(HudKeys.WHEEL), text("hint.open"));
        if (f.alt()) {
            chip(g, font, f, f.layout().altHint(), "Alt", text("hint.alt"));
        }
    }

    private static void chip(GuiGraphics g, Font font, Frame f, Cell c, String key, String label) {
        UiKit.draw(g, f.kit(), Part.HUD_PILL, c.x(), c.y(), c.w(), c.h());
        int ink = UiKit.labelOver(f.kit(), Part.HUD_PILL, f.pal().hintInk());
        g.drawString(font, key, c.x() + 8, c.y() + 3, ink, false);
        g.drawString(font, label, c.x() + 8 + w(font, key) + 6, c.y() + 3, ink, false);
    }

    private static void modeLists(GuiGraphics g, Font font, Frame f) {
        List<Cell> outer = f.layout().ring(Ring.OUTER);
        for (Cell c : outer) {
            if (c.index() >= f.page().size() || f.page().get(c.index()).modeNames().size() < 2) {
                continue;
            }
            WheelEntry e = f.page().get(c.index());
            Cell a = f.layout().modeListAnchor(c.index());
            UiKit.draw(g, f.kit(), Part.TOOLTIP, a.x(), a.y(), a.w(), a.h());
            int ink = UiKit.labelOver(f.kit(), Part.TOOLTIP, f.pal().hintInk());
            // a power with many modes (a stone has up to a few dozen) shows the three round the current one
            int first = Math.max(0, Math.min(e.state().mode() - 1, e.modeNames().size() - 3));
            for (int i = first; i < Math.min(first + 3, e.modeNames().size()); i++) {
                boolean current = i == e.state().mode();
                if (current) {
                    UiKit.shared(g, KitSprites.MODE_MARK, a.x() + 5, a.y() + 7 + (i - first) * 12, 7, 7, 1f);
                }
                g.drawString(font, cut(font, WheelStrings.of(e.modeNames().get(i)), a.w() - 20), a.x() + 15, a.y() + 6 + (i - first) * 12, ink, false);
            }
        }
    }

    private static void picker(GuiGraphics g, Font font, Frame f) {
        Picker p = f.picker();
        Cell a = p.anchor();
        UiKit.draw(g, f.kit(), Part.TOOLTIP, a.x(), a.y(), a.w(), a.h());
        int ink = UiKit.labelOver(f.kit(), Part.TOOLTIP, f.pal().hintInk());
        g.drawString(font, cut(font, text("picker.title", WheelStrings.of(p.entry().name())), a.w() - 12), a.x() + 8, a.y() + 5, ink, false);
        if (p.none()) {
            g.drawString(font, text("picker.none"), a.x() + 8, a.y() + 30, ink, false);
            return;
        }
        for (int i = 0; i < p.slots().size(); i++) {
            int sx = a.x() + 8 + i * 31;
            int sy = a.y() + 17;
            UiKit.draw(g, f.kit(), i == p.hoverSlot() ? Part.SLOT_ACTIVE : Part.SLOT, sx + 3, sy + 3, 22, 22);
            WheelEntry held = p.holds().get(i);
            if (held != null) {
                ClientIcons.draw(g, held.icon(), sx + 6, sy + 6, 16, 1f);
            } else {
                UiKit.icon(g, "plus", sx + 6, sy + 6, 16, 0x80FFFFFF);
            }
            String key = WheelStrings.key(HudKeys.SLOTS[i]);
            UiKit.draw(g, f.kit(), Part.KEY_BADGE, sx + 17, sy + 17, 11, 11);
            g.drawString(font, key, sx + 17 + (11 - w(font, key)) / 2, sy + 19, UiKit.labelOver(f.kit(), Part.KEY_BADGE, f.pal().onPill()), false);
        }
        String bottom = p.hoverSlot() < 0 ? "" : p.holds().get(p.hoverSlot()) == null ? text("picker.empty") : text("picker.replaces", WheelStrings.of(p.holds().get(p.hoverSlot()).name()));
        g.drawString(font, cut(font, bottom, a.w() - 80), a.x() + 8, a.y() + a.h() - 12, ink, false);
        String esc = text("picker.cancel");
        g.drawString(font, esc, a.x() + a.w() - 8 - w(font, esc), a.y() + a.h() - 12, ink, false);
    }

    /** The aim hint under the crosshair and the power's icon under it (the wheel is closed). */
    static void aimHint(GuiGraphics g, Font font, HudKit kit, WheelPalette pal, ResourceLocation icon, int screenW, int screenH) {
        String t = text("aim.hint");
        int w = Math.min(screenW - 8, 244);
        int x = screenW / 2 - w / 2;
        int y = screenH * 62 / 100;
        UiKit.draw(g, kit, Part.HUD_LABEL, x, y, w, 14);
        g.drawString(font, cut(font, t, w - 8), x + (w - w(font, cut(font, t, w - 8))) / 2, y + 3, UiKit.labelOver(kit, Part.HUD_LABEL, pal.hintInk()), false);
        UiKit.draw(g, kit, Part.SLOT, x + w / 2 - 11 + 0, y + 18 + 3, 22, 22);
        ClientIcons.draw(g, icon, x + w / 2 - 8, y + 18 + 6, 16, 1f);
        corners(g, screenW / 2 - 18, screenH / 2 - 18, 36, 36, 6, pal.innerLine());
    }
}
