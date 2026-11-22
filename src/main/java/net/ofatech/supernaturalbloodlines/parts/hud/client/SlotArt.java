package net.ofatech.supernaturalbloodlines.parts.hud.client;

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
import net.ofatech.supernaturalbloodlines.shared.hub.kit.KitSprites.Part;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.KitDraw;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.client.UiKit;
import net.ofatech.supernaturalbloodlines.shared.hud.ClientIcons;
import net.ofatech.supernaturalbloodlines.shared.hud.HudFaces;
import net.ofatech.supernaturalbloodlines.shared.hud.HudKit;
import net.ofatech.supernaturalbloodlines.shared.hud.HudStyle;

/**
 * One slot, drawn from the kit: the theme's frame (a square tile in Tensura mode, whatever the theme, since every Tensura icon is square), the icon, the cooldown's
 * pie and seconds, a lock on a skill that is not unlocked, the mastery bar of a Tensura skill, the key badge, the ring of blood round Bite and the mark of Tensura mode.
 * Everything is multiplied by {@code alpha}, which is how the HUD fades when idle. A sprite a resource pack removed is drawn flat in the theme's colours.
 */
final class SlotArt {
    private static final String MODE_MARK = "kit/shared/mode_mark";

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
        int s = HudLayout.SLOT;
        if (v.active()) {
            UiKit.draw(g, kit, framePart(shape, true), b.x(), b.y(), s, s, alpha * (0.75f + 0.25f * pulse));
        } else {
            UiKit.draw(g, kit, framePart(shape, false), b.x(), b.y(), s, s, alpha);
            if (glow > 0f) {
                UiKit.draw(g, kit, framePart(shape, true), b.x(), b.y(), s, s, alpha * glow);
            }
        }
        int lift = v.mastery() != null ? MasteryBar.LIFT : 0;
        if (!v.isEmpty()) {
            boolean lock = locked(v);
            icon(g, v.icon(), b, (lock ? 0.35f : v.greyed() ? 0.45f : 1f) * alpha);
            if (v.cooldown() > 0f) {
                pie(g, b, v.cooldown(), alpha);
                if (v.cooldownSeconds() > 0) {
                    // lifted over the pie: text is drawn in its own pass, which would otherwise put the pie over the number
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    String secs = SlotBadge.timeLabel(v.cooldownSeconds());
                    HudLayout.Box at = SlotBadge.seconds(SlotBadge.Look.valueOf(style.name()), b, font.width(secs), lift);
                    g.drawString(font, secs, at.x(), at.y(), fade(0xFFFFFFFF, alpha), true);
                    g.pose().popPose();
                }
            }
            if (lock) {
                KitDraw.icon(g, Icons.LOCK, b.x() + (s - 10) / 2, b.y() + (s - 10) / 2, 10, fade(0xFFE6E0D0, alpha));
            }
            if (v.mastery() != null) {
                masteryBar(g, kit, style, b, v.mastery(), alpha);
            }
        }
        if (bloodRing >= 0f) {
            int x = b.x() + s / 2 - FrameStrip.RING_SIZE / 2;
            int y = b.y() + s / 2 - FrameStrip.RING_SIZE / 2;
            UiKit.frame(g, FrameStrip.RING, FrameStrip.ringFrame(bloodRing), FrameStrip.RING_SIZE, FrameStrip.RING_STEPS + 1, x, y, alpha);
        }
        key(g, font, kit, style, b, v.keyLabel(), alpha, lift);
        if (shape.marked()) {
            HudLayout.Box mark = SlotBadge.tensuraMark(b);
            UiKit.shared(g, MODE_MARK, mark.x(), mark.y(), mark.w(), mark.h(), alpha);
        }
    }

    /** The slot's picture: our own actions have a 16 px icon in the kit, everything else (Tensura's skills, items) keeps its own texture. */
    private static void icon(GuiGraphics g, ResourceLocation icon, HudLayout.Box b, float alpha) {
        String ours = HudIcons.kitIcon(icon.getNamespace(), icon.getPath(), SupernaturalBloodlines.MODID);
        if (ours != null && UiKit.icon(g, ours, b.x() + 3, b.y() + 3, 16, fade(0xFFFFFFFF, alpha))) {
            return;
        }
        ClientIcons.draw(g, icon, b.x() + 3, b.y() + 3, 16, alpha);
    }

    /** The kit's pie over the icon: it covers {@code left} of the circle from twelve o'clock clockwise; a dimmed icon when the strip is not there. */
    private static void pie(GuiGraphics g, HudLayout.Box b, float left, float alpha) {
        int frame = FrameStrip.sweepFrame(left);
        if (!UiKit.frame(g, FrameStrip.SWEEP, frame, FrameStrip.SWEEP_SIZE, FrameStrip.SWEEP_STEPS + 1, b.x() + 3, b.y() + 3, alpha)) {
            g.fill(b.x() + 3, b.y() + 3, b.x() + 19, b.y() + 19, fade(0x80000000, alpha));
        }
    }

    /** The thin mastery bar of a Tensura skill: the kit's track and its fill (red or teal while it grows, gold once mastered, grey-violet while a resist skill is being learned). */
    private static void masteryBar(GuiGraphics g, HudKit kit, HudStyle.Style style, HudLayout.Box slot, Mastery m, float alpha) {
        HudLayout.Box back = MasteryBar.box(slot);
        HudLayout.Box in = MasteryBar.inner(slot);
        UiKit.draw(g, kit, Part.MASTERY_TRACK, back.x(), back.y(), back.w(), back.h(), alpha);
        int fill = MasteryBar.fill(m, in.w());
        if (fill <= 0) {
            return;
        }
        Part part = fillPart(m.state());
        if (UiKit.has(kit.kit(), part)) {
            UiKit.draw(g, kit, part, in.x(), in.y(), fill, in.h(), alpha);
        } else {
            g.fill(in.x(), in.y(), in.x() + fill, in.bottom(), fade(MasteryBar.color(SlotBadge.Look.valueOf(style.name()), m.state()), alpha));
        }
    }

    static Part fillPart(Mastery.State state) {
        return switch (state) {
            case MASTERED -> Part.MASTERY_FILL_MASTERED;
            case LEARNING -> Part.MASTERY_FILL_LEARNING;
            case PROGRESS -> Part.MASTERY_FILL;
        };
    }

    private static void key(GuiGraphics g, Font font, HudKit kit, HudStyle.Style style, HudLayout.Box slot, String label, float alpha, int lift) {
        if (label.isEmpty()) {
            return;
        }
        // at the normal size (nothing is under 8 GUI pixels): a letter, or the first two characters of a longer key name
        String text = label.length() > 2 ? label.substring(0, 2) : label;
        int w = font.width(text);
        HudLayout.Box badge = SlotBadge.key(SlotBadge.Look.valueOf(style.name()), slot, w, lift);
        Part part = badge.w() == SlotBadge.BADGE_W ? Part.KEY_BADGE : Part.KEY_BADGE_WIDE;
        UiKit.draw(g, kit, part, badge.x(), badge.y(), badge.w(), badge.h(), alpha);
        g.drawString(font, text, badge.x() + (badge.w() - w) / 2, badge.y() + 2, fade(UiKit.labelOver(kit, part, HudFaces.keyLetter(kit)), alpha), false);
    }
}