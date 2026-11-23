package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * How many task rows of a detail pane to draw, and how tall each is, in the room the pane has. A pane that is short must not show one task and silently drop the
 * rest: the rows are first drawn at the size they asked for, then (each with one line of text) in the small size, and what still does not fit is replaced by a line
 * "+N more" that is itself given room. Pure.
 */
public final class DetailRows {
    /**
     * {@code shown} rows are drawn, the first {@code shown}, each {@code compact} (one line of label) or at its own height; {@code more} are left out (0 when all fit);
     * {@code moreLine}: the "+N more" line has room (a pane with no room even for it says nothing rather than draw it over the buttons).
     */
    public record Plan(int shown, boolean compact, int more, boolean moreLine) {
    }

    private DetailRows() {
    }

    /**
     * @param full    each row's height at its own number of label lines
     * @param compact each row's height with one line of label
     * @param room    the pixels the rows may take
     * @param gap     the space after a row
     * @param moreH   the height of the "+N more" line, kept free when rows are left out
     */
    public static Plan plan(int[] full, int[] compact, int room, int gap, int moreH) {
        int n = full.length;
        if (total(full, n, gap) <= room) {
            return new Plan(n, false, 0, false);
        }
        if (total(compact, n, gap) <= room) {
            return new Plan(n, true, 0, false);
        }
        int shown = 0;
        int used = 0;
        for (int i = 0; i < n; i++) {
            if (used + compact[i] + gap + moreH > room) {
                break;
            }
            used += compact[i] + gap;
            shown++;
        }
        int left = n - shown;
        return new Plan(shown, true, left, left > 0 && used + moreH <= room);
    }

    private static int total(int[] heights, int n, int gap) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += heights[i] + gap;
        }
        return sum;
    }
}
