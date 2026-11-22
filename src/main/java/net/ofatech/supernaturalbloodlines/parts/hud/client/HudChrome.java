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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
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
            UiKit.shared(g, KitSprites.MODE_MARK, x + 8, y + (box.h() - 7) / 2, 7, 7, alpha);
        } else {
            UiKit.shared(g, KitSprites.BLOOD_FULL, x + 7, y + (box.h() - 9) / 2, 9, 9, alpha);
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
     * What each slot is, held Alt: beside each filled slot, on its outer side ({@link HudLayout#labels}), level with it: its name and its state (why it cannot be
     * used, or its cooldown, with the mastery when there is room). A label is at most two lines, so labels never cover each other. The rest (the full name, the whole
     * reason, the mastery, what the skill does) is in the column's panel ({@link #panels}), which shows once Alt has been held for a second ({@code detail}); a column
     * whose labels do not fit (a state is wider than the room beside it, or there is none) shows the panel at once instead of them.
     */
    static void labels(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box[] slots, SlotView[] views, int previewTop, float alpha, boolean cacheable, boolean lifted, boolean detail) {
        HudKit kit = HudKit.of(style);
        HudLayout.Box[] boxes = HudLayout.labels(g.guiWidth(), g.guiHeight());
        boolean fresh = !cacheable || labelsTick != tick() || labelsWidth != g.guiWidth() || labelsStyle != style;
        if (fresh) {
            for (int i = 0; i < boxes.length; i++) {
                int room = boxes[i].w() - SlotLabel.PAD * 2;
                LABELS[i] = views[i].isEmpty() ? null : room < SlotLabel.MIN_ROOM ? SlotLabel.TOO_NARROW : label(views[i], room, font::width);
            }
        }
        boolean[] fallback = SlotLabel.fallbackColumns(LABELS);
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
        boolean[] columns = {fallback[0] || detail, fallback[1] || detail};
        if (columns[0] || columns[1]) {
            panels(g, font, style, slots, views, previewTop, alpha, cacheable, lifted, columns, fallback);
        }
    }

    /** The label of a slot. A countdown and a percentage are decided at their widest ("Ready in 99 s", 100 %), so a label does not switch to the panel and back as they change. */
    private static SlotLabel.Label label(SlotView v, int room, java.util.function.ToIntFunction<String> measure) {
        String state = v.greyed() ? Component.translatable(v.greyedReason()).getString() : cooldown(v.cooldownSeconds());
        String widest = v.greyed() ? state : v.cooldownSeconds() > 0 ? cooldown(99) : null;
        return SlotLabel.of(v.name(), v.greyed(), state, widest, mastery(v.mastery(), -1), mastery(v.mastery(), 100), room, measure);
    }

    private static String cooldown(int seconds) {
        return seconds > 0 ? Component.translatable("supernaturalbloodlines.hud.slot.cooldown", seconds).getString() : null;
    }

    /** @param percent the figure to show, or a negative number for the slot's own */
    private static String mastery(net.ofatech.supernaturalbloodlines.core.hud.Mastery m, int percent) {
        if (m == null) {
            return null;
        }
        int p = percent >= 0 ? percent : SlotLabel.percent(m);
        return switch (m.state()) {
            case MASTERED -> Component.translatable("supernaturalbloodlines.hud.slot.mastered").getString();
            case LEARNING -> Component.translatable("supernaturalbloodlines.hud.slot.learning", p).getString();
            case PROGRESS -> Component.translatable(m.learned() ? "supernaturalbloodlines.hud.slot.learned" : "supernaturalbloodlines.hud.slot.mastery", p).getString();
        };
    }

    /** One column's panel: its lines and the panel's width. */
    record Panel(java.util.List<PanelText.Line> lines, int w) {
    }

    private static final Panel[] PANELS = new Panel[2];
    private static long panelsTick = Long.MIN_VALUE;
    private static int panelsKey;

    /**
     * What each slot of a column is, in full, as one panel above the column: the key and name, why it cannot be used, its cooldown and mastery, and what it does.
     * One panel per column keeps the entries from covering each other; the left one stays in the left half of the screen and the right one stacks above the mode
     * panel and under the moon warning. It is fitted to the room there is ({@link PanelText}: the description goes first).
     *
     * @param columns  which columns get a panel
     * @param fallback which of them have no labels beside them (their panel is all there is, so it keeps at least three lines)
     */
    static void panels(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box[] boxes, SlotView[] views, int previewTop, float alpha, boolean cacheable, boolean lifted, boolean[] columns, boolean[] fallback) {
        HudKit kit = HudKit.of(style);
        int half = g.guiWidth() / 2;
        int key = java.util.Objects.hash(g.guiWidth(), g.guiHeight(), previewTop, lifted, style);
        boolean fresh = !cacheable || panelsTick != tick() || panelsKey != key;
        for (int col = 0; col < 2; col++) {
            if (!columns[col]) {
                continue;
            }
            HudLayout.Box top = boxes[col * 3];
            int stackTop = col == 1 && previewTop >= 0 ? previewTop : top.y();
            if (fresh) {
                int lines = HudLayout.panelLines(HudLayout.panelRoom(col == 1, g.guiWidth(), stackTop, lifted));
                PANELS[col] = column(font, views, col, top, half, g.guiWidth(), fallback[col] ? Math.max(3, lines) : lines);
            }
            Panel c = PANELS[col];
            if (c == null) {
                continue;
            }
            java.util.List<PanelText.Line> lines = c.lines();
            int w = c.w();
            int h = 8 + 10 * lines.size();
            int x = col == 0 ? Math.max(4, top.x()) : Math.max(half + 2, Math.min(top.right() - w, g.guiWidth() - w - 4));
            int y = col == 0 ? HudLayout.leftTooltipTop(top.y(), h, lifted) : HudLayout.rightPanelTop(g.guiWidth(), stackTop, h);
            labelBox(g, kit, x, y, w, h, Math.max(alpha, 0.95f));
            int ty = y + 5;
            for (PanelText.Line line : lines) {
                int color = switch (line.role()) {
                    case NAME -> HudFaces.labelName(kit);
                    case WARN -> HudFaces.labelWarn(kit);
                    case BODY -> HudFaces.labelBody(kit);
                };
                g.drawString(font, line.text(), x + 6, ty, SlotArt.fade(UiKit.labelOver(kit, Part.HUD_LABEL, color), 1f), false);
                ty += 10;
            }
        }
        panelsTick = tick();
        panelsKey = key;
    }

    /** A blocked slot's full sentence (the panel has the room for it); a reason with no long form is shown as it is. */
    private static String longReason(String key) {
        return Component.translatable(net.minecraft.client.resources.language.I18n.exists(key + SlotLabel.LONG) ? key + SlotLabel.LONG : key).getString();
    }

    /** The fitted lines of one column's panel (null for a column with nothing in it). */
    private static Panel column(Font font, SlotView[] views, int col, HudLayout.Box top, int half, int screenW, int maxLines) {
        int limit = col == 0 ? Math.max(60, half - 2 - Math.max(4, top.x())) : Math.max(60, Math.min(screenW - 4, top.right()) - (half + 2));
        int textW = Math.min(200, limit) - 12;
        java.util.List<PanelText.Entry> entries = new java.util.ArrayList<>();
        for (int i = col * 3; i < col * 3 + 3; i++) {
            SlotView v = views[i];
            if (v.isEmpty()) {
                continue;
            }
            String state = join(cooldown(v.greyed() ? 0 : v.cooldownSeconds()), mastery(v.mastery(), -1));
            entries.add(new PanelText.Entry((v.keyLabel().isEmpty() ? "" : v.keyLabel() + "  ") + v.name(), v.greyed() ? longReason(v.greyedReason()) : null, state, join(v.detail(), v.description())));
        }
        java.util.List<PanelText.Line> lines = PanelText.lines(entries, textW, maxLines, font::width);
        if (lines.isEmpty()) {
            return null;
        }
        int widest = 0;
        for (PanelText.Line l : lines) {
            widest = Math.max(widest, font.width(l.text()));
        }
        return new Panel(lines, widest + 12);
    }

    private static String join(String... parts) {
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                out.append(out.length() == 0 ? "" : " \u00b7 ").append(p.strip());
            }
        }
        return out.length() == 0 ? null : out.toString();
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
        net.ofatech.supernaturalbloodlines.shared.hud.HudPlacement.trackerBox(new HudLayout.Box(x, y, w, h));
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
