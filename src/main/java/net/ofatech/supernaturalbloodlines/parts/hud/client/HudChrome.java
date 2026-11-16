package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.MoonClock;
import net.ofatech.supernaturalbloodlines.core.hud.SlotMode;
import net.ofatech.supernaturalbloodlines.core.hud.TrackerAnchor;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.LabelFit;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;
import net.ofatech.supernaturalbloodlines.shared.hud.TrackerAccess;

/**
 * What stands round the slots: the mode pill ("SUPERNATURAL . Caps") above the hotbar, the panel that shows the other mode's slots, the full moon warning
 * ("Forced shift in 1:40") in the top right, and the tracked task's line in the top left. Each is in the colours of the hub's theme.
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

    /** "SUPERNATURAL . Caps" centred over the hotbar. */
    static void pill(GuiGraphics g, Font font, HudStyle.Style style, SlotMode mode, int screenW, int screenH, float alpha) {
        String key = HudKeys.HUD_MODE.getTranslatedKeyMessage().getString();
        String name = Component.translatable("supernaturalbloodlines.hud.pill." + mode.name().toLowerCase(Locale.ROOT)).getString();
        String text = name + "  \u00b7  " + key;
        int w = font.width(text) + 22;
        int x = screenW / 2 - w / 2;
        int y = screenH - 22 - 52;
        pillBox(g, style, x, y, w, 14, alpha);
        // the diamond that marks the mode
        for (int dy = -3; dy <= 3; dy++) {
            int half = 3 - Math.abs(dy);
            g.fill(x + 8 - half, y + 7 + dy, x + 9 + half, y + 8 + dy, SlotArt.fade(0xFF000000 | edge(style), alpha));
        }
        g.drawString(font, text, x + 16, y + 3, SlotArt.fade(ink(style), alpha), false);
    }

    /** The panel's top edge, for the tooltips to stack above it. */
    static int previewTop(HudLayout.Box[] boxes) {
        return boxes[3].y() - 50;
    }

    /** The preview panel's width: six slots and a little air; it stays in the right half of the screen even at 480 GUI pixels. */
    static final int PREVIEW_W = 6 * HudLayout.SLOT + 8;

    /** The panel with the other mode's six slots, in the theme's shape: "Caps again -> TENSURA mode". */
    static void preview(GuiGraphics g, Font font, HudStyle.Style style, SlotMode other, SlotView[] views, HudLayout.Box[] boxes, float alpha) {
        String key = HudKeys.HUD_MODE.getTranslatedKeyMessage().getString();
        String name = Component.translatable("supernaturalbloodlines.hud.pill." + other.name().toLowerCase(Locale.ROOT)).getString();
        String head = LabelFit.cut(Component.translatable("supernaturalbloodlines.hud.pill.next", key, name).getString(), PREVIEW_W - 12, font::width);
        int x = Math.max(4, Math.min(boxes[3].right() - PREVIEW_W, g.guiWidth() - PREVIEW_W - 4));
        int y = previewTop(boxes);
        pillBox(g, style, x, y, PREVIEW_W, 46, alpha);
        g.drawString(font, head, x + 6, y + 5, SlotArt.fade(ink(style), alpha), false);
        for (int i = 0; i < 6; i++) {
            HudLayout.Box slot = new HudLayout.Box(x + 4 + i * HudLayout.SLOT, y + 20, HudLayout.SLOT - 1, HudLayout.SLOT - 1);
            SlotArt.draw(g, font, style, slot, views[i], alpha, -1f, 0f);
        }
    }

    /**
     * What each slot is, as one panel above each column (Alt held): the key and name, what it does, and its cooldown or why it cannot be used. One panel per
     * column keeps the entries from covering each other; the left one stays in the left half of the screen and the right one stacks above the mode panel.
     */
    static void tooltips(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box[] boxes, SlotView[] views, boolean previewUp, float alpha) {
        int half = g.guiWidth() / 2;
        for (int col = 0; col < 2; col++) {
            HudLayout.Box top = boxes[col * 3];
            int limit = col == 0 ? Math.max(60, half - 2 - Math.max(4, top.x())) : Math.max(60, Math.min(g.guiWidth() - 4, top.right()) - (half + 2));
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
                    colors.add(ink(style));
                }
                if (v.detail() != null) {
                    for (String l : LabelFit.wrap(v.detail(), textW, 2, font::width)) {
                        lines.add(l);
                        colors.add(0xFFB8B0B4);
                    }
                }
                if (v.greyed()) {
                    for (String l : LabelFit.wrap(Component.translatable(v.greyedReason()).getString(), textW, 3, font::width)) {
                        lines.add(l);
                        colors.add(0xFFFF7A70);
                    }
                } else if (v.cooldownSeconds() > 0) {
                    lines.add(Component.translatable("supernaturalbloodlines.hud.slot.cooldown", v.cooldownSeconds()).getString());
                    colors.add(0xFFB8B0B4);
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            int w = 12;
            int widest = 0;
            for (String l : lines) {
                widest = Math.max(widest, font.width(l));
            }
            w += widest;
            int h = 8 + 10 * lines.size();
            int x = col == 0 ? Math.max(4, top.x()) : Math.max(half + 2, Math.min(top.right() - w, g.guiWidth() - w - 4));
            // above the column (or the mode panel), but never off the top of the screen
            int y = Math.max(2, (col == 1 && previewUp ? previewTop(boxes) : top.y()) - h - 6);
            pillBox(g, style, x, y, w, h, Math.max(alpha, 0.95f));
            int ty = y + 5;
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x + 6, ty, SlotArt.fade(colors.get(i), 1f), false);
                ty += 10;
            }
        }
    }
    /** The full moon's warning: a gold moon and the time to the forced shift, top right. */
    static void moon(GuiGraphics g, Font font, HudStyle.Style style, long ticks, int screenW, float pulse) {
        float a = ticks <= 1000 ? 0.65f + 0.35f * pulse : 1f;
        int x = screenW - 6 - 22 - 30; // left of the mods' own corner icon
        int y = 6;
        pillBox(g, style, x, y, 22, 22, a);
        for (int dy = -7; dy <= 7; dy++) {
            int half = (int) Math.sqrt(49 - dy * dy);
            g.fill(x + 11 - half, y + 11 + dy, x + 11 + half, y + 12 + dy, SlotArt.fade(GOLD, a));
        }
        Component line = Component.translatable("supernaturalbloodlines.hud.moon", MoonClock.clock(ticks));
        g.drawString(font, line, x + 22 - font.width(line), y + 26, SlotArt.fade(0xFFFFFFFF, a), true);
    }

    /** The tracked task: its name and a bar of its progress, at the configured anchor (the top centre, under the boss bars, by default). */
    static void tracker(GuiGraphics g, Font font, HudStyle.Style style, TrackerAccess.Line line, float alpha, TrackerAnchor anchor, int bossBottom) {
        String text = LabelFit.cut(line.text(), 150, font::width);
        String count = line.total() > 1 ? "  " + line.done() + "/" + line.total() : "";
        int w = font.width(text + count) + 14;
        int[] at = anchor.place(g.guiWidth(), g.guiHeight(), w, bossBottom);
        int x = at[0];
        int y = at[1];
        pillBox(g, style, x, y, w, TrackerAnchor.HEIGHT, alpha);
        g.drawString(font, text + count, x + 7, y + 4, SlotArt.fade(ink(style), alpha), false);
        int bar = w - 14;
        g.fill(x + 7, y + 15, x + 7 + bar, y + 17, SlotArt.fade(0x80000000, alpha));
        g.fill(x + 7, y + 15, x + 7 + Math.round(bar * line.fraction()), y + 17, SlotArt.fade(0xFF000000 | edge(style), alpha));
    }}
