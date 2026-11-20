package net.ofatech.supernaturalbloodlines.core.hud;

/**
 * The outline of a slot. Every Tensura skill icon is square, so Tensura mode draws square tiles (in the colours and with the key badge of the hub theme);
 * Supernatural mode keeps the theme's own shape (a tile for Grimoire, round for Blood Moon, a hexagon for Tensura). Pure.
 */
public enum SlotShape {
    THEMED,
    SQUARE;

    /** Tensura mode is also marked (a teal frame and a corner diamond, in every theme), so the mode reads without the pill: in Grimoire both modes are square. */
    public boolean marked() {
        return this == SQUARE;
    }

    public static SlotShape of(SlotMode mode) {
        return mode == SlotMode.TENSURA ? SQUARE : THEMED;
    }
}
