package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.PieSweep;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.BakedTexture;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * One slot, square in Tensura mode (every Tensura icon is square) and otherwise in the shape of the hub's theme: an inked tile with a wax key badge (Grimoire), a round slot with a gold key letter (Blood Moon), a hexagon
 * with a key tab (Tensura). On top of the shape, the same for all: the icon, a dark pie that sweeps clockwise as the cooldown runs out with the seconds in
 * the middle, a gold outline and halo while it is active, a lock on one that is not unlocked, and a ring of blood round Bite. Everything is multiplied by
 * {@code alpha}, which is how the HUD fades when idle.
 */
final class SlotArt {
    private static final int GOLD = 0xFFFFD36B;
    private static final int BLOOD = 0xFFC01830;

    private SlotArt() {
    }

    static int fade(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return a << 24 | (argb & 0xFFFFFF);
    }

    /** True for a slot that cannot be used because the skill or action is not unlocked (it gets a lock), not for one that is only on cooldown. */
    static boolean locked(SlotView v) {
        String r = v.greyedReason();
        return r != null && (r.endsWith("skill_missing") || r.endsWith("slot.locked") || r.endsWith("not_unlocked") || r.endsWith("action_missing"));
    }

    /**
     * @param bloodRing 0 to 1 of a ring of blood round the slot, or a negative number for none (only Bite has one)
     * @param pulse     0 to 1, the halo's breathing
     */
    static void draw(GuiGraphics g, Font font, HudStyle.Style style, SlotShape shape, HudLayout.Box b, SlotView v, float alpha, float bloodRing, float pulse) {
        int s = HudLayout.SLOT;
        if (v.active()) {
            KitDraw.shape(g, shapeOf(style, shape), b.x() - 2, b.y() - 2, s + 4, fade(0x00000000 | ((int) (0x22 + 0x22 * pulse) << 24) | 0xFFD36B, alpha));
        }
        base(g, style, shape, b, v.active(), alpha);
        if (!v.isEmpty()) {
            boolean lock = locked(v);
            ClientIcons.draw(g, v.icon(), b.x() + 3, b.y() + 3, 16, (lock ? 0.35f : v.greyed() ? 0.45f : 1f) * alpha);
            if (v.cooldown() > 0f) {
                pie(g, b, v.cooldown(), alpha);
                if (v.cooldownSeconds() > 0) {
                    // lifted over the pie: text is drawn in its own pass, which would otherwise put the pie over the number
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    // a small label over the dimmed icon: "4s", at three quarters of the text size, with the font's own shadow
                    String secs = v.cooldownSeconds() + "s";
                    // at the top of the slot, so the key badge in the bottom corner never covers it
                    g.drawString(font, secs, b.x() + (s - font.width(secs)) / 2, b.y() + 2, fade(0xFFFFFFFF, alpha), true);
                    g.pose().popPose();
                }
            }
            if (lock) {
                KitDraw.icon(g, Icons.LOCK, b.x() + (s - 10) / 2, b.y() + (s - 10) / 2, 10, fade(0xFFE6E0D0, alpha));
            }
        }
        if (bloodRing >= 0f) {
            ring(g, b, bloodRing, alpha);
        }
        key(g, font, style, b, v.keyLabel(), alpha);
    }

    /** Tensura mode is square whatever the theme (every Tensura icon is square); Supernatural mode takes the theme's shape. */
    static Shapes.Shape shapeOf(HudStyle.Style style, SlotShape shape) {
        if (shape == SlotShape.SQUARE) {
            return Shapes.Shape.SQUARE;
        }
        return switch (style) {
            case GRIMOIRE -> Shapes.Shape.SQUARE;
            case BLOOD_MOON -> Shapes.Shape.ROUND;
            case TENSURA -> Shapes.Shape.HEX;
        };
    }

    private static void base(GuiGraphics g, HudStyle.Style style, SlotShape shape, HudLayout.Box b, boolean active, float alpha) {
        int s = HudLayout.SLOT;
        int edge = active ? GOLD : switch (style) {
            case GRIMOIRE -> 0xFF5A3A1E;
            case BLOOD_MOON -> 0xFFE2493F;
            case TENSURA -> 0xFF3FD0D6;
        };
        int face = switch (style) {
            case GRIMOIRE -> 0xFF2A1A10;
            case BLOOD_MOON -> 0xFF140810;
            case TENSURA -> 0xFF0F2438;
        };
        KitDraw.shape(g, shapeOf(style, shape), b.x(), b.y(), s, fade(edge, alpha));
        KitDraw.shape(g, shapeOf(style, shape), b.x() + 1, b.y() + 1, s - 2, fade(0xE0000000 | face & 0xFFFFFF, alpha));
        if (style == HudStyle.Style.GRIMOIRE) {
            // an inked tile: a lighter paper line inside the edge
            g.renderOutline(b.x() + 2, b.y() + 2, s - 4, s - 4, fade(0x40E8D8B0, alpha));
        }
    }

