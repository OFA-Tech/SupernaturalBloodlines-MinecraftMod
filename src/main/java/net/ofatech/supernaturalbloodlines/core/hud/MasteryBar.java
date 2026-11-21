package net.ofatech.supernaturalbloodlines.core.hud;

import net.ofatech.supernaturalbloodlines.core.hud.HudLayout.Box;
import net.ofatech.supernaturalbloodlines.core.hud.SlotBadge.Look;

/**
 * Where a slot's mastery bar stands and what colour it is, per hub theme. Tensura mode only (Supernatural mode has no mastery), where every slot is a square.
 * The bar runs along the bottom edge inside the frame: a dark backing 4 px tall with a 2 px track inside it, filled left to right. The key badge and the
 * cooldown's seconds that would stand there are lifted by {@link #LIFT} (see {@code SlotBadge}), so nothing covers the bar. Pure.
 */
public final class MasteryBar {
    public static final int GOLD = 0xFFFFD36B;
    public static final int LEARNING = 0xFF8E8AA8;
    public static final int TRACK = 0xFF05080C;
    /** The track the fill runs along; every fill colour is at least 3:1 against it ({@code MasteryTest}). */
    public static final int TRACK_INNER = 0xFF2A2F36;
    /** The backing's height. */
    public static final int HEIGHT = 4;
    /** How far the key badge and the seconds are lifted: the backing and a pixel of air. */
    public static final int LIFT = HEIGHT + 1;

    private MasteryBar() {
    }

    /** The dark backing: the slot's width less the frame and a pixel each side, a pixel above the bottom frame line. */
    public static Box box(Box slot) {
        return new Box(slot.x() + 2, slot.bottom() - 1 - HEIGHT, HudLayout.SLOT - 4, HEIGHT);
    }

    /** The track inside the backing, 2 px tall; the fill runs along it. */
    public static Box inner(Box slot) {
        Box b = box(slot);
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
                case GRIMOIRE -> 0xFFE0554A;
                case BLOOD_MOON -> 0xFFE2493F;
                case TENSURA -> 0xFF3FD0D6;
            };
        };
    }
}
