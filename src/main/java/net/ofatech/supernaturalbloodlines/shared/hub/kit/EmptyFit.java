package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/** How tall an empty state is, and whether it drops its hexagon and caption to fit the room it has (GUI scale 4 on a small window). Pure. */
public final class EmptyFit {
    public static final int HEX = 34;

    private EmptyFit() {
    }

    /** The height from {@code top} to the bottom of its last button: the hexagon and the caption (when {@code art}), the title, {@code bodyLines} of text, one or two buttons. */
    public static int height(boolean art, boolean caption, int bodyLines, boolean second) {
        int h = 6;
        if (art) {
            h += HEX + 4 + (caption ? 10 : 0);
        }
        h += 6 + 16 + bodyLines * 10 + 8 + 16;
        if (second) {
            h += 6 + 16;
        }
        return h + 6;
    }

    /** Whether the hexagon and caption are drawn: when the room runs to {@code bottom} and the whole would not fit in it (a region with no height, {@code bottom <= top}, is unbounded). */
    public static boolean showArt(int top, int bottom, boolean caption, int bodyLines, boolean second) {
        return bottom <= top || top + height(true, caption, bodyLines, second) <= bottom;
    }
}