package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.PieSweep;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Shapes;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * One slot in the shape of the hub's theme: an inked tile with a wax key badge (Grimoire), a round slot with a gold key letter (Blood Moon), a hexagon
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
    static void draw(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box b, SlotView v, float alpha, float bloodRing, float pulse) {
        int s = HudLayout.SLOT;
        if (v.active()) {
            KitDraw.shape(g, shapeOf(style), b.x() - 2, b.y() - 2, s + 4, fade(0x00000000 | ((int) (0x22 + 0x22 * pulse) << 24) | 0xFFD36B, alpha));
        }
        base(g, style, b, v.active(), alpha);
        if (!v.isEmpty()) {
            boolean lock = locked(v);
            ClientIcons.draw(g, v.icon(), b.x() + 3, b.y() + 3, 16, (lock ? 0.35f : v.greyed() ? 0.45f : 1f) * alpha);
            if (v.cooldown() > 0f) {
                pie(g, b, v.cooldown(), alpha);
                if (v.cooldownSeconds() > 0) {
                    // lifted over the pie: text is drawn in its own pass, which would otherwise put the pie over the number
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    String secs = Integer.toString(v.cooldownSeconds());
                    int sw = font.width(secs);
                    // a dark plate under the digits, so they read on a bright icon too
                    g.fill(b.x() + (s - sw) / 2 - 2, b.y() + s / 2 - 5, b.x() + (s + sw) / 2 + 2, b.y() + s / 2 + 5, fade(0xC0000000, alpha));
                    g.drawCenteredString(font, secs, b.x() + s / 2, b.y() + s / 2 - 4, fade(0xFFFFFFFF, alpha));
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

    private static Shapes.Shape shapeOf(HudStyle.Style style) {
        return switch (style) {
            case GRIMOIRE -> Shapes.Shape.SQUARE;
            case BLOOD_MOON -> Shapes.Shape.ROUND;
            case TENSURA -> Shapes.Shape.HEX;
        };
    }

    private static void base(GuiGraphics g, HudStyle.Style style, HudLayout.Box b, boolean active, float alpha) {
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
        KitDraw.shape(g, shapeOf(style), b.x(), b.y(), s, fade(edge, alpha));
        KitDraw.shape(g, shapeOf(style), b.x() + 1, b.y() + 1, s - 2, fade(0xE0000000 | face & 0xFFFFFF, alpha));
        if (style == HudStyle.Style.GRIMOIRE) {
            // an inked tile: a lighter paper line inside the edge
            g.renderOutline(b.x() + 2, b.y() + 2, s - 4, s - 4, fade(0x40E8D8B0, alpha));
        }
    }

    /** The dark sweep over the icon: it covers {@code left} of the circle from twelve o'clock clockwise. */
    private static void pie(GuiGraphics g, HudLayout.Box b, float left, float alpha) {
        int color = fade(0xB0000000, alpha);
        for (int y = 0; y < 16; y++) {
            int run = -1;
            for (int x = 0; x <= 16; x++) {
                boolean on = x < 16 && PieSweep.covered(x + 0.5 - 8, y + 0.5 - 8, left);
                if (on && run < 0) {
                    run = x;
                } else if (!on && run >= 0) {
                    g.fill(b.x() + 3 + run, b.y() + 3 + y, b.x() + 3 + x, b.y() + 4 + y, color);
                    run = -1;
                }
            }
        }
    }

    /** A ring of blood round the slot, filled clockwise from the top to {@code share}. */
    private static void ring(GuiGraphics g, HudLayout.Box b, float share, float alpha) {
        int cx = b.x() + HudLayout.SLOT / 2;
        int cy = b.y() + HudLayout.SLOT / 2;
        int steps = 72;
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            boolean full = i / (double) steps < share;
            int px = cx + (int) Math.round(Math.sin(a) * 12.5) - 1;
            int py = cy - (int) Math.round(Math.cos(a) * 12.5) - 1;
            g.fill(px, py, px + 2, py + 2, fade(full ? BLOOD : 0x40601020, alpha));
        }
    }

    private static void key(GuiGraphics g, Font font, HudStyle.Style style, HudLayout.Box b, String label, float alpha) {
        if (label.isEmpty()) {
            return;
        }
        String text = label.length() > 3 ? label.substring(0, 3) : label;
        int w = font.width(text);
        var pose = g.pose();
        switch (style) {
            case GRIMOIRE -> {
                // a red wax badge at the foot of the tile
                int cx = b.right() - 7;
                KitDraw.shape(g, Shapes.Shape.ROUND, cx - 4, b.bottom() - 10, 9, fade(0xFF8E1A1A, alpha));
                pose.pushPose();
                pose.translate(cx - w * 0.6f / 2f + 0.5f, b.bottom() - 8, 0);
                pose.scale(0.6f, 0.6f, 1f);
                g.drawString(font, text, 0, 0, fade(0xFFF4E2C0, alpha), false);
                pose.popPose();
            }
            case BLOOD_MOON -> {
                // gold letter on the ring's lower edge
                int cx = b.x() + HudLayout.SLOT / 2;
                pose.pushPose();
                pose.translate(cx - w * 0.65f / 2f, b.bottom() - 6, 0);
                pose.scale(0.65f, 0.65f, 1f);
                g.drawString(font, text, 0, 0, fade(GOLD, alpha), true);
                pose.popPose();
            }
            case TENSURA -> {
                // a small dark tab with a teal edge at the top left
                int tw = Math.max(7, (int) (w * 0.6f) + 4);
                g.fill(b.x() - 1, b.y() - 2, b.x() - 1 + tw, b.y() + 6, fade(0xFF3FD0D6, alpha));
                g.fill(b.x(), b.y() - 1, b.x() - 2 + tw, b.y() + 5, fade(0xFF0A1A28, alpha));
                pose.pushPose();
                pose.translate(b.x() + 1, b.y(), 0);
                pose.scale(0.6f, 0.6f, 1f);
                g.drawString(font, text, 0, 0, fade(0xFF3FD0D6, alpha), false);
                pose.popPose();
            }
            default -> { }
        }
    }
}