    private static final int PIE_STEPS = 32;
    private static final int RING_SIZE = 32;
    private static final int RING_DOTS = 72;
    private static final int RING_STEPS = 24;

    /** The dark sweep over the icon: it covers {@code left} of the circle from twelve o'clock clockwise. One baked mask per 1/32, tinted. */
    private static void pie(GuiGraphics g, HudLayout.Box b, float left, float alpha) {
        int step = Math.max(1, Math.min(PIE_STEPS, Math.round(left * PIE_STEPS)));
        var mask = BakedTexture.get(BakedTexture.Key.of("pie", step), 16, 16, (x, y) -> PieSweep.covered(x + 0.5 - 8, y + 0.5 - 8, step / (float) PIE_STEPS) ? 0xFFFFFFFF : 0);
        BakedTexture.blitTinted(g, mask, b.x() + 3, b.y() + 3, 16, 16, fade(0xB0000000, alpha));
    }

    /** Whether the dot {@code i} of the ring is lit when {@code lit} dots are. */
    static boolean dotLit(int i, int lit) {
        return i < lit;
    }

    /** The ring's dots as a mask: the first {@code lit} of {@link #RING_DOTS}, clockwise from the top, each 2 x 2. */
    private static int ringPixel(int x, int y, int lit) {
        for (int i = 0; i < RING_DOTS; i++) {
            if (!dotLit(i, lit)) {
                continue;
            }
            double a = 2 * Math.PI * i / RING_DOTS;
            int px = RING_SIZE / 2 + (int) Math.round(Math.sin(a) * 12.5) - 1;
            int py = RING_SIZE / 2 - (int) Math.round(Math.cos(a) * 12.5) - 1;
            if (x >= px && x < px + 2 && y >= py && y < py + 2) {
                return 0xFFFFFFFF;
            }
        }
        return 0;
    }

    /** A ring of blood round the slot, filled clockwise from the top to {@code share}: the whole ring dim and the lit part over it, two blits. */
    private static void ring(GuiGraphics g, HudLayout.Box b, float share, float alpha) {
        int x = b.x() + HudLayout.SLOT / 2 - RING_SIZE / 2;
        int y = b.y() + HudLayout.SLOT / 2 - RING_SIZE / 2;
        int step = Math.max(0, Math.min(RING_STEPS, Math.round(share * RING_STEPS)));
        var all = BakedTexture.get(BakedTexture.Key.of("ring", -1), RING_SIZE, RING_SIZE, (px, py) -> ringPixel(px, py, RING_DOTS));
        BakedTexture.blitTinted(g, all, x, y, RING_SIZE, RING_SIZE, fade(0x40601020, alpha));
        if (step > 0) {
            int lit = step * RING_DOTS / RING_STEPS;
            var part = BakedTexture.get(BakedTexture.Key.of("ring", step), RING_SIZE, RING_SIZE, (px, py) -> ringPixel(px, py, lit));
            BakedTexture.blitTinted(g, part, x, y, RING_SIZE, RING_SIZE, fade(BLOOD, alpha));
        }
    }
    private static void key(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box b, String label, float alpha) {
        if (label.isEmpty()) {
            return;
        }
        // at the normal size (nothing is under 8 GUI pixels): a letter, or the first two characters of a longer key name
        String text = label.length() > 2 ? label.substring(0, 2) : label;
        int w = font.width(text);
        switch (style) {
            case GRIMOIRE -> {
                // a red wax badge in the corner of the tile
                int bw = Math.max(11, w + 5);
                KitDraw.shape(g, Shapes.Shape.ROUND, b.right() - bw - 1, b.bottom() - 12, bw, fade(0xFF8E1A1A, alpha));
                g.drawString(font, text, b.right() - bw - 1 + (bw - w) / 2, b.bottom() - 12 + (bw - 8) / 2, fade(0xFFF4E2C0, alpha), false);
            }
            case BLOOD_MOON -> {
                // a gold letter on the ring's lower edge, on a dark plate so it reads over the icon
                int x = b.x() + (HudLayout.SLOT - w) / 2;
                g.fill(x - 1, b.bottom() - 9, x + w + 1, b.bottom() - 1, fade(0xB0140810, alpha));
                g.drawString(font, text, x, b.bottom() - 9, fade(GOLD, alpha), false);
            }
            case TENSURA -> {
                // a dark tab with a teal edge at the top left
                int tw = w + 4;
                g.fill(b.x() - 2, b.y() - 3, b.x() - 2 + tw, b.y() + 8, fade(0xFF3FD0D6, alpha));
                g.fill(b.x() - 1, b.y() - 2, b.x() - 3 + tw, b.y() + 7, fade(0xFF0A1A28, alpha));
                g.drawString(font, text, b.x(), b.y() - 1, fade(0xFF3FD0D6, alpha), false);
            }
            default -> { }
        }
    }
}