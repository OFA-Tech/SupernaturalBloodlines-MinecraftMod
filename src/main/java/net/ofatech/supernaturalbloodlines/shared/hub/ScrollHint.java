package net.ofatech.supernaturalbloodlines.shared.hub;

/** Whether a scrolled region has more below what is shown: the page then says so, so nobody takes a long chapter for a short one. Pure. */
public final class ScrollHint {
    /** The strip under a region that holds more than fits, for the "more below" marker: the content is clipped above it, so the marker never covers text. */
    public static final int GUTTER = 10;

    private ScrollHint() {
    }

    public static boolean moreBelow(int offset, int max) {
        return max > 0 && offset < max;
    }

    /** Whether content of {@code contentH} pixels overflows a viewport of {@code fullH}: it then gives up the {@link #GUTTER} (and scrolls to show it all above it). */
    public static boolean overflows(int contentH, int fullH) {
        return contentH > fullH;
    }

    /** Whether a row {@code h} tall standing at {@code y} is whole above {@code clipBottom}: a row that is not is left to the scroll, not drawn half under the marker. */
    public static boolean rowFits(int y, int h, int clipBottom) {
        return y + h <= clipBottom;
    }

    /** How many of the first lines of a block of {@code count} lines {@code h} tall, from {@code y}, are whole above {@code clipBottom}: the page draws only those. */
    public static int wholeLines(int y, int h, int count, int clipBottom) {
        int n = 0;
        while (n < count && rowFits(y + n * h, h, clipBottom)) {
            n++;
        }
        return n;
    }

    /** The height a viewport of {@code fullH} clips its content to. */
    public static int clipHeight(int fullH, boolean overflowed) {
        return overflowed ? Math.max(0, fullH - GUTTER) : fullH;
    }
}
