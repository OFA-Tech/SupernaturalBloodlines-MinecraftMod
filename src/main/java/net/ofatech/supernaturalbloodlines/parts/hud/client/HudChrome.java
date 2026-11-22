package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.MoonClock;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudFaces;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * What stands round the slots: the mode pill ("SUPERNATURAL . Caps") above the hotbar, the panel that shows the other mode's slots, the full moon warning
 * ("Forced shift in 1:40") in the top right, and the tracked task's line (top centre, under the boss bars, by default). Each is drawn from the kit's
 * pictures (the pill, the label box) in the look of the hub's theme.
 */
final class HudChrome {
    private static final String MODE_MARK = "kit/shared/mode_mark";
    private static final String BLOOD_MARK = "kit/shared/blood_full";
    /** The moon warning's box: a 16 px icon and the kit label's 4 px border round it. */
    static final int MOON_BOX = 24;

    private HudChrome() {
    }

    // The texts below are worked out once a tick, not once a frame: translating, measuring and wrapping them is the cost, and they only change with the game.
    private static String pillText = "";
    private static int pillWidth;
    private static SlotMode pillMode;
    private static long pillTick = Long.MIN_VALUE;

    private static final SlotLabel.Label[] LABELS = new SlotLabel.Label[6];
    private static long labelsTick = Long.MIN_VALUE;
    private static int labelsWidth = -1;
    private static HudStyle.Style labelsStyle;

    private record WrapKey(String text, int width, int lines) {
    }

    private static final java.util.Map<WrapKey, java.util.List<String>> WRAPS = new java.util.HashMap<>();
    private static long wrapsTick = Long.MIN_VALUE;

    /** {@link LabelFit#wrap} kept for the rest of the tick, keyed by (text, width, lines): the same pill is drawn every frame of a tick. */
    private static java.util.List<String> wrap(Font font, String text, int width, int maxLines) {
        long now = tick();
        if (now != wrapsTick) {
            WRAPS.clear();
            wrapsTick = now;
        }
        return WRAPS.computeIfAbsent(new WrapKey(text, width, maxLines), k -> LabelFit.wrap(text, width, maxLines, font::width));
    }

    private static long tick() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    /** The kit's label box (the Alt labels, the other mode's panel, the tracker). */
    static void labelBox(GuiGraphics g, HudKit kit, int x, int y, int w, int h, float alpha) {
        UiKit.draw(g, kit, Part.HUD_LABEL, x, y, w, h, alpha);
    }

