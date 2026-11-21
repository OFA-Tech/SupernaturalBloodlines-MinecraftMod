package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.MoonClock;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * What stands round the slots: the mode pill ("SUPERNATURAL . Caps") above the hotbar, the panel that shows the other mode's slots, the full moon warning
 * ("Forced shift in 1:40") in the top right, and the tracked task's line (top centre, under the boss bars, by default). Each is in the colours of the hub's theme.
 */
final class HudChrome {
    private static final int GOLD = 0xFFFFD36B;

    private HudChrome() {
    }

    static int edge(HudStyle.Style style) {
        return switch (style) {
            case GRIMOIRE -> 0xFF8E5A2A;
            case BLOOD_MOON -> 0xFFE2493F;
            case TENSURA -> 0xFF3FD0D6;
        };
    }

    static int face(HudStyle.Style style) {
        return switch (style) {
            case GRIMOIRE -> 0xE02A1A10;
            case BLOOD_MOON -> 0xE0140810;
            case TENSURA -> 0xE00F2438;
        };
    }

    static int ink(HudStyle.Style style) {
        return style == HudStyle.Style.GRIMOIRE ? 0xFFF4E2C0 : 0xFFF2F6FA;
    }

    /** A box with a one pixel edge and its corners taken off, as a pill. */
    static void pillBox(GuiGraphics g, HudStyle.Style style, int x, int y, int w, int h, float alpha) {
        int edge = SlotArt.fade(0xFF000000 | edge(style), alpha);
        int face = SlotArt.fade(face(style), alpha);
        g.fill(x + 1, y, x + w - 1, y + h, edge);
        g.fill(x, y + 1, x + w, y + h - 1, edge);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, face);
    }

    // The texts below are worked out once a tick, not once a frame: translating, measuring and wrapping them is the cost, and they only change with the game.
    private static String pillText = "";
    private static int pillWidth;
    private static SlotMode pillMode;
    private static long pillTick = Long.MIN_VALUE;

    private static final String[][] LABELS = new String[6][];
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
        HudLayout.Box box = HudLayout.pill(screenW, screenH, pillWidth, lifted);
        int x = box.x();
        int y = box.y();
        String text = pillWidth + 22 > box.w() ? font.plainSubstrByWidth(pillText, box.w() - 22) : pillText;
        pillBox(g, style, x, y, box.w(), box.h(), alpha);
        // the diamond that marks the mode
        for (int dy = -3; dy <= 3; dy++) {
            int half = 3 - Math.abs(dy);
            g.fill(x + 8 - half, y + 7 + dy, x + 9 + half, y + 8 + dy, SlotArt.fade(0xFF000000 | edge(style), alpha));
        }
        g.drawString(font, text, x + 16, y + 3, SlotArt.fade(ink(style), alpha), false);
    }

    /** The preview panel's width: six slots and a little air; it stays in the right half of the screen even at 480 GUI pixels. */
    static final int PREVIEW_W = HudLayout.PREVIEW_W;

    /**
     * The panel with the other mode's six slots, in the theme's shape: "Caps Lock -> TENSURA". The head wraps to two lines when it has to.
     *
     * @return the panel's top edge, for the tooltips to stack above it
     */
    static int preview(GuiGraphics g, Font font, HudStyle.Style style, SlotMode other, SlotView[] views, HudLayout.Box[] boxes, float alpha) {
        String key = HudKeys.HUD_MODE.getTranslatedKeyMessage().getString();
        String name = Component.translatable("supernaturalbloodlines.hud.pill." + other.name().toLowerCase(Locale.ROOT)).getString();
        java.util.List<String> head = wrap(font, Component.translatable("supernaturalbloodlines.hud.pill.next", key, name).getString(), PREVIEW_W - 12, 2);
        HudLayout.Box panel = HudLayout.preview(g.guiWidth(), g.guiHeight(), head.size());
        int h = panel.h();
        int x = panel.x();
        int y = panel.y();
        pillBox(g, style, x, y, PREVIEW_W, h, alpha);
        for (int i = 0; i < head.size(); i++) {
            g.drawString(font, head.get(i), x + 6, y + 5 + 9 * i, SlotArt.fade(ink(style), alpha), false);
        }
        for (int i = 0; i < 6; i++) {
            HudLayout.Box slot = new HudLayout.Box(x + 4 + i * HudLayout.SLOT, y + 7 + 9 * head.size(), HudLayout.SLOT - 1, HudLayout.SLOT - 1);
            SlotArt.draw(g, font, style, SlotShape.of(other), slot, views[i], alpha, -1f, 0f);
        }
        return y;
    }
    /**
     * What each slot is, held Alt: two lines beside each filled slot, on its outer side ({@link HudLayout#labels}), level with it: the name, then the mastery,
     * why it cannot be used or its cooldown, the mode and a short description. The name is cut only when it is wider than the room; the second line is cut with
     * an ellipsis. Nothing is stacked above the slots.
     */
    static void labels(GuiGraphics g, Font font, HudStyle.Style style, SlotView[] views, float alpha, boolean cacheable) {
        HudLayout.Box[] boxes = HudLayout.labels(g.guiWidth(), g.guiHeight());
        boolean fresh = !cacheable || labelsTick != tick() || labelsWidth != g.guiWidth() || labelsStyle != style;
        for (int i = 0; i < boxes.length; i++) {
            SlotView v = views[i];
            int room = boxes[i].w() - SlotLabel.PAD * 2;
            if (v.isEmpty() || room < SlotLabel.MIN_ROOM) {
                continue;
            }
            if (fresh || LABELS[i] == null) {
                String[] raw = label(v);
                LABELS[i] = new String[] {SlotLabel.fit(raw[0], room, font::width), SlotLabel.fit(raw[1], room, font::width)};
            }
            String[] text = LABELS[i];
            int w = Math.max(font.width(text[0]), font.width(text[1])) + SlotLabel.PAD * 2;
            HudLayout.Box box = boxes[i];
            int x = i < 3 ? box.right() - w : box.x();
            pillBox(g, style, x, box.y(), w, box.h(), Math.max(alpha, 0.95f));
            g.drawString(font, text[0], x + SlotLabel.PAD, box.y() + 2, SlotArt.fade(ink(style), 1f), false);
            if (!text[1].isEmpty()) {
                g.drawString(font, text[1], x + SlotLabel.PAD, box.y() + 11, SlotArt.fade(v.greyed() ? 0xFFFF7A70 : 0xFFB8B0B4, 1f), false);
            }
        }
        labelsTick = tick();
        labelsWidth = g.guiWidth();
        labelsStyle = style;
    }

    private static String[] label(SlotView v) {
        String state = v.greyed() ? Component.translatable(v.greyedReason()).getString()
                : v.cooldownSeconds() > 0 ? Component.translatable("supernaturalbloodlines.hud.slot.cooldown", v.cooldownSeconds()).getString() : null;
        return SlotLabel.lines(v.name(), mastery(v.mastery()), state, v.detail(), v.description());
    }
    private static String mastery(net.ofatech.supernaturalbloodlines.core.hud.Mastery m) {
        if (m == null) {
            return null;
        }
        return switch (m.state()) {
            case MASTERED -> Component.translatable("supernaturalbloodlines.hud.slot.mastered").getString();
            case LEARNING -> Component.translatable("supernaturalbloodlines.hud.slot.learning", SlotLabel.percent(m)).getString();
            case PROGRESS -> Component.translatable("supernaturalbloodlines.hud.slot.mastery", SlotLabel.percent(m)).getString();
        };
    }

    /** The full moon's warning: a gold moon and the time to the forced shift, top right. */
    static void moon(GuiGraphics g, Font font, HudStyle.Style style, long ticks, int screenW, float pulse) {
        float a = ticks <= 1000 ? 0.65f + 0.35f * pulse : 1f;
        int x = screenW - 6 - 22 - 30; // left of the mods' own corner icon
        int y = 6;
        pillBox(g, style, x, y, 22, 22, a);
        net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw.shape(g, net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes.Shape.ROUND, x + 4, y + 4, 14, SlotArt.fade(GOLD, a));
        Component line = Component.translatable("supernaturalbloodlines.hud.moon", MoonClock.clock(ticks));
        g.drawString(font, line, x + 22 - font.width(line), y + 26, SlotArt.fade(0xFFFFFFFF, a), true);
    }

    /** The tracked task: its name and a bar of its progress, at the configured anchor (the top centre, under the boss bars, by default). */
    static void tracker(GuiGraphics g, Font font, HudStyle.Style style, TrackerAccess.Line line, float alpha, TrackerAnchor anchor, int bossBottom) {
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
        pillBox(g, style, x, y, w, h, alpha);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + 7, y + 4 + 9 * i, SlotArt.fade(ink(style), alpha), false);
        }
        int bar = w - 14;
        int by = y + 5 + 9 * lines.size();
        g.fill(x + 7, by, x + 7 + bar, by + 2, SlotArt.fade(0x80000000, alpha));
        g.fill(x + 7, by, x + 7 + Math.round(bar * line.fraction()), by + 2, SlotArt.fade(0xFF000000 | edge(style), alpha));
    }
}
