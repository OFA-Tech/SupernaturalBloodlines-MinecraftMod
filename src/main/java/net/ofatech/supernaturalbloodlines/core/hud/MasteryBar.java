package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look;

/**
 * Where a slot's mastery bar stands and what colour it is, per hub theme. Tensura mode only (Supernatural mode has no mastery), where every slot is a square.
 * The bar runs along the top inside edge, clear of the key badge (Grimoire's seal and Blood Moon's plate are at the bottom; the Tensura theme's tab hangs off the
 * top left, so its bar starts to the right of the tab) and of the mode diamond outside the top right corner. Pure, so the boxes are tested not to cover those.
 */
public final class MasteryBar {
    public static final int GOLD = 0xFFFFD36B;
    public static final int LEARNING = 0xFF8E8AA8;
    public static final int TRACK = 0xFF05080C;

    private MasteryBar() {
    }

    /**
     * The bar, 3 px tall on the slot's top edge: the top row is its dark backing (over the frame line), the two rows under it are the track and the fill. It
     * ends above the first row of the cooldown's digits (the font leaves its first pixel row empty), which the Grimoire and Blood Moon themes draw at the top.
     * {@code labelW} is the key text's width, which sets how wide the Tensura theme's tab is.
     */
    public static Box box(Look look, Box slot, int labelW) {
        int left = look == Look.TENSURA ? Math.min(HudLayout.SLOT - 9, labelW + 4) : 4;
        return new Box(slot.x() + left, slot.y(), HudLayout.SLOT - 3 - left, 3);
    }

    /** The track inside the bar, 2 px tall. */
    public static Box inner(Look look, Box slot, int labelW) {
        Box b = box(look, slot, labelW);
        return new Box(b.x() + 1, b.y() + 1, b.w() - 2, 2);
    }

    /** Whole pixels of {@code innerWidth} to fill: at least one for any progress at all, all of it only when mastered or the fraction is full. */
    public static int fill(Mastery m, int innerWidth) {
        if (m.mastered() || m.fraction() >= 1f) {
            return innerWidth;
        }
        if (m.fraction() <= 0f) {
            return 0;
        }
        return Math.max(1, Math.min(innerWidth - 1, Math.round(m.fraction() * innerWidth)));
    }

    public static int color(Look look, Mastery.State state) {
        return switch (state) {
            case MASTERED -> GOLD;
            case LEARNING -> LEARNING;
            case PROGRESS -> switch (look) {
                case GRIMOIRE -> 0xFFC0392B;
                case BLOOD_MOON -> 0xFFE2493F;
                case TENSURA -> 0xFF3FD0D6;
            };
        };
    }
}