    /** "SUPERNATURAL . Caps" centred over the hotbar. */
    static void pill(GuiGraphics g, Font font, HudStyle.Style style, SlotMode mode, int screenW, int screenH, float alpha, boolean lifted) {
        if (pillMode != mode || pillTick != tick()) {
            String key = HudKeys.HUD_MODE.getTranslatedKeyMessage().getString();
            String name = Component.translatable("supernaturalbloodlines.hud.pill." + mode.name().toLowerCase(Locale.ROOT)).getString();
            pillText = name + "  \u00b7  " + key;
            pillWidth = font.width(pillText);
            pillMode = mode;
            pillTick = tick();
        }
        HudKit kit = HudKit.of(style);
        int pillH = HudLayout.pillHeight(net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look.valueOf(style.name()));
        HudLayout.Box box = HudLayout.pill(screenW, screenH, pillWidth, lifted, pillH);
        int x = box.x();
        int y = box.y();
        String text = pillWidth + HudLayout.PILL_EXTRA > box.w() ? font.plainSubstrByWidth(pillText, box.w() - HudLayout.PILL_EXTRA) : pillText;
        UiKit.draw(g, kit, Part.HUD_PILL, x, y, box.w(), box.h(), alpha);
        // the mark of the mode: Tensura's teal diamond, or a drop of blood
        if (mode == SlotMode.TENSURA) {
            UiKit.shared(g, MODE_MARK, x + 8, y + (box.h() - 7) / 2, 7, 7, alpha);
        } else {
            UiKit.shared(g, BLOOD_MARK, x + 7, y + (box.h() - 9) / 2, 9, 9, alpha);
        }
        // 7 rows of capitals in the dark rows of the picture: 3 from the top of the 14 px pills, 4 in the 16 px one
        g.drawString(font, text, x + 18, y + (box.h() - 8) / 2 + 1, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_PILL, HudFaces.pillText(kit)), alpha), false);
    }

    /** The preview panel's width: six slots with their gaps and a little air; it stays in the right half of the screen even at 480 GUI pixels. */
    static final int PREVIEW_W = HudLayout.PREVIEW_W;

    /**
     * The panel with the other mode's six slots, in the theme's shape: "Caps Lock -> TENSURA". The head wraps to two lines when it has to.
     *
     * @return the panel's top edge, for the tooltips to stack above it
     */
    static int preview(GuiGraphics g, Font font, HudStyle.Style style, SlotMode other, SlotView[] views, HudLayout.Box[] boxes, float alpha) {
        HudKit kit = HudKit.of(style);
        String key = HudKeys.HUD_MODE.getTranslatedKeyMessage().getString();
        String name = Component.translatable("supernaturalbloodlines.hud.pill." + other.name().toLowerCase(Locale.ROOT)).getString();
        java.util.List<String> head = wrap(font, Component.translatable("supernaturalbloodlines.hud.pill.next", key, name).getString(), PREVIEW_W - 12, 2);
        HudLayout.Box panel = HudLayout.preview(g.guiWidth(), g.guiHeight(), head.size());
        int h = panel.h();
        int x = panel.x();
        int y = panel.y();
        labelBox(g, kit, x, y, PREVIEW_W, h, alpha);
        for (int i = 0; i < head.size(); i++) {
            g.drawString(font, head.get(i), x + 6, y + 5 + 9 * i, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, kit.ink()), alpha), false);
        }
        for (int i = 0; i < 6; i++) {
            HudLayout.Box slot = new HudLayout.Box(x + HudLayout.PREVIEW_INSET + i * HudLayout.SLOT, y + HudLayout.previewSlotsTop(head.size()), HudLayout.SLOT, HudLayout.SLOT);
            SlotArt.draw(g, font, style, SlotShape.of(other), slot, views[i], alpha, -1f, 0f, 0f);
        }
        return y;
    }

    /**
     * What each slot is, held Alt: beside each filled slot, on its outer side ({@link HudLayout#labels}), level with it: the name, then why it cannot be used,
     * or its cooldown, mastery, mode and a short description. A label is at most two lines, so labels never cover each other. A column in which any label does
     * not fit uncut (a name, a description or a reason is cut, or there is no room beside it) is shown as the wrapped panel above the column ({@link #panels})
     * instead, whole.
     */
    static void labels(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box[] slots, SlotView[] views, int previewTop, float alpha, boolean cacheable, boolean lifted) {
        HudKit kit = HudKit.of(style);
        HudLayout.Box[] boxes = HudLayout.labels(g.guiWidth(), g.guiHeight());
        boolean fresh = !cacheable || labelsTick != tick() || labelsWidth != g.guiWidth() || labelsStyle != style;
        if (fresh) {
            for (int i = 0; i < boxes.length; i++) {
                int room = boxes[i].w() - SlotLabel.PAD * 2;
                LABELS[i] = views[i].isEmpty() ? null : room < SlotLabel.MIN_ROOM ? SlotLabel.TOO_NARROW : label(views[i], room, font::width);
            }
        }
        boolean[] fallback = new boolean[2];
        for (int i = 0; i < boxes.length; i++) {
            if (LABELS[i] != null && LABELS[i].cut()) {
                fallback[i / 3] = true;
            }
        }
        for (int i = 0; i < boxes.length; i++) {
            SlotLabel.Label label = LABELS[i];
            if (label == null || fallback[i / 3]) {
                continue;
            }
            String[] text = label.lines();
            int widest = 0;
            for (String line : text) {
                widest = Math.max(widest, font.width(line));
            }
            int w = widest + SlotLabel.PAD * 2;
            int h = HudLayout.drawnHeight(text.length);
            HudLayout.Box box = boxes[i];
            int x = i < 3 ? box.right() - w : box.x();
            int y = box.y() + (box.h() - h) / 2;
            labelBox(g, kit, x, y, w, h, Math.max(alpha, 0.95f));
            g.drawString(font, text[0], x + SlotLabel.PAD, y + 2, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, HudFaces.labelName(kit)), 1f), false);
            int color = views[i].greyed() ? HudFaces.labelWarn(kit) : HudFaces.labelBody(kit);
            g.drawString(font, text[1], x + SlotLabel.PAD, y + 2 + 9, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, color), 1f), false);
        }
        labelsTick = tick();
        labelsWidth = g.guiWidth();
        labelsStyle = style;
        if (fallback[0] || fallback[1]) {
            panels(g, font, style, slots, views, previewTop, alpha, cacheable, lifted, fallback);
        }
    }
    private static SlotLabel.Label label(SlotView v, int room, java.util.function.ToIntFunction<String> measure) {
        String state = v.greyed() ? Component.translatable(v.greyedReason()).getString()
                : v.cooldownSeconds() > 0 ? Component.translatable("supernaturalbloodlines.hud.slot.cooldown", v.cooldownSeconds()).getString() : null;
        return SlotLabel.of(v.name(), v.greyed(), state, mastery(v.mastery()), v.detail(), v.description(), room, measure);
    }

    private static String mastery(net.ofatech.supernaturalbloodlines.core.hud.Mastery m) {
        if (m == null) {
            return null;
        }
        return switch (m.state()) {
            case MASTERED -> Component.translatable("supernaturalbloodlines.hud.slot.mastered").getString();
            case LEARNING -> Component.translatable("supernaturalbloodlines.hud.slot.learning", SlotLabel.percent(m)).getString();
            case PROGRESS -> Component.translatable(m.learned() ? "supernaturalbloodlines.hud.slot.learned" : "supernaturalbloodlines.hud.slot.mastery", SlotLabel.percent(m)).getString();
        };
    }

    /** One column's fallback panel: its lines, each line's colour, and the panel's width. */
    record Panel(java.util.List<String> lines, java.util.List<Integer> colors, int w) {
    }

    private static final Panel[] PANELS = new Panel[2];
    private static long panelsTick = Long.MIN_VALUE;
    private static int panelsWidth = -1;
    private static HudStyle.Style panelsStyle;

    /**
     * The fallback of {@link #labels} where a column leaves no room beside it (a small window at a big GUI scale): what each slot is, as one panel above the
     * column (Alt held): the key and name, what it does, and its cooldown or why it cannot be used. One panel per column keeps the entries from covering
     * each other; the left one stays in the left half of the screen and the right one stacks above the mode panel.
     */
    static void panels(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box[] boxes, SlotView[] views, int previewTop, float alpha, boolean cacheable, boolean lifted, boolean[] columns) {
        HudKit kit = HudKit.of(style);
        int half = g.guiWidth() / 2;
        boolean fresh = !cacheable || panelsTick != tick() || panelsWidth != g.guiWidth() || panelsStyle != style;
        for (int col = 0; col < 2; col++) {
            if (!columns[col]) {
                continue;
            }
            HudLayout.Box top = boxes[col * 3];
            if (fresh) {
                PANELS[col] = column(font, kit, views, col, top, half, g.guiWidth());
            }
            Panel c = PANELS[col];
            if (c == null) {
                continue;
            }
            java.util.List<String> lines = c.lines();
            java.util.List<Integer> colors = c.colors();
            int w = c.w();
            int h = 8 + 10 * lines.size();
            int x = col == 0 ? Math.max(4, top.x()) : Math.max(half + 2, Math.min(top.right() - w, g.guiWidth() - w - 4));
            // above the column (or the mode panel), but never off the top of the screen
            int y = col == 0 ? HudLayout.leftTooltipTop(top.y(), h, lifted) : Math.max(2, (col == 1 && previewTop >= 0 ? previewTop : top.y()) - h - 6);
            labelBox(g, kit, x, y, w, h, Math.max(alpha, 0.95f));
            int ty = y + 5;
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x + 6, ty, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, colors.get(i)), 1f), false);
                ty += 10;
            }
        }
        panelsTick = tick();
        panelsWidth = g.guiWidth();
        panelsStyle = style;
    }

    /** A blocked slot's full sentence (the panel has the room for it); a reason with no long form is shown as it is. */
    private static String longReason(String key) {
        return Component.translatable(net.minecraft.client.resources.language.I18n.exists(key + SlotLabel.LONG) ? key + SlotLabel.LONG : key).getString();
    }

    /** The wrapped lines of one column's fallback panel (null for a column with nothing in it). */
    private static Panel column(Font font, HudKit kit, SlotView[] views, int col, HudLayout.Box top, int half, int screenW) {
        int limit = col == 0 ? Math.max(60, half - 2 - Math.max(4, top.x())) : Math.max(60, Math.min(screenW - 4, top.right()) - (half + 2));
        int textW = Math.min(200, limit) - 12;
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<Integer> colors = new java.util.ArrayList<>();
        for (int i = col * 3; i < col * 3 + 3; i++) {
            SlotView v = views[i];
            if (v.isEmpty()) {
                continue;
            }
            for (String l : LabelFit.wrap((v.keyLabel().isEmpty() ? "" : v.keyLabel() + "  ") + v.name(), textW, 2, font::width)) {
                lines.add(l);
                colors.add(HudFaces.labelName(kit));
            }
            if (v.detail() != null) {
                for (String l : LabelFit.wrap(v.detail(), textW, 2, font::width)) {
                    lines.add(l);
                    colors.add(HudFaces.labelBody(kit));
                }
            }
            if (v.greyed()) {
                for (String l : LabelFit.wrap(longReason(v.greyedReason()), textW, 3, font::width)) {
                    lines.add(l);
                    colors.add(HudFaces.labelWarn(kit));
                }
            } else if (v.cooldownSeconds() > 0) {
                lines.add(Component.translatable("supernaturalbloodlines.hud.slot.cooldown", v.cooldownSeconds()).getString());
                colors.add(HudFaces.labelBody(kit));
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        int widest = 0;
        for (String l : lines) {
            widest = Math.max(widest, font.width(l));
        }
        return new Panel(lines, colors, widest + 12);
    }

    /** The full moon's warning: the moon in a label box and the time to the forced shift under it, top right. */
    static void moon(GuiGraphics g, Font font, HudStyle.Style style, long ticks, int screenW, float pulse) {
        HudKit kit = HudKit.of(style);
        float a = ticks <= 1000 ? 0.65f + 0.35f * pulse : 1f;
        int x = screenW - 36 - MOON_BOX; // left of the mods' own corner icon
        int y = 6;
        labelBox(g, kit, x, y, MOON_BOX, MOON_BOX, a);
        UiKit.icon(g, "moon_full", x + (MOON_BOX - 16) / 2, y + (MOON_BOX - 16) / 2, 16, SlotArt.fade(0xFFFFFFFF, a));
        Component line = Component.translatable("supernaturalbloodlines.hud.moon", MoonClock.clock(ticks));
        g.drawString(font, line, x + MOON_BOX - font.width(line), y + MOON_BOX + 4, SlotArt.fade(0xFFFFFFFF, a), true);
    }

    /** The tracked task: its name and a bar of its progress (the kit's mastery track), at the configured anchor (the top centre, under the boss bars, by default). */
    static void tracker(GuiGraphics g, Font font, HudStyle.Style style, TrackerAccess.Line line, float alpha, net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor anchor, int bossBottom) {
        HudKit kit = HudKit.of(style);
        String count = line.total() > 1 ? "  " + line.done() + "/" + line.total() : "";
        java.util.List<String> lines = wrap(font, line.text() + count, 150, 2);
        int widest = 0;
        for (String l : lines) {
            widest = Math.max(widest, font.width(l));
        }
        int w = widest + 14;
        int h = TrackerAnchor.height(lines.size());
        int[] at = anchor.place(g.guiWidth(), g.guiHeight(), w, h, bossBottom);
        int x = at[0];
        int y = at[1];
        labelBox(g, kit, x, y, w, h, alpha);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + 7, y + 4 + 9 * i, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, kit.ink()), alpha), false);
        }
        int bar = w - 14;
        int by = y + 4 + 9 * lines.size();
        UiKit.draw(g, kit, Part.MASTERY_TRACK, x + 7, by, bar, 4, alpha);
        int fill = Math.round((bar - 2) * line.fraction());
        if (fill > 0) {
            UiKit.draw(g, kit, Part.MASTERY_FILL, x + 8, by + 1, fill, 2, alpha);
        }
    }
}
