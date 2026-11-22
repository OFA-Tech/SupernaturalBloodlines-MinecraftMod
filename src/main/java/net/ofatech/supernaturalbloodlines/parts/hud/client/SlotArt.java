package net.ofatech.supernaturalbloodlines.parts.hud.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.HudIcons;
import net.ofatech.supernaturalbloodlines.core.hud.HudLayout;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.hud.MasteryBar;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge;
import net.ofatech.supernaturalbloodlines.core.hud.SlotShape;
import net.ofatech.supernaturalbloodlines.core.hud.SlotView;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.FrameStrip;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Icons;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudFaces;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * One slot, drawn from the kit: the theme's frame (a square tile in Tensura mode, whatever the theme, since every Tensura icon is square), the icon, the ready
 * glow, the cooldown's pie and seconds, a lock on a skill that is not unlocked, the mastery bar of a Tensura skill, the key badge, the ring of blood round Bite and
 * the mark of Tensura mode. Everything is multiplied by {@code alpha}, which is how the HUD fades when idle. A sprite a resource pack removed is drawn flat in
 * the theme's colours.
 */
final class SlotArt {
    private static final int BLOOD = 0xFFC01830;
    private static final int RIM = 0xFF0A1A28;

    private SlotArt() {
    }

    static int fade(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return a << 24 | (argb & 0xFFFFFF);
    }

    /** The frame of a slot: Tensura mode is a square tile in every theme (every Tensura icon is square), Supernatural mode takes the theme's own picture (a tile, a round, a hexagon). */
    static Part framePart(SlotShape shape, boolean active) {
        boolean square = shape == SlotShape.SQUARE;
        return active ? (square ? Part.SLOT_SQUARE_ACTIVE : Part.SLOT_ACTIVE) : (square ? Part.SLOT_SQUARE : Part.SLOT);
    }

    /** True for a slot that cannot be used because the skill or action is not unlocked (it gets a lock), not for one that is only on cooldown. */
    static boolean locked(SlotView v) {
        String r = v.greyedReason();
        return r != null && (r.endsWith("skill_missing") || r.endsWith("slot.locked") || r.endsWith("not_unlocked") || r.endsWith("action_missing"));
    }

    /**
     * @param bloodRing 0 to 1 of a ring of blood round the slot, or a negative number for none (only Bite has one)
     * @param pulse     0 to 1, the active frame's breathing
     * @param glow      0 to 1, the glow of a slot that has just become ready
     */
    static void draw(GuiGraphics g, Font font, HudStyle.Style style, SlotShape shape, HudLayout.Box b, SlotView v, float alpha, float bloodRing, float pulse, float glow) {
        HudKit kit = HudKit.of(style);
        SlotBadge.Look look = SlotBadge.placement(SlotBadge.Look.valueOf(style.name()), shape == SlotShape.SQUARE);
        int s = HudLayout.SLOT;
        if (v.active()) {
            UiKit.draw(g, kit, framePart(shape, true), b.x(), b.y(), s, s, alpha * (0.75f + 0.25f * pulse));
        } else {
            UiKit.draw(g, kit, framePart(shape, false), b.x(), b.y(), s, s, alpha);
        }
        int lift = v.mastery() != null ? MasteryBar.LIFT : 0;
        if (!v.isEmpty()) {
            boolean lock = locked(v);
            icon(g, v.icon(), b, (lock ? 0.35f : v.greyed() ? 0.45f : 1f) * alpha);
            if (glow > 0f) {
                readyGlow(g, b, glow * alpha);
            }
            if (v.cooldown() > 0f) {
                pie(g, b, v.cooldown(), alpha);
                if (v.cooldownSeconds() > 0) {
                    seconds(g, font, look, b, SlotBadge.timeLabel(v.cooldownSeconds()), lift, alpha);
                }
            }
            if (lock) {
                KitDraw.icon(g, Icons.LOCK, b.x() + (s - 10) / 2, b.y() + (s - 10) / 2, 10, fade(0xFFE6E0D0, alpha));
            }
            if (v.mastery() != null) {
                masteryBar(g, kit, look, b, v.mastery(), alpha);
            }
        }
        if (bloodRing >= 0f) {
            bloodRing(g, b, bloodRing, alpha);
        }
        key(g, font, kit, look, b, v.keyLabel(), alpha, lift);
        if (shape.marked()) {
            HudLayout.Box mark = SlotBadge.tensuraMark(b);
            UiKit.shared(g, KitSprites.MODE_MARK, mark.x(), mark.y(), mark.w(), mark.h(), alpha);
        }
    }

