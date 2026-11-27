package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * A row of short labels under equal cells (the moon calendar's days). When a label is wider than its cell, every other one is left out so the
 * rest have two cells; a label is kept inside the row. Pure.
 */
public final class LabelRow {
    /** The space kept between two labels. */
    public static final int GAP = 2;

    private LabelRow() {
    }

    /** Whether the row must thin: a label (the widest) does not fit its own cell. */
    public static boolean thin(int widest, int cell) {
        return widest > cell - GAP;
    }

    /** Whether the label of column {@code col} is drawn. */
    public static boolean shown(int col, boolean thin) {
        return !thin || col % 2 == 0;
    }

    /** The left x of a label of {@code width} centred under its cell and kept inside {@code [left, left + cols * cell)}. */
    public static int x(int left, int col, int cell, int cols, int width) {
        int centred = left + col * cell + (cell - width) / 2;
        return Math.max(left, Math.min(centred, left + cols * cell - width));
    }
}