    /** The slot's picture: our own actions have a HUD icon in the kit (a pale outline, so they read on the dark slots), everything else keeps its own texture. */
    private static void icon(GuiGraphics g, ResourceLocation icon, HudLayout.Box b, float alpha) {
        String ours = HudIcons.kitIcon(icon.getNamespace(), icon.getPath(), SupernaturalBloodlines.MODID);
        if (ours != null && UiKit.shared(g, KitSprites.HUD_ICON + ours, b.x() + 3, b.y() + 3, 16, 16, alpha)) {
            return;
        }
        ClientIcons.draw(g, icon, b.x() + 3, b.y() + 3, 16, alpha);
    }

    /** A slot that has just become ready: a gold outline round its icon and a pale wash over it, for one breath. Its own look, not the active frame's. */
    private static void readyGlow(GuiGraphics g, HudLayout.Box b, float glow) {
        g.fill(b.x() + 3, b.y() + 3, b.x() + 19, b.y() + 19, fade(0x80FFFFFF, glow * 0.5f));
        g.renderOutline(b.x() + 2, b.y() + 2, 18, 18, fade(MasteryBar.GOLD, Math.min(1f, glow * 1.6f)));
    }

    /** The cooldown's seconds over the pie; smaller (and at the top) when the badge is lifted over a mastery bar, so they never share a row with it. */
    private static void seconds(GuiGraphics g, Font font, SlotBadge.Look look, HudLayout.Box b, String secs, int lift, float alpha) {
        float scale = SlotBadge.secondsScale(look, lift);
        HudLayout.Box at = SlotBadge.seconds(look, b, Math.round(font.width(secs) * scale), lift);
        // lifted over the pie: text is drawn in its own pass, which would otherwise put the pie over the number
        g.pose().pushPose();
        g.pose().translate(at.x(), at.y(), 200);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, secs, 0, 0, fade(0xFFFFFFFF, alpha), true);
        g.pose().popPose();
    }

    /** The kit's veil over the icon: it covers {@code left} of the circle from twelve o'clock clockwise; a plain dim when the strip is not there. */
    private static void pie(GuiGraphics g, HudLayout.Box b, float left, float alpha) {
        int frame = FrameStrip.sweepFrame(left);
        if (!UiKit.frame(g, FrameStrip.SWEEP, frame, FrameStrip.SWEEP_SIZE, FrameStrip.SWEEP_STEPS + 1, b.x() + 3, b.y() + 3, alpha)) {
            g.fill(b.x() + 3, b.y() + 3, b.x() + 19, b.y() + 19, fade(0x80000000, alpha));
        }
    }

    /** The ring of blood round Bite: the kit's strip, or, when a pack changed its size, the same share as an outline round the slot. */
    private static void bloodRing(GuiGraphics g, HudLayout.Box b, float share, float alpha) {
        int s = HudLayout.SLOT;
        int x = b.x() + s / 2 - FrameStrip.RING_SIZE / 2;
        int y = b.y() + s / 2 - FrameStrip.RING_SIZE / 2;
        if (UiKit.frame(g, FrameStrip.RING, FrameStrip.ringFrame(share), FrameStrip.RING_SIZE, FrameStrip.RING_STEPS + 1, x, y, alpha)) {
            return;
        }
        int ox = b.x() - 3;
        int oy = b.y() - 3;
        int size = s + 6;
        g.renderOutline(ox, oy, size, size, fade(0x40601020, alpha));
        for (int[] r : FrameStrip.outlineSegments(size, size, share)) {
            g.fill(ox + r[0], oy + r[1], ox + r[0] + r[2], oy + r[1] + r[3], fade(BLOOD, alpha));
        }
    }

    /**
     * The thin mastery bar of a Tensura skill: the kit's track and its fill (red or teal while it grows, gold once mastered, grey-violet while a resist skill is
     * being learned). The state is also in the shape: a learning bar is hollow (every other pixel column), a mastered one has a diamond.
     */
    private static void masteryBar(GuiGraphics g, HudKit kit, SlotBadge.Look look, HudLayout.Box slot, Mastery m, float alpha) {
        HudLayout.Box back = MasteryBar.box(slot);
        HudLayout.Box in = MasteryBar.inner(slot);
        UiKit.draw(g, kit, Part.MASTERY_TRACK, back.x(), back.y(), back.w(), back.h(), alpha);
        int fill = MasteryBar.fill(m, in.w());
        Part part = fillPart(m.state());
        boolean sprite = UiKit.has(kit.kit(), part);
        int color = fade(MasteryBar.color(look, m.state()), alpha);
        for (int c = 0; c < fill; c++) {
            if (!MasteryBar.lit(m.state(), c)) {
                continue;
            }
            // a solid bar is one piece; a hollow one a column at a time
            int run = m.state() == Mastery.State.LEARNING ? 1 : fill;
            if (sprite) {
                UiKit.draw(g, kit, part, in.x() + c, in.y(), run, in.h(), alpha);
            } else {
                g.fill(in.x() + c, in.y(), in.x() + c + run, in.bottom(), color);
            }
            if (run == fill) {
                break;
            }
        }
        if (m.state() == Mastery.State.MASTERED) {
            diamond(g, MasteryBar.diamond(slot), alpha);
        }
    }

    /** A gold diamond with a dark rim, 5 x 5: it reads on any frame. */
    private static void diamond(GuiGraphics g, HudLayout.Box d, float alpha) {
        for (int dy = -2; dy <= 2; dy++) {
            int half = 2 - Math.abs(dy);
            g.fill(d.x() + 2 - half, d.y() + 2 + dy, d.x() + 3 + half, d.y() + 3 + dy, fade(RIM, alpha));
        }
        for (int dy = -1; dy <= 1; dy++) {
            int half = 1 - Math.abs(dy);
            g.fill(d.x() + 2 - half, d.y() + 2 + dy, d.x() + 3 + half, d.y() + 3 + dy, fade(MasteryBar.GOLD, alpha));
        }
    }

    static Part fillPart(Mastery.State state) {
        return switch (state) {
            case MASTERED -> Part.MASTERY_FILL_MASTERED;
            case LEARNING -> Part.MASTERY_FILL_LEARNING;
            case PROGRESS -> Part.MASTERY_FILL;
        };
    }

    private static void key(GuiGraphics g, Font font, HudKit kit, SlotBadge.Look look, HudLayout.Box slot, String label, float alpha, int lift) {
        if (label.isEmpty()) {
            return;
        }
        // a letter, or two characters of a longer key name ("LS" for Left Shift, "M4" for Mouse Button 4): the badge is one or two characters wide
        String text = SlotBadge.shortKey(label);
        int w = font.width(text);
        HudLayout.Box badge = SlotBadge.key(look, slot, w, lift);
        Part part = badge.w() == SlotBadge.BADGE_W ? Part.KEY_BADGE : Part.KEY_BADGE_WIDE;
        UiKit.draw(g, kit, part, badge.x(), badge.y(), badge.w(), badge.h(), alpha);
        g.drawString(font, text, badge.x() + (badge.w() - w) / 2, badge.y() + 2, fade(UiKit.labelOver(kit, part, HudFaces.keyLetter(kit)), alpha), false);
    }

    /** The segments of an outline: see {@link FrameStrip#outlineSegments}. */
    static List<int[]> segments(int w, int h, float share) {
        return FrameStrip.outlineSegments(w, h, share);
    }
}
